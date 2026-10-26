"""Regression for exported locomotion, tail motion and seamless loop endpoints."""
import hashlib
import json
import math
import struct
from pathlib import Path
from skin_patch_contract import assert_payload_preserved
from mob_correction_contract import legacy_heading
from beast_anatomy_contract import pre_anatomy

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'asset_sources/animation-readability-v2-20261017'
manifest=json.loads((OUT/'integration-manifest.json').read_text(encoding='utf8'))

def read(p):
    raw=p.read_bytes();n=struct.unpack_from('<I',raw,12)[0]
    return json.loads(raw[20:20+n]),raw[28+n:]

def vals(d,b,i):
    a=d['accessors'][i];v=d['bufferViews'][a['bufferView']]
    assert a['componentType']==5126
    width={'SCALAR':1,'VEC3':3,'VEC4':4}[a['type']];offset=v.get('byteOffset',0)+a.get('byteOffset',0)
    return [struct.unpack_from('<'+'f'*width,b,offset+j*v.get('byteStride',4*width)) for j in range(a['count'])]

def distance(a,b):
    dot=abs(sum(x*y for x,y in zip(a,b)))/(sum(x*x for x in a)*sum(y*y for y in b))**.5
    return math.degrees(2*math.acos(min(1,dot)))

for ident,entry in manifest.items():
    path=pre_anatomy(ident)
    assert hashlib.sha256(path.read_bytes()).hexdigest()==entry['output_sha256'],(ident,'unreviewed runtime asset')
    d,b=read(path);old,ob=read(OUT/'original-runtime'/(ident+'.glb'))
    assert_payload_preserved(ident,old,ob,d,b)
    for field in ('nodes','meshes','skins','materials','images','textures','samplers'):assert (legacy_heading(d).get(field) if ident=='lie_ren_lang' else d.get(field))==old.get(field),(ident,field)
    for clip in entry['replaced_clips']:
        a=next(x for x in d['animations'] if x['name']==ident+'.'+clip)
        rotations={}
        for c in a['channels']:
            s=a['samplers'][c['sampler']];v=vals(d,b,s['output']);times=vals(d,b,s['input'])
            assert all(t2[0]>t1[0] for t1,t2 in zip(times,times[1:])),(ident,clip,'non-increasing times')
            node=d['nodes'][c['target']['node']]['name']
            if c['target']['path']=='rotation':
                assert distance(v[0],v[-1])<.12,(ident,clip,node,'rotation loop pop')
                rotations[node]=max(distance(x,y) for x in v for y in v)
            else:
                assert max(abs(x-y) for x,y in zip(v[0],v[-1]))<1e-4,(ident,clip,node,'translation/scale loop pop')
        if ident=='lie_yan_zhu':
            assert all(rotations.get(f'leg_{s}{i}',0)>28 for s in 'LR' for i in range(1,5)),(ident,clip,'eight legs must move')
        elif ident=='ting_yu_ya':
            if clip=='walk':assert all(42<rotations.get('leg_upper.'+s,0)<46 for s in 'LR'),(ident,'stationary walk legs')
            if clip=='fly':
                for part,minimum,maximum in [('upper',98,102),('middle',38,42),('tip',26,30)]:
                    assert all(minimum<rotations.get('wing_'+part+'.'+s,0)<maximum for s in 'LR'),(ident,part,'wing amplitude')
            assert rotations['tail_0']>5 and rotations['tail_1']>9,(ident,clip,'invisible tail')
        else:
            if clip in ('walk','run'):
                assert all((38 if clip=='walk' else 44)<rotations[p+'_upper.'+s]<52 for p in ('front','hind') for s in 'LR'),(ident,clip,'leg amplitude')
                assert all((34 if clip=='walk' else 40)<rotations[p+'_lower.'+s]<46 for p in ('front','hind') for s in 'LR'),(ident,clip,'knee amplitude')
            assert all(rotations['tail_'+str(i)]>5 for i in range(4)),(ident,clip,'stationary tail')
        if clip in ('walk','run') and ident not in ('ting_yu_ya','lie_yan_zhu'):
            expected=40/24 if clip=='walk' else 20/24
            assert abs(max(vals(d,b,s['input'])[-1][0] for s in a['samplers'])-expected)<.001,(ident,clip,'follower padding returned')
print('PASS: 14 species, real limb/tail channels, cycle endpoints, exact original geometry/2K atlases/combat clips')
