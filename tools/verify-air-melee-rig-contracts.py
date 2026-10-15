"""Read-only qualification of adopted ordinary air attacks, excluding preview movement and cycles."""
import argparse, hashlib, importlib.util, json
from pathlib import Path
import numpy as np

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('air_melee_reader', ROOT/'tools/import-player-combat-art.py')
reader = importlib.util.module_from_spec(spec); spec.loader.exec_module(reader)

def main():
    parser = argparse.ArgumentParser(); parser.add_argument('--output', required=True); options = parser.parse_args()
    folder = ROOT/'art/player-animation-v1/air-attack/v01'
    assert json.loads((folder/'docs/APPROVAL.json').read_text())['adopted'] is True
    metadata = json.loads((folder/'exports/air-data.json').read_text())
    assert metadata['rootMotion'] is False and metadata['inputLocked'] is False
    selected = {weapon+'_attack_'+equip+'_'+side for weapon in ['sword','axe'] for equip in ['empty','carry'] for side in ['R','L']}
    rows = []
    for variant in ['standard','slim']:
        path = next((folder/'exports').glob(variant+'*.glb')); export = reader.Export(path)
        names = [export.nodes[i]['name'].split('/')[-1] for i in export.palette]
        assert len(export.joints) == 20
        for side in ['left','right']:
            assert all(side+'_'+anchor in names for anchor in ['palm_grip','sword_reference','axe_reference','shield_mount'])
        inverse = export.array(export.data['skins'][0]['inverseBindMatrices'])
        error = max(float(np.max(np.abs(export.rest[i]@inverse[i].reshape(4,4).T-np.eye(4)))) for i in range(20))
        assert error < 1e-5
        animations = {a['name']:a for a in export.data['animations']}; assert selected.issubset(animations)
        checks = []
        for name in sorted(selected):
            tracks = export.tracks(animations[name]); duration = max(float(t[2][-1]) for t in tracks)
            assert abs(duration-(.25 if name.startswith('sword') else .45)) < 1e-5
            root_error = 0
            for t in np.linspace(0,duration,round(duration*120)+1):
                worlds = export.world(tracks,float(t)); assert all(np.isfinite(m).all() for m in worlds)
                root_error = max(root_error,float(np.max(np.abs(worlds[0]-export.rest[0]))))
            assert root_error < 1e-5
            checks.append({'clip':name,'duration':duration,'rootDeviation':root_error})
        rows.append({'source':str(path.relative_to(ROOT)),'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),
                     'bindInverseError':error,'selectedSourceClips':checks,'sourceSkinBones':20,
                     'weaponReference':'select actual sword or axe reference per clip before mapping to runtime weapon slot'})
    target = Path(options.output); target.parent.mkdir(parents=True,exist_ok=True)
    target.write_text(json.dumps({'scope':'Source only; native AIR_SWING, physics, items, damage and GPU still require actual client verification',
                                  'localOnly':True,'variants':rows},ensure_ascii=False,indent=2)+'\n')
    print('PASS ordinary air source:',len(rows),'variants,',sum(len(r['selectedSourceClips']) for r in rows),'selected clips')

if __name__ == '__main__': main()
