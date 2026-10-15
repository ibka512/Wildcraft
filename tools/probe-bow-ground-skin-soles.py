"""Check actual converted skin soles in selected draw subsegment/hold; source-only, no GPU claim."""
import argparse,hashlib,importlib.util,json
from pathlib import Path
import numpy as np
ROOT=Path(__file__).resolve().parents[1]
s=importlib.util.spec_from_file_location('sole_source_reader',ROOT/'tools/import-player-combat-art.py');r=importlib.util.module_from_spec(s);s.loader.exec_module(r)
def main():
 parser=argparse.ArgumentParser();parser.add_argument('--output',required=True);options=parser.parse_args();records=[]
 for variant in ['standard','slim']:
  path=ROOT/f'src/main/resources/assets/wildcraft/animation/{variant}-combat-rig.json';data=json.loads(path.read_text());meshes=[m for m in data['meshes']if not m['outer']and m['part']in ['left_leg','right_leg']];worst=0;assert len(meshes)==2
  for name in ['bow_ground_draw_empty_L','bow_ground_hold_empty_L']:
   clip=data['clips'][name];start=round(clip['pullTimes'][0]*(len(clip['frames'])-1)) if clip.get('pullTimes') else 0
   for i in np.linspace(start,len(clip['frames'])-1,101).astype(int):
    transforms=[]
    for a,b in zip(clip['frames'][i],data['inverseBind']):
     world=np.eye(4);world[:3,:3]=r.rotation(a[3:7])@np.diag(a[7:]);world[:3,3]=a[:3];transforms.append(world@np.asarray(b).reshape(4,4).T)
    transforms=np.asarray(transforms)
    for mesh in meshes:
     points=np.asarray(mesh['positions']).reshape(-1,3);points=np.c_[points,np.ones(len(points))];indices=np.asarray(mesh['joints']).reshape(-1,4);weights=np.asarray(mesh['weights']).reshape(-1,4)
     vertices=(np.einsum('nvij,nj->nvi',transforms[indices],points)*weights[:,:,None]).sum(axis=1);low=float(vertices[:,1].min());worst=max(worst,abs(low));assert abs(low)<.025,(variant,name,int(i),mesh['part'],low)
  records.append({'variant':variant,'runtimeSha256':hashlib.sha256(path.read_bytes()).hexdigest(),'actualSelectedDrawSubsegmentOnly':True,'holdFullLoop':True,'samplesPerClip':101,'maxActualSkinnedSoleDeviation':worst})
 out=Path(options.output);out.parent.mkdir(parents=True,exist_ok=True);out.write_text(json.dumps({'localOnly':True,'scope':'Numerical converted skin/source only, not gameplay or GPU proof. Unselected raise/step prelude explicitly excluded. Actual game tests separately verify fixed body/leg mask and flat support.','results':records},indent=2)+'\n');print('PASS selected ground draw/hold actual converted skin soles',max(x['maxActualSkinnedSoleDeviation']for x in records))
if __name__=='__main__':main()
