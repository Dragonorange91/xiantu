package com.longju.xiantu.qa;

import com.longju.xiantu.cultivation.*;
import com.longju.xiantu.gongfa.GongfaService;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import java.util.UUID;

/** Actual server attributes, Forge heal/hurt hooks and lifecycle; always restore the QA player. */
public final class PhysiqueIntegration {
    private static int checks;
    private static void check(boolean ok,String message) {
        if(!ok)throw new IllegalStateException("[Physique QA FAIL] "+message);
        checks++;
    }
    private static void near(double a,double b,String message) { check(Math.abs(a-b)<.002,message+": "+a+" vs "+b); }
    public static int run(ServerPlayer player) {
        if(!player.getServer().getWorldPath(LevelResource.ROOT).normalize().getFileName().toString().startsWith("Xiantu-QA"))return 0;
        var data=(PlayerCultivationData)GongfaService.data(player);
        var saved=new CompoundTag();data.save(saved);
        var persistent=player.getPersistentData().copy();var hand=player.getMainHandItem().copy();
        float health=player.getHealth();double vanillaHealth=player.getAttribute(Attributes.MAX_HEALTH).getBaseValue();
        checks=0;
        try {
            GongfaService.cancel(player,"");player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
            player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(20);
            data.setPhysique(PhysiqueType.ORDINARY);data.setCharacterInitialized(true);data.setCultivationUnlocked(true);
            data.setDefenseBonus(0);data.gongfa().clear();data.setRealmStage(0,0);
            player.getPersistentData().putDouble("xiantu_gongfa_power_max_bonus",0);
            player.getPersistentData().putDouble("xiantu_physique_power_max_bonus",0);
            player.getPersistentData().putDouble("xiantu_gongfa_power_speed_bonus",0);
            data.setPowerMax(100);data.setPowerSpeed(1);GongfaService.refresh(player);
            double baseArmor=player.getAttributeValue(Attributes.ARMOR);
            for(var type:PhysiqueType.values()) {
                data.setPhysique(type);data.setPower(7);player.setHealth(7);
                GongfaService.refresh(player);
                near(player.getMaxHealth(),20*(1+type.healthBonus),"Health percentage "+type.id);
                near(player.getAttributeValue(Attributes.ARMOR),baseArmor+type.armor,"Armor "+type.id);
                near(data.powerMax(),100*(1+type.powerBonus),"Power cap "+type.id);
                for(int i=0;i<8;i++)GongfaService.refresh(player);
                near(player.getMaxHealth(),20*(1+type.healthBonus),"Refresh health does not stack "+type.id);
                near(data.powerMax(),100*(1+type.powerBonus),"Refresh power does not stack "+type.id);
                near(player.getHealth(),7,"No free healing from cap increase "+type.id);
                near(data.power(),7,"No free power from cap increase "+type.id);
                var tag=new CompoundTag();data.save(tag);data.setPhysique(PhysiqueType.ORDINARY);data.load(tag);
                GongfaService.refresh(player);check(data.physique()==type,"Reload keeps body "+type.id);
                data.gongfa().clear();data.gongfa().learn("jin_guang_jue");data.gongfa().commit("jin_guang_jue",UUID.randomUUID());
                for(int i=0;i<20;i++)GongfaService.meditationExperience(player);
                near(data.gongfa().activeProgress().experience(),Math.floor(type.methodExperience),"Valid meditation experience "+type.id);
                GongfaService.addExperience(player,20);
                near(data.gongfa().activeProgress().experience(),Math.floor(21*type.methodExperience),"Kill base experience multiplier "+type.id);
                check(data.gongfa().activeLevel()==0,"No unrelated instant level "+type.id);
            }
            data.gongfa().activeProgress().add(100000);data.setPhysique(PhysiqueType.CHANGXI);GongfaService.refresh(player);
            double methodBonus=GongfaService.active(data).powerBonus(9);
            near(data.powerMax(),Math.floor(100*(1+methodBonus+.15)),"Body and method capacity percentages add");
            double recovery=data.powerSpeed();data.setPhysique(PhysiqueType.ORDINARY);GongfaService.refresh(player);
            near(data.powerSpeed(),recovery,"Sea body does not change passive recovery");
            near(data.powerMax(),Math.floor(100*(1+methodBonus)),"Body change removes old capacity bonus");
            data.gongfa().clear();data.setPhysique(PhysiqueType.QINGDI);GongfaService.refresh(player);
            player.setHealth(10);player.heal(2);near(player.getHealth(),12.2,"Actual Forge heal event gains 10 percent");
            player.heal(100);near(player.getHealth(),player.getMaxHealth(),"Healing never exceeds maximum");
            data.setPhysique(PhysiqueType.DOUZHAN);GongfaService.refresh(player);
            LivingEntity target=EntityType.COW.create(player.serverLevel());
            check(target!=null,"Native damage fixture created");
            target.setHealth(10);target.hurt(player.damageSources().playerAttack(player),5);
            near(target.getHealth(),4.6,"Actual direct melee gets 8 percent before defense");
            target.hurtTime=0;target.invulnerableTime=0;target.setHealth(10);
            target.hurt(player.damageSources().indirectMagic(player,player),5);
            near(target.getHealth(),5,"Indirect magic never inherits melee bonus");
            target.hurtTime=0;target.invulnerableTime=0;target.setHealth(10);
            target.hurt(player.damageSources().thorns(player),5);
            near(target.getHealth(),5,"Reflection never inherits melee bonus");
            data.setPhysique(PhysiqueType.JINGLIAN);data.setInnerDemon(0);
            PhysiqueService.addInnerDemon(data,10);near(data.innerDemon(),8,"Failed small breakthrough base delta is discounted");
            var original=new FakePlayer(player.serverLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"PhysiqueOldQA"));
            var old=(PlayerCultivationData)GongfaService.data(original);old.setPhysique(PhysiqueType.HUNYUAN);old.setCharacterInitialized(true);
            var clone=new FakePlayer(player.serverLevel(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"PhysiqueNewQA"));
            CultivationEvents.clone(new PlayerEvent.Clone(clone,original,false));
            check(GongfaService.data(clone).physique()==PhysiqueType.HUNYUAN,"Non-death clone preserves body");
            CultivationEvents.clone(new PlayerEvent.Clone(clone,original,true));
            var death=GongfaService.data(clone).physique();
            CultivationEvents.clone(new PlayerEvent.Clone(clone,original,true));
            check(GongfaService.data(clone).physique()==death,"Repeated death Clone uses same stored draw");
            var mutable=(PlayerCultivationData)GongfaService.data(clone);
            for(int i=0;i<10;i++)PhysiqueService.initialize(clone,mutable);
            check(mutable.physique()==death,"Respawn/login initialization does not reroll");
            data.setRealmStage(8,3);data.setPhysique(PhysiqueType.PANSHI);GongfaService.refresh(player);
            near(player.getMaxHealth(),666*1.1,"High realm body scales derived integer health");
            data.setRealmStage(0,0);GongfaService.refresh(player);
            near(player.getMaxHealth(),22,"Realm rollback recomputes body once");
            player.sendSystemMessage(Component.literal("[Physique QA] All "+checks+" assertions passed"));
            org.slf4j.LoggerFactory.getLogger(PhysiqueIntegration.class).info("[Physique QA] All {} assertions passed",checks);
            return checks;
        } catch(Throwable e) {
            org.slf4j.LoggerFactory.getLogger(PhysiqueIntegration.class).error("[Physique QA] FAILED after "+checks,e);
            player.sendSystemMessage(Component.literal("[Physique QA] FAILED: "+e));return 0;
        } finally {
            data.load(saved);
            for(String key:java.util.List.copyOf(player.getPersistentData().getAllKeys()))player.getPersistentData().remove(key);
            player.getPersistentData().merge(persistent);
            player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(vanillaHealth);
            player.setItemInHand(InteractionHand.MAIN_HAND,hand);GongfaService.refresh(player);
            player.setHealth(Math.min(health,player.getMaxHealth()));CultivationEvents.sync(player);
        }
    }
}
