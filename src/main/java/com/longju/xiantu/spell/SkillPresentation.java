package com.longju.xiantu.spell;
import com.longju.xiantu.network.*;
import com.longju.xiantu.refining.EquipmentSkillEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import java.util.UUID;
/** Read-only bridge: the authoritative services remain the sole damage owners. */
@Mod.EventBusSubscriber(modid="xiantu")
public final class SkillPresentation {
 public static void emit(ServerPlayer p,UUID tx,String skill,String phase,Vec3 at,Vec3 direction){
  if(p==null||!Double.isFinite(at.lengthSqr())||!Double.isFinite(direction.lengthSqr()))return;
  var packet=new SkillVfxPacket(tx,p.getUUID(),p.getId(),p.level().dimension().location(),skill,phase,at,direction);
  XiantuNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(()->p),packet);
 }
 @SubscribeEvent public static void spell(SpellLifecycleEvent e){
  Vec3 at=e.position;
  if(e.phase==SpellLifecycleEvent.Phase.RELEASE){
   if(e.spellId.equals("fire_bolt"))at=e.caster.getEyePosition();
   else if(e.target!=null&&e.caster.serverLevel().getEntity(e.target) instanceof net.minecraft.world.entity.LivingEntity t)at=t.position();
  }
  emit(e.caster,e.transaction,e.spellId,e.phase.name(),at,e.direction);
 }
 @SubscribeEvent public static void equipment(EquipmentSkillEvent e){emit(e.owner,e.transaction,e.skill,e.phase.name(),e.position,e.direction);}
 private SkillPresentation(){}
}
