package com.longju.xiantu.mixin;
import net.minecraft.world.level.border.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(BorderChangeListener.DelegateBorderChangeListener.class)
public interface InstanceBorderDelegateAccess {
    @Accessor("worldBorder") WorldBorder xiantu$target();
}
