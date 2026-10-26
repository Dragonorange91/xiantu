package com.longju.xiantu.qa;

import com.longju.xiantu.entity.*;
import com.longju.xiantu.cultivation.*;
import com.longju.xiantu.gongfa.GongfaService;
import com.longju.xiantu.registry.ModEntities;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Charged, actual ServerPlayer.attack callbacks; avoids attributing math-only tests to runtime. */
@Mod.EventBusSubscriber(modid="xiantu_qa")
public final class BossBalanceAudit {
 private static SpellAudit.Session state;
 private static Mob boss;
 private static int species,hits,checks;
 private static long wait;
 private static float initial;
 private static final List<String> IDS=List.of("spirit_ape_king","meteor_general","five_element_warden");
 private static void check(boolean value,String text){checks++;if(!value)throw new IllegalStateException(text);}
 private static void finish(){if(boss!=null)boss.discard();boss=null;if(state!=null){var s=state;state=null;s.finish();}}
 private static void identity(Mob mob){var n=mob.getPersistentData();n.putInt(BeastResourceDrops.REALM,5);n.putInt(BeastRealmService.STAGE,3);n.putBoolean(BeastRealmService.INITIALIZED,true);HostileRealmService.initialize(mob);}
 private static void spawn(){
  var p=state.p;boss=ModEntities.COMPLETION.get(IDS.get(species)).get().create(p.serverLevel());
  ((Entity)boss).moveTo(state.at.getX()+.5,300,state.at.getZ()+3.5,180,0);boss.setNoAi(true);((Entity)boss).setNoGravity(true);identity(boss);boss.setHealth(boss.getMaxHealth());
  check(p.serverLevel().addFreshEntity(boss),"boss insertion");
  check(Math.abs(boss.getMaxHealth()-12375)<.1,"Lianxu peak health for "+IDS.get(species));
  check(Math.abs(boss.getAttributeValue(Attributes.ATTACK_DAMAGE)-270.6)<.01,"stage-derived boss attack");
  float hp=boss.getMaxHealth();HostileRealmService.initialize(boss);check(boss.getMaxHealth()==hp,"tracking initialization does not stack attributes");
  initial=boss.getHealth();hits=0;wait=p.serverLevel().getGameTime()+22;
 }
 @SubscribeEvent public static void commands(RegisterCommandsEvent e){e.getDispatcher().register(Commands.literal("xiantubossbalance")
  .executes(c->{var p=c.getSource().getPlayerOrException();if(state!=null||!p.server.getWorldPath(LevelResource.ROOT).normalize().getFileName().toString().equals("Xiantu-QA-BeastFlat-20261017"))return 0;
   checks=species=0;state=new SpellAudit.Session(p);
   try{state.prepare();com.longju.xiantu.spell.SpellService.cancel(p);p.getInventory().clearContent();state.data.setCultivationUnlocked(true);state.data.setRealmStage(5,0);GongfaService.refresh(p);CultivationService.applyRealmAttributes(p,state.data);CultivationEvents.sync(p);
    check(Math.abs(p.getAttributeValue(Attributes.ATTACK_DAMAGE)-96)<.01,"Lianxu early barehand damage is 96; actual="+p.getAttributeValue(Attributes.ATTACK_DAMAGE)+", base="+p.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue()+", modifiers="+p.getAttribute(Attributes.ATTACK_DAMAGE).getModifiers());
    for(var type:List.of(EntityType.WITHER,EntityType.ENDER_DRAGON,EntityType.WARDEN,EntityType.ELDER_GUARDIAN)){
     var m=(Mob)type.create(p.serverLevel());identity(m);double hp=m.getMaxHealth();check(hp>=12374,"vanilla boss absolute realm floor "+type);
     HostileRealmService.initialize(m);check(m.getMaxHealth()==hp,"vanilla repeat initialization stable");
     var copy=(Mob)type.create(p.serverLevel());copy.load(m.saveWithoutId(new CompoundTag()));HostileRealmService.initialize(copy);check(Math.abs(copy.getMaxHealth()-hp)<.1,"saved boss statistics round trip");m.discard();copy.discard();
    }
    spawn();p.sendSystemMessage(Component.literal("[Boss balance QA] Started: actual eight barehand attacks per boss"));return 1;
   }catch(Exception|LinkageError ex){p.sendSystemMessage(Component.literal("[Boss balance QA] FAIL "+checks+": "+ex));finish();return 0;}})
  .then(Commands.literal("restore").executes(c->{finish();return 1;})));}
 @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e){if(e.phase!=TickEvent.Phase.END||state==null)return;
  try{var p=state.p;if(p.serverLevel().getGameTime()<wait)return;
   if(hits<8){float hp=boss.getHealth();boss.invulnerableTime=0;p.attack(boss);hits++;p.sendSystemMessage(Component.literal("[Boss balance QA] hit="+hits+" hp="+hp+"->"+boss.getHealth()+" max="+boss.getMaxHealth()+" attack="+p.getAttributeValue(Attributes.ATTACK_DAMAGE)+" alive="+boss.isAlive()));wait=p.serverLevel().getGameTime()+22;return;}
   check(boss.isAlive()&&boss.getHealth()>initial*.6,"eight charged barehand strikes leave boss alive with substantial health");
   check(boss.getHealth()<initial,"real attacks produce actual damage");
   p.sendSystemMessage(Component.literal("[Boss balance QA] "+IDS.get(species)+" eight strikes: "+initial+" -> "+boss.getHealth()));
   boss.invulnerableTime=0;float before=boss.getHealth();float amount=initial*.1F;boss.hurt(p.damageSources().magic(),amount);
   check(Math.abs((before-boss.getHealth())-BossBalance.received(amount,initial))<1,"native boss final soft cap runs once");
   boss.discard();boss=null;if(++species<IDS.size()){spawn();return;}
   p.sendSystemMessage(Component.literal("[Boss balance QA] PASS "+checks+" assertions; all three native boss attack paths"));finish();
  }catch(Exception|LinkageError ex){state.p.sendSystemMessage(Component.literal("[Boss balance QA] FAIL "+checks+": "+ex));finish();}}
 private BossBalanceAudit(){}
}
