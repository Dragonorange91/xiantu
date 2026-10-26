"""Second motion review, run only through the running Blender MCP scene.

Apply to v1 actions. Enlarge motion about each channel's midpoint so static
joint offsets do not grow. Keep cycle duration, phase, tails and body motion.
"""
import math
import bpy
from mathutils import Quaternion, Vector
from bl_ext.blender_org.animal_animator.core import composer, posing
from refine_quadruped_motion import curves, span

REVISION = 'readable-gait-wings-v2'

def reset(rig):
    rig.animation_data.action = None
    posing.reset_pose(rig)
    bpy.context.scene.frame_set(0)

def enlarge(rig, mesh, action, targets, grounded=True, wing_bias=False):
    if action.get('xiantu_motion_revision') == REVISION:
        return {'already_applied': True}
    scene = bpy.context.scene
    reset(rig)
    ev = mesh.evaluated_get(bpy.context.evaluated_depsgraph_get())
    floor = min((ev.matrix_world @ v.co).z for v in ev.data.vertices)
    composer.assign_action(rig, action)
    start, end = map(int, action.frame_range)
    samples = []
    for f in range(start, end):
        scene.frame_set(f)
        samples.append({p.name: p.matrix_basis.decompose() for p in rig.pose.bones})
    centres = {}; factors = {}; before = {}
    for name, target in targets.items():
        qs = [s[name][1] for s in samples]
        a,b = max(((a,b) for a in qs for b in qs), key=lambda ab: abs(ab[0].rotation_difference(ab[1]).angle))
        before[name] = span(qs)
        centres[name] = a.slerp(b, .5)
        factors[name] = max(1., target / max(before[name], .001))
        assert factors[name] < 4, (name, 'unexpected input action', before[name])
    for bag in curves(action):
        for fc in list(bag.fcurves): bag.fcurves.remove(fc)
    for f in range(start, end+1):
        for p in rig.pose.bones:
            loc, q, scale = samples[(f-start) % (end-start)][p.name]
            p.rotation_mode = 'QUATERNION'
            p.location, p.scale = loc, scale
            if p.name in centres:
                centre = centres[p.name]
                delta = centre.inverted() @ q
                if delta.w < 0: delta.negate()
                axis, angle = delta.to_axis_angle()
                q = centre @ Quaternion(axis, angle*factors[p.name])
            if wing_bias and p.name.startswith('wing_upper.'):
                axis = p.bone.matrix_local.to_3x3().inverted() @ Vector((0,1,0))
                q = Quaternion(axis, math.radians(30 if p.name.endswith('.L') else -30)) @ q
            p.rotation_quaternion = q
            if grounded and p.name == 'root': p.location = (0,0,0)
            for prop in ('location','rotation_quaternion','scale'):
                p.keyframe_insert(data_path=prop, frame=f, group=p.name)
    lifts = []
    if grounded:
        for f in range(start,end):
            scene.frame_set(f)
            ev = mesh.evaluated_get(bpy.context.evaluated_depsgraph_get())
            lifts.append(max(0., floor-min((ev.matrix_world @ v.co).z for v in ev.data.vertices)))
        p = rig.pose.bones['root']
        up = p.bone.matrix_local.to_3x3().inverted() @ rig.matrix_world.to_3x3().inverted() @ Vector((0,0,1))
        smooth = [max(x,sum(lifts[(i+j)%len(lifts)] for j in (-1,0,1))/3) for i,x in enumerate(lifts)]
        for f in range(start,end+1):
            p.location = up*smooth[(f-start)%len(smooth)]
            p.keyframe_insert(data_path='location', frame=f, group='root')
    for bag in curves(action):
        for fc in bag.fcurves:
            for k in fc.keyframe_points: k.interpolation='LINEAR'
    values={n:[] for n in targets}; feet={p.name:[] for p in rig.pose.bones if p.name.startswith(('paw','foot','wing_tip.'))}
    floors=[]
    for f in range(start,end+1):
        scene.frame_set(f)
        for n in values: values[n].append(rig.pose.bones[n].rotation_quaternion.copy())
        for n in feet: feet[n].append(tuple(rig.pose.bones[n].tail))
        if grounded:
            ev=mesh.evaluated_get(bpy.context.evaluated_depsgraph_get())
            floors.append(min((ev.matrix_world @ v.co).z for v in ev.data.vertices))
    for n,qs in values.items():
        assert qs[0].rotation_difference(qs[-1]).angle < .001, (n,'loop seam')
    if grounded: assert min(floors) >= floor-1e-5, (rig.name,'floor penetration')
    action['xiantu_motion_revision']=REVISION
    reset(rig)
    return {'before_degrees':before,'after_degrees':{n:span(q) for n,q in values.items()},
            'floor_min':min(floors) if floors else None,'rest_floor':floor,
            'root_lift_max':max(lifts) if lifts else 0,
            'tip_bounds':{n:{'min':[min(p[i] for p in ps) for i in range(3)],'max':[max(p[i] for p in ps) for i in range(3)]} for n,ps in feet.items()}}

def refine(rig,mesh,actions):
    ident=rig.name.removesuffix('_rig');report={}
    if ident=='ting_yu_ya':
        report['walk']=enlarge(rig,mesh,actions['walk'],{part+'.'+side:angle for side in 'LR' for part,angle in [('leg_upper',44),('leg_lower',40)]})
        report['fly']=enlarge(rig,mesh,actions['fly'],{part+'.'+side:angle for side in 'LR' for part,angle in [('wing_upper',100),('wing_middle',40),('wing_tip',28)]},False,True)
        # A gliding bird should extend its wings rather than retain the raised rest silhouette.
        report['glide']=enlarge(rig,mesh,actions['glide'],{},False,True)
    else:
        heavy=ident in ('tai_bei_xiong','zhen_yue_xiong','yan_bei_ye_zhu','hou_jiao_xi')
        for clip,upper,lower in [('walk',40 if heavy else 44,36 if heavy else 40),('run',46 if heavy else 50,42 if heavy else 44)]:
            report[clip]=enlarge(rig,mesh,actions[clip],{p+'_'+part+'.'+side:angle for p in ('front','hind') for side in 'LR' for part,angle in [('upper',upper),('lower',lower)]})
    return report
