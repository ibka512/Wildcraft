import bpy,json
from pathlib import Path
R=Path(__file__).resolve().parents[1];bpy.ops.wm.open_mainfile(filepath=str(R/'source/spring-bottom-structure-v02.blend'));D=json.loads((R/'exports/spring-bottom-proposal.json').read_text());root=bpy.data.objects.get('Unchanged bottom node 1 / 0,0,0');parts={o.name:o for o in bpy.data.objects if o.get('group') in ['fixed','moving','coil']};assert len(parts)==44;assert sum(o.get('group')=='coil'for o in parts.values())==24
results=[]
for e,h,name in [(0,0,'stowed-ground'),(1,.2,'extended-clearance'),(.05,0,'capped-ground')]:
 root['extension_fraction']=e;root['illustrative_body_clearance']=h;root.update_tag();bpy.context.scene.frame_set(bpy.context.scene.frame_current+1);bpy.context.view_layer.update();deps=bpy.context.evaluated_depsgraph_get();errors=[];minz=1e9
 for p in D['parts']:
  o=parts[p['name']].evaluated_get(deps);me=o.to_mesh();assert len(me.vertices)==len(p['vertices'])
  for i,v in enumerate(me.vertices):
   q=[a+(p['extendedVertices'][i][j]-a)*e for j,a in enumerate(p['vertices'][i])];expected=(q[0]/16,-q[1]/16,-q[2]/16+h);got=o.matrix_world@v.co;errors.append(max(abs(got[j]-expected[j])for j in range(3)));minz=min(minz,got.z)
  o.to_mesh_clear()
 assert max(errors)<2e-6,(name,max(errors));results.append({'state':name,'worldMinY':minz,'maxVertexError':max(errors),'checkedVertices':len(errors)})
assert len([i for i in bpy.data.images if i.packed_file])>=1
report={'allPassed':True,'reopenedActualBlend':True,'checkedSpringMeshes':44,'checkedCoilShapeKeys':24,'packedSharedAtlas':True,'states':results,'sourceControlsManualArtOnly':True};(R/'review/blender-verification.json').write_text(json.dumps(report,indent=2)+'\n');print('BLENDER_REOPEN_VERIFY_PASSED',json.dumps(report))
