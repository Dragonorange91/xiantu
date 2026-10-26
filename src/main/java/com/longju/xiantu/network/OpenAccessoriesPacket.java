package com.longju.xiantu.network;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;
import com.longju.xiantu.equipment.AccessoryMenu;
public record OpenAccessoriesPacket() {
    public static void encode(OpenAccessoriesPacket p,FriendlyByteBuf b){}
    public static OpenAccessoriesPacket decode(FriendlyByteBuf b){return new OpenAccessoriesPacket();}
    public static void handle(OpenAccessoriesPacket msg,Supplier<NetworkEvent.Context> supplier){
        var context=supplier.get();var p=context.getSender();
        if(p!=null && p.isAlive() && !p.isSpectator() && p.containerMenu==p.inventoryMenu
            && p.inventoryMenu.getCarried().isEmpty() && !com.longju.xiantu.cultivation.InnerDemonService.active(p))
            NetworkHooks.openScreen(p,new SimpleMenuProvider((id,inv,player)->new AccessoryMenu(id,inv),Component.translatable("container.xiantu.accessories")));
        context.setPacketHandled(true);
    }
}
