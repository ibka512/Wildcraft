"""Static, full-size bottom float art proposal; retains runtime and original sources."""
from pathlib import Path
import json,hashlib,shutil,datetime,uuid,base64
R=Path(__file__).resolve().parents[1];OLD=R.parent/'v01';P=R.parents[3]
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
records=[]
for rel in ['exports/buoyancy-geometry.json','exports/buoyancy-state.json','references/approved-machine-geometry.json','exports/shared-fabric-machine-atlas-128x64.png','source/shared-fabric-machine-atlas-128x64.aseprite','exports/buoyancy-item-16.png','exports/buoyancy-item-32.png','source/buoyancy-item-16.aseprite','source/buoyancy-item-32.aseprite']:
 p=OLD/rel;dest=R/'references'/p.name;shutil.copyfile(p,dest);records.append({'path':str(p.relative_to(P)),'copy':str(dest.relative_to(R)),'sha256':sha(p)})
for rel in ['src/main/java/dev/wildcraft/mechanics/MachineNodes.java','src/main/java/dev/wildcraft/mechanics/MachineEntity.java','src/client/java/dev/wildcraft/client/mechanics/MachineRenderer.java','art/ADOPTED-ASSETS.json','art/production-v2/F05-06/v02/exports/spring-bottom-proposal.json']:
 p=P/rel;dest=R/'references'/p.name;shutil.copyfile(p,dest);records.append({'path':rel,'copy':str(dest.relative_to(R)),'sha256':sha(p)})
(R/'docs/BASELINE.json').write_text(json.dumps({'checkedAt':datetime.datetime.now(datetime.timezone.utc).isoformat(),'files':records,'originalPackageFiles':{p.relative_to(OLD).as_posix():sha(p)for p in OLD.rglob('*')if p.is_file()and not p.name.startswith('._')and '__pycache__'not in p.parts}},ensure_ascii=False,indent=2)+'\n')
G=json.loads((R/'references/buoyancy-geometry.json').read_text());C=json.loads((R/'references/approved-machine-geometry.json').read_text());FM={'north':[0,1,2,3],'south':[5,4,7,6],'east':[1,5,6,2],'west':[4,0,3,7],'up':[3,2,6,7],'down':[4,5,1,0]}
def rot(v,n):x,y,z=v;return [(x,z,-y),(x,-z,y),(-x,y,-z),(x,y,z),(-z,y,x),(z,y,-x)][n]
def box(p):
 x0,y0,z0=p['from'];x1,y1,z1=p['to'];vs=[[x0,y0,z0],[x1,y0,z0],[x1,y1,z0],[x0,y1,z0],[x0,y0,z1],[x1,y0,z1],[x1,y1,z1],[x0,y1,z1]]
 if 'node'in p:vs=[[a+b for a,b in zip(rot(v,p['node']),C['nodes'][p['node']]['point'])]for v in vs]
 fs=[]
 for f,ids in FM.items():u0,v0,u1,v1=p['faces'][f]['uv'];fs.append({'indices':ids,'uvs':[[u0/2,v1],[u1/2,v1],[u1/2,v0],[u0/2,v0]]})
 return {'name':p['name'],'group':'body','vertices':vs,'faces':fs}
body=[box(p)for p in C['parts']];newbody=[]
# Full-size float requires its own wider rectangular cavity; spring cavity is not wide enough.
for p in C['parts']:
 if p.get('node')==1:continue
 if p['name']not in ['body_core','lower_plinth']:newbody.append(box(p));continue
 x0,y0,z0=p['from'];x1,y1,z1=p['to'];depth=min(y1,6.56);ax=5.52;az=3.6
 regions=[([x0,y0,z0],[-ax,depth,z1]),([ax,y0,z0],[x1,depth,z1]),([-ax,y0,z0],[ax,depth,-az]),([-ax,y0,az],[ax,depth,z1])]
 if y1>6.56:regions.append(([x0,6.56,z0],[x1,y1,z1]))
 for i,(lo,hi)in enumerate(regions):newbody.append(box({**p,'name':p['name']+'/float_pocket_'+str(i),'from':lo,'to':hi}))
for i,(lo,hi)in enumerate([([-6.12,.16,-4.2],[-5.52,.64,4.2]),([5.52,.16,-4.2],[6.12,.64,4.2]),([-5.52,.16,-4.2],[5.52,.64,-3.6]),([-5.52,.16,3.6],[5.52,.64,4.2])]):newbody.append(box({**C['parts'][2],'name':'float_pocket_lip_'+str(i),'from':lo,'to':hi}))
parts=[]
for p in G['parts']:
 q=json.loads(json.dumps(p));q['vertices']=[[x,y,z-6.56]for x,y,z in p['vertices']];parts.append(q)
D={'asset':'F05-09','version':'v02-structure-proposal','status':'pending-user-structure-review','gameIntegrated':False,'productionReady':False,'units':'1/16 block','textureSize':[128,64],'texture':'shared-fabric-machine-atlas-128x64.png','nodes':C['nodes'],'parts':parts,'originalFloat':G['parts'],'originalBody':body,'candidateBody':newbody,'bottomLocalOffset':[0,0,-6.56],'pocket':{'halfWidthX':5.52,'halfWidthZ':3.6,'ceiling':6.56},'staticAcrossStates':True,'movingParts':0,'bottomMinYBlocks':.01,'bottomMaxYBlocks':.39,'waterSource':'machine WATER height, not visual float immersion','existingCapacityPerDevice':1.75,'existingLiftFormula':'.07 * floats / mass * min(1, machineWaterHeight/.4)','otherNodes':'unchanged v01 float and original body','requirements':['F05-01 bottom-float pocket visual variant, also in matching ghost preview','Keep original renderer passive geometry; no deployment/airbag/retraction states','Runtime ground/water/overload/roll and real first/third-person validation']}
for rel in ['source/structure-geometry.json','exports/buoyancy-bottom-proposal.json']:(R/rel).write_text(json.dumps(D,ensure_ascii=False,indent=2)+'\n')
for name in ['shared-fabric-machine-atlas-128x64.png','buoyancy-item-16.png','buoyancy-item-32.png']:shutil.copyfile(R/'references'/name,R/'exports'/name)
def bbmodel(ps,name):
 es=[]
 for p in ps:es.append({'name':p['name'],'uuid':str(uuid.uuid5(uuid.NAMESPACE_URL,'wildcraft/float-v02/'+name+'/'+p['name'])),'type':'mesh','origin':[0,0,0],'vertices':{str(i):v for i,v in enumerate(p['vertices'])},'faces':{str(i):{'vertices':[str(j)for j in f['indices']],'uv':{str(j):[u*8,v*4]for j,(u,v)in zip(f['indices'],f['uvs'])},'texture':0}for i,f in enumerate(p['faces'])}})
 return {'meta':{'format_version':'4.10','model_format':'free','box_uv':False},'name':name,'resolution':{'width':128,'height':64},'elements':es,'outliner':[e['uuid']for e in es],'textures':[{'name':D['texture'],'id':'0','uuid':str(uuid.uuid5(uuid.NAMESPACE_URL,'wildcraft/fabric128')),'width':128,'height':64,'uv_width':128,'uv_height':64,'source':'data:image/png;base64,'+base64.b64encode((R/'exports'/D['texture']).read_bytes()).decode(),'mode':'bitmap'}]}
(R/'source/buoyancy-bottom-v02.bbmodel').write_text(json.dumps(bbmodel(parts,'Wildcraft bottom static float v02'),ensure_ascii=False,indent=2)+'\n');(R/'source/chassis-float-pocket-proposal.bbmodel').write_text(json.dumps(bbmodel(newbody,'Wildcraft bottom float pocket v02'),ensure_ascii=False,indent=2)+'\n')
(R/'exports/chassis-float-pocket-proposal.json').write_text(json.dumps({'asset':'F05-01','status':'supporting-art-proposal-only','gameIntegrated':False,'units':'1/16 block','textureSize':[128,64],'useOnlyForBottomFloat':True,'parts':newbody},ensure_ascii=False,indent=2)+'\n')
print(len(parts),'full-size static float meshes,',len(newbody),'pocket-body meshes; zero new texture pixels.')
