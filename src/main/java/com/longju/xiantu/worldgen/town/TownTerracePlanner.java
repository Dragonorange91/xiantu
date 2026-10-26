package com.longju.xiantu.worldgen.town;

import java.util.*;
import java.util.function.Consumer;
import static com.longju.xiantu.worldgen.town.TownTerrainPlan.*;

/** Constrained building terraces. All calculations are local, deterministic and chunk-independent. */
final class TownTerracePlanner {
    private static final int W=PLAN_WIDTH,D=PLAN_DEPTH,INF=100000,EARTHWORK=8;
    private static Optional<TownTerrainPlan> reject(Consumer<String> why,String reason){why.accept(reason);return Optional.empty();}
    static Optional<TownTerrainPlan> create(Surface source,int sea,int ceiling,Consumer<String> why){
        int cols=(W-1+7)/8+1,rows=(D-1+7)/8+1;
        int[][] raw=new int[cols][rows],wet=new int[cols][rows],smooth=new int[cols][rows];
        int water=0,deep=0;
        for(int x=0;x<cols;x++)for(int z=0;z<rows;z++){
            raw[x][z]=source.height(x*8-MARGIN,z*8-MARGIN);
            wet[x][z]=source.waterDepth(x*8-MARGIN,z*8-MARGIN,sea);
            if(wet[x][z]>0)water++;
            if(wet[x][z]>3)deep++;
            // A few shallow edge pools are fillable; oceans/rivers are not town foundations.
            if(deep>0)return reject(why,"deep_water");
            if(water>cols*rows*.08)return reject(why,"water_area");
            if(raw[x][z]+40>=ceiling)return reject(why,"ceiling");
        }
        for(int x=0;x<cols;x++)for(int z=0;z<rows;z++){
            int[] neighborhood=new int[9];int k=0;
            for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)neighborhood[k++]=raw[Math.max(0,Math.min(cols-1,x+dx))][Math.max(0,Math.min(rows-1,z+dz))];
            Arrays.sort(neighborhood);smooth[x][z]=neighborhood[4];
        }
        int[] natural=interpolate(smooth),actual=interpolate(raw);
        int n=PLOTS.size();int[] lo=new int[n],hi=new int[n],preferred=new int[n];int[][] distance=new int[n][n];
        for(int i=0;i<n;i++){
            var p=PLOTS.get(i);int min=INF,max=-INF;var values=new ArrayList<Integer>();
            for(int x=p.x0();x<=p.x1();x++)for(int z=p.z0();z<=p.z1();z++){
                int h=natural[index(x,z)];values.add(h);min=Math.min(min,h);max=Math.max(max,h);
            }
            Collections.sort(values);preferred[i]=values.get(values.size()/2);
            lo[i]=max-EARTHWORK;hi[i]=min+EARTHWORK;
            if(lo[i]>hi[i])return reject(why,"building_earthwork");
            // The plaza and buildings need dry ground, not a submerged median hidden by smoothing.
            if(preferred[i]<sea)return reject(why,"submerged_plot");
            for(int j=0;j<i;j++){
                var q=PLOTS.get(j);
                distance[i][j]=distance[j][i]=Math.max(0,Math.max(p.x0()-q.x1(),q.x0()-p.x1()))+Math.max(0,Math.max(p.z0()-q.z1(),q.z0()-p.z1()));
            }
        }
        // Buildings face each other across the two seven-wide main avenues. Keep their thresholds
        // close enough that a continuous, level-width road can meet both sides without a central spine.
        for(int[] pair:new int[][]{{5,6},{7,8},{9,11},{10,12}})
            distance[pair[0]][pair[1]]=distance[pair[1]][pair[0]]=Math.min(2,distance[pair[0]][pair[1]]);
        // Propagate floor intervals first; never independently choose incompatible adjacent floor heights.
        if(!propagate(lo,hi,distance))return reject(why,"plot_conflict");
        for(int i=0;i<n;i++){
            int floor=Math.max(lo[i],Math.min(hi[i],preferred[i]));lo[i]=hi[i]=floor;
            if(!propagate(lo,hi,distance))return reject(why,"plot_conflict");
        }
        int[] sorted=lo.clone();Arrays.sort(sorted);
        if(sorted[n-1]-sorted[0]>24)return reject(why,"terrace_span");
        int[] upper=new int[W*D],negLower=new int[W*D];Arrays.fill(upper,INF);Arrays.fill(negLower,INF);
        for(int i=0;i<n;i++){
            var p=PLOTS.get(i);
            for(int x=p.x0();x<=p.x1();x++)for(int z=p.z0();z<=p.z1();z++){
                int at=index(x,z);upper[at]=Math.min(upper[at],lo[i]);negLower[at]=Math.min(negLower[at],-lo[i]);
            }
        }
        int[] fixedUpper=upper.clone(),fixedNegLower=negLower.clone();
        distanceTransform(upper);distanceTransform(negLower);
        int[] lowSurface=natural.clone(),highSurface=natural.clone();for(int i=0;i<highSurface.length;i++)highSurface[i]=-highSurface[i];
        distanceTransform(lowSurface);distanceTransform(highSurface);
        int[] result=new int[W*D];
        for(int i=0;i<result.length;i++){
            if(-negLower[i]>upper[i])return reject(why,"plot_conflict");
            int h=Math.floorDiv(lowSurface[i]-highSurface[i],2);
            result[i]=Math.max(-negLower[i],Math.min(upper[i],h));
        }
        // Fit complete wall cross-sections to building intervals, then re-project courtyard slopes.
        // Copying a center-line height directly would otherwise create a four-block jump at the inner edge.
        for(int side=0;side<4;side++)if(!wall(side,lo,result,fixedUpper,fixedNegLower))return reject(why,"wall_conflict");
        distanceTransform(fixedUpper);distanceTransform(fixedNegLower);
        // Corners and complete road widths are constraints, not independently draped columns.
        for(var landing:TownRoads.LANDINGS){
            int low=-INF,high=INF;
            for(int x=landing.x0();x<=landing.x1();x++)for(int z=landing.z0();z<=landing.z1();z++){
                int i=index(x,z);low=Math.max(low,-fixedNegLower[i]);high=Math.min(high,fixedUpper[i]);
            }
            if(low>high)return reject(why,"road_landing");
            int h=Math.max(low,Math.min(high,result[index((landing.x0()+landing.x1())/2,(landing.z0()+landing.z1())/2)]));
            fix(landing,h,fixedUpper,fixedNegLower);
            distanceTransform(fixedUpper);distanceTransform(fixedNegLower);
        }
        for(var road:TownRoads.ALL){
            if(!road(road,result,fixedUpper,fixedNegLower))return reject(why,"road_grade");
            distanceTransform(fixedUpper);distanceTransform(fixedNegLower);
        }
        for(int i=0;i<result.length;i++){
            if(-fixedNegLower[i]>fixedUpper[i])return reject(why,"wall_conflict");
            result[i]=Math.max(-fixedNegLower[i],Math.min(fixedUpper[i],result[i]));
        }
        long totalWork=0;int exceptionalFill=0;
        for(int x=-MARGIN;x<WIDTH+MARGIN;x++)for(int z=-MARGIN;z<DEPTH+MARGIN;z++){
            int i=index(x,z),delta=result[i]-actual[i];totalWork+=Math.abs(delta);
            if(delta<-12)return reject(why,"excavation");
            if(delta>96)return reject(why,"foundation_depth");
            if(delta>EARTHWORK)exceptionalFill++;
            if(x<WIDTH+MARGIN-1&&Math.abs(result[i]-result[i+1])>1||z<DEPTH+MARGIN-1&&Math.abs(result[i]-result[i+W])>1)return reject(why,"wall_grade");
        }
        if(totalWork>result.length*3.5)return reject(why,"earthwork_volume");
        // Deep dry fissures may receive solid piers; they must occupy less than 2% of the footprint.
        if(exceptionalFill>result.length*.02)return reject(why,"foundation_area");
        return Optional.of(new TownTerrainPlan(result));
    }
    private static int index(int x,int z){return (z+MARGIN)*W+x+MARGIN;}
    private static void fix(Plot p,int h,int[] upper,int[] negLower){
        for(int x=p.x0();x<=p.x1();x++)for(int z=p.z0();z<=p.z1();z++){
            int i=index(x,z);upper[i]=Math.min(upper[i],h);negLower[i]=Math.min(negLower[i],-h);
        }
    }
    private static boolean road(TownRoads.Road road,int[] preferred,int[] upper,int[] negLower){
        int length=road.end()-road.start()+1;int[] low=new int[length],high=new int[length];
        for(int t=0;t<length;t++){
            low[t]=-INF;high[t]=INF;var p=road.section(t+road.start());
            for(int x=p.x0();x<=p.x1();x++)for(int z=p.z0();z<=p.z1();z++){
                int i=index(x,z);low[t]=Math.max(low[t],-negLower[i]);high[t]=Math.min(high[t],upper[i]);
            }
        }
        for(int t=1;t<length;t++){low[t]=Math.max(low[t],low[t-1]-1);high[t]=Math.min(high[t],high[t-1]+1);}
        for(int t=length-2;t>=0;t--){low[t]=Math.max(low[t],low[t+1]-1);high[t]=Math.min(high[t],high[t+1]+1);}
        int previous=0;
        for(int t=0;t<length;t++){
            if(t>0){low[t]=Math.max(low[t],previous-1);high[t]=Math.min(high[t],previous+1);}
            if(low[t]>high[t])return false;
            var p=road.section(t+road.start());
            int target=preferred[index((p.x0()+p.x1())/2,(p.z0()+p.z1())/2)];
            previous=Math.max(low[t],Math.min(high[t],target));fix(p,previous,upper,negLower);
        }
        return true;
    }
    private static int distance(Plot a,Plot b){return Math.max(0,Math.max(a.x0()-b.x1(),b.x0()-a.x1()))+Math.max(0,Math.max(a.z0()-b.z1(),b.z0()-a.z1()));}
    private static boolean gate(int side,int t){return side<2?t>=61&&t<=83:t>=61&&t<=71;}
    private static boolean wall(int side,int[] floors,int[] preferred,int[] fixedUpper,int[] fixedNegLower){
        boolean horizontal=side<2;int length=horizontal?WIDTH:DEPTH;
        int[] low=new int[length],high=new int[length],chosen=new int[length];
        Plot[] strips=new Plot[length];
        for(int t=0;t<length;t++){
            Plot strip=side==0?new Plot(t,0,t,8):side==1?new Plot(t,DEPTH-9,t,DEPTH-1):side==2?new Plot(0,t,8,t):new Plot(WIDTH-9,t,WIDTH-1,t);strips[t]=strip;
            low[t]=-INF;high[t]=INF;
            for(int j=0;j<PLOTS.size();j++){int d=distance(strip,PLOTS.get(j));low[t]=Math.max(low[t],floors[j]-d);high[t]=Math.min(high[t],floors[j]+d);}
            int corner=side==0?(t<10?13:14):side==1?(t<10?15:16):side==2?(t<10?13:15):(t<10?14:16);
            if(t<10||t>=length-10){low[t]=Math.max(low[t],floors[corner]);high[t]=Math.min(high[t],floors[corner]);}
        }
        // Open gate lanes are solved with the roads, rather than copying a wall center height into them.
        for(int t=1;t<length;t++)if(!gate(side,t)&&!gate(side,t-1)){low[t]=Math.max(low[t],low[t-1]-1);high[t]=Math.min(high[t],high[t-1]+1);}
        for(int t=length-2;t>=0;t--)if(!gate(side,t)&&!gate(side,t+1)){low[t]=Math.max(low[t],low[t+1]-1);high[t]=Math.min(high[t],high[t+1]+1);}
        for(int t=0;t<length;t++){
            if(gate(side,t))continue;
            if(t>0&&!gate(side,t-1)){low[t]=Math.max(low[t],chosen[t-1]-1);high[t]=Math.min(high[t],chosen[t-1]+1);}
            if(low[t]>high[t])return false;
            var strip=strips[t];int target=preferred[index((strip.x0()+strip.x1())/2,(strip.z0()+strip.z1())/2)];
            int h=chosen[t]=Math.max(low[t],Math.min(high[t],target));fix(strip,h,fixedUpper,fixedNegLower);
        }
        return true;
    }
    private static boolean propagate(int[] lo,int[] hi,int[][] d){
        boolean changed=true;int guard=0;
        while(changed&&guard++<=lo.length*lo.length){
            changed=false;
            for(int i=0;i<lo.length;i++)for(int j=0;j<lo.length;j++){
                int a=Math.max(lo[i],lo[j]-d[i][j]),b=Math.min(hi[i],hi[j]+d[i][j]);
                if(a>b)return false;if(a!=lo[i]||b!=hi[i]){lo[i]=a;hi[i]=b;changed=true;}
            }
        }
        return !changed;
    }
    private static int[] interpolate(int[][] grid){
        int[] out=new int[W*D];
        for(int x=0;x<W;x++)for(int z=0;z<D;z++){
            int a=x/8,b=z/8,an=Math.min(grid.length-1,a+1),bn=Math.min(grid[0].length-1,b+1);double fx=x%8/8.0,fz=z%8/8.0;
            out[z*W+x]=(int)Math.round((grid[a][b]*(1-fx)+grid[an][b]*fx)*(1-fz)+(grid[a][bn]*(1-fx)+grid[an][bn]*fx)*fz);
        }
        return out;
    }
    /** Exact Manhattan min-plus transform, unit slope in both directions. */
    private static void distanceTransform(int[] a){
        for(int z=0;z<D;z++)for(int x=0;x<W;x++){int i=z*W+x;if(x>0)a[i]=Math.min(a[i],a[i-1]+1);if(z>0)a[i]=Math.min(a[i],a[i-W]+1);}
        for(int z=D-1;z>=0;z--)for(int x=W-1;x>=0;x--){int i=z*W+x;if(x<W-1)a[i]=Math.min(a[i],a[i+1]+1);if(z<D-1)a[i]=Math.min(a[i],a[i+W]+1);}
    }
}
