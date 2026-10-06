from pathlib import Path
from PIL import Image
import json,subprocess,math,hashlib
R=Path(__file__).resolve().parents[1];CTL='/Volumes/固态/开发/AI/工具/Aseprite Web 本机版/plugins/aseprite-web/scripts/aseprite-web-control'
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



assert (R/'exports/shared-fabric-machine-atlas-128x64.png').read_bytes()==(R/'references/shared-fabric-machine-atlas-128x64.png').read_bytes()
G=json.loads((R/'source/geometry-and-uv.json').read_text());assert len(G['parts'])==15;faces=0
for p in G['parts']:
 for v in p['vertices']:assert abs(v[0])<=5.12+1e-9 and abs(v[1])<=3.2+1e-9 and .32-1e-9<=v[2]<=6.4+1e-9
 center=[sum(v[i]for v in p['vertices'])/len(p['vertices'])for i in range(3)]
 for f in p['faces']:
  assert len(f['indices'])==len(f['uvs']) and all(0<=v<=16 for uv in f['uvs']for v in uv);vv=[p['vertices'][i]for i in f['indices']];aa=[vv[1][i]-vv[0][i]for i in range(3)];bb=[vv[2][i]-vv[0][i]for i in range(3)];n=[aa[1]*bb[2]-aa[2]*bb[1],aa[2]*bb[0]-aa[0]*bb[2],aa[0]*bb[1]-aa[1]*bb[0]];fc=[sum(v[i]for v in vv)/len(vv)for i in range(3)];assert sum(n[i]*(fc[i]-center[i])for i in range(3))>0,p['name'];faces+=1
import base64
b=json.loads((R/'source/buoyancy-v01.bbmodel').read_text());assert len(b['elements'])==15 and len(b['outliner'])==1;assert base64.b64decode(b['textures'][0]['source'].split(',',1)[1])==(R/'exports/shared-fabric-machine-atlas-128x64.png').read_bytes()
for e,p in zip(b['elements'],G['parts']):assert list(e['vertices'].values())==p['vertices']
assert all(p['group']=='fixed'for p in G['parts'])
for f in json.loads((R/'references/code-baseline.json').read_text()):assert hashlib.sha256((Path('/Volumes/仕事/Wildcraft/开发环境/project')/f['path']).read_bytes()).hexdigest()==f['sha256']
result=dict(asset='F05-09',allPassed=True,nativeFiles=checks,atlasBytesExact=True,newProductionTexturePixels=0,parts=15,movingMeshes=0,indicatorMeshes=0,outwardFaces=faces,blockbenchStructurallyValid=True,blockbenchAppOpened=False,javaAndDev14Unchanged=True,gameIntegrated=False)
(R/'review/asset-checks.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');cli('open',R/'source/buoyancy-item-16.aseprite');print('Native files reopened and pixel identity, static geometry and runtime baseline PASS')
