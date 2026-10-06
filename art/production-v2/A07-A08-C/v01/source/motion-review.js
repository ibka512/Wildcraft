const $=id=>document.getElementById(id),viewers={current:new MotionViewer($('current')),revised:new MotionViewer($('revised')),first:new MotionViewer($('first'))};
const state={id:'climb_glide',variant:'standard',main:'right',cover:'cape',time:.72,yaw:-35,pitch:-8,fov:70,reduced:false,fusion:false};
let playing=false,lastStamp=null,raf=null;window.reviewErrors=[];
function render(){
 for(const id of ['id','variant','main','cover'])state[id]=$(id).value;
 for(const id of ['time','yaw','pitch','fov'])state[id]=Number($(id).value);
 for(const id of ['reduced','fusion'])state[id]=$(id).checked;
 const current=viewers.current.draw({...state,corrected:false}),revised=viewers.revised.draw({...state,corrected:true}),first=viewers.first.draw({...state,corrected:true},'first');
 $('clock').textContent=state.time.toFixed(3)+' s';$('note').textContent=SPEC.scenarios.find(s=>s.id===state.id).reviewNote;
 $('status').textContent=(revised.open?'双手持伞':revised.action==='none'?'动作衔接':revised.action==='spare'?'备用剑在手，登记剑保留':'原版动作即时优先')+' · 伞 '+(revised.open?'显示':'隐藏')+' · 背负 '+revised.backIds.length+' 类 · '+(state.time<.65?'触发前':'触发后');
 $('first-note').textContent=first.firstPersonPolicy==='steady_native_viewmodel'?'已采用的第一人称稳态握持与伞框；中心框仅为对照。':first.firstPersonPolicy==='native_action_guide'?'原版动作优先的简化位置参考；实际第一人称物品动画待游戏验证。':'自定义持伞层已清空；原版空手/失效显示不在此画布重建。';
 window.lastRender={current,revised,first};return window.lastRender;
}
function stop(){playing=false;lastStamp=null;if(raf!==null)cancelAnimationFrame(raf);raf=null;$('play').textContent='播放';}
function tick(stamp){if(!playing)return;if(lastStamp!==null){$('time').value=Math.min(1.8,state.time+(stamp-lastStamp)/1000*Number($('speed').value));render();}lastStamp=stamp;if(state.time>=1.8)stop();else raf=requestAnimationFrame(tick);}
function toggle(){if(playing){stop();return;}if(state.time>=1.8)$('time').value=0;playing=true;lastStamp=null;$('play').textContent='暂停';render();raf=requestAnimationFrame(tick);}
for(const id of ['id','variant','main','cover','yaw','pitch','fov','reduced','fusion'])$(id).addEventListener('input',()=>{stop();render();});
$('time').addEventListener('input',()=>{stop();render();});$('play').onclick=toggle;
$('restart').onclick=()=>{stop();$('time').value=0;render();};
$('event').onclick=()=>{stop();$('time').value=.65;render();};
$('step').onclick=()=>{stop();$('time').value=Math.min(1.8,state.time+1/60);render();};
for(const [id,yaw]of [['front',0],['side',90],['back',180]])$(id).onclick=()=>{$('yaw').value=yaw;render();};
let pointer=null;for(const id of ['current','revised']){
 const canvas=$(id);canvas.onpointerdown=e=>{stop();pointer=[e.clientX,e.clientY,state.yaw,state.pitch];canvas.setPointerCapture(e.pointerId);};canvas.onpointermove=e=>{if(pointer){$('yaw').value=Math.max(-180,Math.min(180,pointer[2]+(e.clientX-pointer[0])*.6));$('pitch').value=Math.max(-75,Math.min(75,pointer[3]+(e.clientY-pointer[1])*.4));render();}};canvas.onpointerup=canvas.onpointercancel=()=>pointer=null;
}
document.addEventListener('visibilitychange',()=>{if(document.hidden)stop();});window.addEventListener('pagehide',stop);
document.addEventListener('keydown',e=>{if(e.code==='Space'&&!['INPUT','SELECT','BUTTON'].includes(document.activeElement.tagName)){e.preventDefault();toggle();}});
function boards(){const tiny=document.createElement('canvas');tiny.width=300;tiny.height=300;const renderer=new MotionViewer(tiny),board=$('board'),ctx=board.getContext('2d');
 const text=(t,x,y,size=18,color='#dfdfcc')=>{ctx.fillStyle=color;ctx.font=size+'px -apple-system,"PingFang SC",sans-serif';ctx.fillText(t,x,y);};
 ctx.fillStyle='#101b16';ctx.fillRect(0,0,board.width,board.height);text('WILDCRAFT / A07-A08-C · 探索动作衔接',24,42,28);text('修订参考 · 标准手臂 · 披风侧腰 · 无实体根运动或操作延迟',24,76,17,'#acbdab');
 for(let row=0;row<4;row++){const id=['climb_glide','release_empty','glide_sword','glide_bow'][row];text(SPEC.scenarios.find(s=>s.id===id).label,24,112+row*325,21,'#d6b879');
  for(let col=0;col<4;col++){const time=[.64,.65,.75,.95][col];renderer.draw({id,variant:'standard',main:'right',cover:'cape',corrected:true,time,yaw:-35,pitch:-8,reduced:false});ctx.drawImage(tiny,col*300,125+row*325,300,265);text(time.toFixed(2)+'s',col*300+20,413+row*325,18);}
 }
 text('0.65s为审稿触发点；剑、弓等简化手部位置用于看占用，实际原版手部显示待验证。',24,1454,17,'#acbdab');renderer.dispose();window.boardReady=true;
}
try{render();boards();window.appReady=true;}catch(e){reviewErrors.push(String(e));throw e;}
window.addEventListener('error',e=>reviewErrors.push(e.message));window.reviewApp={state,render,stop,toggle,get playing(){return playing;},viewers};
