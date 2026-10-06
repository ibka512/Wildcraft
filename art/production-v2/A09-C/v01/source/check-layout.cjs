const fs=require('fs'),vm=require('vm'),assert=require('assert'),path=require('path');const R=path.resolve(__dirname,'..');const j=n=>JSON.parse(fs.readFileSync(path.join(R,n),'utf8'));
const sandbox={GEOMETRY:j('source/reference-geometry.json'),LAYOUT:j('exports/waist-layout.json'),BACK:j('references/adopted-back-layout.json'),FUSE:j('references/adopted-fuse-layout.json'),module:{exports:{}}};vm.runInNewContext(fs.readFileSync(path.join(__dirname,'waist-engine.js'),'utf8'),sandbox);const api=sandbox.module.exports;
const checks=[];const check=(name,fn)=>{fn();checks.push({name,passed:true});};const distance=(a,b)=>Math.hypot(...a.map((v,i)=>v-b[i]));
for(const variant of ['standard','slim'])for(const pose of ['stand','crouch','swim','glide'])for(const id of Object.keys(sandbox.LAYOUT.profiles)){
 check('scale and rigid geometry '+variant+'/'+pose+'/'+id,()=>{
  const waist=api.item(id,pose,variant),held=api.item(id,pose,variant,'held');assert.equal(waist.ratio,1);assert.equal(waist.scale,held.scale);
  const a=waist.objects[0],b=held.objects[0];assert.equal(a.vertices.length,b.vertices.length);assert.equal(a.faces.length,b.faces.length);
  for(let k=0;k<a.vertices.length;k+=8){const diff=Math.abs(distance(a.vertices[k],a.vertices[k+1])-distance(b.vertices[k],b.vertices[k+1]));assert(diff<1e-5);}
 });
}
for(const cover of ['cape','elytra'])for(const group of ['sword-shield-bow','sword-shield-crossbow','axe-shield-bow','axe-shield-crossbow'])for(const variant of ['standard','slim'])for(const pose of ['stand','crouch','swim','glide']){
 check('three actual records '+cover+'/'+group+'/'+variant+'/'+pose,()=>{const s=api.scene({cover,group,variant,pose});assert.equal(s.items.length,3);assert.equal(new Set(s.items.map(i=>sandbox.LAYOUT.profiles[i.reference].category)).size,3);});
}
check('slim evaluated reference preserves intrinsic 3-unit cross edge',()=>{const p=sandbox.GEOMETRY.poses.stand.slim;assert.equal(p.bodyMatrix[1][3],24);assert(Math.abs(distance(p.parts.left_arm.vertices[0],p.parts.left_arm.vertices[1])-3)<1e-5);});
check('standard intrinsic cross edge4',()=>{const p=sandbox.GEOMETRY.poses.stand.standard;assert(Math.abs(distance(p.parts.left_arm.vertices[0],p.parts.left_arm.vertices[1])-4)<1e-5);});
check('none preserves old back targets',()=>{const s=api.scene({cover:'none',pose:'stand',variant:'standard',group:'sword-shield-bow'});for(const i of s.items)assert.deepEqual(Array.from(i.target),Array.from(api.item(i.reference,'stand','standard','back').target));});
check('legacy cape hides three',()=>assert.equal(api.scene({cover:'cape',mode:'legacy'}).items.length,0));
check('legacy elytra keeps melee only',()=>assert.equal(api.scene({cover:'elytra',mode:'legacy'}).items.length,1));
check('both uses elytra proxy alone',()=>{const s=api.scene({cover:'both'});assert(s.objects.some(x=>x.name.startsWith('PROXY_elytra')));assert(!s.objects.some(x=>x.name.startsWith('PROXY_cape')));});
for(const id of ['melee','shield','ranged'])check('same-reference held removes '+id,()=>{const s=api.scene({inHand:id});assert.equal(s.items.length,2);assert(!s.items.some(i=>sandbox.LAYOUT.profiles[i.reference].category===id));});
check('same-looking spare does not remove recorded item',()=>assert.equal(api.scene({inHand:'spare'}).items.length,3));
check('gliding hands preserve registered equipment',()=>assert.equal(api.scene({pose:'glide',inHand:'melee'}).items.length,3));
check('no ranged Fuse material invented',()=>{for(const id of ['bow','crossbow'])assert.equal(api.item(id,'stand','standard','waist',1,true).objects.length,1);});
check('only three record categories',()=>assert.equal(sandbox.LAYOUT.categories,3));
check('no production texture mesh or inventory slot',()=>assert.equal(sandbox.LAYOUT.newProductionModels+sandbox.LAYOUT.newTextures+sandbox.LAYOUT.newInventorySlots,0));
const out={allPassed:true,cases:checks.length,checks,gameRuntimeVerified:false,scope:'rigid scale, five profiles, three record combinations, adopted arm evaluation, proxy visibility and reference eligibility'};fs.writeFileSync(path.join(R,'review/layout-checks.json'),JSON.stringify(out,null,2)+'\n');console.log(JSON.stringify({allPassed:true,cases:checks.length}));
