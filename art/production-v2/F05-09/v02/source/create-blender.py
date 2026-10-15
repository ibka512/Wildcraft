import bpy,json,bmesh,math
from pathlib import Path
from mathutils import Vector
R=Path(__file__).resolve().parents[1];D=json.loads((R/'source/structure-geometry.json').read_text());bpy.ops.object.select_all(action='SELECT');bpy.ops.object.delete(use_global=False);bpy.context.preferences.filepaths.save_version=0
s=bpy.context.scene;s.render.engine='CYCLES';s.cycles.samples=12;s.render.resolution_x=900;s.render.resolution_y=700;s.render.resolution_percentage=100;s.world.color=(.2,.2,.2);s.view_settings.view_transform='Standard'
mat=bpy.data.materials.new('Adopted shared128x64 atlas / unchanged');mat.use_nodes=True;bs=next(n for n in mat.node_tree.nodes if n.type=='BSDF_PRINCIPLED');bs.inputs['Roughness'].default_value=.87;tex=mat.node_tree.nodes.new('ShaderNodeTexImage');tex.image=bpy.data.images.load(str(R/'exports/shared-fabric-machine-atlas-128x64.png'));tex.image.pack();tex.interpolation='Closest';mat.node_tree.links.new(tex.outputs['Color'],bs.inputs['Base Color'])
def coll(n):c=bpy.data.collections.new(n);s.collection.children.link(c);return c
body=coll('PROPOSAL F05-01 float pocket / art only');floats=coll('PROPOSAL F05-09 static full-size float');studio=coll('REVIEW only')
root=bpy.data.objects.new('Unchanged bottom node 1 / 0,0,0',None);floats.objects.link(root);root['illustrative_body_height']=0.;root['illustrative_roll_deg']=0.;root['not_game_integrated']=True;root['static_float_no_deploy_states']=True
for path,index,key,expr in [('location',2,'illustrative_body_height','h'),('rotation_euler',1,'illustrative_roll_deg','-a*pi/180')]:
 fc=root.driver_add(path,index);v=fc.driver.variables.new();v.name='h'if key.endswith('height')else'a';v.type='SINGLE_PROP';v.targets[0].id=root;v.targets[0].data_path='["'+key+'"]';fc.driver.expression=expr
def world(v,bottom=False):x,y,z=v;return(x/16,-y/16,-z/16)if bottom else(x/16,-z/16,y/16)
def mesh(p,c,parent,bottom=False):
 me=bpy.data.meshes.new(p['name']);me.from_pydata([world(v,bottom)for v in p['vertices']],[],[f['indices']for f in p['faces']]);me.update();o=bpy.data.objects.new(p['name'],me);c.objects.link(o);o.parent=parent;me.materials.append(mat);o['group']='float'if bottom else'body';uv=me.uv_layers.new(name='Original 128x64 UV / zero new pixels')
 for poly,f in zip(me.polygons,p['faces']):
  for j,(u,v)in zip(poly.loop_indices,f['uvs']):uv.data[j].uv=(u/16,1-v/16)
 bm=bmesh.new();bm.from_mesh(me);bmesh.ops.recalc_face_normals(bm,faces=bm.faces);bm.to_mesh(me);bm.free();return o
for p in D['candidateBody']:mesh(p,body,root)
for p in D['parts']:mesh(p,floats,root,True)
pocket=bpy.data.objects.new('Pocket 0.69x0.45 / depth0.41 (reference)',None);studio.objects.link(pocket);pocket.empty_display_type='CUBE';pocket.empty_display_size=1;pocket.location.z=.205;pocket.scale=(.345,.225,.205)
bpy.ops.mesh.primitive_plane_add(size=8,location=(0,0,0));floor=bpy.context.object;floor.name='REVIEW ground, no collision change';fm=bpy.data.materials.new('Review floor');fm.diffuse_color=(.09,.14,.12,1);floor.data.materials.append(fm)
for name,loc,power in [('Key',(-3,-4,5),500),('Fill',(4,2,3),300),('Bottom',(1,-2,.2),50)]:
 data=bpy.data.lights.new(name,'AREA');data.energy=power;data.shape='DISK';data.size=4;o=bpy.data.objects.new(name,data);studio.objects.link(o);o.location=loc;o.rotation_euler=(Vector((0,0,.3))-o.location).to_track_quat('-Z','Y').to_euler()
camd=bpy.data.cameras.new('Review camera');cam=bpy.data.objects.new('Review camera',camd);studio.objects.link(cam);s.camera=cam;camd.type='ORTHO';camd.ortho_scale=2.2;cam.location=(2.3,-3.2,1.6);cam.rotation_euler=(Vector((0,0,.28))-cam.location).to_track_quat('-Z','Y').to_euler()
s['proposal_only']=True;s['no_runtime_changes']=True;s['waterline_not_equilibrium']=True;bpy.ops.wm.save_as_mainfile(filepath=str(R/'source/buoyancy-bottom-structure-v02.blend'))
s.render.filepath=str(R/'review/blender-complete-body.png');bpy.ops.render.render(write_still=True)
# Actual open-front cutaway in an unsaved review scene: keep original editable source complete.
for o in body.objects:
 if o.type!='MESH':continue
 bm=bmesh.new();bm.from_mesh(o.data);bmesh.ops.bisect_plane(bm,geom=list(bm.verts)+list(bm.edges)+list(bm.faces),plane_co=(0,0,0),plane_no=(0,1,0),clear_inner=True,dist=.000001);bm.to_mesh(o.data);bm.free()
s.render.filepath=str(R/'review/blender-pocket-cutaway.png');bpy.ops.render.render(write_still=True)
print('BLENDER_FLOAT_PROPOSAL_SAVED: complete editable body pocket, 15 static float meshes, original packed128x64 atlas.')
