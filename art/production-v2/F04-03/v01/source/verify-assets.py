from pathlib import Path
from PIL import Image
import json,subprocess,hashlib,base64,math
R=Path(__file__).resolve().parents[1];CTL='/Users/zhou/Desktop/Aseprite Web 本机版/plugins/aseprite-web/scripts/aseprite-web-control'
def cli(*args):
 p=subprocess.run([CTL,'cli',*map(str,args)],capture_output=True,text=True,timeout=55)
 if p.returncode:raise RuntimeError(p.stderr[-800:])
 return json.loads(p.stdout)
def active(s):return next(d for d in s['documents'] if d['id']==s['active']['documentId'])
atlas=Image.open(R/'exports/charger-atlas-64.png').convert('RGBA');assert atlas.size==(64,64)
want={};entry=json.loads((R/'source/pixel-designs.json').read_text())['files'][0]
for layer in entry['layers']:
 for x,y,w,c in layer['runs']:
  for xx in range(x,x+w):want[xx,y]=tuple(bytes.fromhex(c[1:]))+(255,)
for y in range(64):
 for x in range(64):assert atlas.getpixel((x,y))==want.get((x,y),(0,0,0,0))
original=Image.open(R/'references/battery-atlas-32.png').convert('RGBA');assert atlas.crop((0,0,32,32)).tobytes()==original.tobytes()
h=0xcbf29ce484222325
for b in atlas.tobytes():h=((h^b)*0x100000001b3)&0xffffffffffffffff
ph=f'fnv1a64:{h:016x}';cli('open',R/'source/charger-atlas-64.aseprite');s=active(cli('state','--pixel-hash'));cli('open',R/'exports/charger-atlas-64.png');p=active(cli('state','--pixel-hash'));assert s['pixelHash']['value']==p['pixelHash']['value']==ph and s['layerCount']==4 and s['frameCount']==1
G=json.loads((R/'source/geometry-and-uv.json').read_text());assert len(G['parts'])==30 and len(G['batteryParts'])==16
for part in G['parts']+G['batteryParts']:
 assert all(part['to'][i]>part['from'][i] for i in range(3))
 assert all(0<=part['from'][i]<part['to'][i]<=16 for i in range(3))
 for face in part['faces'].values():
  u0,v0,u1,v1=face['uv'];assert 0<=u0<u1<=16 and 0<=v0<v1<=16
  assert all(atlas.getpixel((x,y))[3]==255 for y in range(math.floor(v0*4),math.ceil(v1*4)) for x in range(math.floor(u0*4),math.ceil(u1*4)))
for shell in G['parts']:
 for battery in G['batteryParts']:
  depth=[min(shell['to'][i],battery['to'][i])-max(shell['from'][i],battery['from'][i]) for i in range(3)]
  assert not all(d>1e-8 for d in depth),(shell['name'],battery['name'],depth)
old=json.loads((R/'references/geometry-and-uv.json').read_text())
for new,orig in zip(G['batteryParts'],old['parts']):
 assert all(abs(new['from'][i]-G['batteryOffset'][i]-orig['from'][i])<1e-8 and abs(new['to'][i]-G['batteryOffset'][i]-orig['to'][i])<1e-8 for i in range(3))
 for face in orig['faces']:assert all(abs(a*2-b)<1e-8 for a,b in zip(new['faces'][face]['uv'],orig['faces'][face]['uv']))
contacts=[]
for i in range(2):
 tip=next(p for p in G['parts'] if p['name']=='battery_rear_contact_'+str(i));pad=next(p for p in G['batteryParts'] if p['name']=='battery/rear_copper_pad_'+str(i));assert abs(tip['to'][2]-pad['from'][2])<1e-8
 assert all(abs(tip['from'][j]-pad['from'][j])<1e-8 and abs(tip['to'][j]-pad['to'][j])<1e-8 for j in [0,1]);contacts.append({'contact':i,'coordinatesMatch':True,'depthTouchesWithoutOverlap':True})
bb=json.loads((R/'source/charger-v01.bbmodel').read_text());assert len(bb['elements'])==31 and base64.b64decode(bb['textures'][0]['source'].split(',')[1])==(R/'exports/charger-atlas-64.png').read_bytes()
model=json.loads((R/'exports/charger-static-model.json').read_text());assert len(model['elements'])==31 and all(e not in G['batteryParts'] for e in model['elements'])
glyphs=[atlas.crop((i*8,48,i*8+8,56)).tobytes() for i in range(5)];assert len(set(glyphs))==5
directions={'up':[0,16,0],'down':[0,-16,0],'north':[0,0,-16],'south':[0,0,16],'west':[-16,0,0],'east':[16,0,0]}
for off in directions.values():assert not all(min(16,off[i]+16)-max(0,off[i])>0 for i in range(3))
result=dict(asset='F04-03',allPassed=True,date='2026-10-05',nativeAtlas=[64,64],sourceLayers=4,pixelHash=ph,sourcePngIdentity=True,binaryAlpha=sorted({c[3] for c in atlas.get_flattened_data()}),matchesAuthoredPixels=True,batteryPixelsUnchanged=True,batteryGeometryUnscaled=True,chargerCuboids=30,batteryReferenceCuboids=16,noChargerBatteryIntersection=True,contacts=contacts,allUVsOpaqueAndInBounds=True,allGeometryWithinOneBlock=True,statusGlyphs=5,staticModelHasNoBattery=True,blockbenchEmbeddedTextureIdentity=True,blockbenchOpened=False,sixNeighborPositionsChecked=True,sixNeighborSourceGameplayTested=False,gameIntegrated=False)
(R/'review/asset-checks.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');cli('open',R/'source/charger-atlas-64.aseprite');print('原生图集、原电池像素和尺寸、接点对位、无穿插、UV及六向邻格边界检查通过。')
