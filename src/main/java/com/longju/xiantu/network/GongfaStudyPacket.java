package com.longju.xiantu.network;

import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

/** Presentation only: the server's Pending transaction remains the source of truth. */
public record GongfaStudyPacket(UUID player, String method, int ticks) {
    public static void encode(GongfaStudyPacket p, FriendlyByteBuf b) {
        b.writeUUID(p.player); b.writeUtf(p.method, 128); b.writeVarInt(p.ticks);
    }
    public static GongfaStudyPacket decode(FriendlyByteBuf b) {
        return new GongfaStudyPacket(b.readUUID(), b.readUtf(128), b.readVarInt());
    }
    public static void handle(GongfaStudyPacket p, Supplier<NetworkEvent.Context> supplier) {
        com.longju.xiantu.client.GongfaStudyRenderer.receive(p);
        supplier.get().setPacketHandled(true);
    }
}
