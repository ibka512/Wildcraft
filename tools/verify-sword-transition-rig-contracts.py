"""Read-only qualification of adopted bounded sword gestures; native ownership still needs runtime proof."""
import argparse, hashlib, importlib.util, json
from pathlib import Path
import numpy as np
ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('transition_reader',ROOT/'tools/import-player-combat-art.py')
reader=importlib.util.module_from_spec(spec);spec.loader.exec_module(reader)

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--output',required=True);options=parser.parse_args()
    folder=ROOT/'art/player-animation-v1/equipment-transitions/v01'
    approval=json.loads((folder/'docs/APPROVAL.json').read_text());assert approval['date']=='2026-10-08' and approval['quote'].startswith('采用')
    metadata=json.loads((folder/'exports/transition-data.json').read_text())
    assert metadata['ownership'].startswith('instant;') and 'not an exact anatomical' in metadata['gesture']
    selected={phase+'_'+mount+'_'+equip+'_'+side for phase in ['draw','stow'] for mount in ['back','waist'] for equip in ['empty','shield'] for side in ['R','L']}
    rows=[]
    for variant in ['standard','slim']:
        path=folder/'exports'/f'{variant}-equipment-transitions.glb';export=reader.Export(path);assert len(export.joints)==20
        names=[export.nodes[i]['name'].split('/')[-1] for i in export.palette];assert all(side+'_'+anchor in names for side in ['left','right'] for anchor in ['palm_grip','sword_reference','shield_mount'])
        inverse=export.array(export.data['skins'][0]['inverseBindMatrices']);error=max(float(np.max(np.abs(export.rest[i]@inverse[i].reshape(4,4).T-np.eye(4)))) for i in range(20));assert error<1e-5
        animations={a['name']:a for a in export.data['animations']};assert selected==set(animations)
        checks=[]
        for name in sorted(selected):
            tracks=export.tracks(animations[name]);duration=max(float(row[2][-1]) for row in tracks);assert abs(duration-(.34 if name.startswith('draw') else .38))<1e-5
            start=export.world(tracks,0);root_error=0;feet_error=0;head_error=0
            for t in np.linspace(0,duration,round(duration*120)+1):
                worlds=export.world(tracks,float(t));assert all(np.isfinite(m).all() for m in worlds)
                root_error=max(root_error,float(np.max(np.abs(worlds[0]-export.rest[0]))))
                for foot in ['left_foot','right_foot']:
                    i=names.index(foot);feet_error=max(feet_error,float(np.max(np.abs(worlds[i]-start[i]))))
                i=names.index('head');head_error=max(head_error,float(np.max(np.abs(worlds[i][:3,:3]-start[i][:3,:3]))))
            assert root_error<1e-5 and feet_error<1e-5 and head_error<1e-5
            checks.append({'clip':name,'duration':duration,'rootDeviation':root_error,'feetMatrixDeviation':feet_error,'headRotationDeviation':head_error,'selectedForDefaultBackRuntime':'_back_' in name})
        rows.append({'source':str(path.relative_to(ROOT)),'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'bindInverseError':error,'sourceClips':checks})
    target=Path(options.output);target.parent.mkdir(parents=True,exist_ok=True);target.write_text(json.dumps({'localOnly':True,'scope':'Source only; no runtime promotion, ownership/physical hilt handoff/native models/GPU/first person are not proved by this check','variants':rows},ensure_ascii=False,indent=2)+'\n')
    print('PASS adopted sword transition source:',len(rows),'variants;',sum(len(r['sourceClips']) for r in rows),'clips; default back candidates only, runtime pending')

if __name__=='__main__':main()
