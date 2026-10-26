"""Promote an audited run-v2 GLB while retaining byte-for-byte deploy backups."""
import copy
import hashlib
import json
import shutil
from pathlib import Path

from audit_yanwei_animation_candidate import Glb, ROOT

OUT = ROOT / 'asset_sources/yanwei-fox-run-v2-20261015'
MODEL = ROOT / 'src/main/resources/assets/xiantu/models/entity/yanwei_fox.glb'
MANIFEST = OUT / 'animation-manifest.json'
BACKUP = ROOT / 'work/deploy-backups/yanwei-run-adjustment-20261015'
EXPECTED_OLD = '417D9013C4102E3FF0CFE97069A3F2620AC9E259B94EE762C7AB98E37AC2E2E5'

old_hash = hashlib.sha256(MODEL.read_bytes()).hexdigest().upper()
if old_hash != EXPECTED_OLD:
    raise SystemExit(f'Production asset changed since candidate review: {old_hash}')
candidate = OUT / 'yanwei_fox_run_v2.glb'
candidate_hash = hashlib.sha256(candidate.read_bytes()).hexdigest().upper()
if candidate_hash != json.loads((OUT/'run-audit.json').read_text(encoding='utf-8'))['candidate_sha256']:
    raise SystemExit('Candidate changed after its audit')

old_manifest = json.loads((ROOT/'asset_sources/yanwei-fox-gfbs-animation-20261014/animation-manifest.json')
                          .read_text(encoding='utf-8'))
for path in (MODEL, ROOT/'src/main/resources/assets/xiantu/models/entity/yanwei_fox_lod.glb',
             ROOT/'asset_sources/yanwei-fox-gfbs-animation-20261014/animation-manifest.json'):
    BACKUP.mkdir(parents=True, exist_ok=True)
    target=BACKUP/path.name
    if target.exists():
        target=BACKUP/f'{path.stem}-before-diagonal-fix{path.suffix}'
    shutil.copy2(path, target)

glb = Glb(candidate)
manifest = copy.deepcopy(old_manifest)
manifest['output_sha256'] = candidate_hash
manifest['status'] = 'authored_run_v2; animation_only_geometry_preserved; game_visual_review_pending'
manifest['gait_revision'] = 'transverse_gallop_v2'
manifest['run_sampling_hz'] = 96
for clip_name, clip in glb.clips.items():
    entry = manifest['clips'][clip_name]
    entry['duration_seconds'] = float(max(glb.accessor(s['input'])[-1,0] for s in clip['samplers']))
    entry['keyed_bones'] = sorted({glb.names[c['target']['node']] for c in clip['channels']
                                   if glb.names[c['target']['node']] != 'FoxAnimationRoot'})
    entry['animated_root_node'] = any(glb.names[c['target']['node']] == 'FoxAnimationRoot'
                                      for c in clip['channels'])
    entry['loop'] = clip_name not in {'yanwei_fox.attack','yanwei_fox.hurt','yanwei_fox.death'}
    entry['key_entries'] = sum(glb.doc['accessors'][s['input']]['count'] for s in clip['samplers'])
MODEL.write_bytes(candidate.read_bytes())
manifest['output_sha256'] = hashlib.sha256(MODEL.read_bytes()).hexdigest().upper()
MANIFEST.write_text(json.dumps(manifest,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps({'production_model':str(MODEL),'sha256':manifest['output_sha256'],
                  'manifest':str(MANIFEST),'backup':str(BACKUP)},ensure_ascii=False,indent=2))
