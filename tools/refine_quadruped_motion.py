"""Blender MCP animation-only repair; keep rigs, weights, materials and combat clips.

Call refine(rig, mesh, actions) after Animal Animator authoring and before export.
Source cycle lengths are 100/40/20; follower padding is not part of the gait.
"""
import math
import bpy
from mathutils import Quaternion, Vector
from bl_ext.blender_org.animal_animator.core import composer, posing

REVISION = 'readable-gait-tail-v1'
PROFILES = {
    'chi_yao_shi': (28, 26, 35, 32, 1.0),
    'jin_zong_shi': (28, 26, 35, 32, 1.0),
    'tie_ya_huan': (28, 26, 34, 30, .8),
    'tai_bei_xiong': (26, 24, 32, 30, .65),
    'zhen_yue_xiong': (26, 24, 32, 30, .65),
    'yan_bei_ye_zhu': (26, 24, 32, 30, .75),
    'hou_jiao_xi': (26, 24, 32, 30, .8),
    'qing_teng_lu': (30, 28, 36, 34, .8),
    'lei_jiao_lu': (30, 28, 36, 34, .8),
    'yanwei_fox': (30, 28, 36, 34, .85),
    'lie_ren_lang': (30, 28, 36, 34, .9),
    'han_ya_lang': (30, 28, 36, 34, .9),
}

def curves(action):
    for layer in action.layers:
        for strip in layer.strips:
            for bag in strip.channelbags:
                yield bag

def span(quats):
    return max(math.degrees(a.rotation_difference(b).angle) for a in quats for b in quats)

def refine(rig, mesh, actions):
    ident = rig.name.removesuffix('_rig')
    profile = PROFILES[ident]
    scene = bpy.context.scene
    rig.animation_data.action = None
    posing.reset_pose(rig)
    scene.frame_set(0)
    evaluated = mesh.evaluated_get(bpy.context.evaluated_depsgraph_get())
    floor = min((evaluated.matrix_world @ v.co).z for v in evaluated.data.vertices)
    report = {}
    for clip, end in [('idle',100),('walk',40),('run',20)]:
        action = actions[clip]
        if action.get('xiantu_motion_revision') in (REVISION, 'readable-gait-wings-v2'):
            continue
        posing.reset_pose(rig)
        composer.assign_action(rig, action)
        samples = []
        for f in range(end):
            scene.frame_set(f)
            samples.append({p.name: (p.location.copy(), p.rotation_quaternion.copy(), p.scale.copy()) for p in rig.pose.bones})
        before = {n:span([s[n][1] for s in samples]) for n in samples[0] if '_upper.' in n or '_lower.' in n or n.startswith('tail_')}
        factors = {}
        if clip != 'idle':
            targets = profile[:2] if clip == 'walk' else profile[2:4]
            for n, value in before.items():
                if '_upper.' in n or '_lower.' in n:
                    target = targets[0 if '_upper.' in n else 1]
                    factors[n] = min(2.5, max(1., target/max(value, .001)))
        for bag in curves(action):
            for fc in list(bag.fcurves):
                bag.fcurves.remove(fc)
        # World lateral sway expressed in each rest bone's local coordinates.
        axes = {p.name:p.bone.matrix_local.to_3x3().inverted() @ Vector((0,0,1)) for p in rig.pose.bones if p.name.startswith('tail_')}
        for f in range(end+1):
            sample = samples[f % end]
            for p in rig.pose.bones:
                loc, rot, scale = sample[p.name]
                p.location, p.rotation_quaternion, p.scale = loc, rot, scale
                if p.name == 'root':
                    p.location = (0,0,0)  # remove old floor correction before recomputing
                if p.name in factors:
                    axis, angle = rot.to_axis_angle()
                    p.rotation_quaternion = Quaternion(axis, angle*factors[p.name])
                if p.name in axes:
                    i = int(p.name.split('_')[-1])
                    cycles = 2 if clip == 'idle' else 1
                    amplitude = ((5+2*i) if clip=='idle' else (7+2*i) if clip=='walk' else (4+2*i))*profile[4]
                    angle = math.radians(amplitude)*math.sin(math.tau*cycles*f/end-i*.55)
                    p.rotation_quaternion = Quaternion(axes[p.name], angle)
                for prop in ('location','rotation_quaternion','scale'):
                    p.keyframe_insert(data_path=prop, frame=f, group=p.name)
        # Match end to start for every channel, including template body/noise channels.
        raw = []
        for f in range(end):
            scene.frame_set(f)
            evaluated = mesh.evaluated_get(bpy.context.evaluated_depsgraph_get())
            raw.append(max(0., floor-min((evaluated.matrix_world @ v.co).z for v in evaluated.data.vertices)))
        root = rig.pose.bones['root']
        up = root.bone.matrix_local.to_3x3().inverted() @ Vector((0,0,1))
        lifts = [max(raw[f], sum(raw[(f+d)%end] for d in (-1,0,1))/3) for f in range(end)]
        for f in range(end+1):
            root.location = up*lifts[f%end]
            root.keyframe_insert(data_path='location', frame=f, group='root')
        for bag in curves(action):
            for fc in bag.fcurves:
                for key in fc.keyframe_points:
                    key.interpolation = 'LINEAR'
        for track in rig.animation_data.nla_tracks:
            for strip in track.strips:
                if strip.action == action:
                    strip.action_frame_end = end
                    strip.frame_end = strip.frame_start+end
        positions=[]
        observed={n:[] for n in before}
        tailtip=[]
        for f in range(end+1):
            scene.frame_set(f)
            evaluated = mesh.evaluated_get(bpy.context.evaluated_depsgraph_get())
            positions.append(min((evaluated.matrix_world @ v.co).z for v in evaluated.data.vertices))
            for n in observed: observed[n].append(rig.pose.bones[n].rotation_quaternion.copy())
            tailtip.append(tuple(rig.pose.bones['tail_3'].tail))
        assert min(positions)>floor-1e-5,(ident,clip,min(positions),floor)
        after={n:span(qs) for n,qs in observed.items()}
        for n,qs in observed.items():
            assert qs[0].rotation_difference(qs[-1]).angle<.001,(ident,clip,n,'loop seam')
        report[clip]={'frames':end,'seconds':end/24,'before_degrees':before,'after_degrees':after,'floor_min':min(positions),'floor_max':max(positions),'max_root_lift':max(lifts),'tail_tip_span':(Vector(tuple(max(p[i] for p in tailtip) for i in range(3)))-Vector(tuple(min(p[i] for p in tailtip) for i in range(3)))).length}
        action['xiantu_motion_revision'] = REVISION
    rig.animation_data.action = None
    posing.reset_pose(rig)
    scene.frame_set(0)
    from refine_motion_v2 import refine as refine_v2
    report['v2'] = refine_v2(rig, mesh, actions)
    return report
