package com.longju.xiantu.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.UUID;
import java.util.function.Supplier;

/** Only input intent; never accepts charge time, power, size, damage or a target. */
public record GreatFireballInputPacket(int slot,UUID gesture,int action){
    public static void encode(GreatFireballInputPacket p,FriendlyByteBuf b){b.writeByte(p.slot);b.writeUUID(p.gesture);b.writeByte(p.action);}
    public static GreatFireballInputPacket decode(FriendlyByteBuf b){return new GreatFireballInputPacket(b.readByte(),b.readUUID(),b.readByte());}
    public static void handle(GreatFireballInputPacket p,Supplier<NetworkEvent.Context> context){
        var player=context.get().getSender();if(player!=null){String error=com.longju.xiantu.spell.GreatFireballService.input(player,p.slot,p.gesture,p.action);if(!error.isEmpty())player.displayClientMessage(net.minecraft.network.chat.Component.literal(error),true);}context.get().setPacketHandled(true);
    }
}
