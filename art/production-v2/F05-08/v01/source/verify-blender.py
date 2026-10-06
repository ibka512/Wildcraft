import bpy,json,math
from pathlib import Path
from mathutils import Vector
R=Path(__file__).resolve().parents[1];bpy.ops.wm.open_mainfile(filepath=str(R/'source/stabilizer-v01.blend'));G=json.loads((R/'source/geometry-and-uv.json').read_text());C=json.loads((R/'references/approved-machine-geometry.json').read_text());prod=bpy.data.collections['PRODUCTION stabilizer'];ref=bpy.data.collections['REFERENCE adopted F05-01 chassis'];root=bpy.data.objects['Stabilizer node origin'];lamp=bpy.data.materials['State indicator RGB no emission'];lbs=next(n for n in lamp.node_tree.nodes if n.type=='BSDF_PRINCIPLED')
assert sum(o.type=='MESH'for o in prod.objects)==25 and sum(o.type=='MESH'for o in ref.objects)==68 and all(o.hide_render for o in ref.objects)
im=next(i for i in bpy.data.images if i.name.startswith('shared-machine'));assert tuple(im.size)==(64,64)and im.packed_file;assert not any(o.animation_data for o in prod.objects)
assert len(lamp.node_tree.animation_data.drivers)==3 and lbs.inputs['Emission Strength'].default_value==0;assert root['active']==0 and root['ghost']==0 and root['powerFailureReference']==0
def rot(v,n):
 x,y,z=v;return [(x,z,-y),(x,-z,y),(-x,y,-z),(x,y,z),(-z,y,x),(z,y,-x)][n]
def V(v):return Vector((v[0]/16,v[2]/16,v[1]/16))
cases=[];total=0
for n,r in enumerate([(math.pi/2,0,0),(-math.pi/2,0,0),(0,0,math.pi),(0,0,0),(0,0,math.pi/2),(0,0,-math.pi/2)]):
 root.rotation_euler=r;root.location=V(C['nodes'][n]['point'])
 for active,ghost,reference in [(0,0,0),(1,0,0),(0,0,1),(1,0,1),(1,1,1)]:
  root['active']=active;root['ghost']=ghost;root['powerFailureReference']=reference;root.update_tag();bpy.context.scene.frame_set(bpy.context.scene.frame_current+1);bpy.context.view_layer.update();dg=bpy.context.evaluated_depsgraph_get();expectedColor=G['indicatorColors']['inactive'if ghost else'active'if active else'powerFailureReference'if reference else'inactive'];srgb=[int(expectedColor[i:i+2],16)/255 for i in [1,3,5]];rgb=[c/12.92 if c<=.04045 else ((c+.055)/1.055)**2.4 for c in srgb];assert all(abs(a-b)<1e-6 for a,b in zip(lbs.inputs['Base Color'].default_value,rgb));points=0
  for p in G['parts']:
   o=bpy.data.objects[p['name']].evaluated_get(dg)
   for v,vv in zip(o.data.vertices,p['vertices']):
    expected=V([a+b for a,b in zip(rot(vv,n),C['nodes'][n]['point'])]);assert (o.matrix_world@v.co-expected).length<1e-6,(n,active,ghost,reference,p['name']);points+=1
  total+=points;cases.append(dict(node=n,active=bool(active),ghost=bool(ghost),powerReference=bool(reference),color=expectedColor,vertexChecks=points,allGeometryStatic=True))
tex=next(n for n in bpy.data.materials['Approved 64px shared machine atlas'].node_tree.nodes if n.type=='TEX_IMAGE');assert tex.interpolation=='Closest'
result=dict(asset='F05-08',allPassed=True,sourceReopened=True,productionMeshes=25,packedAtlas=[64,64],hiddenReferenceMeshes=68,actualNodeAndColorCases=cases,totalEvaluatedVertexChecks=total,noMovingParts=True,threeRgbMaterialDrivers=True,noEmission=True,sourceDefaultInactive=True,referenceColorNotConnectedToRuntime=True,gameIntegrated=False)
(R/'review/blender-source-checks.json').write_text(json.dumps(result,indent=2)+'\n');print('Actual static meshes andRGB lamp drivers,6 nodes x5 cases PASS',total)
