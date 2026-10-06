"""Entity chassis art; geometry in 1/16 block with bottom origin, six existing oriented mounts."""
from pathlib import Path
import json,copy,uuid,base64
R=Path(__file__).resolve().parents[1];U=json.loads((R/'source/uv-regions.json').read_text())['regions'];parts=[]
def cube(name,a,b,key,override=None,group='body',node=None):
 p=dict(name=name,**{'from':a,'to':b},faces={f:dict(texture='#atlas',uv=U[(override or {}).get(f,key)]['uv'],materialRegion=(override or {}).get(f,key)) for f in ['north','south','east','west','up','down']},group=group)
 if node is not None:p['node']=node
 parts.append(p)
# All core and rim parts stay within the current 1.4 x .65 body.
cube('body_core',[-10.56,.8,-10.56],[10.56,8.96,10.56],'housing',{'north':'vent','down':'underside'})
cube('upper_deck',[-11.2,8.64,-11.2],[11.2,10.4,11.2],'edge',{'up':'deck','down':'underside'})
cube('lower_plinth',[-11.2,0,-11.2],[11.2,1.44,11.2],'base',{'down':'underside'})
for i,(x,z) in enumerate([(-11.2,-11.2),(9.76,-11.2),(-11.2,9.76),(9.76,9.76)]):
 cube('steel_corner_'+str(i),[x,1.44,z],[x+1.44,8.64,z+1.44],'steel')
 cube('copper_fastener_'+str(i),[x+.18,10.4,z+.18],[x+1.26,10.56,z+1.26],'copper')
# Narrow strengthening ribs flanking each centered pad, kept under deck.
for side in ['front','back']:
 z0,z1=(10.56,10.9) if side=='front' else (-10.9,-10.56)
 for x in [-6.6,5.8]:cube(side+'_rib_'+str(x),[x,1.44,z0],[x+.8,8.64,z1],'joint')
for side in ['left','right']:
 x0,x1=(-10.9,-10.56) if side=='left' else (10.56,10.9)
 for z in [-6.6,5.8]:cube(side+'_rib_'+str(z),[x0,1.44,z],[x1,8.64,z+.8],'joint')
# Panel faces exactly within current functional panel envelope x +-.25, y .55-.72, z .32-.5.
cube('control_plate',[-4,10.4,5.12],[4,10.8,8.0],'steel',{'up':'control'},group='control')
# Same mounting assembly reused six times; its origin is the exact MachineNodes point.
N=[dict(id=0,name='顶',point=[0,10.4,0],normal=[0,1,0]),dict(id=1,name='底',point=[0,0,0],normal=[0,-1,0]),dict(id=2,name='后',point=[0,5.2,-11.2],normal=[0,0,-1]),dict(id=3,name='前',point=[0,5.2,11.2],normal=[0,0,1]),dict(id=4,name='左',point=[-11.2,5.2,0],normal=[-1,0,0]),dict(id=5,name='右',point=[11.2,5.2,0],normal=[1,0,0])]
for n in range(6):
 cube(f'node{n}/recess',[-2.08,-2.08,-.64],[2.08,2.08,.38],'recess',{'south':'port'},'node',n)
 for name,a,b in [('left',[-2.08,-2.08,.38],[-1.52,2.08,.64]),('right',[1.52,-2.08,.38],[2.08,2.08,.64]),('top',[-1.52,1.52,.38],[1.52,2.08,.64]),('bottom',[-1.52,-2.08,.38],[1.52,-1.52,.64])]:cube(f'node{n}/rim_'+name,a,b,'trim',group='node',node=n)
 for i,x in enumerate([-1.0,.44]):cube(f'node{n}/contact{i}',[x,-.8,.38],[x+.56,.8,.62],'copper',group='node',node=n)
 # Upper key mark embossed in steel, not a second mount.
 cube(f'node{n}/key',[-.3,1.55,.64],[.3,1.95,.68],'steel',group='node',node=n)
B=json.loads((R/'references/geometry-and-uv.json').read_text());battery=copy.deepcopy(B['parts']);windows=copy.deepcopy(B['windows'])
# Center original authored battery around its mounting origin. Rear face = node outward .04 block.
O=[-8,-8,.64-7.2]
for p in battery:
 p['name']='battery/'+p['name'];p['from']=[v+O[i] for i,v in enumerate(p['from'])];p['to']=[v+O[i] for i,v in enumerate(p['to'])];p['group']='battery'
 for face in p['faces'].values():face['uv']=[v/2 for v in face['uv']]
for p in windows:p['from']=[v+O[i] for i,v in enumerate(p['from'])];p['to']=[v+O[i] for i,v in enumerate(p['to'])]
G=dict(asset='F05-01',units='1/16 block',origin='body bottom center',front='+Z',textureSize=[64,64],parts=parts,nodes=N,batteryParts=battery,batteryWindows=windows,batteryEnergyUV=[3,4,6,6],batteryScale=1,batteryOriginalEnvelope=B['envelope'],collision={'width':1.4,'height':.65,'unchanged':True},controlHitbox={'min':[-.25,.55,.32],'max':[.25,.72,.5]},nodeTemplateOuter=[.26,.26,.08],nodeSelectionRadius=.34,integrated=False)
(R/'source/geometry-and-uv.json').write_text(json.dumps(G,ensure_ascii=False,indent=2)+'\n');(R/'exports/machine-presentation.json').write_text(json.dumps(dict(asset='F05-01',version='v01',status='draft_pending_review',integrated=False,nodes=N,controlHitbox=G['controlHitbox'],collision=G['collision'],batteryScale=1,panel='neutral mechanical power mark, no enabled/working status baked',nodePlacement='copy same collar; transform matches existing orient() plus exact MachineNodes point',nextBatteryMountVerification='F05-05 retains full six-node adaptation responsibility'),ensure_ascii=False,indent=2)+'\n')
# Free-form editable source: meshes retain explicit corner UV under all six rotations.
def rot(v,n):
 x,y,z=v;return [(x,z,-y),(x,-z,y),(-x,y,-z),(x,y,z),(-z,y,x),(z,y,-x)][n]
def world(v,p):
 if 'node' not in p:return v
 return [x+y for x,y in zip(rot(v,p['node']),N[p['node']]['point'])]
def corners(p):
 x0,y0,z0=p['from'];x1,y1,z1=p['to'];return [[x0,y0,z0],[x1,y0,z0],[x1,y1,z0],[x0,y1,z0],[x0,y0,z1],[x1,y0,z1],[x1,y1,z1],[x0,y1,z1]]
FM={'north':[0,1,2,3],'south':[5,4,7,6],'east':[1,5,6,2],'west':[4,0,3,7],'up':[3,2,6,7],'down':[4,5,1,0]};elems=[];groups={}
for p in parts:
 vs={str(i):world(v,p) for i,v in enumerate(corners(p))};faces={}
 for name,ids in FM.items():
  u0,v0,u1,v1=[v*4 for v in p['faces'][name]['uv']];co=[[u0,v1],[u1,v1],[u1,v0],[u0,v0]];faces[name]={'vertices':[str(i) for i in ids],'uv':{str(i):uv for i,uv in zip(ids,co)},'texture':0}
 uid=str(uuid.uuid4());elems.append(dict(name=p['name'],uuid=uid,type='mesh',vertices=vs,faces=faces,origin=[0,0,0]));key='body' if p['group']=='body' else 'control' if p['group']=='control' else 'node'+str(p['node']);groups.setdefault(key,[]).append(uid)
outliner=[dict(name=k,uuid=str(uuid.uuid4()),origin=N[int(k[4:])]['point'] if k.startswith('node') else [0,0,0],children=v) for k,v in groups.items()]
bb=dict(meta={'format_version':'4.10','model_format':'free','box_uv':False},name='Wildcraft mechanical chassis F05-01 v01',resolution={'width':64,'height':64},elements=elems,outliner=outliner,textures=[dict(name='machine-atlas-64.png',id='0',uuid=str(uuid.uuid4()),width=64,height=64,uv_width=64,uv_height=64,source='data:image/png;base64,'+base64.b64encode((R/'exports/machine-atlas-64.png').read_bytes()).decode(),mode='bitmap')]);(R/'source/machine-body-v01.bbmodel').write_text(json.dumps(bb,indent=2)+'\n')
print(f'主体/控制板{sum(p["group"]!="node" for p in parts)}盒；6个共享接头各8盒；电池引用比例1.00。')
