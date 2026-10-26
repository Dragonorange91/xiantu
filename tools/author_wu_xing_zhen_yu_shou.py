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
SOURCE = ROOT / 'asset_sources/creature-batch-20261016/sources/五行镇狱兽.glb'
OUT.mkdir(exist_ok=True)
assert bpy.context.scene.name == 'CompletionRig_wu_xing_zhen_yu_shou'
# Remove scene objects only. Factory reset disables the running MCP and addons.
for obj in list(bpy.context.scene.objects):
    bpy.data.objects.remove(obj, do_unlink=True)
for action in list(bpy.data.actions):
    if action.name.startswith('wu_xing_zhen_yu_shou'):
        bpy.data.actions.remove(action)
bpy.ops.import_scene.gltf(filepath=str(SOURCE))
mesh = next(o for o in bpy.context.scene.objects if o.type == 'MESH')
mesh.name = 'wu_xing_zhen_yu_shou_mesh'
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
yaw = math.radians(20)
rotation = Matrix.Rotation(yaw, 4, 'Z')
mesh.data.transform(rotation)

mesh.data.transform(Matrix.Translation(Vector((0,0,0.2565920054912567))))
def pt(v):
    return Vector(v)+Vector((0,0,0.2565920054912567))

data = bpy.data.armatures.new('wu_xing_zhen_yu_shou_rig')
rig = bpy.data.objects.new('wu_xing_zhen_yu_shou_rig', data)
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
bone('root',(0,0,-0.2565920054912567),(0,0,-.25)).use_deform=False
bone('hips',[0.04, 0.14, -0.035],[0.04, -0.01999999999999999, -0.027500000000000004],'root')
bone('spine',[0.04, -0.01999999999999999, -0.027500000000000004],[0.04, -0.18, -0.02],'hips')
bone('chest',[0.04, -0.18, -0.02],[0.04, -0.26, 0.045],'spine')
bone('neck',[0.04, -0.26, 0.045],[0.04, -0.34, 0.1],'chest')
bone('head',[0.04, -0.34, 0.1],[0.07762221576582357, -0.44336618828644997, 0.1],'neck')
bone('front_upper.L',[-0.080818746328141, -0.18, -0.04],[-0.12780381434464028, -0.19613621586401547, -0.13159816973975727],'chest')
bone('front_lower.L',[-0.12780381434464028, -0.19613621586401547, -0.13159816973975727],[-0.12780381434464028, -0.23727243172803095, -0.22319633947951453],'front_upper.L')
bone('front_foot.L',[-0.12780381434464028, -0.23727243172803095, -0.22319633947951453],[-0.12780381434464028, -0.297272431728031, -0.24319633947951452],'front_lower.L')
bone('front_upper.R',[0.1527530189600858, -0.18, -0.04],[0.19660141522234137, -0.23287260659415313, -0.13233155416418807],'chest')
bone('front_lower.R',[0.19660141522234137, -0.23287260659415313, -0.13233155416418807],[0.19660141522234137, -0.3107452131883062, -0.22466310832837616],'front_upper.R')
bone('front_foot.R',[0.19660141522234137, -0.3107452131883062, -0.22466310832837616],[0.19660141522234137, -0.3707452131883062, -0.24466310832837615],'front_lower.R')
bone('hind_upper.L',[-0.04815475071104999, 0.14, -0.05500000000000001],[-0.08243715376534722, 0.09881109852671124, -0.13866426481986144],'hips')
bone('hind_lower.L',[-0.08243715376534722, 0.09881109852671124, -0.13866426481986144],[-0.08243715376534722, 0.16262219705342248, -0.2223285296397229],'hind_upper.L')
bone('hind_foot.L',[-0.08243715376534722, 0.16262219705342248, -0.2223285296397229],[-0.08243715376534722, 0.10262219705342249, -0.2423285296397229],'hind_lower.L')
bone('hind_upper.R',[0.1326174000379887, 0.14, -0.05500000000000001],[0.16863527783053986, 0.10090146293506977, -0.13901869758963586],'hips')
bone('hind_lower.R',[0.16863527783053986, 0.10090146293506977, -0.13901869758963586],[0.16863527783053986, 0.16680292587013953, -0.2230373951792717],'hind_upper.R')
bone('hind_foot.R',[0.16863527783053986, 0.16680292587013953, -0.2230373951792717],[0.16863527783053986, 0.10680292587013954, -0.2430373951792717],'hind_lower.R')
bone('tail_0',[0.04, 0.22, -0.04],[0.05, 0.31, -0.1],'hips')
bone('tail_1',[0.05, 0.31, -0.1],[0.06, 0.4, -0.13],'tail_0')
bone('tail_2',[0.06, 0.4, -0.13],[0.06, 0.47, -0.04],'tail_1')
bone('tail_3',[0.06, 0.47, -0.04],[0.06, 0.49, 0.12],'tail_2')
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
    p=v.co-Vector((0,0,0.2565920054912567))
    if p.y < -0.30000000000000004 and p.z > 0.015:
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
    result = composer.build_action(rig,mapping,anim,['quad_core','basicquad'],intensity=intensity,unit=.728516,name_prefix='wu_xing_zhen_yu_shou')
    if result.missing_slots:
        raise RuntimeError((name,result.missing_slots))
    action = result.action
    action.name = 'wu_xing_zhen_yu_shou.'+name
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

# J breath: 1.2 seconds preparation, one directed release, one second recovery.
from bl_ext.blender_org.animal_animator.core import posing
from mathutils import Quaternion, Euler
posing.reset_pose(rig)
breath=bpy.data.actions.new('wu_xing_zhen_yu_shou.breath')
composer.assign_action(rig,breath)
for frame,inhale,release in [(0,0,0),(12,.5,0),(28,1,0),(29,.5,1),(35,0,.6),(44,0,0),(53,0,0)]:
    posing.reset_pose(rig)
    rig.pose.bones['chest'].rotation_quaternion=Euler((math.radians(-9)*inhale,0,0)).to_quaternion()
    rig.pose.bones['neck'].rotation_quaternion=Euler((math.radians(-12)*inhale+math.radians(18)*release,0,0)).to_quaternion()
    rig.pose.bones['head'].rotation_quaternion=Euler((math.radians(8)*inhale-math.radians(10)*release,0,0)).to_quaternion()
    for side in ('L','R'):
        rig.pose.bones['front_lower.'+side].rotation_quaternion=Euler((math.radians(10)*inhale,0,0)).to_quaternion()
    for pb in rig.pose.bones:
        for prop in ('location','rotation_quaternion','scale'):pb.keyframe_insert(data_path=prop,frame=frame,group=pb.name)
actions['breath']=breath
track=rig.animation_data.nla_tracks.new();track.name=breath.name;track.mute=True;strip=track.strips.new(breath.name,0,breath);strip.name=breath.name
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
(OUT/'wu-xing-zhen-yu-shou-grounding.json').write_text(json.dumps(floor_audit,indent=2),encoding='utf-8')
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
PROFILES["wu_xing_zhen_yu_shou"]=(28,26,35,32,1.0)
motion_audit = refine_locomotion(rig, mesh, actions)
from refine_motion_v2 import refine as refine_v2
motion_audit["v2"]=refine_v2(rig,mesh,actions)
(OUT / (rig.name.removesuffix('_rig') + '-motion-audit.json')).write_text(json.dumps(motion_audit, indent=2), encoding='utf-8')

animated = OUT/'wu_xing_zhen_yu_shou.glb'
bpy.ops.export_scene.gltf(filepath=str(animated),export_format='GLB',use_selection=True,use_active_scene=True,export_animations=True,export_animation_mode='NLA_TRACKS',export_force_sampling=True,export_frame_range=False,export_skins=True,export_all_influences=False,export_yup=True,export_image_format='AUTO')

# A single static distant asset, never a second animated instance.
lod = mesh.copy()
lod.data = mesh.data.copy()
bpy.context.scene.collection.objects.link(lod)
lod.name = 'wu_xing_zhen_yu_shou_static_lod'
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
bpy.ops.export_scene.gltf(filepath=str(OUT/'wu_xing_zhen_yu_shou_lod.glb'),export_format='GLB',use_selection=True,use_active_scene=True,export_animations=False,export_skins=False,export_yup=True,export_image_format='AUTO')
lod.hide_render = True
lod.hide_set(True)
bpy.context.view_layer.objects.active = rig
mesh.select_set(True)
rig.select_set(True)
bpy.ops.wm.save_as_mainfile(filepath=str(OUT/'wu-xing-zhen-yu-shou-animated.blend'))
report = {'name':'五行镇狱兽','source':str(SOURCE),'source_sha256':hashlib.sha256(SOURCE.read_bytes()).hexdigest(),'source_triangles':source_triangles,'isolated_vertices_repaired':unweighted_before_repair,'mesh_vertices':len(mesh.data.vertices),'rig_joints':len(data.bones),'clips':{k:float(a.frame_range[1])/24 for k,a in actions.items()},'animated_sha256':hashlib.sha256(animated.read_bytes()).hexdigest(),'torso_yaw_degrees':20,'head_yaw_degrees':-20,'target_height_blocks':3.6,'authoring':'Blender MCP + Animal Animator quad_core/basicquad','status':'exported_candidate_pending_visual_and_runtime_qa'}
(OUT/'wu-xing-zhen-yu-shou-manifest.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(report,ensure_ascii=False))
