package com.longju.xiantu.refining;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import java.util.UUID;

/** A short elemental contact accent for successful normal weapon hits, including low tiers. */
@Mod.EventBusSubscriber(modid="xiantu")
public final class WeaponContactEffects {
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void hit(LivingDamageEvent e){
        if(e.isCanceled()||e.getAmount()<=0||!(e.getSource().getEntity() instanceof ServerPlayer p)
            ||e.getSource().getDirectEntity()!=p||!e.getSource().is(DamageTypes.PLAYER_ATTACK))return;
        if(WeaponPassives.active(p)==null)return;
        long now=com.longju.xiantu.spell.SpellService.clock();var tag=p.getPersistentData();
        if(tag.getLong("xiantu_weapon_contact_until")>now)return;tag.putLong("xiantu_weapon_contact_until",now+6);
        var target=e.getEntity();
        com.longju.xiantu.spell.SkillPresentation.emit(p,UUID.randomUUID(),"weapon_contact_"+WeaponPassives.element(p.getMainHandItem()).id(),"HIT",target.position().add(0,target.getBbHeight()*.5,0),p.getLookAngle());
    }
    private WeaponContactEffects(){}
}
