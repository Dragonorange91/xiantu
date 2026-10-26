package com.longju.xiantu.mixin;

import com.longju.xiantu.refining.EquipmentConditions;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

/** Apply natural-recovery bonuses only to food regeneration, never to pills or spell healing. */
@Mixin(FoodData.class)
public abstract class NaturalHealingMixin {
    @Redirect(method="tick",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/player/Player;heal(F)V"))
    private void xiantu$naturalRecovery(Player player,float amount){
        player.heal(player.level().isClientSide?amount:(float)(amount*(1+EquipmentConditions.naturalHealingBonus(player))));
    }
}
