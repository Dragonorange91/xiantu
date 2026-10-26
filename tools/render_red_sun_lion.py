import bpy
from pathlib import Path
from mathutils import Vector
from bl_ext.blender_org.animal_animator.core import composer, posing
OUT=Path(r'asset_sources\creature-batch-next-20261017\red-lion-previews')
OUT.mkdir(exist_ok=True)
scene=bpy.context.scene
scene.render.engine='BLENDER_WORKBENCH'
scene.render.resolution_x=512
scene.render.resolution_y=512
scene.render.resolution_percentage=100
scene.display.shading.color_type='TEXTURE'
scene.display.shading.light='STUDIO'
scene.display.shading.background_type='WORLD'
if scene.world is None: scene.world=bpy.data.worlds.new('PreviewWorld')
scene.world.color=(.16,.16,.16)
cd=bpy.data.cameras.new('preview_camera')
cam=bpy.data.objects.new('preview_camera',cd)
scene.collection.objects.link(cam)
scene.camera=cam
cd.type='ORTHO'
cd.ortho_scale=1.65
rig=bpy.data.objects['chi_yao_shi_rig']
for label,clip,frame,offset in [('front','idle',0,(0,-3,.65)),('side','idle',0,(3,0,.65)),('walk1','walk',10,(2,-3,1)),('walk2','walk',30,(2,-3,1)),('run','run',7,(2,-3,1)),('slam','slam',32,(2,-3,1)),('death','death',48,(2,-3,1))]:
    posing.reset_pose(rig)
    composer.assign_action(rig,bpy.data.actions['chi_yao_shi.'+clip])
    scene.frame_set(frame)
    target=Vector((0,.07,.34))
    cam.location=target+Vector(offset)
    cam.rotation_euler=(target-cam.location).to_track_quat('-Z','Y').to_euler()
    scene.render.filepath=str(OUT/('chi_yao_shi_'+label+'.png'))
    bpy.ops.render.render(write_still=True)
rig.animation_data.action=None
posing.reset_pose(rig)
scene.frame_set(0)
print('PREVIEWS_COMPLETE')
