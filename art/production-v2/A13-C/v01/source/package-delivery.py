from pathlib import Path
import hashlib,json,zipfile
R=Path(__file__).resolve().parents[1]
h=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
load=lambda p:json.loads(p.read_text())
assert load(R/'review/delivery-checks.json')['allPassed']
files=[{'path':str(p.relative_to(R)),'bytes':p.stat().st_size,'sha256':h(p)} for p in sorted(R.rglob('*')) if p.is_file() and p.name!='manifest.json']
m={'asset':'A13-C','version':'v01','date':'2026-10-06','allPassed':True,'adopted':False,'integrated':False,'gameRuntimeVerified':False,'humanListeningVerified':False,'scenarios':8,'profiles':2,'reusedFocusOgg':2,'reusedS01Ogg':12,'nativeLocalReferenceOnly':8,'newProductionAudio':0,'newProductionMasters':0,'nativeAudioBundled':0,'portablePreviewWav':8,'policyChecks':143,'browserChecks':98,'deliveryChecks':27,'protectedOriginals':30,'files':files}
(R/'manifest.json').write_text(json.dumps(m,ensure_ascii=False,indent=2)+'\n')
archive=R.parent/'A13-C-专注音效混音复核-v01-审稿包.zip';assert not archive.exists(),'Do not overwrite historical review archive'
with zipfile.ZipFile(archive,'w',zipfile.ZIP_DEFLATED) as z:
 for p in sorted(R.rglob('*')):
  if p.is_file():z.write(p,Path('A13-C-v01')/p.relative_to(R))
with zipfile.ZipFile(archive) as z:
 assert z.testzip() is None
 for f in files:assert hashlib.sha256(z.read('A13-C-v01/'+f['path'])).hexdigest()==f['sha256']
 assert hashlib.sha256(z.read('A13-C-v01/manifest.json')).hexdigest()==h(R/'manifest.json')
receipt={'allPassed':True,'archive':str(archive),'archiveSha256':h(archive),'archiveBytes':archive.stat().st_size,'files':len(files)+1,'matchingFiles':len(files)+1,'manifestSha256':h(R/'manifest.json'),'adopted':False,'integrated':False,'nativeAudioBundled':0}
(R.parent/'A13-C-v01-打包校验.json').write_text(json.dumps(receipt,ensure_ascii=False,indent=2)+'\n');print(json.dumps(receipt,ensure_ascii=False))
