"""Copy reviewed animation channels into original GLBs without touching geometry.

Default writes review candidates only; --install publishes after visual review.
Baseline bytes are retained for rollback and comparisons.
"""
import copy
import hashlib
import json
import math
import struct
import sys
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'asset_sources/animation-readability-v2-20261017'
ASSETS=ROOT/'src/main/resources/assets/xiantu/models/entity'
BASE=OUT/'original-runtime';BASE.mkdir(exist_ok=True)
READY=OUT/'ready';READY.mkdir(exist_ok=True)

def read(path):
    raw=path.read_bytes();magic,version,length=struct.unpack_from('<III',raw)
    assert (magic,version,length)==(0x46546c67,2,len(raw))
    n,t=struct.unpack_from('<II',raw,12);assert t==0x4e4f534a
    d=json.loads(raw[20:20+n]);return d,raw[28+n:]

def values(d,b,i):
    a=d['accessors'][i];v=d['bufferViews'][a['bufferView']]
    width={'SCALAR':1,'VEC3':3,'VEC4':4}[a['type']]
    assert a['componentType']==5126
    offset=v.get('byteOffset',0)+a.get('byteOffset',0);stride=v.get('byteStride',width*4)
    return [struct.unpack_from('<'+'f'*width,b,offset+j*stride) for j in range(a['count'])]

def write(path,d,b):
    d['buffers'][0]['byteLength']=len(b)
    js=json.dumps(d,separators=(',',':'),ensure_ascii=False).encode('utf8');js+=b' '*((-len(js))%4)
    b+=b'\0'*((-len(b))%4)
    path.write_bytes(struct.pack('<III',0x46546c67,2,28+len(js)+len(b))+struct.pack('<II',len(js),0x4e4f534a)+js+struct.pack('<II',len(b),0x004e4942)+b)

def transform(n):
    return [*n.get('translation',[0,0,0]),*n.get('rotation',[0,0,0,1]),*n.get('scale',[1,1,1])]

report={}
for candidate in sorted(OUT.glob('*-candidate.glb')):
    ident=candidate.name.removesuffix('-candidate.glb');runtime=ASSETS/(ident+'.glb');baseline=BASE/runtime.name
    if not baseline.exists():baseline.write_bytes(runtime.read_bytes())
    old,ob=read(baseline);new,nb=read(candidate);doc=copy.deepcopy(old);binary=bytearray(ob)
    names={n['name']:i for i,n in enumerate(old['nodes']) if 'name' in n}
    chosen={'walk','run'} if ident=='lie_yan_zhu' else {'idle','walk','fly','glide'} if ident=='ting_yu_ya' else {'idle','walk','run'}
    replacements={};acc_map={};view_map={}
    def acc(i):
        if i in acc_map:return acc_map[i]
        a=copy.deepcopy(new['accessors'][i]);vi=a['bufferView'];assert 'sparse' not in a
        if vi not in view_map:
            v=copy.deepcopy(new['bufferViews'][vi]);off=v.get('byteOffset',0)
            binary.extend(b'\0'*((-len(binary))%4));v['byteOffset']=len(binary);v['buffer']=0
            binary.extend(nb[off:off+v['byteLength']]);view_map[vi]=len(doc['bufferViews']);doc['bufferViews'].append(v)
        a['bufferView']=view_map[vi];acc_map[i]=len(doc['accessors']);doc['accessors'].append(a);return acc_map[i]
    for animation in new['animations']:
        suffix=animation['name'].split('.')[1]
        if suffix not in chosen:continue
        a=copy.deepcopy(animation);a['name']=ident+'.'+suffix
        for ch in a['channels']:
            node=new['nodes'][ch['target']['node']];name=node['name'];assert name in names,(ident,name)
            oldnode=old['nodes'][names[name]]
            # Same rest rig required: animation retargeting is not implicit here.
            assert 'matrix' not in node and 'matrix' not in oldnode
            assert max(abs(x-y) for x,y in zip(transform(node),transform(oldnode)))<1e-4,(ident,name,'rest transform mismatch',transform(node),transform(oldnode))
            ch['target']['node']=names[name]
        for sampler in a['samplers']:
            sampler['input']=acc(sampler['input']);sampler['output']=acc(sampler['output'])
        replacements[a['name']]=a
    assert set(replacements)=={ident+'.'+c for c in chosen},(ident,replacements.keys())
    doc['animations']=[replacements.get(a['name'],a) for a in old['animations']]
    output=READY/runtime.name;write(output,doc,bytes(binary));check,cb=read(output)
    for field in ['meshes','nodes','skins','materials','images','textures','samplers']:
        assert check.get(field)==old.get(field),(ident,field)
    assert cb[:len(ob)]==ob,'original geometry/texture/skin buffer changed'
    for a in check['animations']:
        if a['name'] not in replacements:
            assert a==next(x for x in old['animations'] if x['name']==a['name'])
    report[ident]={'original_sha256':hashlib.sha256(baseline.read_bytes()).hexdigest(),'output_sha256':hashlib.sha256(output.read_bytes()).hexdigest(),'replaced_clips':sorted(chosen),'preserved':'exact mesh, UV, textures, rest rig, weights, combat animations; static LOD untouched','bytes_before':baseline.stat().st_size,'bytes_after':output.stat().st_size}
    print('READY',ident,','.join(sorted(chosen)))
(OUT/'integration-manifest.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf8')
if '--install' in sys.argv:
    for ident in report:
        (ASSETS/(ident+'.glb')).write_bytes((READY/(ident+'.glb')).read_bytes())
    manifests=[* (ROOT/'asset_sources/creature-batch-next-20261017').glob('*-manifest.json'),ROOT/'asset_sources/creature-batch-20261016/fox-animation-manifest.json']
    for path in manifests:
        m=json.loads(path.read_text(encoding='utf8'))
        ident='yanwei_fox' if path.name=='fox-animation-manifest.json' else path.name.removesuffix('-manifest.json').replace('-','_')
        if ident not in report:continue
        backup=BASE/path.name
        if not backup.exists():backup.write_bytes(path.read_bytes())
        key='animated_sha256' if 'animated_sha256' in m else 'output_sha256'
        m[key]=report[ident]['output_sha256'].upper() if ident=='yanwei_fox' else report[ident]['output_sha256']
        m['motion_revision']='readable-gait-tail-v1' if ident=='lie_yan_zhu' else 'readable-gait-wings-v2';m['motion_audit']=str(OUT/(ident+'-audit.json'))
        m['motion_status']='integrated_pending_build_and_runtime_test'
        if ident=='yanwei_fox':
            d,b=read(READY/(ident+'.glb'))
            m['clips']={a['name']:max(values(d,b,s['input'])[-1][0] for s in a['samplers']) for a in d['animations']}
        if isinstance(m.get('clips'),dict):
            d,b=read(READY/(ident+'.glb'))
            for a in d['animations']:
                if a['name'].split('.')[1] in m['clips']:m['clips'][a['name'].split('.')[1]]=max(values(d,b,s['input'])[-1][0] for s in a['samplers'])
        path.write_text(json.dumps(m,ensure_ascii=False,indent=2),encoding='utf8')
    print('INSTALLED',len(report),'animation-only assets')
