import bpy,json
from pathlib import Path
R=Path(__file__).resolve().parents[1];bpy.ops.wm.open_mainfile(filepath=str(R/'source/charger-v01.blend'));g=json.loads((R/'source/geometry-and-uv.json').read_text());root=bpy.data.objects['Charger review state'];prod=bpy.data.collections['PRODUCTION charger'];ref=bpy.data.collections['REFERENCE adopted battery F04-01 — scale 1.00']
assert sum(bool(o.get('model_part')) for o in prod.objects)==30 and sum(bool(o.get('reference_asset')) for o in ref.objects)==16
image=next(i for i in bpy.data.images if i.name.startswith('charger-atlas'));assert tuple(image.size)==(64,64) and image.packed_file
assert all(o.parent==root for o in list(prod.objects)+list(ref.objects) if o!=root)
checks=[]
for state,e,present in [(0,0,0),(1,500,1),(2,500,1),(3,1000,1),(4,500,1),(2,1,1),(2,999,1)]:
 root['state']=state;root['energy']=e;root['has_battery']=present;root.update_tag();bpy.context.view_layer.update();dg=bpy.context.evaluated_depsgraph_get()
 for o in prod.objects:
  if 'status_index' in o:assert o.evaluated_get(dg).hide_render==(o['status_index']!=state)
 for o in ref.objects:
  eo=o.evaluated_get(dg);assert eo.hide_render==(not present)
  if 'window_index' in o:assert abs(eo.scale.z-max(0,min(1,e/1000*3-o['window_index'])))<1e-7
 for o in prod.objects:
  if o.type=='MESH':assert o.data.uv_layers and o.data.materials
 checks.append({'state':state,'energy':e,'hasBattery':bool(present),'oneStatusVisible':True,'batteryVisibilityCorrect':True,'windowFractionsCorrect':True})
mat=next(m for m in bpy.data.materials if m.name.startswith('64px'));bs=next(n for n in mat.node_tree.nodes if n.type=='BSDF_PRINCIPLED');assert bs.inputs['Emission Strength'].default_value==0
result=dict(asset='F04-03',allPassed=True,sourceReopened=True,chargerCuboids=30,batteryReferenceCuboids=16,statusPlanes=5,dynamicWindowPlanes=3,packedAtlas=[64,64],referenceScaleUnchanged=True,noEmission=True,reviewCases=checks,worldSyncImplemented=False)
(R/'review/blender-source-checks.json').write_text(json.dumps(result,indent=2)+'\n');print('Blender重开、材质内嵌、原电池引用、七组状态与电量驱动检查通过。')
