package com.longju.xiantu.qa;
import com.longju.xiantu.spell.*;
import com.longju.xiantu.gongfa.GongfaService;
import com.longju.xiantu.cultivation.*;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;
/** Disposable-world authoritative transaction audit; does not simulate physical keys. */
@Mod.EventBusSubscriber(modid="xiantu_qa")
public final class GreatFireballAudit {
 private static SpellAudit.Session snapshot,preview;private static Zombie target;
 private static UUID gesture;private static int phase,checks;private static long start;private static float smallDamage,targetHealth;private static double before,recovered,lastPower;
 private static void check(boolean ok,String why){checks++;if(!ok)throw new IllegalStateException(why);}
 private static ServerPlayer player(){return snapshot.p;}
 private static void begin(){gesture=UUID.randomUUID();start=SpellService.now(player());String denial=GreatFireballService.input(player(),0,gesture,0);check(denial.isEmpty(),"begin accepted: "+denial);check(GreatFireballService.casting(player()),"charge active at phase "+phase);}
 private static void target(){
  var p=player();target=EntityType.ZOMBIE.create(p.serverLevel());((Entity)target).moveTo(p.getX(),p.getY(),p.getZ()+20,180,0);((Mob)target).setNoAi(true);((Entity)target).setNoGravity(true);target.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.CARVED_PUMPKIN));target.getPersistentData().putInt(com.longju.xiantu.entity.BeastResourceDrops.REALM,2);p.serverLevel().addFreshEntity(target);target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1024);target.setHealth(target.getMaxHealth());targetHealth=target.getHealth();
 }
 private static void prepare(ServerPlayer p){
  snapshot=new SpellAudit.Session(p);checks=0;phase=0;p.closeContainer();p.setGameMode(GameType.SURVIVAL);p.setNoGravity(true);p.getInventory().clearContent();p.server.setDifficulty(net.minecraft.world.Difficulty.NORMAL,true);
  p.teleportTo(p.serverLevel(),snapshot.at.getX()+.5,260,snapshot.at.getZ()+.5,0,0);p.serverLevel().getChunkAt(p.blockPosition().offset(0,0,40));
  var d=snapshot.data;d.setCultivationUnlocked(true);d.resetSpiritRoots();d.setPhysique(PhysiqueType.ORDINARY);d.gongfa().clear();d.gongfa().learn("jin_guang_jue");d.gongfa().commit("jin_guang_jue",UUID.randomUUID());d.spells().load(new CompoundTag());d.setRealmStage(1,0);GongfaService.refresh(p);
  check(!SpellService.denial(p,GreatFireballService.spell(),false).isEmpty(),"Foundation cannot learn");
  d.setRealmStage(2,0);GongfaService.refresh(p);d.setPower(d.powerMax());d.setPowerSpeed(0);
  check(SpellService.denial(p,GreatFireballService.spell(),false).isEmpty(),"rootless Golden Core may learn");d.spells().learn("great_fireball");
  check(d.spells().configure(d.spells().revision(),List.of("great_fireball","","","","","")),"active slot configured");
  begin();before=d.power();GreatFireballService.input(p,0,UUID.randomUUID(),0);check(GreatFireballService.casting(p),"duplicate begin retains original charge");check(d.power()==before,"duplicate begin no extra debit");
  GreatFireballService.input(p,0,UUID.randomUUID(),2);check(GreatFireballService.casting(p),"stale release ignored");
  GreatFireballService.input(p,0,gesture,2);check(!GreatFireballService.casting(p),"tap cancels");start=SpellService.now(p);phase=1;
 }
 private static void step(){
  var p=player();long now=SpellService.now(p),age=now-start;
  if(phase==1&&age>=24){target();snapshot.data.setPower(snapshot.data.powerMax());begin();phase=2;}
  else if(phase==2){if(age%5==0)GreatFireballService.input(p,0,gesture,1);if(age==10){double power=snapshot.data.power();GreatFireballService.input(p,0,UUID.randomUUID(),0);check(power==snapshot.data.power(),"delayed duplicate begin cannot replace or debit charge");}if(age>=20){GreatFireballService.input(p,0,gesture,2);check(!GreatFireballService.casting(p),"short release retires charge");before=snapshot.data.power();GreatFireballService.input(p,0,gesture,2);check(before==snapshot.data.power(),"duplicate release no debit");start=now;phase=3;}}
  else if(phase==3&&age>=40){smallDamage=targetHealth-target.getHealth();check(smallDamage>0,"small projectile inflicts actual damage");((Entity)target).discard();target();snapshot.data.spells().setCooldown("great_fireball",now);snapshot.data.setPower(snapshot.data.powerMax());before=snapshot.data.power();begin();recovered=0;lastPower=snapshot.data.power();phase=4;}
  else if(phase==4){if(age%5==0)GreatFireballService.input(p,0,gesture,1);if(age>=100){GreatFireballService.input(p,0,gesture,2);check(!GreatFireballService.casting(p),"full release retires charge");check(snapshot.data.spells().cooldown("great_fireball",now)>0,"release starts cooldown");check(Math.abs(before+recovered-snapshot.data.power()-SpellService.cost(p,GreatFireballService.spell())/1000.0)<.01,"whole charge billed exactly once including measured regeneration");start=now;phase=5;}else lastPower=snapshot.data.power();}
  else if(phase==5&&age>=40){check(targetHealth-target.getHealth()>smallDamage*1.5,"full projectile damage exceeds short charge");snapshot.data.spells().setCooldown("great_fireball",now);snapshot.data.setPower(snapshot.data.powerMax());begin();phase=6;}
  else if(phase==6&&age>=20){check(!GreatFireballService.casting(p),"lost heartbeat cancels rather than fires");p.sendSystemMessage(Component.literal("[Great Fireball QA] PASS "+checks));finish();}
 }
 private static void finish(){if(snapshot==null)return;GreatFireballService.cancel(player());if(target!=null)((Entity)target).discard();target=null;snapshot.finish();snapshot=null;}
 @SubscribeEvent public static void commands(RegisterCommandsEvent e){e.getDispatcher().register(Commands.literal("xiantugreatfireballaudit").requires(s->s.hasPermission(2)).executes(c->{var p=c.getSource().getPlayerOrException();if(snapshot!=null||preview!=null||!allowed(p))return 0;try{prepare(p);return 1;}catch(Exception|LinkageError ex){com.mojang.logging.LogUtils.getLogger().error("[Great Fireball QA] FAIL {}",checks,ex);finish();throw ex;}})
  .then(Commands.literal("preview").executes(c->{var p=c.getSource().getPlayerOrException();if(snapshot!=null||preview!=null||!allowed(p))return 0;preview=new SpellAudit.Session(p);p.closeContainer();p.getInventory().clearContent();var d=preview.data;d.setCultivationUnlocked(true);d.setRealmStage(2,0);d.setPhysique(PhysiqueType.ORDINARY);d.gongfa().clear();d.gongfa().learn("jin_guang_jue");d.gongfa().commit("jin_guang_jue",UUID.randomUUID());GongfaService.refresh(p);d.spells().load(new CompoundTag());d.spells().learn("great_fireball");d.spells().learn("sword_flight");d.spells().learn("rejuvenation");d.spells().configure(d.spells().revision(),List.of("great_fireball","sword_flight","","","rejuvenation",""));d.setPower(d.powerMax());com.longju.xiantu.cultivation.CultivationEvents.sync(p);SpellService.sync(p);return 1;}))
  .then(Commands.literal("restore").executes(c->{var p=c.getSource().getPlayerOrException();if(preview==null||preview.p!=p)return 0;GreatFireballService.cancel(p);preview.finish();preview=null;return 1;})));}
 private static boolean allowed(ServerPlayer p){return p.server.getWorldPath(LevelResource.ROOT).normalize().getFileName().toString().equals("Xiantu-QA-BeastFlat-20261017");}
 @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOWEST) public static void regeneration(TickEvent.PlayerTickEvent e){if(e.phase==TickEvent.Phase.END&&snapshot!=null&&phase==4&&e.player==snapshot.p)recovered+=Math.max(0,snapshot.data.power()-lastPower);}
 @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.LOWEST) public static void tick(TickEvent.ServerTickEvent e){if(e.phase!=TickEvent.Phase.END||snapshot==null)return;try{step();}catch(Exception|LinkageError ex){com.mojang.logging.LogUtils.getLogger().error("[Great Fireball QA] FAIL {}",checks,ex);player().sendSystemMessage(Component.literal("[Great Fireball QA] FAIL "+checks+": "+ex));finish();}}
 private GreatFireballAudit(){}
}
