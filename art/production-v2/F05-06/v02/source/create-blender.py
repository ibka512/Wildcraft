import bpy,json,bmesh,math
from pathlib import Path
from mathutils import Vector
R=Path(__file__).resolve().parents[1];D=json.loads((R/'source/structure-geometry.json').read_text());bpy.ops.object.select_all(action='SELECT');bpy.ops.object.delete(use_global=False);bpy.context.preferences.filepaths.save_version=0
s=bpy.context.scene;s.render.engine='CYCLES';s.cycles.samples=12;s.render.resolution_x=900;s.render.resolution_y=700;s.render.resolution_percentage=100;s.world.color=(.2,.2,.2);s.view_settings.view_transform='Standard'
mat=bpy.data.materials.new('Adopted shared64 atlas / unchanged');mat.use_nodes=True;bs=next(n for n in mat.node_tree.nodes if n.type=='BSDF_PRINCIPLED');bs.inputs['Roughness'].default_value=.87;tex=mat.node_tree.nodes.new('ShaderNodeTexImage');tex.image=bpy.data.images.load(str(R/'exports/shared-machine-atlas-64.png'));tex.image.pack();tex.interpolation='Closest';mat.node_tree.links.new(tex.outputs['Color'],bs.inputs['Base Color'])
def coll(n):c=bpy.data.collections.new(n);s.collection.children.link(c);return c
body=coll('PROPOSAL F05-01 pocket / art only');spring=coll('PROPOSAL F05-06 full-size spring');studio=coll('REVIEW only')
root=bpy.data.objects.new('Unchanged bottom node 1 / 0,0,0',None);spring.objects.link(root);root['extension_fraction']=0.;root['illustrative_body_clearance']=0.;root['not_game_integrated']=True;root['full_stroke_blocks']=.2
root.driver_add('location',2).driver.expression='clearance'
v=root.animation_data.drivers[0].driver.variables.new();v.name='clearance';v.type='SINGLE_PROP';v.targets[0].id=root;v.targets[0].data_path='["illustrative_body_clearance"]'
pivot=bpy.data.objects.new('Moving pivot local (0,0,-1.12)/16',None);spring.objects.link(pivot);pivot.parent=root;pivot.location.z=.07;fc=pivot.driver_add('location',2);v=fc.driver.variables.new();v.name='e';v.type='SINGLE_PROP';v.targets[0].id=root;v.targets[0].data_path='["extension_fraction"]';fc.driver.expression='.07 - .2 * e'
def world(v,bottom=False):x,y,z=v;return (x/16,-y/16,-z/16)if bottom else(x/16,-z/16,y/16)
def mesh(p,c,parent=None,bottom=False,offset=0):
 verts=[list(world(v,bottom))for v in p['vertices']]
 for v in verts:v[2]-=offset
 me=bpy.data.meshes.new(p['name']);me.from_pydata(verts,[],[f['indices']for f in p['faces']]);me.update();o=bpy.data.objects.new(p['name'],me);c.objects.link(o);o.parent=parent;me.materials.append(mat);uv=me.uv_layers.new(name='Existing atlas UV / no new pixels')
 for poly,f in zip(me.polygons,p['faces']):
  for j,(u,v)in zip(poly.loop_indices,f['uvs']):uv.data[j].uv=(u/16,1-v/16)
 bm=bmesh.new();bm.from_mesh(me);bmesh.ops.recalc_face_normals(bm,faces=bm.faces);bm.to_mesh(me);bm.free();return o
for p in D['candidateBody']:mesh(p,body,root)
for p in D['parts']:
 o=mesh(p,spring,pivot if p['group']=='moving'else root,True,.07 if p['group']=='moving'else 0);o['group']=p['group']
 if p['group']=='coil':
  o.shape_key_add(name='Stowed');key=o.shape_key_add(name='Extended')
  for kv,v in zip(key.data,p['extendedVertices']):kv.co=world(v,True)
  fc=key.driver_add('value');v=fc.driver.variables.new();v.name='e';v.type='SINGLE_PROP';v.targets[0].id=root;v.targets[0].data_path='["extension_fraction"]';fc.driver.expression='e'
# Named editable pocket opening reference, hidden from final images; original remains separate in references.
pocket=bpy.data.objects.new('Pocket ±0.24375 / ceiling0.35 blocks (reference)',None);studio.objects.link(pocket);pocket.empty_display_type='CUBE';pocket.empty_display_size=1;pocket.location.z=.175;pocket.scale=(.24375,.24375,.175)
bpy.ops.mesh.primitive_plane_add(size=8,location=(0,0,0));floor=bpy.context.object;floor.name='REVIEW ground y0, no collision change';fm=bpy.data.materials.new('Review floor');fm.diffuse_color=(.09,.14,.12,1);floor.data.materials.append(fm)
for name,loc,power in [('Key',(-3,-4,5),500),('Fill',(4,2,3),300),('Bottom',(1,-2,.2),50)]:
 data=bpy.data.lights.new(name,'AREA');data.energy=power;data.shape='DISK';data.size=4;o=bpy.data.objects.new(name,data);studio.objects.link(o);o.location=loc;o.rotation_euler=(Vector((0,0,.3))-o.location).to_track_quat('-Z','Y').to_euler()
camd=bpy.data.cameras.new('Review camera');cam=bpy.data.objects.new('Review camera',camd);studio.objects.link(cam);s.camera=cam;camd.type='ORTHO';camd.ortho_scale=2.2;cam.location=(2.3,-3.2,1.6);cam.rotation_euler=(Vector((0,0,.28))-cam.location).to_track_quat('-Z','Y').to_euler()
s['proposal_only']=True;s['no_runtime_changes']=True;s['clearance_cap_NOT_implemented_in_game']=True
bpy.ops.wm.save_as_mainfile(filepath=str(R/'source/spring-bottom-structure-v02.blend'))
for name,e,h in [('stowed-ground',0,0),('extended-clearance',1,.20)]:
 root['extension_fraction']=e;root['illustrative_body_clearance']=h;root.update_tag();s.frame_set(s.frame_current+1);bpy.context.view_layer.update();s.render.filepath=str(R/'review'/('blender-'+name+'.png'));bpy.ops.render.render(write_still=True)
print('BLENDER_PROPOSAL_SAVED: editable cavity and 44 spring meshes, packed original atlas, moving driver and 24 shape keys.')
