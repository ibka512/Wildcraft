/* Art reference fixtures only. No Minecraft state is changed by this review page. */
(async function(){
'use strict';
const $=id=>document.getElementById(id),NAMES={sword:'剑',axe:'斧',shield:'盾',arrow:'箭'},CONTEXTS={gui:'物品展示',held:'手持参考',back:'背负参考',ground:'掉落参考',flight:'飞行箭参考'};
const img=new Image();img.src=ATLAS;await img.decode();
let host='sword',phase='data-unavailable',context='gui',uses=8,yaw=0,pitch=8,light=false;
const viewer=new FuseReview.Viewer($('live')),scratch=document.createElement('canvas');scratch.width=32;scratch.height=32;const sv=new FuseReview.Viewer(scratch);
function material(){return phase==='restored-reference'?'stone':'fallback';}
function fused(){return phase!=='unfused';}
function text(){
 if(phase==='data-unavailable')return {title:'融合记录 · 材料不可用',name:'不可用材料',lines:[`保留的剩余次数：${uses} 次`,'效果和拆卸已停用；原始数据保留。','缺失材料 ID：example:missing_material'],hint:'材料数据无法完整解析（kind=6）',disabled:true};
 if(phase==='model-unavailable')return {title:'融合记录 · 显示资源不可用',name:'石头（真实类别：石类）',lines:[`实际剩余次数：${uses} 次`,'效果及拆卸遵循真实服务端状态。','此例仅模拟模型显示失败，未改变材料数据。'],hint:'仅客户端显示故障（有效服务端类别）',disabled:false};
 if(phase==='restored-reference')return {title:'已融合 · 恢复后参考',name:'石头',lines:[`实际剩余次数：${uses} 次`,'恢复后显示真实材料；不补充次数。','这是视觉对照，没有执行资源修复。'],hint:'恢复后真实外观参考',disabled:false};
 return {title:'普通宿主',name:NAMES[host],lines:['没有融合记录、回退符号或融合角标。','宿主的原版功能保持实际状态。'],hint:'无融合记录',disabled:false};
}
function slot(c,x,y,size=16,h=host,mat=material(),withMaterial=fused(),mark=withMaterial){
 scratch.width=size;scratch.height=size;sv.draw(h,mat,'gui',0,4,false,withMaterial);
 c.fillStyle='#252d29';c.fillRect(x-1,y-1,size+2,size+2);c.strokeStyle='#7b897a';c.strokeRect(x-1.5,y-1.5,size+3,size+3);c.drawImage(scratch,x,y);
 if(mark)c.drawImage(img,48,0,5,5,x-1,y-1,5,5);
 // Native durability is only represented in this fixture; it is unrelated to material uses.
 if(h!=='arrow'){c.fillStyle='#10140f';c.fillRect(x+2,y+size-3,size-4,2);c.fillStyle='#71a145';c.fillRect(x+2,y+size-3,Math.round((size-4)*.65),1);}
}
function tile(c,x,y,k=1){c.drawImage(img,32,16,16,16,x,y,16*k,16*k);}
function tooltip(c,x,y,w,phaseName,count=8){
 const keep={phase,uses};phase=phaseName;uses=count;const t=text();phase=keep.phase;uses=keep.uses;
 c.fillStyle='#151019';c.fillRect(x,y,w,176);c.strokeStyle='#625274';c.strokeRect(x+.5,y+.5,w-1,175);c.fillStyle='#e4e8db';c.font='bold 20px "PingFang SC",sans-serif';c.fillText(t.title,x+20,y+30);c.font='16px "PingFang SC",sans-serif';
 c.fillStyle='#c9d0c3';c.fillText(t.name,x+20,y+60);
 t.lines.forEach((s,i)=>{c.fillStyle=i===1&&t.disabled?'#b7bab8':'#d1d8c8';c.fillText(s,x+20,y+91+i*28);});
}
function setBg(c,x,y,w,h,bright=false){c.fillStyle=bright?'#c7c6b9':'#16251d';c.fillRect(x,y,w,h);}
function drawLive(){
 viewer.draw(host,material(),context,yaw,pitch,false,fused());$('live').style.background=light?'#c7c6b9':'#16251d';
 $('live').setAttribute('aria-label',`${NAMES[host]} · ${CONTEXTS[context]} · ${text().hint}`);
 const c=$('slots').getContext('2d');c.imageSmoothingEnabled=false;c.clearRect(0,0,256,88);c.fillStyle='#e0e5d7';c.font='12px "PingFang SC",sans-serif';
 c.fillText('原生16px',12,15);slot(c,20,28,16);c.fillText('原生32px宿主渲染',106,15);slot(c,128,28,32);
 c.fillStyle='#adbba5';c.font='10px sans-serif';c.fillText('左上角标沿用F03-02；底部为宿主耐久示意',8,82);
 const t=text();$('stateTitle').textContent=t.title;$('materialName').textContent=t.name;$('tooltipLines').replaceChildren(...t.lines.map((s,i)=>{const p=document.createElement('p');p.textContent=s;if(i===0&&uses<=4&&!t.disabled&&phase!=='unfused')p.style.color='#e2b868';return p;}));
 $('stateHint').textContent=t.hint;$('useLabel').textContent=uses+' 次';$('angleLabel').textContent=Math.round(yaw)+'°';
 $('backOption').disabled=host==='arrow';$('flightOption').disabled=host!=='arrow';
 window.reviewState={host,phase,context,uses,yaw,pitch,light,...viewer.last};
}
function update(){host=$('host').value;phase=$('phase').value;context=$('context').value;uses=Number($('uses').value);yaw=Number($('angle').value);light=$('light').checked;
 if(context==='back'&&host==='arrow'||context==='flight'&&host!=='arrow'){context='gui';$('context').value='gui';}drawLive();}
for(const id of ['host','phase','context','uses','angle','light'])$(id).addEventListener('input',update);
$('front').addEventListener('click',()=>{yaw=0;pitch=8;$('angle').value=0;drawLive();});
$('side').addEventListener('click',()=>{yaw=72;pitch=10;$('angle').value=72;drawLive();});
$('live').addEventListener('keydown',e=>{if(e.key==='ArrowLeft'||e.key==='ArrowRight'){e.preventDefault();yaw=Math.max(-180,Math.min(180,yaw+(e.key==='ArrowRight'?15:-15)));$('angle').value=yaw;drawLive();}});
let drag=null;$('live').addEventListener('pointerdown',e=>{drag={x:e.clientX,y:e.clientY,yaw,pitch};$('live').setPointerCapture(e.pointerId);});
$('live').addEventListener('pointermove',e=>{if(drag){yaw=Math.max(-180,Math.min(180,drag.yaw+(e.clientX-drag.x)*.5));pitch=Math.max(-60,Math.min(60,drag.pitch+(e.clientY-drag.y)*.3));$('angle').value=yaw;drawLive();}});
$('live').addEventListener('pointerup',()=>drag=null);$('live').addEventListener('pointercancel',()=>drag=null);
function board(){
 const c=$('board').getContext('2d');c.imageSmoothingEnabled=false;c.fillStyle='#102017';c.fillRect(0,0,1200,1880);c.fillStyle='#dab779';c.font='16px sans-serif';c.fillText('WILDCRAFT / F03-03 · v01 / 2026-10-06 · 待视觉审稿',42,40);
 c.fillStyle='#e4eadb';c.font='bold 34px "PingFang SC",sans-serif';c.fillText('未知材料 · 通用回退外观',42,90);c.font='18px "PingFang SC",sans-serif';c.fillStyle='#b7c6aa';c.fillText('断开连接方片 / 原生16像素 / 灰银金属 / 无发光与元素暗示',42,130);
 setBg(c,42,163,346,300);setBg(c,410,163,346,300,true);tile(c,100,175,14);tile(c,468,175,14);c.fillStyle='#9eae98';c.font='16px sans-serif';c.fillText('深色背景 · 14倍整数放大',64,448);c.fillStyle='#354139';c.fillText('浅色背景 · 14倍整数放大',432,448);
 setBg(c,778,163,380,300);tile(c,800,188,8);c.strokeStyle='#52604e';for(let n=0;n<=16;n++){c.beginPath();c.moveTo(800+n*8,188);c.lineTo(800+n*8,316);c.stroke();c.beginPath();c.moveTo(800,188+n*8);c.lineTo(928,188+n*8);c.stroke();}tile(c,998,201,1);tile(c,1044,194,2);
 c.fillStyle='#b7c6aa';c.font='16px sans-serif';c.fillText('16×16网格',800,354);c.fillText('1倍 / 2倍展示',988,354);c.fillText('两段金属 · 透明断口 · 6色',800,386);c.fillText('2倍展示不等于另画32像素版本',800,418);
 c.fillStyle='#e4eadb';c.font='bold 24px "PingFang SC",sans-serif';c.fillText('四宿主与飞行箭：共用一个回退符号',42,510);
 const rc=document.createElement('canvas');rc.width=190;rc.height=180;const rv=new FuseReview.Viewer(rc);
 ['sword','axe','shield','arrow'].forEach((h,i)=>{let x=42+i*282;setBg(c,x,532,270,310);rv.draw(h,'fallback','gui',0,8);c.drawImage(rc,x+40,540);c.fillStyle='#e3e7d8';c.font='bold 18px "PingFang SC",sans-serif';c.fillText(NAMES[h]+' · 上限 '+SPEC.profiles[h].maxSpan,x+20,752);slot(c,x+40,783,16,h,'fallback');slot(c,x+145,775,32,h,'fallback');c.font='13px sans-serif';c.fillStyle='#9eae98';c.fillText('原生16 / 32宿主渲染参考',x+20,830);});
 c.font='bold 24px "PingFang SC",sans-serif';c.fillStyle='#e3e7d8';c.fillText('正侧视、飞箭与手背同尺度',42,891);
 rc.width=220;rc.height=190;
 [['shield','gui',70,'盾侧视 · 保留薄片厚度'],['arrow','flight',12,'飞行箭 · 上限0.18'],['sword','held',0,'手持 · 0.85'],['sword','back',0,'背负 · 0.85']].forEach(([h,ctx,a,label],i)=>{const x=42+i*282;setBg(c,x,912,270,251);rv.draw(h,'fallback',ctx,a,8);c.drawImage(rc,x+25,919);c.fillStyle='#b7c6aa';c.font='16px "PingFang SC",sans-serif';c.fillText(label,x+14,1142);});
 c.fillStyle='#e4eadb';c.font='bold 24px "PingFang SC",sans-serif';c.fillText('同一符号，区分材料数据与显示资源故障',42,1212);
 tooltip(c,42,1235,548,'data-unavailable',8);tooltip(c,610,1235,548,'model-unavailable',8);
 setBg(c,42,1432,548,220);setBg(c,610,1432,548,220);
 rc.width=180;rc.height=160;rv.draw('sword','stone','gui',0,8);c.drawImage(rc,60,1450);rv.draw('sword','fallback','gui',0,8,false,false);c.drawImage(rc,626,1450);
 c.fillStyle='#e0e5d7';c.font='bold 20px "PingFang SC",sans-serif';c.fillText('恢复后参考',260,1488);c.fillText('普通宿主',826,1488);c.font='16px "PingFang SC",sans-serif';c.fillStyle='#b7c6aa';c.fillText('真实材料重新显示；仍为8次。',260,1530);c.fillText('没有附着物与融合角标。',826,1530);c.fillText('视觉对照，没有执行资源修复。',260,1562);c.fillText('保留原版宿主实际状态。',826,1562);
 c.fillStyle='#e4eadb';c.font='bold 24px "PingFang SC",sans-serif';c.fillText('复用图集：64×32保持尺寸，新区域16×16',42,1700);c.drawImage(img,44,1720,256,128);
 c.strokeStyle='#dab779';c.strokeRect(44+32*4-.5,1720+16*4-.5,65,65);c.fillStyle='#b7c6aa';c.font='16px "PingFang SC",sans-serif';c.fillText('左侧32符号、右上16符号与5角标均沿用F03-02。',334,1750);c.fillText('右下(32,16)新增回退区域；旧区域像素保持一致。',334,1784);c.fillText('物品几何、字体、耐久为审稿参考；真实游戏与第三方模型待接入复核。',334,1818);
 c.fillStyle='#93a28b';c.font='14px "PingFang SC",sans-serif';c.fillText('未采用 · 未接入游戏 · 新增原创符号1个 / 源稿2个 / PNG2个 / 无新增生产模型',42,1862);
}
drawLive();board();window.renderReviewBoard=board;window.reviewReady=true;
})();
