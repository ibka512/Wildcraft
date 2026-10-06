"""Fan geometry in original node-local model units; reuse approved shared atlas."""
from pathlib import Path
import json,math,uuid,base64
R=Path(__file__).resolve().parents[1];U=json.loads((R/'references/uv-regions.json').read_text())['regions'];parts=[]
def cube(name,a,b,key,override=None,group='housing'):
 x0,y0,z0=a;x1,y1,z1=b;vv=[[x0,y0,z0],[x1,y0,z0],[x1,y1,z0],[x0,y1,z0],[x0,y0,z1],[x1,y0,z1],[x1,y1,z1],[x0,y1,z1]];fm={'north':[0,1,2,3],'south':[5,4,7,6],'east':[1,5,6,2],'west':[4,0,3,7],'up':[3,2,6,7],'down':[4,5,1,0]};faces=[]
 for f,ids in fm.items():
  keyf=(override or {}).get(f,key);u0,v0,u1,v1=U[keyf]['uv'];faces.append(dict(name=f,indices=ids,uvs=[[u0,v1],[u1,v1],[u1,v0],[u0,v0]],materialRegion=keyf))
 parts.append(dict(name=name,group=group,kind='cuboid',vertices=vv,faces=faces))
# Existing housing +- .29 block, start .04; open center, not a solid square.
for name,a,b in [('top',[-4.64,3.84,.64],[4.64,4.64,2.72]),('bottom',[-4.64,-4.64,.64],[4.64,-3.84,2.72]),('left',[-4.64,-3.84,.64],[-3.84,3.84,2.72]),('right',[3.84,-3.84,.64],[4.64,3.84,2.72])]:cube('frame_'+name,a,b,'housing',{'south':'edge','north':'joint'})
for i,(x,y) in enumerate([(-4.64,-4.64),(3.2,-4.64),(-4.64,3.2),(3.2,3.2)]):
 cube('corner_steel_'+str(i),[x,y,2.70],[x+1.44,y+1.44,2.9],'steel');cube('corner_copper_'+str(i),[x+.4,y+.4,2.9],[x+1.04,y+1.04,3.05],'copper')
cube('rear_horizontal_support',[-3.84,-.24,.64],[3.84,.24,1.1],'joint');cube('rear_vertical_support',[-.24,-3.84,.64],[.24,3.84,1.1],'joint')
cube('rear_square_mount',[-2.08,-2.08,.64],[2.08,2.08,1.12],'recess',{'north':'port'})
for i,x in enumerate([-1.0,.44]):cube('rear_contact_'+str(i),[x,-.8,.60],[x+.56,.8,.64],'copper')
cube('fixed_bearing',[-.85,-.85,1.12],[.85,.85,1.76],'steel')
# Four equal flat swept paddles; arbitrary-angle rotation at original axle z=.11 block.
shape=[[.55,-.65],[3.3,-.55],[3.5,.85],[2.65,1.75],[1.1,1.45],[.55,.6]]
def rotor_point(x,y,a,z):return [x*math.cos(a)-y*math.sin(a),x*math.sin(a)+y*math.cos(a),z]
for n in range(4):
 a=n*math.pi/2;vv=[rotor_point(x,y,a,z)for z in [1.76,2.40]for x,y in shape];length=len(shape);front=list(reversed(range(length,2*length)));back=list(range(length));faces=[]
 for name,ids,key in [('front',front,'steel'),('back',back,'joint')]+[(f'edge{i}',[i,(i+1)%length,(i+1)%length+length,i+length],'edge')for i in range(length)]:
  u0,v0,u1,v1=U[key]['uv']
  if len(ids)==4:co=[[u0,v1],[u1,v1],[u1,v0],[u0,v0]]
  else:
   # Native UV follows this same blade polygon; rotation does not repaint texture.
   co=[[u0+(vv[i][0]*math.cos(a)+vv[i][1]*math.sin(a)-.55)/(3.5-.55)*(u1-u0),v1-(-vv[i][0]*math.sin(a)+vv[i][1]*math.cos(a)+.65)/(1.75+.65)*(v1-v0)]for i in ids]
  faces.append(dict(name=name,indices=ids,uvs=co,materialRegion=key))
 parts.append(dict(name='blade_'+str(n),group='rotor',kind='paddle_prism',vertices=vv,faces=faces))
cube('rotor_hub',[-1.12,-1.12,2.40],[1.12,1.12,3.08],'steel',group='rotor');cube('copper_axle_cap',[-.65,-.65,3.08],[.65,.65,3.2],'copper',group='rotor')
# Export outward face winding with UV corner identity preserved.
for p in parts:
 center=[sum(v[i] for v in p['vertices'])/len(p['vertices']) for i in range(3)]
 for f in p['faces']:
  vv=[p['vertices'][i] for i in f['indices']];a=[vv[1][i]-vv[0][i] for i in range(3)];b=[vv[2][i]-vv[0][i] for i in range(3)];normal=[a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]];fc=[sum(v[i] for v in vv)/len(vv) for i in range(3)]
  if sum(normal[i]*(fc[i]-center[i]) for i in range(3))<0:f['indices'].reverse();f['uvs'].reverse()
G=dict(asset='F05-03',units='1/16 block',origin='existing node point; local +Z outward',textureSize=[64,64],texture='shared-machine-atlas-64.png',parts=parts,rotorPivot=[0,0,1.76],rotationAxis=[0,0,1],rotationRateRadiansPerWorldTick=.7,rotorRadius=max(math.hypot(x,y)for x,y in shape),minimumFrameClearanceRadius=3.84,localEnvelope={'min':[-4.64,-4.64,.60],'max':[4.64,4.64,3.2]},thrustLocal=[0,0,-1],exhaustLocal=[0,0,1],activeRule='per-node active mask for real rotation; not battery presence or enabled alone',ghostRotates=False,integrated=False)
(R/'source/geometry-and-uv.json').write_text(json.dumps(G,indent=2)+'\n');(R/'exports/fan-geometry.json').write_text(json.dumps(G,indent=2)+'\n');(R/'exports/fan-animation.json').write_text(json.dumps(dict(asset='F05-03',version='v01',status='draft_pending_review',integrated=False,pivot=G['rotorPivot'],axis=G['rotationAxis'],movingGroup='rotor',fixedGroup='housing',worldTickRate=.7,angle='active && !ghost ? ageInTicks * .7 : 0',stopRestAngle=0,motionBlur=False,customFxAdded=False,exhaustLocal=[0,0,1],thrustLocal=[0,0,-1]),indent=2)+'\n')
# Free Blockbench meshes retain explicit UV and hierarchy at the real pivot.
e=[];groups={}
for p in parts:
 uid=str(uuid.uuid4());faces={}
 for i,f in enumerate(p['faces']):faces[str(i)]={'vertices':[str(j)for j in f['indices']],'uv':{str(j):[v*4 for v in uv]for j,uv in zip(f['indices'],f['uvs'])},'texture':0}
 e.append(dict(name=p['name'],uuid=uid,type='mesh',vertices={str(i):v for i,v in enumerate(p['vertices'])},faces=faces,origin=G['rotorPivot'] if p['group']=='rotor' else [0,0,0]));groups.setdefault(p['group'],[]).append(uid)
bb=dict(meta={'format_version':'4.10','model_format':'free','box_uv':False},name='Wildcraft fan F05-03 v01',resolution={'width':64,'height':64},elements=e,outliner=[dict(name=k,uuid=str(uuid.uuid4()),origin=G['rotorPivot']if k=='rotor'else[0,0,0],children=v)for k,v in groups.items()],textures=[dict(name='shared-machine-atlas-64.png',id='0',uuid=str(uuid.uuid4()),width=64,height=64,uv_width=64,uv_height=64,source='data:image/png;base64,'+base64.b64encode((R/'exports/shared-machine-atlas-64.png').read_bytes()).decode(),mode='bitmap')]);(R/'source/fan-v01.bbmodel').write_text(json.dumps(bb,indent=2)+'\n');print(f'{len(parts)}部件：{len(groups["housing"])}固定件、4叶片+2轮毂转动件；同64px采用图集。')
