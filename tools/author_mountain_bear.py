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
SOURCE = ROOT / 'asset_sources/creature-batch-20261016/sources/镇岳熊.glb'
OUT.mkdir(exist_ok=True)
assert bpy.context.scene.name == 'MountainBearAuthoring'
# Remove scene objects only. Factory reset disables the running MCP and addons.
for obj in list(bpy.context.scene.objects):
    bpy.data.objects.remove(obj, do_unlink=True)
for action in list(bpy.data.actions):
    if action.name.startswith('zhen_yue_xiong'):
        bpy.data.actions.remove(action)
bpy.ops.import_scene.gltf(filepath=str(SOURCE))
mesh = next(o for o in bpy.context.scene.objects if o.type == 'MESH')
mesh.name = 'zhen_yue_xiong_mesh'
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
mesh.data.transform(Matrix.Translation(Vector((0,0,.294189))))
def pt(v):
    return Vector(v)+Vector((0,0,.294189))

data = bpy.data.armatures.new('zhen_yue_xiong_rig')
rig = bpy.data.objects.new('zhen_yue_xiong_rig', data)
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
bone('root',(0,0,-.294189),(0,0,-.20)).use_deform=False
bone('hips',(0,.25,-.005),(0,.12,.045),'root')
bone('spine',(0,.12,.045),(0,-.015,.085),'hips')
bone('chest',(0,-.015,.085),(0,-.16,.105),'spine')
bone('neck',(0,-.16,.105),(.025,-.30,.105),'chest')
bone('head',(.025,-.30,.105),(.075,-.445,.08),'neck')
legs={
 'front_upper.L':((-.19,-.09,.07),(-.22,-.17,-.09),(-.205,-.25,-.255),(-.205,-.33,-.278)),
 'front_upper.R':((.19,-.09,.07),(.22,-.18,-.09),(.19,-.27,-.255),(.19,-.35,-.278)),
 'hind_upper.L':((-.145,.25,-.005),(-.18,.30,-.15),(-.17,.30,-.26),(-.17,.21,-.278)),
 'hind_upper.R':((.145,.24,-.005),(.18,.29,-.15),(.17,.28,-.26),(.17,.19,-.278)),
}
for name,points in legs.items():
 prefix,side=name.split('.');base=prefix.split('_')[0]
 bone(name,points[0],points[1],'chest' if base=='front' else 'hips')
 bone(base+'_lower.'+side,points[1],points[2],name)
 bone(base+'_foot.'+side,points[2],points[3],base+'_lower.'+side)
tail=[(0,.35,-.03),(0,.385,-.04),(0,.42,-.055),(0,.455,-.07),(0,.49,-.08)]
for i in range(4):bone('tail_'+str(i),tail[i],tail[i+1],'hips' if i==0 else 'tail_'+str(i-1))
bpy.ops.object.mode_set(mode='OBJECT')
bpy.ops.object.select_all(action='DESELECT')
mesh.select_set(True)
rig.select_set(True)
bpy.context.view_layer.objects.active = rig
# Analytic anatomical weights for the disconnected armoured source surface.
# Each side/limb is constrained to its own local chain to avoid cross-leg bleed.
mesh.parent = rig
mod=mesh.modifiers.new('MountainBearSkin','ARMATURE');mod.object=rig
for b in data.bones:
    if b.use_deform: mesh.vertex_groups.new(name=b.name)
def segment_distance(p,b):
    a=b.head_local;delta=b.tail_local-a
    t=max(0,min(1,(p-a).dot(delta)/max(delta.length_squared,1e-12)))
    return (p-(a+delta*t)).length
for v in mesh.data.vertices:
    p=v.co-Vector((0,0,.294189));side='L' if p.x<0 else 'R'
    names=['hips','spine','chest','neck','head']
    if abs(p.x)>.075 and p.z<.14:
        prefix='front' if p.y<.02 else 'hind'
        names += [prefix+'_'+part+'.'+side for part in ('upper','lower','foot')]
        if p.z<-.12: names=[prefix+'_'+part+'.'+side for part in ('upper','lower','foot')]
    if p.y>.36 and abs(p.x)<.085: names=['hips']+['tail_'+str(i) for i in range(4)]
    scores=sorted([(n,1/(segment_distance(v.co,data.bones[n])+.025)**4) for n in names],key=lambda x:x[1],reverse=True)[:4]
    total=sum(w for _,w in scores)
    for n,w in scores:mesh.vertex_groups[n].add([v.index],w/total,'REPLACE')
assert all(v.groups for v in mesh.data.vertices)


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

# Skull follows the head; dorsal moss and twigs follow the torso.
for v in mesh.data.vertices:
    p=v.co-Vector((0,0,.294189))
    if p.y < -.31 and p.z > -.045:
        for group in mesh.vertex_groups: group.remove([v.index])
        mesh.vertex_groups['head'].add([v.index],1.0,'REPLACE')
    elif p.z > .15 and p.y > -.21:
        for group in mesh.vertex_groups: group.remove([v.index])
        mesh.vertex_groups['chest' if p.y < .02 else 'spine'].add([v.index],1.0,'REPLACE')
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
clips = [('idle','idle_breathe',.25),('walk','walk',.28),('run','run',.34),('attack','attack_claw.L',.65),('hurt','hit_react',.4),('death','death',.65)]
actions = {}
for name,source,intensity in clips:
    anim = library.anim_by_id(['quad_core','basicquad'],source)
    result = composer.build_action(rig,mapping,anim,['quad_core','basicquad'],intensity=intensity,unit=.585,name_prefix='zhen_yue_xiong')
    if result.missing_slots:
        raise RuntimeError((name,result.missing_slots))
    action = result.action
    action.name = 'zhen_yue_xiong.'+name
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

# Bear-specific two-paw heavy strike: 1.5s tell then 2s recovery.
from bl_ext.blender_org.animal_animator.core import posing
from mathutils import Quaternion, Euler
posing.reset_pose(rig)
slam=bpy.data.actions.new('zhen_yue_xiong.slam')
composer.assign_action(rig,slam)
for frame,amount in [(0,0),(12,.4),(30,1),(35,1),(38,-.12),(48,0),(84,0)]:
    posing.reset_pose(rig)
    for side in ('L','R'):
        rig.pose.bones['front_upper.'+side].rotation_quaternion=Euler((math.radians(-62)*amount,0,0)).to_quaternion()
        rig.pose.bones['front_lower.'+side].rotation_quaternion=Euler((math.radians(78)*amount,0,0)).to_quaternion()
        rig.pose.bones['front_foot.'+side].rotation_quaternion=Euler((math.radians(-12)*amount,0,0)).to_quaternion()
    rig.pose.bones['chest'].rotation_quaternion=Euler((math.radians(-12)*amount,0,0)).to_quaternion()
    rig.pose.bones['head'].rotation_quaternion=Euler((math.radians(9)*amount,0,0)).to_quaternion()
    for pb in rig.pose.bones:
        for prop in ('location','rotation_quaternion','scale'):pb.keyframe_insert(data_path=prop,frame=frame,group=pb.name)
actions['slam']=slam
track=rig.animation_data.nla_tracks.new();track.name=slam.name;track.mute=True;strip=track.strips.new(slam.name,0,slam);strip.name=slam.name
rig.animation_data.action=None

# Side fall with shoulder plates following the torso.
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
(OUT/'zhen-yue-xiong-grounding.json').write_text(json.dumps(floor_audit,indent=2),encoding='utf-8')
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

animated = OUT/'zhen_yue_xiong.glb'
bpy.ops.export_scene.gltf(filepath=str(animated),export_format='GLB',use_selection=True,use_active_scene=True,export_animations=True,export_animation_mode='NLA_TRACKS',export_force_sampling=True,export_frame_range=False,export_skins=True,export_all_influences=False,export_yup=True,export_image_format='AUTO')

# A single static distant asset, never a second animated instance.
lod = mesh.copy()
lod.data = mesh.data.copy()
bpy.context.scene.collection.objects.link(lod)
lod.name = 'zhen_yue_xiong_static_lod'
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
bpy.ops.export_scene.gltf(filepath=str(OUT/'zhen_yue_xiong_lod.glb'),export_format='GLB',use_selection=True,use_active_scene=True,export_animations=False,export_skins=False,export_yup=True,export_image_format='AUTO')
lod.hide_render = True
lod.hide_set(True)
bpy.context.view_layer.objects.active = rig
mesh.select_set(True)
rig.select_set(True)
bpy.ops.wm.save_as_mainfile(filepath=str(OUT/'zhen-yue-xiong-animated.blend'))
report = {'name':'镇岳熊','source':str(SOURCE),'source_sha256':hashlib.sha256(SOURCE.read_bytes()).hexdigest(),'source_triangles':source_triangles,'mesh_vertices':len(mesh.data.vertices),'rig_joints':len(data.bones),'clips':{k:float(a.frame_range[1])/24 for k,a in actions.items()},'animated_sha256':hashlib.sha256(animated.read_bytes()).hexdigest(),'torso_yaw_degrees':20,'head_yaw_degrees':-20,'target_height_blocks':2.5,'authoring':'Blender MCP + Animal Animator quad_core/basicquad','status':'exported_candidate_pending_visual_and_runtime_qa'}
(OUT/'zhen-yue-xiong-manifest.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(report,ensure_ascii=False))
