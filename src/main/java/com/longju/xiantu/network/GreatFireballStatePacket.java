package com.longju.xiantu.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.UUID;
import java.util.function.Supplier;

/** Server-confirmed world-space presentation shared with nearby clients. */
public record GreatFireballStatePacket(UUID transaction,UUID owner,int entity,ResourceLocation dimension,int phase,int ticks,Vec3 position,Vec3 velocity){
    public static void encode(GreatFireballStatePacket p,FriendlyByteBuf b){b.writeUUID(p.transaction);b.writeUUID(p.owner);b.writeVarInt(p.entity);b.writeResourceLocation(p.dimension);b.writeByte(p.phase);b.writeVarInt(p.ticks);b.writeDouble(p.position.x);b.writeDouble(p.position.y);b.writeDouble(p.position.z);b.writeDouble(p.velocity.x);b.writeDouble(p.velocity.y);b.writeDouble(p.velocity.z);}
    public static GreatFireballStatePacket decode(FriendlyByteBuf b){return new GreatFireballStatePacket(b.readUUID(),b.readUUID(),b.readVarInt(),b.readResourceLocation(),b.readByte(),b.readVarInt(),new Vec3(b.readDouble(),b.readDouble(),b.readDouble()),new Vec3(b.readDouble(),b.readDouble(),b.readDouble()));}
    public static void handle(GreatFireballStatePacket p,Supplier<NetworkEvent.Context> context){context.get().enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->com.longju.xiantu.client.GreatFireballRenderer.receive(p)));context.get().setPacketHandled(true);}
}
