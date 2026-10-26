package com.longju.xiantu.entity;

import com.longju.xiantu.refining.WeaponPassives;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Short-lived, bounded damage threat. It never changes damage credit, drops or XP ownership. */
@Mod.EventBusSubscriber(modid="xiantu")
public final class BeastThreat {
    private static final String KEY="xiantu_damage_threat";
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void damage(LivingDamageEvent e){
        if(e.getAmount()<=0||!(e.getSource().getEntity() instanceof ServerPlayer p)||!BeastRealmService.isBeast(e.getEntity()))return;
        var mob=e.getEntity();long now=p.server.overworld().getGameTime();var data=mob.getPersistentData().getCompound(KEY);
        data.getAllKeys().removeIf(id->data.getCompound(id).getLong("Until")<=now);
        var id=p.getUUID().toString();if(!data.contains(id)&&data.size()>=16)return;var n=data.getCompound(id);
        n.putDouble("Damage",Math.min(1e9,n.getDouble("Damage")+e.getAmount()*Math.max(.5,WeaponPassives.threatMultiplier(p))));n.putLong("Until",now+200);data.put(id,n);mob.getPersistentData().put(KEY,data);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void target(LivingChangeTargetEvent e){
        if(!(e.getEntity() instanceof Mob mob)||!BeastRealmService.isBeast(mob)||!(mob.level() instanceof net.minecraft.server.level.ServerLevel level)||!(e.getNewTarget() instanceof ServerPlayer original))return;
        var data=mob.getPersistentData().getCompound(KEY);long now=level.getServer().overworld().getGameTime();double best=0;ServerPlayer selected=original;
        for(var id:new ArrayList<>(data.getAllKeys())){var n=data.getCompound(id);if(n.getLong("Until")<=now){data.remove(id);continue;}
            ServerPlayer p;try{p=level.getServer().getPlayerList().getPlayer(UUID.fromString(id));}catch(IllegalArgumentException bad){data.remove(id);continue;}
            if(p==null||p.level()!=level||!p.isAlive()||p.isCreative()||p.isSpectator()||mob.isAlliedTo(p)||mob.distanceToSqr(p)>256||!mob.hasLineOfSight(p)||!BeastRealmService.mayTarget(mob,p))continue;
            if(n.getDouble("Damage")>best){best=n.getDouble("Damage");selected=p;}
        }e.setNewTarget(selected);
    }
    private BeastThreat(){}
}
