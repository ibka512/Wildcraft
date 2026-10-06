from pathlib import Path
from PIL import Image
import json,hashlib,subprocess
ROOT=Path(__file__).resolve().parents[1]
CTL='/Users/zhou/Desktop/Aseprite Web 本机版/plugins/aseprite-web/scripts/aseprite-web-control'
def cli(*a):
 p=subprocess.run([CTL,'cli',*map(str,a)],capture_output=True,text=True,timeout=55)
 if p.returncode:raise RuntimeError(p.stderr[-1000:])
 return json.loads(p.stdout)
def active(st):return next(d for d in st['documents'] if d['id']==st['active']['documentId'])
s=cli('state','--pixel-hash');d=active(s);(ROOT/'review/aseprite-starting-document.json').write_text(json.dumps({'document':d['name'],'size':[d['width'],d['height']],'hash':d.get('pixelHash'),'document_id':d['id']},ensure_ascii=False,indent=2))
checks=[]
for f in sorted((ROOT/'exports').glob('*.png')):
 src=ROOT/'source'/(f.stem+'.aseprite');im=Image.open(f).convert('RGBA');n=int(f.stem.rsplit('-',1)[1]);assert im.size==(n,n)
 cli('open',src);a=active(cli('state','--pixel-hash'));cli('open',f);b=active(cli('state','--pixel-hash'));assert a['pixelHash']['value']==b['pixelHash']['value']
 checks.append({'asset':f.stem,'size':[n,n],'source_layers':a['layerCount'],'source_color_mode':a['colorMode'],'source_hash':a['pixelHash']['value'],'png_hash':b['pixelHash']['value'],'pixel_identity':True,'png_sha256':hashlib.sha256(f.read_bytes()).hexdigest(),'source_sha256':hashlib.sha256(src.read_bytes()).hexdigest()});print(f.stem+' 已采用原件与源稿重开一致',flush=True)
for row in json.loads((ROOT/'references/reused-assets.json').read_text()):assert hashlib.sha256((ROOT/row['copied']).read_bytes()).hexdigest()==row['sha256']
(ROOT/'review/icon-verification.json').write_text(json.dumps({'reused':True,'modified':False,'all_passed':True,'checks':checks},ensure_ascii=False,indent=2)+'\n')
cli('open',ROOT/'source/stamina-recovery-16.aseprite')
