"""Read-only adopted mace air source qualification; no runtime promotion."""
import argparse, hashlib, importlib.util, json
from pathlib import Path
import numpy as np
ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('mace_air_source_reader',ROOT/'tools/import-player-combat-art.py');reader=importlib.util.module_from_spec(spec);spec.loader.exec_module(reader)
def main():
 p=argparse.ArgumentParser();p.add_argument('--output',required=True);args=p.parse_args();folder=ROOT/'art/player-animation-v1/mace-air/v01';assert json.loads((folder/'docs/APPROVAL.json').read_text())['adopted'] is True;rows=[]
 for variant in ['standard','slim']:
  path=folder/'exports'/f'{variant}-mace-air.glb';e=reader.Export(path);names=[e.nodes[i]['name'].split('/')[-1]for i in e.palette];assert len(e.joints)==22;inverse=e.array(e.data['skins'][0]['inverseBindMatrices']);bind=max(float(np.max(np.abs(e.rest[i]@inverse[i].reshape(4,4).T-np.eye(4))))for i in range(22));assert bind<1e-5
  for animation in e.data['animations']:
   name=animation['name'];side='right'if name.endswith('R')else'left';off='left'if side=='right'else'right';tracks=e.tracks(animation);duration=max(float(t[2][-1])for t in tracks);assert abs(duration-(.6 if name.startswith('light')else 2.5))<1e-5;start=e.world(tracks,0);root=feet=main_gap=off_gap=0;support_min=99;support_max=-99
   for t in np.linspace(0,duration,161):
    world=e.world(tracks,float(t));root=max(root,float(np.max(np.abs(world[names.index('root')]-start[names.index('root')]))));feet=max(feet,max(float(np.linalg.norm(world[names.index(s+'_foot')][:3,3]-start[names.index(s+'_foot')][:3,3]))for s in ['left','right']));reference=world[names.index(side+'_mace_reference')];center=reference[:3,3];axis=reference[:3,1]/np.linalg.norm(reference[:3,1]);palm=(world[names.index(side+'_wrist')]@np.array([0,.0625,0,1]))[:3];main_gap=max(main_gap,float(np.linalg.norm(palm-center)));other=(world[names.index(off+'_wrist')]@np.array([0,.0625,0,1]))[:3];along=float(np.dot(other-center,axis));support_min=min(support_min,along);support_max=max(support_max,along);off_gap=max(off_gap,float(np.linalg.norm(other-center-axis*along)))
   assert root<1e-5;rows.append({'variant':variant,'clip':name,'duration':duration,'sourceSHA256':hashlib.sha256(path.read_bytes()).hexdigest(),'bindInverseError':bind,'rootDeviation':root,'airborneFootOriginExcursion':feet,'mainReferenceGripGap':main_gap,'freeSupportTransverseGap':off_gap if '_empty_'in name else None,'supportMin':support_min,'supportMax':support_max,'mainUnderNative015':bool(main_gap<.015)})
 out=Path(args.output);out.parent.mkdir(parents=True,exist_ok=True);out.write_text(json.dumps({'localOnly':True,'scope':'Read-only source qualification, not native model/GPU/gameplay acceptance; no live56 renderer or conversion activated. Ground-only source cannot supersede native airborne smash.','rows':rows},indent=2)+'\n');print('Mace ground16 source/root/bind measured; max main',max(x['mainReferenceGripGap']for x in rows),'free support',max(x['freeSupportTransverseGap']or 0 for x in rows),'foot',max(x['airborneFootOriginExcursion']for x in rows))
if __name__=='__main__':main()
