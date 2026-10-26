package com.longju.xiantu.network;

import com.longju.xiantu.client.InnerDemonClientState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record InnerDemonStatePacket(boolean locked) {
    public static void encode(InnerDemonStatePacket packet, FriendlyByteBuf buffer) { buffer.writeBoolean(packet.locked); }
    public static InnerDemonStatePacket decode(FriendlyByteBuf buffer) { return new InnerDemonStatePacket(buffer.readBoolean()); }
    public static void handle(InnerDemonStatePacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> InnerDemonClientState.setLocked(packet.locked)));
        context.get().setPacketHandled(true);
    }
}
