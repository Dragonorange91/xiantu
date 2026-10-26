"""Regression of packaged completion resources: independent skins, moving clips and original atlases."""
import json,hashlib,math
from integrate_completion_models import ROOT,OUT,RES,read,values,images
from python_head_patch_contract import verify as verify_python_head
from beast_anatomy_contract import pre_anatomy
verify_python_head()
rows=json.loads((OUT/'integration-manifest.json').read_text(encoding='utf8'))
assert len(rows)==22 and len({r['runtime_id'] for r in rows})==22
for r in rows:
 d,b=read(pre_anatomy(r['model']));ld,lb=read(RES/'models/entity'/(r['model']+'_lod.glb'))
 assert hashlib.sha256(pre_anatomy(r['model']).read_bytes()).hexdigest()==r['sha256']
 assert len(d['skins'])==1 and not ld.get('skins') and not ld.get('animations')
 assert images(d,b)==images(ld,lb)
 for clip in d['animations']:
  assert clip['name'].startswith(r['model']+'.')
  channels=[values(d,b,clip['samplers'][c['sampler']]['output']) for c in clip['channels'] if c['target']['path']=='rotation']
  assert any(any(abs(v[k]-seq[0][k])>.001 for v in seq for k in range(4)) for seq in channels),(r['model'],clip['name'],'static animation')
 for mesh in d['meshes']:
  for primitive in mesh['primitives']:
   a=primitive['attributes'];assert 'JOINTS_1' not in a
   assert all(abs(sum(w)-1)<1e-4 for w in values(d,b,a['WEIGHTS_0']))
   assert max(max(j) for j in values(d,b,a['JOINTS_0']))<r['joints']
 for lang in ('zh_cn','en_us'):
  names=json.loads((RES/'lang'/(lang+'.json')).read_text(encoding='utf8'))
  assert 'entity.xiantu.'+r['runtime_id'] in names
  assert 'item.xiantu.'+r['runtime_id']+'_spawn_egg' in names
 assert (RES/'models/item'/(r['runtime_id']+'_spawn_egg.json')).is_file()
print('PASS 22 completion GLBs: original maps, finite skin weights, moving clips, static LODs and item resources')
