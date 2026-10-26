package com.longju.xiantu.qa;

import net.minecraft.commands.Commands;
import net.minecraft.world.item.*;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

/** Build real tab contents, including NBT variants, then compare their item identities with the live registry. */
@Mod.EventBusSubscriber(modid="xiantu_qa")
public final class CreativeCoverageAudit {
    @SubscribeEvent public static void commands(RegisterCommandsEvent e){e.getDispatcher().register(Commands.literal("xiantucreativeaudit").requires(s->s.hasPermission(2)).executes(c->{
        var p=c.getSource().getPlayerOrException();
        var root=p.server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).normalize();
        if(!root.getFileName().toString().startsWith("Xiantu-QA"))return 0;
        var shown=Collections.newSetFromMap(new IdentityHashMap<Item,Boolean>());
        var parameters=new CreativeModeTab.ItemDisplayParameters(p.serverLevel().enabledFeatures(),true,p.server.registryAccess());
        var report=new com.google.gson.JsonObject();var tabs=new com.google.gson.JsonObject();
        for(var holder:com.longju.xiantu.registry.ModCreativeTabs.TABS.getEntries()){
            var tab=holder.get();tab.buildContents(parameters);
            tab.getDisplayItems().forEach(s->shown.add(s.getItem()));tab.getSearchTabDisplayItems().forEach(s->shown.add(s.getItem()));
            tabs.addProperty(holder.getId().toString(),tab.getDisplayItems().size());
        }
        var missing=new com.google.gson.JsonArray();var excluded=new com.google.gson.JsonArray();int count=0;
        for(var entry:ForgeRegistries.ITEMS.getEntries()){
            var id=entry.getKey().location();if(!id.getNamespace().equals("xiantu"))continue;count++;
            if(shown.contains(entry.getValue()))continue;
            // These intermediates carry a server-owned ledger serial; an arbitrary creative stack cannot be forged.
            if(id.getPath().equals("equipment_blank"))excluded.add(id+": server ledger intermediate, obtained from furnace");
            else missing.add(id.toString());
        }
        for(String id:List.of("array_base_1","array_rune_brick_1","array_conduit_pillar_1","dongfu_child_token"))
            if(!shown.contains(ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation("xiantu",id))))throw new IllegalStateException("Required creative item missing: "+id);
        var staff=com.longju.xiantu.refining.RefiningRecipes.definition("jingu_staff");
        if(com.longju.xiantu.refining.RefinedEquipment.minimumRealm(staff)!=3)throw new IllegalStateException("Jingu staff must require Yuanying");
        report.addProperty("registered",count);report.addProperty("visibleItems",shown.size());report.add("tabs",tabs);report.add("intentionalExclusions",excluded);report.add("missing",missing);
        try{java.nio.file.Files.writeString(root.resolve("creative-coverage-audit.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(report));}catch(java.io.IOException ex){throw new java.io.UncheckedIOException(ex);}
        c.getSource().sendSuccess(()->net.minecraft.network.chat.Component.literal("Creative coverage: "+missing.size()+" missing; Jingu Yuanying gate PASS"),false);
        return missing.isEmpty()?1:0;
    }));}
    private CreativeCoverageAudit(){}
}
