package com.longju.xiantu.worldgen;

import com.google.gson.JsonObject;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Administrative placement is budgeted; production generation uses chunk-local pieces. */
@Mod.EventBusSubscriber(modid="xiantu")
public final class RuinPlacementQueue {
    private static final ArrayDeque<Job> JOBS=new ArrayDeque<>();
    private static final class Job {
        final CommandSourceStack source;final ServerLevel level;final BlockPos origin;final JsonObject spec;
        final String root;final RuinTemplateIndex index;final int width,depth;int x,z,cell;
        final List<BlockPos> fluids=new ArrayList<>();
        Job(CommandSourceStack source,BlockPos pos,JsonObject spec,StructureTemplate template,String root){
            this.source=source;level=source.getLevel();origin=pos;this.spec=spec;this.root=root;
            index=RuinTemplateIndex.of(template);width=template.getSize().getX();depth=template.getSize().getZ();
        }
    }
    private static String completed="";
    public static boolean busy(){return !JOBS.isEmpty();}
    public static String completed(){return completed;}
    public static boolean enqueue(CommandSourceStack source,BlockPos pos,JsonObject spec,StructureTemplate template,String root){
        if(!JOBS.isEmpty()){source.sendFailure(Component.literal("已有遗迹正在放置，请等待完成"));return false;}
        // Reserve before the first write: interruption must never enable reward replay.
        RuinInstanceData.get(source.getLevel()).place(root);JOBS.add(new Job(source,pos,spec,template,root));return true;
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e){
        if(e.phase!=TickEvent.Phase.END||JOBS.isEmpty())return;var j=JOBS.peek();
        long until=System.nanoTime()+8_000_000;int writes=0;
        try{
            while(j.z<j.depth&&writes<2500&&System.nanoTime()<until){
                var cells=j.index.column(j.x,j.z);
                if(j.cell>=cells.size()){j.cell=0;if(++j.x>=j.width){j.x=0;j.z++;}continue;}
                var c=cells.get(j.cell++);var at=j.origin.offset(j.x,c.y(),j.z);
                if(!j.level.hasChunkAt(at))j.level.getChunk(at);
                var info=new StructureTemplate.StructureBlockInfo(at,c.state(),c.data());
                var p=RuinHerbProcessor.INSTANCE.processBlock(j.level,j.origin,j.origin,info,info,new StructurePlaceSettings());
                j.level.setBlock(at,p.state(),2);
                if(!p.state().getFluidState().isEmpty())j.fluids.add(at.immutable());
                if(c.data()!=null&&j.level.getBlockEntity(at)!=null){CompoundTag n=c.data().copy();n.putInt("x",at.getX());n.putInt("y",at.getY());n.putInt("z",at.getZ());j.level.getBlockEntity(at).load(n);}
                writes++;
            }
            if(j.z>=j.depth){
                // Let water flow only after its containing shell has been written.
                for(var at:j.fluids){var fluid=j.level.getFluidState(at);if(!fluid.isEmpty())j.level.scheduleTick(at,fluid.getType(),fluid.getType().getTickDelay(j.level));}
                RuinAdventureService.initialize(j.level,j.origin,j.spec,j.root);completed=j.root;JOBS.remove();j.source.sendSuccess(()->Component.literal("遗迹放置完成："+j.spec.get("name").getAsString()),false);}
        }catch(RuntimeException ex){JOBS.remove();org.slf4j.LoggerFactory.getLogger(RuinPlacementQueue.class).error("Ruin placement interrupted; reserved identity retained",ex);j.source.sendFailure(Component.literal("遗迹放置中断，已保留实例锁以防重复奖励；请检查日志"));}
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent e){JOBS.clear();}
    private RuinPlacementQueue(){}
}
