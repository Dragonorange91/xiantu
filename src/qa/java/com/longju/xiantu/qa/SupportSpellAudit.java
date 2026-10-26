package com.longju.xiantu.qa;
import com.longju.xiantu.spell.*;
import com.longju.xiantu.gongfa.GongfaService;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;
/** Actual heal callbacks, loadout, cooldown and power transaction in a disposable world. */
@Mod.EventBusSubscriber(modid="xiantu_qa")
public final class SupportSpellAudit {
 private static int checks;
 private static void check(boolean value,String why){checks++;if(!value)throw new IllegalStateException(why);}
 @SubscribeEvent public static void commands(RegisterCommandsEvent e){e.getDispatcher().register(Commands.literal("xiantusupportspellaudit").requires(s->s.hasPermission(2)).executes(c->{
  var p=c.getSource().getPlayerOrException();if(!p.server.getWorldPath(LevelResource.ROOT).normalize().getFileName().toString().equals("Xiantu-QA-BeastFlat-20261017"))return 0;
  var snapshot=new SpellAudit.Session(p);var effects=p.getActiveEffects().stream().map(net.minecraft.world.effect.MobEffectInstance::new).toList();checks=0;
  try{
   p.closeContainer();p.setGameMode(GameType.SURVIVAL);p.removeAllEffects();p.getInventory().clearContent();var d=snapshot.data;
   d.setCultivationUnlocked(true);d.setRealmStage(0,0);d.resetSpiritRoots();d.setPhysique(com.longju.xiantu.cultivation.PhysiqueType.ORDINARY);
   d.gongfa().clear();check(d.gongfa().learn("jin_guang_jue")&&d.gongfa().commit("jin_guang_jue",UUID.randomUUID()),"non-healing main method installed");
   GongfaService.refresh(p);d.setPower(d.powerMax());d.spells().load(new CompoundTag());d.spells().learn("rejuvenation");
   check(SpellService.denial(p,SpellCatalog.find("rejuvenation"),true).isEmpty(),"rootless player can equip healing");
   p.setHealth(p.getMaxHealth()*.25F);check(!RejuvenationService.attempt(p),"un-equipped passive cannot heal");
   check(d.spells().configure(d.spells().revision(),List.of("","","","","rejuvenation","")),"passive slot configured");
   p.setHealth(p.getMaxHealth()*.5F);check(!RejuvenationService.attempt(p),"healthy player does not spend power");
   p.setHealth(p.getMaxHealth()*.25F);double before=p.getHealth(),power=d.power(),cost=SpellService.cost(p,SpellCatalog.find("rejuvenation"))/1000.0;
   check(RejuvenationService.attempt(p),"low health activates actual passive");
   check(Math.abs(p.getHealth()-before-p.getMaxHealth()*.10)<.001,"initial real healing ten percent");
   check(Math.abs(power-d.power()-cost)<.001,"power paid once");
   p.setHealth(p.getMaxHealth()*.25F);before=p.getHealth();power=d.power();check(!RejuvenationService.attempt(p)&&p.getHealth()==before&&d.power()==power,"cooldown prevents second heal or payment");
   var n=d.spells().save();var xp=n.getCompound("XP");xp.putInt("rejuvenationLevel",9);n.put("XP",xp);d.spells().load(n);d.spells().setCooldown("rejuvenation",SpellService.now(p));d.setPower(d.powerMax());before=p.getHealth();
   check(RejuvenationService.attempt(p)&&Math.abs(p.getHealth()-before-p.getMaxHealth()*.325)<.001,"mastery increases actual healing");
   d.spells().setCooldown("rejuvenation",SpellService.now(p));p.setHealth(p.getMaxHealth()*.25F);d.setPower(0);before=p.getHealth();check(!RejuvenationService.attempt(p)&&p.getHealth()==before,"no free healing without mana");
   p.sendSystemMessage(Component.literal("[Support Spell QA] PASS "+checks));return 1;
  }catch(Exception|LinkageError failure){com.mojang.logging.LogUtils.getLogger().error("[Support Spell QA] FAIL {}",checks,failure);throw failure;
  }finally{snapshot.finish();p.removeAllEffects();effects.forEach(p::addEffect);}
 }));}
 private SupportSpellAudit(){}
}
