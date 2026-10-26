"""Blender MCP: fitted humanoid rigs, UAL2 combat retargeting, rigid weapon binding."""
import sys,math,json
from pathlib import Path
sys.path.insert(0,str(Path(r'tools')))
from completion_authoring import *

SPECS=[('百战将魂','bai_zhan_jiang_hun',3.0,.12,.18,'百战将魂的战刀',.65),
       ('星陨天将_角色_','xing_yun_tian_jiang',3.5,.14,.21,'星陨天将_星陨长枪',1.25),
       ('通灵猿王','tong_ling_yuan_wang',3.2,.18,.28,'通灵猿王_镇岳铁棒',1.15),
       ('玄天城主残魂','xuan_tian_cheng_zhu',3.4,.16,.26,None,0),
       ('云海天君遗念','yun_hai_tian_jun',3.2,.13,.20,None,0)]
MAPPING={'pelvis':'hips','spine_01':'spine','spine_03':'chest','neck_01':'neck','Head':'head'}
for side in ('l','r'):
    for src,dst in [('upperarm','arm_upper'),('lowerarm','arm_lower'),('hand','hand'),('thigh','leg_upper'),('calf','leg_lower'),('foot','foot')]:MAPPING[src+'_'+side]=dst+'.'+side.upper()

source_rig=bpy.data.objects['CompletionUALSource']
for name,ident,height,shoulder,wrist,weapon,length in SPECS:
    scene,mesh,rig,bone,source,ground=setup(name,ident)
    bone('hips',(0,0,-.055),(0,0,.035))
    bone('spine',(0,0,.035),(0,0,.14),'hips')
    bone('chest',(0,0,.14),(0,0,.23),'spine')
    bone('neck',(0,0,.23),(0,-.012,.30),'chest')
    bone('head',(0,-.012,.30),(0,-.012,.42),'neck')
    for side,sgn in [('L',1),('R',-1)]:
        bone('arm_upper.'+side,(sgn*shoulder,0,.225),(sgn*(shoulder+wrist)/2,-.005,.065),'chest')
        bone('arm_lower.'+side,(sgn*(shoulder+wrist)/2,-.005,.065),(sgn*wrist,-.025,-.065),'arm_upper.'+side)
        bone('hand.'+side,(sgn*wrist,-.025,-.065),(sgn*(wrist+.012),-.035,-.12),'arm_lower.'+side)
        bone('leg_upper.'+side,(sgn*.075,0,-.045),(sgn*.085,-.005,-.25),'hips')
        bone('leg_lower.'+side,(sgn*.085,-.005,-.25),(sgn*.09,0,-.425),'leg_upper.'+side)
        bone('foot.'+side,(sgn*.09,0,-.425),(sgn*.09,-.105,-.46),'leg_lower.'+side)
        bone('robe.'+side,(sgn*.07,.045,-.04),(sgn*.12,.055,-.25),'hips')
        bone('robe_tip.'+side,(sgn*.12,.055,-.25),(sgn*.15,.07,-.43),'robe.'+side)
    if ident=='tong_ling_yuan_wang':
        tail=[(0,.08,-.05),(0,.17,-.12),(0,.22,-.19),(0,.23,-.08),(0,.23,.02)]
        for i in range(4):bone('tail_'+str(i),tail[i],tail[i+1],'hips' if i==0 else 'tail_'+str(i-1))
    # Match anatomical rolls to the installed motion library while fitting the A-pose mesh.
    for src,dst in MAPPING.items():
        b=rig.data.edit_bones[dst];b.align_roll(source_rig.data.bones[src].matrix_local.to_3x3()@Vector((0,0,1)))
    repaired=bind(mesh,rig)
    # Helmet, hair and shoulder armour follow their anatomical carrier rigidly.
    for v in mesh.data.vertices:
        p=v.co+Vector((0,0,ground));group=None
        if p.z>.29:group='head'
        elif p.y>.045 and p.z<-.08 and abs(p.x)>.105 and ident!='tong_ling_yuan_wang':group=('robe_tip.' if p.z<-.27 else 'robe.')+('L' if p.x>0 else 'R')
        if group:
            for g in mesh.vertex_groups:g.remove([v.index])
            mesh.vertex_groups[group].add([v.index],1,'REPLACE')
    # Join only this source's companion weapon, preserving its own atlas and rigid hand weight.
    mesh['body_height']=max(v.co.z for v in mesh.data.vertices)-min(v.co.z for v in mesh.data.vertices)
    if weapon:
        before=set(scene.objects)
        bpy.ops.import_scene.gltf(filepath=str(ROOT/'asset_sources/creature-batch-20261016/sources'/(weapon+'.glb')))
        item=next(o for o in scene.objects if o not in before and o.type=='MESH')
        item.data.transform(item.matrix_world);item.matrix_world.identity()
        mins=Vector([min(v.co[i] for v in item.data.vertices) for i in range(3)]);maxs=Vector([max(v.co[i] for v in item.data.vertices) for i in range(3)])
        dims=maxs-mins;axis=max(range(3),key=lambda i:dims[i]);direction=Vector([int(i==axis) for i in range(3)])
        align=direction.rotation_difference(Vector((0,0,1))).to_matrix();centre=(mins+maxs)/2
        hand=rig.data.bones['hand.R'].head_local
        # Long weapons are gripped below the centre; the blade stays above the hand at rest.
        grip=-length*(.34 if '战刀' in weapon else .16)
        for v in item.data.vertices:
            local=align@(v.co-centre)*(length/dims[axis]);local.z-=grip
            v.co=hand+local+Vector((-.025,-.025,0))
        item.vertex_groups.clear();g=item.vertex_groups.new(name='hand.R');g.add(list(range(len(item.data.vertices))),1,'REPLACE')
        bpy.ops.object.select_all(action='DESELECT');item.select_set(True);mesh.select_set(True);bpy.context.view_layer.objects.active=mesh;bpy.ops.object.join()
    actions={}
    def idle(f,n):
        rotate(rig,'chest',(1,0,0),1.4*math.sin(math.tau*f/n));rotate(rig,'head',(0,0,1),3*math.sin(math.tau*f/n))
        for side,sgn in [('L',1),('R',-1)]:rotate(rig,'robe.'+side,(1,0,0),2*math.sin(math.tau*f/n+sgn*.4))
        if ident=='tong_ling_yuan_wang':
            for i in range(4):rotate(rig,'tail_'+str(i),(0,0,1),(5+2*i)*math.sin(math.tau*f/n-i*.6))
    actions['idle']=author_action(rig,mesh,'idle',96,idle,True)
    for clip,frames,amplitude in [('walk',36,22),('run',24,30)]:
        def gait(f,n):
            phase=math.tau*f/n
            for side,sgn in [('L',1),('R',-1)]:
                p=phase+(0 if sgn>0 else math.pi)
                rotate(rig,'leg_upper.'+side,(1,0,0),amplitude*math.sin(p))
                rotate(rig,'leg_lower.'+side,(1,0,0),-35*max(0,math.sin(p)))
                rotate(rig,'arm_upper.'+side,(1,0,0),-12*math.sin(p))
                rotate(rig,'robe.'+side,(1,0,0),amplitude*.45*math.sin(p-.3))
                rotate(rig,'robe_tip.'+side,(1,0,0),amplitude*.25*math.sin(p-.7))
            if ident=='tong_ling_yuan_wang':
                for i in range(4):rotate(rig,'tail_'+str(i),(0,0,1),(5+2*i)*math.sin(phase-i*.6))
        actions[clip]=author_action(rig,mesh,clip,frames,gait,True)
    # Retarget the CC0 Universal Animation Library combat pose, retaining destination proportions.
    scene.collection.objects.link(source_rig);source_rig.hide_render=True
    for clip,source_action,frames in [('attack','Sword_Regular_A',26),('combo','Sword_Heavy_Combo',90),('hurt','Hit_Knockback',20)]:
        action=bpy.data.actions[source_action];composer.assign_action(source_rig,action)
        a,b=action.frame_range;samples=[]
        for f in range(frames+1):
            at=a+(b-a)*f/frames;scene.frame_set(math.floor(at),subframe=at%1)
            samples.append({dst:source_rig.pose.bones[src].matrix.to_quaternion().copy() for src,dst in MAPPING.items()})
        source_rig.animation_data.action=None
        def retarget(f,n):
            desired=samples[f];world={'root':rig.data.bones['root'].matrix_local.to_quaternion()}
            for p in rig.pose.bones:
                if p.name=='root':continue
                parent=p.parent.name;rest=p.bone.matrix_local.to_quaternion();parent_rest=p.parent.bone.matrix_local.to_quaternion()
                parent_pose=world.get(parent,parent_rest)
                base=parent_pose@parent_rest.inverted()@rest
                p.rotation_quaternion=base.inverted()@desired[p.name] if p.name in desired else Quaternion()
                world[p.name]=base@p.rotation_quaternion
        actions[clip]=author_action(rig,mesh,clip,frames,retarget)
    scene.collection.objects.unlink(source_rig)
    def cast(f,n):
        t=math.sin(math.pi*f/n)
        for side,sgn in [('L',1),('R',-1)]:
            rotate(rig,'arm_upper.'+side,(0,1,0),-sgn*65*t)
            rotate(rig,'arm_lower.'+side,(1,0,0),-30*t)
        rotate(rig,'head',(1,0,0),-8*t)
    actions['cast']=author_action(rig,mesh,'cast',60,cast)
    def death(f,n):
        t=min(1,f/35);rotate(rig,'root',(1,0,0),85*t)
        for side in ('L','R'):rotate(rig,'leg_lower.'+side,(1,0,0),-25*t)
    actions['death']=author_action(rig,mesh,'death',48,death)
    finish(name,ident,mesh,rig,source,actions,height,'Blender MCP, ARP anatomy reference + CC0 Universal Animation Library 2 retargeted combat')
