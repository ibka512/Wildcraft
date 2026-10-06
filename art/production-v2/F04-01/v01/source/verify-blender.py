import bpy,json
from pathlib import Path
R=Path(__file__).resolve().parents[1];bpy.ops.wm.open_mainfile(filepath=str(R/'source/battery-v01.blend'))
prod=bpy.data.collections['PRODUCTION battery — fixed shell and charge windows'];root=bpy.data.objects['Battery — energy 0..1000'];fixed=[o for o in prod.objects if o.type=='MESH' and 'model_part' in o];windows=[o for o in prod.objects if o.type=='MESH' and 'window_index' in o]
assert len(fixed)==16 and len(windows)==3 and all(o.parent==root for o in fixed+windows)
assert not any('REVIEW ONLY' in o.name for o in bpy.data.objects)
tex=next(n for m in bpy.data.materials if m.name.startswith('32px native') for n in m.node_tree.nodes if n.type=='TEX_IMAGE').image
assert tex.packed_file is not None and list(tex.size)==[32,32]
values=[]
for n in [0,1,333,500,667,999,1000]:
 root['energy']=n;root.update_tag();bpy.context.view_layer.update();actual=[o.evaluated_get(bpy.context.evaluated_depsgraph_get()).scale.z for o in sorted(windows,key=lambda v:v['window_index'])];expected=[max(0,min(1,n/1000*3-i)) for i in range(3)];assert all(abs(a-b)<1e-5 for a,b in zip(actual,expected)),(n,actual,expected)
 values.append({'energy':n,'bottomMiddleTopFraction':actual})
for o in windows:assert o.data.polygons[0].normal.y>0.99
record={'allPassed':True,'modelReopened':True,'fixedCuboids':16,'dynamicWindowPlanes':3,'packedAtlasSize':[32,32],'noFitFixturesInSource':True,'windowNormalsFrontFacing':True,'windowDriverChecks':values,'gameIntegrationTested':False}
(R/'review/blender-source-checks.json').write_text(json.dumps(record,indent=2)+'\n');print(json.dumps(record))
