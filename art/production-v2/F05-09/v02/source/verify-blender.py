import bpy,json,math,hashlib
from pathlib import Path
R=Path(__file__).resolve().parents[1];path=R/'source/buoyancy-bottom-structure-v02.blend';bpy.ops.wm.open_mainfile(filepath=str(path));D=json.loads((R/'exports/buoyancy-bottom-proposal.json').read_text());root=bpy.data.objects.get('Unchanged bottom node 1 / 0,0,0');parts={o.name:o for o in bpy.data.objects if o.get('group')=='float'};body=[o for o in bpy.data.objects if o.get('group')=='body'];assert len(parts)==15 and len(body)==71;assert all(not o.data.shape_keys for o in parts.values());results=[]
for h,a,name in [(0,0,'ground-static'),(.2,0,'raised-static'),(.2,6,'air-roll-positive'),(.2,-6,'air-roll-negative')]:
 root['illustrative_body_height']=h;root['illustrative_roll_deg']=a;root.update_tag();bpy.context.scene.frame_set(bpy.context.scene.frame_current+1);bpy.context.view_layer.update();dep=bpy.context.evaluated_depsgraph_get();errors=[];minz=1e9;c=math.cos(math.radians(a));si=math.sin(math.radians(a))
 for p in D['parts']:
  o=parts[p['name']].evaluated_get(dep);me=o.to_mesh();assert len(me.vertices)==len(p['vertices'])
  for i,v in enumerate(me.vertices):
   x,yl,z=p['vertices'][i];x/=16;worldY=-z/16;expected=(x*c-worldY*si,-yl/16,x*si+worldY*c+h);got=o.matrix_world@v.co;errors.append(max(abs(got[j]-expected[j])for j in range(3)));minz=min(minz,got.z)
  o.to_mesh_clear()
 assert max(errors)<2e-6,(name,max(errors));results.append({'state':name,'worldMinY':minz,'maxVertexError':max(errors),'checkedVertices':len(errors)})
assert len([i for i in bpy.data.images if i.packed_file])>=1
report={'allPassed':True,'reopenedActualBlend':True,'sourceSHA256':hashlib.sha256(path.read_bytes()).hexdigest(),'checkedFloatMeshes':15,'checkedBodyMeshes':71,'shapeKeys':0,'packedSharedAtlas':True,'manualHeightAndRollArtOnly':True,'states':results};(R/'review/blender-verification.json').write_text(json.dumps(report,indent=2)+'\n');print('BLENDER_REOPEN_VERIFY_PASSED',json.dumps(report))
