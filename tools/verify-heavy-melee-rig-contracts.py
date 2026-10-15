"""Read-only adopted sword heavy/axe sources; proposed art durations never become new combat rules."""
import argparse,hashlib,importlib.util,json
from pathlib import Path
import numpy as np
ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('adopted_heavy_reader',ROOT/'tools/import-player-combat-art.py');reader=importlib.util.module_from_spec(spec);spec.loader.exec_module(reader)
PACKAGES={'sword-heavy/v01':['charge','hold','release','cancel'],'axe-light/v02':['attack'],'axe-heavy/v01':['charge','hold','release','cancel']}
def main():
 parser=argparse.ArgumentParser();parser.add_argument('--output',required=True);args=parser.parse_args();rows=[]
 for package,phases in PACKAGES.items():
  folder=ROOT/'art/player-animation-v1'/package;assert json.loads((folder/'docs/APPROVAL.json').read_text()).get('adopted') is True
  metadata=json.loads(next((folder/'exports').glob('*data.json')).read_text());assert metadata['rootMotion'] is False and metadata['inputLocked'] is False
  selected={phase+'_'+equipment+'_'+side for phase in phases for equipment in ['empty','carry'] for side in ['R','L']}
  for variant in ['standard','slim']:
   path=next((folder/'exports').glob(variant+'*.glb'));e=reader.Export(path);assert len(e.joints)==20
   refs=[i for i,n in enumerate(e.nodes) if n.get('name','').endswith('_axe_reference')]
   e.palette.extend(i for i in refs if i not in e.palette);names=[e.nodes[i]['name'].split('/')[-1] for i in e.palette]
   assert all(side+('_axe_reference' if package.startswith('axe') else '_sword_reference') in names for side in ['left','right'])
   inverse=e.array(e.data['skins'][0]['inverseBindMatrices']);error=max(float(np.max(np.abs(e.rest[i]@inverse[i].reshape(4,4).T-np.eye(4)))) for i in range(20));assert error<1e-5
   animations={a['name']:a for a in e.data['animations']};assert selected.issubset(animations);checks=[]
   for name in sorted(selected):
    tracks=e.tracks(animations[name]);duration=max(float(t[2][-1]) for t in tracks);root_error=0
    for t in np.linspace(0,duration,round(duration*60)+1):
     worlds=e.world(tracks,float(t));assert all(np.isfinite(m).all() for m in worlds);root_error=max(root_error,float(np.max(np.abs(worlds[0]-e.rest[0]))))
    assert root_error<1e-5;checks.append({'clip':name,'duration':duration,'rootDeviation':root_error})
   rows.append({'source':str(path.relative_to(ROOT)),'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'sourceSkinBones':20,'targetRuntimeSkinBones':22,'bindInverseError':error,'targetRetarget':'missing wrists from actual source forearm and qualified bind; axe references use explicit legacy weapon slot aliases, never invented sword references','selectedSourceClips':checks})
 target=Path(args.output);target.parent.mkdir(parents=True,exist_ok=True);target.write_text(json.dumps({'scope':'Source qualification only; existing native charge/attack/durability/Fuse/support hands must be checked in game; art timings are not new rules','localOnly':True,'variants':rows},ensure_ascii=False,indent=2)+'\n')
 print('PASS adopted heavy melee sources:',len(rows),'variants;',sum(len(r['selectedSourceClips']) for r in rows),'selected source clips; retarget/grip/gameplay pending')
if __name__=='__main__':main()
