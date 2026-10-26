package com.longju.xiantu.qa;
import com.longju.xiantu.worldgen.RuinProtection;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;
/** Explicit author-only refresh of the disposable current gallery; keeps block entities. */
@Mod.EventBusSubscriber(modid="xiantu_qa")
public final class GalleryRevision {
 private record Cell(BlockPos pos,BlockState state){}
 private static ServerPlayer owner;private static ArrayDeque<Cell> queue;private static int changed,failed;
 public static void commands(RegisterCommandsEvent event){event.getDispatcher().register(Commands.literal("xiantugalleryrefresh").requires(s->s.hasPermission(2)).then(Commands.argument("number",com.mojang.brigadier.arguments.IntegerArgumentType.integer(1,20)).executes(c->{
  var p=c.getSource().getPlayerOrException();var root=p.server.getWorldPath(LevelResource.ROOT).normalize();
  if(!root.getFileName().toString().equals("Xiantu-QA-Ruins-Interiors-202610")||!p.isCreative()||owner!=null)return 0;
  try{
   var manifest=TagParser.parseTag(java.nio.file.Files.readString(root.resolve("ruin-gallery.snbt")));
   var row=manifest.getList("Entries",10).getCompound(com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c,"number")-1);
   var origin=new BlockPos(row.getInt("X"),row.getInt("Y"),row.getInt("Z"));
   var template=p.serverLevel().getStructureManager().get(new net.minecraft.resources.ResourceLocation("xiantu","ruins/"+row.getString("Id"))).orElseThrow();
   var n=template.save(new CompoundTag());var palette=n.getList("palette",10);var states=new ArrayList<BlockState>();
   var lookup=p.serverLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BLOCK);
   for(int i=0;i<palette.size();i++)states.add(NbtUtils.readBlockState(lookup,palette.getCompound(i)));
   queue=new ArrayDeque<>();for(var raw:n.getList("blocks",10)){var b=(CompoundTag)raw;var xyz=b.getList("pos",3);var at=origin.offset(xyz.getInt(0),xyz.getInt(1),xyz.getInt(2));queue.add(new Cell(at,states.get(b.getInt("state"))));}
   owner=p;changed=failed=0;p.sendSystemMessage(Component.literal("Gallery refresh queued: "+row.getString("Id")));return 1;
  }catch(Exception ex){throw new IllegalStateException(ex);}
 })));}
 @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e){if(e.phase!=TickEvent.Phase.END||owner==null)return;
  if(owner.hasDisconnected()){owner=null;queue=null;return;}
  var l=owner.serverLevel();RuinProtection.internal(()->{for(int i=0;i<4096&&!queue.isEmpty();i++){var cell=queue.remove();var old=l.getBlockState(cell.pos());
   if(old.equals(cell.state()))continue;
   if(l.getBlockEntity(cell.pos())!=null){if(old.getBlock()!=cell.state().getBlock())failed++;continue;}
   if(l.setBlock(cell.pos(),cell.state(),2))changed++;else failed++;
  }return true;});
  if(queue.isEmpty()){owner.sendSystemMessage(Component.literal("Gallery refresh complete: "+changed+" changed, "+failed+" failed"));owner=null;queue=null;}
 }
 @SubscribeEvent public static void stop(net.minecraftforge.event.server.ServerStoppedEvent e){owner=null;queue=null;}
 private GalleryRevision(){}
}
