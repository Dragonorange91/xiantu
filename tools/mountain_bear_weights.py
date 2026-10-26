# Analytic anatomical weights for the disconnected armoured source surface.
# Each side/limb is constrained to its own local chain to avoid cross-leg bleed.
mesh.parent = rig
mod=mesh.modifiers.new('MountainBearSkin','ARMATURE');mod.object=rig
for b in data.bones:
    if b.use_deform: mesh.vertex_groups.new(name=b.name)
def segment_distance(p,b):
    a=b.head_local;delta=b.tail_local-a
    t=max(0,min(1,(p-a).dot(delta)/max(delta.length_squared,1e-12)))
    return (p-(a+delta*t)).length
for v in mesh.data.vertices:
    p=v.co-Vector((0,0,.294189));side='L' if p.x<0 else 'R'
    names=['hips','spine','chest','neck','head']
    if abs(p.x)>.075 and p.z<.14:
        prefix='front' if p.y<.02 else 'hind'
        names += [prefix+'_'+part+'.'+side for part in ('upper','lower','foot')]
        if p.z<-.12: names=[prefix+'_'+part+'.'+side for part in ('upper','lower','foot')]
    if p.y>.36 and abs(p.x)<.085: names=['hips']+['tail_'+str(i) for i in range(4)]
    scores=sorted([(n,1/(segment_distance(v.co,data.bones[n])+.025)**4) for n in names],key=lambda x:x[1],reverse=True)[:4]
    total=sum(w for _,w in scores)
    for n,w in scores:mesh.vertex_groups[n].add([v.index],w/total,'REPLACE')
assert all(v.groups for v in mesh.data.vertices)
