"""Local-only source conversion; immutable six-bone skin, no reference equipment or gameplay events."""
from pathlib import Path
import json,hashlib,math
ROOT=Path(__file__).resolve().parents[1]
BONES=[s+'_'+b for s in ('right','left') for b in ('upper_arm','forearm','elbow_deform')]
def mesh(m):
 o=dict(part=m['part'],outer=m['outer'],positions=[],normals=[],uv=[],joints=[],weights=[],indices=[])
 for face,uv in zip(m['faces'],m['uvs']):
  p=[m['vertices'][i] for i in face];a=[p[1][i]-p[0][i] for i in range(3)];b=[p[2][i]-p[0][i] for i in range(3)];n=[a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]];length=math.sqrt(sum(x*x for x in n));assert length>0;n=[x/length for x in n];start=len(o['positions'])//3
  for i,t in zip(face,uv):
   o['positions']+=m['vertices'][i];o['normals']+=n;o['uv']+=[t[0],1-t[1]];pairs=[(BONES.index(b),w) for b,w in m['weights'][i].items() if w>0];assert len(pairs)<=4 and abs(sum(w for _,w in pairs)-1)<1e-6;pairs += [(0,0)]*(4-len(pairs));o['joints'] += [b for b,w in pairs];o['weights'] += [w for b,w in pairs]
  for i in range(1,len(face)-1):o['indices'] += [start,start+i,start+i+1]
 return o
def palm(bone,rotation):
 w,x,y,z=rotation;return [bone[0]-.2425*2*(x*z+y*w),bone[1]-.2425*2*(y*z-x*w),bone[2]-.2425*(1-2*(x*x+y*y))]
def main():
 approval=json.loads((ROOT/'art/reviews/2026-10-21/FP-HEAVY-AXE-LOCAL-ADOPTION.json').read_text());assert approval['status']=='authorized-local-integration';out=ROOT/'src/main/resources/assets/wildcraft/animation';records=[]
 for source in approval['scope']:
  f=ROOT/source['path'];assert hashlib.sha256(f.read_bytes()).hexdigest()==source['sha256'];d=json.loads(f.read_text());kind=source['asset'];prefixes=['charge','hold','release']+(['light'] if kind=='axe' else [])
  for variant,v in d['variants'].items():
   clips={};weights={};maximum=0
   for name,c in v['clips'].items():
    if not any(name.startswith(prefix+'_') or name.startswith('shield_'+prefix+'_') for prefix in prefixes):continue
    frames=[];weights[name]=[]
    for frame in c['frames']:
     q=lambda a:a[1:]+a[:1];row={'bones':[frame['origins'][b]+q(frame['rotations'][b]) for b in BONES],'grips':[frame['grips'][s] for s in ['right','left']],'swords':[q(frame['swordMountRotations'][s]) for s in ['right','left']],'shields':[q(frame['shieldMountRotations'][s]) for s in ['right','left']]};frames.append(row);weights[name].append(frame.get('supportWeight',0))
     for s in ['right','left']:maximum=max(maximum,math.dist(palm(frame['origins'][s+'_forearm'],frame['rotations'][s+'_forearm']),frame['grips'][s]))
    clips[name]={'duration':c['duration'],'frames':frames}
   assert maximum<2e-6,maximum
   # Shared art-root placement: both palms inside 90% frame at vertical FOV60/aspect4:3; no camera or scale writes.
   up=.10;forward=.0
   for clip in clips.values():
    for frame in clip['frames']:
     for x,y,z in frame['grips']:forward=max(forward,abs(z-d['camera']['eye'][2]+up)/math.tan(math.pi/6)/.9+y,abs(x)/math.tan(math.pi/6)/(4/3)/.9+y)
   forward=math.ceil((max(0,forward)+.01)*100)/100
   data={'schema':1,'variant':variant.lower(),'bones':BONES,'pivots':[v['pivots'][b] for b in BONES],'eye':d['camera']['eye'],'meshes':[mesh(m) for m in v['meshes'].values()],'clips':clips}
   dest=out/f'{variant.lower()}-first-person-{kind}.json';dest.write_text(json.dumps({'data':data,'supportWeights':weights,'placement':[forward,up]},separators=(',',':'),allow_nan=False)+'\n');records.append({'source':source,'output':str(dest.relative_to(ROOT)),'sha256':hashlib.sha256(dest.read_bytes()).hexdigest(),'clips':len(clips),'sourcePalmMaximumError':maximum,'sharedArtRootForwardUp':[forward,up]})
 (out/'first-person-heavy-axe-provenance.json').write_text(json.dumps({'localOnly':True,'scope':'Sword heavy / grounded axe light-heavy, original server state and timing only','records':records},ensure_ascii=False,indent=2)+'\n');print(json.dumps(records,ensure_ascii=False,indent=2))
if __name__=='__main__':main()
