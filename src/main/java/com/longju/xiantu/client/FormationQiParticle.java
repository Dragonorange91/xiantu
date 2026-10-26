package com.longju.xiantu.client;

import com.longju.xiantu.XiantuMod;
import com.longju.xiantu.formation.FormationCoreBlock;
import com.longju.xiantu.registry.ModParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Small fullbright vanilla spark sprites following a shrinking spiral, entirely client-side. */
public final class FormationQiParticle extends TextureSheetParticle {
    private final double centerX, centerY, centerZ, startY, radius, angle;
    private final float baseSize;
    private final BlockPos corePos;
    private final SpriteSet sprites;

    private FormationQiParticle(ClientLevel level, double x, double y, double z,
                                double toX, double toY, double toZ, SpriteSet sprites) {
        super(level, x, y, z);
        centerX = x + toX; centerY = y + toY; centerZ = z + toZ;
        startY = y; radius = Math.hypot(toX, toZ);
        angle = Math.atan2(z - centerZ, x - centerX);
        corePos = BlockPos.containing(centerX, centerY - 1.8 + .01, centerZ);
        this.sprites = sprites;
        int tier = radius > 8 ? 3 : radius > 5 ? 2 : 1;
        lifetime = 48 + tier * 6 + random.nextInt(7);
        baseSize = .075f + random.nextFloat() * .035f;
        quadSize = baseSize;
        hasPhysics = false;
        // Pale blue qi, with the higher arrays slightly brighter and more silver-white.
        setColor(.55f + .07f * tier, .86f + .025f * tier, 1f);
        setAlpha(0);
        setSpriteFromAge(sprites);
    }

    @Override public void tick() {
        xo = x; yo = y; zo = z;
        if (++age >= lifetime || !level.hasChunkAt(corePos)) { remove(); return; }
        BlockState state = level.getBlockState(corePos);
        if (!(state.getBlock() instanceof FormationCoreBlock)
                || !state.getValue(FormationCoreBlock.ACTIVE)) { remove(); return; }
        double t = (double) age / lifetime;
        double remaining = radius * Math.pow(1 - t, .85);
        double turn = angle + t * Math.PI * .8;
        setPos(centerX + Math.cos(turn) * remaining,
                startY + (centerY - startY) * t + Math.sin(Math.PI * t) * .18,
                centerZ + Math.sin(turn) * remaining);
        alpha = (float) Math.min(.8, Math.sin(Math.PI * t) * 1.05);
        quadSize = baseSize * (1 - (float)t * .4f);
        oRoll = roll;
        roll += .035f;
        setSpriteFromAge(sprites);
    }

    @Override public int getLightColor(float partialTick) { return 0xF000F0; }
    @Override public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }

    private record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override public Particle createParticle(SimpleParticleType type, ClientLevel level,
                double x, double y, double z, double toX, double toY, double toZ) {
            return new FormationQiParticle(level, x, y, z, toX, toY, toZ, sprites);
        }
    }

    @Mod.EventBusSubscriber(modid = XiantuMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void register(RegisterParticleProvidersEvent event) {
            event.registerSpriteSet(ModParticles.FORMATION_QI.get(), Provider::new);
        }
    }
}
