package com.longju.xiantu.network;

import com.longju.xiantu.entity.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.UUID;
import java.util.function.Supplier;

public record BeastRealmPacket(ResourceLocation dimension,UUID uuid,int realm,int stage,long progress,boolean elite,String element){
    public static BeastRealmPacket of(Mob m){var d=m.getPersistentData();return new BeastRealmPacket(m.level().dimension().location(),m.getUUID(),BeastRealmService.realm(m),d.getInt(BeastRealmService.STAGE),d.getLong(BeastRealmService.PROGRESS),BeastRealmService.elite(m),d.getString(BeastRealmService.ELEMENT));}
    public static void encode(BeastRealmPacket p,FriendlyByteBuf b){b.writeResourceLocation(p.dimension);b.writeUUID(p.uuid);b.writeVarInt(p.realm);b.writeVarInt(p.stage);b.writeVarLong(p.progress);b.writeBoolean(p.elite);b.writeUtf(p.element,16);}
    public static BeastRealmPacket decode(FriendlyByteBuf b){return new BeastRealmPacket(b.readResourceLocation(),b.readUUID(),b.readVarInt(),b.readVarInt(),b.readVarLong(),b.readBoolean(),b.readUtf(16));}
    public static void handle(BeastRealmPacket p,Supplier<NetworkEvent.Context> c){c.get().enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->com.longju.xiantu.client.BeastRealmDisplay.receive(p)));c.get().setPacketHandled(true);}
}
