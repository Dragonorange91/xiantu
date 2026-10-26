package com.longju.xiantu.wood;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;

public final class SpiritTreeFeature extends Feature<NoneFeatureConfiguration>{
    private static final TagKey<Biome> SPIRIT_WOOD_FORESTS = TagKey.create(
            Registries.BIOME, new ResourceLocation("xiantu", "spirit_wood/vanilla_forests"));
    private final SpiritWoodType type;
    public SpiritTreeFeature(SpiritWoodType type){super(NoneFeatureConfiguration.CODEC);this.type=type;}
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context){
        var l=context.level();var p=l.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG,context.origin());
        boolean spiritWoodForest = l.getBiome(p).is(SPIRIT_WOOD_FORESTS);
        double chance = SpiritWoodWorldgenRules.chance(type, spiritWoodForest);
        if(context.random().nextDouble()>=chance)return false;
        if((!spiritWoodForest && p.getY()<type.minimumHeight())||!l.getBlockState(p.below()).is(BlockTags.DIRT))return false;
        return SpiritTreeGrowth.grow(l,p,type);
    }
}
