from pathlib import Path
from PIL import Image
import json,subprocess,hashlib
R=Path(__file__).resolve().parents[1]
CTL='/Users/zhou/Desktop/Aseprite Web 本机版/plugins/aseprite-web/scripts/aseprite-web-control'
def cli(*a):
 p=subprocess.run([CTL,'cli',*map(str,a)],capture_output=True,text=True,timeout=55)
 if p.returncode:raise RuntimeError(p.stderr[-1000:])
 return json.loads(p.stdout)
def active(s):return next(d for d in s['documents'] if d['id']==s['active']['documentId'])
entries=[];regions=json.loads((R/'exports/energy-regions.json').read_text())['regions']
for n in [16,32]:
 endpoints=[]
 for state in ['empty','full']:
  name=f'battery-{state}-{n}';png=R/'exports'/(name+'.png');src=R/'source'/(name+'.aseprite');im=Image.open(png).convert('RGBA');assert im.size==(n,n);endpoints.append(im)
  h=0xcbf29ce484222325
  for b in im.tobytes():h=((h^b)*0x100000001b3)&0xffffffffffffffff
  ph=f'fnv1a64:{h:016x}';cli('open',src);s=active(cli('state','--pixel-hash'));cli('open',png);p=active(cli('state','--pixel-hash'))
  assert s['pixelHash']['value']==p['pixelHash']['value']==ph and s['frameCount']==1
  assert {c[3] for c in im.get_flattened_data()}=={0,255}
  entries.append(dict(name=name,size=[n,n],colorMode=s['colorMode'],layers=s['layerCount'],sourcePngHash=ph,sourceReopened=True,pngReopened=True,binaryAlpha=True,visibleColors=len({c for c in im.get_flattened_data() if c[3]})))
 mask={(x,y) for r in regions[str(n)] for y in range(r['y'],r['y']+r['h']) for x in range(r['x'],r['x']+r['w'])}
 assert all(endpoints[0].getpixel((x,y))==endpoints[1].getpixel((x,y)) for y in range(n) for x in range(n) if (x,y) not in mask)
 assert all(endpoints[0].getpixel((x,y))[3]==endpoints[1].getpixel((x,y))[3] for y in range(n) for x in range(n))
 changed=sum(endpoints[0].getpixel((x,y))!=endpoints[1].getpixel((x,y)) for x,y in mask);assert changed==(30 if n==16 else 81)
 print(f'{n}px 两端点源稿重开一致，{changed}个变化像素全在窗口内',flush=True)
provenance=json.loads((R/'references/approved-icon-provenance.json').read_text())
for p in provenance:
 assert hashlib.sha256((R/p['copy']).read_bytes()).hexdigest()==p['sha256']==hashlib.sha256(Path(p['source']).read_bytes()).hexdigest()
result=dict(asset='F04-02',allPassed=True,date='2026-10-05',reusedPng=4,reusedAseprite=4,sourceFiles=entries,endpointExteriorIdentical=True,alphaIdentical=True,originalEightFilesUnchanged=True,newArtworkGenerated=False,gameIntegrated=False)
(R/'review/asset-checks.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');cli('open',R/'source/battery-full-16.aseprite')
