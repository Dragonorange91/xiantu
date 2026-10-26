package com.longju.xiantu.mixin.client;

import com.longju.xiantu.refining.RefinedEquipment;
import com.longju.xiantu.refining.WeaponActives;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** CONSUME suppresses swing, but vanilla still lowers the held item on every predicted click. */
@Mixin(ItemInHandRenderer.class)
public abstract class WeaponUseHandMixin {
    @Inject(method="itemUsed",at=@At("HEAD"),cancellable=true)
    private void xiantu$serverCastOwnsHand(InteractionHand hand,CallbackInfo ci){
        var player=Minecraft.getInstance().player;if(player==null||hand!=InteractionHand.MAIN_HAND)return;
        var stack=player.getMainHandItem();var recipe=RefinedEquipment.recipe(stack);if(recipe==null)return;
        if(recipe.id().equals("jingu_staff")||WeaponActives.available(stack)
            ||recipe.tier()>=5&&(recipe.theme().equals("开天斧")||recipe.theme().equals("贯日枪")))ci.cancel();
    }
}
