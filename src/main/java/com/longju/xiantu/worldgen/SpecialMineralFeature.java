package com.longju.xiantu.worldgen;
import com.longju.xiantu.config.XiantuConfig;
import com.longju.xiantu.mining.SpecialMineralRegistry;
import com.longju.xiantu.terrain.TerrainBiomes;
import net.minecraft.core.*;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import java.util.*;

/** One per-chunk roll, one Y origin, bounded connected vein; never rescans existing chunks. */
public final class SpecialMineralFeature extends Feature<NoneFeatureConfiguration> {
    private final SpecialMineralRegistry.Mineral mineral;
    public SpecialMineralFeature(SpecialMineralRegistry.Mineral mineral) { super(NoneFeatureConfiguration.CODEC); this.mineral=mineral; }
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> c) {
        var level=c.level(); var origin=c.origin();
        var configured=XiantuConfig.SPECIAL_ORE_CHANCES.get(mineral.id());
        if (c.random().nextDouble()>=(configured==null?mineral.chance():configured.get()) || !habitat(level,origin)) return false;
        if (!host(level,origin)) return false;
        int target=mineral.minVein()+c.random().nextInt(mineral.maxVein()-mineral.minVein()+1);
        List<BlockPos> placed=new ArrayList<>();
        if (!put(level,origin)) return false;
        placed.add(origin.immutable());
        for(int tries=0;placed.size()<target && tries<target*12;tries++) {
            BlockPos pos=placed.get(c.random().nextInt(placed.size())).relative(Direction.values()[c.random().nextInt(6)]);
            if (pos.getY()<mineral.minY() || pos.getY()>mineral.maxY() || placed.contains(pos)
                    || Math.abs(pos.getX()-origin.getX())>3 || Math.abs(pos.getZ()-origin.getZ())>3) continue;
            if (put(level,pos)) placed.add(pos);
        }
        return true;
    }
    private boolean habitat(WorldGenLevel level,BlockPos pos) {
        var biome=level.getBiome(pos);
        return switch(mineral.habitat()) {
            case "mountain" -> biome.is(BiomeTags.IS_MOUNTAIN);
            case "cold" -> biome.value().getBaseTemperature()<0.2F;
            case "thunder" -> biome.is(TerrainBiomes.THUNDER_WASTES);
            case "nether" -> biome.is(Biomes.BASALT_DELTAS)||biome.is(Biomes.NETHER_WASTES);
            case "end" -> (long)pos.getX()*pos.getX()+(long)pos.getZ()*pos.getZ()>1048576L
                    && (biome.is(Biomes.END_HIGHLANDS)||biome.is(Biomes.END_MIDLANDS)||biome.is(Biomes.END_BARRENS));
            case "lava" -> nearLava(level,pos);
            default -> true;
        };
    }
    private boolean nearLava(WorldGenLevel level,BlockPos pos) {
        for(BlockPos q:BlockPos.betweenClosed(pos.offset(-6,-6,-6),pos.offset(6,6,6)))
            if(q.distSqr(pos)<=36 && level.hasChunk(q.getX()>>4,q.getZ()>>4)) {
                var s=level.getBlockState(q);
                if(s.is(Blocks.LAVA) && s.getFluidState().isSource()) return true;
            }
        return false;
    }
    private boolean host(WorldGenLevel level,BlockPos pos) {
        if(level.isOutsideBuildHeight(pos)||!level.ensureCanWrite(pos)||level.getBlockEntity(pos)!=null) return false;
        if(inStructure(level,pos)) return false;
        var state=level.getBlockState(pos);
        return switch(mineral.habitat()) {
            case "deep" -> state.is(Blocks.DEEPSLATE);
            case "nether" -> state.is(Blocks.NETHERRACK)||state.is(Blocks.BASALT);
            case "end" -> state.is(Blocks.END_STONE);
            default -> state.is(Blocks.STONE)||state.is(Blocks.DEEPSLATE)||state.is(Blocks.GRANITE)
                    ||state.is(Blocks.DIORITE)||state.is(Blocks.ANDESITE)||state.is(Blocks.TUFF)
                    ||state.is(com.longju.xiantu.terrain.TerrainBlockRegistry.BLOCKS_BY_ID.get("thunder_rock").get())
                    ||state.is(com.longju.xiantu.terrain.TerrainBlockRegistry.BLOCKS_BY_ID.get("meteor_rock").get())
                    ||state.is(com.longju.xiantu.terrain.TerrainBlockRegistry.BLOCKS_BY_ID.get("valley_streamstone").get());
        };
    }
    private boolean inStructure(WorldGenLevel level,BlockPos pos) {
        var chunk=level.getChunk(pos);
        for(var start:chunk.getAllStarts().values())
            if(start.isValid()&&start.getBoundingBox().isInside(pos)) return true;
        // Structure references can point to a start in another chunk. Never load a missing chunk.
        for(var refs:chunk.getAllReferences().values()) for(long packed:refs) {
            var cp=new ChunkPos(packed);
            var referenced=level.getChunk(cp.x,cp.z,net.minecraft.world.level.chunk.ChunkStatus.STRUCTURE_STARTS,false);
            if(referenced==null) continue;
            for(var start:referenced.getAllStarts().values())
                if(start.isValid()&&start.getBoundingBox().isInside(pos)) return true;
        }
        return false;
    }
    private boolean put(WorldGenLevel level,BlockPos pos) {
        var rare=com.longju.xiantu.mining.AdvancedMinerals.ORES.get(mineral.id());
        var state=rare==null?SpecialMineralRegistry.ORES.get(mineral.id()).get().defaultBlockState():rare.get().defaultBlockState();
        if(state.hasProperty(com.longju.xiantu.terrain.NaturalResourceOreBlock.NATURAL_NODE))state=state.setValue(com.longju.xiantu.terrain.NaturalResourceOreBlock.NATURAL_NODE,true);
        return host(level,pos) && level.setBlock(pos,state,2);
    }
}
