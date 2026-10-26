"""Model-specific rest-pose calibration, run via Blender MCP before candidate export."""
import bpy, math, json
import numpy as np
from mathutils import Vector,Matrix,Quaternion
import completion_authoring as ca
from refine_posture_grip import OUT, smooth, clear_weights, weight, segment_distance
from bl_ext.blender_org.animal_animator.core import composer

def select(ident):
    r=bpy.data.objects[ident+'_rig'];m=bpy.data.objects[ident+'_mesh']
    bpy.context.window.scene=next((s for s in r.users_scene if s.name.startswith('CompletionRig_')),r.users_scene[0])
    ca.reset(r)
    for t in r.animation_data.nla_tracks:t.mute=True
    bpy.ops.object.select_all(action='DESELECT');r.select_set(True);bpy.context.view_layer.objects.active=r
    return r,m

def remove_tail_weights(m,r):
    tails={g.index for g in m.vertex_groups if g.name.startswith('tail')}
    for v in m.data.vertices:
        remaining=[(m.vertex_groups[w.group].name,w.weight) for w in v.groups if w.group not in tails]
        if not any(w.group in tails for w in v.groups):continue
        clear_weights(m,v.index);total=sum(w for n,w in remaining)
        if total>.05:
            for n,w in remaining:weight(m,v.index,n,w/total)
        else:weight(m,v.index,'hips')

def tail_weights(m,indices,points,names,base):
    for i in indices:
        p=m.data.vertices[i].co
        ranked=sorted((segment_distance(p,a,b)[0],n) for n,a,b in zip(names,points,points[1:]))[:2]
        clear_weights(m,i);ws=[1/max(.008,d)**3 for d,n in ranked];total=sum(ws)
        blend=smooth(base,base+.045,p.y)
        if blend<1:weight(m,i,'hips',1-blend)
        for (d,n),w in zip(ranked,ws):weight(m,i,n,blend*w/total)

def ape_tail():
    ident='tong_ling_yuan_wang';r,m=select(ident)
    assert not m.get('tail_calibrated')
    body={i for p in m.data.polygons if p.material_index==0 for i in p.vertices}
    ids=[i for i in body if m.data.vertices[i].co.y>.065 and m.data.vertices[i].co.z<.48]
    remove_tail_weights(m,r)
    for i in ids:
        p=m.data.vertices[i].co;p.x-=.70*(p.y-.065)*smooth(.065,.12,p.y)
    pts=[Vector(p) for p in [(0,.07,.40),(0,.19,.21),(0,.30,.17),(0,.335,.285),(0,.265,.33)]]
    names=['tail_'+str(i) for i in range(4)]
    bpy.ops.object.mode_set(mode='EDIT')
    for n,a,b in zip(names,pts,pts[1:]):r.data.edit_bones[n].head=a;r.data.edit_bones[n].tail=b
    bpy.ops.object.mode_set(mode='OBJECT');tail_weights(m,ids,pts,names,.065)
    m['tail_calibrated']=True
    print('APE_TAIL',len(ids),'vertices fitted to curved centreline')

def bird(ident):
    r,m=select(ident);assert not m.get('flight_calibrated')
    before=np.array([list(v.co) for v in m.data.vertices])
    # The bat source is already bilateral. Do not manufacture a correction for it.
    if ident!='zi_dian_fu':
        bins=np.linspace(.10,.48,9);bias=[]
        for x in bins:
            left=before[(abs(before[:,0]-x)<.055)&(before[:,2]>.32)]
            right=before[(abs(before[:,0]+x)<.055)&(before[:,2]>.32)]
            bias.append((np.median(left[:,1])-np.median(right[:,1]))*.5 if len(left)>3 and len(right)>3 else 0)
        head=before[(before[:,1]<-.30)&(before[:,2]>.40)]
        headx=float(np.median(head[:,0]))
        ys=np.linspace(.12,float(before[:,1].max()),8)
        centres=[]
        for y in ys:
            p=before[(abs(before[:,1]-y)<.05)&(before[:,2]<.38)]
            centres.append(float(np.median(p[:,0])) if len(p) else 0)
        def calibrated(p):
            p=Vector(p);ax=abs(p.x)
            wing=smooth(.1,.19,ax)*smooth(.25,.4,p.z)
            p.y-=np.interp(ax,bins,bias)*(1 if p.x>0 else -1)*wing
            p.x-=headx*smooth(.19,.34,-p.y)*smooth(.3,.45,p.z)
            tail=smooth(.10,.20,p.y)*(1-smooth(.32,.43,p.z))
            p.x-=np.interp(p.y,ys,centres)*tail
            return p
        for v in m.data.vertices:v.co=calibrated(v.co)
        bpy.ops.object.mode_set(mode='EDIT')
        for b in r.data.edit_bones:
            b.head=calibrated(b.head);b.tail=calibrated(b.tail)
        bpy.ops.object.mode_set(mode='OBJECT')
        # Fit the chain to the visible feather fan, rather than a generic empty-space path.
        pts=[]
        for y in [.12,.29,float(before[:,1].max())-.04]:
            p=np.array([list(v.co) for v in m.data.vertices if abs(v.co.y-y)<.045 and v.co.z<.38])
            assert len(p)>3
            pts.append(Vector((0,y,float(np.median(p[:,2])))))
        bpy.ops.object.mode_set(mode='EDIT')
        for i in range(2):r.data.edit_bones['tail_'+str(i)].head=pts[i];r.data.edit_bones['tail_'+str(i)].tail=pts[i+1]
        for side,sign in [('L',1),('R',-1)]:
            b=r.data.edit_bones['tail_feather.'+side];b.head=pts[0]+Vector((sign*.025,0,0));b.tail=pts[-1]+Vector((sign*.06,0,0))
        bpy.ops.object.mode_set(mode='OBJECT')
        remove_tail_weights(m,r)
        ids=[v.index for v in m.data.vertices if v.co.y>.12 and v.co.z<.38]
        tail_weights(m,ids,pts,['tail_0','tail_1'],.12)
        report={'wing_depth_bias':list(map(float,bias)),'head_offset':headx,'tail_points':[list(p) for p in pts]}
    else:report={'geometry':'bilateral source preserved'}
    # Bank is handled by server heading; forward flight gets a separate body pitch.
    # Preserve the approved wing cycle and its amplitude, changing only root posture.
    for clip,angle in [('fly',28),('glide',32),('takeoff',28),('land',28),('attack',28)]:
        a=bpy.data.actions.get(ident+'.'+clip)
        if not a:continue
        ca.reset(r);composer.assign_action(r,a);end=round(a.frame_range[1]);samples=[]
        for f in range(end+1):
            bpy.context.scene.frame_set(f);samples.append((r.pose.bones['root'].location.copy(),r.pose.bones['root'].rotation_quaternion.copy()))
        axis=r.data.bones['root'].matrix_local.to_3x3().inverted()@Vector((1,0,0))
        for f,(loc,q) in enumerate(samples):
            t=f/max(1,end);blend=smooth(0,1,t) if clip=='takeoff' else (1-smooth(0,1,t) if clip=='land' else 1)
            p=r.pose.bones['root'];p.location=loc;p.rotation_quaternion=Quaternion(axis,math.radians(angle*blend))@q
            p.keyframe_insert(data_path='rotation_quaternion',frame=f,group=p.name)
    ca.reset(r);m['flight_calibrated']=True
    (OUT/(ident+'-flight-calibration.json')).write_text(json.dumps(report,indent=2),encoding='utf8')
    print('BIRD_CALIBRATED',ident,report)
