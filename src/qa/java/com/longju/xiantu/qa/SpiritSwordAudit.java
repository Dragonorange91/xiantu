package com.longju.xiantu.qa;
import com.longju.xiantu.spell.*;
import com.longju.xiantu.gongfa.GongfaService;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Real summon/scroll/ride callbacks and server-tick movement; restores its bounded fixture. */
@Mod.EventBusSubscriber(modid="xiantu_qa")
public final class SpiritSwordAudit {
 private static SpellAudit.Session state;private static int phase;private static long wait;private static SpiritSwordEntity sword;private static Vec3 start;private static double power;
 private static void check(boolean v,String m){state.check(v,m);}
 private static void finish(){if(state==null)return;var s=state;state=null;if(s.p.isPassenger())s.p.stopRiding();SpiritSwordEntity.dismiss(s.p);s.finish();sword=null;}
 @SubscribeEvent public static void commands(RegisterCommandsEvent e){e.getDispatcher().register(Commands.literal("xiantuswordinspect").executes(c->{var p=c.getSource().getPlayerOrException();if(!p.server.getWorldPath(LevelResource.ROOT).normalize().getFileName().toString().equals("Xiantu-QA-BeastFlat-20261017"))return 0;String text=p.getVehicle() instanceof SpiritSwordEntity s?s.flightDebug():"not riding";p.sendSystemMessage(Component.literal("[Spirit Sword Inspect] "+text));return 1;}));e.getDispatcher().register(Commands.literal("xiantuswordaudit")
  .executes(c->{var p=c.getSource().getPlayerOrException();if(state!=null||!p.server.getWorldPath(LevelResource.ROOT).normalize().getFileName().toString().equals("Xiantu-QA-BeastFlat-20261017"))return 0;state=new SpellAudit.Session(p);try{state.prepare();SpellService.cancel(p);
   var scroll=SpellScrolls.stack("sword_flight");p.getInventory().setItem(p.getInventory().selected,scroll);((Item)scroll.getItem()).use(p.level(),p,InteractionHand.MAIN_HAND);check(scroll.getCount()==1&&!state.data.spells().knows("sword_flight"),"Qi cannot learn or consume sword scroll");
   state.data.setRealmStage(1,0);GongfaService.refresh(p);state.data.setPower(state.data.powerMax());state.data.setPowerSpeed(0);((Item)scroll.getItem()).use(p.level(),p,InteractionHand.MAIN_HAND);check(scroll.isEmpty()&&state.data.spells().knows("sword_flight"),"Foundation learns rootless sword scroll once");
   check(state.data.spells().configure(state.data.spells().revision(),List.of("fire_bolt","","sword_flight","","","")),"sword configured as active");
   // Natural realm recovery has a nonzero floor. Raise fixture capacity so upkeep exceeds it.
   state.data.setPowerMax(1000000);state.data.setPower(state.data.powerMax());
   var wall=state.at.offset(0,1,1);state.blocks.put(wall,p.level().getBlockState(wall));
   p.level().setBlock(wall,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),2);
   check(SpellService.safeDestination(p,2,new Vec3(0,0,1)).distanceToSqr(p.position())<.001,"movement collision sweep rejects a solid wall");
   var detached=com.longju.xiantu.registry.ModEntities.SPIRIT_SWORD.get().create(p.serverLevel());
   check(detached!=null,"collision fixture sword created without inserting an entity");
   Vec3 bladeAt=p.position().add(0,-SpiritSwordEntity.DECK_HEIGHT,0);((Entity)detached).setPos(bladeAt);
   Vec3 clipped=SwordFlightCollision.clip(p,detached,bladeAt,new Vec3(0,0,1.4));
   check(clipped.z>=0&&clipped.z<.3,"continuous flight collision stops before a wall even at boost speed");
   Vec3 slide=SwordFlightCollision.clip(p,detached,bladeAt,new Vec3(.35,0,1.4));
   check(slide.x>.3&&slide.z<.3,"flight slides along wall instead of cancelling every movement axis");
   check(((Entity)detached).getControllingPassenger()==null,"vanilla client vehicle packets do not own sword movement");
   p.level().setBlock(wall,state.blocks.get(wall),2);
   power=state.data.power();var origin=p.position();p.teleportTo(p.getX()+15,p.getY(),p.getZ());check(SpiritSwordEntity.placement(p)==null&&!SpellService.begin(p,2).isEmpty()&&state.data.power()==power,"no-ground summon rejected without payment");p.teleportTo(origin.x,origin.y,origin.z);phase=0;wait=SpellService.now(p)+6;p.sendSystemMessage(Component.literal("[Spirit Sword QA] Started"));return 1;
  }catch(Exception|LinkageError failure){p.sendSystemMessage(Component.literal("[Spirit Sword QA] FAIL "+failure));finish();return 0;}})
  .then(Commands.literal("restore").executes(c->{finish();return 1;})));}
 @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e){if(e.phase!=TickEvent.Phase.END||state==null)return;try{
  var p=state.p;drive(p);if(SpellService.now(p)<wait)return;
  if(phase==0){power=state.data.power();check(SpellService.begin(p,2).isEmpty(),"sword begins at Foundation");check(state.data.power()==power,"no windup debit");phase=1;wait=SpellService.now(p)+16;}
  else if(phase==1){var all=p.serverLevel().getEntitiesOfClass(SpiritSwordEntity.class,p.getBoundingBox().inflate(15),s->p.getUUID().equals(s.owner()));check(all.size()==1,"exactly one blue sword entity summoned");sword=all.get(0);check(!p.isPassenger(),"summon waits for right click");check(Math.abs(((Entity)sword).getY()-300.04)<.02,"sword placed above ground");check(state.data.power()<power&&state.data.spells().cooldown("sword_flight",SpellService.now(p))==0&&SpellCatalog.find("sword_flight").cooldown()==0,"successful summon pays without cooldown");check(!sword.shouldRiderSit(),"standing mount pose");check(((Entity)sword).interact(p,InteractionHand.MAIN_HAND).consumesAction()&&p.getVehicle()==sword,"real right-click callback mounts owner");start=((Entity)sword).position();power=state.data.power();phase=2;wait=SpellService.now(p)+30;}
  else if(phase==2){check(((Entity)sword).position().distanceToSqr(start)<.001,"no input holds position");check(Math.abs(p.getY()-((Entity)sword).getY()-SpiritSwordEntity.DECK_HEIGHT)<.01,"feet on blade deck");check(state.data.power()<power,"mounted flight drains real power");start=((Entity)sword).position();phase=3;wait=SpellService.now(p)+15;}
  else if(phase==3){check(((Entity)sword).getZ()>start.z+4&&((Entity)sword).getY()>start.y+1,"server input flies forward and up");check(((Entity)sword).getDeltaMovement().length()>.7,"normal flight reaches requested speed");start=((Entity)sword).position();phase=8;wait=SpellService.now(p)+10;}
  else if(phase==8){check(((Entity)sword).getY()>start.y+8&&((Entity)sword).getDeltaMovement().y>1.2,"boost reaches higher authoritative speed");start=((Entity)sword).position();p.setShiftKeyDown(true);phase=9;wait=SpellService.now(p)+12;}
  else if(phase==9){check(p.getVehicle()==sword&&((Entity)sword).getY()<start.y-2,"held Shift descends and retains rider");p.setShiftKeyDown(false);sword.input(p,0,0,0,false,true);check(!p.isPassenger()&&p.hasEffect(net.minecraft.world.effect.MobEffects.SLOW_FALLING),"explicit double-Shift dismount has safe fall protection");phase=4;wait=SpellService.now(p)+70;}
  else if(phase==4){check(Math.abs(((Entity)sword).getY()-300.04)<.02,"unmounted sword descends and stops on the real floor");p.teleportTo(((Entity)sword).getX()-1,300,((Entity)sword).getZ());check(((Entity)sword).interact(p,InteractionHand.MAIN_HAND).consumesAction(),"same landed sword can be remounted");state.data.setPower(0);phase=5;wait=SpellService.now(p)+25;}
  else if(phase==5){check(((Entity)sword).isRemoved()&&!p.isPassenger(),"empty power dismisses sword and dismounts");p.teleportTo(p.serverLevel(),state.at.getX()+.5,300,state.at.getZ()+.5,0,0);state.data.setPower(state.data.powerMax());check(SpellService.begin(p,2).isEmpty(),"resummon has no cooldown");phase=6;wait=SpellService.now(p)+16;}
  else if(phase==6){var all=p.serverLevel().getEntitiesOfClass(SpiritSwordEntity.class,p.getBoundingBox().inflate(15),s->p.getUUID().equals(s.owner()));check(all.size()==1,"resummon creates exactly one sword");sword=all.get(0);power=state.data.power();check(SpellService.begin(p,2).isEmpty()&&((Entity)sword).isRemoved()&&state.data.power()==power&&!SpellService.casting(p),"second press instantly recalls grounded sword without payment");check(SpellService.begin(p,2).isEmpty(),"summon allowed immediately after recall");phase=7;wait=SpellService.now(p)+16;}
  else if(phase==7){var all=p.serverLevel().getEntitiesOfClass(SpiritSwordEntity.class,p.getBoundingBox().inflate(15),s->p.getUUID().equals(s.owner()));check(all.size()==1,"one sword after recall and summon");sword=all.get(0);check(((Entity)sword).interact(p,InteractionHand.MAIN_HAND).consumesAction()&&p.isPassenger(),"mount recalled replacement");power=state.data.power();check(SpellService.begin(p,2).isEmpty()&&((Entity)sword).isRemoved()&&!p.isPassenger()&&state.data.power()==power,"recall while riding dismounts without payment");check(p.hasEffect(net.minecraft.world.effect.MobEffects.SLOW_FALLING),"air recall retains fall protection");p.sendSystemMessage(Component.literal("[Spirit Sword QA] PASS "+state.checks+" assertions"));finish();return;}
  if(state!=null)drive(p);
 }catch(Exception|LinkageError failure){state.p.sendSystemMessage(Component.literal("[Spirit Sword QA] FAIL "+state.checks+": "+failure));finish();}}
 private static void drive(ServerPlayer p){if(sword==null||p.getVehicle()!=sword)return;if(phase==3)sword.input(p,1,0,.5F);else if(phase==8)sword.input(p,0,0,1,true,false);else if(phase==9){p.setShiftKeyDown(true);sword.input(p,0,0,-1,false,false);}}
 private SpiritSwordAudit(){}
}
