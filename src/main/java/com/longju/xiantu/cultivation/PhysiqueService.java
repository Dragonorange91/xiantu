package com.longju.xiantu.cultivation;

import com.longju.xiantu.XiantuMod;
import com.longju.xiantu.config.XiantuConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=XiantuMod.MOD_ID, bus=Mod.EventBusSubscriber.Bus.FORGE)
public final class PhysiqueService {
    private PhysiqueService() {}
    public static double[] weights() {
        return XiantuConfig.PHYSIQUE_WEIGHTS.stream().mapToDouble(v->v.get()).toArray();
    }
    public static void initialize(Player player, PlayerCultivationData data) {
        if (!player.level().isClientSide && !data.physiqueInitialized())
            data.setPhysique(PhysiqueType.roll(player.getRandom().nextDouble(),weights()));
    }
    public static void deathClone(Player player, PlayerCultivationData original, PlayerCultivationData restored) {
        PhysiqueType next=original.deathPhysique(player.getRandom(),weights());
        restored.setPhysique(next);
        restored.clearDeathPhysique();
    }
    /** Existing qualified sources submit a positive delta; absolute admin/load setters never apply a bonus. */
    public static void addInnerDemon(CultivationData data, double amount) {
        if (!Double.isFinite(amount) || amount<=0) return;
        if (data instanceof PlayerCultivationData p) p.accumulateInnerDemon(amount*data.physique().demonGrowth*(1-Math.min(.2,data.medicine().effect("clear_mind",com.longju.xiantu.spell.SpellService.clock()))));
    }
    public static double meditationRecovery(CultivationData data) {
        return data.powerMax()*.01*com.longju.xiantu.formation.FormationRules.rootMultiplier(data.spiritRootCount())
                *data.physique().meditationRecovery/20;
    }
    public static double effectiveMethodExperience(CultivationData data, double base) {
        return base*data.physique().methodExperience;
    }
    // Later modules call these only AFTER valid-use / low-recipe-decay checks, preserving fractional XP.
    public static double effectiveSpellExperience(CultivationData data, double base) { return base*data.physique().spellExperience; }
    public static double effectiveProfessionExperience(CultivationData data, double base) { return base*data.physique().professionExperience; }
    @SubscribeEvent(priority=EventPriority.LOW) public static void heal(LivingHealEvent event) {
        if (event.getEntity().level().isClientSide || event.getAmount()<=0 || !(event.getEntity() instanceof ServerPlayer p)) return;
        p.getCapability(CultivationCapability.DATA).ifPresent(d->{
            var method=com.longju.xiantu.gongfa.GongfaService.active(d);
            double methodBonus=method!=null&&method.buff()==com.longju.xiantu.gongfa.GongfaBuff.MENDING
                    ?com.longju.xiantu.gongfa.GongfaService.buffBonus(d):0;
            event.setAmount((float)(event.getAmount()*Math.max(0,1+(d.physique().healing-1)+methodBonus
                    +com.longju.xiantu.refining.EquipmentEffects.healingBonus(p))));
        });
    }
    @SubscribeEvent public static void melee(LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide || event.getAmount()<=0
                || !event.getSource().is(DamageTypes.PLAYER_ATTACK)
                || !(event.getSource().getDirectEntity() instanceof ServerPlayer p)
                || event.getSource().getEntity()!=p) return;
        p.getCapability(CultivationCapability.DATA).ifPresent(d->event.setAmount((float)(event.getAmount()
                *Math.max(.1,Math.min(com.longju.xiantu.refining.QitianSkills.form(p)?2.8:2.2,1+Math.max(0,d.physique().meleeBonus)
                +Math.max(0,com.longju.xiantu.gongfa.GongfaService.attackBonus(d)+com.longju.xiantu.refining.WeaponPassives.damageBonus(p,event.getEntity()))
                +com.longju.xiantu.alchemy.MedicineService.meleeBonus(p)+com.longju.xiantu.refining.EquipmentEffects.attackBonus(p)+com.longju.xiantu.refining.QitianSkills.meleeBonus(p))))));
    }
}
