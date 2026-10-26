"""Asset audit for the deliberate run-gait change; no production-check bypass."""
import json
import numpy as np
from audit_yanwei_animation_candidate import Glb, ROOT

OUT=ROOT/'asset_sources/yanwei-fox-run-v2-20261015'

def channel_map(g, clip):
    a=g.clips[clip]
    return {(g.names[c['target']['node']],c['target']['path']):
            (a['samplers'][c['sampler']],c) for c in a['channels']}

def main():
    base=Glb(ROOT/'asset_sources/yanwei-fox-optimize-20261015/yanwei_fox_optimized_candidate.glb')
    g=Glb(OUT/'yanwei_fox_run_v2.glb')
    production=Glb(OUT/'production-before-run-v2.glb' if (OUT/'production-before-run-v2.glb').exists()
                   else ROOT/'src/main/resources/assets/xiantu/models/entity/yanwei_fox.glb')
    assert g.nodes==production.nodes==base.nodes
    assert g.doc['materials']==production.doc['materials']
    assert g.image_info()==production.image_info()
    assert g.doc['skins'][0]['joints']==production.doc['skins'][0]['joints']
    assert len(g.doc['skins'][0]['joints'])==43
    assert np.array_equal(g.accessor(g.doc['skins'][0]['inverseBindMatrices']),
                          production.accessor(production.doc['skins'][0]['inverseBindMatrices']))
    p=g.doc['meshes'][0]['primitives'][0];pp=production.doc['meshes'][0]['primitives'][0]
    for name,index in p['attributes'].items():
        assert np.array_equal(g.accessor(index),production.accessor(pp['attributes'][name])),name
    assert np.array_equal(g.accessor(p['indices']),production.accessor(pp['indices']))
    assert g.clips.keys()==base.clips.keys()
    unchanged=[]
    for clip in base.clips:
        if clip=='yanwei_fox.run':continue
        ca,cb=channel_map(base,clip),channel_map(g,clip)
        assert ca.keys()==cb.keys()
        for target in ca:
            sa,_=ca[target];sb,_=cb[target]
            assert sa.get('interpolation','LINEAR')==sb.get('interpolation','LINEAR')
            for field in ('input','output'):
                assert np.array_equal(base.accessor(sa[field]),g.accessor(sb[field])),(clip,target,field)
        unchanged.append(clip)
    clip='yanwei_fox.run'
    duration=float(max(g.accessor(s['input'])[-1,0] for s in g.clips[clip]['samplers']))
    old_duration=float(max(base.accessor(s['input'])[-1,0] for s in base.clips[clip]['samplers']))
    assert duration==old_duration
    baseline_runtime=Glb(ROOT/'src/main/resources/assets/xiantu/models/entity/yanwei_fox.glb')
    old_min=[]
    for time in np.linspace(0,old_duration,round(old_duration*96)+1):
        old_min.append(float(baseline_runtime.skinned(baseline_runtime.matrices(clip,time))[:,1].min()))
    first=g.skinned(g.matrices(clip,0));last=g.skinned(g.matrices(clip,duration))
    seam=float(np.linalg.norm(first-last,axis=1).max())
    assert seam<1e-6,seam
    floor=[];changed=[];root_y=[]
    for time in np.linspace(0,duration,round(duration*96)+1):
        m=g.matrices(clip,time);v=g.skinned(m)
        assert np.isfinite(v).all()
        floor.append(float(v[:,1].min()))
        root_y.append(float(m[g.names.index('FoxAnimationRoot'),1,3]))
        old=base.skinned(base.matrices(clip,time))
        changed.append(float(np.linalg.norm(v-old,axis=1).max()))
    # The source asset itself extends below the entity origin; compare like-for-like.
    assert min(floor)>=min(old_min)-.01,(min(floor),min(old_min))
    assert max(root_y)-min(root_y)<.10,'excessive body bounce'
    assert max(changed)>.10,'new gait did not reach the runtime GLB'
    stats={}
    for name in ['Bone_015','Bone_021','Bone_026','Bone_031',
                 'Bone_014','Bone_020','Bone_025','Bone_030']:
        sampler,channel=channel_map(g,clip)[(name,'rotation')]
        q=g.accessor(sampler['output']).astype(float)
        q/=np.linalg.norm(q,axis=1,keepdims=True)
        node=g.nodes[g.names.index(name)]
        rest=np.asarray(node.get('rotation',[0,0,0,1]),dtype=float)
        rest/=np.linalg.norm(rest)
        dots=np.clip(np.abs(q@rest),0,1)
        maximum=float(np.degrees(2*np.arccos(dots)).max())
        if name in {'Bone_015','Bone_021','Bone_026','Bone_031'}:
            assert maximum<=23.0,(name,maximum)
        else:
            assert maximum<=15.5,(name,maximum)
        stats[name]={'max_rotation_from_rest_degrees':maximum}
    root_rotation=channel_map(g,clip)[('FoxAnimationRoot','translation')][0]
    root_y=g.accessor(root_rotation['output'])[:,1]
    bounce=float(root_y.max()-root_y.min())
    assert bounce<=.0161,bounce
    report={'status':'offline_asset_contract_and_96Hz_run_sampling_passed; Blender_preview_rendered; GFBS_game_test_pending',
            'candidate_sha256':g.sha256,'production_sha256':production.sha256,
            'vertices':len(g.accessor(p['attributes']['POSITION'])),'triangles':len(g.accessor(p['indices']))//3,
            'joints':43,'images':g.image_info(),'duration_seconds':duration,
            'unchanged_clips_exact_sampler_payload':unchanged,'loop_max_world_vertex_gap':seam,
            'lowest_surface_y_range':[min(floor),max(floor)],'root_vertical_span':float(max(root_y)-min(root_y)),
            'run_root_bounce_y_component':bounce,
            'production_baseline_lowest_surface_y_range':[min(old_min),max(old_min)],
            'max_world_vertex_change_from_old_run':max(changed),'hip_motion':stats}
    (OUT/'run-audit.json').write_text(json.dumps(report,indent=2),encoding='utf-8')
    print(json.dumps(report,indent=2))

if __name__=='__main__':main()
