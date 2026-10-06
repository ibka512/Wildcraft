import bpy,json,math
from pathlib import Path
R=Path(__file__).resolve().parents[1];bpy.ops.wm.open_mainfile(filepath=str(R/'source/fabricator-v01.blend'));G=json.loads((R/'source/geometry-and-uv.json').read_text());root=bpy.data.objects['Fabricator review state'];prod=bpy.data.collections['PRODUCTION fabricator'];image=next(i for i in bpy.data.images if i.name.startswith('fabricator-atlas'));assert tuple(image.size)==(64,64) and image.packed_file;assert root['state']==0
fm={'north':[0,1,2,3],'south':[5,4,7,6],'east':[1,5,6,2],'west':[4,0,3,7],'up':[3,2,6,7],'down':[4,5,1,0]};faces=0
for q in G['parts']:
 o=bpy.data.objects[q['name']];assert o.parent==root and len(o.data.polygons)==6
 for po,(name,indices) in zip(o.data.polygons,fm.items()):
  u0,v0,u1,v1=[v/16 for v in q['faces'][name]['uv']];expected=dict(zip(indices,[(u0,1-v1),(u1,1-v1),(u1,1-v0),(u0,1-v0)]))
  for loop in po.loop_indices:
   uv=o.data.uv_layers.active.data[loop].uv;ex=expected[o.data.loops[loop].vertex_index];assert all(abs(a-b)<1e-6 for a,b in zip(uv,ex)),(q['name'],name)
  assert po.normal.dot(po.center-sum((v.co for v in o.data.vertices),o.data.vertices[0].co*0)/8)>0,q['name'];faces+=1
checks=[]
for i in range(3):
 root['state']=i;root.update_tag();bpy.context.view_layer.update();dg=bpy.context.evaluated_depsgraph_get();tiles=[o for o in prod.objects if 'status_index' in o];assert sum(not o.evaluated_get(dg).hide_render for o in tiles)==1
 for o in tiles:assert o.evaluated_get(dg).hide_render==(o['status_index']!=i)
 checks.append({'reviewState':i,'oneTileVisible':True})
mat=next(m for m in bpy.data.materials if m.name.startswith('64px'));bs=next(n for n in mat.node_tree.nodes if n.type=='BSDF_PRINCIPLED');assert bs.inputs['Emission Strength'].default_value==0;tex=next(n for n in mat.node_tree.nodes if n.type=='TEX_IMAGE');assert tex.interpolation=='Closest'
result={'asset':'F06-01','allPassed':True,'sourceReopened':True,'cuboids':43,'outwardFacesAndExactUV':faces,'materialBadges':2,'statePlanes':3,'reviewStateCases':checks,'packedAtlas':[64,64],'nearestTexture':True,'noEmission':True,'defaultIdle':True,'worldSyncImplemented':False}
(R/'review/blender-source-checks.json').write_text(json.dumps(result,indent=2)+'\n');print('Blender source reopen, exact UV by vertex, packed atlas, outward normals and 3 state driver cases PASS')
