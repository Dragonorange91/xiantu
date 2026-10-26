package com.longju.xiantu.alchemy;
import com.longju.xiantu.item.PillItem;
import com.longju.xiantu.cultivation.CultivationEvents;
import net.minecraft.server.level.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Read existing region headers first; never request generation for an unvisited destination. */
public final class CrossDimensionMedicine {
    private static final Map<UUID,Pending> PENDING=new HashMap<>();
    private static final class Pending {
        final InteractionHand hand;final ItemStack item;final Vec3 start;final ResourceKey<Level> from;
        final ServerLevel destination;final BlockPos marker;final long started;
        final CompletableFuture<Boolean> existing;
        CompletableFuture<Boolean> ready;boolean ticket;
        Pending(ServerPlayer p,InteractionHand hand,ServerLevel target,BlockPos pos){
            this.hand=hand;item=p.getItemInHand(hand).copyWithCount(1);start=p.position();from=p.level().dimension();
            destination=target;marker=pos;started=MedicineService.now(p);
            List<CompletableFuture<Boolean>> reads=new ArrayList<>();var cp=new ChunkPos(pos);
            for(int x=cp.x-1;x<=cp.x+1;x++)for(int z=cp.z-1;z<=cp.z+1;z++){
                var at=new ChunkPos(x,z);
                if(target.hasChunk(x,z))reads.add(CompletableFuture.completedFuture(true));
                else reads.add(target.getChunkSource().chunkMap.read(at).thenApply(tag->tag.isPresent()&&tag.get().getString("Status").endsWith("full")));
            }
            existing=CompletableFuture.allOf(reads.toArray(CompletableFuture[]::new)).thenApply(v->reads.stream().allMatch(CompletableFuture::join)).exceptionally(e->false);
        }
    }
    public static boolean active(ServerPlayer p){return PENDING.containsKey(p.getUUID());}
    public static ServerLevel binding(ServerPlayer p){
        var s=MedicineService.data(p).medicine().state();
        if(!s.hasUUID("waymark_world")||!s.getUUID("waymark_world").equals(WaymarkData.get(p.serverLevel()).world())
                ||!s.hasUUID("waymark_owner")||!s.getUUID("waymark_owner").equals(p.getUUID()))return null;
        var id=ResourceLocation.tryParse(s.getString("waymark_dimension"));if(id==null)return null;
        var key=ResourceKey.create(Registries.DIMENSION,id);
        if(!Set.of(Level.OVERWORLD,Level.NETHER,Level.END).contains(key)||key.equals(p.level().dimension()))return null;
        return p.getServer().getLevel(key);
    }
    public static ServerLevel destination(ServerPlayer p){
        var level=binding(p);if(level==null)return null;
        BlockPos pos=BlockPos.of(MedicineService.data(p).medicine().state().getLong("waymark_position"));
        if(!level.hasChunk(pos.getX()>>4,pos.getZ()>>4)||!level.getWorldBorder().isWithinBounds(pos)
            ||!WaymarkData.get(level).owned(level,pos,p.getUUID())||!level.getBlockState(pos).is(PillWaymark.BLOCK.get()))return null;
        for(int i=1;i<=2;i++)if(!level.getBlockState(pos.above(i)).getCollisionShape(level,pos.above(i)).isEmpty()||!level.getFluidState(pos.above(i)).isEmpty())return null;
        return level;
    }
    public static void begin(ServerPlayer p,InteractionHand hand){
        var stack=p.getItemInHand(hand);String reason=MedicineService.denial(p,stack);
        if(!reason.isEmpty()){p.sendSystemMessage(Component.literal(reason));return;}
        cancel(p);var target=binding(p);if(target==null)return;
        var state=MedicineService.data(p).medicine().state();
        if(state.contains("seat_mode"))state.putString("seat_mode","idle");
        state.remove("detox_active");state.remove("detox_ticks");
        var pos=BlockPos.of(MedicineService.data(p).medicine().state().getLong("waymark_position"));
        PENDING.put(p.getUUID(),new Pending(p,hand,target,pos));
        p.sendSystemMessage(Component.literal("凝神破界：保持原地8秒，移动或受伤中止；成功才消耗丹药"));
    }
    public static void cancel(ServerPlayer p){
        Pending t=PENDING.remove(p.getUUID());if(t!=null&&t.ticket)t.destination.getChunkSource().removeRegionTicket(TicketType.PORTAL,new ChunkPos(t.marker),1,t.marker);
    }
    private static void reject(ServerPlayer p,String text){cancel(p);p.sendSystemMessage(Component.literal(text+"，丹药未消耗"));}
    public static void tick(ServerPlayer p){
        var t=PENDING.get(p.getUUID());if(t==null)return;var item=p.getItemInHand(t.hand);long elapsed=MedicineService.now(p)-t.started;
        if(!p.isAlive()||p.isSpectator()||!p.level().dimension().equals(t.from)||p.position().distanceToSqr(t.start)>.01
                ||!ItemStack.isSameItemSameTags(item,t.item)||!MedicineService.denial(p,item).isEmpty()){reject(p,"破界已中止");return;}
        if(t.existing.isDone()&&t.ready==null){
            if(!t.existing.join()){reject(p,"目标周边未完整生成或读取失败");return;}
            t.destination.getChunkSource().addRegionTicket(TicketType.PORTAL,new ChunkPos(t.marker),1,t.marker);t.ticket=true;
            t.ready=t.destination.getChunkSource().getChunkFuture(t.marker.getX()>>4,t.marker.getZ()>>4,ChunkStatus.FULL,false)
                .thenApply(result->result.left().isPresent()).exceptionally(e->false);
        }
        boolean ready=t.ready!=null&&t.ready.isDone()&&t.ready.join();
        if(elapsed>=100&&!ready){reject(p,"界标区块5秒内未就绪");return;}
        if(elapsed<160)return;
        if(!ready||destination(p)!=t.destination){reject(p,"界标已损坏、无权通行或落点不安全");return;}
        var d=((PillItem)item.getItem()).definition();int q=PillCatalog.quality(item);
        p.teleportTo(t.destination,t.marker.getX()+.5,t.marker.getY()+1,t.marker.getZ()+.5,p.getYRot(),p.getXRot());
        if(p.serverLevel()==t.destination){MedicineService.commitDose(p,item,d,q);MedicineService.data(p).medicine().effect("landing",1,MedicineService.now(p)+20);p.fallDistance=0;CultivationEvents.sync(p);}
        cancel(p);
    }
    private CrossDimensionMedicine(){}
}
