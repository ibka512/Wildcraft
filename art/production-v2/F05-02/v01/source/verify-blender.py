import bpy,json,math
from pathlib import Path
from mathutils import Vector
R=Path(__file__).resolve().parents[1];bpy.ops.wm.open_mainfile(filepath=str(R/'source/wing-v01.blend'));G=json.loads((R/'source/geometry-and-uv.json').read_text());C=json.loads((R/'references/approved-machine-geometry.json').read_text());prod=bpy.data.collections['PRODUCTION passive wing'];ref=bpy.data.collections['REFERENCE adopted F05-01 chassis'];root=bpy.data.objects['Wing node origin']
assert sum(o.type=='MESH'for o in prod.objects)==28 and sum(o.type=='MESH'for o in ref.objects)==68;assert all(o.hide_render for o in ref.objects)
im=next(i for i in bpy.data.images if i.name.startswith('wing-atlas'));assert tuple(im.size)==(128,64)and im.packed_file
tex=next(n for n in bpy.data.materials['Wing unified 128x64 atlas'].node_tree.nodes if n.type=='TEX_IMAGE');assert tex.interpolation=='Closest';bs=next(n for n in bpy.data.materials['Wing unified 128x64 atlas'].node_tree.nodes if n.type=='BSDF_PRINCIPLED');assert bs.inputs['Emission Strength'].default_value==0
assert not any(o.animation_data for o in prod.objects)
def rot(v,n):
 x,y,z=v;return [(x,z,-y),(x,-z,y),(-x,y,-z),(x,y,z),(-z,y,x),(z,y,-x)][n]
def V(v):return Vector((v[0]/16,v[2]/16,v[1]/16))
cases=[]
for n,r in enumerate([(math.pi/2,0,0),(-math.pi/2,0,0),(0,0,math.pi),(0,0,0),(0,0,math.pi/2),(0,0,-math.pi/2)]):
 root.rotation_euler=r;root.location=V(C['nodes'][n]['point']);bpy.context.view_layer.update();points=0
 for p in G['parts']:
  o=bpy.data.objects[p['name']]
  for vv,v in zip(p['vertices'],o.data.vertices):
   expected=V([a+b for a,b in zip(rot(vv,n),C['nodes'][n]['point'])]);assert (o.matrix_world@v.co-expected).length<1e-6;points+=1
 direction=root.matrix_world.to_3x3()@Vector((0,1,0));assert (direction-V([x*16 for x in C['nodes'][n]['normal']])).length<1e-6
 cases.append(dict(node=n,vertexChecks=points,sharedNodeBasisExact=True,scaleUnchanged=True))
result=dict(asset='F05-02',allPassed=True,sourceReopened=True,productionMeshes=28,hiddenAdoptedReferenceMeshes=68,packedAtlas=[128,64],actualNodeTransformCases=cases,noEmission=True,noMovingParts=True,gameIntegrated=False)
(R/'review/blender-source-checks.json').write_text(json.dumps(result,indent=2)+'\n');print('Blender重开、图集内嵌及六方向实际模型顶点一致通过。')
