package com.longju.xiantu.client;
import com.longju.xiantu.network.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid="xiantu",value=Dist.CLIENT)
public final class AccessoryButtonEvents {
    @SubscribeEvent public static void init(ScreenEvent.Init.Post e){
        if(e.getScreen() instanceof InventoryScreen s){
            // Relative to the live screen origin, including the recipe book's shifted layout.
            e.addListener(new Button(s.getGuiLeft()+79,s.getGuiTop()+27,16,18,Component.literal("饰"),
                b->{if(!InnerDemonClientState.locked())XiantuNetwork.CHANNEL.sendToServer(new OpenAccessoriesPacket());},supplier -> supplier.get()){
                @Override public void renderWidget(net.minecraft.client.gui.GuiGraphics g,int mx,int my,float dt){
                    setX(s.getGuiLeft()+79);setY(s.getGuiTop()+27);super.renderWidget(g,mx,my,dt);
                }
            });
        }
    }
}
