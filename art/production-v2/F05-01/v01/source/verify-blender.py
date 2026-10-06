import bpy,json,math
from pathlib import Path
from mathutils import Vector
R=Path(__file__).resolve().parents[1];bpy.ops.wm.open_mainfile(filepath=str(R/'source/machine-body-v01.blend'));g=json.loads((R/'source/geometry-and-uv.json').read_text());prod=bpy.data.collections['PRODUCTION body and six nodes'];ref=bpy.data.collections['REFERENCE adopted battery scale 1.00'];br=bpy.data.objects['Battery review mount']
assert sum(o.type=='MESH' for o in prod.objects)==68;assert sum(bool(o.get('reference_asset')) for o in ref.objects)==16
im=next(i for i in bpy.data.images if i.name.startswith('machine-atlas'));assert tuple(im.size)==(64,64) and im.packed_file
for o in list(prod.objects)+list(ref.objects):
 if o.type=='MESH':assert o.data.uv_layers and o.data.materials
rotations=[(math.pi/2,0,0),(-math.pi/2,0,0),(0,0,math.pi),(0,0,0),(0,0,math.pi/2),(0,0,-math.pi/2)]
for o in ref.objects:o.hide_viewport=o.hide_render=False
bpy.context.view_layer.update()
checks=[]
for n in range(6):
 point=g['nodes'][n]['point'];normal=g['nodes'][n]['normal'];br.location=(point[0]/16,point[2]/16,point[1]/16);br.rotation_euler=rotations[n];bpy.context.view_layer.update();direction=br.matrix_world.to_3x3()@Vector((0,1,0));assert (direction-Vector((normal[0],normal[2],normal[1]))).length<1e-6
 assert all(abs(v-1)<1e-9 for v in br.scale)
 for e in [0,1,500,999,1000]:
  br['energy']=e;br.update_tag();bpy.context.view_layer.update();dg=bpy.context.evaluated_depsgraph_get()
  for o in ref.objects:
   if 'window_index' in o:assert abs(o.evaluated_get(dg).scale.z-max(0,min(1,e/1000*3-o['window_index'])))<1e-6, (n,e,o.name,o.evaluated_get(dg).scale.z)
  checks.append(dict(node=n,energy=e,orientationCorrect=True,scale=1,windowFractionsCorrect=True))
mat=next(m for m in bpy.data.materials if m.name.startswith('64px'));bs=next(n for n in mat.node_tree.nodes if n.type=='BSDF_PRINCIPLED');assert bs.inputs['Emission Strength'].default_value==0
result=dict(asset='F05-01',allPassed=True,sourceReopened=True,productionCuboids=68,bodyAndControl=20,nodeCuboids=48,referenceBatteryCuboids=16,dynamicWindowPlanes=3,packedAtlas=[64,64],noEmission=True,nodeChargeCases=checks,fullSixMountRuntimeFitTested=False,gameIntegrated=False)
(R/'review/blender-source-checks.json').write_text(json.dumps(result,indent=2)+'\n');print('Blender源重开、68盒显式UV、内嵌图集、6向原电池30组朝向/余量驱动通过。')
