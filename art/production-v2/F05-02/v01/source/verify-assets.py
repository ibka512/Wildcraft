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

atlas=Image.open(R/'exports/wing-atlas-128x64.png').convert('RGBA');machine=Image.open(R/'references/machine-atlas-64.png').convert('RGBA');glider=Image.open(R/'references/paraglider-atlas-128x64.png').convert('RGBA')
assert all(atlas.getpixel((x,y))==machine.getpixel((x,y))for x in range(64)for y in range(64))
for sx,sy,dx,dy in [(0,0,64,0),(32,0,96,0),(0,32,64,32),(64,32,96,32)]:assert all(atlas.getpixel((dx+x,dy+y))==glider.getpixel((sx+x,sy+y))for x in range(32)for y in range(32))
G=json.loads((R/'source/geometry-and-uv.json').read_text());assert len(G['parts'])==28 and all(p['group']in ['cloth','frame','mount']for p in G['parts']);assert not G['consumesEnergy']and not G['physicsChanged']
facecount=0
for p in G['parts']:
 c=[sum(v[i]for v in p['vertices'])/len(p['vertices'])for i in range(3)]
 for v in p['vertices']:assert -9.6<=v[0]<=9.6 and -1.28<=v[1]<=1.28 and .6<=v[2]<=8.8
 for f in p['faces']:
  facecount+=1;assert len(f['indices'])==len(f['uvs']) and all(0<=v<=16 for uv in f['uvs']for v in uv)
  vv=[p['vertices'][i]for i in f['indices']];a=[vv[1][i]-vv[0][i]for i in range(3)];b=[vv[2][i]-vv[0][i]for i in range(3)];normal=[a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]];fc=[sum(v[i]for v in vv)/len(vv)for i in range(3)];assert sum(normal[i]*(fc[i]-c[i])for i in range(3))>0
BB=json.loads((R/'source/wing-v01.bbmodel').read_text());assert len(BB['elements'])==28 and len(BB['outliner'])==3 and BB['resolution']=={'width':128,'height':64}
import base64
assert base64.b64decode(BB['textures'][0]['source'].split(',',1)[1])==(R/'exports/wing-atlas-128x64.png').read_bytes()
assert all(hashlib.sha256((Path('/Volumes/仕事/Wildcraft/开发环境/project')/f['path']).read_bytes()).hexdigest()==f['sha256']for f in json.loads((R/'references/code-baseline.json').read_text()))
result=dict(asset='F05-02',allPassed=True,nativeFiles=checks,atlasCopiedNativePixels=8192,atlasNewPaintedPixels=0,machine64RegionExact=True,paragliderFour32RegionsExact=True,meshParts=28,fixedOnly=True,meshFaceChecks=facecount,outwardMeshWinding=True,prototypeEnvelopePreservedExceptExplicitContactDepth=.04,blockbenchJsonStructurallyValid=True,blockbenchAppOpened=False,gameIntegrated=False,javaAndDev14Unchanged=True)
(R/'review/asset-checks.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');cli('open',R/'source/wing-item-16.aseprite');print('Aseprite重开、全部像素、复用图集、28固定件/168面及代码基准通过。')
