"""Blender MCP only. Source-preserving head/neck correction with local ARAP relaxation.

The face and disconnected ornaments are rigid constraints, never a height-gradient twist.
Only the connecting neck surface is allowed to relax. Hand and weapon vertices are untouched.
"""
import bpy, math, json
import numpy as np
from mathutils import Vector, Matrix
import completion_authoring as ca
import refine_posture_grip as rp
import audit_original_boss_hands as audit

OUT=ca.ROOT/'asset_sources/boss-combat-v4-20261018'

def repair(ident):
    rig,mesh=rp.select_rig(ident)
    with bpy.data.libraries.load(str(rp.OUT/'baseline-library.blend'),link=False) as (src,dst):
        dst.objects=[ident+'_mesh']
    source=dst.objects[0]
    p=np.array([v.co[:] for v in source.data.vertices],dtype=float)
    body={i for f in source.data.polygons if f.material_index==0 for i in f.vertices}
    edges=np.array([e.vertices[:] for e in source.data.edges if all(i in body for i in e.vertices)])
    adj={i:[] for i in body}
    for a,b in edges:adj[a].append(b);adj[b].append(a)
    remaining=set(body);parts=[]
    while remaining:
        q=[remaining.pop()];part=[]
        while q:
            i=q.pop();part.append(i)
            for j in adj[i]:
                if j in remaining:remaining.remove(j);q.append(j)
        parts.append(part)
    s=rp.SPECS[ident]
    bodyrot=np.array(Matrix.Rotation(math.radians(-s['yaw']),3,'Z'))
    headrot=np.array(Matrix.Rotation(math.radians(-s['head']),3,'Z'))
    # Face, jaw, ears and crown stay one rigid surface, including the low chin.
    neck_low=.645 if ident=='tong_ling_yuan_wang' else .710
    head_low=.735 if ident=='tong_ling_yuan_wang' else .728
    radius=.155 if ident=='tong_ling_yuan_wang' else .120
    central=np.abs(p[:,0])<radius
    head=(p[:,2]>=head_low)&central
    head |= p[:,2]>.805
    flexible=(p[:,2]>neck_low)&(p[:,2]<.805)&(np.abs(p[:,0])<radius+.045)&~head
    q=p@bodyrot.T
    q[head]=p[head]@headrot.T
    free=set(np.flatnonzero(flexible))&body
    # Small disconnected beads/armour/hair pieces must remain rigid, not stretch individually.
    ornaments=set()
    for part in parts:
        if len(part)>400:continue
        centre=p[part].mean(axis=0)
        necklace=ident=='tong_ling_yuan_wang' and len(part)<=32 and .65<centre[2]<.83
        if necklace:ornaments.update(part)
        ishead=not necklace and (centre[2]>.805 or (centre[2]>.745 and abs(centre[0])<radius*.65 and centre[1]<-.035))
        rot=headrot if ishead else bodyrot
        q[part]=p[part]@rot.T
        free.difference_update(part)
        head[part]=ishead
    ids=np.array(sorted(free));index={i:j for j,i in enumerate(ids)}
    # Uniform ARAP avoids cotangent instability on thin imported triangles.
    matrix=np.zeros((len(ids),len(ids)))
    for i in ids:
        row=index[i];matrix[row,row]=len(adj[i])
        for j in adj[i]:
            if j in index:matrix[row,index[j]]-=1
    inverse=np.linalg.inv(matrix)
    a,b=edges[:,0],edges[:,1];rest=p[a]-p[b]
    for iteration in range(24):
        current=q[a]-q[b]
        cov=np.zeros((len(p),3,3));outer=np.einsum('ni,nj->nij',current,rest)
        np.add.at(cov,a,outer);np.add.at(cov,b,outer)
        u,_,vt=np.linalg.svd(cov)
        d=np.linalg.det(u@vt);u[:,:,2]*=d[:,None];rot=u@vt
        rhs=np.zeros((len(ids),3))
        for i in ids:
            row=index[i]
            for j in adj[i]:
                rhs[row]+=.5*(rot[i]+rot[j])@(p[i]-p[j])
                if j not in index:rhs[row]+=q[j]
        q[ids]=inverse@rhs
    # Restore the complete chest and necklace from source, including the region previously
    # modified by the rejected broad transition. Hands below this boundary remain untouched.
    affected={i for i in body if p[i,2]>.60}
    for i in affected:mesh.data.vertices[i].co=q[i]
    # Rigid face weights include jaw. Collar/armour keep chest, rather than neck leakage.
    for i in affected:
        rp.clear_weights(mesh,i)
        if head[i]:rp.weight(mesh,i,'head')
        elif ident=='xing_yun_tian_jiang' and p[i,2]<.711 and abs(p[i,0])<.115 and p[i,1]<-.035:
            rp.weight(mesh,i,'chest')
        elif i in free or i in ornaments:
            t=rp.smooth(neck_low,head_low,float(p[i,2])) if i in free else 0
            rp.weight(mesh,i,'chest',1-t)
            if t:rp.weight(mesh,i,'head',t)
        else:
            for g in source.data.vertices[i].groups:
                rp.weight(mesh,i,source.vertex_groups[g.group].name,g.weight)
    # One head rest orientation, independent of the former height-gradient edit.
    bpy.context.view_layer.objects.active=rig;bpy.ops.object.mode_set(mode='EDIT')
    for name in ('neck','head'):
        bone=rig.data.edit_bones[name]
        # All original centres have x=0; rotate original y about the vertical centreline.
        z0=.70583 if name=='neck' else .77583
        bone.head=Vector((0,0,z0)) if name=='neck' else Vector(headrot@np.array([0,-.012,z0]))
        bone.tail=Vector(headrot@np.array([0,-.012,.77583 if name=='neck' else .89583]))
    bpy.ops.object.mode_set(mode='OBJECT');mesh.data.update()
    lengths=np.linalg.norm(rest,axis=1);strain=np.abs(np.linalg.norm(q[a]-q[b],axis=1)/np.maximum(lengths,1e-8)-1)
    face_edges=head[a]&head[b]&(lengths>1e-6)
    report={'id':ident,'free_neck_vertices':len(ids),'face_vertices':int(head[list(body)].sum()),
        'face_max_edge_strain':float(strain[face_edges].max()),'neck_p95_edge_strain':float(np.quantile(strain,.95)),
        'uv_identical':[[tuple(x.uv) for x in uv.data] for uv in source.data.uv_layers]==[[tuple(x.uv) for x in uv.data] for uv in mesh.data.uv_layers],
        'images_identical':audit.texture_hashes(source)==audit.texture_hashes(mesh),
        'necklace_source_vertices':len(ornaments),
        'necklace_rigid_error':float(np.linalg.norm(q[list(ornaments)]-p[list(ornaments)]@bodyrot.T,axis=1).max()) if ornaments else 0}
    assert report['face_max_edge_strain']<1e-5 and report['uv_identical'] and report['images_identical'],report
    bpy.data.objects.remove(source,do_unlink=True)
    mesh['surface_v4']=True
    (OUT/(ident+'-surface-audit.json')).write_text(json.dumps(report,indent=2),encoding='utf8')
    ca.reset(rig)
    for label,offset in [('front',(0,-3,.2)),('side',(3,-1,.2))]:
        audit.capture(mesh,OUT/(ident+'-repaired-'+label+'.png'),(0,0,.78),offset,.54)
    print(report)
