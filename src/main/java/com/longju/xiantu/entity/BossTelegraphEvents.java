package com.longju.xiantu.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="xiantu")
public final class BossTelegraphEvents {
    @SubscribeEvent public static void attack(AttackEntityEvent e){
        var anchor=e.getTarget();if(!anchor.getTags().contains("xiantu_boss_telegraph"))return;
        e.setCanceled(true);
        if(anchor.level() instanceof ServerLevel level && anchor.getPersistentData().hasUUID("BossOwner")){
            var owner=level.getEntity(anchor.getPersistentData().getUUID("BossOwner"));
            if(owner instanceof CompletionLandEntity boss)boss.strikeBossAnchor(e.getEntity(),anchor);
        }
    }
    @SubscribeEvent public static void knockback(LivingKnockBackEvent e){if(e.getEntity() instanceof CompletionLandEntity boss&&boss.bossBannerActive())e.setCanceled(true);}
    @SubscribeEvent public static void interact(PlayerInteractEvent.EntityInteract e){
        interactAnchor(e,e.getTarget());
    }
    // ArmorStand handles interactAt before the generic interaction. Cancel both paths,
    // otherwise right clicking the displayed core may equip/swap its item instead.
    @SubscribeEvent public static void interactAt(PlayerInteractEvent.EntityInteractSpecific e){
        interactAnchor(e,e.getTarget());
    }
    private static void interactAnchor(PlayerInteractEvent e,net.minecraft.world.entity.Entity anchor){
        if(!anchor.getTags().contains("xiantu_boss_telegraph"))return;
        e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);
        if(anchor.level() instanceof ServerLevel level && anchor.getPersistentData().hasUUID("BossOwner")){
            var owner=level.getEntity(anchor.getPersistentData().getUUID("BossOwner"));
            if(owner instanceof CompletionLandEntity boss)boss.interruptBoss(e.getEntity(),anchor);
        }
    }
    @SubscribeEvent public static void expire(LivingEvent.LivingTickEvent e){
        var anchor=e.getEntity();if(!(anchor.level() instanceof ServerLevel level)||!anchor.getTags().contains("xiantu_boss_telegraph"))return;
        var n=anchor.getPersistentData();var owner=n.hasUUID("BossOwner")?level.getEntity(n.getUUID("BossOwner")):null;
        if(!(owner instanceof CompletionLandEntity boss)||!boss.isAlive()||boss.bossSequence()!=n.getInt("BossSequence")||level.getGameTime()>n.getLong("BossExpires"))anchor.discard();
    }
    private BossTelegraphEvents(){}
}
