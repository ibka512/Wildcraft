import bpy,json,bmesh,math
from pathlib import Path
from mathutils import Vector
R=Path(__file__).resolve().parents[1];D=json.loads((R/'source/structure-geometry.json').read_text());bpy.ops.object.select_all(action='SELECT');bpy.ops.object.delete(use_global=False);bpy.context.preferences.filepaths.save_version=0
s=bpy.context.scene;s.render.engine='CYCLES';s.cycles.samples=12;s.render.resolution_x=900;s.render.resolution_y=700;s.render.resolution_percentage=100;s.world.color=(.2,.2,.2);s.view_settings.view_transform='Standard'
mat=bpy.data.materials.new('Adopted shared64x64 atlas / unchanged');mat.use_nodes=True;bs=next(n for n in mat.node_tree.nodes if n.type=='BSDF_PRINCIPLED');bs.inputs['Roughness'].default_value=.87;tex=mat.node_tree.nodes.new('ShaderNodeTexImage');tex.image=bpy.data.images.load(str(R/'exports/shared-machine-atlas-64.png'));tex.image.pack();tex.interpolation='Closest';mat.node_tree.links.new(tex.outputs['Color'],bs.inputs['Base Color'])
def coll(n):c=bpy.data.collections.new(n);s.collection.children.link(c);return c
body=coll('PROPOSAL F05-01 fan pocket / art only');fan=coll('PROPOSAL F05-03 fixed housing and driven rotor');studio=coll('REVIEW only');root=bpy.data.objects.new('Unchanged bottom node 1 / 0,0,0',None);fan.objects.link(root)
for k,v in {'illustrative_body_height':0.,'illustrative_roll_deg':0.,'active':False,'ghost':False,'ageInTicks':0.,'not_game_integrated':True}.items():root[k]=v
for prop,index,key,expr in [('location',2,'illustrative_body_height','h'),('rotation_euler',1,'illustrative_roll_deg','-a*pi/180')]:
 fc=root.driver_add(prop,index);v=fc.driver.variables.new();v.name='h'if key.endswith('height')else'a';v.type='SINGLE_PROP';v.targets[0].id=root;v.targets[0].data_path='["'+key+'"]';fc.driver.expression=expr
rotor=bpy.data.objects.new('Rotor translated exact pivot / actual active snapshot',None);fan.objects.link(rotor);rotor.parent=root;rotor.location=(0,0,1.92/16);fc=rotor.driver_add('rotation_euler',2)
for key in ['active','ghost','ageInTicks']:
 v=fc.driver.variables.new();v.name=key;v.type='SINGLE_PROP';v.targets[0].id=root;v.targets[0].data_path='["'+key+'"]'
fc.driver.expression='-ageInTicks*.7 if active>.5 and ghost<.5 else 0'
def world(v,bottom=False):x,y,z=v;return Vector((x/16,-y/16,-z/16)if bottom else(x/16,-z/16,y/16))
def mesh(p,c,bottom=False):
 moving=p['group']=='rotor';parent=rotor if moving else root;vertices=[world(v,bottom)-(rotor.location if moving else Vector((0,0,0)))for v in p['vertices']];me=bpy.data.meshes.new(p['name']);me.from_pydata(vertices,[],[f['indices']for f in p['faces']]);me.update();o=bpy.data.objects.new(p['name'],me);c.objects.link(o);o.parent=parent;me.materials.append(mat);o['group']=p['group'] if bottom else'body';uv=me.uv_layers.new(name='Original 64x64 UV / zero new pixels')
 for poly,f in zip(me.polygons,p['faces']):
  for j,(u,v)in zip(poly.loop_indices,f['uvs']):uv.data[j].uv=(u/16,1-v/16)
 bm=bmesh.new();bm.from_mesh(me);bmesh.ops.recalc_face_normals(bm,faces=bm.faces);bm.to_mesh(me);bm.free()
for p in D['candidateBody']:mesh(p,body)
for p in D['parts']+D['adapters']:mesh(p,fan,True)
bpy.ops.mesh.primitive_plane_add(size=8,location=(0,0,0));floor=bpy.context.object;floor.name='REVIEW ground, no collision change';fm=bpy.data.materials.new('Review floor');fm.diffuse_color=(.09,.14,.12,1);floor.data.materials.append(fm)
for name,loc,power in [('Key',(-3,-4,5),500),('Fill',(4,2,3),300),('Bottom',(1,-2,.2),100)]:
 data=bpy.data.lights.new(name,'AREA');data.energy=power;data.shape='DISK';data.size=4;o=bpy.data.objects.new(name,data);studio.objects.link(o);o.location=loc;o.rotation_euler=(Vector((0,0,.3))-o.location).to_track_quat('-Z','Y').to_euler()
camd=bpy.data.cameras.new('Review camera');cam=bpy.data.objects.new('Review camera',camd);studio.objects.link(cam);s.camera=cam;camd.type='ORTHO';camd.ortho_scale=2.2;cam.location=(2.3,-3.2,1.6);cam.rotation_euler=(Vector((0,0,.28))-cam.location).to_track_quat('-Z','Y').to_euler();s['proposal_only']=True;s['no_runtime_changes']=True;bpy.ops.wm.save_as_mainfile(filepath=str(R/'source/fan-bottom-structure-v02.blend'));s.render.filepath=str(R/'review/blender-complete-body.png');bpy.ops.render.render(write_still=True)
for o in body.objects:
 bm=bmesh.new();bm.from_mesh(o.data);bmesh.ops.bisect_plane(bm,geom=list(bm.verts)+list(bm.edges)+list(bm.faces),plane_co=(0,0,0),plane_no=(0,1,0),clear_inner=True,dist=.000001);bm.to_mesh(o.data);bm.free()
s.render.filepath=str(R/'review/blender-pocket-cutaway.png');bpy.ops.render.render(write_still=True);root['active']=True;root['ageInTicks']=2.;root.update_tag();bpy.context.scene.frame_set(1);bpy.context.view_layer.update();s.render.filepath=str(R/'review/blender-running-cutaway.png');bpy.ops.render.render(write_still=True)
print('BLENDER_FAN_PROPOSAL_SAVED: complete editable body,24 original meshes,2 fixed spacers,exact translated rotor pivot,packed64x64 atlas.')
