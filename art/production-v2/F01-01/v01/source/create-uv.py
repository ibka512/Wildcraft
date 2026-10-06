from pathlib import Path
import subprocess,json
ROOT=Path(__file__).resolve().parents[1]
CTL='/Users/zhou/Desktop/Aseprite Web 本机版/plugins/aseprite-web/scripts/aseprite-web-control'
def cli(*args):
    p=subprocess.run([CTL,'cli',*map(str,args)],capture_output=True,text=True,timeout=55)
    if p.returncode: raise RuntimeError(p.stderr[-1500:])
    return json.loads(p.stdout)
def runs(data):
    args=['runs','--frame','0']
    for x,y,w,c in data:args += ['--run',f'{x},{y},{w},{c}ff']
    return cli(*args)
def rect(x,y,w,h,c):return [(x,j,w,c) for j in range(y,y+h)]
state=cli('state','--pixel-hash')
assert state['documents'][-1]['width']==64
layer=state['active']['layerId']
cli('layer','update',layer,'--name','01 材质分区与体积色阶')
data=[]
regions={}
def region(name,x,y,w,h,colors):
    regions[name]={'pixels':[x,y,w,h],'model_uv':[x/4,y/4,(x+w)/4,(y+h)/4]}
    for j in range(h):
        c=colors[min(len(colors)-1,j*len(colors)//h)]
        data.append((x,y+j,w,c))
region('outer_wall',0,0,32,16,['#727c81','#535e65','#465159','#3c464f','#364149','#2e3942','#28333b','#222d34'])
region('inner_wall',32,0,32,16,['#454c50','#343d42','#2c353b','#273037','#222b32','#20292f','#1c252b','#1a2329'])
region('rim_top',0,16,32,8,['#9ba4a5','#858f92','#748086','#66737b'])
region('rim_side',0,24,32,8,['#647179','#4e5b64','#3b4852','#2c3943'])
region('inner_floor',32,16,32,16,['#303c43','#303c43','#344149','#39464d','#344149','#303c43','#2b363e','#27323a'])
region('wood',0,32,16,16,['#a7794b','#986a3e','#8a5d35','#7d512d','#704626','#643d21','#57351d','#4b2d19'])
region('copper',16,32,8,8,['#c89d76','#b9875e','#a57650','#986445','#875638','#76482e','#6b402c','#553524'])
region('bracket',24,32,8,8,['#52606a','#424f59','#35434d','#2b3741'])
region('underside',32,32,16,16,['#293137','#242b31','#20272d','#1c2328'])
runs(data)
cli('layer','create','02 金属锻面与木纹像素簇')
detail=[]
for r in [(3,3,8,2,'#4d5961'),(15,2,6,2,'#57646c'),(23,4,7,2,'#4c5962'),(6,6,9,3,'#414f58'),(20,7,8,2,'#394750'),(2,10,6,2,'#34424b'),(10,11,9,2,'#303d47'),(24,11,6,2,'#2a3740'),(4,14,11,1,'#28343c'),(19,14,9,1,'#25323a'),
(35,3,10,2,'#303b42'),(49,4,11,2,'#29343b'),(39,8,12,2,'#253037'),(53,10,8,2,'#202a31'),
(2,16,7,1,'#adb6b5'),(12,17,10,1,'#949fa1'),(25,18,5,2,'#819095'),(4,20,8,1,'#7c898e'),(16,21,7,2,'#6d7d84'),
(3,25,9,1,'#5a6972'),(19,26,9,1,'#495a64'),(7,29,12,1,'#374751'),
(37,21,7,2,'#3c4950'),(45,23,8,3,'#3d4b52'),(51,26,7,2,'#334048'),(39,28,10,1,'#2c3942'),
(1,34,2,6,'#b48550'),(5,33,2,7,'#9d6a37'),(9,35,2,9,'#845426'),(13,34,1,6,'#a3723e'),(2,42,3,1,'#754a26'),(7,43,5,1,'#67411f'),(4,46,3,1,'#57361b'),
(17,33,5,1,'#d4ac85'),(17,34,1,3,'#c9956d'),(21,35,2,2,'#a36e4a'),(18,38,4,1,'#74492f'),(25,33,5,1,'#677784'),(25,34,1,4,'#4b5d6b')]:
    detail += rect(*r)
runs(detail)
cli('save','cooking-pot-atlas-64.aseprite','--output',ROOT/'source/cooking-pot-atlas-64.aseprite')
cli('export','cooking-pot-atlas-64.png','--output',ROOT/'exports/cooking-pot-atlas-64.png')
state=cli('state','--pixel-hash')
(ROOT/'review/aseprite-saved-state.json').write_text(json.dumps(state,ensure_ascii=False,indent=2))
(ROOT/'source/uv-regions.json').write_text(json.dumps({'atlas_size':[64,64],'regions':regions},indent=2))
print('Aseprite已绘制2图层64×64原生UV材质，保存源稿与PNG。')
