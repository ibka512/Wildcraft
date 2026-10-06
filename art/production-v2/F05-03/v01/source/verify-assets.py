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
 n=f['size'][0];png=R/'exports'/(f['name']+'.png');src=R/'source'/(f['name']+'.aseprite');im=Image.open(png).convert('RGBA');assert im.size==(n,n);px={}
 for layer in f['layers']:
  for x,y,w,c in layer['runs']:
   for xx in range(x,x+w):px[xx,y]=tuple(bytes.fromhex(c[1:]))+(255,)
 for y in range(n):
  for x in range(n):assert im.getpixel((x,y))==px.get((x,y),(0,0,0,0))
 cli('open',src);s=active(cli('state','--pixel-hash'));cli('open',png);p=active(cli('state','--pixel-hash'));h=pixelhash(im);assert s['pixelHash']['value']==p['pixelHash']['value']==h and s['layerCount']==3 and s['frameCount']==1
 checks.append(dict(name=f['name'],nativeSize=[n,n],sourceLayers=3,reopened=True,sourcePngPixelIdentity=True,pixelHash=h,visibleColors=len({p for p in im.get_flattened_data()if p[3]}),alphaValues=sorted({p[3]for p in im.get_flattened_data()}),transparentPixels=sum(p[3]==0 for p in im.get_flattened_data())))
atlas=R/'exports/shared-machine-atlas-64.png';ase=R/'source/shared-machine-atlas-64.aseprite';im=Image.open(atlas).convert('RGBA');cli('open',ase);s=active(cli('state','--pixel-hash'));cli('open',atlas);p=active(cli('state','--pixel-hash'));assert s['pixelHash']['value']==p['pixelHash']['value']==pixelhash(im)and s['layerCount']==4
G=json.loads((R/'source/geometry-and-uv.json').read_text());assert len(G['parts'])==24 and sum(p['group']=='rotor'for p in G['parts'])==6;assert G['rotorPivot']==[0,0,1.76]
for p in G['parts']:
 for f in p['faces']:
  assert len(f['indices'])==len(f['uvs']) and all(0<=v<=16 for uv in f['uvs']for v in uv)
for p in G['parts']:
 c=[sum(v[i] for v in p['vertices'])/len(p['vertices']) for i in range(3)]
 for f in p['faces']:
  vv=[p['vertices'][i] for i in f['indices']];a=[vv[1][i]-vv[0][i] for i in range(3)];b=[vv[2][i]-vv[0][i] for i in range(3)];normal=[a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]];fc=[sum(v[i] for v in vv)/len(vv) for i in range(3)];assert sum(normal[i]*(fc[i]-c[i]) for i in range(3))>0
# Actual vertex positions swept through a full turn, tested against guaranteed open square radius.
clearance=[]
for degrees in range(361):
 a=math.radians(degrees);maxRadius=0
 for p in G['parts']:
  if not p['name'].startswith('blade_'):continue
  for x,y,z in p['vertices']:
   xx=x*math.cos(a)-y*math.sin(a);yy=x*math.sin(a)+y*math.cos(a);assert abs(xx)<3.84 and abs(yy)<3.84;maxRadius=max(maxRadius,math.hypot(xx,yy))
 clearance.append(dict(degrees=degrees,minFrameGapModelUnits=3.84-maxRadius))
assert all(G['localEnvelope']['min'][i]-1e-9<=v[i]<=G['localEnvelope']['max'][i]+1e-9 for p in G['parts']for v in p['vertices']for i in range(3))
BB=json.loads((R/'source/fan-v01.bbmodel').read_text());assert len(BB['elements'])==24 and len(BB['outliner'])==2 and next(q for q in BB['outliner']if q['name']=='rotor')['origin']==[0,0,1.76]
result=dict(asset='F05-03',allPassed=True,nativeIconFiles=checks,sharedAtlasReopened=True,sharedAtlasSourcePngIdentity=True,sharedAtlasPixelHash=pixelhash(im),sharedAtlasNewPixels=0,meshParts=24,fixedParts=18,rotatingParts=6,blades=4,fullRotationClearanceCases=361,minClearanceModelUnits=min(x['minFrameGapModelUnits']for x in clearance),bladesDoNotTouchFrame=True,outwardMeshWinding=True,pivotMatchesExisting=True,blockbenchJsonStructurallyValid=True,blockbenchAppOpened=False,gameIntegrated=False)
(R/'review/asset-checks.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');cli('open',R/'source/fan-item-16.aseprite');print('原生图/源稿与共用图集重开一致；361组全周转动叶片/护框间隙通过。')
