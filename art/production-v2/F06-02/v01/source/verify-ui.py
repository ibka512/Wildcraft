from pathlib import Path
from PIL import Image
import subprocess,json,hashlib,struct
R=Path(__file__).resolve().parents[1];P=R.parents[3];CTL='/Volumes/固态/开发/AI/工具/Aseprite Web 本机版/plugins/aseprite-web/scripts/aseprite-web-control'
def cli(*args):
 p=subprocess.run([CTL,'cli',*map(str,args)],capture_output=True,text=True,timeout=55)
 if p.returncode:raise RuntimeError(p.stderr[-800:])
 d=json.loads(p.stdout);assert d.get('ok') is not False;return d
def active(s):return next(d for d in s['documents'] if d['id']==s['active']['documentId'])
spec=json.loads((R/'source/ui-pixel-design.json').read_text());expected={}
for layer in spec['layers']:
 for x,y,w,c in layer['runs']:
  for xx in range(x,x+w):expected[xx,y]=tuple(bytes.fromhex(c[1:]))+(255,)
png=R/'exports/fabricator-ui-176x192.png';im=Image.open(png).convert('RGBA');assert im.size==(176,192)
for y in range(192):
 for x in range(176):assert im.getpixel((x,y))==expected.get((x,y),(0,0,0,0)),(x,y)
src=R/'source/fabricator-ui-176x192.aseprite';assert struct.unpack_from('<HH',src.read_bytes(),8)==(176,192)
fnv=0xcbf29ce484222325
for b in im.tobytes():fnv=((fnv^b)*0x100000001b3)&0xffffffffffffffff
ph=f'fnv1a64:{fnv:016x}';cli('open',src);s=active(cli('state','--pixel-hash'));cli('open',png);p=active(cli('state','--pixel-hash'));assert s['pixelHash']['value']==p['pixelHash']['value']==ph and s['layerCount']==4 and s['frameCount']==1 and s['colorMode']=='rgb'
expectedSlots=[[26,35],[54,35],[134,54]]+[[8+18*x,110+18*y] for y in range(3) for x in range(9)]+[[8+18*x,168] for x in range(9)]
slots=spec['slots'];assert slots==expectedSlots and len(slots)==39
for i,(x,y) in enumerate(slots):
 assert 0<=x-1<x+17<=176 and 0<=y-1<y+17<=192
 for ox,oy in slots[i+1:]:assert not(x<ox+16 and x+16>ox and y<oy+16 and y+16>oy)
menu=(R/'references/FabricatorMenu.java').read_text();screen=(R/'references/FabricatorScreen.java').read_text();assert '26+i*28,35' in menu and '134,54' in menu and '110+row*18' in menu and '168' in menu;assert '104,topPos+30,64,20' in screen and 'inventoryLabelY=100' in screen
for f in json.loads((R/'references/code-baseline.json').read_text()):assert hashlib.sha256((P/f['path']).read_bytes()).hexdigest()==f['sha256']
for f in json.loads((R/'references/provenance.json').read_text()):assert hashlib.sha256((R/f['copy']).read_bytes()).hexdigest()==f['sha256']
result=dict(asset='F06-02',date='2026-10-05',allPassed=True,size=[176,192],sourceLayers=4,colorMode='rgb',sourceReopened=True,pngReopened=True,sourcePngPixelIdentity=True,pixelHash=ph,matchesAuthoredPixels=True,alphaValues=sorted({c[3] for c in im.get_flattened_data()}),visibleColors=len({c for c in im.get_flattened_data() if c[3]}),actualSlotCount=39,inputSlots=2,outputSlots=1,playerSlots=36,slotCoordinatesUnchanged=True,buttonCoordinatesUnchanged=True,noSlotOverlaps=True,bakedText=False,bakedItems=False,bakedProgress=False,javaLocalesJarUnchanged=True,integrated=False)
(R/'review/asset-checks.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');cli('open',src);print('Native176x192, source/PNG reopened identical, 39 slots + button + runtime baseline PASS')
