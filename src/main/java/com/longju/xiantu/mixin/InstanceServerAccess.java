package com.longju.xiantu.mixin;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.concurrent.Executor;

/** Pinned to Forge 47.4.26 / Mojmap 1.20.1; used only by the instance lifecycle adapter. */
@Mixin(MinecraftServer.class)
public interface InstanceServerAccess {
    @Accessor("executor") Executor xiantu$executor();
    @Accessor("storageSource") LevelStorageSource.LevelStorageAccess xiantu$storage();
}
