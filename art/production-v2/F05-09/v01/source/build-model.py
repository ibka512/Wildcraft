"""Fan geometry in original node-local model units; reuse approved shared atlas."""
from pathlib import Path
import json,math,uuid,base64
R=Path(__file__).resolve().parents[1];U=json.loads((R/'references/uv-regions.json').read_text())['regions'];parts=[]
for r in U.values():r["uv"][0]/=2;r["uv"][2]/=2
for key,px in {"cream":[64,0,96,32],"cloth_back":[96,32,128,64]}.items():U[key]={"uv":[px[0]/8,px[1]/4,px[2]/8,px[3]/4]}
def cube(name,a,b,key,override=None,group='housing'):
 x0,y0,z0=a;x1,y1,z1=b;vv=[[x0,y0,z0],[x1,y0,z0],[x1,y1,z0],[x0,y1,z0],[x0,y0,z1],[x1,y0,z1],[x1,y1,z1],[x0,y1,z1]];fm={'north':[0,1,2,3],'south':[5,4,7,6],'east':[1,5,6,2],'west':[4,0,3,7],'up':[3,2,6,7],'down':[4,5,1,0]};faces=[]
 for f,ids in fm.items():
  keyf=(override or {}).get(f,key);u0,v0,u1,v1=U[keyf]['uv'];faces.append(dict(name=f,indices=ids,uvs=[[u0,v1],[u1,v1],[u1,v0],[u0,v0]],materialRegion=keyf))
 parts.append(dict(name=name,group=group,kind='cuboid',vertices=vv,faces=faces))

xy=[(5.12,2.16),(4.4,2.88),(-4.4,2.88),(-5.12,2.16),(-5.12,-2.16),(-4.4,-2.88),(4.4,-2.88),(5.12,-2.16)]
v=[[x,y,z]for z in [.64,6.08]for x,y in xy];fs=[]
for i,ids in enumerate([list(range(8)),list(reversed(range(8,16)))]+[[i,(i+1)%8,(i+1)%8+8,i+8]for i in range(8)]):
 key='cloth_back'if i==0 else'cream';u0,v0,u1,v1=U[key]['uv'];uv=[[u0+(v[j][0]+5.12)/10.24*(u1-u0),v1-(v[j][1]+2.88)/5.76*(v1-v0)]for j in ids]if len(ids)>4 else[[u0,v1],[u1,v1],[u1,v0],[u0,v0]];fs.append(dict(name='face'+str(i),indices=ids,uvs=uv,materialRegion=key))
parts.append(dict(name='chamfered_canvas_float',group='fixed',kind='octagonal_prism',vertices=v,faces=fs))
cube('rear_mount_plate',[-1.72,-1.72,.32],[1.72,1.72,.64],'housing',{'north':'joint'},'fixed')
cube('central_strap_front',[-.64,-2.88,6.08],[.64,2.88,6.24],'copper',group='fixed')
cube('central_strap_back',[-.64,-2.88,.32],[.64,2.88,.64],'copper',group='fixed')
cube('central_strap_top',[-.64,2.88,.32],[.64,3.2,6.4],'copper',group='fixed')
cube('central_strap_bottom',[-.64,-3.2,.32],[.64,-2.88,6.4],'copper',group='fixed')
for i,(lo,hi)in enumerate([([-.8,-.85,6.24],[.8,-.61,6.4]),([-.8,.61,6.24],[.8,.85,6.4]),([-.8,-.61,6.24],[-.56,.61,6.4]),([.56,-.61,6.24],[.8,.61,6.4])]):cube('steel_buckle_'+str(i),lo,hi,'steel',group='fixed')
cube('buckle_tongue',[-.56,-.10,6.24],[.56,.10,6.38],'steel',group='fixed')
# Two understated stitched edge strips; do not add a second attachment strap.
for i,x in enumerate([-4.4,4.24]):cube('canvas_end_seam_'+str(i),[x,-2.1,6.08],[x+.16,2.1,6.12],'cloth_back',group='fixed')
for i,x in enumerate([-.36,.20]):cube('strap_rivet_'+str(i),[x,1.35,6.24],[x+.16,1.55,6.30],'steel',group='fixed')
for p in parts:
 center=[sum(v[i]for v in p['vertices'])/len(p['vertices'])for i in range(3)]
 for f in p['faces']:
  vs=[p['vertices'][j]for j in f['indices']];a=[vs[1][i]-vs[0][i]for i in range(3)];b=[vs[2][i]-vs[0][i]for i in range(3)];n=[a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]];fc=[sum(v[i]for v in vs)/len(vs)for i in range(3)]
  if sum(n[i]*(fc[i]-center[i])for i in range(3))<0:f['indices'].reverse();f['uvs'].reverse()
G=dict(asset='F05-09',version='v01',status='draft_pending_review',adopted=False,integrated=False,units='1/16 block',origin='existing node point; local +Z outward',textureSize=[128,64],texture='shared-fabric-machine-atlas-128x64.png',parts=parts,localEnvelope=dict(min=[-5.12,-3.2,.32],max=[5.12,3.2,6.4]),movingParts=0,consumesEnergy=False,capacityPerDevice=1.75,physicsChanged=False,collisionChanged=False,textureReuse='adopted F05-02 atlas byte exact; cream/cloth patches and machine metals',activeRule='server node active bit; passive water lift independent of enabled or energy',waterSource='machine WATER fluid height, not this visual float submersion',waterlineIsReviewOnly=True)
for name in ['source/geometry-and-uv.json','exports/buoyancy-geometry.json']:(R/name).write_text(json.dumps(G,indent=2)+'\n')
es=[]
for p in parts:
 uid=str(uuid.uuid4());es.append(dict(name=p['name'],uuid=uid,type='mesh',vertices={str(i):v for i,v in enumerate(p['vertices'])},faces={str(i):dict(vertices=[str(j)for j in f['indices']],uv={str(j):[uv[0]*8,uv[1]*4]for j,uv in zip(f['indices'],f['uvs'])},texture=0)for i,f in enumerate(p['faces'])},origin=[0,0,0]))
bb=dict(meta=dict(format_version='4.10',model_format='free',box_uv=False),name='Wildcraft buoyancy F05-09 v01',resolution=dict(width=128,height=64),elements=es,outliner=[dict(name='fixed',uuid=str(uuid.uuid4()),origin=[0,0,0],children=[e['uuid']for e in es])],textures=[dict(name=G['texture'],id='0',uuid=str(uuid.uuid4()),width=128,height=64,uv_width=128,uv_height=64,source='data:image/png;base64,'+base64.b64encode((R/'exports'/G['texture']).read_bytes()).decode(),mode='bitmap')]);(R/'source/buoyancy-v01.bbmodel').write_text(json.dumps(bb,indent=2)+'\n')
print(len(parts),'static meshes; exact adopted128x64 atlas')
