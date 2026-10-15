"""Qualify only the adopted v04 idle bow source; draw/air/opposite-hand supplements are not promoted."""
import argparse,hashlib,importlib.util,json
from pathlib import Path
import numpy as np
ROOT=Path(__file__).resolve().parents[1]
s=importlib.util.spec_from_file_location('bow_idle_reader',ROOT/'tools/import-player-combat-art.py');r=importlib.util.module_from_spec(s);s.loader.exec_module(r)
def main():
 p=argparse.ArgumentParser();p.add_argument('--output',required=True);a=p.parse_args();folder=ROOT/'art/player-animation-v1/bow-hold/v04';assert json.loads((folder/'docs/APPROVAL.json').read_text())['adopted'] is True
 meta=json.loads((folder/'exports/bow-data.json').read_text());assert meta['rootMotion'] is False and meta['inputLocked'] is False
 expected={f'bow_idle_{equip}_{arm}' for equip in ['empty','carry'] for arm in ['R','L']};rows=[]
 for variant,label in [('standard','Standard'),('slim','Slim')]:
  path=folder/'exports'/f'{variant}-bow-hold.glb';e=r.Export(path);assert len(e.joints)==22
  inverse=e.array(e.data['skins'][0]['inverseBindMatrices']);bind=max(float(np.max(np.abs(e.rest[i]@inverse[i].reshape(4,4).T-np.eye(4)))) for i in range(22));assert bind<1e-5
  animations={x['name']:x for x in e.data['animations']};assert set(animations)==expected
  names={n.get('name','').split('/')[-1]:i for i,n in enumerate(e.nodes)};checks=[]
  for name,animation in animations.items():
   side='right' if name.endswith('_R') else 'left';selected=[e.joints[0],names[side+'_bow_reference'],names[side+'_palm_grip']];tracks=e.tracks(animation);duration=max(float(t[2][-1]) for t in tracks);assert abs(duration-8/3)<1e-5
   root_error=0;grip_error=0
   for t in np.linspace(0,duration,161):
    world=e.world(tracks,float(t),selected);assert all(np.isfinite(m).all() for m in world);root_error=max(root_error,float(np.max(np.abs(world[0]-e.rest[0]))));grip_error=max(grip_error,float(np.linalg.norm(world[1][:3,3]-world[2][:3,3])))
   assert root_error<1e-5 and grip_error<.015,(name,root_error,grip_error)
   first=e.world(tracks,0);last=e.world(tracks,duration);assert max(float(np.max(np.abs(x-y))) for x,y in zip(first,last))<1e-4
   checks.append({'clip':name,'duration':duration,'gripArm':side,'rootDeviation':root_error,'actualBowReferenceToPalmDistance':grip_error})
  rows.append({'source':str(path.relative_to(ROOT)),'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'skinBones':22,'skinMeshes':len(e.meshes()),'bindInverseError':bind,'clips':checks})
 out=Path(a.output);out.parent.mkdir(parents=True,exist_ok=True);out.write_text(json.dumps({'localOnly':True,'scope':'Source idle only; actual native bow sprite axis/grip, item ownership, skin/GPU, ground/empty-or-shield-carry, cancels and unsupported draw/air/first-person fallback require runtime qualification. Pending supplements excluded.','variants':rows},ensure_ascii=False,indent=2)+'\n');print('PASS adopted bow idle v04: 2 variants/8 closed clips, actual bow references/palms/fixed roots; runtime pending')
if __name__=='__main__':main()
