"""Blender MCP choreography: distinct blade, spear, reverse-gripped staff and caster actions.

Authoring uses Animal Animator's pose/action tools, preserves source fingers/UV, and
keeps weapon socket geometry rigid. Shared time contract is boss_combat.json.
"""
import bpy,math,json
from mathutils import Vector,Quaternion
import completion_authoring as ca
import refine_posture_grip as rp
import author_boss_holding as hold
import audit_original_boss_hands as audit
from bl_ext.blender_org.animal_animator.core import composer
from boss_combat_catalogue import MODELS,catalogue
OUT=ca.ROOT/'asset_sources/boss-combat-v4-20261018'

def remove_action(rig,name):
    for tr in list(rig.animation_data.nla_tracks):
        if any(s.action and s.action.name==name for s in tr.strips):rig.animation_data.nla_tracks.remove(tr)
    a=bpy.data.actions.get(name)
    if a:bpy.data.actions.remove(a)

def envelope(t,a,b,c,d):
    return rp.smooth(a,b,t)*(1-rp.smooth(c,d,t))

def pose(rig,angles):
    for n,(x,y,z) in angles.items():
        if n not in rig.pose.bones:continue
        p=rig.pose.bones[n];basis=p.bone.matrix_local.to_3x3().inverted()
        p.rotation_quaternion=Quaternion(basis@Vector((0,0,1)),math.radians(z))@Quaternion(basis@Vector((0,1,0)),math.radians(y))@Quaternion(basis@Vector((1,0,0)),math.radians(x))

def base(ident):
    # Reverse staff carry lives beside the right hip, pointing back; centre remains at grip_fraction=.5.
    if ident=='tong_ling_yuan_wang':return {'arm_upper.R':(12,0,-12),'arm_lower.R':(-14,0,0)}
    if ident in rp.HOLDING:return {'arm_lower.R':(-12,0,0)}
    return {}

def weapon_axis(ident,rig,axis):
    if ident in rp.HOLDING:
        bpy.context.view_layer.update()
        hold.solve_axis(rig,Vector(rp.HOLDING[ident]['axis']).normalized(),Vector(axis).normalized())

def carry_axis(ident):
    return (-.12,.94,.32) if ident=='tong_ling_yuan_wang' else rp.HOLDING.get(ident,{}).get('axis',(0,-1,0))

def author(ident,boss):
    rig,mesh=rp.select_rig(ident)
    humanoid='leg_upper.R' in rig.pose.bones
    if not humanoid:return author_quadruped(ident,boss)
    # Remove the imported flying-back reaction. Feet/root never rotate or translate for hurt.
    remove_action(rig,ident+'.hurt')
    def hurt(f,n):
        t=f/n;p=envelope(t,0,.18,.30,1)
        angles=base(ident)|{'spine':(-2*p,0,0),'chest':(-5*p,0,2*p),'head':(3*p,0,0)}
        pose(rig,angles);weapon_axis(ident,rig,carry_axis(ident))
    ca.author_action(rig,mesh,'hurt',12,hurt,grounded=False)
    if ident=='tong_ling_yuan_wang':
        # Rebase existing accepted gait only at weapon arm; legs and tail keep their gait curves.
        for clip in ('idle','walk','run'):
            a=bpy.data.actions[ident+'.'+clip];composer.assign_action(rig,a)
            for f in range(int(a.frame_range[1])+1):
                bpy.context.scene.frame_set(f);pose(rig,base(ident));weapon_axis(ident,rig,carry_axis(ident))
                for name in ('arm_upper.R','arm_lower.R','hand.R'):
                    rig.pose.bones[name].keyframe_insert(data_path='rotation_quaternion',frame=f,group=name)
    reports=[]
    basic=dict(id='attack',motion={'bai_zhan_jiang_hun':'cut','xing_yun_tian_jiang':'thrust','tong_ling_yuan_wang':'sweep'}.get(ident,'weave'),windup=14,active=12,recovery=40)
    for skill in [basic]+catalogue()[boss]:
        clip=skill['id'];remove_action(rig,ident+'.'+clip)
        w,a,r=[skill[k] for k in ('windup','active','recovery')];total=w+a+r;frames=round(total*24/20)
        def animate(f,n):
            tick=f/n*total;charge=rp.smooth(0,w*.85,tick);release=rp.smooth(w,w+a*.50,tick)
            settle=1-rp.smooth(w+a,total,tick);pre=charge*(1-release)*settle;hit=release*settle
            motion=skill['motion'];angles=base(ident);axis=Vector(carry_axis(ident));target=axis.copy()
            angles.update({'spine':(0,0,-10*pre+12*hit),'chest':(3*pre+7*hit,0,-8*pre+12*hit),'head':(0,0,6*pre-8*hit),
                           'arm_upper.L':(-15*pre-12*hit,12*pre,0),'arm_lower.L':(-28*pre-20*hit,0,0)})
            if motion in ('sweep','cut','finisher') and ident in rp.HOLDING:
                angles['arm_upper.R']=(-36*pre-28*hit,-20*pre+25*hit,-30*pre+35*hit)
                angles['arm_lower.R']=(-46*pre-12*hit,0,0)
                target=Vector((-.65,-.2,.73)).lerp(Vector((.85,-.45,-.10)),release)
            elif motion in ('thrust','charge'):
                angles['arm_upper.R']=(-18*pre-65*hit,-12*hit,12*pre)
                angles['arm_lower.R']=(-65*pre-8*hit,0,0)
                angles['chest']=(5*pre+12*hit,0,-12*pre+6*hit)
                target=Vector((-.25,-.5,.82)).lerp(Vector((-.05,-.99,.08)),release)
            elif motion in ('slam','leap') and ident in rp.HOLDING:
                angles['arm_upper.R']=(-80*pre-35*hit,-15*pre,0)
                angles['arm_lower.R']=(-40*pre-18*hit,0,0)
                angles['chest']=(-8*pre+18*hit,0,0)
                target=Vector((-.2,.4,.89)).lerp(Vector((-.2,-.95,-.22)),release)
            elif motion=='guard':
                angles['arm_upper.R']=(-35*charge*settle,0,0);angles['arm_lower.R']=(-65*charge*settle,0,0)
                target=Vector((-.3,-.8,.5))
            else:
                # Caster actions use two-hand mudras, opening then projecting; never a sword retarget.
                for side,sign in [('R',-1),('L',1)]:
                    lift=60 if motion in ('summon','finisher') else 35
                    angles['arm_upper.'+side]=(-lift*pre-48*hit,sign*(22*pre+35*hit),sign*10*pre)
                    angles['arm_lower.'+side]=(-50*pre-12*hit,0,0)
                    angles['hand.'+side]=(10*pre-15*hit,0,sign*12*hit)
                    for digit in ('index','middle','ring','little','thumb'):
                        for j in range(1,4):
                            angles[f'{digit}_{j}.{side}']=(0,sign*(3 if digit in ('index','middle') else 9)*pre,0)
            # Feathered robe/tail follow-through, delayed against chest recovery.
            sway=math.sin(math.pi*min(1,tick/total))*settle
            for side,sign in [('R',-1),('L',1)]:angles['robe.'+side]=(4*sway,0,sign*2*sway)
            for i in range(4):angles['tail_'+str(i)]=(0,0,5*math.sin(tick*.11-i*.6)*sway)
            pose(rig,angles)
            if ident in rp.HOLDING:
                blend=charge*settle;desired=axis.lerp(target,blend).normalized()
                weapon_axis(ident,rig,desired)
        ca.author_action(rig,mesh,clip,frames,animate,grounded=False)
        reports.append(dict(clip=clip,frames=frames,windup=w,active=a,recovery=r,motion=skill['motion']))
    ca.reset(rig);mesh['combat_v4']=True
    # Contact sheet samples: actual evaluated skin, not just skeleton poses.
    for clip in ('idle','hurt','attack',catalogue()[boss][1]['id']):
        composer.assign_action(rig,bpy.data.actions[ident+'.'+clip]);bpy.context.scene.frame_set(4 if clip=='hurt' else 0 if clip=='idle' else 30)
        audit.capture(mesh,OUT/(ident+'-'+clip+'.png'),(0,0,.5),(1,-3,.5),1.55)
    ca.reset(rig)
    (OUT/(ident+'-choreography.json')).write_text(json.dumps(reports,indent=2),encoding='utf8')
    print('AUTHORED',ident,len(reports),'distinct combat clips')

def author_quadruped(ident,boss):
    rig,mesh=rp.select_rig(ident)
    basic=dict(id='attack',motion='stomp',windup=14,active=12,recovery=40)
    for skill in [dict(id='hurt',motion='hurt',windup=2,active=3,recovery=5),basic]+catalogue()[boss]:
        clip=skill['id'];remove_action(rig,ident+'.'+clip)
        w,a,r=[skill[k] for k in ('windup','active','recovery')];total=w+a+r
        def animate(f,n):
            tick=f/n*total;pre=rp.smooth(0,w*.8,tick)*(1-rp.smooth(w,w+a*.5,tick));hit=rp.smooth(w,w+a*.5,tick)*(1-rp.smooth(w+a,total,tick))
            motion=skill['motion'];angles={}
            if motion=='hurt':angles={'chest':(-4*(pre+hit),0,0),'head':(3*(pre+hit),0,0)}
            elif motion in ('stomp','finisher'):
                angles={'chest':(-8*pre+6*hit,0,0),'head':(-10*pre+12*hit,0,0)}
                for side in ('L','R'):
                    angles['front_upper.'+side]=(-22*pre,0,0);angles['front_lower.'+side]=(25*pre,0,0)
            else:angles={'neck':(-14*pre+9*hit,0,0),'head':(-12*pre+12*hit,0,0),'chest':(-3*pre,0,0)}
            for i in range(4):angles['tail_'+str(i)]=(0,0,(5+i)*math.sin(tick*.13-i*.5)*(pre+hit))
            pose(rig,angles)
        ca.author_action(rig,mesh,clip,round(total*24/20),animate,grounded=False)
    mesh['combat_v4']=True;ca.reset(rig);print('AUTHORED',ident,'quadruped skills')

if __name__=='__main__':
    for boss,ident in MODELS.items():author(ident,boss)
