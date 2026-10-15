"""Read-only adopted axe gesture qualification; native mount/model/use must be verified in game."""
import argparse,hashlib,importlib.util,json
from pathlib import Path
import numpy as np
ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('axe_transition_reader',ROOT/'tools/import-player-combat-art.py');reader=importlib.util.module_from_spec(spec);spec.loader.exec_module(reader)
def main():
    parser=argparse.ArgumentParser();parser.add_argument('--output',required=True);options=parser.parse_args();folder=ROOT/'art/player-animation-v1/axe-transitions/v01'
    assert json.loads((folder/'docs/APPROVAL.json').read_text())['adopted'] is True
    metadata=json.loads((folder/'exports/transition-data.json').read_text());assert metadata['bones']==22 and metadata['gripLocalOffset']==[0,0,-.0625]
    selected={f'{phase}_{mount}_{equip}_{side}' for phase in ['draw','stow'] for mount in ['back','waist'] for equip in ['empty','shield'] for side in ['R','L']};rows=[]
    for variant in ['standard','slim']:
        path=folder/'exports'/f'{variant}-axe-transitions.glb';e=reader.Export(path);assert len(e.joints)==22
        names=[e.nodes[i]['name'].split('/')[-1] for i in e.palette];assert all(side+'_'+suffix in names for side in ['left','right'] for suffix in ['wrist','palm_grip','axe_reference','shield_mount'])
        inverse=e.array(e.data['skins'][0]['inverseBindMatrices']);error=max(float(np.max(np.abs(e.rest[i]@inverse[i].reshape(4,4).T-np.eye(4)))) for i in range(22));assert error<1e-5
        animations={a['name']:a for a in e.data['animations']};assert selected==set(animations);checks=[]
        for name in sorted(selected):
            tracks=e.tracks(animations[name]);duration=max(float(t[2][-1]) for t in tracks);assert abs(duration-(.34 if name.startswith('draw_') else .38))<1e-5;start=e.world(tracks,0);root_error=feet_error=head_error=grip_error=0
            side="right" if name.endswith("R") else "left"
            for t in np.linspace(0,duration,40):
                worlds=e.world(tracks,float(t));assert all(np.isfinite(m).all() for m in worlds);root_error=max(root_error,float(np.max(np.abs(worlds[0]-e.rest[0]))))
                for bone in ['left_foot','right_foot']:
                    i=names.index(bone);feet_error=max(feet_error,float(np.max(np.abs(worlds[i]-start[i]))))
                wrist=worlds[names.index(side+"_wrist")];mount=worlds[names.index(side+"_axe_reference")];grip_error=max(grip_error,float(np.linalg.norm((wrist@np.array([0,.0625,0,1]))[:3]-(mount@np.array([0,0,0,1]))[:3])))
                i=names.index('head');head_error=max(head_error,float(np.max(np.abs(worlds[i][:3,:3]-start[i][:3,:3]))))
            assert root_error<1e-5 and feet_error<1e-5 and head_error<1e-5, (variant,name,root_error,feet_error,head_error)
            if "_back_" in name:assert grip_error<.002, (variant,name,grip_error)
            checks.append({'clip':name,'duration':duration,'rootDeviation':root_error,'feetDeviation':feet_error,'headRotationDeviation':head_error,'authoredWristAxeHiltGap':grip_error,'selectedForDefaultBack':'_back_' in name,'gripQualifiedUnderBackThreshold':grip_error<.002})
        rows.append({'source':str(path.relative_to(ROOT)),'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'bindInverseError':error,'sourceClips':checks})
    out=Path(options.output);out.parent.mkdir(parents=True,exist_ok=True);out.write_text(json.dumps({'localOnly':True,'scope':'Only root/bind/foot/head qualification32 and default-back grip16; waist grip report only and excluded; actual main axe reference and current .34/.38 Settle, native axe/shield real grip and attack/wear/Fuse/GPU pending; not exact hilt handoff or formal first-person','variants':rows},ensure_ascii=False,indent=2)+'\n');print('PASS axe source32 root/bind/feet/head and default-back16 grip; waist grip report-only excluded, runtime pending')
if __name__=='__main__':main()
