package com.longju.xiantu.spell;

import com.longju.xiantu.worldgen.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import java.util.HashSet;

/** One swept player-body collision per step, without synchronous chunk generation. */
public final class SwordFlightCollision {
 public static boolean loaded(Level level,AABB swept){
  // BlockCollisions also visits neighbouring cells for fences and other extended shapes.
  for(int x=Mth.floor(swept.minX-1)>>4;x<=Mth.floor(swept.maxX+1)>>4;x++)
   for(int z=Mth.floor(swept.minZ-1)>>4;z<=Mth.floor(swept.maxZ+1)>>4;z++)
    if(level instanceof ServerLevel server?server.getChunkSource().getChunkNow(x,z)==null:!level.hasChunk(x,z))return false;
  return true;
 }
 public static Vec3 clip(Player rider,Entity sword,Vec3 at,Vec3 velocity){
  if(velocity.lengthSqr()==0)return velocity;
  var level=sword.level();var feet=at.add(0,SpiritSwordEntity.DECK_HEIGHT,0);
  var body=rider.getBoundingBox().move(feet.subtract(rider.position()));
  var blade=sword.getBoundingBox().move(at.subtract(sword.position()));
  var swept=body.minmax(blade).expandTowards(velocity);
  if(!loaded(level,swept)||!level.getWorldBorder().isWithinBounds(body.move(velocity)))return Vec3.ZERO;
  Vec3 safe=Entity.collideBoundingBox(rider,velocity,body,level,level.getEntityCollisions(rider,body.expandTowards(velocity)));
  if(rider instanceof ServerPlayer server&&!arenaAllows(server,feet,feet.add(safe)))return Vec3.ZERO;
  return Entity.collideBoundingBox(sword,safe,blade,level,level.getEntityCollisions(sword,blade.expandTowards(safe)));
 }
 private static boolean arenaAllows(ServerPlayer p,Vec3 start,Vec3 end){
  var ledger=RuinAdventureData.get(p.serverLevel());var candidates=new HashSet<RuinAdventureData.Instance>();
  var path=new AABB(start,end);
  for(int x=Mth.floor(path.minX)>>4;x<=Mth.floor(path.maxX)>>4;x++)
   for(int z=Mth.floor(path.minZ)>>4;z<=Mth.floor(path.maxZ)>>4;z++)candidates.addAll(ledger.at(new BlockPos(x<<4,0,z<<4)));
  for(var instance:candidates)if(instance.boss()&&!instance.defeated()){
   var arena=instance.arena();boolean inside=arena.contains(start),next=arena.contains(end);
   if(inside!=next||!inside&&arena.clip(start,end).isPresent()||next&&!RuinAdventureService.legal(p,instance))return false;
  }
  return true;
 }
 private SwordFlightCollision(){}
}
