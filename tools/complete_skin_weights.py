"""Repair only unweighted vertices using anatomical segments and surface neighbours."""
from mathutils import Vector

def repair(mesh,rig):
    bones=[b for b in rig.data.bones if b.use_deform]
    groups={b.name:mesh.vertex_groups.get(b.name) or mesh.vertex_groups.new(name=b.name) for b in bones}
    missing=[v.index for v in mesh.data.vertices if not any(g.weight>1e-8 for g in v.groups)]
    rows={}
    for index in missing:
        p=mesh.data.vertices[index].co
        distances=[]
        for b in bones:
            segment=b.tail_local-b.head_local
            t=max(0.,min(1.,(p-b.head_local).dot(segment)/max(segment.length_squared,1e-10)))
            d=(p-b.head_local-segment*t).length
            distances.append((d,b.name))
        closest=sorted(distances)[:4]
        weights={name:1/max(.018,d)**4 for d,name in closest};total=sum(weights.values())
        rows[index]={name:w/total for name,w in weights.items()}
    adjacency={i:set() for i in missing}
    for edge in mesh.data.edges:
        a,b=edge.vertices
        if a in adjacency:adjacency[a].add(b)
        if b in adjacency:adjacency[b].add(a)
    # Connected-surface smoothing removes hard per-face transitions at elbows.
    for _ in range(2):
        nxt={}
        for i,weights in rows.items():
            neighbours=[rows[j] for j in adjacency[i] if j in rows]
            accum={k:.7*v for k,v in weights.items()}
            if neighbours:
                for row in neighbours:
                    for k,v in row.items():accum[k]=accum.get(k,0)+.3*v/len(neighbours)
            top=sorted(accum.items(),key=lambda x:x[1],reverse=True)[:4];total=sum(v for _,v in top)
            nxt[i]={k:v/total for k,v in top}
        rows=nxt
    for i,row in rows.items():
        for name,w in row.items():groups[name].add([i],w,'REPLACE')
    return len(missing)
