"""Execute through Blender MCP in the saved refined-all-beasts scene."""
import bpy
from pathlib import Path
from mathutils import Vector
from bl_ext.blender_org.animal_animator.core import composer
from refine_quadruped_motion import PROFILES

OUT=(Path(__file__).resolve().parents[1] / 'asset_sources/animation-readability-20261017/previews')
OUT.mkdir(exist_ok=True)
for ident in [*PROFILES,'ting_yu_ya','lie_yan_zhu']:
    rig=bpy.data.objects[ident+'_rig'];mesh=bpy.data.objects[ident+'_mesh']
    scene=next(s for s in bpy.data.scenes if rig.name in s.objects);bpy.context.window.scene=scene
    scene.render.engine='BLENDER_WORKBENCH';scene.render.resolution_x=384;scene.render.resolution_y=280;scene.render.resolution_percentage=100
    scene.display.shading.color_type='TEXTURE';scene.display.shading.light='STUDIO';scene.display.shading.background_type='WORLD'
    if not scene.world:scene.world=bpy.data.worlds.new('MotionPreviewWorld')
    scene.world.color=(.16,.16,.16)
    cam=scene.camera
    if not cam:
        cam=bpy.data.objects.new('motion_preview_'+ident,bpy.data.cameras.new('motion_preview_'+ident));scene.collection.objects.link(cam);scene.camera=cam
    rig.animation_data.action=None
    for p in rig.pose.bones:
        p.location=(0,0,0);p.rotation_quaternion=(1,0,0,0);p.rotation_euler=(0,0,0);p.scale=(1,1,1)
    scene.frame_set(0);bpy.context.view_layer.update()
    ev=mesh.evaluated_get(bpy.context.evaluated_depsgraph_get())
    points=[ev.matrix_world @ v.co for v in ev.data.vertices]
    lower=Vector(tuple(min(p[i] for p in points) for i in range(3)));upper=Vector(tuple(max(p[i] for p in points) for i in range(3)))
    target=(lower+upper)/2;size=max(upper-lower)
    cam.data.type='ORTHO';cam.data.ortho_scale=size*1.9
    cam.location=target+Vector((3,.6,1))*size;cam.rotation_euler=(target-cam.location).to_track_quat('-Z','Y').to_euler()
    action=next(t.strips[0].action for t in rig.animation_data.nla_tracks if t.name==ident+'.walk')
    composer.assign_action(rig,action);start,end=action.frame_range
    for i in range(4):
        scene.frame_set(round(start+(end-start)*i/4));scene.render.filepath=str(OUT/f'{ident}_{i}.png');bpy.ops.render.render(write_still=True)
    rig.animation_data.action=None
    for p in rig.pose.bones:
        p.location=(0,0,0);p.rotation_quaternion=(1,0,0,0);p.rotation_euler=(0,0,0);p.scale=(1,1,1)
    scene.frame_set(0)
print('Motion previews complete: 14 species, four walk phases each')
