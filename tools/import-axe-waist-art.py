"""Local-only waist axe wrist-reference adapter; leave old clips and all skin joints intact."""
import argparse
import copy
import hashlib
import importlib.util
import json
from pathlib import Path
import numpy as np

ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('combat_reader',ROOT/'tools/import-player-combat-art.py')
reader=importlib.util.module_from_spec(spec);spec.loader.exec_module(reader)

def matrix(pose):
    value=np.eye(4);value[:3,:3]=reader.rotation(pose[3:7])*np.asarray(pose[7:])[None,:];value[:3,3]=pose[:3];return value

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--write',action='store_true');parser.add_argument('--report',required=True);options=parser.parse_args()
    folder=ROOT/'art/player-animation-v1/axe-transitions/v01'
    assert json.loads((folder/'docs/APPROVAL.json').read_text())['adopted'] is True
    metadata=json.loads((folder/'exports/transition-data.json').read_text());assert metadata['bones']==22 and metadata['gripLocalOffset']==[0,0,-.0625]
    target=ROOT/'src/main/resources/assets/wildcraft/animation';prepared=[];rows=[]
    for variant in ['standard','slim']:
        path=target/f'{variant}-combat-rig.json';data=json.loads(path.read_text());names=data['bones']
        export=reader.Export(folder/'exports'/f'{variant}-axe-transitions.glb');assert len(export.joints)==22
        selected=[f'{phase}_waist_{equipment}_{side}' for phase in ['draw','stow'] for equipment in ['empty','shield'] for side in ['R','L']]
        clips=export.clips(selected,names,[matrix(p) for p in data['bindWorld']],weapon_reference='axe_reference')
        for name,clip in clips.items():
            original=copy.deepcopy(clip);reader.reanchor_transition_palms(clip,names)
            side='right' if name.endswith('_R') else 'left';reference=names.index(side+'_sword_reference');palm=names.index(side+'_palm_grip');wrist=names.index(side+'_wrist');raw_max=0
            for frame,raw in zip(clip['frames'],original['frames']):
                wanted=matrix(frame[wrist])@np.array([0,.0625,0,1]);raw_max=max(raw_max,float(np.linalg.norm(wanted[:3]-raw[reference][:3])))
                # Source waist's independently interpolated auxiliary reference drifts from
                # its declared wrist grip. Correct translation only; never move skin or axes.
                frame[reference][:3]=frame[palm][:3]
                assert frame[:22]==raw[:22] and frame[reference][3:]==raw[reference][3:]
                assert np.linalg.norm(wanted[:3]-frame[reference][:3])<1e-6
            clip['loop']=False;key='axe_equipment_'+name
            if key in data['clips']:assert data['clips'][key]==clip
            data['clips'][key]=clip
            rows.append({'variant':variant,'clip':key,'rawStoredSampleGripGap':raw_max,'source22SkinJointsUnchanged':True,'referenceRotationScaleUnchanged':True,'adapter':'actual declared wrist+.0625 translation; native model scale unchanged','runtimeProved':False})
        assert len([k for k in data['clips'] if k not in {'axe_equipment_'+n for n in selected}])==345
        prepared.append((path,data,export.path))
    if options.write:
        provenance_path=target/'provenance.json';provenance=json.loads(provenance_path.read_text())
        for path,data,source in prepared:
            path.write_text(json.dumps(data,separators=(',',':'))+'\n')
            entry={'source':str(source.relative_to(ROOT)),'sha256':hashlib.sha256(source.read_bytes()).hexdigest(),'use':'C6AU bounded visible-cape unarmored axe waist source; only auxiliary weapon translation reanchored to declared wrist+.0625, source22 skin joints and weapon rotation/scale unchanged; old raw waist drift remains documented; runtime status in docs/C6AU-AXE-WAIST-VERIFICATION.md','excluded':'raw uncorrected waist grip, reference equipment meshes/textures, wings/armor/other weapons/formal first-person/exact anatomical handoff'}
            if entry not in provenance['sources']:provenance['sources'].append(entry)
        provenance_path.write_text(json.dumps(provenance,ensure_ascii=False,indent=2)+'\n')
    out=Path(options.report);out.parent.mkdir(parents=True,exist_ok=True);out.write_text(json.dumps({'localOnly':True,'runtimeProved':False,'rows':rows},indent=2)+'\n')
    print('PASS',len(rows),'derived source clips; old345 and skin joints preserved; raw failure not promoted; runtime proof pending')

if __name__=='__main__':main()
