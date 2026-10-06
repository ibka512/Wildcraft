"""Fixed-front one-block fabricator. Static production shell, no result or state assumptions."""
from pathlib import Path
import json,uuid,base64
R=Path(__file__).resolve().parents[1];U=json.loads((R/'source/uv-regions.json').read_text())['regions'];parts=[]
def cube(name,a,b,key,override=None):
 parts.append({'name':name,'from':a,'to':b,'faces':{f:{'texture':'#atlas','uv':U[(override or {}).get(f,key)]['uv'],'materialRegion':(override or {}).get(f,key)} for f in ['north','south','east','west','up','down']}})
for i,(x,z) in enumerate([(0,0),(13,0),(0,13),(13,13)]):cube('foot_'+str(i),[x,0,z],[x+3,1,z+3],'housing')
cube('base_plinth',[0,1,0],[16,3,15.8],'steel',{'down':'bottom','south':'trim'})
cube('closed_processing_housing',[2,3,1],[14,14,12.5],'housing',{'north':'rear','south':'front','up':'top'})
cube('left_side_panel',[1,3,1],[2,14,14],'housing',{'west':'side'});cube('right_side_panel',[14,3,1],[15,14,14],'housing',{'east':'side'})
for i,(x,z) in enumerate([(.5,.5),(13.5,.5),(.5,13.5),(13.5,13.5)]):cube('steel_corner_'+str(i),[x,3,z],[x+2,14,z+2],'steel')
cube('roof_cap',[.5,14,.5],[15.5,15.5,15.5],'steel',{'up':'top','south':'trim'});cube('roof_inset_plate',[3,15.5,3],[13,16,12],'top')
for x in [2,13]:cube('top_bracket_'+str(x),[x,15.5,4],[x+1,16,10],'rim')
for i,x in enumerate([3,9.5]):
 cube('material_'+str(i)+'_back',[x,11,12.5],[x+3.5,13.6,14.65],'inset')
 for label,a,b in [('left',[x,11,14.65],[x+.4,13.6,15.2]),('right',[x+3.1,11,14.65],[x+3.5,13.6,15.2]),('top',[x+.4,13.2,14.65],[x+3.1,13.6,15.2]),('bottom',[x+.4,11,14.65],[x+3.1,11.4,15.2])]:cube('material_'+str(i)+'_rim_'+label,a,b,'rim')
cube('press_stem',[7,9.6,12.5],[9,11,14.4],'rim');cube('fixed_press_head',[5.5,8.1,12.5],[10.5,9.6,14.8],'copper')
cube('assembly_floor',[3,6,12.5],[13,6.7,15.2],'steel',{'south':'trim'})
for x in [2.5,12.5]:cube('assembly_side_'+str(x),[x,6.7,12.5],[x+1,11,15],'housing',{'south':'rim'})
cube('output_back',[3.5,3.3,12.5],[12.5,5.6,12.7],'inset')
cube('output_tray',[3,3,12.5],[13,3.5,15.7],'inset',{'south':'rim'})
cube('output_lintel',[3,5.6,12.5],[13,6,15.5],'steel',{'south':'trim'})
for x in [3,12.5]:cube('output_jamb_'+str(x),[x,3.5,12.5],[x+.5,5.6,15.5],'rim')
cube('side_vent_left',[.91,6,4],[1.001,10,10],'vent');cube('side_vent_right',[14.999,6,4],[15.09,10,10],'vent')
for x in [2,13]:cube('base_copper_fastener_'+str(x),[x,1.7,15.82],[x+1,2.5,16],'copper')
cube('rear_service_hatch',[4,5,0.88],[12,11,1],'rear');cube('rear_copper_bracket',[6.5,7,0.78],[9.5,9,.88],'copper')
cube('status_back',[6.75,11,12.5],[9.25,13.6,15.2],'housing',{'south':'inset'})
status={'from':[7.05,11.5,15.211],'to':[8.95,13.1,15.211],'normal':[0,0,1],'tiles':{str(i):U[key]['uv'] for i,key in enumerate(['idle','working','complete'])},'symbols':['neutral outline','double chevron','check'],'nonEmissive':True,'source':'art reference only; existing progress/status synchronized to menu, no world snapshot'}
badges=[{'name':'input_copper_badge','from':[10.15,11.45,14.661],'to':[12.35,13.15,14.661],'uv':U['input_copper']['uv']},{'name':'input_redstone_badge','from':[3.65,11.45,14.661],'to':[5.85,13.15,14.661],'uv':U['input_redstone']['uv']}]
G={'asset':'F06-01','version':'v01','status':'draft_pending_review','adopted':False,'integrated':False,'units':'1/16 block','front':'+Z fixed south; existing block has no facing property','textureSize':[64,64],'parts':parts,'badges':badges,'statusPanel':status,'envelope':{'from':[0,0,0],'to':[16,16,16]},'collision':'existing full cube unchanged; decorative recesses do not change collision','worldDisplayImplemented':False,'movingParts':0,'randomProductVisible':False}
(R/'source/geometry-and-uv.json').write_text(json.dumps(G,indent=2)+'\n')
E=[{'from':p['from'],'to':p['to'],'faces':{f:{'texture':'#atlas','uv':q['uv']} for f,q in p['faces'].items()}} for p in parts]
for q in [*badges,{'from':status['from'],'to':status['to'],'uv':status['tiles']['0']}]:E.append({'from':q['from'],'to':q['to'],'shade':True,'faces':{'south':{'texture':'#atlas','uv':q['uv']}}})
display={'gui':{'rotation':[20,210,0],'translation':[0,0,0],'scale':[.85,.85,.85]},'ground':{'rotation':[0,0,0],'translation':[0,3,0],'scale':[.25,.25,.25]},'fixed':{'rotation':[0,180,0],'translation':[0,0,0],'scale':[.5,.5,.5]}}
model={'parent':'minecraft:block/block','ambientocclusion':True,'textures':{'atlas':'wildcraft:block/fabricator_atlas','particle':'wildcraft:block/fabricator_atlas'},'elements':E,'display':display};(R/'exports/fabricator-static-model.json').write_text(json.dumps(model,indent=2)+'\n')
(R/'exports/fabricator-presentation.json').write_text(json.dumps({'asset':'F06-01','status':'draft_pending_review','adopted':False,'integrated':False,'worldStateNeedsSynchronization':True,'statusPanel':status,'menuMapping':{'0':'idle ready','1':'idle insufficient','2':'working','3':'complete blocked output','4':'complete actual output present'},'materialsPerBatch':{'copper_ingot':4,'redstone':2},'durationLoadedWorldTicks':100,'randomResultHiddenUntilTransfer':True,'noPowerNetworkInput':True,'noNewInventoryInsertionAPI':True,'noIndependentlyPaintedItemIcons':True},indent=2)+'\n')
bbe=[]
for e in E:
 i=len(bbe);bbe.append({'name':parts[i]['name'] if i<len(parts) else ['input_copper_badge','input_redstone_badge','status_neutral_plane'][i-len(parts)],'uuid':str(uuid.uuid4()),'type':'cube','from':e['from'],'to':e['to'],'origin':[8,8,8],'box_uv':False,'autouv':0,'faces':{f:{'texture':0,'uv':[v*4 for v in q['uv']]} for f,q in e['faces'].items()}})
bb={'meta':{'format_version':'4.10','model_format':'java_block','box_uv':False},'name':'Wildcraft fabricator F06-01 v01','resolution':{'width':64,'height':64},'elements':bbe,'outliner':[p['uuid'] for p in bbe],'textures':[{'name':'fabricator-atlas-64.png','id':'0','uuid':str(uuid.uuid4()),'width':64,'height':64,'uv_width':64,'uv_height':64,'source':'data:image/png;base64,'+base64.b64encode((R/'exports/fabricator-atlas-64.png').read_bytes()).decode(),'mode':'bitmap','particle':True}],'display':display};(R/'source/fabricator-v01.bbmodel').write_text(json.dumps(bb,indent=2)+'\n')
print(f'{len(parts)} cuboids + 2 material badges + neutral status; 1-block envelope, no revealed result')
