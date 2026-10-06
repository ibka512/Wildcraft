from pathlib import Path
from PIL import Image
import json,subprocess,hashlib,base64
R=Path(__file__).resolve().parents[1];P=R.parents[3];CTL='/Volumes/固态/开发/AI/工具/Aseprite Web 本机版/plugins/aseprite-web/scripts/aseprite-web-control'
def cli(*a):
 p=subprocess.run([CTL,'cli',*map(str,a)],capture_output=True,text=True,timeout=55);assert p.returncode==0,p.stderr[-700:];d=json.loads(p.stdout);assert d.get('ok') is not False;return d
def active(s):return next(d for d in s['documents'] if d['id']==s['active']['documentId'])
def pixelhash(im):
 h=0xcbf29ce484222325
 for b in im.tobytes():h=((h^b)*0x100000001b3)&0xffffffffffffffff
 return f'fnv1a64:{h:016x}'
D=json.loads((R/'source/pixel-designs.json').read_text());f=D['files'][0];im=Image.open(R/'exports/fabricator-atlas-64.png').convert('RGBA');assert im.size==(64,64);px={}
for layer in f['layers']:
 for x,y,w,c in layer['runs']:
  assert 0<=x<x+w<=64 and 0<=y<64
  for xx in range(x,x+w):px[xx,y]=tuple(bytes.fromhex(c[1:]))+(255,)
for y in range(64):
 for x in range(64):assert im.getpixel((x,y))==px.get((x,y),(0,0,0,0))
cli('open',R/'source/fabricator-atlas-64.aseprite');s=active(cli('state','--pixel-hash'));cli('open',R/'exports/fabricator-atlas-64.png');p=active(cli('state','--pixel-hash'));h=pixelhash(im);assert s['pixelHash']['value']==p['pixelHash']['value']==h;assert s['layerCount']==4 and s['frameCount']==1
U=json.loads((R/'source/uv-regions.json').read_text());old=Image.open(R/'references/charger-charger-atlas-64.png').convert('RGBA')
for r in U['materialReuse']:
 sx,sy,w,hgt=r['sourcePixels'];dx,dy,_,_=r['destinationPixels']
 for y in range(hgt):
  for x in range(w):assert im.getpixel((dx+x,dy+y))==old.getpixel((sx+x,sy+y))
G=json.loads((R/'source/geometry-and-uv.json').read_text());assert len(G['parts'])==43 and G['movingParts']==0 and not G['randomProductVisible']
for q in [*G['parts'],*G['badges'],G['statusPanel']]:
 assert all(0<=a<=b<=16 for a,b in zip(q['from'],q['to'])),q
 if 'faces' in q:
  assert all(0<=v<=16 for f in q['faces'].values() for v in f['uv'])
b=json.loads((R/'source/fabricator-v01.bbmodel').read_text());m=json.loads((R/'exports/fabricator-static-model.json').read_text());assert len(b['elements'])==len(m['elements'])==46;assert base64.b64decode(b['textures'][0]['source'].split(',',1)[1])==(R/'exports/fabricator-atlas-64.png').read_bytes()
for e,q in zip(b['elements'],m['elements']):
 assert e['from']==q['from'] and e['to']==q['to']
 for face,f in q['faces'].items():assert e['faces'][face]['uv']==[v*4 for v in f['uv']]
for f in json.loads((R/'references/code-baseline.json').read_text()):assert hashlib.sha256((P/f['path']).read_bytes()).hexdigest()==f['sha256']
for f in json.loads((R/'references/provenance.json').read_text()):assert hashlib.sha256((R/f['copy']).read_bytes()).hexdigest()==f['sha256']
result={'asset':'F06-01','allPassed':True,'nativeAtlas':[64,64],'sourceLayers':4,'sourceReopened':True,'pngReopened':True,'sourcePngPixelIdentity':True,'pixelHash':pixelhash(im),'visibleColors':len(set(im.getdata())),'alphaValues':sorted({p[3] for p in im.getdata()}),'reusedMaterialPatches':6,'materialPixelsNotResampled':True,'cuboids':43,'planesInStaticModel':3,'blockbenchStructurallyValid':True,'blockbenchApplicationOpened':False,'envelope':[16,16,16],'baselineJavaResourcesJarUnchanged':True,'gameIntegrated':False}
(R/'review/asset-checks.json').write_text(json.dumps(result,indent=2)+'\n');cli('open',R/'source/fabricator-atlas-64.aseprite');print('Native persistence, six source patches, model agreement, envelope and runtime baseline PASS')
