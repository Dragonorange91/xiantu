"""Read-only GLB skin/animation audit; metrics flag candidates, not aesthetic verdicts."""
from pathlib import Path
import json, re, sys
import numpy as np
from audit_yanwei_animation_candidate import Glb

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT/'work/qa/model-code-audit-20261024'
MODELS = ROOT/'src/main/resources/assets/xiantu/models/entity'

def inventory():
    java = (ROOT/'src/main/java/com/longju/xiantu/entity/CompletionBestiary.java').read_text(encoding='utf8')
    active = java.split('public static final List<Definition> ALL',1)[1]
    rows = []
    for line in active.splitlines():
        if 'new Definition(' not in line: continue
        parts=line.split('new Definition(',1)[1].split(')',1)[0].split(', ')
        rows.append(dict(id=parts[0].strip('"'),model=parts[1].strip('"'),scale=float(parts[7].removesuffix('F')),offset=0.))
    mapping={'yanwei_fox':('yanwei_fox',2.0,0.),'lie_ren_lang':('cracked_blade_wolf',1.7,.43),
             'lie_yan_zhu':('crag_rock_spider',1.4,.02), 'han_ya_lang':('cold_fang_wolf',2.175,0),
             'ting_yu_ya':('thunder_feather_raven',1.5,0),'qing_teng_lu':('vine_deer',2.203,0),
             'lei_jiao_lu':('thunder_deer',2.413,0),'yan_bei_ye_zhu':('rock_back_boar',2.772,0),
             'tie_ya_huan':('iron_tooth_badger',2.422,0),'tai_bei_xiong':('moss_back_bear',2.966,0),
             'zhen_yue_xiong':('mountain_bear',4.27,0),'hou_jiao_xi':('thick_horn_rhino',3.809,0),
             'jin_zong_shi':('gold_mane_lion',3.012,0),'chi_yao_shi':('red_sun_lion',3.020,0)}
    for model,(ident,scale,offset) in mapping.items():rows.append(dict(id=ident,model=model,scale=scale,offset=offset))
    assert len(rows)==33
    return rows

def skin_parts(g, matrices):
    points=[]
    for node in g.nodes:
        if 'mesh' not in node:continue
        skin=g.doc['skins'][node['skin']]
        inv=g.accessor(skin['inverseBindMatrices']).reshape(-1,4,4).transpose(0,2,1)
        transforms=matrices[skin['joints']]@inv
        for p in g.doc['meshes'][node['mesh']]['primitives']:
            a=p['attributes'];pos=g.accessor(a['POSITION']);j=g.accessor(a['JOINTS_0']).astype(int);w=g.accessor(a['WEIGHTS_0']).astype(float)
            assert np.all(j>=0) and np.all(j<len(skin['joints']))
            assert np.all(w>=0) and np.max(np.abs(w.sum(axis=1)-1))<.0001
            weighted=np.sum(transforms[j]*w[:,:,None,None],axis=1)
            result=np.einsum('vij,vj->vi',weighted,np.c_[pos,np.ones(len(pos))])[:,:3]
            assert np.isfinite(result).all()
            points.append(result)
    return np.concatenate(points)

def run(row):
    g=Glb(MODELS/(row['model']+'.glb'));scale=row['scale']
    # An empty clip evaluates authored rest matrices with the same hierarchy.
    g.clips['_rest']={'channels':[],'samplers':[]}
    rest=skin_parts(g,g.matrices('_rest',0))
    edges=[];offset=0;triangles=0;degenerate=0
    for node in g.nodes:
        if 'mesh' not in node:continue
        for p in g.doc['meshes'][node['mesh']]['primitives']:
            assert p.get('mode',4)==4
            n=len(g.accessor(p['attributes']['POSITION']));ix=g.accessor(p['indices']).reshape(-1,3).astype(int)
            assert ix.min()>=0 and ix.max()<n
            tri=rest[offset+ix];area=np.linalg.norm(np.cross(tri[:,1]-tri[:,0],tri[:,2]-tri[:,0]),axis=1)
            degenerate+=int(np.sum(area<1e-10));triangles+=len(ix)
            for a,b in ((0,1),(1,2),(2,0)):edges.append(ix[:,[a,b]]+offset)
            offset+=n
    edges=np.unique(np.sort(np.concatenate(edges),axis=1),axis=0)
    lengths=np.linalg.norm(rest[edges[:,0]]-rest[edges[:,1]],axis=1)
    valid=lengths>1e-5;edges=edges[valid];lengths=lengths[valid]
    record={**row,'sha256':g.sha256,'bytes':g.path.stat().st_size,'triangles':triangles,
        'vertices':len(rest),'joints':sum(len(s['joints']) for s in g.doc['skins']),
        'degenerate_triangles':degenerate,'images':g.image_info(),'materials':g.doc.get('materials',[]),
        'rest_min':(rest.min(axis=0)*scale+np.array([0,row['offset']*scale,0])).tolist(),
        'rest_max':(rest.max(axis=0)*scale+np.array([0,row['offset']*scale,0])).tolist(),'clips':[]}
    for name,clip in g.clips.items():
        if name=='_rest':continue
        duration=max(float(g.accessor(s['input'])[-1,0]) for s in clip['samplers'])
        targets=[(c['target']['node'],c['target']['path']) for c in clip['channels']]
        assert len(targets)==len(set(targets)),(name,'duplicate target channel')
        qerr=0
        for c in clip['channels']:
            s=clip['samplers'][c['sampler']];v=g.accessor(s['output']);ts=g.accessor(s['input'])[:,0]
            assert np.all(np.diff(ts)>0) and np.isfinite(v).all()
            if c['target']['path']=='rotation':qerr=max(qerr,float(np.max(np.abs(np.linalg.norm(v,axis=1)-1))))
        first=skin_parts(g,g.matrices(name,0));last=skin_parts(g,g.matrices(name,duration))
        stats={'name':name,'seconds':duration,'channels':len(targets),'quat_norm_error':qerr,
               'loop_vertex_error_blocks':float(np.max(np.linalg.norm(first-last,axis=1))*scale)}
        max_stretch=0;over2=0;miny=1e9;maxy=-1e9;motion=0;growth=0;large_edges=0;worst=None
        # Uniform 24 Hz sampling also covers the shared transition endpoints.
        for t in np.linspace(0,duration,max(3,round(duration*24)+1)):
            pts=skin_parts(g,g.matrices(name,t));current=np.linalg.norm(pts[edges[:,0]]-pts[edges[:,1]],axis=1);ratios=current/lengths
            increases=(current-lengths)*scale
            large_edges=max(large_edges,int(np.sum((ratios>2)&(increases>.05))))
            if increases.max()>growth:
                ei=int(np.argmax(increases));growth=float(increases[ei]);worst=dict(time=float(t),vertices=edges[ei].tolist(),rest_blocks=float(lengths[ei]*scale),deformed_blocks=float(current[ei]*scale))
            max_stretch=max(max_stretch,float(ratios.max()));over2=max(over2,int(np.sum(ratios>2)))
            miny=min(miny,float(pts[:,1].min()+row['offset'])*scale);maxy=max(maxy,float(pts[:,1].max()+row['offset'])*scale)
            motion=max(motion,float(np.linalg.norm(pts-first,axis=1).max())*scale)
        stats.update(max_edge_stretch=max_stretch,edges_over_2x=over2,max_edge_growth_blocks=growth,edges_over_2x_and_5cm=large_edges,worst_edge=worst,min_y_blocks=miny,max_y_blocks=maxy,max_vertex_motion_blocks=motion)
        record['clips'].append(stats)
    return record

if __name__=='__main__':
    OUT.mkdir(parents=True,exist_ok=True);rows=[]
    for row in inventory():
        result=run(row);rows.append(result)
        (OUT/(row['id']+'.json')).write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf8')
        print(row['id'],result['triangles'],'tris',result['joints'],'joints',len(result['clips']),'clips',flush=True)
    (OUT/'models.json').write_text(json.dumps(rows,ensure_ascii=False,indent=2),encoding='utf8')
