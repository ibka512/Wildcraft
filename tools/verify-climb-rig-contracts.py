"""Read-only preflight of adopted climb sources; never substitutes for actual G/input/contact tests.

No runtime dependency or production resource writes. Reuses the existing local GLB reader.
"""
import importlib.util,json,hashlib,argparse
from pathlib import Path
import numpy as np
ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('combat_art_reader',ROOT/'tools/import-player-combat-art.py')
reader=importlib.util.module_from_spec(spec);spec.loader.exec_module(reader)
PACKAGES=[('climb-up/v02',.43,['climb_enter','climb_up']),('climb-side/v01',.32,['climb_enter_left','climb_enter_right','climb_left','climb_right']),('climb-idle/v01',0,['climb_idle_up','climb_idle_left','climb_idle_right','climb_settle_up','climb_settle_left','climb_settle_right'])]
def main():
 parser=argparse.ArgumentParser();parser.add_argument('--output',required=True);args=parser.parse_args();rows=[];base_names=None
 for package,stride,expected in PACKAGES:
  folder=ROOT/'art/player-animation-v1'/package;approval=json.loads((folder/'docs/APPROVAL.json').read_text());assert approval.get('adopted') is True
  data=next((folder/'exports').glob('*data.json'));meta=json.loads(data.read_text());assert meta['rootMotion'] is False and abs(meta['strideBlocks']-stride)<1e-6 and abs(meta['entrySeconds']-.3)<1e-6
  for variant in ['standard','slim']:
   path=next((folder/'exports').glob(variant+'*.glb'));e=reader.Export(path);names=[e.nodes[i]['name'].split('/')[-1] for i in e.joints];assert len(names)==22
   if base_names is None:base_names=names
   assert names==base_names,'Climb sources must share the qualified 22-bone palette'
   meshes=e.meshes();assert len(meshes)==12
   inverse=e.array(e.data['skins'][0]['inverseBindMatrices']);bind_error=max(float(np.max(np.abs(e.rest[i]@inverse[i].reshape(4,4).T-np.eye(4)))) for i in range(22));assert bind_error<1e-5
   clips={a['name']:a for a in e.data['animations']};assert set(clips)==set(expected)
   measurements=[]
   for name,a in clips.items():
    tracks=e.tracks(a);duration=max(float(t[2][-1]) for t in tracks);root_error=0;finite=True
    for t in np.linspace(0,duration,round(duration*30)+1):
     world=e.world(tracks,float(t));root_error=max(root_error,float(np.max(np.abs(world[0]-e.rest[0]))));finite=finite and all(np.isfinite(x).all() for x in world)
    assert finite and root_error<1e-5,'Authored root must remain fixed; movement belongs to the existing entity'
    first=e.world(tracks,0);last=e.world(tracks,duration);seam=max(float(np.max(np.abs(a-b))) for a,b in zip(first,last))
    measurements.append({'clip':name,'durationSeconds':duration,'sampledRootDeviation':root_error,'endToStartMatrixDeviation':seam})
   rows.append({'source':str(path.relative_to(ROOT)),'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'skinBones':22,'skinMeshes':12,'vertices':sum(len(m['positions'])//3 for m in meshes),'bindInverseError':bind_error,'strideBlocks':stride,'wallPlaneBlenderY':meta['wallPlaneY'],'clips':measurements})
 report={'scope':'Source qualification only; no runtime climb/real input/contact pass claimed','localOnly':True,'approvedSourceVariants':len(rows),'checks':'actual source hashes, shared bone palette, mesh weights/indices, bind inverse, finite sampled transforms, fixed root, exact approved clip identities','next':'Real G activation, displacement-driven phase/blocked freeze, wall-facing/actual skin contacts and head limits; keep unqualified descending/mantle/corners native','variants':rows}
 target=Path(args.output);target.parent.mkdir(parents=True,exist_ok=True);target.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n');print('PASS source-only climb contracts:',len(rows),'variants;',sum(len(r['clips']) for r in rows),'clips; runtime/contact pending')
if __name__=='__main__':main()
