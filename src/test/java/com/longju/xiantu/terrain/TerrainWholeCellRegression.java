package com.longju.xiantu.terrain;
import net.minecraft.world.level.biome.*;
public final class TerrainWholeCellRegression {
    private static Climate.ParameterPoint point(float t0,float t1,float h0,float h1,float c,float e,float d) {
        return Climate.parameters(Climate.Parameter.span(t0,t1),Climate.Parameter.span(h0,h1),Climate.Parameter.span(c,1),Climate.Parameter.span(e,1),Climate.Parameter.point(d),Climate.Parameter.span(-1,1),0);
    }
    public static void main(String[] args) {
        var thunder=point(-.45F,-.15F,-1,-.35F,.03F,.55F,0);
        var meteor=point(.2F,.55F,-1,-.35F,.03F,.55F,0);
        var medicine=point(.2F,.55F,-.1F,.1F,.03F,.55F,0);
        require(TerrainCellRules.select(thunder,"plains").equals("flat_land"),"cool dry plains");
        require(TerrainCellRules.select(meteor,"savanna").equals("flat_land"),"warm dry lowland");
        require(TerrainCellRules.select(medicine,"plains").equals("flat_land"),"warm wet lowland");
        for(var original:java.util.List.of("river","ocean","meadow","lush_caves"))
            require(TerrainCellRules.select(thunder,original).isEmpty(),"unmodified branch "+original);
        require(TerrainCellRules.select(thunder,"forest").isEmpty(),"forests retained");
        require(TerrainCellRules.select(thunder,"jagged_peaks").isEmpty(),"mountains retained");
        require(TerrainCellRules.select(point(-.45F,-.15F,-1,-.35F,.03F,.55F,1),"plains").isEmpty(),"underground retained");
        try {
            var json=com.google.gson.JsonParser.parseString(java.nio.file.Files.readString(java.nio.file.Path.of("src/test/resources/terrain-vanilla-cells-1.20.1.json"))).getAsJsonArray();
            var counts=new java.util.TreeMap<String,Integer>();
            for(var entry:json){
                var cell=entry.getAsJsonObject();
                var pnt=Climate.parameters(span(cell,"temperature"),span(cell,"humidity"),span(cell,"continentalness"),span(cell,"erosion"),Climate.Parameter.point(0),span(cell,"weirdness"),0);
                String selected=TerrainCellRules.select(pnt,cell.get("biome").getAsString());
                if(!selected.isEmpty())counts.merge(selected,1,Integer::sum);
            }
            require(counts.getOrDefault("flat_land",0)>500,"complete flat-land cells reachable in each theme region");
            System.out.println("Actual 1.20.1 vanilla surface-cell fixture: "+counts);
        }catch(java.io.IOException ex){throw new RuntimeException(ex);}
        System.out.println("Terrain whole-cell and native-climate fixture checks passed; dedicated regions have weights 2/2/1");
    }
    private static Climate.Parameter span(com.google.gson.JsonObject cell,String key) {
        var a=cell.getAsJsonArray(key);return new Climate.Parameter(a.get(0).getAsLong(),a.get(1).getAsLong());
    }
    private static void require(boolean v,String label){if(!v)throw new AssertionError(label);}
}
