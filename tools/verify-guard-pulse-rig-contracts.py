"""Read-only source mask qualification; success/blocking rules remain native."""
import argparse,hashlib,importlib.util,json
from pathlib import Path
import numpy as np
ROOT=Path(__file__).resolve().parents[1]
s=importlib.util.spec_from_file_location('reader',ROOT/'tools/import-player-combat-art.py');r=importlib.util.module_from_spec(s);s.loader.exec_module(r)
def main():
 p=argparse.ArgumentParser();p.add_argument('--output',required=True);a=p.parse_args();folder=ROOT/'art/player-animation-v1/perfect-guard/v01';assert json.loads((folder/'docs/APPROVAL.json').read_text())['adopted'] is True;rows=[]
 for variant in ['standard','slim']:
  path=next((folder/'exports').glob(variant+'*.glb'));e=r.Export(path);names=[e.nodes[i]['name'].split('/')[-1] for i in e.palette];assert len(e.joints)==20
  for side in ['R','L']:
   animation=next(x for x in e.data['animations'] if x['name']=='pulse_guard_'+side);tracks=e.tracks(animation);duration=max(float(x[2][-1]) for x in tracks);assert abs(duration-.4)<1e-6
   baseline=e.world(tracks,0);active='left_' if side=='R' else 'right_';changed=[];end=e.world(tracks,duration)
   for i,name in enumerate(names):
    deviation=0
    for t in np.linspace(0,duration,81):
     m=e.world(tracks,float(t))[i];assert np.isfinite(m).all();deviation=max(deviation,float(np.max(np.abs(m-baseline[i]))))
    allowed=name.startswith(active) and any(x in name for x in ['upper_arm','elbow','forearm','wrist','palm_grip','shield_mount','sword_reference'])
    if not allowed:assert deviation<1e-5,(name,deviation)
    if deviation>1e-5:changed.append(name)
    assert float(np.max(np.abs(end[i]-baseline[i])))<1e-5,(name,'not returned')
   assert any('shield_mount' in x for x in changed)
   rows.append({'source':str(path.relative_to(ROOT)),'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'clip':animation['name'],'duration':duration,'changedOnlyActiveArmAndAnchors':changed,'rootFixed':True,'returnsToStart':True})
 target=Path(a.output);target.parent.mkdir(parents=True,exist_ok=True);target.write_text(json.dumps({'localOnly':True,'scope':'source mask only; actual native successful block and limited standing overlay pending','clips':rows},ensure_ascii=False,indent=2)+'\n');print('PASS guard source: 2 variants/4 pulse clips, active arm only, root/opposite/body/legs fixed, closed return')
if __name__=='__main__':main()
