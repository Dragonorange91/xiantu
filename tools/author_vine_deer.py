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
SOURCE = ROOT / 'asset_sources/creature-batch-20261016/sources/青藤鹿.glb'
OUT.mkdir(exist_ok=True)
# Remove scene objects only. Factory reset disables the running MCP and addons.
for obj in list(bpy.context.scene.objects):
    bpy.data.objects.remove(obj, do_unlink=True)
for action in list(bpy.data.actions):
    if action.name.startswith('qing_teng_lu'):
        bpy.data.actions.remove(action)
bpy.ops.import_scene.gltf(filepath=str(SOURCE))
mesh = next(o for o in bpy.context.scene.objects if o.type == 'MESH')
mesh.name = 'qing_teng_lu_mesh'
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
yaw = math.radians(46)
rotation = Matrix.Rotation(yaw, 4, 'Z')
origin = Vector((-.025, -.005, -.47631800174713135))
transform = rotation @ Matrix.Translation(-origin)
mesh.data.transform(transform)
def pt(v):
    return transform @ Vector(v)

data = bpy.data.armatures.new('qing_teng_lu_rig')
rig = bpy.data.objects.new('qing_teng_lu_rig', data)
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
bone('root', (-.025,-.005,-.476318), (-.025,-.005,-.36)).use_deform = False
bone('hips', (.10,.075,-.09), (.015,.01,-.065), 'root')
bone('spine', (.015,.01,-.065), (-.075,-.065,-.035), 'hips')
bone('chest', (-.075,-.065,-.035), (-.145,-.13,.015), 'spine')
bone('neck', (-.145,-.13,.015), (-.16,-.205,.16), 'chest')
bone('head', (-.16,-.205,.16), (-.16,-.31,.15), 'neck')
legs = {
 'front_upper.L': ((-.22,-.075,-.12),(-.205,-.07,-.28),(-.2369,-.0841,-.441),(-.25,-.115,-.466)),
 'front_upper.R': ((-.09,-.19,-.12),(-.07,-.16,-.28),(-.098,-.2068,-.44),(-.11,-.235,-.465)),
 'hind_upper.L': ((.045,.14,-.105),(-.005,.085,-.25),(.0268,.1486,-.44),(.01,.12,-.465)),
 'hind_upper.R': ((.18,.025,-.105),(.155,-.02,-.25),(.2168,.0539,-.439),(.20,.025,-.465)),
}
for name, points in legs.items():
 prefix,side=name.split('.');base=prefix.split('_')[0]
 bone(name,points[0],points[1],'chest' if base=='front' else 'hips')
 bone(base+'_lower.'+side,points[1],points[2],name)
 bone(base+'_foot.'+side,points[2],points[3],base+'_lower.'+side)
tail=[(.14,.12,-.07),(.205,.19,-.10),(.27,.25,-.08),(.32,.30,-.015),(.35,.32,.065)]
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

# Straighten the head independently without rotating the whole animal back.
# Antlers are rigid head ornaments, never nearest-bone limbs.
for v in mesh.data.vertices:
    src = transform.inverted() @ v.co
    if src.z > .12:
        for group in mesh.vertex_groups: group.remove([v.index])
        mesh.vertex_groups['head'].add([v.index],1.0,'REPLACE')
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
profiles.store_mapping(rig,'HORSE',mapping)
profiles.sync_collection(rig,mapping)
bpy.context.scene.animal_animator.target = rig
bpy.context.scene.render.fps = 24
clips = [('idle','idle_breathe',.3),('walk','walk',.32),('run','run',.4),('hurt','hit_react',.45),('death','death',.7)]
actions = {}
for name,source,intensity in clips:
    anim = library.anim_by_id(['quad_core','horse'],source)
    result = composer.build_action(rig,mapping,anim,['quad_core','horse'],intensity=intensity,unit=.95,name_prefix='qing_teng_lu')
    if result.missing_slots:
        raise RuntimeError((name,result.missing_slots))
    action = result.action
    action.name = 'qing_teng_lu.'+name
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

# Keep the collapsing body above the ground throughout the authored death clip.
# Correct the deforming pose itself; renderer offsets must stay identical for all clips.
composer.assign_action(rig, actions['death'])
death_floor = []
for frame in range(49):
    bpy.context.scene.frame_set(frame)
    evaluated = mesh.evaluated_get(bpy.context.evaluated_depsgraph_get())
    low = min((evaluated.matrix_world @ v.co).z for v in evaluated.data.vertices)
    death_floor.append(max(0.0, -low))
for frame, lift in enumerate(death_floor):
    pb = rig.pose.bones['root']
    pb.location = pb.bone.matrix_local.to_3x3().inverted() @ Vector((0,0,lift))
    pb.keyframe_insert(data_path='location', frame=frame, group='root')
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

animated = OUT/'qing_teng_lu.glb'
bpy.ops.export_scene.gltf(filepath=str(animated),export_format='GLB',use_selection=True,export_animations=True,export_animation_mode='NLA_TRACKS',export_force_sampling=True,export_frame_range=False,export_skins=True,export_all_influences=False,export_yup=True,export_image_format='AUTO')

# A single static distant asset, never a second animated instance.
lod = mesh.copy()
lod.data = mesh.data.copy()
bpy.context.scene.collection.objects.link(lod)
lod.name = 'qing_teng_lu_static_lod'
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
bpy.ops.export_scene.gltf(filepath=str(OUT/'qing_teng_lu_lod.glb'),export_format='GLB',use_selection=True,export_animations=False,export_skins=False,export_yup=True,export_image_format='AUTO')
lod.hide_render = True
lod.hide_set(True)
bpy.context.view_layer.objects.active = rig
mesh.select_set(True)
rig.select_set(True)
bpy.ops.wm.save_as_mainfile(filepath=str(OUT/'qing-teng-lu-animated.blend'))
report = {'name':'青藤鹿','source':str(SOURCE),'source_sha256':hashlib.sha256(SOURCE.read_bytes()).hexdigest(),'source_triangles':source_triangles,'mesh_vertices':len(mesh.data.vertices),'rig_joints':len(data.bones),'clips':{k:float(a.frame_range[1])/24 for k,a in actions.items()},'animated_sha256':hashlib.sha256(animated.read_bytes()).hexdigest(),'torso_yaw_degrees':46,'head_yaw_degrees':-46,'target_height_blocks':2.1,'authoring':'Blender MCP + Animal Animator quad_core/horse','status':'exported_candidate_pending_visual_and_runtime_qa'}
(OUT/'qing-teng-lu-manifest.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(report,ensure_ascii=False))
