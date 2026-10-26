package com.longju.xiantu.qa;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Read-only, actual placed block collision checks in the isolated QA gallery. */
@Mod.EventBusSubscriber(modid="xiantu_qa")
public final class RuinPathAudit {
    private record Cell(int x,int halfY,int z) {}
    private static AABB body(Cell c,int ox,int oz) {
        double x=ox+c.x+.5,y=260+c.halfY*.5,z=oz+c.z+.5;
        return new AABB(x-.3,y+.001,z-.3,x+.3,y+1.8,z+.3);
    }
    private static boolean clear(ServerLevel level,Cell c,int ox,int oz) {
        return level.noCollision(body(c,ox,oz));
    }
    private static boolean stand(ServerLevel level,Cell c,int ox,int oz,int width) {
        if(c.x<0||c.z<0||c.x>=width||c.z>=width||c.halfY<3||c.halfY>16)return false;
        AABB box=body(c,ox,oz);
        return level.noCollision(box)&&!level.noCollision(new AABB(box.minX,box.minY-.06,box.minZ,box.maxX,box.minY,box.maxZ));
    }
    @SubscribeEvent public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("xianturuinpathaudit").requires(s->s.hasPermission(2)).executes(c->{
            var p=c.getSource().getPlayerOrException();
            if(!p.server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).normalize().getFileName().toString().startsWith("Xiantu-QA"))return 0;
            String[] ids={"yunling_observatory","guiyuan_herb_garden","wilderness_sword_tomb"};
            int[] origins={10000,10060,10120},widths={25,27,31};
            int[][][] targets={{{12,4,19},{19,4,17},{12,8,8}},{{13,4,20},{13,4,12},{7,6,7},{17,4,7}},{{15,4,18},{6,4,10},{15,8,7}}};
            var results=new com.google.gson.JsonArray();int total=0;
            for(int i=0;i<3;i++) {
                int ox=origins[i],oz=18000,w=widths[i];var level=p.serverLevel();
                for(int x=ox;x<ox+w;x+=8)for(int z=oz;z<oz+w;z+=8)if(!level.hasChunkAt(new net.minecraft.core.BlockPos(x,262,z)))throw new IllegalStateException("Load QA gallery first");
                Cell start=new Cell(w/2,4,w-1);if(!stand(level,start,ox,oz,w))throw new IllegalStateException(ids[i]+" blocked entrance");
                Set<Cell> reached=new HashSet<>();ArrayDeque<Cell> queue=new ArrayDeque<>();reached.add(start);queue.add(start);
                Map<Cell,Boolean> viable=new HashMap<>();
                while(!queue.isEmpty()) {
                    Cell a=queue.remove();
                    for(int[] dir:new int[][]{{1,0},{-1,0},{0,1},{0,-1}})for(int dy:new int[]{0,1,-1,2,-2}) {
                        Cell b=new Cell(a.x+dir[0],a.halfY+dy,a.z+dir[1]);
                        if(reached.contains(b)||!viable.computeIfAbsent(b,v->stand(level,v,ox,oz,w)))continue;
                        int high=Math.max(a.halfY,b.halfY);
                        if(!clear(level,new Cell(a.x,high,a.z),ox,oz)||!clear(level,new Cell(b.x,high,b.z),ox,oz))continue;
                        // Sweep the player footprint across the edge at the higher elevation.
                        AABB aa=body(new Cell(a.x,high,a.z),ox,oz),bb=body(new Cell(b.x,high,b.z),ox,oz);
                        if(!level.noCollision(aa.minmax(bb)))continue;
                        reached.add(b);queue.add(b);
                    }
                }
                for(int[] target:targets[i])if(!reached.contains(new Cell(target[0],target[1],target[2])))throw new IllegalStateException(ids[i]+" unreachable "+Arrays.toString(target));
                var row=new com.google.gson.JsonObject();row.addProperty("id",ids[i]);row.addProperty("reachableCells",reached.size());row.addProperty("targetsPassed",targets[i].length);row.addProperty("passed",true);results.add(row);total+=targets[i].length;
            }
            var report=new com.google.gson.JsonObject();report.addProperty("passed",true);report.addProperty("method","ServerLevel collision shapes, 0.6x1.8 player swept footprint, half-block grid, up to one-block jump; not a physical keyboard walk test");report.add("structures",results);
            try {var file=net.minecraftforge.fml.loading.FMLPaths.GAMEDIR.get().resolve("xiantu-qa/ruin-paths.json");java.nio.file.Files.createDirectories(file.getParent());java.nio.file.Files.writeString(file,report.toString());}catch(java.io.IOException ex){throw new java.io.UncheckedIOException(ex);}
            int count=total;c.getSource().sendSuccess(()->Component.literal("Ruin collision routes PASS "+count),false);return count;
        }));
    }
}
