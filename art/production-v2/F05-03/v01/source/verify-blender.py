import bpy,json,math
from pathlib import Path
from mathutils import Vector
R=Path(__file__).resolve().parents[1];bpy.ops.wm.open_mainfile(filepath=str(R/'source/fan-v01.blend'));G=json.loads((R/'source/geometry-and-uv.json').read_text());prod=bpy.data.collections['PRODUCTION fan'];ref=bpy.data.collections['REFERENCE adopted F05-01 chassis'];root=bpy.data.objects['Fan node origin'];rotor=bpy.data.objects['Rotor exact axis +Z']
assert sum(o.type=='MESH'for o in prod.objects)==24 and sum(o.type=='MESH'for o in ref.objects)==68;assert all(o.hide_render for o in ref.objects)
im=next(i for i in bpy.data.images if i.name.startswith('shared-machine-atlas'));assert tuple(im.size)==(64,64)and im.packed_file;assert (rotor.location-Vector((0,1.76/16,0))).length<1e-7
fixed={o.name:o.matrix_world.copy()for o in prod.objects if o.get('group')=='housing'};cases=[]
for active,ghost in [(0,0),(1,0),(1,1)]:
 for age in [0,1,10,45,100]:
  root['active']=active;root['ghost']=ghost;root['ageInTicks']=age;root.update_tag();bpy.context.view_layer.update();dg=bpy.context.evaluated_depsgraph_get();angle=age*.7 if active and not ghost else 0;er=rotor.evaluated_get(dg);assert abs(er.rotation_euler.y+angle)<1e-5,(age,active,ghost,er.rotation_euler.y)
  for o in prod.objects:
   if o.get('group')=='housing':assert sum(abs(o.evaluated_get(dg).matrix_world[i][j]-fixed[o.name][i][j])for i in range(4)for j in range(4))<1e-7
  # Independent actual vertex under evaluated parent matches local XY rotation.
  o=bpy.data.objects['blade_0'];v=o.evaluated_get(dg).matrix_world@o.data.vertices[0].co;x,y,z=G['parts'][18]['vertices'][0];expected=Vector(((x*math.cos(angle)-y*math.sin(angle))/16,z/16,(x*math.sin(angle)+y*math.cos(angle))/16));assert (v-expected).length<1e-5
  cases.append(dict(age=age,active=bool(active),ghost=bool(ghost),angle=angle,fixedHousingUnmoved=True,actualBladeVertexMatches=True))
normalchecks=[]
for n,rot in enumerate([(math.pi/2,0,0),(-math.pi/2,0,0),(0,0,math.pi),(0,0,0),(0,0,math.pi/2),(0,0,-math.pi/2)]):
 root.rotation_euler=rot;bpy.context.view_layer.update();d=root.matrix_world.to_3x3()@Vector((0,1,0));normal=[[0,0,1],[0,0,-1],[0,-1,0],[0,1,0],[-1,0,0],[1,0,0]][n];assert (d-Vector(normal)).length<1e-6;normalchecks.append(dict(node=n,exhaustCorrect=True,thrustOpposite=True))
tex=next(n for n in bpy.data.materials['Approved 64px shared machine atlas'].node_tree.nodes if n.type=='TEX_IMAGE');assert tex.interpolation=='Closest';bs=next(n for n in bpy.data.materials['Approved 64px shared machine atlas'].node_tree.nodes if n.type=='BSDF_PRINCIPLED');assert bs.inputs['Emission Strength'].default_value==0
result=dict(asset='F05-03',allPassed=True,sourceReopened=True,productionMeshes=24,hiddenAdoptedReferenceMeshes=68,packedAtlas=[64,64],actualAnimationCases=cases,actualNodeDirectionCases=normalchecks,noEmission=True,motionBlur=False,gameIntegrated=False);(R/'review/blender-source-checks.json').write_text(json.dumps(result,indent=2)+'\n');print('Blender重开、15组实际叶轮/固定壳体驱动和6向排风轴通过。')
