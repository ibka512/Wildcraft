const fs=require('fs'),path=require('path');const R=path.resolve(__dirname,'..');
const read=rel=>JSON.parse(fs.readFileSync(path.join(R,rel))),write=(rel,d)=>fs.writeFileSync(path.join(R,rel),JSON.stringify(d,null,2)+'\n');
global.CLIPS=read('references/runtime-clips.json');global.SPEC=read('exports/transition-spec.json');global.GEOMETRY=read('source/reference-geometry.json');global.LAYOUT=read('references/adopted-waist.json');global.BACK=read('references/adopted-back.json');global.FUSE=read('references/adopted-fuse.json');global.FIRST_FRAME=read('references/first-person-frame.json');
global.WaistReview=require('../references/waist-engine.js');const E=require('./transition-engine.js');
const checks=[],check=(name,pass,detail={})=>checks.push({name,pass,...detail});
const distance=(a,b)=>Math.hypot(...a.map((v,i)=>v-b[i]));
const difference=(a,b)=>Math.max(...a.map((l,i)=>distance(l.p,b[i].p)+2*Math.acos(Math.min(1,Math.abs(l.q.reduce((s,x,j)=>s+x*b[i].q[j],0))))));
let samples=0;
for(const variant of ['standard','slim'])for(const main of ['right','left'])for(const sc of SPEC.scenarios){
 let valid=true;
 for(let n=0;n<=216;n++){
  const v=E.evaluate(sc.id,n/120,variant,true,main);
  valid=valid&&v.limbs.every(l=>l.p.every(Number.isFinite)&&l.q.every(Number.isFinite)&&Math.abs(Math.hypot(...l.q)-1)<1e-6)&&v.rootMotion.every(x=>x===0);
  const expected=['sword','shield','bow'].filter(k=>v.owners[k]==='back');valid=valid&&expected.join()===v.backIds.join();
  if(v.hand&&v.action!=='spare')valid=valid&&!v.backIds.includes(v.hand);
  if(v.action==='bow'||v.action==='block')valid=valid&&difference(v.limbs,E.native(v.action,variant,main))<1e-7&&!v.drawSettleAllowed;
  if(v.dead)valid=valid&&!v.open&&!v.hand&&v.backIds.length===0;
  samples++;
 }
 check(`finite-owner-and-action:${variant}:${main}:${sc.id}`,valid);
 for(const event of (sc.id==='rapid_glide'?[.65,.73]:[.65]))if(['climb_glide','release_empty','climb_turn','glide_close','rapid_glide','mantle_restore','blocked_climb'].includes(sc.id)){
  const a=E.evaluate(sc.id,event-1e-7,variant,true,main),b=E.evaluate(sc.id,event,variant,true,main);const delta=difference(a.limbs,b.limbs);check(`snapshot-continuity:${variant}:${main}:${sc.id}:${event}`,delta<1e-4,{delta});
 }
 for(const id of ['glide_sword','glide_bow','glide_block','same_spare']){
  const a=E.evaluate(id,.65,variant,true,main);check(`immediate-takeover:${variant}:${main}:${id}`,!a.open&&a.hand!==null&&difference(a.limbs,E.native(a.action,variant,main))<1e-7);
 }
}
for(const variant of ['standard','slim']){
 const grip=E.firstGrip(variant),c=variant==='slim'?.5:1;
 for(const i of [0,1]){const sign=i===0?-1:1,error=distance(E.javaPoint([sign*c,10,0],grip[i]),[sign*7.5,31,-7]);check(`first-grip:${variant}:${i}`,error<1e-7,{error});}
 const raw=E.sample(variant,'release',4.5,false),fixed=E.sample(variant,'release',4.5,true);
 const angle=(a,b)=>2*Math.acos(Math.min(1,Math.abs(a.reduce((sum,x,i)=>sum+x*b[i],0))))*180/Math.PI;
 const rawExcursion=angle(E.sample(variant,'release',4,false)[1].q,raw[1].q),shortExcursion=angle(E.sample(variant,'release',4,true)[1].q,fixed[1].q);
 check(`wrap-shortest-path:${variant}`,rawExcursion>150&&shortExcursion<15,{rawMidpointDegrees:rawExcursion,revisedMidpointDegrees:shortExcursion});
 for(const id of ['release_empty','glide_close','mantle_restore'])check(`empty-exit-endpoint:${variant}:${id}`,difference(E.evaluate(id,.85,variant).limbs,E.native('empty',variant))<1e-6);
 check(`entry-endpoint:${variant}`,difference(E.evaluate('climb_glide',.95,variant).limbs,E.sample(variant,'glide',9,true))<1e-6);
 const reduced=E.evaluate('climb_glide',.65,variant,true,'right',true);check(`reduced-motion-entry:${variant}`,reduced.open&&difference(reduced.limbs,E.sample(variant,'glide',9,true))<1e-7);
 check(`reference-scale:${variant}`,Object.values(LAYOUT.profiles).every(p=>p.backToHeldRatio===1));
}
check('zero-production-geometry-and-textures',SPEC.newProductionModels===0&&SPEC.newTextures===0);
const result={allPassed:checks.every(c=>c.pass),sampledFrames:samples,checks,gameRuntimeVerified:false,scope:'Independent endpoint/ownership/interrupt/wrap checks against actual bake and native pose formulas; synthetic input'};write('review/transition-checks.json',result);
if(!result.allPassed)throw Error(JSON.stringify(checks.filter(c=>!c.pass)));
// Bake object matrices, not per-frame replacement meshes. Held meshes have a guide transform.
const scenes=[];for(const variant of ['standard','slim'])for(const sc of SPEC.scenarios){
 const frames=[];for(let n=0;n<=108;n++){
  const state={id:sc.id,time:n/60,variant,main:'right',corrected:true,cover:'cape'};
  const s=E.scene(state);frames.push({frame:n+1,time:n/60,open:s.open,action:s.action,owners:s.owners,limbs:s.limbs,objects:s.objects});
 }
 scenes.push({id:sc.id,label:sc.label,variant,fps:60,frames});
}
// Store static object shapes once; limbs use affine tracks; the few held guide paths are vertex samples.
const baked=scenes.map(s=>{const shape={};const frames=s.frames.map(f=>{
 const objects=f.objects.map(o=>{
  if(!shape[o.name])shape[o.name]={name:o.name,vertices:o.baseVertices??o.vertices,faces:o.faces};
  if(o.matrix)return {name:o.name,matrix:o.matrix};
  if(o.name.startsWith('HAND_')){
   const base=shape[o.name].vertices,translation=o.vertices[0].map((x,i)=>x-base[0][i]);
   if(o.vertices.some((v,k)=>v.some((x,i)=>Math.abs(x-base[k][i]-translation[i])>1e-6)))throw Error('Hand guide must remain rigid');
   return {name:o.name,translation};
  }
  return {name:o.name};
 });return {...f,objects};
 });return {...s,frames,shapes:shape};});
fs.writeFileSync(path.join(R,'exports/transition-tracks.json'),JSON.stringify({asset:'A07-A08-C',referenceOnly:true,adopted:false,integrated:false,fps:60,scenes:baked})+'\n');
console.log(JSON.stringify({allPassed:result.allPassed,checks:checks.length,sampledFrames:samples,editableTracks:scenes.length}));
