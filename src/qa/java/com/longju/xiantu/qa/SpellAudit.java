package com.longju.xiantu.qa;

import com.longju.xiantu.spell.*;
import com.longju.xiantu.cultivation.*;
import com.longju.xiantu.gongfa.GongfaService;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Real server ticks, actual scroll callbacks and production casts; temporary state restored. */
@Mod.EventBusSubscriber(modid="xiantu_qa")
public final class SpellAudit {
 private static Session active;
 private static Session preview;
 static final class Session {
  final ServerPlayer p;final ServerLevel level;final PlayerCultivationData data;final CompoundTag saved=new CompoundTag(),persistent;
  final ListTag inventory;final Vec3 position;final float yaw,pitch,health,absorption;final GameType mode;
  final net.minecraft.world.Difficulty difficulty;final boolean noGravity;
  final Map<BlockPos,BlockState> blocks=new LinkedHashMap<>();final BlockPos at;
  int checks,phase;long wait;Zombie target;double power,initialDamage;
  Session(ServerPlayer p){this.p=p;level=p.serverLevel();data=(PlayerCultivationData)GongfaService.data(p);data.save(saved);persistent=p.getPersistentData().copy();inventory=p.getInventory().save(new ListTag());position=p.position();yaw=p.getYRot();pitch=p.getXRot();health=p.getHealth();absorption=p.getAbsorptionAmount();mode=p.gameMode.getGameModeForPlayer();difficulty=level.getDifficulty();noGravity=p.isNoGravity();at=p.blockPosition().offset(500,0,500).atY(299);}
  void check(boolean value,String text){checks++;if(!value)throw new IllegalStateException(text);}
  void prepare(){
   p.closeContainer();p.setGameMode(GameType.SURVIVAL);p.setNoGravity(true);p.hurtTime=0;p.server.setDifficulty(net.minecraft.world.Difficulty.NORMAL,true);
   for(int x=-2;x<=2;x++)for(int z=-2;z<=20;z++){var pos=at.offset(x,0,z);level.getChunkAt(pos);check(level.getBlockState(pos).isAir(),"isolated empty site");blocks.put(pos,level.getBlockState(pos));level.setBlock(pos,Blocks.STONE.defaultBlockState(),2);}
   p.teleportTo(level,at.getX()+.5,300,at.getZ()+.5,0,0);
   data.setCultivationUnlocked(true);data.setRealmStage(0,0);data.setPowerMax(100);data.setPhysique(PhysiqueType.ORDINARY);data.resetSpiritRoots();data.gongfa().clear();data.gongfa().learn("tai_chu_dao_jing");data.gongfa().commit("tai_chu_dao_jing",UUID.randomUUID());GongfaService.refresh(p);data.setPower(data.powerMax());data.setPowerSpeed(0);data.spells().load(new CompoundTag());
   p.getInventory().clearContent();p.getInventory().setItem(p.getInventory().selected,SpellScrolls.stack("fire_bolt"));
   var item=p.getMainHandItem();((Item)item.getItem()).use(level,p,InteractionHand.MAIN_HAND);check(item.isEmpty()&&data.spells().knows("fire_bolt"),"rootless player learns from real scroll and consumes once");
   p.getInventory().setItem(p.getInventory().selected,SpellScrolls.stack("fire_bolt"));((Item)p.getMainHandItem().getItem()).use(level,p,InteractionHand.MAIN_HAND);check(p.getMainHandItem().getCount()==1,"known scroll not consumed");
   var ids=new ArrayList<>(Collections.nCopies(SpellData.TOTAL_SLOTS,""));ids.set(0,"fire_bolt");int rev=data.spells().revision();check(data.spells().configure(rev,ids),"server config accepts known slot");check(!data.spells().configure(rev,ids),"stale revision rejected");
   ids.set(1,"fire_bolt");check(!data.spells().configure(data.spells().revision(),ids),"duplicate slot rejected");ids.set(1,"");ids.set(4,"fire_bolt");check(!data.spells().configure(data.spells().revision(),ids),"active in reaction rejected");
   check(SpellData.ACTIVE_SLOTS==4&&SpellData.PASSIVE_SLOTS==2&&SpellData.TOTAL_SLOTS==6,"four active and two passive slots");
   check(!data.spells().configure(data.spells().revision(),Collections.nCopies(8,"")),"eight-slot configuration rejected");
   var obsolete=data.spells().save();obsolete.getCompound("Slots").putString("s6","fire_bolt");obsolete.getCompound("Slots").putString("s7","fire_bolt");
   var bounded=new SpellData();bounded.load(obsolete);check(bounded.slot(6).isEmpty()&&bounded.slot(7).isEmpty()&&!bounded.save().getCompound("Slots").contains("s6")&&!bounded.save().getCompound("Slots").contains("s7"),"removed passive slots cannot survive load");
   var copied=new PlayerCultivationData();copied.copyFrom(data);check(copied.spells().knows("fire_bolt")&&copied.spells().slot(0).equals("fire_bolt"),"capability copy preserves independent spells");
   var save=new CompoundTag();data.save(save);copied.load(save);check(copied.spells().save().equals(data.spells().save()),"capability NBT round trip");
   check(SpellCatalog.ALL.size()==5&&SpellCatalog.find("heaven_thunder")==null&&SpellScrolls.ITEMS.size()==5,"five current spell definitions and scrolls registered");
   var learnedBeforeRealmChecks=data.spells().save();
   for(var candidate:SpellCatalog.ALL){
    data.spells().learn(candidate.id());
    for(int realm=candidate.realm();realm<=8;realm++){data.setRealmStage(realm,0);check(SpellService.denial(p,candidate,true).isEmpty(),"learned low spell usable in higher realm "+realm+" "+candidate.id());}
    if(candidate.realm()>0){data.setRealmStage(candidate.realm()-1,0);check(!SpellService.denial(p,candidate,false).isEmpty(),"minimum learning realm remains "+candidate.id());}
   }
   data.spells().load(learnedBeforeRealmChecks);
   data.setRealmStage(0,0);GongfaService.refresh(p);data.setPower(data.powerMax());
   power=data.power();check(SpellService.begin(p,0).isEmpty(),"first cast begins");check(!SpellService.begin(p,0).isEmpty(),"duplicate intent rejected");check(data.power()==power&&data.spells().cooldown("fire_bolt",SpellService.now(p))==0,"windup has no debit or cooldown");
   SpellService.cancel(p);check(!SpellService.casting(p)&&data.power()==power&&data.spells().cooldown("fire_bolt",SpellService.now(p))==0,"cancel before release has no debit");
   wait=SpellService.now(p)+5;phase=1;
  }
  void step(){
   if(SpellService.now(p)<wait)return;
   if(phase==1){power=data.power();String denial=SpellService.begin(p,0);check(denial.isEmpty(),"new attempt starts after global gap: "+denial);wait=SpellService.now(p)+10;phase=2;}
   else if(phase==2){check(!SpellService.casting(p)&&data.spells().cooldown("fire_bolt",SpellService.now(p))>0,"real ticks release and start cooldown");check(data.power()<power,"missed spell still consumes power");check(data.spells().experience("fire_bolt")==0,"empty cast no mastery");check(!SpellService.begin(p,0).isEmpty(),"cooldown prevents second release");wait=SpellService.now(p)+45;phase=3;}
   else if(phase==3){target=EntityType.ZOMBIE.create(level);((Entity)target).moveTo(at.getX()+.5,300,at.getZ()+6.5,180,0);((Mob)target).setNoAi(true);((Entity)target).setNoGravity(true);target.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.CARVED_PUMPKIN));target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);target.setHealth(100);target.getPersistentData().putInt(com.longju.xiantu.entity.BeastResourceDrops.REALM,0);level.addFreshEntity(target);target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);target.setHealth(100);check(SpellService.begin(p,0).isEmpty(),"targeted projectile begins");wait=SpellService.now(p)+18;phase=4;}
   else if(phase==4){check(target.getHealth()<100,"server projectile hits real entity");check(data.spells().experience("fire_bolt")==1,"one valid hit one mastery award");int exp=data.spells().experience("fire_bolt");var cast=UUID.randomUUID();data.spells().award("fire_bolt",1000,SpellService.now(p),cast,((Entity)target).getUUID());check(data.spells().experience("fire_bolt")==exp,"same five-second window cannot farm mastery");check(data.spells().cooldown("fire_bolt",SpellService.now(p))>0,"projectile hit does not reset cooldown");
    initialDamage=100-target.getHealth();
    var state=data.spells().save();var xp=state.getCompound("XP");xp.putInt("fire_boltLevel",9);state.put("XP",xp);data.spells().load(state);
    data.setRealmStage(3,0);GongfaService.refresh(p);data.setPower(data.powerMax());
    target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(5000);target.setHealth(5000);target.invulnerableTime=0;
    check(SpellService.damagePreview(p,SpellCatalog.find("fire_bolt"))>50,"server preview includes absolute realm and mastery growth");
    var low=com.longju.xiantu.gongfa.GongfaRegistry.get("xuan_shui_jue");check(low.learnableAt(8,0)&&low.usableAt(8),"high realm low-method learning and effects allowed");
    data.gongfa().learn(low.id());data.gongfa().commit(low.id(),UUID.randomUUID());check(GongfaService.active(data)==low,"lower method remains active beyond its breakthrough ceiling");check(!GongfaService.breakthroughDenial(data).isEmpty(),"lower method breakthrough ceiling retained");
    wait=SpellService.now(p)+45;phase=5;}
   else if(phase==5){check(SpellService.begin(p,0).isEmpty(),"grown spell begins with lower method active");wait=SpellService.now(p)+18;phase=6;}
   else if(phase==6){check(5000-target.getHealth()>initialDamage*5,"actual projectile damage grows with realm and mastery");
    p.getInventory().setItem(p.getInventory().selected,SpellScrolls.stack("thunder_strike"));
    ((Item)p.getMainHandItem().getItem()).use(level,p,InteractionHand.MAIN_HAND);
    check(p.getMainHandItem().isEmpty()&&data.spells().knows("thunder_strike"),"rootless player learns thunder scroll and consumes once");
    check(data.spells().configure(data.spells().revision(),List.of("fire_bolt","thunder_strike","","","","")),"thunder active slot configured");
    target.setHealth(5000);target.invulnerableTime=0;power=data.power();
    check(SpellService.begin(p,1).isEmpty(),"thunder targeted cast begins");
    check(target.getHealth()==5000&&data.power()==power,"thunder windup has no damage or payment");
    wait=SpellService.now(p)+18;phase=7;}
   else if(phase==7){check(target.getHealth()<5000,"real thunder release damages visible target");
    check(data.power()<power,"thunder release pays actual power");
    check(data.spells().cooldown("thunder_strike",SpellService.now(p))>0,"thunder release starts cooldown");
    check(data.spells().experience("thunder_strike")==2,"thunder hit credits tier-three mastery once");
    check(!SpellService.begin(p,1).isEmpty(),"thunder cooldown blocks duplicate cast");
    initialDamage=target.getHealth();wait=SpellService.now(p)+22;phase=8;}
   else if(phase==8){check(target.getHealth()==initialDamage,"thunder presentation does not repeat damage");
    p.sendSystemMessage(Component.literal("[Spell QA] PASS "+checks+" assertions"));finish();}
  }
  void finish(){
   SpellService.cancel(p);SpellService.logout(new net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(p));if(target!=null)((Entity)target).discard();blocks.forEach((pos,state)->level.setBlock(pos,state,2));
   p.closeContainer();p.getInventory().load(inventory);p.getPersistentData().getAllKeys().stream().toList().forEach(p.getPersistentData()::remove);p.getPersistentData().merge(persistent);data.load(saved);p.setNoGravity(noGravity);p.setGameMode(mode);p.server.setDifficulty(difficulty,true);p.teleportTo(level,position.x,position.y,position.z,yaw,pitch);GongfaService.refresh(p);p.setHealth(health);p.setAbsorptionAmount(absorption);p.inventoryMenu.broadcastChanges();CultivationEvents.sync(p);SpellService.sync(p);active=null;
  }
 }
 @SubscribeEvent public static void commands(RegisterCommandsEvent e){e.getDispatcher().register(Commands.literal("xiantuspellaudit").executes(c->{var p=c.getSource().getPlayerOrException();if(active!=null||preview!=null||!p.server.getWorldPath(LevelResource.ROOT).normalize().getFileName().toString().startsWith("Xiantu-QA")){p.sendSystemMessage(Component.literal("[Spell QA] Refused: active audit/preview or non-QA world"));return 0;}active=new Session(p);try{active.prepare();p.sendSystemMessage(Component.literal("[Spell QA] Started; testing real server ticks"));return 1;}catch(Exception|LinkageError failure){p.sendSystemMessage(Component.literal("[Spell QA] FAIL "+active.checks+": "+failure));active.finish();throw failure;}}));}
 @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e){if(e.phase!=TickEvent.Phase.END||active==null)return;try{active.step();}catch(Exception|LinkageError failure){active.p.sendSystemMessage(Component.literal("[Spell QA] FAIL "+active.checks+": "+failure));active.finish();}}
 @SubscribeEvent public static void preview(RegisterCommandsEvent e){e.getDispatcher().register(Commands.literal("xiantuspellpreview")
  .executes(c->{var p=c.getSource().getPlayerOrException();if(active!=null||preview!=null||!p.server.getWorldPath(LevelResource.ROOT).normalize().getFileName().toString().startsWith("Xiantu-QA"))return 0;preview=new Session(p);p.closeContainer();var d=preview.data;d.setCultivationUnlocked(true);d.setRealmStage(5,0);for(var root:SpiritRootType.values())d.addSpiritRoot(root);d.gongfa().learn("tai_chu_dao_jing");d.gongfa().commit("tai_chu_dao_jing",UUID.randomUUID());for(var s:SpellCatalog.ALL)if(SpellCatalog.enabled(s))d.spells().learn(s.id());d.spells().configure(d.spells().revision(),List.of("fire_bolt","thunder_strike","sword_flight","","",""));SpellMenu.open(p);return 1;})
  .then(Commands.literal("restore").executes(c->{if(preview==null||preview.p!=c.getSource().getPlayerOrException())return 0;preview.finish();preview=null;c.getSource().sendSuccess(()->Component.literal("[Spell QA] Preview restored"),false);return 1;})));}
 private SpellAudit(){}
}
