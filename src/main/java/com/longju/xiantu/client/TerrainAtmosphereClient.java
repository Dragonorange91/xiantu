package com.longju.xiantu.client;

import com.longju.xiantu.XiantuMod;
import com.longju.xiantu.registry.ModSounds;
import com.longju.xiantu.terrain.TerrainBiomes;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

@Mod.EventBusSubscriber(modid = XiantuMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TerrainAtmosphereClient {
    private static boolean wasInThunderWastes;
    private static int thunderParticleTicks;
    private static int meteorSkyTicks;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            wasInThunderWastes = false;
            thunderParticleTicks = 0;
            meteorSkyTicks = 0;
            return;
        }
        boolean inThunderWastes = minecraft.level.getBiome(minecraft.player.blockPosition()).is(TerrainBiomes.THUNDER_WASTES);
        if (inThunderWastes && !wasInThunderWastes) {
            minecraft.player.playSound(ModSounds.THUNDER_WASTES_ENTRY.get(), 0.55F, 1.0F);
        }
        wasInThunderWastes = inThunderWastes;

        if (inThunderWastes && ++thunderParticleTicks >= 40) {
            thunderParticleTicks = 0;
            if (minecraft.level.random.nextInt(4) == 0) {
                BlockPos center = minecraft.player.blockPosition();
                double x = center.getX() + 0.5D + minecraft.level.random.nextDouble() * 8.0D - 4.0D;
                double y = center.getY() + 1.0D + minecraft.level.random.nextDouble() * 3.0D;
                double z = center.getZ() + 0.5D + minecraft.level.random.nextDouble() * 8.0D - 4.0D;
                minecraft.level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z,
                        (minecraft.level.random.nextDouble() - 0.5D) * 0.02D,
                        (minecraft.level.random.nextDouble() - 0.5D) * 0.01D,
                        (minecraft.level.random.nextDouble() - 0.5D) * 0.02D);
            }
        } else if (!inThunderWastes) {
            thunderParticleTicks = 0;
        }

        boolean inMeteorWasteland = minecraft.level.getBiome(minecraft.player.blockPosition()).is(TerrainBiomes.METEOR_WASTELAND);
        if (!inMeteorWasteland) {
            meteorSkyTicks = 0;
            return;
        }
        if (++meteorSkyTicks >= 200) {
            meteorSkyTicks = 0;
            long dayTime = minecraft.level.getDayTime() % 24000L;
            if (dayTime >= 13000L && dayTime < 23000L && minecraft.level.random.nextInt(5) == 0) {
                BlockPos center = minecraft.player.blockPosition();
                DustParticleOptions indigoAurora = new DustParticleOptions(new Vector3f(0.31F, 0.22F, 0.88F), 1.05F);
                DustParticleOptions cyanAurora = new DustParticleOptions(new Vector3f(0.2F, 0.76F, 0.82F), 0.85F);
                double phase = minecraft.level.random.nextDouble() * Math.PI * 2.0D;
                double direction = minecraft.level.random.nextDouble() * Math.PI * 2.0D;
                for (int i = 0; i < 24; i++) {
                    double along = (i / 23.0D - 0.5D) * 56.0D;
                    double wave = Math.sin(phase + along * 0.11D) * 2.2D;
                    double x = center.getX() + 0.5D + Math.cos(direction) * along
                            + (minecraft.level.random.nextDouble() - 0.5D) * 1.4D;
                    double z = center.getZ() + 0.5D + Math.sin(direction) * along
                            + (minecraft.level.random.nextDouble() - 0.5D) * 1.4D;
                    double y = center.getY() + 28.0D + wave + minecraft.level.random.nextDouble() * 1.5D;
                    DustParticleOptions color = i % 4 == 0 ? cyanAurora : indigoAurora;
                    minecraft.level.addParticle(color, x, y, z,
                            (minecraft.level.random.nextDouble() - 0.5D) * 0.006D,
                            0.002D + minecraft.level.random.nextDouble() * 0.004D,
                            (minecraft.level.random.nextDouble() - 0.5D) * 0.006D);
                }
            }
        }
    }

    private TerrainAtmosphereClient() {}
}
