"""Author exact pixel layers and draw through the authorized Aseprite Web CLI.
No generated image sampling, resizing, or raster image editing is performed here.
"""
from pathlib import Path
import subprocess,json,sys
ROOT=Path(__file__).resolve().parents[1]
CTL='/Users/zhou/Desktop/Aseprite Web 本机版/plugins/aseprite-web/scripts/aseprite-web-control'
WOOD={'o':'#352218','e':'#4c3020','d':'#634027','c':'#7c502f','b':'#98673c','a':'#b68149','h':'#d4a15e','g':'#a97642','k':'#e5b977'}
NAMES={'vegetable':'普通蔬菜','warming':'保暖','cooling':'耐热','recovery':'精力恢复'}
def cli(*a):
 p=subprocess.run([CTL,'cli',*map(str,a)],capture_output=True,text=True,timeout=55)
 if p.returncode:raise RuntimeError(' '.join(map(str,a[:3]))+': '+p.stderr[-1000:]+p.stdout[-500:])
 return json.loads(p.stdout)
def span(layer,x,y,w,c):
 for i in range(w):layer[(x+i,y)]=c
def stamp(layer,x,y,rows,colors):
 for j,row in enumerate(rows):
  for i,v in enumerate(row):
   if v!='.':layer[(x+i,y+j)]=colors[v]
def write(layer,size):
 data=[]
 for y in range(size):
  x=0
  while x<size:
   c=layer.get((x,y))
   if c is None:x+=1;continue
   w=1
   while x+w<size and layer.get((x+w,y))==c:w+=1
   data.append([x,y,w,c]);x+=w
 for off in range(0,len(data),180):
  args=['runs','--frame','0']
  for x,y,w,c in data[off:off+180]:args+=['--run',f'{x},{y},{w},{c}ff']
  cli(*args)
 return data

def bowl(n):
 L={};F={}
 if n==32:
  profile=[(10,12),(7,18),(5,22),(4,24),(3,26),(2,28),(2,28),(2,28),(2,28),(3,26),(3,26),(4,24),(5,22),(6,20),(7,18),(9,14),(10,12)]
  for j,(x,w) in enumerate(profile):
   y=j+10;span(L,x,y,w,WOOD['o'])
   if j:span(L,x+1,y,w-2,WOOD['a' if j<5 else 'b' if j<12 else 'c' if j<15 else 'd'])
   if 2<j<17:
    span(L,x+1,y,2,WOOD['g' if j<11 else 'c']);span(L,x+w-4,y,3,WOOD['d' if j<13 else 'e'])
  for x,y,w,key in [(11,10,10,'g'),(9,11,9,'h'),(8,12,4,'k'),(5,13,3,'h'),(4,14,2,'h'),(3,15,2,'a'),(3,16,2,'b'),(22,12,3,'b'),(25,13,2,'c'),(26,14,2,'d')]:span(L,x,y,w,WOOD[key])
  # Front rim curve is above food, body wood clusters below it.
  for x,y,w,key in [(4,17,2,'h'),(26,17,2,'b'),(5,18,3,'a'),(24,18,3,'c'),(6,19,4,'h'),(22,19,4,'b'),(9,20,14,'a'),(9,21,13,'c'),(11,20,7,'h'),(20,20,3,'b'),(4,20,3,'b'),(5,21,4,'a'),(7,22,6,'b'),(13,22,7,'g'),(20,22,3,'c'),(6,23,4,'g'),(10,23,6,'c'),(17,23,6,'b'),(8,24,5,'b'),(14,24,7,'c'),(9,25,3,'g'),(12,25,5,'c'),(19,25,4,'d'),(11,26,4,'d'),(16,26,5,'e')]:span(F,x,y,w,WOOD[key])
 else:
  profile=[(5,6),(3,10),(2,12),(1,14),(1,14),(1,14),(2,12),(3,10),(4,8),(5,6)]
  for j,(x,w) in enumerate(profile):
   y=j+5;span(L,x,y,w,WOOD['o'])
   if j:span(L,x+1,y,w-2,WOOD['a' if j<4 else 'b' if j<7 else 'c'])
   if j>1:span(L,x+w-3,y,2,WOOD['d' if j<7 else 'e'])
  for x,y,w,key in [(6,5,4,'g'),(5,6,5,'h'),(3,7,2,'a'),(2,8,1,'h'),(11,6,1,'b'),(12,7,1,'c')]:span(L,x,y,w,WOOD[key])
  for x,y,w,key in [(2,9,1,'h'),(13,9,1,'b'),(3,10,2,'h'),(11,10,2,'b'),(5,11,6,'a'),(5,11,3,'h'),(10,11,2,'b'),(4,12,2,'b'),(6,12,3,'g'),(9,12,2,'c'),(5,13,2,'b'),(7,13,4,'d')]:span(F,x,y,w,WOOD[key])
 return L,F

def food(kind,n):
 L={};steam={}
 pal={
 'vegetable':['#9c571e','#d28b2d','#ebac40','#f7c564'],
 'warming':['#6c2035','#942a46','#b73750','#ce4b57'],
 'cooling':['#8c4637','#b3694c','#d49667','#e7b488'],
 'recovery':['#916234','#ad7c47','#c49a60','#dec188']
 }[kind]
 mask=[(10,12),(7,18),(6,20),(5,22),(5,22),(6,20),(7,18),(10,12)] if n==32 else [(5,6),(4,8),(3,10),(3,10),(4,8)]
 for j,(x,w) in enumerate(mask):
  y=j+(12 if n==32 else 6);span(L,x,y,w,pal[min(3,j*4//len(mask))]);span(L,x+w-2,y,2,pal[0])
 # Hand-authored liquid reflection clusters; no random texture.
 if n==32:
  for x,y,w,v in [(10,13,3,3),(22,14,2,2),(7,17,3,2),(14,18,4,3),(20,17,2,1),(12,19,3,2)]:span(L,x,y,w,pal[v])
 else:
  for x,y,w,v in [(4,8,2,3),(8,10,2,2)]:span(L,x,y,w,pal[v])
 cream={'H':'#fff0ba','M':'#ead190','L':'#c5a361','D':'#997445'}
 carrot={'H':'#ffd079','M':'#f69c36','L':'#d96722','D':'#9d3f19'}
 beet={'H':'#f38e8c','M':'#d65771','L':'#a82950','D':'#771d39'}
 if kind in ('vegetable','warming'):
  chunks=carrot if kind=='vegetable' else beet
  if n==32:
   for x,y,pattern in [(8,12,['.HH.','HMMM','MMLD','.LDD']),(19,15,['.HM.','HMMM','MMLD','.DDD']),(12,16,['.HH.','HMM.','MMLD','.DD.'])]:stamp(L,x,y,pattern,chunks)
   for x,y,pattern in [(17,11,['.HH.','HHMM','MMLL','.LD.']),(7,16,['.HH.','HMML','.LLD']),(23,13,['.H.','HMM','MLD','.D.'])]:stamp(L,x,y,pattern,cream)
  else:
   stamp(L,5,7,['HM','LD'],chunks);stamp(L,9,9,['HM','DD'],chunks)
   stamp(L,9,6,['HM','LD'],cream);stamp(L,4,9,['HM','LD'],cream)
  if kind=='warming':
   c={'H':'#fff0cf','M':'#e8cfa4','D':'#c7aa84'}
   if n==32:
    stamp(steam,10,3,['H..','MH.','.MH','.MH','MH.','D..'],c);stamp(steam,21,4,['.H','HM','HM','.M','.D'],c)
   else:stamp(steam,5,1,['H.','.M','HM','D.'],c);stamp(steam,10,2,['.H','HM','.D'],c)
 elif kind=='cooling':
  apple={'R':'#ac2932','r':'#e34b42','H':'#fff0c2','M':'#f0d799','L':'#d2af72','D':'#914734'}
  melon={'G':'#407448','g':'#77ad51','h':'#c9d47b','H':'#ff9b79','M':'#ed5c52','L':'#b93640','s':'#6d2730'}
  if n==32:
   stamp(L,8,10,['...R..','..rH..','.rHHM.','rHHMML','RHMMML','.RMLD.','..DDD.'],apple)
   stamp(L,17,11,['....Gg.','...MHhG','..HMMLg','.HMMLLg','HMsMLLg','HMLsLgh','.LLLhg.','..Ggg..'],melon)
   stamp(L,10,15,['..Rr..','.HHMr.','HHMMLr','.MMLR.','..DD..'],apple)
  else:
   stamp(L,4,6,['.RH','RHML','.RLD'],apple)
   stamp(L,8,6,['..G',' .Hg'.replace(' ','.'),'HMLg','MLhg','.Gg.'],melon)
 elif kind=='recovery':
  mush={'r':'#913036','R':'#c44e49','H':'#ed8c65','s':'#fff1bd','g':'#ebd89e','d':'#ab8152'}
  if n==32:
   stamp(L,7,10,['..HHH..','.HRRRR.','RRsRsRR','rRRRRrr','.ddgdd.','..sgg..','..gdd..'],mush)
   stamp(L,20,12,['.HHR..','HRsRR.','RsRRsR','.rrrd.','..sg..','..dd..'],mush)
   stamp(L,14,15,['.sg.','sggd','.dd.'],mush)
   for x,y,w in [(17,12,1),(6,15,1),(18,18,2)]:span(L,x,y,w,mush['s'])
  else:
   stamp(L,4,6,['.HHR.','RsRsR','.rdg.','..g..'],mush)
   stamp(L,9,8,['HRR','Rrs','.g.'],mush)
   span(L,8,7,1,mush['s']);span(L,6,10,1,mush['g'])
 return L,steam

def composite(layers):
 d={}
 for l in layers:d.update(l)
 return d

def produce():
 starting=cli('state','--pixel-hash');(ROOT/'review/aseprite-starting-state.json').write_text(json.dumps(starting,ensure_ascii=False,indent=2))
 specs=[];states=[]
 for kind in NAMES:
  for n in (32,16):
   base,front=bowl(n);contents,steam=food(kind,n)
   layers=[('01 共享木碗',base),('02 食材与汤汁',contents),('03 前沿遮挡与木纹',front),('04 蒸汽',steam)]
   cli('new',n,n,'--color-mode','rgb');st=cli('state');cli('layer','update',st['active']['layerId'],'--name',layers[0][0])
   saved=[]
   for i,(name,pixels) in enumerate(layers):
    if i:cli('layer','create',name)
    saved.append({'name':name,'runs':write(pixels,n)})
   name=f'meal-{kind}-{n}';cli('save',name+'.aseprite','--output',ROOT/'source'/f'{name}.aseprite');cli('export',name+'.png','--output',ROOT/'exports'/f'{name}.png')
   st=cli('state','--pixel-hash');states.append({'name':name,'state':st})
   specs.append({'name':name,'kind':list(NAMES).index(kind),'size':[n,n],'label':NAMES[kind],'layers':saved,'pixels':[[x,y,c] for (x,y),c in sorted(composite([p for _,p in layers]).items())]})
   print(name+' 已保存原生源稿与PNG',flush=True)
 (ROOT/'source/pixel-designs.json').write_text(json.dumps(specs,ensure_ascii=False,indent=2))
 (ROOT/'review/aseprite-saved-states.json').write_text(json.dumps(states,ensure_ascii=False,indent=2))
 return specs
if __name__=='__main__':produce()
