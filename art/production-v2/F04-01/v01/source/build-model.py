"""Create an exact flat cartridge geometry, explicit atlas UV, and one dynamic window layer."""
from pathlib import Path
import json,uuid,base64
R=Path(__file__).resolve().parents[1];regions=json.loads((R/'source/uv-regions.json').read_text())['regions'];parts=[]
def cube(name,a,b,material,overrides=None):
 e={'name':name,'from':a,'to':b,'faces':{}}
 for face in ['north','south','east','west','up','down']:
  key=(overrides or {}).get(face,material);e['faces'][face]={'texture':'#atlas','uv':regions[key]['uv'],'materialRegion':key}
 parts.append(e)
# Geometry centre 8,8,8.16, envelope 6.4x7.36x1.92. Front is +Z.
cube('shell_flat_back',[4.8,4.32,7.2],[11.2,10.95,8.72],'shell',{'north':'rear','south':'front'})
cube('bottom_steel_cap',[4.8,4.32,8.72],[11.2,4.8,9.12],'rim')
cube('top_stepped_cap',[5.2,10.70,8.72],[10.8,11.36,9.12],'rim')
cube('top_cap_back',[5.2,10.95,7.2],[10.8,11.36,8.72],'rim')
cube('left_steel_rail',[4.8,4.8,8.72],[5.2,10.7,9.12],'rim')
cube('right_steel_rail',[10.8,4.8,8.72],[11.2,10.7,9.12],'rim')
cube('top_copper_contact',[6.6,11.36,7.5],[9.4,11.68,8.42],'copper')
# Recesses and steel separators. Energy fill is a forward-facing quad, no glass shader.
windows=[]
for i,y in enumerate([5.05,6.88,8.71]):
 cube('window_recess_'+str(i),[5.7,y,8.72],[10.3,y+1.4,8.8],'window')
 windows.append({'indexBottomUp':i,'from':[5.7,y,8.811],'to':[10.3,y+1.4,8.811],'direction':'south','materialRegion':'energy','fill':'height from bottom','fraction':'clamp(energy/1000*3-indexBottomUp,0,1)'})
for i,y in enumerate([6.45,8.28]):cube('separator_'+str(i),[5.2,y,8.72],[10.8,y+.43,8.92],'shell')
# The rear docking pads are recessed within the envelope, not an additional plug/fuel port.
for i,x in enumerate([6.0,8.7]):cube('rear_copper_pad_'+str(i),[x,5.4,7.19],[x+1.3,6.8,7.201],'copper')
# One notch strip and two small fasteners give a serviceable cartridge silhouette.
cube('left_fastener',[5.28,10.30,8.72],[5.65,10.65,8.85],'copper')
cube('right_fastener',[10.35,4.91,8.72],[10.72,5.26,8.85],'copper')
# Rear contact pads use a 0.01-unit offset to avoid coplanar fighting.
geo={'units':'1/16 block','front':'+Z','textureSize':[32,32],'parts':parts,'windows':windows,'envelope':{'from':[4.8,4.32,7.19],'to':[11.2,11.68,9.12],'size':[6.4,7.36,1.93]},'mountTransform':{'localUnits':'block','modelOrigin':[8,8,6.55],'scale':1/16,'rotation':'reuse current orient(node)','nodeMaxFootprint':[.4,.46],'backOffset':.04,'frontMaxOffset':(9.12-6.55)/16},'charge':{'capacity':1000,'source':'actual BatteryData / MachineRenderer.state.energy','noSeparateBatteryData':True},'display':{'firstperson_righthand':{'rotation':[0,0,0],'translation':[0,0,0],'scale':[1,1,1]},'firstperson_lefthand':{'rotation':[0,0,0],'translation':[0,0,0],'scale':[1,1,1]},'thirdperson_righthand':{'rotation':[0,0,0],'translation':[0,0,0],'scale':[1,1,1]},'thirdperson_lefthand':{'rotation':[0,0,0],'translation':[0,0,0],'scale':[1,1,1]},'gui':{'rotation':[20,210,0],'translation':[0,0,0],'scale':[1.2,1.2,1.2]},'ground':{'rotation':[0,0,0],'translation':[0,2,0],'scale':[1,1,1]},'fixed':{'rotation':[0,180,0],'translation':[0,0,0],'scale':[1,1,1]}}}
(R/'source/geometry-and-uv.json').write_text(json.dumps(geo,indent=2)+'\n')
elements=[]
for p in parts:elements.append({'from':p['from'],'to':p['to'],'faces':{f:{'texture':q['texture'],'uv':q['uv']} for f,q in p['faces'].items()}})
model={'parent':'minecraft:block/block','textures':{'atlas':'wildcraft:entity/battery','particle':'wildcraft:entity/battery'},'ambientocclusion':True,'elements':elements,'display':geo['display']}
(R/'exports/battery-static-model.json').write_text(json.dumps(model,indent=2)+'\n')
(R/'exports/battery-dynamic-windows.json').write_text(json.dumps({'capacity':1000,'texture':'battery-atlas-32.png','windows':windows,'energyUV':regions['energy']['uv'],'rendering':'flat +Z quads just above the dark recessed panel, cropped in height; no added bloom','mountTransform':geo['mountTransform']},indent=2)+'\n')
bbe=[]
for e in parts:
 bbe.append({'name':e['name'],'uuid':str(uuid.uuid4()),'type':'cube','from':e['from'],'to':e['to'],'origin':[8,8,8],'faces':{f:{'texture':0,'uv':[v*2 for v in q['uv']]} for f,q in e['faces'].items()},'box_uv':False,'autouv':0,'shade':True})
image=base64.b64encode((R/'exports/battery-atlas-32.png').read_bytes()).decode()
bb={'meta':{'format_version':'4.10','model_format':'java_block','box_uv':False},'name':'Wildcraft battery F04-01 v01','resolution':{'width':32,'height':32},'elements':bbe,'outliner':[e['uuid'] for e in bbe],'textures':[{'name':'battery-atlas-32.png','id':'0','uuid':str(uuid.uuid4()),'width':32,'height':32,'uv_width':32,'uv_height':32,'source':'data:image/png;base64,'+image,'mode':'bitmap','particle':True}],'display':geo['display']}
(R/'source/battery-v01.bbmodel').write_text(json.dumps(bb,indent=2)+'\n')
print(f'{len(parts)}方盒电池模型、动态窗口参数与内嵌纹理Blockbench源稿已导出。')
