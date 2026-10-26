"""Run through Blender MCP; measured source centre-lines + installed Slither waves."""
import sys,math,json
from pathlib import Path
sys.path.insert(0,str(Path(r'tools')))
from completion_authoring import *

SPECS=[('沧鳞蟒','cang_lin_mang',60,2.0,[(0,-.39,.09),(0,-.31,.015),(-.02,-.24,-.20),(-.03,-.19,-.32),(-.04,-.10,-.23),(-.04,0,-.12),(-.04,.10,-.14),(-.03,.21,-.23),(-.02,.31,-.32),(-.02,.40,-.29),(-.02,.47,-.15),(-.02,.51,.02)]),
       ('苍枝蟒','cang_zhi_mang',58,2.1,[(-.03,-.34,.17),(-.03,-.32,-.05),(-.04,-.31,-.26),(-.04,-.23,-.35),(-.04,-.17,-.23),(-.035,-.09,-.04),(-.04,.02,-.06),(-.04,.15,-.22),(-.02,.27,-.36),(-.015,.38,-.36),(-.04,.47,-.15),(-.05,.54,.06)])]

for name,ident,yaw,height,points in SPECS:
    scene,mesh,rig,bone,source,ground=setup(name,ident,yaw)
    bone('head',points[0],(points[0][0]+.07,points[0][1]-.06,points[0][2]))
    for i in range(len(points)-1):bone('spine_'+str(i),points[i],points[i+1],'root' if i==0 else 'spine_'+str(i-1))
    bind(mesh,rig)
    for v in mesh.data.vertices:
        p=v.co+Vector((0,0,ground))
        if p.y < -.22 and p.z > .015:
            for g in mesh.vertex_groups:g.remove([v.index])
            mesh.vertex_groups['head'].add([v.index],1,'REPLACE')
    head_correct(mesh,rig,yaw)
    actions={};chain=[p for p in rig.pose.bones if p.name.startswith('spine_')]
    for clip,frames,amplitude in [('idle',96,4),('walk',40,12),('run',24,17),('swim',36,14)]:
        reset(rig);bpy.context.view_layer.objects.active=rig
        bpy.ops.object.mode_set(mode='POSE')
        for p in rig.pose.bones:p.select=p in chain;p.rotation_mode='XYZ'
        settings=scene.slither_animation_settings;settings.amplitude_degrees=amplitude;settings.phase_degrees=35;settings.cycle_frames=frames;settings.axis='Z'
        bpy.ops.slither.create_wave();generated=rig.animation_data.action
        # Sample the plugin's phase propagation, then express it in world-horizontal axes.
        samples=[]
        for f in range(frames+1):
            scene.frame_set(f);samples.append({p.name:float(p.rotation_euler.z) for p in chain})
        bpy.ops.object.mode_set(mode='OBJECT');rig.animation_data.action=None
        def wave(f,end):
            for p in chain:rotate(rig,p.name,(0,0,1),math.degrees(samples[f][p.name]))
            rotate(rig,'head',(0,0,1),2*math.sin(math.tau*f/end))
        actions[clip]=author_action(rig,mesh,clip,frames,wave,True)
        bpy.data.actions.remove(generated)
    def strike(f,end):
        t=f/end;amount=math.sin(math.pi*t)
        rotate(rig,'head',(0,0,1),-35*amount)
        for i in range(4):rotate(rig,'spine_'+str(i),(0,0,1),12*amount)
    actions['attack']=author_action(rig,mesh,'attack',30,strike)
    actions['sweep']=author_action(rig,mesh,'sweep',54,strike)
    actions['hurt']=author_action(rig,mesh,'hurt',16,lambda f,n:rotate(rig,'head',(1,0,0),-12*math.sin(math.pi*f/n)))
    def death(f,n):
        t=min(1,f/30);rotate(rig,'root',(0,1,0),80*t)
        translate(rig,'root',(.10*t,0,-.08*t))
    actions['death']=author_action(rig,mesh,'death',48,death)
    finish(name,ident,mesh,rig,source,actions,height,'Blender MCP + Slither phase propagation, measured S-curve spine')

# Ray: two segmented pectoral fins and a five-link whip tail, no terrestrial legs.
name='潮纹鳐';ident='chao_wen_yao'
scene,mesh,rig,bone,source,ground=setup(name,ident)
bone('body',(0,-.17,0),(0,-.28,0));bone('head',(0,-.28,0),(0,-.44,0),'body')
for side,sgn in [('L',1),('R',-1)]:
    bone('fin_upper.'+side,(sgn*.06,-.23,0),(sgn*.20,-.22,0),'body')
    bone('fin_tip.'+side,(sgn*.20,-.22,0),(sgn*.36,-.20,0),'fin_upper.'+side)
tail=[(0,-.05,0),(0,.07,.005),(.11,.19,.01),(.09,.28,.02),(-.02,.36,.025),(-.02,.46,.035)]
for i in range(5):bone('tail_'+str(i),tail[i],tail[i+1],'body' if i==0 else 'tail_'+str(i-1))
bind(mesh,rig);actions={}
for clip,frames,amplitude in [('idle',90,10),('swim',36,25),('walk',36,25),('run',24,32)]:
    def swim(f,n):
        for side,sgn in [('L',1),('R',-1)]:
            rotate(rig,'fin_upper.'+side,(0,1,0),sgn*amplitude*math.sin(math.tau*f/n))
            rotate(rig,'fin_tip.'+side,(0,1,0),sgn*amplitude*.6*math.sin(math.tau*f/n-.6))
        for i in range(5):rotate(rig,'tail_'+str(i),(0,0,1),(5+2*i)*math.sin(math.tau*f/n-i*.65))
    actions[clip]=author_action(rig,mesh,clip,frames,swim,True,False)
actions['hurt']=author_action(rig,mesh,'hurt',16,lambda f,n:rotate(rig,'body',(0,1,0),12*math.sin(math.pi*f/n)),False,False)
actions['death']=author_action(rig,mesh,'death',36,lambda f,n:rotate(rig,'root',(0,1,0),100*min(1,f/24)),False,False)
finish(name,ident,mesh,rig,source,actions,.30,'Blender MCP, segmented fin undulation and tail phase wave')
