package com.longju.xiantu.registry;

import com.longju.xiantu.XiantuMod;
import com.longju.xiantu.worldgen.SmallOreFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModWorldgen {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(ForgeRegistries.FEATURES, XiantuMod.MOD_ID);
    public static final RegistryObject<Feature<OreConfiguration>> SMALL_SPIRIT_ORE =
            FEATURES.register("small_spirit_ore", SmallOreFeature::new);
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> LIGHTNING_SCARRED_TREE =
            FEATURES.register("lightning_scarred_tree", com.longju.xiantu.worldgen.LightningScarredTreeFeature::new);
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> METEOR_CRATER =
            FEATURES.register("meteor_crater", com.longju.xiantu.worldgen.MeteorCraterFeature::new);
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> METEOR_FISSURE =
            FEATURES.register("meteor_fissure", com.longju.xiantu.worldgen.MeteorFissureFeature::new);
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> STORM_BAMBOO_PATCH =
            FEATURES.register("storm_bamboo_patch", com.longju.xiantu.worldgen.StormBambooPatchFeature::new);
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> HABITAT_LANDMARK =
            FEATURES.register("habitat_landmark", com.longju.xiantu.worldgen.HabitatLandmarkFeature::new);
    public static final RegistryObject<Feature<net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration>> HABITAT_PLANT =
            FEATURES.register("habitat_simple_block", com.longju.xiantu.worldgen.HabitatPlantFeature::new);
    static { for(var type:com.longju.xiantu.wood.SpiritWoodType.values()) FEATURES.register(type.id+"_tree",()->new com.longju.xiantu.wood.SpiritTreeFeature(type)); }
    static { FEATURES.register("tribulation_stone_outcrop",()->new com.longju.xiantu.worldgen.SpecialMineralFeature(com.longju.xiantu.mining.AdvancedMinerals.THUNDER_OUTCROP)); }
    static { for(var m:com.longju.xiantu.mining.AdvancedMinerals.NATURAL)
        FEATURES.register(m.oreId(),()->new com.longju.xiantu.worldgen.SpecialMineralFeature(m)); }
    static { for(var m:com.longju.xiantu.mining.SpecialMineralRegistry.ALL)
        FEATURES.register(m.oreId(),()->new com.longju.xiantu.worldgen.SpecialMineralFeature(m)); }
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> SURFACE_ROCK_LAYER =
            FEATURES.register("surface_rock_layer", com.longju.xiantu.worldgen.TerrainRockLayerFeature::new);
    private ModWorldgen() {}
}
