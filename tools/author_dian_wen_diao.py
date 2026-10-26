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
OUT = ROOT / 'asset_sources/creature-completion-20261017'
SOURCE = ROOT / 'asset_sources/creature-batch-20261016/sources/电纹貂.glb'
OUT.mkdir(exist_ok=True)
assert bpy.context.scene.name == 'CompletionRig_dian_wen_diao'
# Remove scene objects only. Factory reset disables the running MCP and addons.
for obj in list(bpy.context.scene.objects):
    bpy.data.objects.remove(obj, do_unlink=True)
for action in list(bpy.data.actions):
    if action.name.startswith('dian_wen_diao'):
        bpy.data.actions.remove(action)
bpy.ops.import_scene.gltf(filepath=str(SOURCE))
mesh = next(o for o in bpy.context.scene.objects if o.type == 'MESH')
mesh.name = 'dian_wen_diao_mesh'
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
yaw = math.radians(52)
rotation = Matrix.Rotation(yaw, 4, 'Z')
mesh.data.transform(rotation)

mesh.data.transform(Matrix.Translation(Vector((0,0,0.3254390060901642))))
def pt(v):
    return Vector(v)+Vector((0,0,0.3254390060901642))

data = bpy.data.armatures.new('dian_wen_diao_rig')
rig = bpy.data.objects.new('dian_wen_diao_rig', data)
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
bone('root',(0,0,-0.3254390060901642),(0,0,-.25)).use_deform=False
bone('hips',[-0.03, 0.08, -0.12],[-0.03, -0.08499999999999999, -0.11],'root')
bone('spine',[-0.03, -0.08499999999999999, -0.11],[-0.03, -0.25, -0.1],'hips')
bone('chest',[-0.03, -0.25, -0.1],[-0.03, -0.36, 0.04],'spine')
bone('neck',[-0.03, -0.36, 0.04],[-0.03, -0.44, 0.16],'chest')
bone('head',[-0.03, -0.44, 0.16],[0.05668118289673943, -0.5077227622858225, 0.16],'neck')
bone('front_upper.L',[-0.10131836172014803, -0.25, -0.12000000000000001],[-0.12905328016687226, -0.296393656697902, -0.20584422042081646],'chest')
bone('front_lower.L',[-0.12905328016687226, -0.296393656697902, -0.20584422042081646],[-0.12905328016687226, -0.36778731339580406, -0.29168844084163287],'front_upper.L')
bone('front_foot.L',[-0.12905328016687226, -0.36778731339580406, -0.29168844084163287],[-0.12905328016687226, -0.4277873133958041, -0.3116884408416329],'front_lower.L')
bone('front_upper.R',[0.03548100262345938, -0.25, -0.12000000000000001],[0.06094583697702693, -0.28148612410857754, -0.20581484498648805],'chest')
bone('front_lower.R',[0.06094583697702693, -0.28148612410857754, -0.20581484498648805],[0.06094583697702693, -0.33797224821715516, -0.2916296899729761],'front_upper.R')
bone('front_foot.R',[0.06094583697702693, -0.33797224821715516, -0.2916296899729761],[0.06094583697702693, -0.39797224821715516, -0.31162968997297613],'front_lower.R')
bone('hind_upper.L',[-0.09690025257812096, 0.08, -0.13999999999999999],[-0.12291701746961245, 0.024413130138642512, -0.21613201489815345],'hips')
bone('hind_lower.L',[-0.12291701746961245, 0.024413130138642512, -0.21613201489815345],[-0.12291701746961245, 0.07382626027728503, -0.2922640297963069],'hind_upper.L')
bone('hind_foot.L',[-0.12291701746961245, 0.07382626027728503, -0.2922640297963069],[-0.12291701746961245, 0.013826260277285021, -0.3122640297963069],'hind_lower.L')
bone('hind_upper.R',[0.039919194622729956, 0.08, -0.13999999999999999],[0.06710999253156938, 0.03354390835095392, -0.21618927353306822],'hips')
bone('hind_lower.R',[0.06710999253156938, 0.03354390835095392, -0.21618927353306822],[0.06710999253156938, 0.09208781670190785, -0.2923785470661364],'hind_upper.R')
bone('hind_foot.R',[0.06710999253156938, 0.09208781670190785, -0.2923785470661364],[0.06710999253156938, 0.032087816701907856, -0.31237854706613644],'hind_lower.R')
bone('tail_0',[0, 0.17, -0.12],[0.015, 0.29, -0.21],'hips')
bone('tail_1',[0.015, 0.29, -0.21],[0.04, 0.41, -0.24],'tail_0')
bone('tail_2',[0.04, 0.41, -0.24],[0.05, 0.5, -0.16],'tail_1')
bone('tail_3',[0.05, 0.5, -0.16],[0.06, 0.54, -0.01],'tail_2')
bpy.ops.object.mode_set(mode='OBJECT')
bpy.ops.object.select_all(action='DESELECT')
mesh.select_set(True)
rig.select_set(True)
bpy.context.view_layer.objects.active = rig
# Heat weights from this asset's own anatomy, followed by rigid skull/armour correction.
bpy.ops.object.parent_set(type='ARMATURE_AUTO')
unweighted_before_repair = sum(not v.groups for v in mesh.data.vertices)
print('Heat-weight isolated vertices repaired by nearest anatomical bone:', unweighted_before_repair)

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

# Keep each head and its ornaments together; shell meshes remain rigid at the torso.
for v in mesh.data.vertices:
    p=v.co-Vector((0,0,0.3254390060901642))
    if p.y < -0.4 and p.z > 0.075:
        for group in mesh.vertex_groups:group.remove([v.index])
        mesh.vertex_groups['head'].add([v.index],1,'REPLACE')
pivot = data.bones['head'].head_local.copy()
head_group = mesh.vertex_groups['head'].index
neck_group = mesh.vertex_groups['neck'].index
for v in mesh.data.vertices:
    weight = sum(g.weight*(1 if g.group==head_group else .22) for g in v.groups if g.group in (head_group,neck_group))
    if weight:
        v.co = pivot + Matrix.Rotation(-yaw*weight,3,'Z') @ (v.co-pivot)
bpy.ops.object.mode_set(mode='EDIT')
b = data.edit_bones['head']
b.tail = pivot + Matrix.Rotation(-yaw,3,'Z') @ (b.tail-pivot)
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
clips = [('idle','idle_breathe',.25),('walk','walk',.25),('run','run',.32),('attack','attack_claw.L',.65),('hurt','hit_react',.4),('death','death',.65)]
actions = {}
for name,source,intensity in clips:
    anim = library.anim_by_id(['quad_core','basicquad'],source)
    result = composer.build_action(rig,mapping,anim,['quad_core','basicquad'],intensity=intensity,unit=.728516,name_prefix='dian_wen_diao')
    if result.missing_slots:
        raise RuntimeError((name,result.missing_slots))
    action = result.action
    action.name = 'dian_wen_diao.'+name
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
# Dedicated leap pose uses this animal's own leg lengths and root lift.
leap=bpy.data.actions.new('dian_wen_diao.leap');composer.assign_action(rig,leap)
for frame,crouch,lift in [(0,0,0),(10,1,0),(19,1,0),(24,-.4,.10),(31,-.2,.07),(38,0,0),(48,0,0)]:
    posing.reset_pose(rig)
    for side in ('L','R'):
        rig.pose.bones['hind_upper.'+side].rotation_quaternion=Euler((math.radians(25)*crouch,0,0)).to_quaternion()
        rig.pose.bones['front_upper.'+side].rotation_quaternion=Euler((math.radians(-18)*crouch,0,0)).to_quaternion()
    root=rig.pose.bones['root'];root.location=root.bone.matrix_local.to_3x3().inverted()@Vector((0,0,lift))
    for pb in rig.pose.bones:
        for prop in ('location','rotation_quaternion','scale'):pb.keyframe_insert(data_path=prop,frame=frame,group=pb.name)
actions['leap']=leap
track=rig.animation_data.nla_tracks.new();track.name=leap.name;track.mute=True;strip=track.strips.new(leap.name,0,leap);strip.name=leap.name
rig.animation_data.action=None

# Side fall with rigid mane following the head.
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
(OUT/'dian-wen-diao-grounding.json').write_text(json.dumps(floor_audit,indent=2),encoding='utf-8')
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
from refine_quadruped_motion import refine as refine_locomotion, PROFILES
PROFILES["dian_wen_diao"]=(28,26,35,32,1.0)
motion_audit = refine_locomotion(rig, mesh, actions)
from refine_motion_v2 import refine as refine_v2
motion_audit["v2"]=refine_v2(rig,mesh,actions)
(OUT / (rig.name.removesuffix('_rig') + '-motion-audit.json')).write_text(json.dumps(motion_audit, indent=2), encoding='utf-8')

animated = OUT/'dian_wen_diao.glb'
bpy.ops.export_scene.gltf(filepath=str(animated),export_format='GLB',use_selection=True,use_active_scene=True,export_animations=True,export_animation_mode='NLA_TRACKS',export_force_sampling=True,export_frame_range=False,export_skins=True,export_all_influences=False,export_yup=True,export_image_format='AUTO')

# A single static distant asset, never a second animated instance.
lod = mesh.copy()
lod.data = mesh.data.copy()
bpy.context.scene.collection.objects.link(lod)
lod.name = 'dian_wen_diao_static_lod'
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
bpy.ops.export_scene.gltf(filepath=str(OUT/'dian_wen_diao_lod.glb'),export_format='GLB',use_selection=True,use_active_scene=True,export_animations=False,export_skins=False,export_yup=True,export_image_format='AUTO')
lod.hide_render = True
lod.hide_set(True)
bpy.context.view_layer.objects.active = rig
mesh.select_set(True)
rig.select_set(True)
bpy.ops.wm.save_as_mainfile(filepath=str(OUT/'dian-wen-diao-animated.blend'))
report = {'name':'电纹貂','source':str(SOURCE),'source_sha256':hashlib.sha256(SOURCE.read_bytes()).hexdigest(),'source_triangles':source_triangles,'isolated_vertices_repaired':unweighted_before_repair,'mesh_vertices':len(mesh.data.vertices),'rig_joints':len(data.bones),'clips':{k:float(a.frame_range[1])/24 for k,a in actions.items()},'animated_sha256':hashlib.sha256(animated.read_bytes()).hexdigest(),'torso_yaw_degrees':52,'head_yaw_degrees':-52,'target_height_blocks':1.15,'authoring':'Blender MCP + Animal Animator quad_core/basicquad','status':'exported_candidate_pending_visual_and_runtime_qa'}
(OUT/'dian-wen-diao-manifest.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(report,ensure_ascii=False))
