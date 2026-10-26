package com.longju.xiantu.spell;

import com.longju.xiantu.cultivation.*;
import com.longju.xiantu.gongfa.*;
import com.longju.xiantu.combat.*;
import com.longju.xiantu.network.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.DamageTypeTags;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import java.util.*;

/** Server-owned casts, payment, cooldowns and effects. No visual callback authorizes gameplay. */
@Mod.EventBusSubscriber(modid="xiantu")
public final class SpellService {
 private record Cast(UUID transaction,SpellCatalog.Spell spell,int slot,long finish,Vec3 start,Vec3 direction,UUID target,net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension){}
 private record Flight(UUID transaction,UUID owner,SpellCatalog.Spell spell,ServerLevel level,Vec3 direction,Set<UUID> hits,int age,Vec3 position){}
 private static final Map<UUID,Cast> CASTS=new HashMap<>();
 private static final Map<UUID,Long> REQUESTS=new HashMap<>();
 private static final List<Flight> FLIGHTS=new ArrayList<>();
 public static long clock(){var s=net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();return s==null?0:s.overworld().getGameTime();}
 public static SpellData data(Player p){return p.getCapability(CultivationCapability.DATA).orElseThrow(IllegalStateException::new).spells();}
 public static long now(ServerPlayer p){return p.getServer().overworld().getGameTime();}
 public static boolean casting(ServerPlayer p){return CASTS.containsKey(p.getUUID())||GreatFireballService.casting(p);}
 public static String denial(ServerPlayer p,SpellCatalog.Spell s,boolean learned){
  if(com.longju.xiantu.talisman.TalismanEffects.locked(p))return "灵力被封锁";
  if(s==null||!p.isAlive()||p.isSpectator()||p instanceof net.minecraftforge.common.util.FakePlayer)return "当前不能施法";
  if(!SpellCatalog.enabled(s))return "此法术的生存来源尚未开放";
  var d=GongfaService.data(p);if(!d.cultivationUnlocked()||d.majorRealm()<s.realm())return "修为境界不足";
  var method=GongfaService.active(d);if(method==null||method.maxRealm()<s.realm())return "当前主功法的最高境界不足";
  if(learned&&!data(p).knows(s.id()))return "尚未学会此法术";
  if(InnerDemonService.active(p))return "心魔挑战中不能使用独立法术";return "";
 }
 public static void sync(ServerPlayer p){var state=data(p).save();var reasons=new net.minecraft.nbt.CompoundTag();var costs=new net.minecraft.nbt.CompoundTag();var damages=new net.minecraft.nbt.CompoundTag();for(var s:SpellCatalog.ALL){reasons.putString(s.id(),denial(p,s,true));costs.putLong(s.id(),s.type().equals("charged")?(long)Math.ceil(cost(p,s)*GreatFireballRules.costFraction(0)):cost(p,s));damages.putDouble(s.id(),damagePreview(p,s));}state.put("Eligibility",reasons);state.put("Costs",costs);state.put("Damages",damages);state.putDouble("PowerMilli",GongfaService.data(p).power()*1000);XiantuNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new SpellStatePacket(state,now(p),GreatFireballService.casting(p)?"great_fireball":CASTS.containsKey(p.getUUID())?CASTS.get(p.getUUID()).spell.id():""));}
 public static long cost(ServerPlayer p,SpellCatalog.Spell s){return Math.max(0,(long)Math.ceil(SpellRules.cost(GongfaService.data(p).powerMax(),s.cost(),data(p).mastery(s.id()))*(1-com.longju.xiantu.refining.EquipmentConditions.costReduction(p,s))));}
 private static void notify(ServerPlayer p,SpellCatalog.Spell s,UUID transaction,LivingEntity target,SpellLifecycleEvent.Phase phase,Vec3 at,Vec3 direction){net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new SpellLifecycleEvent(p,s.id(),transaction,target==null?null:target.getUUID(),phase,at,direction));}
 private static LivingEntity target(ServerPlayer p,SpellCatalog.Spell s,Vec3 direction){
  Vec3 a=p.getEyePosition(),b=a.add(direction.scale(s.range()));b=clip(p.serverLevel(),p,a,b);
  final Vec3 end=b;
  return p.serverLevel().getEntitiesOfClass(LivingEntity.class,new AABB(a,b).inflate(1),e->enemy(p,e)) .stream().filter(e->e.getBoundingBox().inflate(.25).clip(a,end).isPresent()).min(Comparator.comparingDouble(p::distanceToSqr)).orElse(null);
 }
 public static boolean enemy(ServerPlayer p,LivingEntity e){
  if(e==p||!e.isAlive()||e.isInvulnerable()||e.isAlliedTo(p)||p.isAlliedTo(e)||e instanceof net.minecraft.world.entity.decoration.ArmorStand)return false;
  if(e instanceof Player other)return !other.isCreative()&&!other.isSpectator()&&p.getServer().isPvpAllowed()&&p.canHarmPlayer(other);
  if(e instanceof TamableAnimal pet&&pet.getOwnerUUID()!=null)return !pet.isOwnedBy(p)&&!(pet.getOwner()!=null&&p.isAlliedTo(pet.getOwner()));
  return true;
 }
 public static Vec3 clip(ServerLevel l,Entity source,Vec3 a,Vec3 b){var hit=l.clip(new ClipContext(a,b,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,source));return hit.getType()==HitResult.Type.MISS?b:hit.getLocation();}
 public static Vec3 safeDestination(ServerPlayer p,double range,Vec3 dir){
  Vec3 start=p.position(),last=start;for(double d=Math.min(.25,range);d<=range&&d>0;d=Math.min(range,d+.25)){var at=start.add(dir.scale(d));var pos=BlockPos.containing(at);if(!p.serverLevel().hasChunkAt(pos)||!p.serverLevel().getWorldBorder().isWithinBounds(p.getBoundingBox().move(at.subtract(start)))||!p.serverLevel().noCollision(p,p.getBoundingBox().move(at.subtract(start))))break;
   var ledger=com.longju.xiantu.worldgen.RuinAdventureData.get(p.serverLevel());var nearby=new HashSet<>(ledger.at(BlockPos.containing(last)));nearby.addAll(ledger.at(pos));
   boolean forbidden=false;for(var i:nearby)if(i.boss()&&!i.defeated()&&(i.arena().contains(last)!=i.arena().contains(at)||i.arena().contains(at)&&!com.longju.xiantu.worldgen.RuinAdventureService.legal(p,i)))forbidden=true;
   if(forbidden)break;last=at;if(d>=range)break;
  }return last;
 }
 public static String begin(ServerPlayer p,int slot){
  long now=now(p);if(slot<0||slot>=4||p.containerMenu!=p.inventoryMenu)return "请关闭界面后施法";
  var selected=SpellCatalog.find(data(p).slot(slot));
  // Recall is immediate and free, including while riding or after eligibility changes.
  if(selected!=null&&selected.type().equals("mount")&&SpiritSwordEntity.recall(p)){cancel(p);sync(p);return "";}
  if(com.longju.xiantu.talisman.TalismanEffects.casting(p)||casting(p)||com.longju.xiantu.refining.QitianSkills.casting(p)||p.isUsingItem()||REQUESTS.getOrDefault(p.getUUID(),Long.MIN_VALUE)>now)return "正在施法或施法间隔中";
  var s=SpellCatalog.find(data(p).slot(slot));String error=denial(p,s,true);if(!error.isEmpty())return error;
  if(s.type().equals("charged"))return "请长按对应法术键凝聚，松开发射";
  if(data(p).cooldown(s.id(),now)>0)return "法术冷却中";
  if(GongfaService.data(p).power()*1000<cost(p,s))return "灵力不足";
  if(s.type().equals("mount")&&(p.isPassenger()||SpiritSwordEntity.placement(p)==null))return "请在前方有落脚空间的地面召唤灵剑";
  var dir=p.getLookAngle();var target=target(p,s,dir);
  if(s.type().equals("target")&&target==null)return "没有可见的合法目标";
  com.longju.xiantu.talisman.TalismanEffects.breakInvisibility(p);REQUESTS.put(p.getUUID(),now+4);GongfaService.cancel(p,"发动法术");if(!s.type().equals("mount"))GongfaService.markCombat(p);com.longju.xiantu.alchemy.CrossDimensionMedicine.cancel(p);
  var cast=new Cast(UUID.randomUUID(),s,slot,now+Math.max(0,(int)Math.ceil(s.windup()*(1-(com.longju.xiantu.refining.EquipmentEffects.windupBonus(p)+com.longju.xiantu.refining.EquipmentConditions.windupBonus(p)+com.longju.xiantu.refining.WeaponConditions.windupBonus(p))))),p.position(),dir,target==null?null:target.getUUID(),p.level().dimension());CASTS.put(p.getUUID(),cast);com.longju.xiantu.refining.EquipmentConditions.began(p);com.longju.xiantu.refining.WeaponConditions.spellBegan(p);notify(p,s,cast.transaction,target,SpellLifecycleEvent.Phase.WINDUP,p.position(),dir);sync(p);return "";
 }
 public static void cancel(ServerPlayer p){GreatFireballService.cancel(p);var cast=CASTS.remove(p.getUUID());if(cast!=null){notify(p,cast.spell,cast.transaction,null,SpellLifecycleEvent.Phase.CANCEL,p.position(),cast.direction);sync(p);}}
 private static void release(ServerPlayer p,Cast c){
  var s=c.spell;String denial=denial(p,s,true);long now=now(p);var d=GongfaService.data(p);
  if(!denial.isEmpty()||!data(p).slot(c.slot).equals(s.id())||data(p).cooldown(s.id(),now)>0)return;
  LivingEntity target=c.target!=null&&p.serverLevel().getEntity(c.target) instanceof LivingEntity e?e:null;
  if(c.target!=null&&(target==null||!enemy(p,target)||p.distanceToSqr(target)>s.range()*s.range()||!p.hasLineOfSight(target)))return;
  long cost=cost(p,s);if(d.power()*1000+1e-6<cost)return;
  if(s.type().equals("mount")){var at=SpiritSwordEntity.placement(p);if(p.isPassenger()||at==null||SpiritSwordEntity.summon(p,at)==null)return;}
  // Retire pending cast before publishing its unique release, payment and cooldown.
  CASTS.remove(p.getUUID());d.setPower(d.power()-cost/1000.0);com.longju.xiantu.refining.EquipmentConditions.paid(p,s);data(p).setCooldown(s.id(),now+s.cooldown());
  notify(p,s,c.transaction,target,SpellLifecycleEvent.Phase.RELEASE,p.position(),c.direction);
  if(s.type().equals("projectile"))FLIGHTS.add(new Flight(c.transaction,p.getUUID(),s,p.serverLevel(),c.direction,new HashSet<>(),0,p.getEyePosition()));
  else if(s.type().equals("target"))damage(p,target,s,c.transaction,1);
  CultivationEvents.sync(p);sync(p);
 }
 /** Server preview before target realm, elemental resistance, armor and boss mitigation. */
 public static double damagePreview(ServerPlayer p,SpellCatalog.Spell s){
  var d=GongfaService.data(p);
  double bonus=Math.max(.1,Math.min(com.longju.xiantu.refining.QitianSkills.form(p)?2.8:2.2,1+GongfaService.attackBonus(d)+com.longju.xiantu.refining.EquipmentEffects.attackBonus(p)+com.longju.xiantu.refining.QitianSkills.damageBonus(p)));
  return SpellRules.damage(s.damage(),s.realm(),d.majorRealm(),d.stage(),data(p).mastery(s.id()))*bonus;
 }
 public static void damage(ServerPlayer p,LivingEntity target,SpellCatalog.Spell s,UUID cast,double multiplier){
  if(!enemy(p,target))return;var d=GongfaService.data(p);
  var info=GongfaEvents.target(target);int defender=target instanceof Player other?other.getCapability(CultivationCapability.DATA).map(v->v.majorRealm()+1).orElse(1):info.realmRank();
  double amount=damagePreview(p,s)*multiplier*SpellRules.realmMultiplier(d.majorRealm()+1,defender);
  var source=new ElementalDamageSource(p.damageSources().indirectMagic(p,p),s.element());float before=target.getHealth()+target.getAbsorptionAmount();
  if(target.hurt(source,(float)amount)&&(target.getHealth()+target.getAbsorptionAmount()<before)){notify(p,s,cast,target,SpellLifecycleEvent.Phase.HIT,target.position(),p.getLookAngle());award(p,s,cast,target);}
 }
 private static void award(ServerPlayer p,SpellCatalog.Spell s,UUID cast,LivingEntity target){
  if(p.isCreative()||p.isSpectator()||target!=null&&(!GongfaEvents.target(target).eligible()||target instanceof Player||target.getPersistentData().getBoolean("xiantu_no_method_experience")))return;
  data(p).award(s.id(),(int)Math.round(PhysiqueService.effectiveSpellExperience(GongfaService.data(p),SpellRules.experience(s.tier()))*(1+com.longju.xiantu.refining.EquipmentEffects.spellExperience(p))*1000),now(p),cast,target==null?null:target.getUUID());
 }
 public static double defense(Player p){return 0;}
 @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e){
  if(e.phase!=TickEvent.Phase.END)return;var server=net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();if(server==null)return;
  for(var p:server.getPlayerList().getPlayers()){
   var cast=CASTS.get(p.getUUID());if(cast!=null){if(!p.isAlive()||p.hurtTime>0||!p.level().dimension().equals(cast.dimension)||p.containerMenu!=p.inventoryMenu||cast.spell.stationary()&&p.position().distanceToSqr(cast.start)>.04)cancel(p);else if(now(p)>=cast.finish){release(p,cast);if(CASTS.get(p.getUUID())==cast)cancel(p);sync(p);}}
   if(p.tickCount%20==0)sync(p);
  }
  for(int i=FLIGHTS.size()-1;i>=0;i--){var f=FLIGHTS.get(i);var p=server.getPlayerList().getPlayer(f.owner);if(p==null||p.level()!=f.level||!p.isAlive()){FLIGHTS.remove(i);continue;}
   Vec3 end=clip(f.level,p,f.position,f.position.add(f.direction.scale(1.25)));boolean wall=end.distanceToSqr(f.position.add(f.direction.scale(1.25)))>.001;
   for(var target:f.level.getEntitiesOfClass(LivingEntity.class,new AABB(f.position,end).inflate(.4),t->enemy(p,t)&&!f.hits.contains(t.getUUID())).stream().sorted(Comparator.comparingDouble(t->t.distanceToSqr(f.position))).toList())if(target.getBoundingBox().inflate(.3).clip(f.position,end).isPresent()){
    f.hits.add(target.getUUID());damage(p,target,f.spell,f.transaction,1);if(!f.hits.isEmpty()){wall=true;break;}
   }
   if(f.age%2==0)notify(p,f.spell,f.transaction,null,SpellLifecycleEvent.Phase.FLIGHT,end,f.direction);
   if(wall||(f.age+1)*1.25>=f.spell.range()){notify(p,f.spell,f.transaction,null,SpellLifecycleEvent.Phase.END,end,f.direction);FLIGHTS.remove(i);}else FLIGHTS.set(i,new Flight(f.transaction,f.owner,f.spell,f.level,f.direction,f.hits,f.age+1,end));
  }
 }
 @SubscribeEvent(priority=EventPriority.LOWEST) public static void damage(LivingDamageEvent e){
  if(e.getEntity() instanceof ServerPlayer p&&e.getAmount()>0)cancel(p);
 }
 private static void leave(Player p){if(p instanceof ServerPlayer s){SpiritSwordEntity.dismiss(s);var cast=CASTS.remove(p.getUUID());if(cast!=null)notify(s,cast.spell,cast.transaction,null,SpellLifecycleEvent.Phase.CANCEL,s.position(),cast.direction);REQUESTS.remove(p.getUUID());FLIGHTS.removeIf(e->{if(!e.owner.equals(p.getUUID()))return false;notify(s,e.spell,e.transaction,null,SpellLifecycleEvent.Phase.END,e.position,e.direction);return true;});s.getPersistentData().remove("xiantu_spell_shield");s.getPersistentData().remove("xiantu_spell_shield_until");}}
 @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){leave(e.getEntity());}
 @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e){leave(e.getEntity());}
 @SubscribeEvent public static void death(LivingDeathEvent e){if(e.getEntity() instanceof Player p)leave(p);}
 @SubscribeEvent public static void stopped(net.minecraftforge.event.server.ServerStoppedEvent e){CASTS.clear();REQUESTS.clear();FLIGHTS.clear();}
 private SpellService(){}
}
