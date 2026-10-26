package com.longju.xiantu.registry;

import com.longju.xiantu.XiantuMod;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModParticles {
    public static final DeferredRegister<net.minecraft.core.particles.ParticleType<?>> PARTICLES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, XiantuMod.MOD_ID);
    public static final RegistryObject<SimpleParticleType> FORMATION_QI = PARTICLES.register("formation_qi", () -> new SimpleParticleType(false));
    private ModParticles() {}
}
