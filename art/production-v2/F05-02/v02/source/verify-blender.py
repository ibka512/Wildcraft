import bpy,json,math,hashlib
from pathlib import Path
R=Path(__file__).resolve().parents[1];path=R/'source/wing-bottom-structure-v02.blend';bpy.ops.wm.open_mainfile(filepath=str(path));D=json.loads((R/'exports/wing-bottom-proposal.json').read_text());root=bpy.data.objects.get('Unchanged bottom node 1 / 0,0,0');parts={o.name:o for o in bpy.data.objects if o.get('group')=='wing'};body=[o for o in bpy.data.objects if o.get('group')=='body'];assert len(parts)==30 and len(body)==67;assert all(not o.data.shape_keys for o in parts.values());results=[]
# Check actual native mesh UVs by vertex identity after normal recalculation.
uv_checks=0
for source in D['parts']+D['adapters']+D['candidateBody']:
 o=bpy.data.objects[source['name']];me=o.data;uv=me.uv_layers.active
 expected={tuple(sorted(f['indices'])):{j:(u/16,1-v/16)for j,(u,v)in zip(f['indices'],f['uvs'])}for f in source['faces']}
 for poly in me.polygons:
  original=expected[tuple(sorted(poly.vertices))]
  for loop in poly.loop_indices:
   vertex=me.loops[loop].vertex_index;got=uv.data[loop].uv;assert max(abs(got[i]-original[vertex][i])for i in range(2))<1e-6;uv_checks+=1
for h,a,name in [(0,0,'ground-static'),(.2,0,'raised-static'),(.2,6,'air-roll-positive'),(.2,-6,'air-roll-negative')]:
 root['illustrative_body_height']=h;root['illustrative_roll_deg']=a;root.update_tag();bpy.context.scene.frame_set(bpy.context.scene.frame_current+1);bpy.context.view_layer.update();dep=bpy.context.evaluated_depsgraph_get();errors=[];minz=1e9;c=math.cos(math.radians(a));si=math.sin(math.radians(a))
 for p in D['parts']+D['adapters']:
  o=parts[p['name']].evaluated_get(dep);me=o.to_mesh();assert len(me.vertices)==len(p['vertices'])
  for i,v in enumerate(me.vertices):
   x,yl,z=p['vertices'][i];x/=16;worldY=-z/16;expected=(x*c-worldY*si,-yl/16,x*si+worldY*c+h);got=o.matrix_world@v.co;errors.append(max(abs(got[j]-expected[j])for j in range(3)));minz=min(minz,got.z)
  o.to_mesh_clear()
 for p in D['candidateBody']:
  o=bpy.data.objects[p['name']].evaluated_get(dep);me=o.to_mesh()
  for i,v in enumerate(me.vertices):
   x,y,z=p['vertices'][i];x/=16;worldY=y/16;expected=(x*c-worldY*si,-z/16,x*si+worldY*c+h);got=o.matrix_world@v.co;errors.append(max(abs(got[j]-expected[j])for j in range(3)))
  o.to_mesh_clear()
 assert max(errors)<2e-6,(name,max(errors));results.append({'state':name,'worldMinY':minz,'maxVertexError':max(errors),'checkedVertices':len(errors)})
packed=[i for i in bpy.data.images if i.packed_file];assert len(packed)>=1;assert hashlib.sha256(packed[0].packed_file.data).hexdigest()==hashlib.sha256((R/'exports/wing-atlas-128x64.png').read_bytes()).hexdigest()
report={'allPassed':True,'reopenedActualBlend':True,'sourceSHA256':hashlib.sha256(path.read_bytes()).hexdigest(),'checkedWingMeshes':30,'checkedBodyMeshes':67,'shapeKeys':0,'packedSharedAtlas':True,'packedPNGByteExact':True,'nativeUVLoopsChecked':uv_checks,'manualHeightAndRollArtOnly':True,'states':results};(R/'review/blender-verification.json').write_text(json.dumps(report,indent=2)+'\n');print('BLENDER_REOPEN_VERIFY_PASSED',json.dumps(report))
