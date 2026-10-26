"""Run through Blender MCP. Preserve the source atlas and author an independent rig."""
import bpy
import bmesh
import math
import json
import hashlib
from pathlib import Path
from mathutils import Matrix, Vector
from bl_ext.blender_org.animal_animator.core import composer, library, profiles

ROOT = (Path(__file__).resolve().parents[1])
OUT = ROOT / 'asset_sources/creature-batch-next-20261017'
SOURCE = ROOT / 'asset_sources/creature-batch-20261016/sources/厚角犀.glb'
OUT.mkdir(exist_ok=True)
assert bpy.context.scene.name == 'ThickHornRhinoAuthoring'
# Remove scene objects only. Factory reset disables the running MCP and addons.
for obj in list(bpy.context.scene.objects):
    bpy.data.objects.remove(obj, do_unlink=True)
for action in list(bpy.data.actions):
    if action.name.startswith('hou_jiao_xi'):
        bpy.data.actions.remove(action)
bpy.ops.import_scene.gltf(filepath=str(SOURCE))
mesh = next(o for o in bpy.context.scene.objects if o.type == 'MESH')
mesh.name = 'hou_jiao_xi_mesh'
mesh.data.transform(mesh.matrix_world)
mesh.matrix_world.identity()
source_triangles = sum(len(p.vertices) - 2 for p in mesh.data.polygons)
bm = bmesh.new()
bm.from_mesh(mesh.data)
bmesh.ops.remove_doubles(bm, verts=list(bm.verts), dist=1e-6)
bmesh.ops.dissolve_degenerate(bm, edges=list(bm.edges), dist=1e-7)
bm.to_mesh(mesh.data)
bm.free()

# The torso points diagonally in the supplied three-quarter source. Landmarks
# were reviewed against its front/side/top renders and low-height paw clusters.
yaw = math.radians(16)
rotation = Matrix.Rotation(yaw, 4, 'Z')
mesh.data.transform(rotation)
mesh.data.transform(Matrix.Translation(Vector((0,0,.289307))))
def pt(v):
    return rotation @ Vector(v)+Vector((0,0,.289307))

data = bpy.data.armatures.new('hou_jiao_xi_rig')
rig = bpy.data.objects.new('hou_jiao_xi_rig', data)
bpy.context.scene.collection.objects.link(rig)
bpy.ops.object.select_all(action='DESELECT')
rig.select_set(True)
bpy.context.view_layer.objects.active = rig
bpy.ops.object.mode_set(mode='EDIT')
def bone(name, head, tail, parent=None):
    b = data.edit_bones.new(name)
    b.head, b.tail = pt(head), pt(tail)
    if parent:
        b.parent = data.edit_bones[parent]
    return b
bone('root',(0,0,-.289307),(0,0,-.19)).use_deform=False
bone('hips',(.08,.20,.015),(.035,.08,.035),'root')
bone('spine',(.035,.08,.035),(-.015,-.045,.06),'hips')
bone('chest',(-.015,-.045,.06),(-.02,-.15,.08),'spine')
bone('neck',(-.02,-.15,.08),(.01,-.255,.065),'chest')
bone('head',(.01,-.255,.065),(.065,-.425,.09),'neck')
legs={
 'front_upper.L':((-.155,-.10,.035),(-.20,-.13,-.10),(-.215,-.13,-.245),(-.215,-.205,-.277)),
 'front_upper.R':((.135,-.12,.035),(.17,-.145,-.10),(.16,-.155,-.245),(.16,-.23,-.277)),
 'hind_upper.L':((-.035,.22,-.015),(-.045,.20,-.125),(-.04,.29,-.25),(-.04,.235,-.278)),
 'hind_upper.R':((.20,.18,-.015),(.21,.16,-.125),(.215,.235,-.25),(.215,.175,-.278)),
}
for name,points in legs.items():
 prefix,side=name.split('.');base=prefix.split('_')[0]
 bone(name,points[0],points[1],'chest' if base=='front' else 'hips')
 bone(base+'_lower.'+side,points[1],points[2],name)
 bone(base+'_foot.'+side,points[2],points[3],base+'_lower.'+side)
tail=[(.105,.30,.025),(.125,.355,-.005),(.145,.405,-.055),(.16,.45,-.10),(.18,.475,-.13)]
for i in range(4):bone('tail_'+str(i),tail[i],tail[i+1],'hips' if i==0 else 'tail_'+str(i-1))
bpy.ops.object.mode_set(mode='OBJECT')
bpy.ops.object.select_all(action='DESELECT')
mesh.select_set(True)
rig.select_set(True)
bpy.context.view_layer.objects.active = rig
bpy.ops.object.parent_set(type='ARMATURE_AUTO')

# Normalize and cap influences for the GFBS common four-weight path.
deform = {b.name for b in data.bones if b.use_deform}
for v in mesh.data.vertices:
    weights = sorted([(g.group,g.weight) for g in v.groups if g.weight>1e-8],key=lambda p:p[1],reverse=True)
    if not weights:
        closest = min((b for b in data.bones if b.name in deform),key=lambda b:(v.co-(b.head_local+b.tail_local)*.5).length)
        mesh.vertex_groups.get(closest.name).add([v.index],1,'REPLACE')
        weights = [(mesh.vertex_groups[closest.name].index,1)]
    for gi,_ in weights[4:]:
        mesh.vertex_groups[gi].remove([v.index])
    total = sum(w for _,w in weights[:4])
    for gi,w in weights[:4]:
        mesh.vertex_groups[gi].add([v.index],w/total,'REPLACE')

# Keep teeth and skull rigid while the neck blends into the shoulders.
for v in mesh.data.vertices:
    p=rotation.inverted() @ (v.co-Vector((0,0,.289307)))
    if p.y < -.285 and p.z > -.15:
        for group in mesh.vertex_groups: group.remove([v.index])
        mesh.vertex_groups['head'].add([v.index],1.0,'REPLACE')
pivot = data.bones['head'].head_local.copy()
head_group = mesh.vertex_groups['head'].index
neck_group = mesh.vertex_groups['neck'].index
for v in mesh.data.vertices:
    weight = sum(g.weight*(1 if g.group==head_group else .22) for g in v.groups if g.group in (head_group,neck_group))
    if weight:
        v.co = pivot + Matrix.Rotation(math.radians(-34)*weight,3,'Z') @ (v.co-pivot)
bpy.ops.object.mode_set(mode='EDIT')
b = data.edit_bones['head']
b.tail = pivot + Matrix.Rotation(math.radians(-34),3,'Z') @ (b.tail-pivot)
bpy.ops.object.mode_set(mode='OBJECT')

mapping = {'HIPS':[['hips',1]],'SPINE':[['spine',.6],['chest',.4]],'NECK':[['neck',1]],'HEAD':[['head',1]],'TAIL':[['tail_'+str(i),1] for i in range(4)]}
for side in ('L','R'):
    for label,prefix in (('F','front'),('H','hind')):
        for slot,suffix in (('LEG_UPPER','upper'),('LEG_LOWER','lower'),('FOOT','foot')):
            mapping[label+'_'+slot+'.'+side] = [[prefix+'_'+suffix+'.'+side,1]]
profiles.store_mapping(rig,'QUAD_BASIC',mapping)
profiles.sync_collection(rig,mapping)
bpy.context.scene.animal_animator.target = rig
bpy.context.scene.render.fps = 24
clips = [('idle','idle_breathe',.22),('walk','walk',.22),('run','run',.30),('windup','idle_breathe',.18),('attack','hit_react',.3),('hurt','hit_react',.35),('death','death',.55)]
actions = {}
for name,source,intensity in clips:
    anim = library.anim_by_id(['quad_core','basicquad'],source)
    result = composer.build_action(rig,mapping,anim,['quad_core','basicquad'],intensity=intensity,unit=.577,name_prefix='hou_jiao_xi')
    if result.missing_slots:
        raise RuntimeError((name,result.missing_slots))
    action = result.action
    action.name = 'hou_jiao_xi.'+name
    paths = {fc.data_path for layer in action.layers for strip in layer.strips
             for bag in strip.channelbags for fc in bag.fcurves}
    # Explicit constant reset channels prevent a previous clip's pose leaking
    # into a newly started clip that only animates a subset of the skeleton.
    for pb in rig.pose.bones:
        for prop in ('location','rotation_quaternion','scale'):
            if pb.path_from_id(prop) not in paths:
                pb.keyframe_insert(data_path=prop,frame=0,group=pb.name)
    actions[name] = action
    track = next(t for t in rig.animation_data.nla_tracks if any(s.action==action for s in t.strips))
    track.name = action.name
    track.strips[0].name = action.name

from bl_ext.blender_org.animal_animator.core import posing
from mathutils import Quaternion, Euler
# Custom 1.2-second charge tell, baked on this boar's independently authored bones.
windup=actions['windup']
for layer in windup.layers:
    for strip in layer.strips:
        for bag in strip.channelbags:
            for curve in list(bag.fcurves): bag.fcurves.remove(curve)
composer.assign_action(rig,windup)
for frame in range(30):
    posing.reset_pose(rig)
    amount=min(1,frame/8)
    rig.pose.bones['head'].rotation_quaternion=Euler((math.radians(16)*amount,0,0)).to_quaternion()
    rig.pose.bones['front_upper.L'].rotation_quaternion=Euler((math.radians(12)*math.sin(frame/29*math.pi*4)*amount,0,0)).to_quaternion()
    for pb in rig.pose.bones:
        for prop in ('location','rotation_quaternion','scale'):pb.keyframe_insert(data_path=prop,frame=frame,group=pb.name)
rig.animation_data.action=None
# Horn jab aligned to the server's ordinary melee contact near frame 10.
attack=actions['attack']
for layer in attack.layers:
    for strip in layer.strips:
        for bag in strip.channelbags:
            for curve in list(bag.fcurves): bag.fcurves.remove(curve)
composer.assign_action(rig,attack)
for frame,amount in [(0,0),(6,.3),(10,1),(14,.4),(20,0)]:
    posing.reset_pose(rig)
    rig.pose.bones['head'].rotation_quaternion=Euler((math.radians(22)*amount,0,0)).to_quaternion()
    rig.pose.bones['neck'].rotation_quaternion=Euler((math.radians(7)*amount,0,0)).to_quaternion()
    for pb in rig.pose.bones:
        for prop in ('location','rotation_quaternion','scale'):pb.keyframe_insert(data_path=prop,frame=frame,group=pb.name)
rig.animation_data.action=None
# Rhino falls sideways while preserving the horn and shoulder plates.
death = actions['death']
for layer in death.layers:
    for strip in layer.strips:
        for bag in strip.channelbags:
            for curve in list(bag.fcurves):
                bag.fcurves.remove(curve)
composer.assign_action(rig, death)
roll_axis = rig.pose.bones['root'].bone.matrix_local.to_3x3().inverted() @ Vector((0,1,0))
for frame, amount in [(0,0),(8,.15),(20,.8),(30,1),(48,1)]:
    posing.reset_pose(rig)
    rig.pose.bones['root'].rotation_quaternion = Quaternion(roll_axis, math.radians(82)*amount)
    rig.pose.bones['head'].rotation_quaternion = Euler((math.radians(12)*amount,0,0)).to_quaternion()
    for side in ('L','R'):
        for name in ('front_lower.','hind_lower.'):
            rig.pose.bones[name+side].rotation_quaternion = Euler((math.radians(20)*amount,0,0)).to_quaternion()
    for pb in rig.pose.bones:
        for prop in ('location','rotation_quaternion','scale'):
            pb.keyframe_insert(data_path=prop,frame=frame,group=pb.name)
rig.animation_data.action=None

# Ground the authored deformation, not the renderer. Preserve root motion and
# smooth the correction over neighbouring frames without pushing paws below Z=0.
floor_audit = {}
for name, action in actions.items():
    posing.reset_pose(rig)
    composer.assign_action(rig, action)
    end = round(action.frame_range[1])
    samples = []
    for frame in range(end+1):
        bpy.context.scene.frame_set(frame)
        evaluated = mesh.evaluated_get(bpy.context.evaluated_depsgraph_get())
        low = min((evaluated.matrix_world @ v.co).z for v in evaluated.data.vertices)
        samples.append((max(0.0, -low), rig.pose.bones['root'].location.copy()))
    lifts = []
    looping = name in ('idle','walk','run')
    for frame, (raw, _) in enumerate(samples):
        indexes = [((frame+d)%end if looping and end else min(end,max(0,frame+d))) for d in (-2,-1,0,1,2)]
        lifts.append(max(raw,sum(samples[i][0] for i in indexes)/5))
    if looping:
        lifts[0] = lifts[-1] = max(lifts[0],lifts[-1])
    for frame, lift in enumerate(lifts):
        pb = rig.pose.bones['root']
        pb.location = samples[frame][1] + pb.bone.matrix_local.to_3x3().inverted() @ Vector((0,0,lift))
        pb.keyframe_insert(data_path='location', frame=frame, group='root')
    minima=[]
    for frame in range(end+1):
        bpy.context.scene.frame_set(frame)
        evaluated=mesh.evaluated_get(bpy.context.evaluated_depsgraph_get())
        minima.append(min((evaluated.matrix_world @ v.co).z for v in evaluated.data.vertices))
    floor_audit[name]={'minimum_z':min(minima),'maximum_lowest_z':max(minima),'maximum_lift':max(lifts)}
    assert min(minima)>-1e-5,(name,min(minima))
(OUT/'hou-jiao-xi-grounding.json').write_text(json.dumps(floor_audit,indent=2),encoding='utf-8')
rig.animation_data.action = None
for pb in rig.pose.bones:
    pb.location = (0,0,0)
    pb.rotation_mode = 'QUATERNION'
    pb.rotation_quaternion = (1,0,0,0)
    pb.scale = (1,1,1)
bpy.context.scene.frame_set(0)
bpy.ops.object.select_all(action='DESELECT')
mesh.select_set(True)
rig.select_set(True)
bpy.context.view_layer.objects.active = rig
# Keep locomotion readable after every full asset rebuild (Blender MCP).
import sys
sys.path.insert(0, str(ROOT / 'tools'))
from refine_quadruped_motion import refine as refine_locomotion
motion_audit = refine_locomotion(rig, mesh, actions)
(OUT / (rig.name.removesuffix('_rig') + '-motion-audit.json')).write_text(json.dumps(motion_audit, indent=2), encoding='utf-8')

animated = OUT/'hou_jiao_xi.glb'
bpy.ops.export_scene.gltf(filepath=str(animated),export_format='GLB',use_selection=True,use_active_scene=True,export_animations=True,export_animation_mode='NLA_TRACKS',export_force_sampling=True,export_frame_range=False,export_skins=True,export_all_influences=False,export_yup=True,export_image_format='AUTO')

# A single static distant asset, never a second animated instance.
lod = mesh.copy()
lod.data = mesh.data.copy()
bpy.context.scene.collection.objects.link(lod)
lod.name = 'hou_jiao_xi_static_lod'
lod.parent = None
lod.matrix_world.identity()
lod.modifiers.clear()
lod.vertex_groups.clear()
bpy.ops.object.select_all(action='DESELECT')
lod.select_set(True)
bpy.context.view_layer.objects.active = lod
dec = lod.modifiers.new('distant_only','DECIMATE')
dec.ratio = .55
bpy.ops.object.modifier_apply(modifier=dec.name)
lod.data.validate(verbose=True)
lod.data.update()
bpy.ops.export_scene.gltf(filepath=str(OUT/'hou_jiao_xi_lod.glb'),export_format='GLB',use_selection=True,use_active_scene=True,export_animations=False,export_skins=False,export_yup=True,export_image_format='AUTO')
lod.hide_render = True
lod.hide_set(True)
bpy.context.view_layer.objects.active = rig
mesh.select_set(True)
rig.select_set(True)
bpy.ops.wm.save_as_mainfile(filepath=str(OUT/'hou-jiao-xi-animated.blend'))
report = {'name':'厚角犀','source':str(SOURCE),'source_sha256':hashlib.sha256(SOURCE.read_bytes()).hexdigest(),'source_triangles':source_triangles,'mesh_vertices':len(mesh.data.vertices),'rig_joints':len(data.bones),'clips':{k:float(a.frame_range[1])/24 for k,a in actions.items()},'animated_sha256':hashlib.sha256(animated.read_bytes()).hexdigest(),'torso_yaw_degrees':16,'head_yaw_degrees':-34,'target_height_blocks':2.2,'authoring':'Blender MCP + Animal Animator quad_core/basicquad','status':'exported_candidate_pending_visual_and_runtime_qa'}
(OUT/'hou-jiao-xi-manifest.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(report,ensure_ascii=False))
