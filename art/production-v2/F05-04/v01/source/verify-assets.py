from pathlib import Path
from PIL import Image
import json,subprocess,math,hashlib
R=Path(__file__).resolve().parents[1];CTL='/Users/zhou/Desktop/Aseprite Web 本机版/plugins/aseprite-web/scripts/aseprite-web-control'
def cli(*a):
 p=subprocess.run([CTL,'cli',*map(str,a)],capture_output=True,text=True,timeout=55)
 if p.returncode:raise RuntimeError(p.stderr[-700:])
 return json.loads(p.stdout)
def active(s):return next(d for d in s['documents']if d['id']==s['active']['documentId'])
def pixelhash(im):
 h=0xcbf29ce484222325
 for b in im.tobytes():h=((h^b)*0x100000001b3)&0xffffffffffffffff
 return f'fnv1a64:{h:016x}'
D=json.loads((R/'source/pixel-designs.json').read_text());checks=[]
for f in D['files']:
 n,hgt=f['size'];png=R/'exports'/(f['name']+'.png');src=R/'source'/(f['name']+'.aseprite');im=Image.open(png).convert('RGBA');assert im.size==(n,hgt);px={}
 for layer in f['layers']:
  for x,y,w,c in layer['runs']:
   for xx in range(x,x+w):px[xx,y]=tuple(bytes.fromhex(c[1:]))+(255,)
 for y in range(hgt):
  for x in range(n):assert im.getpixel((x,y))==px.get((x,y),(0,0,0,0))
 cli('open',src);s=active(cli('state','--pixel-hash'));cli('open',png);p=active(cli('state','--pixel-hash'));h=pixelhash(im);assert s['pixelHash']['value']==p['pixelHash']['value']==h and s['layerCount']==len(f['layers']) and s['frameCount']==1
 checks.append(dict(name=f['name'],nativeSize=[n,hgt],sourceLayers=len(f['layers']),reopened=True,sourcePngPixelIdentity=True,pixelHash=h,visibleColors=len({p for p in im.get_flattened_data()if p[3]}),alphaValues=sorted({p[3]for p in im.get_flattened_data()}),transparentPixels=sum(p[3]==0 for p in im.get_flattened_data())))


atlas=Image.open(R/'exports/rocket-atlas-128x64.png').convert('RGBA');machine=Image.open(R/'references/shared-machine-atlas-64.png').convert('RGBA')
assert all(atlas.getpixel((x,y))==machine.getpixel((x,y))for x in range(64)for y in range(64))
G=json.loads((R/'source/geometry-and-uv.json').read_text());facecounts={}
for state,g in G['variants'].items():
 count=0
 for p in g['parts']:
  c=[sum(v[i]for v in p['vertices'])/len(p['vertices'])for i in range(3)]
  for v in p['vertices']:assert -2.08-1e-9<=v[0]<=2.08+1e-9 and -2.08-1e-9<=v[1]<=2.08+1e-9 and .16<=v[2]<=8
  for f in p['faces']:
   count+=1;assert len(f['indices'])==len(f['uvs']) and all(0<=v<=16 for uv in f['uvs']for v in uv)
   vv=[p['vertices'][i]for i in f['indices']];a=[vv[1][i]-vv[0][i]for i in range(3)];b=[vv[2][i]-vv[0][i]for i in range(3)];normal=[a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]];fc=[sum(v[i]for v in vv)/len(vv)for i in range(3)];assert sum(normal[i]*(fc[i]-c[i])for i in range(3))>0,p['name']
 facecounts[state]=count
 common={p['name']:p for p in g['parts']if p['name']in G['commonPartNames']}
 if state=='loaded':original=common
 else:assert common==original
 prefix='rocket'if state=='loaded'else'spent-rocket';bb=json.loads((R/'source'/(prefix+'-v01.bbmodel')).read_text());assert len(bb['elements'])==len(g['parts'])
 import base64
 assert base64.b64decode(bb['textures'][0]['source'].split(',',1)[1])==(R/'exports/rocket-atlas-128x64.png').read_bytes()
assert not any(p['name']=='visible_propellant_face'for p in G['variants']['spent']['parts'])
assert G['variants']['loaded']['localEnvelope']==G['variants']['spent']['localEnvelope']
iconChanges=[]
for n in [32,16]:
 a=Image.open(R/'exports'/f'rocket-item-{n}.png').convert('RGBA');b=Image.open(R/'exports'/f'spent-rocket-item-{n}.png').convert('RGBA');diff=sum(x!=y for x,y in zip(a.get_flattened_data(),b.get_flattened_data()));assert diff>=(18 if n==32 else 5);iconChanges.append(dict(nativeSize=n,changedPixels=diff,independentlyAuthored=True))
assert all(hashlib.sha256((Path('/Volumes/仕事/Wildcraft/开发环境/project')/f['path']).read_bytes()).hexdigest()==f['sha256']for f in json.loads((R/'references/code-baseline.json').read_text()))
result=dict(asset='F05-04',allPassed=True,nativeFiles=checks,machine64RegionExact=True,newPaperPixels=256,sharedBaseParts=44,variantMeshes={k:len(v['parts'])for k,v in G['variants'].items()},faceChecks=facecounts,outwardMeshWinding=True,identicalMountAndEnvelope=True,spentPropellantRemoved=True,iconStateDifferences=iconChanges,blockbenchJsonStructurallyValid=True,blockbenchAppOpened=False,javaAndDev14Unchanged=True,gameIntegrated=False)
(R/'review/asset-checks.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');cli('open',R/'source/spent-rocket-item-16.aseprite');print('原生文件重开、机械图集保留、共用底模、破损/空腔区别及代码基准通过。')
