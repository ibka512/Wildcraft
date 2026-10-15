"""Reproducible art proposal, never changes runtime meshes or gameplay."""
from pathlib import Path
import json,hashlib,shutil,datetime,uuid,base64
R=Path(__file__).resolve().parents[1];OLD=R.parent/'v01';P=R.parents[3]
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
refs=['exports/spring-geometry.json','references/approved-machine-geometry.json','exports/shared-machine-atlas-64.png','source/shared-machine-atlas-64.aseprite','references/uv-regions.json']
records=[]
for rel in refs:
 p=OLD/rel;dest=R/'references'/p.name;shutil.copyfile(p,dest);records.append({'path':str(p.relative_to(P)),'copy':str(dest.relative_to(R)),'sha256':sha(p)})
for rel in ['src/main/java/dev/wildcraft/mechanics/MachineNodes.java','src/main/java/dev/wildcraft/mechanics/MachineEntity.java','src/main/java/dev/wildcraft/mechanics/SpringData.java','src/client/java/dev/wildcraft/client/mechanics/MachineRenderer.java','art/ADOPTED-ASSETS.json']:
 p=P/rel;dest=R/'references'/p.name;shutil.copyfile(p,dest);records.append({'path':rel,'copy':str(dest.relative_to(R)),'sha256':sha(p)})
(R/'docs/BASELINE.json').write_text(json.dumps({'checkedAt':datetime.datetime.now(datetime.timezone.utc).isoformat(),'files':records,'originalPackageFiles':{str(p.relative_to(OLD)):sha(p) for p in OLD.rglob('*') if p.is_file() and not p.name.startswith('._') and '__pycache__'not in p.parts}},ensure_ascii=False,indent=2)+'\n')
G=json.loads((R/'references/spring-geometry.json').read_text());C=json.loads((R/'references/approved-machine-geometry.json').read_text())
FM={'north':[0,1,2,3],'south':[5,4,7,6],'east':[1,5,6,2],'west':[4,0,3,7],'up':[3,2,6,7],'down':[4,5,1,0]}
def rot(v,n):x,y,z=v;return [(x,z,-y),(x,-z,y),(-x,y,-z),(x,y,z),(-z,y,x),(z,y,-x)][n]
def box(p):
 x0,y0,z0=p['from'];x1,y1,z1=p['to'];vs=[[x0,y0,z0],[x1,y0,z0],[x1,y1,z0],[x0,y1,z0],[x0,y0,z1],[x1,y0,z1],[x1,y1,z1],[x0,y1,z1]]
 if 'node'in p:vs=[[a+b for a,b in zip(rot(v,p['node']),C['nodes'][p['node']]['point'])] for v in vs]
 fs=[]
 for f,ids in FM.items():u0,v0,u1,v1=p['faces'][f]['uv'];fs.append({'indices':ids,'uvs':[[u0,v1],[u1,v1],[u1,v0],[u0,v0]]})
 return {'name':p['name'],'group':'body','vertices':vs,'faces':fs}
body=[box(p) for p in C['parts']]
newbody=[]
# Carve a real square pocket through the lower plinth and into core. Do not merely hide overlapping faces.
for p in C['parts']:
 if p.get('node')==1:continue
 if p['name']not in ['body_core','lower_plinth']:newbody.append(box(p));continue
 x0,y0,z0=p['from'];x1,y1,z1=p['to'];depth=min(y1,5.6);a=3.9
 regions=[([x0,y0,z0],[-a,depth,z1]),([a,y0,z0],[x1,depth,z1]),([-a,y0,z0],[a,depth,-a]),([-a,y0,a],[a,depth,z1])]
 if y1>5.6:regions.append(([x0,5.6,z0],[x1,y1,z1]))
 for i,(lo,hi)in enumerate(regions):q={**p,'name':p['name']+'/pocket_'+str(i),'from':lo,'to':hi};newbody.append(box(q))
# Four shallow lips outside the opening; all remain above the collision bottom.
for i,(lo,hi)in enumerate([([-4.5,.16,-4.5],[-3.9,.64,4.5]),([3.9,.16,-4.5],[4.5,.64,4.5]),([-3.9,.16,-4.5],[3.9,.64,-3.9]),([-3.9,.16,3.9],[3.9,.64,4.5])]):
 q={**C['parts'][2],'name':'pocket_lip_'+str(i),'from':lo,'to':hi};newbody.append(box(q))
parts=[]
for p in G['parts']:
 q=json.loads(json.dumps(p));q['vertices']=[[x,y,z-5.92] for x,y,z in p['vertices']];q['extendedVertices']=[[x,y,z-5.92]for x,y,z in p['extendedVertices']];parts.append(q)
D={'asset':'F05-06','version':'v02-structure-proposal','status':'pending-user-structure-review','gameIntegrated':False,'productionReady':False,'units':'1/16 block','textureSize':[64,64],'texture':'shared-machine-atlas-64.png','nodes':C['nodes'],'parts':parts,'originalSpring':G['parts'],'originalBody':body,'candidateBody':newbody,'bottomLocalOffset':[0,0,-5.92],'travel':3.2,'movingPivot':[0,0,-1.12],'pocket':{'halfWidth':3.9,'ceiling':5.6},'compressedBottomClearanceBlocks':.01,'fullExtendedBottomMinYBlocks':-.19,'futureVisualClearanceContract':'travelBlocks = min(requestedTravelBlocks, max(0, clearanceBelowBodyBlocks + 0.01)); proposed renderer only, not implemented','existingRequestedPose':'!ghost && cooldown > 30 ? extended : compressed','otherNodes':'use unchanged v01 spring and unchanged body','requirements':['F05-01 bottom-mounted visual pocket variant and matching ghost preview','Client renderer ground-clearance query for the real swept footprint; capped moving/coil art displacement','Real client grounded/airborne/landing and first/third-person validation; no physical thrust or hitbox changes']}
for rel in ['source/structure-geometry.json','exports/spring-bottom-proposal.json']:(R/rel).write_text(json.dumps(D,ensure_ascii=False,indent=2)+'\n')
shutil.copyfile(R/'references/shared-machine-atlas-64.png',R/'exports/shared-machine-atlas-64.png')
# Editable free-mesh source in native node-local axes, separate pose snapshots.
for state in ['stowed','extended']:
 els=[];groups={}
 for p in parts:
  vs=p['vertices']if state=='stowed'else p['extendedVertices'];uid=str(uuid.uuid4());els.append({'name':p['name'],'uuid':uid,'type':'mesh','vertices':{str(i):v for i,v in enumerate(vs)},'faces':{str(i):{'vertices':[str(j)for j in f['indices']],'uv':{str(j):[u*4,v*4] for j,(u,v)in zip(f['indices'],f['uvs'])},'texture':0}for i,f in enumerate(p['faces'])},'origin':D['movingPivot']if p['group']=='moving'else[0,0,0]});groups.setdefault(p['group'],[]).append(uid)
 bb={'meta':{'format_version':'4.10','model_format':'free','box_uv':False},'name':'Wildcraft bottom spring structure '+state,'resolution':{'width':64,'height':64},'elements':els,'outliner':[{'name':k,'uuid':str(uuid.uuid4()),'origin':D['movingPivot']if k=='moving'else[0,0,0],'children':v}for k,v in groups.items()],'textures':[{'name':D['texture'],'id':'0','uuid':str(uuid.uuid4()),'width':64,'height':64,'uv_width':64,'uv_height':64,'source':'data:image/png;base64,'+base64.b64encode((R/'exports'/D['texture']).read_bytes()).decode(),'mode':'bitmap'}]}
 (R/'source'/('spring-bottom-'+state+'.bbmodel')).write_text(json.dumps(bb,ensure_ascii=False,indent=2)+'\n')
print('44 original-size spring meshes; actual pocket-body proposal; v01 preserved. Not runtime ready.')
