"""Import four locally authorized v01 bottom proposals; preserve upstream exports and physics."""
from pathlib import Path
import copy, hashlib, importlib.util, json, math
ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('bottom',ROOT/'tools/import-bottom-art.py')
b=importlib.util.module_from_spec(spec);spec.loader.exec_module(b)
SOURCE=ROOT/'art/review-batches/2026-10-11-机械补齐与反手弓/mechanical/exports'
ADOPTION=ROOT/'art/reviews/2026-10-21/BOTTOM-LOCAL-ADOPTION.json'
def local_turn(p):x,y,z=p;return [-y,x,z]
def body_turn(p):x,y,z=p;return [-z,y,x]
def turn(parts,fn):
 for p in parts:p['vertices']=[fn(v) for v in p['vertices']]
def extra(record,**fields):
 p=ROOT/record['target'];data=json.loads(p.read_text());data.update(fields);p.write_text(json.dumps(data,separators=(',',':'),allow_nan=False)+'\n');record['sha256']=hashlib.sha256(p.read_bytes()).hexdigest()
def main():
 approval=json.loads(ADOPTION.read_text());assert approval['status']=='authorized-local-integration'
 records=[]
 for name in ['battery','rocket','wheel','stabilizer']:
  source=SOURCE/f'{name}-bottom-proposal.json';digest=hashlib.sha256(source.read_bytes()).hexdigest();assert any(x['sha256']==digest and x['path']==str(source.relative_to(ROOT)) for x in approval['sources'])
  d=json.loads(source.read_text());parts=copy.deepcopy(d['parts']+d.get('adapters',[]));body=copy.deepcopy(d['candidateBody']);texture='rocket_atlas' if name=='rocket' else 'machine_atlas';pivot=d.get('rotationPivot')
  if name=='wheel':
   # Local Z+90 == body Y-90 under the bottom node's Rx+90. Turn the pocket only;
   # the square chassis exterior, five remaining mounts and interaction panel stay exact.
   turn(parts,local_turn)
   for p in body:
    if '/pocket_' in p['name']:turn([p],body_turn)
   pivot=local_turn(pivot)
  assert (b.RES/'textures/entity'/f'{texture}.png').is_file(),'Missing native texture mapping'
  outputs=[b.bake(name+'_bottom',parts,texture,d['textureSize'],pivot),b.bake('machine_bottom_'+name,body,'machine_atlas',[64,64],node=True)]
  if name=='battery':
   extra(outputs[0],windows=[{'index':w['indexBottomUp'],'from':[v/16 for v in w['from']],'to':[v/16 for v in w['to']]} for w in d['batteryWindows']],windowTexture='machine_atlas',windowUv=[v/16 for v in d['batteryEnergyUV']])
  if name=='rocket':
   extra(outputs[0],flameBounds=[v/16 for k in ['min','max'] for v in d['flameBounds'][k]])
   outputs.append(b.bake('spent_rocket_bottom',d['variants']['spent']['parts']+d.get('adapters',[]),texture,d['textureSize']))
  if name=='wheel':
   extra(outputs[0],rotationAxis=[1,0,0])
  for p,q in zip(d['candidateBody'],body):
   if '/pocket_' not in p['name']:assert p==q,'Other nodes/panel/decorations changed'
  # Exact model-scale, bottom world clearance and finite UV contract, independent of simulation.
  low=min(-v[2]/16 for p in parts for v in p['vertices']);assert low>=.0299,(name,low)
  records.append({'source':str(source.relative_to(ROOT)),'sourceSha256':digest,'localAdoption':str(ADOPTION.relative_to(ROOT)),'outputs':outputs,'bottomMinY':low,'adaptation':'wheel local Z+90 / body pocket Y-90 / actual-displacement rotation' if name=='wheel' else 'authored baked offset, scale 1'})
 (b.RES/'bottom-additions-provenance.json').write_text(json.dumps(records,ensure_ascii=False,indent=2)+'\n')
 print(json.dumps(records,ensure_ascii=False,indent=2))
if __name__=='__main__':main()
