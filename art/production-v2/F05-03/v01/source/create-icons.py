"""Native pixels through Aseprite typed operations; PIL only reads adopted pixels."""
from pathlib import Path
from PIL import Image
import json,subprocess
R=Path(__file__).resolve().parents[1];CTL='/Users/zhou/Desktop/Aseprite Web 本机版/plugins/aseprite-web/scripts/aseprite-web-control'
P={'outline':'#293632','dark':'#354047','shadow':'#47545b','mid':'#5d6e75','steel':'#83949a','rim':'#b6c3c3','shine':'#d7ddce','green_d':'#3e4f49','green':'#596e64','green_l':'#7d9186','copper_d':'#6e3c28','copper':'#a95b3a','copper_l':'#db8a55','copper_h':'#f2b879'}
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

def runs(d,n):
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
 cli('new',n,n,'--color-mode','rgb');authored=[]
 for i,(label,d) in enumerate(layers):
  if i:cli('layer','create',label)
  else:cli('layer','update',cli('state')['active']['layerId'],'--name',label)
  rs=runs(d,n);authored.append(dict(name=label,runs=rs))
  for off in range(0,len(rs),160):
   args=['runs','--frame','0']
   for x,y,w,c in rs[off:off+160]:args+=['--run',f'{x},{y},{w},{c}ff']
   cli(*args)
 cli('save',name+'.aseprite','--output',R/'source'/(name+'.aseprite'));cli('export',name+'.png','--output',R/'exports'/(name+'.png'))
 print(name+' 原生源稿及PNG已保存',flush=True);return dict(name=name,size=[n,n],layers=authored)


def icon(n):
 a={};d={};b={}
 if n==32:
  # Thin back case and square front frame, true transparent opening.
  poly(a,[(3,6),(6,3),(28,3),(25,6)],'green_l');poly(a,[(25,6),(28,3),(28,25),(25,28)],'green_d')
  rect(a,3,6,23,3,'rim');rect(a,3,26,23,3,'shadow');rect(a,3,9,3,17,'steel');rect(a,23,9,3,17,'mid')
  line(a,(3,6),(25,6),'shine');line(a,(3,6),(3,28),'outline');line(a,(25,7),(25,28),'outline');line(a,(3,28),(25,28),'outline');line(a,(4,27),(24,27),'steel')
  # Rear bearing support is narrow and stays behind four visible paddles.
  rect(a,6,16,17,1,'green_d');rect(a,14,9,1,17,'green_d')
  for x,y,w,h,col in [(8,6,5,1,'steel'),(17,7,4,1,'mid'),(3,12,1,5,'rim'),(4,21,2,3,'shadow'),(24,11,1,4,'steel'),(23,22,2,2,'dark'),(9,27,4,1,'mid'),(18,26,3,1,'dark'),(26,8,1,5,'green'),(26,18,1,4,'outline'),(10,4,4,1,'green'),(20,4,3,1,'green_d')]:rect(d,x,y,w,h,col)
  for x,y in [(3,6),(22,6),(3,25),(22,25)]:rect(d,x,y,4,4,'steel');rect(d,x+1,y+1,2,2,'copper');rect(d,x+1,y+1,2,1,'copper_l')
  cx,cy,s=14,17,2.1
 else:
  poly(a,[(1,3),(3,1),(14,1),(12,3)],'green_l');poly(a,[(12,3),(14,1),(14,12),(12,14)],'green_d')
  rect(a,1,3,12,2,'rim');rect(a,1,13,12,2,'shadow');rect(a,1,5,2,8,'steel');rect(a,11,5,2,8,'mid');line(a,(1,3),(12,3),'shine');line(a,(1,14),(12,14),'outline');rect(a,3,8,8,1,'green_d');rect(a,7,5,1,8,'green_d')
  for x,y,w,h,col in [(5,3,2,1,'steel'),(8,4,2,1,'mid'),(1,6,1,3,'rim'),(11,9,1,2,'shadow'),(5,13,2,1,'mid'),(13,5,1,2,'green')]:rect(d,x,y,w,h,col)
  for x,y in [(1,3),(11,3),(1,13),(11,13)]:rect(d,x,y,2,2,'steel');rect(d,x,y,1,1,'copper_l')
  cx,cy,s=6.5,8.5,1.0
 # Each size uses own native pixel coverage; same four swept-blade identity.
 import math
 shape=[[.55,-.65],[3.3,-.55],[3.5,.85],[2.65,1.75],[1.1,1.45],[.55,.6]]
 for k in range(4):
  q=k*math.pi/2;points=[(round(cx-(x*math.cos(q)-y*math.sin(q))*s),round(cy-(x*math.sin(q)+y*math.cos(q))*s))for x,y in shape];poly(b,points,'steel');line(b,points[1],points[2],'rim');line(b,points[2],points[3],'shine');line(b,points[4],points[5],'mid')
 if n==32:
  rect(b,12,15,5,5,'shadow');rect(b,12,15,5,1,'rim');rect(b,13,16,3,3,'copper');rect(b,13,16,3,1,'copper_h');rect(b,15,18,1,1,'copper_d')
 else:
  rect(b,5,7,3,3,'shadow');rect(b,5,7,3,1,'steel');rect(b,6,8,2,2,'copper');rect(b,6,8,2,1,'copper_l')
 return [('01 薄方框与开放后撑',a),('02 钢色明暗与铜紧固点',d),('03 四片同向叶片与铜轮毂',b)]
if __name__=='__main__':
 start=cli('state','--pixel-hash');(R/'review/aseprite-starting-document.json').write_text(json.dumps({'active':start['active']},indent=2)+'\n');files=[create(f'fan-item-{n}',n,icon(n))for n in [32,16]];(R/'source/pixel-designs.json').write_text(json.dumps(dict(palette=P,files=files),ensure_ascii=False,indent=2)+'\n')
