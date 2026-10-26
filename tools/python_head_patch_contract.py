"""Check the reviewed head correction independently of the skin-weight contract."""
from pathlib import Path
import hashlib, json, struct
import numpy as np
ROOT=Path(__file__).resolve().parents[1]
SOURCE=ROOT/'asset_sources/python-head-20261025'
MODELS=ROOT/'src/main/resources/assets/xiantu/models/entity'

def read(path):
    raw=path.read_bytes(); size=struct.unpack_from('<I',raw,12)[0]
    return raw,json.loads(raw[20:20+size]),28+size

def positions(raw,doc,start):
    masked=bytearray(raw); arrays=[]
    for mesh in doc['meshes']:
        for primitive in mesh['primitives']:
            a=doc['accessors'][primitive['attributes']['POSITION']];v=doc['bufferViews'][a['bufferView']]
            assert a['componentType']==5126 and a['type']=='VEC3' and not a.get('sparse')
            offset=start+v.get('byteOffset',0)+a.get('byteOffset',0); stride=v.get('byteStride',12)
            arrays.append(np.ndarray((a['count'],3),dtype='<f4',buffer=raw,offset=offset,strides=(stride,4)).copy())
            for i in range(a['count']):masked[offset+i*stride:offset+i*stride+12]=bytes(12)
    return bytes(masked),arrays

def verify():
    records={}
    for model in ['cang_lin_mang','cang_lin_mang_lod']:
        from mob_correction_contract import pre_edit
        old,od,os=read(SOURCE/(model+'.before.glb'));new,nd,ns=read(pre_edit(model))
        assert od==nd,(model,'JSON topology, rig, materials or animation changed')
        om,op=positions(old,od,os);nm,np_=positions(new,nd,ns)
        assert om==nm,(model,'non-position bytes changed')
        changes=0; maximum=0.
        for a,b in zip(op,np_):
            delta=b-a
            assert np.isfinite(b).all() and np.array_equal(a[:,1:],b[:,1:]),(model,'invalid/YZ changes')
            assert delta[:,0].max()<=1e-7 and delta[:,0].min()>=-.046001,(model,'head displacement bounds')
            changes+=int(np.count_nonzero(delta[:,0]));maximum=max(maximum,float(np.abs(delta[:,0]).max()))
        assert changes>0 and maximum>.04,(model,'correction missing')
        assert new==(SOURCE/(model+'.glb')).read_bytes(),(model,'reviewed candidate mismatch')
        records[model]={'before_sha256':hashlib.sha256(old).hexdigest(),'sha256':hashlib.sha256(new).hexdigest(),'changed_vertices':changes,'max_displacement':maximum}
    from mob_correction_contract import verify as verify_layers
    verify_layers()
    print('PASS python head: POSITION-only X correction; original Y/Z, topology, weights, UV, normals, rig, animations and textures preserved; matching LOD')
    return records

if __name__=='__main__':verify()
