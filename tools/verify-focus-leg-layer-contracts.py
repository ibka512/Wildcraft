"""Read-only adopted focus leg layer qualification, not a world-rate/gameplay claim."""
import argparse,hashlib,importlib.util,json
from pathlib import Path
import numpy as np
ROOT=Path(__file__).resolve().parents[1]
s=importlib.util.spec_from_file_location('focus_layer_reader',ROOT/'tools/import-player-combat-art.py');r=importlib.util.module_from_spec(s);s.loader.exec_module(r)
def matrix(values):
 m=np.eye(4);m[:3,:3]=r.rotation(values[3:7])@np.diag(values[7:]);m[:3,3]=values[:3];return m
def turn(axis,angle):
 axis=axis/np.linalg.norm(axis);half=angle/2;return r.rotation([* (axis*np.sin(half)),np.cos(half)])
def apply(baseline,names,weight):
 result=[m.copy()for m in baseline];axis=baseline[names.index('pelvis')][:3,:3]@np.array([1,0,0]);tilt=turn(axis,np.deg2rad(3*weight));calf=turn(axis,np.deg2rad(2*weight));lower=calf@tilt
 for side in ['right','left']:
  ids={n:names.index(side+'_'+n)for n in ['thigh','shin','foot','knee_deform','ankle_deform']};hip=baseline[ids['thigh']][:3,3];knee=baseline[ids['shin']][:3,3];ankle=baseline[ids['foot']][:3,3]
  positions={'thigh':hip,'shin':hip+tilt@(knee-hip)};positions['foot']=positions['shin']+lower@(ankle-knee)
  for name in ['thigh','shin','foot']:
   result[ids[name]][:3,3]=positions[name];result[ids[name]][:3,:3]=(tilt if name=='thigh' else lower)@baseline[ids[name]][:3,:3]
  for helper,a,b in [('knee_deform','thigh','shin'),('ankle_deform','shin','foot')]:
   qa=r.quaternion(result[ids[a]][:3,:3]);qb=r.quaternion(result[ids[b]][:3,:3]);qb=qb if np.dot(qa,qb)>=0 else -qb
   result[ids[helper]][:3,3]=positions[b];result[ids[helper]][:3,:3]=r.rotation(qa+qb)
 return result
def main():
 p=argparse.ArgumentParser();p.add_argument('--output',required=True);options=p.parse_args();folder=ROOT/'art/player-animation-v1/link-time/v01';assert json.loads((folder/'docs/APPROVAL.json').read_text())['adopted'] is True
 config=json.loads((folder/'exports/focus-layer.json').read_text());mask={side+'_'+n for side in ['right','left']for n in ['thigh','shin','foot','knee_deform','ankle_deform']};assert set(config['bodyLayer']['mask'])==mask and config['enterSeconds']==.16 and config['exitSeconds']==.2
 assert config['bodyLayer']['thighBackDegrees']==3 and config['bodyLayer']['shinExtraBackDegrees']==2 and config['timing']['worldExampleOnly'] is True
 keys=json.loads((folder/'exports/focus-reference-keys.json').read_text());rows=[]
 for variant,label in [('standard','Standard'),('slim','Slim')]:
  copy=folder/'exports'/f'{variant}-bow-air.glb';source=ROOT/f'art/player-animation-v1/bow-air/v01/exports/{variant}-bow-air.glb';assert copy.read_bytes()==source.read_bytes()
  data=json.loads((ROOT/f'src/main/resources/assets/wildcraft/animation/{variant}-combat-rig.json').read_text());names=data['bones'];baseline=[matrix(v)for v in data['clips']['bow_air_draw_empty_L']['frames'][-1]];max_error=0;samples=0
  for mode,clip in keys['variants'][label].items():
   for frame in clip['frames']:
    actual=apply(baseline,names,frame['weight']);samples+=1
    for name in mask:
     o=frame['origins'][name];w,x,y,z=frame['rotations'][name];expected=np.eye(4);expected[:3,3]=[o[0],o[2],-o[1]];expected[:3,:3]=r.rotation([x,z,-y,w]);max_error=max(max_error,float(np.max(np.abs(actual[names.index(name)]-expected))))
    for name,old,new in zip(names,baseline,actual):
     if name not in mask:assert np.array_equal(old,new)
  assert max_error<3e-6,(variant,max_error)
  current_samples=0;segment_error=0
  for clip_name in ['bow_air_draw_empty_L','bow_air_hold_empty_L']:
   clip=data['clips'][clip_name];first=round(clip['pullTimes'][0]*(len(clip['frames'])-1)) if clip.get('pullTimes') else 0
   for at in np.linspace(first,len(clip['frames'])-1,101).astype(int):
    moving=[matrix(v)for v in clip['frames'][at]]
    for weight in [0,.5,1]:
     actual=apply(moving,names,weight);current_samples+=1
     assert all(np.array_equal(x,y)for n,x,y in zip(names,moving,actual)if n not in mask)
     for side in ['left','right']:
      for a,b in [('thigh','shin'),('shin','foot')]:
       ia=names.index(side+'_'+a);ib=names.index(side+'_'+b);error=abs(np.linalg.norm(moving[ia][:3,3]-moving[ib][:3,3])-np.linalg.norm(actual[ia][:3,3]-actual[ib][:3,3]));segment_error=max(segment_error,float(error));assert error<1e-6
  rows.append({'variant':variant,'sourceCopySha256':hashlib.sha256(copy.read_bytes()).hexdigest(),'referenceSamples':samples,'maxConvertedReferenceError':max_error,'protectedMatrixExact':True,'currentPoseSamples':current_samples,'currentSegmentLengthError':segment_error})
 out=Path(options.output);out.parent.mkdir(parents=True,exist_ok=True);out.write_text(json.dumps({'localOnly':True,'scope':'Source layer only: reference poses and protected matrices, no new clips or physics. Original source world-example .15 excluded from gameplay; existing native lease remains owner. Renderer/envelope/current moving base/pause/exit/skin/GPU still need validation.','mask':sorted(mask),'results':rows},indent=2)+'\n');print('PASS adopted focus leg layer 380 reference frames/10 bones, zero protected changes; maxError',max(x['maxConvertedReferenceError']for x in rows),'runtime pending')
if __name__=='__main__':main()
