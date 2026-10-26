package com.longju.xiantu.network;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;
/** Visual identity only; never transfers attributes, skills, collision or AI. */
public record QitianAppearancePacket(UUID owner,ResourceLocation dimension,String species){
 public static void encode(QitianAppearancePacket p,FriendlyByteBuf b){b.writeUUID(p.owner);b.writeResourceLocation(p.dimension);b.writeUtf(p.species,64);}
 public static QitianAppearancePacket decode(FriendlyByteBuf b){return new QitianAppearancePacket(b.readUUID(),b.readResourceLocation(),b.readUtf(64));}
 public static void handle(QitianAppearancePacket p,Supplier<NetworkEvent.Context> c){DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->com.longju.xiantu.client.QitianAppearance.receive(p));c.get().setPacketHandled(true);}
}
