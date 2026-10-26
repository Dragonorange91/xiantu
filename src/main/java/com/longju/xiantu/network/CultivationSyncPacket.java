package com.longju.xiantu.network;

import com.longju.xiantu.cultivation.CultivationCapability;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record CultivationSyncPacket(int realm, int stage, long powerMilli, long powerMaxMilli, long progressMilli, long progressMax, int rootMask, int totalDoses, double toxicity, boolean cultivationUnlocked, net.minecraft.nbt.CompoundTag gongfa, String physique, boolean trueSight) {
    public static void encode(CultivationSyncPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.realm);
        buf.writeVarInt(packet.stage);
        buf.writeLong(packet.powerMilli);
        buf.writeLong(packet.powerMaxMilli);
        buf.writeLong(packet.progressMilli);
        buf.writeLong(packet.progressMax);
        buf.writeVarInt(packet.rootMask);
        buf.writeVarInt(packet.totalDoses);
        buf.writeDouble(packet.toxicity);
        buf.writeBoolean(packet.cultivationUnlocked); buf.writeNbt(packet.gongfa);
        buf.writeUtf(packet.physique,32); buf.writeBoolean(packet.trueSight);
    }

    public static CultivationSyncPacket decode(FriendlyByteBuf buf) {
        return new CultivationSyncPacket(buf.readVarInt(), buf.readVarInt(), buf.readLong(), buf.readLong(), buf.readLong(), buf.readLong(), buf.readVarInt(), buf.readVarInt(), buf.readDouble(), buf.readBoolean(), buf.readNbt(),buf.readUtf(32),buf.readBoolean());
    }

    public static void handle(CultivationSyncPacket packet, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        CultivationCapability.clientUpdate(packet);
        context.setPacketHandled(true);
    }
}
