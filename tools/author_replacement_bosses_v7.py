"""Run through Blender MCP. New anatomy, original surface/UV, local finger and cloth rigs."""
import bpy,bmesh,math,json,hashlib
from pathlib import Path
from mathutils import Vector,Matrix,Quaternion
import completion_authoring as ca
import refine_posture_grip as rp
import refine_boss_motion_v5 as v5
import author_boss_combat_v4 as v4
import audit_original_boss_hands as audit
from bl_ext.blender_org.animal_animator.core import composer
from boss_combat_catalogue import catalogue
OUT=ca.ROOT/'asset_sources/boss-replacement-v7-20261018'
SPECS={'xuan_tian_cheng_zhu':('城主','xuantians_remnant',3.4),'yun_hai_tian_jun':('残念','cloud_sea_lord',3.2)}
def smooth(a,b,t):return rp.smooth(a,b,t)
def seg(p,a,b):
 v=b-a;t=max(0,min(1,(p-a).dot(v)/max(1e-9,v.length_squared)))
 return (p-a-v*t).length,t

def build(ident):
 name,boss,height=SPECS[ident];caster=ident.startswith('yun')
 # Keep complete previous rigs as archive; exact production names belong to new sources.
 for o in list(bpy.data.objects):
  if o.name in (ident+'_rig',ident+'_mesh'):o.name='archive_v6_'+o.name
 for a in list(bpy.data.actions):
  if a.name.startswith(ident+'.'):a.name='archive_v6_'+a.name
 scene=bpy.data.scenes.new('CompletionRig_'+ident+'_v7');bpy.context.window.scene=scene
 src=(Path.home() / 'Downloads')/(name+'.glb')
 bpy.ops.import_scene.gltf(filepath=str(src));mesh=next(o for o in scene.objects if o.type=='MESH');mesh.name=ident+'_mesh'
 mesh.data.transform(mesh.matrix_world);mesh.matrix_world.identity()
 bm=bmesh.new();bm.from_mesh(mesh.data);bmesh.ops.remove_doubles(bm,verts=list(bm.verts),dist=1e-6);bm.to_mesh(mesh.data);bm.free()
 ground=min(v.co.z for v in mesh.data.vertices);mesh.data.transform(Matrix.Translation((0,0,-ground)))
 uv=[[list(u.uv) for u in layer.data] for layer in mesh.data.uv_layers];textures=audit.texture_hashes(mesh)
 data=bpy.data.armatures.new(ident+'_rig');rig=bpy.data.objects.new(ident+'_rig',data);scene.collection.objects.link(rig)
 bpy.ops.object.select_all(action='DESELECT');rig.select_set(True);bpy.context.view_layer.objects.active=rig;bpy.ops.object.mode_set(mode='EDIT')
 def bone(n,h,t,parent='root'):
  b=data.edit_bones.new(n);b.head=h;b.tail=t;b.use_deform=n!='root'
  if parent:b.parent=data.edit_bones[parent]
  return b
 bone('root',(0,0,0),(0,0,.1),None)
 bone('hips',(0,0,.47),(0,0,.55));bone('spine',(0,0,.55),(0,0,.65),'hips');bone('chest',(0,0,.65),(0,0,.73),'spine')
 bone('neck',(0,0,.73),(0,-.005,.79),'chest');bone('head',(0,-.005,.79),(0,-.005,.89),'neck')
 for side,sgn in [('L',1),('R',-1)]:
  def p(x,y,z):return (sgn*x,y,z)
  shoulder=(.098,-.002,.704) if caster else (.108,-.005,.721)
  elbow=(.157,-.008,.616) if caster else (.171,-.01,.602)
  wrist=(.198,-.045,.539) if caster else (.214,-.052,.503)
  palm=(.218,-.057,.506) if caster else (.232,-.066,.464)
  bone('arm_upper.'+side,p(*shoulder),p(*elbow),'chest');bone('arm_lower.'+side,p(*elbow),p(*wrist),'arm_upper.'+side);bone('hand.'+side,p(*wrist),p(*palm),'arm_lower.'+side)
  bone('leg_upper.'+side,p(.067,.018,.475),p(.101,-.009,.266),'hips');bone('leg_lower.'+side,p(.101,-.009,.266),p(.115,.003,.075),'leg_upper.'+side);bone('foot.'+side,p(.115,.003,.075),p(.115,-.07,.023),'leg_lower.'+side)
  for section,x,y in [('robe',.11,.045),('robe_front',.06,-.078)]:
   bone(section+'.'+side,p(x,y,.51),p(x*1.22,y,.34),'hips');bone(section+'_tip.'+side,p(x*1.22,y,.34),p(x*1.4,y,.20),section+'.'+side)
  if caster:
   bone('hair.'+side,p(.042,.045,.80),p(.06,.075,.68),'head');bone('hair_tip.'+side,p(.06,.075,.68),p(.075,.105,.57),'hair.'+side)
   bone('sleeve.'+side,p(.185,.012,.56),p(.205,.026,.47),'arm_lower.'+side)
  digits=({'little':[(.202,-.055,.497),(.207,-.052,.482),(.211,-.049,.468),(.212,-.047,.459)],
    'ring':[(.213,-.058,.501),(.220,-.056,.482),(.225,-.053,.466),(.228,-.051,.454)],
    'middle':[(.225,-.062,.506),(.235,-.058,.485),(.242,-.056,.469),(.248,-.053,.456)],
    'index':[(.234,-.064,.515),(.246,-.060,.497),(.252,-.056,.482),(.256,-.052,.472)],
    'thumb':[(.224,-.057,.532),(.233,-.058,.519),(.242,-.057,.509),(.248,-.056,.499)]} if caster else {
    'little':[(.221,-.057,.449),(.216,-.060,.439),(.208,-.064,.431),(.205,-.066,.425)],
    'ring':[(.230,-.067,.446),(.232,-.067,.431),(.228,-.071,.417),(.223,-.073,.409)],
    'middle':[(.240,-.075,.449),(.244,-.075,.431),(.243,-.080,.416),(.240,-.082,.407)],
    'index':[(.249,-.083,.458),(.257,-.082,.447),(.263,-.083,.434),(.264,-.085,.424)],
    'thumb':[(.242,-.076,.482),(.250,-.084,.477),(.259,-.088,.473),(.266,-.090,.474)]})
  for digit,chain in digits.items():
   for i in range(3):bone(f'{digit}_{i+1}.{side}',p(*chain[i]),p(*chain[i+1]),'hand.'+side if i==0 else f'{digit}_{i}.{side}')
 bpy.ops.object.mode_set(mode='OBJECT');assert len(rig.data.bones)<=64
 # Heat weights are constrained below so hair, sleeves and the opposite leg cannot pull each other.
 ca.bind(mesh,rig)
 groups={b.name:mesh.vertex_groups.get(b.name) or mesh.vertex_groups.new(name=b.name) for b in rig.data.bones if b.use_deform}
 def weights(index,row):
  for g in list(mesh.data.vertices[index].groups):mesh.vertex_groups[g.group].remove([index])
  top=sorted(row.items(),key=lambda kv:-kv[1])[:4];total=sum(w for _,w in top)
  for n,w in top:
   if w>1e-7:groups[n].add([index],w/total,'REPLACE')
 def near(index,names):
  pt=mesh.data.vertices[index].co;ds=sorted((seg(pt,data.bones[n].head_local,data.bones[n].tail_local)[0],n) for n in names)
  # Narrow blends preserve blocklike plate silhouette while allowing joint bends.
  ds=[x for x in ds[:3] if x[0]<ds[0][0]+.018]
  weights(index,{n:1/max(.003,d)**5 for d,n in ds})
 for v in mesh.data.vertices:
  x,y,z=v.co;s='L' if x>0 else 'R';ax=abs(x)
  if z>.782:weights(v.index,{'head':1});continue
  if caster and y>.038 and z>.57 and ax<.11:
   near(v.index,['head','hair.'+s,'hair_tip.'+s]);continue
  # Open hands are disjoint from sleeves at the wrist, fit each digit to its original surface.
  hand_limit=.531 if caster else .497
  if ax>(.190 if caster else .196) and z<hand_limit and z>(.445 if caster else .398):
   near(v.index,['hand.'+s]+[f'{d}_{j}.{s}' for d in digits for j in (1,2,3)]);continue
  arm_boundary=.095+max(0,.70-z)*.35
  if z>.47 and ax>arm_boundary:
   names=['chest','arm_upper.'+s,'arm_lower.'+s,'hand.'+s]
   if caster and y>.004 and z<.57:names+=['sleeve.'+s]
   near(v.index,names);continue
  if z>.51:near(v.index,['hips','spine','chest','neck','head']);continue
  # Tunic front and back are not bound to legs: two independent two-link skirt panels per side.
  robe=(z>.19 and (y<-.065 or y>.04 or ax>(.105 if caster else .125)))
  if robe:
   prefix='robe_front' if y<-.04 else 'robe'
   near(v.index,['hips',prefix+'.'+s,prefix+'_tip.'+s]);continue
  near(v.index,['hips','leg_upper.'+s,'leg_lower.'+s,'foot.'+s])
 # Local surface smoothing reduces transitions, without ever crossing to a disconnected surface.
 adjacency=[set() for _ in mesh.data.vertices]
 for e in mesh.data.edges:a,b=e.vertices;adjacency[a].add(b);adjacency[b].add(a)
 for _ in range(2):
  current=[{mesh.vertex_groups[g.group].name:g.weight for g in v.groups} for v in mesh.data.vertices];updates={}
  for v in mesh.data.vertices:
   if v.co.z>.80:continue
   row={n:w*.85 for n,w in current[v.index].items()}
   if adjacency[v.index]:
    for j in adjacency[v.index]:
     for n,w in current[j].items():row[n]=row.get(n,0)+.15*w/len(adjacency[v.index])
   updates[v.index]=row
  for i,row in updates.items():weights(i,row)
 assert uv==[[list(u.uv) for u in layer.data] for layer in mesh.data.uv_layers] and textures==audit.texture_hashes(mesh)
 rig.animation_data_create();mesh['combat_v7']=True;mesh['body_height']=max(v.co.z for v in mesh.data.vertices)
 meta={'name':name,'id':ident,'source':str(src),'source_sha256':hashlib.sha256(src.read_bytes()).hexdigest(),'joints':len(data.bones),'body_height':mesh['body_height'],'target_height_blocks':height,'model_scale':height/mesh['body_height'],'uv_preserved':True,'images_preserved':True,'role':'caster' if caster else 'unarmed_brawler'}
 (OUT/(ident+'-manifest.json')).write_text(json.dumps(meta,ensure_ascii=False,indent=2),encoding='utf8')
 ca.reset(rig);audit.capture(mesh,OUT/(ident+'-rig-rest.png'),(0,0,.48),(0,-3,0),1.12)
 print('RIGGED',ident,len(data.bones),len(mesh.data.vertices))

def fingers(rig,close):
 if rig.name.startswith('xuan_tian_cheng_zhu'):
  if close <= .5:return
  from refine_city_fist_v6 import fist
  fist(rig)
  return
 # Curl toward the palm (+Y), using the actual local finger axes, not a global thumb substitute.
 for side in ('L','R'):
  for digit in ('index','middle','ring','little','thumb'):
   for j in (1,2,3):
    p=rig.pose.bones[f'{digit}_{j}.{side}'];direction=(p.bone.tail_local-p.bone.head_local).normalized();axis=direction.cross(Vector((0,1,0))).normalized()
    amount=([24,22,12] if digit=='thumb' else [65,55,20])[j-1]*close
    p.rotation_quaternion=Quaternion(p.bone.matrix_local.to_3x3().inverted()@axis,math.radians(amount))

def arm_target(rig,side,target):
 upper=rig.pose.bones['arm_upper.'+side];lower=rig.pose.bones['arm_lower.'+side]
 # Solve from the untwisted anatomical chain, not from unrelated Euler poses.
 for p in (upper,lower,rig.pose.bones['hand.'+side]):p.rotation_quaternion=(1,0,0,0)
 bpy.context.view_layer.update();hip=upper.head.copy();delta=Vector(target)-hip
 l1=upper.bone.length;l2=lower.bone.length;dist=max(abs(l1-l2)+1e-5,min(delta.length,l1+l2-.001));axis=delta.normalized()
 along=(l1*l1-l2*l2+dist*dist)/(2*dist);h=math.sqrt(max(0,l1*l1-along*along))
 pole=Vector((1 if side=='L' else -1,.2,-.25));pole=(pole-axis*pole.dot(axis)).normalized()
 v5.rotate_to(upper,hip+axis*along+pole*h);v5.rotate_to(lower,hip+axis*dist)
 # Preserve the wrist's local orientation along the solved arm, avoiding a
 # fixed world-space palm roll that corkscrewed the forearm and sleeve.
 p=rig.pose.bones['hand.'+side];p.rotation_quaternion=(1,0,0,0)
 bpy.context.view_layer.update()

def animate(ident):
 rig,mesh=rp.select_rig(ident);caster=ident.startswith('yun');boss=SPECS[ident][1]
 feet={s:rig.data.bones['leg_lower.'+s].tail_local.copy() for s in ('L','R')}
 def neutral_rest():
  # Arms beside the body; open source hands stay relaxed outside combat.
  natural_arms()
  fingers(rig,0)
 def natural_arms(phase=0,swing=0):
  for side,sign in [('L',1),('R',-1)]:
   u=rig.pose.bones['arm_upper.'+side];l=rig.pose.bones['arm_lower.'+side]
   for p in (u,l,rig.pose.bones['hand.'+side]):p.rotation_quaternion=(1,0,0,0)
   bpy.context.view_layer.update()
   v5.rotate_to(u,u.head+Vector((sign*.23,sign*swing*phase,-1)))
   v5.rotate_to(l,l.head+Vector((sign*.09,-.11+sign*swing*phase,-1)))
 def combat_rest():
  angles={'arm_upper.L':(0,-10,-12),'arm_upper.R':(0,10,12)} if caster else {'arm_upper.L':(-15,-12,-17),'arm_upper.R':(-15,12,17),'arm_lower.L':(-42,0,0),'arm_lower.R':(-42,0,0)}
  v4.pose(rig,angles);fingers(rig,.10 if caster else 1.05)
  if not caster:
   for side,sign in [('L',1),('R',-1)]:arm_target(rig,side,(sign*.12,-.17,.64))
 def secondary(t,amplitude=1):
  for side,sign in [('L',1),('R',-1)]:
   for prefix in ['robe','robe_front']:
    v4.pose(rig,{prefix+'.'+side:(2.5*amplitude*math.sin(t+sign*.5),0,sign*1.5*amplitude),prefix+'_tip.'+side:(3.5*amplitude*math.sin(t-.4+sign*.5),0,sign*amplitude)})
   if caster:v4.pose(rig,{'hair.'+side:(1.2*math.sin(t-.3),0,1.5*math.sin(t-.4)),'hair_tip.'+side:(2*math.sin(t-.6),0,2*math.sin(t-.7)),'sleeve.'+side:(2*math.sin(t-.2),0,sign*2*math.sin(t))})
 def author(name,frames,fn,loop=False):
  v4.remove_action(rig,ident+'.'+name);return ca.author_action(rig,mesh,name,frames,fn,loop,grounded=False)
 def idle(f,n):
  neutral_rest();t=f/n*math.tau;v4.pose(rig,{'chest':(1.2*math.sin(t),0,0),'head':(-.8*math.sin(t),0,1.1*math.sin(t))});secondary(t,.4)
 author('idle',96,idle,True)
 for clip,n,stride,lift in [('walk',36,.065 if caster else .085,.035),('run',24,.10 if caster else .13,.055)]:
  def gait(f,n):
   neutral_rest();t=f/n*math.tau;phase=math.sin(t);ca.translate(rig,'root',(.008*math.cos(t),0,-.014+.003*math.cos(t*2)))
   v4.pose(rig,{'spine':(3,0,4*phase),'chest':(2,0,-6*phase),'head':(-3,0,2*phase)})
   for side,sign in [('L',1),('R',-1)]:
    a=t+(0 if sign==1 else math.pi);v5.plant(rig,side,feet[side]+Vector((0,stride*math.cos(a),lift*max(0,math.sin(a))**1.4)),4*math.sin(a))
   natural_arms(phase,.32 if clip=='run' else .21)
   secondary(t,1)
  author(clip,n,gait,True)
 def hurt(f,n):
  neutral_rest();e=math.sin(math.pi*f/n)**2;v4.pose(rig,{'chest':(-4*e,0,2*e),'head':(2*e,0,0)})
 author('hurt',14,hurt)
 def death(f,n):
  neutral_rest();e=smooth(0,.7,f/n);ca.translate(rig,'root',(0,0,-.21*e));v4.pose(rig,{'spine':(22*e,0,0),'chest':(25*e,0,0),'head':(18*e,0,0)})
  for s in ('L','R'):v5.plant(rig,s,feet[s]);v4.pose(rig,{'arm_upper.'+s:(-15*(1-e),0,0),'arm_lower.'+s:(-15*(1-e),0,0)})
 author('death',48,death)
 basic=dict(id='attack',motion='weave' if caster else 'punch',windup=14,active=12,recovery=40)
 skills=[basic]+catalogue()[boss]
 if not caster:skills+=[dict(id='counter_hit',motion='punch',windup=0,active=8,recovery=32)]
 for skill in skills:
  w,a,r=[skill[k] for k in ('windup','active','recovery')];total=w+a+r
  def combat(f,n):
   combat_rest();t=f/n*total;load=smooth(0,max(.01,w*.75),t);strike=smooth(w-2,w+4,t);fade=1-smooth(w+a*.65,total,t);pre=load*(1-strike);hit=strike*fade
   if caster:
    mode=skill['id'];high=mode in ('water_pressure','palace_cloud','final_river');opening=mode in ('cloud_locks','mirror_water')
    ca.translate(rig,'root',(0,.008*pre,-.012*load*fade))
    v4.pose(rig,{'spine':(-2*pre+4*hit,0,2*hit),'chest':(-4*pre+6*hit,0,-4*hit),'head':(2*pre-3*hit,0,2*hit)})
    for side,sign in [('L',1),('R',-1)]:
     v4.pose(rig,{'arm_upper.'+side:(-(65 if high else 28)*pre-(58 if high else 43)*hit,-sign*(8+12*pre+20*hit),-sign*(12+8*hit)), 'arm_lower.'+side:(-46*pre-15*hit-8,0,0),'hand.'+side:(10*pre-8*hit,sign*8*hit,sign*4*hit)})
    fingers(rig,.10+.16*pre)
   else:
    # Guard -> hip windup -> shoulder -> elbow extension -> damped recovery.
    combo=skill['id']=='sword_array';slam=skill['id']=='wall_fall';counter=skill['id']=='city_seal'
    if combo and t>=w:
     local=(t-w)%10;strike=smooth(0,3,local);hit=strike*(1-smooth(5,10,local))*fade;pre=(1-strike)*fade
    left=combo and int(max(0,t-w)/10)%2==0
    sign=1 if left else -1;side='L' if left else 'R';other='R' if left else 'L'
    ca.translate(rig,'root',(-sign*.014*pre+sign*.018*hit,.015*pre-.035*hit,-.022-.018*pre))
    v4.pose(rig,{'hips':(0,0,sign*(-7*pre+10*hit)),'spine':(-3*pre+7*hit,0,sign*(-8*pre+12*hit)),'chest':(-4*pre+9*hit,0,sign*(-10*pre+16*hit)),'head':(2*pre-5*hit,0,sign*(12*pre-20*hit))})
    v4.pose(rig,{'arm_upper.'+side:(-15-5*pre-65*hit,-sign*(12+20*hit),sign*17),'arm_lower.'+side:(-42-38*pre+32*hit,0,0),'arm_upper.'+other:(-15,sign*12,-sign*17),'arm_lower.'+other:(-42-16*hit,0,0)})
    if slam:
     for s,sg in [('L',1),('R',-1)]:v4.pose(rig,{'arm_upper.'+s:(-15-105*pre-50*hit,-sg*10,-sg*12),'arm_lower.'+s:(-42-20*pre+20*hit,0,0)})
     v4.pose(rig,{'chest':(-10*pre+24*hit,0,0)})
    if counter:
     for s,sg in [('L',1),('R',-1)]:v4.pose(rig,{'arm_upper.'+s:(-35*load*fade,-sg*12,-sg*12),'arm_lower.'+s:(-65*load*fade,0,0)})
    fingers(rig,1.05)
    for ss,sg in [('L',1),('R',-1)]:
     target=Vector((sg*.12,-.17,.64))
     if ss==side:target+=Vector((-sg*.035*hit,.045*pre-.13*hit,.035*pre+.035*hit))
     if slam:target=Vector((sg*.10,-.12-.07*hit,.64+.21*pre-.15*hit))
     if counter:target=Vector((sg*.065,-.15,.73))
     arm_target(rig,ss,target)
   secondary(t*.11,.7*(pre+hit))
   for s in ('L','R'):v5.plant(rig,s,feet[s])
  author(skill['id'],round(total*24/20),combat)
 ca.reset(rig);mesh['combat_v7']=True
 print('ANIMATED',ident)

def preview(ident,clip,frame,label):
 rig,m=rp.select_rig(ident);composer.assign_action(rig,bpy.data.actions[ident+'.'+clip]);bpy.context.scene.frame_set(frame);bpy.context.view_layer.update()
 audit.capture(m,OUT/(ident+'-'+label+'.png'),(0,0,.48),(1,-3,.1),1.12)
 ca.reset(rig)

def refine_weights(ident):
 rig,m=rp.select_rig(ident);caster=ident.startswith('yun')
 def put(i,row):
  for g in list(m.data.vertices[i].groups):m.vertex_groups[g.group].remove([i])
  top=sorted(row.items(),key=lambda a:-a[1])[:4];total=sum(w for _,w in top)
  for n,w in top:
   if w>1e-6:m.vertex_groups[n].add([i],w/total,'REPLACE')
 for v in m.data.vertices:
  x,y,z=v.co;ax=abs(x);s='L' if x>0 else 'R'
  if caster and .40<z<.60 and ax>.132:
   is_hand=ax>.192 and y<-.025 and z<.535
   if not is_hand:put(v.index,{'arm_lower.'+s:1})
  if .145<z<.52 and ax<.04 and y<-.06:
   put(v.index,{'robe_front.L' if z>.34 else 'robe_front_tip.L':1})
  if .60<z<.77 and .065<ax<.18:
   arm=smooth(.075,.155,ax);put(v.index,{'chest':1-arm,'arm_upper.'+s:arm})
  if (ax>.195 and .395<z<.493 and not caster) or (caster and ax>.192 and y<-.025 and .445<z<.533):
   candidates=[]
   for digit in ('thumb','index','middle','ring','little'):
    for j in (1,2,3):
     n=f'{digit}_{j}.{s}';b=rig.data.bones[n];d,t=seg(v.co,b.head_local,b.tail_local);candidates.append((d,n,t))
   d,n,t=min(candidates);b=rig.data.bones[n];digit=n.split('_')[0];first=rig.data.bones[f'{digit}_1.{s}'];direction=(first.tail_local-first.head_local).normalized();along=(v.co-first.head_local).dot(direction)
   mix=smooth(-.005,.008,along)
   if d<.015:
    j=int(n.split('_')[1].split('.')[0]);row={'hand.'+s:1-mix,n:mix}
    if j>1 and t<.30:
     share=(.30-t)/.60;row[n]=mix*(1-share);row[f'{digit}_{j-1}.{s}']=mix*share
    elif j<3 and t>.70:
     share=(t-.70)/.60;row[n]=mix*(1-share);row[f'{digit}_{j+1}.{s}']=mix*share
    put(v.index,row)
 adj=[set() for _ in m.data.vertices]
 for e in m.data.edges:a,b=e.vertices;adj[a].add(b);adj[b].add(a)
 for _ in range(12):
  rows=[{m.vertex_groups[g.group].name:g.weight for g in v.groups} for v in m.data.vertices]
  for v in m.data.vertices:
   if not (.59<v.co.z<.78 and .055<abs(v.co.x)<.185):continue
   row={n:w*.65 for n,w in rows[v.index].items()}
   for j in adj[v.index]:
    for n,w in rows[j].items():row[n]=row.get(n,0)+.35*w/max(1,len(adj[v.index]))
   put(v.index,row)
 print('REFINED',ident)

def smooth_deformation(ident):
 """Repair discontinuous cloth/shoulder weights using actual sampled deformation, never geometry."""
 import numpy as np
 rig,m=rp.select_rig(ident);pts=np.array([v.co[:] for v in m.data.vertices]);edges=np.array([list(e.vertices) for e in m.data.edges]);base=np.linalg.norm(pts[edges[:,0]]-pts[edges[:,1]],axis=1)
 adj=[set() for _ in m.data.vertices]
 for a,b in edges:adj[a].add(int(b));adj[b].add(int(a))
 acts=[a for a in bpy.data.actions if a.name.startswith(ident+'.')];history=[]
 for iteration in range(8):
  worst=np.zeros(len(edges))
  for a in acts:
   composer.assign_action(rig,a)
   for f in range(0,int(a.frame_range[1])+1,12):
    bpy.context.scene.frame_set(f);bpy.context.view_layer.update();p=np.array([v.co[:] for v in m.evaluated_get(bpy.context.evaluated_depsgraph_get()).data.vertices]);worst=np.maximum(worst,np.linalg.norm(p[edges[:,0]]-p[edges[:,1]],axis=1)-base)
  history.append(float(worst.max()));bad=set(int(i) for e in edges[worst>.035] for i in e)
  if not bad:break
  affected=bad|set(j for i in bad for j in adj[i])
  for _ in range(5):
   rows=[{m.vertex_groups[g.group].name:g.weight for g in v.groups} for v in m.data.vertices]
   for i in affected:
    if m.data.vertices[i].co.z>.80:continue
    row={n:w*.5 for n,w in rows[i].items()}
    for j in adj[i]:
     for n,w in rows[j].items():row[n]=row.get(n,0)+.5*w/max(1,len(adj[i]))
    top=sorted(row.items(),key=lambda a:-a[1])[:4];total=sum(w for _,w in top)
    for g in list(m.data.vertices[i].groups):m.vertex_groups[g.group].remove([i])
    for n,w in top:m.vertex_groups[n].add([i],w/total,'REPLACE')
 ca.reset(rig);(OUT/(ident+'-weight-repair.json')).write_text(json.dumps({'sampled_max_extension_history':history,'mesh_positions_unchanged':bool(np.array_equal(pts,np.array([v.co[:] for v in m.data.vertices])))}),encoding='utf8');print(ident,history)
