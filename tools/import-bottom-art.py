"""Convert four adopted bottom structures without changing legacy nodes or simulation.

Only production-v2 v02 approved proposals are read; offsets are already baked by the artist.
Body pockets retain node transforms. The same mesh is used for installation ghosts.
"""
from pathlib import Path
import hashlib,json,math
ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'src/main/resources/assets/wildcraft'
TASKS=[('F05-06','spring','machine_atlas'),('F05-09','buoyancy','fabric_atlas'),('F05-02','wing','fabric_atlas'),('F05-03','fan','machine_atlas')]
POINTS=[[0,10.4,0],[0,0,0],[0,5.2,-11.2],[0,5.2,11.2],[-11.2,5.2,0],[11.2,5.2,0]]
def orient(p,n):
 x,y,z=p
 return [(x,z,-y),(x,-z,y),(-x,y,-z),(x,y,z),(-z,y,x),(z,y,-x)][n]
def quads(part,size,node=False,extended=False):
 vertices=part.get('extendedVertices',part['vertices']) if extended else part['vertices']
 if node and part.get('group')=='node':
  n=part['node'];vertices=[[a+b for a,b in zip(orient(v,n),POINTS[n])] for v in vertices]
 result=[]
 for face in part['faces']:
  ids,uv=face['indices'],face['uvs'];assert len(ids)==len(uv)
  groups=[list(range(4))] if len(ids)==4 else [[0,i,i+1,i+1] for i in range(1,len(ids)-1)]
  assert all(0<=v<=16 for pair in uv for v in pair),'Adopted exports use native 0..16 UV units, not atlas pixel coordinates'
  for group in groups:
   row=[]
   for i in group:
    row += [round(v/16,8) for v in vertices[ids[i]]] + [round(uv[i][j]/16,8) for j in range(2)]
   assert len(row)==20 and all(math.isfinite(x) for x in row)
   result.append(row)
 return result

def bake(name,parts,texture,size,pivot=None,node=False):
 rows=[]
 for part in parts:
  row={'name':part['name'],'group':part.get('group','fixed'),'quads':quads(part,size,node)}
  if 'extendedVertices' in part:
   row['extendedQuads']=quads(part,size,node,True);assert len(row['quads'])==len(row['extendedQuads'])
  rows.append(row)
 data={'texture':texture,'pivot':[x/16 for x in (pivot or [0,0,0])],'parts':rows}
 target=RES/'geometry'/f'{name}.json';target.write_text(json.dumps(data,separators=(',',':'),allow_nan=False)+'\n')
 return {'target':str(target.relative_to(ROOT)),'sha256':hashlib.sha256(target.read_bytes()).hexdigest(),'parts':len(parts),'quads':sum(len(p['quads']) for p in rows)}

def main():
 records=[]
 for task,name,texture in TASKS:
  folder=ROOT/'art/production-v2'/task/'v02';approval=json.loads((folder/'docs/APPROVAL.json').read_text());assert approval.get('adopted',approval.get('approved',False)) or approval.get('status')=='adopted'
  source=folder/'exports'/f'{name}-bottom-proposal.json';d=json.loads(source.read_text())
  parts=d['parts']+d.get('adapters',[])
  outputs=[bake(name+'_bottom',parts,texture,d['textureSize'],d.get('rotorPivot')),
           bake('machine_bottom_'+name,d['candidateBody'],'machine_atlas',[64,64],node=True)]
  print(name,[(x['parts'],x['quads']) for x in outputs])
  records.append({'source':str(source.relative_to(ROOT)),'sha256':hashlib.sha256(source.read_bytes()).hexdigest(),'approval':str((folder/'docs/APPROVAL.json').relative_to(ROOT)),'outputs':outputs})
 (RES/'bottom-art-provenance.json').write_text(json.dumps(records,ensure_ascii=False,indent=2)+'\n')
if __name__=='__main__':main()
