"""Fan geometry in original node-local model units; reuse approved shared atlas."""
from pathlib import Path
import json,math,uuid,base64
R=Path(__file__).resolve().parents[1];U=json.loads((R/'references/uv-regions.json').read_text())['regions'];parts=[]
for r in U.values():r['uv'][0]/=2;r['uv'][2]/=2
U['paper']={'uv':[8,0,10,4]}

def cube(name,a,b,key,override=None,group='housing'):
 x0,y0,z0=a;x1,y1,z1=b;vv=[[x0,y0,z0],[x1,y0,z0],[x1,y1,z0],[x0,y1,z0],[x0,y0,z1],[x1,y0,z1],[x1,y1,z1],[x0,y1,z1]];fm={'north':[0,1,2,3],'south':[5,4,7,6],'east':[1,5,6,2],'west':[4,0,3,7],'up':[3,2,6,7],'down':[4,5,1,0]};faces=[]
 for f,ids in fm.items():
  keyf=(override or {}).get(f,key);u0,v0,u1,v1=U[keyf]['uv'];faces.append(dict(name=f,indices=ids,uvs=[[u0,v1],[u1,v1],[u1,v0],[u0,v0]],materialRegion=keyf))
 parts.append(dict(name=name,group=group,kind='cuboid',vertices=vv,faces=faces))

K=math.sqrt(2)-1
def octagon(r):return [[r,K*r],[K*r,r],[-K*r,r],[-r,K*r],[-r,-K*r],[-K*r,-r],[K*r,-r],[r,-K*r]]
def add_prism(name,vv,key,group,overrides=None,uv_transform=None):
 faces=[]
 fm={'back':[3,2,1,0],'front':[4,5,6,7],'outer':[0,1,5,4],'side1':[1,2,6,5],'inner':[2,3,7,6],'side0':[3,0,4,7]}
 for fn,ids in fm.items():
  region=(overrides or {}).get(fn,key);u0,v0,u1,v1=U[region]['uv'];uvs=[[u0,v1],[u1,v1],[u1,v0],[u0,v0]]
  faces.append(dict(name=fn,indices=ids,uvs=uvs,materialRegion=region))
 parts.append(dict(name=name,group=group,kind='convex_wall',vertices=vv,faces=faces))
def tube(name,z0,z1,ro0,ro1,ri0,ri1,key,group='shell',inner='recess'):
 a,b,c,d=octagon(ro0),octagon(ro1),octagon(ri0),octagon(ri1)
 for i in range(8):
  j=(i+1)%8;vv=[[a[i][0],a[i][1],z0],[a[j][0],a[j][1],z0],[c[j][0],c[j][1],z0],[c[i][0],c[i][1],z0],[b[i][0],b[i][1],z1],[b[j][0],b[j][1],z1],[d[j][0],d[j][1],z1],[d[i][0],d[i][1],z1]]
  add_prism(name+'_'+str(i),vv,key,group,{'inner':inner})
def disk(name,z0,z1,r,key,group='shell'):
 xy=octagon(r);vv=[[x,y,z]for z in [z0,z1]for x,y in xy];faces=[]
 for fn,ids in [('back',list(range(8))),('front',list(range(8,16)))]+[(f'edge{i}',[i,(i+1)%8,(i+1)%8+8,i+8])for i in range(8)]:
  u0,v0,u1,v1=U[key]['uv']
  if len(ids)==4:uvs=[[u0,v1],[u1,v1],[u1,v0],[u0,v0]]
  else:uvs=[[u0+(vv[i][0]+r)/(2*r)*(u1-u0),v0+(vv[i][1]+r)/(2*r)*(v1-v0)]for i in ids]
  faces.append(dict(name=fn,indices=ids,uvs=uvs,materialRegion=key))
 parts.append(dict(name=name,group=group,kind='octagonal_end',vertices=vv,faces=faces))
# Shared root, tube, band and true hollow exhaust. Exhaust +Z, thrust -Z.
cube('root_square_mount',[-2.08,-2.08,.64],[2.08,2.08,1.12],'steel',{'north':'port'},group='mount')
for i,x in enumerate([-1,.44]):cube('root_contact_'+str(i),[x,-.8,.60],[x+.56,.8,.64],'copper',group='mount')
tube('copper_body',1.12,6.5,1.94,1.94,1.48,1.48,'copper')
tube('root_neck',1.12,1.48,2.02,1.94,1.48,1.48,'copper')
tube('steel_sleeve_band',2.10,2.40,2.04,2.04,1.94,1.94,'steel')
tube('copper_nozzle_taper',6.50,7.72,1.94,2.08,1.30,1.56,'copper',group='nozzle')
tube('copper_exhaust_lip',7.72,8,2.08,2.08,1.56,1.56,'copper',group='nozzle')
disk('dark_chamber_back',1.16,1.24,1.479,'neutral')
common=list(parts)
def sleeve(spent):
 ro,ri=octagon(1.986),octagon(1.945)
 for i in range(8):
  j=(i+1)%8;profile=[(0,5.7),(1,5.7)]if not spent else [(0,4.85+(.15 if i%2 else 0)),(.30,5.40),(.58,4.05), (1,4.9)]
  for segment in range(len(profile)-1):
   t0,e0=profile[segment];t1,e1=profile[segment+1]
   def pos(poly,t,z):return [poly[i][0]*(1-t)+poly[j][0]*t,poly[i][1]*(1-t)+poly[j][1]*t,z]
   vv=[pos(ro,t0,2.4),pos(ro,t1,2.4),pos(ri,t1,2.4),pos(ri,t0,2.4),pos(ro,t0,e0),pos(ro,t1,e1),pos(ri,t1,e1),pos(ri,t0,e0)]
   add_prism(f'paper_sleeve_{i}_{segment}',vv,'paper','sleeve')
   # Continuous paper UV across the jagged seam strips; no texture-stretch cue per segment.
   p=parts[-1]
   for f in p['faces']:
    if f['name']=='outer':f['uvs']=[[8+2*t0,4],[8+2*t1,4],[8+2*t1,4*(5.7-e1)/3.3],[8+2*t0,4*(5.7-e0)/3.3]]
variants={}
for spent,key in [(False,'loaded'),(True,'spent')]:
 parts=list(common);sleeve(spent)
 if not spent:disk('visible_propellant_face',6.52,6.64,1.299,'paper',group='charge')
 # Every wall is a convex discrete prism, so inward tube surfaces are also wound correctly.
 for p in parts:
  center=[sum(v[i]for v in p['vertices'])/len(p['vertices'])for i in range(3)]
  for f in p['faces']:
   vv=[p['vertices'][i]for i in f['indices']];a=[vv[1][i]-vv[0][i]for i in range(3)];b=[vv[2][i]-vv[0][i]for i in range(3)];normal=[a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]];fc=[sum(v[i]for v in vv)/len(vv)for i in range(3)]
   if sum(normal[i]*(fc[i]-center[i])for i in range(3))<0:f['indices'].reverse();f['uvs'].reverse()
 variants[key]=dict(parts=parts,localEnvelope={k:[fn(v[i]for p in parts for v in p['vertices'])for i in range(3)]for k,fn in [('min',min),('max',max)]})
G=dict(asset='F05-04',version='v01',status='draft_pending_review',integrated=False,units='1/16 block',origin='existing node, local +Z exhaust, local -Z thrust',texture='rocket-atlas-128x64.png',textureSize=[128,64],commonPartNames=[p['name']for p in common],variants=variants,allPartsFixed=True,consumesBattery=False,fuelDurationWorldTicks=80,prototypeBounds={'min':[-2.08,-2.08,.16],'max':[2.08,2.08,8]},exhaustMouth=[0,0,8],flameBounds={'min':[-1.28,-1.28,8],'max':[1.28,1.28,13.76]},flameColor='#FFD58B',flameAlpha=221/255,flameRule='actual active node && actual fuel > 0 && !ghost',spentCanInstall=False)
(R/'source/geometry-and-uv.json').write_text(json.dumps(G,indent=2)+'\n')
for key,geo in variants.items():
 prefix='rocket'if key=='loaded'else'spent-rocket';ex={k:v for k,v in G.items()if k!='variants'};ex.update(state=key,**geo);(R/'exports'/(prefix+'-geometry.json')).write_text(json.dumps(ex,indent=2)+'\n')
 e=[];groups={}
 for p in geo['parts']:
  uid=str(uuid.uuid4());faces={}
  for i,f in enumerate(p['faces']):faces[str(i)]={'vertices':[str(j)for j in f['indices']],'uv':{str(j):[uv[0]*8,uv[1]*4]for j,uv in zip(f['indices'],f['uvs'])},'texture':0}
  e.append(dict(name=p['name'],uuid=uid,type='mesh',vertices={str(i):v for i,v in enumerate(p['vertices'])},faces=faces,origin=[0,0,0]));groups.setdefault(p['group'],[]).append(uid)
 bb=dict(meta={'format_version':'4.10','model_format':'free','box_uv':False},name='Wildcraft '+prefix+' F05-04 v01',resolution={'width':128,'height':64},elements=e,outliner=[dict(name=k,uuid=str(uuid.uuid4()),origin=[0,0,0],children=v)for k,v in groups.items()],textures=[dict(name=G['texture'],id='0',uuid=str(uuid.uuid4()),width=128,height=64,uv_width=128,uv_height=64,source='data:image/png;base64,'+base64.b64encode((R/'exports'/G['texture']).read_bytes()).decode(),mode='bitmap')]);(R/'source'/(prefix+'-v01.bbmodel')).write_text(json.dumps(bb,indent=2)+'\n')
print('Loaded/spent fixed parts:',{k:len(v['parts'])for k,v in variants.items()},'common',len(common))
