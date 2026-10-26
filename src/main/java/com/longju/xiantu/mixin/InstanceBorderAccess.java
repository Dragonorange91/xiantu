package com.longju.xiantu.mixin;
import net.minecraft.world.level.border.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import java.util.List;
@Mixin(WorldBorder.class)
public interface InstanceBorderAccess {
    @Invoker("getListeners") List<BorderChangeListener> xiantu$listeners();
}
