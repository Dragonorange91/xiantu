package com.longju.xiantu.network;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;
public record FormationProjectionPacket(int tier,BlockPos locked){
    public static void encode(FormationProjectionPacket p,FriendlyByteBuf b){b.writeVarInt(p.tier);b.writeBoolean(p.locked!=null);if(p.locked!=null)b.writeBlockPos(p.locked);}
    public static FormationProjectionPacket decode(FriendlyByteBuf b){int tier=b.readVarInt();return new FormationProjectionPacket(tier,b.readBoolean()?b.readBlockPos():null);}
    public static void handle(FormationProjectionPacket p,Supplier<NetworkEvent.Context> context){DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->com.longju.xiantu.client.FormationProjectionClient.receive(p));context.get().setPacketHandled(true);}
}
