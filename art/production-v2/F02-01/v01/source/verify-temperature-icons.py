from pathlib import Path
from PIL import Image
import json,hashlib,subprocess
ROOT=Path(__file__).resolve().parents[1];CTL='/Users/zhou/Desktop/Aseprite Web 本机版/plugins/aseprite-web/scripts/aseprite-web-control'
def cli(*a):
 p=subprocess.run([CTL,'cli',*map(str,a)],capture_output=True,text=True,timeout=55)
 if p.returncode:raise RuntimeError(p.stderr[-900:])
 return json.loads(p.stdout)
def active(st):return next(d for d in st['documents'] if d['id']==st['active']['documentId'])
s=active(cli('state','--pixel-hash'));(ROOT/'review/aseprite-starting-document.json').write_text(json.dumps({'name':s['name'],'size':[s['width'],s['height']],'pixelHash':s.get('pixelHash')},ensure_ascii=False,indent=2))
checks=[]
for n in (16,32):
 name=f'ambient-temperature-color-{n}';p=ROOT/'exports'/f'{name}.png';src=ROOT/'source'/f'{name}.aseprite';im=Image.open(p).convert('RGBA');assert im.size==(n,n);assert set(im.getchannel('A').getdata())=={0,255}
 cli('open',src);s=active(cli('state','--pixel-hash'));cli('open',p);e=active(cli('state','--pixel-hash'));assert s['pixelHash']['value']==e['pixelHash']['value']
 expected={16:'fnv1a64:f712c9fbc6472d90',32:'fnv1a64:a31f4c6d893b485f'};assert e['pixelHash']['value']==expected[n]
 checks.append({'name':name,'size':[n,n],'sourceColorMode':s['colorMode'],'sourceLayers':s['layerCount'],'binaryAlpha':True,'sourceHash':s['pixelHash']['value'],'pngHash':e['pixelHash']['value'],'matchesHistoricalV2Hash':True,'pixelIdentity':True,'pngSha256':hashlib.sha256(p.read_bytes()).hexdigest(),'sourceSha256':hashlib.sha256(src.read_bytes()).hexdigest()});print(name+' v2源稿与PNG重开一致，历史像素哈希保持',flush=True)
(ROOT/'review/icon-verification.json').write_text(json.dumps({'date':'2026-10-05','allPassed':True,'modified':False,'adopted':False,'checks':checks},ensure_ascii=False,indent=2)+'\n')
cli('open',ROOT/'source/ambient-temperature-color-16.aseprite')
