"""Blender MCP: anatomical palm orientation and grounded unarmed choreography.

Operate on accepted v7 geometry/weights only; never auto-bind the rig again.
"""
import bpy, math, json, hashlib
from mathutils import Vector, Matrix, Quaternion
import completion_authoring as ca
import refine_posture_grip as rp
import refine_boss_motion_v5 as motion
import author_boss_combat_v4 as v4
import audit_original_boss_hands as audit
from bl_ext.blender_org.animal_animator.core import composer
from boss_combat_catalogue import catalogue

OUT=ca.ROOT/'asset_sources/boss-anatomy-v8-20261018'
OUT.mkdir(parents=True,exist_ok=True)
IDENTS=('xuan_tian_cheng_zhu','yun_hai_tian_jun')
SIDES=(('L',1),('R',-1))

def smooth(a,b,t):return rp.smooth(a,b,t)

def palm_rest(rig,side):
    h=rig.data.bones['hand.'+side]
    across=rig.data.bones['index_1.'+side].head_local-rig.data.bones['little_1.'+side].head_local
    n=(h.tail_local-h.head_local).cross(across).normalized()
    return -n if n.y>0 else n

def palm_world(rig,side):
    p=rig.pose.bones['hand.'+side]
    return ((p.matrix@p.bone.matrix_local.inverted()).to_3x3()@palm_rest(rig,side)).normalized()

def axis_roll(p,angle):
    axis=(p.tail-p.head).normalized()
    p.matrix=Matrix.Translation(p.head)@Quaternion(axis,angle).to_matrix().to_4x4()@Matrix.Translation(-p.head)@p.matrix
    bpy.context.view_layer.update()

def palm_angle(rig,side,bone,target):
    p=rig.pose.bones[bone+'.'+side];axis=(p.tail-p.head).normalized()
    normal=palm_world(rig,side);goal=Vector(target)
    normal-=axis*normal.dot(axis);goal-=axis*goal.dot(axis)
    if normal.length<1e-6 or goal.length<1e-6:return 0
    normal.normalize();goal.normalize()
    return math.atan2(axis.dot(normal.cross(goal)),normal.dot(goal))

def orient_palm(rig,side,target):
    # Share pronation over forearm and wrist; do not twist the hand 90 degrees
    # at a single seam, or roll the entire upper arm to solve a palm problem.
    lower=rig.pose.bones['arm_lower.'+side]
    angle=palm_angle(rig,side,'arm_lower',target)
    axis_roll(lower,angle*.75)
    axis_roll(rig.pose.bones['hand.'+side],palm_angle(rig,side,'hand',target))

def fingers(rig,side,closed=0):
    normal=palm_rest(rig,side)
    rest={'index':(10,18,8),'middle':(13,22,10),'ring':(17,26,11),'little':(20,30,13),'thumb':(8,10,6)}
    grip=(100,100,55)
    for digit in rest:
        for j in (1,2,3):
            p=rig.pose.bones[f'{digit}_{j}.{side}'];direction=(p.bone.tail_local-p.bone.head_local).normalized()
            axis=direction.cross(normal).normalized()
            degrees=rest[digit][j-1]*(1-closed)+(grip[j-1] if digit!='thumb' else (20,30,18)[j-1])*closed
            p.rotation_quaternion=Quaternion(p.bone.matrix_local.to_3x3().inverted()@axis,math.radians(degrees))
    if closed>0:
        bpy.context.view_layer.update()
        h=rig.pose.bones['hand.'+side];rest_h=h.bone
        transform=h.matrix@rest_h.matrix_local.inverted()
        mid=rig.data.bones['middle_1.'+side]
        forward=(mid.tail_local-mid.head_local).normalized()
        forward=(forward-normal*forward.dot(normal)).normalized()
        def close_chain(chain,goal):
            starts=[p.rotation_quaternion.copy() for p in chain]
            for _ in range(12):
                for p in reversed(chain):
                    a=chain[-1].tail-p.head;b=goal-p.head
                    if min(a.length,b.length)<1e-6:continue
                    q=a.rotation_difference(b)
                    p.matrix=Matrix.Translation(p.head)@q.to_matrix().to_4x4()@Matrix.Translation(-p.head)@p.matrix
                    bpy.context.view_layer.update()
            for p,start in zip(chain,starts):
                p.rotation_quaternion=start.slerp(p.rotation_quaternion,closed)
            bpy.context.view_layer.update()
        for digit in ('index','middle','ring','little'):
            chain=[rig.pose.bones[f'{digit}_{j}.{side}'] for j in (1,2,3)]
            direction=(chain[0].bone.head_local-rest_h.head_local).normalized()
            goal=transform@(chain[0].bone.head_local-direction*.028+normal*.012)
            close_chain(chain,goal)
        goal=rig.pose.bones['index_3.'+side].tail+transform.to_3x3()@normal*.007
        close_chain([rig.pose.bones[f'thumb_{j}.{side}'] for j in (1,2,3)],goal)

def rest_hands(rig):
    for side,sign in SIDES:
        orient_palm(rig,side,(-sign,.08,0));fingers(rig,side)

def backup(ident):
    rig,m=rp.select_rig(ident)
    p=OUT/(ident+'-before-v8.blend')
    if not p.exists():bpy.data.libraries.write(str(p),{bpy.context.scene},fake_user=True,compress=True)
    return {'vertices':len(m.data.vertices),'triangles':sum(len(p.vertices)-2 for p in m.data.polygons),'coordinates':hashlib.sha256(b''.join(__import__('struct').pack('<3f',*v.co) for v in m.data.vertices)).hexdigest(),'uv':hashlib.sha256(b''.join(__import__('struct').pack('<2f',*x.uv) for u in m.data.uv_layers for x in u.data)).hexdigest(),'textures':audit.texture_hashes(m),'joints':len(rig.data.bones)}

def natural_pass(ident):
    rig,m=rp.select_rig(ident);before=backup(ident)
    for clip in ('idle','walk','run','hurt','death'):
        a=bpy.data.actions[ident+'.'+clip];composer.assign_action(rig,a);end=int(a.frame_range[1]);samples=[]
        for f in range(end+1):
            bpy.context.scene.frame_set(f);bpy.context.view_layer.update()
            samples.append({p.name:p.matrix_basis.copy() for p in rig.pose.bones})
        v4.remove_action(rig,ident+'.'+clip)
        def frame(f,n):
            for p in rig.pose.bones:p.matrix_basis=samples[f][p.name]
            bpy.context.view_layer.update();rest_hands(rig)
        ca.author_action(rig,m,clip,end,frame,loop=clip in ('idle','walk','run'),grounded=False)
    assert backup(ident)==before
    (OUT/(ident+'-surface.json')).write_text(json.dumps(before,indent=2),encoding='utf8')
    m['combat_v8']=True
    print('NATURAL_HANDS',ident)

def solve_arm(rig,side,target,palm=(0,0,-1)):
    upper=rig.pose.bones['arm_upper.'+side];lower=rig.pose.bones['arm_lower.'+side]
    for p in (upper,lower,rig.pose.bones['hand.'+side]):p.rotation_quaternion=(1,0,0,0)
    bpy.context.view_layer.update()
    shoulder=upper.head.copy();delta=Vector(target)-shoulder
    l1=upper.bone.length;l2=lower.bone.length;dist=max(abs(l1-l2)+1e-5,min(delta.length,l1+l2-.008));axis=delta.normalized()
    along=(l1*l1-l2*l2+dist*dist)/(2*dist);height=math.sqrt(max(0,l1*l1-along*along))
    pole=Vector(((1 if side=='L' else -1)*.20,.10,-1));pole=(pole-axis*pole.dot(axis)).normalized()
    motion.rotate_to(upper,shoulder+axis*along+pole*height)
    motion.rotate_to(lower,shoulder+axis*dist)
    orient_palm(rig,side,palm)

def natural_arms(rig):
    for side,sign in SIDES:
        for n in ('arm_upper','arm_lower','hand'):rig.pose.bones[n+'.'+side].rotation_quaternion=(1,0,0,0)
        bpy.context.view_layer.update()
        p=rig.pose.bones['arm_upper.'+side];motion.rotate_to(p,p.head+Vector((sign*.20,0,-1)))
        p=rig.pose.bones['arm_lower.'+side];motion.rotate_to(p,p.head+Vector((sign*.08,-.08,-1)))
    rest_hands(rig)

def pulse(t,start,length):
    # Brief acceleration, visible contact, then an eased pullback. A separate
    # preparation value starts the hip/shoulder before elbow extension.
    x=t-start
    hit=smooth(0,length*.22,x)*(1-smooth(length*.42,length,x))
    pre=smooth(-5,-1,x)*(1-smooth(-1,length*.12,x))
    return pre,hit

def city_attacks():
    ident=IDENTS[0];rig,m=rp.select_rig(ident);feet={s:rig.data.bones['leg_lower.'+s].tail_local.copy() for s,_ in SIDES}
    basic=dict(id='attack',windup=14,active=12,recovery=40)
    skills=[basic]+catalogue()['xuantians_remnant']+[dict(id='counter_hit',windup=0,active=8,recovery=32)]
    for skill in skills:
        clip=skill['id'];w,a,r=[skill[k] for k in ('windup','active','recovery')];total=w+a+r
        v4.remove_action(rig,ident+'.'+clip)
        def frame(f,n):
            t=f/n*total;ready=(1 if w==0 else smooth(0,6,t))*(1-smooth(total-14,total-2,t))
            pre,hit=pulse(t,w,a if clip!='sword_array' else 9)
            side='R';twist=-8*pre+13*hit
            if clip=='sword_array':
                index=max(0,min(2,int((t-w)/10)));side='L' if index in (0,2) else 'R';pre,hit=pulse(t,w+index*10,9)
                twist=(1 if side=='R' else -1)*(-7*pre+13*hit)
            slam=clip=='wall_fall';guard=clip=='city_seal';illusion=clip=='soul_phantoms';finisher=clip=='final_city'
            if guard:pre=hit=0;twist=0
            if illusion:pre=hit=0;twist=4*math.sin(t*.08)*ready
            if finisher:pre=smooth(2,w-2,t)*(1-smooth(w-1,w+3,t));twist=-13*pre+20*hit
            if slam:pre=smooth(3,w-3,t)*(1-smooth(w-1,w+4,t));twist=0
            ca.translate(rig,'root',((.009*pre-.013*hit)*ready,(.008*pre-.018*hit)*ready,-(.008+.010*pre+.009*hit)*ready))
            v4.pose(rig,{'hips':(0,0,twist*.40*ready),'spine':((-2*pre+4*hit)*ready,0,twist*.35*ready),'chest':((-3*pre+7*hit)*ready,0,twist*.55*ready),'head':((2*pre-3*hit)*ready,0,-twist*.60*ready)})
            if slam:v4.pose(rig,{'hips':(5*pre+7*hit,0,0),'spine':(-4*pre+8*hit,0,0),'chest':(-7*pre+13*hit,0,0)})
            bpy.context.view_layer.update();natural_arms(rig)
            neutral={p.name:p.rotation_quaternion.copy() for p in rig.pose.bones if p.name.startswith(('arm_','hand.'))}
            chest=rig.pose.bones['chest'];transform=chest.matrix@chest.bone.matrix_local.inverted()
            for ss,sign in SIDES:
                target=Vector((sign*.112,-.140,.670));normal=Vector((-sign*.45,-.12,-.7))
                if ss==side:
                    target+=Vector((-sign*.012*hit,.045*pre-(.093 if finisher else .079)*hit,.018*pre+.045*hit));normal=Vector((0,0,-1))
                if slam:target=Vector((sign*.108,-.125-.057*hit,.63+.13*pre-.13*hit));normal=Vector((-sign,0,0))
                if guard:target=Vector((sign*.082,-.132,.655));normal=Vector((-sign*.7,-.2,-.3))
                if illusion:target=Vector((sign*.13,-.12,.625));normal=Vector((-sign*.7,0,-.4))
                solve_arm(rig,ss,transform@target,transform.to_3x3()@normal)
                for n in ('arm_upper','arm_lower','hand'):
                    p=rig.pose.bones[n+'.'+ss];p.rotation_quaternion=neutral[p.name].slerp(p.rotation_quaternion,ready)
                fingers(rig,ss,ready)
            # Small stance load with feet planted; no full-body violent rocking.
            for ss,sign in SIDES:
                motion.plant(rig,ss,feet[ss]+Vector((sign*.008*ready,-sign*.018*ready,0)))
                v4.pose(rig,{'robe.'+ss:(2*math.sin(t*.12)*ready,0,sign*ready),'robe_front.'+ss:(1.5*math.sin(t*.12-.3)*ready,0,0)})
        ca.author_action(rig,m,clip,round(total*24/20),frame,grounded=False)
    ca.reset(rig);m['combat_v8']=True
    print('CITY_ATTACKS',len(skills))

def preview(ident,clip,frame,label,side=False,hand=False):
    rig,m=rp.select_rig(ident);composer.assign_action(rig,bpy.data.actions[ident+'.'+clip]);bpy.context.scene.frame_set(frame);bpy.context.view_layer.update()
    if hand:
        target=rig.pose.bones['hand.L'].head.copy();target.z-=.03;offset=(1,-3,.1);scale=.26
    else:target=(0,0,.48);offset=(3,-.3,0) if side else (0,-3,0);scale=1.10
    audit.capture(m,OUT/(ident+'-'+label+'.png'),target,offset,scale);ca.reset(rig)


def smooth_combat():
    ident=IDENTS[0];r,m=rp.select_rig(ident)
    clips=['attack','sword_array','wall_fall','soul_phantoms','city_seal','final_city','counter_hit']
    for clip in clips:
        action=bpy.data.actions[ident+'.'+clip];composer.assign_action(r,action);end=int(action.frame_range[1]);samples=[]
        for f in range(end+1):
            bpy.context.scene.frame_set(f);samples.append({p.name:(p.location.copy(),p.rotation_quaternion.copy(),p.scale.copy()) for p in r.pose.bones})
        for _ in range(2):
            filtered=[]
            for f in range(end+1):
                row=dict(samples[f])
                for name,(loc,q,scale) in samples[f].items():
                    if not name.startswith(('arm_','hand.','index_','middle_','ring_','little_','thumb_')):continue
                    weights=[(max(0,min(end,f+j)),w) for j,w in [(-2,1),(-1,2),(0,3),(1,2),(2,1)]]
                    avg=None;total=0
                    for ff,w in weights:
                        other=samples[ff][name][1]
                        avg=other.copy() if avg is None else avg.slerp(other,w/(total+w));total+=w
                    row[name]=(loc,avg,scale)
                filtered.append(row)
            samples=filtered
        v4.remove_action(r,ident+'.'+clip)
        def frame(f,n):
            for p in r.pose.bones:p.location,p.rotation_quaternion,p.scale=samples[f][p.name]
        ca.author_action(r,m,clip,end,frame,grounded=False)
    ca.reset(r);print('TEMPORAL_FILTER_OK')
