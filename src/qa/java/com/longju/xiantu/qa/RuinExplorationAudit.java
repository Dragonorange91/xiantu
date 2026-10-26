package com.longju.xiantu.qa;

import com.google.gson.*;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Read-only collision route audit of the actual template instance, never edits the world. */
@Mod.EventBusSubscriber(modid="xiantu_qa")
public final class RuinExplorationAudit {
    private record Cell(int x,int halfY,int z) {}
    private static AABB body(Cell c,BlockPos o) {
        double x=o.getX()+c.x+.5,y=o.getY()+c.halfY*.5,z=o.getZ()+c.z+.5;
        return new AABB(x-.3,y+.001,z-.3,x+.3,y+1.8,z+.3);
    }
    private static boolean stand(ServerLevel l,Cell c,BlockPos o,int w,int h,int d) {
        if(c.x<0||c.z<0||c.x>=w||c.z>=d||c.halfY<1||c.halfY>h*2-3)return false;
        AABB b=body(c,o);
        return l.noCollision(b)&&!l.noCollision(new AABB(b.minX,b.minY-.06,b.minZ,b.maxX,b.minY,b.maxZ));
    }
    @SubscribeEvent public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("xiantuexplorationaudit").requires(s->s.hasPermission(2))
            .then(Commands.argument("id",StringArgumentType.word()).then(Commands.argument("origin",BlockPosArgument.blockPos()).executes(c->{
                var source=c.getSource();var p=source.getPlayerOrException();
                if(!p.server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).normalize().getFileName().toString().startsWith("Xiantu-QA"))return 0;
                String id=StringArgumentType.getString(c,"id");if(!id.matches("[a-z0-9_]+"))return 0;
                try {
                    var o=BlockPosArgument.getLoadedBlockPos(c,"origin");ServerLevel level=p.serverLevel();
                    JsonObject spec;
                    try(var reader=level.getServer().getResourceManager().getResource(new ResourceLocation("xiantu","ruin_blueprints/"+id+".json")).orElseThrow().openAsReader()) {spec=JsonParser.parseReader(reader).getAsJsonObject();}
                    var size=spec.getAsJsonArray("size");int w=size.get(0).getAsInt(),h=size.get(1).getAsInt(),d=size.get(2).getAsInt();
                    for(int x=0;x<w;x+=8)for(int z=0;z<d;z+=8)if(!level.hasChunkAt(o.offset(x,0,z)))throw new IllegalStateException("Load the whole sample first");
                    var ent=spec.getAsJsonArray("entry");Cell start=new Cell(ent.get(0).getAsInt(),ent.get(1).getAsInt()*2,ent.get(2).getAsInt());
                    if(!stand(level,start,o,w,h,d))throw new IllegalStateException("Blocked entry "+start);
                    Set<Cell> reached=new HashSet<>();ArrayDeque<Cell> queue=new ArrayDeque<>();Map<Cell,Boolean> viable=new HashMap<>();reached.add(start);queue.add(start);
                    while(!queue.isEmpty()) {
                        Cell a=queue.remove();
                        for(int[] dir:new int[][]{{1,0},{-1,0},{0,1},{0,-1}})for(int dy:new int[]{0,1,-1,2,-2}) {
                            Cell b=new Cell(a.x+dir[0],a.halfY+dy,a.z+dir[1]);
                            if(reached.contains(b)||!viable.computeIfAbsent(b,v->stand(level,v,o,w,h,d)))continue;
                            int high=Math.max(a.halfY,b.halfY);AABB aa=body(new Cell(a.x,high,a.z),o),bb=body(new Cell(b.x,high,b.z),o);
                            if(!level.noCollision(aa.minmax(bb)))continue;
                            reached.add(b);queue.add(b);
                        }
                    }
                    JsonArray targets=new JsonArray();boolean passed=true;
                    for(var target:spec.getAsJsonArray("routeTargets")) {
                        var t=target.getAsJsonArray();int x=t.get(0).getAsInt(),y=t.get(1).getAsInt()*2,z=t.get(2).getAsInt();
                        // A stair target can stand on its half-height tread; horizontal target is exact.
                        boolean ok=reached.contains(new Cell(x,y,z))||reached.contains(new Cell(x,y-1,z))||reached.contains(new Cell(x,y+1,z));
                        JsonObject r=new JsonObject();r.add("target",t);r.addProperty("reachable",ok);targets.add(r);passed&=ok;
                    }
                    JsonObject report=new JsonObject();report.addProperty("id",id);report.addProperty("passed",passed);report.addProperty("reachableCells",reached.size());report.add("targets",targets);report.addProperty("method","Actual collision shapes; swept 0.6x1.8 footprint; half-block grid; not keyboard walking or a combat test");
                    var file=net.minecraftforge.fml.loading.FMLPaths.GAMEDIR.get().resolve("xiantu-qa/exploration-"+id+".json");java.nio.file.Files.createDirectories(file.getParent());java.nio.file.Files.writeString(file,report.toString());
                    final boolean result=passed;source.sendSuccess(()->Component.literal("Exploration "+id+" "+(result?"PASS":"FAIL")+" targets="+targets.size()+" reached="+reached.size()),false);return passed?1:0;
                } catch(Throwable ex) {org.slf4j.LoggerFactory.getLogger(RuinExplorationAudit.class).error("Exploration audit failed",ex);source.sendFailure(Component.literal("Exploration audit failed: "+ex));return 0;}
            }))));
    }
}
