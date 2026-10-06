"""Native texture authoring through typed Aseprite operations; PIL is read-only."""
from pathlib import Path
from PIL import Image
import json,subprocess
R=Path(__file__).resolve().parents[1];CTL='/Users/zhou/Desktop/Aseprite Web 本机版/plugins/aseprite-web/scripts/aseprite-web-control'
C={'dark':'#293632','shadow':'#3e4f49','mid':'#596e64','light':'#7d9186','steel_d':'#47545b','steel':'#83949a','rim':'#b6c3c3','shine':'#d7ddce','copper_d':'#6e3c28','copper':'#a95b3a','copper_l':'#db8a55','copper_h':'#f2b879','off':'#8b9c91','amber':'#d8a24d','green':'#7aae83'}
def cli(*args):
 p=subprocess.run([CTL,'cli',*map(str,args)],capture_output=True,text=True,timeout=55)
 if p.returncode:raise RuntimeError(p.stderr[-800:])
 return json.loads(p.stdout)
def rect(d,x,y,w,h,color):
 for yy in range(y,y+h):
  for xx in range(x,x+w):d[xx,yy]=C.get(color,color)
def runs(d):
 result=[]
 for y in range(64):
  x=0
  while x<64:
   c=d.get((x,y))
   if c is None:x+=1;continue
   w=1
   while x+w<64 and d.get((x+w,y))==c:w+=1
   result.append([x,y,w,c]);x+=w
 return result
base={};detail={};borrowed={};glyph={};regions={}
def region(name,x,y,w,h,colors):
 regions[name]={'pixels':[x,y,w,h],'uv':[x/4,y/4,(x+w)/4,(y+h)/4]}
 for j in range(h):rect(base,x,y+j,w,1,colors[min(len(colors)-1,j*len(colors)//h)])
region('steel',32,0,32,16,['rim','steel','steel_d','dark']);region('rim',32,16,16,16,['shine','rim','steel','steel_d']);region('copper',48,16,16,16,['copper_h','copper_l','copper','copper_d']);region('housing',0,32,16,16,['light','mid','shadow','dark']);region('inset',16,32,16,16,['shadow','dark']);region('vent',32,32,16,16,['shadow','dark']);region('rearplate',48,32,16,16,['mid','shadow','dark']);region('neutral',40,48,24,16,['dark']);region('top',0,56,40,8,['rim','steel','steel_d']);
for x,y,w,h,col in [(34,3,8,2,'steel'),(48,5,10,2,'steel_d'),(38,9,12,2,'steel_d'),(55,12,6,2,'dark'),(33,17,10,1,'shine'),(35,22,8,2,'steel'),(43,27,4,2,'steel_d'),(49,17,10,1,'copper_h'),(51,23,7,2,'copper'),(59,27,3,2,'copper_d'),(1,35,5,2,'mid'),(7,38,7,2,'shadow'),(2,42,8,2,'dark'),(18,35,9,2,'dark'),(24,41,5,2,'shadow'),(50,35,10,2,'dark'),(51,41,3,3,'copper_d'),(61,37,2,3,'copper'),(3,57,10,1,'rim'),(24,60,12,1,'steel_d')]:rect(detail,x,y,w,h,col)
for y in [35,39,43]:rect(detail,34,y,12,2,'dark');rect(detail,35,y,10,1,'steel_d')
im=Image.open(R/'references/battery-atlas-32.png').convert('RGBA')
for y in range(32):
 for x in range(32):
  r,g,b,a=im.getpixel((x,y))
  if a:borrowed[x,y]=f'#{r:02x}{g:02x}{b:02x}'
colors={0:'off',1:'amber',2:'green',3:'green',4:'amber'}
shapes={0:[(x,y) for x in range(2,6) for y in range(2,6) if x in (2,5) or y in (2,5)],1:[(x,y) for x in [1,2,5,6] for y in [3,4]],2:[(2,1),(3,2),(4,3),(5,4),(4,5),(3,6),(2,7)],3:[(1,4),(2,5),(3,6),(4,5),(5,4),(6,3)],4:[(x,y) for x in [2,5] for y in range(2,7)]}
for s in range(5):
 x=s*8;region('status_'+str(s),x,48,8,8,['dark']);rect(glyph,x,48,8,1,'shadow')
 for xx,yy in shapes[s]:rect(glyph,x+xx,48+yy,1,1,colors[s])
start=cli('state','--pixel-hash');(R/'review/aseprite-starting-document.json').write_text(json.dumps({'active':start['active']},indent=2)+'\n')
cli('new',64,64,'--color-mode','rgb');layers=[]
for i,(name,pixels) in enumerate([('01 充电器材质与色阶',base),('02 金属铜面像素簇',detail),('03 原电池32px材质原样复用',borrowed),('04 五种状态形状',glyph)]):
 if i:cli('layer','create',name)
 else:cli('layer','update',cli('state')['active']['layerId'],'--name',name)
 spans=runs(pixels);layers.append({'name':name,'runs':spans})
 for offset in range(0,len(spans),160):
  args=['runs','--frame','0']
  for x,y,w,c in spans[offset:offset+160]:args+=['--run',f'{x},{y},{w},{c}ff']
  cli(*args)
cli('save','charger-atlas-64.aseprite','--output',R/'source/charger-atlas-64.aseprite');cli('export','charger-atlas-64.png','--output',R/'exports/charger-atlas-64.png')
(R/'source/pixel-designs.json').write_text(json.dumps({'palette':C,'files':[{'name':'charger-atlas-64','size':[64,64],'layers':layers}]},ensure_ascii=False,indent=2)+'\n');(R/'source/uv-regions.json').write_text(json.dumps({'atlasSize':[64,64],'regions':regions,'borrowedBatteryArea':[0,0,32,32],'batteryUvTransform':'old battery UV / 2'},indent=2)+'\n')
print('64px共用图集与4层源稿已保存；32px原电池区域不重采样。',flush=True)
