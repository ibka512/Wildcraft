from pathlib import Path
import json,math,uuid
ROOT=Path(__file__).resolve().parents[1]
uv=json.loads((ROOT/'source/uv-regions.json').read_text())['regions']
parts=[]
faces=['north','south','east','west','up','down']
def cube(name,a,b,material='outer_wall',rotation=None,overrides=None):
    e={'name':name,'from':a,'to':b,'faces':{}}
    if rotation:e['rotation']=rotation
    for face in faces:
        mat=(overrides or {}).get(face,material)
        x,y,w,h=uv[mat]['pixels']
        sx,sy,sz=[b[i]-a[i] for i in range(3)]
        lengths={'north':(sx,sy),'south':(sx,sy),'east':(sz,sy),'west':(sz,sy),'up':(sx,sz),'down':(sx,sz)}
        lu,lv=lengths[face]
        # Each UV is explicit and maps to a named material region.
        usedw,usedh=min(w,lu*2),min(h,lv*2)
        e['faces'][face]={'texture':'#atlas','uv':[x/4,y/4,(x+usedw)/4,(y+usedh)/4]}
    parts.append(e)
    return e
# Filled octagonal base with flush side footprint, not a square pedestal.
cube('base_longitudinal',[4,0,2],[12,2,14],'underside',overrides={'up':'inner_floor'})
cube('base_cross',[2,.01,4],[14,1.99,12],'underside',overrides={'up':'inner_floor'})
for tag,cx,cz in [('NW',4,4),('NE',12,4),('SE',12,12),('SW',4,12)]:
    half=math.sqrt(2)
    cube('base_corner_'+tag,[cx-half,.02,cz-half],[cx+half,2.01,cz+half],'underside',rotation={'origin':[cx,1,cz],'axis':'y','angle':45,'rescale':False},overrides={'up':'inner_floor'})
def ring(prefix,y0,y1,thickness,top):
    t=thickness
    cube(prefix+'_north',[4,y0,2],[12,y1-(.02 if prefix=='rim' else 0),2+t],top,overrides={'south':'inner_wall','up':'rim_top' if prefix=='rim' else top})
    cube(prefix+'_south',[4,y0,14-t],[12,y1-(.02 if prefix=='rim' else 0),14],top,overrides={'north':'inner_wall','up':'rim_top' if prefix=='rim' else top})
    cube(prefix+'_west',[2,y0,4],[2+t,y1-(.02 if prefix=='rim' else 0),12],top,overrides={'east':'inner_wall','up':'rim_top' if prefix=='rim' else top})
    cube(prefix+'_east',[14-t,y0,4],[14,y1-(.02 if prefix=='rim' else 0),12],top,overrides={'west':'inner_wall','up':'rim_top' if prefix=='rim' else top})
    shift=t/(2*math.sqrt(2));L=2*math.sqrt(2)
    for tag,cx,cz,angle,out in [('NW',3+shift,3+shift,45,'north'),('NE',13-shift,3+shift,-45,'north'),('SE',13-shift,13-shift,45,'south'),('SW',3+shift,13-shift,-45,'south')]:
        inner='south' if out=='north' else 'north'
        cube(prefix+'_'+tag,[cx-L/2,y0,cz-t/2],[cx+L/2,y1,cz+t/2],top,rotation={'origin':[cx,(y0+y1)/2,cz],'axis':'y','angle':angle,'rescale':False},overrides={inner:'inner_wall','up':'rim_top' if prefix=='rim' else top})
ring('wall',2,8.65,1.15,'outer_wall')
ring('rim',8.65,10,1.45,'rim_side')
# Two compact handles: brackets separated in z, with a real gap under the grip.
for side,grip,bridge,cuff,rivet in [('left',[0,7.1,6],[1,7.5,6],[0,7.05,6],[0,7.5,6.18]),('right',[15,7.1,6],[14,7.5,6],[15,7.05,6],[15.96,7.5,6.18])]:
    cube(side+'_wood_grip',[.04 if side=='left' else 15,7.1,6.5],[1 if side=='left' else 15.96,8.75,9.5],'wood')
    for z in [6,9.2]:
        bx=1 if side=='left' else 13.3
        cube(side+f'_mount_{z}',[bx,7.5,z],[bx+1.7,8.4,z+.8],'bracket')
    for z in [6,9.5]:
        cube(side+f'_metal_cap_{z}',[.04 if side=='left' else 15,7.05,z],[1 if side=='left' else 15.96,8.85,z+.5],'bracket')
        rx=0 if side=='left' else 15.96
        cube(side+f'_copper_pin_{z}',[rx,7.55,z+.1],[rx+.04,8.22,z+.4],'copper')
model={'parent':'minecraft:block/block','ambientocclusion':True,'textures':{'atlas':'wildcraft:block/cooking_pot','particle':'wildcraft:block/cooking_pot'},'elements':[{k:v for k,v in e.items() if k!='name'} for e in parts], 'display':{'gui':{'rotation':[30,225,0],'translation':[0,1,0],'scale':[.7,.7,.7]},'fixed':{'rotation':[0,0,0],'translation':[0,0,0],'scale':[.6,.6,.6]}}}
(ROOT/'exports/cooking-pot.block-model.json').write_text(json.dumps(model,indent=2))
(ROOT/'source/geometry-and-uv.json').write_text(json.dumps({'units':'1/16 block','collision_from':[2,0,2],'collision_to':[14,10,14],'texture_size':[64,64],'parts':parts,'display':model['display']},indent=2))
# Editable Blockbench standard Java block model, using pixel UV coordinates.
bbe=[]
for e in parts:
    el={'name':e['name'],'uuid':str(uuid.uuid4()),'type':'cube','from':e['from'],'to':e['to'],'origin':e.get('rotation',{}).get('origin',[8,8,8]),'faces':{face:{'uv':[v*4 for v in val['uv']],'texture':0} for face,val in e['faces'].items()},'box_uv':False,'rescale':False,'autouv':0,'shade':True}
    if 'rotation' in e:el['rotation']=[0,e['rotation']['angle'],0]
    bbe.append(el)
bb={'meta':{'format_version':'4.10','model_format':'java_block','box_uv':False},'name':'Wildcraft cooking pot F01-01 v01','resolution':{'width':64,'height':64},'elements':bbe,'outliner':[e['uuid'] for e in bbe],'textures':[{'name':'cooking-pot-atlas-64.png','path':str(ROOT/'exports/cooking-pot-atlas-64.png'),'id':'0','uuid':str(uuid.uuid4()),'width':64,'height':64,'uv_width':64,'uv_height':64,'particle':True,'render_mode':'default'}],'display':model['display']}
(ROOT/'source/cooking-pot-v01.bbmodel').write_text(json.dumps(bb,indent=2))
print(f'已导出{len(parts)}方盒的锅体模型，含8段45度切角。运行资源尚未替换。')
