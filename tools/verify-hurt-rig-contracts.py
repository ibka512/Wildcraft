"""Read-only adopted ordinary recoil source, stripping stationary pelvis buffering for an upper layer."""
import argparse, hashlib, importlib.util, json
from pathlib import Path
import numpy as np
ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('hurt_reader',ROOT/'tools/import-player-combat-art.py')
reader=importlib.util.module_from_spec(spec);spec.loader.exec_module(reader)

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--output',required=True);options=parser.parse_args()
    folder=ROOT/'art/player-animation-v1/hurt-reaction/v01';approval=json.loads((folder/'docs/APPROVAL.json').read_text());assert approval['date']=='2026-10-08' and approval['quote'].startswith('采用')
    metadata=json.loads((folder/'exports/hurt-data.json').read_text());assert metadata['rootMotion'] is False and metadata['inputLocked'] is False and metadata['nominalWorldTicks']==6
    selected={f'chest_{equip}_{direction}_{side}' for equip in ['empty','carry','guard'] for direction in ['front','back','left','right'] for side in ['R','L']}
    rows=[]
    for variant in ['standard','slim']:
        path=folder/'exports'/f'{variant}-hurt-reaction.glb';export=reader.Export(path);assert len(export.joints)==20
        names=[export.nodes[i]['name'].split('/')[-1] for i in export.palette];pelvis=names.index('pelvis');head=names.index('head')
        inverse=export.array(export.data['skins'][0]['inverseBindMatrices']);error=max(float(np.max(np.abs(export.rest[i]@inverse[i].reshape(4,4).T-np.eye(4)))) for i in range(20));assert error<1e-5
        animations={a['name']:a for a in export.data['animations']};assert len(animations)==48 and selected.issubset(animations)
        checks=[]
        for name in sorted(selected):
            tracks=export.tracks(animations[name]);duration=max(float(t[2][-1]) for t in tracks);assert abs(duration-.3)<1e-5
            start=export.world(tracks,0);end=export.world(tracks,duration);return_error=max(float(np.max(np.abs(a-b))) for a,b in zip(start,end));assert return_error<1e-5
            root_error=head_error=delta_max=rigid_error=0
            for t in np.linspace(0,duration,61):
                worlds=export.world(tracks,float(t));assert all(np.isfinite(m).all() for m in worlds)
                root_error=max(root_error,float(np.max(np.abs(worlds[0]-export.rest[0]))))
                head_error=max(head_error,float(np.max(np.abs(worlds[head][:3,:3]-start[head][:3,:3]))))
                strip=start[pelvis]@np.linalg.inv(worlds[pelvis]);body=names.index('body');body_delta=strip@worlds[body]@np.linalg.inv(start[body])
                for i,bone in enumerate(names):
                    if bone=='body' or any(tag in bone for tag in ['arm','elbow','palm','reference','mount']):
                        delta=strip@worlds[i]@np.linalg.inv(start[i]);assert np.isfinite(delta).all();delta_max=max(delta_max,float(np.max(np.abs(delta-np.eye(4)))));rigid_error=max(rigid_error,float(np.max(np.abs(delta-body_delta))))
            assert root_error<1e-5 and head_error<1e-5 and delta_max>1e-3 and rigid_error<1e-5
            checks.append({'clip':name,'duration':duration,'rootDeviation':root_error,'headRotationDeviation':head_error,'returnError':return_error,'strippedUpperDeltaMax':delta_max,'allArmItemRigidDeltaError':rigid_error})
        rows.append({'source':str(path.relative_to(ROOT)),'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'sourceSkinBones':20,'bindInverseError':error,'selectedSourceClips':checks})
    target=Path(options.output);target.parent.mkdir(parents=True,exist_ok=True);target.write_text(json.dumps({'localOnly':True,'scope':'Source only, renderer/current native movement, real damage-direction/physics/items/GPU not yet proved; no new hit-region rule','variants':rows},ensure_ascii=False,indent=2)+'\n')
    print('PASS ordinary hurt source: 2 variants,48 selected chest clips, source roots/return/head and stripped upper delta only; runtime pending')

if __name__=='__main__':main()
