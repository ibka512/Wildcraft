from pathlib import Path
import json,subprocess,hashlib,struct
from PIL import Image
ROOT=Path(__file__).resolve().parents[1]
CTL='/Users/zhou/Desktop/Aseprite Web 本机版/plugins/aseprite-web/scripts/aseprite-web-control'
def cli(*a):
 p=subprocess.run([CTL,'cli',*map(str,a)],capture_output=True,text=True,timeout=55)
 if p.returncode:raise RuntimeError(p.stderr[-1000:])
 return json.loads(p.stdout)
def active(st):return next(d for d in st['documents'] if d['id']==st['active']['documentId'])
def fnv(data):
 h=0xcbf29ce484222325
 for b in data:h=((h^b)*0x100000001b3)&0xffffffffffffffff
 return f'fnv1a64:{h:016x}'
spec=json.loads((ROOT/'source/pixel-designs.json').read_text());results=[]
for d in spec:
 name=d['name'];n=d['size'][0];p=ROOT/'exports'/f'{name}.png';im=Image.open(p).convert('RGBA');assert im.size==(n,n)
 colors=im.getcolors(n*n);alphas=sorted({c[3] for _,c in colors});assert alphas==[0,255]
 expected={(x,y):c for x,y,c in d['pixels']}
 for y in range(n):
  for x in range(n):
   c=expected.get((x,y));want=tuple(bytes.fromhex(c[1:]))+(255,) if c else (0,0,0,0)
   assert im.getpixel((x,y))==want,(name,x,y,im.getpixel((x,y)),want)
 native_hash=fnv(im.tobytes())
 src=ROOT/'source'/f'{name}.aseprite';raw=src.read_bytes();assert struct.unpack_from('<H',raw,4)[0]==0xa5e0;assert struct.unpack_from('<HH',raw,8)==(n,n)
 cli('open',src);s=active(cli('state','--pixel-hash'));cli('open',p);e=active(cli('state','--pixel-hash'))
 assert s['pixelHash']['value']==e['pixelHash']['value']==native_hash
 assert s['layerCount']==4 and s['colorMode']=='rgb' and s['frameCount']==1
 item={'name':name,'size':[n,n],'opaque_colors':sum(c[3]>0 for _,c in colors),'alpha_values':alphas,'source_layers':s['layerCount'],'source_reopened_hash':s['pixelHash']['value'],'png_reopened_hash':e['pixelHash']['value'],'png_readonly_rgba_hash':native_hash,'pixel_identity':True,'matches_authored_design':True,'png_sha256':hashlib.sha256(p.read_bytes()).hexdigest(),'source_sha256':hashlib.sha256(raw).hexdigest()}
 results.append(item);print(name+' 保存重开与逐像素比对通过',flush=True)
for n in (32,16):
 group=[d for d in spec if d['size'][0]==n]
 for i in (0,2):assert all(d['layers'][i]['runs']==group[0]['layers'][i]['runs'] for d in group)
(ROOT/'review/asset-checks.json').write_text(json.dumps({'asset':'F01-02','version':'v01','date':'2026-10-05','all_passed':True,'shared_bowl_layers_identical':True,'no_downsampling':True,'game_integration_tested':False,'files':results},ensure_ascii=False,indent=2)+'\n')
# Leave the editable main warming file available in the editor, not a flattened PNG.
cli('open',ROOT/'source/meal-warming-32.aseprite')
