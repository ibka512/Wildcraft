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
  # Vertical profile plus a three-quarter square top; coils never merge into a solid fill.
  poly(a,[(3,23),(15,19),(28,23),(28,27),(16,31),(3,27)],'outline');poly(a,[(4,23),(15,20),(27,23),(16,27)],'steel');poly(a,[(4,24),(16,28),(16,30),(4,26)],'shadow');poly(a,[(16,28),(27,24),(27,26),(16,30)],'mid')
  rect(b,15,10,3,13,'dark');rect(b,15,11,1,11,'steel');rect(b,16,12,1,10,'mid')
  # Three staggered U-fronts with rear diagonal continuations imply one continuous helix.
  for y in [12,16,20]:
   line(b,(9,y),(15,y-2),'shadow');line(b,(15,y-2),(23,y),'mid');line(b,(8,y),(9,y+2),'steel');line(b,(9,y+2),(17,y+4),'mid');line(b,(17,y+4),(23,y+2),'steel');line(b,(23,y+2),(23,y),'shadow')
   line(d,(9,y+1),(16,y+3),'rim');line(d,(17,y+3),(22,y+1),'steel');rect(d,12,y+2,2,1,'steel');rect(d,20,y+1,1,1,'rim')
  poly(a,[(3,6),(15,1),(28,6),(28,10),(16,15),(3,10)],'outline');poly(b,[(4,6),(15,2),(27,6),(16,11)],'rim');poly(b,[(4,7),(16,12),(16,14),(4,9)],'mid');poly(b,[(16,12),(27,7),(27,9),(16,14)],'shadow')
  poly(d,[(8,6),(15,4),(23,6),(16,9)],'green');line(d,(8,6),(15,4),'green_l');line(d,(15,4),(23,6),'green_d');rect(d,13,6,2,1,'green_d');rect(d,17,7,2,1,'green_l');rect(d,19,6,1,1,'green_d')
  for x,y in [(5,6),(15,2),(25,6),(16,10),(5,23),(25,23),(16,27)]:rect(d,x,y,2,1,'copper_l');rect(d,x,y,1,1,'copper_h')
  line(d,(5,8),(10,10),'steel');line(d,(20,11),(24,9),'mid');line(d,(5,25),(10,27),'steel');line(d,(19,28),(23,26),'shadow');rect(d,10,24,2,1,'mid');rect(d,21,24,2,1,'rim')
 else:
  # Independent 16-pixel silhouette, three separated steel zig-zags.
  poly(a,[(1,11),(7,9),(14,11),(14,13),(8,15),(1,13)],'outline');line(b,(2,11),(7,10),'steel');line(b,(7,10),(13,11),'mid');line(b,(2,12),(8,14),'shadow');line(b,(8,14),(13,12),'mid')
  rect(b,7,5,2,7,'dark');rect(d,7,6,1,5,'steel')
  for y in [5,7,9]:line(b,(4,y),(5,y+1),'steel');line(b,(5,y+1),(8,y+2),'mid');line(b,(8,y+2),(11,y),'steel');rect(d,5,y+1,1,1,'rim');rect(d,10,y+1,1,1,'rim')
  poly(a,[(1,3),(7,0),(14,3),(14,5),(8,7),(1,5)],'outline');poly(b,[(2,3),(7,1),(13,3),(8,5)],'steel');line(b,(2,4),(8,6),'mid');line(b,(8,6),(13,4),'shadow');poly(d,[(4,3),(7,2),(11,3),(8,4)],'green');rect(d,6,2,2,1,'green_l');rect(d,9,3,1,1,'green_d')
  for x,y in [(2,3),(7,1),(12,3),(8,5),(2,11),(12,11),(8,13)]:rect(d,x,y,1,1,'copper_l')
  rect(d,7,1,1,1,'copper_h');rect(d,3,4,1,1,'rim');rect(d,11,4,1,1,'mid');rect(d,4,12,1,1,'steel');rect(d,10,12,1,1,'steel')
 return [('01 底座与方形推板轮廓',a),('02 连续钢制弹簧与导向杆',b),('03 绿垫铜钉与像素明暗',d)]
if __name__=='__main__':
 start=cli('state','--pixel-hash');(R/'review/aseprite-start.json').write_text(json.dumps(dict(active=start['active']),indent=2));files=[create(f'spring-item-{n}',n,icon(n))for n in [32,16]];(R/'source/pixel-designs.json').write_text(json.dumps(dict(palette=P,files=files),ensure_ascii=False,indent=2)+'\n')
