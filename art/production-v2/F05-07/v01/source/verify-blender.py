import bpy,json,math
from pathlib import Path
from mathutils import Vector
R=Path(__file__).resolve().parents[1];bpy.ops.wm.open_mainfile(filepath=str(R/'source/wheel-v01.blend'));G=json.loads((R/'source/geometry-and-uv.json').read_text());C=json.loads((R/'references/approved-machine-geometry.json').read_text());prod=bpy.data.collections['PRODUCTION wheel'];ref=bpy.data.collections['REFERENCE adopted F05-01 chassis'];root=bpy.data.objects['Wheel node origin'];rotor=bpy.data.objects['Rotor exact axis +Z']
assert sum(o.type=='MESH'for o in prod.objects)==67 and sum(o.type=='MESH'for o in ref.objects)==68 and all(o.hide_render for o in ref.objects)
im=next(i for i in bpy.data.images if i.name.startswith('shared-machine'));assert tuple(im.size)==(64,64)and im.packed_file;assert (rotor.location-Vector((0,.25,0))).length<1e-7
tex=next(n for n in bpy.data.materials['Approved 64px shared machine atlas'].node_tree.nodes if n.type=='TEX_IMAGE');assert tex.interpolation=='Closest';bs=next(n for n in bpy.data.materials['Approved 64px shared machine atlas'].node_tree.nodes if n.type=='BSDF_PRINCIPLED');assert bs.inputs['Emission Strength'].default_value==0;assert len(rotor.animation_data.drivers)==1
assert root['active']==0 and root['ghost']==0 and root['rateRadiansPerWorldTick']==.4
def rot(v,n):
 x,y,z=v;return [(x,z,-y),(x,-z,y),(-x,y,-z),(x,y,z),(-z,y,x),(z,y,-x)][n]
def V(v):return Vector((v[0]/16,v[2]/16,v[1]/16))
cases=[];total=0
for n,r in enumerate([(math.pi/2,0,0),(-math.pi/2,0,0),(0,0,math.pi),(0,0,0),(0,0,math.pi/2),(0,0,-math.pi/2)]):
 root.rotation_euler=r;root.location=V(C['nodes'][n]['point'])
 for active,ghost in [(0,0),(1,0),(1,1)]:
  for age in [0,1,math.pi/(4*.4),20,100]:
   root['active']=active;root['ghost']=ghost;root['ageInTicks']=age;root.update_tag();bpy.context.scene.frame_set(bpy.context.scene.frame_current+1);bpy.context.view_layer.update();dg=bpy.context.evaluated_depsgraph_get();angle=age*.4 if active and not ghost else 0;assert abs(rotor.evaluated_get(dg).rotation_euler.y+angle)<1e-5;points=0
   for p in G['parts']:
    o=bpy.data.objects[p['name']].evaluated_get(dg)
    for v,vv in zip(o.data.vertices,p['vertices']):
     x,y,z=vv
     if p['group']=='rotating':vv=[x*math.cos(angle)-y*math.sin(angle),x*math.sin(angle)+y*math.cos(angle),z]
     expected=V([a+b for a,b in zip(rot(vv,n),C['nodes'][n]['point'])]);assert (o.matrix_world@v.co-expected).length<1e-5,(n,active,ghost,age,p['name']);points+=1
   axis=root.matrix_world.to_3x3()@Vector((0,1,0));assert (axis-V([x*16 for x in C['nodes'][n]['normal']])).length<1e-6
   total+=points;cases.append(dict(node=n,active=bool(active),ghost=bool(ghost),age=age,angle=angle,vertexChecks=points,fixedMountUnmoved=True))
result=dict(asset='F05-07',allPassed=True,sourceReopened=True,productionMeshes=67,packedAtlas=[64,64],hiddenReferenceMeshes=68,actualRotationCases=cases,totalEvaluatedVertexChecks=total,fixedMountAndAxleUnmoved=True,actualActiveGhostDriver=True,sourceDefaultIdle=True,noProductionEmission=True,gameIntegrated=False)
(R/'review/blender-source-checks.json').write_text(json.dumps(result,indent=2)+'\n');print('Actual Blender wheel driver and6 node bases x15 cases PASS',total)
