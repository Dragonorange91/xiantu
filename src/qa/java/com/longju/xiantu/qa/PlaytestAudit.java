package com.longju.xiantu.qa;

import com.google.gson.GsonBuilder;
import com.longju.xiantu.cultivation.*;
import com.longju.xiantu.gongfa.GongfaService;
import com.longju.xiantu.refining.*;
import com.longju.xiantu.spell.*;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.*;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.*;
import java.util.*;

/** Disposable real damage and continuous production-flight checks; never runs in a user world. */
@Mod.EventBusSubscriber(modid="xiantu_qa")
public final class PlaytestAudit {
 private static SpellAudit.Session state;
 private static final List<Map<String,Object>> rows=new ArrayList<>();
 private static Zombie attacker; private static SpiritSwordEntity sword;
 private static String task;private static int phase,age,flightIndex,flightStart,xpStart,levelStart;
 private static double initialPower,capacity;private static long deadline;
 private static CompoundTag food;private static List<MobEffectInstance> effects;private static boolean regeneration;
 public static volatile boolean steer,boost;public static volatile float heading;
 private static final String[] CASES={"naked","diamond","t1q1","t1q5","t4q1","t4q5","t7q1","t7q5"};
 private static final int[] TIERS={0,0,1,1,4,4,7,7},QUALITY={0,0,1,5,1,5,1,5};
 private static void check(boolean ok,String message){state.check(ok,message);}
 @SubscribeEvent public static void commands(RegisterCommandsEvent e){e.getDispatcher().register(Commands.literal("xiantuplaytest").requires(s->s.hasPermission(2))
  .then(Commands.literal("armor").executes(c->start(c.getSource().getPlayerOrException(),"armor")))
  .then(Commands.literal("flight").executes(c->start(c.getSource().getPlayerOrException(),"flight")))
  .then(Commands.literal("restore").executes(c->{if(state==null||state.p!=c.getSource().getPlayerOrException())return 0;finish("ABORT","requested");return 1;})));}
 private static int start(ServerPlayer p,String mode){
  if(state!=null||!p.server.getWorldPath(LevelResource.ROOT).normalize().getFileName().toString().startsWith("Xiantu-QA")){p.sendSystemMessage(Component.literal("[Playtest] Refused active/non-QA world"));return 0;}
  state=new SpellAudit.Session(p);task=mode;rows.clear();phase=age=flightIndex=0;food=new CompoundTag();p.getFoodData().addAdditionalSaveData(food);effects=p.getActiveEffects().stream().map(MobEffectInstance::new).toList();regeneration=p.level().getGameRules().getBoolean(GameRules.RULE_NATURAL_REGENERATION);
  try{state.prepare();SpellService.cancel(p);p.removeAllEffects();p.level().getGameRules().getRule(GameRules.RULE_NATURAL_REGENERATION).set(false,p.server);state.data.gongfa().clear();state.data.spells().load(new CompoundTag());state.data.medicine().clearTemporary();state.data.setDefenseBonus(0);p.getInventory().clearContent();((net.minecraft.world.Container)new com.longju.xiantu.equipment.AccessoryInventory(p)).clearContent();
   if(mode.equals("armor")){state.data.setRealmStage(8,0);GongfaService.refresh(p);attacker=EntityType.ZOMBIE.create(state.level);check(attacker!=null,"attacker exists");((Entity)attacker).moveTo(p.getX()+2,p.getY(),p.getZ(),0,0);attacker.setNoAi(true);attacker.getPersistentData().putBoolean(com.longju.xiantu.entity.BeastRealmService.INITIALIZED,true);attacker.getPersistentData().putInt(com.longju.xiantu.entity.BeastResourceDrops.REALM,8);attacker.getPersistentData().putInt(com.longju.xiantu.entity.BeastRealmService.STAGE,0);state.level.addFreshEntity(attacker);equip(0);deadline=SpellService.now(p)+100;}
   else{state.data.gongfa().learn("tai_chu_dao_jing");state.data.gongfa().commit("tai_chu_dao_jing",UUID.randomUUID());state.data.spells().learn("sword_flight");check(state.data.spells().configure(state.data.spells().revision(),List.of("sword_flight","","","","","")),"flight slot");prepareFlight();}
   p.sendSystemMessage(Component.literal("[Playtest] START "+mode));return 1;
  }catch(Exception|LinkageError ex){finish("FAIL",ex.toString());return 0;}
 }
 private static void equip(int index){var p=state.p;for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET))p.setItemSlot(slot,ItemStack.EMPTY);
  if(index==1){for(var item:List.of(Items.DIAMOND_HELMET,Items.DIAMOND_CHESTPLATE,Items.DIAMOND_LEGGINGS,Items.DIAMOND_BOOTS)){var s=new ItemStack(item);p.setItemSlot(((ArmorItem)item).getEquipmentSlot(),s);}}
  else if(index>1){int first=TIERS[index]==1?31:TIERS[index]==4?35:43;for(int n=first;n<first+4;n++){var r=RefiningRecipes.find(String.format("equipment_%03d_tier_%d",n,TIERS[index]));check(r!=null,"armor recipe "+n);var stack=RefinedEquipment.finish(r,QUALITY[index]);p.setItemSlot(((ArmorItem)stack.getItem()).getEquipmentSlot(),stack);}}
  EquipmentEffects.refresh(p);RefinedEquipment.refreshEligibility(p);
 }
 private static void sampleArmor(){var p=state.p;var row=new LinkedHashMap<String,Object>();row.put("case",CASES[phase]);row.put("realm",state.data.majorRealm());row.put("armor",p.getAttributeValue(Attributes.ARMOR));row.put("toughness",p.getAttributeValue(Attributes.ARMOR_TOUGHNESS));row.put("max_health",p.getMaxHealth());
  for(int damage:new int[]{30,120}){p.setHealth(p.getMaxHealth());p.setAbsorptionAmount(0);p.invulnerableTime=0;p.hurtTime=0;float before=p.getHealth();boolean accepted=p.hurt(state.level.damageSources().mobAttack(attacker),damage);double lost=before-p.getHealth();check(accepted&&lost>0&&lost<=damage+.01,"real damage "+CASES[phase]+"/"+damage+" accepted="+accepted+" lost="+lost+" immune="+p.isInvulnerableTo(state.level.damageSources().mobAttack(attacker))+" ability="+p.getAbilities().invulnerable);row.put("damage_"+damage,lost);}
  rows.add(row);phase++;if(phase==CASES.length){double diamond=((Number)rows.get(1).get("damage_120")).doubleValue(),tier1=((Number)rows.get(2).get("damage_120")).doubleValue();check(tier1<=diamond+.01,"tier1 no worse than diamond against same hit");for(int damage:new int[]{30,120}){for(int i:new int[]{2,4,6})check(((Number)rows.get(i+1).get("damage_"+damage)).doubleValue()<((Number)rows.get(i).get("damage_"+damage)).doubleValue(),"quality increases actual defense");for(int q=0;q<2;q++)for(int i:new int[]{2,4})check(((Number)rows.get(i+2+q).get("damage_"+damage)).doubleValue()<((Number)rows.get(i+q).get("damage_"+damage)).doubleValue(),"higher tier improves actual defense through native cap");}finish("PASS","same attacker; native armor plus ranked spiritual protection");}else{equip(phase);deadline=SpellService.now(p)+4;}
 }
 private static void prepareFlight(){var p=state.p;steer=false;boost=false;state.data.setRealmStage(flightIndex==0?1:2,0);GongfaService.refresh(p);state.data.setPowerMax(RealmRegistry.get(state.data.majorRealm(),0).powerCap());state.data.setPower(state.data.powerMax());initialPower=state.data.power();capacity=state.data.powerMax();p.teleportTo(state.level,state.at.getX()+.5,300,state.at.getZ()+.5,0,0);
  var at=SpiritSwordEntity.placement(p);check(at!=null,"production summon ground");sword=SpiritSwordEntity.summon(p,at);check(sword!=null,"production sword summoned");check(((Entity)sword).interact(p,InteractionHand.MAIN_HAND).consumesAction()&&p.getVehicle()==sword,"production mount interaction");
  // Deliberately fill after summon: measure the travel budget independently of summon payment.
  state.data.setPower(capacity);flightStart=age;xpStart=state.data.spells().experience("sword_flight");levelStart=state.data.spells().mastery("sword_flight");heading=0;steer=true;boost=flightIndex==1;phase=0;CultivationEvents.sync(p);
 }
 private static void sampleFlight(){var p=state.p;int elapsed=age-flightStart;heading=(elapsed/40)%2==0?0:180;
  if(p.getVehicle()==sword&&elapsed%1200==0){rows.add(Map.of("flight",flightIndex,"ticks",elapsed,"power",state.data.power(),"capacity",state.data.powerMax(),"experience",state.data.spells().experience("sword_flight"),"mastery",state.data.spells().mastery("sword_flight")));}
  if(p.getVehicle()!=sword){int expect=flightIndex==0?6000:3000;check(((Entity)sword).isRemoved(),"exhaustion removes sword");check(Math.abs(elapsed-expect)<=45,"real flight budget ticks actual="+elapsed+" expected="+expect);check(state.data.spells().experience("sword_flight")>xpStart||state.data.spells().mastery("sword_flight")>levelStart,"continuous moving flight gains mastery");rows.add(Map.of("completed",flightIndex,"ticks",elapsed,"initial_power",initialPower,"remaining_power",state.data.power(),"boost",flightIndex==1));steer=false;boost=false;
   if(++flightIndex==2){finish("PASS","Foundation normal and Golden Core boosted; real server ticks and production configurable-key packets");}else{((Entity)sword).discard();sword=null;deadline=SpellService.now(p)+20;phase=1;}
  }else check(elapsed<6500,"flight does not run forever");
 }
 @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e){if(e.phase!=TickEvent.Phase.END||state==null)return;try{age++;if(task.equals("armor")){if(SpellService.now(state.p)>=deadline)sampleArmor();}else if(phase==1){if(SpellService.now(state.p)>=deadline)prepareFlight();}else sampleFlight();}catch(Exception|LinkageError ex){finish("FAIL",ex.toString());}}
 private static void finish(String status,String detail){if(state==null)return;steer=false;boost=false;var s=state;String name=task;try{if(attacker!=null)((Entity)attacker).discard();attacker=null;if(s.p.isPassenger())s.p.stopRiding();SpiritSwordEntity.dismiss(s.p);s.p.removeAllEffects();s.p.level().getGameRules().getRule(GameRules.RULE_NATURAL_REGENERATION).set(regeneration,s.p.server);s.p.getFoodData().readAdditionalSaveData(food);s.finish();effects.forEach(s.p::addEffect);EquipmentEffects.refresh(s.p);RefinedEquipment.refreshEligibility(s.p);s.p.setHealth(Math.min(s.health,s.p.getMaxHealth()));
   var report=new LinkedHashMap<String,Object>();report.put("status",status);report.put("task",name);report.put("detail",detail);report.put("checks",s.checks);report.put("server_ticks",age);report.put("rows",List.copyOf(rows));Path folder=s.p.server.getWorldPath(LevelResource.ROOT).resolve("xiantu-playtest-20261026");Files.createDirectories(folder);Files.writeString(folder.resolve(name+".json"),new GsonBuilder().setPrettyPrinting().create().toJson(report));s.p.sendSystemMessage(Component.literal("[Playtest] "+status+" "+name+" checks="+s.checks+" "+detail));
  }catch(Exception|LinkageError ex){com.mojang.logging.LogUtils.getLogger().error("Playtest restore/report failed",ex);}finally{state=null;sword=null;}
 }
 private PlaytestAudit(){}
}
