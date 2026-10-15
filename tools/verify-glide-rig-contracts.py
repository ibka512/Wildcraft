"""Read-only adopted glide/canopy qualification; not a claim of native input or camera integration."""
import argparse,hashlib,importlib.util,json
from pathlib import Path
import numpy as np
ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('adopted_glb_reader',ROOT/'tools/import-player-combat-art.py')
reader=importlib.util.module_from_spec(spec);spec.loader.exec_module(reader)
PACKAGES={'glide':{'glide_enter','glide_hold','glide_exit'},'glide-turn':{'glide_hold','glide_turn_left','glide_turn_right'}}
def main():
 parser=argparse.ArgumentParser();parser.add_argument('--output',required=True);args=parser.parse_args();rows=[];names=None
 for package,expected in PACKAGES.items():
  folder=ROOT/'art/player-animation-v1'/package/'v01';assert json.loads((folder/'docs/APPROVAL.json').read_text()).get('adopted') is True
  metadata=json.loads(next((folder/'exports').glob('*data.json')).read_text());assert metadata['rootMotion'] is False
  for variant in ['standard','slim']:
   path=next((folder/'exports').glob(variant+'*.glb'));e=reader.Export(path)
   joints=[e.nodes[i]['name'].split('/')[-1] for i in e.joints];assert len(joints)==22
   if names is None:names=joints
   assert joints==names
   meshes=e.meshes();inverse=e.array(e.data['skins'][0]['inverseBindMatrices'])
   bind_error=max(float(np.max(np.abs(e.rest[i]@inverse[i].reshape(4,4).T-np.eye(4)))) for i in range(22));assert bind_error<1e-5
   bar=next(i for i,n in enumerate(e.nodes) if n.get('name','').endswith('/grip_crossbar'))
   vertices=e.array(e.data['meshes'][e.nodes[bar]['mesh']]['primitives'][0]['attributes']['POSITION'])
   center=(vertices.min(axis=0)+vertices.max(axis=0))/2
   # Same approved crossbar as ParagliderMesh: reflected native y=-7px/z=-7px.
   assert np.max(np.abs(center-np.array([0,1.9375,.4375])))<1e-6
   e.palette.append(bar);palette=[e.nodes[i]['name'].split('/')[-1] for i in e.palette]
   anchors=[palette.index(side+'_palm_grip') for side in ['left','right']]
   animations={a['name']:a for a in e.data['animations']};assert set(animations)==expected
   checks=[]
   for name,a in animations.items():
    tracks=e.tracks(a);duration=max(float(t[2][-1]) for t in tracks);root_error=0;grip_error=0;locked=0
    source=metadata['variants']['Standard' if variant=='standard' else 'Slim']['clips'][name]['frames']
    for time in np.linspace(0,duration,round(duration*30)+1):
     world=e.world(tracks,float(time));assert all(np.isfinite(m).all() for m in world)
     root_error=max(root_error,float(np.max(np.abs(world[0]-e.rest[0]))))
     authored=min(source,key=lambda f:abs(f['time']-time))
     for side,anchor,x in zip(['left','right'],anchors,[.46875,-.46875]):
      if not authored['canopyVisible'] or not authored['grips'][side]['locked']:continue
      actual=(world[anchor]@np.array([0,.0625,0,1]))[:3]
      target=(world[-1]@np.array([x,center[1],center[2],1]))[:3]
      grip_error=max(grip_error,float(np.linalg.norm(actual-target)));locked+=1
    assert root_error<1e-5,'Source must not move native entity root'
    assert grip_error<.002,f'Real crossbar/palm mismatch: {package}/{variant}/{name}: {grip_error}'
    checks.append({'clip':name,'duration':duration,'sampledRootDeviation':root_error,'declaredLockedSamples':locked,'maximumActualCrossbarGripError':grip_error})
   rows.append({'source':str(path.relative_to(ROOT)),'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'skinBones':22,'skinMeshes':len(meshes),'bindInverseError':bind_error,'clips':checks})
 target=Path(args.output);target.parent.mkdir(parents=True,exist_ok=True);target.write_text(json.dumps({'scope':'Adopted source only: real game jump/equipment, native GPU canopy, FOV/aspect, live bank and item handover still require runtime checks','localOnly':True,'variants':rows},ensure_ascii=False,indent=2)+'\n')
 print('PASS source-only glide/canopy contracts:',len(rows),'variants;',sum(len(r['clips']) for r in rows),'clips; real gameplay/camera integration pending')
if __name__=='__main__':main()
