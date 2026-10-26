package com.longju.xiantu.entity;

import com.longju.xiantu.network.BeastVfxPacket;
import com.longju.xiantu.network.XiantuNetwork;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

/** Server-owned stage events; Photon is exclusively a client presentation dependency. */
public final class BeastVfx {
    private BeastVfx() {}
    public static void emit(Entity owner, String stage, Vec3 position, Vec3 direction, int ticks, boolean follow) {
        ResourceLocation type = ForgeRegistries.ENTITY_TYPES.getKey(owner.getType());
        if (type == null) return;
        emit(owner, type.getPath(), stage, position, direction, ticks, follow);
    }
    public static void emit(Entity owner, String species, String stage, Vec3 position, Vec3 direction, int ticks, boolean follow) {
        if (!(owner.level() instanceof ServerLevel)) return;
        float yaw = (float)Math.toDegrees(Math.atan2(-direction.x, direction.z));
        XiantuNetwork.CHANNEL.send(PacketDistributor.TRACKING_ENTITY.with(() -> owner),
                new BeastVfxPacket(new ResourceLocation("xiantu", "beasts/"+species+"/"+stage),
                        owner.level().dimension().location(),owner.getId(),owner.getUUID(),position,yaw,ticks,follow));
    }
    public static void cancel(Entity owner) {
        emit(owner, "cancel", owner.position(), owner.getLookAngle(), 1, false);
    }
}
