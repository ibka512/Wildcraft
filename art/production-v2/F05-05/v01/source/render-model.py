"""Render actual meshes with packed native atlas and separate unchanged battery reference."""
import bpy,bmesh,json,math
from pathlib import Path
from mathutils import Vector
R=Path(__file__).resolve().parents[1];G=json.loads((R/'source/geometry-and-uv.json').read_text())
bpy.ops.object.select_all(action='SELECT');bpy.ops.object.delete(use_global=False);bpy.context.preferences.filepaths.save_version=0
s=bpy.context.scene;s.render.engine='CYCLES';s.cycles.samples=8;s.render.resolution_x=s.render.resolution_y=640;s.render.resolution_percentage=100;s.render.image_settings.file_format='PNG';s.view_settings.view_transform='Standard';s.world.color=(.22,.23,.23)
prod=bpy.data.collections.new('PRODUCTION body and six nodes');s.collection.children.link(prod);ref=bpy.data.collections.new('ADAPTED adopted battery scale 1.00');s.collection.children.link(ref);studio=bpy.data.collections.new('REVIEW studio');s.collection.children.link(studio)
root=bpy.data.objects.new('Machine chassis',None);prod.objects.link(root);root['capacity']=1000;root['collision_width']=1.4;root['collision_height']=.65;root['art_only']=True
br=bpy.data.objects.new('Battery review mount',None);ref.objects.link(br);br.parent=root;br['energy']=500;br['node']=3
mat=bpy.data.materials.new('64px native shared machine steel green copper');mat.use_nodes=True;bs=next(n for n in mat.node_tree.nodes if n.type=='BSDF_PRINCIPLED');bs.inputs['Roughness'].default_value=.9;bs.inputs['Metallic'].default_value=.04;tex=mat.node_tree.nodes.new('ShaderNodeTexImage');tex.image=bpy.data.images.load(str(R/'references/F05-01-machine-atlas-64.png'));tex.image.pack();tex.interpolation='Closest';mat.node_tree.links.new(tex.outputs['Color'],bs.inputs['Base Color'])
FM={'north':[0,1,2,3],'south':[5,4,7,6],'east':[1,5,6,2],'west':[4,0,3,7],'up':[3,2,6,7],'down':[4,5,1,0]}
def V(v):x,y,z=v;return (x/16,z/16,y/16)
def rot(v,n):
 x,y,z=v;return [(x,z,-y),(x,-z,y),(-x,y,-z),(x,y,z),(-z,y,x),(z,y,-x)][n]
def world(v,p):
 if 'node' not in p:return v
 return [a+b for a,b in zip(rot(v,p['node']),G['nodes'][p['node']]['point'])]
def box(p,col,parent,transform=True):
 x0,y0,z0=p['from'];x1,y1,z1=p['to'];vv=[[x0,y0,z0],[x1,y0,z0],[x1,y1,z0],[x0,y1,z0],[x0,y0,z1],[x1,y0,z1],[x1,y1,z1],[x0,y1,z1]];me=bpy.data.meshes.new(p['name']);me.from_pydata([V(world(v,p) if transform else v) for v in vv],[],list(FM.values()));me.update();o=bpy.data.objects.new(p['name'],me);col.objects.link(o);o.parent=parent;o.data.materials.append(mat);o['model_part']=p['name'];uv=me.uv_layers.new(name='Explicit 64px nearest UV')
 for po,f in zip(me.polygons,FM):
  u0,v0,u1,v1=[v/16 for v in p['faces'][f]['uv']]
  for loop,co in zip(po.loop_indices,[(u0,1-v1),(u1,1-v1),(u1,1-v0),(u0,1-v0)]):uv.data[loop].uv=co
 bm=bmesh.new();bm.from_mesh(me);bmesh.ops.recalc_face_normals(bm,faces=bm.faces);bm.to_mesh(me);bm.free();return o
for p in G['parts']:box(p,prod,root)
for p in G['batteryParts']:o=box(p,ref,br,False);o['reference_asset']='F04-01 adopted, unscaled';o['adaptation_only']=True

for w in G['batteryWindows']:
 i=w['indexBottomUp'];x0,y0,z=w['from'];x1,y1,_=w['to'];me=bpy.data.meshes.new('Window '+str(i));me.from_pydata([V(v) for v in [[x0,0,z],[x0,y1-y0,z],[x1,y1-y0,z],[x1,0,z]]],[],[(0,1,2,3)]);me.update();o=bpy.data.objects.new('Battery dynamic window '+str(i),me);ref.objects.link(o);o.parent=br;o.location.z=y0/16
 wm=mat.copy();wm.name='Dynamic window '+str(i)+' geometry AND UV crop';nt=wm.node_tree;texw=next(n for n in nt.nodes if n.type=='TEX_IMAGE');attr=nt.nodes.new('ShaderNodeTexCoord');sep=nt.nodes.new('ShaderNodeSeparateXYZ');combine=nt.nodes.new('ShaderNodeCombineXYZ');nt.links.new(attr.outputs['UV'],sep.inputs[0]);nt.links.new(sep.outputs['X'],combine.inputs['X']);u0,v0,u1,v1=[v/16 for v in G['batteryEnergyUV']];bottom=1-v1
 sub=nt.nodes.new('ShaderNodeMath');sub.operation='SUBTRACT';sub.inputs[1].default_value=bottom;nt.links.new(sep.outputs['Y'],sub.inputs[0]);mul=nt.nodes.new('ShaderNodeMath');mul.operation='MULTIPLY';nt.links.new(sub.outputs[0],mul.inputs[0]);add=nt.nodes.new('ShaderNodeMath');add.operation='ADD';add.inputs[1].default_value=bottom;nt.links.new(mul.outputs[0],add.inputs[0]);nt.links.new(add.outputs[0],combine.inputs['Y']);nt.links.new(combine.outputs[0],texw.inputs['Vector']);o.data.materials.append(wm)
 uv=me.uv_layers.new(name='Original energy region cropped continuously')
 for loop,co in zip(me.polygons[0].loop_indices,[(u1,1-v1),(u1,1-v0),(u0,1-v0),(u0,1-v1)]):uv.data[loop].uv=co
 for d in [o.driver_add('scale',2).driver,mul.inputs[1].driver_add('default_value').driver]:
  v=d.variables.new();v.name='charge';v.targets[0].id=br;v.targets[0].data_path='["energy"]';d.expression=f'min(1,max(0,charge/1000*3-{i}))'
 o['window_index']=i
br['exploded_distance_model_units']=0.0
for axis in range(3):
 for prop in ['location','rotation_euler']:
  d=br.driver_add(prop,axis).driver;v=d.variables.new();v.name='node';v.targets[0].id=br;v.targets[0].data_path='["node"]'
  if prop=='location':
   v=d.variables.new();v.name='gap';v.targets[0].id=br;v.targets[0].data_path='["exploded_distance_model_units"]'
   coord=[n['point'][[0,2,1][axis]]/16 for n in G['nodes']];out=[n['normal'][[0,2,1][axis]]/16 for n in G['nodes']];d.expression=f'{tuple(coord)}[int(node)]+gap*{tuple(out)}[int(node)]'
  else:
   coords=[(math.pi/2,0,0),(-math.pi/2,0,0),(0,0,math.pi),(0,0,0),(0,0,math.pi/2),(0,0,-math.pi/2)];d.expression=f'{tuple(v[axis] for v in coords)}[int(node)]'
def set_mount(n,present=True,gap=0,e=500):
 br['node']=n;br['energy']=e;br['exploded_distance_model_units']=gap;br.update_tag()
 for o in ref.objects:
  if o!=br:o.hide_render=o.hide_viewport=not present
 bpy.context.view_layer.update();s.frame_set(s.frame_current)
studio_ground=bpy.data.materials.new('Review ground');studio_ground.diffuse_color=(.20,.22,.21,1);bpy.ops.mesh.primitive_plane_add(size=200,location=(0,0,-.22));floor=bpy.context.object;floor.name='REVIEW ground below lowest battery; not collision';floor.data.materials.append(studio_ground)
for c in list(floor.users_collection):c.objects.unlink(floor)
studio.objects.link(floor)
for name,loc,power,size in [('Key',(-3,4,5),430,4),('Fill',(4,2,3),220,4),('Rim',(0,-4,4),260,4),('Underside fill',(0,1,-3),180,4)]:
 data=bpy.data.lights.new(name,'AREA');data.energy=power;data.shape='DISK';data.size=size;o=bpy.data.objects.new(name,data);studio.objects.link(o);o.location=loc;o.rotation_euler=(Vector((0,0,.4))-o.location).to_track_quat('-Z','Y').to_euler()
data=bpy.data.cameras.new('Actual assembly review');cam=bpy.data.objects.new('Actual assembly review',data);studio.objects.link(cam);s.camera=cam;data.type='ORTHO'
def camera(loc,target=(0,0,.32),scale=2.4):cam.location=loc;cam.rotation_euler=(Vector(target)-cam.location).to_track_quat('-Z','Y').to_euler();data.ortho_scale=scale
def render(name):s.render.filepath=str(R/'review'/name);bpy.ops.render.render(write_still=True)
set_mount(3);camera((2.2,3.4,2.7));bpy.ops.wm.save_as_mainfile(filepath=str(R/'source/battery-six-node-v01.blend'))
views=[((2.2,3.4,3.6),(0,0,.4)),((2.2,3.4,-2.8),(0,0,.1)),((2.2,-3.4,2.7),(0,-.1,.32)),((2.2,3.4,2.7),(0,.1,.32)),((-3.4,2.2,2.7),(-.1,0,.32)),((3.4,2.2,2.7),(.1,0,.32))]
for n,(loc,target) in enumerate(views):
 set_mount(n);camera(loc,target);floor.hide_render=n==1;render('mount-'+str(n)+'.png')
floor.hide_render=False
set_mount(3,False);camera((2.2,3.4,2.7));render('empty.png')
set_mount(3,True,6);camera((2.2,3.4,2.7),(0,.15,.38),2.55);render('exploded.png')
for e in [0,99,500,1000]:
 set_mount(3,True,0,e);camera((.4,4,1.6),(0,.2,.34),2.1);render('energy-'+str(e)+'.png')
set_mount(3);camera((2.2,3.4,2.7));bpy.ops.wm.save_as_mainfile(filepath=str(R/'source/battery-six-node-v01.blend'))
print('Saved editable one-battery assembly, automatic node / energy / exploded-distance drivers; 12 actual renders.')
