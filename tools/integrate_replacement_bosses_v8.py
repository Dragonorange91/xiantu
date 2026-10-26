"""Validate new sources, skins, clips and static LOD before replacing both assets."""
import json,hashlib,math,shutil,re
from pathlib import Path
from integrate_completion_models import ROOT,read,values,images
from boss_combat_catalogue import catalogue
BASE=ROOT/'asset_sources/boss-anatomy-v8-20261018';READY=BASE/'ready'
SPECS={'xuantians_remnant':'xuan_tian_cheng_zhu','cloud_sea_lord':'yun_hai_tian_jun'}
def validate():
 result=[]
 for boss,ident in SPECS.items():
  meta=json.loads((READY/(ident+'-manifest.json')).read_text(encoding='utf8'))
  d,b=read(READY/(ident+'.glb'));ld,lb=read(READY/(ident+'_lod.glb'));sd,sb=read(Path(meta['source']))
  assert hashlib.sha256(Path(meta['source']).read_bytes()).hexdigest()==meta['source_sha256']
  assert len(d['skins'])==1 and len(d['skins'][0]['joints'])<=64
  assert not ld.get('skins') and not ld.get('animations')
  assert images(d,b)==images(ld,lb)==images(sd,sb)
  clips={a['name']:a for a in d['animations']}
  assert {ident+'.'+c for c in ['idle','walk','run','attack','hurt','death']}<=clips.keys()
  for s in catalogue()[boss]:
   a=clips[ident+'.'+s['id']];end=max(values(d,b,t['input'])[-1][0] for t in a['samplers'])
   assert abs(end-(s['windup']+s['active']+s['recovery'])/20)<.045
  for a in clips.values():
   for sampler in a['samplers']:
    times=[v[0] for v in values(d,b,sampler['input'])];assert times==sorted(set(times))
    assert all(math.isfinite(v) for row in values(d,b,sampler['output']) for v in row)
  tris=0
  for mesh in d['meshes']:
   for p in mesh['primitives']:
    at=p['attributes'];assert 'JOINTS_1' not in at
    assert all(abs(sum(w)-1)<1e-4 for w in values(d,b,at['WEIGHTS_0']))
    assert max(max(j) for j in values(d,b,at['JOINTS_0']))<len(d['skins'][0]['joints'])
    tris+=len(values(d,b,p['indices']))//3
  result.append(dict(id=ident,joints=len(d['skins'][0]['joints']),clips=len(clips),triangles=tris,images=images(d,b),source_sha256=meta['source_sha256']))
 (BASE/'export-validation.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf8')
 print('PASS',result);return result
def integrate():
 result={r['id']:r for r in validate()};author=ROOT/'asset_sources/creature-completion-20261017';res=ROOT/'src/main/resources/assets/xiantu/models/entity';backup=BASE/'pre-integration-backup';backup.mkdir(exist_ok=True)
 mf=author/'integration-manifest.json';rows=json.loads(mf.read_text(encoding='utf8'))
 if not (backup/mf.name).exists():shutil.copy2(mf,backup/mf.name)
 for row in rows:
  ident=row['model']
  if ident not in result:continue
  meta=json.loads((READY/(ident+'-manifest.json')).read_text(encoding='utf8'));d,b=read(READY/(ident+'.glb'))
  for suffix in ('.glb','_lod.glb','-manifest.json'):
   f=ident+suffix
   if (author/f).exists() and not (backup/f).exists():shutil.copy2(author/f,backup/f)
   shutil.copy2(READY/f,author/f)
   if suffix.endswith('.glb'):shutil.copy2(READY/f,res/f)
  row.update(source=meta['source'],source_sha256=meta['source_sha256'],sha256=meta['animated_sha256'],lod_sha256=meta['lod_sha256'],joints=meta['joints'],model_scale=meta['model_scale'],clips=[a['name'] for a in d['animations']],images=images(d,b),triangles=result[ident]['triangles'],status='v8_anatomical_rest_and_fists_pending_runtime',authoring_manifest=str(READY/(ident+'-manifest.json')))
  java=ROOT/'src/main/java/com/longju/xiantu/entity/CompletionBestiary.java';s=java.read_text(encoding='utf8')
  s=re.sub(r'(new Definition\("'+row['runtime_id']+r'"[^\n]+?F, [0-9.]+F, )[0-9.]+F',lambda m:m.group(1)+f'{meta["model_scale"]:.6f}F',s)
  java.write_text(s,encoding='utf8')
 mf.write_text(json.dumps(rows,ensure_ascii=False,indent=2),encoding='utf8')
 print('Integrated two new source models; originals and v5 candidates retained.')
if __name__=='__main__':
 import sys
 integrate() if '--integrate' in sys.argv else validate()

