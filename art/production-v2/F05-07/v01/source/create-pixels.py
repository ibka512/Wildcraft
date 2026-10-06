"""Native pixels through Aseprite typed operations; PIL only reads adopted pixels."""
from pathlib import Path
from PIL import Image
import json,subprocess
R=Path(__file__).resolve().parents[1];CTL='/Volumes/固态/开发/AI/工具/Aseprite Web 本机版/plugins/aseprite-web/scripts/aseprite-web-control'
P={'outline':'#293632','dark':'#354047','shadow':'#47545b','mid':'#5d6e75','steel':'#83949a','rim':'#b6c3c3','shine':'#d7ddce','green_d':'#3e4f49','green':'#596e64','green_l':'#7d9186','copper_d':'#6e3c28','copper':'#a95b3a','copper_l':'#db8a55','copper_h':'#f2b879','cream':'#ddccb0','cream_l':'#f1e1c5','cream_m':'#c7b496','cream_d':'#ac997b','olive':'#727756','olive_l':'#909674','olive_d':'#535d40','wood':'#936636','wood_l':'#be9154','wood_d':'#634528','paper':'#e7dec3','paper_m':'#d9ceb2','paper_d':'#c9bea1','paper_h':'#f3e9cf','paper_edge':'#aea387'}
def cli(*args):
 p=subprocess.run([CTL,'cli',*map(str,args)],capture_output=True,text=True,timeout=55)
 if p.returncode:raise RuntimeError(p.stderr[-700:])
 return json.loads(p.stdout)
def rect(d,x,y,w,h,c):
 for yy in range(y,y+h):
  for xx in range(x,x+w):d[xx,yy]=P.get(c,c)
def poly(d,points,c):
 for y in range(min(p[1] for p in points),max(p[1] for p in points)+1):
  for x in range(min(p[0] for p in points),max(p[0] for p in points)+1):
   px=x+.5;py=y+.5;inside=False
   for i,a in enumerate(points):
    b=points[(i+1)%len(points)]
    if (a[1]>py)!=(b[1]>py) and px<(b[0]-a[0])*(py-a[1])/(b[1]-a[1])+a[0]:inside=not inside
   if inside:d[x,y]=P.get(c,c)
def line(d,a,b,c):
 x,y=a;xx,yy=b;dx=abs(xx-x);sx=1 if x<xx else -1;dy=-abs(yy-y);sy=1 if y<yy else -1;err=dx+dy
 while True:
  d[x,y]=P.get(c,c)
  if x==xx and y==yy:break
  e=2*err
  if e>=dy:err+=dy;x+=sx
  if e<=dx:err+=dx;y+=sy

def runs(d,n,height=None):
 out=[]
 for y in range(height or n):
  x=0
  while x<n:
   c=d.get((x,y))
   if c is None:x+=1;continue
   w=1
   while x+w<n and d.get((x+w,y))==c:w+=1
   out.append([x,y,w,c]);x+=w
 return out

def create(name,n,layers,height=None):
 cli('new',n,height or n,'--color-mode','rgb');authored=[]
 for i,(label,d) in enumerate(layers):
  if i:cli('layer','create',label)
  else:cli('layer','update',cli('state')['active']['layerId'],'--name',label)
  rs=runs(d,n,height);authored.append(dict(name=label,runs=rs))
  for off in range(0,len(rs),160):
   args=['runs','--frame','0']
   for x,y,w,c in rs[off:off+160]:args+=['--run',f'{x},{y},{w},{c}ff']
   cli(*args)
 cli('save',name+'.aseprite','--output',R/'source'/(name+'.aseprite'));cli('export',name+'.png','--output',R/'exports'/(name+'.png'))
 print(name+' 原生源稿及PNG已保存',flush=True);return dict(name=name,size=[n,height or n],layers=authored)




def icon(n):
 a={};b={};d={}
 if n==32:
  # Rear depth shifts left; actual front ring has four structural spokes and open sectors.
  poly(a,[(9,2),(18,1),(24,4),(28,10),(28,21),(24,27),(17,30),(8,27),(3,21),(2,12),(5,6)],'outline')
  poly(b,[(7,5),(11,3),(9,9),(6,14),(6,20),(9,25),(13,28),(8,27),(4,20),(3,13)],'shadow');line(d,(6,6),(9,4),'steel');line(d,(3,12),(4,10),'mid');line(d,(4,20),(5,22),'mid');line(d,(6,25),(9,27),'steel')
  outer=[(14,2),(22,2),(27,6),(30,12),(30,20),(27,26),(22,30),(14,30),(9,26),(6,20),(6,12),(9,6)]
  poly(a,outer,'outline');poly(b,[(14,3),(21,3),(26,7),(29,12),(29,20),(26,25),(21,29),(14,29),(10,25),(7,20),(7,12),(10,7)],'dark')
  rim=[(14,5),(21,5),(25,9),(27,13),(27,19),(25,23),(21,27),(14,27),(10,23),(9,19),(9,13),(10,9)];poly(b,rim,'steel')
  opening=[(15,7),(20,7),(23,10),(25,14),(25,18),(23,22),(20,25),(15,25),(12,22),(11,18),(11,14),(12,10)]
  holes={};poly(holes,opening,'outline')
  for xy in holes:
   a.pop(xy,None);b.pop(xy,None);d.pop(xy,None)
  # Steel framework, copper inlays kept small so wheel never becomes one solid coin.
  rect(b,16,7,3,18,'shadow');rect(b,11,14,14,3,'shadow');rect(d,16,7,1,7,'rim');rect(d,11,14,6,1,'rim');rect(d,20,14,5,1,'steel');rect(d,16,18,1,7,'steel')
  for x,y,w,h in [(17,8,2,4),(12,15,3,2),(21,15,3,2),(17,20,2,4)]:rect(d,x,y,w,h,'copper');rect(d,x,y,w,1,'copper_l')
  poly(b,[(16,12),(20,12),(22,14),(22,18),(20,20),(16,20),(14,18),(14,14)],'shadow');poly(d,[(17,13),(20,13),(21,15),(21,18),(19,19),(16,18),(15,15)],'copper');line(d,(17,13),(20,13),'copper_h');line(d,(16,14),(16,16),'copper_l');rect(d,19,17,2,1,'copper_d');rect(d,18,15,1,1,'copper_l')
  for p,q in [((14,5),(20,5)),((11,8),(12,7)),((9,13),(9,18)),((11,23),(14,26)),((23,25),(25,23))]:line(d,p,q,'rim')
  for x,y in [(14,3),(19,3),(25,7),(28,13),(28,19),(25,25),(20,28),(14,28),(8,19),(8,13)]:rect(d,x,y,2,1,'mid')
  for p,q in [((12,4),(11,6)),((24,4),(23,6)),((28,9),(26,10)),((28,23),(26,22)),((24,28),(23,26)),((12,28),(13,26)),((7,22),(9,21))]:line(d,p,q,'outline')
 else:
  # Independently planned 16px circle: four open quadrants and an unambiguous copper hub.
  poly(a,[(5,1),(10,1),(13,4),(15,7),(15,10),(12,14),(8,15),(4,13),(1,10),(1,6),(3,3)],'outline')
  poly(b,[(3,4),(5,2),(4,6),(3,10),(5,13),(3,11),(2,9),(2,6)],'shadow')
  poly(b,[(7,2),(10,2),(13,5),(14,7),(14,10),(12,13),(9,14),(6,13),(4,10),(4,7),(5,4)],'dark')
  poly(b,[(7,3),(10,3),(12,5),(13,7),(13,10),(11,12),(9,13),(6,11),(5,9),(5,7),(6,5)],'steel')
  holes={};poly(holes,[(8,4),(10,4),(12,7),(12,9),(10,12),(8,11),(6,9),(6,7)],'outline')
  for xy in holes:a.pop(xy,None);b.pop(xy,None);d.pop(xy,None)
  rect(b,8,4,2,8,'shadow');rect(b,6,7,6,2,'shadow');rect(d,8,4,1,2,'copper_l');rect(d,6,7,1,2,'copper');rect(d,11,7,1,2,'copper_l');rect(d,8,10,1,2,'copper')
  rect(d,8,7,2,2,'copper');rect(d,8,7,2,1,'copper_h');rect(d,9,8,1,1,'copper_d');rect(d,7,7,1,1,'rim');rect(d,9,6,1,1,'steel')
  line(d,(7,3),(10,3),'rim');rect(d,6,5,1,1,'rim');rect(d,5,8,1,2,'rim');rect(d,7,12,1,1,'steel');rect(d,12,10,1,1,'mid')
  for x,y in [(10,2),(13,6),(13,11),(9,13),(4,10),(4,6)]:rect(d,x,y,1,1,'mid')
 return [('01 轮胎外轮廓与真实开孔',a),('02 深色胎面钢圈和四辐条',b),('03 铜轮毂胎面槽及像素明暗',d)]
if __name__=='__main__':
 start=cli('state','--pixel-hash');(R/'review/aseprite-start.json').write_text(json.dumps(dict(active=start['active']),indent=2));files=[create(f'wheel-item-{n}',n,icon(n))for n in [32,16]];(R/'source/pixel-designs.json').write_text(json.dumps(dict(palette=P,files=files),ensure_ascii=False,indent=2)+'\n')
