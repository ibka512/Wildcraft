"""Build 24 editable art timelines from the reviewed, baked reference transforms."""
from pathlib import Path
import bpy,json
from mathutils import Matrix, Vector
R=Path(__file__).resolve().parents[1]
D=json.loads((R/'exports/transition-tracks.json').read_text())
bpy.ops.wm.read_factory_settings(use_empty=True)
bpy.context.preferences.filepaths.save_version=0
convert=Matrix(((1,0,0,0),(0,0,1,0),(0,1,0,0),(0,0,0,1)))
def point(v):return (v[0]/16,v[2]/16,v[1]/16)
materials={}
def mat(color):
    key=tuple(color)
    if key not in materials:
        m=bpy.data.materials.new('Reference_color');m.diffuse_color=(*color,1);materials[key]=m
    return materials[key]
first=None
for track in D['scenes']:
    scene=bpy.data.scenes.new(track['variant']+'_'+track['id']);first=first or scene
    scene.render.fps=60;scene.frame_start=1;scene.frame_end=109;scene.render.engine='BLENDER_WORKBENCH';scene.display.shading.color_type='MATERIAL'
    scene['referenceOnly']=True;scene['gameRuntimeVerified']=False;scene['adopted']=False;scene['rootMotionApplied']=False
    collection=bpy.data.collections.new('Editable_'+scene.name);scene.collection.children.link(collection);objects={}
    for name,shape in track['shapes'].items():
        mesh=bpy.data.meshes.new(name);mesh.from_pydata([point(v)for v in shape['vertices']],[],[f['indices']for f in shape['faces']]);mesh.update()
        colors={}
        for p,f in zip(mesh.polygons,shape['faces']):
            color=tuple(f['color'])
            if color not in colors:colors[color]=len(colors);mesh.materials.append(mat(color))
            p.material_index=colors[color]
        obj=bpy.data.objects.new(name,mesh);collection.objects.link(obj);obj['referenceOnly']=True;obj['reviewId']=name;obj.rotation_mode='QUATERNION';objects[name]=obj
    for f in track['frames']:
        active={o['name']:o for o in f['objects']}
        for name,obj in objects.items():
            visible=name in active;obj.hide_render=obj.hide_viewport=not visible
            obj.keyframe_insert('hide_render',frame=f['frame']);obj.keyframe_insert('hide_viewport',frame=f['frame'])
            if not visible:continue
            sample=active[name]
            if 'matrix' in sample:
                m=convert@Matrix(sample['matrix'])@convert;m.translation/=16
                obj.location=m.translation;obj.rotation_quaternion=m.to_quaternion()
                obj.keyframe_insert('location',frame=f['frame']);obj.keyframe_insert('rotation_quaternion',frame=f['frame'])
            elif 'translation' in sample:
                obj.location=point(sample['translation']);obj.keyframe_insert('location',frame=f['frame'])
    camera_data=bpy.data.cameras.new('ReferenceCamera');camera=bpy.data.objects.new('ReferenceCamera',camera_data);scene.collection.objects.link(camera)
    for obj in objects.values():
        if not obj.animation_data:continue
        action=obj.animation_data.action
        for layer in action.layers:
            for strip in layer.strips:
                for curve in strip.channelbag(obj.animation_data.action_slot).fcurves:
                    for key in curve.keyframe_points:key.interpolation='CONSTANT' if curve.data_path.startswith('hide_') else 'LINEAR'
    camera.location=(3,-5,2.8);camera.rotation_euler=(Vector((0,0,1.4))-camera.location).to_track_quat('-Z','Y').to_euler();camera_data.type='ORTHO';camera_data.ortho_scale=4;scene.camera=camera
    scene.timeline_markers.new('Immediate state change',frame=40)
    if track['id']=='rapid_glide':scene.timeline_markers.new('Reopen',frame=45);scene.timeline_markers.new('Native bow takeover',frame=54)
    scene.frame_set(1)
empty=bpy.data.scenes.get('Scene')
if empty:bpy.data.scenes.remove(empty)
bpy.context.window.scene=first
bpy.ops.wm.save_as_mainfile(filepath=str(R/'source/exploration-joins-v01.blend'))
bpy.ops.wm.open_mainfile(filepath=str(R/'source/exploration-joins-v01.blend'),load_ui=False,use_scripts=False)
checks=[];vertex_errors=[];visibility_errors=[]
for scene in bpy.data.scenes:
    mesh=[o for o in scene.objects if o.type=='MESH'];checks.append({'scene':scene.name,'meshObjects':len(mesh),'referenceOnly':all(o.get('referenceOnly')for o in mesh),'frameEnd':scene.frame_end,'fps':scene.render.fps})
    track=next(t for t in D['scenes'] if scene.name==t['variant']+'_'+t['id'])
    bpy.context.window.scene=scene
    for n in [0,39,45,54,108]:
        scene.frame_set(n+1);bpy.context.view_layer.update();active={o['name']:o for o in track['frames'][n]['objects']}
        for obj in mesh:
            name=obj['reviewId'];sample=active.get(name)
            if obj.hide_render!=(sample is None):visibility_errors.append([scene.name,n+1,name])
            if sample is None or obj.hide_viewport:continue
            m=Matrix(sample.get('matrix',Matrix.Identity(4)));translation=Vector(sample.get('translation',[0,0,0]))
            # Compare independently with evaluated Blender coordinates at sampled times.
            for idx in [0,len(obj.data.vertices)-1]:
                v=Vector(track['shapes'][name]['vertices'][idx]);expected=m@v+translation
                actual=obj.matrix_world@obj.data.vertices[idx].co
                vertex_errors.append((actual-Vector(point(expected))).length)
passed=len(checks)==24 and all(c['referenceOnly']and c['frameEnd']==109 and c['fps']==60 for c in checks) and max(vertex_errors)<1e-5 and not visibility_errors
(R/'review/editable-checks.json').write_text(json.dumps({'allPassed':passed,'sceneCount':len(checks),'reopened':True,'checks':checks,'sampledVertexComparisons':len(vertex_errors),'maximumVertexErrorBlocks':max(vertex_errors),'visibilityErrors':visibility_errors,'gameRuntimeVerified':False},ensure_ascii=False,indent=2)+'\n')
if not passed:raise RuntimeError('Editable timelines failed verification')
print('REOPENED',len(checks),'editable art-only timelines')
