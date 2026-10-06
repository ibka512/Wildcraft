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
  poly(a,[(8,3),(22,1),(28,5),(29,27),(25,31),(9,30),(4,25),(4,7)],'outline')
  poly(b,[(5,7),(10,5),(11,28),(8,28),(5,25)],'green_d');line(d,(5,8),(5,23),'green_l');rect(d,6,10,2,3,'green');rect(d,6,21,2,2,'green')
  poly(b,[(11,5),(25,5),(28,8),(28,26),(25,29),(11,29),(9,26),(9,8)],'green');rect(d,12,8,10,4,'green_d');rect(d,12,9,2,2,'green_l');rect(d,20,11,4,2,'green_d');rect(d,11,23,11,3,'green_d');rect(d,13,24,2,2,'green');rect(d,23,22,2,3,'green_l');rect(d,16,25,3,1,'green_l')
  poly(b,[(8,4),(21,2),(26,5),(24,7),(10,7),(5,6)],'steel');line(d,(8,4),(21,2),'rim');line(d,(10,6),(23,6),'mid');rect(d,14,4,5,1,'shadow');rect(d,15,3,2,1,'steel');rect(d,22,4,2,1,'rim')
  poly(b,[(9,27),(25,27),(27,28),(24,30),(10,30),(6,27)],'steel');line(d,(10,28),(24,28),'rim');rect(d,12,29,4,1,'mid');rect(d,20,29,2,1,'shadow')
  for x,y in [(9,8),(25,8),(9,24),(25,24)]:rect(b,x,y,3,3,'steel');rect(d,x,y,1,2,'rim');rect(d,x+1,y+1,1,1,'copper_l')
  rect(b,5,14,4,5,'copper_d');rect(d,5,14,4,1,'copper_l');rect(d,6,16,1,2,'copper');rect(b,10,14,4,5,'copper');rect(b,24,14,4,5,'copper');rect(d,10,14,4,1,'copper_h');rect(d,24,14,4,1,'copper_l');rect(d,11,17,2,1,'copper_d');rect(d,26,16,1,2,'copper_d')
  rect(b,14,12,10,10,'outline');rect(b,15,13,8,8,'green_l');rect(d,15,13,8,1,'green_d');rect(d,15,14,2,2,'green');rect(d,21,19,2,2,'green');rect(d,16,17,6,1,'shine');rect(d,19,16,1,3,'green_d');rect(d,18,18,1,1,'green')
 else:
  poly(a,[(4,1),(10,0),(14,3),(14,13),(12,15),(4,15),(1,12),(1,4)],'outline')
  poly(b,[(2,4),(5,3),(5,13),(3,13),(2,11)],'green_d');rect(d,2,5,1,5,'green');poly(b,[(5,3),(12,3),(13,5),(13,12),(11,14),(5,14),(4,12),(4,5)],'green');rect(d,6,4,5,2,'green_d');rect(d,7,5,1,1,'green_l');rect(d,6,11,4,2,'green_d');rect(d,10,12,1,1,'green_l')
  line(b,(4,2),(10,1),'steel');line(d,(4,2),(9,1),'rim');line(d,(6,3),(11,3),'mid');rect(d,11,2,1,1,'steel');line(b,(4,13),(11,14),'steel');line(d,(5,13),(10,13),'rim')
  rect(b,2,7,3,3,'copper_d');rect(d,2,7,3,1,'copper_l');rect(b,5,7,1,3,'copper');rect(b,12,7,1,3,'copper');rect(d,12,7,1,1,'copper_h')
  rect(b,6,6,6,5,'outline');rect(d,7,7,4,3,'green_l');rect(d,7,8,4,1,'shine');rect(d,9,7,1,3,'green_d');rect(d,7,9,1,1,'green')
  for x,y in [(4,4),(12,4),(4,11),(12,11)]:rect(d,x,y,1,1,'steel')
  rect(d,5,4,1,1,'copper_l');rect(d,11,12,1,1,'copper_l')
 return [('01 倒角壳体轮廓与厚度',a),('02 钢端盖铜腰带与方形灯框',b),('03 水平标记紧固件及像素明暗',d)]
if __name__=='__main__':
 start=cli('state','--pixel-hash');(R/'review/aseprite-start.json').write_text(json.dumps(dict(active=start['active']),indent=2));files=[create(f'stabilizer-item-{n}',n,icon(n))for n in [32,16]];(R/'source/pixel-designs.json').write_text(json.dumps(dict(palette=P,files=files),ensure_ascii=False,indent=2)+'\n')
