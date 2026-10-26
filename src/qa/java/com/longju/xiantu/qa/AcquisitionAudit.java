package com.longju.xiantu.qa;
import net.minecraft.commands.Commands;
import net.minecraft.world.item.*;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;
/** Live registry audit; unknown items are reported rather than assumed obtainable. */
@Mod.EventBusSubscriber(modid="xiantu_qa")
public final class AcquisitionAudit {
 private static final Map<Item,Set<String>> sources=new LinkedHashMap<>();
 private static void add(Item item,String source){if(item!=null&&item!=Items.AIR)sources.computeIfAbsent(item,k->new TreeSet<>()).add(source);}
 private static void id(String id,String source){add(ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation(id)),source);}
 private static void loot(com.google.gson.JsonElement e,String path){if(e.isJsonObject()){var o=e.getAsJsonObject();if(o.has("type")&&o.get("type").getAsString().equals("minecraft:item")&&o.has("name"))id(o.get("name").getAsString(),path);for(var v:o.entrySet())loot(v.getValue(),path);}else if(e.isJsonArray())for(var v:e.getAsJsonArray())loot(v,path);}
 @SubscribeEvent public static void commands(RegisterCommandsEvent event){event.getDispatcher().register(Commands.literal("xiantuacquisitionaudit").requires(s->s.hasPermission(2)).executes(c->{
  var p=c.getSource().getPlayerOrException();if(!p.server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).normalize().getFileName().toString().startsWith("Xiantu-QA"))return 0;sources.clear();var l=p.serverLevel();
  try{
  for(var r:l.getRecipeManager().getRecipes())add(r.getResultItem(l.registryAccess()).getItem(),"recipe:"+r.getId());
  var rm=p.server.getResourceManager();for(var entry:rm.listResources("loot_tables",f->f.getPath().endsWith(".json")&&(f.getPath().startsWith("loot_tables/chests/")||f.getPath().startsWith("loot_tables/entities/")||f.getPath().equals("loot_tables/blocks/spirit_gold_essence_ore.json"))).entrySet())if(entry.getKey().getNamespace().equals("xiantu"))try(var reader=entry.getValue().openAsReader()){loot(com.google.gson.JsonParser.parseReader(reader),"loot:"+entry.getKey());}catch(java.io.IOException ex){throw new java.io.UncheckedIOException(ex);}
  for(var r:com.longju.xiantu.refining.RefiningRecipes.ALL)id(r.output(),"refining:"+r.id());
  for(var r:com.longju.xiantu.refining.FacilityRecipes.ALL)id(r.output(),"facility:"+r.id());
  for(var r:com.longju.xiantu.alchemy.AlchemyRecipes.ALL)add(r.preview().getItem(),"alchemy:"+r.id());
  for(var r:com.longju.xiantu.talisman.TalismanRecipes.ALL)add(r.output(1).getItem(),"talisman:"+r.id());
  for(var r:com.longju.xiantu.smelting.SmeltingRecipes.ALL)add(r.output().getItem(),"smelting:"+r.metal());
  for(var r:com.longju.xiantu.trade.TownTradeService.OFFERS)add(r.stack().getItem(),"merchant:"+r.id());
  for(var spell:com.longju.xiantu.spell.SpellSources.pool(2,3,"generic",null))
   add(com.longju.xiantu.spell.SpellScrolls.stack(spell.id()).getItem(),"programmatic ruin chest reward: SpellSources.chest low-tier generic pool");
  for(var r:com.longju.xiantu.medicine.MedicineRegistry.HERB_ITEMS.values())add(r.get(),"herb gathering / existing medicine drop rules");
  for(var r:com.longju.xiantu.medicine.MedicineRegistry.SEED_ITEMS.values())add(r.get(),"herb seed harvesting");
  for(var t:com.longju.xiantu.wood.SpiritWoodType.values())for(var part:List.of("log","stripped_log","leaves","sapling"))add(com.longju.xiantu.wood.SpiritWoodRegistry.item(t,part),"natural spirit tree / harvesting");
  com.longju.xiantu.mining.MetalMaterialRegistry.RAW_ORES.values().forEach(v->add(v.get(),"mining metal ore"));
  com.longju.xiantu.mining.SpecialMineralRegistry.MATERIALS.values().forEach(v->add(v.get(),"mining special ore"));
  com.longju.xiantu.mining.AdvancedMinerals.MATERIALS.values().forEach(v->add(v.get(),"mining natural resource ore"));
  for(var t:com.longju.xiantu.item.SpiritStoneTier.values())add(com.longju.xiantu.registry.ModItems.getSpiritStone(t),"mining spirit ore");
  for(var name:List.of("beast_hide","beast_bone","beast_fang","beast_claw","beast_sinew","beast_scale","beast_horn","beast_venom_sac","beast_silk","beast_feather","beast_blood_sac","beast_carapace","wolf_hair","fox_hair","vulture_feather","lion_mane","falcon_feather","thunder_crow_feather","demon_core"))id("xiantu:"+name,"beast drops");
  id("xiantu:space_sand","natural cinnabar ore bonus 5%");id("xiantu:condensed_spirit_fire","player spirit-fire condensation");id("xiantu:inner_demon_crystal","inner-demon victory / instance rules");
  id("xiantu:equipment_blank","refining intermediate");id("xiantu:refining_scrap","failed refining / salvage");id("xiantu:failed_pill","failed alchemy");id("xiantu:talisman_residue","failed talisman drawing");id("xiantu:broken_talisman_brush","brush durability consumed");
  id("xiantu:cultivation_manual","starter once / replacement recipe");id("xiantu:guiding_stone","starter journey");id("xiantu:xuan_yuan_yang_qi_jue","town guide first dialogue");id("xiantu:dongfu_child_token","cave owner grant transaction");
  var report=new com.google.gson.JsonObject();var missing=new com.google.gson.JsonArray();var items=new com.google.gson.JsonArray();int total=0,covered=0,decor=0,test=0;
  for(var entry:ForgeRegistries.ITEMS.getEntries()){if(!entry.getKey().location().getNamespace().equals("xiantu"))continue;total++;var item=entry.getValue();String name=entry.getKey().location().toString();var row=new com.google.gson.JsonObject();row.addProperty("id",name);row.addProperty("name",item.getDescription().getString());
   if(sources.containsKey(item)){covered++;row.add("sources",new com.google.gson.Gson().toJsonTree(sources.get(item)));}
   else if(item instanceof SpawnEggItem||name.equals("xiantu:realm_test_jade")){test++;row.addProperty("classification","creative / QA only");}
   else if(item instanceof BlockItem b&&!Set.of("formation_core","meditation_cushion","appraisal_altar","equipment_repair_table","smelting_furnace","dimension_waymark").contains(entry.getKey().location().getPath())&&!name.contains("furnace")&&!name.contains("array_")&&!name.contains("forging")&&!name.contains("talisman_writing")){decor++;row.addProperty("classification","decorative / terrain: recipe not required; harvest ruin or natural blocks");}
   else{row.addProperty("classification","unverified acquisition: needs review");missing.add(name);}items.add(row);
  }
  report.addProperty("registered",total);report.addProperty("sourceAssigned",covered);report.addProperty("decorativeOrTerrainExempt",decor);report.addProperty("creativeOnly",test);report.add("unverified",missing);report.add("items",items);
  try{java.nio.file.Files.writeString(p.server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("acquisition-audit.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(report));}catch(java.io.IOException ex){throw new java.io.UncheckedIOException(ex);}
  String result="Acquisition audit: "+total+" registered, "+missing.size()+" unverified";c.getSource().sendSuccess(()->net.minecraft.network.chat.Component.literal(result),false);return 1;
  }catch(Exception ex){ex.printStackTrace();try{var w=new java.io.StringWriter();ex.printStackTrace(new java.io.PrintWriter(w));java.nio.file.Files.writeString(p.server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("acquisition-audit-error.txt"),w.toString());}catch(java.io.IOException ignored){}throw ex;}
 }));}
 private AcquisitionAudit(){}
}
