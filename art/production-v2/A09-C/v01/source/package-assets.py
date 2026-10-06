"""Package the A09-C art draft, preserving the adopted and runtime baselines."""
from pathlib import Path
import csv, hashlib, json, zipfile
from PIL import Image

R = Path(__file__).resolve().parents[1]
P = R.parents[3]
def sha(p):
    return hashlib.sha256(p.read_bytes()).hexdigest()
def read(rel):
    return json.loads((R / rel).read_text())
def dump(path, value):
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n')

geometry = read('review/geometry-checks.json')
layout = read('review/layout-checks.json')
browser = read('review/browser-checks.json')
editable = read('review/editable-checks.json')
assert all(d['allPassed'] for d in [geometry, layout, browser, editable])
assert geometry['cases'] == 128 and browser['browserCases'] == 144 and editable['sceneCount'] == 8
assert len(layout['checks']) == 118
assert not (R / 'adoption.json').exists(), 'Do not overwrite an adopted draft'
baseline = read('references/baseline.json')
for entry in baseline:
    assert sha(P / entry['path']) == entry['sha256'], entry['path']
previous = P / 'art/production-v2/S01/v01'
assert json.loads((previous / 'adoption.json').read_text())['adopted']
for entry in json.loads((previous / 'manifest.json').read_text())['files']:
    assert sha(previous / entry['path']) == entry['sha256'], entry['path']
spec = read('exports/waist-layout.json')
assert not spec['adopted'] and not spec['integrated'] and not spec['gameRuntimeVerified']
assert len(spec['profiles']) == 5 and spec['categories'] == 3 and spec['newInventorySlots'] == 0
assert spec['newProductionModels'] == spec['newTextures'] == 0
assert all(p['backToHeldRatio'] == 1 for p in spec['profiles'].values())
html = (R / 'review/preview.html').read_text()
for rel in ['source/waist-engine.js', 'source/waist-review.js']:
    assert (R / rel).read_text() in html
for rel in ['source/reference-geometry.json', 'exports/waist-layout.json', 'references/adopted-back-layout.json', 'references/adopted-fuse-layout.json']:
    assert json.dumps(read(rel), ensure_ascii=False, separators=(',', ':')) in html
dimensions = {
    'waist-default': (920, 800), 'waist-cape-back': (920, 800), 'waist-elytra-back': (920, 800),
    'waist-five-items': (1280, 470), 'waist-pose-review': (1280, 1500),
    'same-scale-held': (600, 520), 'same-scale-waist': (600, 520),
}
for name, size in dimensions.items():
    with Image.open(R / 'review' / (name + '.png')) as im:
        assert im.size == size
        im.verify()
with (R / 'exports/waist-profile-map.csv').open(encoding='utf-8-sig', newline='') as f:
    assert len(list(csv.DictReader(f))) == 5
assert not list(R.rglob('*.aseprite')) and not list((R / 'exports').rglob('*.png'))
queue = P / 'art/production-docs/ART-DESIGN-PRODUCTION-SCHEDULE-2026-10-04.csv'
with queue.open(encoding='utf-8-sig', newline='') as f:
    reader = csv.DictReader(f)
    fields, rows = reader.fieldnames, list(reader)
assert next(row for row in rows if row['编号'] == 'A07-A08-C')['制作阶段状态'] == '计划未启动'
assert not (P / 'art/production-v2/A07-A08-C').exists()
for row in rows:
    if row['编号'] == 'A09-C':
        row['制作阶段状态'] = 'v01草稿交付；五类侧腰/8可编辑场景/交互与穿插参考齐全，待视觉采用，未接入'
with queue.open('w', encoding='utf-8-sig', newline='') as f:
    writer = csv.DictWriter(f, fieldnames=fields)
    writer.writeheader()
    writer.writerows(rows)
index = P / 'art/production-v2/README.md'
entry = '| A09-C 披风／鞘翅下的侧腰摆放 | v01 | 2026-10-06草稿待视觉采用；五类装备/8可编辑姿态场景、同尺度与避让规范齐全；新增生产模型贴图0，未接入 | [侧腰布局审稿与文件](A09-C/v01/README.md) |\n'
text = index.read_text()
if entry not in text:
    at = text.index('后续资产按')
    index.write_text(text[:at] + entry + '\n' + text[at:])
for rel, link in [('art/README.md', 'production-v2/A09-C/v01/README.md'), ('art/production-docs/README.md', '../production-v2/A09-C/v01/README.md')]:
    p = P / rel
    line = f'\n2026-10-06：[A09-C披风／鞘翅下的侧腰摆放v01]({link})草稿已交付；五类装备保持手持尺度，8可编辑场景与披风/鞘翅避让审稿齐全，待用户视觉采用，未接入。下一项A07-A08-C仍计划未启动。\n'
    if line not in p.read_text():
        p.write_text(p.read_text() + line)

verification = '''# A09-C v01 验证记录

2026-10-06。以下验证针对可编辑美术参考、文件交付和浏览器审稿；没有运行Minecraft验收。

- 五类原物件几何复用同一原始轮廓。118项布局检查通过，覆盖刚性尺度、手持与腰侧边长一致、三类组合、标准/细手臂、代理遮挡规则及Fuse范围。有效比例沿用第三人称手持，腰侧与手持尺寸比为1.00。
- 基础配置为80个单件和64个三类组合。Blender实际变换后的几何表面做128组BVH审计，包含四姿态、双手臂、披风/鞘翅、展开/摆幅端点、落定端点以及代表Fuse石头。代理表面、角色表面和当前三类物件未检出交叉；站立/蹲下最低点不低于检查地面。
- 表面交叉检查不证明任意模型体积包含、动画中间帧、真实披风布面或第三方物品不会穿插；这部分需按接入说明在游戏内检查。
- 浏览器144个配置真实WebGL渲染通过。另核对真实选择与背面按钮、旧隐藏规则、无披风/鞘翅回原背位、正在持握的类别、滑翔保留三类记录、Fuse/展开/落定端点、390px无横向溢出。共154项检查通过，零浏览器错误。
- 重新打开Blender交付源稿成功，8场景、每场景五类物件均完整，所有网格标记为参考，未保存改动。原采用源稿保持。
- 7张PNG来自实际浏览器画布，尺寸核对通过；交互页内嵌源代码、原几何及布局参数与交付文件一致。
- 16份原采用与运行基线哈希保持；S01采用记录、母带与分层清单以及历史审稿ZIP保持；未改Java、运行资源、正式JAR、真实物品归属、附件、数据、网络或依赖。

详细证据：[布局检查](review/layout-checks.json)、[几何检查](review/geometry-checks.json)、[浏览器检查](review/browser-checks.json)、[源稿重开检查](review/editable-checks.json)、[打包检查](review/package-checks.json)。

需要用户审稿决定的位置与观感：完整盾面、侧腰远程与近战的层次、蹲下上移、披风和鞘翅背部留空。本轮未采用、未接入；下一项A07-A08-C未启动。
'''
(R / 'verification.md').write_text(verification)
checks = {'asset': 'A09-C', 'allPassed': True, 'adopted': False, 'integrated': False, 'gameRuntimeVerified': False,
          'profiles': 5, 'editableScenes': 8, 'previewPng': 7, 'newProductionModels': 0, 'newTextures': 0,
          'baseSingleItemCases': 80, 'baseThreeRecordCases': 64, 'geometryScenarioCases': 128, 'layoutCases': 118,
          'actualBrowserRenderCases': 144, 'browserChecks': 154, 'baselineFilesUnchanged': 16,
          'previousAssetAdoptedAndManifestUnchanged': True, 'embeddedGeometryConfigAndSourceExact': True,
          'readableEditableSource': True, 'nextAssetUntouched': True}
dump(R / 'review/package-checks.json', checks)
files = [{'path': str(p.relative_to(R)), 'bytes': p.stat().st_size, 'sha256': sha(p)}
         for p in sorted(R.rglob('*')) if p.is_file() and p.name != 'manifest.json' and '__pycache__' not in p.parts]
manifest = {**checks, 'version': 'v01', 'date': '2026-10-06', 'status': 'draft_for_visual_review',
            'humanVisualApproved': False, 'nextAfterAdoption': 'A07-A08-C 探索动作衔接复核', 'files': files}
dump(R / 'manifest.json', manifest)
archive = R.parent / 'A09-C-披风鞘翅侧腰摆放-v01-审稿包.zip'
prefix = 'A09-C-披风鞘翅侧腰摆放-v01/'
with zipfile.ZipFile(archive, 'w', zipfile.ZIP_DEFLATED, compresslevel=7) as z:
    for entry in files:
        z.write(R / entry['path'], prefix + entry['path'])
    z.write(R / 'manifest.json', prefix + 'manifest.json')
with zipfile.ZipFile(archive) as z:
    assert z.testzip() is None
    for entry in files:
        assert hashlib.sha256(z.read(prefix + entry['path'])).hexdigest() == entry['sha256']
receipt = {'asset': 'A09-C', 'allPassed': True, 'zipBytes': archive.stat().st_size, 'zipSha256': sha(archive),
           'manifestSha256': sha(R / 'manifest.json'), 'zipEntries': len(files) + 1, 'zipCRCAndFileHashesVerified': True}
dump(R.parent / 'A09-C-v01-打包校验.json', receipt)
print(json.dumps({'allPassed': True, 'files': len(files), 'zipBytes': archive.stat().st_size,
                  'archive': str(archive), 'adopted': False, 'integrated': False}, ensure_ascii=False))
