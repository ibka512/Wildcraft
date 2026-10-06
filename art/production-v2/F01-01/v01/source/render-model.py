import bpy,bmesh,json,math
from pathlib import Path
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[1]
geo=json.loads((ROOT/'source/geometry-and-uv.json').read_text())
bpy.ops.object.select_all(action='SELECT');bpy.ops.object.delete(use_global=False)
bpy.context.preferences.filepaths.save_version=0
scene=bpy.context.scene
scene.render.engine='CYCLES';scene.cycles.samples=32
scene.render.resolution_x=1600;scene.render.resolution_y=920;scene.render.resolution_percentage=100
scene.world.color=(.28,.28,.28)
scene.view_settings.view_transform='Standard';scene.view_settings.look='Medium High Contrast';scene.view_settings.exposure=0;scene.view_settings.gamma=1
scene.render.image_settings.file_format='PNG'
mat=bpy.data.materials.new('Cast iron / rim / wood / copper — actual pixel UV');mat.use_nodes=True
nodes=mat.node_tree.nodes;bsdf=next(n for n in nodes if n.type=='BSDF_PRINCIPLED');bsdf.inputs['Roughness'].default_value=.9;bsdf.inputs['Metallic'].default_value=.12
tex=nodes.new('ShaderNodeTexImage');tex.image=bpy.data.images.load(str(ROOT/'exports/cooking-pot-atlas-64.png'));tex.interpolation='Closest'
mat.node_tree.links.new(tex.outputs['Color'],bsdf.inputs['Base Color'])
collection=bpy.data.collections.new('PRODUCTION — exported block geometry');scene.collection.children.link(collection)
review=bpy.data.collections.new('REVIEW ONLY — stew and steam, not runtime');scene.collection.children.link(review)
rig=bpy.data.objects.new('Pot coordinate root: MC 16 units = 1 block',None);collection.objects.link(rig)
def rotate(v,e):
    if 'rotation' not in e:return v
    ox,oy,oz=e['rotation']['origin'];a=math.radians(e['rotation']['angle']);x,y,z=v
    dx,dz=x-ox,z-oz
    return [ox+math.cos(a)*dx+math.sin(a)*dz,y,oz-math.sin(a)*dx+math.cos(a)*dz]
face_map={'north':[0,1,2,3],'south':[5,4,7,6],'east':[1,5,6,2],'west':[4,0,3,7],'up':[3,2,6,7],'down':[4,5,1,0]}
for e in geo['parts']:
    a=e['from'];b=e['to'];x0,y0,z0=a;x1,y1,z1=b
    verts=[[x0,y0,z0],[x1,y0,z0],[x1,y1,z0],[x0,y1,z0],[x0,y0,z1],[x1,y0,z1],[x1,y1,z1],[x0,y1,z1]]
    verts=[rotate(v,e) for v in verts];verts=[((x-8)/16,(z-8)/16,y/16) for x,y,z in verts]
    mesh=bpy.data.meshes.new(e['name']);mesh.from_pydata(verts,[],list(face_map.values()));mesh.update()
    obj=bpy.data.objects.new(e['name'],mesh);collection.objects.link(obj);obj.parent=rig;obj.data.materials.append(mat)
    obj['original_model_part']=e['name'];obj['model_units']='1/16 block'
    layer=mesh.uv_layers.new(name='Minecraft explicit UV / atlas64')
    for polygon,face in zip(mesh.polygons,face_map):
        u0,v0,u1,v1=e['faces'][face]['uv'];u0/=16;v0/=16;u1/=16;v1/=16
        coords=[(u0,1-v1),(u1,1-v1),(u1,1-v0),(u0,1-v0)]
        for loop,co in zip(polygon.loop_indices,coords):layer.data[loop].uv=co
    bm=bmesh.new();bm.from_mesh(mesh);bmesh.ops.recalc_face_normals(bm,faces=bm.faces);bm.to_mesh(mesh);bm.free()
# Studio scene is kept separate from the production root.
studio=bpy.data.collections.new('REVIEW ONLY — cameras and lighting');scene.collection.children.link(studio)
def solid(name,color):
    m=bpy.data.materials.new(name);m.diffuse_color=(*color,1);m.use_nodes=True;next(n for n in m.node_tree.nodes if n.type=='BSDF_PRINCIPLED').inputs['Base Color'].default_value=(*color,1);next(n for n in m.node_tree.nodes if n.type=='BSDF_PRINCIPLED').inputs['Roughness'].default_value=.92
    return m
floor_mat=solid('Studio neutral warm grey',(.22,.205,.185))
bpy.ops.mesh.primitive_plane_add(size=200,location=(0,0,-.005));floor=bpy.context.object;floor.name='Review studio floor';floor.data.materials.append(floor_mat)
for c in list(floor.users_collection):c.objects.unlink(floor)
studio.objects.link(floor)
def area(name,loc,energy,size):
    d=bpy.data.lights.new(name,'AREA');d.energy=energy;d.shape='DISK';d.size=size;o=bpy.data.objects.new(name,d);studio.objects.link(o);o.location=loc;o.rotation_euler=((Vector((0,0,.25))-o.location).to_track_quat('-Z','Y').to_euler())
area('upper-left broad key',(-3,-4,5),400,4)
area('soft fill',(3,-1,2.8),110,4)
area('rim',(0,3,4),180,3)
camdata=bpy.data.cameras.new('Orthographic art review');cam=bpy.data.objects.new('Orthographic art review',camdata);studio.objects.link(cam);scene.camera=cam
camdata.type='ORTHO'
def camera(loc,target,scale):
    cam.location=loc;cam.rotation_euler=(Vector(target)-cam.location).to_track_quat('-Z','Y').to_euler();camdata.ortho_scale=scale
# Single model: true geometry and UV, not generated image.
camera((1.65,-2.8,2.3),(0,0,.3),2.05)
scene.render.filepath=str(ROOT/'review/cooking-pot-idle.png');bpy.ops.render.render(write_still=True)
# Save a clean editable production scene before decorative cooking content exists.
tex.image.pack();bpy.ops.wm.save_as_mainfile(filepath=str(ROOT/'source/cooking-pot-v01.blend'))
# Side and overhead views for structural inspection.
scene.render.resolution_x=960;scene.render.resolution_y=820
camera((0,-4,1.15),(0,0,.3),1.35);scene.render.filepath=str(ROOT/'review/cooking-pot-front.png');bpy.ops.render.render(write_still=True)
camera((0,0,4),(0,0,0),1.25);scene.render.filepath=str(ROOT/'review/cooking-pot-top.png');bpy.ops.render.render(write_still=True)
# Paired review: same actual production model. Right stew is demonstration only.
parts=list(rig.children)
rig.location.x=-.72;rig.rotation_euler.z=math.radians(-25)
rig2=bpy.data.objects.new('Review copy of SAME pot',None);review.objects.link(rig2);rig2.location.x=.72;rig2.rotation_euler.z=math.radians(-25)
for ob in parts:
    duplicate=ob.copy();duplicate.data=ob.data.copy();review.objects.link(duplicate);duplicate.parent=rig2
materials={'stew':solid('Review stew only',(.43,.18,.036)),'carrot':solid('Review carrot only',(.85,.31,.038)),'potato':solid('Review potato only',(.79,.66,.30)),'green':solid('Review leaves only',(.23,.34,.095))}
def rbox(name,loc,scale,material,parent=rig2):
    bpy.ops.mesh.primitive_cube_add(size=1);o=bpy.context.object;o.name=name
    for c in list(o.users_collection):c.objects.unlink(o)
    review.objects.link(o);o.parent=parent;o.location=loc;o.scale=scale;o.data.materials.append(material);return o
mesh=bpy.data.meshes.new('Review octagonal stew surface')
r=.292;c=.210
mesh.from_pydata([(-c,-r,.445),(c,-r,.445),(r,-c,.445),(r,c,.445),(c,r,.445),(-c,r,.445),(-r,c,.445),(-r,-c,.445)],[],[tuple(range(8))]);mesh.update()
o=bpy.data.objects.new('REVIEW ONLY octagonal stew',mesh);review.objects.link(o);o.parent=rig2;o.data.materials.append(materials['stew'])
for i,(x,y,z,s,key) in enumerate([(-.13,-.1,.393,.08,'carrot'),(.12,.1,.393,.10,'potato'),(.07,-.13,.39,.07,'potato'),(-.1,.12,.39,.065,'carrot'),(-.14,.01,.391,.042,'green'),(.17,-.03,.388,.038,'green'),(.015,.07,.387,.04,'green')]):
    rbox('REVIEW ONLY vegetable '+str(i),(x,y,z+.075),(s,s,s*.38),materials[key])
steam=solid('Review steam parameter cue',(.53,.55,.52))
steam.use_nodes=True;next(n for n in steam.node_tree.nodes if n.type=='BSDF_PRINCIPLED').inputs['Alpha'].default_value=.25
steam.surface_render_method='DITHERED'
for i,(x,y) in enumerate([(-.09,-.05),(.12,.06)]):
    for k in range(3):rbox('REVIEW ONLY steam '+str(i)+'-'+str(k),(x+([0,.015,-.006][k]),y,.665+k*.055),(.025,.01,.052),steam)
scene.render.resolution_x=1600;scene.render.resolution_y=920
camera((0,-5,3.25),(0,0,.33),2.6)
scene.render.filepath=str(ROOT/'review/cooking-pot-paired-review.png');bpy.ops.render.render(write_still=True)
# This second source is a clearly separate demonstration scene.
bpy.ops.wm.save_as_mainfile(filepath=str(ROOT/'review/cooking-pot-demonstration.blend'))
# Heat-source stacking demonstration. This is a voxel fixture, not a game screenshot.
review.hide_render=True
rig.location.x=0;rig.rotation_euler.z=math.radians(-25)
firecollection=bpy.data.collections.new('REVIEW ONLY — heat source stacking fixture');scene.collection.children.link(firecollection)
logmat=solid('Review wooden heat-source fixture',(.20,.105,.045))
ember=solid('Review ember fixture',(.48,.11,.018))
bs=next(n for n in ember.node_tree.nodes if n.type=='BSDF_PRINCIPLED');bs.inputs['Emission Color'].default_value=(.9,.15,.016,1);bs.inputs['Emission Strength'].default_value=1.2
stone=solid('Review stone ground fixture',(.20,.22,.22))
def fixture(name,loc,scale,material,angle=0):
    bpy.ops.mesh.primitive_cube_add(size=1,location=loc);o=bpy.context.object;o.name=name;o.scale=scale;o.rotation_euler.z=math.radians(angle);o.data.materials.append(material)
    for c in list(o.users_collection):c.objects.unlink(o)
    firecollection.objects.link(o)
fixture('heat block bottom / demonstration',(0,0,-.65),(1.15,1.15,.12),stone)
for j in [-.20,.20]:
    fixture('heat source log',(0,j,-.50),(.8,.13,.14),logmat)
    fixture('heat source cross log',(j,0,-.38),(.65,.13,.12),logmat,90)
for x,y,h in [(-.14,-.05,.26),(.13,.1,.32),(.05,-.16,.34)]:fixture('heat cue only',(x,y,-.35+h/2),(.07,.08,h),ember,12)
floor.hide_render=True
scene.render.resolution_x=1200;scene.render.resolution_y=1100
camera((1.6,-2.8,1.8),(0,0,-.03),2.05)
scene.render.filepath=str(ROOT/'review/cooking-pot-heat-source-mockup.png');bpy.ops.render.render(write_still=True)
print('Rendered actual cuboid model, explicit UV, clean source and cooking-only demonstration.')
