"""Copy adopted runtime exports only; raw sources remain in art/production-v2.
Run with Pillow available. Generated resources are owned by SecondArtModelProvider.
"""
from pathlib import Path
import hashlib, json, shutil
ROOT=Path(__file__).resolve().parents[1]
ART=ROOT/'art/production-v2'
RES=ROOT/'src/main/resources/assets/wildcraft'
imports=[]
def copy(task,name,target,version='v01'):
    src=ART/task/version/'exports'/name
    dst=RES/target
    dst.parent.mkdir(parents=True,exist_ok=True)
    shutil.copyfile(src,dst)
    imports.append({'source':str(src.relative_to(ROOT)),'target':str(dst.relative_to(ROOT)), 'sha256':hashlib.sha256(src.read_bytes()).hexdigest()})
copy('F01-01','cooking-pot-atlas-64.png','textures/block/cooking_pot.png')
copy('F01-01','cooking-pot.block-model.json','models/block/cooking_pot.json')
for kind in ['vegetable','warming','cooling','recovery']:
    copy('F01-02',f'meal-{kind}-16.png',f'textures/item/meal_{kind}.png')
for name in ['warmth-meal','cooling-meal','stamina-recovery']:
    copy('F01-03',f'{name}-16.png',f'textures/gui/{name}.png')
copy('F02-01','ambient-temperature-color-16.png','textures/gui/ambient-temperature.png')
for task,name,target in [('F01-04','cooking-ui-176x182.png','cooking'),('F04-04','charger-ui-176x166.png','charger'),('F06-02','fabricator-ui-176x192.png','fabricator')]:
    copy(task,name,f'textures/gui/{target}.png')
for task,model,atlas,target in [('F04-03','charger-static-model.json','charger-atlas-64.png','charger'),('F06-01','fabricator-static-model.json','fabricator-atlas-64.png','fabricator')]:
    copy(task,model,f'models/block/{target}.json');copy(task,atlas,f'textures/block/{target}_atlas.png')
for name in ['empty','full']:
    copy('F04-02',f'battery-{name}-16.png',f'textures/gui/battery-{name}.png')
copy('F04-01','battery-atlas-32.png','textures/entity/battery.png')
copy('F04-01','battery-static-model.json','models/item/battery_3d.json')
copy('F04-01','battery-item-16.png','textures/item/battery.png')
# Translation exports become source inputs for the language provider, not generated files.
labels={locale:{} for locale in ['en_us','zh_cn']}
for task,prefix in [('F04-04','charger'),('F06-02','fabricator')]:
    d=json.loads((ART/task/'v01/exports'/f'{prefix}-ui-labels-candidate.json').read_text())
    if task=='F04-04':
        for lang,entries in d['labels'].items():labels[lang].update(entries)
    else:
        for row in d['states']:
            for lang,locale in [('zh','zh_cn'),('en','en_us')]:
                labels[locale][f'fabrication.wildcraft.screen.status.{row["id"]}']=row[lang]
                labels[locale][f'fabrication.wildcraft.screen.help.{row["id"]}']=row['help'+lang.capitalize()]
for lang,zh in [('zh_cn',True),('en_us',False)]:
    labels[lang].update({'cooking.wildcraft.output':'成品' if zh else 'Meal', 'energy.wildcraft.battery_label':'电池' if zh else 'Battery','energy.wildcraft.charge_label':'电量' if zh else 'Charge'})
    for i,n in enumerate(['保暖','耐热','精力恢复'] if zh else ['Warmth','Cooling','Stamina recovery'],1):labels[lang][f'hud.wildcraft.meal.name.{i}']=n
p=ROOT/'src/client/resources/wildcraft-datagen-labels.json';p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(labels,ensure_ascii=False,indent=2)+'\n')
(RES/'second-art-provenance.json').write_text(json.dumps(imports,ensure_ascii=False,indent=2)+'\n')
print(f'Imported {len(imports)} adopted exports')
# Mesh exports use model units and arbitrary convex faces, not Minecraft cuboids.
# Triangulate only polygons with >4 vertices; triangles use the native degenerate-quad convention.
import math
face_indices={'north':[0,3,2,1],'south':[4,5,6,7],'west':[0,4,7,3],'east':[1,2,6,5],'down':[0,1,5,4],'up':[3,7,6,2]}
points=[[0,10.4,0],[0,0,0],[0,5.2,-11.2],[0,5.2,11.2],[-11.2,5.2,0],[11.2,5.2,0]]
def orient(p,n):
    x,y,z=p
    return [(x,z,-y),(x,-z,y),(-x,y,-z),(x,y,z),(-z,y,x),(z,y,-x)][n]
def quads(part,extended=False,node=False):
    if 'vertices' in part:
        vertices=part.get('extendedVertices',part['vertices']) if extended else part['vertices'];faces=part['faces']
    else:
        x0,y0,z0=part['from'];x1,y1,z1=part['to'];vertices=[[x0,y0,z0],[x1,y0,z0],[x1,y1,z0],[x0,y1,z0],[x0,y0,z1],[x1,y0,z1],[x1,y1,z1],[x0,y1,z1]]
        faces=[]
        for name,f in part['faces'].items():
            u0,v0,u1,v1=f['uv'];uv=[[u0,v1],[u1,v1],[u1,v0],[u0,v0]]
            # Native face rotation is clockwise in texture coordinates.
            shift=f.get('rotation',0)//90;uv=uv[-shift:]+uv[:-shift] if shift else uv
            faces.append({'indices':face_indices[name],'uvs':uv})
        if 'rotation' in part:
            r=part['rotation'];o=r['origin'];axis='xyz'.index(r['axis']);a=math.radians(r['angle']);c,s=math.cos(a),math.sin(a)
            for idx,p in enumerate(vertices):
                q=[p[i]-o[i] for i in range(3)];j,k=[(1,2),(2,0),(0,1)][axis];q[j],q[k]=c*q[j]-s*q[k],s*q[j]+c*q[k];vertices[idx]=[q[i]+o[i] for i in range(3)]
    if node and part.get('group')=='node':
        n=part['node'];vertices=[[v[i]+points[n][i] for i in range(3)] for v in [orient(p,n) for p in vertices]]
    result=[]
    for f in faces:
        ids=f['indices'];uv=f['uvs'];groups=[list(range(len(ids)))] if len(ids)==4 else [[0,i,i+1,i+1] for i in range(1,len(ids)-1)]
        for group in groups:
            q=[]
            for i in group:q+= [round(v/16,8) for v in vertices[ids[i]]]+[round(v/16,8) for v in uv[i]]
            result.append(q)
    return result
mesh_dir=RES/'geometry';mesh_dir.mkdir(exist_ok=True)
def bake(name,parts,texture,node=False,pivot=None,windows=None):
    out={'texture':texture,'pivot': [v/16 for v in (pivot or [0,0,0])], 'parts':[]}
    for p in parts:
        entry={'name':p.get('name','part_'+str(len(out['parts']))),'group':p.get('group','fixed'),'quads':quads(p,node=node)}
        if 'extendedVertices' in p:entry['extendedQuads']=quads(p,True,node)
        out['parts'].append(entry)
    if windows:
        out['windows']=[{'index':w['indexBottomUp'],'from':[v/16 for v in w['from']],'to':[v/16 for v in w['to']]} for w in windows]
    (mesh_dir/(name+'.json')).write_text(json.dumps(out,separators=(',',':'))+'\n')
body=json.loads((ART/'F05-01/v01/exports/machine-body-geometry.json').read_text())
bake('machine',body['parts'],'machine_atlas',node=True)
copy('F05-01','machine-atlas-64.png','textures/entity/machine_atlas.png')
copy('F05-01','machine-body-item-16.png','textures/item/machine_body.png')
for task,name,tex in [('F05-02','wing','fabric_atlas'),('F05-03','fan','machine_atlas'),('F05-04','rocket','rocket_atlas'),('F05-06','spring','machine_atlas'),('F05-07','wheel','machine_atlas'),('F05-08','stabilizer','machine_atlas'),('F05-09','buoyancy','fabric_atlas')]:
    d=json.loads((ART/task/'v01/exports'/(name+'-geometry.json')).read_text());bake(name,d['parts'],tex,pivot=d.get('rotorPivot',d.get('rotationPivot')))
    copy(task,name+'-item-16.png','textures/item/'+name+'.png')
copy('F05-02','wing-atlas-128x64.png','textures/entity/fabric_atlas.png')
copy('F05-04','rocket-atlas-128x64.png','textures/entity/rocket_atlas.png')
d=json.loads((ART/'F05-04/v01/exports/spent-rocket-geometry.json').read_text());bake('spent_rocket',d['parts'],'rocket_atlas')
copy('F05-04','spent-rocket-item-16.png','textures/item/spent_rocket.png')
d=json.loads((ART/'F05-10/v02/references/F05-05-geometry.json').read_text());bake('battery',d['batteryParts'],'battery',windows=d['batteryWindows'])
(RES/'second-art-provenance.json').write_text(json.dumps(imports,ensure_ascii=False,indent=2)+'\n')
print('Baked 10 mechanical meshes without changing original source geometry')
# Same full-size battery geometry for held/dropped items and the charger's display.
model=json.loads((ART/'F04-01/v01/exports/battery-static-model.json').read_text())
windows=json.loads((ART/'F04-01/v01/exports/battery-dynamic-windows.json').read_text())
bake('battery_item',model['elements'],'battery',windows=windows['windows'])
# Native 16px inventory masks have 18 total fill columns and 19 distinct views.
from PIL import Image
base=Image.open(RES/'textures/item/battery.png').convert('RGBA')
full=Image.open(RES/'textures/gui/battery-full.png').convert('RGBA')
# The item and readout masks have different widths; use the authored red energy tile.
energy_atlas=Image.open(RES/'textures/entity/battery.png').convert('RGBA')
for total in range(19):
    image=base.copy()
    for i in range(3):
        width=max(0,min(6,total-6*i))
        if width:
            for yy in range(2):
                for xx in range(width):image.putpixel((5+xx,12-3*i+yy),energy_atlas.getpixel((12+xx,16+yy*3)))
    dst=RES/f'textures/item/battery_charge_{total}.png';image.save(dst)
copy('F03-03','fuse-shared-atlas-64x32.png','textures/gui/fuse-status.png')
copy('F03-03','fuse-unavailable-16.png','textures/item/fuse_unavailable.png')
(RES/'second-art-provenance.json').write_text(json.dumps(imports,ensure_ascii=False,indent=2)+'\n')

for audio in (ART/'S01/v01/exports/sounds').glob('*.ogg'):
    copy('S01','sounds/'+audio.name,'sounds/s01/'+audio.name)
soundfile=RES/'sounds.json';sounds=json.loads(soundfile.read_text());sounds.update(json.loads((ART/'S01/v01/exports/sounds-additions-proposal.json').read_text()));soundfile.write_text(json.dumps(sounds,ensure_ascii=False,indent=2)+'\n')
labels_path=ROOT/'src/client/resources/wildcraft-datagen-labels.json';labels=json.loads(labels_path.read_text());subs=json.loads((ART/'S01/v01/exports/subtitles-proposal.json').read_text())
for lang,entries in subs.items():labels[lang].update(entries)
labels_path.write_text(json.dumps(labels,ensure_ascii=False,indent=2)+'\n')
(RES/'second-art-provenance.json').write_text(json.dumps(imports,ensure_ascii=False,indent=2)+'\n')
