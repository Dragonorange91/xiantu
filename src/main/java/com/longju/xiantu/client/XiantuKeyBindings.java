package com.longju.xiantu.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.Arrays;

/** A single registration entry; repeated events must not duplicate controls. */
@Mod.EventBusSubscriber(modid="xiantu",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class XiantuKeyBindings {
    @SubscribeEvent public static void register(RegisterKeyMappingsEvent event){
        registerOnce(event,SpellClient.OPEN);
        for(var key:SpellClient.KEYS)registerOnce(event,key);
        registerOnce(event,EquipmentSkillClient.USE);
        registerOnce(event,SpiritFireClient.CONDENSE);
    }
    private static void registerOnce(RegisterKeyMappingsEvent event,KeyMapping key){
        if(Arrays.stream(Minecraft.getInstance().options.keyMappings).noneMatch(existing->existing.getName().equals(key.getName())))event.register(key);
    }
    private XiantuKeyBindings(){}
}
