"""Fan geometry in original node-local model units; reuse approved shared atlas."""
from pathlib import Path
import json,math,uuid,base64
R=Path(__file__).resolve().parents[1];U=json.loads((R/'references/uv-regions.json').read_text())['regions'];parts=[]
for r in U.values():r['uv'][0]/=2;r['uv'][2]/=2
for name,px in {'cream':[64,0,96,32],'olive':[96,0,128,32],'wood':[64,32,96,64],'cloth_back':[96,32,128,64]}.items():U[name]={'uv':[px[0]/8,px[1]/4,px[2]/8,px[3]/4]}

def cube(name,a,b,key,override=None,group='housing'):
 x0,y0,z0=a;x1,y1,z1=b;vv=[[x0,y0,z0],[x1,y0,z0],[x1,y1,z0],[x0,y1,z0],[x0,y0,z1],[x1,y0,z1],[x1,y1,z1],[x0,y1,z1]];fm={'north':[0,1,2,3],'south':[5,4,7,6],'east':[1,5,6,2],'west':[4,0,3,7],'up':[3,2,6,7],'down':[4,5,1,0]};faces=[]
 for f,ids in fm.items():
  keyf=(override or {}).get(f,key);u0,v0,u1,v1=U[keyf]['uv'];faces.append(dict(name=f,indices=ids,uvs=[[u0,v1],[u1,v1],[u1,v0],[u0,v0]],materialRegion=keyf))
 parts.append(dict(name=name,group=group,kind='cuboid',vertices=vv,faces=faces))

# All fixed construction in node-local game units. Native X/Z rigid airfoil plane, no canopy.
def panel(name,points,key):
 vv=[[x,y,z]for y in [-.16,.16]for x,z in points];n=len(points);faces=[]
 for fn,ids,mat in [('top',list(range(n,2*n)),key),('underside',list(reversed(range(n))),'cloth_back')]+[(f'edge{i}',[i,(i+1)%n,(i+1)%n+n,i+n],key)for i in range(n)]:
  u0,v0,u1,v1=U[mat]['uv'];xs=[p[0]for p in points];zs=[p[1]for p in points]
  if fn in ['top','underside']:uvs=[[u0+(abs(vv[i][0])-min(abs(x)for x in xs))/(max(abs(x)for x in xs)-min(abs(x)for x in xs))*(u1-u0),v0+(vv[i][2]-min(zs))/(max(zs)-min(zs))*(v1-v0)]for i in ids]
  else:uvs=[[u0,v1],[u1,v1],[u1,v0],[u0,v0]]
  faces.append(dict(name=fn,indices=ids,uvs=uvs,materialRegion=mat))
 parts.append(dict(name=name,group='cloth',kind='flat_panel',vertices=vv,faces=faces))
def beam(name,a,b,width=.3,depth=.5):
 # Narrow rectangular prism along a fixed X/Z beam, no arbitrary box approximation.
 x0,z0=a;x1,z1=b;dx=x1-x0;dz=z1-z0;length=math.hypot(dx,dz);nx=-dz/length*width/2;nz=dx/length*width/2
 vv=[[x,y,z]for y in [.16,.16+depth]for x,z in [(x0+nx,z0+nz),(x1+nx,z1+nz),(x1-nx,z1-nz),(x0-nx,z0-nz)]]
 faces=[]
 for fn,ids in [('bottom',[3,2,1,0]),('top',[4,5,6,7]),('left',[0,1,5,4]),('tip',[1,2,6,5]),('right',[2,3,7,6]),('root',[3,0,4,7])]:
  u0,v0,u1,v1=U['wood']['uv'];faces.append(dict(name=fn,indices=ids,uvs=[[u0,v1],[u1,v1],[u1,v0],[u0,v0]],materialRegion='wood'))
 parts.append(dict(name=name,group='frame',kind='fixed_spar',vertices=vv,faces=faces))
# Wing edge contracts remain inside prototype envelope, with intentional exposed spar joints.
# Left/right are the same silhouette and material identity.
for sign,label in [(-1,'left'),(1,'right')]:
 def pt(x,z):return [sign*x,z]
 def leading(x):return 1.3+(x-1)*.13
 def trailing(x):return 8.5 if x<=7 else 8.5-(x-7)*2.36
 for i,(lo,hi,key)in enumerate([(1,3,'cream'),(3,5,'olive'),(5,7,'cream'),(7,9.3,'cream')]):
  points=[pt(lo,leading(lo)),pt(hi,leading(hi)),pt(hi,trailing(hi)),pt(lo,trailing(lo))];panel(label+'_cloth_'+str(i),points,key)
 beam(label+'_leading_spar',pt(1,1.3),pt(9.3,2.379),.42,.54)
 beam(label+'_trailing_spar',pt(1,8.5),pt(7,8.5),.32,.40)
 beam(label+'_outer_spar',pt(7,8.5),pt(9.3,3.072),.34,.40)
 for x in [3,5]:beam(label+'_fixed_rib_'+str(x),pt(x,leading(x)),pt(x,trailing(x)),.18,.22)
 for i,(x,z)in enumerate([(9.3,2.5),(7,8.5)]):cube(label+'_copper_fastener_'+str(i),[sign*x-.18,.16,z-.18],[sign*x+.18,.86,z+.18],'copper',group='frame')
cube('central_wood_keel',[-.55,-.72,.64],[.55,.92,8.8],'wood',group='frame')
cube('root_mount_plate',[-2.08,-1.28,.64],[2.08,1.28,1.12],'steel',{'north':'port'},group='mount')
for i,x in enumerate([-1,.44]):cube('root_copper_contact_'+str(i),[x,-.8,.60],[x+.56,.8,.64],'copper',group='mount')
for i,x in enumerate([-1.5,1.02]):cube('root_copper_clamp_'+str(i),[x,-.9,1.12],[x+.48,.92,1.72],'copper',group='mount')
# Correct every convex face winding while preserving exact UV corner correspondence.
for p in parts:
 center=[sum(v[i]for v in p['vertices'])/len(p['vertices'])for i in range(3)]
 for f in p['faces']:
  vv=[p['vertices'][i]for i in f['indices']];a=[vv[1][i]-vv[0][i]for i in range(3)];b=[vv[2][i]-vv[0][i]for i in range(3)];n=[a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]];fc=[sum(v[i]for v in vv)/len(vv)for i in range(3)]
  if sum(n[i]*(fc[i]-center[i])for i in range(3))<0:f['indices'].reverse();f['uvs'].reverse()
bounds={k:[fn(v[i]for p in parts for v in p['vertices'])for i in range(3)]for k,fn in [('min',min),('max',max)]}
G=dict(asset='F05-02',version='v01',status='draft_pending_review',integrated=False,units='1/16 block',origin='existing node, local +Z outward',textureSize=[128,64],texture='wing-atlas-128x64.png',parts=parts,localEnvelope=bounds,groupMotion='all parts fixed',activeRule='existing wing descent condition, HUD indication only; no moving geometry',consumesEnergy=False,physicsChanged=False,prototypeBounds={'min':[-9.6,-1.28,.64],'max':[9.6,1.28,8.8]},contactException=[.60,.64],textureReuse='left 64x64 machine pixels exact; right four adopted 32x32 glider patches exact',body=None)
(R/'source/geometry-and-uv.json').write_text(json.dumps(G,ensure_ascii=False,indent=2)+'\n');(R/'exports/wing-geometry.json').write_text(json.dumps(G,ensure_ascii=False,indent=2)+'\n')
e=[];groups={}
for p in parts:
 uid=str(uuid.uuid4());faces={}
 for i,f in enumerate(p['faces']):faces[str(i)]={'vertices':[str(j)for j in f['indices']],'uv':{str(j):[uv[0]*8,uv[1]*4]for j,uv in zip(f['indices'],f['uvs'])},'texture':0}
 e.append(dict(name=p['name'],uuid=uid,type='mesh',vertices={str(i):v for i,v in enumerate(p['vertices'])},faces=faces,origin=[0,0,0]));groups.setdefault(p['group'],[]).append(uid)
bb=dict(meta={'format_version':'4.10','model_format':'free','box_uv':False},name='Wildcraft passive wing F05-02 v01',resolution={'width':128,'height':64},elements=e,outliner=[dict(name=k,uuid=str(uuid.uuid4()),origin=[0,0,0],children=v)for k,v in groups.items()],textures=[dict(name=G['texture'],id='0',uuid=str(uuid.uuid4()),width=128,height=64,uv_width=128,uv_height=64,source='data:image/png;base64,'+base64.b64encode((R/'exports'/G['texture']).read_bytes()).decode(),mode='bitmap')]);(R/'source/wing-v01.bbmodel').write_text(json.dumps(bb,indent=2)+'\n')
print(len(parts),'fixed parts',bounds)
