"""Read-only qualification of the four adopted locked movement packages and their flat contact data."""
import argparse,hashlib,importlib.util,json
from pathlib import Path
import numpy as np
ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('adopted_walk_reader',ROOT/'tools/import-player-combat-art.py');reader=importlib.util.module_from_spec(spec);spec.loader.exec_module(reader)
DISTANCES={'forward':.6,'backward':.4,'left':.32,'right':.32}
def main():
 parser=argparse.ArgumentParser();parser.add_argument('--output',required=True);args=parser.parse_args();rows=[];bones=None
 for direction,distance in DISTANCES.items():
  folder=ROOT/'art/player-animation-v1'/('lock-'+direction)/'v01';assert json.loads((folder/'docs/APPROVAL.json').read_text()).get('adopted') is True
  metadata=json.loads(next((folder/'exports').glob('*data.json')).read_text());assert metadata['rootMotion'] is False and metadata['inputLocked'] is False and abs(metadata['cycleDistanceBlocks']-distance)<1e-6
  expected={phase+'_'+equipment+'_'+side for phase in [direction,'enter','exit'] for equipment in ['empty','carry'] for side in ['R','L']}
  for variant in ['standard','slim']:
   path=next((folder/'exports').glob(variant+'*.glb'));e=reader.Export(path);names=[e.nodes[i]['name'].split('/')[-1] for i in e.joints];assert len(names)==22
   if bones is None:bones=names
   assert names==bones;meshes=e.meshes();inverse=e.array(e.data['skins'][0]['inverseBindMatrices']);error=max(float(np.max(np.abs(e.rest[i]@inverse[i].reshape(4,4).T-np.eye(4)))) for i in range(22));assert error<1e-5
   clips={a['name']:a for a in e.data['animations']};assert set(clips)==expected;checks=[]
   for name,a in clips.items():
    tracks=e.tracks(a);duration=max(float(t[2][-1]) for t in tracks);root_error=0
    for t in np.linspace(0,duration,round(duration*30)+1):
     worlds=e.world(tracks,float(t));assert all(np.isfinite(m).all() for m in worlds);root_error=max(root_error,float(np.max(np.abs(worlds[0]-e.rest[0]))))
    assert root_error<1e-5
    checks.append({'clip':name,'duration':duration,'rootDeviation':root_error})
   rows.append({'source':str(path.relative_to(ROOT)),'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'distancePerCycle':distance,'skinBones':22,'skinMeshes':len(meshes),'bindInverseError':error,'clips':checks})
 target=Path(args.output);target.parent.mkdir(parents=True,exist_ok=True);target.write_text(json.dumps({'scope':'Source only; real locked movement, blocked phase, original sword/shield, native flat ground and transition priorities still require runtime qualification','localOnly':True,'variants':rows},ensure_ascii=False,indent=2)+'\n')
 print('PASS adopted lock walk source contracts:',len(rows),'variants;',sum(len(r['clips']) for r in rows),'source clips; gameplay/terrain integration pending')
if __name__=='__main__':main()
