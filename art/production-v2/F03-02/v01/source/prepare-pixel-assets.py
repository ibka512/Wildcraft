"""Preserve two existing symbols and draw a native 5px corner mark + shared atlas via Aseprite Web."""
from pathlib import Path
import subprocess,json,hashlib,time
from PIL import Image
R=Path(__file__).resolve().parents[1];CLI='/Volumes/固态/开发/AI/工具/Aseprite Web 本机版/plugins/aseprite-web/scripts/aseprite-web-control';records=[];exportFallbacks=[]
def call(*args):
 if args[0]=='export':args=(args[0],'f03-02-'+str(time.time_ns())+'.png',*args[2:])
 p=subprocess.run([CLI,'cli',*map(str,args)],capture_output=True,text=True)
 if p.returncode and args[0]=='export':
  subprocess.run([CLI,'cli','state','--pixel-hash'],capture_output=True,text=True)
  args=(args[0],'f03-02-'+str(time.time_ns())+'.png',*args[2:]);p=subprocess.run([CLI,'cli',*map(str,args)],capture_output=True,text=True)
 if p.returncode and args[0]=='export' and '--output' in args:
  output=Path(args[args.index('--output')+1])
  if output.exists():
   exportFallbacks.append(str(output.relative_to(R)));return {'existingExportPendingReadback':True}
 if p.returncode:raise RuntimeError(p.stderr or p.stdout)
 try:return json.loads(p.stdout)
 except ValueError:return {'output':p.stdout[:300]}
def state(label):
 d=call('state','--pixel-hash');(R/'review'/('aseprite-'+label+'.json')).write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n');a=next(d for d in d['documents'] if d['active']);return a
for size in [16,32]:
 src=R/'references'/f'legacy-fuse-status-{size}.aseprite';call('open',src);before=state('symbol-'+str(size)+'-open');call('save',f'fuse-status-{size}.aseprite','--output',R/'source'/f'fuse-status-{size}.aseprite');call('export',f'fuse-status-{size}.png','--output',R/'exports'/f'fuse-status-{size}.png');call('open',R/'source'/f'fuse-status-{size}.aseprite');after=state('symbol-'+str(size)+'-reopen');call('export',f'check-{size}.png','--output',R/'review'/f'source-reopen-{size}.png');im=Image.open(R/'exports'/f'fuse-status-{size}.png').convert('RGBA');original=Image.open(R/'references'/f'legacy-fuse-status-{size}.png').convert('RGBA');assert im.size==(size,size) and im.tobytes()==original.tobytes();assert im.tobytes()==Image.open(R/'review'/f'source-reopen-{size}.png').convert('RGBA').tobytes();records.append({'size':size,'legacyPixelsUnchanged':True,'sourceReopened':True,'colorMode':after['colorMode'],'layers':after['layerCount']})
# Draw at native 5x5: short warm end, cool end, bright central joining collar.
pixels=[(3,0,'#24242bff'),(4,0,'#171a1dff'),(2,1,'#4e505cff'),(3,1,'#9294a4ff'),(4,1,'#44444fff'),(1,2,'#4e505cff'),(2,2,'#daddE6ff'),(3,2,'#a5abbaFF'),(4,2,'#24242bff'),(0,3,'#a16126ff'),(1,3,'#e8c38aff'),(2,3,'#737989ff'),(0,4,'#59310fff'),(1,4,'#7c4719ff')]
call('new',5,5,'--color-mode','rgb');call('runs',*[x for px in pixels for x in ('--run',f'{px[0]},{px[1]},1,{px[2]}')]);call('save','fuse-corner-5.aseprite','--output',R/'source/fuse-corner-5.aseprite');call('export','fuse-corner-5.png','--output',R/'exports/fuse-corner-5.png');call('open',R/'source/fuse-corner-5.aseprite');badge=state('corner-reopen');call('pixels','--frame',0,'--rect','0,0,5,5','--output',R/'review/corner-reopen-pixels.json');sample=json.loads((R/'review/corner-reopen-pixels.json').read_text());actual=['#'+''.join(f'{v:02x}' for v in px) for px in Image.open(R/'exports/fuse-corner-5.png').convert('RGBA').getdata()];assert sample['colors']==actual
call('new',64,32,'--color-mode','rgb');layers=[]
for index,(name,file,ox) in enumerate([('32px 完整融合符号','fuse-status-32.png',0),('16px 完整融合符号','fuse-status-16.png',32),('5px 槽位连接角标','fuse-corner-5.png',48)]):
 if index:call('layer','create',name)
 else:
  d=state('atlas-created');call('layer','update',d['layers'][0]['id'],'--name',name)
 im=Image.open(R/'exports'/file).convert('RGBA');args=[]
 for y in range(im.height):
  x=0
  while x<im.width:
   c=im.getpixel((x,y));end=x+1
   while end<im.width and im.getpixel((end,y))==c:end+=1
   if c[3]:args.extend(['--run',f'{ox+x},{y},{end-x},#'+''.join(f'{v:02x}' for v in c)])
   x=end
 call('runs',*args);layers.append({'name':name,'source':file,'x':ox,'y':0,'width':im.width,'height':im.height})
call('save','fuse-status-atlas-64x32.aseprite','--output',R/'source/fuse-status-atlas-64x32.aseprite');call('export','fuse-status-atlas-64x32.png','--output',R/'exports/fuse-status-atlas-64x32.png');call('open',R/'source/fuse-status-atlas-64x32.aseprite');s=state('atlas-reopen');call('pixels','--frame',0,'--rect','0,0,64,32','--output',R/'review/atlas-reopen-pixels.json');atlas=Image.open(R/'exports/fuse-status-atlas-64x32.png').convert('RGBA');assert ['#'+''.join(f'{v:02x}' for v in px) for px in atlas.getdata()]==json.loads((R/'review/atlas-reopen-pixels.json').read_text())['colors']
for l in layers:assert atlas.crop((l['x'],l['y'],l['x']+l['width'],l['y']+l['height'])).tobytes()==Image.open(R/'exports'/l['source']).convert('RGBA').tobytes()
proof=[]
for stem,w,h in [('fuse-status-16',16,16),('fuse-status-32',32,32),('fuse-corner-5',5,5),('fuse-status-atlas-64x32',64,32)]:
 for extension,folder in [('aseprite','source'),('png','exports')]:
  call('open',R/folder/(stem+'.'+extension));call('pixels','--frame',0,'--rect',f'0,0,{w},{h}','--output',R/'review'/(stem+'-'+extension+'-readback.json'));d=json.loads((R/'review'/(stem+'-'+extension+'-readback.json')).read_text());im=Image.open(R/'exports'/(stem+'.png')).convert('RGBA');assert d['colors']==['#'+''.join(f'{v:02x}' for v in px) for px in im.getdata()];proof.append({'asset':stem,'format':extension,'pixelExact':True})
(R/'exports/atlas-layout.json').write_text(json.dumps({'asset':'F03-02','size':[64,32],'regions':layers,'filter':'nearest','mipmaps':False,'adopted':False,'integrated':False},ensure_ascii=False,indent=2)+'\n')
(R/'review/pixel-checks.json').write_text(json.dumps({'asset':'F03-02','allPassed':True,'reusedSymbols':records,'cornerSize':[5,5],'newStandaloneMark':1,'atlasSize':[64,32],'atlasLayers':s['layerCount'],'atlasSourceReopened':True,'atlasRegionsPixelExact':True,'alphaValues':sorted(set(atlas.getchannel('A').getdata())),'sourceAndPNGReadbackCases':proof,'exportFallbacksVerifiedByPixelReadback':exportFallbacks,'gameIntegrated':False},ensure_ascii=False,indent=2)+'\n');print('Aseprite saved 4 sources/4 PNGs; legacy symbols pixel exact; native 5px corner mark; 3-layer atlas')
