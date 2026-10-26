"""Blender MCP bird-specific rig, original atlas and Animal Animator motions."""
import bpy,bmesh,math,json,hashlib,copy
from pathlib import Path
from mathutils import Vector,Matrix
from bl_ext.blender_org.animal_animator.core import composer,library,profiles,posing
ROOT=(Path(__file__).resolve().parents[1])
OUT=ROOT/'asset_sources/creature-completion-20261017'
SOURCE=ROOT/'asset_sources/creature-batch-20261016/sources/火喙隼.glb'
for o in list(bpy.context.scene.objects):bpy.data.objects.remove(o,do_unlink=True)
for a in list(bpy.data.actions):
    if a.name.startswith('huo_hui_sun'):bpy.data.actions.remove(a)
bpy.ops.import_scene.gltf(filepath=str(SOURCE))
m=next(o for o in bpy.context.scene.objects if o.type=='MESH');m.name='huo_hui_sun_mesh'
m.data.transform(m.matrix_world);m.matrix_world.identity()
bm=bmesh.new();bm.from_mesh(m.data)
bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=1e-6)
bmesh.ops.dissolve_degenerate(bm,edges=list(bm.edges),dist=1e-7)
bm.to_mesh(m.data);bm.free()
m.data.transform(Matrix.Rotation(math.radians(30),4,'Z'))
ground=min(v.co.z for v in m.data.vertices)
transform=Matrix.Translation((0,0,-ground));m.data.transform(transform)
def pt(v):return transform@Vector(v)
data=bpy.data.armatures.new('huo_hui_sun_rig');rig=bpy.data.objects.new('huo_hui_sun_rig',data);bpy.context.scene.collection.objects.link(rig)
bpy.ops.object.select_all(action='DESELECT');rig.select_set(True);bpy.context.view_layer.objects.active=rig;bpy.ops.object.mode_set(mode='EDIT')
def bone(n,h,t,parent=None):
    b=data.edit_bones.new(n);b.head=pt(h);b.tail=pt(t)
    if parent:b.parent=data.edit_bones[parent]
    return b
bone('root',(0,0,ground),(0,0,ground+.1)).use_deform=False
bone('hips',(0,0,-.2),(0,-.07,-.12),'root')
bone('spine',(0,-.07,-.12),(0,-.12,0),'hips')
bone('chest',(0,-.12,0),(0,-.18,.08),'spine')
bone('neck',(0,-.18,.08),(0,-.27,.14),'chest')
bone('head',(0,-.27,.14),(.05,-.37,.12),'neck')
bone('beak',(.035,-.35,.10),(.075,-.43,.075),'head')
for side,sgn in [('L',1),('R',-1)]:
    bone('wing_upper.'+side,(sgn*.11,-.08,-.015),(sgn*.23,-.015,.10),'chest')
    bone('wing_middle.'+side,(sgn*.23,-.015,.10),(sgn*.34,.04,.24),'wing_upper.'+side)
    bone('wing_tip.'+side,(sgn*.34,.04,.24),(sgn*.47,.08,.38),'wing_middle.'+side)
    bone('leg_upper.'+side,(sgn*.10,-.06,-.21),(sgn*.10,-.1,-.31),'hips')
    bone('leg_lower.'+side,(sgn*.10,-.1,-.31),(sgn*.10,-.16,-.385),'leg_upper.'+side)
    bone('foot.'+side,(sgn*.10,-.16,-.385),(sgn*.10,-.28,-.405),'leg_lower.'+side)
    bone('tail_feather.'+side,(sgn*.035,.12,-.23),(sgn*.1,.36,-.37),'hips')
bone('tail_0',(0,.08,-.20),(0,.22,-.285),'hips')
bone('tail_1',(0,.22,-.285),(0,.39,-.38),'tail_0')
bpy.ops.object.mode_set(mode='OBJECT')
bpy.ops.object.select_all(action='DESELECT');m.select_set(True);rig.select_set(True);bpy.context.view_layer.objects.active=rig
bpy.ops.object.parent_set(type='ARMATURE_AUTO')
import sys
sys.path.insert(0,str(ROOT/'tools'))
from complete_skin_weights import repair
repaired=repair(m,rig)
for v in m.data.vertices:
    weights=sorted([(g.group,g.weight) for g in v.groups if g.weight>1e-8],key=lambda p:p[1],reverse=True)
    if not weights:
        closest=min((b for b in data.bones if b.use_deform),key=lambda b:(v.co-(b.head_local+b.tail_local)*.5).length)
        group=m.vertex_groups[closest.name];group.add([v.index],1,'REPLACE');weights=[(group.index,1)]
    for gi,_ in weights[4:]:m.vertex_groups[gi].remove([v.index])
    total=sum(w for _,w in weights[:4])
    for gi,w in weights[:4]:m.vertex_groups[gi].add([v.index],w/total,'REPLACE')
# Counter-rotate the weighted skull, retaining a smooth neck transition.
pivot=data.bones['head'].head_local.copy()
head_groups={m.vertex_groups[n].index:1.0 for n in ('head','beak')}
head_groups[m.vertex_groups['neck'].index]=.35
for v in m.data.vertices:
 weight=sum(g.weight*head_groups.get(g.group,0) for g in v.groups)
 if weight:v.co=pivot+Matrix.Rotation(math.radians(-30)*weight,3,'Z')@(v.co-pivot)
bpy.ops.object.mode_set(mode='EDIT')
for n in ('head','beak'):
 b=data.edit_bones[n];b.head=pivot+Matrix.Rotation(math.radians(-30),3,'Z')@(b.head-pivot);b.tail=pivot+Matrix.Rotation(math.radians(-30),3,'Z')@(b.tail-pivot)
bpy.ops.object.mode_set(mode='OBJECT')
mapping={'HIPS':[['hips',1]],'SPINE':[['spine',.6],['chest',.4]],'NECK':[['neck',1]],'HEAD':[['head',1]],'BEAK':[['beak',1]],'TAIL':[['tail_0',.6],['tail_1',.4]]}
for side in ['L','R']:
    mapping['WING.'+side]=[['wing_upper.'+side,1],['wing_middle.'+side,.6],['wing_tip.'+side,.4]]
    mapping['TAIL_FEATHER.'+side]=[['tail_feather.'+side,1]]
    for slot,n in [('LEG_UPPER','leg_upper'),('LEG_LOWER','leg_lower'),('FOOT','foot')]:mapping[slot+'.'+side]=[[n+'.'+side,1]]
profiles.store_mapping(rig,'BIRD',mapping);profiles.sync_collection(rig,mapping);bpy.context.scene.animal_animator.target=rig
bpy.context.scene.render.fps=24
clips=[('idle','idle_stand',.4),('walk','walk_strut',.45),('fly','fly_flap',.55),('glide','fly_glide',.45),('takeoff','takeoff',.5),('land','land',.5),('alert','flap_display',.45),('attack','peck_ground',.65),('hurt','ruffle_shake',.45)]
actions={}
for name,src,intensity in clips:
    result=composer.build_action(rig,mapping,library.anim_by_id(['bird'],src),['bird'],unit=.84,intensity=intensity,name_prefix='huo_hui_sun')
    if result.missing_slots:raise RuntimeError((name,result.missing_slots))
    a=result.action;a.name='huo_hui_sun.'+name;actions[name]=a
    paths={fc.data_path for layer in a.layers for strip in layer.strips for bag in strip.channelbags for fc in bag.fcurves}
    for pb in rig.pose.bones:
        for prop in ['location','rotation_quaternion','scale']:
            if pb.path_from_id(prop) not in paths:pb.keyframe_insert(data_path=prop,frame=0,group=pb.name)
    t=next(t for t in rig.animation_data.nla_tracks if any(s.action==a for s in t.strips));t.name=a.name;t.strips[0].name=a.name
# Library has no death for birds: author a held side-collapse and fold the wings.
posing.reset_pose(rig);a=bpy.data.actions.new('huo_hui_sun.death');composer.assign_action(rig,a)
for frame in [0,8,18,24]:
    bpy.context.scene.frame_set(frame)
    posing.reset_pose(rig);t=frame/24
    rig.pose.bones['root'].rotation_quaternion=__import__('mathutils').Quaternion((0,0,1),math.radians(88)*t)
    rig.pose.bones['hips'].location.z=-.25*t
    for side,sgn in [('L',1),('R',-1)]:rig.pose.bones['wing_upper.'+side].rotation_quaternion=__import__('mathutils').Quaternion((0,1,0),sgn*math.radians(25)*t)
    bpy.context.view_layer.update()
    evaluated=m.evaluated_get(bpy.context.evaluated_depsgraph_get())
    low=min((evaluated.matrix_world@v.co).z for v in evaluated.data.vertices)
    rig.pose.bones['root'].location.y -= low
    for pb in rig.pose.bones:
        for prop in ['location','rotation_quaternion','scale']:pb.keyframe_insert(data_path=prop,frame=frame,group=pb.name)
composer.stash_action(rig,a);t=rig.animation_data.nla_tracks[-1];t.name=a.name;t.strips[0].name=a.name;actions['death']=a
rig.animation_data.action=None;posing.reset_pose(rig);bpy.context.scene.frame_set(0)
bpy.ops.object.select_all(action='DESELECT');m.select_set(True);rig.select_set(True);bpy.context.view_layer.objects.active=rig
import sys
sys.path.insert(0, str(ROOT / 'tools'))
from refine_bird_spider_motion import bird as readable_bird
from refine_motion_v2 import refine as readable_bird_v2
readable_bird(rig,m,actions)
from refine_motion_v2 import enlarge
enlarge(rig,m,actions['walk'],{p+'.'+side:a for side in 'LR' for p,a in [('leg_upper',44),('leg_lower',40)]})
enlarge(rig,m,actions['fly'],{p+'.'+side:a for side in 'LR' for p,a in [('wing_upper',100),('wing_middle',40),('wing_tip',28)]},False,True)
enlarge(rig,m,actions['glide'],{},False,True)
file=OUT/'huo_hui_sun.glb'
bpy.ops.export_scene.gltf(filepath=str(file),export_format='GLB',use_selection=True,use_active_scene=True,export_animations=True,export_animation_mode='NLA_TRACKS',export_force_sampling=True,export_frame_range=False,export_skins=True,export_all_influences=False,export_yup=True,export_image_format='AUTO')
lod=m.copy();lod.data=m.data.copy();bpy.context.scene.collection.objects.link(lod);lod.name='huo_hui_sun_lod';lod.parent=None;lod.matrix_world.identity();lod.modifiers.clear();lod.vertex_groups.clear()
bpy.ops.object.select_all(action='DESELECT');lod.select_set(True);bpy.context.view_layer.objects.active=lod
dec=lod.modifiers.new('distant_only','DECIMATE');dec.ratio=.55;bpy.ops.object.modifier_apply(modifier=dec.name);lod.data.validate();lod.data.update()
bpy.ops.export_scene.gltf(filepath=str(OUT/'huo_hui_sun_lod.glb'),export_format='GLB',use_selection=True,use_active_scene=True,export_animations=False,export_skins=False,export_yup=True,export_image_format='AUTO')
lod.hide_render=True;lod.hide_set(True)
bpy.ops.wm.save_as_mainfile(filepath=str(OUT/'huo-hui-sun-animated.blend'))
(OUT/'huo-hui-sun-manifest.json').write_text(json.dumps({'name':'火喙隼','target_height_blocks':1.4,'source':str(SOURCE),'source_sha256':hashlib.sha256(SOURCE.read_bytes()).hexdigest(),'output_sha256':hashlib.sha256(file.read_bytes()).hexdigest(),'joints':len(data.bones),'clips':list(actions),'status':'candidate_not_integrated_pending_visual_review','authoring':'Blender MCP + Animal Animator bird family + authored death'},ensure_ascii=False,indent=2),encoding='utf-8')
print('BIRD_EXPORTED',len(data.bones),list(actions))
