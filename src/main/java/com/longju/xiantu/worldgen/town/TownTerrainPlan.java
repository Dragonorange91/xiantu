package com.longju.xiantu.worldgen.town;

import java.util.*;

/** Deterministic terrain fit without reading or generating neighboring chunks. Heights are surface blocks. */
public final class TownTerrainPlan {
    public static final int MARGIN=8, WIDTH=TownLayout.WIDTH, DEPTH=TownLayout.DEPTH, PLAN_WIDTH=WIDTH+MARGIN*2, PLAN_DEPTH=DEPTH+MARGIN*2;
    public record Plot(int x0,int z0,int x1,int z1) {
        public int distance(int x,int z){return Math.max(Math.max(x0-x,x-x1),0)+Math.max(Math.max(z0-z,z-z1),0);}
        boolean overlaps(Plot b){return x0<=b.x1&&x1>=b.x0&&z0<=b.z1&&z1>=b.z0;}
    }
    public interface Surface {
        int height(int x,int z);
        default int waterDepth(int x,int z,int seaLevel){return Math.max(0,seaLevel-1-height(x,z));}
    }
    public static final List<Plot> PLOTS=plots();
    private final int[] heights;
    TownTerrainPlan(int[] heights){this.heights=heights;}
    private static List<Plot> plots(){
        return List.of(
            new Plot(59,53,85,79),
            new Plot(12,12,52,46),
            new Plot(92,12,132,46),
            new Plot(11,86,53,120),
            new Plot(93,86,133,120),
            new Plot(54,18,68,34),
            new Plot(76,18,90,34),
            new Plot(54,98,68,114),
            new Plot(76,98,90,114),
            new Plot(12,48,28,62),
            new Plot(116,48,132,62),
            new Plot(12,70,28,84),
            new Plot(116,70,132,84),
            new Plot(4,4,9,9),
            new Plot(135,4,140,9),
            new Plot(4,123,9,128),
            new Plot(135,123,140,128),
            new Plot(61,0,83,16),
            new Plot(61,116,83,132),
            new Plot(40,81,46,85),
            new Plot(98,81,104,85),
            new Plot(53,42,57,46),
            new Plot(87,42,91,46),
            new Plot(53,87,57,91),
            new Plot(87,87,91,91),
            new Plot(40,59,44,63),
            new Plot(100,59,104,63));
    }
    public static Optional<TownTerrainPlan> create(Surface source,int seaLevel,int maxBuildHeight){
        return create(source,seaLevel,maxBuildHeight,reason->{});
    }
    public static Optional<TownTerrainPlan> create(Surface source,int seaLevel,int maxBuildHeight,java.util.function.Consumer<String> rejected){
        return TownTerracePlanner.create(source,seaLevel,maxBuildHeight,rejected);
    }
    public static Optional<TownTerrainPlan> create(Surface source,int seaLevel,int maxBuildHeight,int totalSpan,int stepSpan,int plotSpan,int earthwork,java.util.function.Consumer<String> rejected){
        // 8-block lattice, at most 600 base-noise columns per candidate, never force-load chunks.
        int cols=(PLAN_WIDTH-1+7)/8+1,rows=(PLAN_DEPTH-1+7)/8+1;
        int[][] sample=new int[cols][rows];int min=Integer.MAX_VALUE,max=Integer.MIN_VALUE;
        for(int x=0;x<cols;x++)for(int z=0;z<rows;z++){
            int h=source.height(x*8-MARGIN,z*8-MARGIN);sample[x][z]=h;min=Math.min(min,h);max=Math.max(max,h);
            if(h<seaLevel){rejected.accept("water");return Optional.empty();}
            if(h+40>=maxBuildHeight){rejected.accept("ceiling");return Optional.empty();}
            if(max-min>totalSpan){rejected.accept("total_span");return Optional.empty();}
            if(x>0&&Math.abs(h-sample[x-1][z])>stepSpan||z>0&&Math.abs(h-sample[x][z-1])>stepSpan){rejected.accept("slope");return Optional.empty();}
        }
        int[] natural=new int[PLAN_WIDTH*PLAN_DEPTH];
        for(int x=0;x<PLAN_WIDTH;x++)for(int z=0;z<PLAN_DEPTH;z++){
            int a=x/8,b=z/8;double fx=x%8/8.0,fz=z%8/8.0;
            natural[z*PLAN_WIDTH+x]=(int)Math.round((sample[a][b]*(1-fx)+sample[Math.min(a+1,cols-1)][b]*fx)*(1-fz)
                +(sample[a][Math.min(b+1,rows-1)]*(1-fx)+sample[Math.min(a+1,cols-1)][Math.min(b+1,rows-1)]*fx)*fz);
        }
        // Overlapping roof/tower plots share one elevation; never shear an accepted building.
        int n=PLOTS.size();int[] group=new int[n];for(int i=0;i<n;i++)group[i]=i;
        for(int i=0;i<n;i++)for(int j=0;j<i;j++)if(PLOTS.get(i).overlaps(PLOTS.get(j))){int from=group[i],to=group[j];for(int k=0;k<n;k++)if(group[k]==from)group[k]=to;}
        int[] floors=new int[n];
        for(int i=0;i<n;i++){
            var values=new ArrayList<Integer>();
            for(int j=0;j<n;j++)if(group[j]==group[i]){var p=PLOTS.get(j);for(int x=p.x0;x<=p.x1;x+=4)for(int z=p.z0;z<=p.z1;z+=4)values.add(natural[(z+MARGIN)*PLAN_WIDTH+x+MARGIN]);}
            Collections.sort(values);floors[i]=values.get(values.size()/2);
            if(values.get(values.size()-1)-values.get(0)>plotSpan){rejected.accept("plot_span");return Optional.empty();}
        }
        int[] result=natural.clone();
        for(int x=-MARGIN;x<WIDTH+MARGIN;x++)for(int z=-MARGIN;z<DEPTH+MARGIN;z++){
            int lower=Integer.MIN_VALUE,upper=Integer.MAX_VALUE;
            for(int i=0;i<n;i++){int d=PLOTS.get(i).distance(x,z);lower=Math.max(lower,floors[i]-d);upper=Math.min(upper,floors[i]+d);}
            if(lower>upper){rejected.accept("plot_conflict");return Optional.empty();}
            int index=(z+MARGIN)*PLAN_WIDTH+x+MARGIN;
            result[index]=Math.max(lower,Math.min(upper,natural[index]));
            if(Math.abs(result[index]-natural[index])>earthwork){rejected.accept("earthwork");return Optional.empty();}
        }
        // Brackets and lamps on a rampart share the wall's cross-section height.
        for(int x=0;x<WIDTH;x++)for(int z=0;z<DEPTH;z++){
            int a=x<9?5:x>WIDTH-10?WIDTH-6:x,b=z<9?5:z>DEPTH-10?DEPTH-6:z;
            boolean protectedPlot=false;for(var p:PLOTS)if(p.distance(x,z)==0){protectedPlot=true;break;}
            if(!protectedPlot)result[(z+MARGIN)*PLAN_WIDTH+x+MARGIN]=result[(b+MARGIN)*PLAN_WIDTH+a+MARGIN];
        }
        return Optional.of(new TownTerrainPlan(result));
    }
    public int height(int x,int z){return heights[(z+MARGIN)*PLAN_WIDTH+x+MARGIN];}
    public boolean isPlot(int x,int z){for(var p:PLOTS)if(p.distance(x,z)==0)return true;return false;}
    public int minimum(){return Arrays.stream(heights).min().orElseThrow();}
    public int maximum(){return Arrays.stream(heights).max().orElseThrow();}
}
