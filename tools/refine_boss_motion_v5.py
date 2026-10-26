"""Blender MCP authoring. Full-body planted-foot choreography; preserve original skin/UV."""
import bpy, math, json
from mathutils import Vector, Matrix, Quaternion
import completion_authoring as ca
import refine_posture_grip as rp
import author_boss_combat_v4 as v4
import audit_original_boss_hands as audit
from boss_combat_catalogue import MODELS,catalogue
from bl_ext.blender_org.animal_animator.core import composer
OUT=ca.ROOT/'asset_sources/boss-art-v5-20261018'

def centre():
 for ident,dx in [('xing_yun_tian_jiang',.020),('tong_ling_yuan_wang',-.018)]:
  rig,m=rp.select_rig(ident);ca.reset(rig)
  assert not m.get('centre_v5'),'already corrected'
  uv=[[tuple(u.uv) for u in a.data] for a in m.data.uv_layers];images=audit.texture_hashes(m)
  if ident=='xing_yun_tian_jiang':
   groups={m.vertex_groups[n].index for n in ['head']}
   for v in m.data.vertices:
    w=sum(g.weight for g in v.groups if g.group in groups)
    # All face vertices move identically; only neck transition blends.
    if w:v.co.x+=dx*w
   bpy.context.view_layer.objects.active=rig;bpy.ops.object.mode_set(mode='EDIT')
   b=rig.data.edit_bones['head'];b.head.x+=dx;b.tail.x+=dx
   rig.data.edit_bones['neck'].tail.x+=dx
   bpy.ops.object.mode_set(mode='OBJECT')
  else:
   # Connected component selection keeps each original bead and its UV completely rigid.
   adj=[[] for v in m.data.vertices]
   for e in m.data.edges:
    a,b=e.vertices;adj[a].append(b);adj[b].append(a)
   remaining=set(range(len(adj)));parts=[]
   while remaining:
    todo=[remaining.pop()];p=[]
    while todo:
     i=todo.pop();p.append(i)
     for j in adj[i]:
      if j in remaining:remaining.remove(j);todo.append(j)
    c=sum((m.data.vertices[i].co for i in p),Vector())/len(p)
    if 18<=len(p)<=32 and .65<c.z<.84:parts.append(p)
   cord={i for f in m.data.polygons if m.data.materials[f.material_index].name.startswith('Necklace dark woven') for i in f.vertices}
   assert len(parts)==12,(ident,len(parts))
   for i in set(i for p in parts for i in p)|cord:m.data.vertices[i].co.x+=dx
  m.data.update();m['centre_v5']=dx
  assert uv==[[tuple(u.uv) for u in a.data] for a in m.data.uv_layers] and images==audit.texture_hashes(m)
  audit.capture(m,OUT/(ident+'-centred-front.png'),(0,0,.73),(0,-3,0),.62)
  (OUT/(ident+'-centering.json')).write_text(json.dumps({'x_translation':dx,'uv_preserved':True,'images_preserved':True,'method':'rigid head plus weighted neck' if ident.startswith('xing') else '12 rigid source beads and complete cord'}),encoding='utf8')

def rotate_to(p,point):
 direction=point-p.head
 if direction.length<1e-6:return
 q=(p.tail-p.head).rotation_difference(direction)
 p.matrix=Matrix.Translation(p.head)@q.to_matrix().to_4x4()@Matrix.Translation(-p.head)@p.matrix
 bpy.context.view_layer.update()

def plant(rig,side,target,foot_pitch=0):
 upper=rig.pose.bones['leg_upper.'+side];lower=rig.pose.bones['leg_lower.'+side]
 bpy.context.view_layer.update();hip=upper.head.copy();d=target-hip
 l1=upper.bone.length;l2=lower.bone.length;dist=max(abs(l1-l2)+1e-5,min(d.length,l1+l2-1e-5));axis=d.normalized()
 along=(l1*l1-l2*l2+dist*dist)/(2*dist);height=math.sqrt(max(0,l1*l1-along*along))
 pole=Vector((0,-1,0));pole=(pole-axis*pole.dot(axis)).normalized()
 knee=hip+axis*along+pole*height
 rotate_to(upper,knee);rotate_to(lower,target)
 name='foot.'+side
 if name in rig.pose.bones:
  p=rig.pose.bones[name];rest=p.bone.matrix_local.to_quaternion()
  p.matrix=Matrix.LocRotScale(p.head,Quaternion((1,0,0),math.radians(foot_pitch))@rest,Vector((1,1,1)))
 bpy.context.view_layer.update()

def author(ident,boss):
 rig,mesh=rp.select_rig(ident);ca.reset(rig)
 if 'leg_upper.R' not in rig.pose.bones:return quadruped(ident,boss)
 feet={s:rig.data.bones['leg_lower.'+s].tail_local.copy() for s in ('L','R')}
 heavy=ident in ('tong_ling_yuan_wang','bai_zhan_jiang_hun')
 for clip,frames,stride,lift in [('walk',36,.075,.028),('run',24,.13,.055)]:
  v4.remove_action(rig,ident+'.'+clip)
  def gait(f,n):
   t=f/n*math.tau;phase=math.sin(t);bounce=(1-math.cos(2*t))*.003
   ca.translate(rig,'root',(.009*math.cos(t),0,-.012+bounce))
   angles=v4.base(ident)|{'spine':(4 if heavy else 2,0,3*phase),'chest':(2,0,-5*phase),'head':(-2,0,2*phase)}
   angles['arm_upper.L']=(16*phase-5,0,2);angles['arm_lower.L']=(-18-8*max(phase,0),0,0)
   if ident not in rp.HOLDING:
    angles['arm_upper.R']=(-16*phase-5,0,-2);angles['arm_lower.R']=(-18-8*max(-phase,0),0,0)
   else:
    bx,by,bz=angles.get('arm_upper.R',(0,0,0));angles['arm_upper.R']=(bx-4*phase,by,bz)
   for i in range(4):angles['tail_'+str(i)]=(2*math.sin(t-i*.5),0,(7+2*i)*math.sin(t-i*.6))
   for side,sign in [('L',1),('R',-1)]:angles['robe.'+side]=(5*math.sin(t+sign*.7),0,2*sign*phase)
   v4.pose(rig,angles);v4.weapon_axis(ident,rig,v4.carry_axis(ident))
   for side,offset in [('L',0),('R',math.pi)]:
    a=t+offset;plant(rig,side,feet[side]+Vector((0,stride*math.cos(a),lift*max(0,math.sin(a))**1.4)),4*math.sin(a))
  ca.author_action(rig,mesh,clip,frames,gait,loop=True,grounded=False)
 basic=dict(id='attack',motion={'bai_zhan_jiang_hun':'cut','xing_yun_tian_jiang':'thrust','tong_ling_yuan_wang':'sweep'}.get(ident,'weave'),windup=14,active=12,recovery=40)
 for skill in [basic]+catalogue()[boss]:
  clip=skill['id'];v4.remove_action(rig,ident+'.'+clip);w,a,r=[skill[k] for k in ('windup','active','recovery')];total=w+a+r
  def animate(f,n):
   tick=f/n*total
   load=rp.smooth(0,w*.72,tick);strike=rp.smooth(w-2,w+4,tick);recover=1-rp.smooth(w+a*.55,total,tick)
   pre=load*(1-strike);hit=strike*recover;follow=rp.smooth(w+2,w+9,tick)*recover
   motion=skill['motion'];slash=motion in ('sweep','cut','finisher');slam=motion in ('slam','leap');thrust=motion in ('thrust','charge')
   twist=(-18*pre+22*hit) if slash else -7*pre+9*hit
   ca.translate(rig,'root',(.015*pre-.020*hit,.018*pre-.030*hit,-.018-.018*pre-.008*hit))
   angles=v4.base(ident)|{'spine':(-3*pre+7*hit,0,twist*.45),'chest':(-6*pre+12*hit,0,twist*.55),'head':(3*pre-5*hit,0,-twist*.6),
     'arm_upper.L':(-20*pre-12*hit,8*pre,8*pre-15*hit),'arm_lower.L':(-35*pre-25*hit,0,0)}
   axis=Vector(v4.carry_axis(ident));target=axis.copy()
   if slash and ident in rp.HOLDING:
    angles['arm_upper.R']=(-38*pre-24*hit,-28*pre+32*hit,-24*pre+42*hit);angles['arm_lower.R']=(-62*pre-15*hit,0,0)
    target=Vector((-.7,.15,.70)).lerp(Vector((.88,-.45,-.14)),strike)
   elif thrust:
    angles['arm_upper.R']=(-10*pre-70*hit,-12*hit,10*pre);angles['arm_lower.R']=(-72*pre-6*hit,0,0)
    target=Vector((-.2,.35,.9)).lerp(Vector((-.03,-1,.04)),strike)
   elif slam and ident in rp.HOLDING:
    angles['arm_upper.R']=(-105*pre-38*hit,-15*pre,5*pre);angles['arm_lower.R']=(-55*pre-12*hit,0,0)
    angles['chest']=(-12*pre+24*hit,0,-5*pre+5*hit)
    target=Vector((-.2,.45,.86)).lerp(Vector((-.2,-.94,-.28)),strike)
   elif motion=='guard':
    angles['arm_upper.R']=(-30*load*recover,0,0);angles['arm_lower.R']=(-65*load*recover,0,0);target=Vector((-.3,-.8,.5))
   else:
    for side,sign in [('R',-1),('L',1)]:
     lift=72 if motion in ('summon','finisher') else 36
     angles['arm_upper.'+side]=(-lift*pre-48*hit,sign*(20*pre+32*hit),sign*8*pre)
     angles['arm_lower.'+side]=(-55*pre-16*hit,0,0);angles['hand.'+side]=(12*pre-10*hit,0,sign*10*follow)
   for side,sign in [('L',1),('R',-1)]:angles['robe.'+side]=(5*pre-7*follow,0,sign*3*follow)
   for i in range(4):angles['tail_'+str(i)]=(0,0,(8+i*2)*math.sin(tick*.13-i*.65)*(pre+hit))
   v4.pose(rig,angles);v4.weapon_axis(ident,rig,axis.lerp(target,load*recover).normalized())
   for side in ('L','R'):plant(rig,side,feet[side])
  ca.author_action(rig,mesh,clip,round(total*24/20),animate,grounded=False)
 mesh['combat_v5']=True;ca.reset(rig)
 print('FULL_BODY',ident,'walk/run and',len(catalogue()[boss])+1,'attacks')

def quadruped(ident,boss):
 rig,mesh=rp.select_rig(ident)
 # Keep accepted quadruped gait, add shoulder/hip transfer to existing server-timed skills.
 for s in catalogue()[boss]:
  action=bpy.data.actions[ident+'.'+s['id']];composer.assign_action(rig,action)
  for f in range(int(action.frame_range[1])+1):
   bpy.context.scene.frame_set(f);t=f/24*20;load=rp.smooth(0,s['windup']*.8,t)*(1-rp.smooth(s['windup']-2,s['windup']+4,t));impact=rp.smooth(s['windup']-2,s['windup']+4,t)*(1-rp.smooth(s['windup']+s['active']*.5,s['windup']+s['active']+40,t))
   ca.translate(rig,'root',(0,.015*load-.02*impact,-.012*(load+impact)))
   rig.pose.bones['root'].keyframe_insert(data_path='location',frame=f)
 mesh['combat_v5']=True;ca.reset(rig)

if __name__=='__main__':
 centre()
 for boss,ident in MODELS.items():author(ident,boss)
