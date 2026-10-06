"""Actual meshes and shared UV, fixed housing + pivoted rotor, separate adopted chassis reference."""
import bpy,bmesh,json,math
from pathlib import Path
from mathutils import Vector
R=Path(__file__).resolve().parents[1];G=json.loads((R/'source/geometry-and-uv.json').read_text());C=json.loads((R/'references/approved-machine-geometry.json').read_text())
bpy.ops.object.select_all(action='SELECT');bpy.ops.object.delete(use_global=False);bpy.context.preferences.filepaths.save_version=0;s=bpy.context.scene;s.render.engine='CYCLES';s.cycles.samples=16;s.render.resolution_x=s.render.resolution_y=800;s.render.resolution_percentage=100;s.render.image_settings.file_format='PNG';s.view_settings.view_transform='Standard';s.world.color=(.22,.23,.23)
prod=bpy.data.collections.new('PRODUCTION fan');s.collection.children.link(prod);ref=bpy.data.collections.new('REFERENCE adopted F05-01 chassis');s.collection.children.link(ref);studio=bpy.data.collections.new('REVIEW studio');s.collection.children.link(studio)
root=bpy.data.objects.new('Fan node origin',None);prod.objects.link(root);root['active']=0;root['ghost']=0;root['ageInTicks']=0.;root['rateRadiansPerWorldTick']=.7;root['art_only']=True
rotor=bpy.data.objects.new('Rotor exact axis +Z',None);prod.objects.link(rotor);rotor.parent=root;rotor.location=(0,1.76/16,0)
mat=bpy.data.materials.new('Approved 64px shared machine atlas');mat.use_nodes=True;bs=next(n for n in mat.node_tree.nodes if n.type=='BSDF_PRINCIPLED');bs.inputs['Roughness'].default_value=.9;bs.inputs['Metallic'].default_value=.04;tex=mat.node_tree.nodes.new('ShaderNodeTexImage');tex.image=bpy.data.images.load(str(R/'exports/shared-machine-atlas-64.png'));tex.image.pack();tex.interpolation='Closest';mat.node_tree.links.new(tex.outputs['Color'],bs.inputs['Base Color'])
def V(v):x,y,z=v;return (x/16,z/16,y/16)
def mesh(p,col,parent,offset=0):
 vv=[[x,y,z-offset]for x,y,z in p['vertices']];me=bpy.data.meshes.new(p['name']);me.from_pydata([V(v)for v in vv],[],[f['indices']for f in p['faces']]);me.update();o=bpy.data.objects.new(p['name'],me);col.objects.link(o);o.parent=parent;o.data.materials.append(mat);o['part_name']=p['name'];o['group']=p.get('group','reference');uv=me.uv_layers.new(name='Explicit shared64 UV')
 for po,f in zip(me.polygons,p['faces']):
  for loop,(u,v)in zip(po.loop_indices,f['uvs']):uv.data[loop].uv=(u/16,1-v/16)
 bm=bmesh.new();bm.from_mesh(me);bmesh.ops.recalc_face_normals(bm,faces=bm.faces);bm.to_mesh(me);bm.free();return o
for p in G['parts']:mesh(p,prod,rotor if p['group']=='rotor'else root,1.76 if p['group']=='rotor'else 0)
d=rotor.driver_add('rotation_euler',1).driver
for name,key in [('age','ageInTicks'),('active','active'),('ghost','ghost')]:v=d.variables.new();v.name=name;v.targets[0].id=root;v.targets[0].data_path='["'+key+'"]'
d.expression='-age*.7 if active and not ghost else 0'
# Adopted body is a hidden named review reference, not baked into fan model.
FM={'north':[0,1,2,3],'south':[5,4,7,6],'east':[1,5,6,2],'west':[4,0,3,7],'up':[3,2,6,7],'down':[4,5,1,0]}
def rot(v,n):
 x,y,z=v;return [(x,z,-y),(x,-z,y),(-x,y,-z),(x,y,z),(-z,y,x),(z,y,-x)][n]
for p in C['parts']:
 x0,y0,z0=p['from'];x1,y1,z1=p['to'];vs=[[x0,y0,z0],[x1,y0,z0],[x1,y1,z0],[x0,y1,z0],[x0,y0,z1],[x1,y0,z1],[x1,y1,z1],[x0,y1,z1]]
 if 'node'in p:vs=[[a+b for a,b in zip(rot(v,p['node']),C['nodes'][p['node']]['point'])]for v in vs]
 faces=[]
 for f,ids in FM.items():u0,v0,u1,v1=p['faces'][f]['uv'];faces.append(dict(indices=ids,uvs=[[u0,v1],[u1,v1],[u1,v0],[u0,v0]]))
 o=mesh(dict(name='reference/'+p['name'],vertices=vs,faces=faces),ref,None);o.hide_render=o.hide_viewport=True;o['approved_reference']=True
fmat=bpy.data.materials.new('Review neutral ground');fmat.use_nodes=True;b=next(n for n in fmat.node_tree.nodes if n.type=='BSDF_PRINCIPLED');b.inputs['Base Color'].default_value=(.20,.22,.21,1);b.inputs['Roughness'].default_value=.95;bpy.ops.mesh.primitive_plane_add(size=200,location=(0,0,-.295));floor=bpy.context.object;floor.name='REVIEW ground';floor.data.materials.append(fmat)
for c in list(floor.users_collection):c.objects.unlink(floor)
studio.objects.link(floor)
for name,loc,power,size in [('Key',(-3,4,5),430,4),('Fill',(4,2,3),180,4),('Rim',(0,-4,4),210,4),('Bottom review fill',(0,1,-3),100,4)]:
 data=bpy.data.lights.new(name,'AREA');data.energy=power;data.shape='DISK';data.size=size;o=bpy.data.objects.new(name,data);studio.objects.link(o);o.location=loc;o.rotation_euler=(Vector((0,0,.1))-o.location).to_track_quat('-Z','Y').to_euler()
data=bpy.data.cameras.new('Orthographic actual fan');cam=bpy.data.objects.new('Orthographic actual fan',data);studio.objects.link(cam);s.camera=cam;data.type='ORTHO'
def camera(loc,target,scale):cam.location=loc;cam.rotation_euler=(Vector(target)-cam.location).to_track_quat('-Z','Y').to_euler();data.ortho_scale=scale
def state(age,active,ghost=0):root['ageInTicks']=age;root['active']=active;root['ghost']=ghost;root.update_tag();bpy.context.view_layer.update()
def render(name):s.render.filepath=str(R/'review'/name);bpy.ops.render.render(write_still=True)
state(0,0);camera((1.6,3,1.3),(0,.08,0),.9);bpy.ops.wm.save_as_mainfile(filepath=str(R/'source/fan-v01.blend'));render('fan-idle-three-quarter.png');camera((0,3,0),(0,.08,0),.78);render('fan-front.png');camera((1.6,-3,1.3),(0,.08,0),.9);render('fan-back.png');camera((3,.08,0),(0,.08,0),.75);render('fan-side.png');camera((1.6,3,1.3),(0,.08,0),.9);state(math.pi/(4*.7),1);render('fan-running-phase.png')
state(0,0);floor.location.z=-.05
for o in ref.objects:o.hide_render=o.hide_viewport=False
# Install at existing front node without scaling or altering shaft.
root.location=(0,.7,.325);camera((2.2,3.4,2.7),(0,.08,.32),2.45);render('fan-mounted-front.png');root.location=(0,0,.65);root.rotation_euler=(math.pi/2,0,0);camera((2.2,3.4,2.7),(0,0,.4),2.45);render('fan-mounted-top.png')
print('Saved fan source with fixed housing, exact rotor pivot and hidden adopted chassis reference; rendered 7 actual views.')
