/* Deterministic art reference emitter. Not a Minecraft particle provider/network implementation. */
(function(root){
'use strict';
function random(seed){let s=seed>>>0;return()=>{s=(Math.imul(1664525,s)+1013904223)>>>0;return s/4294967296;};}
function gate(e){
 if(!['fire','ice','elastic','fuse','split'].includes(e.type))return 'unknown-event';
 if(e.result==='inactive')return 'material-data-unavailable';
 if(e.result==='refused')return 'not-confirmed';
 if(!['confirmed','model-only'].includes(e.result))return 'unknown-result';
 if(e.type==='fuse'||e.type==='split')return e.channel==='action'&&e.committed?'accepted':'transaction-not-committed';
 if(!e.effectApplied||!e.targetAlive)return 'no-applied-effect';
 if(e.channel==='melee')return e.damageTaken>0?'accepted':'no-damage';
 if(e.channel==='shield-melee')return e.fullBlock&&e.livingDirectAttacker?'accepted':'no-elemental-counter';
 if(e.channel==='arrow-entity')return e.damageAccepted&&e.livingTarget?'accepted':'arrow-damage-not-accepted';
 return 'no-elemental-trigger';
}
class Emitter{
 constructor(config){this.config=config;this.particles=[];this.lastSequence=-1;this.epoch='reference';}
 reset(epoch='reference'){this.particles=[];this.lastSequence=-1;this.epoch=epoch;}
 prune(now){this.particles=this.particles.filter(p=>now<p.birth+p.life);}
 emit(e,now=e.tick){
  this.prune(now);
  if(e.epoch!==this.epoch)return {accepted:false,reason:'connection-epoch',spawned:0};
  if(!Number.isSafeInteger(e.sequence)||e.sequence<=this.lastSequence)return {accepted:false,reason:'duplicate-or-old-sequence',spawned:0};
  this.lastSequence=e.sequence;
  if(!Number.isFinite(e.tick)||e.tick>now||now-e.tick>this.config.budget.dedupWindowTicks)return {accepted:false,reason:'stale-or-invalid-time',spawned:0};
  const reason=gate(e);if(reason!=='accepted')return {accepted:false,reason,spawned:0};
  if(!Number.isFinite(e.distance)||e.anchor&&(!Array.isArray(e.anchor)||e.anchor.length!==3||!e.anchor.every(Number.isFinite)))return {accepted:false,reason:'invalid-display-input',spawned:0};
  if(e.particleMode==='minimal')return {accepted:true,reason:'minimal-particle-setting',spawned:0};
  if(e.distance<this.config.budget.extraEmissionNearCameraCutoff)return {accepted:true,reason:'near-camera',spawned:0};
  const preset=this.config.presets[e.type],count=e.particleMode==='reduced'?Math.ceil(preset.count*this.config.budget.reducedParticleFactor):preset.count;
  const local=this.particles.filter(p=>p.origin===e.origin).length,available=Math.max(0,Math.min(this.config.budget.maximumLiveForThisBatch-this.particles.length,this.config.budget.maximumLivePerOrigin-local));
  const n=Math.min(count,available,this.config.budget.maximumPerEvent),rnd=random((e.seed||42)^e.sequence),startAge=Math.max(0,now-e.tick);
  let spawned=0;
  for(let i=0;i<n;i++){
   const angle=(i/preset.count)*Math.PI*2+(rnd()-.5)*.3;
   const start=preset.movement==='converge'?.15:.015+rnd()*.022;
   const life=preset.lifetimeTicks,phase=angle,spread=.012+rnd()*.012;
   if(startAge>=life)continue;
   this.particles.push({type:e.type,origin:e.origin,birth:e.tick,life,angle:phase,start,spread,color:preset.paletteReference[i%preset.paletteReference.length],index:i,direction:e.direction===-1?-1:1,anchor:e.anchor||[0,0,0],size:preset.scaleReferenceBlocks});spawned++;
  }
  return {accepted:true,reason:spawned===count?'accepted':'particle-budget',spawned};
 }
 sample(now){
  this.prune(now);const out=[];
  for(const p of this.particles){const age=now-p.birth;if(age<0||age>=p.life)continue;const u=age/p.life,ca=Math.cos(p.angle),sa=Math.sin(p.angle);let x=0,y=0,z=0;
   if(p.type==='fire'){x=ca*p.start*(1+u*.4);y=age*(.02+p.spread*.2);z=sa*p.start;}
   else if(p.type==='ice'){const r=p.start+age*.018;x=ca*r;y=sa*r-age*age*.0006;z=Math.sin(p.angle*2)*.018;}
   else if(p.type==='elastic'){x=p.direction*(p.start+age*.022)+ca*age*.006;y=sa*(p.start+age*.006)+.012*age-.002*age*age;z=Math.sin(p.angle*2)*age*.007;}
   else if(p.type==='fuse'){const r=p.start*(1-u);x=ca*r;y=sa*r;z=Math.sin(p.angle*2)*r*.25;}
   else {const r=p.start+age*.014;x=ca*r;y=sa*r-.001*age*age;z=Math.sin(p.angle*2)*.02;}
   const length=Math.hypot(x,y,z),limit=this.config.budget.maxWorldRadius;if(length>limit){x*=limit/length;y*=limit/length;z*=limit/length;}
   const alpha=Math.max(0,Math.min(1,age<1?age+.35:(p.life-age)/2));
   out.push({...p,age,alpha,position:[x+p.anchor[0],y+p.anchor[1],z+p.anchor[2]],offset:[x,y,z],size:p.size*(p.type==='elastic'?1-u*.4:1-u*.55)});
  }
  return out;
 }
}
function fixture(type,channel='melee',result='confirmed',extra={}){
 if(type==='fuse'||type==='split')channel='action';
 return {type,channel,result,committed:result!=='refused',effectApplied:result!=='refused'&&result!=='inactive',targetAlive:true,livingTarget:true,damageTaken:1,damageAccepted:true,fullBlock:true,livingDirectAttacker:channel==='shield-melee',epoch:'reference',sequence:1,tick:0,seed:42,origin:'review-target',direction:1,particleMode:'all',distance:4,anchor:[0,0,0],...extra};
}
root.FuseFeedback={Emitter,gate,fixture};if(typeof module!=='undefined')module.exports=root.FuseFeedback;
})(typeof window!=='undefined'?window:globalThis);
