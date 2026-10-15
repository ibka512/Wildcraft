"""Read-only qualification of adopted moving-guard sources; actual blocking/terrain remains a runtime gate."""
import argparse, hashlib, importlib.util, json
from pathlib import Path
import numpy as np
ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('shield_walk_reader',ROOT/'tools/import-player-combat-art.py')
reader=importlib.util.module_from_spec(spec);spec.loader.exec_module(reader)
DISTANCES={'forward':.6,'backward':.4,'left':.32,'right':.32}
def main():
 p=argparse.ArgumentParser();p.add_argument('--output',required=True);args=p.parse_args()
 folder=ROOT/'art/player-animation-v1/shield-move/v01'
 assert json.loads((folder/'docs/APPROVAL.json').read_text())['adopted'] is True
 meta=json.loads((folder/'exports/shield-move-data.json').read_text())
 assert meta['rootMotion'] is False and meta['inputLocked'] is False
 expected={direction+phase+'_guard_'+side for direction in DISTANCES for phase in ['', 'In','Out'] for side in ['R','L']}
 rows=[]
 for variant,label in [('standard','Standard'),('slim','Slim')]:
  path=folder/'exports'/f'{variant}-shield-move.glb';e=reader.Export(path)
  bones=[e.nodes[i]['name'].split('/')[-1] for i in e.joints];assert len(bones)==22
  assert len(e.meshes())==12
  inverse=e.array(e.data['skins'][0]['inverseBindMatrices'])
  bind_error=max(float(np.max(np.abs(e.rest[i]@inverse[i].reshape(4,4).T-np.eye(4)))) for i in range(22));assert bind_error<1e-5
  animations={a['name']:a for a in e.data['animations']};assert set(animations)==expected==set(meta['variants'][label]['clips'])
  clips=[]
  for name,a in animations.items():
   direction=next(d for d in DISTANCES if name.startswith(d));assert abs(meta['directions'][direction]['distanceBlocks']-DISTANCES[direction])<1e-6
   tracks=e.tracks(a);duration=max(float(t[2][-1]) for t in tracks);data=meta['variants'][label]['clips'][name];loop=name==direction+'_guard_'+name[-1]
   assert abs(duration-(1.2 if loop else .24 if direction in ['left','right'] else .18))<1e-6 and data['loop']==loop
   root_error=0
   for t in np.linspace(0,duration,round(duration*60)+1):
    worlds=e.world(tracks,float(t));assert all(np.isfinite(m).all() for m in worlds)
    root_error=max(root_error,float(np.max(np.abs(worlds[0]-e.rest[0]))))
   assert root_error<1e-5
   if loop:assert max(float(np.max(np.abs(a-b))) for a,b in zip(e.world(tracks,0),e.world(tracks,duration)))<1e-4
   planted=0;foot_error=0
   for frame in data['frames']:
    worlds=e.world(tracks,frame['time'])
    for side in ['left','right']:
     foot=frame['footContact'][side];assert np.isfinite(foot['anchor']).all()
     if foot['planted']:
      planted+=1;assert abs(foot['anchor'][2]-.125)<1e-5
      index=next(i for i,n in enumerate(e.palette) if e.nodes[n]['name'].split('/')[-1]==side+'_foot')
      # Reader outputs source geometry in x/up/forward coordinates.
      target=np.array([foot['anchor'][0],foot['anchor'][2],-foot['anchor'][1]])
      foot_error=max(foot_error,float(np.max(np.abs(worlds[index][:3,3]-target))))
   assert planted>0 and foot_error<1e-5,(name,planted,foot_error)
   clips.append({'clip':name,'duration':duration,'loop':loop,'distancePerCycle':DISTANCES[direction],'rootDeviation':root_error,'plantedSamples':planted,'footAnchorError':foot_error})
  rows.append({'source':str(path.relative_to(ROOT)),'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'skinBones':22,'skinMeshes':12,'bindInverseError':bind_error,'clips':clips})
 out=Path(args.output);out.parent.mkdir(parents=True,exist_ok=True);out.write_text(json.dumps({'localOnly':True,'scope':'Source only: actual offhand use/block/wear/Fuse, cardinal native slowdown/distance phase, real flat contact, stops/blocked/cancellations and moving parry delta remain runtime gates','variants':rows},ensure_ascii=False,indent=2)+'\n')
 print('PASS moving-guard sources: 2 variants/48 clips, fixed roots, native-scale skin binds and flat foot anchors; runtime pending')
if __name__=='__main__':main()
