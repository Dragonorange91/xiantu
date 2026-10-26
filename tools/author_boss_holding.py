"""Source-preserving weapon poses and grounded attack arcs; Blender MCP only."""
import bpy, math, json
from mathutils import Vector, Matrix
import refine_posture_grip as rp
from bl_ext.blender_org.animal_animator.core import composer

def curve(points,t):
    for (ta,a),(tb,b) in zip(points,points[1:]):
        if t<=tb:
            return Vector(a).lerp(Vector(b),rp.smooth(ta,tb,t)).normalized()
    return Vector(points[-1][1]).normalized()

def solve_axis(rig,axis0,desired):
    hand=rig.pose.bones['hand.R']
    basis=hand.matrix.to_3x3()@hand.bone.matrix_local.to_3x3().inverted()
    current=(basis@axis0).normalized()
    delta=current.rotation_difference(desired).to_matrix().to_4x4()
    matrix=hand.matrix.copy();head=matrix.translation.copy()
    hand.matrix=Matrix.Translation(head)@delta@Matrix.Translation(-head)@matrix
    bpy.context.view_layer.update()

def apply(ident):
    rig,mesh=rp.select_rig(ident)
    assert not mesh.get('holding_motion_v3'),'Restore/recalibrate before rebaking holding motion'
    axis0=Vector(rp.HOLDING[ident]['axis']).normalized();centre=Vector(mesh['grip_centre'])
    weapon={i for p in mesh.data.polygons if p.material_index!=0 for i in p.vertices}
    axial=[(mesh.data.vertices[i].co-centre).dot(axis0) for i in weapon]
    lo=-min(axial);hi=max(axial)
    radius=max(((mesh.data.vertices[i].co-centre)-axis0*a).length for i,a in zip(weapon,axial))
    default=tuple(axis0)
    if ident=='bai_zhan_jiang_hun':
        points=[(0,default),(.30,(-.55,-.20,.81)),(.58,(.4,-.85,-.34)),(.80,(-.65,-.3,-.7)),(1,default)]
    elif ident=='xing_yun_tian_jiang':
        points=[(0,default),(.30,(-.5,-.25,.83)),(.55,(-.12,-.99,.04)),(.7,(-.12,-.99,.04)),(1,default)]
    else:
        points=[(0,default),(.30,(.65,-.5,.57)),(.58,(.7,-.7,-.13)),(.8,(.8,-.4,-.3)),(1,default)]
    reports={}
    for clip in ('idle','walk','run','attack','combo','cast','hurt','death'):
        rp.ca.reset(rig);a=bpy.data.actions[ident+'.'+clip];composer.assign_action(rig,a)
        end=int(a.frame_range[1]);min_z=100;worst_frame=0
        # Capture all source curves before replacing any samples.
        source=[]
        for f in range(end+1):
            bpy.context.scene.frame_set(f)
            source.append({n:(rig.pose.bones[n].location.copy(),rig.pose.bones[n].rotation_quaternion.copy()) for n in ['arm_upper.R','arm_lower.R','hand.R']})
        for f in range(end+1):
            bpy.context.scene.frame_set(f)
            for n,(loc,q) in source[f].items():rig.pose.bones[n].location=loc;rig.pose.bones[n].rotation_quaternion=q
            t=f/max(end,1);phase=t if clip!='combo' else (t*2)%1
            if clip in ('attack','combo'):
                pulse=math.sin(math.pi*phase)**2
                rp.ca.rotate(rig,'arm_upper.R',(1,0,0),-20*pulse)
                rp.ca.rotate(rig,'arm_lower.R',(1,0,0),rp.HOLDING[ident]['forearm']-35*pulse)
                bpy.context.view_layer.update();desired=curve(points,phase)
            else:
                bpy.context.view_layer.update();hand=rig.pose.bones['hand.R']
                desired=(hand.matrix.to_3x3()@hand.bone.matrix_local.to_3x3().inverted()@axis0).normalized()
            # Rotate the actual wrist/weapon chain, not the model root or detached weapon.
            for _ in range(3):
                solve_axis(rig,axis0,desired)
                hand=rig.pose.bones['hand.R'];world_centre=hand.matrix@hand.bone.matrix_local.inverted()@centre
                allowance=max(.005,world_centre.z-.018-radius)
                z=max(-min(.98,allowance/max(hi,1e-6)),min(min(.98,allowance/max(lo,1e-6)),desired.z))
                if abs(z-desired.z)<1e-6:break
                h=Vector((desired.x,desired.y,0)).normalized()*math.sqrt(1-z*z);desired=Vector((h.x,h.y,z))
            for n in ('arm_upper.R','arm_lower.R','hand.R'):
                pb=rig.pose.bones[n]
                for prop in ('location','rotation_quaternion'):pb.keyframe_insert(data_path=prop,frame=f,group=n)
            bpy.context.view_layer.update();ev=mesh.evaluated_get(bpy.context.evaluated_depsgraph_get())
            low=min(ev.data.vertices[i].co.z for i in weapon)
            if low<min_z:min_z=low;worst_frame=f
        reports[clip]={'weapon_min_z':min_z,'worst_frame':worst_frame}
        assert min_z>-.005,(ident,clip,min_z)
    rp.ca.reset(rig);mesh['holding_motion_v3']=True
    (rp.OUT/(ident+'-holding-motion.json')).write_text(json.dumps(reports,indent=2),encoding='utf8')
    print('HOLDING',ident,reports)
