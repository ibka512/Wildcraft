"""Local numerical proposal only: native spear grip alignment, no renderer/resource promotion."""
import argparse,importlib.util,json
from pathlib import Path
import numpy as np
ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('reader',ROOT/'tools/import-player-combat-art.py');reader=importlib.util.module_from_spec(spec);spec.loader.exec_module(reader)
def main():
    p=argparse.ArgumentParser();p.add_argument('--output',required=True);args=p.parse_args();folder=ROOT/'art/player-animation-v1/spear-transitions/v01';rows=[]
    raw_grip=np.array([.70,.30,.5,1]);raw_support=np.array([.53,.47,.5,1]);native_to_source=np.eye(4);v=1.7/np.sqrt(2);native_to_source[:3,:3]=np.array([[v,v,0],[-v,v,0],[0,0,.85]]);native_to_source[:3,3]=-native_to_source[:3,:3]@raw_grip[:3]
    for variant in ['standard','slim']:
        e=reader.Export(folder/'exports'/f'{variant}-spear-transitions.glb');e.palette += [i for i,n in enumerate(e.nodes) if n.get('name','').endswith('spear_reference') and i not in e.palette];e.rest=e.world(None,0);names=[e.nodes[i]['name'].split('/')[-1] for i in e.palette]
        for a in e.data['animations']:
            if '_back_' not in a['name']:continue
            side='right' if a['name'].endswith('R') else 'left';tracks=e.tracks(a);duration=max(float(t[2][-1]) for t in tracks);before=after=body_delta=orientation_delta=scale_delta=support_axis_error=0
            for t in np.linspace(0,duration,121):
                worlds=e.world(tracks,float(t));ref=worlds[names.index(side+'_spear_reference')];wrist=worlds[names.index(side+'_wrist')];palm=(wrist@np.array([0,.0625,0,1]))[:3];before=max(before,float(np.linalg.norm(ref[:3,3]-palm)))
                aligned=ref.copy();aligned[:3,3]=palm;native_model=aligned@native_to_source;after=max(after,float(np.linalg.norm((native_model@raw_grip)[:3]-palm)))
                orientation_delta=max(orientation_delta,float(np.max(np.abs(aligned[:3,:3]-ref[:3,:3]))));scale_delta=max(scale_delta,float(np.max(np.abs((native_model@np.linalg.inv(native_to_source))[:3,:3]-ref[:3,:3]))))
                support_axis_error=max(support_axis_error,float(np.linalg.norm((native_model@raw_support)[:3]-(aligned@np.array([0,.17*1.7*np.sqrt(2),0,1]))[:3])))
                proposed=list(worlds);proposed[names.index(side+'_spear_reference')]=aligned
                body_delta=max(body_delta,max(float(np.max(np.abs(proposed[i]-worlds[i]))) for i in range(22)))
            assert after<1e-12 and body_delta==0 and orientation_delta==0 and scale_delta<1e-12 and support_axis_error<1e-12
            rows.append({'variant':variant,'clip':a['name'],'sampled':121,'rawGripDeviation':before,'proposedNativeSpearGripError':after,'skinJointDeviation':body_delta,'referenceOrientationDeviation':orientation_delta,'nativeScaleDeviation':scale_delta,'nativeSecondaryAxisError':support_axis_error})
    out=Path(args.output);out.parent.mkdir(parents=True,exist_ok=True);out.write_text(json.dumps({'localOnly':True,'scope':'Numerical reference translation proposal and raw native spear geometry with original thirdperson1.7/1.7/.85 scale only. Actual resolved item display/local, source secondary palm distance, GPU/gameplay/renderer NOT VERIFIED. No runtime asset changed. Raw .002/.015 source failures preserved.','rows':rows},indent=2)+'\n');print('PASS proposed back16 x121 native spear grip/geometry axis, zero22-joint/orientation/scale changes; actual game item/GPU/support pending')
if __name__=='__main__':main()
