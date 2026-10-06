from pathlib import Path
from PIL import Image
import json,hashlib,subprocess,struct
R=Path(__file__).resolve().parents[1];CTL='/Users/zhou/Desktop/Aseprite Web 本机版/plugins/aseprite-web/scripts/aseprite-web-control'
def cli(*a):
 p=subprocess.run([CTL,'cli',*map(str,a)],capture_output=True,text=True,timeout=55)
 if p.returncode:raise RuntimeError(p.stderr[-800:])
 return json.loads(p.stdout)
def active(s):return next(d for d in s['documents'] if d['id']==s['active']['documentId'])
D=json.loads((R/'source/pixel-designs.json').read_text());checks=[]
for f in D['files']:
 n=f['size'][0];im=Image.open(R/'exports'/(f['name']+'.png')).convert('RGBA');assert im.size==(n,n);px={}
 for layer in f['layers']:
  for x,y,w,c in layer['runs']:
   for xx in range(x,x+w):px[xx,y]=tuple(bytes.fromhex(c[1:]))+(255,)
 for y in range(n):
  for x in range(n):assert im.getpixel((x,y))==px.get((x,y),(0,0,0,0)),(f['name'],x,y)
 fnv=0xcbf29ce484222325
 for b in im.tobytes():fnv=((fnv^b)*0x100000001b3)&0xffffffffffffffff
 h=f'fnv1a64:{fnv:016x}';src=R/'source'/(f['name']+'.aseprite');assert struct.unpack_from('<HH',src.read_bytes(),8)==(n,n);cli('open',src);s=active(cli('state','--pixel-hash'));cli('open',R/'exports'/(f['name']+'.png'));p=active(cli('state','--pixel-hash'));assert s['pixelHash']['value']==p['pixelHash']['value']==h and s['layerCount']==len(f['layers']) and s['frameCount']==1
 checks.append(dict(name=f['name'],nativeSize=[n,n],sourceLayers=len(f['layers']),reopened=True,sourcePngPixelIdentity=True,pixelHash=h,visibleColors=len({p for p in im.get_flattened_data() if p[3]}),alphaValues=sorted({p[3] for p in im.get_flattened_data()})))
a=Image.open(R/'exports/machine-atlas-64.png').convert('RGBA');b=Image.open(R/'references/battery-atlas-32.png').convert('RGBA');assert a.crop((0,0,32,32)).tobytes()==b.tobytes()
g=json.loads((R/'source/geometry-and-uv.json').read_text());assert [n['point'] for n in g['nodes']]==[[0,10.4,0],[0,0,0],[0,5.2,-11.2],[0,5.2,11.2],[-11.2,5.2,0],[11.2,5.2,0]]
for p in g['parts']:
 assert all(p['from'][i]<p['to'][i] for i in range(3))
 for f in p['faces'].values():u,v,uu,vv=f['uv'];assert 0<=u<uu<=16 and 0<=v<vv<=16
 c=p['group']
 if c=='control':assert -.25<=p['from'][0]/16<p['to'][0]/16<=.25 and .55<=p['from'][1]/16<p['to'][1]/16<=.72 and .32<=p['from'][2]/16<p['to'][2]/16<=.5
assert [sum(p.get('node')==n for p in g['parts']) for n in range(6)]==[8]*6
b0=json.loads((R/'references/geometry-and-uv.json').read_text())
for src,ref in zip(g['batteryParts'],b0['parts']):
 assert all(abs((src['to'][i]-src['from'][i])-(ref['to'][i]-ref['from'][i]))<1e-9 for i in range(3))
 for f in ref['faces']:assert src['faces'][f]['uv']==[v/2 for v in ref['faces'][f]['uv']]
# Reopenable JSON source structural verification, not a claim of Blockbench application import.
bb=json.loads((R/'source/machine-body-v01.bbmodel').read_text());assert len(bb['elements'])==len(g['parts']);assert all(len(e['vertices'])==8 and len(e['faces'])==6 for e in bb['elements']);assert bb['resolution']=={'width':64,'height':64}
result=dict(asset='F05-01',allPassed=True,nativeFiles=checks,bodyAndControlCuboids=20,sharedNodeCuboids=8,nodeInstances=6,atlasBatteryRegionPixelIdentity=True,exactNodePoints=True,controlArtWithinExistingHitbox=True,batteryScale=1,batteryEnvelopeUnchanged=True,blockbenchJsonStructurallyValid=True,blockbenchAppOpened=False,gameIntegrated=False)
(R/'review/asset-checks.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');cli('open',R/'source/machine-atlas-64.aseprite');print('三份原生源稿/PNG重开一致；六节点、控制板与原电池尺寸检查通过。')
