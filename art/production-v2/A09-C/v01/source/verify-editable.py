"""Reopen and check the delivered reference file without saving it."""
from pathlib import Path
import bpy, json

root = Path(__file__).resolve().parents[1]
bpy.ops.wm.open_mainfile(filepath=str(root / 'source/waist-placement-v01.blend'))
expected = {'A09C_' + variant + '_' + pose for variant in ['standard', 'slim'] for pose in ['stand', 'crouch', 'swim', 'glide']}
checks = []
for scene in bpy.data.scenes:
    meshes = [o for o in scene.objects if o.type == 'MESH']
    checks.append({'scene': scene.name, 'meshObjects': len(meshes),
                   'referenceOnly': all(o.get('referenceOnly') for o in meshes),
                   'fiveItems': all(any(o.name.startswith('ITEM_' + item) for o in meshes) for item in ['sword', 'axe', 'shield', 'bow', 'crossbow']),
                   'notAdopted': scene.get('adopted') is False,
                   'proxyGuides': bool(scene.get('capeAndElytraAreProxies')),
                   'engine': scene.render.engine})
passed = set(bpy.data.scenes.keys()) == expected and all(c['meshObjects'] > 10 and c['referenceOnly'] and c['fiveItems'] and c['notAdopted'] and c['proxyGuides'] and c['engine'] == 'BLENDER_WORKBENCH' for c in checks)
receipt = {'allPassed': passed, 'sceneCount': len(checks), 'openedWithoutSaving': True, 'checks': checks, 'gameRuntimeVerified': False}
(root / 'review/editable-checks.json').write_text(json.dumps(receipt, ensure_ascii=False, indent=2) + '\n')
print(json.dumps({'allPassed': passed, 'sceneCount': len(checks)}))
if not passed:
    raise RuntimeError('Delivered reference file failed reopen verification')
