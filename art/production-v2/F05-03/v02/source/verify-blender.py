import bpy,json,math,hashlib
from pathlib import Path
R=Path(__file__).resolve().parents[1];path=R/'source/fan-bottom-structure-v02.blend';bpy.ops.wm.open_mainfile(filepath=str(path));D=json.loads((R/'exports/fan-bottom-proposal.json').read_text());root=bpy.data.objects['Unchanged bottom node 1 / 0,0,0'];rotor=bpy.data.objects['Rotor translated exact pivot / actual active snapshot'];parts={o.name:o for o in bpy.data.objects if o.get('group')in ['housing','rotor']};body=[o for o in bpy.data.objects if o.get('group')=='body'];assert len(parts)==26 and len(body)==67;assert sum(o.get('group')=='rotor'for o in parts.values())==6;assert all(not o.data.shape_keys for o in parts.values());uv_checks=0
for source in D['parts']+D['adapters']+D['candidateBody']:
 me=bpy.data.objects[source['name']].data;uv=me.uv_layers.active;expected={tuple(sorted(f['indices'])):{j:(u/16,1-v/16)for j,(u,v)in zip(f['indices'],f['uvs'])}for f in source['faces']}
 for poly in me.polygons:
  original=expected[tuple(sorted(poly.vertices))]
  for loop in poly.loop_indices:
   vertex=me.loops[loop].vertex_index;got=uv.data[loop].uv;assert max(abs(got[i]-original[vertex][i])for i in range(2))<1e-6;uv_checks+=1
results=[]
for h,a,active,ghost,age in [(h,a,active,ghost,age)for h,a in[(0,0),(.2,0),(.2,6),(.2,-6)]for active,ghost in[(False,False),(True,False),(True,True)]for age in[0,1,10,45,100]]:
 root['illustrative_body_height']=h;root['illustrative_roll_deg']=a;root['active']=active;root['ghost']=ghost;root['ageInTicks']=age;root.update_tag();bpy.context.scene.frame_set(bpy.context.scene.frame_current+1);bpy.context.view_layer.update();dep=bpy.context.evaluated_depsgraph_get();errors=[];minz=1e9;c=math.cos(math.radians(a));si=math.sin(math.radians(a));angle=age*.7 if active and not ghost else 0;ca=math.cos(angle);sa=math.sin(angle);assert abs(rotor.evaluated_get(dep).rotation_euler.z+angle)<1e-5
 for p in D['parts']+D['adapters']+D['candidateBody']:
  o=bpy.data.objects[p['name']].evaluated_get(dep);me=o.to_mesh();assert len(me.vertices)==len(p['vertices'])
  for i,v in enumerate(me.vertices):
   x,y,z=p['vertices'][i]
   if p['group']=='rotor':x,y=x*ca-y*sa,x*sa+y*ca
   if p['group']=='body':x/=16;worldY=y/16;depth=-z/16
   else:x/=16;worldY=-z/16;depth=-y/16
   expected=(x*c-worldY*si,depth,x*si+worldY*c+h);got=o.matrix_world@v.co;errors.append(max(abs(got[j]-expected[j])for j in range(3)))
   if p['group']!='body':minz=min(minz,got.z)
  o.to_mesh_clear()
 assert max(errors)<3e-6,(h,a,active,ghost,age,max(errors));results.append({'height':h,'roll':a,'active':active,'ghost':ghost,'age':age,'worldMinY':minz,'maxVertexError':max(errors),'checkedVertices':len(errors)})
packed=[i for i in bpy.data.images if i.packed_file];assert len(packed)>=1;assert hashlib.sha256(packed[0].packed_file.data).hexdigest()==hashlib.sha256((R/'exports/shared-machine-atlas-64.png').read_bytes()).hexdigest()
report={'allPassed':True,'reopenedActualBlend':True,'sourceSHA256':hashlib.sha256(path.read_bytes()).hexdigest(),'fanMeshes':26,'bodyMeshes':67,'rotorMeshes':6,'fixedMeshes':20,'shapeKeys':0,'packedPNGByteExact':True,'nativeUVLoopsChecked':uv_checks,'actualDriverCases':len(results),'states':results};(R/'review/blender-verification.json').write_text(json.dumps(report,indent=2)+'\n');print('BLENDER_REOPEN_VERIFY_PASSED',len(results),'driver cases',uv_checks,'UV loops')
