package com.longju.xiantu.combat;

import com.longju.xiantu.cultivation.CultivationCapability;
import com.longju.xiantu.entity.*;
import com.longju.xiantu.spell.SpellRules;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.tags.DamageTypeTags;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

/** Native attacks get realm scaling once; every boss hit gets one final soft cap. */
@Mod.EventBusSubscriber(modid="xiantu")
public final class RealmCombat {
 public static int rank(LivingEntity e){if(e instanceof Player p)return p.getCapability(CultivationCapability.DATA).map(d->d.cultivationUnlocked()?d.majorRealm()+1:0).orElse(0);return BeastRealmService.valid(e)?BeastRealmService.realm(e)+1:1;}
 @SubscribeEvent(priority=EventPriority.HIGH) public static void hurt(LivingHurtEvent e){
  if(e.getEntity().level().isClientSide||e.getAmount()<=0||e.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)||!(e.getSource().getEntity() instanceof LivingEntity attacker))return;
  var s=e.getSource();
  // Independent spells, talismans and active weapon skills already apply their own realm term.
  boolean nativePlayer=attacker instanceof Player&&s.is(DamageTypes.PLAYER_ATTACK)&&s.getDirectEntity()==attacker;
  boolean nativeMob=!(attacker instanceof Player)&&BeastRealmService.valid(attacker)&&(s.is(DamageTypes.MOB_ATTACK)||s.is(DamageTypes.MOB_PROJECTILE)||s.is(DamageTypes.THROWN)||s instanceof ElementalDamageSource);
  if(nativePlayer||nativeMob)e.setAmount((float)(e.getAmount()*SpellRules.realmMultiplier(rank(attacker),rank(e.getEntity()))));
 }
 @SubscribeEvent(priority=EventPriority.LOWEST) public static void damage(LivingDamageEvent e){
  if(e.getEntity().level().isClientSide||e.getAmount()<=0||!HostileRealmService.boss(e.getEntity())||e.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY))return;
  double capHealth=e.getEntity().getMaxHealth();var boss=e.getEntity();
  // Extra participants increase endurance, never the per-hit ceiling.
  if(boss.getPersistentData().getBoolean(com.longju.xiantu.worldgen.RuinAdventureService.BOSS)&&BeastRealmService.valid(boss))capHealth=Math.min(capHealth,BossBalance.health(BeastRealmService.realm(boss),boss.getPersistentData().getInt(BeastRealmService.STAGE)));
  e.setAmount(BossBalance.received(e.getAmount(),capHealth));
 }
 private RealmCombat(){}
}
