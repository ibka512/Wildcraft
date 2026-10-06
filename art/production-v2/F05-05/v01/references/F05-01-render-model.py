"""Render actual meshes with packed native atlas and separate unchanged battery reference."""
import bpy,bmesh,json,math
from pathlib import Path
from mathutils import Vector
R=Path(__file__).resolve().parents[1];G=json.loads((R/'source/geometry-and-uv.json').read_text())
bpy.ops.object.select_all(action='SELECT');bpy.ops.object.delete(use_global=False);bpy.context.preferences.filepaths.save_version=0
s=bpy.context.scene;s.render.engine='CYCLES';s.cycles.samples=16;s.render.resolution_x=s.render.resolution_y=800;s.render.resolution_percentage=100;s.render.image_settings.file_format='PNG';s.view_settings.view_transform='Standard';s.world.color=(.22,.23,.23)
prod=bpy.data.collections.new('PRODUCTION body and six nodes');s.collection.children.link(prod);ref=bpy.data.collections.new('REFERENCE adopted battery scale 1.00');s.collection.children.link(ref);studio=bpy.data.collections.new('REVIEW studio');s.collection.children.link(studio)
root=bpy.data.objects.new('Machine chassis',None);prod.objects.link(root);root['capacity']=1000;root['collision_width']=1.4;root['collision_height']=.65;root['art_only']=True
br=bpy.data.objects.new('Battery review mount',None);ref.objects.link(br);br['energy']=500;br['node']=3
mat=bpy.data.materials.new('64px native shared machine steel green copper');mat.use_nodes=True;bs=next(n for n in mat.node_tree.nodes if n.type=='BSDF_PRINCIPLED');bs.inputs['Roughness'].default_value=.9;bs.inputs['Metallic'].default_value=.04;tex=mat.node_tree.nodes.new('ShaderNodeTexImage');tex.image=bpy.data.images.load(str(R/'exports/machine-atlas-64.png'));tex.image.pack();tex.interpolation='Closest';mat.node_tree.links.new(tex.outputs['Color'],bs.inputs['Base Color'])
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
for p in G['batteryParts']:o=box(p,ref,br,False);o['reference_asset']='F04-01 adopted, unscaled'
for w in G['batteryWindows']:
 x0,y0,z=w['from'];x1,y1,_=w['to'];me=bpy.data.meshes.new('Window '+str(w['indexBottomUp']));me.from_pydata([V(v) for v in [[x0,0,z],[x0,y1-y0,z],[x1,y1-y0,z],[x1,0,z]]],[],[(0,1,2,3)]);me.update();o=bpy.data.objects.new('Battery dynamic window '+str(w['indexBottomUp']),me);ref.objects.link(o);o.parent=br;o.location.z=y0/16;o.data.materials.append(mat);uv=me.uv_layers.new(name='Battery energy region');u0,v0,u1,v1=[v/16 for v in G['batteryEnergyUV']]
 for loop,co in zip(me.polygons[0].loop_indices,[(u1,1-v1),(u1,1-v0),(u0,1-v0),(u0,1-v1)]):uv.data[loop].uv=co
 d=o.driver_add('scale',2).driver;v=d.variables.new();v.name='charge';v.targets[0].id=br;v.targets[0].data_path='["energy"]';d.expression=f'min(1,max(0,charge/1000*3-{w["indexBottomUp"]}))';o['window_index']=w['indexBottomUp']
def set_mount(n,present):
 br['node']=n;br.location=V(G['nodes'][n]['point']);br.rotation_euler=[(math.pi/2,0,0),(-math.pi/2,0,0),(0,0,math.pi),(0,0,0),(0,0,math.pi/2),(0,0,-math.pi/2)][n]
 for o in ref.objects:
  if o!=br:o.hide_render=o.hide_viewport=not present
 bpy.context.view_layer.update()
# Default empty source; adopted battery remains a separately named hidden reference.
set_mount(3,False)
fmat=bpy.data.materials.new('Review ground');fmat.diffuse_color=(.20,.22,.21,1);bpy.ops.mesh.primitive_plane_add(size=200,location=(0,0,-.05));floor=bpy.context.object;floor.name='REVIEW ground';floor.data.materials.append(fmat)
for c in list(floor.users_collection):c.objects.unlink(floor)
studio.objects.link(floor)
for name,loc,power,size in [('Key',(-3,4,5),430,4),('Fill',(4,2,3),180,4),('Rim',(0,-4,4),210,4),('Underside review fill',(0,1,-3),150,4)]:
 data=bpy.data.lights.new(name,'AREA');data.energy=power;data.shape='DISK';data.size=size;o=bpy.data.objects.new(name,data);studio.objects.link(o);o.location=loc;o.rotation_euler=(Vector((0,0,.4))-o.location).to_track_quat('-Z','Y').to_euler()
data=bpy.data.cameras.new('Orthographic actual model review');cam=bpy.data.objects.new('Orthographic actual model review',data);studio.objects.link(cam);s.camera=cam;data.type='ORTHO'
def camera(loc,target,scale):cam.location=loc;cam.rotation_euler=(Vector(target)-cam.location).to_track_quat('-Z','Y').to_euler();data.ortho_scale=scale
camera((2.2,3.4,2.7),(0,0,.32),2.25);bpy.ops.wm.save_as_mainfile(filepath=str(R/'source/machine-body-v01.blend'))
def render(name):s.render.filepath=str(R/'review'/name);bpy.ops.render.render(write_still=True)
render('machine-front-three-quarter.png');camera((2.2,-3.4,2.7),(0,0,.32),2.25);render('machine-back-three-quarter.png');camera((0,3,.33),(0,0,.33),1.8);render('machine-front.png');camera((0,0,4),(0,0,0),1.8);render('machine-top.png')
floor.hide_render=True;camera((2.2,3.4,-2.5),(0,0,.28),2.25);render('machine-underside.png');floor.hide_render=False
set_mount(3,True);camera((2.2,3.4,2.7),(0,.12,.32),2.45);render('machine-battery-front.png');set_mount(0,True);camera((2.2,3.4,2.7),(0,0,.42),2.45);render('machine-battery-top.png')
set_mount(3,False);print('Saved actual mesh/UV Blender source; rendered 5 chassis views and 2 adopted-battery fit views.')
