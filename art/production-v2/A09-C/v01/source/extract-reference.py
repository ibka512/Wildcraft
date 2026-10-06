"""Read adopted reference geometry. Never save the approved .blend file."""
from pathlib import Path
import bpy,json,hashlib
from mathutils import Matrix,Vector
R=Path(__file__).resolve().parents[1];P=R.parents[3]
original=P/'art/approved-v1/A09-背负装备挂点-v1/source/back-equipment-placement.blend'
bpy.ops.wm.open_mainfile(filepath=str(original),load_ui=False,use_scripts=False)
def color(o,p):
    m=o.data.materials[p.material_index]
    if m.use_nodes:
        n=next((n for n in m.node_tree.nodes if n.type=='BSDF_PRINCIPLED'),None)
        if n:return list(n.inputs['Base Color'].default_value[:3])
    return list(m.diffuse_color[:3])
def serialize(o,world=False,offset=(0,0,0)):
    vertices=[]
    for v in o.data.vertices:
        p=o.matrix_world@v.co if world else v.co
        # Blender X/right Y/back Z/up -> design X/right Y/up Z/back, in pixel units.
        vertices.append([(p.x-offset[0])*16,(p.z-offset[2])*16,(p.y-offset[1])*16])
    return {'name':o.name,'vertices':vertices,'faces':[{'indices':list(p.vertices),'color':color(o,p)} for p in o.data.polygons]}
names={'sword':'Reference_diamond_sword','axe':'Reference_diamond_axe','shield':'Reference_shield','bow':'Reference_bow','crossbow':'Reference_crossbow_standby'}
items={k:serialize(bpy.data.objects[n]) for k,n in names.items()}
poses={}
def enable(scene):
    bpy.context.window.scene=scene
    for collection in bpy.data.collections:collection.hide_viewport=False
    def include(layer):
        layer.exclude=False;layer.hide_viewport=False
        for child in layer.children:include(child)
    include(bpy.context.view_layer.layer_collection)
    for obj in scene.objects:
        obj.hide_viewport=False;obj.hide_set(False)
    scene.frame_set(1);bpy.context.view_layer.update()
for pose in ['stand','crouch','swim','glide']:
    scene=bpy.data.scenes['A09_'+pose.upper()];enable(scene);poses[pose]={}
    for variant in ['standard','slim']:
        anchor=next(o for o in scene.objects if o.name.startswith(variant+'_BodyAnchor'))
        offset=(anchor.location.x,0,0)
        parts={}
        for key in ['head','torso','left_arm','right_arm','left_leg','right_leg']:
            o=next((o for o in scene.objects if o.name.split('.')[0]==variant+'_'+key),None)
            if o is None and key=='torso':o=next(o for o in scene.objects if o.parent==anchor and 'body' in o.name.lower())
            assert o is not None,(pose,variant,key)
            parts[key]=serialize(o,True,offset)
        # Store affine in design units so equipment stays on the live body anchor.
        C=Matrix(((1,0,0,0),(0,0,1,0),(0,1,0,0),(0,0,0,1)))
        M=C@anchor.matrix_world@C;M.translation=Vector((0,anchor.matrix_world.translation.z*16,anchor.matrix_world.translation.y*16))
        poses[pose][variant]={'bodyMatrix':[[float(x) for x in row] for row in M],'parts':parts}
# Adopted canopy as a textured-colour guide: no new glider mesh or texture exported.
glider=[];scene=bpy.data.scenes['A09_GLIDE'];enable(scene)
for o in scene.objects:
    if o.type=='MESH' and any(m and m.name.startswith('Wildcraft_Cloth_Oak') for m in o.data.materials):
        mesh=serialize(o,True)
        for face in mesh['faces']:face['color']=[.68,.65,.49] if 'cream' in o.name else [.43,.50,.35] if 'sage' in o.name else [.30,.22,.14]
        glider.append(mesh)
out={'referenceOnly':True,'basis':'X right/Y up/Z back; unit 1/16 block','adoptedSource':str(original.relative_to(P)),
    'adoptedSourceSha256':hashlib.sha256(original.read_bytes()).hexdigest(),'items':items,'poses':poses,'glider':glider,'newProductionMeshes':0}
(R/'source/reference-geometry.json').write_text(json.dumps(out,separators=(',',':'))+'\n')
print('EXTRACTED',json.dumps({'items':list(items),'poses':len(poses),'variants':2,'canopyParts':len(glider),'newProductionMeshes':0}))
