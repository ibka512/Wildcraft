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
  poly(a,[(2,9),(9,4),(26,4),(31,9),(30,24),(26,28),(6,27),(1,22)],'outline')
  poly(b,[(3,10),(9,6),(26,6),(29,9),(24,13),(7,13)],'cream_l');line(d,(9,6),(25,6),'paper_h');line(d,(5,10),(11,7),'cream');rect(d,11,8,3,1,'cream');rect(d,23,8,2,1,'paper');rect(d,8,10,4,1,'paper_h');rect(d,25,10,2,1,'cream_m')
  poly(b,[(2,12),(7,14),(7,25),(5,24),(2,21)],'cream_d');line(d,(3,13),(6,15),'cream_m');rect(d,3,17,2,2,'cream_m');rect(d,4,21,2,1,'wood_l')
  poly(b,[(8,14),(24,14),(29,10),(29,23),(25,26),(9,25),(7,22),(7,15)],'cream');line(d,(9,14),(23,14),'cream_l');rect(d,10,16,4,2,'cream_l');rect(d,21,15,3,2,'cream_l');rect(d,26,14,2,2,'paper');rect(d,10,20,3,2,'cream_m');rect(d,23,20,3,2,'cream_m');rect(d,26,22,2,1,'cream_d');rect(d,13,23,2,1,'cream_l');rect(d,20,23,2,1,'cream_m');line(d,(8,19),(27,19),'cream_d');rect(d,10,18,2,1,'cream_l');rect(d,25,18,2,1,'cream_l')
  line(d,(9,15),(9,23),'cream_m');line(d,(27,13),(27,21),'cream_m');rect(d,10,24,2,1,'cream_d');rect(d,25,23,1,1,'cream_d')
  poly(b,[(14,6),(18,6),(23,10),(20,14),(20,26),(16,26),(16,14),(19,11)],'copper');line(d,(14,6),(18,6),'copper_l');line(d,(15,7),(19,11),'copper_h');rect(d,16,15,1,4,'copper_l');rect(d,19,14,1,9,'copper_d');rect(d,17,24,2,2,'copper_d');rect(d,17,16,2,1,'copper_d')
  rect(b,15,18,7,6,'outline');rect(d,16,19,5,4,'steel');rect(d,17,20,3,2,'copper_d');line(d,(16,19),(20,19),'rim');rect(d,16,20,1,2,'rim');line(d,(17,21),(19,21),'mid');rect(d,20,22,1,1,'shadow');rect(d,17,14,1,1,'steel')
 else:
  poly(a,[(1,5),(4,2),(12,2),(15,5),(15,11),(12,14),(3,13),(0,10)],'outline')
  poly(b,[(2,5),(5,3),(12,3),(14,5),(11,7),(4,7)],'cream_l');line(d,(5,3),(11,3),'paper_h');rect(d,4,5,2,1,'cream');rect(d,11,5,2,1,'cream')
  poly(b,[(1,6),(4,8),(4,12),(2,11),(1,9)],'cream_d');rect(d,2,8,1,2,'cream_m')
  poly(b,[(4,8),(11,8),(14,6),(14,10),(11,12),(4,12)],'cream');rect(d,5,8,2,1,'cream_l');rect(d,11,7,2,2,'cream_l');rect(d,5,11,2,1,'cream_m');rect(d,12,10,1,1,'cream_d');line(d,(4,10),(13,10),'cream_d');rect(d,5,9,1,1,'cream_m')
  poly(b,[(7,3),(9,3),(11,5),(10,7),(10,12),(8,12),(8,7),(9,5)],'copper');line(d,(7,3),(9,5),'copper_l');rect(d,8,8,1,2,'copper_l');rect(d,9,7,1,3,'copper_d');rect(b,7,9,4,3,'outline');rect(d,8,9,3,1,'steel');rect(d,7,10,1,1,'rim');rect(d,8,10,2,1,'copper_d');rect(d,8,11,2,1,'steel')
 return [('01 浮体轮廓与侧面厚度',a),('02 浅色浮体与中央束带',b),('03 钢扣缝线与像素明暗',d)]
if __name__=='__main__':
 start=cli('state','--pixel-hash');(R/'review/aseprite-start.json').write_text(json.dumps(dict(active=start['active']),indent=2));files=[create(f'buoyancy-item-{n}',n,icon(n))for n in [32,16]];(R/'source/pixel-designs.json').write_text(json.dumps(dict(palette=P,files=files),ensure_ascii=False,indent=2)+'\n')
