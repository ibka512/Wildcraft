"""Author native 64px atlas via typed Aseprite operations. PIL only reads source patches."""
from pathlib import Path
from PIL import Image
import json,subprocess
R=Path(__file__).resolve().parents[1]
CTL='/Volumes/固态/开发/AI/工具/Aseprite Web 本机版/plugins/aseprite-web/scripts/aseprite-web-control'
C={'dark':'#293632','shadow':'#3e4f49','mid':'#596e64','light':'#7d9186','steel_d':'#47545b','steel':'#83949a','rim':'#b6c3c3','shine':'#d7ddce','copper_d':'#6e3c28','copper':'#a95b3a','copper_l':'#db8a55','copper_h':'#f2b879','off':'#8b9c91','amber':'#d8a24d','green':'#7aae83','red_d':'#6d302a','red':'#b74a39','red_l':'#db7151'}
def cli(*args):
 p=subprocess.run([CTL,'cli',*map(str,args)],capture_output=True,text=True,timeout=55)
 if p.returncode:raise RuntimeError(p.stderr[-1000:])
 d=json.loads(p.stdout)
 if d.get('ok') is False:raise RuntimeError(str(d))
 return d
def rect(d,x,y,w,h,color):
 for yy in range(y,y+h):
  for xx in range(x,x+w):d[xx,yy]=C.get(color,color)
def spans(d):
 a=[]
 for y in range(64):
  x=0
  while x<64:
   c=d.get((x,y))
   if c is None:x+=1;continue
   w=1
   while x+w<64 and d.get((x+w,y))==c:w+=1
   a.append([x,y,w,c]);x+=w
 return a
base={};borrowed={};detail={};glyph={};regions={};reuse=[]
def region(name,x,y,w,h,colors):
 regions[name]={'pixels':[x,y,w,h],'uv':[x/4,y/4,(x+w)/4,(y+h)/4]}
 for j in range(h):rect(base,x,y+j,w,1,colors[min(len(colors)-1,j*len(colors)//h)])
region('front',0,0,32,32,['mid','shadow','dark']);region('rear',32,0,16,16,['light','mid','shadow','dark']);region('side',48,0,16,16,['light','mid','shadow','dark']);region('top',32,16,16,16,['rim','steel','steel_d']);region('vent',48,16,16,16,['shadow','dark'])
region('steel',0,32,16,16,['rim','steel','steel_d','dark']);region('rim',16,32,8,16,['shine','rim','steel','steel_d']);region('copper',24,32,8,16,['copper_h','copper_l','copper','copper_d']);region('housing',32,32,16,16,['light','mid','shadow','dark']);region('inset',48,32,16,16,['shadow','dark']);region('bottom',0,48,16,8,['steel_d','dark'])
for name,x in [('input_copper',16),('input_redstone',24),('idle',32),('working',40),('complete',48),('blank',56)]:region(name,x,48,8,8,['dark'])
region('trim',0,56,64,8,['rim','steel','steel_d','dark'])
im=Image.open(R/'references/charger-charger-atlas-64.png').convert('RGBA');old=json.loads((R/'references/charger-uv-regions.json').read_text())['regions']
for name in ['steel','rim','copper','housing','inset','vent']:
 sx,sy,sw,sh=old[name]['pixels'];dx,dy,w,h=regions[name]['pixels'];assert w<=sw and h<=sh
 for y in range(h):
  for x in range(w):
   r,g,b,a=im.getpixel((sx+x,sy+y));assert a==255;borrowed[x+dx,y+dy]=f'#{r:02x}{g:02x}{b:02x}'
 reuse.append({'region':name,'sourcePixels':[sx,sy,w,h],'destinationPixels':[dx,dy,w,h],'resampled':False})
# Device-specific panel seams and deliberate large clusters, not noise.
for x,y,w,h,c in [(1,1,30,1,'light'),(1,2,1,27,'shadow'),(30,2,1,28,'dark'),(2,29,28,2,'dark'),(4,7,24,1,'dark'),(5,8,22,1,'light'),(7,12,18,2,'dark'),(7,14,1,8,'light'),(24,14,1,8,'shadow'),(9,16,14,2,'shadow'),(9,18,5,2,'dark'),(17,21,5,2,'shadow'),(4,25,8,1,'shadow'),(17,25,11,1,'shadow'),(33,1,14,1,'light'),(34,4,12,1,'dark'),(34,11,12,1,'dark'),(34,7,3,2,'shadow'),(43,7,3,2,'shadow'),(49,1,14,1,'light'),(49,2,1,12,'shadow'),(62,2,1,12,'dark'),(51,4,10,1,'dark'),(51,11,10,1,'dark'),(52,7,4,2,'mid'),(58,8,3,2,'shadow'),(33,17,14,1,'rim'),(33,18,1,12,'steel'),(46,18,1,12,'dark'),(35,20,10,1,'steel_d'),(35,26,10,1,'dark'),(35,21,2,5,'copper'),(37,24,6,2,'copper_d'),(42,22,2,4,'copper_l'),(36,20,1,1,'copper_h')]:rect(detail,x,y,w,h,c)
for x,y in [(2,3),(27,3),(2,26),(27,26),(34,2),(44,2),(34,13),(44,13),(50,3),(60,3),(50,12),(60,12)]:rect(detail,x,y,2,2,'steel_d');rect(detail,x,y,1,1,'rim')
for x in [4,16,28,40,52]:rect(detail,x,57,7,1,'rim');rect(detail,x+1,59,5,1,'steel_d');rect(detail,x+3,62,4,1,'dark')
# Shape-coded materials and state glyphs. No hidden product depicted.
for name in ['input_copper','input_redstone','idle','working','complete']:x,y,w,h=regions[name]['pixels'];rect(glyph,x,y,w,1,'shadow')
x=16;rect(glyph,x+1,51,6,3,'copper_d');rect(glyph,x+2,50,4,3,'copper');rect(glyph,x+2,50,4,1,'copper_h');rect(glyph,x+1,51,1,2,'copper_l')
x=24
for xx,yy,c in [(3,1,'red_l'),(2,2,'red'),(3,2,'red_l'),(4,2,'red'),(1,3,'red_d'),(2,3,'red'),(3,3,'red'),(4,3,'red_l'),(5,3,'red'),(2,4,'red_d'),(3,4,'red'),(4,4,'red'),(3,5,'red_d')]:rect(glyph,x+xx,48+yy,1,1,c)
for xx in range(2,6):
 for yy in range(2,6):
  if xx in [2,5] or yy in [2,5]:rect(glyph,32+xx,48+yy,1,1,'off')
for xx,yy in [(1,2),(2,2),(3,3),(4,4),(3,5),(2,6),(1,6),(4,2),(5,3),(6,4),(5,5),(4,6)]:rect(glyph,40+xx,48+yy,1,1,'amber')
for xx,yy in [(1,4),(2,5),(3,6),(4,5),(5,4),(6,3),(6,2)]:rect(glyph,48+xx,48+yy,1,1,'green')
start=cli('state','--pixel-hash');(R/'review/aseprite-starting-document.json').write_text(json.dumps({'active':start['active']},indent=2)+'\n')
cli('new',64,64,'--color-mode','rgb');layers=[]
for i,(name,pixels) in enumerate([('01 制造机基础面板',base),('02 已采用充电器材质原样复用',borrowed),('03 面板接缝与像素簇',detail),('04 双材料与三态符号',glyph)]):
 if i:cli('layer','create',name)
 else:cli('layer','update',cli('state')['active']['layerId'],'--name',name)
 runs=spans(pixels);layers.append({'name':name,'runs':runs})
 for off in range(0,len(runs),160):
  args=['runs','--frame','0']
  for x,y,w,c in runs[off:off+160]:args+=['--run',f'{x},{y},{w},{c}ff']
  cli(*args)
cli('save','fabricator-atlas-64.aseprite','--output',R/'source/fabricator-atlas-64.aseprite');cli('export','fabricator-atlas-64.png','--output',R/'exports/fabricator-atlas-64.png')
(R/'source/pixel-designs.json').write_text(json.dumps({'palette':C,'files':[{'name':'fabricator-atlas-64','size':[64,64],'layers':layers}]},ensure_ascii=False,indent=2)+'\n')
(R/'source/uv-regions.json').write_text(json.dumps({'atlasSize':[64,64],'regions':regions,'materialReuse':reuse},indent=2)+'\n')
print('Native64 atlas, 4 layers, six unresampled adopted material patches; 2 material and 3 state glyphs',flush=True)
