package com.longju.xiantu.instance;

import com.google.gson.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import java.util.*;

/** Geometry and gameplay share one immutable blueprint, including vertical destinations. */
public record RealmLayout(String title,List<Cell> blocks,List<Zone> zones,List<List<Integer>> routes,
                          List<List<Integer>> footprints,List<BlockPos> ores,List<BlockPos> herbs,Map<String,BlockPos> rewards) {
    public record Cell(BlockPos at,String state){}
    public record Zone(String id,int x,int y,int z,int risk){public BlockPos at(){return new BlockPos(x,y,z);}}
    private static final Map<MinecraftServer,Map<String,RealmLayout>> CACHE=new WeakHashMap<>();
    public static RealmLayout get(MinecraftServer server,String theme){
        if(!server.isSameThread())throw new IllegalStateException("Load realm blueprints on server thread");
        var cached=CACHE.get(server);if(cached==null){
            cached=new HashMap<>();
            try(var in=server.getResourceManager().getResource(new ResourceLocation("xiantu:instance/realm_courts.json")).orElseThrow().openAsReader()){
                var root=JsonParser.parseReader(in).getAsJsonObject();if(root.get("schema_version").getAsInt()!=3)throw new IllegalStateException("Expected independent realm layouts");
                for(var e:root.getAsJsonObject("layouts").entrySet()){
                    var o=e.getValue().getAsJsonObject();var cells=new ArrayList<Cell>();var zones=new ArrayList<Zone>();var rewards=new HashMap<String,BlockPos>();
                    for(var v:o.getAsJsonArray("blocks")){var a=v.getAsJsonArray();cells.add(new Cell(pos(a),a.get(3).getAsString()));}
                    for(var v:o.getAsJsonArray("zones")){var z=v.getAsJsonObject();zones.add(new Zone(z.get("id").getAsString(),z.get("x").getAsInt(),z.get("y").getAsInt(),z.get("z").getAsInt(),z.get("risk").getAsInt()));}
                    o.getAsJsonObject("rewards").entrySet().forEach(v->rewards.put(v.getKey(),pos(v.getValue().getAsJsonArray())));
                    cached.put(e.getKey(),new RealmLayout(o.get("title").getAsString(),List.copyOf(cells),List.copyOf(zones),arrays(o,"routes"),arrays(o,"footprints"),positions(o,"ore_cells"),positions(o,"herb_cells"),Map.copyOf(rewards)));
                }
            }catch(java.io.IOException ex){throw new IllegalStateException("Cannot load realm layout",ex);}CACHE.put(server,Map.copyOf(cached));
        }var found=cached.get(theme);if(found==null)throw new IllegalArgumentException("Unknown realm theme "+theme);return found;
    }
    private static BlockPos pos(JsonArray a){return new BlockPos(a.get(0).getAsInt(),a.get(1).getAsInt(),a.get(2).getAsInt());}
    private static List<List<Integer>> arrays(JsonObject o,String key){var out=new ArrayList<List<Integer>>();for(var v:o.getAsJsonArray(key)){var a=new ArrayList<Integer>();v.getAsJsonArray().forEach(i->a.add(i.getAsInt()));out.add(List.copyOf(a));}return List.copyOf(out);}
    private static List<BlockPos> positions(JsonObject o,String key){var out=new ArrayList<BlockPos>();o.getAsJsonArray(key).forEach(v->out.add(pos(v.getAsJsonArray())));return List.copyOf(out);}
    public Zone zone(String id){return zones.stream().filter(z->z.id().equals(id)).findFirst().orElseThrow();}
    public boolean protectedColumn(int x,int z){return footprints.stream().anyMatch(f->Math.abs(x-f.get(0))<=f.get(3)+3&&Math.abs(z-f.get(2))<=f.get(4)+3)||routes.stream().anyMatch(r->distance(x,z,r)<7);}
    public int surface(int x,int z,int original){
        double best=0,target=original;
        for(var f:footprints){double d=Math.max(Math.abs(x-f.get(0))-f.get(3),Math.abs(z-f.get(2))-f.get(4));double weight=1-Math.max(0,Math.min(1,d/20));if(weight>best){best=weight;target=f.get(1);}}
        for(var r:routes){double t=projection(x,z,r),d=distance(x,z,r),weight=1-Math.max(0,Math.min(1,(d-4)/14));if(weight>best){best=weight;target=r.get(1)+(r.get(4)-r.get(1))*t;}}
        return (int)Math.round(original+(target-original)*best*best*(3-2*best));
    }
    private static double projection(int x,int z,List<Integer> r){double dx=r.get(3)-r.get(0),dz=r.get(5)-r.get(2);return Math.max(0,Math.min(1,((x-r.get(0))*dx+(z-r.get(2))*dz)/(dx*dx+dz*dz)));}
    private static double distance(int x,int z,List<Integer> r){double t=projection(x,z,r);return Math.hypot(x-r.get(0)-(r.get(3)-r.get(0))*t,z-r.get(2)-(r.get(5)-r.get(2))*t);}
    public static void clear(){CACHE.clear();}
}
