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
SOURCE = ROOT / 'asset_sources/creature-batch-20261016/sources/燧甲蜥.glb'
OUT.mkdir(exist_ok=True)
assert bpy.context.scene.name == 'CompletionRig_sui_jia_xi'
# Remove scene objects only. Factory reset disables the running MCP and addons.
for obj in list(bpy.context.scene.objects):
    bpy.data.objects.remove(obj, do_unlink=True)
for action in list(bpy.data.actions):
    if action.name.startswith('sui_jia_xi'):
        bpy.data.actions.remove(action)
bpy.ops.import_scene.gltf(filepath=str(SOURCE))
mesh = next(o for o in bpy.context.scene.objects if o.type == 'MESH')
mesh.name = 'sui_jia_xi_mesh'
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
yaw = math.radians(34)
rotation = Matrix.Rotation(yaw, 4, 'Z')
mesh.data.transform(rotation)

mesh.data.transform(Matrix.Translation(Vector((0,0,0.24536100029945374))))
def pt(v):
    return Vector(v)+Vector((0,0,0.24536100029945374))

data = bpy.data.armatures.new('sui_jia_xi_rig')
rig = bpy.data.objects.new('sui_jia_xi_rig', data)
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
bone('root',(0,0,-0.24536100029945374),(0,0,-.25)).use_deform=False
bone('hips',[-0.02, 0.08, -0.09],[-0.02, -0.055, -0.075],'root')
bone('spine',[-0.02, -0.055, -0.075],[-0.02, -0.19, -0.06],'hips')
bone('chest',[-0.02, -0.19, -0.06],[-0.02, -0.27, 0.02],'spine')
bone('neck',[-0.02, -0.27, 0.02],[-0.02, -0.37, 0.08],'chest')
bone('head',[-0.02, -0.37, 0.08],[0.04151121938178216, -0.4611941329810546, 0.08],'neck')
bone('front_upper.L',[-0.11108240342310496, -0.19, -0.08],[-0.1465033380876458, -0.2238418461642568, -0.1464008058489315],'chest')
bone('front_lower.L',[-0.1465033380876458, -0.2238418461642568, -0.1464008058489315],[-0.1465033380876458, -0.2826836923285136, -0.21280161169786302],'front_upper.L')
bone('front_foot.L',[-0.1465033380876458, -0.2826836923285136, -0.21280161169786302],[-0.1465033380876458, -0.3426836923285136, -0.232801611697863],'front_lower.L')
bone('front_upper.R',[0.07398362991809845, -0.19, -0.08],[0.1105328193306923, -0.1979965434074402, -0.14655368411540987],'chest')
bone('front_lower.R',[0.1105328193306923, -0.1979965434074402, -0.14655368411540987],[0.1105328193306923, -0.23099308681488037, -0.21310736823081972],'front_upper.R')
bone('front_foot.R',[0.1105328193306923, -0.23099308681488037, -0.21310736823081972],[0.1105328193306923, -0.29099308681488034, -0.2331073682308197],'front_lower.R')
bone('hind_upper.L',[-0.09272778458228478, 0.08, -0.11],[-0.12101081191983998, 0.00759139128475108, -0.16135181256848524],'hips')
bone('hind_lower.L',[-0.12101081191983998, 0.00759139128475108, -0.16135181256848524],[-0.12101081191983998, 0.040182782569502154, -0.21270362513697047],'hind_upper.L')
bone('hind_foot.L',[-0.12101081191983998, 0.040182782569502154, -0.21270362513697047],[-0.12101081191983998, -0.01981721743049785, -0.23270362513697046],'hind_lower.L')
bone('hind_upper.R',[0.06752979026396472, 0.08, -0.11],[0.10156915314439544, 0.020846623136080746, -0.16125767140684272],'hips')
bone('hind_lower.R',[0.10156915314439544, 0.020846623136080746, -0.16125767140684272],[0.10156915314439544, 0.0666932462721615, -0.21251534281368542],'hind_upper.R')
bone('hind_foot.R',[0.10156915314439544, 0.0666932462721615, -0.21251534281368542],[0.10156915314439544, 0.006693246272161488, -0.2325153428136854],'hind_lower.R')
bone('tail_0',[0, 0.16, -0.08],[0.01, 0.28, -0.14],'hips')
bone('tail_1',[0.01, 0.28, -0.14],[0.02, 0.4, -0.16],'tail_0')
bone('tail_2',[0.02, 0.4, -0.16],[0.03, 0.5, -0.1],'tail_1')
bone('tail_3',[0.03, 0.5, -0.1],[0.03, 0.57, -0.02],'tail_2')
bpy.ops.object.mode_set(mode='OBJECT')
bpy.ops.object.select_all(action='DESELECT')
mesh.select_set(True)
rig.select_set(True)
bpy.context.view_layer.objects.active = rig
# Heat weights from this asset's own anatomy, followed by rigid skull/armour correction.
bpy.ops.object.parent_set(type='ARMATURE_AUTO')
unweighted_before_repair = sum(not v.groups for v in mesh.data.vertices)
import sys
sys.path.insert(0,str(ROOT/'tools'))
from complete_skin_weights import repair
print('Unweighted vertices repaired with segment weights and surface smoothing:',repair(mesh,rig))

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
    p=v.co-Vector((0,0,0.24536100029945374))
    if p.y < -0.32 and p.z > -0.0050000000000000044:
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
    result = composer.build_action(rig,mapping,anim,['quad_core','basicquad'],intensity=intensity,unit=.728516,name_prefix='sui_jia_xi')
    if result.missing_slots:
        raise RuntimeError((name,result.missing_slots))
    action = result.action
    action.name = 'sui_jia_xi.'+name
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
leap=bpy.data.actions.new('sui_jia_xi.leap');composer.assign_action(rig,leap)
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
(OUT/'sui-jia-xi-grounding.json').write_text(json.dumps(floor_audit,indent=2),encoding='utf-8')
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
PROFILES["sui_jia_xi"]=(28,26,35,32,1.0)
motion_audit = refine_locomotion(rig, mesh, actions)
from refine_motion_v2 import refine as refine_v2
motion_audit["v2"]=refine_v2(rig,mesh,actions)
(OUT / (rig.name.removesuffix('_rig') + '-motion-audit.json')).write_text(json.dumps(motion_audit, indent=2), encoding='utf-8')

animated = OUT/'sui_jia_xi.glb'
bpy.ops.export_scene.gltf(filepath=str(animated),export_format='GLB',use_selection=True,use_active_scene=True,export_animations=True,export_animation_mode='NLA_TRACKS',export_force_sampling=True,export_frame_range=False,export_skins=True,export_all_influences=False,export_yup=True,export_image_format='AUTO')

# A single static distant asset, never a second animated instance.
lod = mesh.copy()
lod.data = mesh.data.copy()
bpy.context.scene.collection.objects.link(lod)
lod.name = 'sui_jia_xi_static_lod'
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
bpy.ops.export_scene.gltf(filepath=str(OUT/'sui_jia_xi_lod.glb'),export_format='GLB',use_selection=True,use_active_scene=True,export_animations=False,export_skins=False,export_yup=True,export_image_format='AUTO')
lod.hide_render = True
lod.hide_set(True)
bpy.context.view_layer.objects.active = rig
mesh.select_set(True)
rig.select_set(True)
bpy.ops.wm.save_as_mainfile(filepath=str(OUT/'sui-jia-xi-animated.blend'))
report = {'name':'燧甲蜥','source':str(SOURCE),'source_sha256':hashlib.sha256(SOURCE.read_bytes()).hexdigest(),'source_triangles':source_triangles,'isolated_vertices_repaired':unweighted_before_repair,'mesh_vertices':len(mesh.data.vertices),'rig_joints':len(data.bones),'clips':{k:float(a.frame_range[1])/24 for k,a in actions.items()},'animated_sha256':hashlib.sha256(animated.read_bytes()).hexdigest(),'torso_yaw_degrees':34,'head_yaw_degrees':-34,'target_height_blocks':1.25,'authoring':'Blender MCP + Animal Animator quad_core/basicquad','status':'exported_candidate_pending_visual_and_runtime_qa'}
(OUT/'sui-jia-xi-manifest.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(report,ensure_ascii=False))
