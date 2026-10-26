"""Verify exported original-detail hand candidates before targeted integration."""
import json, math, hashlib, re
from pathlib import Path
from integrate_completion_models import ROOT,read,values,images
BASE=ROOT/'asset_sources/posture-grip-20261018'
READY=BASE/'ready'
IDS=['bai_zhan_jiang_hun','xing_yun_tian_jiang','tong_ling_yuan_wang','xuan_tian_cheng_zhu','yun_hai_tian_jun']
def main():
    reports=[]
    skin_source=(ROOT/'third_party/gfbs/src/main/java/org/lytharalab/gfbs/gltf/api/model/GltfSkin.java').read_text()
    joint_limit=int(re.search(r'MAX_JOINTS\s*=\s*(\d+)',skin_source)[1])
    for ident in IDS:
        m=json.loads((READY/(ident+'-manifest.json')).read_text(encoding='utf8'))
        audit=json.loads((BASE/'original-hand-review'/(ident+'-audit.json')).read_text())
        assert all(audit[k] for k in ['topology_identical','uv_identical','original_images_identical'])
        assert all(x['max_rigid_surface_error']<1e-6 for x in audit['hands'].values())
        d,b=read(READY/(ident+'.glb'));ld,lb=read(READY/(ident+'_lod.glb'));sd,sb=read(Path(m['source']))
        assert len(d['skins'])==1 and len(d['skins'][0]['joints'])==m['joints']
        assert m['joints']<=joint_limit, f'{ident}: {m["joints"]} joints exceeds GFBS limit {joint_limit}'
        assert images(d,b)==images(ld,lb) and all(im in images(d,b) for im in images(sd,sb))
        assert not ld.get('skins') and not ld.get('animations')
        names={n['name'] for n in d['nodes']}
        assert {f'{digit}_{j}.{side}' for digit in ['thumb','index','middle','ring','little'] for j in range(1,4) for side in ['L','R']}<=names
        clips=d['animations'];assert len(clips)==8
        assert {a['name'].split('.')[-1] for a in clips}=={'idle','walk','run','attack','combo','cast','hurt','death'}
        for a in clips:
            for sampler in a['samplers']:
                times=[x[0] for x in values(d,b,sampler['input'])];assert times==sorted(set(times))
                assert all(math.isfinite(v) for row in values(d,b,sampler['output']) for v in row)
        for mesh in d['meshes']:
            for p in mesh['primitives']:
                a=p['attributes'];assert 'JOINTS_1' not in a
                assert all(abs(sum(w)-1)<1e-4 and min(w)>=0 for w in values(d,b,a['WEIGHTS_0']))
                assert max(max(j) for j in values(d,b,a['JOINTS_0']))<m['joints']
        assert hashlib.sha256((READY/(ident+'.glb')).read_bytes()).hexdigest()==m['animated_sha256']
        reports.append({'id':ident,'joints':m['joints'],'clips':len(clips),'images':images(d,b),'sha256':m['animated_sha256']})
    assert json.loads((READY/'tong_ling_yuan_wang-manifest.json').read_text(encoding='utf8'))['holding']['grip_fraction']==.5
    (BASE/'export-validation.json').write_text(json.dumps(reports,indent=2),encoding='utf8')
    print('PASS 5 Bosses: original topology/UV/maps; 30 finger joints each; 8 clips each; normalized 4-influence skins; static idle LOD; ape centre grip.')
if __name__=='__main__':main()
