"""Extract only adopted skin meshes and idle/single sword clips from the local GLB exports.

This is an offline converter, not a game dependency or a general GLTF importer.
Reference equipment, diagnostic textures, combo clips and vendor files are excluded.
Initial qualification writes test resources; --runtime promotes the same format after validation.
"""
from pathlib import Path
import argparse, hashlib, json, math, struct
import numpy as np

ROOT = Path(__file__).resolve().parents[1]
DTYPES = {5120: np.dtype('i1'), 5121: np.dtype('u1'), 5122: np.dtype('<i2'),
          5123: np.dtype('<u2'), 5125: np.dtype('<u4'), 5126: np.dtype('<f4')}
WIDTHS = {'SCALAR': 1, 'VEC2': 2, 'VEC3': 3, 'VEC4': 4, 'MAT4': 16}

def rotation(q):
    x,y,z,w = np.asarray(q)/np.linalg.norm(q)
    return np.array([[1-2*(y*y+z*z),2*(x*y-z*w),2*(x*z+y*w)],
                     [2*(x*y+z*w),1-2*(x*x+z*z),2*(y*z-x*w)],
                     [2*(x*z-y*w),2*(y*z+x*w),1-2*(x*x+y*y)]])

def quaternion(m):
    trace = np.trace(m)
    if trace > 0:
        s=math.sqrt(trace+1)*2
        q=np.array([(m[2,1]-m[1,2])/s,(m[0,2]-m[2,0])/s,(m[1,0]-m[0,1])/s,s/4])
    else:
        i=int(np.argmax(np.diag(m)));j=(i+1)%3;k=(i+2)%3
        s=math.sqrt(1+m[i,i]-m[j,j]-m[k,k])*2
        q=np.zeros(4);q[i]=s/4;q[j]=(m[j,i]+m[i,j])/s;q[k]=(m[k,i]+m[i,k])/s;q[3]=(m[k,j]-m[j,k])/s
    return q/np.linalg.norm(q)

def pose(m):
    scale=np.linalg.norm(m[:3,:3],axis=0)
    return [*m[:3,3],*quaternion(m[:3,:3]/scale),*scale]

class Export:
    def __init__(self,path):
        self.path=path;raw=path.read_bytes();magic,version,size=struct.unpack_from('<4sII',raw)
        assert magic==b'glTF' and version==2 and size==len(raw)
        length,kind=struct.unpack_from('<II',raw,12);assert kind==0x4e4f534a
        self.data=json.loads(raw[20:20+length]);at=20+length;self.binary=None
        while at<len(raw):
            length,kind=struct.unpack_from('<II',raw,at);at+=8
            if kind==0x004e4942:self.binary=raw[at:at+length]
            at+=length
        self.nodes=self.data['nodes'];self.parents={child:i for i,n in enumerate(self.nodes) for child in n.get('children',[])}
        self.joints=self.data['skins'][0]['joints']
        assert len(self.joints) in (20,22),'Only adopted player skeleton versions are accepted'
        self.anchors=[i for i,n in enumerate(self.nodes) if any(n.get('name','').endswith(x) for x in ['palm_grip','sword_reference','axe_reference','trident_reference','spear_reference','mace_reference','crossbow_reference','shield_mount'])]
        # Bow-only exports use actual bow references in the existing weapon slots.
        node_names={n.get('name','').split('/')[-1] for n in self.nodes}
        self.anchors += [i for i,n in enumerate(self.nodes) if n.get('name','').endswith('bow_reference') and n['name'].split('/')[-1].replace('bow_reference','sword_reference') not in node_names]
        self.palette=self.joints+self.anchors
        self.rest=self.world(None,0)

    def array(self,index):
        a=self.data['accessors'][index];v=self.data['bufferViews'][a['bufferView']];dt=DTYPES[a['componentType']];width=WIDTHS[a['type']]
        assert 'sparse' not in a
        arr=np.ndarray((a['count'],width),dtype=dt,buffer=self.binary,
                       offset=v.get('byteOffset',0)+a.get('byteOffset',0),strides=(v.get('byteStride',dt.itemsize*width),dt.itemsize))
        assert np.isfinite(arr).all()
        return arr.copy()

    def tracks(self,animation):
        rows=[]
        for c in animation['channels']:
            s=animation['samplers'][c['sampler']]
            assert s.get('interpolation','LINEAR') in ['LINEAR','STEP']
            rows.append((c['target']['node'],c['target']['path'],self.array(s['input'])[:,0],self.array(s['output']),s.get('interpolation','LINEAR')))
        return rows

    def world(self,tracks,time,nodes=None):
        trs={i:[n.get('translation',[0,0,0]),n.get('rotation',[0,0,0,1]),n.get('scale',[1,1,1])] for i,n in enumerate(self.nodes)}
        for node,path,times,values,interp in tracks or []:
            if path=='weights':
                # Reference bow morphs are not player skin; the actual native bow keeps its own pull model.
                reference=self.nodes[node]
                if reference.get('skin') is not None or not reference.get('name','').endswith('_bow'):raise ValueError('Unsupported player/unknown morph track '+reference.get('name',''))
                continue
            slot={'translation':0,'rotation':1,'scale':2}[path]
            j=int(np.searchsorted(times,time,side='right'))
            if j==0:value=values[0]
            elif j==len(times) or interp=='STEP':value=values[j-1]
            else:
                t=(time-times[j-1])/(times[j]-times[j-1]);a=values[j-1];b=values[j]
                if path=='rotation':
                    dot=float(np.dot(a,b));b=b if dot>=0 else -b;dot=abs(dot)
                    if dot>.9995:value=a*(1-t)+b*t
                    else:
                        theta=math.acos(min(1,dot));value=(math.sin((1-t)*theta)*a+math.sin(t*theta)*b)/math.sin(theta)
                    value=value/np.linalg.norm(value)
                else:value=a*(1-t)+b*t
            trs[node][slot]=value
        worlds={}
        def calculate(i):
            if i in worlds:return worlds[i]
            node=self.nodes[i]
            if 'matrix' in node:m=np.asarray(node['matrix']).reshape(4,4).T
            else:
                t,q,s=trs[i];m=np.eye(4);m[:3,:3]=rotation(q)@np.diag(s);m[:3,3]=t
            worlds[i]=calculate(self.parents[i])@m if i in self.parents else m
            return worlds[i]
        return [calculate(i) for i in (self.palette if nodes is None else nodes)]

    def meshes(self):
        meshes=[]
        for node in self.nodes:
            if node.get('skin')!=0:continue
            part=node['extras']['part'];outer=node['extras']['layer']=='outer'
            for primitive in self.data['meshes'][node['mesh']]['primitives']:
                a=primitive['attributes'];positions=self.array(a['POSITION']);normals=self.array(a['NORMAL']);uv=self.array(a['TEXCOORD_0']);joints=self.array(a['JOINTS_0']).astype(int);weights=self.array(a['WEIGHTS_0'])
                assert np.max(joints)<len(self.joints) and np.max(np.abs(weights.sum(axis=1)-1))<1e-5
                indices=self.array(primitive['indices']).reshape(-1).astype(int);assert np.max(indices)<len(positions)
                meshes.append({'part':part,'outer':outer,'positions':positions.flatten().tolist(),'normals':normals.flatten().tolist(),'uv':uv.flatten().tolist(),'joints':joints.flatten().tolist(),'weights':weights.flatten().tolist(),'indices':indices.tolist()})
        assert len(meshes)==12
        return meshes

    def clips(self,selected,target_names=None,target_rest=None,weapon_reference=None):
        clips={}
        for a in self.data['animations']:
            if a['name'] not in selected:continue
            tracks=self.tracks(a);duration=max(float(x[2][-1]) for x in tracks)
            fps=400 if '/jump-takeoff-landing/' in str(self.path) else 120 if 'sword-regular' in str(self.path) or '/bow-ground/' in str(self.path) or '/bow-air/' in str(self.path) or '/air-attack/' in str(self.path) or '/equipment-transitions/' in str(self.path) or '/hurt-reaction/' in str(self.path) or '/bow-transitions/' in str(self.path) or '/shield-transitions/' in str(self.path) or '/axe-transitions/' in str(self.path) or '/trident-transitions/' in str(self.path) or '/spear-transitions/' in str(self.path) or '/mace-transitions/' in str(self.path) or '/crossbow-transitions/' in str(self.path) or '/trident-attacks/' in str(self.path) or '/spear-attacks/' in str(self.path) or '/mace-attacks/' in str(self.path) or '/trident-air/' in str(self.path) or '/spear-air/' in str(self.path) or '/mace-air/' in str(self.path) else 60
            count=round(duration*fps)+1;frames=[];previous=None
            for t in np.linspace(0,duration,count):
                worlds=self.world(tracks,t)
                if target_names is not None:
                    source_names=[self.nodes[i]['name'].split('/')[-1] for i in self.palette]
                    by_name=dict(zip(source_names,worlds));bind=dict(zip(target_names,target_rest));retargeted=[]
                    for name in target_names:
                        if weapon_reference and name.endswith('sword_reference'):
                            retargeted.append(by_name[name.replace('sword_reference',weapon_reference)])
                        elif name in by_name:retargeted.append(by_name[name])
                        elif name.endswith('sword_reference') and name.replace('sword_reference','axe_reference') in by_name:
                            # Existing named weapon slots store the actual adopted axe reference, never a fabricated sword.
                            retargeted.append(by_name[name.replace('sword_reference','axe_reference')])
                        elif name.endswith('sword_reference') and name.replace('sword_reference','bow_reference') in by_name:
                            retargeted.append(by_name[name.replace('sword_reference','bow_reference')])
                        elif name in ['left_wrist','right_wrist']:
                            forearm=name.replace('wrist','forearm')
                            retargeted.append(by_name[forearm]@np.linalg.inv(bind[forearm])@bind[name])
                        elif name.endswith(('sword_reference','shield_mount')) and any(tag in str(self.path) for tag in ['climb-','/glide/']):
                            # Empty-hand traversal exports omit equipment. Keep inactive compatibility anchors attached to the actual forearm.
                            forearm=name.split('_',1)[0]+'_forearm'
                            retargeted.append(by_name[forearm]@np.linalg.inv(bind[forearm])@bind[name])
                        else:raise ValueError('Missing adopted bone '+name)
                    worlds=retargeted
                frame=np.asarray([pose(m) for m in worlds])
                if previous is not None:
                    flip=np.sum(frame[:,3:7]*previous[:,3:7],axis=1)<0
                    frame[flip,3:7]*=-1
                assert np.isfinite(frame).all()
                previous=frame;frames.append(np.round(frame,7).tolist())
            clips[a['name']]={'duration':duration,'loop':a['name'] in ['idle_R','idle_L','shield_R','shield_L','hold_R','hold_L'],'frames':frames}
        assert set(clips)==set(selected)
        return clips

def mirrored_ground_bow_clip(clip,names,rest):
    """Mirror approved LEFT draw/hold only; auxiliary weapon references are physical targets, not skin binds."""
    pair=[names.index('right_'+n[5:]) if n.startswith('left_') else names.index('left_'+n[6:]) if n.startswith('right_') else i for i,n in enumerate(names)]
    reflection=np.diag([-1.,1.,1.,1.])
    # Only the 22 deforming skin joints must fit the existing symmetric bind palette.
    # Weapon references intentionally have different rest offsets/axes and do not deform the skin.
    for i in range(22):assert np.max(np.abs(reflection@rest[pair[i]]@reflection-rest[i]))<2e-5
    frames=[];previous=None
    for source in clip['frames']:
        matrices=[]
        for a in source:
            value=np.eye(4);value[:3,:3]=rotation(a[3:7])*np.array(a[7:10])[None,:];value[:3,3]=a[:3];matrices.append(value)
        frame=np.array([pose(reflection@matrices[j]@reflection) for j in pair])
        if previous is not None:
            flip=np.sum(frame[:,3:7]*previous[:,3:7],axis=1)<0;frame[flip,3:7]*=-1
        assert np.isfinite(frame).all();previous=frame;frames.append(np.round(frame,7).tolist())
    return {**clip,'frames':frames}

def reanchor_transition_palms(clip,names):
    """Derive only auxiliary palm positions from the adopted transition's explicit .0625 wrist grip."""
    for frame in clip['frames']:
        for side in ['left','right']:
            wrist=frame[names.index(side+'_wrist')];m=np.eye(4);m[:3,:3]=rotation(wrist[3:7])@np.diag(wrist[7:]);m[:3,3]=wrist[:3]
            frame[names.index(side+'_palm_grip')][:3]=(m@np.array([0,.0625,0,1]))[:3].tolist()

def main():
    args=argparse.ArgumentParser();args.add_argument('--runtime',action='store_true');args.add_argument('--climb-probe',action='store_true');args.add_argument('--climb-runtime',action='store_true');args.add_argument('--glide-runtime',action='store_true');args.add_argument('--walk-runtime',action='store_true');args.add_argument('--heavy-runtime',action='store_true');args.add_argument('--guard-pulse-runtime',action='store_true');args.add_argument('--hop-runtime',action='store_true');args.add_argument('--dodge-feedback-runtime',action='store_true');args.add_argument('--shield-walk-runtime',action='store_true');args.add_argument('--bow-idle-runtime',action='store_true');args.add_argument('--ground-bow-runtime',action='store_true');args.add_argument('--air-bow-runtime',action='store_true');args.add_argument('--focus-leg-runtime',action='store_true');args.add_argument('--air-melee-runtime',action='store_true');args.add_argument('--sword-transition-runtime',action='store_true');args.add_argument('--hurt-runtime',action='store_true');args.add_argument('--bow-transition-runtime',action='store_true');args.add_argument('--shield-transition-runtime',action='store_true');args.add_argument('--axe-transition-runtime',action='store_true');args.add_argument('--trident-transition-runtime',action='store_true');args.add_argument('--spear-transition-runtime',action='store_true');args.add_argument('--mace-transition-runtime',action='store_true');args.add_argument('--crossbow-transition-runtime',action='store_true');args.add_argument('--ground-trident-runtime',action='store_true');args.add_argument('--ground-spear-runtime',action='store_true');args.add_argument('--ground-mace-runtime',action='store_true');args.add_argument('--air-trident-runtime',action='store_true');args.add_argument('--air-spear-runtime',action='store_true');args.add_argument('--air-mace-runtime',action='store_true');args.add_argument('--jump-runtime',action='store_true');args.add_argument('--ground-bow-right-runtime',action='store_true');args.add_argument('--air-bow-right-runtime',action='store_true');options=args.parse_args()
    if options.air_bow_right_runtime and not options.ground_bow_right_runtime:args.error('--air-bow-right-runtime preserves qualified RIGHT ground source; requires --ground-bow-right-runtime')
    if options.ground_bow_right_runtime and not options.jump_runtime:args.error('--ground-bow-right-runtime preserves all previously qualified slices; requires --jump-runtime')
    if options.runtime and options.climb_probe:args.error('Climb qualification is test-only; no production promotion')
    if options.climb_runtime and not options.runtime:args.error('--climb-runtime requires explicit --runtime; local candidate resources only')
    if options.glide_runtime and not (options.runtime and options.climb_runtime):args.error('--glide-runtime requires --runtime --climb-runtime to preserve the qualified climb slice')
    if options.walk_runtime and not (options.runtime and options.climb_runtime and options.glide_runtime):args.error('--walk-runtime preserves the qualified climb/glide slices; supply all runtime flags')
    if options.heavy_runtime and not (options.runtime and options.climb_runtime and options.glide_runtime and options.walk_runtime):args.error('--heavy-runtime preserves all previously qualified source slices; supply all runtime flags')
    if options.guard_pulse_runtime and not options.heavy_runtime:args.error('--guard-pulse-runtime requires all previous runtime slices including --heavy-runtime')
    if options.hop_runtime and not options.guard_pulse_runtime:args.error('--hop-runtime requires all previously qualified slices including --guard-pulse-runtime')
    if options.dodge_feedback_runtime and not options.hop_runtime:args.error('--dodge-feedback-runtime requires all previously qualified slices including --hop-runtime')
    if options.shield_walk_runtime and not options.dodge_feedback_runtime:args.error('--shield-walk-runtime requires all previously qualified slices including --dodge-feedback-runtime')
    if options.bow_idle_runtime and not options.shield_walk_runtime:args.error('--bow-idle-runtime requires all previously qualified slices including --shield-walk-runtime')
    if options.ground_bow_runtime and not options.bow_idle_runtime:args.error('--ground-bow-runtime requires all previous qualified slices including --bow-idle-runtime')
    if options.air_bow_runtime and not options.ground_bow_runtime:args.error('--air-bow-runtime requires all previously qualified slices including --ground-bow-runtime')
    if options.focus_leg_runtime and not options.air_bow_runtime:args.error('--focus-leg-runtime requires previously qualified --air-bow-runtime')
    if options.air_melee_runtime and not options.focus_leg_runtime:args.error('--air-melee-runtime requires all prior slices including --focus-leg-runtime')
    if options.sword_transition_runtime and not options.air_melee_runtime:args.error('--sword-transition-runtime requires all prior slices including --air-melee-runtime')
    if options.hurt_runtime and not options.sword_transition_runtime:args.error('--hurt-runtime requires all prior slices including --sword-transition-runtime')
    if options.bow_transition_runtime and not options.hurt_runtime:args.error('--bow-transition-runtime requires all prior slices including --hurt-runtime')
    if options.shield_transition_runtime and not options.bow_transition_runtime:args.error('--shield-transition-runtime requires all prior slices including --bow-transition-runtime')
    if options.axe_transition_runtime and not options.shield_transition_runtime:args.error('--axe-transition-runtime requires all prior slices including --shield-transition-runtime')
    if options.trident_transition_runtime and not options.axe_transition_runtime:args.error('--trident-transition-runtime requires all prior slices including --axe-transition-runtime')
    if options.spear_transition_runtime and not options.trident_transition_runtime:args.error('--spear-transition-runtime requires all prior slices including --trident-transition-runtime')
    if options.mace_transition_runtime and not options.spear_transition_runtime:args.error('--mace-transition-runtime requires all prior slices including --spear-transition-runtime')
    if options.crossbow_transition_runtime and not options.mace_transition_runtime:args.error('--crossbow-transition-runtime requires all prior slices including --mace-transition-runtime')
    if options.ground_trident_runtime and not options.crossbow_transition_runtime:args.error('--ground-trident-runtime requires all prior slices including --crossbow-transition-runtime')
    if options.ground_spear_runtime and not options.ground_trident_runtime:args.error('--ground-spear-runtime requires all previous slices')
    if options.ground_mace_runtime and not options.ground_spear_runtime:args.error('--ground-mace-runtime requires all prior slices')
    if options.air_trident_runtime and not options.ground_mace_runtime:args.error('--air-trident-runtime requires all prior slices')
    if options.air_spear_runtime and not options.air_trident_runtime:args.error('--air-spear-runtime requires all prior slices')
    if options.air_mace_runtime and not options.air_spear_runtime:args.error('--air-mace-runtime requires all prior slices')
    if options.jump_runtime and not options.air_mace_runtime:args.error('--jump-runtime requires all prior slices')
    target=ROOT/('src/main/resources/assets/wildcraft/animation' if options.runtime else 'src/gametest/resources/assets/wildcraft-test/animation')
    target.mkdir(parents=True,exist_ok=True)
    provenance=[];outputs=[]
    for variant in ['standard','slim']:
        idle=Export(ROOT/f'art/player-animation-v1/locked-idle/v02/exports/{variant}-locked-idle.glb')
        sword=Export(ROOT/f'art/player-animation-v1/sword-regular/v02/exports/{variant}-regular-sword.glb')
        base=Export(ROOT/f'art/player-animation-v1/climb-up/v02/exports/{variant}-climb-up.glb')
        guard=Export(ROOT/f'art/player-animation-v1/shield-guard/v01/exports/{variant}-shield-guard.glb')
        assert [n['name'].split('/')[-1] for n in [idle.nodes[i] for i in idle.palette]]==[n['name'].split('/')[-1] for n in [sword.nodes[i] for i in sword.palette]]
        names=[base.nodes[i]['name'].split('/')[-1] for i in base.joints]+[idle.nodes[i]['name'].split('/')[-1] for i in idle.anchors]
        rest=base.rest[:22]+idle.rest[20:]
        inverse=base.array(base.data['skins'][0]['inverseBindMatrices'])
        data={'schema':1,'variant':variant,'bones':names,'skinBones':22,'bindWorld':[pose(x) for x in rest],'inverseBind':inverse.tolist(),'meshes':base.meshes(),'clips':{**idle.clips(['idle_R','idle_L','shield_R','shield_L'],names,rest),**sword.clips(['A_R','A_L','shield_A_R','shield_A_L'],names,rest),**guard.clips(['raise_R','raise_L','hold_R','hold_L','lower_R','lower_L'],names,rest)}}
        if options.climb_probe or options.climb_runtime:
            for package,selected in [('climb-up/v02',['climb_enter','climb_up']),('climb-side/v01',['climb_enter_left','climb_enter_right','climb_left','climb_right']),('climb-idle/v01',['climb_idle_up','climb_idle_left','climb_idle_right','climb_settle_up','climb_settle_left','climb_settle_right'])]:
                folder=ROOT/'art/player-animation-v1'/package
                assert json.loads((folder/'docs/APPROVAL.json').read_text()).get('adopted') is True
                export=Export(next((folder/'exports').glob(variant+'*.glb')))
                extracted=export.clips(selected,names,rest)
                metadata=json.loads(next((folder/'exports').glob('*data.json')).read_text())['variants']['Standard' if variant=='standard' else 'Slim']['clips']
                for name,clip in extracted.items():
                    authored=metadata[name]['frames'];times=np.asarray([f['time'] for f in authored])
                    contacts=[]
                    for t in np.linspace(0,clip['duration'],len(clip['frames'])):
                        hi=min(int(np.searchsorted(times,t,side='right')),len(times)-1);lo=max(0,hi-1)
                        mix=0 if times[hi]==times[lo] else float(np.clip((t-times[lo])/(times[hi]-times[lo]),0,1));points=[]
                        for key in ['left_hand','right_hand','left_foot','right_foot']:
                            a=authored[lo]['contacts'][key];b=authored[hi]['contacts'][key]
                            x,y,z=np.asarray(a['target'])*(1-mix)+np.asarray(b['target'])*mix
                            points.append([round(float(x),7),round(float(z),7),round(float(-y),7),1 if a['planted'] and b['planted'] else 0])
                        contacts.append(points)
                    clip['contacts']=contacts;clip['loop']=name.startswith('climb_idle_') or name in ['climb_up','climb_left','climb_right']
                data['clips'].update(extracted)
                provenance.append({'source':str(export.path.relative_to(ROOT)),'sha256':hashlib.sha256(export.path.read_bytes()).hexdigest()})
        if options.glide_runtime:
            folder=ROOT/'art/player-animation-v1/glide/v01'
            assert json.loads((folder/'docs/APPROVAL.json').read_text()).get('adopted') is True
            export=Export(next((folder/'exports').glob(variant+'*.glb')))
            extracted=export.clips(['glide_enter','glide_hold','glide_exit'],names,rest)
            bar=next(i for i,n in enumerate(export.nodes) if n.get('name','').endswith('/grip_crossbar'))
            for name,clip in extracted.items():
                animation=next(a for a in export.data['animations'] if a['name']==name);tracks=export.tracks(animation)
                # Exit hides the source canopy by zero scale; it has no renderable canopy transform.
                if name!='glide_exit':clip['canopy']=[np.round(pose(export.world(tracks,float(t),[bar])[0]),7).tolist() for t in np.linspace(0,clip['duration'],len(clip['frames']))]
                clip['loop']=name=='glide_hold'
            data['clips'].update(extracted)
            provenance.append({'source':str(export.path.relative_to(ROOT)),'sha256':hashlib.sha256(export.path.read_bytes()).hexdigest()})
            turn=ROOT/f'art/player-animation-v1/glide-turn/v01/exports/{variant}-glide-turn.glb'
            assert json.loads((turn.parent.parent/'docs/APPROVAL.json').read_text()).get('adopted') is True
            provenance.append({'source':str(turn.relative_to(ROOT)),'sha256':hashlib.sha256(turn.read_bytes()).hexdigest(),'use':'adopted live-bank/pivot/response contract; baked turn cycles are source qualification, not recurring gameplay inputs'})
        if options.walk_runtime:
            for direction in ['forward','backward','left','right']:
                folder=ROOT/'art/player-animation-v1'/('lock-'+direction)/'v01'
                assert json.loads((folder/'docs/APPROVAL.json').read_text()).get('adopted') is True
                export=Export(next((folder/'exports').glob(variant+'*.glb')));metadata=json.loads(next((folder/'exports').glob('*data.json')).read_text())
                source=metadata['variants']['Standard' if variant=='standard' else 'Slim']['clips']
                for name,clip in export.clips(list(source),names,rest).items():
                    frames=source[name]['frames'];times=np.asarray([f['time'] for f in frames]);feet=[]
                    for t in np.linspace(0,clip['duration'],len(clip['frames'])):
                        hi=min(int(np.searchsorted(times,t,side='right')),len(times)-1);lo=max(0,hi-1);mix=0 if times[hi]==times[lo] else float(np.clip((t-times[lo])/(times[hi]-times[lo]),0,1));points=[]
                        for side in ['left','right']:
                            a=frames[lo]['footContact'][side];b=frames[hi]['footContact'][side];x,y,z=np.asarray(a['anchor'])*(1-mix)+np.asarray(b['anchor'])*mix
                            points.append([round(float(x),7),round(float(z),7),round(float(-y),7),1 if a['planted'] and b['planted'] else 0])
                        feet.append(points)
                    clip['feet']=feet;clip['distance']=metadata['cycleDistanceBlocks'];clip['loop']=name.startswith(direction+'_')
                    phase='cycle' if clip['loop'] else name.split('_',1)[0];suffix=name.split('_',1)[1]
                    data['clips']['walk_'+direction+'_'+phase+'_'+suffix]=clip
                provenance.append({'source':str(export.path.relative_to(ROOT)),'sha256':hashlib.sha256(export.path.read_bytes()).hexdigest()})
        if options.heavy_runtime:
            for package,phases,prefix in [('sword-heavy/v01',['charge','hold','release','cancel'],'sword_heavy'),('axe-light/v02',['attack'],'axe_light'),('axe-heavy/v01',['charge','hold','release','cancel'],'axe_heavy')]:
                folder=ROOT/'art/player-animation-v1'/package;assert json.loads((folder/'docs/APPROVAL.json').read_text()).get('adopted') is True
                export=Export(next((folder/'exports').glob(variant+'*.glb')))
                selected=[phase+'_'+equipment+'_'+side for phase in phases for equipment in ['empty','carry'] for side in ['R','L']]
                for name,clip in export.clips(selected,names,rest).items():clip['loop']=name.startswith('hold_');data['clips'][prefix+'_'+name]=clip
                record={'source':str(export.path.relative_to(ROOT)),'sha256':hashlib.sha256(export.path.read_bytes()).hexdigest()}
                if package.startswith('axe'):record['referenceAliases']={'left_sword_reference':'actual left_axe_reference','right_sword_reference':'actual right_axe_reference'}
                provenance.append(record)
        if options.guard_pulse_runtime:
            folder=ROOT/'art/player-animation-v1/perfect-guard/v01';assert json.loads((folder/'docs/APPROVAL.json').read_text()).get('adopted') is True
            export=Export(next((folder/'exports').glob(variant+'*.glb')))
            for name,clip in export.clips(['pulse_guard_R','pulse_guard_L'],names,rest).items():clip['loop']=False;data['clips']['guard_pulse_'+name[-1]]=clip
            provenance.append({'source':str(export.path.relative_to(ROOT)),'sha256':hashlib.sha256(export.path.read_bytes()).hexdigest(),'use':'active shield arm delta only over current qualified guard body; preview cycle excluded'})
        if options.hop_runtime:
            for package,directions in [('back-hop/v01',['hop']),('side-hop/v01',['left','right'])]:
                folder=ROOT/'art/player-animation-v1'/package;assert json.loads((folder/'docs/APPROVAL.json').read_text()).get('adopted') is True
                export=Export(next((folder/'exports').glob(variant+'*.glb')))
                selected=[direction+'_'+equip+'_'+arm for direction in directions for equip in ['empty','carry'] for arm in ['R','L']]
                for name,clip in export.clips(selected,names,rest).items():clip['loop']=False;data['clips']['dodge_'+name.replace('hop_','back_',1)]=clip
                provenance.append({'source':str(export.path.relative_to(ROOT)),'sha256':hashlib.sha256(export.path.read_bytes()).hexdigest(),'use':'actual six-world-tick hop; empty/carry only because native hop releases defense; no preview displacement/cycles/guard or forced planted feet'})
        if options.dodge_feedback_runtime:
            folder=ROOT/'art/player-animation-v1/perfect-dodge/v01';assert json.loads((folder/'docs/APPROVAL.json').read_text()).get('adopted') is True
            export=Export(next((folder/'exports').glob(variant+'*.glb')))
            selected=['pulse-'+direction+'_'+equip+'_'+arm for direction in ['left','right','back'] for equip in ['empty','carry'] for arm in ['R','L']]
            for name,clip in export.clips(selected,names,rest).items():clip['loop']=False;data['clips']['dodge_feedback_'+name.split('-',1)[1]]=clip
            provenance.append({'source':str(export.path.relative_to(ROOT)),'sha256':hashlib.sha256(export.path.read_bytes()).hexdigest(),'use':'actual native successful ReactionView serial; independent .24 effective-real-second upper-body delta only; preview normal/cycles/guard and root movement excluded'})
        if options.shield_walk_runtime:
            folder=ROOT/'art/player-animation-v1/shield-move/v01';assert json.loads((folder/'docs/APPROVAL.json').read_text()).get('adopted') is True
            export=Export(folder/'exports'/f'{variant}-shield-move.glb');metadata=json.loads((folder/'exports/shield-move-data.json').read_text());source=metadata['variants']['Standard' if variant=='standard' else 'Slim']['clips']
            for name,clip in export.clips(list(source),names,rest).items():
                direction=next(d for d in ['forward','backward','left','right'] if name.startswith(d));phase='enter' if name.startswith(direction+'In_') else 'exit' if name.startswith(direction+'Out_') else 'cycle'
                frames=source[name]['frames'];times=np.asarray([f['time'] for f in frames]);feet=[]
                for t in np.linspace(0,clip['duration'],len(clip['frames'])):
                    hi=min(int(np.searchsorted(times,t,side='right')),len(times)-1);lo=max(0,hi-1);mix=0 if times[hi]==times[lo] else float(np.clip((t-times[lo])/(times[hi]-times[lo]),0,1));points=[]
                    for side in ['left','right']:
                        a=frames[lo]['footContact'][side];b=frames[hi]['footContact'][side];x,y,z=np.asarray(a['anchor'])*(1-mix)+np.asarray(b['anchor'])*mix
                        points.append([round(float(x),7),round(float(z),7),round(float(-y),7),1 if a['planted'] and b['planted'] else 0])
                    feet.append(points)
                clip['feet']=feet;clip['distance']=metadata['directions'][direction]['distanceBlocks'];clip['loop']=phase=='cycle'
                data['clips']['walk_'+direction+'_'+phase+'_guard_'+name[-1]]=clip
            provenance.append({'source':str(export.path.relative_to(ROOT)),'sha256':hashlib.sha256(export.path.read_bytes()).hexdigest(),'use':'actual offhand shield use and cardinal native displacement; original shield slowdown/block/wear remain authoritative'})
        if options.bow_idle_runtime:
            folder=ROOT/'art/player-animation-v1/bow-hold/v04';assert json.loads((folder/'docs/APPROVAL.json').read_text()).get('adopted') is True
            export=Export(folder/'exports'/f'{variant}-bow-hold.glb')
            selected=['bow_idle_'+equip+'_'+arm for equip in ['empty','carry'] for arm in ['R','L']]
            for name,clip in export.clips(selected,names,rest).items():clip['loop']=True;data['clips'][name]=clip
            provenance.append({'source':str(export.path.relative_to(ROOT)),'sha256':hashlib.sha256(export.path.read_bytes()).hexdigest(),'use':'grounded stationary actual mainhand bow idle only; no use/draw/air/first-person assets promoted','referenceAliases':{'left_sword_reference':'actual left_bow_reference','right_sword_reference':'actual right_bow_reference'}})
        if options.ground_bow_runtime:
            folder=ROOT/'art/player-animation-v1/bow-ground/v05';assert json.loads((folder/'docs/APPROVAL.json').read_text()).get('adopted') is True
            export=Export(folder/'exports'/f'{variant}-bow-ground.glb');mapping=json.loads((ROOT/'development-assets/local-only/dev41/bow-ground-pull-mapping-probe.json').read_text())
            record=next(row for row in mapping['variants'] if row['source']==str(export.path.relative_to(ROOT)));assert record['sha256']==hashlib.sha256(export.path.read_bytes()).hexdigest()
            assert record['tableInterpolationMaxRelativeError']<.005
            for name,clip in export.clips(['bow_draw_empty_L','bow_hold_empty_L'],names,rest).items():
                clip['loop']='_hold_' in name
                if '_draw_' in name:clip['pullTimes']=[row['sourceSeconds']/clip['duration'] for row in record['nativePullToSourceTime']]
                data['clips']['bow_ground_'+name.removeprefix('bow_')]=clip
            provenance.append({'source':str(export.path.relative_to(ROOT)),'sha256':hashlib.sha256(export.path.read_bytes()).hexdigest(),'use':'left-grip actual-use draw/hold source; geometry pull curve and native20tick charge, original native bow sprite and fixed-length drawing-arm adaptation; runtime status in C6P-GROUND-BOW-VERIFICATION','referenceAliases':{'left_sword_reference':'actual left_bow_reference','right_sword_reference':'actual right_bow_reference'},'excluded':'reference bow mesh/morph tracks, preview cameras, source long release/cancel; source conversion alone is not gameplay proof'})
        if options.air_bow_runtime:
            folder=ROOT/'art/player-animation-v1/bow-air/v01';assert json.loads((folder/'docs/APPROVAL.json').read_text()).get('adopted') is True
            export=Export(folder/'exports'/f'{variant}-bow-air.glb');mapping=json.loads((ROOT/'development-assets/local-only/dev42/bow-air-pull-mapping-probe.json').read_text())
            record=next(row for row in mapping['variants'] if row['source']==str(export.path.relative_to(ROOT)));assert record['sha256']==hashlib.sha256(export.path.read_bytes()).hexdigest() and record['tableInterpolationMaxRelativeError']<.005
            for name,clip in export.clips(['bow_draw_empty_L','bow_hold_empty_L'],names,rest).items():
                clip['loop']='_hold_' in name
                if '_draw_' in name:clip['pullTimes']=[row['sourceSeconds']/clip['duration'] for row in record['nativePullToSourceTime']]
                data['clips']['bow_air_'+name.removeprefix('bow_')]=clip
            provenance.append({'source':str(export.path.relative_to(ROOT)),'sha256':hashlib.sha256(export.path.read_bytes()).hexdigest(),'use':'actual left used-arm airborne draw/hold source; native charge, physics and FocusTime unchanged; runtime status in C6Q-AIR-BOW-VERIFICATION','referenceAliases':{'left_sword_reference':'actual left_bow_reference','right_sword_reference':'actual right_bow_reference'},'excluded':'previewLift/root translation, reference bow meshes/morphs, preview cameras, long source release/cancel'})
        if options.air_melee_runtime:
            folder=ROOT/'art/player-animation-v1/air-attack/v01';assert json.loads((folder/'docs/APPROVAL.json').read_text())['adopted'] is True
            export=Export(next((folder/'exports').glob(variant+'*.glb')))
            for weapon in ['sword','axe']:
                selected=[weapon+'_attack_'+equip+'_'+side for equip in ['empty','carry'] for side in ['R','L']]
                for name,clip in export.clips(selected,names,rest,weapon+'_reference').items():
                    clip['loop']=False;data['clips'][name.replace('_attack_','_air_attack_',1)]=clip
            provenance.append({'source':str(export.path.relative_to(ROOT)),'sha256':hashlib.sha256(export.path.read_bytes()).hexdigest(),'use':'ordinary actual AIR_SWING sword/axe only; original recovery/world physics and native attack remain authoritative; runtime status in C6S verification','excluded':'preview lift, air idle/cycle, charged/drop and long weapons'})
        if options.sword_transition_runtime:
            folder=ROOT/'art/player-animation-v1/equipment-transitions/v01';approval=json.loads((folder/'docs/APPROVAL.json').read_text());assert approval['date']=='2026-10-08' and approval['quote'].startswith('采用')
            export=Export(folder/'exports'/f'{variant}-equipment-transitions.glb')
            selected=[phase+'_back_'+equip+'_'+side for phase in ['draw','stow'] for equip in ['empty','shield'] for side in ['R','L']]
            for name,clip in export.clips(selected,names,rest).items():
                clip['loop']=False;data['clips']['equipment_'+name]=clip
            provenance.append({'source':str(export.path.relative_to(ROOT)),'sha256':hashlib.sha256(export.path.read_bytes()).hexdigest(),'use':'bounded same-reference native sword draw/stow at current instant hand/back ownership; existing .34/.38 actual-time Settle clock, default-back only; qualification is not runtime proof','excluded':'reference equipment meshes/textures, waist/other weapons/formal first person or exact anatomical hilt handoff'})
        if options.hurt_runtime:
            folder=ROOT/'art/player-animation-v1/hurt-reaction/v01';approval=json.loads((folder/'docs/APPROVAL.json').read_text());assert approval['date']=='2026-10-08' and approval['quote'].startswith('采用')
            export=Export(folder/'exports'/f'{variant}-hurt-reaction.glb')
            selected=[f'chest_{equip}_{direction}_{side}' for equip in ['empty','carry','guard'] for direction in ['front','back','left','right'] for side in ['R','L']]
            for name,clip in export.clips(selected,names,rest).items():
                clip['loop']=False;data['clips']['hurt_'+name]=clip
            provenance.append({'source':str(export.path.relative_to(ROOT)),'sha256':hashlib.sha256(export.path.read_bytes()).hexdigest(),'use':'ordinary actual hurt6/hurtYaw, stripped pelvis/body rigid upper delta over current native pose; source qualification is not runtime proof','excluded':'fixed source legs/pelvis buffering, head variants/new hit-region rules, reference equipment meshes/textures, heavy stagger/formal first person'})
        if options.bow_transition_runtime:
            folder=ROOT/'art/player-animation-v1/bow-transitions/v01';assert json.loads((folder/'docs/APPROVAL.json').read_text())['adopted'] is True
            export=Export(folder/'exports'/f'{variant}-bow-transitions.glb')
            selected=[f'{phase}_back_{equip}_{side}' for phase in ['draw','stow'] for equip in ['empty','shield'] for side in ['R','L']]
            for name,clip in export.clips(selected,names,rest).items():
                clip['loop']=False;data['clips']['bow_equipment_'+name]=clip
            provenance.append({'source':str(export.path.relative_to(ROOT)),'sha256':hashlib.sha256(export.path.read_bytes()).hexdigest(),'use':'bounded native same-reference bow back draw/stow, actual .32 Settle clock and native bow wood anchor/model; qualification alone is not runtime proof','referenceAliases':{'left_sword_reference':'actual left_bow_reference','right_sword_reference':'actual right_bow_reference'},'excluded':'reference equipment meshes/textures, source alternative mount layout, waist/other ranged/formal first-person/exact hilt handoff'})
        if options.shield_transition_runtime:
            folder=ROOT/'art/player-animation-v1/shield-transitions/v01';assert json.loads((folder/'docs/APPROVAL.json').read_text())['adopted'] is True
            export=Export(folder/'exports'/f'{variant}-shield-transitions.glb')
            selected=[f'{phase}_back_{equip}_{side}' for phase in ['draw','stow'] for equip in ['empty','sword'] for side in ['R','L']]
            for name,clip in export.clips(selected,names,rest).items():
                # The inherited preview palm empties still use .125. This adopted source explicitly
                # authors gripLocalOffset -.0625 in Blender; GLB wrist's local +Y points into the palm.
                # Only non-skin grip anchors change; all actual authored joint samples remain intact.
                reanchor_transition_palms(clip,names)
                clip['loop']=False;data['clips']['shield_equipment_'+name]=clip
            provenance.append({'source':str(export.path.relative_to(ROOT)),'sha256':hashlib.sha256(export.path.read_bytes()).hexdigest(),'use':'bounded actual physical shield arm back gesture on same-reference native Settle owner1/2; both grip anchors use explicit authored .0625 wrist offset, no skin joint edits','excluded':'reference shield geometry/textures, source alternative dock layout, main-off direct transfer replay, waist/formal first-person/exact handle docking'})
        if options.axe_transition_runtime:
            folder=ROOT/'art/player-animation-v1/axe-transitions/v01';assert json.loads((folder/'docs/APPROVAL.json').read_text())['adopted'] is True
            export=Export(folder/'exports'/f'{variant}-axe-transitions.glb')
            selected=[f'{phase}_back_{equip}_{side}' for phase in ['draw','stow'] for equip in ['empty','shield'] for side in ['R','L']]
            for name,clip in export.clips(selected,names,rest,weapon_reference='axe_reference').items():
                reanchor_transition_palms(clip,names);clip['loop']=False;data['clips']['axe_equipment_'+name]=clip
            provenance.append({'source':str(export.path.relative_to(ROOT)),'sha256':hashlib.sha256(export.path.read_bytes()).hexdigest(),'use':'bounded same-reference main native axe back draw/stow at original .34/.38 Settle, explicit .0625 auxiliary grip and actual axe_reference; source qualification is not gameplay proof','excluded':'reference meshes/textures/source dock layouts, waist grip with larger interpolation error, other weapons/complex/formal first-person/exact anatomical hilt handoff'})
        if options.trident_transition_runtime:
            folder=ROOT/'art/player-animation-v1/trident-transitions/v01';assert json.loads((folder/'docs/APPROVAL.json').read_text())['adopted'] is True
            export=Export(folder/'exports'/f'{variant}-trident-transitions.glb')
            selected=[f'{phase}_back_{equip}_{side}' for phase in ['draw','stow'] for equip in ['empty','shield'] for side in ['R','L']]
            for name,clip in export.clips(selected,names,rest,weapon_reference='trident_reference').items():
                reanchor_transition_palms(clip,names);clip['loop']=False;data['clips']['trident_equipment_'+name]=clip
            provenance.append({'source':str(export.path.relative_to(ROOT)),'sha256':hashlib.sha256(export.path.read_bytes()).hexdigest(),'use':'bounded same-reference main native trident .34/.38 Settle, true raw pole1.10 and sampled wrist grip reference reanchor; actual runtime proof still required','excluded':'reference meshes/textures/source dock layout, claiming continuous secondary contact from a late support path, waist/complex/formal first-person/exact anatomical back grasp'})
        if options.spear_transition_runtime:
            folder=ROOT/'art/player-animation-v1/spear-transitions/v01';assert json.loads((folder/'docs/APPROVAL.json').read_text())['adopted'] is True
            export=Export(folder/'exports'/f'{variant}-spear-transitions.glb')
            selected=[f'{phase}_back_{equip}_{side}' for phase in ['draw','stow'] for equip in ['empty','shield'] for side in ['R','L']]
            for name,clip in export.clips(selected,names,rest,weapon_reference='spear_reference').items():
                reanchor_transition_palms(clip,names);clip['loop']=False;data['clips']['spear_equipment_'+name]=clip
            provenance.append({'source':str(export.path.relative_to(ROOT)),'sha256':hashlib.sha256(export.path.read_bytes()).hexdigest(),'use':'bounded same-reference main native spear .34/.38 Settle/current sampled wrist reference and real 32px native material model/raw(.70,.30,.5) shaft; no duplicate1.7 scale','excluded':'iron reference mesh/texture replacing seven native materials, claiming continuous secondary contact from late support, source mount layout/waist/complex/formal first-person/exact back grasp'})
        if options.mace_transition_runtime:
            folder=ROOT/'art/player-animation-v1/mace-transitions/v01';assert json.loads((folder/'docs/APPROVAL.json').read_text())['adopted'] is True
            export=Export(folder/'exports'/f'{variant}-mace-transitions.glb')
            selected=[f'{phase}_back_{equip}_{side}' for phase in ['draw','stow'] for equip in ['empty','shield'] for side in ['R','L']]
            for name,clip in export.clips(selected,names,rest,weapon_reference='mace_reference').items():
                reanchor_transition_palms(clip,names);clip['loop']=False;data['clips']['mace_equipment_'+name]=clip
            provenance.append({'source':str(export.path.relative_to(ROOT)),'sha256':hashlib.sha256(export.path.read_bytes()).hexdigest(),'use':'bounded main native mace back .34/.38 Settle/current wrist reference; art-third raw(.08,.08,.5), no change to old first-person .23/.35; actual runtime proof required','excluded':'reference meshes/textures/dock layout, continuous secondary contact claims, waist/complex/formal first-person/exact back grasp'})
        if options.crossbow_transition_runtime:
            folder=ROOT/'art/player-animation-v1/crossbow-transitions/v01';assert json.loads((folder/'docs/APPROVAL.json').read_text())['adopted'] is True
            export=Export(folder/'exports'/f'{variant}-crossbow-transitions.glb')
            selected=[f'{phase}_back_{equip}_{side}' for phase in ['draw','stow'] for equip in ['empty','shield'] for side in ['R','L']]
            for name,clip in export.clips(selected,names,rest,weapon_reference='crossbow_reference').items():
                clip['loop']=False;data['clips']['crossbow_equipment_'+name]=clip
            provenance.append({'source':str(export.path.relative_to(ROOT)),'sha256':hashlib.sha256(export.path.read_bytes()).hexdigest(),'use':'main native crossbow back .32 Settle/source main wrist .125/raw native(.90625,.09375,.5); source reference preserved, true .9 native display scale once; actual runtime verification required','excluded':'source reference mesh/texture/dock, added bow-crossbow Fuse host or nested projectile back sync, waist/complex/formal first-person/exact back grasp'})
        if options.ground_trident_runtime:
            folder=ROOT/'art/player-animation-v1/trident-attacks/v01';assert json.loads((folder/'docs/APPROVAL.json').read_text())['adopted'] is True
            export=Export(folder/'exports'/f'{variant}-trident-attacks.glb')
            selected=[f'{phase}_attack_{equip}_{side}' for phase in ['light','heavy'] for equip in ['empty','shield'] for side in ['R','L']]
            for name,clip in export.clips(selected,names,rest,weapon_reference='trident_reference').items():
                clip['loop']=False;phase,equip,side=name.split('_')[0],name.split('_')[-2],name.split('_')[-1];data['clips'][f'trident_attack_{phase}_{equip}_{side}']=clip
            provenance.append({'source':str(export.path.relative_to(ROOT)),'sha256':hashlib.sha256(export.path.read_bytes()).hexdigest(),'use':'ground main native trident light7 and native charge16/variable hold/release12 source segments; source reference sliding preserved, actual hand pole contacts y1.10..1.60; no fixed-wrist weapon reanchor or model scale change; actual runtime proof required','excluded':'source review damage/hold deadlines/extra hits, air integration and source animation hit events, source reference mesh/texture, complex/armor/formal first-person'})
        if options.ground_spear_runtime:
            folder=ROOT/'art/player-animation-v1/spear-attacks/v01';assert json.loads((folder/'docs/APPROVAL.json').read_text())['adopted'] is True
            export=Export(folder/'exports'/f'{variant}-spear-attacks.glb')
            selected=[f'{phase}_attack_{equip}_{side}' for phase in ['light','heavy'] for equip in ['empty','shield'] for side in ['R','L']]
            for name,clip in export.clips(selected,names,rest,weapon_reference='spear_reference').items():
                clip['loop']=False;phase,equip,side=name.split('_')[0],name.split('_')[-2],name.split('_')[-1];data['clips'][f'spear_attack_{phase}_{equip}_{side}']=clip
            provenance.append({'source':str(export.path.relative_to(ROOT)),'sha256':hashlib.sha256(export.path.read_bytes()).hexdigest(),'use':'ground main native spear light8 and native charge18/variable hold/release13 source segments; source reference sliding preserved, actual hand axial slide0..-.60/native32px1.7 display once; no fixed-wrist weapon reanchor or model scale change; actual runtime proof required','excluded':'source review damage/hold deadlines/extra hits, air integration and source animation hit events, source reference mesh/texture, complex/armor/formal first-person'})
        if options.ground_mace_runtime:
            folder=ROOT/'art/player-animation-v1/mace-attacks/v01';assert json.loads((folder/'docs/APPROVAL.json').read_text())['adopted'] is True
            export=Export(folder/'exports'/f'{variant}-mace-attacks.glb')
            selected=[f'{phase}_attack_{equip}_{side}' for phase in ['light','heavy'] for equip in ['empty','shield'] for side in ['R','L']]
            for name,clip in export.clips(selected,names,rest,weapon_reference='mace_reference').items():
                clip['loop']=False;phase,equip,side=name.split('_')[0],name.split('_')[-2],name.split('_')[-1];data['clips'][f'mace_attack_{phase}_{equip}_{side}']=clip
            provenance.append({'source':str(export.path.relative_to(ROOT)),'sha256':hashlib.sha256(export.path.read_bytes()).hexdigest(),'use':'ground main native mace light12 and native charge26/variable hold/release20 source segments; source reference sliding preserved, actual native16px(.08,.08,.5) third-person grip/display1 once; original airborne smash excluded; no fixed-wrist weapon reanchor or model scale change; actual runtime proof required','excluded':'source review damage/hold deadlines/extra hits, air integration and source animation hit events, source reference mesh/texture, complex/armor/formal first-person'})
        if options.air_trident_runtime:
            folder=ROOT/'art/player-animation-v1/trident-air/v01';assert json.loads((folder/'docs/APPROVAL.json').read_text())['adopted'] is True
            export=Export(folder/'exports'/f'{variant}-trident-air.glb')
            selected=[f'{phase}_air_{equip}_{side}' for phase in ['light','heavy'] for equip in ['empty','shield'] for side in ['R','L']]
            for name,clip in export.clips(selected,names,rest,weapon_reference='trident_reference').items():
                clip['loop']=False;phase,equip,side=name.split('_')[0],name.split('_')[-2],name.split('_')[-1];data['clips'][f'trident_attack_air_{phase}_{equip}_{side}']=clip
            provenance.append({'source':str(export.path.relative_to(ROOT)),'sha256':hashlib.sha256(export.path.read_bytes()).hexdigest(),'use':'actual airborne main native trident light7 and native charge16/variable hold/release12 source segments; source reference sliding preserved, actual hand pole contacts y1.10..1.60; no fixed-wrist weapon reanchor or model scale change; actual runtime proof required','excluded':'source review damage/hold deadlines/extra hits, air integration and source animation hit events, source reference mesh/texture, complex/armor/formal first-person'})
        if options.air_spear_runtime:
            folder=ROOT/'art/player-animation-v1/spear-air/v01';assert json.loads((folder/'docs/APPROVAL.json').read_text())['adopted'] is True
            export=Export(folder/'exports'/f'{variant}-spear-air.glb')
            selected=[f'{phase}_air_{equip}_{side}' for phase in ['light','heavy'] for equip in ['empty','shield'] for side in ['R','L']]
            for name,clip in export.clips(selected,names,rest,weapon_reference='spear_reference').items():
                clip['loop']=False;phase,equip,side=name.split('_')[0],name.split('_')[-2],name.split('_')[-1];data['clips'][f'spear_attack_air_{phase}_{equip}_{side}']=clip
            provenance.append({'source':str(export.path.relative_to(ROOT)),'sha256':hashlib.sha256(export.path.read_bytes()).hexdigest(),'use':'actual airborne main native spear light8 and native charge18/variable hold/release13 source segments; source reference sliding preserved, actual hand pole contacts source axis0..-.6/native32px1.7 once; no fixed-wrist weapon reanchor or model scale change; actual runtime proof required','excluded':'source review damage/hold deadlines/extra hits, air integration and source animation hit events, source reference mesh/texture, complex/armor/formal first-person'})
        if options.air_mace_runtime:
            folder=ROOT/'art/player-animation-v1/mace-air/v01';assert json.loads((folder/'docs/APPROVAL.json').read_text())['adopted'] is True
            export=Export(folder/'exports'/f'{variant}-mace-air.glb')
            selected=[f'{phase}_air_{equip}_{side}' for phase in ['light','heavy'] for equip in ['empty','shield'] for side in ['R','L']]
            for name,clip in export.clips(selected,names,rest,weapon_reference='mace_reference').items():
                clip['loop']=False;phase,equip,side=name.split('_')[0],name.split('_')[-2],name.split('_')[-1];data['clips'][f'mace_attack_air_{phase}_{equip}_{side}']=clip
            provenance.append({'source':str(export.path.relative_to(ROOT)),'sha256':hashlib.sha256(export.path.read_bytes()).hexdigest(),'use':'actual airborne main native mace light12 and native charge26/variable hold/release20 source segments; source reference sliding preserved, actual hand pole contacts native16px(.08,.08,.5)/.38 support; native smash excluded; no fixed-wrist weapon reanchor or model scale change; actual runtime proof required','excluded':'source review damage/hold deadlines/extra hits, air integration and source animation hit events, source reference mesh/texture, complex/armor/formal first-person'})
        if options.jump_runtime:
            folder=ROOT/'art/player-animation-v1/jump-takeoff-landing/v01'
            assert json.loads((folder/'docs/APPROVAL.json').read_text())['adopted'] is True
            export=Export(folder/'exports'/f'{variant}-jump-takeoff-landing.glb')
            selected=[f'{phase}_{equip}_{side}' for phase in ['takeoff','air','land'] for equip in ['empty','carry','guard'] for side in ['R','L']]
            for name,clip in export.clips(selected,names,rest).items():
                clip['loop']=name.startswith('air_');data['clips']['jump_'+name]=clip
            provenance.append({'source':str(export.path.relative_to(ROOT)),'sha256':hashlib.sha256(export.path.read_bytes()).hexdigest(),'use':'actual native takeoff-air-land observation only, 400Hz source samples preserve rapid grounded foot contact; existing 20-to-22 wrist derivation; actual default sword skin-native equipment GPU qualification required','excluded':'review cycle, previewLift, mesh/textures, input wait, root motion, physics/control/stamina/damage changes, HOP/unsupported/complex/formal first-person'})
        if options.ground_bow_right_runtime:
            for phase in ['draw','hold']:
                original=data['clips'][f'bow_ground_{phase}_empty_L']
                data['clips'][f'bow_ground_{phase}_empty_R']=mirrored_ground_bow_clip(original,names,rest)
            provenance.append({'source':'art/player-animation-v1/bow-ground/v05','use':'derived rigid X-reflection and left/right palette pairing of approved LEFT draw/hold only; same source pullTimes, existing 22 symmetric skin binds; auxiliary bow reference mirrored physically rather than forced into asymmetric sword rest; RIGHT actual-use/native sprite-string and GPU must be verified; not a new original animation asset'})
        if options.air_bow_right_runtime:
            for phase in ['draw','hold']:
                original=data['clips'][f'bow_air_{phase}_empty_L']
                data['clips'][f'bow_air_{phase}_empty_R']=mirrored_ground_bow_clip(original,names,rest)
            provenance.append({'source':'art/player-animation-v1/bow-air/v01','use':'derived rigid X-reflection and paired 22-bone symmetric skin of approved LEFT draw/hold only; source root/previewLift exclusion/pullTimes and native20tick/Focus clocks remain unchanged; actual RIGHT native sprite-string-GPU-arrow and Focus exits must be verified; not a new original animation asset'})
        path=target/f'{variant}-combat-rig.json';serialized=json.dumps(data,separators=(',',':'),allow_nan=False)+'\n';outputs.append((path,serialized))
        for export in [base,idle,sword,guard]:provenance.append({'source':str(export.path.relative_to(ROOT)),'sha256':hashlib.sha256(export.path.read_bytes()).hexdigest()})
        print(variant,'skin vertices',sum(len(m['positions'])//3 for m in data['meshes']),'clips',len(data['clips']),'bytes',len(serialized.encode()))
    if options.focus_leg_runtime:
        folder=ROOT/'art/player-animation-v1/link-time/v01';assert json.loads((folder/'docs/APPROVAL.json').read_text())['adopted'] is True
        layer=json.loads((folder/'exports/focus-layer.json').read_text());config={key:layer[key] for key in ['version','enterSeconds','exitSeconds']};config.update({key:layer['bodyLayer'][key]for key in ['thighBackDegrees','shinExtraBackDegrees','mask']})
        outputs.append((target/'focus-leg-layer.json',json.dumps(config,indent=2)+'\n'))
        provenance.append({'source':str((folder/'exports/focus-layer.json').relative_to(ROOT)),'sha256':hashlib.sha256((folder/'exports/focus-layer.json').read_bytes()).hexdigest(),'use':'ten-bone layer config only over current qualified native airborne bow; no duplicate body clips, world-example rate/presentation parameters excluded; actual status in C6R-FOCUS-LEG-LAYER-VERIFICATION'})
    for path,serialized in outputs:path.write_text(serialized)
    (target/'provenance.json').write_text(json.dumps({'localOnly':True,'mixedSourceLicense':'Preserve adopted source license chain; no diagnostic textures or reference meshes distributed','sources':provenance},ensure_ascii=False,indent=2)+'\n')

if __name__=='__main__':main()
