package com.longju.xiantu.network;

import com.longju.xiantu.client.BeastPhotonClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.UUID;
import java.util.function.Supplier;

public record BeastVfxPacket(ResourceLocation effect, ResourceLocation dimension, int entityId,
                             UUID owner, Vec3 position, float yaw, int ticks, boolean follow) {
    public static void encode(BeastVfxPacket p,FriendlyByteBuf b) {
        b.writeResourceLocation(p.effect);b.writeResourceLocation(p.dimension);b.writeVarInt(p.entityId);b.writeUUID(p.owner);
        b.writeDouble(p.position.x);b.writeDouble(p.position.y);b.writeDouble(p.position.z);
        b.writeFloat(p.yaw);b.writeVarInt(p.ticks);b.writeBoolean(p.follow);
    }
    public static BeastVfxPacket decode(FriendlyByteBuf b) {
        return new BeastVfxPacket(b.readResourceLocation(),b.readResourceLocation(),b.readVarInt(),b.readUUID(),
                new Vec3(b.readDouble(),b.readDouble(),b.readDouble()),b.readFloat(),b.readVarInt(),b.readBoolean());
    }
    public static void handle(BeastVfxPacket p,Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,() -> () -> BeastPhotonClient.receive(p)));
        ctx.get().setPacketHandled(true);
    }
}
