from pathlib import Path
import json,base64
R=Path(__file__).resolve().parents[1]
images={f'{s}{n}':'data:image/png;base64,'+base64.b64encode((R/f'exports/battery-{s}-{n}.png').read_bytes()).decode() for n in [16,32] for s in ['empty','full']}
for n in [16,32]: images[f'physical{n}']='data:image/png;base64,'+base64.b64encode((R/f'references/battery-item-{n}.png').read_bytes()).decode()
regions=json.loads((R/'exports/energy-regions.json').read_text())['regions']
physical=json.loads((R/'references/battery-item-energy-regions.json').read_text())
print('physical schema:',list(physical))
html='''<!doctype html><html lang="zh-CN"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Wildcraft · 电量显示 v01</title>
<style>
*{box-sizing:border-box}body{margin:0;background:#121d18;color:#e4eadd;font:15px system-ui,"PingFang SC",sans-serif}main{max-width:1240px;margin:auto;padding:32px 24px}h1{font-size:28px;margin:0 0 12px}p{line-height:1.8;color:#b5c2b6}.tag{color:#e2bb75}.controls{display:flex;flex-wrap:wrap;gap:16px;align-items:center;background:#223128;padding:18px;border:1px solid #405545;border-radius:8px}label{display:flex;gap:8px;align-items:center}select,input{accent-color:#be402d;color:inherit;background:#17261d;border:1px solid #61705f;padding:6px;border-radius:3px}input[type=range]{width:230px;padding:0}input[type=number]{width:90px}button{background:#324636;color:#e3e9df;border:1px solid #62765f;border-radius:4px;padding:8px 12px;cursor:pointer}button:hover{background:#435c46}.samples{display:flex;flex-wrap:wrap;gap:8px;margin:16px 0}.live{display:grid;grid-template-columns:1fr 1fr;gap:18px}.panel{background:#223128;border:1px solid #405545;padding:18px;border-radius:8px;min-width:0}.panel h2{font-size:17px;margin:0 0 18px}.pixel{image-rendering:pixelated;display:block}.preview-pair{display:flex;gap:28px;align-items:center;min-height:132px;overflow:auto}.muted{color:#a8b7ab;font-size:13px}.nowrap{overflow:auto}.board{width:100%;height:auto;display:block;margin:24px 0}#semantic{color:#e5b96c}output{font-variant-numeric:tabular-nums}a{color:#c8d4b7}@media(max-width:1050px){.live{grid-template-columns:1fr}main{padding:20px 14px}}
</style><main>
<span class="tag">F04-02 · v01 · 新显示规范待审 / 端点原图已采用 / 未接入游戏</span><h1>三格电量，保留准确余量</h1>
<p>沿用已采用的金属外框、铜接点和红石窗口。窗口由下向上填格，格内从左向右；中间电量复用满电图的像素。数字读取 0–1000 的实际余量。</p>
<div class="controls"><label>余量 <input id="energy" type="range" min="0" max="1000" value="500"></label><label>精确值 <input id="number" type="number" min="0" max="1000" value="500"></label>
<label>原生尺寸<select id="size"><option value="16">16 × 16</option><option value="32">32 × 32</option></select></label><label>显示倍数<select id="scale"><option>1</option><option>2</option><option selected>4</option><option>8</option></select></label>
<label>底色<select id="theme"><option value="dark">深色</option><option value="light">浅色</option></select></label><label>场景<select id="scenario"><option value="2">充电中</option><option value="1">缺少固定红石源</option><option value="4">等待共享供电</option><option value="3">已充满</option><option value="0">没有电池</option></select></label><label>文字<select id="language"><option value="zh_cn">中文</option><option value="en_us">English</option></select></label></div>
<div class="samples" id="samples"></div><div class="live">
<section class="panel"><h2>原图遮罩与读数</h2><div class="preview-pair"><canvas class="pixel" id="icon"></canvas><div><canvas class="pixel" id="physical"></canvas><p class="muted">已采用电池物品图</p></div></div><div class="nowrap"><canvas class="pixel" id="readout" width="112" height="20"></canvas></div><p id="semantic"></p><p class="muted" id="widths"></p></section>
<section class="panel"><h2>充电器内的读数位置模拟</h2><div class="nowrap"><canvas class="pixel" id="charger" width="176" height="80"></canvas></div><p id="status"></p><p class="muted">沿用真实电池槽、读数区与进度条位置。本页只核对电量组件；完整 176 × 166 充电器界面安排在 F04-04。浏览器文字为近似显示。</p></section></div>
<canvas id="board" class="board" width="1200" height="1510"></canvas>
<p>16px 窗口只有 5 列、32px 只有 9 列，图形按原生像素跳变；非常低的非零余量仍显示准确数字，999 不提前显示满电。无电池显示“— / 1000”，不冒充空电池。此次不新增常驻玩家 HUD。</p></main>
<script>__READOUT__</script><script>__PHYSICAL__</script><script>
const imageSources=__IMAGES__,regions=__REGIONS__,physicalRegions=__PHYSICAL_REGIONS__,labels=__LABELS__;
const B=WildcraftBatteryReadout,P=WildcraftBatteryPreview,assets={},samples=[0,1,38,67,99,100,333,334,500,666,667,999,1000];
function get(id){return document.getElementById(id)}
const semantic={no_battery:'没有电池 · 不是空电池',empty:'空电 · 0 / 1000',low:'低电量 · 仍保留真实余量',partial:'部分电量',full:'满电 · 1000 / 1000'};
function text(c,s,x,y,size=18,color='#dfe7d8',weight=400){c.fillStyle=color;c.font=weight+' '+size+'px system-ui, "PingFang SC", sans-serif';c.fillText(s,x,y);}
function panel(c,x,y,w,h){c.fillStyle='#24362a';c.fillRect(x,y,w,h);c.strokeStyle='#435743';c.strokeRect(x+.5,y+.5,w-1,h-1);}
function drawCharger(c,e,present,status,lang='zh_cn'){
 c.clearRect(0,0,176,80);c.fillStyle='#b4c2b5';c.fillRect(0,0,176,80);c.fillStyle='#d3ddd1';c.fillRect(3,3,170,77);c.fillStyle='#354438';c.font='8px system-ui';c.fillText(lang==='zh_cn'?'充电器':'Charger',8,14);
 c.fillStyle='#637066';c.fillRect(25,34,18,18);c.fillStyle='#9aa99d';c.fillRect(26,35,16,16);
 if(present){P.drawIcon(c,assets.physical16,physicalRegions['16'],16,e,26,35,1);c.fillStyle=e<100?'#d8a24d':'#7aae83';c.fillRect(28,48,Math.round(13*e/1000),1);}
 B.drawReadout(c,{empty:assets.empty16,full:assets.full16},regions['16'],e,present,54,29,1,'light',status);
 c.fillStyle='#637066';c.fillRect(54,47,107,7);c.fillStyle='#608865';if(present)c.fillRect(55,48,Math.floor(105*e/1000),5);
 c.fillStyle='#354438';c.font='7px system-ui';let str=labels[lang]['energy.wildcraft.status.'+status];c.fillText(str,8,71);
 return {slot:[26,35],icon:[54,29],number:[74,32],barWidth:present?Math.floor(105*e/1000):0,text: B.reading(e,present,status).text,statusText:str};
}
function setEnergy(e){e=B.energyValue(e);if(get('scenario').value==='3'&&e<1000)get('scenario').value='2';get('energy').value=get('number').value=e;update();}
function update(){
 let e=B.energyValue(+get('number').value),status=+get('scenario').value;const present=status!==0;if(status===3)e=1000;else if(present&&e===1000)status=3;
 get('energy').value=get('number').value=e;get('scenario').value=status;
 const n=+get('size').value,scale=+get('scale').value,theme=get('theme').value,lang=get('language').value;
 const bg=theme==='dark'?'#15251c':'#d3ddd1';for(const id of ['icon','physical']){const c=get(id);c.width=c.height=n;c.style.width=c.style.height=n*scale+'px';c.style.background=bg;}
 let masked=null;if(present){masked=B.drawStatusIcon(get('icon').getContext('2d'),assets['empty'+n],assets['full'+n],regions[n],e);P.drawIcon(get('physical').getContext('2d'),assets['physical'+n],physicalRegions[n],n,e);}
 const rc=get('readout');rc.style.width=112*scale+'px';rc.style.height=20*scale+'px';const ctx=rc.getContext('2d');ctx.fillStyle=bg;ctx.fillRect(0,0,112,20);const r=B.drawReadout(ctx,{empty:assets.empty16,full:assets.full16},regions[16],e,present,0,2,1,theme,status);
 get('semantic').textContent=semantic[r.semantic];get('widths').textContent=present?'三格份额（下→上）：'+B.fractions(e).map(v=>Math.round(v*1000)/10+'%').join(' / ')+'；'+n+'px 显示列数：'+masked.widthsBottomUp.join(' / '):'图标隐藏；余量占位，不读取为零电量';
 const cc=get('charger');cc.style.width=176*2+'px';cc.style.height=80*2+'px';const charger=drawCharger(cc.getContext('2d'),e,present,status,lang);get('status').textContent=charger.statusText;
 window.currentState={e,present,status,n,scale,theme,readout:r,charger};
}
function drawBoard(){
 const c=get('board').getContext('2d');c.imageSmoothingEnabled=false;c.fillStyle='#15251c';c.fillRect(0,0,1200,1510);
 text(c,'WILDCRAFT / ENERGY',38,49,16,'#a6b998',600);text(c,'电量显示 · 三格遮罩与准确读数',38,95,32,'#e4ead9',600);text(c,'F04-02 v01  ·  端点原图已采用  ·  新映射与布局待审  ·  未接入游戏',38,128,17,'#dfb574');
 panel(c,38,160,552,164);panel(c,610,160,552,164);
 text(c,'实际显示组件 · 500 / 1000',58,190,17,'#b9cbb0');B.drawReadout(c,{empty:assets.empty16,full:assets.full16},regions[16],500,true,70,215,4);
 text(c,'同一电量 · 已采用电池物品图',630,190,17,'#b9cbb0');P.drawIcon(c,assets.physical32,physicalRegions[32],32,500,635,197,3);text(c,'逻辑：下格满 / 中格50% / 上格空',760,248,18);text(c,'图标按原生整数列取整',760,278,16,'#acbea4');
 text(c,'端点保留；中间电量只覆盖能量窗口',38,370,23,'#e4ead9',600);text(c,'原生 16px 与 32px 分别使用自己的矩形，不缩小高分辨率图。',38,402,17,'#acbea4');
 const es=[0,1,99,100,500,999,1000],xs=es.map((_,i)=>42+i*159);for(let i=0;i<es.length;i++){panel(c,xs[i],425,149,362);text(c,es[i]+' / 1000',xs[i]+12,454,17,es[i]<100?'#e5b96c':'#dfe7d8',600);text(c,'16px × 4',xs[i]+14,484,14,'#acbea4');B.drawStatusIcon(c,assets.empty16,assets.full16,regions[16],es[i],xs[i]+42,500,4);text(c,'32px × 3',xs[i]+14,602,14,'#acbea4');B.drawStatusIcon(c,assets.empty32,assets.full32,regions[32],es[i],xs[i]+26,615,3);text(c,es[i]===0?'空电':es[i]===1000?'满电':es[i]<100?'低电量':'部分电量',xs[i]+14,764,16,es[i]<100?'#e5b96c':'#c9d7c0');}
 panel(c,38,809,1124,101);text(c,'1 / 1000 仍有余量；图形看似空，也不会被标为“空电”。',58,845,20,'#e5b96c');text(c,'999 / 1000 保留顶格末列空隙；满电端点必须等到 1000。',58,880,18,'#c9d7c0');
 text(c,'五种充电状态 · 仅作电量位置模拟',38,954,23,'#e4ead9',600);text(c,'真实单槽与进度条位置保留；完整充电器界面在 F04-04 制作。',38,986,17,'#acbea4');
 const cases=[{title:'没有电池',e:0,p:false,s:0},{title:'缺少电源 · 保留 500',e:500,p:true,s:1},{title:'充电中 · 500',e:500,p:true,s:2},{title:'已充满 · 1000',e:1000,p:true,s:3},{title:'等待共享供电 · 保留 500',e:500,p:true,s:4}];
 cases.forEach((s,i)=>{const x=38+(i%3)*382,y=1010+Math.floor(i/3)*207;panel(c,x,y,360,192);text(c,s.title,x+12,y+28,17,'#c9d7c0');const temp=document.createElement('canvas');temp.width=176;temp.height=80;drawCharger(temp.getContext('2d'),s.e,s.p,s.s);c.drawImage(temp,x+4,y+32,352,160);});
 text(c,'统一：同一 E / 1000 → 三格逻辑份额 → 各尺寸原生像素遮罩',38,1460,18,'#c9d7c0');text(c,'本页为浏览器审稿；字体近似，未进行游戏界面验证。',38,1490,15,'#94a78d');
}
async function checkPixels(){
 const cases=[],bounds=[0,1,37,38,66,67,99,100,333,334,500,666,667,999,1000];
 for(const n of [16,32]){
  const cv=document.createElement('canvas');cv.width=cv.height=n;const cc=cv.getContext('2d',{willReadFrequently:true});const bytes=img=>{cc.clearRect(0,0,n,n);cc.drawImage(img,0,0);return Array.from(cc.getImageData(0,0,n,n).data)};const empty=bytes(assets['empty'+n]),full=bytes(assets['full'+n]),rs=regions[n];let prev=-1,first=null;const points=new Set();rs.forEach(r=>{for(let y=r.y;y<r.y+r.h;y++)for(let x=r.x;x<r.x+r.w;x++)points.add(y*n+x);});
  for(let e=0;e<=1000;e++){
   cc.clearRect(0,0,n,n);B.drawStatusIcon(cc,assets['empty'+n],assets['full'+n],rs,e);const data=cc.getImageData(0,0,n,n).data;let count=0;
   for(let k=0;k<n*n;k++){const sameEmpty=[0,1,2,3].every(ch=>data[k*4+ch]===empty[k*4+ch]),sameFull=[0,1,2,3].every(ch=>data[k*4+ch]===full[k*4+ch]);if(!sameEmpty&&!sameFull)throw new Error('new pixel '+n+'/'+e+'/'+k);if(!points.has(k)&&!sameEmpty)throw new Error('outside changed');if(data[k*4+3]!==empty[k*4+3])throw new Error('alpha changed');if(!sameEmpty)count++;}
   if(count<prev)throw new Error('non-monotonic');prev=count;if(count>0&&first===null)first=e;
   if(e===0&&!data.every((v,k)=>v===empty[k]))throw new Error('empty endpoint changed');if(e===1000&&!data.every((v,k)=>v===full[k]))throw new Error('full endpoint changed');if(e===999&&data.every((v,k)=>v===full[k]))throw new Error('premature full');
   const widths=[];for(const r of rs.slice().reverse()){let width=0,stopped=false;for(let x=r.x;x<r.x+r.w;x++){const on=Array.from({length:r.h},(_,dy)=>{const k=((r.y+dy)*n+x)*4;return [0,1,2,3].every(ch=>data[k+ch]===full[k+ch]);});if(on.some(Boolean)&&!on.every(Boolean))throw new Error('partial column');if(on.every(Boolean)){if(stopped)throw new Error('gap');width++;}else stopped=true;}widths.push(width);}if(widths[1]&&widths[0]!==rs[2].w)throw new Error('middle before bottom');if(widths[2]&&widths[1]!==rs[1].w)throw new Error('top before middle');
   const frac=B.fractions(e),physicalFractions=P.fractions(e);if(frac.some((v,i)=>Math.abs(v-physicalFractions[i])>1e-12)||Math.abs(frac.reduce((a,b)=>a+b,0)-e/1000*3)>1e-12)throw new Error('physical identity');if(bounds.includes(e))cases.push({n,e,pixelsChanged:count,widthsBottomUp:widths,semantic:B.reading(e).semantic});
  }
  if(first!==(n===16?67:38))throw new Error('first pixel threshold');
 }
 const absent=B.reading(500,false,0);if(absent.energy!==null||absent.showIcon||absent.text!=='— / 1000')throw new Error('missing battery');if(B.reading(1).semantic!=='low'||B.reading(99).tone!=='low'||B.reading(100).tone!=='normal'||B.reading(999).semantic==='full')throw new Error('threshold semantics');
 for(const status of [1,2,4])if(B.reading(500,true,status).text!=='500 / 1000')throw new Error('status changed energy');
 const cv=document.createElement('canvas');cv.width=176;cv.height=80;const cc=cv.getContext('2d');const number=B.drawNumber(cc,'1000 / 1000',74,32,'#354438');if(74+number.width>168||32+number.height>=47)throw new Error('numeric bounds');
 window.pixelChecks={allPassed:true,energiesPerSize:1001,nativeSizes:[16,32],actualPixelCases:2002,allOutsidePixelsPreserved:true,alphaPreserved:true,onlyEndpointPixelsUsed:true,monotonic:true,bottomUpContiguous:true,endpointIdentity:true,firstLit16:67,firstLit32:38,physicalFractionsMatch:true,noBatteryDistinct:true,sourceStatusDoesNotChangeEnergy:true,numericWidth:number.width,gameFontTested:false,boundaryCases:cases};return window.pixelChecks;
}
window.ready=(async()=>{await Promise.all(Object.entries(imageSources).map(async([key,src])=>{const im=new Image();im.src=src;await im.decode();assets[key]=im;}));for(const e of samples){const button=document.createElement('button');button.textContent=String(e);button.onclick=()=>{if(get('scenario').value==='0')get('scenario').value='2';else if(get('scenario').value==='3'&&e!==1000)get('scenario').value='2';setEnergy(e)};get('samples').append(button)}
 get('energy').oninput=()=>setEnergy(+get('energy').value);get('number').oninput=()=>setEnergy(+get('number').value);for(const id of ['size','scale','theme','scenario','language'])get(id).onchange=update;update();drawBoard();await checkPixels();window.appReady=true;return true;})();
</script></html>'''
html=html.replace('__READOUT__',(R/'source/battery-readout.js').read_text()).replace('__PHYSICAL__',(R/'references/battery-preview.js').read_text()).replace('__IMAGES__',json.dumps(images)).replace('__REGIONS__',json.dumps(regions)).replace('__PHYSICAL_REGIONS__',json.dumps(physical['regions'])).replace('__LABELS__',(R/'references/energy-labels.json').read_text())
(R/'review/preview.html').write_text(html)
print('self-contained review written',len(html))
