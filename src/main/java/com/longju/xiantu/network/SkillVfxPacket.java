package com.longju.xiantu.network;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.UUID;
import java.util.function.Supplier;
/** Presentation only. No client packet can authorize a hit. */
public record SkillVfxPacket(UUID transaction,UUID owner,int entityId,ResourceLocation dimension,
 String skill,String phase,Vec3 position,Vec3 direction) {
 public static void encode(SkillVfxPacket p,FriendlyByteBuf b){b.writeUUID(p.transaction);b.writeUUID(p.owner);b.writeVarInt(p.entityId);b.writeResourceLocation(p.dimension);b.writeUtf(p.skill,64);b.writeUtf(p.phase,16);b.writeDouble(p.position.x);b.writeDouble(p.position.y);b.writeDouble(p.position.z);b.writeDouble(p.direction.x);b.writeDouble(p.direction.y);b.writeDouble(p.direction.z);}
 public static SkillVfxPacket decode(FriendlyByteBuf b){return new SkillVfxPacket(b.readUUID(),b.readUUID(),b.readVarInt(),b.readResourceLocation(),b.readUtf(64),b.readUtf(16),new Vec3(b.readDouble(),b.readDouble(),b.readDouble()),new Vec3(b.readDouble(),b.readDouble(),b.readDouble()));}
 public static void handle(SkillVfxPacket p,Supplier<NetworkEvent.Context> c){c.get().enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->com.longju.xiantu.client.SkillPresentationClient.receive(p)));c.get().setPacketHandled(true);}
}
