package com.longju.xiantu.terrain;
import net.minecraft.world.level.biome.Climate;

/** Select an entire existing vanilla cell; never subdivide or alter its climate extents. */
public final class TerrainCellRules {
    public static String select(Climate.ParameterPoint p,String original) {
        // A region carries one theme. Vanilla's complete flat-land cells decide
        // suitability; extra temperature/humidity/erosion cuts create slivers.
        if(p.depth().min()!=0||p.depth().max()!=0) return "";
        if(!java.util.Set.of("plains","sunflower_plains","savanna").contains(original)) return "";
        return "flat_land";
    }
    private TerrainCellRules() {}
}
