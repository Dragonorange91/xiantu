"""Blender MCP anatomical inspection: orthographic views, not perspective guesses."""
import bpy, sys
from pathlib import Path
from mathutils import Vector
sys.path.insert(0, str(Path(__file__).parent))
import completion_authoring as ca
OUT=ca.ROOT/'asset_sources/posture-grip-20261018'

def render(ident,label,target,offset,scale=1.2,clip=None,frame=0):
    rig=bpy.data.objects[ident+'_rig'];mesh=bpy.data.objects[ident+'_mesh']
    scene=next((s for s in rig.users_scene if s.name.startswith('CompletionRig_')),rig.users_scene[0])
    bpy.context.window.scene=scene;ca.reset(rig)
    if clip:
        from bl_ext.blender_org.animal_animator.core import composer
        composer.assign_action(rig,bpy.data.actions[ident+'.'+clip])
        scene.frame_set(frame)
    for obj in scene.objects:
        if obj.type=='MESH':obj.hide_render=obj!=mesh
    scene.render.engine='BLENDER_WORKBENCH'
    scene.render.resolution_x=600;scene.render.resolution_y=600;scene.render.resolution_percentage=100
    scene.display.shading.color_type='TEXTURE';scene.display.shading.light='STUDIO'
    scene.display.shading.show_shadows=False;scene.display.shading.show_cavity=True
    scene.display.shading.background_type='VIEWPORT';scene.display.shading.background_color=(.12,.12,.12)
    cd=bpy.data.cameras.new('anatomy');cam=bpy.data.objects.new('anatomy',cd);scene.collection.objects.link(cam)
    scene.camera=cam;cd.type='ORTHO';cd.ortho_scale=scale
    target=Vector(target);cam.location=target+Vector(offset);cam.rotation_euler=(target-cam.location).to_track_quat('-Z','Y').to_euler()
    scene.render.filepath=str(OUT/(ident+'-'+label+'.png'));bpy.ops.render.render(write_still=True)
    bpy.data.objects.remove(cam,do_unlink=True)

def audit(ident):
    r=bpy.data.objects[ident+'_rig']
    mid=r.data.bones['chest'].head_local
    render(ident,'front',(0,0,.5),(0,-3,0),1.35)
    render(ident,'top',(0,0,.5),(0,0,3),.85)
    if 'hand.R' in r.data.bones:
        p=r.data.bones['hand.R'].head_local
        render(ident,'hand-front',p,(0,-3,0),.28)
        render(ident,'hand-side',p,(-3,0,0),.28)
        render(ident,'hand-back',p,(0,3,0),.28)

if __name__=='__main__':
    for ident in ['bai_zhan_jiang_hun','xing_yun_tian_jiang','tong_ling_yuan_wang','xuan_tian_cheng_zhu','yun_hai_tian_jun']:
        audit(ident)
