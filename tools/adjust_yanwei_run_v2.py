"""Run in Blender MCP: a restrained transverse gallop, not a sped-up trot.

Only edits the run action. Keeps the imported rig, skin and all other actions.
"""
import bpy
import json
import math
from pathlib import Path
from mathutils import Vector, Quaternion

ROOT = (Path(__file__).resolve().parents[1])
OUT = ROOT / 'asset_sources/yanwei-fox-run-v2-20261015'
OUT.mkdir(parents=True, exist_ok=True)
scene = bpy.context.scene
arm = bpy.data.objects['SmartRigArmature']
root = bpy.data.objects['FoxAnimationRoot']
mesh = bpy.data.objects['Mesh_0']
action = bpy.data.actions['yanwei_fox.run']
assert len(arm.data.bones) == 43
assert len(bpy.data.actions) == 7
bpy.ops.wm.save_as_mainfile(filepath=str(OUT / 'before-run-adjustment.blend'))

for obj in (arm, root):
    obj.animation_data.action = None
    for track in obj.animation_data.nla_tracks:
        track.mute = track.name != 'yanwei_fox.run'
        track.is_solo = False

bags = {}
for layer in action.layers:
    for strip in layer.strips:
        for slot in action.slots:
            bag = strip.channelbag(slot)
            if bag:
                bags[slot.identifier] = bag

def curve(bag, path, index, values):
    fc = next((f for f in bag.fcurves if f.data_path == path and f.array_index == index), None)
    if fc is None:
        fc = bag.fcurves.new(data_path=path, index=index)
    fc.keyframe_points.clear()
    fc.keyframe_points.add(len(values))
    for point, (frame, value) in zip(fc.keyframe_points, values):
        point.co = (frame, value)
        point.interpolation = 'LINEAR'
    fc.update()

def smooth(x):
    return x*x*(3-2*x)

def interp(points, phase):
    for (a, va), (b, vb) in zip(points, points[1:]):
        if a <= phase <= b:
            return va + (vb-va)*smooth((phase-a)/(b-a))
    raise ValueError(phase)

# Forefeet land close together, followed by the hindfeet; never diagonal trot.
# A short support phase gives distinct push-off and folded recovery.
legs = [
    ('Bone_015','Bone_014','Bone_013',0.00,1.00),
    ('Bone_021','Bone_020','Bone_019',0.08,1.00),
    ('Bone_026','Bone_025','Bone_024',0.43,0.92),
    ('Bone_031','Bone_030','Bone_029',0.51,0.92),
]
stance = .30
samples = 72
frames = [1 + 18*i/samples for i in range(samples+1)]
rotations = {n: [] for leg in legs for n in leg[:3]}
rotations.update({n: [] for n in ['Bone_004','Bone_003','Bone_036','Bone_035','Bone_034','Bone_033','Bone_032']})
axes = {n: arm.data.bones[n].matrix_local.to_3x3().transposed() @ Vector((1,0,0)) for n in rotations}
for i, frame in enumerate(frames):
    t = i/samples
    for hip,knee,paw,offset,scale in legs:
        p = (t-offset)%1
        h = interp([(0,-16),(.30,17),(.52,9),(.78,-17),(1,-16)], p)*scale
        k = interp([(0,1),(.22,0),(.40,5),(.58,10),(.78,6),(1,1)], p)
        f = interp([(0,1),(.30,-3),(.56,-3.5),(.78,2),(1,1)], p)
        for name, deg in [(hip,h),(knee,k),(paw,f)]:
            rotations[name].append(Quaternion(axes[name], math.radians(deg)))
    # One body pulse per stride, a steady head, and a small delayed tail wave.
    rotations['Bone_004'].append(Quaternion(axes['Bone_004'],math.radians(.9)*math.sin(math.tau*(t-.12))))
    rotations['Bone_003'].append(Quaternion(axes['Bone_003'],math.radians(-.7)*math.sin(math.tau*(t-.12))))
    for j,n in enumerate(['Bone_036','Bone_035','Bone_034','Bone_033','Bone_032']):
        axis = arm.data.bones[n].matrix_local.to_3x3().transposed() @ Vector((0,0,1))
        rotations[n].append(Quaternion(axis, math.radians(2+j*.3)*math.sin(math.tau*(t-.10)-j*.22)))

bag = bags['OBSmartRigArmature']
for name, quats in rotations.items():
    arm.pose.bones[name].rotation_mode = 'QUATERNION'
    for component in range(4):
        curve(bag, f'pose.bones["{name}"].rotation_quaternion', component,
              [(f,q[component]) for f,q in zip(frames,quats)])

rb = bags['OBFoxAnimationRoot']
for c in range(3):
    curve(rb,'location',c,[(f,.48 if c==2 else 0) for f in frames])

# Measure the actual skinned surface for QA without hiding limb issues through
# per-frame whole-body offsets. Keep the existing bounded run-bob envelope.
foot_indices = [v.index for v in mesh.data.vertices if v.co.z < .17]
assert len(foot_indices)>10
ground = []
foot_data = []
for i,frame in enumerate(frames):
    scene.frame_set(int(frame),subframe=frame-int(frame))
    dg=bpy.context.evaluated_depsgraph_get()
    evaluated=mesh.evaluated_get(dg)
    geo=evaluated.to_mesh()
    floor=min((evaluated.matrix_world @ geo.vertices[k].co).z for k in foot_indices)
    evaluated.to_mesh_clear()
    t=i/samples
    supported=any((t-offset)%1 < stance for _,_,_,offset,_ in legs)
    ground.append((frame,.48+.014*max(0.0,math.sin(math.tau*t))))
    foot_data.append({'frame':frame,'phase':t,'support':supported,'surface_min_with_bounded_bob':floor})
curve(rb,'location',2,ground)
scene.frame_start=1
scene.frame_end=18
scene.render.fps=24
scene.frame_set(1)
scene['run_v2_notes']='Transverse gallop; staggered fore/hind contacts; bounded 0.014-unit vertical root travel; 72 subframe samples; no rig changes.'
(OUT/'authoring-metrics.json').write_text(json.dumps({'legs':legs,'stance_fraction':stance,'duration':.75,'root_y_min':min(v for _,v in ground),'root_y_max':max(v for _,v in ground),'foot_surface':foot_data},indent=2),encoding='utf-8')
bpy.ops.wm.save_as_mainfile(filepath=str(OUT/'yanwei_fox_run_v2.blend'))
print('RUN_V2_AUTHORED',str(OUT), 'root span',min(v for _,v in ground),max(v for _,v in ground))
