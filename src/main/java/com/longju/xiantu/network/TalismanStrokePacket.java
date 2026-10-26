package com.longju.xiantu.network;
import com.longju.xiantu.talisman.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.*;
import java.util.function.Supplier;
public record TalismanStrokePacket(int menu,long nonce,int stroke,List<TalismanDrawing.Point> points,boolean end){
 public static void encode(TalismanStrokePacket p,FriendlyByteBuf b){b.writeVarInt(p.menu);b.writeLong(p.nonce);b.writeVarInt(p.stroke);b.writeVarInt(p.points.size());for(var point:p.points){b.writeShort((int)(point.x()*65535));b.writeShort((int)(point.y()*65535));}b.writeBoolean(p.end);}
 public static TalismanStrokePacket decode(FriendlyByteBuf b){int menu=b.readVarInt();long nonce=b.readLong();int stroke=b.readVarInt(),n=b.readVarInt();if(n<1||n>32||stroke<0||stroke>6)throw new IllegalArgumentException("stroke bounds");var points=new ArrayList<TalismanDrawing.Point>();for(int i=0;i<n;i++)points.add(new TalismanDrawing.Point(b.readUnsignedShort()/65535.,b.readUnsignedShort()/65535.));return new TalismanStrokePacket(menu,nonce,stroke,List.copyOf(points),b.readBoolean());}
 public static void handle(TalismanStrokePacket p,Supplier<NetworkEvent.Context> context){var c=context.get();var sender=c.getSender();if(sender!=null&&sender.containerMenu instanceof TalismanMenu m&&m.containerId==p.menu&&m.stillValid(sender)&&m.materials instanceof TalismanTableEntity be){var n=sender.getPersistentData();long now=sender.level().getGameTime();if(n.getLong("xiantu_stroke_packet_tick")!=now){n.putLong("xiantu_stroke_packet_tick",now);n.putInt("xiantu_stroke_packet_count",0);}int count=n.getInt("xiantu_stroke_packet_count");if(count<4){n.putInt("xiantu_stroke_packet_count",count+1);be.stroke(sender,p.nonce,p.stroke,p.points,p.end);m.broadcastChanges();}}c.setPacketHandled(true);}
}
