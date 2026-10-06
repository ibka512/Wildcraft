"""Draw native16 fallback through typed Aseprite Web commands; Pillow only reads/verifies."""
from pathlib import Path
import subprocess, json, time, hashlib, shutil, sys
from PIL import Image
R=Path(__file__).resolve().parents[1]
CLI='/Volumes/固态/开发/AI/工具/Aseprite Web 本机版/plugins/aseprite-web/scripts/aseprite-web-control'
def dump(p,d):p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n')
def call(*args):
 p=subprocess.run([CLI,'cli',*map(str,args)],capture_output=True,text=True)
 if p.returncode:raise RuntimeError(p.stderr or p.stdout)
 d=json.loads(p.stdout)
 if d.get('ok') is False:raise RuntimeError(str(d))
 return d
def state(label):
 d=call('state','--pixel-hash');ds=d.get('documents',[]);active=[x for x in ds if x.get('active')]
 dump(R/'review'/('aseprite-'+label+'.json'),{**d,'documents':active,'totalDocumentCount':len(ds)})
 return active[0] if active else None
def runs(pixels,ox=0,oy=0):
 args=[]
 for (x,y),color in sorted(pixels.items(),key=lambda p:(p[0][1],p[0][0])):args+=['--run',f'{x+ox},{y+oy},1,{color}']
 call('runs','--frame',0,*args)
def persist(stem):
 call('save',stem+'.aseprite','--output',R/'source'/(stem+'.aseprite'))
 try:call('export','f03-03-'+str(time.time_ns())+'.png','--output',R/'exports'/(stem+'.png'))
 except RuntimeError:
  state('export-retry')
  call('export','f03-03-'+str(time.time_ns())+'.png','--output',R/'exports'/(stem+'.png'))
def rgba(im):return ['#'+''.join(f'{c:02x}' for c in p) for p in im.convert('RGBA').getdata()]
state('before')
# Chamfered metal ring with a two-pixel diagonal disconnect. No image resizing/editing.
outer=set()
for y in range(2,14):
 lo,hi=(4,11) if y in (2,13) else (3,12) if y in (3,12) else (2,13)
 outer.update((x,y) for x in range(lo,hi+1))
# Uniform centered aperture; single-pixel chamfer at internal top-left/bottom-right.
hole={(x,y) for x in range(5,11) for y in range(5,11)}
hole.update([(5,4),(10,11),(4,5),(11,10)])
occupied=outer-hole
occupied={p for p in occupied if sum(p) not in (14,15)}
palette=['#30343aff','#464b52ff','#60666eff','#818890ff','#a8afb5ff','#ced2d6ff','#eef0f1ff']
base={p:palette[0] for p in occupied};shade={}
for x,y in occupied:
 # Crisp one-pixel outline, upper bevel, body, lower bevel; no noisy dithering.
 is_outer=(x-1,y) not in outer or (x+1,y) not in outer or (x,y-1) not in outer or (x,y+1) not in outer
 if is_outer:continue
 above=(x,y-1) not in occupied;left=(x-1,y) not in occupied;below=(x,y+1) not in occupied;right=(x+1,y) not in occupied
 if above or left:c=palette[5] if (x+y)%3 else palette[6]
 elif below or right:c=palette[1]
 elif y<=4 or x<=4:c=palette[4] if x%3 else palette[3]
 else:c=palette[2] if x%4 else palette[3]
 shade[(x,y)]=c
# Explicit bevel highlights on top and left; darker outline remains one pixel wide.
for p in [(4,3),(5,3),(6,3),(7,3),(8,3),(9,3),(3,4),(3,5),(3,6),(3,7),(3,8),(3,9)]:
 if p in occupied:shade[p]=palette[5]
for p in [(5,3),(6,3),(3,5),(3,6)]:
 if p in occupied:shade[p]=palette[6]
pixels={**base,**shade}
dump(R/'source/pixel-plan.json',{'nativeSize':[16,16],'palette':palette,'pixels':[{'x':x,'y':y,'color':c} for (x,y),c in sorted(pixels.items(),key=lambda i:(i[0][1],i[0][0]))]})
# Save a separate extension of the adopted atlas. Never overwrite adopted source/PNG.
adopted=R.parents[1]/'F03-02/v01'
if '--verify-only' not in sys.argv:
 call('new',16,16,'--color-mode','rgb');a=state('created');call('layer','update',a['layers'][0]['id'],'--name','断开轮廓')
 runs(base);call('layer','create','金属亮暗面');runs(shade);persist('fuse-unavailable-16');state('sprite-saved')
 call('open',adopted/'source/fuse-status-atlas-64x32.aseprite');state('adopted-atlas-open')
 call('layer','create','16px 不可用连接方片');runs(pixels,32,16);persist('fuse-shared-atlas-64x32');state('atlas-saved')
proof=[]
for stem,w,h in [('fuse-unavailable-16',16,16),('fuse-shared-atlas-64x32',64,32)]:
 for ext,folder in [('aseprite','source'),('png','exports')]:
  call('open',R/folder/(stem+'.'+ext));state(stem+'-'+ext+'-reopened')
  target=R/'review'/(stem+'-'+ext+'-pixels.json');call('pixels','--frame',0,'--rect',f'0,0,{w},{h}','--output',target)
  assert json.loads(target.read_text())['colors']==rgba(Image.open(R/'exports'/(stem+'.png')))
  proof.append({'asset':stem,'format':ext,'reopened':True,'pixelExact':True})
sprite=Image.open(R/'exports/fuse-unavailable-16.png').convert('RGBA');atlas=Image.open(R/'exports/fuse-shared-atlas-64x32.png').convert('RGBA');previous=Image.open(adopted/'exports/fuse-status-atlas-64x32.png').convert('RGBA')
assert sprite.size==(16,16) and atlas.size==(64,32)
assert atlas.crop((32,16,48,32)).tobytes()==sprite.tobytes()
layout=json.loads((adopted/'exports/atlas-layout.json').read_text())
for region in layout['regions']:
 box=(region['x'],region['y'],region['x']+region['width'],region['y']+region['height']);assert atlas.crop(box).tobytes()==previous.crop(box).tobytes()
for y in range(32):
 for x in range(64):
  if not(32<=x<48 and 16<=y<32):assert atlas.getpixel((x,y))==previous.getpixel((x,y))
# Flood fill verifies a deliberate broken connection, not accidental loose pixels.
left=set(occupied);components=[]
while left:
 todo=[left.pop()];c=[]
 while todo:
  p=todo.pop();c.append(p)
  for q in [(p[0]+1,p[1]),(p[0]-1,p[1]),(p[0],p[1]+1),(p[0],p[1]-1)]:
   if q in left:left.remove(q);todo.append(q)
 components.append(len(c))
assert len(components)==2,components
check={'asset':'F03-03','allPassed':True,'spriteSize':[16,16],'opaquePixels':len(occupied),'connectedComponents':sorted(components),'colorCount':len({c for c in pixels.values()}),'alphaValues':sorted(set(sprite.getchannel('A').getdata())),'spriteLayers':2,'atlasLayers':4,'oldAtlasPixelsUnchangedOutsideNewRegion':True,'newAtlasRegionPixelExact':True,'sourceAndPNGReopenedReadback':proof,'native32SpriteCreated':False,'gameIntegrated':False}
dump(R/'review/pixel-checks.json',check)
dump(R/'exports/atlas-layout.json',{'asset':'F03-03','version':'v01','adopted':False,'integrated':False,'size':[64,32],'filter':'nearest','mipmaps':False,'regions':layout['regions']+[{'name':'16px 不可用连接方片','source':'fuse-unavailable-16.png','x':32,'y':16,'width':16,'height':16}],'reusedRegionSources':'references/adopted-atlas-layout.json; old regions are reused, not separate new production sprites'})
print(json.dumps(check,ensure_ascii=False))
