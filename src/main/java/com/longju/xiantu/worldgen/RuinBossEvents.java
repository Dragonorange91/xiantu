package com.longju.xiantu.worldgen;

import com.longju.xiantu.cultivation.RealmRegistry;
import com.longju.xiantu.entity.CompletionLandEntity;
import net.minecraft.server.level.*;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="xiantu")
public final class RuinBossEvents {
    private static RuinAdventureData.Instance instance(net.minecraft.world.entity.LivingEntity mob){
        var n=mob.getPersistentData();return mob.level() instanceof ServerLevel l&&n.getBoolean(RuinAdventureService.BOSS)&&n.hasUUID(RuinEncounterService.SOURCE)?RuinAdventureData.get(l).find(n.getUUID(RuinEncounterService.SOURCE).toString()):null;
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void attack(LivingAttackEvent e){
        var i=instance(e.getEntity());if(i==null||i.defeated())return;
        if(e.getSource().getEntity() instanceof ServerPlayer p&&(!RuinAdventureService.legal(p,i)||!i.arena().contains(p.position())))e.setCanceled(true);
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void death(LivingDeathEvent e){
        var i=instance(e.getEntity());if(i==null||i.defeated())return;
        boolean valid=e.getSource().getEntity() instanceof ServerPlayer p&&RuinAdventureService.legal(p,i)&&i.arena().contains(p.position());
        if(!valid&&!e.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)){
            e.setCanceled(true);e.getEntity().setHealth(e.getEntity().getMaxHealth());
            if(e.getEntity() instanceof CompletionLandEntity boss)boss.resetRuinEncounter();
        }
    }
    private RuinBossEvents(){}
}
