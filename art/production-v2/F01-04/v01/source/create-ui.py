"""Author native GUI pixel layers via typed Aseprite Web runs; no raster resizing.
The editable pixel specification is independent of the concept reference.
"""
from pathlib import Path
import subprocess,json,hashlib
R=Path(__file__).resolve().parents[1]
CTL='/Users/zhou/Desktop/Aseprite Web 本机版/plugins/aseprite-web/scripts/aseprite-web-control'
W,H=176,182
P={'outline':'#202b30','metal_dark':'#35434b','metal':'#56676f','metal_light':'#81959c','metal_edge':'#bdcccb','paper_dark':'#b5aa89','paper':'#d0c4a5','paper_light':'#e2d7b8','paper_high':'#eee3c8','well_dark':'#716d5d','well':'#a49c82','well_light':'#c3b899','wood_dark':'#705039','wood':'#a8794c','copper':'#be8b53','copper_light':'#e2b778'}
layers=[]
def layer(name):
 d={};layers.append((name,d));return d
def rect(d,x,y,w,h,c):
 for yy in range(y,y+h):
  for xx in range(x,x+w):
   assert 0<=xx<W and 0<=yy<H;(d.__setitem__((xx,yy),P.get(c,c)))
def line(d,x,y,w,c):rect(d,x,y,w,1,c)
def slot(d,x,y):
 rect(d,x-1,y-1,18,18,'well_dark');rect(d,x,y,17,17,'well');line(d,x,y,16,'well_dark');rect(d,x,y,1,16,'well_dark');line(d,x,y+16,17,'paper_high');rect(d,x+16,y,1,17,'paper_high');line(d,x+1,y+1,15,'well_light');
a=layer('01 金属外框与铆钉')
rect(a,2,0,172,H,'outline');rect(a,0,2,W,H-4,'outline');rect(a,2,2,172,H-4,'metal');line(a,3,2,170,'metal_edge');rect(a,2,3,1,176,'metal_light');line(a,3,178,170,'metal_dark');line(a,3,179,170,'outline');rect(a,173,3,1,175,'metal_dark')
# Small wood grip inlays reference the adopted pot, away from slots and text.
for x in (6,161):
 rect(a,x,4,9,3,'wood_dark');line(a,x+1,4,7,'wood');line(a,x+2,5,4,'copper_light')
for x,y in ((3,3),(170,3),(3,175),(170,175)):
 rect(a,x,y,3,3,'metal_dark');rect(a,x,y,2,2,'metal_light');rect(a,x,y,1,1,'metal_edge')
b=layer('02 羊皮纸与区域底板')
rect(b,5,5,166,171,'paper');line(b,5,5,166,'paper_high');rect(b,5,6,1,170,'paper_light');line(b,6,175,165,'paper_dark');rect(b,170,10,1,165,'paper_dark')
# Header and preparation area are quiet, no baked words or false decorative slots.
rect(b,6,6,164,12,'paper_light');line(b,8,18,160,'paper_dark');line(b,8,19,160,'paper_high')
rect(b,10,31,77,42,'paper_light');line(b,11,72,75,'paper_dark');rect(b,127,41,27,24,'paper_light');line(b,127,65,27,'paper_dark')
# Subtle authored paper clusters only in margins, outside text and hit regions.
for x,y,w in [(10,28,4),(82,25,3),(113,21,3),(161,34,3),(12,64,3),(165,69,2),(11,94,3)]:line(b,x,y,w,'paper_light')
# A clear status strip above the inventory; game paints icon and words.
rect(b,8,75,160,12,'paper_light');line(b,8,75,160,'paper_dark');line(b,8,86,160,'paper_high')
rect(b,5,98,166,77,'paper_dark');line(b,6,98,164,'paper_light')
c=layer('03 固定五槽与原版库存槽')
pot=[(26,33),(46,33),(66,33),(46,58),(134,45)]
inv=[(8+18*x,100+18*y) for y in range(3) for x in range(9)]+[(8+18*x,158) for x in range(9)]
for x,y in pot+inv:slot(c,x,y)
# Restrained extra rim highlights distinguish the output without expanding its hitbox.
line(c,133,43,18,'copper');line(c,133,62,18,'copper_light')
d=layer('04 进度轨道与空碗槽提示')
rect(d,155,7,10,5,'wood_dark');rect(d,156,8,8,3,'wood');line(d,157,8,5,'copper_light')
# Original progress rectangle 91,48..123,55, decorative arrow within the same extent.
rect(d,91,48,27,7,'well_dark');rect(d,92,49,25,5,'well');rect(d,118,49,1,5,'well_dark');rect(d,119,50,1,3,'well_dark');rect(d,120,51,3,1,'well_dark');line(d,91,55,27,'paper_high')
# Tiny bowl ghost is baked into the EMPTY bowl well only; real stack covers it.
for x,y,w,key in [(49,63,10,'well_dark'),(49,64,1,'well_dark'),(58,64,1,'well_dark'),(50,65,8,'well_dark'),(51,66,6,'well_dark'),(52,67,4,'well_dark'),(51,64,6,'well_light')]:line(d,x,y,w,key)

def runs(d):
 out=[]
 for y in range(H):
  x=0
  while x<W:
   color=d.get((x,y))
   if color is None:x+=1;continue
   n=1
   while x+n<W and d.get((x+n,y))==color:n+=1
   out.append([x,y,n,color]);x+=n
 return out
spec={'name':'cooking-ui-176x182','size':[W,H],'palette':P,'layers':[{'name':n,'runs':runs(d)} for n,d in layers],'slots':{'pot':pot,'inventory':inv},'no_baked_text':True,'no_extra_fuel_slot':True}
(R/'source/ui-pixel-design.json').write_text(json.dumps(spec,ensure_ascii=False,indent=2)+'\n')
def cli(*args):
 p=subprocess.run([CTL,'cli',*map(str,args)],capture_output=True,text=True,timeout=55)
 if p.returncode:raise RuntimeError(p.stderr[-800:]+p.stdout[-500:])
 return json.loads(p.stdout)
if __name__=='__main__':
 before=cli('state','--pixel-hash');(R/'review/aseprite-starting-document.json').write_text(json.dumps({'active':before.get('active')},ensure_ascii=False,indent=2)+'\n')
 cli('new',W,H,'--color-mode','rgb')
 for i,entry in enumerate(spec['layers']):
  if i:cli('layer','create',entry['name'])
  else:cli('layer','update',cli('state')['active']['layerId'],'--name',entry['name'])
  for offset in range(0,len(entry['runs']),180):
   args=['runs','--frame','0']
   for x,y,w,color in entry['runs'][offset:offset+180]:args+=['--run',f'{x},{y},{w},{color}ff']
   cli(*args)
  print(entry['name']+' 绘制完成',flush=True)
 cli('save','cooking-ui-176x182.aseprite','--output',R/'source/cooking-ui-176x182.aseprite')
 cli('export','cooking-ui-176x182.png','--output',R/'exports/cooking-ui-176x182.png')
 print('原生176×182界面源稿与PNG已保存。',flush=True)
