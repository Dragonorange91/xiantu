package com.longju.xiantu.formation;

import java.util.*;

/** Two reciprocal ports per tile, including diagonal corner joints. No extra structure blocks. */
public final class FormationTrace {
    private FormationTrace() {}
    // Clockwise from north, in world X/Z coordinates.
    public static final int[] DX = {0,1,1,1,0,-1,-1,-1};
    public static final int[] DZ = {-1,-1,0,1,1,1,0,-1};
    public record Tile(int x, int z, int trace, int tier) {}
    private static final List<List<Tile>> RINGS = buildRings();

    public static int pair(int first, int second) {
        if (first == second) throw new IllegalArgumentException("Distinct ports required");
        int a = Math.min(first, second), b = Math.max(first, second), id = 1;
        for (int i=0;i<8;i++) for (int j=i+1;j<8;j++,id++) if(i==a&&j==b)return id;
        throw new IllegalArgumentException("Invalid ports");
    }
    public static int[] ports(int trace) {
        int id=1;
        for(int a=0;a<8;a++)for(int b=a+1;b<8;b++,id++)if(id==trace)return new int[]{a,b};
        return new int[0];
    }
    public static int transform(int trace, int quarterTurns, boolean mirrorX, boolean mirrorZ) {
        int[] ports=ports(trace);
        if(ports.length==0)return 0;
        for(int i=0;i<2;i++) {
            int p=ports[i];
            if(mirrorX)p=(8-p)%8;
            if(mirrorZ)p=(4-p+8)%8;
            ports[i]=(p+quarterTurns*2+8)%8;
        }
        return pair(ports[0],ports[1]);
    }
    public static List<Tile> ring(int tier, boolean outer) {
        List<Tile> tiles = new ArrayList<>();
        for (int ringTier = 1; ringTier <= tier; ringTier++) tiles.addAll(ringStage(ringTier, outer));
        return List.copyOf(tiles);
    }

    public static List<Tile> ringStage(int tier, boolean outer) { return RINGS.get((tier-1)*2+(outer?0:1)); }
    private static List<List<Tile>> buildRings() {
        List<List<Tile>> rings=new ArrayList<>();
        for(int tier=1;tier<=3;tier++)for(boolean outer:new boolean[]{true,false}) {
            int r=FormationLayout.outerRadius(tier);
            List<int[]> cells=new ArrayList<>();
            for(int x=-r;x<=r;x++)for(int z=-r;z<=r;z++) {
                int ringTier = outer ? FormationLayout.baseRingTierAt(tier,x,z) : FormationLayout.runeRingTierAt(tier,x,z);
                if(ringTier == tier) cells.add(new int[]{x,z});
            }
            cells.sort(Comparator.comparingDouble(p->Math.atan2(p[1],p[0])));
            List<Tile> ring=new ArrayList<>();
            for(int i=0;i<cells.size();i++) {
                int[] p=cells.get(i), prev=cells.get((i+cells.size()-1)%cells.size()), next=cells.get((i+1)%cells.size());
                ring.add(new Tile(p[0],p[1],pair(direction(prev[0]-p[0],prev[1]-p[1]), direction(next[0]-p[0],next[1]-p[1])),tier));
            }
            rings.add(List.copyOf(ring));
        }
        return List.copyOf(rings);
    }
    private static int direction(int x,int z) {
        for(int i=0;i<8;i++)if(DX[i]==x&&DZ[i]==z)return i;
        throw new IllegalStateException("Nonadjacent ring cells: "+x+","+z);
    }
}
