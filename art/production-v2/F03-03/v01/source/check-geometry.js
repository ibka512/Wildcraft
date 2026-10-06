// Verify review geometry against actual opaque-pixel boxes and native reference triangles.
const fs=require('node:fs'),path=require('node:path'),vm=require('node:vm');
const R=path.resolve(__dirname,'..'),read=p=>JSON.parse(fs.readFileSync(path.join(R,p),'utf8'));
const SPEC=read('references/adopted-mount-spec.json'),MESH=read('source/review-meshes.json'),BACK=read('references/adopted-back-layout.json'),BOXES=read('review/fallback-reference-boxes.json');
const ctx={SPEC,MESH,BACK,window:{},Math,Number,Error,Float32Array};vm.runInNewContext(fs.readFileSync(path.join(R,'source/native-reference-renderer.js'),'utf8'),ctx);const F=ctx.window.FuseReview;
const assert=(v,msg)=>{if(!v)throw Error(msg);},near=(a,b)=>Math.abs(a-b)<1e-6;
function triangleBox(points,box){
 if([0,1,2].some(k=>Math.max(...points.map(v=>v[k]))<box.min[k]-1e-8||Math.min(...points.map(v=>v[k]))>box.max[k]+1e-8))return false;
 let poly=points;
 for(let axis=0;axis<3;axis++)for(const bound of ['min','max']){
  const value=box[bound][axis],inside=p=>bound==='min'?p[axis]>=value-1e-9:p[axis]<=value+1e-9;let next=[];
  for(let i=0;i<poly.length;i++){const a=poly[i],b=poly[(i+1)%poly.length],ai=inside(a),bi=inside(b);if(ai)next.push(a);if(ai!==bi){const t=(value-a[axis])/(b[axis]-a[axis]);next.push(a.map((v,k)=>v+(b[k]-v)*t));}}
  poly=next;if(!poly.length)return false;
 }
 return true;
}
const mounts=[];
for(const host of ['sword','axe','shield','arrow','flight']){
 const flight=host==='flight',h=flight?'arrow':host,p=F.placement(h,MESH.materials.fallback,flight),profile=flight?SPEC.profiles.arrow.flight:SPEC.profiles[h];assert(p,'valid fallback');
 const boxes=BOXES.map(b=>({min:b.min.map((v,k)=>v*p.scale+p.translate[k]),max:b.max.map((v,k)=>v*p.scale+p.translate[k])}));
 const transformed=MESH.materials.fallback.vertices.map(v=>v.map((n,k)=>n*p.scale+p.translate[k]));
 const low=[0,1,2].map(k=>Math.min(...transformed.map(v=>v[k]))),high=[0,1,2].map(k=>Math.max(...transformed.map(v=>v[k])));
 assert(near(Math.max(...high.map((n,k)=>n-low[k])),profile.maxSpan),'cap exact');
 const axis=flight?0:1;assert(near(low[axis],profile.contact[axis]-profile.penetration),'penetration exact');
 const native=MESH.hosts[host],triangles=native.faces.flatMap(f=>f.indices.slice(1,-1).map((j,i)=>[native.vertices[f.indices[0]],native.vertices[j],native.vertices[f.indices[i+2]]]));
 const hits=boxes.filter(b=>triangles.some(t=>triangleBox(t,b))).length;assert(hits>0,host+' opaque geometry contact');
 let protectedClear=true;
 if(host==='shield'){const r=profile.protectedPatternRect;protectedClear=boxes.every(b=>b.max[0]<=r.x[0]||b.min[0]>=r.x[1]||b.max[2]<=r.z[0]||b.min[2]>=r.z[1]);assert(protectedClear,'shield pattern clear');}
 mounts.push({host,maxSpan:profile.maxSpan,actualLongestSide:Math.max(...high.map((n,k)=>n-low[k])),minimum:low,maximum:high,penetration:profile.penetration,opaqueBoxesTouchingActualHostTriangles:hits,protectedPatternClear:protectedClear});
}
const scales=[];
for(const host of ['sword','axe','shield']){const held=F.geometry(host,'fallback','held',false),back=F.geometry(host,'fallback','back',false);assert(held.scale===back.scale,'equal hand/back world scale');const hDist=Math.hypot(...held.objects[1].transform(MESH.materials.fallback.vertices[0]).map((v,k)=>v-held.objects[1].transform(MESH.materials.fallback.vertices[1])[k])),bDist=Math.hypot(...back.objects[1].transform(MESH.materials.fallback.vertices[0]).map((v,k)=>v-back.objects[1].transform(MESH.materials.fallback.vertices[1])[k]));assert(near(hDist,bDist),'same transformed edge length');scales.push({host,held:held.scale,back:back.scale,ratio:bDist/hDist});}
const invalids=[{vertices:[]},{vertices:[[NaN,0,0],[1,1,1]]},{vertices:[[0,0,0],[Infinity,1,1]]},{vertices:[[0,0,0],[0,0,0]]},{vertices:[[0,0,0],[.00001,0,0]]},{vertices:[[0,0,0],[101,1,1]]}];
assert(invalids.every(m=>F.placement('sword',m)===null),'invalid bounds guarded');
const scenes=[];
for(const h of ['sword','axe','shield','arrow'])for(const c of ['gui','held','back','ground','flight']){
 if(c==='back'&&h==='arrow'||c==='flight'&&h!=='arrow')continue;
 for(const material of ['fallback','stone'])for(const fused of [true,false]){const s=F.geometry(h,material,c,false,fused);assert(s.objects.length===(['held','back'].includes(c)?(fused?8:7):(fused?2:1)),'one host and max one attachment');for(const o of s.objects)assert(o.mesh.vertices.every(v=>o.transform(v).every(Number.isFinite)),'finite transformed vertices');scenes.push({host:h,context:c,material,fused,objects:s.objects.length});}
}
const check={asset:'F03-03',allPassed:true,mounts,handBackScaleCases:scales,invalidBoundsRejected:invalids.length,transformedSceneCases:scenes.length,sceneCases:scenes,contactMethod:'clip actual native host triangles against every transformed opaque-pixel box; not overall bounds alone',referenceOnly:true,gameRuntimeVerified:false};
fs.writeFileSync(path.join(R,'review/geometry-checks.json'),JSON.stringify(check,null,2)+'\n');console.log(JSON.stringify({allPassed:true,contactHits:mounts.map(m=>[m.host,m.opaqueBoxesTouchingActualHostTriangles]),scenes:scenes.length,invalids:invalids.length}));
