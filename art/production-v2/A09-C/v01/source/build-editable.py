"""Editable reference scenes and independent Blender BVH intersection audit."""
from pathlib import Path
import bpy,json,math,hashlib,itertools
from mathutils import Matrix,Vector
from mathutils.bvhtree import BVHTree
R=Path(__file__).resolve().parents[1]
G=json.loads((R/'source/reference-geometry.json').read_text());L=json.loads((R/'exports/waist-layout.json').read_text());F=json.loads((R/'references/adopted-fuse-layout.json').read_text())
def rx(d):return Matrix.Rotation(math.radians(d),3,'X')
def ry(d):return Matrix.Rotation(math.radians(d),3,'Y')
def rz(d):return Matrix.Rotation(math.radians(d),3,'Z')
def mesh_world(mesh,fn,name=None):return {'name':name or mesh['name'],'vertices':[list(fn(Vector(v))) for v in mesh['vertices']],'faces':mesh['faces']}
def body(v,pose,variant):return Matrix(G['poses'][pose][variant]['bodyMatrix'])@v
def cube(a,b,name,color):
    x,y,z=a;X,Y,Z=b
    return {'name':name,'vertices':[[x,y,z],[X,y,z],[X,Y,z],[x,Y,z],[x,y,Z],[X,y,Z],[X,Y,Z],[x,Y,Z]],'faces':[{'indices':f,'color':color} for f in [[0,3,2,1],[4,5,6,7],[0,1,5,4],[1,2,6,5],[2,3,7,6],[3,0,4,7]]]}
def bbox(mesh):return [(min(v[i] for v in mesh['vertices']),max(v[i] for v in mesh['vertices'])) for i in range(3)]
def build_item(id,pose,variant,settle=1,fusion=False):
    p=L['profiles'][id];m=G['items'][id];bounds=bbox(m);center=Vector([(a+b)/2 for a,b in bounds]);rot=ry(p['yawDegrees'])@rz(p['rollDegrees']);scale=p['effectiveScale']
    target=Vector(p['centerBodyLocal'])+Vector(p['poseOffsets'][pose])+Vector(L['waistSettle'][p['category']]['offset'])*(1-settle)**3
    def fn(v):return body(rot@((v-center)*scale)+target,pose,variant)
    objects=[mesh_world(m,fn,'ITEM_'+id)]
    if fusion and id in ['sword','axe','shield']:
        f=F['profiles'][id];span=f['maxSpan']*16;contact=Vector((f['contact'][0]*16,f['contact'][2]*16,f['contact'][1]*16+span/2-f['penetration']*16))
        material=cube(list(contact-Vector((span/2,)*3)),list(contact+Vector((span/2,)*3)),'FUSE_'+id,[.48,.50,.47]);objects.append(mesh_world(material,fn))
    return objects
def obstruction(kind,pose,variant,amount):
    meshes=[]
    if kind=='cape':
        angle=L['capeProxy']['swingDegrees'];r=rx(-(angle[0]+amount*(angle[1]-angle[0])))
        meshes.append(mesh_world(cube([-5,-16,3.2],[5,0,4.2],'PROXY_cape',[.37,.18,.15]),lambda v:body(r@v,pose,variant)))
    else:
        angles=L['elytraProxy']['spreadDegrees'];spread=angles[0]+amount*(angles[1]-angles[0])
        for side in [-1,1]:
            r=ry(-side*spread);offset=Vector((side*2,-2,3.2));m=cube([-10,-20,0] if side<0 else [0,-20,0],[0,0,2] if side<0 else [10,0,2],'PROXY_elytra_'+str(side),[.38,.45,.49])
            meshes.append(mesh_world(m,lambda v:body(r@v+offset,pose,variant)))
    return meshes
def tree(mesh):return BVHTree.FromPolygons([Vector(v) for v in mesh['vertices']],[f['indices'] for f in mesh['faces']],all_triangles=False,epsilon=.0001)
def overlaps(a,b):
    A=bbox(a);B=bbox(b)
    if any(A[i][1]<B[i][0] or B[i][1]<A[i][0] for i in range(3)):return False
    return bool(tree(a).overlap(tree(b)))
def safe_crouch_lifts():
    for id,p in L['profiles'].items():
        # Actual transformed vertices, with/without adopted representative material.
        p['poseOffsets']['crouch']=[0,0,0]
        minimum=min(v[1] for variant in ['standard','slim'] for m in build_item(id,'crouch',variant,fusion=True) for v in m['vertices'])
        yy=G['poses']['crouch']['standard']['bodyMatrix'][1][1]
        lift=max(0,(.8-minimum)/yy)
        p['poseOffsets']['crouch'][1]=math.ceil(lift*10)/10
    (R/'exports/waist-layout.json').write_text(json.dumps(L,ensure_ascii=False,indent=2)+'\n')
safe_crouch_lifts()
groups=[['sword','shield','bow'],['sword','shield','crossbow'],['axe','shield','bow'],['axe','shield','crossbow']]
audit=[]
for variant,pose,cover,amount in itertools.product(['standard','slim'],['stand','crouch','swim','glide'],['cape','elytra'],[0,1]):
    parts=list(G['poses'][pose][variant]['parts'].values());covers=obstruction(cover,pose,variant,amount)
    for fused,settle in itertools.product([False,True],[0,1]):
        built={id:build_item(id,pose,variant,settle,fused) for id in L['profiles']}
        collisions=[]
        for id,objects in built.items():
            for m in objects:
                for b in parts+covers:
                    if overlaps(m,b):collisions.append([id,m['name'],b['name']])
        for group in groups:
            for a,b in itertools.combinations(group,2):
                if any(overlaps(x,y) for x in built[a] for y in built[b]):collisions.append([a,b,'item_pair'])
        floor=min(v[1] for objects in built.values() for m in objects for v in m['vertices'])
        audit.append({'variant':variant,'pose':pose,'cover':cover,'swingOrSpread':amount,'fusion':fused,'settle':settle,'collisions':collisions,'minimumY':round(floor,4),'floorPassed':pose not in ['stand','crouch'] or floor>=.7})
bad=[r for r in audit if r['collisions'] or not r['floorPassed']]
(R/'review/geometry-checks.json').write_text(json.dumps({'allPassed':not bad,'cases':len(audit),'baseSingleItemCases':80,'baseThreeRecordCases':64,'rows':audit,'method':'Blender BVH actual surface intersection; conservative cape/elytra guides; floor check on actual vertices; not real game animation/collision','gameRuntimeVerified':False},ensure_ascii=False,indent=2)+'\n')
print('AUDIT',json.dumps({'cases':len(audit),'bad':len(bad),'firstFailures':bad[:5],'crouchLifts':{k:p['poseOffsets']['crouch'][1] for k,p in L['profiles'].items()}},ensure_ascii=False))
if bad:raise RuntimeError('Reference geometry needs layout repair before saving review source')
# Build eight editable scenes; same raw reference item meshes, fixed effective scale.
bpy.ops.wm.read_factory_settings(use_empty=True);bpy.context.preferences.filepaths.save_version=0
palette={};data={}
def material(rgb):
    key=tuple(round(v,5) for v in rgb)
    if key not in palette:
        m=bpy.data.materials.new('REF_'+'_'.join(str(round(x*255)) for x in key));m.diffuse_color=tuple(key)+(1,);m.use_nodes=True;m.node_tree.nodes.get('Principled BSDF').inputs['Base Color'].default_value=tuple(key)+(1,);palette[key]=m
    return palette[key]
def add_mesh(mesh,col):
    me=bpy.data.meshes.new(mesh['name']);me.from_pydata([(v[0]/16,v[2]/16,v[1]/16) for v in mesh['vertices']],[],[f['indices'] for f in mesh['faces']]);me.update()
    ids={}
    for p,f in zip(me.polygons,mesh['faces']):
        key=tuple(f['color'])
        if key not in ids:ids[key]=len(ids);me.materials.append(material(key))
        p.material_index=ids[key]
    o=bpy.data.objects.new(mesh['name'],me);col.objects.link(o);o['referenceOnly']=True;return o
first=None
for variant,pose in itertools.product(['standard','slim'],['stand','crouch','swim','glide']):
    scene=bpy.data.scenes.new('A09C_'+variant+'_'+pose);first=first or scene
    bodies=bpy.data.collections.new('BODY_'+variant+'_'+pose);scene.collection.children.link(bodies)
    for m in G['poses'][pose][variant]['parts'].values():add_mesh(m,bodies)
    for cover in ['cape','elytra']:
        col=bpy.data.collections.new(cover.upper()+'_PROXY_'+variant+'_'+pose);scene.collection.children.link(col)
        for m in obstruction(cover,pose,variant,0):o=add_mesh(m,col);o.hide_render=o.hide_viewport=cover=='elytra'
    for id in L['profiles']:
        col=bpy.data.collections.new('WAIST_'+id+'_'+variant+'_'+pose);scene.collection.children.link(col)
        for m in build_item(id,pose,variant):o=add_mesh(m,col);o.hide_render=o.hide_viewport=id in ['axe','crossbow']
    if pose=='glide':
        col=bpy.data.collections.new('ADOPTED_CANOPY_'+variant);scene.collection.children.link(col)
        for m in G['glider']:add_mesh(m,col)
    camera=bpy.data.cameras.new('Review camera');o=bpy.data.objects.new('Review camera',camera);scene.collection.objects.link(o);o.location=(3,-5,2.8);o.rotation_euler=(Vector((0,0,1))-o.location).to_track_quat('-Z','Y').to_euler();camera.type='ORTHO';camera.ortho_scale=4;scene.camera=o
    scene['newProductionModels']=0;scene['capeAndElytraAreProxies']=True;scene['adopted']=False
    scene.render.engine='BLENDER_WORKBENCH';scene.display.shading.color_type='MATERIAL';scene.render.resolution_x=960;scene.render.resolution_y=800;scene.render.resolution_percentage=100
empty=bpy.data.scenes.get('Scene')
if empty:bpy.data.scenes.remove(empty)
bpy.context.window.scene=first
bpy.ops.wm.save_as_mainfile(filepath=str(R/'source/waist-placement-v01.blend'))
print('SAVED editable eight-scene reference; no replacement equipment models')
