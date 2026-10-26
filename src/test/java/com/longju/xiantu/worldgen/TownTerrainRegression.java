package com.longju.xiantu.worldgen;

import com.longju.xiantu.worldgen.town.TownTerrainPlan;
import com.longju.xiantu.worldgen.town.TownRoads;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class TownTerrainRegression {
    private static int checks;
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);checks++;}
    private static void roads(TownTerrainPlan plan){
        for(var road:TownRoads.ALL)for(int t=road.start();t<=road.end();t++){
            var p=road.section(t);int h=plan.height(p.x0(),p.z0());
            for(int x=p.x0();x<=p.x1();x++)for(int z=p.z0();z<=p.z1();z++)check(plan.height(x,z)==h,"Road cross-section has no central ridge or sideways stair");
            if(t>road.start()){
                var prev=road.section(t-1);int before=plan.height(prev.x0(),prev.z0());
                check(Math.abs(h-before)<=1,"Road rise never skips a tread");
            }
        }
        for(var p:TownRoads.LANDINGS)for(int x=p.x0();x<=p.x1();x++)for(int z=p.z0();z<=p.z1();z++)check(plan.height(x,z)==plan.height(p.x0(),p.z0()),"Road turns have level landings");
    }
    public static void main(String[] args)throws Exception{
        var flat=TownTerrainPlan.create((x,z)->70,63,320).orElseThrow();
        check(flat.minimum()==70&&flat.maximum()==70,"Flat site remains flat");
        var slope=TownTerrainPlan.create((x,z)->70+(int)Math.floor(x/45.0+z/60.0),63,320).orElseThrow();
        check(slope.maximum()>slope.minimum(),"Natural slope survives: no city-wide flattening");
        roads(flat);roads(slope);
        for(var plot:TownTerrainPlan.PLOTS){int first=slope.height(plot.x0(),plot.z0());
            for(int x=plot.x0();x<=plot.x1();x++)for(int z=plot.z0();z<=plot.z1();z++)check(slope.height(x,z)==first,"Rigid building/tree/pond plot remains intact");
        }
        for(int x=-8;x<TownTerrainPlan.WIDTH+7;x++)for(int z=-8;z<TownTerrainPlan.DEPTH+7;z++){
            check(Math.abs(slope.height(x,z)-slope.height(x+1,z))<=1,"East-west walkable grade");
            check(Math.abs(slope.height(x,z)-slope.height(x,z+1))<=1,"North-south walkable grade");
        }
        check(TownTerrainPlan.create((x,z)->62,63,320).isEmpty(),"Ocean rejected");
        check(TownTerrainPlan.create((x,z)->x>60?95:64,63,320).isEmpty(),"Cliff rejected");
        check(TownTerrainPlan.create((x,z)->z==80?58:72,63,320).isEmpty(),"River crossing rejected");
        check(TownTerrainPlan.create((x,z)->300,63,320).isEmpty(),"Build ceiling rejected");
        var pond=TownTerrainPlan.create(new TownTerrainPlan.Surface(){
            public int height(int x,int z){return x>=96&&x<=104&&z>=64&&z<=72?61:64;}
            public int waterDepth(int x,int z,int sea){return height(x,z)==61?1:0;}
        },63,320);
        check(pond.isPresent(),"Small shallow pool can receive a foundation");
        var fissure=TownTerrainPlan.create(new TownTerrainPlan.Surface(){
            public int height(int x,int z){return x==72&&z==64?35:70;}
            public int waterDepth(int x,int z,int sea){return 0;}
        },63,320);
        check(fissure.isPresent(),"Small dry fissure is not mistaken for a river");
        try(var in=TownTerrainRegression.class.getResourceAsStream("/town_terrain_sites.json")){
            var captured=JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();
            for(var entry:captured.getAsJsonArray("terrainSamples")){
                var sample=entry.getAsJsonObject();var grid=sample.getAsJsonArray("heights");var wet=sample.getAsJsonArray("waterDepth");
                var plan=TownTerrainPlan.create(new TownTerrainPlan.Surface(){
                    public int height(int x,int z){return grid.get((x+8)/8).getAsJsonArray().get((z+8)/8).getAsInt();}
                    public int waterDepth(int x,int z,int sea){return wet.get((x+8)/8).getAsJsonArray().get((z+8)/8).getAsInt();}
                },63,320,why->System.out.println("Captured "+sample.get("chunk")+" rejected: "+why));
                check(plan.isPresent()==sample.get("expectedAccepted").getAsBoolean(),"Captured terrain decision "+sample.get("chunk"));
                if(plan.isEmpty())continue;
                var fit=plan.get();
                roads(fit);
                for(var plot:TownTerrainPlan.PLOTS){int h=fit.height(plot.x0(),plot.z0());
                    for(int x=plot.x0();x<=plot.x1();x++)for(int z=plot.z0();z<=plot.z1();z++)check(fit.height(x,z)==h,"Captured site preserves complete rigid building");
                }
                for(int x=-8;x<TownTerrainPlan.WIDTH+7;x++)for(int z=-8;z<TownTerrainPlan.DEPTH+7;z++){
                    check(Math.abs(fit.height(x,z)-fit.height(x+1,z))<=1,"Captured east-west grade");
                    check(Math.abs(fit.height(x,z)-fit.height(x,z+1))<=1,"Captured north-south grade");
                }
                var again=TownTerrainPlan.create(new TownTerrainPlan.Surface(){
                    public int height(int x,int z){return grid.get((x+8)/8).getAsJsonArray().get((z+8)/8).getAsInt();}
                    public int waterDepth(int x,int z,int sea){return wet.get((x+8)/8).getAsJsonArray().get((z+8)/8).getAsInt();}
                },63,320).orElseThrow();
                check(fit.height(72,66)==again.height(72,66),"Same seed terrain produces stable plan");
            }
        }
        String replay=System.getenv("XIANTU_TERRAIN_SAMPLES");
        if(replay!=null){
            var data=JsonParser.parseString(java.nio.file.Files.readString(java.nio.file.Path.of(replay))).getAsJsonObject();
            var accepted=new java.util.ArrayList<Object>();var rejected=new java.util.TreeMap<String,Integer>();
            for(var entry:data.getAsJsonArray("terrainSamples")){
                var sample=entry.getAsJsonObject();var grid=sample.getAsJsonArray("heights");var wet=sample.getAsJsonArray("waterDepth");
                TownTerrainPlan.Surface source=new TownTerrainPlan.Surface(){
                    public int height(int x,int z){return grid.get((x+8)/8).getAsJsonArray().get((z+8)/8).getAsInt();}
                    public int waterDepth(int x,int z,int sea){return wet==null?Math.max(0,sea-1-height(x,z)):wet.get((x+8)/8).getAsJsonArray().get((z+8)/8).getAsInt();}
                };
                var plan=TownTerrainPlan.create(source,63,320,why->rejected.merge(why,1,Integer::sum));
                if(plan.isPresent())accepted.add(java.util.Map.of("chunk",sample.get("chunk"),"biomeEligible",sample.get("biomeEligible"),"min",plan.get().minimum(),"max",plan.get().maximum()));
            }
            System.out.println("Terrain replay "+new com.google.gson.Gson().toJson(java.util.Map.of("accepted",accepted,"rejections",rejected)));
        }
        try(var stream=TownTerrainRegression.class.getResourceAsStream("/data/xiantu/worldgen/structure_set/cultivation_towns.json")){
            var config=JsonParser.parseReader(new InputStreamReader(stream,StandardCharsets.UTF_8)).getAsJsonObject();var placement=config.getAsJsonObject("placement");
            check(placement.get("spacing").getAsInt()==34,"Village-scale attempt density");
            check(placement.get("separation").getAsInt()*16>TownTerrainPlan.PLAN_WIDTH,"Town footprints cannot overlap each other");
            check((placement.get("separation").getAsInt()+1)*16-64>TownTerrainPlan.PLAN_WIDTH,"Alternate sites retain a gap between neighboring towns");
            check(TownTerrainPlan.WIDTH+8-64+32<=128&&64+8+32<=128,"Alternate pieces stay inside native structure reference radius");
            check(placement.get("salt").getAsInt()!=10387312,"Independent from village candidate grid");
            check(placement.getAsJsonObject("exclusion_zone").get("other_set").getAsString().equals("minecraft:villages"),"Avoid vanilla villages");
        }
        check(!java.nio.file.Files.exists(java.nio.file.Path.of("src/main/resources/data/minecraft/worldgen/structure_set/villages.json")),"Vanilla villages not overridden");
        System.out.println("Town terrain regression PASS: "+checks);
    }
}
