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
SOURCE = ROOT / 'asset_sources/creature-batch-20261016/sources/砂甲犰狳.glb'
OUT.mkdir(exist_ok=True)
assert bpy.context.scene.name == 'CompletionRig_sha_jia_qiu_yu'
# Remove scene objects only. Factory reset disables the running MCP and addons.
for obj in list(bpy.context.scene.objects):
    bpy.data.objects.remove(obj, do_unlink=True)
for action in list(bpy.data.actions):
    if action.name.startswith('sha_jia_qiu_yu'):
        bpy.data.actions.remove(action)
bpy.ops.import_scene.gltf(filepath=str(SOURCE))
mesh = next(o for o in bpy.context.scene.objects if o.type == 'MESH')
mesh.name = 'sha_jia_qiu_yu_mesh'
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
yaw = math.radians(7)
rotation = Matrix.Rotation(yaw, 4, 'Z')
mesh.data.transform(rotation)

mesh.data.transform(Matrix.Translation(Vector((0,0,0.1965329945087433))))
def pt(v):
    return Vector(v)+Vector((0,0,0.1965329945087433))

data = bpy.data.armatures.new('sha_jia_qiu_yu_rig')
rig = bpy.data.objects.new('sha_jia_qiu_yu_rig', data)
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
bone('root',(0,0,-0.1965329945087433),(0,0,-.25)).use_deform=False
bone('hips',[0.01, 0.15, -0.03],[0.01, -0.0050000000000000044, -0.03],'root')
bone('spine',[0.01, -0.0050000000000000044, -0.03],[0.01, -0.16, -0.03],'hips')
bone('chest',[0.01, -0.16, -0.03],[0.01, -0.25, -0.05],'spine')
bone('neck',[0.01, -0.25, -0.05],[0.01, -0.36, -0.09],'chest')
bone('head',[0.01, -0.36, -0.09],[0.023405627774566222, -0.4691800766805454, -0.09],'neck')
bone('front_upper.L',[-0.06719767908523248, -0.16, -0.05],[-0.09721899872948957, -0.17716904386878016, -0.10570001178702643],'chest')
bone('front_lower.L',[-0.09721899872948957, -0.17716904386878016, -0.10570001178702643],[-0.09721899872948957, -0.21933808773756028, -0.16140002357405286],'front_upper.L')
bone('front_foot.L',[-0.09721899872948957, -0.21933808773756028, -0.16140002357405286],[-0.09721899872948957, -0.2793380877375603, -0.18140002357405285],'front_lower.L')
bone('front_upper.R',[0.09118458769529768, -0.16, -0.05],[0.12275637179902456, -0.15110356144534734, -0.10725313546298777],'chest')
bone('front_lower.R',[0.12275637179902456, -0.15110356144534734, -0.10725313546298777],[0.12275637179902456, -0.16720712289069464, -0.16450627092597556],'front_upper.R')
bone('front_foot.R',[0.12275637179902456, -0.16720712289069464, -0.16450627092597556],[0.12275637179902456, -0.22720712289069464, -0.18450627092597555],'front_lower.R')
bone('hind_upper.L',[-0.0726247724433099, 0.15, -0.05],[-0.10475662839348598, 0.10942677802135867, -0.10611831713748235],'hips')
bone('hind_lower.L',[-0.10475662839348598, 0.10942677802135867, -0.10611831713748235],[-0.10475662839348598, 0.17385355604271735, -0.1622366342749647],'hind_upper.L')
bone('hind_foot.L',[-0.10475662839348598, 0.17385355604271735, -0.1622366342749647],[-0.10475662839348598, 0.11385355604271735, -0.1822366342749647],'hind_lower.L')
bone('hind_upper.R',[0.08674918488473006, 0.15, -0.05],[0.11659609011768066, 0.09312927631219636, -0.10601789659753288],'hips')
bone('hind_lower.R',[0.11659609011768066, 0.09312927631219636, -0.10601789659753288],[0.11659609011768066, 0.14125855262439277, -0.16203579319506578],'hind_upper.R')
bone('hind_foot.R',[0.11659609011768066, 0.14125855262439277, -0.16203579319506578],[0.11659609011768066, 0.08125855262439276, -0.18203579319506577],'hind_lower.R')
bone('tail_0',[0.01, 0.25, -0.06],[0.01, 0.32, -0.09],'hips')
bone('tail_1',[0.01, 0.32, -0.09],[0.01, 0.38, -0.12],'tail_0')
bone('tail_2',[0.01, 0.38, -0.12],[0.01, 0.44, -0.14],'tail_1')
bone('tail_3',[0.01, 0.44, -0.14],[0.01, 0.49, -0.16],'tail_2')
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
    p=v.co-Vector((0,0,0.1965329945087433))
    if p.y < -0.305 and p.z > -0.175:
        for group in mesh.vertex_groups:group.remove([v.index])
        mesh.vertex_groups['head'].add([v.index],1,'REPLACE')
    elif -0.25 < p.y < 0.25 and p.z > -0.05:
        for group in mesh.vertex_groups:group.remove([v.index])
        mesh.vertex_groups['spine'].add([v.index],1,'REPLACE')
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
    result = composer.build_action(rig,mapping,anim,['quad_core','basicquad'],intensity=intensity,unit=.728516,name_prefix='sha_jia_qiu_yu')
    if result.missing_slots:
        raise RuntimeError((name,result.missing_slots))
    action = result.action
    action.name = 'sha_jia_qiu_yu.'+name
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
(OUT/'sha-jia-qiu-yu-grounding.json').write_text(json.dumps(floor_audit,indent=2),encoding='utf-8')
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
PROFILES["sha_jia_qiu_yu"]=(28,26,35,32,1.0)
motion_audit = refine_locomotion(rig, mesh, actions)
from refine_motion_v2 import refine as refine_v2
motion_audit["v2"]=refine_v2(rig,mesh,actions)
(OUT / (rig.name.removesuffix('_rig') + '-motion-audit.json')).write_text(json.dumps(motion_audit, indent=2), encoding='utf-8')

animated = OUT/'sha_jia_qiu_yu.glb'
bpy.ops.export_scene.gltf(filepath=str(animated),export_format='GLB',use_selection=True,use_active_scene=True,export_animations=True,export_animation_mode='NLA_TRACKS',export_force_sampling=True,export_frame_range=False,export_skins=True,export_all_influences=False,export_yup=True,export_image_format='AUTO')

# A single static distant asset, never a second animated instance.
lod = mesh.copy()
lod.data = mesh.data.copy()
bpy.context.scene.collection.objects.link(lod)
lod.name = 'sha_jia_qiu_yu_static_lod'
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
bpy.ops.export_scene.gltf(filepath=str(OUT/'sha_jia_qiu_yu_lod.glb'),export_format='GLB',use_selection=True,use_active_scene=True,export_animations=False,export_skins=False,export_yup=True,export_image_format='AUTO')
lod.hide_render = True
lod.hide_set(True)
bpy.context.view_layer.objects.active = rig
mesh.select_set(True)
rig.select_set(True)
bpy.ops.wm.save_as_mainfile(filepath=str(OUT/'sha-jia-qiu-yu-animated.blend'))
report = {'name':'砂甲犰狳','source':str(SOURCE),'source_sha256':hashlib.sha256(SOURCE.read_bytes()).hexdigest(),'source_triangles':source_triangles,'isolated_vertices_repaired':unweighted_before_repair,'mesh_vertices':len(mesh.data.vertices),'rig_joints':len(data.bones),'clips':{k:float(a.frame_range[1])/24 for k,a in actions.items()},'animated_sha256':hashlib.sha256(animated.read_bytes()).hexdigest(),'torso_yaw_degrees':7,'head_yaw_degrees':-7,'target_height_blocks':1.0,'authoring':'Blender MCP + Animal Animator quad_core/basicquad','status':'exported_candidate_pending_visual_and_runtime_qa'}
(OUT/'sha-jia-qiu-yu-manifest.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(report,ensure_ascii=False))
