"""Audit real exported motion, texture preservation, skin bounds and registration."""
import hashlib
import io
import json
import struct
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT/'src/main/resources/assets/xiantu'
OUT = ROOT/'asset_sources/creature-batch-next-20261017'

def read(path):
    b=path.read_bytes()
    assert struct.unpack_from('<III',b)==(0x46546c67,2,len(b)),path
    n,t=struct.unpack_from('<II',b,12)
    assert t==0x4e4f534a
    d=json.loads(b[20:20+n]);bn,bt=struct.unpack_from('<II',b,20+n)
    binary=b[28+n:]
    assert bt==0x004e4942 and len(binary)==bn
    assert all(not x.get('uri') for x in d.get('buffers',[])+d.get('images',[]))
    for v in d['bufferViews']:
        assert v.get('byteOffset',0)+v['byteLength']<=len(binary)
    return d,binary

def values(d,b,i):
    a=d['accessors'][i];v=d['bufferViews'][a['bufferView']]
    fmt,size={5126:('f',4),5123:('H',2),5121:('B',1),5125:('I',4)}[a['componentType']]
    width={'SCALAR':1,'VEC3':3,'VEC4':4,'MAT4':16}[a['type']]
    off=v.get('byteOffset',0)+a.get('byteOffset',0);stride=v.get('byteStride',size*width)
    return [struct.unpack_from('<'+fmt*width,b,off+j*stride) for j in range(a['count'])]

def images(d,b):
    result=[]
    for im in d['images']:
        v=d['bufferViews'][im['bufferView']];off=v.get('byteOffset',0);raw=b[off:off+v['byteLength']]
        size=Image.open(io.BytesIO(raw)).size
        result.append((hashlib.sha256(raw).hexdigest(),size))
    return sorted(result)

manifest=json.loads((OUT/'tai-bei-xiong-manifest.json').read_text(encoding='utf-8'))
source=Path(manifest['source'])
assert hashlib.sha256(source.read_bytes()).hexdigest()==manifest['source_sha256']
model=ASSETS/'models/entity/tai_bei_xiong.glb'
assert hashlib.sha256(model.read_bytes()).hexdigest()==manifest['animated_sha256']
d,b=read(model);sd,sb=read(source);lod,lb=read(ASSETS/'models/entity/tai_bei_xiong_lod.glb')
assert len(d['scenes'])==1 and len(d['meshes'])==1,'foreign scene exported'
assert len(d['skins'])==1 and len(d['skins'][0]['joints'])==22
assert not lod.get('skins') and not lod.get('animations')
assert images(sd,sb)==images(d,b)==images(lod,lb),'source atlas pixels or resolution changed'
clips={a['name']:a for a in d['animations']}
assert set(clips)=={'tai_bei_xiong.'+n for n in ['idle','walk','run','attack','hurt','death','slam']}
for name in ['walk','run','death','slam']:
    clip=clips['tai_bei_xiong.'+name]
    changing=[]
    for c in clip['channels']:
        node=d['nodes'][c['target']['node']]['name']
        if c['target']['path']!='rotation':continue
        samples=values(d,b,clip['samplers'][c['sampler']]['output'])
        span=max(max(v[k] for v in samples)-min(v[k] for v in samples) for k in range(4))
        if span>.005:changing.append(node)
    assert changing,(name,'exported clip has no meaningful rotation')
    if name in ('walk','run'):
        assert sum('upper.' in n for n in changing)==4,(name,changing)
for mesh in d['meshes']:
    for p in mesh['primitives']:
        a=p['attributes'];assert p.get('mode',4)==4
        assert {'POSITION','NORMAL','TEXCOORD_0','JOINTS_0','WEIGHTS_0'}<=a.keys()
        for w in values(d,b,a['WEIGHTS_0']):assert abs(sum(w)-1)<1e-4
        assert max(max(j) for j in values(d,b,a['JOINTS_0']))<22
        pos=values(d,b,a['POSITION']);low=min(v[1] for v in pos);high=max(v[1] for v in pos)
        assert abs(low)<.015 and 2.05<(high-low)*2.966<2.15,(low,high)
entity=(ROOT/'src/main/java/com/longju/xiantu/entity/MossBackBearEntity.java').read_text(encoding='utf8')
assert 'extends PathfinderMob' in entity
assert 'new WaterAvoidingRandomStrollGoal' in entity
assert 'elapsed == 30' in entity and 'elapsed >= 70' in entity and 'now + 180' in entity
assert 'forward <= 3' in entity and 'Math.abs(lateral) <= 1' in entity
assert 'getSensing().hasLineOfSight(victim)' in entity
assert 'peaceful()' in entity and 'setTarget(null)' in entity
assert 'return "slam"' in entity and 'return "attack"' in entity
registry=(ROOT/'src/main/java/com/longju/xiantu/registry/ModEntities.java').read_text(encoding='utf8')
assert 'event.put(MOSS_BACK_BEAR.get(),' in registry
bear_attributes=registry[registry.index('event.put(MOSS_BACK_BEAR.get()'):registry.index('event.put(INNER_DEMON.get()')]
assert 'Attributes.ATTACK_DAMAGE' in bear_attributes
print('PASS Mossback Bear: 22 joints, 7 moving clips, original atlases, 2.1-block height, heavy-strike timing and attack attribute')
