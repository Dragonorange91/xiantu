package com.longju.xiantu.qa;

import com.longju.xiantu.refining.*;
import com.longju.xiantu.cultivation.*;
import com.longju.xiantu.equipment.*;
import com.longju.xiantu.gongfa.GongfaService;
import com.longju.xiantu.mining.MetalMaterialRegistry;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.Container;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Real registered items, real menu payments, original player state restored in finally. */
@Mod.EventBusSubscriber(modid="xiantu_qa")
public final class EquipmentCodeAudit {
 private static int checks;
 private static void check(boolean value,String message){checks++;if(!value)throw new IllegalStateException(message);}
 private static void near(double actual,double expected,String message){check(Math.abs(actual-expected)<.002,message+" actual="+actual+" expected="+expected);}
 private static RefiningRecipes.Recipe recipe(String theme,int tier){return RefiningRecipes.ALL.stream().filter(r->r.theme().equals(theme)&&r.tier()==tier).findFirst().orElseThrow();}
 private static void accessories(ServerPlayer p,ItemStack a,ItemStack b,ItemStack c){var inv=new AccessoryInventory(p);((Container)inv).setItem(0,a);((Container)inv).setItem(1,b);((Container)inv).setItem(2,c);((Container)inv).setChanged();}
 private static void tick(ServerPlayer p){EquipmentEffects.tick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END,p));}
 @SubscribeEvent public static void register(RegisterCommandsEvent event){event.getDispatcher().register(Commands.literal("xiantuequipmentcodeaudit").executes(context->{
  var p=context.getSource().getPlayerOrException();
  if(!p.server.getWorldPath(LevelResource.ROOT).normalize().getFileName().toString().startsWith("Xiantu-QA")){context.getSource().sendFailure(Component.literal("仅限QA存档"));return 0;}
  p.closeContainer();checks=0;var d=(PlayerCultivationData)GongfaService.data(p);var saved=new CompoundTag();d.save(saved);
  var persistent=p.getPersistentData().copy();var inventory=p.getInventory().save(new ListTag());var mode=p.gameMode.getGameModeForPlayer();var health=p.getHealth();var absorption=p.getAbsorptionAmount();
  var pos=p.position();float yaw=p.getYRot(),pitch=p.getXRot();var l=p.serverLevel();var at=p.blockPosition().offset(180,0,180).atY(300);l.getChunkAt(at);var old=l.getBlockState(at);
  try{
   check(old.isAir(),"isolated repair site is empty");p.setGameMode(GameType.SURVIVAL);d.setCultivationUnlocked(true);d.setRealmStage(8,0);d.setPhysique(PhysiqueType.ORDINARY);d.medicine().clearTemporary();d.gongfa().clear();
   for(String key:List.of("xiantu_equipment_power_bonus","xiantu_gongfa_power_max_bonus","xiantu_physique_power_max_bonus","xiantu_pill_power_bonus"))p.getPersistentData().remove(key);
   p.getInventory().clearContent();accessories(p,ItemStack.EMPTY,ItemStack.EMPTY,ItemStack.EMPTY);d.setPowerMax(1000);d.setPower(500);GongfaService.refresh(p);EquipmentEffects.refresh(p);p.setHealth(p.getMaxHealth());
   double baseline=d.powerMax();float baseHealth=p.getMaxHealth();
   for(var r:RefiningRecipes.ALL)if(r.theme().equals("沧溟镇岳甲")&&r.tier()==1){var s=RefinedEquipment.finish(r,1);var armor=(ArmorItem)s.getItem();p.setItemSlot(armor.getEquipmentSlot(),s);}
   check(EquipmentEffects.read(p).set().equals("沧溟镇岳甲"),"ordinary set requires four registered armor slots");EquipmentEffects.refresh(p);near(p.getMaxHealth(),baseHealth*1.06,"complete armor health bonus");
   double once=p.getMaxHealth();
   for(int i=0;i<40;i++)EquipmentEffects.refresh(p);near(p.getMaxHealth(),once,"repeated ticks never stack armor health buff");
   var helmet=p.getItemBySlot(EquipmentSlot.HEAD);p.setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);EquipmentEffects.refresh(p);near(p.getMaxHealth(),baseHealth,"three pieces revoke entire set");p.setItemSlot(EquipmentSlot.HEAD,helmet);
   var wrong=RefinedEquipment.finish(RefiningRecipes.ALL.stream().filter(r->r.theme().equals("归藏曜甲")&&r.slot().equals("helmet")).findFirst().orElseThrow(),1);p.setItemSlot(EquipmentSlot.HEAD,wrong);EquipmentEffects.refresh(p);near(p.getMaxHealth(),baseHealth,"mixed themes revoke set bonus");p.setItemSlot(EquipmentSlot.HEAD,helmet);
   var poor=helmet.copy();poor.setDamageValue(poor.getMaxDamage());p.setItemSlot(EquipmentSlot.HEAD,poor);EquipmentEffects.refresh(p);near(p.getMaxHealth(),baseHealth,"broken part revokes armor buff");p.setItemSlot(EquipmentSlot.HEAD,helmet);
   d.setCultivationUnlocked(false);check(EquipmentEffects.read(p).set().isEmpty(),"realm eligibility revokes full set");d.setCultivationUnlocked(true);
   var rankedHelmet=RefinedEquipment.finish(RefinedEquipment.recipe(helmet),5);p.setItemSlot(EquipmentSlot.HEAD,rankedHelmet);
   near(EquipmentEffects.armorProtection(p),.02,"one supreme tier-one piece supplies two percent spiritual protection");
   var ordinaryHit=new net.minecraftforge.event.entity.living.LivingHurtEvent(p,p.damageSources().mobAttack(p),100);EquipmentEffects.hurt(ordinaryHit);near(ordinaryHit.getAmount(),98,"ordinary damage applies per-piece protection");
   var magicHit=new net.minecraftforge.event.entity.living.LivingHurtEvent(p,p.damageSources().magic(),100);EquipmentEffects.hurt(magicHit);near(magicHit.getAmount(),100,"armor-bypassing magic does not gain spiritual protection");
   rankedHelmet.setDamageValue(rankedHelmet.getMaxDamage());near(EquipmentEffects.armorProtection(p),0,"broken part loses spiritual protection");
   rankedHelmet.setDamageValue(0);d.setCultivationUnlocked(false);near(EquipmentEffects.armorProtection(p),0,"ineligible player loses spiritual protection");d.setCultivationUnlocked(true);
   for(var slot:List.of(EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET))p.setItemSlot(slot,ItemStack.EMPTY);
   accessories(p,ItemStack.EMPTY,ItemStack.EMPTY,ItemStack.EMPTY);
   double bareRecovery=com.longju.xiantu.formation.FormationSeatEntity.recoveryPerTick(p);
   accessories(p,RefinedEquipment.finish(recipe("聚灵戒",1),1),ItemStack.EMPTY,ItemStack.EMPTY);
   near(com.longju.xiantu.formation.FormationSeatEntity.recoveryPerTick(p),bareRecovery*1.05,"cushion recovery includes actual equipped ring");
   accessories(p,ItemStack.EMPTY,ItemStack.EMPTY,ItemStack.EMPTY);
   near(com.longju.xiantu.formation.FormationSeatEntity.recoveryPerTick(p),bareRecovery,"removing ring revokes cushion recovery bonus");
   var ring=RefinedEquipment.finish(recipe("玄枢戒",1),1);var stronger=RefinedEquipment.finish(recipe("玄枢戒",3),5);
   accessories(p,ring,stronger,ItemStack.EMPTY);near(EquipmentEffects.powerBonus(p),.05*WeaponPassives.scale(stronger),"duplicate ring uses stronger copy only");tick(p);
   double equipped=d.powerMax();near(equipped,baseline*(1+EquipmentEffects.powerBonus(p)),"equipment capacity derives from raw base");
   for(int i=0;i<25;i++){GongfaService.refresh(p);com.longju.xiantu.alchemy.MedicineService.refreshCapacity(p);tick(p);}near(d.powerMax(),equipped,"refresh cannot compound equipment capacity");
   accessories(p,ring,ItemStack.EMPTY,RefinedEquipment.finish(recipe("玄灵链",1),1));tick(p);near(d.powerMax(),baseline*1.09,"different accessories add in same capacity zone");
   accessories(p,ItemStack.EMPTY,ItemStack.EMPTY,ItemStack.EMPTY);tick(p);near(d.powerMax(),baseline,"unequip removes capacity without drift");near(d.power(),500,"capacity changes do not refill power");
   var malformed=ring.copy();malformed.getOrCreateTag().getCompound(RefinedEquipment.KEY).putString("Recipe","invalid");accessories(p,malformed,ItemStack.EMPTY,ItemStack.EMPTY);check(EquipmentEffects.powerBonus(p)==0,"unregistered recipe cannot create accessory bonus");
   accessories(p,ItemStack.EMPTY,ItemStack.EMPTY,RefinedEquipment.finish(recipe("神灵链",6),1));p.getPersistentData().putLong("xiantu_gear_life_guard_until",com.longju.xiantu.spell.SpellService.now(p)+20);accessories(p,ItemStack.EMPTY,ItemStack.EMPTY,ItemStack.EMPTY);tick(p);check(!p.getPersistentData().contains("xiantu_gear_life_guard_until"),"removing required necklace cancels immunity");
   var sword=RefinedEquipment.finish(recipe("青霜剑",1),3);sword.setDamageValue(sword.getMaxDamage()*3/4);sword.enchant(Enchantments.SHARPNESS,2);var before=sword.copy();var metal=new ItemStack(MetalMaterialRegistry.INGOTS.get(MetalMaterialRegistry.ALL.get(0).id()).get(),2);
   var repaired=EquipmentRepair.preview(sword,metal);check(repaired.error().isEmpty(),"same tier metal previews repair");check(ItemStack.matches(before,sword)&&metal.getCount()==2,"preview never mutates inputs");near(repaired.stack().getDamageValue(),sword.getDamageValue()-sword.getMaxDamage()/4,"one main metal restores exactly quarter");check(repaired.stack().getTag().getCompound(RefinedEquipment.KEY).equals(sword.getTag().getCompound(RefinedEquipment.KEY)),"repair preserves quality and recipe");
   near(repaired.cost()/1000.0,RefiningRules.energy(1)*.1,"repair fee is ten percent of complete forging energy");
   check(!EquipmentRepair.preview(sword,new ItemStack(Items.IRON_INGOT)).error().isEmpty(),"incorrect material rejected");var full=sword.copy();full.setDamageValue(0);check(!EquipmentRepair.preview(full,metal).error().isEmpty(),"full durability rejected");
   var second=sword.copy();second.setDamageValue(second.getMaxDamage()-20);second.enchant(Enchantments.SHARPNESS,2);var combined=EquipmentRepair.preview(sword,second);check(combined.error().isEmpty()&&EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SHARPNESS,combined.stack())==3,"same enchantments combine using anvil levels");
   var conflict=second.copy();EnchantmentHelper.setEnchantments(Map.of(Enchantments.SMITE,2),conflict);check(!EquipmentRepair.preview(sword,conflict).error().isEmpty(),"incompatible enchantments reject atomically");
   var higher=RefinedEquipment.finish(recipe("青霜剑",2),1);check(!EquipmentRepair.preview(sword,higher).error().isEmpty(),"same name different tier cannot merge");
   l.setBlock(at,EquipmentRepairBlock.BLOCK.get().defaultBlockState(),2);p.teleportTo(l,at.getX()+.5,300,at.getZ()+2.5,180,0);
   var menu=new EquipmentRepairMenu(0,p.getInventory(),at);p.containerMenu=menu;((Container)menu.inputs).setItem(0,sword);((Container)menu.inputs).setItem(1,metal);
   d.setPower(0);((AbstractContainerMenu)menu).clickMenuButton(p,0);check(ItemStack.matches(sword,menu.inputs.getItem(0))&&metal.getCount()==2,"no-power confirmation does not consume inputs");
   d.setPower(1000);double power=d.power();((AbstractContainerMenu)menu).clickMenuButton(p,0);near(d.power(),power-repaired.cost()/1000.0,"real menu charges fee once");check(menu.inputs.getItem(1).getCount()==1&&menu.inputs.getItem(0).getDamageValue()==repaired.stack().getDamageValue(),"real menu commits both input changes");
   var result=menu.inputs.getItem(0).copy();l.setBlock(at,Blocks.AIR.defaultBlockState(),2);power=d.power();((AbstractContainerMenu)menu).clickMenuButton(p,0);check(ItemStack.matches(result,menu.inputs.getItem(0))&&d.power()==power,"removed table rejects stale confirmation");
   p.closeContainer();check(menu.inputs.isEmpty(),"closing returns all repair inputs");
   var loaded=l.getRecipeManager().byKey(new net.minecraft.resources.ResourceLocation("xiantu:equipment_repair_table"));check(loaded.isPresent(),"repair crafting recipe loads with registered wood material");
   var spellState=d.spells().save();try{d.spells().load(new CompoundTag());var pool=com.longju.xiantu.spell.SpellSources.beastPool(1,5,"fire",p);check(pool.stream().map(com.longju.xiantu.spell.SpellCatalog.Spell::id).collect(java.util.stream.Collectors.toSet()).equals(java.util.Set.of("fire_bolt","great_fireball")),"available fire scroll pool includes both current fire spells");check(com.longju.xiantu.spell.SpellSources.beastPool(3,5,"fire",p).stream().map(com.longju.xiantu.spell.SpellCatalog.Spell::id).toList().equals(java.util.List.of("great_fireball")),"Golden Core charge spell has an elite source");}finally{d.spells().load(spellState);}check(com.longju.xiantu.spell.SpellSources.beastPool(7,8,"fire",p).isEmpty(),"missing rare sources remain disabled");
   p.sendSystemMessage(Component.literal("[Equipment Code QA] PASS "+checks+" assertions"));return 1;
  }catch(Exception|LinkageError failure){p.sendSystemMessage(Component.literal("[Equipment Code QA] FAIL "+checks+": "+failure));throw failure;}
  finally{
   p.closeContainer();l.setBlock(at,old,2);p.getInventory().load(inventory);p.getPersistentData().getAllKeys().stream().toList().forEach(p.getPersistentData()::remove);p.getPersistentData().merge(persistent);d.load(saved);p.setGameMode(mode);p.teleportTo(l,pos.x,pos.y,pos.z,yaw,pitch);GongfaService.refresh(p);RefinedEquipment.refreshEligibility(p);EquipmentEffects.refresh(p);p.setHealth(health);p.setAbsorptionAmount(absorption);p.inventoryMenu.broadcastChanges();CultivationEvents.sync(p);
  }
 }));}
 private EquipmentCodeAudit(){}
}
