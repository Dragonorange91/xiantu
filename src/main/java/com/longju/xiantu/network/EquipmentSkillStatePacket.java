package com.longju.xiantu.network;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;
public record EquipmentSkillStatePacket(CompoundTag state,long time){
    public static void encode(EquipmentSkillStatePacket p,FriendlyByteBuf b){b.writeNbt(p.state);b.writeLong(p.time);}
    public static EquipmentSkillStatePacket decode(FriendlyByteBuf b){return new EquipmentSkillStatePacket(b.readNbt(),b.readLong());}
    public static void handle(EquipmentSkillStatePacket p,Supplier<NetworkEvent.Context> c){DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->com.longju.xiantu.client.EquipmentSkillClient.update(p));c.get().setPacketHandled(true);}
}
