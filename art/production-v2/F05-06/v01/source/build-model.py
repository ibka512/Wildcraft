"""Spring geometry in original node-local model units; reuse approved shared atlas."""
from pathlib import Path
import json,math,uuid,base64
R=Path(__file__).resolve().parents[1];U=json.loads((R/'references/uv-regions.json').read_text())['regions'];parts=[]
def cube(name,a,b,key,override=None,group='housing'):
 x0,y0,z0=a;x1,y1,z1=b;vv=[[x0,y0,z0],[x1,y0,z0],[x1,y1,z0],[x0,y1,z0],[x0,y0,z1],[x1,y0,z1],[x1,y1,z1],[x0,y1,z1]];fm={'north':[0,1,2,3],'south':[5,4,7,6],'east':[1,5,6,2],'west':[4,0,3,7],'up':[3,2,6,7],'down':[4,5,1,0]};faces=[]
 for f,ids in fm.items():
  keyf=(override or {}).get(f,key);u0,v0,u1,v1=U[keyf]['uv'];faces.append(dict(name=f,indices=ids,uvs=[[u0,v1],[u1,v1],[u1,v0],[u0,v0]],materialRegion=keyf))
 parts.append(dict(name=name,group=group,kind='cuboid',vertices=vv,faces=faces))

cube('fixed_base',[-3.52,-3.52,.64],[3.52,3.52,1.6],'steel',{'north':'joint','south':'housing'},group='fixed')
for i,x in enumerate([-1,.44]):cube('rear_contact_'+str(i),[x,-.8,.60],[x+.56,.8,.64],'copper',group='fixed')
for i,(x,y)in enumerate([(-3.28,-3.28),(2.80,-3.28),(-3.28,2.80),(2.80,2.80)]):
 cube('base_stud_'+str(i),[x,y,1.4],[x+.48,y+.48,1.72],'copper',group='fixed')
for i,(a,b)in enumerate([([-.56,-.56,1.6],[.56,-.30,4.64]),([-.56,.30,1.6],[.56,.56,4.64]),([-.56,-.30,1.6],[-.30,.30,4.64]),([.30,-.30,1.6],[.56,.30,4.64])]):cube('fixed_guide_'+str(i),a,b,'joint',group='fixed')
cube('telescoping_rod',[-.28,-.28,.80],[.28,.28,4.8],'steel',group='moving')
cube('push_plate',[-3.52,-3.52,4.8],[3.52,3.52,5.44],'steel',{'north':'joint'},group='moving')
cube('recessed_green_pad',[-2.94,-2.94,5.44],[2.94,2.94,5.70],'housing',group='moving')
for i,(x,y)in enumerate([(-3.28,-3.28),(2.8,-3.28),(-3.28,2.8),(2.8,2.8)]):cube('plate_stud_'+str(i),[x,y,5.44],[x+.48,y+.48,5.76],'copper',group='moving')
r=1.94;k=math.sqrt(2)-1;pts=[(r,k*r),(k*r,r),(-k*r,r),(-r,k*r),(-r,-k*r),(-k*r,-r),(k*r,-r),(r,-k*r)]
for i in range(24):
 a=pts[i%8];b=pts[(i+1)%8];dx=b[0]-a[0];dy=b[1]-a[1];l=math.hypot(dx,dy);tx=dx/l;ty=dy/l;nx=-ty;ny=tx;vv=[];qs=[]
 for zoff in [-.24,.24]:
  for endpoint,side in [(0,-1),(1,-1),(1,1),(0,1)]:
   x,y=a if endpoint==0 else b;q=(i+endpoint)/24;extension=(-.08 if endpoint==0 else .08);vv.append([x+tx*extension+nx*side*.24,y+ty*extension+ny*side*.24,1.84+2.72*q+zoff]);qs.append(q)
 fm=[[0,1,2,3],[5,4,7,6],[1,5,6,2],[4,0,3,7],[3,2,6,7],[4,5,1,0]];faces=[];u0,v0,u1,v1=U['steel']['uv']
 for j,ids in enumerate(fm):faces.append(dict(name='face'+str(j),indices=ids,uvs=[[u0,v1],[u1,v1],[u1,v0],[u0,v0]],materialRegion='steel'))
 parts.append(dict(name='coil_segment_'+str(i),group='coil',kind='helical_prism',vertices=vv,extendedVertices=[[x,y,z+3.2*q]for (x,y,z),q in zip(vv,qs)],deformationWeights=qs,faces=faces))
# Seats join helix ends to fixed base and moving plate.
x,y=pts[0];cube('bottom_coil_seat',[x-.36,y-.36,1.6],[x+.36,y+.36,1.84],'copper',group='fixed');cube('top_coil_seat',[x-.36,y-.36,4.56],[x+.36,y+.36,4.8],'copper',group='moving')
for p in parts:
 if p['group']=='moving':p['extendedVertices']=[[x,y,z+3.2]for x,y,z in p['vertices']]
 elif p['group']=='fixed':p['extendedVertices']=p['vertices']
 center=[sum(v[i]for v in p['vertices'])/len(p['vertices'])for i in range(3)]
 for f in p['faces']:
  v=[p['vertices'][i]for i in f['indices']];a=[v[1][i]-v[0][i]for i in range(3)];b=[v[2][i]-v[0][i]for i in range(3)];n=[a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]];fc=[sum(w[i]for w in v)/len(v)for i in range(3)]
  if sum(n[i]*(fc[i]-center[i])for i in range(3))<0:f['indices'].reverse();f['uvs'].reverse()
G=dict(asset='F05-06',version='v01',status='draft_pending_review',integrated=False,units='1/16 block',textureSize=[64,64],texture='shared-machine-atlas-64.png',parts=parts,pushPlatePivot=[0,0,4.8],travel=3.2,wireThickness=.48,coilTurns=3,coilSegments=24,poseRule='!ghost && cooldown > 30 ? extended : compressed',cooldownWorldTicks=40,energyPerReadySpring=20,thrustLocal=[0,0,-1],localEnvelopeCompressed=dict(min=[-3.52,-3.52,.60],max=[3.52,3.52,5.76]),localEnvelopeExtended=dict(min=[-3.52,-3.52,.60],max=[3.52,3.52,8.96]))
for name in ['source/geometry-and-uv.json','exports/spring-geometry.json']:(R/name).write_text(json.dumps(G,indent=2)+'\n')
for state in ['compressed','extended']:
 es=[];groups={}
 for p in parts:
  vs=p['vertices']if state=='compressed'else p['extendedVertices'];uid=str(uuid.uuid4());faces={str(i):dict(vertices=[str(j)for j in f['indices']],uv={str(j):[v*4 for v in uv]for j,uv in zip(f['indices'],f['uvs'])},texture=0)for i,f in enumerate(p['faces'])};es.append(dict(name=p['name'],uuid=uid,type='mesh',vertices={str(i):v for i,v in enumerate(vs)},faces=faces,origin=[0,0,4.8]if p['group']=='moving'else[0,0,0]));groups.setdefault(p['group'],[]).append(uid)
 bb=dict(meta=dict(format_version='4.10',model_format='free',box_uv=False),name='Wildcraft spring '+state+' v01',resolution=dict(width=64,height=64),elements=es,outliner=[dict(name=k,uuid=str(uuid.uuid4()),origin=[0,0,4.8]if k=='moving'else[0,0,0],children=v)for k,v in groups.items()],textures=[dict(name=G['texture'],id='0',uuid=str(uuid.uuid4()),width=64,height=64,uv_width=64,uv_height=64,source='data:image/png;base64,'+base64.b64encode((R/'exports'/G['texture']).read_bytes()).decode(),mode='bitmap')]);(R/'source'/('spring-'+state+'-v01.bbmodel')).write_text(json.dumps(bb,indent=2)+'\n')
(R/'exports/spring-state-and-motion.json').write_text(json.dumps(dict(asset='F05-06',status='draft_pending_review',integrated=False,poseRule=G['poseRule'],fixedGroup='fixed',translateGroup='moving',deformGroup='coil',pushPlateTravel=3.2,coilDeformation='Z += 3.2 * endpointQ; square wire cross-section constant',cooldown=40,extendedCooldownRange=[31,40],compressedCooldownRange=[0,30],ghostPose='compressed',activeBitDoesNotDrivePose=True,clientArmedAvailable=False,rearm='stopped && grounded && previous cooldown == 0',fire='enabled && grounded && armed && cooldown == 0 && full shared battery payment',energy=20,extraPhysics=False),indent=2)+'\n');print(len(parts),'parts, shared64 atlas; 24 constant-thickness coil segments')
