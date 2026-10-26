package com.longju.xiantu.network;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.*;
import java.util.function.Supplier;
public record SpellIntentPacket(int slot){
 public static void encode(SpellIntentPacket p,FriendlyByteBuf b){b.writeInt(p.slot);}
 public static SpellIntentPacket decode(FriendlyByteBuf b){return new SpellIntentPacket(b.readInt());}
 public static void handle(SpellIntentPacket m,Supplier<NetworkEvent.Context> c){var p=c.get().getSender();if(p!=null){if(m.slot==-1)com.longju.xiantu.spell.SpellMenu.open(p);else{String error=com.longju.xiantu.spell.SpellService.begin(p,m.slot);if(!error.isEmpty())p.displayClientMessage(Component.literal(error),true);}}c.get().setPacketHandled(true);}
}
