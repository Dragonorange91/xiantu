package com.longju.xiantu.qa;

import com.longju.xiantu.worldgen.*;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Sequential, bounded placement into a new disposable flat save; no overwrite/reward replay. */
@Mod.EventBusSubscriber(modid="xiantu_qa", bus=Mod.EventBusSubscriber.Bus.FORGE)
public final class RuinGallery {
 private static final String KEY="xiantu_qa_ruin_gallery";
 private static final List<String> IDS=List.of("ancient_battlefield","ancient_cultivator_cave","broken_bridge_pavilion","cinnabar_seal_tower","cloudsea_palace","earthvein_mine_palace","fallen_cultivator_necropolis","five_elements_prison_grand","floating_herb_garden","guiyuan_herb_garden","mysterious_bird_nest","ninefold_star_observatory","skyship_wreck","starfall_sanctuary_grand","taixu_tomb_passage","water_curtain_caverns_grand","water_palace","wilderness_sword_tomb","xuantian_ruined_city","yunling_observatory");
 private static ServerPlayer owner;private static int next;private static String pending;private static ListTag entries;
 private static boolean guard(ServerPlayer p){return p.server.getWorldPath(LevelResource.ROOT).normalize().getFileName().toString().startsWith("Xiantu-QA-Ruins")&&p.level().dimension().equals(net.minecraft.world.level.Level.OVERWORLD);}
 private static java.nio.file.Path manifest(ServerPlayer p){return p.server.getWorldPath(LevelResource.ROOT).resolve("ruin-gallery.snbt");}
 private static CompoundTag read(ServerPlayer p){try{return java.nio.file.Files.exists(manifest(p))?TagParser.parseTag(java.nio.file.Files.readString(manifest(p),java.nio.charset.StandardCharsets.UTF_8)):new CompoundTag();}catch(java.io.IOException|com.mojang.brigadier.exceptions.CommandSyntaxException ex){throw new IllegalStateException("遗迹展示记录无法读取",ex);}}
 // Registered once by MedicineIntegration; tick/stopped remain Forge subscribers.
 public static void commands(RegisterCommandsEvent e){
  e.getDispatcher().register(Commands.literal("xianturuingallery")
   .then(Commands.literal("build").executes(c->{var p=c.getSource().getPlayerOrException();if(!guard(p)){c.getSource().sendFailure(Component.literal("只允许新建的 Xiantu-QA-Ruins 超平坦测试存档"));return 0;}
    if(!(p.serverLevel().getChunkSource().getGenerator() instanceof net.minecraft.world.level.levelgen.FlatLevelSource)){c.getSource().sendFailure(Component.literal("请先进入超平坦展示存档"));return 0;}
    if(owner!=null||RuinPlacementQueue.busy()||java.nio.file.Files.exists(manifest(p))){c.getSource().sendFailure(Component.literal("展示场已经建立或正在放置，不会覆盖"));return 0;}
    entries=new ListTag();int x=0;
    for(String id:IDS){var spec=RuinSpecs.load(p.server,id);var size=spec.getAsJsonArray("size");var entry=spec.getAsJsonObject("placement").getAsJsonArray("entry");var n=new CompoundTag();n.putString("Id",id);n.putString("Name",spec.get("name").getAsString());n.putInt("X",x);n.putInt("Y",-59);n.putInt("Z",-entry.get(2).getAsInt());n.putInt("EntryX",x+entry.get(0).getAsInt());n.putInt("EntryY",-59+entry.get(1).getAsInt());entries.add(n);x+=size.get(0).getAsInt()+32;}
    owner=p;next=0;pending=null;p.getPersistentData().put(KEY,entries.copy());write(p);return 1;}))
   .then(Commands.literal("status").executes(c->{var p=c.getSource().getPlayerOrException();if(!guard(p))return 0;var saved=read(p);c.getSource().sendSuccess(()->Component.literal("遗迹展示："+saved.getInt("Completed")+" / 20"+(owner==null?"":"（依次施工中）")),false);return 1;}))
   .then(Commands.literal("goto").then(Commands.argument("number",IntegerArgumentType.integer(1,20)).executes(c->{var p=c.getSource().getPlayerOrException();if(!guard(p))return 0;var saved=read(p);var list=saved.getList("Entries",10);int index=IntegerArgumentType.getInteger(c,"number")-1;if(index>=list.size()||index>=saved.getInt("Completed")){c.getSource().sendFailure(Component.literal("此编号尚未放置完成"));return 0;}visit(p,list.getCompound(index));return 1;}))));
 }
 private static void visit(ServerPlayer p,CompoundTag n){p.teleportTo(p.serverLevel(),n.getInt("EntryX")+.5,n.getInt("EntryY")+1,7.5,180,8);p.sendSystemMessage(Component.literal(n.getString("Name")+" · "+n.getString("Id")));}
 private static void write(ServerPlayer p){try{var path=manifest(p);var root=new CompoundTag();root.put("Entries",p.getPersistentData().getList(KEY,10).copy());root.putInt("Completed",p.getPersistentData().getInt(KEY+"_done"));var tmp=path.resolveSibling("ruin-gallery.snbt.tmp");java.nio.file.Files.writeString(tmp,root.toString(),java.nio.charset.StandardCharsets.UTF_8);java.nio.file.Files.move(tmp,path,java.nio.file.StandardCopyOption.REPLACE_EXISTING);}catch(java.io.IOException ex){throw new java.io.UncheckedIOException(ex);}}
 @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e){
  if(e.phase!=TickEvent.Phase.END||owner==null||RuinPlacementQueue.busy())return;var p=owner;
  if(p.hasDisconnected()){owner=null;return;}
  if(pending!=null){if(!pending.equals(RuinPlacementQueue.completed())){p.sendSystemMessage(Component.literal("展示放置失败，已停止；检查日志后处理"));owner=null;return;}p.getPersistentData().putInt(KEY+"_done",next);write(p);pending=null;}
  if(next>=IDS.size()){visit(p,entries.getCompound(0));p.sendSystemMessage(Component.literal("20座正式遗迹已按编号一字排开。/xianturuingallery goto 1..20 可到对应入口。"));owner=null;return;}
  var n=entries.getCompound(next);var id=IDS.get(next);var l=p.serverLevel();var origin=new BlockPos(n.getInt("X"),n.getInt("Y"),n.getInt("Z"));var template=l.getStructureManager().get(new ResourceLocation("xiantu","ruins/"+id)).orElseThrow();var spec=RuinSpecs.load(p.server,id);RuinEncounterService.validate(spec);visit(p,n);
  final int number=next+1;RuinProtection.internal(()->{for(int dx=-3;dx<=3;dx++)for(int dz=2;dz<=12;dz++)l.setBlock(new BlockPos(n.getInt("EntryX")+dx,-60,dz),Blocks.STONE_BRICKS.defaultBlockState(),2);
   var at=new BlockPos(n.getInt("EntryX")-3,-59,8);l.setBlock(at,Blocks.OAK_SIGN.defaultBlockState().setValue(BlockStateProperties.ROTATION_16,0),3);
   if(l.getBlockEntity(at)instanceof SignBlockEntity sign){var text=sign.getFrontText().setMessage(0,Component.literal(String.format("%02d / 20",number))).setMessage(1,Component.literal(n.getString("Name"))).setMessage(2,Component.literal("/xianturuingallery")).setMessage(3,Component.literal("goto "+number));sign.setText(text,true);sign.setText(text,false);sign.setChanged();}return true;});
  pending=RuinEncounterService.key(l,id,origin);if(!RuinPlacementQueue.enqueue(p.createCommandSourceStack(),origin,spec,template,pending)){owner=null;return;}next++;
 }
 @SubscribeEvent public static void stopped(ServerStoppedEvent e){owner=null;pending=null;entries=null;}
 private RuinGallery(){}
}
