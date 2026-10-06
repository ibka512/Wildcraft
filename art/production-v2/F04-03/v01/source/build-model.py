"""Single-block charger, fixed front +Z, actual adopted battery at scale 1."""
from pathlib import Path
import json,uuid,base64,copy
R=Path(__file__).resolve().parents[1];U=json.loads((R/'source/uv-regions.json').read_text())['regions'];parts=[]
def cube(name,a,b,key,override=None):parts.append({'name':name,'from':a,'to':b,'faces':{f:{'texture':'#atlas','uv':U[(override or {}).get(f,key)]['uv'],'materialRegion':(override or {}).get(f,key)} for f in ['north','south','east','west','up','down']}})
cube('base_plinth',[0,1,0],[16,3.5,15.5],'steel')
for i,(x,z) in enumerate([(0,0),(13.5,0),(0,13.5),(13.5,13.5)]):cube('foot_'+str(i),[x,0,z],[x+2.5,1,z+2.5],'housing')
cube('front_fascia_left',[0,1,15.5],[6.5,3.5,15.95],'steel');cube('front_fascia_right',[9.5,1,15.5],[16,3.5,15.95],'steel')
cube('rear_housing',[3.3,3.5,2],[12.7,13.7,10.6],'housing',{'north':'rearplate','south':'inset'})
cube('left_pillar',[1,3.5,2],[3.3,13.7,13.5],'housing',{'south':'rim','west':'vent'});cube('right_pillar',[12.7,3.5,2],[15,13.7,13.5],'housing',{'south':'rim','east':'vent'})
cube('top_canopy',[1.5,13.7,2],[14.5,15.6,11],'steel',{'south':'rim','up':'top'})
cube('left_top_step',[2,15.6,3],[5,16,7],'rim');cube('right_top_step',[11,15.6,3],[14,16,7],'rim');cube('top_copper_trim',[5.5,15.6,3.5],[10.5,16,5.5],'copper')
cube('pocket_floor',[3.3,3.5,10.6],[12.7,4.8,13.5],'inset');cube('retaining_lip',[4.4,4.8,12.9],[11.6,5.65,13.5],'rim')
cube('left_socket_rail',[4.3,5.8,10.6],[4.6,12.9,13.2],'rim');cube('right_socket_rail',[11.4,5.8,10.6],[11.7,12.9,13.2],'rim')
for i,x in enumerate([6.0,8.7]):cube('battery_rear_contact_'+str(i),[x,6.88,10.6],[x+1.3,8.28,10.99],'copper')
for i,x in enumerate([1.7,13.3]):cube('front_fastener_'+str(i),[x,1.7,15.95],[x+1,2.7,16],'copper')
cube('rear_conduction_plate',[6,6,1.9],[10,10,2.001],'copper');cube('left_conduction_plate',[.9,6,6],[1.001,10,10],'copper');cube('right_conduction_plate',[14.999,6,6],[15.1,10,10],'copper')
cube('status_background',[6.9,1.25,15.5],[9.1,3.15,15.66],'inset')
for name,a,b in [('left',[6.5,1.1,15.5],[6.9,3.3,16]),('right',[9.1,1.1,15.5],[9.5,3.3,16]),('bottom',[6.9,1.1,15.5],[9.1,1.25,16]),('top',[6.9,3.15,15.5],[9.1,3.3,16])]:cube('status_bezel_'+name,a,b,'steel')
battery=json.loads((R/'references/geometry-and-uv.json').read_text());offset=[0,1.48,3.8];bp=copy.deepcopy(battery['parts']);windows=copy.deepcopy(battery['windows'])
for p in bp:
 p['name']='battery/'+p['name'];p['from']=[v+offset[i] for i,v in enumerate(p['from'])];p['to']=[v+offset[i] for i,v in enumerate(p['to'])]
 for f in p['faces'].values():f['uv']=[v/2 for v in f['uv']]
for p in windows:
 p['from']=[v+offset[i] for i,v in enumerate(p['from'])];p['to']=[v+offset[i] for i,v in enumerate(p['to'])]
status={'from':[7,1.3,15.671],'to':[9,3.1,15.671],'normal':[0,0,1],'tiles':{str(i):U['status_'+str(i)]['uv'] for i in range(5)},'nonEmissive':True,'symbols':{'0':'empty outline','1':'broken line','2':'chevron','3':'check','4':'pause'},'source':'future world presentation snapshot; current states synchronized only to menu'}
G={'units':'1/16 block','front':'+Z / fixed south, existing block has no facing property','textureSize':[64,64],'parts':parts,'batteryParts':bp,'batteryOriginalEnvelope':battery['envelope'],'batteryOffset':offset,'batteryScale':1,'batteryWindows':windows,'batteryEnergyUV':[3,4,6,6],'statusPanel':status,'envelope':{'from':[0,0,0],'to':[16,16,16]},'collision':'existing full cube unchanged; art does not change VoxelShape','capacity':1000,'worldDisplayImplemented':False}
(R/'source/geometry-and-uv.json').write_text(json.dumps(G,indent=2)+'\n')
E=[{'from':p['from'],'to':p['to'],'faces':{f:{'texture':'#atlas','uv':q['uv']} for f,q in p['faces'].items()}} for p in parts]
E.append({'from':status['from'],'to':status['to'],'shade':True,'faces':{'south':{'texture':'#atlas','uv':status['tiles']['0']}}})
display={'gui':{'rotation':[20,210,0],'translation':[0,0,0],'scale':[.85,.85,.85]},'ground':{'rotation':[0,0,0],'translation':[0,3,0],'scale':[.25,.25,.25]},'fixed':{'rotation':[0,180,0],'translation':[0,0,0],'scale':[.5,.5,.5]}}
model={'parent':'minecraft:block/block','ambientocclusion':True,'textures':{'atlas':'wildcraft:block/charger_atlas','particle':'wildcraft:block/charger_atlas'},'elements':E,'display':display};(R/'exports/charger-static-model.json').write_text(json.dumps(model,indent=2)+'\n');(R/'exports/charger-presentation.json').write_text(json.dumps({'asset':'F04-03','version':'v01','status':'draft_pending_review','integrated':False,'batteryOffset':offset,'batteryScale':1,'batteryWindows':windows,'batteryEnergyUV':G['batteryEnergyUV'],'statusPanel':status,'worldStateNeedsSynchronization':True,'sixNeighborSourceDirections':['down','up','north','south','west','east'],'noBatteryBakedIntoStaticModel':True},indent=2)+'\n')
bbe=[{'name':p['name'],'uuid':str(uuid.uuid4()),'type':'cube','from':p['from'],'to':p['to'],'origin':[8,8,8],'box_uv':False,'autouv':0,'faces':{f:{'texture':0,'uv':[v*4 for v in q['uv']]} for f,q in p['faces'].items()}} for p in parts]
bbe.append({'name':'status_neutral_plane','uuid':str(uuid.uuid4()),'type':'cube','from':status['from'],'to':status['to'],'box_uv':False,'faces':{'south':{'texture':0,'uv':[v*4 for v in status['tiles']['0']]}}})
bb={'meta':{'format_version':'4.10','model_format':'java_block','box_uv':False},'name':'Wildcraft charger F04-03 v01','resolution':{'width':64,'height':64},'elements':bbe,'outliner':[p['uuid'] for p in bbe],'textures':[{'name':'charger-atlas-64.png','id':'0','uuid':str(uuid.uuid4()),'width':64,'height':64,'uv_width':64,'uv_height':64,'source':'data:image/png;base64,'+base64.b64encode((R/'exports/charger-atlas-64.png').read_bytes()).decode(),'mode':'bitmap','particle':True}],'display':display};(R/'source/charger-v01.bbmodel').write_text(json.dumps(bb,indent=2)+'\n')
print(f'充电器{len(parts)}方盒＋独立状态面，原电池16方盒按1.00比例安装。')
