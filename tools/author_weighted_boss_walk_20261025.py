"""Background Blender: author contact-rich locomotion from current runtime rigs.
Only the exported walk/run channels are later transferred; original production
geometry, skin weights, grip, materials and every combat clip stay unchanged.
"""
import bpy,sys,math
from pathlib import Path
from mathutils import Vector
root=(Path(__file__).resolve().parents[1]);sys.path.insert(0,str(root/'tools'))
import completion_authoring as ca
from refine_boss_motion_v5 import plant
from bl_ext.blender_org.animal_animator.core import composer
out=root/'asset_sources/support-spells-beast-motion-20261025';out.mkdir(parents=True,exist_ok=True)
for ident in ['xing_yun_tian_jiang','tong_ling_yuan_wang']:
 bpy.ops.wm.read_factory_settings(use_empty=True)
 bpy.ops.import_scene.gltf(filepath=str(root/'src/main/resources/assets/xiantu/models/entity'/(ident+'.glb')))
 rig=next(o for o in bpy.context.scene.objects if o.type=='ARMATURE');meshes=[o for o in bpy.context.scene.objects if o.type=='MESH'];rig.name=ident+'_rig'
 mesh=meshes[0];mesh.name=ident+'_mesh'
 ca.reset(rig);feet={s:rig.data.bones['foot.'+s].head_local.copy() for s in ['L','R']}
 idle=bpy.data.actions.get(ident+'.idle');assert idle
 composer.assign_action(rig,idle);bpy.context.scene.frame_set(0)
 carry={p.name:p.rotation_quaternion.copy() for p in rig.pose.bones}
 for clip,frames,stride,lift in [('walk',32,.12,.055),('run',22,.19,.095)]:
  for tr in list(rig.animation_data.nla_tracks):
   if any(s.action and s.action.name==ident+'.'+clip for s in tr.strips):rig.animation_data.nla_tracks.remove(tr)
  old=bpy.data.actions.get(ident+'.'+clip)
  if old:bpy.data.actions.remove(old)
  def gait(f,n):
   t=f/n;phase=math.tau*t;heavy=ident=='tong_ling_yuan_wang'
   for p in rig.pose.bones:p.rotation_quaternion=carry[p.name]
   # Weight shifts onto the planted leg; the lifted foot clears the ground.
   ca.translate(rig,'root',(.009*math.cos(phase),0,-.022+.006*(1-math.cos(2*phase))))
   ca.rotate(rig,'hips',(0,0,1),3*math.sin(phase));ca.rotate(rig,'chest',(0,0,1),-4*math.sin(phase))
   ca.rotate(rig,'spine',(1,0,0),6 if heavy else 3)
   ca.rotate(rig,'arm_upper.L',(1,0,0),-9+18*math.cos(phase));ca.rotate(rig,'arm_lower.L',(1,0,0),-22-6*math.sin(phase))
   for side,offset in [('L',0),('R',.5)]:
    a=(t+offset)%1
    if a<.62:forward=stride*(1-2*a/.62);up=0;pitch=3-8*a/.62
    else:
     u=(a-.62)/.38;forward=-stride+2*stride*(u*u*(3-2*u));up=lift*math.sin(math.pi*u)**1.3;pitch=10*math.sin(math.pi*u)
    plant(rig,side,feet[side]+Vector((0,-forward,up)),pitch)
    if 'robe.'+side in rig.pose.bones:ca.rotate(rig,'robe.'+side,(1,0,0),8*math.sin(phase+offset*math.tau-.3))
   for i in range(4):
    if 'tail_'+str(i) in rig.pose.bones:ca.rotate(rig,'tail_'+str(i),(0,0,1),(6+2*i)*math.sin(phase-i*.6))
  ca.author_action(rig,mesh,clip,frames,gait,loop=True,grounded=False)
 ca.reset(rig);bpy.context.scene.render.fps=24
 bpy.ops.object.select_all(action='DESELECT');rig.select_set(True)
 for m in meshes:m.select_set(True)
 bpy.context.view_layer.objects.active=rig
 bpy.ops.export_scene.gltf(filepath=str(out/(ident+'-motion.glb')),export_format='GLB',use_selection=True,export_animation_mode='NLA_TRACKS',export_animations=True,export_force_sampling=True,export_frame_range=False,export_skins=True,export_yup=True)
 bpy.ops.wm.save_as_mainfile(filepath=str(out/(ident+'-motion.blend')))
 print('AUTHORED_WEIGHTED_GAIT',ident,flush=True)
