"""Wheel geometry in original node-local model units; reuse approved shared atlas."""
from pathlib import Path
import json,math,uuid,base64
R=Path(__file__).resolve().parents[1];U=json.loads((R/'references/uv-regions.json').read_text())['regions'];parts=[]
def cube(name,a,b,key,override=None,group='housing'):
 x0,y0,z0=a;x1,y1,z1=b;vv=[[x0,y0,z0],[x1,y0,z0],[x1,y1,z0],[x0,y1,z0],[x0,y0,z1],[x1,y0,z1],[x1,y1,z1],[x0,y1,z1]];fm={'north':[0,1,2,3],'south':[5,4,7,6],'east':[1,5,6,2],'west':[4,0,3,7],'up':[3,2,6,7],'down':[4,5,1,0]};faces=[]
 for f,ids in fm.items():
  keyf=(override or {}).get(f,key);u0,v0,u1,v1=U[keyf]['uv'];faces.append(dict(name=f,indices=ids,uvs=[[u0,v1],[u1,v1],[u1,v0],[u0,v0]],materialRegion=keyf))
 parts.append(dict(name=name,group=group,kind='cuboid',vertices=vv,faces=faces))

cube('fixed_mount_plate',[-1.72,-1.72,.64],[1.72,1.72,1.2],'housing',{'north':'joint','south':'steel'},group='fixed')
for i,x in enumerate([-1,.44]):cube('rear_contact_'+str(i),[x,-.8,.60],[x+.56,.8,.64],'copper',group='fixed')
for i,(x,y)in enumerate([(-1.56,-1.56),(1.16,-1.56),(-1.56,1.16),(1.16,1.16)]):cube('mount_stud_'+str(i),[x,y,1.2],[x+.40,y+.40,1.4],'copper',group='fixed')
cube('fixed_bearing',[-.88,-.88,1.2],[.88,.88,1.76],'steel',group='fixed');cube('fixed_axle',[-.44,-.44,1.76],[.44,.44,3.88],'joint',group='fixed')
def wedge(name,outer,inner,z0,z1,t0,t1,key,outerKey=None):
 xy=[(outer*math.cos(t0),outer*math.sin(t0)),(outer*math.cos(t1),outer*math.sin(t1)),(inner*math.cos(t1),inner*math.sin(t1)),(inner*math.cos(t0),inner*math.sin(t0))];vv=[[x,y,z]for z in [z0,z1]for x,y in xy];faces=[]
 for i,ids in enumerate([[0,1,2,3],[5,4,7,6],[0,4,5,1],[1,5,6,2],[2,6,7,3],[3,7,4,0]]):
  material=outerKey if i==2 and outerKey else key;u0,v0,u1,v1=U[material]['uv'];faces.append(dict(name='face'+str(i),indices=ids,uvs=[[u0,v1],[u1,v1],[u1,v0],[u0,v0]],materialRegion=material))
 parts.append(dict(name=name,group='rotating',kind='annular_segment',vertices=vv,faces=faces))
for i in range(16):
 t0=i*math.tau/16;t1=(i+1)*math.tau/16
 wedge('continuous_tread_core_'+str(i),3.78,3.08,1.6,3.84,t0,t1,'recess')
 wedge('raised_tread_'+str(i),4,3.77,1.6,3.84,t0+.026,t1-.026,'recess','vent')
 wedge('steel_rim_'+str(i),3.10,2.70,3.76,4.00,t0,t1,'steel')
# Four structural spokes leave real openings between axle and inner rim.
for i in range(4):
 cube('spoke_'+str(i),[.82,-.36,2.16],[2.90,.36,3.90],'steel',group='rotating');p=parts[-1];angle=i*math.pi/2;p['vertices']=[[x*math.cos(angle)-y*math.sin(angle),x*math.sin(angle)+y*math.cos(angle),z]for x,y,z in p['vertices']]
 cube('copper_spoke_inlay_'+str(i),[1.48,-.27,3.90],[2.72,.27,4.12],'copper',group='rotating');p=parts[-1];p['vertices']=[[x*math.cos(angle)-y*math.sin(angle),x*math.sin(angle)+y*math.cos(angle),z]for x,y,z in p['vertices']]
def cylinder(name,r,z0,z1,key):
 xy=[(r*math.cos(i*math.pi/4),r*math.sin(i*math.pi/4))for i in range(8)];vv=[[x,y,z]for z in [z0,z1]for x,y in xy];idslist=[list(range(8)),list(reversed(range(8,16)))]+[[i,(i+1)%8,(i+1)%8+8,i+8]for i in range(8)];u0,v0,u1,v1=U[key]['uv'];faces=[]
 for i,ids in enumerate(idslist):
  uv=[[u0+(vv[j][0]+r)/(2*r)*(u1-u0),v1-(vv[j][1]+r)/(2*r)*(v1-v0)]for j in ids]if len(ids)>4 else[[u0,v1],[u1,v1],[u1,v0],[u0,v0]];faces.append(dict(name='face'+str(i),indices=ids,uvs=uv,materialRegion=key))
 parts.append(dict(name=name,group='rotating',kind='octagonal_hub',vertices=vv,faces=faces))
cylinder('steel_hub',1.08,1.60,4.10,'steel');cylinder('copper_axle_cap',.68,4.10,4.32,'copper')
for p in parts:
 center=[sum(v[i]for v in p['vertices'])/len(p['vertices'])for i in range(3)]
 for f in p['faces']:
  v=[p['vertices'][i]for i in f['indices']];a=[v[1][i]-v[0][i]for i in range(3)];b=[v[2][i]-v[0][i]for i in range(3)];n=[a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]];fc=[sum(w[i]for w in v)/len(v)for i in range(3)]
  if sum(n[i]*(fc[i]-center[i])for i in range(3))<0:f['indices'].reverse();f['uvs'].reverse()
G=dict(asset='F05-07',version='v01',status='draft_pending_review',adopted=False,integrated=False,units='1/16 block',textureSize=[64,64],texture='shared-machine-atlas-64.png',parts=parts,rotationPivot=[0,0,4],rotationAxis=[0,0,1],rotationRateRadiansPerWorldTick=.4,rotationRule='active && !ghost ? ageInTicks * .4 : 0',outerRadius=4,treadSegments=16,localEnvelope=dict(min=[-4,-4,.6],max=[4,4,4.32]),energyPerGroundDrivenWheelPerWorldTick=1,reverseInputChangesVisualDirection=False,rotationDistanceBased=False,sideMountNominalGroundGap=1.2,groundContactRule='body.onGround; not independently simulated wheels')
for name in ['source/geometry-and-uv.json','exports/wheel-geometry.json']:(R/name).write_text(json.dumps(G,indent=2)+'\n')
es=[];groups={}
for p in parts:
 uid=str(uuid.uuid4());faces={str(i):dict(vertices=[str(j)for j in f['indices']],uv={str(j):[v*4 for v in uv]for j,uv in zip(f['indices'],f['uvs'])},texture=0)for i,f in enumerate(p['faces'])};es.append(dict(name=p['name'],uuid=uid,type='mesh',vertices={str(i):v for i,v in enumerate(p['vertices'])},faces=faces,origin=G['rotationPivot']if p['group']=='rotating'else[0,0,0]));groups.setdefault(p['group'],[]).append(uid)
bb=dict(meta=dict(format_version='4.10',model_format='free',box_uv=False),name='Wildcraft wheel F05-07 v01',resolution=dict(width=64,height=64),elements=es,outliner=[dict(name=k,uuid=str(uuid.uuid4()),origin=G['rotationPivot']if k=='rotating'else[0,0,0],children=v)for k,v in groups.items()],textures=[dict(name=G['texture'],id='0',uuid=str(uuid.uuid4()),width=64,height=64,uv_width=64,uv_height=64,source='data:image/png;base64,'+base64.b64encode((R/'exports'/G['texture']).read_bytes()).decode(),mode='bitmap')]);(R/'source/wheel-v01.bbmodel').write_text(json.dumps(bb,indent=2)+'\n')
(R/'exports/wheel-motion.json').write_text(json.dumps(dict(asset='F05-07',version='v01',status='draft_pending_review',adopted=False,integrated=False,pivot=G['rotationPivot'],axis=G['rotationAxis'],angle=G['rotationRule'],fixedGroup='fixed',rotatingGroup='rotating',rate=.4,ghostAngle=0,stopAngle=0,sourceRenderer='current tire box fixed, face marker rotates',artAdaptation='apply existing face-marker angle to tread, rim, spokes and cap together',notDistanceBased=True,noReverseVisualDirection=True,independentWheelCollision=False,suspension=False,newPhysics=False),indent=2)+'\n');print(len(parts),'parts',len(groups['fixed']),'fixed',len(groups['rotating']),'rotating; shared64')
