import bpy,json,pathlib
from mathutils import Matrix, Vector
import sys
repo=pathlib.Path(sys.argv[sys.argv.index('--')+1]).resolve()
root=repo/'art/approved-v1'
out=repo/'src/client/resources/assets/wildcraft/animations/character-poses.json'
C=Matrix(((1,0,0),(0,0,-1),(0,1,0)))
result={}
for asset,rel in [('glide','A07-滑翔握持与姿态-v1/source/gliding-pose-rig.blend'),('climb','A08-攀爬动作-v1/source/climbing-motion-rig.blend')]:
 bpy.ops.wm.open_mainfile(filepath=str(root/rel),load_ui=False,use_scripts=False)
 # Both rig variants must participate in animation evaluation, even if the preview hides one.
 for collection in bpy.data.collections: collection.hide_viewport=False
 def include(layer):
  layer.exclude=False;layer.hide_viewport=False
  for child in layer.children: include(child)
 include(bpy.context.view_layer.layer_collection)
 for obj in bpy.data.objects:
  obj.hide_viewport=False
  if obj.name in bpy.context.view_layer.objects: obj.hide_set(False)
 bpy.context.view_layer.update()
 clips=[('glide',1,20)] if asset=='glide' else [(c['id'],c['start'],c['end']) for c in json.loads((root/'A08-攀爬动作-v1/deliverable/climbing-motion-spec.json').read_text())['clips']]
 for variant in ['standard','slim']:
  result[variant]=result.get(variant,{})
  for name,start,end in clips:
   frames=[]
   for frame in range(start,end+1):
    bpy.context.scene.frame_set(frame);poses=[]
    for limb,side in [('left_arm',-1),('right_arm',1),('left_leg',-1),('right_leg',1)]:
     o=bpy.data.objects[variant+'_'+limb]
     # Local object transforms deliberately omit parent EntityTrajectory_PREVIEW_ONLY.
     R=o.matrix_basis.to_3x3().normalized();B=C if 'arm' in limb else C.transposed();Rj=C@R@B
     # Blender XYZ composes Rz*Ry*Rx, matching native rotationZYX(z,y,x).
     e=Rj.to_euler('XYZ');loc=o.location
     center=side*(0.5 if variant=='slim' else 1) if 'arm' in limb else 0
     offset=Rj@Vector((center,0,0));pos=[loc.x*16-offset.x,24-loc.z*16-offset.y,loc.y*16-offset.z]
     poses.append([round(x,6) for x in pos+[e.x,e.y,e.z]])
    frames.append(poses)
   result[variant][name]=frames
out.parent.mkdir(parents=True,exist_ok=True);out.write_text(json.dumps(result,separators=(',',':'))+'\n')
print('BAKED',[(v,[(n,len(f)) for n,f in clips.items()]) for v,clips in result.items()])
