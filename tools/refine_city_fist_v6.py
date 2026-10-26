import bpy,math
from mathutils import Vector,Quaternion,Matrix
import refine_posture_grip as rp,author_replacement_bosses_v6 as v6,audit_original_boss_hands as audit
from bl_ext.blender_org.animal_animator.core import composer

def fist(rig,angles=(100,100,55)):
 for side,sgn in [('L',1),('R',-1)]:
  hand=rig.data.bones['hand.'+side]
  across=rig.data.bones['index_1.'+side].head_local-rig.data.bones['little_1.'+side].head_local
  normal=(hand.tail_local-hand.head_local).cross(across).normalized()
  if normal.y>0:normal=-normal
  for digit in ('index','middle','ring','little'):
   for j,degrees in enumerate(angles,1):
    p=rig.pose.bones[f'{digit}_{j}.{side}'];d=(p.bone.tail_local-p.bone.head_local).normalized();axis=d.cross(normal).normalized()
    p.rotation_quaternion=Quaternion(p.bone.matrix_local.to_3x3().inverted()@axis,math.radians(degrees))
  for j in (1,2,3):rig.pose.bones[f'thumb_{j}.{side}'].rotation_quaternion=(1,0,0,0)
  bpy.context.view_layer.update()
  transform=rig.pose.bones['hand.'+side].matrix@hand.matrix_local.inverted()
  goal=transform@Vector((sgn*.232,-.093,.456))
  for j in (1,2,3):v6.v5.rotate_to(rig.pose.bones[f'thumb_{j}.{side}'],goal)

def roll_hand(rig,side):
 # The source palm is oblique; boxing knuckles face forward with palm down.
 p=rig.pose.bones['hand.'+side];b=p.bone
 across=rig.data.bones['index_1.'+side].head_local-rig.data.bones['little_1.'+side].head_local
 normal=(b.tail_local-b.head_local).cross(across).normalized()
 if normal.y>0:normal=-normal
 normal=(p.matrix@b.matrix_local.inverted()).to_3x3()@normal
 axis=(p.tail-p.head).normalized();normal=(normal-axis*normal.dot(axis)).normalized()
 goal=Vector((0,0,-1));goal=(goal-axis*goal.dot(axis)).normalized()
 angle=math.atan2(axis.dot(normal.cross(goal)),normal.dot(goal))
 p.matrix=Matrix.Translation(p.head)@Quaternion(axis,angle).to_matrix().to_4x4()@Matrix.Translation(-p.head)@p.matrix
 bpy.context.view_layer.update()

def preview():
 rig,m=rp.select_rig('xuan_tian_cheng_zhu');composer.assign_action(rig,bpy.data.actions['xuan_tian_cheng_zhu.idle']);bpy.context.scene.frame_set(0)
 poses={p.name:p.matrix_basis.copy() for p in rig.pose.bones};rig.animation_data.action=None
 for p in rig.pose.bones:p.matrix_basis=poses[p.name]
 for label,angles in [('c',(100,100,55)),('d',(110,105,55))]:
  fist(rig,angles);bpy.context.view_layer.update();c=rig.pose.bones['hand.L'].tail.copy()
  audit.capture(m,v6.OUT/('city-fist-'+label+'.png'),c,(1,-3,.3),.18)
