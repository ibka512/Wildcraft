"""Author native atlas and independent item icon pixel layers via typed Aseprite commands."""
from pathlib import Path
import subprocess,json
R=Path(__file__).resolve().parents[1];CTL='/Users/zhou/Desktop/Aseprite Web 本机版/plugins/aseprite-web/scripts/aseprite-web-control'
P={'outline':'#242b30','dark':'#354047','shadow':'#47545b','mid':'#5d6e75','light':'#83949a','rim':'#b6c3c3','shine':'#d7ddce','copper_d':'#6e3c28','copper':'#a95b3a','copper_l':'#db8a55','copper_h':'#f2b879','well':'#301d24','well_m':'#49242a','well_l':'#613238','red_d':'#78151c','red':'#bc2323','red_l':'#ed4b2d','red_h':'#ff9457'}
def cli(*a):
 p=subprocess.run([CTL,'cli',*map(str,a)],capture_output=True,text=True,timeout=55)
 if p.returncode:raise RuntimeError(p.stderr[-900:])
 return json.loads(p.stdout)
def rect(d,x,y,w,h,c):
 for j in range(h):
  for i in range(w):d[x+i,y+j]=P.get(c,c)
def line(d,x,y,w,c):rect(d,x,y,w,1,c)
def spans(d,n):
 out=[]
 for y in range(n):
  x=0
  while x<n:
   c=d.get((x,y))
   if c is None:x+=1;continue
   w=1
   while x+w<n and d.get((x+w,y))==c:w+=1
   out.append([x,y,w,c]);x+=w
 return out
def create(name,n,layers):
 cli('new',n,n,'--color-mode','rgb');entries=[]
 for i,(label,d) in enumerate(layers):
  if i:cli('layer','create',label)
  else:cli('layer','update',cli('state')['active']['layerId'],'--name',label)
  runs=spans(d,n);entries.append({'name':label,'runs':runs})
  for k in range(0,len(runs),180):
   a=['runs','--frame','0']
   for x,y,w,c in runs[k:k+180]:a+=['--run',f'{x},{y},{w},{c}ff']
   cli(*a)
 cli('save',name+'.aseprite','--output',R/'source'/(name+'.aseprite'));cli('export',name+'.png','--output',R/'exports'/(name+'.png'))
 print(name+' 原生源稿与PNG保存',flush=True);return {'name':name,'size':[n,n],'layers':entries}

def atlas():
 a={};d={};regions={}
 def region(name,x,y,w,h,colors):
  regions[name]={'pixels':[x,y,w,h],'uv':[x/2,y/2,(x+w)/2,(y+h)/2]}
  for j in range(h):line(a,x,y+j,w,colors[min(len(colors)-1,j*len(colors)//h)])
 region('front',0,0,12,16,[P[x] for x in ['light','mid','shadow','dark']])
 region('shell',12,0,12,16,[P[x] for x in ['light','mid','shadow','dark']])
 region('rim',24,0,8,12,[P[x] for x in ['shine','rim','light','mid']])
 region('copper',24,12,8,8,[P[x] for x in ['copper_h','copper_l','copper','copper_d']])
 region('rear',0,16,12,16,[P[x] for x in ['mid','shadow','dark','outline']])
 region('energy',12,16,12,8,[P[x] for x in ['red_h','red_l','red','red_d']])
 region('window',12,24,12,8,[P[x] for x in ['well_l','well_m','well']])
 for x,y,w,h,c in [(1,3,3,2,'mid'),(6,5,4,2,'shadow'),(2,9,5,2,'dark'),(8,11,3,2,'outline'),(13,3,5,2,'mid'),(19,5,4,2,'shadow'),(15,9,4,2,'dark'),(21,12,2,1,'outline'),(25,2,5,1,'rim'),(26,6,4,1,'light'),(25,13,4,1,'copper_h'),(29,15,2,2,'copper'),(26,18,4,1,'copper_d'),(2,18,4,2,'shadow'),(7,22,3,2,'dark'),(1,27,4,1,'outline'),(14,17,4,1,'red_h'),(20,20,3,1,'red'),(15,26,4,1,'well_m')]:rect(d,x,y,w,h,c)
 return [('01 材质分区与色阶',a),('02 金属铜面像素簇',d)],regions

def icon(n):
 a={};d={};w={}
 if n==32:
  # A flat 3/4 cartridge with a stepped top and two-pixel side depth.
  rect(a,7,7,17,22,'outline');rect(a,9,5,12,24,'outline');rect(a,8,8,14,20,'mid');rect(a,22,8,3,19,'dark');rect(a,23,10,2,15,'shadow');
  rect(a,9,6,13,3,'rim');line(a,10,6,10,'shine');rect(a,10,9,11,17,'well');rect(a,8,9,2,18,'light');rect(a,20,9,2,18,'shadow');line(a,9,27,13,'rim');line(a,10,28,11,'shadow')
  rect(a,13,2,6,4,'copper_d');rect(a,13,2,5,2,'copper_l');line(a,14,2,3,'copper_h');rect(a,18,3,2,3,'copper')
  for y in [11,16,21]:rect(w,11,y,9,3,'well_m');line(w,12,y,7,'well_l')
  for x,y,ww,hh,col in [(8,10,1,6,'rim'),(9,18,1,5,'mid'),(22,11,1,5,'mid'),(24,17,1,5,'outline'),(10,7,2,1,'light'),(19,8,2,1,'mid'),(10,25,11,1,'light'),(21,26,2,1,'mid')]:rect(d,x,y,ww,hh,col)
  masks=[{'x':11,'y':11,'w':9,'h':3},{'x':11,'y':16,'w':9,'h':3},{'x':11,'y':21,'w':9,'h':3}]
 else:
  # Independently compose the terminal, rim, side depth and 3 readable 2px windows.
  rect(a,3,4,10,11,'outline');rect(a,4,3,8,12,'outline');rect(a,4,4,8,10,'mid');rect(a,12,5,1,8,'dark');rect(a,4,4,7,1,'rim');rect(a,5,5,6,8,'well');rect(a,4,5,1,8,'light');rect(a,11,5,1,8,'shadow');line(a,4,14,8,'rim')
  rect(a,6,1,4,3,'copper_d');line(a,6,1,3,'copper_l');line(a,7,1,1,'copper_h');line(a,6,2,3,'copper')
  for y in [6,9,12]:rect(w,5,y,6,2,'well_m');line(w,6,y,4,'well_l')
  for x,y,ww,hh,col in [(4,5,1,3,'rim'),(11,7,1,4,'mid'),(5,14,4,1,'light'),(12,10,1,3,'shadow')]:rect(d,x,y,ww,hh,col)
  masks=[{'x':5,'y':6,'w':6,'h':2},{'x':5,'y':9,'w':6,'h':2},{'x':5,'y':12,'w':6,'h':2}]
 return [('01 金属壳与铜接点',a),('02 外壳体积像素簇',d),('03 暗色三格窗口',w)],masks
if __name__=='__main__':
 start=cli('state','--pixel-hash');(R/'review/aseprite-starting-document.json').write_text(json.dumps({'active':start['active']},indent=2)+'\n')
 authored=[];ls,regions=atlas();authored.append(create('battery-atlas-32',32,ls));(R/'source/uv-regions.json').write_text(json.dumps({'atlasSize':[32,32],'regions':regions},indent=2)+'\n')
 iconregions={}
 for n in [32,16]:
  ls,reg=icon(n);authored.append(create(f'battery-item-{n}',n,ls));iconregions[str(n)]=reg
 (R/'source/pixel-designs.json').write_text(json.dumps({'palette':P,'files':authored},ensure_ascii=False,indent=2)+'\n')
 (R/'exports/battery-item-energy-regions.json').write_text(json.dumps({'coordinateSystem':'origin top-left, native pixels','regions':iconregions,'fillOrder':'bottom then middle then top; each cell fills left to right','capacity':1000,'note':'Empty shell is the reusable base. Red fill is rendered by a mask; exact value remains in real item bar/tooltip.'},indent=2)+'\n')
