package com.longju.xiantu.network;
import com.longju.xiantu.refining.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;
public record EquipmentSkillIntentPacket(boolean open){
    public static void encode(EquipmentSkillIntentPacket p,FriendlyByteBuf b){b.writeBoolean(p.open);}
    public static EquipmentSkillIntentPacket decode(FriendlyByteBuf b){return new EquipmentSkillIntentPacket(b.readBoolean());}
    public static void handle(EquipmentSkillIntentPacket m,Supplier<NetworkEvent.Context> c){var p=c.get().getSender();if(p!=null&&QitianSkills.pieces(p)==4){if(m.open)EquipmentSkillMenu.open(p);else{String error=QitianSkills.activate(p);if(!error.isEmpty())p.displayClientMessage(Component.literal(error),true);}}c.get().setPacketHandled(true);}
}
