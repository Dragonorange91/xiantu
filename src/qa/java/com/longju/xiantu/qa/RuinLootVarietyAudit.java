package com.longju.xiantu.qa;

import net.minecraft.commands.Commands;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

/** Native loot-table sampling; no player inventory, blocks, capabilities or chest claims are changed. */
@Mod.EventBusSubscriber(modid="xiantu_qa")
public final class RuinLootVarietyAudit {
    @SubscribeEvent public static void commands(RegisterCommandsEvent e){e.getDispatcher().register(Commands.literal("xianturuinlootaudit").requires(s->s.hasPermission(2)).executes(c->{
        var p=c.getSource().getPlayerOrException();var root=p.server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).normalize();
        if(!root.getFileName().toString().startsWith("Xiantu-QA"))return 0;
        var tables=new com.google.gson.JsonArray();int total=0;
        for(var entry:p.server.getResourceManager().listResources("loot_tables/chests",id->id.getPath().endsWith(".json")).entrySet()){
            var id=entry.getKey();if(!id.getNamespace().equals("xiantu"))continue;
            com.google.gson.JsonObject json;try(var reader=entry.getValue().openAsReader()){json=com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();}catch(java.io.IOException ex){throw new java.io.UncheckedIOException(ex);}
            if(!json.toString().contains("xiantu_finished_medicines"))continue;
            var resource=new ResourceLocation("xiantu",id.getPath().substring("loot_tables/".length(),id.getPath().length()-5));
            var table=p.server.getLootData().getLootTable(resource);var params=new LootParams.Builder(p.serverLevel()).withParameter(LootContextParams.ORIGIN,p.position()).create(LootContextParamSets.CHEST);
            var match=java.util.regex.Pattern.compile("tier_(\\d)").matcher(resource.getPath());int ceiling=match.find()?Integer.parseInt(match.group(1)):resource.getPath().contains("grand_core_wood")?6:resource.getPath().contains("grand_core_thunder")?7:resource.getPath().contains("grand_core_earth")?8:resource.getPath().contains("boss_elements")?5:resource.getPath().contains("boss_")?6:3;
            int pills=0,symbols=0,vanilla=0;var distinct=new TreeSet<String>();
            for(int seed=0;seed<64;seed++)for(ItemStack stack:table.getRandomItems(params,seed*7919L+17)){
                var key=ForgeRegistries.ITEMS.getKey(stack.getItem());if(key==null||stack.isEmpty())throw new IllegalStateException("Invalid loot "+resource);
                distinct.add(key.toString());if(key.getNamespace().equals("minecraft"))vanilla++;
                if(stack.getItem() instanceof com.longju.xiantu.item.PillItem pill){pills++;if(pill.definition().rank()>ceiling||!stack.getOrCreateTag().contains("pill_quality"))throw new IllegalStateException("Invalid pill tier/NBT "+resource);}
                if(key.getPath().startsWith("talisman_")&&!key.getPath().contains("blueprint")&&!key.getPath().contains("paper")){symbols++;if(com.longju.xiantu.talisman.TalismanItems.tier(stack)>ceiling||!stack.getOrCreateTag().contains("TalismanQuality"))throw new IllegalStateException("Invalid talisman tier/NBT "+resource);}
            }
            if(pills<64||symbols<10||vanilla<64||distinct.size()<15)throw new IllegalStateException("Insufficient native loot variety "+resource+" pills="+pills+" symbols="+symbols+" vanilla="+vanilla+" types="+distinct.size());
            var row=new com.google.gson.JsonObject();row.addProperty("table",resource.toString());row.addProperty("ceiling",ceiling);row.addProperty("pills",pills);row.addProperty("talismans",symbols);row.addProperty("vanilla",vanilla);row.addProperty("distinctItems",distinct.size());tables.add(row);total++;
        }
        if(total!=62)throw new IllegalStateException("Expected 62 revised loot tables; got "+total);
        try{java.nio.file.Files.writeString(root.resolve("ruin-loot-variety-audit.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(tables));}catch(java.io.IOException ex){throw new java.io.UncheckedIOException(ex);}
        c.getSource().sendSuccess(()->net.minecraft.network.chat.Component.literal("Ruin loot variety PASS: 62 tables x 64 native samples"),false);return 1;
    }));}
    private RuinLootVarietyAudit(){}
}
