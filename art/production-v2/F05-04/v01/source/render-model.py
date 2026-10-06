"""Actual fixed wing meshes and unified UV, separate hidden adopted chassis reference."""
import bpy,bmesh,json,math
from pathlib import Path
from mathutils import Vector
R=Path(__file__).resolve().parents[1];G=json.loads((R/'source/geometry-and-uv.json').read_text());C=json.loads((R/'references/approved-machine-geometry.json').read_text())
bpy.ops.object.select_all(action='SELECT');bpy.ops.object.delete(use_global=False);bpy.context.preferences.filepaths.save_version=0;s=bpy.context.scene;s.render.engine='CYCLES';s.cycles.samples=16;s.render.resolution_x=s.render.resolution_y=800;s.render.resolution_percentage=100;s.render.image_settings.file_format='PNG';s.view_settings.view_transform='Standard';s.world.color=(.22,.23,.23)
prod=bpy.data.collections.new('PRODUCTION rocket variants');s.collection.children.link(prod);ref=bpy.data.collections.new('REFERENCE adopted F05-01 chassis');s.collection.children.link(ref);studio=bpy.data.collections.new('REVIEW studio');s.collection.children.link(studio)
root=bpy.data.objects.new('Rocket node origin',None);prod.objects.link(root);root['fuelDurationWorldTicks']=80;root['energyCost']=0;root['art_only']=True
mat=bpy.data.materials.new('Rocket unified 128x64 atlas');mat.use_nodes=True;bs=next(n for n in mat.node_tree.nodes if n.type=='BSDF_PRINCIPLED');bs.inputs['Roughness'].default_value=.9;bs.inputs['Metallic'].default_value=.04;tex=mat.node_tree.nodes.new('ShaderNodeTexImage');tex.image=bpy.data.images.load(str(R/'exports/rocket-atlas-128x64.png'));tex.image.pack();tex.interpolation='Closest';mat.node_tree.links.new(tex.outputs['Color'],bs.inputs['Base Color'])
def V(v):x,y,z=v;return (x/16,z/16,y/16)
def mesh(p,col,parent,offset=0):
 vv=[[x,y,z-offset]for x,y,z in p['vertices']];me=bpy.data.meshes.new(p['name']);me.from_pydata([V(v)for v in vv],[],[f['indices']for f in p['faces']]);me.update();o=bpy.data.objects.new(p['name'],me);col.objects.link(o);o.parent=parent;o.data.materials.append(mat);o['part_name']=p['name'];o['group']=p.get('group','reference');uv=me.uv_layers.new(name='Explicit unified128x64 UV')
 for po,f in zip(me.polygons,p['faces']):
  for loop,(u,v)in zip(po.loop_indices,f['uvs']):uv.data[loop].uv=(u/16,1-v/16)
 bm=bmesh.new();bm.from_mesh(me);bmesh.ops.recalc_face_normals(bm,faces=bm.faces);bm.to_mesh(me);bm.free();return o
for state,geo in G['variants'].items():
 for part in geo['parts']:
  p=dict(part);p['name']=state+'/'+part['name'];o=mesh(p,prod,root);o['state']=state;o['source_part']=part['name']
# Adopted body is a hidden named review reference, not baked into fan model.
FM={'north':[0,1,2,3],'south':[5,4,7,6],'east':[1,5,6,2],'west':[4,0,3,7],'up':[3,2,6,7],'down':[4,5,1,0]}
def rot(v,n):
 x,y,z=v;return [(x,z,-y),(x,-z,y),(-x,y,-z),(x,y,z),(-z,y,x),(z,y,-x)][n]
for p in C['parts']:
 x0,y0,z0=p['from'];x1,y1,z1=p['to'];vs=[[x0,y0,z0],[x1,y0,z0],[x1,y1,z0],[x0,y1,z0],[x0,y0,z1],[x1,y0,z1],[x1,y1,z1],[x0,y1,z1]]
 if 'node'in p:vs=[[a+b for a,b in zip(rot(v,p['node']),C['nodes'][p['node']]['point'])]for v in vs]
 faces=[]
 for f,ids in FM.items():u0,v0,u1,v1=p['faces'][f]['uv'];faces.append(dict(indices=ids,uvs=[[u0/2,v1],[u1/2,v1],[u1/2,v0],[u0/2,v0]]))
 o=mesh(dict(name='reference/'+p['name'],vertices=vs,faces=faces),ref,None);o.hide_render=o.hide_viewport=True;o['approved_reference']=True
fmat=bpy.data.materials.new('Review neutral ground');fmat.use_nodes=True;b=next(n for n in fmat.node_tree.nodes if n.type=='BSDF_PRINCIPLED');b.inputs['Base Color'].default_value=(.20,.22,.21,1);b.inputs['Roughness'].default_value=.95;bpy.ops.mesh.primitive_plane_add(size=200,location=(0,0,-.295));floor=bpy.context.object;floor.name='REVIEW ground';floor.data.materials.append(fmat)
for c in list(floor.users_collection):c.objects.unlink(floor)
studio.objects.link(floor)
for name,loc,power,size in [('Key',(-3,4,5),430,4),('Fill',(4,2,3),180,4),('Rim',(0,-4,4),210,4),('Bottom review fill',(0,1,-3),100,4)]:
 data=bpy.data.lights.new(name,'AREA');data.energy=power;data.shape='DISK';data.size=size;o=bpy.data.objects.new(name,data);studio.objects.link(o);o.location=loc;o.rotation_euler=(Vector((0,0,.1))-o.location).to_track_quat('-Z','Y').to_euler()
data=bpy.data.cameras.new('Orthographic actual rocket');cam=bpy.data.objects.new('Orthographic actual rocket',data);studio.objects.link(cam);s.camera=cam;data.type='ORTHO'
def camera(loc,target,scale):cam.location=loc;cam.rotation_euler=(Vector(target)-cam.location).to_track_quat('-Z','Y').to_euler();data.ortho_scale=scale
def render(name):s.render.filepath=str(R/'review'/name);bpy.ops.render.render(write_still=True)


# Review only flame uses current native box footprint and colour; no moving mesh animation.
fx=bpy.data.collections.new('REVIEW native tail flame');s.collection.children.link(fx)
flame=bpy.data.objects.new('REVIEW existing flame box',bpy.data.meshes.new('REVIEW flame box mesh'));fx.objects.link(flame);flame.parent=root
x0,y0,z0=G['flameBounds']['min'];x1,y1,z1=G['flameBounds']['max'];vv=[[x0,y0,z0],[x1,y0,z0],[x1,y1,z0],[x0,y1,z0],[x0,y0,z1],[x1,y0,z1],[x1,y1,z1],[x0,y1,z1]];flame.data.from_pydata([V(v)for v in vv],[],[[0,1,2,3],[5,4,7,6],[1,5,6,2],[4,0,3,7],[3,2,6,7],[4,5,1,0]]);flame.data.update()
fmat=bpy.data.materials.new('REVIEW native #DDFFD58B flame');fmat.use_nodes=True;nt=fmat.node_tree;nt.nodes.clear();out=nt.nodes.new('ShaderNodeOutputMaterial');mix=nt.nodes.new('ShaderNodeMixShader');mix.inputs[0].default_value=221/255;t=nt.nodes.new('ShaderNodeBsdfTransparent');e=nt.nodes.new('ShaderNodeEmission');e.inputs['Color'].default_value=(1,.835294,.545098,1);e.inputs['Strength'].default_value=1;nt.links.new(t.outputs[0],mix.inputs[1]);nt.links.new(e.outputs[0],mix.inputs[2]);nt.links.new(mix.outputs[0],out.inputs['Surface']);flame.data.materials.append(fmat)
def state(which='loaded',burning=False):
 root['selected_state']=which
 for o in prod.objects:
  if o.type=='MESH':o.hide_render=o.hide_viewport=o['state']!=which
 flame.hide_render=flame.hide_viewport=not burning
state();floor.location.z=-.16
camera((1.7,3.2,1.6),(0,.25,0),.78);bpy.ops.wm.save_as_mainfile(filepath=str(R/'source/rocket-and-spent-v01.blend'));render('rocket-three-quarter.png');state('spent');render('spent-rocket-three-quarter.png')
for which,prefix in [('loaded','rocket'),('spent','spent-rocket')]:
 state(which);camera((0,3,.02),(0,.28,0),.46);render(prefix+'-nozzle.png');camera((3,.25,0),(0,.25,0),.69);render(prefix+'-side.png')
state('loaded');camera((1.5,-3,1.5),(0,.23,0),.8);render('rocket-root.png')
state('loaded',True);camera((1.7,3.2,1.6),(0,.40,0),1.08);render('rocket-burning-native-flame.png')
state();floor.location.z=-.9
for o in ref.objects:o.hide_render=o.hide_viewport=False
for n in range(6):
 root.location=V(C['nodes'][n]['point']);root.rotation_euler=[(math.pi/2,0,0),(-math.pi/2,0,0),(0,0,math.pi),(0,0,0),(0,0,math.pi/2),(0,0,-math.pi/2)][n];floor.hide_render=n==1;camera([(2.8,3.7,3.4),(2.8,3.7,-2.2),(2.8,-3.7,3.4),(2.8,3.7,3.4),(-3.2,3.7,3.4),(2.8,3.7,3.4)][n],(0,0,.27),2.4)
 for which,prefix in [('loaded','rocket'),('spent','spent-rocket')]:state(which);render(prefix+'-mounted-'+str(n)+'.png')
print('Saved two fixed variants with hidden chassis and native flame reference; 20 actual render views.')
