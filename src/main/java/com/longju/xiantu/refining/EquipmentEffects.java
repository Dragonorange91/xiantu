package com.longju.xiantu.refining;

import com.longju.xiantu.combat.*;
import com.longju.xiantu.cultivation.*;
import com.longju.xiantu.equipment.*;
import com.longju.xiantu.gongfa.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.item.*;
import net.minecraft.world.effect.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Equipped slots only. Ordinary sets require all four real pieces; duplicate rings use the strongest. */
@Mod.EventBusSubscriber(modid="xiantu")
public final class EquipmentEffects {
 public record Piece(String theme,int tier,double scale){}
 public record Loadout(String set,double scale,Map<String,Piece> accessories){
  public double accessory(String id){var p=accessories.get(id);return p==null?0:p.scale;}
  public double set(String id){return set.equals(id)?scale:0;}
  public boolean high(String id){var p=accessories.get(id);return p!=null&&p.tier>=5;}
 }
 public static Loadout read(Player p){
  String theme="";double k=Double.MAX_VALUE;boolean complete=true;int count=0;
  for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET)){
   var s=p.getItemBySlot(slot);var r=RefinedEquipment.recipe(s);
   if(r==null||s.getCount()!=1||!(s.getItem() instanceof ArmorItem armor)||armor.getEquipmentSlot()!=slot||!RefinedEquipment.usable(p,s)||RefinedEquipment.broken(s)){complete=false;continue;}
   if(count++==0)theme=r.theme();else if(!theme.equals(r.theme()))complete=false;k=Math.min(k,WeaponPassives.scale(s));
  }
  var accessories=new LinkedHashMap<String,Piece>();var inv=new AccessoryInventory(p);
  for(int i=0;i<3;i++){var s=inv.getItem(i);var r=RefinedEquipment.recipe(s);if(r==null||!RefinedEquipment.usable(p,s)||s.getCount()!=1||RefinedEquipment.broken(s)||!s.is(i==2?AccessoryMenu.NECKLACES:AccessoryMenu.RINGS))continue;var next=new Piece(r.theme(),r.tier(),WeaponPassives.scale(s));accessories.merge(next.theme,next,(a,b)->a.scale>=b.scale?a:b);}
  return new Loadout(complete&&count==4?theme:"",complete&&count==4?k:0,Map.copyOf(accessories));
 }
 public static double healthBonus(Player p){return healthBonus(read(p));}
 private static double healthBonus(Loadout a){return a.set("沧溟镇岳甲")*.06+a.set("厚载玄衡甲")*.05+a.accessory("定海链")*.05+a.accessory("避水链")*.04+a.accessory("仙灵链")*.08+a.accessory("神灵链")*.10+a.accessory("道灵链")*.12+a.accessory("长生链")*.09+a.accessory("轮回链")*.10+a.accessory("不朽链")*.14;}
 public static double powerBonus(Player p){return powerBonus(read(p));}
 private static double powerBonus(Loadout a){return a.set("坤元藏真甲")*.08+a.accessory("玄灵链")*.04+a.accessory("玄枢戒")*.05+a.accessory("天枢戒")*.07+a.accessory("仙枢戒")*.10+a.accessory("神枢戒")*.12+a.accessory("道枢戒")*.15;}
 public static double attackBonus(Player p){return read(p).set("离照焚星甲")*.05;}
 public static double cultivationBonus(Player p){var a=read(p);return a.accessory("悟道戒")*.04+a.accessory("仙枢戒")*.05+a.accessory("道枢戒")*.07+(a.high("玄枢戒")?a.accessory("玄枢戒")*.03:0);}
 public static double spellExperience(Player p){return read(p).accessory("悟道戒")*.04;}
 public static double controlResistance(Player p){var a=read(p);return QitianSkills.steadfast(p)?1:Math.min(.5,(a.set("太初镇脉甲")*.08+a.accessory("清心戒")*.05+a.accessory("神灵链")*.06+WeaponPassives.controlResistance(p))*(QitianSkills.pieces(p)==4?1.25:1));}
 public static double controlDuration(Player p){var a=read(p);return QitianSkills.steadfast(p)?0:1-Math.min(.5,Math.max(QitianSkills.slowReduction(p),a.set("寒汐锁月甲")*.08+(a.high("清心戒")?a.accessory("清心戒")*.08:0)));}
 public static double windupBonus(Player p){return Math.min(.5,read(p).set("霆渊逐光甲")*.05);}
 public static double recoveryBonus(Player p){var a=read(p);boolean calm=p.getPersistentData().getLong("xiantu_equipment_last_combat")+100<=com.longju.xiantu.spell.SpellService.clock();return a.set("坤元藏真甲")*.04+a.accessory("聚灵戒")*.05+a.accessory("天枢戒")*.03+a.accessory("神枢戒")*.05+a.accessory("长生链")*.04+EquipmentConditions.recoveryBonus(p)+(calm?a.set("芝华回生甲")*.04:0);}
 private static UUID id(String name){return UUID.nameUUIDFromBytes(("xiantu.gear."+name).getBytes(java.nio.charset.StandardCharsets.UTF_8));}
 private static void modifier(Player p,Attribute attr,String name,double value,AttributeModifier.Operation op){var a=p.getAttribute(attr);if(a==null)return;var old=a.getModifier(id(name));if(old!=null&&old.getAmount()==value)return;a.removeModifier(id(name));if(value!=0)a.addTransientModifier(new AttributeModifier(id(name),name,value,op));}
 public static void refresh(Player p){refresh(p,read(p));}
 @SubscribeEvent public static void changed(LivingEquipmentChangeEvent e){
  if(e.getEntity() instanceof ServerPlayer p)refresh(p);
 }
 private static void refresh(Player p,Loadout a){
  modifier(p,Attributes.MAX_HEALTH,"health",healthBonus(a)+QitianSkills.healthBonus(p),AttributeModifier.Operation.MULTIPLY_BASE);
  modifier(p,Attributes.MOVEMENT_SPEED,"speed",a.set("青梧流云甲")*.05+a.set("霆渊逐光甲")*.03+QitianSkills.speedBonus(p),AttributeModifier.Operation.MULTIPLY_TOTAL);
  for(var el:ElementalAttributes.RESISTANCE.keySet()){double value=(el==Element.WATER?a.set("沧溟镇岳甲")*.04+a.set("寒汐锁月甲")*.04:0)+(el==Element.METAL?a.set("太初镇脉甲")*.04:0)+(el==Element.FIRE?a.set("离照焚星甲")*.04:0)+(el!=Element.THUNDER?a.set("厚载玄衡甲")*.02+a.accessory("道灵链")*.03:0);modifier(p,ElementalAttributes.RESISTANCE.get(el).get(),el.id()+"Resistance",value,AttributeModifier.Operation.ADDITION);}
  if(p.getHealth()>p.getMaxHealth())p.setHealth(p.getMaxHealth());
 }
 private static boolean ready(Player p,String key,long now,long cooldown){var n=p.getPersistentData();if(n.getLong("xiantu_gear_"+key)>now)return false;n.putLong("xiantu_gear_"+key,now+cooldown);return true;}
 public static double healingBonus(Player p){var a=read(p);return a.set("芝华回生甲")*.08+a.accessory("仙灵链")*.06;}
 public static double armorProtection(Player p){
  double protection=0;
  for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET)){
   var stack=p.getItemBySlot(slot);var recipe=RefinedEquipment.recipe(stack);
   if(recipe!=null&&stack.getCount()==1&&stack.getItem() instanceof ArmorItem armor
       &&armor.getEquipmentSlot()==slot&&RefinedEquipment.usable(p,stack))
    protection+=ArmorBalance.protection(recipe.tier(),RefinedEquipment.quality(stack));
  }
  return protection;
 }
 public static double soulResistance(Player p){return Math.min(.5,read(p).accessory("养魂戒")*.05);}
 @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e){
  if(e.phase!=TickEvent.Phase.END||!(e.player instanceof ServerPlayer p)||!p.isAlive())return;
  EquipmentConditions.tick(p);var a=read(p);refresh(p,a);var d=GongfaService.data(p);var n=p.getPersistentData();long now=com.longju.xiantu.spell.SpellService.now(p);
  double old=n.getDouble("xiantu_equipment_power_bonus"),base=Math.max(0,d.powerMax()-old-n.getDouble("xiantu_gongfa_power_max_bonus")-n.getDouble("xiantu_physique_power_max_bonus")-n.getDouble("xiantu_pill_power_bonus"));
  double next=d.cultivationUnlocked()?base*powerBonus(a):0;if(Math.abs(old-next)>.0001){d.setPowerMax(Math.max(0,d.powerMax()-old+next));d.setPower(Math.min(d.power(),d.powerMax()));n.putDouble("xiantu_equipment_power_bonus",next);CultivationEvents.sync(p);}
  if(a.accessory("神灵链")==0)n.remove("xiantu_gear_life_guard_until");
  if(a.accessory("道灵链")==0)n.remove("xiantu_gear_dao_guard_until");
  if(a.accessory("不朽链")==0)n.remove("xiantu_gear_immortal_guard_until");
  if(p.tickCount%20!=0)return;
  if(a.accessory("避水链")>0&&p.isInWaterOrBubble())p.setAirSupply(p.getMaxAirSupply());
  if(a.high("玄灵链")&&d.power()<d.powerMax()*.2&&ready(p,"power_rescue",now,900))d.setPower(Math.min(d.powerMax(),d.power()+d.powerMax()*.06*a.accessory("玄灵链")));
  if(a.accessory("长生链")>0&&n.getLong("xiantu_equipment_last_combat")+160<=now){if(n.getLong("xiantu_gear_longlife_until")<=now&&ready(p,"longlife",now,600))n.putLong("xiantu_gear_longlife_until",now+120);if(n.getLong("xiantu_gear_longlife_until")>now)p.heal((float)(p.getMaxHealth()*.005*a.accessory("长生链")));}
 }
 @SubscribeEvent public static void hurt(LivingHurtEvent e){
  if(e.getAmount()<=0||!(e.getEntity() instanceof ServerPlayer p)||e.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY))return;
  var a=read(p);long now=com.longju.xiantu.spell.SpellService.now(p);var n=p.getPersistentData();n.putLong("xiantu_equipment_last_combat",now);
  double reduction=ElementalCombat.attackElement(e.getSource())==Element.PHYSICAL&&!e.getSource().is(DamageTypeTags.BYPASSES_ARMOR)?a.set("归藏曜甲")*.02:0;
  if(e.getSource().getEntity() instanceof com.longju.xiantu.entity.InnerDemonEntity)reduction+=soulResistance(p);
  if(p.getHealth()<p.getMaxHealth()*.25&&a.accessory("道灵链")>0&&ready(p,"dao_guard",now,1200))n.putLong("xiantu_gear_dao_guard_until",now+100);
  if(p.getHealth()<p.getMaxHealth()*.15&&a.accessory("不朽链")>0&&ready(p,"immortal_guard",now,1800))n.putLong("xiantu_gear_immortal_guard_until",now+60);
  if(n.getLong("xiantu_gear_dao_guard_until")>now)reduction=Math.max(reduction,.08*a.accessory("道灵链"));if(n.getLong("xiantu_gear_immortal_guard_until")>now)reduction=Math.max(reduction,.12*a.accessory("不朽链"));
  if(a.high("定海链")&&ElementalCombat.attackElement(e.getSource())==Element.WATER&&ready(p,"water_guard",now,160))reduction=Math.max(reduction,.04*a.accessory("定海链"));
  double protection=e.getSource().is(DamageTypeTags.BYPASSES_ARMOR)?0:armorProtection(p);
  e.setAmount((float)(e.getAmount()*(1-protection)*(1-Math.min(.5,reduction))));
 }
 @SubscribeEvent(priority=EventPriority.LOWEST) public static void damage(LivingDamageEvent e){
  if(e.getAmount()<=0||e.getEntity().level().isClientSide)return;
  if(e.getSource().getEntity() instanceof ServerPlayer owner)owner.getPersistentData().putLong("xiantu_equipment_last_combat",com.longju.xiantu.spell.SpellService.now(owner));
  if(!(e.getEntity() instanceof ServerPlayer p)||e.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY))return;var a=read(p);long now=com.longju.xiantu.spell.SpellService.now(p);
  if(a.accessory("神灵链")>0&&p.getPersistentData().getLong("xiantu_gear_life_guard_until")>now){e.setAmount(0);return;}
  if(e.getAmount()>=p.getHealth()&&a.accessory("神灵链")>0&&ready(p,"life_guard",now,3600)){e.setAmount(Math.max(0,p.getHealth()-1));p.getPersistentData().putLong("xiantu_gear_life_guard_until",now+20);}
  if(e.getSource().getEntity() instanceof LivingEntity attacker&&e.getSource().getDirectEntity()==attacker&&!e.getSource().is(DamageTypes.THORNS)&&!e.getSource().is(DamageTypeTags.BYPASSES_ARMOR)&&a.set("归藏曜甲")>0&&ready(p,"reflection",now,20)){
   double reflected=e.getAmount()*.05*a.scale;attacker.hurt(p.damageSources().thorns(p),(float)reflected);
  }
 }
 @SubscribeEvent public static void fall(LivingFallEvent e){if(e.getEntity() instanceof ServerPlayer p)e.setDamageMultiplier((float)(e.getDamageMultiplier()*(1-Math.min(.8,read(p).set("青梧流云甲")*.2))));}
 private EquipmentEffects(){}
}
