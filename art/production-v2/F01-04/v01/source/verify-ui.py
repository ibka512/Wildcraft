from pathlib import Path
import subprocess,json,hashlib,struct
from PIL import Image
R=Path(__file__).resolve().parents[1]
CTL='/Users/zhou/Desktop/Aseprite Web 本机版/plugins/aseprite-web/scripts/aseprite-web-control'
def cli(*a):
 p=subprocess.run([CTL,'cli',*map(str,a)],capture_output=True,text=True,timeout=55)
 if p.returncode:raise RuntimeError(p.stderr[-900:])
 return json.loads(p.stdout)
def active(s):return next(d for d in s['documents'] if d['id']==s['active']['documentId'])
spec=json.loads((R/'source/ui-pixel-design.json').read_text());w,h=spec['size'];expected={}
for layer in spec['layers']:
 for x,y,n,c in layer['runs']:
  for i in range(n):expected[x+i,y]=tuple(bytes.fromhex(c[1:]))+(255,)
f=R/'exports/cooking-ui-176x182.png';im=Image.open(f).convert('RGBA');assert im.size==(w,h)
for y in range(h):
 for x in range(w):assert im.getpixel((x,y))==expected.get((x,y),(0,0,0,0)),(x,y)
fnv=0xcbf29ce484222325
for b in im.tobytes():fnv=((fnv^b)*0x100000001b3)&0xffffffffffffffff
hashv=f'fnv1a64:{fnv:016x}'
src=R/'source/cooking-ui-176x182.aseprite';raw=src.read_bytes();assert struct.unpack_from('<HH',raw,8)==(w,h)
cli('open',src);s=active(cli('state','--pixel-hash'));cli('open',f);e=active(cli('state','--pixel-hash'))
assert s['pixelHash']['value']==e['pixelHash']['value']==hashv;assert s['layerCount']==4 and s['frameCount']==1 and s['colorMode']=='rgb'
record={'date':'2026-10-05','allPassed':True,'size':[w,h],'sourceLayers':s['layerCount'],'sourceColorMode':s['colorMode'],'alphaValues':sorted({c[3] for c in im.getdata()}),'opaqueColors':len({c for c in im.getdata() if c[3]}),'sourceHash':s['pixelHash']['value'],'pngHash':e['pixelHash']['value'],'matchesAuthoredPixels':True,'pixelIdentity':True,'pngSha256':hashlib.sha256(f.read_bytes()).hexdigest(),'sourceSha256':hashlib.sha256(raw).hexdigest(),'resizedConcept':False,'adopted':False,'integrated':False}
(R/'review/asset-checks.json').write_text(json.dumps(record,ensure_ascii=False,indent=2)+'\n');cli('open',src)
print(json.dumps(record,ensure_ascii=False))
