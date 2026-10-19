"""Append approved sword waist clips without regenerating any previously frozen animation."""
import argparse
import hashlib
import importlib.util
import json
from pathlib import Path

import numpy as np

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('combat_reader', ROOT / 'tools/import-player-combat-art.py')
reader = importlib.util.module_from_spec(spec)
spec.loader.exec_module(reader)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--write', action='store_true', help='Write local candidate data after source checks')
    parser.add_argument('--report', required=True)
    options = parser.parse_args()
    folder = ROOT / 'art/player-animation-v1/equipment-transitions/v01'
    approval = json.loads((folder / 'docs/APPROVAL.json').read_text())
    assert approval['date'] == '2026-10-08' and approval['quote'].startswith('采用')
    target = ROOT / 'src/main/resources/assets/wildcraft/animation'
    additions = {}
    rows = []
    for variant in ['standard', 'slim']:
        path = target / f'{variant}-combat-rig.json'
        data = json.loads(path.read_text())
        names = data['bones']
        rest = []
        for pose in data['bindWorld']:
            matrix = np.eye(4)
            matrix[:3, :3] = reader.rotation(pose[3:7]) * np.array(pose[7:10])[None, :]
            matrix[:3, 3] = pose[:3]
            rest.append(matrix)
        export = reader.Export(folder / 'exports' / f'{variant}-equipment-transitions.glb')
        selected = [f'{phase}_waist_{equipment}_{side}' for phase in ['draw', 'stow']
                    for equipment in ['empty', 'shield'] for side in ['R', 'L']]
        clips = export.clips(selected, names, rest)
        source_names = [export.nodes[i]['name'].split('/')[-1] for i in export.palette]
        for name, clip in clips.items():
            side = 'right' if name.endswith('_R') else 'left'
            width = .09375 if variant == 'slim' else .125
            animation = next(a for a in export.data['animations'] if a['name'] == name)
            tracks = export.tracks(animation)
            physical_extent = 0
            for t in np.linspace(0, clip['duration'], round(clip['duration'] * 120) + 1):
                world = export.world(tracks, float(t))
                # The skin's distal hand is part of the adopted forearm. A derived
                # 22-joint wrist offset is NOT a physical palm definition for this 20-joint source.
                grip = np.linalg.inv(world[source_names.index(side + '_forearm')]) @ world[source_names.index(side + '_sword_reference')][:, 3]
                assert -.385 < grip[1] < -.24 and abs(grip[0]) < width + .01 and abs(grip[2]) < .135, (variant, name, grip)
                physical_extent = max(physical_extent, float(np.linalg.norm(grip[:3])))
            clip['loop'] = False
            key = 'equipment_' + name
            if key in data['clips']:
                assert data['clips'][key] == clip, 'Never overwrite an earlier candidate with different source values'
            additions.setdefault(variant, {})[key] = clip
            rows.append({'variant': variant, 'clip': key, 'physicalHandBoundsPassed': True, 'maxGripLocalRadius': physical_extent})
        old = {k: v for k, v in data['clips'].items() if k not in additions[variant]}
        assert len(old) == 337, 'Expected the frozen dev67 baseline; do not discard newer source slices'
        data['clips'].update(additions[variant])
        if options.write:
            path.write_text(json.dumps(data, separators=(',', ':')) + '\n')
    if options.write:
        path = target / 'provenance.json'
        provenance = json.loads(path.read_text())
        for variant in ['standard', 'slim']:
            source = folder / 'exports' / f'{variant}-equipment-transitions.glb'
            entry = {'source': str(source.relative_to(ROOT)), 'sha256': hashlib.sha256(source.read_bytes()).hexdigest(),
                     'use': 'C6AT bounded sword waist draw/stow, visible native cape and empty armor slots only; actual same-reference instant ownership and original .34/.38 clock; runtime status in docs/C6AT-SWORD-WAIST-VERIFICATION.md; source report alone is not proof',
                     'excluded': 'reference meshes/textures, elytra/armor, other weapons, formal first person, exact anatomical hilt handoff'}
            if entry not in provenance['sources']:
                provenance['sources'].append(entry)
        path.write_text(json.dumps(provenance, ensure_ascii=False, indent=2) + '\n')
    report = Path(options.report)
    report.parent.mkdir(parents=True, exist_ok=True)
    report.write_text(json.dumps({'localOnly': True, 'runtimeProved': False, 'sourceChecks': rows}, indent=2) + '\n')
    print('PASS', len(rows), 'source clips; prior 337 clips and geometry preserved; runtime proof pending')


if __name__ == '__main__':
    main()
