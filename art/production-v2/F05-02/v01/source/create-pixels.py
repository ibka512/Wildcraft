"""Native pixels through Aseprite typed operations; PIL only reads adopted pixels."""
from pathlib import Path
from PIL import Image
import json,subprocess
R=Path(__file__).resolve().parents[1];CTL='/Users/zhou/Desktop/Aseprite Web 本机版/plugins/aseprite-web/scripts/aseprite-web-control'
P={'outline':'#293632','dark':'#354047','shadow':'#47545b','mid':'#5d6e75','steel':'#83949a','rim':'#b6c3c3','shine':'#d7ddce','green_d':'#3e4f49','green':'#596e64','green_l':'#7d9186','copper_d':'#6e3c28','copper':'#a95b3a','copper_l':'#db8a55','copper_h':'#f2b879','cream':'#ddccb0','cream_l':'#f1e1c5','cream_m':'#c7b496','cream_d':'#ac997b','olive':'#727756','olive_l':'#909674','olive_d':'#535d40','wood':'#936636','wood_l':'#be9154','wood_d':'#634528'}
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
 # Hand authored grid for each resolution: straight rigid spars, two tapered cloth wings, central keel.
 a={};b={};d={}
 if n==32:
  left=[(2,9),(14,5),(14,20),(5,20)];right=[(17,5),(29,9),(26,20),(17,20)]
  for q in [left,right]:
   poly(a,q,'outline');poly(b,[(x+(1 if x<15 else -1),y+1 if y<10 else y-1)for x,y in q],'cream')
  poly(b,[(7,9),(10,8),(10,19),(7,19)],'olive');poly(b,[(21,8),(24,9),(24,19),(21,19)],'olive')
  for y in range(9,19):
   for x in range(3,29):
    c=b.get((x,y))
    if c=='#ddccb0':
     if (x+y*3)%11==0:b[x,y]=P['cream_m']
     elif (x*3+y)%13==0:b[x,y]=P['cream_l']
    elif c=='#727756' and (x+y)%4==0:b[x,y]=P['olive_l'] if y<14 else P['olive_d']
  for aa,bb in [((2,9),(14,5)),((17,5),(29,9)),((5,20),(14,20)),((17,20),(26,20)),((2,9),(5,20)),((29,9),(26,20))]:
   line(d,aa,bb,'wood');line(d,(aa[0],aa[1]-1),(bb[0],bb[1]-1),'wood_l')
  line(d,(11,7),(11,19),'wood_d');line(d,(20,7),(20,19),'wood_d')
  for x,y in [(4,11),(6,14),(8,16),(10,18),(27,11),(25,14),(23,16),(21,18)]:rect(d,x,y,1,1,'cream_d')
  rect(d,14,3,4,20,'outline');rect(d,14,3,3,19,'wood_d');rect(d,14,3,2,18,'wood');rect(d,14,3,1,17,'wood_l');rect(d,15,5,1,3,'cream_d');rect(d,15,12,1,4,'wood_l')
  rect(d,12,19,8,6,'outline');rect(d,12,19,7,5,'shadow');rect(d,12,19,7,1,'rim');rect(d,12,20,1,4,'steel');rect(d,18,20,1,4,'steel');rect(d,13,20,1,3,'copper_l');rect(d,17,20,1,3,'copper');rect(d,15,21,1,2,'dark');rect(d,14,24,4,1,'mid')
  for x,y in [(2,9),(28,9),(5,19),(25,19)]:rect(d,x,y,2,2,'copper');rect(d,x,y,1,1,'copper_h')
 else:
  # 16px is a separate 13px wingspan composition, not a scaled 32px image.
  poly(a,[(1,5),(6,3),(6,11),(2,11)],'outline');poly(a,[(9,3),(15,5),(13,11),(9,11)],'outline')
  poly(b,[(2,5),(6,4),(6,10),(3,10)],'cream');poly(b,[(9,4),(14,5),(12,10),(9,10)],'cream')
  rect(b,4,5,1,5,'olive');rect(b,10,5,1,5,'olive');rect(b,4,5,1,1,'olive_l');rect(b,10,6,1,1,'olive_l');rect(b,4,9,1,1,'olive_d');rect(b,10,9,1,1,'olive_d')
  for x,y,c in [(2,6,'cream_l'),(3,7,'cream_m'),(5,5,'cream_l'),(5,8,'cream_d'),(3,9,'cream_m'),(12,6,'cream_l'),(11,7,'cream_m'),(12,9,'cream_m'),(9,8,'cream_d')]:rect(b,x,y,1,1,c)
  line(d,(1,5),(6,3),'wood_l');line(d,(9,3),(14,5),'wood_l');line(d,(2,10),(6,10),'wood');line(d,(9,10),(13,10),'wood');line(d,(1,6),(2,10),'wood_d');line(d,(14,6),(13,10),'wood_d')
  rect(d,6,3,3,9,'outline');rect(d,6,3,2,8,'wood');rect(d,6,3,1,7,'wood_l');rect(d,7,5,1,2,'wood_d');rect(d,7,8,1,1,'wood_d')
  rect(d,5,10,5,3,'shadow');rect(d,5,10,5,1,'rim');rect(d,5,11,1,2,'steel');rect(d,9,11,1,2,'steel');rect(d,6,11,1,1,'copper_l');rect(d,8,11,1,1,'copper');rect(d,7,11,1,1,'dark');rect(d,6,12,3,1,'mid')
  for x,y in [(1,5),(14,5),(2,10),(12,10)]:rect(d,x,y,1,1,'copper_l')
 return [('01 刚性双翼轮廓',a),('02 亚麻与橄榄布面像素簇',b),('03 木骨架与根部铜接口',d)]

def atlas():
 machine=Image.open(R/'references/machine-atlas-64.png').convert('RGBA');cloth=Image.open(R/'references/paraglider-atlas-128x64.png').convert('RGBA');base={};ext={}
 for y in range(64):
  for x in range(64):
   c=machine.getpixel((x,y))
   if c[3]:base[x,y]='#'+''.join(f'{v:02x}'for v in c[:3])
 # Byte-exact native patch reuse; no resizing, interpolation, recoloring, or Python image editing.
 for sx,sy,dx,dy in [(0,0,64,0),(32,0,96,0),(0,32,64,32),(64,32,96,32)]:
  for y in range(32):
   for x in range(32):
    c=cloth.getpixel((sx+x,sy+y))
    if c[3]:ext[dx+x,dy+y]='#'+''.join(f'{v:02x}'for v in c[:3])
 return [('01 已采用机械64图集逐像素复用',base),('02 已采用滑翔伞布木32区域逐像素复用',ext)]
if __name__=='__main__':
 start=cli('state','--pixel-hash');(R/'review/aseprite-start.json').write_text(json.dumps(dict(active=start['active']),indent=2))
 files=[create('wing-atlas-128x64',128,atlas(),64)]+[create(f'wing-item-{n}',n,icon(n))for n in [32,16]]
 (R/'source/pixel-designs.json').write_text(json.dumps(dict(palette=P,files=files),ensure_ascii=False,indent=2)+'\n')
