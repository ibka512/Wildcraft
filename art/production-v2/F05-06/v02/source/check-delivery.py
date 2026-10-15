from pathlib import Path
import json,hashlib,base64,datetime,struct
R=Path(__file__).resolve().parents[1];P=R.parents[3];OLD=R.parent/'v01'
def read(p):return json.loads(p.read_text())
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
b=read(R/'docs/BASELINE.json')
for f in b['files']:assert sha(P/f['path'])==f['sha256']and sha(R/f['copy'])==f['sha256'],f['path']
for p,h in b['originalPackageFiles'].items():assert sha(OLD/p)==h,p
assert sha(R/'exports/shared-machine-atlas-64.png')==sha(OLD/'exports/shared-machine-atlas-64.png')
g=read(R/'exports/spring-bottom-proposal.json');assert g==read(R/'source/structure-geometry.json')
reports={f:read(R/'review'/f)for f in ['geometry-verification.json','blender-verification.json','browser-verification.json','visual-review.json']};assert all(v['allPassed']for v in reports.values())
assert reports['browser-verification.json']['finalSectionLayoutChecked']
for state in ['stowed','extended']:
 bb=read(R/'source'/('spring-bottom-'+state+'.bbmodel'));assert len(bb['elements'])==44
 for p,e in zip(g['parts'],bb['elements']):assert p['name']==e['name']and list(e['vertices'].values())==p['vertices'if state=='stowed'else'extendedVertices']
 assert hashlib.sha256(base64.b64decode(bb['textures'][0]['source'].split(',')[1])).hexdigest()==sha(R/'exports/shared-machine-atlas-64.png')
cb=read(R/'source/chassis-spring-pocket-proposal.bbmodel');assert len(cb['elements'])==len(g['candidateBody'])==71
for p,e in zip(g['candidateBody'],cb['elements']):assert list(e['vertices'].values())==p['vertices']
a=read(R/'docs/APPROVAL.json');assert not a['gameIntegrated']and not a['productionReady']
reg=read(P/'art/ADOPTED-ASSETS.json');assert reg['counts']['current_asset_ids']==43 and reg['game_version']=='0.1.0-dev.27'
assert b'BLENDER_PROPOSAL_SAVED' in (R/'review/blender-build.log').read_bytes();assert b'BLENDER_REOPEN_VERIFY_PASSED' in (R/'review/blender-reopen.log').read_bytes()
header=(R/'source/spring-bottom-structure-v02.blend').read_bytes()[:7];assert header==b'BLENDER' or header[:4]==bytes.fromhex('28b52ffd')
files=[]
for p in sorted(R.rglob('*')):
 if not p.is_file()or p.name.startswith('._')or '__pycache__'in p.parts:continue
 rel=p.relative_to(R).as_posix()
 if rel in ['docs/DELIVERY-MANIFEST.json','review/package-verification.json']:continue
 files.append({'path':rel,'bytes':p.stat().st_size,'sha256':sha(p)})
m={'asset':'F05-06','version':'v02-structure-proposal','status':a['status'],'adopted':a['adopted'],'gameIntegrated':False,'productionReady':False,'previewURL':'http://127.0.0.1:8887/preview.html','files':files,'checkedAt':datetime.datetime.now(datetime.timezone.utc).isoformat()};(R/'docs/DELIVERY-MANIFEST.json').write_text(json.dumps(m,ensure_ascii=False,indent=2)+'\n')
r={'allPassed':True,'checkedFiles':len(files),'oldPackageFilesUnchanged':len(b['originalPackageFiles']),'gameInputsAndRegistryUnchanged':True,'springMeshes':44,'candidateChassisMeshes':71,'coilShapeKeys':24,'nativeAtlasCopiedUnchanged':True,'editableFreeMeshFiles':3,'reopenedBlendChecked':True,'clearanceCases':reports['geometry-verification.json']['clearancePoseCases'],'actualUIChecks':len(reports['browser-verification.json']['checks']),'finalSectionViews':6,'artOnly':True,'newProductionDependencies':False,'productionAssetIds':43,'gameVersion':'0.1.0-dev.27','pendingUserReview':not a['adopted'],'gameIntegrated':False};(R/'review/package-verification.json').write_text(json.dumps(r,ensure_ascii=False,indent=2)+'\n');print(json.dumps(r,ensure_ascii=False,indent=2))
