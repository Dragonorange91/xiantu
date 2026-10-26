"""Inspect bird-specific skin motion and original embedded texture payloads."""
import json
import hashlib
import runpy
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
helpers=runpy.run_path(str(ROOT/'tools/verify_cold_fang_wolf.py'))
read,values,images=helpers['read'],helpers['values'],helpers['images']
out=ROOT/'asset_sources/creature-batch-next-20261017'
assets=ROOT/'src/main/resources/assets/xiantu'
manifest=json.loads((out/'ting-yu-ya-manifest.json').read_text(encoding='utf-8'))
from beast_anatomy_contract import pre_anatomy
src=Path(manifest['source']);model=pre_anatomy('ting_yu_ya')
assert hashlib.sha256(src.read_bytes()).hexdigest()==manifest['source_sha256']
assert hashlib.sha256(model.read_bytes()).hexdigest()==manifest['output_sha256']
d,b=read(model);sd,sb=read(src);lod,lb=read(assets/'models/entity/ting_yu_ya_lod.glb')
assert len(d['skins'])==1 and len(d['skins'][0]['joints'])==23
assert not lod.get('skins') and not lod.get('animations')
assert images(d,b)==images(sd,sb)==images(lod,lb)
clips={a['name']:a for a in d['animations']}
assert set(clips)=={'ting_yu_ya.'+name for name in manifest['clips']}
fly=clips['ting_yu_ya.fly'];changed=set()
for c in fly['channels']:
    if c['target']['path']!='rotation':continue
    name=d['nodes'][c['target']['node']]['name'];v=values(d,b,fly['samplers'][c['sampler']]['output'])
    if any(max(x[i] for x in v)-min(x[i] for x in v)>.01 for i in range(4)):changed.add(name)
assert {'wing_upper.L','wing_upper.R'}<=changed,changed
for mesh in d['meshes']:
    for p in mesh['primitives']:
        assert p.get('mode',4)==4
        a=p['attributes'];assert {'JOINTS_0','WEIGHTS_0','POSITION','NORMAL','TEXCOORD_0'}<=a.keys()
        for w in values(d,b,a['WEIGHTS_0']):assert abs(sum(w)-1)<1e-4
        assert max(max(v) for v in values(d,b,a['JOINTS_0']))<23
source=(ROOT/'src/main/java/com/longju/xiantu/entity/ThunderFeatherRavenEntity.java').read_text()
assert 'NearestAttackableTargetGoal' not in source,'neutral raven must not attack nearby players automatically'
assert 'Difficulty.PEACEFUL' in source and 'setTarget(null)' in source and 'isNoAi()' in source
assert 'getSensing().hasLineOfSight(target)' in source and 'level().noCollision' in source
print('PASS Thunder-Feather Raven: 23 joints, 10 clips, both animated wings, original atlases, neutral flight/dive wiring')
