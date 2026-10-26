"""Blender MCP: source-specific multi-legged rigs; alternating stance cycles."""
import sys,math
from pathlib import Path
sys.path.insert(0,str(Path(r'tools')))
from completion_authoring import *

for name,ident,yaw,height,kind in [('玄壳蟹','xuan_ke_xie',0,1.1,'crab'),('金背甲虫','jin_bei_jia_chong',0,1.0,'beetle'),('碧叶螳','bi_ye_tang',38,1.65,'mantis'),('蘑伞蟾','mo_san_chan',5,1.2,'toad')]:
    scene,mesh,rig,bone,source,ground=setup(name,ident,yaw)
    bone('body',(0,.1,-.05),(0,-.1,-.05))
    head=(0,-.24,-.08) if kind in ('crab','beetle') else (0,-.30,.15) if kind=='mantis' else (0,-.29,-.03)
    bone('neck',(0,-.12,-.04),head,'body');bone('head',head,(0,-.40,head[2]),'neck')
    legs=[]
    if kind=='crab':
        ys=[-.19,-.05,.10,.24]
        for side,sgn in [('L',1),('R',-1)]:
            for i,y in enumerate(ys):
                n=f'leg_{i}.{side}';legs.append((n,i,sgn))
                bone(n,(sgn*.15,y,-.06),(sgn*.31,y+(i-1.5)*.05,-.10),'body')
                bone(n+'_foot',(sgn*.31,y+(i-1.5)*.05,-.10),(sgn*(.40 if i in (1,2) else .31),y+(i-1.5)*.11,-.25),n)
            bone('claw_upper.'+side,(sgn*.13,-.16,.02),(sgn*.30,-.22,.09),'body')
            bone('claw.'+side,(sgn*.30,-.22,.09),(sgn*.38,-.29,.21),'claw_upper.'+side)
            bone('claw_tip.'+side,(sgn*.38,-.29,.21),(sgn*.28,-.35,.23),'claw.'+side)
    elif kind=='beetle':
        for side,sgn in [('L',1),('R',-1)]:
            for i,y in enumerate([-.28,-.07,.15]):
                n=f'leg_{i}.{side}';legs.append((n,i,sgn));end_y=y+(i-1)*.10
                bone(n,(sgn*.10,y,-.10),(sgn*.23,end_y,-.12),'body')
                bone(n+'_foot',(sgn*.23,end_y,-.12),(sgn*(.17 if i==0 else .33),end_y-.025,-.215),n)
            bone('shell.'+side,(sgn*.025,.05,.12),(sgn*.18,.21,.10),'body')
    elif kind=='mantis':
        bone('abdomen',(0,.08,-.04),(0,.36,.05),'body')
        for side,sgn in [('L',1),('R',-1)]:
            for i,y in enumerate([-.02,.20]):
                n=f'leg_{i}.{side}';legs.append((n,i,sgn))
                bone(n,(sgn*.06,y,-.04),(sgn*.21,y+.06,-.02),'body')
                bone(n+'_foot',(sgn*.21,y+.06,-.02),(sgn*.27,y+.12,-.35),n)
            bone('blade_upper.'+side,(sgn*.06,-.20,.05),(sgn*.20,-.31,.13),'neck')
            bone('blade.'+side,(sgn*.20,-.31,.13),(sgn*.17,-.43,-.10),'blade_upper.'+side)
    else:
        # Mushroom cap follows the torso rigidly; frog hind legs crouch and extend together.
        bone('cap',(0,0,.10),(0,0,.30),'body')
        for side,sgn in [('L',1),('R',-1)]:
            for i,y in enumerate([-.23,.23]):
                n=f'leg_{i}.{side}';legs.append((n,i,sgn))
                bone(n,(sgn*.14,y,-.07),(sgn*.24,y+(.08 if i else -.02),-.19),'body')
                bone(n+'_foot',(sgn*.24,y+(.08 if i else -.02),-.19),(sgn*.25,y-.045,-.315),n)
    bind(mesh,rig)
    if yaw and kind=='mantis':head_correct(mesh,rig,yaw)
    # Shell/cap plates are not rubber and must not inherit nearby limb weights.
    for v in mesh.data.vertices:
        p=v.co+Vector((0,0,ground));group=None
        if kind=='toad' and p.z>.04:group='cap'
        elif kind=='beetle' and p.y>-.13 and p.z>-.015:group='shell.'+('L' if p.x>0 else 'R')
        elif kind=='crab' and abs(p.x)<.22 and -.22<p.y<.27 and p.z>-.05:group='body'
        if group:
            for g in mesh.vertex_groups:g.remove([v.index])
            mesh.vertex_groups[group].add([v.index],1,'REPLACE')
    actions={}
    def idle(f,n):
        rotate(rig,'head',(0,0,1),3*math.sin(math.tau*f/n))
        if kind=='crab':
            for side,sgn in [('L',1),('R',-1)]:rotate(rig,'claw_tip.'+side,(0,0,1),sgn*6*math.sin(math.tau*f/n))
    actions['idle']=author_action(rig,mesh,'idle',96,idle,True)
    for clip,frames,angle in [('walk',36,22),('run',24,30)]:
        def step(f,n):
            for leg,i,sgn in legs:
                phase=math.tau*f/n+math.pi*((i+(sgn<0))%2)
                rotate(rig,leg,(0,0,1),angle*math.sin(phase))
                rotate(rig,leg+'_foot',(0,1,0),sgn*30*max(0,math.sin(phase)))
            if kind=='toad':
                phase=math.tau*f/n
                for leg,i,sgn in legs:
                    rotate(rig,leg,(1,0,0),(30 if i else -18)*math.cos(phase))
                    rotate(rig,leg+'_foot',(1,0,0),-35*max(0,math.cos(phase)))
                translate(rig,'root',(0,0,.07*max(0,math.sin(phase))))
        actions[clip]=author_action(rig,mesh,clip,frames,step,True)
    def attack(f,n):
        t=math.sin(math.pi*f/n)
        if kind=='crab':
            for side,sgn in [('L',1),('R',-1)]:
                rotate(rig,'claw_upper.'+side,(0,0,1),-sgn*40*t)
                rotate(rig,'claw_tip.'+side,(0,0,1),sgn*30*t)
        elif kind=='mantis':
            for side,sgn in [('L',1),('R',-1)]:
                rotate(rig,'blade_upper.'+side,(1,0,0),-50*t)
                rotate(rig,'blade.'+side,(0,0,1),sgn*35*t)
        elif kind=='beetle':
            rotate(rig,'head',(1,0,0),-22*t)
            for side,sgn in [('L',1),('R',-1)]:rotate(rig,'shell.'+side,(0,1,0),sgn*12*t)
        else:
            rotate(rig,'neck',(1,0,0),-15*t)
            rig.pose.bones['body'].scale=(1+.025*t,1+.025*t,1+.025*t)
    actions['attack']=author_action(rig,mesh,'attack',30,attack)
    if kind in ('crab','mantis'):actions['sweep']=author_action(rig,mesh,'sweep',54,attack)
    if kind=='toad':actions['breath']=author_action(rig,mesh,'breath',53,attack)
    if kind=='beetle':actions['charge']=author_action(rig,mesh,'charge',48,attack)
    actions['hurt']=author_action(rig,mesh,'hurt',16,lambda f,n:rotate(rig,'body',(0,0,1),-10*math.sin(math.pi*f/n)))
    def death(f,n):
        t=min(1,f/28);rotate(rig,'root',(0,1,0),85*t)
        for leg,i,sgn in legs:rotate(rig,leg+'_foot',(0,1,0),sgn*45*t)
    actions['death']=author_action(rig,mesh,'death',40,death)
    finish(name,ident,mesh,rig,source,actions,height,'Blender MCP, anatomical multi-leg phase gait and rigid shell/cap groups')
