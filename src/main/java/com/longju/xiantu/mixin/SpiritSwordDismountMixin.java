package com.longju.xiantu.mixin;

import com.longju.xiantu.spell.SpiritSwordEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Shift descends on this mount. Explicit server dismounts still work normally. */
@Mixin(Player.class)
public abstract class SpiritSwordDismountMixin {
 @Inject(method="wantsToStopRiding",at=@At("HEAD"),cancellable=true)
 private void xiantu$descent(CallbackInfoReturnable<Boolean> ci){if(((Player)(Object)this).getVehicle() instanceof SpiritSwordEntity)ci.setReturnValue(false);}
}
