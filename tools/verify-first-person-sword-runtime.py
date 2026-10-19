#!/usr/bin/env python3
"""Independently compare converted skin/UVs and mount contacts with adopted GLBs.

Only the seven-bone independent first-person skeleton is accepted here. The
existing full-body importer's 20/22-bone gate remains untouched.
"""
import argparse
import importlib.util
import json
from pathlib import Path
import struct
import numpy as np

ROOT=Path(__file__).resolve().parents[1]
def load(name,path):
    spec=importlib.util.spec_from_file_location(name,path);module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module);return module
base=load('wildcraft_body_import',ROOT/'tools/import-player-combat-art.py')
fp=load('wildcraft_fp_import',ROOT/'tools/import-first-person-sword.py')

class IndependentExport(base.Export):
    def __init__(self,path):
        raw=path.read_bytes();magic,version,size=struct.unpack_from('<4sII',raw);assert magic==b'glTF' and version==2 and size==len(raw)
        length,kind=struct.unpack_from('<II',raw,12);assert kind==0x4e4f534a
        self.data=json.loads(raw[20:20+length]);offset=20+length
        length,kind=struct.unpack_from('<II',raw,offset);assert kind==0x004e4942;self.binary=raw[offset+8:offset+8+length]
        self.nodes=self.data['nodes'];self.parents={child:i for i,n in enumerate(self.nodes) for child in n.get('children',[])}
        self.joints=self.data['skins'][0]['joints'];assert len(self.joints)==7
        self.palette=list(range(len(self.nodes)))


def verify(runtime):
    converted,source=fp.convert();report=dict(source);report.update(meshes=0,sourceFrames=0,verticesChecked=0,maxVertexError=0.,maxMountError=0.,maxSwordAxisError=0.,maxShieldCenterError=0.)
    # Blender source -> glTF; camera conversion is independently checked by the game GPU audit.
    basis=np.array([[1,0,0],[0,0,1],[0,-1,0]])
    for variant,data in converted.items():
        actual=json.loads((runtime/(variant+'-first-person-sword.json')).read_text());assert actual==data
        original=json.loads(fp.SOURCE.read_text())['variants'][variant.title()]
        export=IndependentExport(fp.SOURCE.parent/(variant+'-first-person-sword.glb'))
        inverse=export.array(export.data['skins'][0]['inverseBindMatrices']).reshape(-1,4,4).transpose(0,2,1)
        meshes=[]
        for i,node in enumerate(export.nodes):
            if 'skin' not in node:continue
            name=node['name'].split('/')[-1];rawMesh=original['meshes'][name];outMesh=next(m for m in data['meshes'] if m['part']==rawMesh['part'] and m['outer']==rawMesh['outer'])
            primitive=export.data['meshes'][node['mesh']]['primitives'][0];attrs=primitive['attributes']
            pos=export.array(attrs['POSITION']);uv=export.array(attrs['TEXCOORD_0']);joints=export.array(attrs['JOINTS_0']);weights=export.array(attrs['WEIGHTS_0'])
            ourPos=np.array(outMesh['positions']).reshape(-1,3);ourUv=np.array(outMesh['uv']).reshape(-1,2)
            distances=np.linalg.norm(pos[:,None,:]-(ourPos@basis.T)[None,:,:],axis=2)+np.linalg.norm(uv[:,None,:]-ourUv[None,:,:],axis=2)
            nearest=np.argmin(distances,axis=1);assert distances[np.arange(len(pos)),nearest].max()<.000003
            ourJoints=np.array(outMesh['joints']).reshape(-1,4)[nearest];ourWeights=np.array(outMesh['weights']).reshape(-1,4)[nearest]
            meshes.append((pos,joints,weights,ourPos[nearest],ourJoints,ourWeights));report['meshes']+=1
        assert len(meshes)==4
        animations={a['name']:a for a in export.data['animations']}
        for name in fp.CLIPS:
            clip=original['clips'][name];tracks=export.tracks(animations[name]);step=clip['duration']/(len(clip['frames'])-1)
            for index,f in enumerate(clip['frames']):
                worlds=export.world(tracks,index*step);skin=np.array([worlds[j] for j in export.joints])@inverse
                ours=[]
                for bone in fp.BONES:
                    m=np.eye(4);m[:3,:3]=fp.rotation(f['rotations'][bone]);m[:3,3]=np.array(f['origins'][bone])-m[:3,:3]@np.array(original['pivots'][bone]);ours.append(m)
                ours=np.array(ours)
                for pos,joints,weights,ourPos,ourJoints,ourWeights in meshes:
                    p=np.column_stack((pos,np.ones(len(pos))));op=np.column_stack((ourPos,np.ones(len(ourPos))))
                    posed=np.einsum('vbij,vj,vb->vi',skin[joints],p,weights)[:,:3]
                    expected=np.einsum('vbij,vj,vb->vi',ours[ourJoints],op,ourWeights)[:,:3]@basis.T
                    error=float(np.linalg.norm(posed-expected,axis=1).max());report['maxVertexError']=max(report['maxVertexError'],error);report['verticesChecked']+=len(pos)
                for side in ('right','left'):
                    sword=next(i for i,n in enumerate(export.nodes) if n.get('name','').endswith(side+'_sword_reference'))
                    gap=float(np.linalg.norm(worlds[sword][:3,3]-basis@np.array(f['grips'][side])));report['maxMountError']=max(report['maxMountError'],gap)
                    fore=fp.rotation(f['rotations'][side+'_forearm']);mount=fp.rotation(f['swordMountRotations'][side]);axis=worlds[sword][:3,:3]@np.array([0,1,0]);axis/=np.linalg.norm(axis)
                    report['maxSwordAxisError']=max(report['maxSwordAxisError'],float(np.linalg.norm(axis-basis@fore@mount@np.array([0,0,1]))))
                    shield=next(i for i,n in enumerate(export.nodes) if n.get('name','').endswith(side+'_held_shield'))
                    center=worlds[shield]@np.array([0,0,.15,1]);report['maxShieldCenterError']=max(report['maxShieldCenterError'],float(np.linalg.norm(center[:3]-basis@np.array(f['grips'][side]))))
                report['sourceFrames']+=1
    assert report['maxVertexError']<.00001,report
    assert report['maxMountError']<.00001 and report['maxSwordAxisError']<.00001 and report['maxShieldCenterError']<.00001,report
    return report

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--runtime',type=Path,default=ROOT/'src/main/resources/assets/wildcraft/animation');parser.add_argument('--report',type=Path);args=parser.parse_args()
    report=verify(args.runtime);text=json.dumps(report,ensure_ascii=False,indent=2)+'\n'
    if args.report:args.report.write_text(text)
    print(text)
