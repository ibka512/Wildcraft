import bpy,json,math
from pathlib import Path
from mathutils import Vector
R=Path(__file__).resolve().parents[1];bpy.ops.wm.open_mainfile(filepath=str(R/'source/rocket-and-spent-v01.blend'));G=json.loads((R/'source/geometry-and-uv.json').read_text());C=json.loads((R/'references/approved-machine-geometry.json').read_text());prod=bpy.data.collections['PRODUCTION rocket variants'];ref=bpy.data.collections['REFERENCE adopted F05-01 chassis'];root=bpy.data.objects['Rocket node origin']
assert sum(o.type=='MESH'for o in prod.objects)==121 and sum(o.type=='MESH'for o in ref.objects)==68;assert all(o.hide_render for o in ref.objects)
assert all(o.hide_render==(o['state']=='spent')for o in prod.objects if o.type=='MESH');assert all(o.hide_render for o in bpy.data.collections['REVIEW native tail flame'].objects)
im=next(i for i in bpy.data.images if i.name.startswith('rocket-atlas'));assert tuple(im.size)==(128,64)and im.packed_file
mat=bpy.data.materials['Rocket unified 128x64 atlas'];tex=next(n for n in mat.node_tree.nodes if n.type=='TEX_IMAGE');assert tex.interpolation=='Closest';bs=next(n for n in mat.node_tree.nodes if n.type=='BSDF_PRINCIPLED');assert bs.inputs['Emission Strength'].default_value==0
assert not any(o.animation_data for o in prod.objects)
def rot(v,n):
 x,y,z=v;return [(x,z,-y),(x,-z,y),(-x,y,-z),(x,y,z),(-z,y,x),(z,y,-x)][n]
def V(v):return Vector((v[0]/16,v[2]/16,v[1]/16))

for o in prod.objects:
 if o.type=='MESH':o.hide_viewport=False
cases=[]
for n,r in enumerate([(math.pi/2,0,0),(-math.pi/2,0,0),(0,0,math.pi),(0,0,0),(0,0,math.pi/2),(0,0,-math.pi/2)]):
 root.rotation_euler=r;root.location=V(C['nodes'][n]['point']);bpy.context.view_layer.update();points=0
 for state,g in G['variants'].items():
  for p in g['parts']:
   o=bpy.data.objects[state+'/'+p['name']]
   for vv,v in zip(p['vertices'],o.data.vertices):
    expected=V([a+b for a,b in zip(rot(vv,n),C['nodes'][n]['point'])]);assert (o.matrix_world@v.co-expected).length<1e-6,(n,state,p['name'],list(v.co),list(expected),list(o.matrix_world@v.co));points+=1
 direction=root.matrix_world.to_3x3()@Vector((0,1,0));assert (direction-V([x*16 for x in C['nodes'][n]['normal']])).length<1e-6
 cases.append(dict(node=n,vertexChecks=points,sharedNodeBasisExact=True,scaleUnchanged=True,exhaustOutward=True,thrustOpposite=True))
flame=bpy.data.objects['REVIEW existing flame box'];assert len(flame.data.vertices)==8;assert all(tuple(round(x*16,4)for x in [v.co.x,v.co.z,v.co.y])in [tuple(p)for p in [[x,y,z]for x in [-1.28,1.28]for y in [-1.28,1.28]for z in [8,13.76]]]for v in flame.data.vertices)
result=dict(asset='F05-04',allPassed=True,sourceReopened=True,productionMeshes={'loaded':53,'spent':68},hiddenAdoptedReferenceMeshes=68,packedAtlas=[128,64],actualNodeTransformCases=cases,sourceDefaultLoaded=True,spentAlternativeHidden=True,reviewNativeFlameHiddenByDefault=True,noMovingParts=True,noProductionEmission=True,gameIntegrated=False)
(R/'review/blender-source-checks.json').write_text(json.dumps(result,indent=2)+'\n');print('Blender重开、两状态六向顶点、原喷口轴/尾焰范围与内嵌图集通过。')
