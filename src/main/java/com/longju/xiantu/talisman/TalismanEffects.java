package com.longju.xiantu.talisman;

import com.longju.xiantu.alchemy.MedicineService;
import com.longju.xiantu.combat.*;
import com.longju.xiantu.entity.*;
import com.longju.xiantu.gongfa.*;
import com.longju.xiantu.spell.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.minecraft.tags.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Server-owned preparation, consumption and bounded transient effects. Visual packets never deal damage. */
@Mod.EventBusSubscriber(modid="xiantu")
public final class TalismanEffects {
 private static final String KEY="xiantu_talisman_effects";
 private static final UUID SPEED=UUID.fromString("c59c7609-138e-418e-a0ac-85466940b352");
 private record Cast(UUID tx,TalismanCatalog.Definition def,InteractionHand hand,ItemStack item,int tier,int quality,long finish,Vec3 start,Vec3 direction,UUID target,Vec3 destination,net.minecraft.resources.ResourceKey<Level> dimension){}
 private static final class Missile {Cast cast;UUID owner;ServerLevel level;Vec3 pos;int age,fuse=-1;Set<UUID> hits=new HashSet<>();Missile(ServerPlayer p,Cast c){cast=c;owner=p.getUUID();level=p.serverLevel();pos=p.getEyePosition();}}
 private record Trap(UUID owner,ServerLevel level,Vec3 center,long until){}
 private static final Map<UUID,Cast> CASTS=new HashMap<>();
 private static final List<Missile> MISSILES=new ArrayList<>();
 private static final List<Trap> TRAPS=new ArrayList<>();
 private static final Set<String> ATTACK=Set.of("fireball","thunder","sword_qi","explosion"), CONTROL=Set.of("soul_seal","spirit_lock","demon_seal","trap"), TELEPORT=Set.of("earth_escape","blink","long_return");
 private static long now(){return SpellService.clock();}
 private static CompoundTag state(LivingEntity e){var p=e.getPersistentData();if(!p.contains(KEY))p.put(KEY,new CompoundTag());return p.getCompound(KEY);}
 private static boolean active(LivingEntity e,String key){return state(e).getLong(key+"_until")>now();}
 private static double effect(LivingEntity e,String key){return active(e,key)?state(e).getDouble(key):0;}
 private static void effect(LivingEntity e,String key,double value,int ticks){var n=state(e);n.putDouble(key,value);n.putLong(key+"_until",now()+Math.max(1,ticks));}
 public static boolean locked(LivingEntity e){return active(e,"lock");}
 public static boolean casting(ServerPlayer p){return CASTS.containsKey(p.getUUID());}
 public static void breakInvisibility(ServerPlayer p){state(p).remove("invisibility_until");}
 public static double defense(net.minecraft.world.entity.player.Player p){return effect(p,"diamond");}
 private static String group(String id){return ATTACK.contains(id)?"attack":CONTROL.contains(id)?"seal":Set.of("healing","cleanse").contains(id)?"heal":id.equals("detox")?"detox":id;}
 private static LivingEntity target(ServerPlayer p,double range,Vec3 dir){Vec3 a=p.getEyePosition(),b=SpellService.clip(p.serverLevel(),p,a,a.add(dir.scale(range)));return p.serverLevel().getEntitiesOfClass(LivingEntity.class,new AABB(a,b).inflate(.5),e->SpellService.enemy(p,e)&&!(e instanceof net.minecraft.world.entity.player.Player)).stream().filter(e->e.getBoundingBox().inflate(.3).clip(a,b).isPresent()).min(Comparator.comparingDouble(p::distanceToSqr)).orElse(null);}
 public static String begin(ServerPlayer p,InteractionHand hand,TalismanCatalog.Definition d){
  var item=p.getItemInHand(hand);int tier=TalismanItems.tier(item);String id=d.id();
  if(!p.isAlive()||p.isSpectator()||p.containerMenu!=p.inventoryMenu||casting(p)||SpellService.casting(p)||com.longju.xiantu.refining.QitianSkills.casting(p)||p.isUsingItem())return "请结束当前动作后使用符箓";
  if(!GongfaService.data(p).cultivationUnlocked()||tier<d.minimum()||GongfaService.data(p).majorRealm()<TalismanRules.realm(tier))return "修为不足以使用此符";
  if(locked(p))return "灵力被封锁";
  if(state(p).getLong("cd_"+id)>now()||state(p).getLong("group_"+group(id))>now())return "符箓冷却中";
  if(Set.of("healing","cleanse","detox").contains(id)&&GongfaService.data(p).medicine().cooldown(group(id),now())>0)return "同类丹药或符箓冷却中";
  Vec3 dir=p.getLookAngle(),destination=null;LivingEntity target=null;
  if(id.equals("thunder")||CONTROL.contains(id)&&!id.equals("trap")){
   target=target(p,id.equals("thunder")?24:12,dir);if(target==null||!p.hasLineOfSight(target))return "没有可见的有效目标";
   if(CONTROL.contains(id)){
    if(!BeastRealmService.isBeast(target)&&!(target instanceof InnerDemonEntity)&&!(target instanceof CompletionLandEntity))return "封印符只能作用于妖兽或心魔";
    if(!TalismanControlRules.allowed(GongfaEvents.target(target).realmRank(),tier,controlCount(target)))return "目标境界过高，无法封印";
    if(controlCount(target)>=2)return "目标暂时免疫连续控制";
    if(GongfaEvents.target(target).boss()&&(!(target instanceof CompletionLandEntity boss)||!boss.weaponInterruptible()))return "只能在首领可打断的蓄势窗口使用";
   }
  }
  if(TELEPORT.contains(id)){
   if(p.isPassenger()||p.isVehicle()||p.isOnPortalCooldown())return "当前不能传送";
   if(id.equals("long_return")){String error=TalismanReturn.begin(p,item,tier);if(!error.isEmpty())return error;}else{destination=destination(p,id,tier,dir);if(destination==null)return "没有已加载且安全的落点";}
  }
  if(id.equals("healing")&&p.getHealth()>=p.getMaxHealth())return "生命已满";
  int windup=id.equals("thunder")?16:id.equals("earth_escape")?20:id.equals("blink")?10:id.equals("long_return")?100:8;
  var cast=new Cast(UUID.randomUUID(),d,hand,item.copy(),tier,TalismanItems.quality(item),now()+windup,p.position(),dir,target==null?null:target.getUUID(),destination,p.level().dimension());
  GongfaService.cancel(p,"使用符箓");SpellService.cancel(p);com.longju.xiantu.alchemy.CrossDimensionMedicine.cancel(p);state(p).remove("invisibility_until");CASTS.put(p.getUUID(),cast);visual(p,cast,"START",p.position());return "";
 }
 private static boolean valid(ServerPlayer p,Cast c){return p.isAlive()&&!p.isSpectator()&&p.level().dimension().equals(c.dimension)&&p.containerMenu==p.inventoryMenu&&ItemStack.isSameItemSameTags(p.getItemInHand(c.hand),c.item)&&p.getItemInHand(c.hand).getCount()>0&&(!TELEPORT.contains(c.def.id())||p.position().distanceToSqr(c.start)<=.04)&&!locked(p);}
 private static void visual(ServerPlayer p,Cast c,String phase,Vec3 at){SkillPresentation.emit(p,c.tx,"talisman_"+c.def.id(),phase,at,c.direction);}
 private static void release(ServerPlayer p,Cast c){
  if(!valid(p,c))return;var d=c.def;String id=d.id();var n=state(p);double q=TalismanRules.QUALITY[c.quality],a=Math.pow(1.6,c.tier-d.minimum());
  LivingEntity t=c.target!=null&&p.serverLevel().getEntity(c.target) instanceof LivingEntity e?e:null;
  if(c.target!=null&&(t==null||!SpellService.enemy(p,t)||!p.hasLineOfSight(t)||p.distanceToSqr(t)>(id.equals("thunder")?576:144)))return;
  if(CONTROL.contains(id)&&t!=null&&(!TalismanControlRules.allowed(GongfaEvents.target(t).realmRank(),c.tier,controlCount(t))||GongfaEvents.target(t).boss()&&(!(t instanceof CompletionLandEntity boss)||!boss.weaponInterruptible())))return;
  if(TELEPORT.contains(id)){var dest=destination(p,id,c.tier,c.direction);if(dest==null||!id.equals("long_return")&&c.destination.distanceToSqr(dest)>4)return;
   var event=new net.minecraftforge.event.entity.EntityTeleportEvent.EnderEntity(p,dest.x,dest.y,dest.z);if(net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event))return;
   dest=new Vec3(event.getTargetX(),event.getTargetY(),event.getTargetZ());if(!safe(p,dest))return;
   p.teleportTo(p.serverLevel(),dest.x,dest.y,dest.z,p.getYRot(),p.getXRot());p.fallDistance=0;n.putLong("safe_fall_until",now()+20);
  }
  // A release is consumed exactly once; canceled/invalid preparation never reaches this boundary.
  p.getItemInHand(c.hand).shrink(1);n.putLong("cd_"+id,now()+d.cooldown()*20L);n.putLong("group_"+group(id),now()+(ATTACK.contains(id)?40:CONTROL.contains(id)?d.cooldown()*20L:0));
  if(Set.of("healing","cleanse","detox").contains(id))GongfaService.data(p).medicine().cooldown(group(id),now(),d.cooldown());
  n.remove("invisibility_until");visual(p,c,"RELEASE",t==null?p.position():t.position());
  if(ATTACK.contains(id)){if(id.equals("thunder"))hit(p,c,t,12*a*q);else {if(MISSILES.size()>=128)MISSILES.remove(0);MISSILES.add(new Missile(p,c));}GongfaService.markCombat(p);return;}
  switch(id){
   case "diamond"->effect(p,"diamond",Math.min(.30,.20+.02*(c.tier-1)),(int)(160*q));
   case "ward"->{double shield=10*a*q;if(shield>effect(p,"ward"))effect(p,"ward",shield,200);}
   case "reflection"->{effect(p,"reflection",.20,(int)(120*q));n.putDouble("reflect_cap",4*a);}
   case "lightbody"->effect(p,"lightbody",.25,(int)(400*q));
   case "speed"->effect(p,"speed",Math.min(.40,.25+.02*(c.tier-2)),(int)(240*q));
   case "invisibility"->effect(p,"invisibility",1,(int)(20*Math.min(25,(8+2*(c.tier-3))*q)));
   case "concealment"->effect(p,"concealment",1,(int)(400*q));
   case "healing"->{effect(p,"healing",p.getMaxHealth()*Math.min(.25,(.12+.02*(c.tier-1))*q),121);n.putInt("heal_ticks",0);n.putDouble("heal_paid",0);}
   case "detox"->{p.removeEffect(MobEffects.POISON);if(c.tier>=3)GongfaService.data(p).medicine().clearEffect("injury_miasma");}
   case "cleanse"->{int left=2;if(p.hasEffect(MobEffects.POISON)){p.removeEffect(MobEffects.POISON);left--;}for(String injury:List.of("miasma","bleed"))if(left>0&&MedicineService.injured(p,injury)){GongfaService.data(p).medicine().clearEffect("injury_"+injury);left--;}if(c.tier>=5)GongfaService.data(p).medicine().clearEffect("injury_meridian");}
   case "soul_seal","spirit_lock","demon_seal"->control(p,c,t,q);
   case "trap"->{var at=SpellService.clip(p.serverLevel(),p,p.getEyePosition(),p.getEyePosition().add(c.direction.scale(12)));if(TRAPS.size()>=64)TRAPS.remove(0);TRAPS.add(new Trap(p.getUUID(),p.serverLevel(),at,now()+(long)(80*q)));}
   default->{}
  }
 }
 private static int controlCount(LivingEntity t){var n=state(t);return n.getLong("control_window")>now()?n.getInt("control_count"):0;}
 private static void control(ServerPlayer p,Cast c,LivingEntity t,double q){if(t==null)return;var info=GongfaEvents.target(t);if(info.boss()){if(t instanceof CompletionLandEntity boss)boss.talismanInterrupt();return;}
  int count=controlCount(t);double factor=TalismanControlRules.duration(info.realmRank(),c.tier,info.elite(),count);if(factor<=0)return;var n=state(t);n.putInt("control_count",count+1);
  if(c.def.id().equals("soul_seal"))effect(t,"soul_seal",.20,(int)(80*q*factor));
  else {effect(t,"lock",1,(int)(20*Math.min(c.def.id().equals("demon_seal")?6:4,(c.def.id().equals("demon_seal")?4:2.5)*q)*factor));if(t instanceof Mob mob)mob.getNavigation().stop();if(c.def.id().equals("demon_seal")&&t instanceof CompletionLandEntity beast)beast.weaponInterrupt();}
  n.putLong("control_window",Math.max(n.getLong("soul_seal_until"),n.getLong("lock_until"))+200);
 }
 private static void hit(ServerPlayer p,Cast c,LivingEntity t,double damage){if(t==null||!SpellService.enemy(p,t))return;var info=GongfaEvents.target(t);damage*=SpellRules.realmMultiplier(GongfaService.data(p).majorRealm()+1,info.realmRank());
  Element element=switch(c.def.element()){case "fire"->Element.FIRE;case "thunder"->Element.THUNDER;case "metal"->Element.METAL;default->Element.PHYSICAL;};
  if(t.hurt(new ElementalDamageSource(p.damageSources().indirectMagic(p,p),element),(float)damage))visual(p,c,"HIT",t.position());
 }
 private static boolean safe(ServerPlayer p,Vec3 at){var l=p.serverLevel();var pos=BlockPos.containing(at);if(!l.hasChunkAt(pos)||!l.hasChunkAt(pos.above())||!l.getWorldBorder().isWithinBounds(pos)||!l.noCollision(p,p.getBoundingBox().move(at.subtract(p.position()))))return false;
  for(var b:List.of(pos,pos.above(),pos.below())){var s=l.getBlockState(b);if(!l.getFluidState(b).isEmpty()||s.is(BlockTags.FIRE)||s.is(net.minecraft.world.level.block.Blocks.MAGMA_BLOCK))return false;}return !l.getBlockState(pos.below()).getCollisionShape(l,pos.below()).isEmpty();
 }
 private static Vec3 destination(ServerPlayer p,String id,int tier,Vec3 direction){
  if(id.equals("long_return")){var pos=TalismanReturn.destination(p);if(pos==null)return null;
   for(int dy=0;dy<=1;dy++)for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++){var at=Vec3.atBottomCenterOf(pos.offset(x,dy,z));if(safe(p,at)&&ruinSafe(p,p.position(),at))return at;}return null;
  }
  double range=id.equals("blink")?Math.min(18,8+2*(tier-3)):Math.min(20,8+2*(tier-2));var dir=direction.multiply(1,0,1).normalize();Vec3 last=null;var stones=new HashSet<BlockPos>();
  for(double d=.25;d<=range;d+=.25){var at=p.position().add(dir.scale(d));var pos=BlockPos.containing(at);if(!p.serverLevel().hasChunkAt(pos)||!p.serverLevel().getWorldBorder().isWithinBounds(pos)||!ruinSafe(p,p.position(),at))break;
   if(!p.serverLevel().noCollision(p,p.getBoundingBox().move(at.subtract(p.position())))){
    if(id.equals("blink"))break;boolean allowed=true;
    for(BlockPos b:BlockPos.betweenClosed(pos,pos.above())){var s=p.serverLevel().getBlockState(b);if(s.getCollisionShape(p.serverLevel(),b).isEmpty())continue;if(p.serverLevel().getBlockEntity(b)!=null||!(s.is(BlockTags.BASE_STONE_OVERWORLD)||s.is(net.minecraft.world.level.block.Blocks.DIRT)||s.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)||s.is(net.minecraft.world.level.block.Blocks.COARSE_DIRT)))allowed=false;else stones.add(b.immutable());}
    if(!allowed||stones.size()>4)break; // At most two columns, each two body cells.
   }else if(d>=2&&safe(p,at))last=at;
  }return last;
 }
 private static boolean ruinSafe(ServerPlayer p,Vec3 from,Vec3 to){var ledger=com.longju.xiantu.worldgen.RuinAdventureData.get(p.serverLevel());var entries=new HashSet<>(ledger.at(BlockPos.containing(from)));entries.addAll(ledger.at(BlockPos.containing(to)));for(var i:entries)if(i.boss()&&!i.defeated()&&(i.arena().contains(from)!=i.arena().contains(to)||i.arena().contains(to)&&!com.longju.xiantu.worldgen.RuinAdventureService.legal(p,i)))return false;return true;}
 @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event){if(event.phase!=TickEvent.Phase.END)return;var server=net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();if(server==null)return;
  for(var p:server.getPlayerList().getPlayers()){var c=CASTS.get(p.getUUID());if(c!=null){TalismanReturn.tick(p);if(!valid(p,c))cancel(p);else if(now()>=c.finish){CASTS.remove(p.getUUID());try{release(p,c);}finally{TalismanReturn.cancel(p);}}}
   var n=state(p);if(active(p,"healing")){int t=n.getInt("heal_ticks")+1;double due=n.getDouble("healing")*Math.min(120,t)/120.;p.heal((float)Math.max(0,due-n.getDouble("heal_paid")));n.putDouble("heal_paid",due);n.putInt("heal_ticks",t);}
   var attribute=p.getAttribute(Attributes.MOVEMENT_SPEED);if(attribute!=null){attribute.removeModifier(SPEED);double bonus=effect(p,"speed");if(bonus>0){double base=attribute.getValue();double nominal=attribute.getBaseValue();double allowed=base<=0?0:Math.max(0,nominal*1.40/base-1);if(allowed>0)attribute.addTransientModifier(new AttributeModifier(SPEED,"神行符",Math.min(allowed,bonus),AttributeModifier.Operation.MULTIPLY_TOTAL));}}
   if(active(p,"invisibility"))p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY,2,0,false,false,false));
  }
  for(int i=MISSILES.size()-1;i>=0;i--){var m=MISSILES.get(i);var p=server.getPlayerList().getPlayer(m.owner);if(p==null||!p.isAlive()||p.level()!=m.level){MISSILES.remove(i);continue;}var c=m.cast;String id=c.def.id();double a=Math.pow(1.6,c.tier-c.def.minimum())*TalismanRules.QUALITY[c.quality];
   if(m.fuse>=0){if(++m.fuse>=20){for(var t:m.level.getEntitiesOfClass(LivingEntity.class,new AABB(m.pos,m.pos).inflate(3),e->SpellService.enemy(p,e)&&e.distanceToSqr(m.pos)<=9))if(SpellService.clip(m.level,p,m.pos,t.getEyePosition()).distanceToSqr(t.getEyePosition())<.01)hit(p,c,t,6*a*(1-.5*Math.min(1,t.position().distanceTo(m.pos)/3)));visual(p,c,"HIT",m.pos);MISSILES.remove(i);}continue;}
   Vec3 end=SpellService.clip(m.level,p,m.pos,m.pos.add(c.direction.scale(.5)));boolean done=end.distanceToSqr(m.pos.add(c.direction.scale(.5)))>.001;
   for(var t:m.level.getEntitiesOfClass(LivingEntity.class,new AABB(m.pos,end).inflate(.25),e->SpellService.enemy(p,e)&&!m.hits.contains(e.getUUID())).stream().sorted(Comparator.comparingDouble(e->e.distanceToSqr(m.pos))).toList())if(t.getBoundingBox().inflate(.25).clip(m.pos,end).isPresent()){m.hits.add(t.getUUID());if(!id.equals("explosion"))hit(p,c,t,(id.equals("sword_qi")?10:8)*a);if(!id.equals("sword_qi")||m.hits.size()>=2){done=true;break;}}
   m.pos=end;m.age++;if(m.age%2==0)visual(p,c,"FLIGHT",end);if(done||m.age>=40){if(id.equals("explosion"))m.fuse=0;else{visual(p,c,"END",end);MISSILES.remove(i);}}
  }
  TRAPS.removeIf(t->t.until<=now()||server.getPlayerList().getPlayer(t.owner)==null);for(var t:TRAPS){var owner=server.getPlayerList().getPlayer(t.owner);for(var mob:t.level.getEntitiesOfClass(Mob.class,new AABB(t.center,t.center).inflate(3),m->m.distanceToSqr(t.center)<=9&&SpellService.enemy(owner,m)&&!GongfaEvents.target(m).boss()))com.longju.xiantu.refining.WeaponConditions.applySlow(mob,.5,2);}
 }
 private static void cancel(ServerPlayer p){TalismanReturn.cancel(p);var c=CASTS.remove(p.getUUID());if(c!=null)visual(p,c,"CANCEL",p.position());}
 @SubscribeEvent(priority=EventPriority.LOW) public static void hurt(LivingHurtEvent e){var living=e.getEntity();if(living.level().isClientSide)return;
  if(e.getSource().getEntity() instanceof LivingEntity attacker&&(e.getSource().getDirectEntity()!=attacker||e.getSource() instanceof ElementalDamageSource)&&effect(attacker,"soul_seal")>0)e.setAmount(e.getAmount()*.8f);
  if(living instanceof ServerPlayer p&&e.getAmount()>0){cancel(p);state(p).remove("invisibility_until");if(e.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY))return;double shield=effect(p,"ward"),used=Math.min(e.getAmount(),shield);if(used>0){state(p).putDouble("ward",shield-used);e.setAmount((float)(e.getAmount()-used));}}
 }
 @SubscribeEvent(priority=EventPriority.LOWEST) public static void damage(LivingDamageEvent e){if(!(e.getEntity() instanceof ServerPlayer p)||e.getAmount()<=0||e.getSource().is(net.minecraft.world.damagesource.DamageTypes.THORNS)||e.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY))return;
  if(effect(p,"reflection")>0&&e.getSource().getEntity() instanceof LivingEntity a&&e.getSource().getDirectEntity()==a){var n=state(p);long second=now()/20;if(n.getLong("reflect_second")!=second){n.putLong("reflect_second",second);n.putDouble("reflect_spent",0);}double amount=Math.min(e.getAmount()*.2,Math.max(0,n.getDouble("reflect_cap")-n.getDouble("reflect_spent")));n.putDouble("reflect_spent",n.getDouble("reflect_spent")+amount);if(amount>0)a.hurt(p.damageSources().thorns(p),(float)amount);}}
 @SubscribeEvent public static void attack(AttackEntityEvent e){if(e.getEntity() instanceof ServerPlayer p){cancel(p);state(p).remove("invisibility_until");}}
 @SubscribeEvent public static void jump(LivingEvent.LivingJumpEvent e){if(effect(e.getEntity(),"lightbody")>0)e.getEntity().setDeltaMovement(e.getEntity().getDeltaMovement().multiply(1,Math.sqrt(1.25),1));}
 @SubscribeEvent public static void fall(LivingFallEvent e){if(active(e.getEntity(),"safe_fall"))e.setCanceled(true);else if(effect(e.getEntity(),"lightbody")>0)e.setDamageMultiplier(e.getDamageMultiplier()*.5f);}
 @SubscribeEvent public static void target(LivingChangeTargetEvent e){if(e.getNewTarget() instanceof ServerPlayer p&&e.getEntity() instanceof Mob mob&&BeastRealmService.isBeast(mob)&&mob.getTarget()!=p&&effect(p,"concealment")>0){var a=mob.getAttribute(Attributes.FOLLOW_RANGE);double range=a==null?16:a.getValue();if(mob.distanceToSqr(p)>Math.max(4,range*range/4))e.setNewTarget(null);}}
 private static void clear(ServerPlayer p){cancel(p);var n=state(p);for(var key:Set.copyOf(n.getAllKeys()))if(!key.startsWith("cd_")&&!key.startsWith("group_"))n.remove(key);var a=p.getAttribute(Attributes.MOVEMENT_SPEED);if(a!=null)a.removeModifier(SPEED);MISSILES.removeIf(m->m.owner.equals(p.getUUID()));TRAPS.removeIf(t->t.owner.equals(p.getUUID()));}
 @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){if(e.getEntity() instanceof ServerPlayer p)clear(p);}
 @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e){if(e.getEntity() instanceof ServerPlayer p)clear(p);}
 @SubscribeEvent public static void death(LivingDeathEvent e){if(e.getEntity() instanceof ServerPlayer p)clear(p);}
 @SubscribeEvent public static void clone(PlayerEvent.Clone e){var old=state(e.getOriginal());var n=state(e.getEntity());for(var key:old.getAllKeys())if(key.startsWith("cd_")||key.startsWith("group_"))n.putLong(key,old.getLong(key));}
 @SubscribeEvent public static void stopped(ServerStoppedEvent e){CASTS.clear();MISSILES.clear();TRAPS.clear();TalismanReturn.clear();}
 private TalismanEffects(){}
}
