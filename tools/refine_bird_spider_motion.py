"""Anatomy-specific repairs, invoked in Blender through MCP."""
import bpy
import math
import copy
from mathutils import Quaternion, Vector
from bl_ext.blender_org.animal_animator.core import composer, library, posing, profiles
from refine_quadruped_motion import curves, span

def bake(rig, mesh, action, start, end, edit, grounded=True):
    scene=bpy.context.scene
    composer.assign_action(rig,action)
    samples=[]
    for frame in range(start,end):
        scene.frame_set(frame)
        samples.append({p.name:p.matrix_basis.decompose() for p in rig.pose.bones})
    for bag in curves(action):
        for fc in list(bag.fcurves):bag.fcurves.remove(fc)
    for frame in range(start,end+1):
        for p in rig.pose.bones:
            loc,rot,scale=samples[(frame-start)%(end-start)][p.name]
            p.rotation_mode='QUATERNION'
            p.location=loc;p.rotation_quaternion=rot;p.scale=scale
            edit(p,frame-start,end-start)
            for prop in ('location','rotation_quaternion','scale'):p.keyframe_insert(data_path=prop,frame=frame,group=p.name)
    if grounded:
        rig.animation_data.action=None;posing.reset_pose(rig);scene.frame_set(0)
        ev=mesh.evaluated_get(bpy.context.evaluated_depsgraph_get())
        floor=min((ev.matrix_world @ v.co).z for v in ev.data.vertices)
        composer.assign_action(rig,action)
        correction=[]
        for frame in range(start,end+1):
            scene.frame_set(frame);ev=mesh.evaluated_get(bpy.context.evaluated_depsgraph_get())
            correction.append((rig.pose.bones['root'].location.copy(),max(0,floor-min((ev.matrix_world @ v.co).z for v in ev.data.vertices))))
        up=rig.data.bones['root'].matrix_local.to_3x3().inverted() @ rig.matrix_world.to_3x3().inverted() @ Vector((0,0,1))
        correction[-1]=correction[0]
        for i,(loc,lift) in enumerate(correction):
            p=rig.pose.bones['root'];p.location=loc+up*lift;p.keyframe_insert(data_path='location',frame=start+i,group=p.name)
    for bag in curves(action):
        for fc in bag.fcurves:
            if len(fc.keyframe_points)>1:
                fc.keyframe_points[-1].co.y=fc.keyframe_points[0].co.y
            for k in fc.keyframe_points:k.interpolation='LINEAR'
    for t in rig.animation_data.nla_tracks:
        for s in t.strips:
            if s.action==action:s.action_frame_end=end;s.frame_end=s.frame_start+(end-start)
    observed={p.name:[] for p in rig.pose.bones}
    composer.assign_action(rig,action)
    for f in range(start,end+1):
        scene.frame_set(f)
        for p in rig.pose.bones:observed[p.name].append(p.matrix_basis.to_quaternion())
    rig.animation_data.action=None;posing.reset_pose(rig);scene.frame_set(0)
    return {n:round(span(q),3) for n,q in observed.items()}

def bird(rig,mesh,actions):
    # The stock constant PERCH_NEUTRAL channel overwrites all six gait bones.
    # Generate legs from the installed plugin without that conflicting channel.
    mapping={'HIPS':[['hips',1]],'SPINE':[['spine',.6],['chest',.4]],'NECK':[['neck',1]],'HEAD':[['head',1]],'BEAK':[['beak',1]],'TAIL':[['tail_0',.6],['tail_1',.4]]}
    for side in ('L','R'):
        mapping['WING.'+side]=[['wing_upper.'+side,1],['wing_middle.'+side,.6],['wing_tip.'+side,.4]]
        mapping['TAIL_FEATHER.'+side]=[['tail_feather.'+side,1]]
        for slot,n in [('LEG_UPPER','leg_upper'),('LEG_LOWER','leg_lower'),('FOOT','foot')]:mapping[slot+'.'+side]=[[n+'.'+side,1]]
    anim=copy.deepcopy(library.anim_by_id(['bird'],'walk_strut'))
    anim['gait']['channels']=[c for c in anim['gait']['channels'] if c['pose']!='PERCH_NEUTRAL']
    result=composer.build_action(rig,mapping,anim,['bird'],intensity=.65,unit=.84,name_prefix='readable')
    assert not result.missing_slots,result.missing_slots
    generated=result.action
    # Transfer only the new leg curves; preserve accepted wing folding/head bob.
    target=actions['walk'];bag=next(curves(target))
    for fc in list(bag.fcurves):
        if any('"'+s in fc.data_path for s in ['leg_upper.','leg_lower.','foot.']):bag.fcurves.remove(fc)
    for source in curves(generated):
        for fc in source.fcurves:
            if any('"'+s in fc.data_path for s in ['leg_upper.','leg_lower.','foot.']):
                dst=bag.fcurves.new(data_path=fc.data_path,index=fc.array_index)
                for k in fc.keyframe_points:
                    nk=dst.keyframe_points.insert(k.co.x,k.co.y);nk.interpolation=k.interpolation
    for t in list(rig.animation_data.nla_tracks):
        if any(s.action==generated for s in t.strips):rig.animation_data.nla_tracks.remove(t)
    report={}
    for clip,end in [('idle',110),('walk',28),('fly',32),('glide',90)]:
        def edit(p,f,length):
            if p.name in ('tail_0','tail_1'):
                i=int(p.name[-1]);axis=p.bone.matrix_local.to_3x3().inverted() @ Vector((1,0,0))
                sway=math.radians((5+2*i) if clip in ('idle','walk') else (3+2*i))*math.sin(math.tau*(2 if clip=='idle' else 1)*f/length-i*.6)
                p.rotation_quaternion=p.rotation_quaternion @ Quaternion(axis,sway)
        report[clip]=bake(rig,mesh,actions[clip],0,end,edit,clip in ('idle','walk'))
    return report

def spider(rig,mesh,actions):
    report={}
    for clip in ('walk','run'):
        for p in rig.pose.bones:p.rotation_mode='XYZ';p.rotation_euler=(0,0,0)
        action=actions[clip];start,end=map(int,action.frame_range)
        def edit(p,f,length):
            if p.name.startswith('leg_'):
                e=p.rotation_quaternion.to_euler('XYZ')
                e.y*=1.7 if clip=='walk' else 1.35
                if not p.name.endswith(('_mid','_foot')):e.z*=1.15 if clip=='walk' else 1.0
                p.rotation_quaternion=e.to_quaternion()
        report[clip]=bake(rig,mesh,action,start,end,edit)
    # Original combat clips remain Euler; keep that authoring representation.
    # Exporter evaluates each action with the rig mode: convert the two repaired
    # clips back to Euler so both old and new actions remain valid in the blend.
    for clip in ('walk','run'):
        action=actions[clip];composer.assign_action(rig,action)
        for p in rig.pose.bones:p.rotation_mode='QUATERNION'
        saved=[]
        for f in range(int(action.frame_range[0]),int(action.frame_range[1])+1):
            bpy.context.scene.frame_set(f);saved.append((f,{p.name:p.rotation_quaternion.to_euler('XYZ') for p in rig.pose.bones}))
        for bag in curves(action):
            for fc in list(bag.fcurves):
                if fc.data_path.endswith('rotation_quaternion'):bag.fcurves.remove(fc)
        for f,rots in saved:
            for p in rig.pose.bones:p.rotation_mode='XYZ';p.rotation_euler=rots[p.name];p.keyframe_insert(data_path='rotation_euler',frame=f,group=p.name)
    rig.animation_data.action=None
    for p in rig.pose.bones:p.rotation_mode='XYZ';p.rotation_euler=(0,0,0);p.location=(0,0,0);p.scale=(1,1,1)
    return report
