package com.longju.xiantu.qa;
import com.longju.xiantu.talisman.*;
import com.longju.xiantu.cultivation.*;
import com.longju.xiantu.gongfa.GongfaService;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;
/** Tick-driven real item use and table transactions; isolated save only, restores player and fixture. */
@Mod.EventBusSubscriber(modid="xiantu_qa")
public final class TalismanRuntimeAudit {
 private static Session active;
 @SubscribeEvent public static void commands(RegisterCommandsEvent e){e.getDispatcher().register(Commands.literal("xiantutalismanruntime").requires(s->s.hasPermission(2)).then(Commands.argument("case",com.mojang.brigadier.arguments.StringArgumentType.word()).executes(c->{
  var p=c.getSource().getPlayerOrException();String id=com.mojang.brigadier.arguments.StringArgumentType.getString(c,"case");
  if(!p.server.getWorldPath(LevelResource.ROOT).normalize().getFileName().toString().equals("Xiantu-QA-BeastFlat-20261017"))return 0;
  if(id.equals("restore")){if(active!=null){active.finish(new RuntimeException("aborted"));active=null;}return 1;}
  if(active!=null||!Set.of("table","auto_make","manual_make").contains(id)&&(TalismanCatalog.find(id)==null||id.equals("rebirth")))return 0;
  active=new Session(p,id);try{active.prepare();return 1;}catch(Exception|LinkageError ex){var done=active;active=null;done.finish(ex);return 0;}
 })));}
 @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e){if(e.phase!=TickEvent.Phase.END||active==null)return;try{active.step();}catch(Exception|LinkageError ex){var done=active;active=null;done.finish(ex);}}
 private static final class Session {
  final ServerPlayer p;final String id;final PlayerCultivationData d;final CompoundTag cultivation=new CompoundTag(),persistent;
  final ListTag inventory;final Vec3 position;final float yaw,pitch,health,absorption;final GameType mode;final boolean gravity,sneak;final List<MobEffectInstance> effects;
  final List<Mob> targets=new ArrayList<>();BlockPos tablePos;net.minecraft.world.level.block.state.BlockState oldBlock,oldSecond;TalismanTableEntity table;Vec3 origin;int age,checks;float targetHealth;
  Session(ServerPlayer p,String id){this.p=p;this.id=id;d=(PlayerCultivationData)GongfaService.data(p);d.save(cultivation);persistent=p.getPersistentData().copy();inventory=p.getInventory().save(new ListTag());position=p.position();yaw=p.getYRot();pitch=p.getXRot();health=p.getHealth();absorption=p.getAbsorptionAmount();mode=p.gameMode.getGameModeForPlayer();gravity=p.isNoGravity();sneak=p.isShiftKeyDown();effects=p.getActiveEffects().stream().map(MobEffectInstance::new).toList();}
  void check(boolean b,String s){checks++;if(!b)throw new IllegalStateException(s);}
  void prepare(){p.closeContainer();p.stopUsingItem();p.setGameMode(GameType.CREATIVE);p.setNoGravity(true);p.setShiftKeyDown(false);p.removeAllEffects();p.getInventory().clearContent();
   d.setCultivationUnlocked(true);d.setRealmStage(8,0);d.setPowerMax(1000000);d.setPower(1000000);d.setPowerSpeed(0);p.getPersistentData().remove("xiantu_talisman_effects");
   p.teleportTo(p.serverLevel(),5100.5,-60,5100.5,0,10);origin=p.position();GongfaService.refresh(p);p.setHealth(p.getMaxHealth());
   if(Set.of("table","long_return","auto_make","manual_make").contains(id)){
    tablePos=p.blockPosition().east();oldBlock=p.serverLevel().getBlockState(tablePos);check(oldBlock.isAir(),"fixture must be empty");
    Block block=net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(new net.minecraft.resources.ResourceLocation("xiantu:talisman_writing_table"));check(block!=null&&block!=Blocks.AIR,"registered table");
    oldSecond=p.serverLevel().getBlockState(tablePos.east());check(oldSecond.isAir(),"second fixture cell must be empty");p.serverLevel().setBlock(tablePos,block.defaultBlockState(),3);block.setPlacedBy(p.serverLevel(),tablePos,block.defaultBlockState(),p,ItemStack.EMPTY);check(p.serverLevel().getBlockEntity(tablePos) instanceof TalismanTableEntity,"native table block entity");table=(TalismanTableEntity)p.serverLevel().getBlockEntity(tablePos);
   }
   if(id.endsWith("_make")){
    var recipe=TalismanRecipes.ALL.stream().filter(x->x.id().equals("fireball")&&x.tier()==1).findFirst().orElseThrow();table.selection=TalismanRecipes.ALL.indexOf(recipe);table.mode=0;table.energy=TalismanTableEntity.CAPACITY;
    var container=(net.minecraft.world.Container)table;container.setItem(0,new ItemStack(TalismanItems.BRUSHES.get(2).get()));container.setItem(1,TalismanItems.paper(1,1,1));
    for(int i=0;i<recipe.ingredients().size();i++)container.setItem(i+2,recipe.ingredients().get(i).example());
    // Knowledge is deliberately absent to exercise denial before learning the exact tome band.
    p.getPersistentData().remove(TalismanProgress.KEY);check(!table.start(p,id.equals("manual_make")).isEmpty()&&!table.busy(),"unknown tome denies manufacture");TalismanProgress.learn(p,recipe.knowledge());
    check(table.start(p,id.equals("manual_make")).isEmpty(),"real symbol manufacture starts");check(table.busy()&&!table.committed&&container.getItem(9).isEmpty(),"symbol is timed and not committed instantly");
    if(id.equals("manual_make"))net.minecraftforge.network.NetworkHooks.openScreen(p,table,b->b.writeBlockPos(tablePos));return;
   }
   if(id.equals("table")){
    var r=TalismanRecipes.ALL.stream().filter(x->x.id().equals("paper_1")).findFirst().orElseThrow();table.selection=TalismanRecipes.ALL.indexOf(r);TalismanProgress.learn(p,r.knowledge());
    for(int i=0;i<r.ingredients().size();i++)((net.minecraft.world.Container)table).setItem(i+1,r.ingredients().get(i).example());table.mode=0;table.energy=TalismanTableEntity.CAPACITY;
    check(table.start(p,false).isEmpty(),"table accepts exact recipe");check(table.busy()&&!table.committed&&((net.minecraft.world.Container)table).getItem(9).isEmpty(),"timed output, no instant result");check(table.cancel(p),"precommit cancel");
    check(((net.minecraft.world.Container)table).getItem(1).getCount()==2&&((net.minecraft.world.Container)table).getItem(2).getCount()==2,"cancel returns exact material quantities");check(table.start(p,false).isEmpty(),"retry after cancellation");return;
   }
   var def=TalismanCatalog.find(id);var stack=TalismanItems.symbol(id,Math.max(6,def.minimum()),1);p.setItemInHand(InteractionHand.MAIN_HAND,stack);
   if(id.equals("long_return")){
    p.setShiftKeyDown(true);Block block=p.serverLevel().getBlockState(tablePos).getBlock();block.use(p.serverLevel().getBlockState(tablePos),p.serverLevel(),tablePos,p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(tablePos),net.minecraft.core.Direction.UP,tablePos,false));p.setShiftKeyDown(false);
    check(stack.getTagElement("TalismanReturn")!=null,"native shift-use binds return item");p.teleportTo(p.serverLevel(),5116.5,-60,5100.5,0,10);origin=p.position();
   }
   if(Set.of("thunder","fireball","sword_qi","explosion","soul_seal","spirit_lock","demon_seal").contains(id)){
    Mob target=Set.of("soul_seal","spirit_lock","demon_seal").contains(id)?com.longju.xiantu.registry.ModEntities.YANWEI_FOX.get().create(p.serverLevel()):EntityType.ZOMBIE.create(p.serverLevel());
    target.setNoAi(true);target.setNoGravity(true);target.setPersistenceRequired();((Entity)target).moveTo(p.getX(),p.getY(),p.getZ()+3,180,0);p.serverLevel().addFreshEntity(target);target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000000);target.setHealth(1000000);targets.add(target);targetHealth=target.getHealth();
    if(com.longju.xiantu.entity.BeastRealmService.isBeast(target)){target.getPersistentData().putInt(com.longju.xiantu.entity.BeastResourceDrops.REALM,0);target.getPersistentData().putInt(com.longju.xiantu.entity.BeastRealmService.STAGE,0);}
   }
   if(id.equals("healing"))p.setHealth(p.getMaxHealth()*.4f);
   if(id.equals("detox")||id.equals("cleanse"))p.addEffect(new MobEffectInstance(MobEffects.POISON,600));
   p.inventoryMenu.broadcastChanges();CultivationEvents.sync(p);
  }
  void step(){age++;
   if(id.endsWith("_make")){
    var container=(net.minecraft.world.Container)table;
    if(id.equals("manual_make")&&table.busy()){
     int count=TalismanCatalog.find("fireball").strokes(),index=table.drawing.stroke,t=table.elapsed;
     if(index<count){int start=TalismanControlRules.strokeStart(table.total(),count,index),end=TalismanControlRules.strokeEnd(table.total(),count,index);
      if(t>=start&&t<=end-3){var path=TalismanDrawing.resample(TalismanDrawing.template("fireball",index));int at=(int)Math.floor((path.size()-1)*Math.min(1,(t-start)/(double)(end-start-3)));
       int first=Math.max(0,at-2);table.stroke(p,table.nonce,index,path.subList(first,at+1),t==end-3);
      }
     }else if(t>=table.total()*85/100&&!table.sealed)check(table.seal(p),"native seal accepted after complete drawing");
    }
    if(age==10)check(table.busy()&&container.getItem(9).isEmpty(),"no instant symbol output");
    if(age==290&&id.equals("manual_make"))check(table.drawing.stroke==TalismanCatalog.find("fireball").strokes()&&table.sealed&&table.drawing.mistakes==0,"all timed strokes completed precisely and sealed");
    if(age>=315){check(!table.busy(),"native symbol timer completes");var out=container.getItem(9);check(out.is(TalismanItems.SYMBOLS.get("fireball").get())||out.is(TalismanItems.RESIDUE.get()),"valid success or failure output");
     check(container.getItem(0).getDamageValue()==1,"exactly one brush durability committed");for(int i=1;i<=5;i++)check(container.getItem(i).isEmpty(),"reserved symbol material consumed "+i);
     check(table.energy==TalismanTableEntity.CAPACITY-TalismanRules.energy(1)*1000L,"exact symbol energy consumed");
     if(id.equals("manual_make"))check(table.drawing.stroke==0,"drawing session cleared on completion");
     if(out.is(TalismanItems.SYMBOLS.get("fireball").get())){check(out.getTag().hasUUID("Maker")&&out.getTag().getUUID("Maker").equals(p.getUUID()),"real maker identity");check(TalismanProgress.xp(p)==10,"profession experience awarded once");}
     active=null;finish(null);
    }return;
   }
   if(id.equals("table")){
    if(age==10)check(table.busy()&&((net.minecraft.world.Container)table).getItem(9).isEmpty(),"table waits real server ticks");
    if(age==70)check(table.committed,"automatic transaction commits once");
    if(age>=215){check(!table.busy(),"timed craft completes");var out=((net.minecraft.world.Container)table).getItem(9);check(out.is(TalismanItems.PAPER.get())&&out.getCount()==4&&TalismanItems.tier(out)==1,"correct timed output");check(table.energy<TalismanTableEntity.CAPACITY,"energy consumed");active=null;finish(null);}return;
   }
   if(age==20){Item item=p.getMainHandItem().getItem();var result=item.use(p.level(),p,InteractionHand.MAIN_HAND);check(result.getResult().consumesAction(),"native symbol use accepted");check(TalismanEffects.casting(p),"real preparation registered");check(p.getMainHandItem().getCount()==1,"preparation does not consume");}
   if(age==23)check(p.getMainHandItem().getCount()==1,"not instant release");
   if(age==32&&Set.of("soul_seal","spirit_lock","demon_seal").contains(id)){var target=targets.get(0);check(id.equals("soul_seal")?target.getPersistentData().getCompound("xiantu_talisman_effects").getLong("soul_seal_until")>com.longju.xiantu.spell.SpellService.clock():TalismanEffects.locked(target),"native control applied");}
   if(age>=150){check(!TalismanEffects.casting(p),"preparation finished");check(p.getMainHandItem().isEmpty(),"successful release consumes exactly one");
    var n=p.getPersistentData().getCompound("xiantu_talisman_effects");check(n.getLong("cd_"+id)>0,"server cooldown recorded");
    if(Set.of("thunder","fireball","sword_qi","explosion").contains(id))check(targets.get(0).getHealth()<targetHealth,"real target damage");
    if(id.equals("healing"))check(p.getHealth()>p.getMaxHealth()*.4f,"real healing");
    if(id.equals("detox")||id.equals("cleanse"))check(!p.hasEffect(MobEffects.POISON),"native poison removed");
    if(Set.of("earth_escape","blink","long_return").contains(id))check(p.position().distanceToSqr(origin)>4,"safe teleport displaced player");
    if(id.equals("long_return"))check(p.distanceToSqr(tablePos.getCenter())<20,"returned to bound table");active=null;finish(null);
   }
  }
  void finish(Throwable failure){String text="[Talisman runtime QA "+(failure==null?"PASS":"FAIL")+"] "+id+" checks="+checks+(failure==null?"":" "+failure);com.mojang.logging.LogUtils.getLogger().info(text);p.sendSystemMessage(Component.literal(text));
   try{var path=p.server.getWorldPath(LevelResource.ROOT).resolve("xiantu-talisman-qa");java.nio.file.Files.createDirectories(path);java.nio.file.Files.writeString(path.resolve(id+".txt"),text);}catch(Exception ex){com.mojang.logging.LogUtils.getLogger().error("Talisman QA report",ex);}
   p.stopUsingItem();p.closeContainer();TalismanReturn.cancel(p);targets.forEach(t->((Entity)t).discard());
   if(table!=null){table.cancel(p);((net.minecraft.world.Container)table).clearContent();p.serverLevel().setBlock(tablePos,oldBlock,3);p.serverLevel().setBlock(tablePos.east(),oldSecond,3);}
   p.getInventory().load(inventory);for(var key:new ArrayList<>(p.getPersistentData().getAllKeys()))p.getPersistentData().remove(key);p.getPersistentData().merge(persistent);d.load(cultivation);p.removeAllEffects();effects.forEach(p::addEffect);p.setGameMode(mode);p.setNoGravity(gravity);p.setShiftKeyDown(sneak);p.teleportTo(p.serverLevel(),position.x,position.y,position.z,yaw,pitch);GongfaService.refresh(p);com.longju.xiantu.refining.RefinedEquipment.refreshEligibility(p);com.longju.xiantu.refining.EquipmentEffects.refresh(p);p.setHealth(Math.min(health,p.getMaxHealth()));p.setAbsorptionAmount(absorption);p.inventoryMenu.broadcastChanges();CultivationEvents.sync(p);
  }
 }
 private TalismanRuntimeAudit(){}
}
