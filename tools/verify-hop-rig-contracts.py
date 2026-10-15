"""Read-only source hop qualification; nominal preview travel never becomes entity movement."""
import argparse,hashlib,importlib.util,json
from pathlib import Path
import numpy as np
ROOT=Path(__file__).resolve().parents[1]
s=importlib.util.spec_from_file_location('reader',ROOT/'tools/import-player-combat-art.py');r=importlib.util.module_from_spec(s);s.loader.exec_module(r)
def main():
 p=argparse.ArgumentParser();p.add_argument('--output',required=True);a=p.parse_args();rows=[]
 for package,prefixes in [('back-hop/v01',['hop']),('side-hop/v01',['left','right'])]:
  folder=ROOT/'art/player-animation-v1'/package;assert json.loads((folder/'docs/APPROVAL.json').read_text())['adopted'] is True
  selected={d+'_'+equip+'_'+arm for d in prefixes for equip in ['empty','carry','guard'] for arm in ['R','L']}
  for variant in ['standard','slim']:
   path=next((folder/'exports').glob(variant+'*.glb'));e=r.Export(path);names=[e.nodes[i]['name'].split('/')[-1] for i in e.palette];assert len(e.joints)==20 and names[0]=='root'
   assert all(side+'_'+ref in names for side in ['left','right'] for ref in ['palm_grip','shield_mount','sword_reference'])
   inverse=e.array(e.data['skins'][0]['inverseBindMatrices']);error=max(float(np.max(np.abs(e.rest[i]@inverse[i].reshape(4,4).T-np.eye(4)))) for i in range(20));assert error<1e-5;clips=[]
   for animation in e.data['animations']:
    if animation['name'] not in selected:continue
    tracks=e.tracks(animation);duration=max(float(x[2][-1]) for x in tracks);assert abs(duration-.3)<1e-6;root_error=0
    for t in np.linspace(0,duration,121):
     worlds=e.world(tracks,float(t));assert all(np.isfinite(m).all() for m in worlds);root_error=max(root_error,float(np.max(np.abs(worlds[0]-e.rest[0]))))
    assert root_error<1e-5;clips.append({'clip':animation['name'],'duration':duration,'rootDeviation':root_error})
   assert len(clips)==len(selected);rows.append({'source':str(path.relative_to(ROOT)),'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'bindInverseError':error,'sourceSkinBones':20,'selected':clips})
 t=Path(a.output);t.parent.mkdir(parents=True,exist_ok=True);t.write_text(json.dumps({'localOnly':True,'scope':'Source only: .30s/6-world-tick art phase, no extra displacement or forced foot contacts; actual native hop and collision/damage/exit pending','variants':rows},ensure_ascii=False,indent=2)+'\n');print('PASS hop source 4 variants/36 phase clips; actual gameplay qualification pending')
if __name__=='__main__':main()
