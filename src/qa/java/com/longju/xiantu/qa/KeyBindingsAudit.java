package com.longju.xiantu.qa;

import com.longju.xiantu.client.*;
import net.minecraft.client.*;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

@Mod.EventBusSubscriber(modid="xiantu_qa",value=Dist.CLIENT)
public final class KeyBindingsAudit {
    @SubscribeEvent public static void commands(RegisterClientCommandsEvent e){
        e.getDispatcher().register(Commands.literal("xiantukeysaudit").executes(c->run()));
        e.getDispatcher().register(Commands.literal("xiantukeyspreview").executes(c->{var mc=Minecraft.getInstance();mc.execute(()->mc.setScreen(new net.minecraft.client.gui.screens.controls.KeyBindsScreen(null,mc.options)));return 1;}));
    }
    private static int run(){
        var mc=Minecraft.getInstance();var keys=new ArrayList<KeyMapping>(Arrays.asList(SpellClient.KEYS));keys.add(SpellClient.OPEN);keys.add(EquipmentSkillClient.USE);keys.add(SpiritFireClient.CONDENSE);
        var failures=new ArrayList<String>();var rows=new ArrayList<Map<String,Object>>();var before=mc.options.keyMappings;var codes=keys.stream().map(KeyMapping::saveString).toList();
        for(var key:keys){long count=Arrays.stream(before).filter(k->k.getName().equals(key.getName())).count();rows.add(Map.of("id",key.getName(),"count",count,"code",key.saveString()));if(count!=1||Arrays.stream(before).noneMatch(k->k==key))failures.add(key.getName()+" count or canonical instance");}
        try{var event=new RegisterKeyMappingsEvent(mc.options);XiantuKeyBindings.register(event);XiantuKeyBindings.register(event);if(mc.options.keyMappings.length!=before.length)failures.add("repeated registration appended mappings");if(!codes.equals(keys.stream().map(KeyMapping::saveString).toList()))failures.add("custom keys changed");}
        finally{mc.options.keyMappings=before;}
        if(Component.translatable("key.categories.xiantu").getString().equals("key.categories.xiantu"))failures.add("missing category translation");
        String message="[Key bindings QA] "+(failures.isEmpty()?"PASS":"FAIL")+" 7 unique mappings; repeat registration and saved keys "+failures;
        org.slf4j.LoggerFactory.getLogger(KeyBindingsAudit.class).info(message);mc.player.displayClientMessage(Component.literal(message),false);
        try{var file=mc.gameDirectory.toPath().resolve("xiantu-qa/key-bindings.json");java.nio.file.Files.createDirectories(file.getParent());java.nio.file.Files.writeString(file,new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(Map.of("rows",rows,"failures",failures)));}catch(java.io.IOException ex){throw new RuntimeException(ex);}return failures.isEmpty()?1:0;
    }
    private KeyBindingsAudit(){}
}
