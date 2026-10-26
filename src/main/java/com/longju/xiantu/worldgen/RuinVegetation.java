package com.longju.xiantu.worldgen;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;

/** Actual vanilla configured features, after all neighbouring template tiles exist.
 * Never grows trees during a chunk's partial structure write or loads neighbours.
 */
public final class RuinVegetation {
    public static void tick(ServerLevel level, RuinAdventureData.Instance instance) {
        if (!instance.spec.has("vegetation") || instance.state.getBoolean("VegetationComplete")) return;
        var entries=instance.spec.getAsJsonArray("vegetation");
        var done=instance.state.getCompound("VegetationAttempts");
        int budget=4;
        boolean changed=false;
        for (int k=0;k<entries.size()&&budget>0;k++) {
            String key=Integer.toString(k);
            if (done.getBoolean(key)) continue;
            var entry=entries.get(k).getAsJsonObject();
            var at=instance.absolute(entry.getAsJsonArray("position"));
            // Vanilla tree/grass implementations may read beyond their origin chunk.
            if (!level.hasChunksAt(at.offset(-16,-4,-16),at.offset(16,16,16))) continue;
            String featureId=entry.get("feature").getAsString();
            boolean tree=featureId.equals("minecraft:oak");
            if (tree&&budget<4) continue;
            boolean space=level.getBlockState(at.below()).is(Blocks.GRASS_BLOCK);
            if(tree) space &= level.getBlockState(at).isAir() && level.getBlockState(at.above()).isAir();
            var feature=level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                    .get(new ResourceLocation(featureId));
            if (space&&feature!=null) {
                var random=RandomSource.create(level.getSeed()^at.asLong()^instance.id.hashCode());
                RuinProtection.internal(()->feature.place(level,level.getChunkSource().getGenerator(),random,at));
            }
            // One attempt per fixed anchor, persisted; revisiting never farms new resources.
            done.putBoolean(key,true);budget-=tree?4:1;changed=true;
        }
        if(changed) {
            instance.state.put("VegetationAttempts",done);
            if(done.getAllKeys().size()==entries.size()) instance.state.putBoolean("VegetationComplete",true);
            RuinAdventureData.get(level).setDirty();
        }
    }
    private RuinVegetation() {}
}
