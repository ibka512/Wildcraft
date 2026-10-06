import bpy,json,math
from pathlib import Path
from mathutils import Vector
R=Path(__file__).resolve().parents[1];bpy.ops.wm.open_mainfile(filepath=str(R/'source/buoyancy-v01.blend'));G=json.loads((R/'source/geometry-and-uv.json').read_text());C=json.loads((R/'references/approved-machine-geometry.json').read_text());prod=bpy.data.collections['PRODUCTION buoyancy'];ref=bpy.data.collections['REFERENCE adopted F05-01 chassis'];root=bpy.data.objects['Buoyancy node origin'];
assert sum(o.type=='MESH'for o in prod.objects)==15 and sum(o.type=='MESH'for o in ref.objects)==68 and all(o.hide_render for o in ref.objects)
im=next(i for i in bpy.data.images if i.name.startswith('shared-fabric'));assert tuple(im.size)==(128,64)and im.packed_file;assert not any(o.animation_data for o in prod.objects)
def rot(v,n):
 x,y,z=v;return [(x,z,-y),(x,-z,y),(-x,y,-z),(x,y,z),(-z,y,x),(z,y,-x)][n]
def V(v):return Vector((v[0]/16,v[2]/16,v[1]/16))
cases=[];total=0
for n,r in enumerate([(math.pi/2,0,0),(-math.pi/2,0,0),(0,0,math.pi),(0,0,0),(0,0,math.pi/2),(0,0,-math.pi/2)]):
 root.rotation_euler=r;root.location=V(C['nodes'][n]['point']);bpy.context.view_layer.update();dg=bpy.context.evaluated_depsgraph_get();points=0
 for p in G['parts']:
  o=bpy.data.objects[p['name']].evaluated_get(dg)
  for v,vv in zip(o.data.vertices,p['vertices']):
   expected=V([a+b for a,b in zip(rot(vv,n),C['nodes'][n]['point'])]);assert (o.matrix_world@v.co-expected).length<1e-6,(n,p['name']);points+=1
  uv=o.data.uv_layers.active
  for po,f in zip(o.data.polygons,p['faces']):
   expectedUV={vi:(u/16,1-v/16)for vi,(u,v)in zip(f['indices'],f['uvs'])}
   for li in po.loop_indices:
    u,v=expectedUV[o.data.loops[li].vertex_index];assert abs(uv.data[li].uv.x-u)<1e-6 and abs(uv.data[li].uv.y-v)<1e-6
 total+=points;cases.append(dict(node=n,vertexChecks=points,allGeometryStatic=True))
tex=next(n for n in bpy.data.materials['Approved 64px shared machine atlas'].node_tree.nodes if n.type=='TEX_IMAGE');assert tex.interpolation=='Closest';assert not tex.id_data.animation_data
result=dict(asset='F05-09',allPassed=True,sourceReopened=True,productionMeshes=15,packedAtlas=[128,64],hiddenReferenceMeshes=68,actualNodeCases=cases,totalEvaluatedVertexChecks=total,noMovingParts=True,productionUVMatches=True,sourceWaterPlaneIncluded=False,gameIntegrated=False)
(R/'review/blender-source-checks.json').write_text(json.dumps(result,indent=2)+'\n');print('Actual meshes andUV:6nodes PASS',total)
