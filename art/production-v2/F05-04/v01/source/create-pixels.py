"""Native pixels through Aseprite typed operations; PIL only reads adopted pixels."""
from pathlib import Path
from PIL import Image
import json,subprocess
R=Path(__file__).resolve().parents[1];CTL='/Users/zhou/Desktop/Aseprite Web 本机版/plugins/aseprite-web/scripts/aseprite-web-control'
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




def icon(n,spent=False):
 # Each native size has its own diagonal grid, retained mounting plate, collar and flared exhaust.
 a={};b={};d={}
 if n==32:
  poly(a,[(3,8),(8,3),(12,5),(29,21),(29,26),(25,29),(20,29),(5,14)],'outline')
  poly(a,[(4,8),(8,4),(11,6),(7,12),(4,12)],'steel');line(d,(4,8),(8,4),'rim');rect(d,7,5,2,2,'copper_l');rect(d,4,9,2,2,'copper')
  poly(b,[(9,7),(13,7),(25,19),(25,25),(21,27),(7,13)],'copper');poly(b,[(9,8),(12,8),(23,19),(23,22),(8,12)],'copper_l');poly(b,[(7,12),(21,26),(24,26),(10,11)],'copper_d')
  poly(b,[(13,10),(17,11),(23,17),(21,23),(18,23),(10,15)],'paper')
  if spent:
   poly(b,[(19,13),(23,17),(21,23),(18,23),(16,21),(18,20),(17,18),(20,17),(18,15)],'dark')
   line(d,(16,15),(17,16),'paper_edge');line(d,(14,17),(15,18),'paper_d');line(d,(20,15),(19,16),'paper_h');rect(d,18,20,1,1,'paper_h')
  else:
   poly(b,[(13,11),(16,12),(21,17),(19,20),(12,14)],'paper_h');line(d,(13,16),(17,20),'paper_d');line(d,(16,13),(18,15),'paper_m');rect(d,18,18,2,1,'paper_d')
  # Fine non-flat copper/paper clusters follow the diagonal material structure.
  for x,y,c in [(10,8,'copper_h'),(11,9,'copper'),(8,11,'copper_h'),(12,18,'copper_l'),(13,19,'copper'),(21,18,'copper_l'),(23,20,'copper_h'),(20,24,'copper'),(15,13,'paper_m'),(14,15,'paper_d')]:
   if not spent or (x+y)%2:rect(d,x,y,1,1,c)
  poly(d,[(10,10),(12,9),(15,12),(10,17),(8,15)],'shadow');line(d,(10,10),(12,9),'rim');line(d,(12,10),(14,12),'steel');line(d,(9,14),(10,15),'mid')
  # Same outer copper rim; a visible pale core versus deep empty bore.
  poly(d,[(23,19),(27,20),(29,23),(28,27),(25,29),(21,28),(20,24)],'copper_d');poly(d,[(23,20),(26,21),(28,23),(27,26),(24,28),(22,27),(21,24)],'copper');line(d,(23,20),(26,21),'copper_h');line(d,(26,21),(28,23),'copper_l');line(d,(21,24),(22,27),'copper_l');poly(d,[(23,22),(25,22),(26,24),(25,26),(23,26),(22,24)],'outline' if spent else 'paper_d')
  if spent:rect(d,23,23,2,2,'dark');rect(d,24,23,1,1,'outline')
  else:rect(d,23,23,2,2,'paper');rect(d,23,23,1,1,'paper_h')
 else:
  poly(a,[(1,4),(4,1),(7,3),(14,10),(14,13),(11,15),(8,14),(2,8)],'outline')
  line(a,(1,4),(4,1),'steel');line(d,(1,4),(3,2),'rim');rect(d,3,2,1,1,'copper_l');rect(d,1,5,1,1,'copper')
  poly(b,[(4,4),(7,4),(12,9),(12,12),(9,13),(3,7)],'copper');line(b,(4,4),(10,9),'copper_l');line(b,(3,7),(9,13),'copper_d');rect(d,5,5,1,1,'copper_h');rect(d,4,6,1,1,'copper')
  poly(b,[(7,6),(9,6),(12,9),(10,12),(6,8)],'paper');line(d,(7,6),(10,9),'paper_h');rect(d,8,9,1,1,'paper_d')
  if spent:
   poly(b,[(10,7),(12,9),(10,12),(9,11),(10,10),(8,9),(10,9)],'dark');rect(d,9,8,1,1,'paper_edge');rect(d,9,9,1,1,'paper_h')
  else:rect(d,9,8,1,1,'paper_m');rect(d,9,10,1,1,'paper_d')
  line(d,(5,5),(3,7),'shadow');rect(d,5,5,1,1,'rim');rect(d,4,6,1,1,'steel')
  poly(d,[(11,9),(14,10),(15,12),(13,14),(10,14),(9,12)],'copper_d');line(d,(11,9),(14,10),'copper_l');rect(d,11,9,1,1,'copper_h');rect(d,14,11,1,2,'copper');rect(d,10,13,2,1,'copper')
  rect(d,11,11,2,2,'outline' if spent else 'paper');rect(d,12,11,1,1,'dark' if spent else 'paper_h')
 if n==32:
  paperColors={P[k]for k in ['paper','paper_h','paper_m','paper_d']}
  for x,y in [(12,14),(13,14),(15,15),(16,15),(17,16),(18,17),(18,18),(17,19),(16,18),(14,16),(15,17),(16,20)]:
   if b.get((x,y))in paperColors:rect(d,x,y,1,1,'paper_m' if (x+y)%2 else 'paper_d')
 return [('01 共用安装板与铜壳轮廓',a),('02 完整或破损纸封套',b),('03 钢箍铜喷口与装填状态',d)]

def atlas():
 machine=Image.open(R/'references/shared-machine-atlas-64.png').convert('RGBA');base={};paper={}
 for y in range(64):
  for x in range(64):
   c=machine.getpixel((x,y))
   if c[3]:base[x,y]='#'+''.join(f'{v:02x}'for v in c[:3])
 # Small hand-designed 16px paper material, authored on the extension; preserve all original 64px texels.
 rows=['aaaccaaddacaaaaa','aabccaaadaacccaa','aacaaabccaacaaaa','ddcaacccaaaaacba','aaaccaaabbaacaaa','acaaadaccaaaddaa','aaabcaaaccaacaaa','cacaaaadacaaaacc','aaddacaaaacbaaaa','acaaaccaabccaaaa','aaaacccaacaaadaa','aacaaabaaaccaaaa','ddacaaaacccaaaba','aacccaaadacaaaaa','aabccaaaacaaacca','cacaaaacaaaddaaa']
 pal={'a':'paper','b':'paper_edge','c':'paper_m','d':'paper_h'}
 for y,row in enumerate(rows):
  assert len(row)==16
  for x,c in enumerate(row):paper[64+x,y]=P[pal[c]]
 return [('01 完整复用机械64图集',base),('02 原生16纸封套',paper)]
if __name__=='__main__':
 start=cli('state','--pixel-hash');(R/'review/aseprite-start.json').write_text(json.dumps(dict(active=start['active']),indent=2))
 files=[create('rocket-atlas-128x64',128,atlas(),64)]+[create(f'{state}-item-{n}',n,icon(n,state=='spent-rocket'))for state in ['rocket','spent-rocket']for n in [32,16]]
 (R/'source/pixel-designs.json').write_text(json.dumps(dict(palette=P,files=files),ensure_ascii=False,indent=2)+'\n')
