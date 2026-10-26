package com.longju.xiantu.terrain;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Registry;
import net.minecraft.resources.*;
import net.minecraft.world.level.biome.*;
import terrablender.api.*;
import java.util.function.Consumer;

/** Reuses complete vanilla climate cells instead of cutting narrow rectangular slivers through them. */
public final class XiantuTerrainRegion extends Region {
    private final ResourceKey<Biome> theme;
    public XiantuTerrainRegion(ResourceLocation id,ResourceKey<Biome> theme,int weight) {
        super(id,RegionType.OVERWORLD,weight);this.theme=theme;
    }
    @Override public void addBiomes(Registry<Biome> registry,
            Consumer<Pair<Climate.ParameterPoint,ResourceKey<Biome>>> mapper) {
        addModifiedVanillaOverworldBiomes(pair -> mapper.accept(Pair.of(pair.getFirst(),
                replacement(pair.getFirst(),pair.getSecond(),theme))), builder -> {});
    }
    public static ResourceKey<Biome> replacement(Climate.ParameterPoint p,ResourceKey<Biome> original,ResourceKey<Biome> theme) {
        if(!original.location().getNamespace().equals("minecraft")) return original;
        return TerrainCellRules.select(p,original.location().getPath()).isEmpty()?original:theme;
    }
}
