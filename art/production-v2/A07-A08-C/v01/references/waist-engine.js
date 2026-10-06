/* Art reference only. X right / Y up / Z back; one unit = 1/16 block. */
(function(){
const rad=d=>d*Math.PI/180;
const bounds=mesh=>[0,1,2].map(i=>[Math.min(...mesh.vertices.map(v=>v[i])),Math.max(...mesh.vertices.map(v=>v[i]))]);
const add=(a,b)=>a.map((x,i)=>x+b[i]);
function roll(v,a){const c=Math.cos(rad(a)),s=Math.sin(rad(a));return [v[0]*c-v[1]*s,v[0]*s+v[1]*c,v[2]];}
function yaw(v,a){const c=Math.cos(rad(a)),s=Math.sin(rad(a));return [v[0]*c+v[2]*s,v[1],-v[0]*s+v[2]*c];}
function pitch(v,a){const c=Math.cos(rad(a)),s=Math.sin(rad(a));return [v[0],v[1]*c-v[2]*s,v[1]*s+v[2]*c];}
function affine(v,m){return [0,1,2].map(i=>m[i][0]*v[0]+m[i][1]*v[1]+m[i][2]*v[2]+m[i][3]);}
function transform(mesh,fn,name=mesh.name){return {name,vertices:mesh.vertices.map(fn),faces:mesh.faces};}
function box(from,to,color,name){const [x,y,z]=from,[X,Y,Z]=to;return {name,vertices:[[x,y,z],[X,y,z],[X,Y,z],[x,Y,z],[x,y,Z],[X,y,Z],[X,Y,Z],[x,Y,Z]],faces:[[0,3,2,1],[4,5,6,7],[0,1,5,4],[1,2,6,5],[2,3,7,6],[3,0,4,7]].map(indices=>({indices,color}))};}
function obstruction(kind,pose,variant,amount=0){
 const bm=GEOMETRY.poses[pose][variant].bodyMatrix,objects=[];
 if(kind==='cape'){
  const angle=LAYOUT.capeProxy.swingDegrees[0]+amount*(LAYOUT.capeProxy.swingDegrees[1]-LAYOUT.capeProxy.swingDegrees[0]);
  const cape=box([-5,-16,3.2],[5,0,4.2],[.37,.18,.15],'PROXY_cape');
  objects.push(transform(cape,v=>affine(pitch(v,-angle),bm)));
  // Simple gold trim is a guide marking the cloth edge, not a new cape asset.
  objects.push(transform(box([-5,-16,4.22],[5,-15.6,4.3],[.67,.50,.25],'PROXY_cape_trim'),v=>affine(pitch(v,-angle),bm)));
 }
 if(kind==='elytra'||kind==='both'){
  const spread=LAYOUT.elytraProxy.spreadDegrees[0]+amount*(LAYOUT.elytraProxy.spreadDegrees[1]-LAYOUT.elytraProxy.spreadDegrees[0]);
  for(const side of [-1,1]){
   const wing=box(side<0?[-10,-20,0]:[0,-20,0],side<0?[0,0,2]:[10,0,2],[.38,.45,.49],'PROXY_elytra_'+side);
   objects.push(transform(wing,v=>affine(add(yaw(v,-side*spread),[side*2,-2,3.2]),bm)));
  }
 }
 return objects;
}
function item(id,pose,variant,mode='waist',settle=1,fusion=false){
 const p=LAYOUT.profiles[id],mesh=GEOMETRY.items[id],b=bounds(mesh),center=b.map(x=>(x[0]+x[1])/2),bm=GEOMETRY.poses[pose][variant].bodyMatrix;
 let target=p.centerBodyLocal,rotation=p.rollDegrees,angle=p.yawDegrees;
 if(mode==='back'){
  const old=BACK.profiles[id];target=[old.center[0],old.center[1]-24,old.center[2]];rotation=old.canvasAngle;angle=0;
 }
 if(mode==='held'){
  target=id==='shield'?[-10,-9,-6]:[8,-10,-6];rotation=id==='shield'?0:45;angle=id==='shield'?-25:0;
 }
 const offset=mode==='waist'?(p.poseOffsets?.[pose]??[0,0,0]):[0,0,0];
 const w=mode==='waist'?Math.pow(1-settle,3):0,secondary=LAYOUT.waistSettle[p.category];
 target=add(add(target,offset),secondary.offset.map(x=>x*w));
 const fn=v=>affine(add(yaw(roll(v.map((x,i)=>(x-center[i])*p.effectiveScale),rotation),angle),target),bm);
 const objects=[transform(mesh,fn,'ITEM_'+id)];
 if(fusion&&['sword','axe','shield'].includes(id)){
  // Adopted F03-01 cap: representative coarse stone; no extra attachment model.
  const f=FUSE.profiles[id],span=f.maxSpan*16,origin=f.contact.map(x=>x*16);
  // Native reference-mesh basis is X right/Y outward/Z up. Convert its contact.
  const contact=[origin[0],origin[2],origin[1]+span/2-f.penetration*16];
  const material=box(contact.map(x=>x-span/2),contact.map(x=>x+span/2),[.48,.50,.47],'PROXY_fused_stone');
  objects.push(transform(material,fn,'FUSE_'+id));
 }
 return {objects,scale:p.effectiveScale,ratio:1,target,reference:id};
}
function scene(state){
 const {pose='stand',variant='standard',cover='cape',group='sword-shield-bow',mode='waist',settle=1,swing=0,fusion=false}=state;
 const objects=Object.entries(GEOMETRY.poses[pose][variant].parts).map(([k,m])=>({...m,name:'BODY_'+k}));
 objects.push(...obstruction(cover,pose,variant,swing));
 if(pose==='glide'&&state.canopy!==false)objects.push(...GEOMETRY.glider.map(m=>({...m,name:'GLIDER_'+m.name})));
 const ids=group==='all'?['sword','shield','bow']:group.split('-');
 const placementMode=mode==='waist'&&cover==='none'?'back':mode;
 let items=ids.map(id=>item(id,pose,variant,placementMode,settle,fusion));
 if(mode==='legacy'){
  items=cover==='cape'?[]:ids.filter(id=>['sword','axe'].includes(id)).map(id=>item(id,pose,variant,'back',1,fusion));
 }
 if(pose!=='glide'&&state.inHand&&state.inHand!=='none'&&state.inHand!=='spare')items=items.filter(i=>LAYOUT.profiles[i.reference].category!==state.inHand);
 for(const i of items)objects.push(...i.objects);
 return {objects,items,state};
}
class Viewer{
 constructor(canvas){this.canvas=canvas;const g=this.gl=canvas.getContext('webgl',{alpha:false,antialias:true,preserveDrawingBuffer:true});if(!g)throw Error('WebGL unavailable');
  const shader=(type,s)=>{const q=g.createShader(type);g.shaderSource(q,s);g.compileShader(q);if(!g.getShaderParameter(q,g.COMPILE_STATUS))throw Error(g.getShaderInfoLog(q));return q;};
  const v=shader(g.VERTEX_SHADER,'attribute vec3 a;attribute vec3 c;varying vec3 color;void main(){gl_Position=vec4(a,1.0);color=c;}'),f=shader(g.FRAGMENT_SHADER,'precision mediump float;varying vec3 color;void main(){gl_FragColor=vec4(color,1.0);}');
  this.program=g.createProgram();g.attachShader(this.program,v);g.attachShader(this.program,f);g.linkProgram(this.program);g.deleteShader(v);g.deleteShader(f);if(!g.getProgramParameter(this.program,g.LINK_STATUS))throw Error('Link failed');this.buffer=g.createBuffer();this.last=null;
 }
 draw(state){const g=this.gl,s=scene(state),verts=[],aspect=this.canvas.width/this.canvas.height;
  const centre=state.pose==='swim'?[0,8,8]:state.pose==='glide'&&state.canopy!==false?[0,23,0]:[0,16,0];
  const radius=state.pose==='glide'&&state.canopy!==false?29:state.pose==='swim'?26:22;
  const rot=v=>pitch(yaw(v.map((x,i)=>x-centre[i]),state.yaw??-35),state.pitch??-8);
  for(const o of s.objects){const points=o.vertices.map(rot);for(const face of o.faces){const p=face.indices.map(i=>points[i]);if(p.length<3)continue;
   const a=p[1].map((x,i)=>x-p[0][i]),b=p[2].map((x,i)=>x-p[0][i]),n=[a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]],length=Math.hypot(...n)||1;
   const shade=.7+.3*Math.abs((n[0]*-.35+n[1]*.7+n[2]*-.6)/length),color=face.color.map(c=>Math.min(1,c*shade));
   for(let i=1;i<p.length-1;i++)for(const q of [p[0],p[i],p[i+1]])verts.push(q[0]/(radius*aspect),q[1]/radius,q[2]/100,...color);
  }}
  g.viewport(0,0,this.canvas.width,this.canvas.height);g.clearColor(.067,.105,.092,1);g.clear(g.COLOR_BUFFER_BIT|g.DEPTH_BUFFER_BIT);g.enable(g.DEPTH_TEST);g.depthFunc(g.LEQUAL);g.useProgram(this.program);g.bindBuffer(g.ARRAY_BUFFER,this.buffer);g.bufferData(g.ARRAY_BUFFER,new Float32Array(verts),g.DYNAMIC_DRAW);
  for(const [key,offset] of [['a',0],['c',12]]){const a=g.getAttribLocation(this.program,key);g.enableVertexAttribArray(a);g.vertexAttribPointer(a,3,g.FLOAT,false,24,offset);}
  g.drawArrays(g.TRIANGLES,0,verts.length/6);
  this.last={...state,visibleItems:s.items.map(i=>i.reference),scaleRatios:s.items.map(i=>i.ratio),triangles:verts.length/18,glError:g.getError()};return this.last;
 }
}
const api={bounds,add,roll,yaw,pitch,affine,transform,box,obstruction,item,scene,Viewer};
if(typeof window!=='undefined')window.WaistReview=api;
if(typeof module!=='undefined')module.exports=api;
})();
