package com.longju.xiantu.qa;

import com.longju.xiantu.cultivation.*;
import com.longju.xiantu.entity.*;
import com.longju.xiantu.gongfa.GongfaService;
import com.longju.xiantu.item.*;
import com.longju.xiantu.refining.*;
import com.longju.xiantu.talisman.*;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.*;
import java.util.*;

/** Production transactions against an isolated player; no player death or permanent ledger entries. */
@Mod.EventBusSubscriber(modid="xiantu_qa")
public final class BatchMechanicsAudit {
 private static int checks;
 private static void check(boolean b,String message){checks++;if(!b)throw new IllegalStateException(message);}
 @SubscribeEvent public static void commands(RegisterCommandsEvent e){e.getDispatcher().register(Commands.literal("xiantubatchmechanics").requires(s->s.hasPermission(2)).executes(c->{
  var p=c.getSource().getPlayerOrException();
  if(!p.server.getWorldPath(LevelResource.ROOT).normalize().getFileName().toString().equals("Xiantu-QA-BeastFlat-20261017"))return 0;
  checks=0;String result;
  var d=(PlayerCultivationData)GongfaService.data(p);var cultivation=new CompoundTag();d.save(cultivation);
  var persistent=p.getPersistentData().copy();var inventory=p.getInventory().save(new ListTag());var mode=p.gameMode.getGameModeForPlayer();
  boolean allowed=p.getAbilities().mayfly,flying=p.getAbilities().flying;float speed=p.getAbilities().getFlyingSpeed(),health=p.getHealth(),absorption=p.getAbsorptionAmount();
  var ledger=ReincarnationLedger.get(p.serverLevel());var ledgerBefore=ledger.save(new CompoundTag());boolean dirty=ledger.isDirty();
  try{
   p.closeContainer();p.stopUsingItem();p.getInventory().clearContent();p.setGameMode(GameType.SURVIVAL);
   p.getPersistentData().remove("xiantu_shape_flight");p.getAbilities().mayfly=false;p.getAbilities().flying=false;
   QitianShapeAbilities.tick(p,"thunder_feather_raven");check(p.getAbilities().mayfly,"bird flight grant");
   QitianShapeAbilities.tick(p,"");check(!p.getAbilities().mayfly&&!p.getAbilities().flying,"bird flight revoked");
   var attribute=p.getAttribute(Attributes.MOVEMENT_SPEED);double base=attribute.getValue();
   QitianShapeAbilities.tick(p,"spirit_deer");check(attribute.getValue()>base*1.14,"land shape speed bonus");
   QitianShapeAbilities.clear(p);check(Math.abs(attribute.getValue()-base)<1e-7,"land speed revoked");
   p.setGameMode(GameType.CREATIVE);QitianShapeAbilities.tick(p,"purple_lightning_bat");QitianShapeAbilities.clear(p);check(p.getAbilities().mayfly,"creative flight retained");
   long now=com.longju.xiantu.spell.SpellService.clock();
   check(!QitianMovement.recentCombat(now+10000,now),"future combat timestamp does not lock skills");
   check(!QitianMovement.recentCombat(now-101,now),"expired combat timer cleared");check(QitianMovement.recentCombat(now-20,now),"recent combat still blocks");
   double previous=0;for(int q=1;q<=5;q++){check(QitianQuality.scale(q)>previous,"quality strictly increases "+q);previous=QitianQuality.scale(q);}
   for(var type:List.of(EntityType.ZOMBIE,EntityType.SKELETON,EntityType.CREEPER,EntityType.WITHER,EntityType.ENDER_DRAGON,EntityType.WARDEN,EntityType.ELDER_GUARDIAN)){
    var mob=(Mob)type.create(p.serverLevel());check(mob!=null&&HostileRealmService.eligible(mob),"hostile realm eligibility "+type);
    HostileRealmService.initialize(mob);check(BeastRealmService.valid(mob),"real realm initialization "+type);
    int r=BeastRealmService.realm(mob),s=mob.getPersistentData().getInt(BeastRealmService.STAGE);double hp=mob.getMaxHealth();
    check(r>=(HostileRealmService.boss(mob)?3:0)&&r<=8,"realm bounds "+type);check(s>=0&&s<RealmRegistry.STAGE_COUNTS[r],"stage bounds "+type);
    HostileRealmService.initialize(mob);check(BeastRealmService.realm(mob)==r&&mob.getMaxHealth()==hp,"repeat initialization stable "+type);
    var n=mob.saveWithoutId(new CompoundTag());var reloaded=(Mob)type.create(p.serverLevel());reloaded.load(n);HostileRealmService.initialize(reloaded);
    check(BeastRealmService.realm(reloaded)==r&&reloaded.getPersistentData().getInt(BeastRealmService.STAGE)==s,"saved realm persists "+type);
    mob.discard();reloaded.discard();
   }
   for(String id:List.of("spirit_ape_king","meteor_general","five_element_warden")){
    var mob=(Mob)com.longju.xiantu.registry.ModEntities.COMPLETION.get(id).get().create(p.serverLevel());
    HostileRealmService.initialize(mob);check(HostileRealmService.boss(mob)&&BeastRealmService.realm(mob)>=3,"Xiantu boss minimum Yuanying "+id);mob.discard();
   }
   // Runs the real bind, death-record preparation, restore, consume and anti-replay paths.
   // Does not kill/respawn the player's body; the real death event itself is a separate test.
   for(int major:new int[]{0,1,3,8}){
    d.setCultivationUnlocked(true);d.setRealmStage(major,major==0?5:2);d.setPowerMax(1000000);d.setPower(1000000);
    var symbol=TalismanItems.symbol("rebirth",3,1);p.setItemInHand(InteractionHand.MAIN_HAND,symbol);
    symbol.getItem().use(p.level(),p,InteractionHand.MAIN_HAND);int recorded=d.realm();
    check(symbol.getTagElement("xiantu_reincarnation_snapshot")!=null,"rebirth native bind "+major);var copied=symbol.copy();
    ReincarnationStoneItem.prepareDeath(d,d,p.getUUID());check(!d.pendingReincarnationStones().isEmpty(),"death transaction prepared "+major);
    d.setRealmStage(0,0);d.setProgress(0);symbol.getItem().use(p.level(),p,InteractionHand.MAIN_HAND);
    check(d.realm()==Math.max(0,recorded-(major==0?1:3)),"actual restore small-stage penalty "+major);
    check(p.getMainHandItem().isEmpty(),"rebirth consumed exactly once "+major);check(d.pendingReincarnationStones().isEmpty(),"death pending cleared "+major);
    p.setItemInHand(InteractionHand.MAIN_HAND,copied);int restored=d.realm();
    check(!copied.getItem().use(p.level(),p,InteractionHand.MAIN_HAND).getResult().consumesAction()&&d.realm()==restored,"copied instance replay rejected "+major);
   }
   result="[Batch mechanics QA PASS] checks="+checks+" traits/flight-revoke/quality/vanilla-and-Xiantu-boss-realms/rebirth-transactions";
  }catch(Exception|LinkageError ex){result="[Batch mechanics QA FAIL] checks="+checks+" "+ex;}
  finally{
   QitianShapeAbilities.clear(p);p.getPersistentData().getAllKeys().clear();p.getPersistentData().merge(persistent);d.load(cultivation);
   p.getInventory().clearContent();p.getInventory().load(inventory);p.setGameMode(mode);p.getAbilities().mayfly=allowed;p.getAbilities().flying=flying;p.getAbilities().setFlyingSpeed(speed);p.onUpdateAbilities();
   try{for(String name:List.of("spent","deaths")){var field=ReincarnationLedger.class.getDeclaredField(name);field.setAccessible(true);field.set(ledger,ledgerBefore.getCompound(name.equals("spent")?"Spent":"Deaths").copy());}ledger.setDirty(dirty);}catch(ReflectiveOperationException ex){throw new IllegalStateException("ledger restoration failed",ex);}
   GongfaService.refresh(p);RefinedEquipment.refreshEligibility(p);p.setHealth(health);p.setAbsorptionAmount(absorption);p.inventoryMenu.broadcastChanges();CultivationEvents.sync(p);QitianSkills.sync(p);
  }
  p.sendSystemMessage(Component.literal(result));try{Path path=p.server.getWorldPath(LevelResource.ROOT).resolve("xiantu-batch-mechanics.txt");Files.writeString(path,result);}catch(Exception ex){throw new IllegalStateException(ex);}return 1;
 }));}
 private BatchMechanicsAudit(){}
}
