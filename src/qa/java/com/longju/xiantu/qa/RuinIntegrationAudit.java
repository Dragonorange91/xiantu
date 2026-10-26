package com.longju.xiantu.qa;

import com.google.gson.*;
import com.longju.xiantu.worldgen.*;
import com.longju.xiantu.building.RewardChestBlockEntity;
import com.longju.xiantu.registry.ModBlocks;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

@Mod.EventBusSubscriber(modid="xiantu_qa")
public final class RuinIntegrationAudit {
    private static int checks;
    private static void check(boolean condition,String reason){checks++;if(!condition)throw new IllegalStateException(reason);}
    @SubscribeEvent public static void register(RegisterCommandsEvent event){event.getDispatcher().register(Commands.literal("xianturuinintegrationaudit").requires(s->s.hasPermission(2)).executes(c->{
        var source=c.getSource();var player=source.getPlayerOrException();var l=player.serverLevel();
        if(!player.server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).normalize().getFileName().toString().startsWith("Xiantu-QA"))return 0;
        var report=new JsonObject();checks=0;
        try{
            var resources=player.server.getResourceManager().listResources("ruin_blueprints",r->r.getNamespace().equals("xiantu")&&r.getPath().endsWith(".json"));
            check(resources.size()==20,"20 metadata files");
            for(var entry:resources.entrySet())try(var reader=entry.getValue().openAsReader()){
                var spec=JsonParser.parseReader(reader).getAsJsonObject();String id=spec.get("id").getAsString();RuinEncounterService.validate(spec);
                var template=l.getStructureManager().get(new ResourceLocation("xiantu","ruins/"+id)).orElseThrow();check(template.getSize().getX()==spec.getAsJsonArray("size").get(0).getAsInt(),id+" template size");
                var index=RuinTemplateIndex.of(template);int blocks=0;
                for(int x=0;x<template.getSize().getX();x++)for(int z=0;z<template.getSize().getZ();z++)blocks+=index.column(x,z).size();check(blocks>0,id+" block index");
                if(spec.getAsJsonObject("placement").get("natural").getAsBoolean())check(l.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.STRUCTURE).containsKey(new ResourceLocation("xiantu",id)),id+" registry");
                for(var m:spec.getAsJsonArray("markers")){var marker=m.getAsJsonObject();if(!marker.get("type").getAsString().equals("reward"))continue;
                    var resource=new ResourceLocation(marker.get("loot").getAsString());var table=player.server.getLootData().getLootTable(resource);
                    check(table!=net.minecraft.world.level.storage.loot.LootTable.EMPTY,id+" loaded loot "+resource);
                    try(var poolReader=player.server.getResourceManager().getResource(new ResourceLocation("xiantu","loot_tables/"+resource.getPath()+".json")).orElseThrow().openAsReader()){
                        var pool=JsonParser.parseReader(poolReader).getAsJsonObject();for(var pv:pool.getAsJsonArray("pools"))for(var item:pv.getAsJsonObject().getAsJsonArray("entries")){
                            var lootEntry=item.getAsJsonObject();var name=new ResourceLocation(lootEntry.get("name").getAsString());
                            if(lootEntry.get("type").getAsString().equals("minecraft:loot_table"))check(player.server.getLootData().getLootTable(name)!=net.minecraft.world.level.storage.loot.LootTable.EMPTY,"loaded nested reward table");
                            else check(ForgeRegistries.ITEMS.containsKey(name),"registered reward");
                        }
                    }
                }
            }
            // An isolated fixture checks the real Level.setBlock mixin and native container transactions.
            var origin=new BlockPos(30000,250,30000);var spec=RuinSpecs.load(player.server,"five_elements_prison_grand").deepCopy();
            spec.addProperty("id","five_elements_prison_grand");spec.add("size",JsonParser.parseString("[8,8,8]"));spec.add("encounters",new JsonArray());spec.add("markers",new JsonArray());
            var marker=JsonParser.parseString("{\"type\":\"reward\",\"id\":\"audit_core\",\"grade\":\"core\",\"position\":[2,1,2],\"minRealm\":0,\"loot\":\"xiantu:chests/grand_core_earth\"}").getAsJsonObject();spec.getAsJsonArray("markers").add(marker);
            String root=RuinEncounterService.key(l,"five_elements_prison_grand",origin);var data=RuinAdventureData.get(l);var record=data.ensure(root,"five_elements_prison_grand",origin,spec);record.state.remove("Defeated");record.state.remove("RewardUnlocked");record.state.remove("Claims");
            var pos=origin.offset(2,1,2);l.getChunk(pos);
            var original=l.getBlockState(pos);var originalBE=l.getBlockEntity(pos);var saved=originalBE==null?null:originalBE.saveWithFullMetadata();
            try{
                RuinProtection.internal(()->l.setBlock(pos,ModBlocks.RUIN_REWARD_CHEST.get().defaultBlockState(),3));
                var chest=(RewardChestBlockEntity)l.getBlockEntity(pos);((net.minecraft.world.level.block.entity.BlockEntity)chest).load(RuinAdventureService.chestData(root,"five_elements_prison_grand",origin,marker));
                check(chest.ruinLocked(),"core locked before boss");check(!l.setBlock(pos,Blocks.AIR.defaultBlockState(),3),"direct mutation blocked");
                ((RandomizableContainerBlockEntity)chest).unpackLootTable(player);check(((net.minecraft.world.level.block.entity.BlockEntity)chest).saveWithoutMetadata().contains("LootTable"),"locked loot still unopened");
                record.state.putBoolean("Defeated",true);record.state.putBoolean("RewardUnlocked",true);check(!chest.ruinLocked(),"defeat unlocks core");
                ((RandomizableContainerBlockEntity)chest).unpackLootTable(player);String first=((net.minecraft.world.level.block.entity.BlockEntity)chest).saveWithoutMetadata().toString();check(!((net.minecraft.world.level.block.entity.BlockEntity)chest).saveWithoutMetadata().contains("LootTable"),"real native loot generated");
                ((RandomizableContainerBlockEntity)chest).unpackLootTable(player);check(first.equals(((net.minecraft.world.level.block.entity.BlockEntity)chest).saveWithoutMetadata().toString()),"repeat does not reroll");
                check(!data.claim(record,"audit_core"),"unique ledger persisted claim");
                check(((net.minecraft.world.level.saveddata.SavedData)data).save(new CompoundTag()).toString().contains("audit_core"),"saved data contains claim");
                check(l.setBlock(pos,Blocks.STONE.defaultBlockState(),3),"defeated region becomes editable");
            }finally{record.state.putBoolean("Defeated",true);RuinProtection.internal(()->l.setBlock(pos,original,3));if(saved!=null&&l.getBlockEntity(pos)!=null)l.getBlockEntity(pos).load(saved);((net.minecraft.world.level.saveddata.SavedData)data).setDirty();}
            report.addProperty("passed",true);source.sendSuccess(()->Component.literal("Ruin integration PASS: "+checks+" assertions"),false);
        }catch(Throwable ex){report.addProperty("passed",false);report.addProperty("error",ex.toString());org.slf4j.LoggerFactory.getLogger(RuinIntegrationAudit.class).error("Ruin integration audit",ex);source.sendFailure(Component.literal("Ruin integration FAIL: "+ex));}
        report.addProperty("checks",checks);try{var path=net.minecraftforge.fml.loading.FMLPaths.GAMEDIR.get().resolve("xiantu-qa/ruin-integration.json");java.nio.file.Files.createDirectories(path.getParent());java.nio.file.Files.writeString(path,report.toString());}catch(java.io.IOException ex){throw new java.io.UncheckedIOException(ex);}return report.get("passed").getAsBoolean()?1:0;
    }));}
}
