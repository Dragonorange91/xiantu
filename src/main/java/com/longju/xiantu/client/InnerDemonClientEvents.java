package com.longju.xiantu.client;

import com.longju.xiantu.XiantuMod;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = XiantuMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class InnerDemonClientEvents {
    private InnerDemonClientEvents() {}
    @SubscribeEvent public static void opening(ScreenEvent.Opening event) {
        if (InnerDemonClientState.locked() && event.getNewScreen() instanceof AbstractContainerScreen<?>)
            event.setCanceled(true);
    }
    @SubscribeEvent public static void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        InnerDemonClientState.setLocked(false);
    }
}
