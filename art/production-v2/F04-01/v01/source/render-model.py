"""Render actual cuboid geometry/UV; keep mounting/charger fixtures separate."""
import bpy,bmesh,json,math
from pathlib import Path
from mathutils import Vector
R=Path(__file__).resolve().parents[1];G=json.loads((R/'source/geometry-and-uv.json').read_text());UV=json.loads((R/'source/uv-regions.json').read_text())['regions']
bpy.ops.object.select_all(action='SELECT');bpy.ops.object.delete(use_global=False);bpy.context.preferences.filepaths.save_version=0
s=bpy.context.scene;s.render.engine='CYCLES';s.cycles.samples=24;s.render.resolution_x=900;s.render.resolution_y=900;s.render.resolution_percentage=100;s.render.image_settings.file_format='PNG';s.view_settings.view_transform='Standard';s.world.color=(.22,.23,.23)
prod=bpy.data.collections.new('PRODUCTION battery — fixed shell and charge windows');s.collection.children.link(prod);studio=bpy.data.collections.new('REVIEW lighting and floor');s.collection.children.link(studio)
root=bpy.data.objects.new('Battery — energy 0..1000',None);prod.objects.link(root);root['energy']=500;root['capacity']=1000;root['source']='actual BatteryData; review property only'
mat=bpy.data.materials.new('32px native steel/copper/redstone atlas');mat.use_nodes=True;bs=next(n for n in mat.node_tree.nodes if n.type=='BSDF_PRINCIPLED');bs.inputs['Roughness'].default_value=.88;bs.inputs['Metallic'].default_value=.05
tex=mat.node_tree.nodes.new('ShaderNodeTexImage');tex.image=bpy.data.images.load(str(R/'exports/battery-atlas-32.png'));tex.interpolation='Closest';tex.image.pack();mat.node_tree.links.new(tex.outputs['Color'],bs.inputs['Base Color'])
def V(v):x,y,z=v;return ((x-8)/16,(z-8.16)/16,(y-4.32)/16)
fm={'north':[0,1,2,3],'south':[5,4,7,6],'east':[1,5,6,2],'west':[4,0,3,7],'up':[3,2,6,7],'down':[4,5,1,0]}
for e in G['parts']:
 x0,y0,z0=e['from'];x1,y1,z1=e['to'];vs=[[x0,y0,z0],[x1,y0,z0],[x1,y1,z0],[x0,y1,z0],[x0,y0,z1],[x1,y0,z1],[x1,y1,z1],[x0,y1,z1]]
 me=bpy.data.meshes.new(e['name']);me.from_pydata([V(v) for v in vs],[],list(fm.values()));me.update();o=bpy.data.objects.new(e['name'],me);prod.objects.link(o);o.parent=root;o.data.materials.append(mat);o['model_part']=e['name'];ul=me.uv_layers.new(name='Explicit atlas32 UV')
 for po,face in zip(me.polygons,fm):
  u0,v0,u1,v1=[v/16 for v in e['faces'][face]['uv']];coords=[(u0,1-v1),(u1,1-v1),(u1,1-v0),(u0,1-v0)]
  for loop,co in zip(po.loop_indices,coords):ul.data[loop].uv=co
 bm=bmesh.new();bm.from_mesh(me);bmesh.ops.recalc_face_normals(bm,faces=bm.faces);bm.to_mesh(me);bm.free()
# Charge surfaces are dynamic planes, not new shell geometry or constant glow.
fill=[]
for window in G['windows']:
 i=window['indexBottomUp'];a=V(window['from']);b=V(window['to']);w=b[0]-a[0];h=b[2]-a[2]
 me=bpy.data.meshes.new('dynamic-window-'+str(i));me.from_pydata([(0,0,0),(w,0,0),(w,0,h),(0,0,h)],[],[(0,3,2,1)]);me.update();o=bpy.data.objects.new('Dynamic redstone window bottom-up '+str(i),me);prod.objects.link(o);o.parent=root;o.location=a;o.data.materials.append(mat);o['window_index']=i;ul=me.uv_layers.new(name='Energy region')
 u0,v0,u1,v1=[v/16 for v in UV['energy']['uv']]
 for loop,co in zip(me.polygons[0].loop_indices,[(u0,1-v1),(u0,1-v0),(u1,1-v0),(u1,1-v1)]):ul.data[loop].uv=co
 curve=o.driver_add('scale',2);driver=curve.driver;var=driver.variables.new();var.name='charge';var.targets[0].id=root;var.targets[0].data_path='["energy"]';driver.expression=f'min(1,max(0,charge/1000*3-{i}))'
 fill.append(o)
def energy(n):root['energy']=n;root.update_tag();bpy.context.view_layer.update()
def solid(name,color):
 m=bpy.data.materials.new(name);m.use_nodes=True;bs=next(n for n in m.node_tree.nodes if n.type=='BSDF_PRINCIPLED');bs.inputs['Base Color'].default_value=(*color,1);bs.inputs['Roughness'].default_value=.95;return m
floor_mat=solid('Review neutral ground',(.20,.22,.21));bpy.ops.mesh.primitive_plane_add(size=200,location=(0,0,-.004));floor=bpy.context.object;floor.name='REVIEW floor';floor.data.materials.append(floor_mat)
for c in list(floor.users_collection):c.objects.unlink(floor)
studio.objects.link(floor)
for name,loc,power,size in [('Key',(-2,3,4),280,3),('Fill',(3,1,2),110,3),('Rim',(0,-3,3),130,3)]:
 d=bpy.data.lights.new(name,'AREA');d.energy=power;d.shape='DISK';d.size=size;o=bpy.data.objects.new(name,d);studio.objects.link(o);o.location=loc;o.rotation_euler=(Vector((0,0,.22))-o.location).to_track_quat('-Z','Y').to_euler()
d=bpy.data.cameras.new('Orthographic art review');camera=bpy.data.objects.new('Orthographic art review',d);studio.objects.link(camera);s.camera=camera;d.type='ORTHO'
def cam(loc,target,scale):camera.location=loc;camera.rotation_euler=(Vector(target)-camera.location).to_track_quat('-Z','Y').to_euler();d.ortho_scale=scale
cam((1.1,2.0,1.15),(0,0,.23),.69);energy(500);bpy.ops.wm.save_as_mainfile(filepath=str(R/'source/battery-v01.blend'))
for n,key in [(0,'empty'),(500,'half'),(1000,'full')]:
 energy(n);s.render.filepath=str(R/f'review/battery-{key}.png');bpy.ops.render.render(write_still=True)
energy(500);cam((0,2,.23),(0,0,.23),.61);s.render.filepath=str(R/'review/battery-front.png');bpy.ops.render.render(write_still=True)
cam((.8,-2,.95),(0,0,.23),.69);s.render.filepath=str(R/'review/battery-back.png');bpy.ops.render.render(write_still=True)
# Greybox is an existing node/charger size fixture, not a new finished machine or charger asset.
fixture=bpy.data.collections.new('REVIEW ONLY fit fixtures');s.collection.children.link(fixture);m=solid('Review unfinished device grey',(.17,.20,.22));copper=solid('Review contact collar',(.34,.23,.12))
def box(name,loc,scale,material):
 bpy.ops.mesh.primitive_cube_add(size=1,location=loc);o=bpy.context.object;o.name=name;o.scale=scale;o.data.materials.append(material)
 for c in list(o.users_collection):c.objects.unlink(o)
 fixture.objects.link(o);return o
# Rear-side node: mounting axis +Z of Minecraft becomes +Y of Blender.
root.location=(0,.800625,.095);box('REVIEW mechanical body 1.4x1.4x.65',(0,0,.325),(1.4,1.4,.65),m);box('REVIEW collar .26 square',(0,.7,.325),(.26,.08,.26),copper);floor.hide_render=True
cam((1.8,2.8,1.85),(0,0,.34),2.05);s.render.resolution_x=1100;s.render.resolution_y=900;s.render.filepath=str(R/'review/battery-node-fit-simulation.png');bpy.ops.render.render(write_still=True)
for o in fixture.objects:o.hide_render=True
# Same-sized battery in a plain socket/pedestal preview, not the F04-03 charger design.
root.location=(0,0,.08);box('REVIEW socket base',(0,0,.04),(.66,.36,.08),m);box('REVIEW left socket rail',(-.24,0,.15),(.06,.20,.22),copper);box('REVIEW right socket rail',(.24,0,.15),(.06,.20,.22),copper);floor.hide_render=False
cam((1.1,2,1.15),(0,0,.29),.91);s.render.resolution_x=900;s.render.resolution_y=900;s.render.filepath=str(R/'review/battery-socket-fit-simulation.png');bpy.ops.render.render(write_still=True)
print('Saved clean editable 16-cuboid/3-window battery source; rendered three charge states, back/front and fit fixtures.')
