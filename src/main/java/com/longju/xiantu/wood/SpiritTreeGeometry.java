package com.longju.xiantu.wood;

import java.util.*;

/** Pure voxel geometry shared by saplings, worldgen and previews. No world mutation here. */
public final class SpiritTreeGeometry {
    public record Cell(int x, int y, int z) {
        public Cell add(int dx, int dy, int dz) { return new Cell(x + dx, y + dy, z + dz); }
    }
    private static final int[][] NEIGHBORS = {{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
    private final Map<Cell, Boolean> cells = new LinkedHashMap<>();
    private final long seed;
    private SpiritTreeGeometry(long seed) { this.seed = seed; }
    private void log(int x, int y, int z) { cells.put(new Cell(x,y,z), true); }
    private void stem(int x,int z,int bottom,int top) { for(int y=bottom;y<=top;y++)log(x,y,z); }

    /** Stair-step limbs include every face-adjacent bridge, including ascending forks. */
    private void limb(int x,int y,int z,int tx,int ty,int tz) {
        int steps=Math.max(Math.abs(tx-x),Math.max(Math.abs(ty-y),Math.abs(tz-z)));
        int ox=x,oy=y,oz=z;log(x,y,z);
        for(int i=1;i<=steps;i++) {
            int nx=x+Math.round((tx-x)*i/(float)steps);
            int ny=y+Math.round((ty-y)*i/(float)steps);
            int nz=z+Math.round((tz-z)*i/(float)steps);
            while(ox!=nx){ox+=Integer.signum(nx-ox);log(ox,oy,oz);}
            while(oz!=nz){oz+=Integer.signum(nz-oz);log(ox,oy,oz);}
            while(oy!=ny){oy+=Integer.signum(ny-oy);log(ox,oy,oz);}
        }
    }
    private void crown(int cx,int cy,int cz,int rx,int ry,int rz) {
        for(int x=-rx;x<=rx;x++)for(int y=-ry;y<=ry;y++)for(int z=-rz;z<=rz;z++) {
            double d=x*x/Math.pow(rx+.35,2)+y*y/Math.pow(ry+.6,2)+z*z/Math.pow(rz+.35,2);
            if(d>1.12)continue;
            // Only soften the outer silhouette; keep the inner crown coherent and supported.
            if(d>.92 && (mix(seed+(cx+x)*734287L+(cy+y)*9127L+(cz+z)*438289L)&7)==0)continue;
            cells.putIfAbsent(new Cell(cx+x,cy+y,cz+z),false);
        }
    }
    private void whorl(int y,int radius) {
        crown(0,y,0,radius,0,radius);
        crown(0,y+1,0,Math.max(0,radius-1),0,Math.max(0,radius-1));
        for(int[] d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}})
            limb(0,y,0,d[0]*Math.max(0,radius-1),y,d[1]*Math.max(0,radius-1));
    }
    public static long positionSeed(int x,int y,int z) {
        return mix((long)x*341873128712L+(long)z*132897987541L+y*42317861L);
    }
    private static long mix(long v) {
        v=(v^(v>>>30))*0xbf58476d1ce4e5b9L;
        v=(v^(v>>>27))*0x94d049bb133111ebL;
        return v^(v>>>31);
    }
    public static Map<Cell,Boolean> layout(int rank,long seed) {
        if(rank<1||rank>8)throw new IllegalArgumentException("Spirit wood rank must be 1..8");
        SpiritTreeGeometry t=new SpiritTreeGeometry(seed);
        int v=(int)(mix(seed)&1),h;
        switch(rank) {
            case 1 -> { // Elm: short visible trunk under three overlapping umbrella lobes.
                h=4+v;t.stem(0,0,0,h);
                t.limb(0,h-1,0,-2,h,1);t.limb(0,h-1,0,2,h+1,-1);t.limb(0,h-1,0,0,h,2);
                t.crown(0,h+1,0,3,1,3);t.crown(-2,h,1,2,1,2);t.crown(2,h+1,-1,2,1,2);
            }
            case 2 -> { // Birch: tall single stem, separated small tufts, asymmetric shoulder.
                h=8+v;t.stem(0,0,0,h);
                t.limb(0,h-4,0,1,h-3,0);t.limb(0,h-2,0,-1,h-1,0);
                t.crown(1,h-3,0,1,1,1);t.crown(-1,h-1,0,2,1,2);t.crown(0,h+1,0,1,1,1);
            }
            case 3 -> { // Maple: low rising forks, unequal rounded lobes rather than a flat slab.
                t.stem(0,0,0,4);
                t.limb(0,2,0,-3,5,0);t.limb(0,3,0,2,6+v,1);t.limb(0,3,0,0,4,-2);
                t.crown(-3,5,0,2,1,3);t.crown(2,6+v,1,3,1,2);
                t.crown(0,5,-2,2,1,2);t.crown(-1,6,0,2,1,2);
            }
            case 4 -> { // Pine: horizontal boughs with visible gaps between layered needle skirts.
                t.stem(0,0,0,10);t.whorl(4,3);t.whorl(7,3);t.whorl(9,2);
                t.crown(0,11,0,1,1,1);t.limb(0,6,0,1,6,-1);
            }
            case 5 -> { // Nanmu: clear high bole and a full, irregular broad round crown.
                h=8+v;t.stem(0,0,0,h);
                t.limb(0,5,0,-3,7,-1);t.limb(0,6,0,3,h,1);
                t.limb(0,6,0,0,8,-3);t.limb(0,6,0,-1,8,3);
                t.crown(0,h+1,0,3,2,3);t.crown(-3,8,-1,2,1,2);
                t.crown(3,h,1,2,1,2);t.crown(0,8,-3,2,1,2);t.crown(-1,8,3,2,1,2);
            }
            case 6 -> { // Cedar: narrow tapering spire; never a broad recolored pine.
                t.stem(0,0,0,12);
                t.whorl(4,2);t.whorl(6,2);t.whorl(8,2);t.whorl(10,1);
                t.crown(0,12,0,1,0,1);t.crown(0,13,0,0,0,0);
            }
            case 7 -> { // Wutong: two tall rising forks spread a wide, shallow fan.
                t.stem(0,0,0,6);t.limb(0,5,0,-3,9,0);t.limb(0,6,0,3,10,0);
                t.limb(0,6,0,0,10,-1);t.limb(-3,8,0,-4,10,1);
                t.crown(-3,10,0,3,1,2);t.crown(3,11,0,3,1,2);
                t.crown(0,11,-1,2,1,2);t.crown(1,12,0,1,1,1);
            }
            case 8 -> { // Sandalwood: rooted bole, climbing limbs and offset cloud-like crowns.
                t.stem(0,0,0,8);t.stem(1,0,0,3);t.stem(0,1,0,2);
                t.limb(0,0,0,-1,0,0);t.limb(0,0,0,0,0,-1);
                t.limb(0,4,0,-3,7,0);t.limb(0,6,0,3,9,1);t.limb(0,7,0,0,11,-3);
                t.limb(-3,7,0,-4,8,-1);t.limb(3,9,1,4,10,1);
                t.limb(0,6,0,-1,8,3);
                t.crown(-3,8,-1,3,1,3);t.crown(3,10,1,3,1,3);
                t.crown(0,12,-3,2,1,2);t.crown(-1,9,3,2,1,2);
            }
        }
        int turn=(int)(mix(seed+97)&3);
        Map<Cell,Boolean> rotated=new LinkedHashMap<>();
        t.cells.forEach((p,wood)->{
            int x=p.x(),z=p.z();for(int i=0;i<turn;i++){int n=-z;z=x;x=n;}
            rotated.put(new Cell(x,p.y(),z),wood);
        });
        // Actual six-neighbor paths, not Manhattan distance through empty air.
        Map<Cell,Integer> distances=leafDistances(rotated);
        rotated.entrySet().removeIf(e->!e.getValue()&&!distances.containsKey(e.getKey()));
        return Collections.unmodifiableMap(rotated);
    }
    public static Map<Cell,Integer> leafDistances(Map<Cell,Boolean> layout) {
        Map<Cell,Integer> result=new HashMap<>();ArrayDeque<Cell> queue=new ArrayDeque<>();
        layout.forEach((p,wood)->{if(wood){result.put(p,0);queue.add(p);}});
        while(!queue.isEmpty()) {
            Cell p=queue.removeFirst();int next=result.get(p)+1;if(next>6)continue;
            for(int[] d:NEIGHBORS) {
                Cell n=p.add(d[0],d[1],d[2]);
                if(Boolean.FALSE.equals(layout.get(n))&&!result.containsKey(n)){result.put(n,next);queue.addLast(n);}
            }
        }
        return result;
    }
}
