package com.longju.xiantu.entity;

import com.longju.xiantu.cultivation.RealmRegistry;
import com.longju.xiantu.network.*;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.UUID;

/** Vanilla hostiles and named bosses share persistent realm identity, without beast ecology/drops. */
@Mod.EventBusSubscriber(modid="xiantu")
public final class HostileRealmService {
    private static final UUID HP=UUID.fromString("83e0817a-d75b-4138-b309-df40683937ef"),ATK=UUID.fromString("b381b36d-e6fe-4c33-8c0c-8e6fa875a35f");
    public static boolean boss(Entity e){return e instanceof EnderDragon||e instanceof WitherBoss||e instanceof Warden||e instanceof ElderGuardian||e instanceof CompletionLandEntity c&&c.boss();}
    public static boolean eligible(Entity e){var id=ForgeRegistries.ENTITY_TYPES.getKey(e.getType());return boss(e)||id!=null&&id.getNamespace().equals("minecraft")&&e instanceof Enemy;}
    public static void initialize(Mob mob){
        if(!(mob.level() instanceof ServerLevel)||!eligible(mob))return;
        var n=mob.getPersistentData();boolean fresh=!BeastRealmService.valid(mob);
        if(fresh){
            int r;
            String encounter=BeastResourceDrops.REALM;
            if(n.contains(encounter,Tag.TAG_INT))r=n.getInt(encounter);
            else {
                r=NearbySpawnRealms.roll(mob,BeastRealmRules.WEIGHTS[0]);
                if(r<0)r=boss(mob)?3+BeastRealmRules.roll(new double[]{70,20,7,2,.8,.2},mob.getRandom().nextDouble()):BeastRealmRules.roll(BeastRealmRules.WEIGHTS[0],mob.getRandom().nextDouble());
            }
            r=Math.max(boss(mob)?3:0,Math.min(8,r));int stage=mob.getRandom().nextInt(RealmRegistry.STAGE_COUNTS[r]);
            n.putInt(BeastResourceDrops.REALM,r);n.putInt(BeastRealmService.STAGE,stage);n.putLong(BeastRealmService.PROGRESS,0);n.putBoolean(BeastRealmService.INITIALIZED,true);
            if(mob instanceof CompletionLandEntity c)n.putString(BeastRealmService.ELEMENT,CompletionBestiary.get(c.getType()).element());
        }
        if(boss(mob)&&BeastRealmService.realm(mob)<3){n.putInt(BeastResourceDrops.REALM,3);n.putInt(BeastRealmService.STAGE,0);}
        // Ruin party scaling is authoritative and already updates health/damage in its own transaction.
        if(n.getBoolean(com.longju.xiantu.worldgen.RuinAdventureService.BOSS))return;
        float fraction=mob.getMaxHealth()>0?mob.getHealth()/mob.getMaxHealth():1;
        int r=BeastRealmService.realm(mob),s=n.getInt(BeastRealmService.STAGE);double base=boss(mob)?BeastRealmRules.health(3,0,false):BeastRealmRules.health(0,0,false);
        var health=mob.getAttribute(Attributes.MAX_HEALTH);
        modify(health,HP,boss(mob)?Math.max(1,BossBalance.health(r,s)/Math.max(1,health.getBaseValue())):Math.max(1,BeastRealmRules.health(r,s,false)/base));
        modify(mob.getAttribute(Attributes.ATTACK_DAMAGE),ATK,damageScale(mob));
        mob.setHealth(fresh?mob.getMaxHealth():mob.getMaxHealth()*fraction);
    }
    private static void modify(AttributeInstance a,UUID id,double factor){if(a==null)return;a.removeModifier(id);if(factor>1)a.addTransientModifier(new AttributeModifier(id,"修为成长",factor-1,AttributeModifier.Operation.MULTIPLY_TOTAL));}
    public static double damageScale(Entity e){if(!BeastRealmService.valid(e))return 1;int r=BeastRealmService.realm(e),s=e.getPersistentData().getInt(BeastRealmService.STAGE);if(boss(e)){var a=e instanceof LivingEntity l?l.getAttribute(Attributes.ATTACK_DAMAGE):null;return Math.max(1,BossBalance.attack(r,s)/(a==null?9:Math.max(1,a.getBaseValue())));}return Math.max(1,BeastRealmRules.attack(r,s,false)/BeastRealmRules.attack(0,0,false));}
    @SubscribeEvent public static void join(EntityJoinLevelEvent e){if(!e.getLevel().isClientSide&&e.getEntity() instanceof Mob m)initialize(m);}
    @SubscribeEvent public static void tracking(PlayerEvent.StartTracking e){if(e.getEntity() instanceof ServerPlayer p&&e.getTarget() instanceof Mob m&&eligible(m)){initialize(m);XiantuNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(()->p),BeastRealmPacket.of(m));}}
    @SubscribeEvent(priority=EventPriority.HIGH) public static void ranged(LivingHurtEvent e){
        if(e.getEntity().level().isClientSide||!(e.getSource().getEntity() instanceof Mob m)||!eligible(m)||m.getPersistentData().getBoolean(com.longju.xiantu.worldgen.RuinAdventureService.BOSS))return;
        if(e.getSource().getDirectEntity()!=m||m.getAttribute(Attributes.ATTACK_DAMAGE)==null)e.setAmount((float)(e.getAmount()*damageScale(m)));
    }
    private HostileRealmService(){}
}
