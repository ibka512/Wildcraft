/* Art-only joins. Runtime and ownership inputs here are synthetic review fixtures. */
(function(){
const clamp=x=>Math.max(0,Math.min(1,x)),smooth=x=>{x=clamp(x);return x*x*(3-2*x);};
const mix=(a,b,t)=>a.map((x,i)=>x+(b[i]-x)*t),dot=(a,b)=>a.reduce((n,x,i)=>n+x*b[i],0);
const unit=q=>{const n=Math.hypot(...q);return q.map(x=>x/n);};
function mul(a,b){const [w,x,y,z]=a,[W,X,Y,Z]=b;return [w*W-x*X-y*Y-z*Z,w*X+x*W+y*Z-z*Y,w*Y-x*Z+y*W+z*X,w*Z+x*Y-y*X+z*W];}
function euler(v){const [x,y,z]=v.map(t=>t/2);return unit(mul(mul([Math.cos(z),0,0,Math.sin(z)],[Math.cos(y),0,Math.sin(y),0]),[Math.cos(x),Math.sin(x),0,0]));}
function slerp(a,b,t){let d=dot(a,b);if(d<0){b=b.map(x=>-x);d=-d;}if(d>.9995)return unit(mix(a,b,t));const angle=Math.acos(Math.min(1,d)),den=Math.sin(angle);return a.map((x,i)=>(x*Math.sin((1-t)*angle)+b[i]*Math.sin(t*angle))/den);}
function rotate(v,q){const [w,x,y,z]=q;const a=[y*v[2]-z*v[1],z*v[0]-x*v[2],x*v[1]-y*v[0]];const b=[y*a[2]-z*a[1],z*a[0]-x*a[2],x*a[1]-y*a[0]];return v.map((n,i)=>n+2*(w*a[i]+b[i]));}
const pose=rows=>rows.map(v=>({p:v.slice(0,3),q:euler(v.slice(3))}));
function blend(a,b,t){return a.map((r,i)=>({p:mix(r.p,b[i].p,t),q:slerp(r.q,b[i].q,t)}));}
function sample(variant,clip,frame,corrected=true){const arr=CLIPS[variant][clip],f=Math.min(arr.length-1,Math.max(0,frame)),a=Math.floor(f),b=Math.min(a+1,arr.length-1),t=f-a;
 return corrected?blend(pose(arr[a]),pose(arr[b]),t):pose(arr[a].map((row,j)=>mix(row,arr[b][j],t)));
}
function native(kind,variant,main='right'){
 const rows=[[-5,2,0,0,0,0],[5,2,0,0,0,0],[-1.9,12,0,0,0,0],[1.9,12,0,0,0,0]],hand=main==='right'?0:1;
 if(kind==='item'||kind==='spare')rows[hand][3]=-Math.PI/10;
 if(kind==='bow'){rows[0][3]=rows[1][3]=-Math.PI/2;rows[0][4]=main==='right'?-.1:-.5;rows[1][4]=main==='right'?.5:.1;}
 if(kind==='block'){rows[hand][3]=-1.3962634;rows[hand][4]=(main==='right'?-1:1)*.5235988;}
 return pose(rows);
}
const event=.65;
function before(id,t,variant,corrected){
 if(['release_empty'].includes(id))return sample(variant,'hold',0,corrected);
 if(id==='mantle_restore')return sample(variant,'mantle',Math.min(24,t/event*24),corrected);
 if(['climb_glide','climb_turn','blocked_climb'].includes(id))return sample(variant,'up',(t/event*12)%24,corrected);
 return sample(variant,'glide',9,corrected);
}
function exit(start,variant,t,corrected,main){const target=native('empty',variant,main),u=clamp(t/.2);
 if(!corrected)return t<.2?sample(variant,'release',t*30,false):target;
 const r=sample(variant,'release',u*18,true),joined=blend(start,r,smooth(u*2));return blend(joined,target,smooth((u-.5)*2));
}
function close(start,variant,t,corrected,main,reduced){const end=native('empty',variant,main);return corrected&&!reduced?blend(start,end,smooth(t/.2)):end;}
function opening(start,variant,t,corrected,reduced){const target=sample(variant,'glide',Math.min(9,t*30),corrected);
 if(reduced)return sample(variant,'glide',9,true);
 return corrected?blend(start,target,smooth(t/.3)):target;
}
function evaluate(id,t,variant='standard',corrected=true,main='right',reduced=false){
 const sc=SPEC.scenarios.find(x=>x.id===id);if(!sc)throw Error('Unknown scenario');
 const start=before(id,event,variant,corrected),dt=t-event;
 let limbs=before(id,t,variant,corrected),stage=sc.from,open=stage==='glide',action='none',hand=null,dead=false,settle=1;
 if(t>=event){
  stage=sc.to;open=false;
  switch(id){
   case 'climb_glide':open=true;limbs=opening(start,variant,dt,corrected,reduced);break;
   case 'release_empty':limbs=reduced?native('empty',variant,main):exit(start,variant,dt,corrected,main);break;
   case 'glide_close':limbs=close(start,variant,dt,corrected,main,reduced);break;
   case 'mantle_restore':limbs=corrected?close(start,variant,dt,corrected,main,reduced):exit(start,variant,dt,false,main);break;
   case 'climb_turn':case 'blocked_climb':{
    const dest=sample(variant,id==='climb_turn'?'left':'hold',id==='climb_turn'?dt*18:0,corrected);
    limbs=corrected&&!reduced?blend(start,dest,smooth(dt/.12)):dest;break;
   }
   case 'glide_sword':case 'same_spare':hand='sword';action=id==='same_spare'?'spare':'item';limbs=native(action,variant,main);settle=reduced?1:clamp(dt/.14);break;
   case 'glide_bow':hand='bow';action='bow';limbs=native('bow',variant,main);break;
   case 'glide_block':hand='shield';action='block';limbs=native('block',variant,main);break;
   case 'loss_reset':dead=true;limbs=native('empty',variant,main);break;
   case 'rapid_glide':{
    if(t<.73){limbs=close(start,variant,dt,corrected,main,reduced);stage='empty';}
    else if(t<.88){open=true;stage='glide';const restart=close(start,variant,.08,corrected,main,reduced);limbs=opening(restart,variant,t-.73,corrected,reduced);}
    else{action='bow';hand='bow';stage='bow';limbs=native('bow',variant,main);}break;
   }
  }
 }
 const registered=['sword','shield','bow'],owners={sword:'back',shield:'back',bow:'back'};
 if(hand&&action!=='spare')owners[hand]='hand';
 if(dead)for(const k of registered)owners[k]='none';
 return {id,time:t,variant,main,corrected,limbs,stage,open,action,hand,dead,owners,settle,backIds:registered.filter(k=>owners[k]==='back'),drawSettleAllowed:action==='item'||action==='spare',rootMotion:[0,0,0],displacementFixture:true};
}
function javaPoint(v,p){const r=rotate(v,p.q);return [r[0]+p.p[0],24-r[1]-p.p[1],r[2]+p.p[2]];}
function matrix(limb){const columns=[[1,0,0],[0,-1,0],[0,0,1]].map(v=>rotate(v,limb.q).map((x,i)=>i===1?-x:x));return [0,1,2].map(i=>[...columns.map(v=>v[i]),i===1?24-limb.p[1]:limb.p[i]]).concat([[0,0,0,1]]);}
const colors={skin:[.62,.47,.31],shirt:[.20,.28,.16],pants:[.11,.17,.13]};
function limbMeshes(limbs,variant){const out=[];
 for(let i=0;i<4;i++){
  const arm=i<2,left=i===1,w=variant==='slim'?3:4,from=arm?[left?-1:-(w-1),-2,-2]:[-2,0,-2],to=arm?[left?w-1:1,10,2]:[2,12,2];
  const mesh=WaistReview.box(from,to,arm?colors.shirt:colors.pants,'LIMB_'+i);
  for(let j=0;j<mesh.faces.length;j++)if(arm)mesh.faces[j].color=j===1?colors.skin:colors.shirt;
  const m=matrix(limbs[i]),base=mesh.vertices.map(v=>[v[0],-v[1],v[2]]);
  out.push({...mesh,baseVertices:base,matrix:m,vertices:mesh.vertices.map(v=>javaPoint(v,limbs[i]))});
 }
 return out;
}
function firstGrip(variant){const c=variant==='slim'?.5:1,rows=[];
 for(const side of [-1,1]){const d=[side*(c===1?.1513897:.2000775),c===1?-.6913463:-.6852656,c===1?-.7064853:-.7002714];const q=unit([1+d[1],d[2],0,-d[0]]),end=rotate([side*c,10,0],q);rows.push({p:[side*7.5-end[0],-7-end[1],-7-end[2]],q});}
 return rows.concat(native('empty',variant).slice(2));
}
function heldMesh(id,limbs,variant,main,settle,allowed){
 const raw=GEOMETRY.items[id],b=WaistReview.bounds(raw),center=id==='sword'?[-5,-5,0]:b.map(x=>(x[0]+x[1])/2),scale=LAYOUT.profiles[id].effectiveScale,hand=main==='right'?0:1,sign=main==='right'?-1:1,c=variant==='slim'?.5:1;
 const wrist=javaPoint([sign*c,10,0],limbs[hand]),w=allowed?Math.pow(1-settle,3):0;
 const fn=v=>{
  // Native silhouette/scale, guide placement at live wrist, not full ItemInHandLayer display context.
  let p=v.map((x,i)=>(x-center[i])*scale);p=WaistReview.roll(p,id==='shield'?0:45);if(id==='sword')p=WaistReview.pitch(p,-65);
  return p.map((x,i)=>x+wrist[i]+[sign*.25,.45,.65][i]*w);
 };
 return {...WaistReview.transform(raw,fn,'HAND_'+id+(allowed&&id==='sword'?'_settle':'')),reference:id,scaleRatio:1};
}
function scene(state,view='third'){
 const v=state.variant,t=state.time,e=evaluate(state.id,t,v,state.corrected,state.main,state.reduced),meshes=[];
 if(e.dead)return {...e,objects:[],firstPersonPolicy:'clear'};
 if(view==='first'){
  if(e.open){meshes.push(...limbMeshes(firstGrip(v),v).slice(0,2),...FIRST_FRAME);}
  else if(e.hand){meshes.push(...limbMeshes(e.limbs,v).slice(0,2),heldMesh(e.hand,e.limbs,v,state.main,e.settle,e.drawSettleAllowed));}
  for(const o of meshes)o.vertices=o.vertices.map(p=>[p[0],p[1]-35,p[2]-5]);
  return {...e,objects:meshes,firstPersonPolicy:e.open?'steady_native_viewmodel':e.hand?'native_action_guide':'native_empty_hands_omitted',perspective:true};
 }
 const parts=GEOMETRY.poses.stand[v].parts;meshes.push({...parts.head,name:'HEAD'},{...parts.torso,name:'BODY'},...limbMeshes(e.limbs,v));
 meshes.push(...WaistReview.obstruction(state.cover??'cape','stand',v,0));
 if(e.open)meshes.push(...GEOMETRY.glider.map(o=>({...o,name:'GLIDER_'+o.name})));
 const placement=state.cover==='none'?'back':'waist';
 for(const id of e.backIds)meshes.push(...WaistReview.item(id,'stand',v,placement,1,state.fusion??false).objects);
 if(e.hand)meshes.push(heldMesh(e.hand,e.limbs,v,state.main,e.settle,e.drawSettleAllowed));
 return {...e,objects:meshes,perspective:false};
}
const api={clamp,smooth,euler,slerp,rotate,blend,sample,native,evaluate,javaPoint,matrix,limbMeshes,firstGrip,heldMesh,scene};
if(typeof window!=='undefined')window.MotionReview=api;if(typeof module!=='undefined')module.exports=api;
})();
