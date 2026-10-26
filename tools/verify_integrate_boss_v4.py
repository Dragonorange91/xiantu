"""Validate all six candidates before any production mutation, then optionally integrate."""
import json,math,hashlib,shutil,sys
from integrate_completion_models import ROOT,read,values,images
from boss_combat_catalogue import MODELS,catalogue
BASE=ROOT/'asset_sources/boss-combat-v4-20261018'
READY=BASE/'ready'
def verify():
    results=[]
    for boss,ident in MODELS.items():
        meta=json.loads((READY/(ident+'-manifest.json')).read_text(encoding='utf8'))
        d,b=read(READY/(ident+'.glb'));ld,lb=read(READY/(ident+'_lod.glb'))
        assert len(d['skins'])==1 and len(d['skins'][0]['joints'])<=64
        assert images(d,b)==images(ld,lb) and not ld.get('skins') and not ld.get('animations')
        clips={a['name']:a for a in d['animations']}
        expected={ident+'.'+s['id'] for s in catalogue()[boss]}
        assert expected<=clips.keys(),expected-clips.keys()
        for s in catalogue()[boss]:
            a=clips[ident+'.'+s['id']]
            duration=max(values(d,b,t['input'])[-1][0] for t in a['samplers'])
            assert abs(duration-(s['windup']+s['active']+s['recovery'])/20)<.045,(ident,s['id'],duration)
        for a in clips.values():
            assert a['channels']
            for sampler in a['samplers']:
                times=[x[0] for x in values(d,b,sampler['input'])];assert times==sorted(set(times))
                assert all(math.isfinite(v) for row in values(d,b,sampler['output']) for v in row)
        hurt=clips[ident+'.hurt']
        for channel in hurt['channels']:
            if d['nodes'][channel['target']['node']].get('name')!='root':continue
            vals=values(d,b,hurt['samplers'][channel['sampler']]['output'])
            if channel['target']['path']=='rotation':assert all(abs(v[3])>.99999 for v in vals),(ident,'root overturn')
            if channel['target']['path']=='translation':assert max(sum(x*x for x in v) for v in vals)<1e-8,(ident,'root lift')
        for mesh in d['meshes']:
            for primitive in mesh['primitives']:
                a=primitive['attributes'];assert 'JOINTS_1' not in a
                assert all(abs(sum(w)-1)<1e-4 for w in values(d,b,a['WEIGHTS_0']))
                assert max(max(w) for w in values(d,b,a['JOINTS_0']))<meta['joints']
        results.append(dict(id=ident,joints=meta['joints'],clips=len(clips),root_hurt_stable=True))
    for ident in ('xing_yun_tian_jiang','tong_ling_yuan_wang'):
        audit=json.loads((BASE/(ident+'-surface-audit.json')).read_text())
        assert audit['face_max_edge_strain']<1e-5 and audit['uv_identical'] and audit['images_identical']
    necklace=json.loads((BASE/'necklace-reconstruction.json').read_text());assert necklace['source_beads']>=12 and necklace['bead_shape_max_error']<1e-6
    (BASE/'export-validation.json').write_text(json.dumps(results,indent=2),encoding='utf8')
    print('PASS',results)
    return results
def integrate():
    verify();author=ROOT/'asset_sources/creature-completion-20261017';res=ROOT/'src/main/resources/assets/xiantu/models/entity';backup=BASE/'pre-integration-backup';backup.mkdir(exist_ok=True)
    manifest=author/'integration-manifest.json'
    if not (backup/manifest.name).exists():shutil.copy2(manifest,backup/manifest.name)
    rows=json.loads(manifest.read_text(encoding='utf8'))
    for row in rows:
        ident=row['model']
        if ident not in MODELS.values():continue
        meta=json.loads((READY/(ident+'-manifest.json')).read_text(encoding='utf8'));d,b=read(READY/(ident+'.glb'))
        for suffix in ('.glb','_lod.glb','-manifest.json'):
            name=ident+suffix
            if (author/name).exists() and not (backup/name).exists():shutil.copy2(author/name,backup/name)
            shutil.copy2(READY/name,author/name)
            if suffix.endswith('.glb'):shutil.copy2(READY/name,res/name)
        row.update(sha256=meta['animated_sha256'],lod_sha256=meta['lod_sha256'],joints=meta['joints'],clips=[a['name'] for a in d['animations']],images=images(d,b),status='combat_v4_integrated_pending_runtime',authoring_manifest=str(READY/(ident+'-manifest.json')))
    manifest.write_text(json.dumps(rows,ensure_ascii=False,indent=2),encoding='utf8')
    print('Integrated six verified candidates; previous runtime assets backed up.')
if __name__=='__main__':integrate() if '--integrate' in sys.argv else verify()
