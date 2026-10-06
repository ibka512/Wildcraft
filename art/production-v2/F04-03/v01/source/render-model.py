"""Actual charger mesh/UV renders, separate unchanged adopted-battery reference."""
import bpy,bmesh,json
from pathlib import Path
from mathutils import Vector
R=Path(__file__).resolve().parents[1];G=json.loads((R/'source/geometry-and-uv.json').read_text())
bpy.ops.object.select_all(action='SELECT');bpy.ops.object.delete(use_global=False);bpy.context.preferences.filepaths.save_version=0
s=bpy.context.scene;s.render.engine='CYCLES';s.cycles.samples=20;s.render.resolution_x=s.render.resolution_y=900;s.render.resolution_percentage=100;s.render.image_settings.file_format='PNG';s.view_settings.view_transform='Standard';s.world.color=(.22,.23,.23)
prod=bpy.data.collections.new('PRODUCTION charger');s.collection.children.link(prod);ref=bpy.data.collections.new('REFERENCE adopted battery F04-01 — scale 1.00');s.collection.children.link(ref);studio=bpy.data.collections.new('REVIEW studio');s.collection.children.link(studio)
root=bpy.data.objects.new('Charger review state',None);prod.objects.link(root);root['energy']=500;root['has_battery']=1;root['state']=2;root['capacity']=1000;root['world_state']='Art review only; current game has menu status but no world presentation sync'
mat=bpy.data.materials.new('64px native common steel/copper atlas');mat.use_nodes=True;bs=next(n for n in mat.node_tree.nodes if n.type=='BSDF_PRINCIPLED');bs.inputs['Roughness'].default_value=.9;bs.inputs['Metallic'].default_value=.04;tex=mat.node_tree.nodes.new('ShaderNodeTexImage');tex.image=bpy.data.images.load(str(R/'exports/charger-atlas-64.png'));tex.image.pack();tex.interpolation='Closest';mat.node_tree.links.new(tex.outputs['Color'],bs.inputs['Base Color'])
def V(v):x,y,z=v;return ((x-8)/16,(z-8)/16,y/16)
fm={'north':[0,1,2,3],'south':[5,4,7,6],'east':[1,5,6,2],'west':[4,0,3,7],'up':[3,2,6,7],'down':[4,5,1,0]}
def visibility(o,prop,expr):
 for key in ['hide_render','hide_viewport']:
  curve=o.driver_add(key);d=curve.driver;v=d.variables.new();v.name='value';v.targets[0].id=root;v.targets[0].data_path='["'+prop+'"]';d.expression=expr
def box(e,col):
 x0,y0,z0=e['from'];x1,y1,z1=e['to'];vs=[[x0,y0,z0],[x1,y0,z0],[x1,y1,z0],[x0,y1,z0],[x0,y0,z1],[x1,y0,z1],[x1,y1,z1],[x0,y1,z1]];me=bpy.data.meshes.new(e['name']);me.from_pydata([V(v) for v in vs],[],list(fm.values()));me.update();o=bpy.data.objects.new(e['name'],me);col.objects.link(o);o.parent=root;o.data.materials.append(mat);o['model_part']=e['name'];uv=me.uv_layers.new(name='Explicit native64 UV')
 for po,face in zip(me.polygons,fm):
  u0,v0,u1,v1=[v/16 for v in e['faces'][face]['uv']];coords=[(u0,1-v1),(u1,1-v1),(u1,1-v0),(u0,1-v0)]
  for loop,co in zip(po.loop_indices,coords):uv.data[loop].uv=co
 bm=bmesh.new();bm.from_mesh(me);bmesh.ops.recalc_face_normals(bm,faces=bm.faces);bm.to_mesh(me);bm.free();return o
for e in G['parts']:box(e,prod)
for e in G['batteryParts']:o=box(e,ref);o['reference_asset']='F04-01 adopted, unscaled';visibility(o,'has_battery','value < 1')
def quad(name,a,b,uv,col):
 x0,y0,z=a;x1,y1,_=b;me=bpy.data.meshes.new(name);me.from_pydata([V(v) for v in [[x0,y0,z],[x0,y1,z],[x1,y1,z],[x1,y0,z]]],[],[(0,1,2,3)]);me.update();o=bpy.data.objects.new(name,me);col.objects.link(o);o.parent=root;o.data.materials.append(mat);ul=me.uv_layers.new(name='Native atlas region');u0,v0,u1,v1=[v/16 for v in uv]
 for loop,co in zip(me.polygons[0].loop_indices,[(u1,1-v1),(u1,1-v0),(u0,1-v0),(u0,1-v1)]):ul.data[loop].uv=co
 return o
for window in G['batteryWindows']:
 i=window['indexBottomUp'];a=window['from'];b=window['to'];o=quad('Battery dynamic charge '+str(i),[a[0],0,a[2]],[b[0],b[1]-a[1],b[2]],G['batteryEnergyUV'],ref);o.location.z=a[1]/16;visibility(o,'has_battery','value < 1');d=o.driver_add('scale',2).driver;v=d.variables.new();v.name='charge';v.targets[0].id=root;v.targets[0].data_path='["energy"]';d.expression=f'min(1,max(0,charge/1000*3-{i}))';o['window_index']=i
for i in range(5):
 q=G['statusPanel'];o=quad('Status tile '+str(i),q['from'],q['to'],q['tiles'][str(i)],prod);visibility(o,'state',f'value != {i}');o['status_index']=i
def solid(name,color):
 m=bpy.data.materials.new(name);m.use_nodes=True;bs=next(n for n in m.node_tree.nodes if n.type=='BSDF_PRINCIPLED');bs.inputs['Base Color'].default_value=(*color,1);bs.inputs['Roughness'].default_value=.95;return m
floor_mat=solid('Review neutral ground',(.20,.22,.21));bpy.ops.mesh.primitive_plane_add(size=200,location=(0,0,-.003));floor=bpy.context.object;floor.name='REVIEW floor';floor.data.materials.append(floor_mat)
for c in list(floor.users_collection):c.objects.unlink(floor)
studio.objects.link(floor)
for name,loc,power,size in [('Key',(-3,4,5),450,4),('Fill',(4,2,3),210,4),('Rim',(0,-4,4),210,4)]:
 data=bpy.data.lights.new(name,'AREA');data.energy=power;data.shape='DISK';data.size=size;o=bpy.data.objects.new(name,data);studio.objects.link(o);o.location=loc;o.rotation_euler=(Vector((0,0,.5))-o.location).to_track_quat('-Z','Y').to_euler()
data=bpy.data.cameras.new('Orthographic actual model review');cam=bpy.data.objects.new('Orthographic actual model review',data);studio.objects.link(cam);s.camera=cam;data.type='ORTHO'
def camera(loc,target,scale):cam.location=loc;cam.rotation_euler=(Vector(target)-cam.location).to_track_quat('-Z','Y').to_euler();data.ortho_scale=scale
def state(i,e,p):root['state']=i;root['energy']=e;root['has_battery']=p;root.update_tag();bpy.context.view_layer.update()
def render(name):s.render.filepath=str(R/'review'/name);bpy.ops.render.render(write_still=True)
camera((1.8,3.2,2.1),(0,0,.49),1.62);state(2,500,1);bpy.ops.wm.save_as_mainfile(filepath=str(R/'source/charger-v01.blend'))
for i,e,p in [(0,0,0),(1,500,1),(2,500,1),(3,1000,1),(4,500,1)]:state(i,e,p);render('charger-state-'+str(i)+'.png')
state(2,500,1);camera((0,3,.5),(0,0,.5),1.25);render('charger-front.png');camera((1.8,-3,2.0),(0,0,.49),1.65);render('charger-back.png')
state(0,0,0);camera((0,3,.5),(0,0,.5),1.25);render('charger-empty-front.png')
state(2,500,1);camera((1.8,3.2,2.1),(0,.08,.49),1.9)
for o in ref.objects:o.location.y+=.34
render('charger-battery-fit-exploded.png')
print('Saved clean charger source with separate adopted battery reference; rendered 5 states, front/back, empty contacts and fit view.')
