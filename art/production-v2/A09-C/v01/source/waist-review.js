const $=id=>document.getElementById(id),viewer=new WaistReview.Viewer($('view')),left=new WaistReview.Viewer($('held')),right=new WaistReview.Viewer($('waist'));
const state={pose:'stand',variant:'standard',cover:'cape',group:'sword-shield-bow',mode:'waist',yaw:-35,pitch:-8,settle:1,swing:0,fusion:false,canopy:true,inHand:'none'};
const names={stand:'站立',crouch:'蹲下',swim:'游泳',glide:'滑翔',standard:'标准手臂',slim:'细手臂',cape:'披风',elytra:'鞘翅',both:'鞘翅与披风外观',none:'无遮挡'};
window.reviewErrors=[];
function render(){
 for(const id of ['pose','variant','cover','group','mode','inHand'])state[id]=$(id).value;
 for(const id of ['yaw','pitch','settle','swing'])state[id]=Number($(id).value);
 for(const id of ['fusion','canopy'])state[id]=$(id).checked;
 $('inHand').disabled=state.pose==='glide';
 const result=viewer.draw(state);$('angle').textContent=state.yaw+'°';$('phase').textContent=Math.round(state.settle*100)+'%';$('spread').textContent=Math.round(state.swing*100)+'%';
 const id=state.group.split('-')[0];left.draw({...state,group:id,mode:'held',canopy:false,inHand:'none'});right.draw({...state,group:id,mode:'waist',canopy:false,inHand:'none'});
 let msg='当前显示 '+result.visibleItems.map(id=>LAYOUT.profiles[id].label).join('、')+'；模型与手持尺度比均为 1.00。';
 if(!result.visibleItems.length)msg='当前规则下，这些腰侧／背负装备隐藏。';
 if(state.cover==='none')msg+=' 无披风或鞘翅时沿用原A09背部位置。';
 if(state.cover==='both')msg+=' 此参考由鞘翅占背部，披风不重复绘制；游戏需按原版实际披风可见性判定。';
 if(state.pose==='crouch')msg+=' 蹲下使用小幅上移补偿，保持完整模型。';
 if(state.pose==='glide')msg+=' 滑翔双手持伞，三类登记物品可继续显示在腰侧。';
 if(state.fusion)msg+=' 灰色块只标示已采用Fuse石头的占用范围，不是新材料模型。';
 $('note').textContent=msg;window.lastRender=result;return result;
}
for(const id of ['pose','variant','cover','group','mode','inHand','yaw','pitch','settle','swing','fusion','canopy'])$(id).addEventListener('input',render);
for(const [id,yaw] of [['front',0],['side',90],['back',180],['reset',-35]])$(id).onclick=()=>{$('yaw').value=yaw;$('pitch').value=-8;render();};
let pointer=null;$('view').onpointerdown=e=>{pointer=[e.clientX,e.clientY,state.yaw,state.pitch];$('view').setPointerCapture(e.pointerId);};$('view').onpointermove=e=>{if(pointer){$('yaw').value=Math.max(-180,Math.min(180,pointer[2]+(e.clientX-pointer[0])*.6));$('pitch').value=Math.max(-75,Math.min(75,pointer[3]+(e.clientY-pointer[1])*.4));render();}};$('view').onpointerup=$('view').onpointercancel=()=>pointer=null;
function text(c,t,x,y,size=22,color='#e6e7d4'){c.fillStyle=color;c.font=size+'px -apple-system,"PingFang SC",sans-serif';c.fillText(t,x,y);}
function boards(){
 const tiny=document.createElement('canvas');tiny.width=320;tiny.height=300;const renderer=new WaistReview.Viewer(tiny);
 const b=$('board'),c=b.getContext('2d');c.fillStyle='#101b16';c.fillRect(0,0,b.width,b.height);text(c,'WILDCRAFT / A09-C · 侧腰与背部避让',30,46,30);text(c,'标准 / 细手臂 × 披风 / 鞘翅 × 四姿态 · 同一剑、盾、弓 · 待视觉审稿',30,82,18,'#acbdab');
 const poses=['stand','crouch','swim','glide'];let row=0;
 for(const variant of ['standard','slim'])for(const cover of ['cape','elytra']){
  const y=115+row*320;text(c,names[variant]+' · '+names[cover],30,y,20,'#dfbb79');
  for(let col=0;col<4;col++){
   renderer.draw({pose:poses[col],variant,cover,group:'sword-shield-bow',mode:'waist',yaw:poses[col]==='swim'?75:145,pitch:-8,swing:0,settle:1,fusion:false,canopy:poses[col]==='glide'});
   c.drawImage(tiny,col*320,y+12,320,275);text(c,names[poses[col]]+(poses[col]==='swim'?' · 侧视':' · 后斜视'),col*320+20,y+301,18);
  }row++;
 }
 text(c,'装备尺寸比 1.00 · 三类记录，不新增库存格 · 披风与鞘翅为避让参考形体',30,1430,20,'#b8c8b5');
 text(c,'原版像素轮廓与采用角色姿态的几何参考；不是游戏截图，实际动画穿插待接入检查。',30,1470,17,'#92a98f');
 const eq=$('equipmentBoard'),e=eq.getContext('2d');e.fillStyle='#101b16';e.fillRect(0,0,eq.width,eq.height);text(e,'五类装备 · 保持原尺度',30,46,29);text(e,'标准站姿 / 披风 / 每格单独显示 / 不会同时携带剑与斧或弓与弩',30,82,18,'#acbdab');
 for(let i=0;i<5;i++){const id=['sword','axe','shield','bow','crossbow'][i];renderer.draw({pose:'stand',variant:'standard',cover:'cape',group:id,mode:'waist',yaw:id==='shield'?55:-65,pitch:-8,settle:1,swing:0,canopy:false});e.drawImage(tiny,i*256,112,256,280);text(e,LAYOUT.profiles[id].label+' · 1.00',i*256+20,428,22);}
 renderer.gl.deleteBuffer(renderer.buffer);renderer.gl.deleteProgram(renderer.program);renderer.gl.getExtension('WEBGL_lose_context')?.loseContext();window.boardReady=true;
}
try{render();boards();window.appReady=true;}catch(e){window.reviewErrors.push(String(e));throw e;}
window.addEventListener('error',e=>window.reviewErrors.push(e.message));window.reviewApp={state,render,viewer,left,right};
