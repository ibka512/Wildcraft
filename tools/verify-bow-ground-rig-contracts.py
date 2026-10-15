"""Read-only adopted left-grip ground bow source probe; gameplay charge/release is not source time."""
import argparse,hashlib,importlib.util,json
from pathlib import Path
import numpy as np
ROOT=Path(__file__).resolve().parents[1]
s=importlib.util.spec_from_file_location('ground_bow_reader',ROOT/'tools/import-player-combat-art.py');r=importlib.util.module_from_spec(s);s.loader.exec_module(r)
def main():
 p=argparse.ArgumentParser();p.add_argument('--output',required=True);a=p.parse_args();folder=ROOT/'art/player-animation-v1/bow-ground/v05';assert json.loads((folder/'docs/APPROVAL.json').read_text())['adopted'] is True
 meta=json.loads((folder/'exports/bow-ground-data.json').read_text());assert meta['rootMotion'] is False;assert meta['handScope']=='left bow / right draw only'
 expected={f'bow_{phase}_empty_L' for phase in ['draw','hold','release','cancel']};rows=[]
 for variant,label in [('standard','Standard'),('slim','Slim')]:
  path=folder/'exports'/f'{variant}-bow-ground.glb';e=r.Export(path);assert len(e.joints)==22 and len(e.palette)==28
  base=r.Export(ROOT/f'art/player-animation-v1/climb-up/v02/exports/{variant}-climb-up.glb');bind_delta=max(float(np.max(np.abs(x-y)))for x,y in zip(base.rest[:22],e.rest[:22]));assert bind_delta<1e-6 and e.meshes()==base.meshes()
  names={n.get('name','').split('/')[-1]:i for i,n in enumerate(e.nodes)};animations={x['name']:x for x in e.data['animations']};assert set(animations)==expected
  inverse=e.array(e.data['skins'][0]['inverseBindMatrices']);error=max(float(np.max(np.abs(e.rest[i]@inverse[i].reshape(4,4).T-np.eye(4)))) for i in range(22));assert error<1e-5;checks=[]
  for name,animation in animations.items():
   tracks=e.tracks(animation);morphs=[t for t in tracks if t[1]=='weights'];assert all(e.nodes[t[0]].get('skin') is None and e.nodes[t[0]].get('name','').endswith('_bow') for t in morphs);tracks=[t for t in tracks if t[1]!='weights'];duration=max(float(t[2][-1]) for t in tracks);assert duration>0;root_error=grip_error=0
   for t in np.linspace(0,duration,round(duration*60)+1):
    world=e.world(tracks,float(t),[e.joints[0],names['left_bow_reference'],names['left_palm_grip']]);assert all(np.isfinite(m).all() for m in world);root_error=max(root_error,float(np.max(np.abs(world[0]-e.rest[0]))));grip_error=max(grip_error,float(np.linalg.norm(world[1][:3,3]-world[2][:3,3])))
   assert root_error<1e-5 and grip_error<.015,(name,root_error,grip_error)
   if '_hold_' in name:assert max(float(np.max(np.abs(x-y))) for x,y in zip(e.world(tracks,0),e.world(tracks,duration)))<1e-4
   preview=meta['variants'][label]['clips'][name]['frames'];progress=[float(f.get('drawProgress',0)) for f in preview]
   mapping=None
   if '_draw_' in name:
    assert all(y>=x-1e-5 for x,y in zip(progress,progress[1:]));end=progress[-1];assert end>0
    mapping={'sourceHoldProgress':end,'firstDrawSeconds':next(f['time'] for f in preview if f.get('drawProgress',0)>1e-5),'relativePullSamples':[]}
    for pull in [0,.25,.5,.75,1]:
     f=max((f for f in preview if float(f.get('drawProgress',0))<=1e-5),key=lambda f:f['time']) if pull==0 else min(preview,key=lambda f:abs(float(f.get('drawProgress',0))-pull*end));mapping['relativePullSamples'].append({'nativePull':pull,'sourceTime':f['time'],'sourceRelativeProgress':float(f.get('drawProgress',0))/end})
   checks.append({'clip':name,'actualGlbDuration':duration,'metadataPreviewDuration':meta['variants'][label]['clips'][name]['duration'],'rootDeviation':root_error,'leftBowReferenceToPalmDistance':grip_error,'referenceBowMorphTracksExcluded':[e.nodes[t[0]]['name'] for t in morphs],'sourceProgressProbe':mapping})
  rows.append({'source':str(path.relative_to(ROOT)),'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'skinBones':22,'runtimeBaseMeshExactlyEqual':True,'runtimeBaseBindDeviation':bind_delta,'bindInverseError':error,'clips':checks})
 out=Path(a.output);out.parent.mkdir(parents=True,exist_ok=True);out.write_text(json.dumps({'localOnly':True,'scope':'Source only: authored left bow/right draw, no root motion. Preview duration is not native 20-tick charge. Actual used hand, native item sprite/string progression/release/ammo/wear, native head/camera, exits/GPU and unsupported hand/air/armor remain runtime gates. Pending supplements excluded.','variants':rows},ensure_ascii=False,indent=2)+'\n');print('PASS adopted bow ground v05:2 variants/8 left-grip source clips, fixed root/real reference/palm; actual GLB timing recorded; runtime pending')
if __name__=='__main__':main()
