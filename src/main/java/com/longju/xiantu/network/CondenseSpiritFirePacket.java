package com.longju.xiantu.network;

import com.longju.xiantu.refining.SpiritFire;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** The client requests an action; inventory and actual power are checked on the server. */
public record CondenseSpiritFirePacket(){
    public static void encode(CondenseSpiritFirePacket packet,FriendlyByteBuf buffer){}
    public static CondenseSpiritFirePacket decode(FriendlyByteBuf buffer){return new CondenseSpiritFirePacket();}
    public static void handle(CondenseSpiritFirePacket packet,Supplier<NetworkEvent.Context> context){
        var p=context.get().getSender();
        if(p!=null&&p.isAlive()&&p.containerMenu==p.inventoryMenu){
            long now=p.serverLevel().getServer().overworld().getGameTime();
            var tag=p.getPersistentData();
            if(now>=tag.getLong("xiantu_next_condense_fire")){
                tag.putLong("xiantu_next_condense_fire",now+10);
                String error=SpiritFire.condense(p);
                p.displayClientMessage(Component.literal(error.isEmpty()?"已凝聚灵火":error),true);
            }
        }
        context.get().setPacketHandled(true);
    }
}
