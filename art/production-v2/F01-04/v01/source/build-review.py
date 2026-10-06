"""Assemble a code-native, self-contained UI review from unchanged sprite exports."""
from pathlib import Path
import json,base64
R=Path(__file__).resolve().parents[1]
def uri(p):return 'data:image/png;base64,'+base64.b64encode(p.read_bytes()).decode()
assets={'panel':uri(R/'exports/cooking-ui-176x182.png')}
for rec in json.loads((R/'references/local-vanilla-items.json').read_text()):assets[rec['name']]=uri(Path(rec['localReviewCopy']))
for kind in ['vegetable','warming','cooling','recovery']:assets[kind]=uri(R/f'references/meal-{kind}-16.png')
html='''<!DOCTYPE html><html lang="zh-CN"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Wildcraft 料理锅界面 · v01 审稿</title>
<style>*{box-sizing:border-box}body{margin:0;background:#111b17;color:#e3e8d9;font:15px/1.6 system-ui,-apple-system,sans-serif}main{max-width:1240px;margin:auto;padding:28px}h1{font-size:28px;margin:5px 0}h2{font-size:19px}.kicker{font-size:12px;color:#d6b77d;letter-spacing:2px}.muted{color:#a4b7a6}.controls{display:flex;gap:16px;flex-wrap:wrap;padding:18px;background:#202d25;border-radius:8px;margin:22px 0}label{display:flex;align-items:center;gap:8px}select,input{font:inherit}select{background:#304331;color:#e3e8d9;padding:6px 10px;border:1px solid #617b57;border-radius:5px}:focus-visible{outline:3px solid #d6b77d;outline-offset:3px}.stage{padding:18px;background:#0a100d;overflow:auto;border:1px solid #3a4c3d}canvas{display:block;image-rendering:pixelated;max-width:none}.status{min-height:50px;padding:14px 0}.grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(260px,1fr));gap:18px}.card{background:#202c23;border:1px solid #35473a;padding:18px;overflow:auto}.card h3{margin:0 0 12px;font-size:17px}.card p{font-size:13px;color:#b7c6ac}.notes{display:grid;grid-template-columns:repeat(auto-fit,minmax(240px,1fr));gap:20px;margin:22px 0}footer{padding:20px 0;color:#a9b8a0;font-size:13px;border-top:1px solid #35473a}</style>
<main><div class="kicker">WILDCRAFT · F01-04 · v01</div><h1>料理锅界面 · 五状态</h1><p class="muted">原生176×182底图，3食材＋空碗＋成品；沿用已采用锅体材质和料理外观。字体近似，属于布局模拟，未接入游戏。</p>
<div class="controls"><label>状态<select id="scenario"></select></label><label>语言<select id="language"><option value="zh">中文</option><option value="en">English</option></select></label><label>显示倍数<select id="scale"><option>1</option><option>2</option><option selected>3</option><option>4</option></select></label><label>实际进度<input id="progress" type="range" min="0" max="99" value="56"><output id="progress-label">56/100</output></label><label><input id="bounds" type="checkbox">检查槽位边界</label><label><input id="hover" type="checkbox">悬停成品提示</label></div>
<div class="stage"><canvas id="live" role="img" aria-label="料理锅五状态界面"></canvas></div><div class="status" id="status" aria-live="polite"></div>
<div class="notes"><section><h2>保留真实槽位</h2><p>三个食材槽与空碗输入汇入成品槽；下方仍是原版27格库存＋9格快捷栏。热源来自锅下，界面不增加燃料槽或开始按钮。</p></section><section><h2>状态比装饰更重要</h2><p>未匹配、缺热源、烹饪中、缺空碗、成品待取分别给文字与小符号。烹饪中成品槽保持空，提交后才显示料理。</p></section><section><h2>一张底图共享五状态</h2><p>标题、提示、状态符号、进度和物品由程序绘制。等级／时长放在悬停提示中；采用料理图不另画五份界面。</p></section></div>
<h2>五状态完整对照</h2><div class="grid" id="gallery"></div><footer>当前草稿待审。底图为Aseprite原生像素；物品示例引用本机原版纹理和已采用料理。文字是浏览器近似排布，悬停框为语义示意，真实Minecraft字体与提示框由后续接入验证。本机原版参考不作为新原创资产或公开分发文件。</footer></main>
<script>__MODEL__</script><script>
const U=WildcraftCookingUi,assets=__ASSETS__,imgs={};let state={...U.presets.cooking,language:'zh',bounds:false,hover:false};
async function load(){await Promise.all(Object.entries(assets).map(([k,url])=>new Promise((resolve,reject)=>{const im=new Image();im.onload=()=>{imgs[k]=im;resolve();};im.onerror=reject;im.src=url;})));}
function text(c,t,x,y,color='#443d32',size=9){c.font=`${size}px monospace,"PingFang SC",sans-serif`;c.textBaseline='top';c.fillStyle=color;c.fillText(t,x,y);}
function glyph(c,id,x,y,color){const patterns=[['........','........','........','.##.##.#','.##.##.#','........','........','........'],['...#....','..###...','..####..','...###..','..####..','.######.','..####..','........'],['.#...#..','..#...#.','..#...#.','.#...#..','........','.######.','..####..','...##...'],['........','.######.','.#....#.','..####..','...##...','........','........','........'],['........','......#.','.....##.','.#..##..','.####...','..##....','........','........']];c.fillStyle=color;patterns[id].forEach((line,j)=>[...line].forEach((v,i)=>{if(v==='#')c.fillRect(x+i,y+j,1,1);}));}
function drawPanel(c,s,x=0,y=0){
 c.save();c.translate(x,y);c.imageSmoothingEnabled=false;c.drawImage(imgs.panel,0,0);const st=U.states[s.status],L=U.labels[s.language];
 for(const name of ['title','ingredients','bowl','output','inventory'])text(c,L[name],U.rules[name].x,U.rules[name].y);
 glyph(c,s.status,11,78,st.color);text(c,st[s.language],24,77,st.color);
 const width=U.displayProgress(s.progress),fill=s.status===2?'#c38442':'#9f9478';c.fillStyle=fill;
 // Shared arrow fill mask; retains the pointed end at every progress value.
 for(let yy=0;yy<5;yy++){const tip=[0,2,5,2,0][yy],limit=25+tip;c.fillRect(92,49+yy,Math.min(width,limit),1);}
 for(const [id,name] of Object.entries(s.slots)){const q=U.itemRegion(Number(id));c.drawImage(imgs[name],q.x,q.y,16,16);}
 if(s.bounds){c.strokeStyle='#dd702d';c.lineWidth=1;for(const q of U.slots)c.strokeRect(q.x-.5,q.y-.5,17,17);}
 if(s.hover&&s.slots[4]){c.fillStyle='rgba(255,255,255,.18)';c.fillRect(134,45,16,16);}
 c.restore();return {status:s.status,derivedStatus:U.deriveStatus(s),progress:s.progress,fillWidth:width,slots:U.slots,outputVisible:!!s.slots[4],textureSize:[176,182],labels:Object.fromEntries(['title','ingredients','bowl','output','inventory'].map(k=>[k,{text:L[k],...U.rules[k]}])),statusLabel:st[s.language]};
}
function drawLive(){const cv=document.getElementById('live'),scale=Number(document.getElementById('scale').value);const tooltip=state.hover&&state.slots[4];cv.width=tooltip?336:176;cv.height=182;cv.style.width=cv.width*scale+'px';cv.style.height=cv.height*scale+'px';const c=cv.getContext('2d');c.clearRect(0,0,cv.width,cv.height);const layout=drawPanel(c,state);
 if(tooltip){c.fillStyle='#211c2b';c.fillRect(180,44,150,52);c.strokeStyle='#6f557c';c.strokeRect(180.5,44.5,149,51);const lines=state.language==='zh'?['保暖料理 II','保暖 II · 3:00','吃完返还空碗']:['Warming Meal II','Warmth II · 3:00','Returns an empty bowl'];lines.forEach((t,i)=>text(c,t,186,50+i*13,i?'#bfc4a9':'#e8d6a5'));}
 cv.setAttribute('aria-label',U.states[state.status][state.language]+'，料理锅布局模拟');document.getElementById('status').textContent=U.presets[U.states[state.status].key].description+'。'+(state.status===2?'100刻后提交，当前'+state.progress+'/100。':'')+(tooltip?'右侧为悬停语义示意。':'');window.lastLayout=layout;
}
function apply(key){state={...U.presets[key],slots:{...U.presets[key].slots},language:document.getElementById('language').value,bounds:document.getElementById('bounds').checked,hover:document.getElementById('hover').checked};document.getElementById('progress').value=state.progress;document.getElementById('progress').disabled=state.status!==2;document.getElementById('progress-label').textContent=state.progress+'/100';drawLive();}
for(const st of U.states)document.getElementById('scenario').add(new Option(st.zh,st.key));document.getElementById('scenario').value='cooking';
document.getElementById('scenario').onchange=e=>apply(e.target.value);document.getElementById('language').onchange=e=>{state.language=e.target.value;drawLive();};document.getElementById('scale').onchange=drawLive;for(const key of ['bounds','hover'])document.getElementById(key).onchange=e=>{state[key]=e.target.checked;drawLive();};document.getElementById('progress').oninput=e=>{if(state.status===2)state.progress=Number(e.target.value);document.getElementById('progress-label').textContent=state.progress+'/100';drawLive();};
window.reviewReady=false;load().then(()=>{apply('cooking');for(const st of U.states){const d=document.createElement('article');d.className='card';const heading=document.createElement('h3');heading.textContent=st.zh;d.append(heading);const cv=document.createElement('canvas');cv.id='sample-'+st.key;cv.width=176;cv.height=182;cv.style.width='176px';drawPanel(cv.getContext('2d'),{...U.presets[st.key],language:'zh'});d.append(cv);const p=document.createElement('p');p.textContent=U.presets[st.key].description;d.append(p);document.getElementById('gallery').append(d);}window.reviewReady=true;});
</script></html>'''
html=html.replace('__MODEL__',(R/'source/cooking-ui.js').read_text()).replace('__ASSETS__',json.dumps(assets))
(R/'review/preview.html').write_text(html)
print('五状态交互界面预览已生成。')
