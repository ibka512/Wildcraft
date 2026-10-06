"""Reopen editable source and verify reference geometry, not runtime collision/gameplay."""
import bpy,json,math,hashlib
from pathlib import Path
from mathutils import Vector,Matrix,Quaternion
from mathutils.bvhtree import BVHTree
R=Path(__file__).resolve().parents[1]
bpy.ops.wm.open_mainfile(filepath=str(R/'source/fuse-mount-review-v01.blend'))
bpy.context.view_layer.update()
S=json.loads((R/'source/mount-spec.json').read_text());cases=[]
def world(o):
 local=Matrix.LocRotScale(o.location,o.rotation_euler.to_quaternion(),o.scale)
 return world(o.parent)@o.matrix_parent_inverse@local if o.parent else local
def tree(o):return BVHTree.FromPolygons([world(o)@v.co for v in o.data.vertices],[list(p.vertices) for p in o.data.polygons],all_triangles=False)
def bb(o):
 vs=[world(o)@v.co for v in o.data.vertices]
 return [[min(v[i] for v in vs),max(v[i] for v in vs)] for i in range(3)]
for host in ['sword','axe','shield','arrow','flight']:
 for material in ['stone','diamond','feather']:
  stem='REVIEW_'+host+'_'+material;h=bpy.data.objects[stem+'/host'];m=bpy.data.objects[stem+'/material'];p=S['profiles']['arrow']['flight'] if host=='flight' else S['profiles'][host];b=bb(m);span=max(v[1]-v[0] for v in b);axis=0 if host=='flight' else 1;penetration=p['contact'][axis]-b[axis][0];contactOK=abs(penetration-p['penetration'])<1e-6;capOK=abs(span-p['maxSpan'])<1e-6;overlap=len(tree(h).overlap(tree(m)))
  patternClear=host!='shield' or not(b[0][0]<.2 and b[0][1]>-.2 and b[2][0]<.4 and b[2][1]>-.4)
  cases.append(dict(host=host,material=material,maxSpanMeasured=span,capOK=capOK,penetrationMeasured=penetration,contactOK=contactOK,referenceTriangleOverlaps=overlap,occupiedReferenceContact=overlap>0,patternClear=patternClear,passed=capOK and contactOK and patternClear and overlap>0))
scaleCases=[]
for h in ['sword','axe','shield']:
 a=bpy.data.objects['COMPARE_'+h+'_held'];b=bpy.data.objects['COMPARE_'+h+'_back'];ratio=b.scale[0]/a.scale[0];scaleCases.append(dict(host=h,backToHeldScaleRatio=ratio,passed=abs(ratio-1)<1e-7))
for o in bpy.data.objects:
 if o.type=='MESH':assert all(math.isfinite(v) for vert in o.data.vertices for v in vert.co)
assert not any(o.name.startswith('POSE_FUSE_arrow_back') for o in bpy.data.objects)
emission=[]
for m in bpy.data.materials:
 if m.use_nodes:
  for n in m.node_tree.nodes:
   if n.type=='BSDF_PRINCIPLED' and n.inputs['Emission Strength'].default_value:emission.append(m.name)
out=dict(asset='F03-01',referenceCases=cases,scaleCases=scaleCases,allPassed=all(c['passed'] for c in cases+scaleCases) and not emission,nativeHostComponentsRendered=False,gameRuntimeVerified=False,newProductionModels=0,newProductionTextures=0,packedImageFiles=[i.name for i in bpy.data.images if i.packed_file],emissiveMaterials=emission,referenceContactIsNotGameCollision=True)
(R/'review/geometry-checks.json').write_text(json.dumps(out,indent=2)+'\n');print(json.dumps(out,indent=2));assert out['allPassed']
