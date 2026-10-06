from pathlib import Path
from PIL import Image
import subprocess,json,hashlib,struct
R=Path(__file__).resolve().parents[1];CTL='/Users/zhou/Desktop/Aseprite Web 本机版/plugins/aseprite-web/scripts/aseprite-web-control'
def cli(*args):
 p=subprocess.run([CTL,'cli',*map(str,args)],capture_output=True,text=True,timeout=55)
 if p.returncode:raise RuntimeError(p.stderr[-800:])
 return json.loads(p.stdout)
def active(s):return next(d for d in s['documents'] if d['id']==s['active']['documentId'])
spec=json.loads((R/'source/ui-pixel-design.json').read_text());expected={}
for layer in spec['layers']:
 for x,y,w,c in layer['runs']:
  for xx in range(x,x+w):expected[xx,y]=tuple(bytes.fromhex(c[1:]))+(255,)
png=R/'exports/charger-ui-176x166.png';im=Image.open(png).convert('RGBA');assert im.size==(176,166)
for y in range(166):
 for x in range(176):assert im.getpixel((x,y))==expected.get((x,y),(0,0,0,0)),(x,y)
src=R/'source/charger-ui-176x166.aseprite';assert struct.unpack_from('<HH',src.read_bytes(),8)==(176,166)
fnv=0xcbf29ce484222325
for b in im.tobytes():fnv=((fnv^b)*0x100000001b3)&0xffffffffffffffff
ph=f'fnv1a64:{fnv:016x}';cli('open',src);s=active(cli('state','--pixel-hash'));cli('open',png);p=active(cli('state','--pixel-hash'));assert s['pixelHash']['value']==p['pixelHash']['value']==ph and s['layerCount']==4 and s['frameCount']==1 and s['colorMode']=='rgb'
slots=spec['slots'];assert len(slots)==37 and slots[0]==[26,35] and slots[1:]==[[8+18*x,84+18*y] for y in range(3) for x in range(9)]+[[8+18*x,142] for x in range(9)]
for i,(x,y) in enumerate(slots):
 assert 0<=x-1<x+17<=176 and 0<=y-1<y+17<=166
 for ox,oy in slots[i+1:]:assert not(x<ox+16 and x+16>ox and y<oy+16 and y+16>oy)
result=dict(asset='F04-04',date='2026-10-05',allPassed=True,size=[176,166],sourceLayers=4,colorMode='rgb',sourceReopened=True,pngReopened=True,sourcePngPixelIdentity=True,pixelHash=ph,matchesAuthoredPixels=True,alphaValues=sorted({c[3] for c in im.get_flattened_data()}),visibleColors=len({c for c in im.get_flattened_data() if c[3]}),actualSlotCount=37,chargerSlots=1,playerSlots=36,slotCoordinatesUnchanged=True,noSlotOverlaps=True,resizedConcept=False,bakedText=False,bakedEnergy=False,integrated=False)
(R/'review/asset-checks.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');cli('open',src);print('原生底图、四层源稿重开一致；37个实际槽位保持原坐标，无重叠。')
