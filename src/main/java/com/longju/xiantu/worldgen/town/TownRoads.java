package com.longju.xiantu.worldgen.town;

import java.util.List;

/** Blueprint road corridors. A cross-section always shares one elevation. */
public final class TownRoads {
    public record Road(boolean alongX,int start,int end,int low,int high) {
        public boolean contains(int x,int z){int t=alongX?x:z,c=alongX?z:x;return t>=start&&t<=end&&c>=low&&c<=high;}
        public TownTerrainPlan.Plot section(int t){return alongX?new TownTerrainPlan.Plot(t,low,t,high):new TownTerrainPlan.Plot(low,t,high,t);}
    }
    public static final List<Road> ALL=List.of(
        new Road(false,-8,140,69,75),new Road(true,-8,152,63,69),
        new Road(true,30,59,53,57),new Road(true,85,119,53,57),
        new Road(true,30,59,75,79),new Road(true,85,111,75,79),
        new Road(false,47,57,30,34),new Road(false,47,57,115,119),
        new Road(false,75,86,30,34),new Road(false,75,86,107,111));
    public static final List<TownTerrainPlan.Plot> LANDINGS=List.of(
        new TownTerrainPlan.Plot(30,53,34,57),new TownTerrainPlan.Plot(115,53,119,57),
        new TownTerrainPlan.Plot(30,75,34,79),new TownTerrainPlan.Plot(107,75,111,79));
    public static Road at(int x,int z){for(var road:ALL)if(road.contains(x,z))return road;return null;}
    private TownRoads(){}
}
