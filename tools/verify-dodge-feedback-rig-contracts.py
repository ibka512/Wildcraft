"""Read-only independent dodge-feedback sources; no preview success or root displacement adopted."""
import argparse,hashlib,importlib.util,json
from pathlib import Path
import numpy as np
ROOT=Path(__file__).resolve().parents[1]
s=importlib.util.spec_from_file_location('reader',ROOT/'tools/import-player-combat-art.py');r=importlib.util.module_from_spec(s);s.loader.exec_module(r)
def main():
 p=argparse.ArgumentParser();p.add_argument('--output',required=True);a=p.parse_args();folder=ROOT/'art/player-animation-v1/perfect-dodge/v01';assert json.loads((folder/'docs/APPROVAL.json').read_text())['adopted'] is True;rows=[]
 for variant in ['standard','slim']:
  path=next((folder/'exports').glob(variant+'*.glb'));e=r.Export(path);names=[e.nodes[i]['name'].split('/')[-1] for i in e.palette];assert len(e.joints)==20
  for animation in e.data['animations']:
   if not animation['name'].startswith('pulse-'):continue
   tracks=e.tracks(animation);duration=max(float(t[2][-1]) for t in tracks);assert abs(duration-.24)<1e-6;start=e.world(tracks,0);finish=e.world(tracks,duration);changes={};root_error=0
   for t in np.linspace(0,duration,97):
    world=e.world(tracks,float(t));assert all(np.isfinite(m).all() for m in world);root_error=max(root_error,float(np.max(np.abs(world[0]-start[0]))))
    for i,n in enumerate(names):changes[n]=max(changes.get(n,0),float(np.max(np.abs(world[i]-start[i]))))
   assert root_error<1e-5
   for i,n in enumerate(names):
    if any(key in n for key in ['pelvis','thigh','knee','shin','ankle','foot']):assert changes[n]<1e-5,(n,changes[n])
    assert float(np.max(np.abs(finish[i]-start[i])))<1e-5,(n,'not closed')
   assert changes['body']>.01
   rows.append({'source':str(path.relative_to(ROOT)),'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'clip':animation['name'],'duration':duration,'changedBones':{n:v for n,v in changes.items() if v>1e-5},'rootFixed':True,'pelvisLegsFixed':True,'returnsToStart':True})
 assert len(rows)==36
 t=Path(a.output);t.parent.mkdir(parents=True,exist_ok=True);t.write_text(json.dumps({'localOnly':True,'scope':'Source only; native successful ReactionView event, current-body overlay, .24 effective-real-second clock, native ownership and GPU pending; guards not assumed valid because actual hop releases shield','clips':rows},ensure_ascii=False,indent=2)+'\n');print('PASS dodge feedback 2 variants/36 independent .24s closed pulses, fixed root/pelvis/legs; runtime pending')
if __name__=='__main__':main()
