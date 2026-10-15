"""Full-size fixed wing, bottom-only rigid adapter; art proposal, no runtime edits."""
from pathlib import Path
import json,hashlib,shutil,datetime,uuid,base64,copy
R=Path(__file__).resolve().parents[1];OLD=R.parent/'v01';P=R.parents[3]
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def save(rel,o):(R/rel).write_text(json.dumps(o,ensure_ascii=False,indent=2)+'\n')
records=[]
for rel in ['exports/wing-geometry.json','exports/wing-mount-and-state.json','references/approved-machine-geometry.json','exports/wing-atlas-128x64.png','source/wing-atlas-128x64.aseprite','exports/wing-item-16.png','exports/wing-item-32.png','source/wing-item-16.aseprite','source/wing-item-32.aseprite']:
 p=OLD/rel;dest=R/'references'/p.name;shutil.copyfile(p,dest);records.append({'path':str(p.relative_to(P)),'copy':str(dest.relative_to(R)),'sha256':sha(p)})
for rel in ['src/main/java/dev/wildcraft/mechanics/MachineNodes.java','src/main/java/dev/wildcraft/mechanics/MachineEntity.java','src/main/java/dev/wildcraft/weather/WindRules.java','src/client/java/dev/wildcraft/client/mechanics/MachineRenderer.java','art/ADOPTED-ASSETS.json','art/production-v2/F05-06/v02/exports/spring-bottom-proposal.json','art/production-v2/F05-09/v02/exports/buoyancy-bottom-proposal.json']:
 p=P/rel;dest=R/'references'/p.name;shutil.copyfile(p,dest);records.append({'path':rel,'copy':str(dest.relative_to(R)),'sha256':sha(p)})
# Protect all production resources/code and historical v01 package.
protected={p.relative_to(P).as_posix():sha(p)for base in ['src','gradle','development-assets']for p in (P/base).rglob('*')if p.is_file()and not p.name.startswith('._')}
for rel in ['build.gradle','gradle.properties','settings.gradle','art/ADOPTED-ASSETS.json','art/ADOPTED-ASSETS.md','art/ADOPTED-ASSETS.csv']:
 if(P/rel).is_file():protected[rel]=sha(P/rel)
save('docs/BASELINE.json',{'checkedAt':datetime.datetime.now(datetime.timezone.utc).isoformat(),'files':records,'protected':protected,'originalPackageFiles':{p.relative_to(OLD).as_posix():sha(p)for p in OLD.rglob('*')if p.is_file()and not p.name.startswith('._')and '__pycache__'not in p.parts}})
G=json.loads((R/'references/wing-geometry.json').read_text());C=json.loads((R/'references/approved-machine-geometry.json').read_text());FM={'north':[0,1,2,3],'south':[5,4,7,6],'east':[1,5,6,2],'west':[4,0,3,7],'up':[3,2,6,7],'down':[4,5,1,0]}
def rot(v,n):x,y,z=v;return [(x,z,-y),(x,-z,y),(-x,y,-z),(x,y,z),(-z,y,x),(z,y,-x)][n]
def box(p):
 x0,y0,z0=p['from'];x1,y1,z1=p['to'];vs=[[x0,y0,z0],[x1,y0,z0],[x1,y1,z0],[x0,y1,z0],[x0,y0,z1],[x1,y0,z1],[x1,y1,z1],[x0,y1,z1]]
 if 'node'in p:vs=[[a+b for a,b in zip(rot(v,p['node']),C['nodes'][p['node']]['point'])]for v in vs]
 fs=[]
 for f,ids in FM.items():u0,v0,u1,v1=p['faces'][f]['uv'];fs.append({'indices':ids,'uvs':[[u0/2,v1],[u1/2,v1],[u1/2,v0],[u0/2,v0]]})
 return {'name':p['name'],'group':'body','vertices':vs,'faces':fs}
body=[box(p)for p in C['parts']];candidate=[];ax=9.88;zlo=.16;zhi=9.2;ceil=5.12
for p in C['parts']:
 if p.get('node')==1:continue
 if p['name']not in ['body_core','lower_plinth']:candidate.append(box(p));continue
 x0,y0,z0=p['from'];x1,y1,z1=p['to'];depth=min(y1,ceil)
 regions=[([x0,y0,z0],[-ax,depth,z1]),([ax,y0,z0],[x1,depth,z1]),([-ax,y0,z0],[ax,depth,zlo]),([-ax,y0,zhi],[ax,depth,z1])]
 if y1>ceil:regions.append(([x0,ceil,z0],[x1,y1,z1]))
 for i,(lo,hi)in enumerate(regions):candidate.append(box({**p,'name':p['name']+'/wing_pocket_'+str(i),'from':lo,'to':hi}))
parts=[]
# Bottom shared basis maps (x,y,z) -> (x,-z,y).
# Bottom-only Rx(+90deg), then local z -=3.36 yields horizontal wing at world y=.21.
for p in G['parts']:
 q=copy.deepcopy(p);q['vertices']=[[x,z,-y-3.36]for x,y,z in p['vertices']];parts.append(q)
adapters=[]
for sign in [-1,1]:
 p=box({**C['parts'][2],'name':'fixed_root_hanger_'+('left'if sign<0 else'right'),'from':([ -1.76,4.64,.72]if sign<0 else[1.12,4.64,.72]),'to':([-1.12,5.12,1.04]if sign<0 else[1.76,5.12,1.04])});p['group']='fixed-adapter';p['vertices']=[[x,z,-y]for x,y,z in p['vertices']];adapters.append(p)
D={'asset':'F05-02','version':'v02-bottom-structure-proposal','status':'pending-user-structure-review','gameIntegrated':False,'productionReady':False,'units':'1/16 block','textureSize':[128,64],'texture':'wing-atlas-128x64.png','nodes':C['nodes'],'parts':parts,'adapters':adapters,'originalWing':G['parts'],'originalBody':body,'candidateBody':candidate,'bottomRigidTransform':'(x,y,z) -> (x,z,-y-3.36), then original bottom node basis','bottomWingPlane':'horizontal world X/Z; fixed at y=.21','pocket':{'halfWidthX':ax,'minZ':zlo,'maxZ':zhi,'ceiling':ceil},'staticAcrossStates':True,'movingParts':0,'originalWingMeshes':28,'fixedAdapterMeshes':2,'bottomMinYBlocks':.13,'bottomMaxYBlocks':.32,'originalBottomMinYBlocks':-.55,'otherNodes':'unchanged v01 wing, basis and body','requirements':['F05-01 bottom-wing pocket display variant; matching installed and ghost geometry','No folding/deployment/scaling or new wing orientation physics','Actual client installation, terrain, riding and compound meshes need follow-up']}
for rel in ['source/structure-geometry.json','exports/wing-bottom-proposal.json']:save(rel,D)
for name in ['wing-atlas-128x64.png','wing-item-16.png','wing-item-32.png']:shutil.copyfile(R/'references'/name,R/'exports'/name)
def bb(ps,name):
 es=[]
 for p in ps:es.append({'name':p['name'],'uuid':str(uuid.uuid5(uuid.NAMESPACE_URL,'wildcraft/wing-v02/'+name+'/'+p['name'])),'type':'mesh','origin':[0,0,0],'vertices':{str(i):v for i,v in enumerate(p['vertices'])},'faces':{str(i):{'vertices':[str(j)for j in f['indices']],'uv':{str(j):[u*8,v*4]for j,(u,v)in zip(f['indices'],f['uvs'])},'texture':0}for i,f in enumerate(p['faces'])}})
 return {'meta':{'format_version':'4.10','model_format':'free','box_uv':False},'name':name,'resolution':{'width':128,'height':64},'elements':es,'outliner':[e['uuid']for e in es],'textures':[{'name':D['texture'],'id':'0','uuid':str(uuid.uuid5(uuid.NAMESPACE_URL,'wildcraft/fabric128')),'width':128,'height':64,'uv_width':128,'uv_height':64,'source':'data:image/png;base64,'+base64.b64encode((R/'exports'/D['texture']).read_bytes()).decode(),'mode':'bitmap'}]}
save('source/wing-bottom-v02.bbmodel',bb(parts+adapters,'Wildcraft fixed horizontal bottom wing v02'));save('source/chassis-wing-pocket-proposal.bbmodel',bb(candidate,'Wildcraft bottom wing pocket v02'));save('exports/chassis-wing-pocket-proposal.json',{'asset':'F05-01','status':'supporting-art-proposal-only','gameIntegrated':False,'units':'1/16 block','textureSize':[128,64],'useOnlyForBottomWing':True,'parts':candidate})
print('Full-size wing meshes',len(parts),'fixed hangers',len(adapters),'body',len(candidate),'protected files',len(protected))
