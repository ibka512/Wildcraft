"""Append adopted seven-material spear waist source; reuse runtime's explicit native wrist adapter."""
import argparse,copy,hashlib,importlib.util,json
from pathlib import Path
import numpy as np
ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('combat_reader',ROOT/'tools/import-player-combat-art.py');reader=importlib.util.module_from_spec(spec);spec.loader.exec_module(reader)
def matrix(p):
    m=np.eye(4);m[:3,:3]=reader.rotation(p[3:7])*np.asarray(p[7:])[None,:];m[:3,3]=p[:3];return m
def main():
    parser=argparse.ArgumentParser();parser.add_argument('--write',action='store_true');parser.add_argument('--report',required=True);options=parser.parse_args()
    target=ROOT/'src/main/resources/assets/wildcraft/animation';prepared=[];sources=[];rows=[]
    for variant in ['standard','slim']:
        path=target/f'{variant}-combat-rig.json';data=json.loads(path.read_text());names=data['bones'];added=set()
        for weapon in ['spear']:
            folder=ROOT/f'art/player-animation-v1/{weapon}-transitions/v01';assert json.loads((folder/'docs/APPROVAL.json').read_text())['adopted'] is True
            export=reader.Export(folder/'exports'/f'{variant}-{weapon}-transitions.glb');assert len(export.joints)==22
            selected=[f'{p}_waist_{q}_{s}' for p in ['draw','stow'] for q in ['empty','shield'] for s in ['R','L']]
            for name,clip in export.clips(selected,names,[matrix(p) for p in data['bindWorld']],weapon_reference=weapon+'_reference').items():
                raw=copy.deepcopy(clip);reader.reanchor_transition_palms(clip,names);clip['loop']=False
                assert all(frame[:22]==before[:22] for frame,before in zip(clip['frames'],raw['frames']))
                assert abs(clip['duration']-(.34 if name.startswith('draw_') else .38))<1e-5
                key=weapon+'_equipment_'+name
                if key in data['clips']:assert data['clips'][key]==clip
                data['clips'][key]=clip;added.add(key)
                rows.append({'variant':variant,'clip':key,'source22SkinJointsUnchanged':True,'rawWeaponReferenceUnmodified':True,'runtimeRequiresExistingSampledWristAdapter':True,'rawGripQualified':False,'runtimeProved':False})
            sources.append({'source':str(export.path.relative_to(ROOT)),'sha256':hashlib.sha256(export.path.read_bytes()).hexdigest(),'use':'C6AW visible-cape unarmored '+weapon+' waist candidate; existing per-sample wrist+.0625 reference adapter and native third-person grip retained, no skin/rotation/scale changes; runtime status in docs/C6AW-SPEAR-WAIST-VERIFICATION.md','excluded':'raw unadapted grip, source equipment meshes/textures, wings/armor, exact anatomical/continuous secondary handoff or formal first person'})
        assert len([k for k in data['clips'] if k not in added])==369;prepared.append((path,data))
    if options.write:
        for path,data in prepared:path.write_text(json.dumps(data,separators=(',',':'))+'\n')
        path=target/'provenance.json';p=json.loads(path.read_text())
        for entry in sources:
            if entry not in p['sources']:p['sources'].append(entry)
        path.write_text(json.dumps(p,ensure_ascii=False,indent=2)+'\n')
    out=Path(options.report);out.parent.mkdir(parents=True,exist_ok=True);out.write_text(json.dumps({'localOnly':True,'runtimeProved':False,'rows':rows},indent=2)+'\n');print('PASS',len(rows),'source candidates; prior369/geometry unchanged; raw grip failures retained; native runtime qualification pending')
if __name__=='__main__':main()
