"""Verify and package the exploration art review without changing runtime assets."""
from pathlib import Path
import csv,hashlib,json,zipfile
from PIL import Image
R=Path(__file__).resolve().parents[1];P=R.parents[3]
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def read(rel):return json.loads((R/rel).read_text())
def dump(p,d):p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n')
assert not (R/'adoption.json').exists(),'Do not overwrite an adopted version'
transition=read('review/transition-checks.json');browser=read('review/browser-checks.json');editable=read('review/editable-checks.json');video=read('review/video-checks.json')
assert all(d['allPassed']for d in [transition,browser,editable,video])
assert len(transition['checks'])==291 and transition['sampledFrames']==10416
assert browser['browserConfigurations']==480 and browser['actualRenderCalls']==1440 and len(browser['checks'])==487
assert editable['sceneCount']==24 and editable['sampledVertexComparisons']==3536 and editable['maximumVertexErrorBlocks']<1e-5
for entry in read('references/baseline.json'):assert sha(P/entry['path'])==entry['sha256'],entry['path']
previous=P/'art/production-v2/A09-C/v01'
assert json.loads((previous/'adoption.json').read_text())['adopted']
for entry in json.loads((previous/'manifest.json').read_text())['files']:assert sha(previous/entry['path'])==entry['sha256'],entry['path']
spec=read('exports/transition-spec.json');tracks=read('exports/transition-tracks.json')
assert len(spec['scenarios'])==12 and len(tracks['scenes'])==24 and all(len(s['frames'])==109 for s in tracks['scenes'])
assert not spec['adopted'] and not spec['integrated'] and not spec['gameRuntimeVerified']
assert spec['newProductionModels']==spec['newTextures']==spec['newAuthoredClipSets']==0
html=(R/'review/preview.html').read_text()
for rel in ['source/transition-engine.js','source/motion-viewer.js','source/motion-review.js','references/waist-engine.js']:assert (R/rel).read_text()in html
for rel in ['references/runtime-clips.json','source/reference-geometry.json','references/adopted-waist.json','references/adopted-back.json','references/adopted-fuse.json','references/first-person-frame.json','exports/transition-spec.json']:assert json.dumps(read(rel),ensure_ascii=False,separators=(',',':'))in html
pngs={
 'motion-current':(620,640),'motion-revised':(620,640),'motion-first':(960,540),'motion-board':(1200,1490),
 'motion-sword-takeover':(620,640),'motion-bow-takeover':(620,640),'release-current':(620,640),'release-revised':(620,640)}
for name,size in pngs.items():
    with Image.open(R/'review'/(name+'.png'))as im:assert im.size==size;im.verify()
assert not list(R.rglob('*.aseprite')) and not list((R/'exports').rglob('*.png'))
queue=P/'art/production-docs/ART-DESIGN-PRODUCTION-SCHEDULE-2026-10-04.csv'
with queue.open(encoding='utf-8-sig',newline='')as f:reader=csv.DictReader(f);fields=reader.fieldnames;rows=list(reader)
assert next(row for row in rows if row['编号']=='A13-C')['制作阶段状态']=='计划未启动'
assert not (P/'art/production-v2/A13-C').exists()
for row in rows:
    if row['编号']=='A07-A08-C':row['制作阶段状态']='v01草稿交付；12场景/24可编辑时间线/动作对照与即时接管规范齐全，待视觉采用，未接入'
with queue.open('w',encoding='utf-8-sig',newline='')as f:w=csv.DictWriter(f,fieldnames=fields);w.writeheader();w.writerows(rows)
entry='| A07-A08-C 探索动作衔接复核 | v01 | 2026-10-06草稿待视觉采用；12场景/24可编辑时间线、短路径与即时接管规范齐全；新增整套动作/生产模型贴图0，未接入 | [探索动作审稿与文件](A07-A08-C/v01/README.md) |\n'
index=P/'art/production-v2/README.md';text=index.read_text()
if entry not in text:
    at=text.index('后续资产按');index.write_text(text[:at]+entry+'\n'+text[at:])
for rel,link in [('art/README.md','production-v2/A07-A08-C/v01/README.md'),('art/production-docs/README.md','../production-v2/A07-A08-C/v01/README.md')]:
    p=P/rel;line=f'\n2026-10-06：[A07-A08-C探索动作衔接v01]({link})草稿已交付，12场景/24可编辑时间线、当前采样与修订对照、第一人称占用及即时接管规范齐全；待用户视觉采用，未接入游戏。A13-C专注混音仍计划未启动。\n'
    if line not in p.read_text():p.write_text(p.read_text()+line)
verification=f'''# A07-A08-C v01 验证记录

2026-10-06。仅验证美术参考数据、衔接与审稿交付；没有运行Minecraft输入、物理或多人验收。

- 本机源码与采用烘焙检查：release原19帧，现200ms只取0～6；两套手臂4→5有约353°Euler跨界，见[source-audit.json](review/source-audit.json)。这是文件证据，非新游戏录像。
- 291项参考检查通过，累计采样10,416帧（12场景×2手臂×2主手×217时刻）：有限/单位旋转、零实体根运动、真实引用测试夹具、无同引用手背重复、触发点接续、即时拉弓/举盾/取剑、备用物件身份、快速重开、退出与入伞端点、减少过渡及第一人称握点。
- 原始release4.5帧的长路径与短路径角度对照核对通过：原插值中点离原第4帧约176.5°；修订中点仅约5.0°，保持原两端姿态。
- 24场景的Blender源稿重新打开成功。5个关键时刻累计3,536次顶点对照，与参考数据最大位置误差{editable['maximumVertexErrorBlocks']:.3g}方块；显隐误差0。显隐关键帧为阶跃，骨骼采样之间使用线性关键帧。
- 浏览器480种配置、1,440次三视图真实WebGL绘制通过，含12场景×双手臂×左右主手×两挂位×5时刻；实际场景/触发点/逐帧/播放/暂停、减少过渡与390px无横溢出，共487项。零浏览器错误。
- 初次自动暂停检查时片段已播放到结尾，点击行为实际重新开始；按四分之一速度和真实键盘操作重新核对暂停通过。这是验证时序修正，不改变产品播放逻辑。
- 8PNG来自实际浏览器画布；108帧/30fps/3.6s/1240×740的H.264对照视频完整解码通过，无音轨，未混入上一音效资产。视频是审稿采样，非实时游戏。
- 23文件运行/采用基线哈希保持；A09-C采用文件清单和历史审稿包保持；未修改Java、原动作烘焙、游戏贴图、正式JAR、附件、数据、网络或依赖。下一项A13-C未启动。

详细证据：[衔接检查](review/transition-checks.json)、[浏览器检查](review/browser-checks.json)、[源稿复开与顶点对照](review/editable-checks.json)、[视频检查](review/video-checks.json)、[交付检查](review/package-checks.json)。

本检查没有对真实墙面接触、移动身体/相机、原版物件显示上下文、盔甲袖层或第三方模型做完整连续碰撞证明。真实原版setupAnim必须作为每帧基准；参考中立站姿不能替代动态原版状态。游戏的同引用归属与网络时序仍使用现有实现，测试夹具不构成真实多人验收。本版待用户视觉采用，未接入。
'''
(R/'verification.md').write_text(verification)
checks={'asset':'A07-A08-C','allPassed':True,'adopted':False,'integrated':False,'gameRuntimeVerified':False,
        'scenarios':12,'editableTimelines':24,'localJoinTemplates':3,'newAuthoredClipSets':0,'newProductionModels':0,'newTextures':0,
        'previewPng':8,'previewVideo':1,'transitionChecks':291,'sampledFrames':10416,'browserConfigurations':480,'actualBrowserRenderCalls':1440,'browserChecks':487,
        'editableVertexComparisons':3536,'baselineFilesUnchanged':23,'previousAssetAdoptedAndManifestUnchanged':True,'nextAssetUntouched':True,'embeddedSourceAndConfigsExact':True}
dump(R/'review/package-checks.json',checks)
files=[{'path':str(p.relative_to(R)),'bytes':p.stat().st_size,'sha256':sha(p)}for p in sorted(R.rglob('*'))if p.is_file()and p.name!='manifest.json'and '__pycache__'not in p.parts]
dump(R/'manifest.json',{**checks,'version':'v01','date':'2026-10-06','status':'draft_for_visual_review','humanVisualApproved':False,'nextAfterAdoption':'A13-C 专注音效混音复核','files':files})
archive=R.parent/'A07-A08-C-探索动作衔接复核-v01-审稿包.zip';prefix='A07-A08-C-探索动作衔接复核-v01/'
with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED,compresslevel=7)as z:
    for f in files:z.write(R/f['path'],prefix+f['path'])
    z.write(R/'manifest.json',prefix+'manifest.json')
with zipfile.ZipFile(archive)as z:
    assert z.testzip()is None
    for f in files:assert hashlib.sha256(z.read(prefix+f['path'])).hexdigest()==f['sha256']
dump(R.parent/'A07-A08-C-v01-打包校验.json',{'asset':'A07-A08-C','allPassed':True,'zipBytes':archive.stat().st_size,'zipSha256':sha(archive),'manifestSha256':sha(R/'manifest.json'),'zipEntries':len(files)+1,'zipCRCAndFileHashesVerified':True})
print(json.dumps({'allPassed':True,'files':len(files),'zipBytes':archive.stat().st_size,'archive':str(archive),'adopted':False,'integrated':False},ensure_ascii=False))
