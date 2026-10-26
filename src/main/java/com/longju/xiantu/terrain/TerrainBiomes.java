package com.longju.xiantu.terrain;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

public final class TerrainBiomes {
    public static final ResourceKey<Biome> THUNDER_WASTES = key("thunder_wastes");
    public static final ResourceKey<Biome> METEOR_WASTELAND = key("meteor_wasteland");
    public static final ResourceKey<Biome> SPIRIT_MEDICINE_PLAIN = key("spirit_medicine_plain");
    private static ResourceKey<Biome> key(String name) {
        return ResourceKey.create(Registries.BIOME, new ResourceLocation("xiantu", name));
    }

    private TerrainBiomes() {}
}
