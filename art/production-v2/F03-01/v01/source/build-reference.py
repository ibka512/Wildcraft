"""Editable art-review meshes from adopted native references and local native pixels.
No raw Minecraft files copied out; derived local colors are read-only references; not replacement item models.
"""
import bpy,bmesh,json,zipfile,io,math,hashlib
from pathlib import Path
from mathutils import Vector,Matrix
R=Path(__file__).resolve().parents[1];S=json.loads((R/'source/mount-spec.json').read_text());JAR=Path('/Volumes/仕事/Wildcraft/开发环境/cache/gradle/caches/fabric-loom/26.3/minecraft-client.jar')
NATIVE=json.loads((R/'references/native-color-reference.json').read_text())
class PixelReference:
 def __init__(self,data):self.size=data['size'];self.pixels=data['pixels']
 def getpixel(self,xy):return self.pixels[xy[1]*self.size[0]+xy[0]]
bpy.ops.object.select_all(action='SELECT');bpy.ops.object.delete(use_global=False);bpy.context.preferences.filepaths.save_version=0
scene=bpy.context.scene;scene.render.engine='CYCLES';scene.cycles.samples=12;scene.render.resolution_x=scene.render.resolution_y=600;scene.render.resolution_percentage=100;scene.render.image_settings.file_format='PNG';scene.view_settings.view_transform='Standard';scene.world.color=(.19,.22,.20)
models=bpy.data.collections.new('REFERENCE native host and material meshes');scene.collection.children.link(models);rig=bpy.data.collections.new('REVIEW mannequin and same-scale comparison');scene.collection.children.link(rig);studio=bpy.data.collections.new('REVIEW studio');scene.collection.children.link(studio)
with bpy.data.libraries.load(str(R/'references/adopted-back-reference.blend'),link=False) as (src,dst):dst.objects=['Reference_diamond_sword','Reference_diamond_axe','Reference_shield']
hosts={}
for key,name in [('sword','Reference_diamond_sword'),('axe','Reference_diamond_axe'),('shield','Reference_shield')]:
 o=bpy.data.objects[name];models.objects.link(o);o.name='HOST_'+key;hosts[key]=o;o.hide_render=True;o.hide_viewport=True;o['referenceOnly']=True
palette={};inputs=[]
def color(name,rgb):
 key=tuple(rgb)
 if key in palette:return palette[key]
 m=bpy.data.materials.new(name);m.use_nodes=True;bs=next(n for n in m.node_tree.nodes if n.type=='BSDF_PRINCIPLED');bs.inputs['Base Color'].default_value=tuple(v/255 for v in rgb)+(1,);bs.inputs['Roughness'].default_value=.9;palette[key]=m;return m
def boxpart(verts,faces,indices,a,b,mi):
 x0,y0,z0=a;x1,y1,z1=b;n=len(verts);verts.extend([(x0,y0,z0),(x1,y0,z0),(x1,y1,z0),(x0,y1,z0),(x0,y0,z1),(x1,y0,z1),(x1,y1,z1),(x0,y1,z1)]);faces.extend(tuple(n+i for i in f) for f in [(0,3,2,1),(4,5,6,7),(0,1,5,4),(1,2,6,5),(2,3,7,6),(3,0,4,7)]);indices.extend([mi]*6)
def mesh(name,verts,faces,mats,indices,col=models):
 me=bpy.data.meshes.new(name);me.from_pydata(verts,[],faces);me.update();o=bpy.data.objects.new(name,me);col.objects.link(o)
 for m in mats:me.materials.append(m)
 for f,i in zip(me.polygons,indices):f.material_index=i
 bm=bmesh.new();bm.from_mesh(me);bmesh.ops.recalc_face_normals(bm,faces=bm.faces);bm.to_mesh(me);bm.free();o.hide_render=True;o.hide_viewport=True;o['referenceOnly']=True;return o
if True:
 def native_image(name):
  data=NATIVE['entries'][name];inputs.append(dict(entry=name,sha256=data['sha256']));return PixelReference(data)
 def sprite(key,item,fixed=True):
  im=native_image('assets/minecraft/textures/item/'+item+'.png');w,h=im.size;verts=[];faces=[];mi=[];mats=[];ids={}
  for y in range(h):
   for x in range(w):
    r,g,b,a=im.getpixel((x,y))
    if a<128:continue
    c=(r,g,b)
    if c not in ids:ids[c]=len(mats);mats.append(color(item,c))
    x0=x/w-.5;x1=(x+1)/w-.5;z0=.5-(y+1)/h;z1=.5-y/h
    if fixed:x0,x1=-x1,-x0
    boxpart(verts,faces,mi,[x0,-1/32,z0],[x1,1/32,z1],ids[c])
  o=mesh(key,verts,faces,mats,mi);return o
 hosts['arrow']=sprite('HOST_arrow','arrow',False)
 materials={'diamond':sprite('MATERIAL_diamond','diamond'),'feather':sprite('MATERIAL_feather','feather')}
 im=native_image('assets/minecraft/textures/block/stone.png');verts=[];faces=[];mi=[];mats=[];ids={}
 # Native pixels tessellate cube surfaces, do not resample a texture or modify the source image.
 for axis in range(3):
  for sign in [-1,1]:
   for y in range(16):
    for x in range(16):
     c=tuple(im.getpixel((x,y))[:3])
     if c not in ids:ids[c]=len(mats);mats.append(color('stone',c))
     a=x/16-.5;b=(x+1)/16-.5;c0=y/16-.5;c1=(y+1)/16-.5
     corners=[(a,c0),(b,c0),(b,c1),(a,c1)];vv=[]
     for u,v in corners:
      p=[u,v,sign*.5]
      if axis==0:p=[sign*.5,u,v]
      elif axis==1:p=[u,sign*.5,v]
      vv.append(tuple(p))
     n=len(verts);verts+=vv;faces.append(tuple(range(n,n+4)));mi.append(ids[c])
 materials['stone']=mesh('MATERIAL_stone',verts,faces,mats,mi)
 # Read-only source metadata only; no game archive/assets/sources enter the deliverable.
(R/'references/native-input-hashes.json').write_text(json.dumps({'archive':str(JAR),'archiveSha256':hashlib.sha256(JAR.read_bytes()).hexdigest(),'entries':[dict(entry=n,sha256=d['sha256']) for n,d in NATIVE['entries'].items()],'rawFilesCopied':False,'approximation':'generated items one-pixel extrusion; cube per-face native pixels; not game extraction'},indent=2)+'\n')
def bounds(o):return [min(v.co[i] for v in o.data.vertices) for i in range(3)],[max(v.co[i] for v in o.data.vertices) for i in range(3)]
def placement(host,material,flight=False):
 p=S['profiles'][host]['flight'] if flight else S['profiles'][host];a,b=bounds(material);extent=[b[i]-a[i] for i in range(3)];scale=p['maxSpan']/max(extent);center=Vector([(a[i]+b[i])/2 for i in range(3)]);axis=0 if flight else 1;target=Vector(p['contact'])+Vector(p['normal'])*(extent[axis]*scale/2-p['penetration']);return scale,center,target
def duplicate(o,name,col=models):
 n=o.copy();n.data=o.data.copy();n.name=name;col.objects.link(n);n.hide_render=False;n.hide_viewport=False;return n
def assembly(host,material,name,col=models,flight=False):
 root=bpy.data.objects.new(name,None);col.objects.link(root);root['host']=host;root['material']=material;root['candidateOnly']=True
 h=duplicate(hosts['flight'] if flight else hosts[host],name+'/host',col);h.parent=root
 child=duplicate(materials[material],name+'/material',col);child.parent=root;scale,center,target=placement(host,child,flight);child.scale=(scale,)*3;child.location=target-center*scale;child['span']=S['profiles'][host]['flight']['maxSpan'] if flight else S['profiles'][host]['maxSpan'];child['contactPenetration']=S['profiles'][host]['flight']['penetration'] if flight else S['profiles'][host]['penetration'];return root,h,child
# Flight reference uses native alpha-cut pixels on the original two cross planes.
im=native_image('assets/minecraft/textures/entity/projectiles/arrow.png');verts=[];faces=[];mi=[];mats=[];ids={}
def planePixel(vv,rgba):
 if rgba[3]<128:return
 c=tuple(rgba[:3])
 if c not in ids:ids[c]=len(mats);mats.append(color('native arrow flight',c))
 n=len(verts);verts.extend(vv);faces.append(tuple(range(n,n+4)));mi.append(ids[c])
for theta in [math.pi/4,3*math.pi/4]:
 for u in range(16):
  for v in range(5):
   vv=[]
   for x,t in [(-.675+u*.9/16,-.1125+v*.225/5),(-.675+(u+1)*.9/16,-.1125+v*.225/5),(-.675+(u+1)*.9/16,-.1125+(v+1)*.225/5),(-.675+u*.9/16,-.1125+(v+1)*.225/5)]:vv.append((x,t*math.cos(theta),t*math.sin(theta)))
   planePixel(vv,im.getpixel((u,v)))
for u in range(5):
 for v in range(5):
  vv=[]
  for y,z in [(-.1125+u*.225/5,-.1125+v*.225/5),(-.1125+(u+1)*.225/5,-.1125+v*.225/5),(-.1125+(u+1)*.225/5,-.1125+(v+1)*.225/5),(-.1125+u*.225/5,-.1125+(v+1)*.225/5)]:vv.append((-.61875,y*math.cos(math.pi/4)-z*math.sin(math.pi/4),y*math.sin(math.pi/4)+z*math.cos(math.pi/4)))
  planePixel(vv,im.getpixel((u,v+5)))
hosts['flight']=mesh('HOST_native_flight_arrow_reference',verts,faces,mats,mi)
sceneObjects={};audit=[]
for host in ['sword','axe','shield','arrow']:
 for material in ['stone','diamond','feather']:
  root,h,c=assembly(host,material,'REVIEW_'+host+'_'+material);sceneObjects[host+'_'+material]=(root,h,c)
  bpy.context.view_layer.update();a,b=bounds(c);points=[Matrix.LocRotScale(c.location,c.rotation_euler.to_quaternion(),c.scale)@Vector(v) for v in c.bound_box];aa=[min(v[i] for v in points) for i in range(3)];bb=[max(v[i] for v in points) for i in range(3)]
  audit.append(dict(host=host,material=material,maxSpan=S['profiles'][host]['maxSpan'],scale=c.scale[0],materialFrom=aa,materialTo=bb,contact=S['profiles'][host]['contact'],penetration=S['profiles'][host]['penetration'],protectedPatternClear=not(host=='shield' and aa[0]<.2 and bb[0]>-.2 and aa[2]<.4 and bb[2]>-.4)))
for material in ['stone','diamond','feather']:sceneObjects['flight_'+material]=assembly('arrow',material,'REVIEW_flight_'+material,flight=True)
# Pattern guide references a protected region, not a baked replacement for vanilla banner components.
blue=color('REVIEW pattern-zone only',(57,93,117));white=color('REVIEW cross only',(205,215,198));verts=[];faces=[];mi=[]
boxpart(verts,faces,mi,[-.2,.1255,-.4],[.2,.1265,.4],0);boxpart(verts,faces,mi,[-.025,.1266,-.4],[.025,.1276,.4],1);boxpart(verts,faces,mi,[-.2,.1266,-.025],[-.025,.1276,.025],1);boxpart(verts,faces,mi,[.025,.1266,-.025],[.2,.1276,.025],1)
pattern=mesh('REVIEW synthetic pattern protection guide',verts,faces,[blue,white],mi);pattern['notVanillaPatternRenderer']=True
patternCopies=[]
for mat in ['stone','diamond','feather']:
 p=duplicate(pattern,'REVIEW_pattern_'+mat);p.parent=sceneObjects['shield_'+mat][0];patternCopies.append(p)
def hideAll():
 for o in models.objects:o.hide_render=True;o.hide_viewport=True
 for o in rig.objects:o.hide_render=True;o.hide_viewport=True
def showGroup(key):
 hideAll();root,h,c=sceneObjects[key]
 for o in [root,h,c]:o.hide_render=False;o.hide_viewport=False
 if key.startswith('shield'):
  p=next(p for p in patternCopies if p.parent==root);p.hide_render=p.hide_viewport=False
 return root
for name,loc,power,size in [('Key',(-3,4,5),380,4),('Fill',(4,2,3),190,4),('Rim',(0,-4,4),180,4)]:
 d=bpy.data.lights.new(name,'AREA');d.energy=power;d.size=size;o=bpy.data.objects.new(name,d);studio.objects.link(o);o.location=loc;o.rotation_euler=(Vector((0,0,0))-o.location).to_track_quat('-Z','Y').to_euler()
d=bpy.data.cameras.new('Orthographic review camera');cam=bpy.data.objects.new('Orthographic review camera',d);studio.objects.link(cam);scene.camera=cam;d.type='ORTHO'
def camera(loc,target,scale):cam.location=loc;cam.rotation_euler=(Vector(target)-cam.location).to_track_quat('-Z','Y').to_euler();d.ortho_scale=scale
def render(n):scene.render.filepath=str(R/'review'/n);bpy.ops.render.render(write_still=True)
showGroup('sword_stone');camera((1.8,3,1.8),(0,0,0),1.65)
# Editable source stores all candidate assemblies separately, default representative visible.
bpy.ops.wm.save_as_mainfile(filepath=str(R/'source/fuse-mount-review-v01.blend'))
for host in ['sword','axe','shield','arrow','flight']:
 for mat in ['stone','diamond','feather']:
  root=showGroup(host+'_'+mat);scale=1.72 if host=='shield' else 1.6 if host=='flight' else 1.55
  camera((.35,3,.25),(0,0,0),scale);render(host+'-'+mat+'-front.png')
  if mat=='stone':camera((2.8,2,.65),(0,0,0),scale);render(host+'-'+mat+'-side.png')
# Same camera, same effective scale; arrow has no back record.
hideAll()
for i,host in enumerate(['sword','axe','shield']):
 for role,x in [('held',-.9),('back',.9)]:
  root,h,c=assembly(host,'stone','COMPARE_'+host+'_'+role,rig);root.location=(x,0,1.6-i*1.7);scale=S['profiles'][host]['heldEffectiveScale'];root.scale=(scale,)*3;root['worldScale']=scale;root['comparisonRole']=role
camera((0,7,0),(0,0,0),5.4);render('hand-back-same-scale.png')
# Static neutral mannequin reference; does not emulate the game hand animation/FOV.
def mannequin(x,name):
 v=[];f=[];ind=[];m=[color('REVIEW neutral cloth',(76,95,84)),color('REVIEW neutral skin',(159,146,127)),color('REVIEW neutral trousers',(62,73,82))]
 boxpart(v,f,ind,[x-.25,-.125,.75],[x+.25,.125,1.5],0)
 boxpart(v,f,ind,[x-.25,-.25,1.5],[x+.25,.25,2],1)
 for sign in [-1,1]:
  a=x+sign*.375;boxpart(v,f,ind,[a-.125,-.125,.75],[a+.125,.125,1.5],0)
  a=x+sign*.125;boxpart(v,f,ind,[a-.125,-.125,0],[a+.125,.125,.75],2)
 return mesh(name,v,f,m,ind,rig)
A09=json.loads((R/'references/adopted-back-layout.json').read_text())
for host in ['sword','axe','shield']:
 hideAll()
 for role,x in [('held',-1.15),('back',1.15)]:
  person=mannequin(x,'POSE_'+host+'_'+role);person.hide_render=person.hide_viewport=False
  root,h,c=assembly(host,'stone','POSE_FUSE_'+host+'_'+role,rig);scale=S['profiles'][host]['heldEffectiveScale'];root.scale=(scale,)*3;root['worldScale']=scale;root['comparisonRole']=role
  if role=='back':
   p=A09['profiles'][host];root.location=(x+p['center'][0]/16,p['center'][2]/16,p['center'][1]/16);root.rotation_euler[1]=math.radians(p['canvasAngle'])
  else:root.location=(x-.6625,.28,1.0125) if host!='shield' else (x-.60,.37,1.08)
 camera((.4,7,2.2),(0,0,1),5.0);render(host+'-held-back-pose.png')
# Dropped item view retains the chosen cap, changes the whole assembly's display scale.
for host in ['sword','axe','shield','arrow']:
 root=showGroup(host+'_stone');root.scale=(.25,)*3 if host=='shield' else (.5,)*3;root['groundReferenceScale']=root.scale[0]
 camera((1.5,3,1.8),(0,0,0),1.05);render(host+'-ground.png');root.scale=(1,1,1)
hideAll();showGroup('sword_stone');camera((1.8,3,1.8),(0,0,0),1.65)
# Save final source after comparison group creation, with neutral default selection restored.
bpy.ops.wm.save_as_mainfile(filepath=str(R/'source/fuse-mount-review-v01.blend'))
def serialize(o):
 return dict(name=o.name,vertices=[list(v.co) for v in o.data.vertices],faces=[dict(indices=list(p.vertices),color=list(next(n for n in o.data.materials[p.material_index].node_tree.nodes if n.type=='BSDF_PRINCIPLED').inputs['Base Color'].default_value[:3])) for p in o.data.polygons])
(R/'source/reference-meshes.json').write_text(json.dumps({'referenceOnly':True,'basis':'X right,Y out,Z up; native approximate silhouettes','hosts':{k:serialize(v) for k,v in hosts.items()},'materials':{k:serialize(v) for k,v in materials.items()},'newProductionModels':0},separators=(',',':'))+'\n')
(R/'review/placement-audit.json').write_text(json.dumps({'asset':'F03-01','cases':audit,'gameIntegrated':False,'exactVanillaSpecialRenderer':False,'newProductionMeshes':0},indent=2)+'\n');print('Saved editable review rig, 28 actual geometry renders and 12 placement cases; no new production item models')
