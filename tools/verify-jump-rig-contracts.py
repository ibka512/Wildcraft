"""Read-only qualification of adopted jump sources; never modifies gameplay/resources."""
import argparse, hashlib, importlib.util, json
from pathlib import Path
import numpy as np
ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('jump_reader',ROOT/'tools/import-player-combat-art.py');reader=importlib.util.module_from_spec(spec);spec.loader.exec_module(reader)
def main():
 p=argparse.ArgumentParser();p.add_argument('--output',required=True);args=p.parse_args();folder=ROOT/'art/player-animation-v1/jump-takeoff-landing/v01';assert json.loads((folder/'docs/APPROVAL.json').read_text())['adopted'] is True;rows=[]
 for variant in ['standard','slim']:
  path=folder/'exports'/f'{variant}-jump-takeoff-landing.glb';e=reader.Export(path);names=[e.nodes[i]['name'].split('/')[-1]for i in e.palette];assert len(e.joints)==20
  inverse=e.array(e.data['skins'][0]['inverseBindMatrices']);bind=max(float(np.max(np.abs(e.rest[i]@inverse[i].reshape(4,4).T-np.eye(4))))for i in range(20));assert bind<1e-5
  for animation in e.data['animations']:
   name=animation['name'];kind=name.split('_')[0];duration={'takeoff':.2,'air':.6,'land':.3,'cycle':1.65}[kind];tracks=e.tracks(animation);actual=max(float(t[2][-1])for t in tracks);assert abs(actual-duration)<1e-5;start=e.world(tracks,0);root=0;foot=0;grip=0
   side='right'if name.endswith('R')else'left'
   for t in np.linspace(0,actual,121):
    w=e.world(tracks,float(t));root=max(root,float(np.max(np.abs(w[names.index('root')]-start[names.index('root')]))));foot=max(foot,max(float(np.linalg.norm(w[names.index(s+'_foot')][:3,3]-start[names.index(s+'_foot')][:3,3]))for s in ['left','right']))
    palm=w[names.index(side+'_palm_grip')][:3,3];reference=w[names.index(side+'_sword_reference')][:3,3];grip=max(grip,float(np.linalg.norm(palm-reference)))
   assert root<1e-5;rows.append({'variant':variant,'clip':name,'duration':actual,'sourceSHA256':hashlib.sha256(path.read_bytes()).hexdigest(),'bindInverseError':bind,'rootDeviation':root,'footOriginExcursion':foot,'sourceSwordReferenceGap':grip,'reviewOnly':kind=='cycle'})
 out=Path(args.output);out.parent.mkdir(parents=True,exist_ok=True);out.write_text(json.dumps({'scope':'Source numeric qualification only; actual native jump, interpolation, floor contact, equipment GPU and exit behavior remain unverified. Cycle and previewLift excluded.','localOnly':True,'rows':rows},indent=2)+'\n');print(json.dumps({'rows':len(rows),'rootMax':max(r['rootDeviation']for r in rows),'gripMax':max(r['sourceSwordReferenceGap']for r in rows),'footMax':max(r['footOriginExcursion']for r in rows)}))
if __name__=='__main__':main()
