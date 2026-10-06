from pathlib import Path
import json,hashlib,zipfile,re,base64,csv
from PIL import Image
R=Path(__file__).resolve().parents[1];P=R.parents[3];A=R.parents[1]/'F03-03/v01'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def dump(p,d):p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n')
for n in ['feedback-checks.json','browser-checks.json','render-checks.json']:assert json.loads((R/'review'/n).read_text())['allPassed']
for f in json.loads((R/'references/code-baseline.json').read_text()):assert sha(P/f['path'])==f['sha256']
for f in json.loads((R/'references/provenance.json').read_text()):assert sha(Path(f['source']))==sha(R/f['copy'])==f['sha256']
am=json.loads((A/'manifest.json').read_text());assert am['adopted'] and not am['integrated'];assert all(sha(A/f['path'])==f['sha256'] for f in am['files'])
assert sha(A.parent/'F03-03-未知材料通用回退外观-v01-审稿包.zip')=='70801fe8efb395f75f7790a3a39f99c46c2898d53c5c7d08ce4e8339572e1fb5'
native=json.loads((R/'references/native-particle-provenance.json').read_text());assert sha(Path(native['archive']))==native['archiveSha256']
with zipfile.ZipFile(native['archive']) as z:
 for entry in native['checkedResources']:assert hashlib.sha256(z.read(entry['entry'])).hexdigest()==entry['sha256']
assert not list((R/'exports').glob('*.png')) and not list(R.rglob('*.aseprite'))
assert not (R/'adoption.json').exists()
assert Image.open(R/'review/fuse-feedback-review.png').size==(1200,1680)
for n in ['fire','ice','elastic','fuse','split']:
 assert Image.open(R/'review'/(n+'-live.png')).size==(480,270);assert Image.open(R/'review'/(n+'-detail.png')).size==(192,144)
html=(R/'review/preview.html').read_text()
for n in ['feedback-engine.js','feedback-review.js']:assert (R/'source'/n).read_text() in html
assert (R/'exports/fuse-feedback-presets.json').read_text() in html
atlas=re.search(r'ATLAS=("data:image/png;base64,[^"]+")',html).group(1);assert base64.b64decode(json.loads(atlas).split(',')[1])==(R/'references/adopted-fuse-atlas.png').read_bytes()
with (P/'art/production-docs/ART-DESIGN-PRODUCTION-SCHEDULE-2026-10-04.csv').open(encoding='utf-8-sig') as f:rows={r['编号']:r for r in csv.DictReader(f)}
assert rows['S01']['制作阶段状态']=='计划未启动'
checks={'asset':'F03-04','allPassed':True,'adopted':False,'integrated':False,'motionPresets':5,'editableMotionJSFiles':2,'newOriginalSprites':0,'productionPNGFiles':0,'asepriteFiles':0,'productionModelFiles':0,'soundFiles':0,'nativeParticlesVerifiedLocally':4,'rawMinecraftFilesCopied':False,'eventLogicCases':90,'timeSamples':245,'highFrequencyEvents':1200,'maximumExtraParticlesInScene':48,'actualBrowserPresetSettingCases':15,'actualCrosshairReferenceCanvasCases':990,'previousAssetAdopted':True,'previousArchiveUnchanged':True,'referencesAndCodeLocalesJarUnchanged':True,'previewSourceAndConfigEmbeddedExact':True,'nextAssetUntouched':True,'gameRuntimeVerified':False}
dump(R/'review/package-checks.json',checks)
m={**checks,'version':'v01','date':'2026-10-06','status':'draft_for_visual_review','newSpritesNeeded':False,'nativeReuseRequiresFutureProviderIntegration':True,'normalRateExtraFeedbackSeconds':[0.3,0.4],'loop':False,'flash':False,'screenFilter':False,'arrowTrail':False,'nextAfterAdoption':'S01 音效配套','files':[{'path':str(p.relative_to(R)),'bytes':p.stat().st_size,'sha256':sha(p)} for p in sorted(R.rglob('*')) if p.is_file() and p.name!='manifest.json' and '__pycache__' not in p.parts]};dump(R/'manifest.json',m)
archive=R.parent/'F03-04-特殊材料及融合过程反馈-v01-审稿包.zip';prefix='F03-04-Fuse短反馈-v01/'
with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED,compresslevel=7) as z:
 for f in m['files']:z.write(R/f['path'],prefix+f['path'])
 z.write(R/'manifest.json',prefix+'manifest.json')
with zipfile.ZipFile(archive) as z:
 assert z.testzip() is None
 for f in m['files']:assert hashlib.sha256(z.read(prefix+f['path'])).hexdigest()==f['sha256']
dump(R.parent/'F03-04-v01-打包校验.json',{'asset':'F03-04','allPassed':True,'zipBytes':archive.stat().st_size,'zipSha256':sha(archive),'manifestSha256':sha(R/'manifest.json'),'zipEntries':len(m['files'])+1,'zipCRCAndFileHashesVerified':True})
print(json.dumps({'allPassed':True,'files':len(m['files']),'zipBytes':archive.stat().st_size,'archive':str(archive),'adopted':False,'integrated':False},ensure_ascii=False))
