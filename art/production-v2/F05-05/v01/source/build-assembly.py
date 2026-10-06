from pathlib import Path
import json,math,hashlib,copy,uuid,itertools
R=Path(__file__).resolve().parents[1];G=json.loads((R/'source/geometry-and-uv.json').read_text());B=json.loads((R/'references/F04-01-geometry-and-uv.json').read_text())
def dump(p,d):p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n')
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def rot(v,n):
 x,y,z=v;return [(x,z,-y),(x,-z,y),(-x,y,-z),(x,y,z),(-z,y,x),(z,y,-x)][n]
def world(v,n):return [a+b for a,b in zip(rot(v,n),G['nodes'][n]['point'])]
def bounds(p,n):
 vv=[world(v,n) for v in itertools.product(*zip(p['from'],p['to']))];return [list(map(min,zip(*vv))),list(map(max,zip(*vv)))]
def overlap(a,b):return [max(0,min(a[1][i],b[1][i])-max(a[0][i],b[0][i])) for i in range(3)]
for a,b in zip(B['parts'],G['batteryParts']):
 assert a['name']==b['name'].split('/')[1]
 for axis in range(3):assert abs((a['to'][axis]-a['from'][axis])-(b['to'][axis]-b['from'][axis]))<1e-9
 for f in a['faces']:assert all(abs(x/2-y)<1e-9 for x,y in zip(a['faces'][f]['uv'],b['faces'][f]['uv']))
contacts=[]
for i in range(2):
 p=next(p for p in G['batteryParts'] if p['name'].endswith('rear_copper_pad_'+str(i)));c=next(p for p in G['parts'] if p['name']=='node3/contact'+str(i));ov=overlap([p['from'],p['to']],[c['from'],c['to']]);assert abs(ov[0]-.3)<1e-9 and abs(ov[1]-1.4)<1e-9;assert abs(p['from'][2]-c['to'][2])<1e-9;contacts.append(dict(index=i,overlapXY=ov[:2],surfaceGap=0))
bodyCollisions=[];interfaces=[]
for n in range(6):
 for p in G['batteryParts']:
  bb=bounds(p,n)
  for q in G['parts']:
   qb=bounds(q,q['node']) if 'node' in q else [q['from'],q['to']];ov=overlap(bb,qb)
   if min(ov)>1e-8:
    record=dict(node=n,battery=p['name'],body=q['name'],overlapModelUnits=ov)
    if q.get('node')==n:interfaces.append(record)
    else:bodyCollisions.append(record)
assert not bodyCollisions,bodyCollisions
assert max(min(r['overlapModelUnits'])for r in interfaces)<=.05000001
top=[bounds(p,0) for p in G['batteryParts']];frontmost=max(b[1][2] for b in top);assert abs(frontmost-1.78)<1e-9
bottom=[bounds(p,1) for p in G['batteryParts']];bottomY=min(b[0][1] for b in bottom);assert abs(bottomY+2.55)<1e-9
provenance=json.loads((R/'references/provenance.json').read_text());assert all(sha(Path(p['source']))==sha(R/p['copy'])==p['sha256']for p in provenance)
dump(R/'review/asset-checks.json',dict(allPassed=True,unchangedBatteryParts=16,unchangedDimensions=True,unchangedUVPixelAreas=True,scale=1,contacts=contacts,bodyOrOtherNodeIntersections=bodyCollisions,intentionalSameNodeInterfaceOverlaps=interfaces,maxAllowedInterfaceEmbeddingBlock=.003125,topControlClearanceBlock=(5.12-frontmost)/16,bottomProtrusionBlock=-bottomY/16,provenanceHashesMatch=True,gameTested=False))
# Editable static assembly snapshot, current native 64px atlas. No new production battery geometry.
bb=json.loads((R/'references/F05-01-machine-body-v01.bbmodel').read_text());group={'name':'ADAPTATION battery front node scale 1.00; E1000 static snapshot','uuid':str(uuid.uuid4()),'origin':G['nodes'][3]['point'],'children':[]}
FM={'north':[0,1,2,3],'south':[5,4,7,6],'east':[1,5,6,2],'west':[4,0,3,7],'up':[3,2,6,7],'down':[4,5,1,0]}
for p in G['batteryParts']:
 x0,y0,z0=p['from'];x1,y1,z1=p['to'];vv=[[x0,y0,z0],[x1,y0,z0],[x1,y1,z0],[x0,y1,z0],[x0,y0,z1],[x1,y0,z1],[x1,y1,z1],[x0,y1,z1]];e={'name':p['name'],'uuid':str(uuid.uuid4()),'type':'mesh','vertices':{str(i):world(v,3)for i,v in enumerate(vv)},'faces':{}}
 for f,ids in FM.items():
  u0,v0,u1,v1=[v*4 for v in p['faces'][f]['uv']];coords=[(u0,v1),(u1,v1),(u1,v0),(u0,v0)];e['faces'][f]={'vertices':list(map(str,ids)),'uv':{str(k):list(c)for k,c in zip(ids,coords)},'texture':0}
 bb['elements'].append(e);group['children'].append(e['uuid'])
for w in G['batteryWindows']:
 x0,y0,z=w['from'];x1,y1,_=w['to'];vs=[[x0,y0,z],[x1,y0,z],[x1,y1,z],[x0,y1,z]];u0,v0,u1,v1=[v*4 for v in G['batteryEnergyUV']];e=dict(name='Battery energy '+str(w['indexBottomUp'])+' E1000 static',uuid=str(uuid.uuid4()),type='mesh',vertices={str(i):world(v,3)for i,v in enumerate(vs)},faces={'south':dict(vertices=['0','1','2','3'],uv={str(i):list(v)for i,v in enumerate([(u0,v1),(u1,v1),(u1,v0),(u0,v0)])},texture=0)});bb['elements'].append(e);group['children'].append(e['uuid'])
# Correct exported assembly face winding while retaining each vertex's original UV mapping.
for e in bb['elements']:
 verts=e['vertices'];center=[sum(v[i]for v in verts.values())/len(verts)for i in range(3)]
 for f in e['faces'].values():
  v=[verts[k]for k in f['vertices']];a=[v[1][i]-v[0][i]for i in range(3)];b=[v[2][i]-v[0][i]for i in range(3)];cross=[a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]];fc=[sum(p[i]for p in v)/len(v)-center[i]for i in range(3)]
  if sum(x*y for x,y in zip(cross,fc))<0:f['vertices'].reverse()
bb['outliner'].append(group);bb['name']='F05-05 adopted assets one-battery assembly scale 1.00 static E1000';dump(R/'source/battery-six-node-v01.bbmodel',bb);assert len(bb['elements'])==87
print('Original geometry / UV / contact alignment / six-direction intersections / reused input hashes checked; static assembly 87 meshes.')
