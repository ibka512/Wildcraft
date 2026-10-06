"""Native charger UI authoring, typed Aseprite pixel runs; no image resampling."""
from pathlib import Path
import subprocess,json
R=Path(__file__).resolve().parents[1];CTL='/Users/zhou/Desktop/Aseprite Web 本机版/plugins/aseprite-web/scripts/aseprite-web-control';W,H=176,166
P={'outline':'#293632','metal_dark':'#47545b','metal':'#83949a','metal_edge':'#b6c3c3','shine':'#d7ddce','copper_d':'#6e3c28','copper':'#a95b3a','copper_l':'#db8a55','copper_h':'#f2b879','panel':'#d3ddd1','panel_l':'#e1e8dc','panel_d':'#b4c2b5','well_dark':'#637066','well':'#9aa99d','well_light':'#b6c5b6','well_high':'#edf1e5'}
layers=[]
def layer(name):d={};layers.append((name,d));return d
def rect(d,x,y,w,h,c):
 for yy in range(y,y+h):
  for xx in range(x,x+w):assert 0<=xx<W and 0<=yy<H;d[xx,yy]=P.get(c,c)
def line(d,x,y,w,c):rect(d,x,y,w,1,c)
def slot(d,x,y):
 rect(d,x-1,y-1,18,18,'well_dark');rect(d,x,y,17,17,'well');line(d,x,y,16,'well_dark');rect(d,x,y,1,16,'well_dark');line(d,x,y+16,17,'well_high');rect(d,x+16,y,1,17,'well_high');line(d,x+1,y+1,15,'well_light')
a=layer('01 钢色边框与铜紧固点');rect(a,2,0,172,H,'outline');rect(a,0,2,W,H-4,'outline');rect(a,2,2,172,H-4,'metal');line(a,3,2,170,'metal_edge');rect(a,2,3,1,158,'metal_edge');line(a,3,163,170,'metal_dark');rect(a,173,3,1,159,'metal_dark')
for x,y in [(3,3),(170,3),(3,159),(170,159)]:rect(a,x,y,3,3,'copper_d');rect(a,x,y,2,2,'copper');line(a,x,y,2,'copper_l');rect(a,x,y,1,1,'copper_h')
b=layer('02 浅绿灰底板与状态分区');rect(b,5,5,166,155,'panel');line(b,5,5,166,'shine');rect(b,5,6,1,154,'panel_l');line(b,6,159,164,'panel_d');rect(b,170,6,1,153,'panel_d');rect(b,6,6,164,12,'panel_l');line(b,8,18,160,'panel_d');line(b,8,19,160,'panel_l')
rect(b,8,57,160,14,'panel_l');line(b,8,57,160,'panel_d');line(b,8,70,160,'panel_d');rect(b,5,82,166,77,'panel_d');line(b,6,82,164,'panel_l')
# Metal socket holder is decoration around the single actual slot, no extra hit target.
rect(b,22,31,24,24,'metal_dark');line(b,22,31,24,'metal_edge');rect(b,22,32,1,22,'metal');line(b,23,54,23,'metal_dark');rect(b,45,32,1,22,'metal_dark')
c=layer('03 单电池槽与原版36格');slots=[(26,35)]+[(8+18*x,84+18*y) for y in range(3) for x in range(9)]+[(8+18*x,142) for x in range(9)]
for x,y in slots:slot(c,x,y)
d=layer('04 电量轨道与克制的材质细节');rect(d,54,47,107,7,'well_dark');rect(d,55,48,105,5,'well');line(d,54,54,107,'well_high');rect(d,156,7,9,4,'copper_d');line(d,156,7,9,'copper_l');line(d,157,8,7,'copper');line(d,158,8,3,'copper_h')
for x,y,w,color in [(11,27,4,'panel_l'),(164,26,3,'panel_l'),(165,43,2,'panel_d'),(12,53,3,'panel_l'),(44,56,3,'panel_d'),(165,75,2,'panel_l')]:line(d,x,y,w,color)
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
spec={'name':'charger-ui-176x166','size':[W,H],'palette':P,'layers':[{'name':name,'runs':runs(pixels)} for name,pixels in layers],'slots':slots,'chargerSlots':1,'playerSlots':36,'no_baked_text':True,'no_baked_energy':True,'no_baked_items':True,'no_extra_input_or_button':True};(R/'source/ui-pixel-design.json').write_text(json.dumps(spec,ensure_ascii=False,indent=2)+'\n')
def cli(*args):
 p=subprocess.run([CTL,'cli',*map(str,args)],capture_output=True,text=True,timeout=55)
 if p.returncode:raise RuntimeError(p.stderr[-800:])
 return json.loads(p.stdout)
before=cli('state','--pixel-hash');(R/'review/aseprite-starting-document.json').write_text(json.dumps({'active':before['active']},indent=2)+'\n');cli('new',W,H,'--color-mode','rgb')
for i,entry in enumerate(spec['layers']):
 if i:cli('layer','create',entry['name'])
 else:cli('layer','update',cli('state')['active']['layerId'],'--name',entry['name'])
 for offset in range(0,len(entry['runs']),170):
  args=['runs','--frame','0']
  for x,y,w,c in entry['runs'][offset:offset+170]:args+=['--run',f'{x},{y},{w},{c}ff']
  cli(*args)
 print(entry['name']+' 已绘制',flush=True)
cli('save','charger-ui-176x166.aseprite','--output',R/'source/charger-ui-176x166.aseprite');cli('export','charger-ui-176x166.png','--output',R/'exports/charger-ui-176x166.png');print('原生176×166底图与四层源稿已保存。',flush=True)
