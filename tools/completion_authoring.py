"""Shared Blender MCP authoring operations, never a substitute for per-source anatomy."""
import bpy, bmesh, math, json, hashlib, sys
from pathlib import Path
from mathutils import Matrix, Vector, Quaternion
from bl_ext.blender_org.animal_animator.core import composer, posing
ROOT=(Path(__file__).resolve().parents[1])
OUT=ROOT/'asset_sources/creature-completion-20261017'
sys.path.insert(0,str(ROOT/'tools'))
from complete_skin_weights import repair

def setup(name,ident,yaw=0):
    scene=bpy.data.scenes.new('CompletionRig_'+ident)
    bpy.context.window.scene=scene
    source=ROOT/'asset_sources/creature-batch-20261016/sources'/(name+'.glb')
    bpy.ops.import_scene.gltf(filepath=str(source))
    mesh=next(o for o in scene.objects if o.type=='MESH');mesh.name=ident+'_mesh'
    mesh.data.transform(mesh.matrix_world);mesh.matrix_world.identity()
    mesh.data.transform(Matrix.Rotation(math.radians(yaw),4,'Z'))
    bm=bmesh.new();bm.from_mesh(mesh.data)
    bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=1e-6)
    bmesh.ops.dissolve_degenerate(bm,edges=list(bm.edges),dist=1e-7)
    bm.to_mesh(mesh.data);bm.free()
    ground=min(v.co.z for v in mesh.data.vertices)
    mesh.data.transform(Matrix.Translation((0,0,-ground)))
    data=bpy.data.armatures.new(ident+'_rig');rig=bpy.data.objects.new(ident+'_rig',data)
    scene.collection.objects.link(rig)
    bpy.ops.object.select_all(action='DESELECT');rig.select_set(True)
    bpy.context.view_layer.objects.active=rig;bpy.ops.object.mode_set(mode='EDIT')
    def bone(name,head,tail,parent='root',deform=True):
        b=data.edit_bones.new(name);b.head=Vector(head)-Vector((0,0,ground));b.tail=Vector(tail)-Vector((0,0,ground));b.use_deform=deform
        if parent:b.parent=data.edit_bones[parent]
        return b
    bone('root',(0,0,ground),(0,0,ground+.1),None,False)
    return scene,mesh,rig,bone,source,ground

def bind(mesh,rig):
    bpy.ops.object.mode_set(mode='OBJECT');bpy.ops.object.select_all(action='DESELECT')
    mesh.select_set(True);rig.select_set(True);bpy.context.view_layer.objects.active=rig
    bpy.ops.object.parent_set(type='ARMATURE_AUTO')
    repaired=repair(mesh,rig)
    for v in mesh.data.vertices:
        weights=sorted([(g.group,g.weight) for g in v.groups if g.weight>1e-8],key=lambda x:-x[1])
        for gi,w in weights[4:]:mesh.vertex_groups[gi].remove([v.index])
        total=sum(w for _,w in weights[:4])
        assert total>0
        for gi,w in weights[:4]:mesh.vertex_groups[gi].add([v.index],w/total,'REPLACE')
    return repaired

def reset(rig):
    rig.animation_data_create();rig.animation_data.action=None;posing.reset_pose(rig)
    for p in rig.pose.bones:p.rotation_mode='QUATERNION';p.rotation_quaternion=(1,0,0,0);p.location=(0,0,0);p.scale=(1,1,1)
    bpy.context.scene.frame_set(0)

def rotate(rig,name,axis,degrees):
    p=rig.pose.bones[name]
    local=p.bone.matrix_local.to_3x3().inverted()@Vector(axis)
    p.rotation_quaternion=Quaternion(local,math.radians(degrees))

def translate(rig,name,offset):
    p=rig.pose.bones[name];p.location=p.bone.matrix_local.to_3x3().inverted()@Vector(offset)

def author_action(rig,mesh,name,frames,pose,loop=False,grounded=True):
    ident=rig.name.removesuffix('_rig');reset(rig)
    a=bpy.data.actions.new(ident+'.'+name);composer.assign_action(rig,a)
    # Every clip keys every property, preventing stale transition poses.
    for frame in range(frames+1):
        posing.reset_pose(rig)
        for p in rig.pose.bones:p.rotation_mode='QUATERNION'
        pose(frame%frames if loop else frame,frames)
        for p in rig.pose.bones:
            for prop in ('location','rotation_quaternion','scale'):p.keyframe_insert(data_path=prop,frame=frame,group=p.name)
    audit=[]
    if grounded:
        for frame in range(frames+1):
            bpy.context.scene.frame_set(frame);ev=mesh.evaluated_get(bpy.context.evaluated_depsgraph_get())
            low=min((ev.matrix_world@v.co).z for v in ev.data.vertices)
            p=rig.pose.bones['root'];p.location+=p.bone.matrix_local.to_3x3().inverted()@Vector((0,0,max(0,-low)))
            p.keyframe_insert(data_path='location',frame=frame,group=p.name);audit.append(low)
    for layer in a.layers:
        for strip in layer.strips:
            for bag in strip.channelbags:
                for fc in bag.fcurves:
                    for k in fc.keyframe_points:k.interpolation='LINEAR'
    composer.stash_action(rig,a);track=rig.animation_data.nla_tracks[-1];track.name=a.name;track.strips[0].name=a.name;track.mute=True
    reset(rig)
    return a

def head_correct(mesh,rig,yaw,names=('head',)):
    if not yaw:return
    pivot=rig.data.bones[names[0]].head_local.copy()
    indices={mesh.vertex_groups[n].index for n in names}
    rot=Matrix.Rotation(math.radians(-yaw),3,'Z')
    for v in mesh.data.vertices:
        weight=sum(g.weight for g in v.groups if g.group in indices)
        if weight:v.co=pivot+Matrix.Rotation(math.radians(-yaw)*weight,3,'Z')@(v.co-pivot)
    bpy.context.view_layer.objects.active=rig;bpy.ops.object.mode_set(mode='EDIT')
    for n in names:
        b=rig.data.edit_bones[n];b.head=pivot+rot@(b.head-pivot);b.tail=pivot+rot@(b.tail-pivot)
    bpy.ops.object.mode_set(mode='OBJECT')

def finish(name,ident,mesh,rig,source,actions,height,method):
    reset(rig);scene=bpy.context.scene;scene.render.fps=24
    bounds=[[min(v.co[i] for v in mesh.data.vertices),max(v.co[i] for v in mesh.data.vertices)] for i in range(3)]
    bpy.ops.object.select_all(action='DESELECT');mesh.select_set(True);rig.select_set(True);bpy.context.view_layer.objects.active=rig
    dest=OUT/(ident+'.glb')
    bpy.ops.export_scene.gltf(filepath=str(dest),export_format='GLB',use_selection=True,use_active_scene=True,export_animations=True,export_animation_mode='NLA_TRACKS',export_force_sampling=True,export_frame_range=False,export_skins=True,export_all_influences=False,export_yup=True,export_image_format='AUTO')
    lod=mesh.copy();lod.data=mesh.data.copy();scene.collection.objects.link(lod);lod.name=ident+'_lod';lod.parent=None;lod.matrix_world.identity();lod.modifiers.clear();lod.vertex_groups.clear()
    bpy.ops.object.select_all(action='DESELECT');lod.select_set(True);bpy.context.view_layer.objects.active=lod
    dec=lod.modifiers.new('distant_only','DECIMATE');dec.ratio=.55;bpy.ops.object.modifier_apply(modifier=dec.name);lod.data.validate();lod.data.update()
    bpy.ops.export_scene.gltf(filepath=str(OUT/(ident+'_lod.glb')),export_format='GLB',use_selection=True,use_active_scene=True,export_animations=False,export_skins=False,export_yup=True,export_image_format='AUTO')
    lod.hide_render=True;lod.hide_set(True)
    bpy.ops.wm.save_as_mainfile(filepath=str(OUT/(ident+'-animated.blend')))
    body_height=mesh.get('body_height',bounds[2][1]-bounds[2][0])
    report={'name':name,'id':ident,'source':str(source),'source_sha256':hashlib.sha256(source.read_bytes()).hexdigest(),'animated_sha256':hashlib.sha256(dest.read_bytes()).hexdigest(),'joints':len(rig.data.bones),'clips':list(actions),'bounds':bounds,'body_height':body_height,'target_height_blocks':height,'model_scale':height/body_height,'authoring':method,'status':'candidate_pending_visual_and_runtime_qa'}
    (OUT/(ident+'-manifest.json')).write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf8')
    print('EXPORTED',ident,len(rig.data.bones),list(actions))
