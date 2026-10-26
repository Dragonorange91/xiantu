package com.longju.xiantu.network;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;
public record SpiritSwordInputPacket(int entity,int sequence,float forward,float strafe,float vertical,float yaw,float pitch,boolean boost,boolean dismount){
 public static void encode(SpiritSwordInputPacket p,FriendlyByteBuf b){b.writeInt(p.entity);b.writeInt(p.sequence);b.writeFloat(p.forward);b.writeFloat(p.strafe);b.writeFloat(p.vertical);b.writeFloat(p.yaw);b.writeFloat(p.pitch);b.writeBoolean(p.boost);b.writeBoolean(p.dismount);}
 public static SpiritSwordInputPacket decode(FriendlyByteBuf b){return new SpiritSwordInputPacket(b.readInt(),b.readInt(),b.readFloat(),b.readFloat(),b.readFloat(),b.readFloat(),b.readFloat(),b.readBoolean(),b.readBoolean());}
 public static void handle(SpiritSwordInputPacket p,Supplier<NetworkEvent.Context> c){var player=c.get().getSender();if(player!=null&&player.getVehicle() instanceof com.longju.xiantu.spell.SpiritSwordEntity sword&&sword.getId()==p.entity)sword.input(player,p.sequence,p.forward,p.strafe,p.vertical,p.yaw,p.pitch,p.boost,p.dismount);c.get().setPacketHandled(true);}
}
