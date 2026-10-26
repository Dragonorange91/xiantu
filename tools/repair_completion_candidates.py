"""Run through Blender MCP against the existing completion scenes; preserve older accepted rigs."""
import sys, importlib
from pathlib import Path
sys.path.insert(0,str(Path(r'tools')))
import completion_authoring as ca
importlib.reload(ca)
from completion_authoring import *

def remove_clip(rig,name):
    old=bpy.data.actions.get(rig.name.removesuffix('_rig')+'.'+name)
    if old:
        for track in list(rig.animation_data.nla_tracks):
            if any(s.action==old for s in track.strips):rig.animation_data.nla_tracks.remove(track)
        bpy.data.actions.remove(old)

specs=[('铜喙鹫','tong_hui_jiu',1.55),('火喙隼','huo_hui_sun',1.4),('紫电蝠','zi_dian_fu',1.05),
       ('百战将魂','bai_zhan_jiang_hun',3),('星陨天将_角色_','xing_yun_tian_jiang',3.5),
       ('通灵猿王','tong_ling_yuan_wang',3.2),('玄天城主残魂','xuan_tian_cheng_zhu',3.4),('云海天君遗念','yun_hai_tian_jun',3.2)]
for name,ident,height in specs:
    rig=bpy.data.objects[ident+'_rig'];mesh=bpy.data.objects[ident+'_mesh']
    bpy.context.window.scene=bpy.data.scenes['CompletionRig_'+ident]
    # Remove only the old candidate LOD before regeneration.
    for obj in list(bpy.context.scene.objects):
        if obj.name.startswith(ident+'_lod'):bpy.data.objects.remove(obj,do_unlink=True)
    if ident in ('tong_hui_jiu','huo_hui_sun','zi_dian_fu'):
        remove_clip(rig,'death')
        def collapse(f,n):
            t=min(1,f/24)
            rotate(rig,'root',(0,1,0),84*t)
            for side,sgn in [('L',1),('R',-1)]:rotate(rig,'wing_upper.'+side,(0,1,0),sgn*15*t)
        author_action(rig,mesh,'death',36,collapse,grounded=True)
    else:
        body_indices={i for p in mesh.data.polygons if p.material_index==0 for i in p.vertices}
        mesh['body_height']=max(mesh.data.vertices[i].co.z for i in body_indices)-min(mesh.data.vertices[i].co.z for i in body_indices)
        if ident in ('xing_yun_tian_jiang','tong_ling_yuan_wang','xuan_tian_cheng_zhu','yun_hai_tian_jun'):
            remove_clip(rig,'attack')
            def strike(f,n):
                t=math.sin(math.pi*f/n)
                if ident in ('xuan_tian_cheng_zhu','yun_hai_tian_jun'):
                    for side,sgn in [('L',1),('R',-1)]:
                        rotate(rig,'arm_upper.'+side,(1,0,0),-55*t)
                        rotate(rig,'arm_lower.'+side,(1,0,0),-25*t)
                    rotate(rig,'chest',(1,0,0),-5*t)
                elif ident=='xing_yun_tian_jiang':
                    rotate(rig,'arm_upper.R',(1,0,0),-55*t)
                    rotate(rig,'arm_lower.R',(1,0,0),-35*t)
                    rotate(rig,'hand.R',(1,0,0),-15*t)
                    rotate(rig,'chest',(0,0,1),-12*t)
                else:
                    rotate(rig,'arm_upper.R',(1,0,0),-100*t)
                    rotate(rig,'arm_lower.R',(1,0,0),-25*t)
                    rotate(rig,'chest',(1,0,0),10*t)
            author_action(rig,mesh,'attack',32,strike)
    actions={a.name.split('.',1)[1]:a for a in bpy.data.actions if a.name.startswith(ident+'.')}
    source=ROOT/'asset_sources/creature-batch-20261016/sources'/(name+'.glb')
    finish(name,ident,mesh,rig,source,actions,height,'Blender MCP, anatomy-specific rig; corrected death and body-only scale')
print('REPAIRED 8 CANDIDATES')
