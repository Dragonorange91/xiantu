package com.longju.xiantu.mixin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import com.longju.xiantu.instance.InstanceService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class InstanceDeathMixin {
    @Inject(method="dropEquipment",at=@At("HEAD"),cancellable=true)
    private void xiantu$keepItems(CallbackInfo ci){if((Object)this instanceof ServerPlayer p&&InstanceService.temporary(p.level()))ci.cancel();}
}
