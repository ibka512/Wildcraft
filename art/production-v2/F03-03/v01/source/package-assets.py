from pathlib import Path
import json,hashlib,zipfile,re,base64
from PIL import Image
R=Path(__file__).resolve().parents[1];P=R.parents[3];A=R.parents[1]/'F03-02/v01'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def dump(p,d):p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n')
for n in ['pixel-checks.json','geometry-checks.json','render-checks.json','browser-checks.json']:assert json.loads((R/'review'/n).read_text())['allPassed']
for f in json.loads((R/'references/code-baseline.json').read_text()):assert sha(P/f['path'])==f['sha256']
for f in json.loads((R/'references/provenance.json').read_text()):assert sha(Path(f['source']))==sha(R/f['copy'])==f['sha256']
am=json.loads((A/'manifest.json').read_text());assert am['adopted'] and not am['integrated'];assert all(sha(A/f['path'])==f['sha256'] for f in am['files'])
assert sha(A.parent/'F03-02-融合标记与剩余次数-v01-审稿包.zip')=='fb026219dd662f2eea5413aa185445dd0ffbeb8415c617ee5f7dfd203ca6a0a9'
assert len(list((R/'source').glob('*.aseprite')))==2 and len(list((R/'exports').glob('*.png')))==2
assert Image.open(R/'exports/fuse-unavailable-16.png').size==(16,16)
assert Image.open(R/'exports/fuse-shared-atlas-64x32.png').size==(64,32)
assert Image.open(R/'review/fuse-fallback-review.png').size==(1200,1880)
atlas=Image.open(R/'exports/fuse-shared-atlas-64x32.png').convert('RGBA');old=Image.open(R/'references/adopted-fuse-status-atlas-64x32.png').convert('RGBA')
layout=json.loads((R/'exports/atlas-layout.json').read_text())
for region in layout['regions']:
 box=(region['x'],region['y'],region['x']+region['width'],region['y']+region['height'])
 if region.get('sourceAtlas'):
  assert (R/'exports'/region['sourceAtlas']).resolve().is_file();assert atlas.crop(box).tobytes()==old.crop(box).tobytes()
 else:assert atlas.crop(box).tobytes()==Image.open(R/'exports'/region['source']).convert('RGBA').tobytes()
html=(R/'review/preview.html').read_text();data=re.search(r'ATLAS=("data:image/png;base64,[^"]+")',html).group(1);assert base64.b64decode(json.loads(data).split(',')[1])==(R/'exports/fuse-shared-atlas-64x32.png').read_bytes()
for n in ['native-reference-renderer.js','fallback-review.js']:assert (R/'source'/n).read_text() in html
assert not (R/'adoption.json').exists()
checks={'asset':'F03-03','allPassed':True,'adopted':False,'integrated':False,'newOriginalGlyphs':1,'productionModelFiles':0,'sourceAsepriteFiles':2,'productionPNGFiles':2,'nativeSpriteSize':[16,16],'actualSpriteColors':6,'atlasSize':[64,32],'atlasLayers':4,'sourceAndPNGReopenedCases':4,'opaqueGeometryMountCases':5,'finiteGeometrySceneCases':64,'WebGLRenderCases':64,'actualDropdownStateCases':16,'referenceCornerOcclusionCases':8,'handBackScaleRatio':1.0,'oldAtlasPixelsUnchangedOutsideNewRegion':True,'previousAssetAdopted':True,'previousArchiveUnchanged':True,'referencesAndCodeLocalesJarUnchanged':True,'editorStartupBlockerResolved':True,'gameRuntimeVerified':False}
dump(R/'review/package-checks.json',checks)
m={**checks,'version':'v01','created':'2026-10-06','status':'draft_for_visual_review','designDocumentsCompleted':True,'productionAssetsCompleted':True,'runtimeIntegrationPending':True,'nextAfterAdoption':'F03-04 特殊材料及融合过程反馈','files':[dict(path=str(p.relative_to(R)),bytes=p.stat().st_size,sha256=sha(p)) for p in sorted(R.rglob('*')) if p.is_file() and p.name!='manifest.json' and '__pycache__' not in p.parts]};dump(R/'manifest.json',m)
archive=R.parent/'F03-03-未知材料通用回退外观-v01-审稿包.zip';prefix='F03-03-未知材料通用回退外观-v01/'
with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED,compresslevel=7) as z:
 for f in m['files']:z.write(R/f['path'],prefix+f['path'])
 z.write(R/'manifest.json',prefix+'manifest.json')
with zipfile.ZipFile(archive) as z:
 assert z.testzip() is None
 for f in m['files']:assert hashlib.sha256(z.read(prefix+f['path'])).hexdigest()==f['sha256']
dump(R.parent/'F03-03-v01-打包校验.json',{'asset':'F03-03','allPassed':True,'zipBytes':archive.stat().st_size,'zipSha256':sha(archive),'manifestSha256':sha(R/'manifest.json'),'zipEntries':len(m['files'])+1,'zipCRCAndFileHashesVerified':True})
print(json.dumps({'allPassed':True,'files':len(m['files']),'zipBytes':archive.stat().st_size,'archive':str(archive),'adopted':False,'integrated':False},ensure_ascii=False))
