"""Author exact native pixels through the local Aseprite Web typed CLI."""
from pathlib import Path
import json,subprocess,hashlib,uuid
R=Path(__file__).resolve().parents[1];CLI='/Volumes/固态/开发/AI/工具/Aseprite Web 本机版/plugins/aseprite-web/scripts/aseprite-web-control'
def command(*args):
 p=subprocess.run([CLI,'cli',*map(str,args)],capture_output=True,text=True,check=True);d=json.loads(p.stdout)
 if d.get('ok') is False:raise RuntimeError(d)
 return d
state=command('state','--pixel-hash');(R/'review/aseprite-starting-state.json').write_text(json.dumps(state,ensure_ascii=False,indent=2)+'\n');new=command('new',64,64,'--color-mode','rgb');doc=next(d for d in new['state']['documents']if d['active'])
PALETTE={'outline':'#243432ff','shade':'#63877fff','body':'#9cbbb0ff','light':'#e1e9d1ff','blueShade':'#47788fff','blue':'#80b7ccff','blueLight':'#d5e8eaff'}
icons=[]
def polygon(pts):
 out=set()
 for y in range(16):
  for x in range(16):
   u,v=x+.5,y+.5;inside=False
   for (a,b),(c,d)in zip(pts,pts[1:]+pts[:1]):
    if (b>v)!=(d>v) and u<(c-a)*(v-b)/(d-b)+a:inside=not inside
   if inside:out.add((x,y))
 return out
def line(points):
 out=set()
 for (x,y),(a,b)in zip(points,points[1:]):
  n=max(abs(a-x),abs(b-y))
  for i in range(n+1):out.add((round(x+(a-x)*i/max(n,1)),round(y+(b-y)*i/max(n,1))))
 return out
def rot(s,n):
 for _ in range(n):s={(15-y,x)for x,y in s}
 return s
def add(name,mask,blue=False):
 outline={(a,b)for x,y in mask for a,b in [(x,y),(x-1,y),(x+1,y),(x,y-1),(x,y+1)]if 0<=a<16 and 0<=b<16};body={}
 for x,y in mask:
  # A light upper/left edge and darker lower/right edge give small forms depth.
  key=('blueLight'if blue else'light')if (x,y-1)not in mask or(x-1,y)not in mask else('blueShade'if blue else'shade')if (x,y+1)not in mask or(x+1,y)not in mask else('blue'if blue else'body')
  body[x,y]=PALETTE[key]
 icons.append({'id':name,'mask':mask,'outline':outline,'body':body})
up=polygon([(7.5,2),(12,7),(9,7),(9,13),(6,13),(6,7),(3,7)])
ne=polygon([(6,3),(13,3),(13,10),(10,7),(5,12),(3,10),(8,5)])
for n,name in enumerate(['forward','front_right','right','back_right','backward','back_left','left','front_left']):add('wind_'+name,rot(up if n%2==0 else ne,n//2))
for count,name in [(1,'light'),(2,'moderate'),(3,'strong')]:
 mask=set()
 ys={1:[7],2:[5,10],3:[5,9,13]}[count]
 for j,y in enumerate(ys):
  if j%2==0:mask|=line([(3,y),(11,y),(13,y-1),(13,y-2),(12,y-3),(10,y-3)])
  else:mask|=line([(2,y),(11,y),(12,y+1),(11,y+2),(9,y+2)])
 add('strength_'+name,mask)
shelter=line([(2,7),(7,2),(8,2),(13,7)])|line([(3,7),(12,7)])|line([(4,8),(4,12),(11,12),(11,8)])
add('sheltered',shelter)
drop=polygon([(8,2),(12,8),(12,11),(10,14),(6,14),(4,11),(4,8)])
add('wet',drop,True)
outlineLayer=doc['layers'][0]['id'];command('layer','update',outlineLayer,'--name','01 · 轮廓')
bodyResult=command('layer','create','02 · 主色与暗部');bodyLayer=bodyResult['state']['active']['layerId']
lightResult=command('layer','create','03 · 亮部');lightLayer=lightResult['state']['active']['layerId']
layers={outlineLayer:{},bodyLayer:{},lightLayer:{}};entries=[]
for idx,icon in enumerate(icons):
 ox=idx%4*16;oy=idx//4*16;entries.append({'id':icon['id'],'x':ox,'y':oy,'width':16,'height':16})
 for x,y in icon['outline']:layers[outlineLayer][ox+x,oy+y]=PALETTE['outline']
 for (x,y),color in icon['body'].items():layers[lightLayer if color in [PALETTE['light'],PALETTE['blueLight']]else bodyLayer][ox+x,oy+y]=color
for lid,pixels in layers.items():
 args=['runs','--layer',lid,'--frame',0]
 for y in range(64):
  x=0
  while x<64:
   if (x,y)not in pixels:x+=1;continue
   color=pixels[x,y];start=x
   while x<64 and pixels.get((x,y))==color:x+=1
   args+=['--run',f'{start},{y},{x-start},{color}']
 command(*args)
command('save','wind-weather-atlas.aseprite','--output',R/'source/wind-weather-atlas.aseprite')
command('pixels','--frame',0,'--rect','0,0,64,64','--output',R/'review/aseprite-pixels.json')
# Preview export uses a fresh managed path; inspect state before one retry if needed.
try:command('preview','--frame',0,'--output',R/'exports/wind-weather-atlas.png')
except subprocess.CalledProcessError:
 command('state','--pixel-hash')
 command('preview','--frame',0,'--output',R/'exports/wind-weather-atlas.png')
command('open',R/'source/wind-weather-atlas.aseprite');command('pixels','--frame',0,'--rect','0,0,64,64','--output',R/'review/reopened-source-pixels.json');command('open',R/'exports/wind-weather-atlas.png');command('pixels','--frame',0,'--rect','0,0,64,64','--output',R/'review/reopened-png-pixels.json');command('open',R/'source/wind-weather-atlas.aseprite');final=command('state','--pixel-hash');(R/'review/aseprite-final-state.json').write_text(json.dumps(final,ensure_ascii=False,indent=2)+'\n')
expected={}
for pixels in layers.values():expected.update(pixels)
(R/'source/pixel-spec.json').write_text(json.dumps({'width':64,'height':64,'palette':PALETTE,'entries':entries,'layers':[{'name':name,'pixels':[[x,y,col]for (x,y),col in layers[lid].items()]}for name,lid in [('轮廓',outlineLayer),('主色与暗部',bodyLayer),('亮部',lightLayer)]],'composite':[[x,y,col]for (x,y),col in expected.items()]},ensure_ascii=False,indent=2)+'\n')
(R/'exports/atlas-map.json').write_text(json.dumps({'assetIds':['F07-01','F07-02'],'nativeIconSize':16,'atlasSize':[64,64],'entries':entries,'directionOrderClockwise':['forward','front_right','right','back_right','backward','back_left','left','front_left'],'directionMeaning':'Wind flows toward arrow, relative to player view; not wind source','strengthThresholdsBlocksPerTick':[.012,.024],'wetThreshold':.2,'shelterReplacesDirectionAndStrength':True,'wetMayCoexist':True,'unusedCells':3,'filter':'nearest','mipmaps':False,'palette':PALETTE},ensure_ascii=False,indent=2)+'\n');print('Created 13 native 16px components; layered source and PNG saved/reopened in Aseprite Web.')
