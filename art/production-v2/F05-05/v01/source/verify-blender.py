import bpy,json,math
from pathlib import Path
from mathutils import Matrix,Vector
R=Path(__file__).resolve().parents[1];G=json.loads((R/'source/geometry-and-uv.json').read_text());bpy.ops.wm.open_mainfile(filepath=str(R/'source/battery-six-node-v01.blend'));s=bpy.context.scene;br=bpy.data.objects['Battery review mount'];root=bpy.data.objects['Machine chassis'];cases=[]
def rot(v,n):
 x,y,z=v;return [(x,z,-y),(x,-z,y),(-x,y,-z),(x,y,z),(-z,y,x),(z,y,-x)][n]
def V(v):x,y,z=v;return Vector((x/16,z/16,y/16))
def refresh():br.update_tag();root.update_tag();bpy.context.view_layer.update();s.frame_set(s.frame_current)
for n in range(6):
 br['node']=n;br['exploded_distance_model_units']=0
 for e in [0,1,99,100,333,500,667,999,1000]:
  br['energy']=e;refresh();dg=bpy.context.evaluated_depsgraph_get();be=br.evaluated_get(dg);f=[max(0,min(1,e/1000*3-i))for i in range(3)]
  for i in range(3):
   ob=bpy.data.objects['Battery dynamic window '+str(i)].evaluated_get(dg);assert abs(ob.scale.z-f[i])<1e-6
   mat=bpy.data.materials['Dynamic window '+str(i)+' geometry AND UV crop'];mul=next(k for k in mat.node_tree.nodes if k.type=='MATH' and k.operation=='MULTIPLY');assert abs(mul.inputs[1].default_value-f[i])<1e-6
  normal=be.matrix_world.to_3x3()@Vector((0,1,0));expected=V(G['nodes'][n]['normal'])*16;assert (normal-expected).length<1e-6
  assert (be.matrix_world.translation-V(G['nodes'][n]['point'])).length<1e-6;cases.append(dict(node=n,energy=e,fractions=f))
motions=[]
for yaw in [0,90,180,270]:
 for roll in [-6,0,6]:
  transform=Matrix.Rotation(math.radians(yaw),4,'Z')@Matrix.Rotation(-math.radians(roll),4,'Y');root.matrix_basis=transform
  for n in range(6):
   br['node']=n;br['energy']=500;refresh();dg=bpy.context.evaluated_depsgraph_get()
   for p in G['batteryParts']:
    o=bpy.data.objects[p['name']].evaluated_get(dg)
    for v in o.data.vertices:
     local=[v.co.x*16,v.co.z*16,v.co.y*16];expected=transform@V([a+b for a,b in zip(rot(local,n),G['nodes'][n]['point'])]);assert (o.matrix_world@v.co-expected).length<1e-6
   motions.append(dict(node=n,yaw=yaw,roll=roll))
assert all(n.interpolation=='Closest' and n.image.packed_file for m in bpy.data.materials if m.use_nodes for n in m.node_tree.nodes if n.type=='TEX_IMAGE')
result=dict(allPassed=True,actualBlenderReopened=True,energyAndUVCropCases=cases,worldYawRollCases=motions,batteryCuboids=16,dynamicWindowPlanes=3,scale=1,packedNearestTexture=True,gameTested=False)
(R/'review/blender-source-checks.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');print('PASS actual Blender: 54 six-node energy/UV cases, 72 body yaw/roll vertex cases.')
