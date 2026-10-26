package com.longju.xiantu.qa;

import com.longju.xiantu.refining.*;
import com.longju.xiantu.cultivation.*;
import com.longju.xiantu.gongfa.GongfaService;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Calls actual item skill transactions twice and verifies one payment/one persisted cooldown. */
@Mod.EventBusSubscriber(modid="xiantu_qa")
public final class WeaponCooldownAudit {
    @SubscribeEvent public static void commands(RegisterCommandsEvent e){e.getDispatcher().register(Commands.literal("xiantuweaponcooldownaudit").requires(s->s.hasPermission(2)).executes(c->{
        var p=c.getSource().getPlayerOrException();String world=p.server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).normalize().getFileName().toString();
        if(!world.startsWith("Xiantu-QA")||world.equals("Xiantu-QA-Ruins-Review-20261024")||world.equals("Xiantu-QA-Ruins-Rework-20261024"))return 0;
        if(!p.serverLevel().getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(12),x->x!=p).isEmpty())throw new IllegalStateException("Move into an empty QA area first");
        if(p.isUsingItem()||com.longju.xiantu.spell.SpellService.casting(p)||QitianSkills.casting(p))return 0;
        var d=(PlayerCultivationData)GongfaService.data(p);var cultivation=new CompoundTag();d.save(cultivation);var persistent=p.getPersistentData().copy();var inventory=p.getInventory().save(new ListTag());
        var effects=p.getActiveEffects().stream().map(net.minecraft.world.effect.MobEffectInstance::new).toList();float health=p.getHealth(),absorption=p.getAbsorptionAmount();int checked=0;
        try{
            p.closeContainer();p.getInventory().clearContent();p.removeAllEffects();d.setCultivationUnlocked(true);d.setRealmStage(8,0);d.setPowerMax(1000000);d.setPower(1000000);
            for(String skill:WeaponActives.SKILLS){
                if(skill.equals("heaven_split")||skill.equals("sun_pierce"))continue;
                var r=RefiningRecipes.ALL.stream().filter(x->x.tier()>=5&&WeaponActives.skillId(x.theme()).equals(skill)).findFirst().orElseThrow();
                // Each skill is an independent full-power case; their combined costs exceed one bar.
                d.setPower(d.powerMax());
                ItemStack stack=RefinedEquipment.finish(r,1);p.setItemSlot(EquipmentSlot.MAINHAND,stack);RefinedEquipment.refreshEligibility(p);
                String timer="xiantu_active_"+r.theme();p.getPersistentData().remove(timer);double before=d.power();
                var accepted=WeaponActives.use(p.level(),p,InteractionHand.MAIN_HAND);long until=p.getPersistentData().getLong(timer);double paid=d.power();
                if(!accepted.getResult().consumesAction()||paid>=before||until<=com.longju.xiantu.spell.SpellService.clock())throw new IllegalStateException("Initial skill rejected "+skill);
                for(int repeat=0;repeat<8;repeat++){
                    var rejected=WeaponActives.use(p.level(),p,InteractionHand.MAIN_HAND);
                    if(rejected.getResult()!=net.minecraft.world.InteractionResult.FAIL||d.power()!=paid||p.getPersistentData().getLong(timer)!=until||p.isUsingItem())throw new IllegalStateException("Cooldown reactivated "+skill);
                }checked++;
            }
            if(checked!=12)throw new IllegalStateException("Missing active coverage");
            c.getSource().sendSuccess(()->net.minecraft.network.chat.Component.literal("Weapon cooldown PASS: 12 active skills, 96 rejected repeats; no repeat payment or charge"),false);return 1;
        }catch(Exception|LinkageError failure){
            com.mojang.logging.LogUtils.getLogger().error("[Weapon cooldown QA] FAIL after {} active skills",checked,failure);
            c.getSource().sendFailure(net.minecraft.network.chat.Component.literal("Weapon cooldown FAIL after "+checked+": "+failure));
            return 0;
        }finally{
            p.stopUsingItem();p.getInventory().clearContent();p.getInventory().load(inventory);d.load(cultivation);var tag=p.getPersistentData();for(String key:new ArrayList<>(tag.getAllKeys()))tag.remove(key);tag.merge(persistent);p.removeAllEffects();effects.forEach(p::addEffect);GongfaService.refresh(p);RefinedEquipment.refreshEligibility(p);p.setHealth(health);p.setAbsorptionAmount(absorption);CultivationEvents.sync(p);p.inventoryMenu.broadcastChanges();
        }
    }));}
    private WeaponCooldownAudit(){}
}
