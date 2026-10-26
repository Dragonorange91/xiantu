package com.longju.xiantu.worldgen;
import com.longju.xiantu.entity.*;
import com.longju.xiantu.network.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.BossEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;
/** Native events remain authoritative for visibility, fallback and removal. */
@Mod.EventBusSubscriber(modid="xiantu")
public final class RuinBossBars {
 private static final Map<UUID,ServerBossEvent> OWNED=new HashMap<>();
 private static boolean reflectionWarned;
 private static ServerBossEvent nativeEvent(Mob m){
  try{
   if(m instanceof WitherBoss w)return ObfuscationReflectionHelper.getPrivateValue(WitherBoss.class,w,"f_31430_");
   if(m instanceof EnderDragon d&&d.getDragonFight()!=null)return ObfuscationReflectionHelper.getPrivateValue(net.minecraft.world.level.dimension.end.EndDragonFight.class,d.getDragonFight(),"f_64060_");
  }catch(RuntimeException ex){if(!reflectionWarned){reflectionWarned=true;com.mojang.logging.LogUtils.getLogger().warn("Native boss HUD access unavailable; keeping vanilla bar",ex);}return null;}
  return OWNED.computeIfAbsent(m.getUUID(),id->new ServerBossEvent(m.getDisplayName(),BossEvent.BossBarColor.YELLOW,BossEvent.BossBarOverlay.PROGRESS));
 }
 @SubscribeEvent public static void stop(ServerStoppedEvent e){OWNED.values().forEach(ServerBossEvent::removeAllPlayers);OWNED.clear();}
 @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e){
  if(e.phase!=TickEvent.Phase.END)return;var server=net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();if(server==null||server.getTickCount()%10!=0)return;
  Set<UUID> alive=new HashSet<>();
  for(var l:server.getAllLevels()){
   Map<UUID,List<BossHudPacket.Entry>> packets=new HashMap<>();for(var p:l.players())packets.put(p.getUUID(),new ArrayList<>());
   for(var entity:l.getAllEntities())if(entity instanceof Mob m&&m.isAlive()&&HostileRealmService.boss(m)){
    alive.add(m.getUUID());var bar=nativeEvent(m);if(bar==null)continue;
    boolean owned=OWNED.containsKey(m.getUUID());
    if(owned){bar.setName(m.getDisplayName());bar.setProgress(Math.max(0,Math.min(1,m.getHealth()/Math.max(1,m.getMaxHealth()))));
     Set<ServerPlayer> visible=new HashSet<>();for(var p:l.players())if(p.distanceToSqr(m)<=4096&&visible(p,m))visible.add(p);
     for(var p:List.copyOf(bar.getPlayers()))if(!visible.contains(p))bar.removePlayer(p);for(var p:visible)bar.addPlayer(p);
    }
    var key=ForgeRegistries.ENTITY_TYPES.getKey(m.getType());if(key==null)continue;
    for(var p:bar.getPlayers())if(p.serverLevel()==l&&packets.containsKey(p.getUUID()))packets.get(p.getUUID()).add(new BossHudPacket.Entry(m.getUUID(),bar.getId(),key.getPath(),m.getDisplayName(),m.getHealth(),m.getMaxHealth(),BeastRealmService.realm(m),m.getPersistentData().getInt(BeastRealmService.STAGE)));
   }
   for(var p:l.players()){var entries=packets.get(p.getUUID());entries.sort(Comparator.comparing(x->x.entity().toString()));XiantuNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new BossHudPacket(l.dimension().location(),List.copyOf(entries.subList(0,Math.min(8,entries.size())))));}
  }
  OWNED.entrySet().removeIf(x->{if(alive.contains(x.getKey()))return false;x.getValue().removeAllPlayers();return true;});
 }
 private static boolean visible(ServerPlayer p,Mob m){
  var n=m.getPersistentData();if(n.getBoolean(RuinAdventureService.BOSS)&&n.hasUUID(RuinEncounterService.SOURCE)){
   var i=RuinAdventureData.get(p.serverLevel()).find(n.getUUID(RuinEncounterService.SOURCE).toString());
   return i!=null&&!i.defeated()&&i.arena().contains(p.position())&&(p.isCreative()||p.isSpectator()||RuinAdventureService.legal(p,i));
  }return p.hasLineOfSight(m)||m.getTarget()==p;
 }
 private RuinBossBars(){}
}
