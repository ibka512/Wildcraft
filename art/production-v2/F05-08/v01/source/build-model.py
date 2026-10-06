"""Stabilizer geometry in original node-local model units; reuse approved shared atlas."""
from pathlib import Path
import json,math,uuid,base64
R=Path(__file__).resolve().parents[1];U=json.loads((R/'references/uv-regions.json').read_text())['regions'];parts=[]
def cube(name,a,b,key,override=None,group='housing'):
 x0,y0,z0=a;x1,y1,z1=b;vv=[[x0,y0,z0],[x1,y0,z0],[x1,y1,z0],[x0,y1,z0],[x0,y0,z1],[x1,y0,z1],[x1,y1,z1],[x0,y1,z1]];fm={'north':[0,1,2,3],'south':[5,4,7,6],'east':[1,5,6,2],'west':[4,0,3,7],'up':[3,2,6,7],'down':[4,5,1,0]};faces=[]
 for f,ids in fm.items():
  keyf=(override or {}).get(f,key);u0,v0,u1,v1=U[keyf]['uv'];faces.append(dict(name=f,indices=ids,uvs=[[u0,v1],[u1,v1],[u1,v0],[u0,v0]],materialRegion=keyf))
 parts.append(dict(name=name,group=group,kind='cuboid',vertices=vv,faces=faces))

# Keep original chamfered body within X/Y2.88/4.16, Z.64..3.52.
xy=[(2.80,3.36),(2.08,4.08),(-2.08,4.08),(-2.80,3.36),(-2.80,-3.36),(-2.08,-4.08),(2.08,-4.08),(2.80,-3.36)];vv=[[x,y,z]for z in [.80,3.52]for x,y in xy];faces=[]
for i,ids in enumerate([list(range(8)),list(reversed(range(8,16)))]+[[i,(i+1)%8,(i+1)%8+8,i+8]for i in range(8)]):
 key='joint'if i==0 else'housing';u0,v0,u1,v1=U[key]['uv'];uv=[[u0+(vv[j][0]+2.88)/5.76*(u1-u0),v1-(vv[j][1]+4.16)/8.32*(v1-v0)]for j in ids]if len(ids)>4 else[[u0,v1],[u1,v1],[u1,v0],[u0,v0]];faces.append(dict(name='face'+str(i),indices=ids,uvs=uv,materialRegion=key))
parts.append(dict(name='chamfered_housing',group='fixed',kind='octagonal_prism',vertices=vv,faces=faces))
cube('rear_mount_plate',[-1.72,-2.72,.64],[1.72,2.72,.96],'joint',{'north':'steel'},group='fixed')
for i,x in enumerate([-1,.44]):cube('rear_contact_'+str(i),[x,-.8,.60],[x+.56,.8,.64],'copper',group='fixed')
for i,(x,y)in enumerate([(-2.36,-3.68),(1.72,-3.68),(-2.36,3.04),(1.72,3.04)]):
 cube('corner_brace_'+str(i),[x,y,3.36],[x+.64,y+.64,3.60],'steel',group='fixed');cube('copper_fastener_'+str(i),[x+.20,y+.20,3.60],[x+.44,y+.44,3.68],'copper',group='fixed')
for i,y in enumerate([-4.16,3.52]):cube('steel_end_cap_'+str(i),[-1.9,y,.76],[1.9,y+.64,3.60],'steel',group='fixed')
for i,(x0,x1)in enumerate([(-2.88,-1.55),(1.55,2.88)]):cube('copper_belt_front_'+str(i),[x0,-.40,3.52],[x1,.40,3.68],'copper',group='fixed')
for i,(x0,x1)in enumerate([(-2.88,-2.72),(2.72,2.88)]):cube('copper_belt_side_'+str(i),[x0,-.40,.96],[x1,.40,3.52],'copper',group='fixed')
for i,(lo,hi)in enumerate([([-1.55,-1.55,3.52],[1.55,-1.28,4.00]),([-1.55,1.28,3.52],[1.55,1.55,4.00]),([-1.55,-1.28,3.52],[-1.28,1.28,4.00]),([1.28,-1.28,3.52],[1.55,1.28,4.00])]):cube('indicator_frame_'+str(i),lo,hi,'recess',group='fixed')
cube('state_indicator',[-1.28,-1.28,3.68],[1.28,1.28,3.98],'housing',group='indicator')
cube('horizontal_level_mark',[-.92,-.08,3.98],[.92,.08,4.00],'steel',group='fixed');cube('center_level_tick',[-.10,-.32,3.98],[.10,.32,4.00],'steel',group='fixed')
for p in parts:
 center=[sum(v[i]for v in p['vertices'])/len(p['vertices'])for i in range(3)]
 for f in p['faces']:
  v=[p['vertices'][i]for i in f['indices']];a=[v[1][i]-v[0][i]for i in range(3)];b=[v[2][i]-v[0][i]for i in range(3)];n=[a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]];fc=[sum(w[i]for w in v)/len(v)for i in range(3)]
  if sum(n[i]*(fc[i]-center[i])for i in range(3))<0:f['indices'].reverse();f['uvs'].reverse()
G=dict(asset='F05-08',version='v01',status='draft_pending_review',adopted=False,integrated=False,units='1/16 block',textureSize=[64,64],texture='shared-machine-atlas-64.png',parts=parts,localEnvelope=dict(min=[-2.88,-4.16,.60],max=[2.88,4.16,4]),movingParts=0,indicatorColors=dict(inactive='#607775',active='#8DD5B7',powerFailureReference='#DB8A55'),indicatorMethod='one fixed mesh; state RGB material/vertex colour; no texture animation or emission',actualStateRule='ghost: uniform preview; active: mint; otherwise muted',powerFailureRule='reference only; explicit authoritative failure reason required; never infer from active0',energyPerWorldTick=2,bodyRollRule='any actual installed active kind7 stabilizer ->0; otherwise original lateral velocity roll',lateralDampingFactor=.78,stabilizersDoNotPreventFalls=True)
for name in ['source/geometry-and-uv.json','exports/stabilizer-geometry.json']:(R/name).write_text(json.dumps(G,indent=2)+'\n')
es=[];groups={}
for p in parts:
 uid=str(uuid.uuid4());faces={str(i):dict(vertices=[str(j)for j in f['indices']],uv={str(j):[v*4 for v in uv]for j,uv in zip(f['indices'],f['uvs'])},texture=0)for i,f in enumerate(p['faces'])};e=dict(name=p['name'],uuid=uid,type='mesh',vertices={str(i):v for i,v in enumerate(p['vertices'])},faces=faces,origin=[0,0,0]);
 if p['group']=='indicator':e['wildcraft_state_material']=G['indicatorColors']
 es.append(e);groups.setdefault(p['group'],[]).append(uid)
bb=dict(meta=dict(format_version='4.10',model_format='free',box_uv=False),name='Wildcraft stabilizer F05-08 v01',resolution=dict(width=64,height=64),elements=es,outliner=[dict(name=k,uuid=str(uuid.uuid4()),origin=[0,0,0],children=v)for k,v in groups.items()],textures=[dict(name=G['texture'],id='0',uuid=str(uuid.uuid4()),width=64,height=64,uv_width=64,uv_height=64,source='data:image/png;base64,'+base64.b64encode((R/'exports'/G['texture']).read_bytes()).decode(),mode='bitmap')]);(R/'source/stabilizer-v01.bbmodel').write_text(json.dumps(bb,indent=2)+'\n')
(R/'exports/stabilizer-state.json').write_text(json.dumps(dict(asset='F05-08',version='v01',status='draft_pending_review',adopted=False,integrated=False,actualStates=['inactive','active'],colors=G['indicatorColors'],activeSource='per-node active bit',inactiveDoesNotMeanNoPower=True,powerFailureReference=dict(implemented=False,designOnly=True,requires='explicit authoritative payment failure reason',cannotInferFrom=['active=0','working=false','enabled alone','some battery energy','zero battery energy without enabled/context']),materialTechnique='state colour on fixed indicator only',emission=0,halo=False,pulsing=False,movingModelParts=0,ghost=dict(color='#5DD7B7',alpha=136/255,noStateColor=True),bodyRollRule=G['bodyRollRule'],energyPerWorldTick=2),indent=2)+'\n');print(len(parts),'fixed geometry parts;1 state-colour indicator;shared64')
