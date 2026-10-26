package com.longju.xiantu.gongfa;

import com.longju.xiantu.XiantuMod;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.EntityType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod;
import java.util.Map;
import java.util.HashMap;
import java.util.function.Function;

@Mod.EventBusSubscriber(modid=XiantuMod.MOD_ID, bus=Mod.EventBusSubscriber.Bus.FORGE)
public final class GongfaEvents {
    public record Target(int realmRank, boolean elite, boolean boss, boolean eligible) {}
    private static final Map<EntityType<?>, Function<LivingEntity, Target>> ADAPTERS = new HashMap<>();
    private GongfaEvents() {}
    /** Server-side registration point for future beasts and adventure-mod integrations. */
    public static void registerTarget(EntityType<?> type, Function<LivingEntity, Target> adapter) { ADAPTERS.put(type, adapter); }
    public static Target target(LivingEntity entity) {
        if (entity.getPersistentData().getBoolean("xiantu_no_method_experience")) return new Target(1,false,false,false);
        Function<LivingEntity,Target> adapter = ADAPTERS.get(entity.getType());
        if (adapter != null) return adapter.apply(entity);
        if(com.longju.xiantu.entity.BeastRealmService.valid(entity))return new Target(com.longju.xiantu.entity.BeastRealmService.realm(entity)+1,com.longju.xiantu.entity.BeastRealmService.elite(entity),com.longju.xiantu.entity.HostileRealmService.boss(entity),true);
        if (entity instanceof EnderDragon) return new Target(5,false,true,true);
        if (entity instanceof WitherBoss || entity instanceof Warden) return new Target(4,false,true,true);
        if (entity instanceof ElderGuardian) return new Target(4,false,true,true);
        return new Target(1,false,false,entity instanceof Monster);
    }
    @SubscribeEvent public static void damage(LivingDamageEvent event) {
        if (event.getAmount() <= 0 || event.getEntity().level().isClientSide) return;
        if (event.getEntity() instanceof ServerPlayer player) { GongfaService.markCombat(player); GongfaService.cancel(player,"受到伤害"); }
        if (event.getSource().getEntity() instanceof ServerPlayer player) { GongfaService.markCombat(player); GongfaService.cancel(player,"发动攻击或法术"); }
    }
    @SubscribeEvent public static void attack(AttackEntityEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) { GongfaService.markCombat(player); GongfaService.cancel(player,"发动攻击"); }
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) GongfaService.cancel(player,"");
    }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) GongfaService.cancel(player,"跨维度");
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void death(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (event.getEntity() instanceof ServerPlayer player) { GongfaService.cancel(player,""); return; }
        LivingEntity victim=event.getEntity(); Target target=target(victim);
        if (!target.eligible || victim.getPersistentData().getBoolean("xiantu_method_death_awarded")) return;
        var killer = event.getSource().getEntity();
        ServerPlayer owner = killer instanceof ServerPlayer player ? player
            : killer instanceof TamableAnimal pet && pet.getOwner() instanceof ServerPlayer player ? player
            : victim.getKillCredit() instanceof ServerPlayer player ? player : null;
        if (owner == null || owner instanceof net.minecraftforge.common.util.FakePlayer) return;
        victim.getPersistentData().putBoolean("xiantu_method_death_awarded",true);
        GongfaService.addExperience(owner,GongfaRules.killExperience(GongfaService.data(owner).majorRealm()+1,
            target.realmRank,target.elite,target.boss));
    }
}
