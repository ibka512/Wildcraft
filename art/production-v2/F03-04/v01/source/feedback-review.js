(async function(){
'use strict';
const $=id=>document.getElementById(id),F=FuseFeedback;
const atlas=new Image();atlas.src=ATLAS;await atlas.decode();
let type='fire',channel='melee',result='confirmed',mode='all',light=false,time=2,playing=false,start=0,stress=false;
const engine=new F.Emitter(CONFIG);let receipt=null,current=null;
function rebuild(){engine.reset();current=F.fixture(type,channel,result,{particleMode:mode,sequence:1});if(result==='duplicate'){engine.lastSequence=stress?24:1;current.result='confirmed';}if(stress){let spawned=0;for(let i=0;i<24;i++){const r=engine.emit({...current,sequence:i+1,origin:'target-'+(i%4)},0);spawned+=r.spawned;}receipt={accepted:spawned>0,reason:'dense-reference',spawned};}else receipt=engine.emit(current,0);}
function scene(c,w,h,key,t,emitter=engine,options={}){
 const bright=options.light??light,config=CONFIG.presets[key],combat=!['fuse','split'].includes(key),point=combat?[212,139]:[111,192];
 c.save();c.scale(w/480,h/270);c.imageSmoothingEnabled=false;
 c.fillStyle=bright?'#bdc8b1':'#22382b';c.fillRect(0,0,480,270);c.fillStyle=bright?'#91a17f':'#314530';c.fillRect(0,177,480,93);
 for(let y=177;y<270;y+=22)for(let x=0;x<480;x+=32){if(((x/32)+(y-177)/22)%2===0){c.fillStyle=bright?'#96a683':'#354934';c.fillRect(x,y,32,22);}}
 c.fillStyle=bright?'#819373':'#1a2d24';c.fillRect(357,94,59,83);c.fillStyle=bright?'#a3b593':'#2a4030';c.fillRect(353,90,67,8);
 // Block figure and weapon are neutral stage props, not production models or native screenshots.
 c.fillStyle='#273026';c.fillRect(205,217,70,5);c.fillStyle='#aaa18e';c.fillRect(225,82,30,31);c.fillStyle='#868775';c.fillRect(225,106,30,7);
 c.fillStyle='#637866';c.fillRect(225,116,30,58);c.fillStyle='#51685b';c.fillRect(213,116,12,52);c.fillRect(255,116,12,52);
 c.fillStyle='#394856';c.fillRect(226,174,12,43);c.fillRect(242,174,12,43);
 c.save();c.translate(92,223);c.rotate(-.70);c.fillStyle='#3c3024';c.fillRect(-4,-17,8,23);c.fillStyle='#c7d3ca';c.fillRect(-4,-67,8,49);c.fillStyle='#6d9e92';c.fillRect(-3,-65,3,46);c.fillStyle='#46564d';c.fillRect(-12,-18,24,5);c.restore();
 const active=options.result??result;
 const committed=active==='confirmed'||active==='model-only';
 const missing=active==='inactive'||active==='model-only';
 if(!combat&&((key==='fuse'&&committed)||(key==='split'&&!committed))){if(missing)c.drawImage(atlas,32,16,16,16,103,182,16,16);else{c.fillStyle='#747d76';c.fillRect(104,184,15,15);c.fillStyle='#b7c1b6';c.fillRect(105,184,12,4);c.fillStyle='#929b92';c.fillRect(104,188,4,10);}}
 if(combat){if(missing)c.drawImage(atlas,32,16,16,16,103,182,16,16);else{c.fillStyle=config.paletteReference[0];c.fillRect(105,185,9,7);c.fillStyle='#d3d7d1';c.fillRect(105,185,6,2);}}
 if(options.showNativeNote!==false){c.fillStyle='#dce6d3';c.font='10px "PingFang SC",sans-serif';c.fillText(combat?'目标与宿主为舞台示意':'物品状态已由成功事务提交；粒子不延迟转移',12,22);}
 const samples=emitter.sample(t);let shown=0,culled=0;
 for(const p of samples){
  const x=Math.round(point[0]+p.offset[0]*80+p.offset[2]*16),y=Math.round(point[1]-p.offset[1]*80+p.offset[2]*8),size=Math.max(1,Math.round(p.size*80));
  const dx=Math.max(0,Math.abs(x-240)-size-1),dy=Math.max(0,Math.abs(y-125)-size-1);
  if(dx*dx+dy*dy<CONFIG.budget.reviewProtectedCrosshairRadiusPx**2){culled++;continue;}
  c.globalAlpha=p.alpha;c.fillStyle=p.color;
  if(key==='fire'){c.fillRect(x-1,y-size,size,Math.max(2,size+1));c.fillStyle='#fff0b8';c.fillRect(x,y-size+1,1,Math.max(1,size-1));}
  else if(key==='ice'){c.fillRect(x-size,y-size,size,1);c.fillRect(x-size,y-size,1,size);c.fillStyle='#f1fcfa';c.fillRect(x,y,1,1);}
  else if(key==='elastic'){c.fillRect(x-size,y-size,size+1,size);c.fillStyle='#b9d992';c.fillRect(x-size,y-size,size,1);}
  else {c.fillRect(x,y,size,size);c.fillStyle='#e2e6df';c.fillRect(x,y,1,1);}
  shown++;
 }
 c.globalAlpha=1;
 c.strokeStyle=bright?'#263a2c':'#e0e8d8';c.lineWidth=1;c.beginPath();c.moveTo(234,125);c.lineTo(238,125);c.moveTo(242,125);c.lineTo(246,125);c.moveTo(240,119);c.lineTo(240,123);c.moveTo(240,127);c.lineTo(240,131);c.stroke();
 if(options.protectedZone){c.setLineDash([2,3]);c.strokeStyle='#c1ab79';c.beginPath();c.arc(240,125,12,0,Math.PI*2);c.stroke();c.setLineDash([]);}
 c.fillStyle=bright?'#344536':'#b2c4a6';c.font='11px sans-serif';c.fillText((t/20).toFixed(2)+'s · '+shown+' 额外粒子',12,256);
 c.restore();return {shown,culled,alive:samples.length};
}
function description(){
 if(result==='inactive')return '材料数据不可用：没有元素反馈；保留已有回退外观。';
 if(result==='refused')return ['fuse','split'].includes(type)?'事务失败：没有成功粒子，保留实际物品状态与原有拒绝提示。':'没有实际生效：没有额外元素反馈。';
 if(result==='duplicate')return '同一确认事件已处理：重复通知不重新播放。';
 if(channel==='arrow-block'&&!['fuse','split'].includes(type))return '射中方块：原版碰撞和融合清除照常；没有元素命中反馈。';
 if(channel==='shield-ranged'&&!['fuse','split'].includes(type))return '完整格挡投射物：没有符合条件的近战攻击者，不播放元素反击反馈。';
 if(channel==='shield-partial'&&!['fuse','split'].includes(type))return '部分格挡：当前合同没有触发元素反击，不播放该反馈。';
 if(result==='model-only')return ['fuse','split'].includes(type)?'物品事务已成功；仅显示资源缺失，操作短反馈仍可播放。':'显示资源缺失：真实效果仍生效，按实际服务端确认播放。';
 return ['fuse','split'].includes(type)?'服务端事务已成功提交：物品立即更新，只补充一次短反馈。':'实际元素效果已生效：一次短促命中反馈，不改变原版伤害或持续时间。';
}
function draw(){const info=scene($('live').getContext('2d'),480,270,type,time,engine,{light,protectedZone:$('protected').checked});const detail=$('detail').getContext('2d'),point=['fuse','split'].includes(type)?[111,192]:[212,139];detail.imageSmoothingEnabled=false;detail.drawImage($('live'),point[0]-32,point[1]-24,64,48,0,0,192,144);$('timeLabel').textContent=(time/20).toFixed(2)+' s';$('typeLabel').textContent=CONFIG.presets[type].label;$('status').textContent=description();$('stats').textContent=`${CONFIG.presets[type].count}粒子预算 · ${CONFIG.presets[type].lifetimeTicks}世界刻（20TPS约${CONFIG.presets[type].normalRateSeconds.toFixed(2)}秒） · 当前${info.shown}个可见`;
 window.reviewState={type,channel,result,mode,light,time,playing,receipt,draw:info,stress,description:description()};}
function sync(){type=$('type').value;channel=$('channel').value;result=$('result').value;mode=$('mode').value;light=$('light').checked;time=Number($('time').value);stress=false;
 const action=['fuse','split'].includes(type);$('channel').disabled=action;$('actionOption').disabled=!action;if(action){channel='action';$('channel').value='action';}else if(channel==='action'){channel='melee';$('channel').value='melee';}$('modelOnlyOption').textContent=action?'显示资源缺失，事务成功':'显示资源缺失，效果生效';
 playing=false;$('play').textContent='播放一次';rebuild();draw();}
for(const id of ['type','channel','result','mode','light'])$(id).addEventListener('input',sync);
$('protected').addEventListener('input',draw);$('time').addEventListener('input',()=>{time=Number($('time').value);playing=false;$('play').textContent='播放一次';rebuild();draw();});
$('play').addEventListener('click',()=>{if(playing){playing=false;$('play').textContent='播放一次';draw();return;}stress=false;rebuild();time=0;start=performance.now();playing=true;$('play').textContent='暂停';draw();});
$('stress').addEventListener('click',()=>{time=2;$('time').value=2;playing=false;$('play').textContent='播放一次';stress=true;rebuild();draw();});
$('reset').addEventListener('click',()=>{$('type').value='fire';$('channel').value='melee';$('result').value='confirmed';$('mode').value='all';$('light').checked=false;$('time').value=2;sync();});
function animate(now){if(playing){time=Math.min(12,(now-start)/50);$('time').value=time;draw();if(time>=12){playing=false;$('play').textContent='播放一次';draw();}}requestAnimationFrame(animate);}
function board(){
 const c=$('board').getContext('2d');c.fillStyle='#102017';c.fillRect(0,0,1200,1680);c.fillStyle='#d9b77b';c.font='16px sans-serif';c.fillText('WILDCRAFT / F03-04 · v01 / 2026-10-06 · 待视觉采用',40,36);c.fillStyle='#e4eadb';c.font='bold 32px "PingFang SC",sans-serif';c.fillText('特殊材料与融合过程 · 短反馈',40,84);c.font='18px "PingFang SC",sans-serif';c.fillStyle='#b7c6aa';c.fillText('三类命中 + 两类操作 / 复用原版精灵 / 原创贴图0 / 无全屏滤镜与飞箭拖尾',40,124);
 const frame=document.createElement('canvas');frame.width=480;frame.height=270;const fc=frame.getContext('2d');
 ['fire','ice','elastic','fuse','split'].forEach((key,i)=>{
  const y=156+i*241,p=CONFIG.presets[key],em=new F.Emitter(CONFIG);em.emit(F.fixture(key),0);c.fillStyle='#e2e8d8';c.font='bold 22px "PingFang SC",sans-serif';c.fillText(p.label,40,y+27);c.font='16px sans-serif';c.fillStyle='#b4c4a7';c.fillText(`${p.count}粒 · ${p.lifetimeTicks}世界刻 · ${p.movement==='converge'?'内收':p.movement==='scatter'?'外散':p.movement==='rise'?'上升余烬':p.movement==='push'?'有方向的碎屑':'短促冷色碎粒'}`,218,y+27);
  const point=['fuse','split'].includes(key)?[111,192]:[212,139];scene(fc,480,270,key,2,em,{light:false,result:'confirmed',showNativeNote:false});c.fillStyle='#1b2c21';c.fillRect(40,y+42,256,144);c.imageSmoothingEnabled=false;c.drawImage(frame,point[0]-32,point[1]-24,64,48,72,y+42,192,144);c.fillStyle='#91a28a';c.font='15px sans-serif';c.fillText('0.10秒 · 3倍局部',48,y+212);
  [0,2,8].forEach((tick,j)=>{scene(fc,480,270,key,tick,em,{light:false,result:'confirmed',showNativeNote:false});c.imageSmoothingEnabled=false;c.drawImage(frame,328+j*288,y+42,256,144);c.fillStyle='#91a28a';c.font='15px sans-serif';c.fillText(`${(tick/20).toFixed(2)}秒`,336+j*288,y+212);});
 });
 const y=1400;c.fillStyle='#25372a';c.fillRect(40,y,1120,209);c.fillStyle='#e2e8d8';c.font='bold 23px "PingFang SC",sans-serif';c.fillText('短反馈只说明已发生的事件',60,y+36);c.fillStyle='#b7c6aa';c.font='17px "PingFang SC",sans-serif';c.fillText('成功命中或事务提交后播放；失败、方块碰撞、重复通知不伪装成成功。',60,y+75);c.fillText('火3秒、冰60刻与弹性击退仍按原玩法；粒子仅为0.30–0.40秒的视觉提示。',60,y+111);c.fillText('每事件最多6粒，五类共用48粒在场上限；减少/最少粒子设置有对应降级。',60,y+147);c.fillText('图中精灵与场景为程序化审稿近似；原版粒子与真实游戏需接入后复核。',60,y+183);
 c.fillStyle='#8e9f88';c.font='14px "PingFang SC",sans-serif';c.fillText('参数与动态源已交付 · 未接入游戏 · 原版燃烧/状态粒子不受本次额外反馈预算控制',40,1652);
}
sync();board();requestAnimationFrame(animate);window.renderReviewBoard=board;window.drawFeedbackScene=scene;window.reviewReady=true;
})();
