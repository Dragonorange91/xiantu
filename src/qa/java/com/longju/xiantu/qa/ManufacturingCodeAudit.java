package com.longju.xiantu.qa;

import com.longju.xiantu.refining.*;
import com.longju.xiantu.alchemy.*;
import com.longju.xiantu.worldgen.*;
import com.longju.xiantu.cultivation.*;
import com.google.gson.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import java.util.*;

/** Production callbacks in an isolated QA fixture; all player and block changes are restored. */
public final class ManufacturingCodeAudit {
 private static int count;
 private static void check(boolean value,String label){count++;if(!value)throw new IllegalStateException(label);}
 public static int run(ServerPlayer p,RefiningBlockEntity furnace){
  count=0;var level=p.serverLevel();var data=GongfaData(p);var at=((net.minecraft.world.level.block.entity.BlockEntity)furnace).getBlockPos();
  var spec=new JsonObject();spec.addProperty("id","qa_gold_resource");spec.add("size",new Gson().toJsonTree(List.of(8,8,8)));spec.add("encounters",new JsonArray());spec.add("markers",new JsonArray());
  String root=UUID.randomUUID().toString();var instance=RuinAdventureData.get(level).ensure(root,"qa_gold_resource",at,spec);
  var oreAt=at.offset(0,-1,0);var original=level.getBlockState(oreAt);float health=p.getHealth();var position=p.position();
  var box=new AABB(at).inflate(12);Set<UUID> existing=new HashSet<>();for(var entity:level.getEntitiesOfClass(ItemEntity.class,box))existing.add(entity.getUUID());
  try{
   p.teleportTo(level,at.getX()+.5,at.getY()+1,at.getZ()+.5,0,0);data.setRealmStage(7,0);p.hurtTime=0;
   var observatory=RuinSpecs.load(level.getServer(),"ninefold_star_observatory");
   int cores=0;
   for(var value:observatory.getAsJsonArray("markers")){
    var marker=value.getAsJsonObject();if(!marker.get("type").getAsString().equals("reward"))continue;
    if(marker.get("grade").getAsString().equals("core")){cores++;check(marker.get("loot").getAsString().equals("xiantu:chests/ruins/ninefold_observatory_core"),"observatory core uses its dedicated cloud silk table");}
    else check(!marker.get("loot").getAsString().equals("xiantu:chests/ruins/ninefold_observatory_core"),"side caches cannot add cloud silk");
   }
   check(cores==1,"exactly one core reward per observatory");
   var table=level.getServer().getLootData().getLootTable(new net.minecraft.resources.ResourceLocation("xiantu:chests/ruins/ninefold_observatory_core"));
   var params=new net.minecraft.world.level.storage.loot.LootParams.Builder(level).withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,p.position()).create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);
   for(int roll=0;roll<20;roll++){
    var rewards=table.getRandomItems(params);int silk=rewards.stream().filter(stack->stack.is(QitianMaterials.ITEMS.get("nine_heaven_cloud_silk").get())).mapToInt(ItemStack::getCount).sum();
    check(silk==2,"real observatory loot always contains two cloud silk");
    check(rewards.stream().anyMatch(stack->!stack.is(QitianMaterials.ITEMS.get("nine_heaven_cloud_silk").get())),"original high tier loot remains present");
   }
   var nodes=new JsonArray();var node=new JsonObject();node.addProperty("resource","spirit_gold_essence");node.add("position",new Gson().toJsonTree(List.of(0,-1,0)));nodes.add(node);spec.add("resourceNodes",nodes);
   RuinAdventureService.initialize(level,at,spec,root);check(level.getBlockState(oreAt).is(QitianMaterials.ORE.get()),"finite gold ore placed");level.removeBlock(oreAt,false);RuinAdventureService.initialize(level,at,spec,root);check(level.getBlockState(oreAt).isAir(),"reinitialization cannot replace mined ore");
   for(String id:List.of("jingu_staff","fengchi_crown","suozi_chestplate","liujin_leggings","ousi_boots")){
    var r=RefiningRecipes.definition(id);check(r!=null,"special gear definition exists "+id);
    for(int q=1;q<=5;q++){var stack=RefinedEquipment.finish(r,q);check(RefinedEquipment.infinite(stack)&&stack.getMaxDamage()==0&&!RefinedEquipment.broken(stack)&&RefinedEquipment.usable(p,stack),"special gear infinite durability "+id);}
   }
   data.setRealmStage(8,0);var r=AlchemyRecipes.find("pill_03");RefiningProgress.learn(p,r.knowledge());((Container)furnace).clearContent();furnace.alchemyAux.clearContent();
   for(int i=0;i<r.inputs().size();i++)((Container)furnace).setItem(4+i,r.inputs().get(i).example());((Container)furnace).setItem(3,new ItemStack(SpiritFire.COMMON.get()));furnace.energyMilli=1000000;
   p.containerMenu=new AlchemyMenu(0,p.getInventory(),furnace);check(AlchemyJob.start(furnace,p,r).isEmpty(),"automatic heat fixture starts");
   var job=furnace.alchemyJob;var menu=(AbstractContainerMenu)p.containerMenu;
   var before=job.save();
   for(int action:new int[]{0,10,11,12,13,14,15,20,21})check(!menu.clickMenuButton(p,action),"removed manual packet rejected "+action);
   check(before.equals(job.save()),"manual packets cannot advance work or change fire");
   for(int tick=0;tick<job.total();tick++){
    if(furnace.alchemyJob==null)break;job=furnace.alchemyJob;
    if(tick==job.total()/2){var saved=job.save();var restored=AlchemyJob.load(saved);check(restored.temperature==job.temperature&&restored.fireMode==job.fireMode&&restored.heldTicks==job.heldTicks,"heat progress serializes");furnace.alchemyJob=restored;job=restored;}
    AlchemyJob.tick(level,furnace);
    if(tick==0)check(furnace.alchemyJob!=null&&job.elapsed==1&&((Container)furnace).getItem(2).isEmpty(),"start never instantly manufactures output");
   }
   check(Arrays.stream(job.events).allMatch(v->v==1),"all six stages complete automatically");check(job.heldTicks>0&&furnace.alchemyJob==null,"mix hold and terminal transition");
   return count;
  }finally{
   if(furnace.alchemyJob!=null)furnace.alchemyJob.cancel(furnace);((Container)furnace).clearContent();furnace.alchemyAux.clearContent();((net.minecraft.world.level.saveddata.SavedData)RuinAdventureData.get(level)).setDirty();RuinProtection.internal(()->level.setBlock(oreAt,original,2));
   for(Entity entity:level.getEntitiesOfClass(ItemEntity.class,box))if(!existing.contains(entity.getUUID()))entity.discard();p.teleportTo(level,position.x,position.y,position.z,0,0);p.setHealth(health);
  }
 }
 private static CultivationData GongfaData(ServerPlayer p){return p.getCapability(CultivationCapability.DATA).orElseThrow(IllegalStateException::new);}
 private ManufacturingCodeAudit(){}
}
