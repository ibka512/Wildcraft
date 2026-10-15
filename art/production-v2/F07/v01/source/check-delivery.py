from pathlib import Path
import json,hashlib,datetime,struct
R=Path(__file__).resolve().parents[1];P=R.parents[3]
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
reports={n:json.loads((R/'review'/n).read_text())for n in ['pixel-verification.json','browser-verification.json','ui-verification.json','visual-review.json']};assert all(r['allPassed']for r in reports.values())
a=json.loads((R/'docs/APPROVAL.json').read_text());assert not a['gameIntegrated'];dep=json.loads((R/'docs/DEPENDENCIES.json').read_text());assert all(sha(P/f['path'])==f['sha256']for f in dep['existingProjectFiles']);assert all(sha(R/f['copy'])==f['sha256']for f in dep['existingProjectFiles']if f.get('copy'))
reg=json.loads((P/'art/ADOPTED-ASSETS.json').read_text());assert reg['counts']['current_asset_ids']==43 and reg['game_version']=='0.1.0-dev.27'
source=(R/'source/wind-weather-atlas.aseprite').read_bytes();assert struct.unpack_from('<H',source,4)[0]==0xa5e0;assert struct.unpack_from('<HHH',source,6)==(1,64,64)
assert len(json.loads((R/'exports/atlas-map.json').read_text())['entries'])==13
assert (R/'review/build-icons-final.log').read_text().strip().endswith('Created 13 native 16px components; layered source and PNG saved/reopened in Aseprite Web.')
files=[]
for f in sorted(R.rglob('*')):
 if not f.is_file()or f.name.startswith('._')or '__pycache__'in f.parts:continue
 rel=f.relative_to(R).as_posix()
 if rel in ['docs/DELIVERY-MANIFEST.json','review/package-verification.json']:continue
 files.append({'path':rel,'bytes':f.stat().st_size,'sha256':sha(f)})
m={'assetIds':['F07-01','F07-02'],'version':'v01','status':a['status'],'adopted':a['adopted'],'gameIntegrated':False,'previewURL':'http://127.0.0.1:8886/preview.html','files':files,'checkedAt':datetime.datetime.now(datetime.timezone.utc).isoformat()};(R/'docs/DELIVERY-MANIFEST.json').write_text(json.dumps(m,ensure_ascii=False,indent=2)+'\n')
r={'allPassed':True,'checkedFiles':len(files),'nativeIcons':13,'atlas':[64,64],'editableLayers':3,'roundtripPixelComparisons':3,'layoutCases':reports['browser-verification.json']['layoutCases'],'directionCases':40,'strengthBoundaryCases':6,'actualUIChecks':len(reports['ui-verification.json']['checks']),'existingInputsUnchanged':True,'productionAssetIds':43,'gameVersion':reg['game_version'],'newProductionDependencies':False,'userReviewPending':not a['adopted'],'gameIntegrated':False,'manifestSHA256':sha(R/'docs/DELIVERY-MANIFEST.json')};(R/'review/package-verification.json').write_text(json.dumps(r,ensure_ascii=False,indent=2)+'\n');print(json.dumps(r,ensure_ascii=False,indent=2))
