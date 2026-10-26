import bpy
from pathlib import Path
from mathutils import Vector
from bl_ext.blender_org.animal_animator.core import composer,posing
ROOT=(Path(__file__).resolve().parents[1])
OUT=ROOT/'asset_sources/creature-completion-20261017/previews'
OUT.mkdir(exist_ok=True)
for ident in ['dian_wen_diao','sui_jia_xi','rong_wen_gui','sha_jia_qiu_yu','bi_lin_e','wu_xing_zhen_yu_shou']:
 sc=bpy.data.scenes['CompletionRig_'+ident];bpy.context.window.scene=sc
 sc.render.engine='BLENDER_WORKBENCH';sc.render.resolution_x=512;sc.render.resolution_y=400;sc.render.resolution_percentage=100
 sc.display.shading.color_type='TEXTURE';sc.display.shading.light='STUDIO'
 cd=bpy.data.cameras.new(ident+'_preview');cam=bpy.data.objects.new(ident+'_preview',cd);sc.collection.objects.link(cam);sc.camera=cam;cd.type='ORTHO';cd.ortho_scale=1.35
 rig=bpy.data.objects[ident+'_rig'];target=Vector((0,.02,.28))
 for label,clip,frame,offset in [('front','idle',0,(0,-3,.4)),('side','idle',0,(3,0,.4)),('walk1','walk',10,(2,-3,1)),('walk2','walk',30,(2,-3,1)),('run','run',7,(2,-3,1)),('death','death',48,(2,-3,1))]:
  posing.reset_pose(rig);composer.assign_action(rig,bpy.data.actions[ident+'.'+clip]);sc.frame_set(frame)
  cam.location=target+Vector(offset);cam.rotation_euler=(target-cam.location).to_track_quat('-Z','Y').to_euler();sc.render.filepath=str(OUT/(ident+'_'+label+'.png'));bpy.ops.render.render(write_still=True)
 rig.animation_data.action=None;posing.reset_pose(rig);sc.frame_set(0)
print('Rendered calibrated quadruped previews')
