package com.longju.xiantu.network;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import java.util.function.Supplier;
public record SpellStatePacket(CompoundTag data,long time,String casting){
 public static void encode(SpellStatePacket p,FriendlyByteBuf b){b.writeNbt(p.data);b.writeLong(p.time);b.writeUtf(p.casting,64);}
 public static SpellStatePacket decode(FriendlyByteBuf b){return new SpellStatePacket(b.readNbt(),b.readLong(),b.readUtf(64));}
 public static void handle(SpellStatePacket p,Supplier<NetworkEvent.Context> c){DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->com.longju.xiantu.client.SpellClient.update(p));c.get().setPacketHandled(true);}
}
