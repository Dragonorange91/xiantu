package com.longju.xiantu.talisman;
import com.longju.xiantu.alchemy.WaymarkData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkStatus;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Same-world item binding; only existing chunks in one 3x3 region may be loaded. */
public final class TalismanReturn {
 private static final String KEY="TalismanReturn";
 private static final Map<UUID,Pending> PENDING=new HashMap<>();
 private static final class Pending {
  final ServerLevel level;final BlockPos pos;final UUID table;final CompletableFuture<Boolean> existing;
  CompletableFuture<Boolean> ready;boolean ticket;
  Pending(ServerPlayer p,BlockPos pos,UUID table){
   this.level=p.serverLevel();this.pos=pos;this.table=table;var reads=new ArrayList<CompletableFuture<Boolean>>();var center=new ChunkPos(pos);
   for(int x=center.x-1;x<=center.x+1;x++)for(int z=center.z-1;z<=center.z+1;z++){var at=new ChunkPos(x,z);reads.add(level.hasChunk(x,z)?CompletableFuture.completedFuture(true):level.getChunkSource().chunkMap.read(at).thenApply(tag->tag.isPresent()&&tag.get().getString("Status").endsWith("full")));}
   existing=CompletableFuture.allOf(reads.toArray(CompletableFuture[]::new)).thenApply(v->reads.stream().allMatch(CompletableFuture::join)).exceptionally(e->false);
  }
 }
 public static String bind(ServerPlayer p,ItemStack s,TalismanTableEntity table){
  var anchor=TalismanProgress.data(p).getCompound("Anchors").getCompound(p.level().dimension().location().toString());
  if(p.distanceToSqr(table.getBlockPos().getCenter())>9||!anchor.hasUUID("Table")||!anchor.getUUID("Table").equals(table.identity))return "请先登记本人制符台，并在三格内绑定";
  var n=new CompoundTag();n.putUUID("World",WaymarkData.get(p.serverLevel()).world());n.putUUID("Owner",p.getUUID());n.putUUID("Table",table.identity);n.putString("Dimension",p.level().dimension().location().toString());n.putLong("Position",table.getBlockPos().asLong());s.getOrCreateTag().put(KEY,n);return "";
 }
 public static String begin(ServerPlayer p,ItemStack item,int tier){
  var n=item.getTagElement(KEY);
  if(n==null||!n.hasUUID("World")||!n.getUUID("World").equals(WaymarkData.get(p.serverLevel()).world())||!n.hasUUID("Owner")||!n.getUUID("Owner").equals(p.getUUID())||!n.hasUUID("Table")||!n.getString("Dimension").equals(p.level().dimension().location().toString()))return "千里符尚未绑定本人在本世界、本维度的制符台";
  var pos=BlockPos.of(n.getLong("Position"));
  if(p.distanceToSqr(pos.getCenter())>Math.pow(Math.min(3072,1024*(tier-5)),2)||!p.serverLevel().getWorldBorder().isWithinBounds(pos))return "归途印记超出距离或世界边界";
  cancel(p);PENDING.put(p.getUUID(),new Pending(p,pos,n.getUUID("Table")));return "";
 }
 public static void tick(ServerPlayer p){var t=PENDING.get(p.getUUID());if(t==null||!t.existing.isDone()||t.ready!=null)return;if(!t.existing.join()){t.ready=CompletableFuture.completedFuture(false);return;}
  t.level.getChunkSource().addRegionTicket(TicketType.PORTAL,new ChunkPos(t.pos),1,t.pos);t.ticket=true;
  t.ready=t.level.getChunkSource().getChunkFuture(t.pos.getX()>>4,t.pos.getZ()>>4,ChunkStatus.FULL,false).thenApply(value->value.left().isPresent()).exceptionally(e->false);
 }
 public static BlockPos destination(ServerPlayer p){var t=PENDING.get(p.getUUID());return t!=null&&t.level==p.level()&&t.ready!=null&&t.ready.isDone()&&t.ready.join()&&t.level.hasChunkAt(t.pos)&&t.level.getBlockEntity(t.pos) instanceof TalismanTableEntity be&&be.identity.equals(t.table)?t.pos:null;}
 public static void cancel(ServerPlayer p){var t=PENDING.remove(p.getUUID());if(t!=null&&t.ticket)t.level.getChunkSource().removeRegionTicket(TicketType.PORTAL,new ChunkPos(t.pos),1,t.pos);}
 public static void clear(){for(var t:PENDING.values())if(t.ticket)t.level.getChunkSource().removeRegionTicket(TicketType.PORTAL,new ChunkPos(t.pos),1,t.pos);PENDING.clear();}
 private TalismanReturn(){}
}
