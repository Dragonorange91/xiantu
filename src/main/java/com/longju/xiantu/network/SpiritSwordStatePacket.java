package com.longju.xiantu.network;

import com.longju.xiantu.spell.SwordFlightMotion.State;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** Server position/velocity plus the last applied input, sent only to the rider. */
public record SpiritSwordStatePacket(int entity,int acknowledged,State state,boolean blocked){
 public static void encode(SpiritSwordStatePacket p,FriendlyByteBuf b){b.writeInt(p.entity);b.writeInt(p.acknowledged);var s=p.state;b.writeDouble(s.x());b.writeDouble(s.y());b.writeDouble(s.z());b.writeDouble(s.vx());b.writeDouble(s.vy());b.writeDouble(s.vz());b.writeBoolean(p.blocked);}
 public static SpiritSwordStatePacket decode(FriendlyByteBuf b){return new SpiritSwordStatePacket(b.readInt(),b.readInt(),new State(b.readDouble(),b.readDouble(),b.readDouble(),b.readDouble(),b.readDouble(),b.readDouble()),b.readBoolean());}
 public static void handle(SpiritSwordStatePacket p,Supplier<NetworkEvent.Context> c){DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->com.longju.xiantu.client.SpiritSwordClient.receive(p));c.get().setPacketHandled(true);}
}
