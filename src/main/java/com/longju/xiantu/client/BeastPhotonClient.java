package com.longju.xiantu.client;

import com.longju.xiantu.XiantuMod;
import com.longju.xiantu.network.BeastVfxPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;

/** Guard optional Photon class loading. No Photon reference reaches server classes. */
@Mod.EventBusSubscriber(modid=XiantuMod.MOD_ID,value=Dist.CLIENT)
public final class BeastPhotonClient {
    private BeastPhotonClient() {}
    public static void receive(BeastVfxPacket packet) {
        if (ModList.get().isLoaded("photon")) BeastPhotonRuntime.receive(packet);
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {

        if(event.phase==TickEvent.Phase.END)SkillCastAnimations.tick();
        if(event.phase==TickEvent.Phase.END && ModList.get().isLoaded("photon")) { BeastPhotonRuntime.tick(); BossPhotonTimeline.tick(); SkillPhotonRuntime.tick(); }
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { clear(); }
    public static void clear() { SkillPresentationClient.clear(); BossPhotonTimeline.clear();if(ModList.get().isLoaded("photon")) BeastPhotonRuntime.clear(); }
}
