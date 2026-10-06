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

def atlas():
 base={};detail={};copy={};marks={};regions={}
 def region(key,x,y,w,h,colors):
  regions[key]=dict(pixels=[x,y,w,h],uv=[x/4,y/4,(x+w)/4,(y+h)/4])
  for j in range(h):rect(base,x,y+j,w,1,colors[min(len(colors)-1,j*len(colors)//h)])
 region('deck',32,0,24,16,['steel','steel','mid']);region('edge',56,0,8,16,['shine','rim','steel','shadow']);region('copper',32,16,16,16,['copper_h','copper_l','copper','copper_d']);region('housing',48,16,16,16,['green_l','green','green_d','outline']);region('recess',0,32,16,16,['green_d','outline']);region('steel',16,32,16,16,['rim','steel','mid','shadow']);region('underside',32,32,16,16,['mid','shadow','dark']);region('vent',48,32,16,16,['green_d','outline']);region('control',0,48,16,8,['shadow','dark']);region('port',16,48,8,8,['dark','outline']);region('trim',24,48,8,8,['rim','steel','shadow']);region('joint',32,48,16,8,['steel','mid','dark']);region('neutral',48,48,16,16,['outline']);region('base',0,56,48,8,['steel','mid','shadow'])
 for x,y,w,h,col in [(34,2,8,2,'shine'),(45,3,8,2,'steel'),(36,7,6,2,'mid'),(47,9,7,2,'shadow'),(33,12,10,2,'mid'),(50,13,4,1,'shadow'),(57,1,6,1,'shine'),(58,6,4,2,'steel'),(59,11,3,2,'shadow'),(33,17,11,1,'copper_h'),(36,22,6,2,'copper'),(42,26,4,2,'copper_d'),(49,18,5,2,'green'),(55,21,7,2,'green_d'),(51,26,6,2,'outline'),(1,34,7,2,'outline'),(9,40,5,2,'green_d'),(17,34,8,1,'shine'),(21,38,8,2,'mid'),(18,43,6,2,'shadow'),(33,34,8,2,'shadow'),(40,39,7,2,'dark'),(35,44,6,1,'outline'),(2,57,10,1,'rim'),(19,59,9,1,'mid'),(35,61,10,1,'shadow'),(33,49,8,1,'rim'),(40,53,6,1,'dark')]:rect(detail,x,y,w,h,col)
 for xy in list(detail):
  if 32<=xy[0]<56 and 0<=xy[1]<16:del detail[xy]
 # Low-contrast steel plates with native seams and compact material clusters.
 for x,y,w,h,col in [(32,0,24,1,'rim'),(32,15,24,1,'shadow'),(43,1,1,14,'mid'),(32,7,24,1,'mid'),(34,2,3,1,'rim'),(39,4,2,2,'mid'),(46,2,3,1,'rim'),(52,4,2,2,'mid'),(34,10,3,2,'steel'),(39,12,2,1,'shadow'),(46,9,3,2,'steel'),(52,12,2,2,'shadow'),(35,5,2,1,'mid'),(47,5,2,1,'mid'),(36,13,2,1,'steel'),(48,13,2,1,'steel')]:rect(detail,x,y,w,h,col)
 for y in [34,38,42]:rect(detail,50,y,12,2,'outline');rect(detail,51,y,10,1,'shadow')
 rect(marks,1,49,14,1,'steel');rect(marks,1,54,14,1,'outline');rect(marks,6,49,4,5,'outline');rect(marks,7,50,2,3,'rim')
 for x in [2,11]:rect(marks,x,51,3,1,'copper_l');rect(marks,x,52,3,1,'copper_d')
 rect(marks,17,49,6,1,'shadow');rect(marks,19,50,2,1,'mid')
 im=Image.open(R/'references/battery-atlas-32.png').convert('RGBA')
 for y in range(32):
  for x in range(32):
   r,g,b,a=im.getpixel((x,y))
   if a:copy[x,y]=f'#{r:02x}{g:02x}{b:02x}'
 return [('01 共用钢壳铜与绿灰材质',base),('02 明暗面与紧凑像素簇',detail),('03 控制板与接头刻纹',marks),('04 采用电池32px区域原样复用',copy)],regions

def icon(n):
 a={};d={};p={}
 if n==32:
  poly(a,[(3,12),(15,19),(15,27),(3,20)],'green');poly(a,[(15,19),(29,12),(29,21),(15,27)],'green_d');poly(a,[(3,12),(17,5),(29,12),(15,19)],'steel')
  for aa,bb,col in [((3,12),(17,5),'rim'),((17,5),(29,12),'steel'),((3,12),(15,19),'shine'),((15,19),(29,12),'rim'),((3,20),(15,27),'steel'),((15,27),(29,21),'mid'),((3,12),(3,20),'outline'),((29,12),(29,21),'outline')]:line(a,aa,bb,col)
  line(a,(4,14),(14,20),'mid');line(a,(16,20),(28,14),'shadow');line(a,(4,19),(14,25),'shadow');line(a,(16,25),(28,19),'dark')
  for x,y,w,h,col in [(6,11,4,1,'rim'),(12,8,4,1,'mid'),(20,10,4,1,'mid'),(19,14,4,1,'shadow'),(5,15,3,2,'green_l'),(10,21,3,2,'green_d'),(24,16,3,2,'outline'),(19,22,3,1,'green'),(4,13,1,5,'rim'),(27,14,1,5,'mid'),(14,21,1,4,'steel')]:rect(d,x,y,w,h,col)
  for x,y in [(16,5),(4,12),(27,12),(14,18)]:rect(d,x,y,2,1,'copper_h');rect(d,x,y+1,2,1,'copper')
  poly(p,[(13,10),(16,9),(20,11),(16,13)],'rim');poly(p,[(14,10),(16,10),(18,11),(16,12)],'outline');rect(p,15,10,1,1,'copper_l');rect(p,17,11,1,1,'copper')
  line(p,(9,14),(14,17),'shadow');line(p,(9,15),(14,18),'rim');rect(p,11,15,2,1,'copper_l')
  rect(p,7,17,4,5,'rim');rect(p,8,18,2,3,'outline');rect(p,8,19,1,2,'copper_l');rect(p,9,19,1,2,'copper_d');rect(p,21,16,4,5,'steel');rect(p,22,17,2,3,'outline');rect(p,22,18,1,2,'copper');rect(p,23,18,1,2,'copper_l')
 else:
  poly(a,[(1,6),(7,10),(7,14),(1,10)],'green');poly(a,[(7,10),(14,6),(14,10),(7,14)],'green_d');poly(a,[(1,6),(8,3),(14,6),(7,10)],'steel')
  for aa,bb,col in [((1,6),(8,3),'rim'),((8,3),(14,6),'mid'),((1,6),(7,10),'shine'),((7,10),(14,6),'rim'),((1,10),(7,14),'steel'),((7,14),(14,10),'mid'),((1,6),(1,10),'outline'),((14,6),(14,10),'outline')]:line(a,aa,bb,col)
  for x,y,w,h,col in [(4,5,2,1,'mid'),(9,6,2,1,'rim'),(2,8,1,1,'green_l'),(5,11,1,1,'green_d'),(12,8,1,1,'outline'),(9,11,2,1,'green'),(7,11,1,2,'shadow')]:rect(d,x,y,w,h,col)
  for x,y in [(8,3),(2,6),(13,6)]:rect(d,x,y,1,1,'copper_l')
  rect(p,6,5,3,2,'rim');rect(p,7,5,1,1,'outline');rect(p,7,6,1,1,'copper_l');line(p,(4,7),(6,8),'shadow');rect(p,5,8,1,1,'copper_l');rect(p,3,9,3,3,'rim');rect(p,4,10,1,2,'copper');rect(p,10,8,3,3,'steel');rect(p,11,9,1,1,'copper_l');rect(p,11,10,1,1,'outline')
 return [('01 低平台三面轮廓',a),('02 钢框铜角与明暗像素簇',d),('03 三可见接头与前上控制板',p)]

if __name__=='__main__':
 s=cli('state','--pixel-hash');(R/'review/aseprite-starting-document.json').write_text(json.dumps({'active':s['active']},indent=2)+'\n');ls,regs=atlas();files=[create('machine-atlas-64',64,ls)]
 for n in [32,16]:files.append(create(f'machine-body-item-{n}',n,icon(n)))
 (R/'source/pixel-designs.json').write_text(json.dumps(dict(palette=P,files=files),ensure_ascii=False,indent=2)+'\n');(R/'source/uv-regions.json').write_text(json.dumps(dict(atlasSize=[64,64],regions=regs,batteryOriginalArea=[0,0,32,32],batteryUvTransform='old 32px UV / 2; no texture resampling'),indent=2)+'\n')
