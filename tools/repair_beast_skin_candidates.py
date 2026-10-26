"""Build reversible local weight-smoothing candidates. Never writes runtime assets.

Keep geometry, UV, normals, rig, animation and embedded images byte-identical.
Only high-strain neighbourhoods are relaxed; finger/weapon weights stay anchored.
"""
import json,struct
from pathlib import Path
import numpy as np
from audit_beast_models import Glb,MODELS,OUT,ROOT,inventory,skin_parts

def candidate(row):
    # Re-running this tool must not smooth an already integrated repair again.
    # Registered patches always start from their pinned, unmodified runtime source.
    manifest=ROOT/'asset_sources/skin-repair-20261024/integration-manifest.json'
    entries=json.loads(manifest.read_text(encoding='utf8')) if manifest.exists() else {}
    registered=entries.get(row['model'])
    src=Path(registered['source']) if registered else MODELS/(row['model']+'.glb')
    g=Glb(src)
    if registered: assert g.sha256.lower()==registered['source_sha256'].lower(), 'Pinned skin source changed'
    g.clips['_rest']={'channels':[],'samplers':[]}
    rest=skin_parts(g,g.matrices('_rest',0));scale=row['scale']
    assert len(g.doc['skins'])==1
    primitives=[p for n in g.nodes if 'mesh' in n for p in g.doc['meshes'][n['mesh']]['primitives']]
    indices=[];joints=[];ws=[];offset=0
    for p in primitives:
        a=p['attributes'];indices.append(g.accessor(p['indices']).reshape(-1,3).astype(int)+offset)
        joints.append(g.accessor(a['JOINTS_0']).astype(int));ws.append(g.accessor(a['WEIGHTS_0']).astype(float));offset+=len(ws[-1])
    ix=np.concatenate(indices);j=np.concatenate(joints);w=np.concatenate(ws);skin=g.doc['skins'][0]
    names=[g.nodes[i]['name'] for i in skin['joints']]
    original=np.zeros((len(w),len(names)));np.add.at(original,(np.arange(len(w))[:,None],j),w)
    edges=np.unique(np.sort(np.concatenate([ix[:,[0,1]],ix[:,[1,2]],ix[:,[2,0]]]),axis=1),axis=0)
    lengths=np.linalg.norm(rest[edges[:,0]]-rest[edges[:,1]],axis=1)*scale
    bad=np.zeros(len(edges),dtype=bool)
    for name,clip in g.clips.items():
        if name=='_rest':continue
        dur=max(float(g.accessor(s['input'])[-1,0]) for s in clip['samplers'])
        for t in np.linspace(0,dur,max(3,round(dur*12)+1)):
            pts=skin_parts(g,g.matrices(name,t));length=np.linalg.norm(pts[edges[:,0]]-pts[edges[:,1]],axis=1)*scale
            bad|=(length>np.maximum(lengths*2.5,lengths+.09))&(lengths<.3)
    # Weld positions for smoothing across UV splits without changing topology.
    _,group=np.unique(np.round(rest,6),axis=0,return_inverse=True);n=int(group.max())+1
    counts=np.bincount(group);initial=np.zeros((n,len(names)));np.add.at(initial,group,original);initial/=counts[:,None]
    links=np.unique(np.sort(group[edges],axis=1),axis=0);links=links[links[:,0]!=links[:,1]]
    marked=np.zeros(n,dtype=bool);marked[group[edges[bad]].reshape(-1)]=True
    # One bounded ring avoids a hard boundary around a repaired patch.
    adjacent=marked[links[:,0]]|marked[links[:,1]];marked[links[adjacent].reshape(-1)]=True
    finger=[i for i,name in enumerate(names) if any(name.startswith(x) for x in ('thumb_','index_','middle_','ring_','little_'))]
    if finger:marked[initial[:,finger].sum(axis=1)>.05]=False
    points=np.zeros((n,3));np.add.at(points,group,rest);points/=counts[:,None]
    factor=1/np.maximum(.015,np.linalg.norm(points[links[:,0]]-points[links[:,1]],axis=1)*scale)
    denom=np.zeros(n);np.add.at(denom,links[:,0],factor);np.add.at(denom,links[:,1],factor)
    values=initial.copy()
    for _ in range(35):
        accum=np.zeros_like(values)
        np.add.at(accum,links[:,0],values[links[:,1]]*factor[:,None]);np.add.at(accum,links[:,1],values[links[:,0]]*factor[:,None])
        smooth=accum/np.maximum(1e-9,denom[:,None]);values[marked]=.08*initial[marked]+.92*smooth[marked]
    output=original.copy();output[marked[group]]=values[group[marked[group]]]
    chosen=np.argsort(-output,axis=1,kind='stable')[:,:4];weights=np.take_along_axis(output,chosen,axis=1);weights/=weights.sum(axis=1)[:,None]
    changed=np.any(np.abs(output-original)>1e-6,axis=1)
    raw=bytearray(src.read_bytes());jsonlen=struct.unpack_from('<I',raw,12)[0];binstart=28+jsonlen
    offset=0
    for p in primitives:
        a=p['attributes'];count=len(g.accessor(a['POSITION']));mask=changed[offset:offset+count]
        for index,data in ((a['JOINTS_0'],chosen),(a['WEIGHTS_0'],weights)):
            acc=g.doc['accessors'][index];view=g.doc['bufferViews'][acc['bufferView']]
            dtype={5121:'u1',5123:'<u2',5126:'<f4'}[acc['componentType']];assert not acc.get('normalized',False)
            size=np.dtype(dtype).itemsize;start=binstart+view.get('byteOffset',0)+acc.get('byteOffset',0)
            array=np.ndarray((acc['count'],4),dtype=dtype,buffer=raw,offset=start,strides=(view.get('byteStride',4*size),size))
            array[mask]=data[offset:offset+count][mask]
        offset+=count
    dest=OUT/'candidates';dest.mkdir(exist_ok=True)
    path=dest/(row['model']+'.glb');path.write_bytes(raw)
    check=Glb(path);check.clips['_rest']={'channels':[],'samplers':[]}
    assert np.max(np.abs(skin_parts(check,check.matrices('_rest',0))-rest))<1e-6
    assert check.doc==g.doc or {**check.doc,'animations':g.doc['animations']}==g.doc
    record=dict(id=row['id'],source_sha256=g.sha256,candidate_sha256=check.sha256,modified_vertices=int(changed.sum()),bad_edges=int(bad.sum()),candidate=str(path))
    (dest/(row['id']+'.json')).write_text(json.dumps(record,indent=2));print(record,flush=True)

if __name__=='__main__':
    import sys
    for row in inventory():
        if row['id'] in sys.argv[1:]:candidate(row)
