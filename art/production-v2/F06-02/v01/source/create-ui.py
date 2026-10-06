"""Native 176x192 panel, through typed Aseprite runs. No baked labels, items or progress."""
from pathlib import Path
import subprocess,json
R=Path(__file__).resolve().parents[1];CTL='/Volumes/固态/开发/AI/工具/Aseprite Web 本机版/plugins/aseprite-web/scripts/aseprite-web-control';W,H=176,192
P={'outline':'#293632','metal_dark':'#47545b','metal':'#83949a','metal_edge':'#b6c3c3','shine':'#d7ddce','copper_d':'#6e3c28','copper':'#a95b3a','copper_l':'#db8a55','copper_h':'#f2b879','panel':'#d3ddd1','panel_l':'#e1e8dc','panel_d':'#b4c2b5','well_dark':'#637066','well':'#9aa99d','well_light':'#b6c5b6','well_high':'#edf1e5'}
layers=[]
def layer(name):d={};layers.append((name,d));return d
def rect(d,x,y,w,h,c):
 for yy in range(y,y+h):
  for xx in range(x,x+w):assert 0<=xx<W and 0<=yy<H;d[xx,yy]=P.get(c,c)
def line(d,x,y,w,c):rect(d,x,y,w,1,c)
def slot(d,x,y):
 rect(d,x-1,y-1,18,18,'well_dark');rect(d,x,y,17,17,'well');line(d,x,y,16,'well_dark');rect(d,x,y,1,16,'well_dark');line(d,x,y+16,17,'well_high');rect(d,x+16,y,1,17,'well_high');line(d,x+1,y+1,15,'well_light')
a=layer('01 钢色边框与铜紧固点');rect(a,2,0,172,H,'outline');rect(a,0,2,W,H-4,'outline');rect(a,2,2,172,H-4,'metal');line(a,3,2,170,'metal_edge');rect(a,2,3,1,H-8,'metal_edge');line(a,3,H-3,170,'metal_dark');rect(a,173,3,1,H-8,'metal_dark')
for x,y in [(3,3),(170,3),(3,185),(170,185)]:rect(a,x,y,3,3,'copper_d');rect(a,x,y,2,2,'copper');line(a,x,y,2,'copper_l');rect(a,x,y,1,1,'copper_h')
b=layer('02 双材料加工区与浅绿灰底板');rect(b,5,5,166,181,'panel');line(b,5,5,166,'shine');rect(b,5,6,1,180,'panel_l');line(b,6,185,164,'panel_d');rect(b,170,6,1,179,'panel_d');rect(b,6,6,164,12,'panel_l');line(b,8,18,160,'panel_d')
rect(b,8,29,160,45,'panel_l');line(b,8,29,160,'panel_d');line(b,8,73,160,'panel_d');rect(b,8,86,160,12,'panel_l');line(b,8,86,160,'panel_d');line(b,8,97,160,'panel_d');rect(b,5,108,166,77,'panel_d');line(b,6,108,164,'panel_l')
for x,y in [(22,31),(50,31),(130,51)]:rect(b,x,y,24,23,'metal_dark');line(b,x,y,24,'metal_edge');rect(b,x,y+1,1,21,'metal');line(b,x+1,y+22,23,'metal_dark')
c=layer('03 两输入一产出与原版36格');slots=[(26,35),(54,35),(134,54)]+[(8+18*x,110+18*y) for y in range(3) for x in range(9)]+[(8+18*x,168) for x in range(9)]
for x,y in slots:slot(c,x,y)
d=layer('04 进度轨道与克制材质细节');rect(d,80,59,43,7,'well_dark');rect(d,81,60,41,5,'well');line(d,80,66,43,'well_high');rect(d,155,7,10,4,'copper_d');line(d,155,7,10,'copper_l');line(d,156,8,8,'copper');line(d,157,8,3,'copper_h')
for x,y,w,color in [(13,32,4,'panel_l'),(13,54,4,'panel_d'),(75,35,3,'panel_d'),(76,45,4,'panel_l'),(124,62,3,'panel_d'),(156,55,4,'panel_l'),(156,69,4,'panel_d'),(166,80,1,'panel_l')]:line(d,x,y,w,color)
def runs(pixels):
 result=[]
 for y in range(H):
  x=0
  while x<W:
   col=pixels.get((x,y))
   if col is None:x+=1;continue
   w=1
   while x+w<W and pixels.get((x+w,y))==col:w+=1
   result.append([x,y,w,col]);x+=w
 return result
spec={'name':'fabricator-ui-176x192','size':[W,H],'palette':P,'layers':[{'name':n,'runs':runs(p)} for n,p in layers],'slots':slots,'inputSlots':2,'outputSlots':1,'playerSlots':36,'button':[104,30,64,20],'progressTrack':[80,59,43,7],'progressFill':[81,60,41,5],'bakedText':False,'bakedItems':False,'bakedProgress':False,'buttonTexture':'reuse vanilla on integration; preview approximation'};(R/'source/ui-pixel-design.json').write_text(json.dumps(spec,ensure_ascii=False,indent=2)+'\n')
def cli(*args):
 p=subprocess.run([CTL,'cli',*map(str,args)],capture_output=True,text=True,timeout=55)
 if p.returncode:raise RuntimeError(p.stderr[-800:])
 d=json.loads(p.stdout);assert d.get('ok') is not False;return d
before=cli('state','--pixel-hash');(R/'review/aseprite-starting-document.json').write_text(json.dumps({'active':before['active']},indent=2)+'\n');cli('new',W,H,'--color-mode','rgb')
for i,e in enumerate(spec['layers']):
 if i:cli('layer','create',e['name'])
 else:cli('layer','update',cli('state')['active']['layerId'],'--name',e['name'])
 for off in range(0,len(e['runs']),170):
  args=['runs','--frame','0']
  for x,y,w,col in e['runs'][off:off+170]:args+=['--run',f'{x},{y},{w},{col}ff']
  cli(*args)
 print(e['name']+' complete',flush=True)
cli('save','fabricator-ui-176x192.aseprite','--output',R/'source/fabricator-ui-176x192.aseprite');cli('export','fabricator-ui-176x192.png','--output',R/'exports/fabricator-ui-176x192.png');print('Native176x192 panel and 4-layer source saved',flush=True)
