"""Read-only adopted air spear source and real native pole contact qualification. No renderer or runtime resource promotion."""
import argparse,hashlib,importlib.util,json
from pathlib import Path
import numpy as np
ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('air_spear_reader',ROOT/'tools/import-player-combat-art.py');reader=importlib.util.module_from_spec(spec);spec.loader.exec_module(reader)
def main():
 p=argparse.ArgumentParser();p.add_argument('--output',required=True);args=p.parse_args();folder=ROOT/'art/player-animation-v1/spear-air/v01';assert json.loads((folder/'docs/APPROVAL.json').read_text())['adopted'] is True;metadata=json.loads((folder/'exports/attack-data.json').read_text());rows=[]
 assert metadata['bones']==22 and metadata['gripLocalOffset']==[0,0,-.0625] and abs(metadata['supportLocalOffset'][2]-.4087077195258245)<1e-10
 for variant in ['standard','slim']:
  path=folder/'exports'/f'{variant}-spear-air.glb';e=reader.Export(path);names=[e.nodes[i]['name'].split('/')[-1] for i in e.palette];assert len(e.joints)==22
  inverse=e.array(e.data['skins'][0]['inverseBindMatrices']);bind=max(float(np.max(np.abs(e.rest[i]@inverse[i].reshape(4,4).T-np.eye(4))))for i in range(22));assert bind<1e-5
  for animation in e.data['animations']:
   name=animation['name'];side='right' if name.endswith('R')else'left';off='left'if side=='right'else'right';tracks=e.tracks(animation);duration=max(float(t[2][-1])for t in tracks);assert abs(duration-(.4 if name.startswith('light')else 1.75))<1e-5;start=e.world(tracks,0);root=feet=head=main_gap=off_gap=main_fixed_gap=0;contact_min=99;contact_max=-99
   for t in np.linspace(0,duration,161):
    worlds=e.world(tracks,float(t));assert all(np.isfinite(m).all()for m in worlds);root=max(root,float(np.max(np.abs(worlds[0]-e.rest[0]))))
    for foot in ['left_foot','right_foot']:
     i=names.index(foot);feet=max(feet,float(np.max(np.abs(worlds[i][:3,3]-start[i][:3,3]))))
    i=names.index('head');head=max(head,float(np.max(np.abs(worlds[i][:3,:3]-start[i][:3,:3]))))
    ref=worlds[names.index(side+'_spear_reference')];axis=ref[:3,1]/np.linalg.norm(ref[:3,1]);center=ref[:3,3];palms={h:(worlds[names.index(h+'_wrist')]@np.array([0,.0625,0,1]))[:3]for h in [side,off]}
    for hand in [side,off] if '_empty_'in name else[side]:
     point=palms[hand];along=float(np.dot(point-center,axis));contact=along;gap=float(np.linalg.norm(point-center-axis*along));main_fixed_gap=max(main_fixed_gap,float(np.linalg.norm(point-center)))if hand==side else main_fixed_gap
     if hand==side:main_gap=max(main_gap,gap);contact_min=min(contact_min,contact);contact_max=max(contact_max,contact)
     else:off_gap=max(off_gap,gap)
   assert root<1e-5,(variant,name,root) # Foot deviations are measured, not promoted by this source-only probe.
   rows.append({'variant':variant,'clip':name,'duration':duration,'sourceSHA256':hashlib.sha256(path.read_bytes()).hexdigest(),'bindInverseError':bind,'rootDeviation':root,'airborneFootOriginExcursion':feet,'headRotationDeviation':head,'mainReferenceAnchorGap':main_fixed_gap,'sourceAxisContactMin':contact_min,'sourceAxisContactMax':contact_max,'mainPoleTransverseGap':main_gap,'freeOffPoleTransverseGap':off_gap,'mainWithinNativeShaftBounds':bool(contact_min>=-.3*np.sqrt(2)*1.7 and contact_max<=.7*np.sqrt(2)*1.7),'mainUnderNativePalm015':main_gap<.015,'freeOffUnderNativePalm015':off_gap<.015})
 out=Path(args.output);out.parent.mkdir(parents=True,exist_ok=True);out.write_text(json.dumps({'localOnly':True,'scope':'Source qualification only; authored pole sliding is intentional, not a fixed(.70,.30,.5) grip failure. Do not reanchor weapon to wrist and erase thrust. Real native model/GPU/damage/clock/support still pending. No live58 renderer or resources changed.','rows':rows},indent=2)+'\n');print('Measured air spear16 source clips/root/bind/planted foot origins and dynamic pole contact, native gameplay/GPU pending');print('max gaps',max(x['mainPoleTransverseGap']for x in rows),max(x['freeOffPoleTransverseGap']for x in rows));print('native range all',all(x['mainWithinNativeShaftBounds']for x in rows))
if __name__=='__main__':main()
