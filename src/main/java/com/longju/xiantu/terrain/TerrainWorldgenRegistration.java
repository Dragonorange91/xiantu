package com.longju.xiantu.terrain;

import com.longju.xiantu.XiantuMod;
import net.minecraft.world.level.levelgen.SurfaceRules;
import net.minecraft.world.level.levelgen.placement.CaveSurface;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import terrablender.api.Regions;
import terrablender.api.SurfaceRuleManager;

public final class TerrainWorldgenRegistration {
    public static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            Regions.register(new XiantuTerrainRegion(new net.minecraft.resources.ResourceLocation(XiantuMod.MOD_ID, "thunder_region"),TerrainBiomes.THUNDER_WASTES,2));
            Regions.register(new XiantuTerrainRegion(new net.minecraft.resources.ResourceLocation(XiantuMod.MOD_ID, "meteor_region"),TerrainBiomes.METEOR_WASTELAND,2));
            Regions.register(new XiantuTerrainRegion(new net.minecraft.resources.ResourceLocation(XiantuMod.MOD_ID, "medicine_region"),TerrainBiomes.SPIRIT_MEDICINE_PLAIN,1));
            SurfaceRuleManager.addSurfaceRules(SurfaceRuleManager.RuleCategory.OVERWORLD, XiantuMod.MOD_ID, surfaceRules());
        });
    }

    private static SurfaceRules.RuleSource surfaceRules() {
        return SurfaceRules.ifTrue(SurfaceRules.abovePreliminarySurface(), SurfaceRules.sequence(
                biome(TerrainBiomes.THUNDER_WASTES, "thunder_rock", "thunder_rock"),
                meteorWasteland(),
                medicinePlain()));
    }

    private static SurfaceRules.RuleSource medicinePlain() {
        return SurfaceRules.ifTrue(
                SurfaceRules.isBiome(TerrainBiomes.SPIRIT_MEDICINE_PLAIN),
                SurfaceRules.ifTrue(
                        SurfaceRules.not(SurfaceRules.hole()),
                        SurfaceRules.ifTrue(SurfaceRules.waterBlockCheck(-1, 0),
                                SurfaceRules.sequence(
                                SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR,
                                        SurfaceRules.state(Blocks.GRASS_BLOCK.defaultBlockState())),
                                SurfaceRules.ifTrue(
                                        SurfaceRules.stoneDepthCheck(TerrainClimateBands.SPECIAL_ROCK_DEPTH,
                                                false, CaveSurface.FLOOR),
                                        SurfaceRules.state(TerrainBlockRegistry.BLOCKS_BY_ID.get("valley_streamstone")
                                                .get().defaultBlockState()))))));
    }

    private static SurfaceRules.RuleSource meteorWasteland() {
        return SurfaceRules.ifTrue(
                SurfaceRules.isBiome(TerrainBiomes.METEOR_WASTELAND),
                SurfaceRules.ifTrue(
                        SurfaceRules.not(SurfaceRules.hole()),
                        SurfaceRules.ifTrue(SurfaceRules.waterBlockCheck(-1, 0),
                                SurfaceRules.sequence(
                                        SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR,
                                                SurfaceRules.state(TerrainBlockRegistry.BLOCKS_BY_ID
                                                        .get("star_dust_sand").get().defaultBlockState())),
                                        SurfaceRules.ifTrue(SurfaceRules.stoneDepthCheck(2, false, CaveSurface.FLOOR),
                                                SurfaceRules.state(TerrainBlockRegistry.BLOCKS_BY_ID
                                                        .get("meteor_soil").get().defaultBlockState())),
                                        SurfaceRules.ifTrue(SurfaceRules.stoneDepthCheck(
                                                        TerrainClimateBands.SPECIAL_ROCK_DEPTH, false, CaveSurface.FLOOR),
                                                SurfaceRules.state(TerrainBlockRegistry.BLOCKS_BY_ID
                                                        .get("meteor_rock").get().defaultBlockState()))))));
    }

    private static SurfaceRules.RuleSource biome(net.minecraft.resources.ResourceKey<net.minecraft.world.level.biome.Biome> biome,
                                                  String surface, String filler) {
        return SurfaceRules.ifTrue(SurfaceRules.isBiome(biome), SurfaceRules.ifTrue(
                SurfaceRules.not(SurfaceRules.hole()), SurfaceRules.ifTrue(
                SurfaceRules.waterBlockCheck(-1, 0), SurfaceRules.sequence(
                        SurfaceRules.ifTrue(SurfaceRules.ON_FLOOR, SurfaceRules.state(
                                TerrainBlockRegistry.BLOCKS_BY_ID.get(surface).get().defaultBlockState())),
                        SurfaceRules.ifTrue(SurfaceRules.stoneDepthCheck(TerrainClimateBands.SPECIAL_ROCK_DEPTH,
                                        false, CaveSurface.FLOOR), SurfaceRules.state(
                                TerrainBlockRegistry.BLOCKS_BY_ID.get(filler).get().defaultBlockState()))))));
    }

    private TerrainWorldgenRegistration() {}
}
