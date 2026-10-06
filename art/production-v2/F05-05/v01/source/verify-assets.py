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
result=dict(allPassed=True,unchangedBatteryParts=16,unchangedDimensions=True,unchangedUVPixelAreas=True,scale=1,contacts=contacts,bodyOrOtherNodeIntersections=bodyCollisions,intentionalSameNodeInterfaceOverlaps=interfaces,maxAllowedInterfaceEmbeddingBlock=.003125,topControlClearanceBlock=(5.12-frontmost)/16,bottomProtrusionBlock=-bottomY/16,provenanceHashesMatch=True,gameTested=False)
existing=json.loads((R/'review/asset-checks.json').read_text()) if (R/'review/asset-checks.json').exists() else {}
existing.update(result);dump(R/'review/asset-checks.json',existing)

print("Battery dimensions / UV / six-node contact and intersection checks passed")
