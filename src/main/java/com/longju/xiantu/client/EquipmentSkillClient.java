package com.longju.xiantu.client;
import com.longju.xiantu.network.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.components.Button;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
@Mod.EventBusSubscriber(modid="xiantu",value=Dist.CLIENT)
public final class EquipmentSkillClient {
    public static CompoundTag state=new CompoundTag();public static long time,received;
    public static final KeyMapping USE=new KeyMapping("key.xiantu.equipment_skill",GLFW.GLFW_KEY_R,"key.categories.xiantu");
    public static void update(EquipmentSkillStatePacket p){state=p.state()==null?new CompoundTag():p.state().copy();time=p.time();received=System.nanoTime();}
    public static long now(){return time+Math.max(0,(System.nanoTime()-received)/50000000);}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){if(e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();while(USE.consumeClick())if(mc.player!=null&&mc.screen==null&&com.longju.xiantu.refining.QitianSkills.pieces(mc.player)==4)XiantuNetwork.CHANNEL.sendToServer(new EquipmentSkillIntentPacket(mc.player.isShiftKeyDown()));}
    @SubscribeEvent public static void inventory(ScreenEvent.Init.Post e){if(e.getScreen() instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen s&&Minecraft.getInstance().player!=null&&com.longju.xiantu.refining.QitianSkills.pieces(Minecraft.getInstance().player)==4)e.addListener(Button.builder(Component.literal("技"),b->XiantuNetwork.CHANNEL.sendToServer(new EquipmentSkillIntentPacket(true))).bounds(s.getGuiLeft()+98,s.getGuiTop()+48,16,18).build());}
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e){state=new CompoundTag();}
    private EquipmentSkillClient(){}
}
