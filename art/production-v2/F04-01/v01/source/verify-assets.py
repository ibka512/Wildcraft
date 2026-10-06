from pathlib import Path
from PIL import Image
import json,subprocess,hashlib,base64,math
R=Path(__file__).resolve().parents[1];CTL='/Users/zhou/Desktop/Aseprite Web 本机版/plugins/aseprite-web/scripts/aseprite-web-control'
def cli(*a):
 p=subprocess.run([CTL,'cli',*map(str,a)],capture_output=True,text=True,timeout=55)
 if p.returncode:raise RuntimeError(p.stderr[-1000:])
 return json.loads(p.stdout)
def active(s):return next(d for d in s['documents'] if d['id']==s['active']['documentId'])
checks=[]
for entry in json.loads((R/'source/pixel-designs.json').read_text())['files']:
 name=entry['name'];size=tuple(entry['size']);png=R/'exports'/(name+'.png');im=Image.open(png).convert('RGBA');assert im.size==size;want={}
 for layer in entry['layers']:
  for x,y,w,color in layer['runs']:
   for i in range(w):want[x+i,y]=tuple(bytes.fromhex(color[1:]))+(255,)
 for y in range(size[1]):
  for x in range(size[0]):assert im.getpixel((x,y))==want.get((x,y),(0,0,0,0)),(name,x,y)
 h=0xcbf29ce484222325
 for b in im.tobytes():h=((h^b)*0x100000001b3)&0xffffffffffffffff
 ph=f'fnv1a64:{h:016x}';src=R/'source'/(name+'.aseprite');cli('open',src);s=active(cli('state','--pixel-hash'));cli('open',png);e=active(cli('state','--pixel-hash'))
 assert s['pixelHash']['value']==e['pixelHash']['value']==ph and s['layerCount']==len(entry['layers']) and s['colorMode']=='rgb' and s['frameCount']==1
 checks.append({'name':name,'size':size,'sourceLayers':s['layerCount'],'alphaValues':sorted({c[3] for c in im.get_flattened_data()}),'opaqueColors':len({c for c in im.get_flattened_data() if c[3]}),'pixelHash':ph,'sourcePngIdentity':True,'matchesAuthoredPixels':True,'pngSha256':hashlib.sha256(png.read_bytes()).hexdigest(),'sourceSha256':hashlib.sha256(src.read_bytes()).hexdigest()});print(name+' 原生像素与源稿重开一致',flush=True)
g=json.loads((R/'source/geometry-and-uv.json').read_text());atlas=Image.open(R/'exports/battery-atlas-32.png').convert('RGBA');assert len(g['parts'])==16
for p in g['parts']:
 assert all(p['to'][i]>p['from'][i] for i in range(3))
 for face,d in p['faces'].items():
  u0,v0,u1,v1=d['uv'];assert 0<=u0<u1<=16 and 0<=v0<v1<=16
  for y in range(math.floor(v0*2),math.ceil(v1*2)):
   for x in range(math.floor(u0*2),math.ceil(u1*2)):assert atlas.getpixel((x,y))[3]==255,(p['name'],face,x,y)
lo=[min(p['from'][i] for p in g['parts']) for i in range(3)];hi=[max(p['to'][i] for p in g['parts']) for i in range(3)];assert lo==g['envelope']['from'] and hi==g['envelope']['to']
assert all(abs((hi[i]-lo[i])-g['envelope']['size'][i])<1e-8 for i in range(3))
bb=json.loads((R/'source/battery-v01.bbmodel').read_text());assert len(bb['elements'])==16 and base64.b64decode(bb['textures'][0]['source'].split(',')[1])==(R/'exports/battery-atlas-32.png').read_bytes()
result={'asset':'F04-01','date':'2026-10-05','allPassed':True,'nativeFiles':checks,'modelCuboids':16,'dynamicWindowQuads':3,'allUVsOpaqueAndInBounds':True,'envelopeChecked':True,'embeddedBlockbenchTextureIdentity':True,'blockbenchOpened':False,'gameIntegrated':False,'adopted':False}
(R/'review/asset-checks.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');cli('open',R/'source/battery-item-32.aseprite')
print('几何边界、显式UV与内嵌纹理检查通过。',flush=True)
