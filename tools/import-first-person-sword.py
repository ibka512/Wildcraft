#!/usr/bin/env python3
"""Offline conversion of adopted independent arms; no preview equipment/combos.

Original Blender export is immutable; optional generated resources remain local.
NumPy is an existing offline importer tool, never a runtime dependency.
"""
import argparse
import hashlib
import json
from pathlib import Path
import numpy as np

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'art/player-animation-v1/first-person-sword/v01/exports/first-person-data.json'
BONES = [s+'_'+b for s in ('right', 'left') for b in ('upper_arm', 'forearm', 'elbow_deform')]
CLIPS = ('A_R', 'A_L', 'shield_A_R', 'shield_A_L')


def rotation(q):
    # Blender writes WXYZ, unlike glTF/JOML.
    w, x, y, z = q
    return np.array([[1-2*(y*y+z*z), 2*(x*y-z*w), 2*(x*z+y*w)],
                     [2*(x*y+z*w), 1-2*(x*x+z*z), 2*(y*z-x*w)],
                     [2*(x*z-y*w), 2*(y*z+x*w), 1-2*(x*x+y*y)]])


def mesh(source):
    out = dict(part=source['part'], outer=source['outer'], positions=[], normals=[], uv=[], joints=[], weights=[], indices=[])
    for face, uv in zip(source['faces'], source['uvs']):
        points = np.array([source['vertices'][i] for i in face])
        normal = np.cross(points[1]-points[0], points[2]-points[0]); normal /= np.linalg.norm(normal)
        first = len(out['positions']) // 3
        for i, tex in zip(face, uv):
            out['positions'].extend(source['vertices'][i]); out['normals'].extend(normal.tolist())
            out['uv'].extend([tex[0], 1-tex[1]])
            pairs = [(BONES.index(b), w) for b, w in source['weights'][i].items() if w > 0]
            assert len(pairs) <= 4 and abs(sum(w for _, w in pairs)-1) < 1e-6
            pairs += [(0, 0)] * (4-len(pairs))
            out['joints'].extend(b for b, _ in pairs); out['weights'].extend(w for _, w in pairs)
        for k in range(1, len(face)-1): out['indices'].extend([first, first+k, first+k+1])
    return out


def convert():
    raw = SOURCE.read_bytes(); data = json.loads(raw)
    assert hashlib.sha256(raw).hexdigest() == '4c876cf0587a2fcdffd3029fa2abe67c8297161878edaf94462245764f594ee7'
    maximum = 0.0; result = {}
    for variant, source in data['variants'].items():
        for clip in source['clips'].values():
            for f in clip['frames']:
                for side in ('right', 'left'):
                    p = np.array(f['origins'][side+'_forearm']) + rotation(f['rotations'][side+'_forearm']) @ [0, 0, -.2425]
                    maximum = max(maximum, float(np.linalg.norm(p-np.array(f['grips'][side]))))
        clips = {}
        for name in CLIPS:
            src = source['clips'][name]; frames = []
            for f in src['frames']:
                frame = dict(bones=[], grips=[], swords=[], shields=[])
                for bone in BONES:
                    q = f['rotations'][bone]; frame['bones'].append(f['origins'][bone] + q[1:] + q[:1])
                for side in ('right', 'left'):
                    frame['grips'].append(f['grips'][side])
                    for target, key in (('swords', 'swordMountRotations'), ('shields', 'shieldMountRotations')):
                        q = f[key][side]; frame[target].append(q[1:] + q[:1])
                frames.append(frame)
            clips[name] = dict(duration=src['duration'], frames=frames)
        result[variant.lower()] = dict(schema=1, variant=variant.lower(), bones=BONES,
            pivots=[source['pivots'][b] for b in BONES], eye=data['camera']['eye'],
            meshes=[mesh(m) for m in source['meshes'].values()], clips=clips)
    assert maximum < .000002, maximum
    return result, dict(localOnly=True, source=str(SOURCE.relative_to(ROOT)), sourceSHA256=hashlib.sha256(raw).hexdigest(),
        adoptedBlendSHA256='b1ddc3e2d01d60215de0df24023cf38a8e759010e5ccd3903c2a43110e6f6dd2',
        sourcePalmClipsChecked=40, runtimeClips=8, sourcePalmMaximumError=maximum,
        scope='Native uncharged grounded sword SWING only; A 0.65–2.05 seconds mapped to native age. No combo gameplay.')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(); parser.add_argument('--output', type=Path, required=True); args = parser.parse_args()
    resources, report = convert(); args.output.mkdir(parents=True, exist_ok=True)
    for name, value in resources.items():
        (args.output/(name+'-first-person-sword.json')).write_text(json.dumps(value, separators=(',', ':'), allow_nan=False)+'\n')
    (args.output/'first-person-sword-provenance.json').write_text(json.dumps(report, indent=2)+'\n')
    print(json.dumps(report))
