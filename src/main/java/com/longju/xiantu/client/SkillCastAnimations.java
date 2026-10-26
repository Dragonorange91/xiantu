package com.longju.xiantu.client;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.HumanoidArm;
import java.util.*;
/** Smooth cast-only additive pose. Normal sword/axe/staff melee stays vanilla. */
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid="xiantu",value=net.minecraftforge.api.distmarker.Dist.CLIENT)
public final class SkillCastAnimations {
 private record Pose(long start,long released,String skill,net.minecraft.world.item.Item item){}
 private static final Map<UUID,Pose> POSES=new HashMap<>();
 private static long now(){var l=Minecraft.getInstance().level;return l==null?0:l.getGameTime();}
 private static net.minecraft.world.item.Item held(UUID id){var l=Minecraft.getInstance().level;var p=l==null?null:l.getPlayerByUUID(id);return p==null?net.minecraft.world.item.Items.AIR:p.getMainHandItem().getItem();}
 static void begin(UUID id,String skill){if(POSES.size()<32)POSES.put(id,new Pose(now(),0,skill,held(id)));}
 static void release(UUID id){var p=POSES.get(id);if(p!=null)POSES.put(id,new Pose(p.start,now(),p.skill,p.item));}
 static void pulse(UUID id,String skill){if(POSES.size()<32||POSES.containsKey(id))POSES.put(id,new Pose(now(),now(),skill,held(id)));}
 static void end(UUID id){var p=POSES.get(id);if(p!=null&&p.released==0)POSES.put(id,new Pose(p.start,now(),p.skill,p.item));}
 static void tick(){POSES.entrySet().removeIf(e->{var p=e.getValue();return held(e.getKey())!=p.item||now()-p.start>(p.skill.equals("great_fireball")?220:65)||p.released>0&&now()-p.released>Math.max(14,AuthoredWeaponClips.duration(p.skill)*20);});}
 static void clear(){POSES.clear();}
 private static float weight(Pose p){double t=now()+Minecraft.getInstance().getFrameTime();double u=Math.max(0,Math.min(1,(t-p.start)/5));double v=p.released==0?1:Math.max(0,Math.min(1,1-(t-p.released)/10));return (float)(u*u*(3-2*u)*v*v*(3-2*v));}
 public static boolean thirdPerson(HumanoidModel<?> m,LivingEntity e){var p=POSES.get(e.getUUID());if(p==null)return false;float w=weight(p);boolean right=e.getMainArm()==HumanoidArm.RIGHT;var arm=right?m.rightArm:m.leftArm;var other=right?m.leftArm:m.rightArm;
  if(AuthoredWeaponClips.family(p.skill)!=null){double seconds=p.released==0?0:(now()+Minecraft.getInstance().getFrameTime()-p.released)/20;float blend=authoredWeight(p,seconds);int sign=right?1:-1;float r=(float)(Math.PI/180);
   var a=AuthoredWeaponClips.sample(p.skill,"right_arm","rotation",seconds);var b=AuthoredWeaponClips.sample(p.skill,"left_arm","rotation",seconds);var body=AuthoredWeaponClips.sample(p.skill,"body","rotation",seconds);
   arm.xRot=arm.xRot*(1-blend)-a[0]*r*blend;arm.yRot=arm.yRot*(1-blend)+a[1]*r*sign*blend;arm.zRot=arm.zRot*(1-blend)-a[2]*r*sign*blend;
   boolean support=java.util.Set.of("spear_thrust","staff_sweep","staff_slam","axe_slam").contains(AuthoredWeaponClips.family(p.skill))&&e.getOffhandItem().isEmpty();
   if(support){other.xRot=other.xRot*(1-blend)-b[0]*r*blend;other.yRot=other.yRot*(1-blend)-b[1]*r*sign*blend;}
   m.body.yRot+=body[1]*r*sign*blend;return true;}
  float age=(float)(now()+Minecraft.getInstance().getFrameTime()-p.start);float progress=Math.max(0,Math.min(1,age/15));float ease=progress*progress*(3-2*progress);float pitch=-1.25f,yaw=right?-.18f:.18f,roll=0;
  if(java.util.Set.of("shadow_sever","dragon_pierce","godslayer_pierce","dao_corrosion","divine_reveal","sun_pierce","staff_extend").contains(p.skill)){pitch=-1.95f+.55f*ease;yaw=(right?-.30f:.30f)*(1-ease);}
  else if(java.util.Set.of("star_shatter","heaven_split","staff_quake").contains(p.skill)){pitch=-2.65f+2.35f*ease;roll=(right?-.25f:.25f)*(1-ease);}
  else if(java.util.Set.of("chaos_suppress","great_dao_quake","spirit_sweep").contains(p.skill)){pitch=-1.3f;yaw=(right?1:-1)*(-.8f+1.5f*ease);roll=(right?-.3f:.3f);}
  else if(java.util.Set.of("radiant_return","forged_guard","celestial_guard").contains(p.skill)){pitch=-1.65f;yaw=(right?-.65f:.65f);roll=(right?.3f:-.3f);}
  else if(p.skill.equals("thunder_strike"))pitch=-2.1f;
  else if(p.skill.equals("great_fireball")){pitch=-1.55f;yaw=right?-.4f:.4f;roll=right?.12f:-.12f;}
  arm.xRot=arm.xRot*(1-w)+pitch*w;arm.yRot=arm.yRot*(1-w)+yaw*w;arm.zRot=arm.zRot*(1-w)+roll*w;
  other.xRot=other.xRot*(1-w)+(p.skill.equals("great_fireball")?-1.55f:-.65f)*w;other.zRot=other.zRot*(1-w)+(right?-.2f:.2f)*w;
  if(p.skill.equals("great_fireball"))other.yRot=other.yRot*(1-w)+(right?.4f:-.4f)*w;return true;}
 @net.minecraftforge.eventbus.api.SubscribeEvent public static void hand(net.minecraftforge.client.event.RenderHandEvent e){var mc=Minecraft.getInstance();if(mc.player==null||e.getHand()!=net.minecraft.world.InteractionHand.MAIN_HAND)return;var p=POSES.get(mc.player.getUUID());if(p==null)return;float w=weight(p);double age=now()+mc.getFrameTime()-p.start;float u=(float)Math.max(0,Math.min(1,age/15));u=u*u*(3-2*u);
  if(AuthoredWeaponClips.family(p.skill)!=null){double seconds=p.released==0?0:(now()+mc.getFrameTime()-p.released)/20;float blend=authoredWeight(p,seconds);int sign=mc.player.getMainArm()==HumanoidArm.RIGHT?1:-1;var rot=AuthoredWeaponClips.sample(p.skill,"weapon","rotation",seconds);var move=AuthoredWeaponClips.sample(p.skill,"weapon","position",seconds);var pose=e.getPoseStack();
   pose.translate(move[0]/16*sign*blend,move[1]/16*blend,move[2]/16*blend);pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-rot[0]*blend));pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(rot[1]*sign*blend));pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-rot[2]*sign*blend));return;}
  boolean heavy=java.util.Set.of("star_shatter","heaven_split","staff_quake").contains(p.skill);
  boolean sweep=java.util.Set.of("chaos_suppress","great_dao_quake","spirit_sweep").contains(p.skill);
  boolean thrust=java.util.Set.of("shadow_sever","dragon_pierce","godslayer_pierce","dao_corrosion","divine_reveal","sun_pierce").contains(p.skill);
  var pose=e.getPoseStack();pose.translate((sweep?-.25+.5*u:-.06)*w,(heavy?.35-.5*u:.12)*w,(thrust?-.1-.48*Math.sin(Math.PI*u):-.12)*w);
  pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees((heavy?-65+115*u:thrust?-8:-16)*w));if(sweep)pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees((-35+70*u)*w));}
 private static float authoredWeight(Pose p,double seconds){if(p.released==0)return weight(p);double length=AuthoredWeaponClips.duration(p.skill);double in=Math.max(0,Math.min(1,seconds/.045)),out=Math.max(0,Math.min(1,(length-seconds)/.15));return (float)(in*in*(3-2*in)*out*out*(3-2*out));}
 private SkillCastAnimations(){}
}
