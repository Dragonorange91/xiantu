import bpy,json
from pathlib import Path
from mathutils import Vector
from bl_ext.blender_org.animal_animator.core import composer,posing
OUT=Path(r'asset_sources\creature-completion-20261017\previews')
IDS=['cang_lin_mang','cang_zhi_mang','chao_wen_yao','xuan_ke_xie','jin_bei_jia_chong','bi_ye_tang','mo_san_chan','bai_zhan_jiang_hun','xing_yun_tian_jiang','tong_ling_yuan_wang','xuan_tian_cheng_zhu','yun_hai_tian_jun']
for ident in IDS:
    rig=bpy.data.objects[ident+'_rig'];mesh=bpy.data.objects[ident+'_mesh'];sc=next(s for s in rig.users_scene if s.name.startswith('CompletionRig_'));bpy.context.window.scene=sc
    sc.render.engine='BLENDER_WORKBENCH';sc.render.resolution_x=400;sc.render.resolution_y=400;sc.render.resolution_percentage=100;sc.display.shading.color_type='TEXTURE';sc.display.shading.light='STUDIO'
    cd=bpy.data.cameras.new(ident+'_preview');cam=bpy.data.objects.new(ident+'_preview',cd);sc.collection.objects.link(cam);sc.camera=cam;cd.type='ORTHO';cd.ortho_scale=1.55
    # Camera covers weapons as well as the source body.
    if ident in ('xing_yun_tian_jiang','tong_ling_yuan_wang'):cd.ortho_scale=1.85
    target=Vector((0,0,.40 if ident not in ('chao_wen_yao','xuan_ke_xie','jin_bei_jia_chong') else .20))
    for label,clip,phase,offset in [('front','idle',0,(0,-3,.4)),('move1','walk',.25,(2,-3,1)),('move2','walk',.75,(2,-3,1)),('attack','attack',.5,(2,-3,1))]:
        action=bpy.data.actions.get(ident+'.'+clip) or bpy.data.actions[ident+'.hurt']
        posing.reset_pose(rig);composer.assign_action(rig,action);sc.frame_set(round(action.frame_range[1]*phase))
        cam.location=target+Vector(offset);cam.rotation_euler=(target-cam.location).to_track_quat('-Z','Y').to_euler();sc.render.filepath=str(OUT/(ident+'_'+label+'.png'));bpy.ops.render.render(write_still=True)
    rig.animation_data.action=None;posing.reset_pose(rig);sc.frame_set(0)
print('Completion visual review frames saved')
