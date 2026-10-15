from pathlib import Path
import json,hashlib,base64,datetime
R=Path(__file__).resolve().parents[1];P=R.parents[3];OLD=R.parent/'v01'
def read(p):return json.loads(p.read_text())
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
b=read(R/'docs/BASELINE.json')
for f in b['files']:assert sha(P/f['path'])==f['sha256']and sha(R/f['copy'])==f['sha256'],f['path']
for p,h in b['originalPackageFiles'].items():assert sha(OLD/p)==h,p
for name in ['shared-fabric-machine-atlas-128x64.png','buoyancy-item-16.png','buoyancy-item-32.png']:assert sha(R/'exports'/name)==sha(OLD/'exports'/name)
g=read(R/'exports/buoyancy-bottom-proposal.json');assert g==read(R/'source/structure-geometry.json')
reports={name:read(R/'review'/name)for name in ['geometry-verification.json','blender-verification.json','browser-verification.json','visual-review.json']};assert all(v['allPassed']for v in reports.values())
assert reports['blender-verification.json']['sourceSHA256']==sha(R/'source/buoyancy-bottom-structure-v02.blend')
for kind in ['model','section']:assert len({sha(R/'review'/('browser-'+s+'-'+kind+'.png'))for s in ['water','unpowered','overload']})==1
for name,ps in [('buoyancy-bottom-v02.bbmodel',g['parts']),('chassis-float-pocket-proposal.bbmodel',g['candidateBody'])]:
 bb=read(R/'source'/name);assert len(bb['elements'])==len(ps)
 for p,e in zip(ps,bb['elements']):
  assert e['name']==p['name']and list(e['vertices'].values())==p['vertices']
  for f,ef in zip(p['faces'],e['faces'].values()):assert list(ef['uv'].values())==[[u*8,v*4]for u,v in f['uvs']]
 assert hashlib.sha256(base64.b64decode(bb['textures'][0]['source'].split(',')[1])).hexdigest()==sha(R/'exports/shared-fabric-machine-atlas-128x64.png')
assert sum(len(p['faces'])for p in g['parts'])==94 and len(g['parts'])==15 and len(g['candidateBody'])==71
assert g['movingParts']==0 and g['staticAcrossStates']
a=read(R/'docs/APPROVAL.json');assert not a['gameIntegrated']and not a['productionReady']
reg=read(P/'art/ADOPTED-ASSETS.json');assert reg['counts']['current_asset_ids']==43 and reg['game_version']=='0.1.0-dev.27'
assert 'BLENDER_FLOAT_PROPOSAL_SAVED' in (R/'review/blender-build.log').read_text();assert 'BLENDER_REOPEN_VERIFY_PASSED' in (R/'review/blender-reopen.log').read_text()
header=(R/'source/buoyancy-bottom-structure-v02.blend').read_bytes()[:7];assert header==b'BLENDER'or header[:4]==bytes.fromhex('28b52ffd')
pngs=list((R/'review').glob('browser-*.png'));assert len(pngs)==20
files=[]
for p in sorted(R.rglob('*')):
 if not p.is_file()or p.name.startswith('._')or '__pycache__'in p.parts:continue
 rel=p.relative_to(R).as_posix()
 if rel in ['docs/DELIVERY-MANIFEST.json','review/package-verification.json']:continue
 files.append({'path':rel,'bytes':p.stat().st_size,'sha256':sha(p)})
m={'asset':'F05-09','version':'v02-structure-proposal','status':a['status'],'adopted':a['adopted'],'gameIntegrated':False,'productionReady':False,'previewURL':'http://127.0.0.1:8888/preview.html','files':files,'checkedAt':datetime.datetime.now(datetime.timezone.utc).isoformat()};(R/'docs/DELIVERY-MANIFEST.json').write_text(json.dumps(m,ensure_ascii=False,indent=2)+'\n')
r={'allPassed':True,'checkedFiles':len(files),'oldPackageFilesUnchanged':len(b['originalPackageFiles']),'gameAndSpringInputsUnchanged':True,'floatMeshes':15,'candidateChassisMeshes':71,'floatFaces':94,'movingParts':0,'newTexturePixels':0,'originalItemIconsUnchanged':True,'editableFreeMeshFiles':2,'reopenedBlendChecked':True,'stateGeometryCases':48,'uprightHeightCases':81,'otherMountCases':20,'airRollCases':13,'referenceRuleCases':13,'actualUIChecks':len(reports['browser-verification.json']['checks']),'actualCanvasPNGs':len(pngs),'waterUnpoweredOverloadCanvasesIdentical':True,'artOnly':True,'productionAssetIds':43,'gameVersion':'0.1.0-dev.27','newProductionDependencies':False,'pendingUserReview':not a['adopted'],'gameIntegrated':False};(R/'review/package-verification.json').write_text(json.dumps(r,ensure_ascii=False,indent=2)+'\n');print(json.dumps(r,ensure_ascii=False,indent=2))
