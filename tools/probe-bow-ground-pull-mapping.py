"""Read-only source geometry inverse: native pull maps to actual draw-hand/grip distance, no player mutation."""
import argparse,hashlib,importlib.util,json
from pathlib import Path
import numpy as np
ROOT=Path(__file__).resolve().parents[1]
s=importlib.util.spec_from_file_location('pull_source_reader',ROOT/'tools/import-player-combat-art.py');r=importlib.util.module_from_spec(s);s.loader.exec_module(r)
def main():
 p=argparse.ArgumentParser();p.add_argument('--output',required=True);a=p.parse_args();folder=ROOT/'art/player-animation-v1/bow-ground/v05';assert json.loads((folder/'docs/APPROVAL.json').read_text())['adopted'] is True
 metadata=json.loads((folder/'exports/bow-ground-data.json').read_text());rows=[]
 for variant,label in [('standard','Standard'),('slim','Slim')]:
  path=folder/'exports'/f'{variant}-bow-ground.glb';e=r.Export(path);nodes={x.get('name','').split('/')[-1]:i for i,x in enumerate(e.nodes)};animation=next(x for x in e.data['animations'] if x['name']=='bow_draw_empty_L');tracks=[t for t in e.tracks(animation) if t[1]!='weights'];end=max(float(t[2][-1]) for t in tracks)
  frames=metadata['variants'][label]['clips']['bow_draw_empty_L']['frames'];start=max(f['time'] for f in frames if float(f.get('drawProgress',0))<=1e-5)
  def distance(t):
   world=e.world(tracks,t,[nodes['left_bow_reference'],nodes['right_palm_grip']]);return float(np.linalg.norm(world[1][:3,3]-world[0][:3,3]))
  times=np.linspace(start,end,1201);distances=[distance(float(t)) for t in times];assert all(y>=x-1e-5 for x,y in zip(distances,distances[1:]));low=distances[0];high=distances[-1];assert high-low>.4
  table=[];max_error=0
  for pull in np.linspace(0,1,101):
   target=low+float(pull)*(high-low);lo=start;hi=end
   for _ in range(28):
    mid=(lo+hi)/2
    if distance(mid)<target:lo=mid
    else:hi=mid
   time=(lo+hi)/2;actual=distance(time);max_error=max(max_error,abs(actual-target));table.append({'nativePull':round(float(pull),6),'sourceSeconds':time,'drawHandGripDistance':actual})
  assert max_error<1e-5
  # Check runtime-sized 101-point table through interpolation at unseen pulls.
  interpolation_error=0
  for pull in np.linspace(0,1,401):
   at=float(pull)*100;i=min(int(at),99);t=table[i]['sourceSeconds']+(table[i+1]['sourceSeconds']-table[i]['sourceSeconds'])*(at-i);actual=distance(t);interpolation_error=max(interpolation_error,abs((actual-low)/(high-low)-float(pull)))
  rows.append({'source':str(path.relative_to(ROOT)),'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'actualDrawStart':start,'actualDrawEnd':end,'nativeZeroDistance':low,'nativeFullDistance':high,'binarySearchMaxDistanceError':max_error,'tableInterpolationMaxRelativeError':interpolation_error,'nativePullToSourceTime':table})
 out=Path(a.output);out.parent.mkdir(parents=True,exist_ok=True);out.write_text(json.dumps({'localOnly':True,'scope':'Source probe only, geometry-derived monotone mapping and interpolation errors recorded. Not runtime delivery: native bow sprite string/nock/aim and used-hand ownership, actual inventory/arrows/wear/GPU/exit tests remain required. No new timing or gameplay permissions.','variants':rows},ensure_ascii=False,indent=2)+'\n');print('PASS actual draw-hand source distance monotone/inverse geometry, 2 variants; interpolation error',max(x['tableInterpolationMaxRelativeError'] for x in rows),'runtime pending')
if __name__=='__main__':main()
