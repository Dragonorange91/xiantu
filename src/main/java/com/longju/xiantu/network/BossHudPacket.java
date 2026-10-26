package com.longju.xiantu.network;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import java.util.*;
import java.util.function.Supplier;
public record BossHudPacket(ResourceLocation dimension,List<Entry> entries){
 public record Entry(UUID entity,UUID event,String skin,Component name,float health,float maximum,int realm,int stage){}
 public static void encode(BossHudPacket p,FriendlyByteBuf b){b.writeResourceLocation(p.dimension);b.writeVarInt(p.entries.size());for(var e:p.entries){b.writeUUID(e.entity);b.writeUUID(e.event);b.writeUtf(e.skin,64);b.writeComponent(e.name);b.writeFloat(e.health);b.writeFloat(e.maximum);b.writeVarInt(e.realm);b.writeVarInt(e.stage);}}
 public static BossHudPacket decode(FriendlyByteBuf b){var dim=b.readResourceLocation();int size=b.readVarInt();if(size<0||size>8)throw new IllegalArgumentException("Boss HUD capacity");var entries=new ArrayList<Entry>();for(int i=0;i<size;i++)entries.add(new Entry(b.readUUID(),b.readUUID(),b.readUtf(64),b.readComponent(),b.readFloat(),b.readFloat(),b.readVarInt(),b.readVarInt()));return new BossHudPacket(dim,List.copyOf(entries));}
 public static void handle(BossHudPacket p,Supplier<NetworkEvent.Context> c){c.get().enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->com.longju.xiantu.client.BossHud.receive(p)));c.get().setPacketHandled(true);}
}
