package com.longju.xiantu.instance;

import com.longju.xiantu.mixin.*;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.border.*;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.LevelEvent;
import java.util.*;

/** Forge 1.20.1 adapter. Never touches a cave, and never moves a directory with an open level. */
public final class InstanceLevelLifecycle {
    private static final Map<MinecraftServer,Job> JOBS=new IdentityHashMap<>();
    private static final class Job {
        final InstanceLedger.Slot slot;final ServerLevel level;
        final long started=System.nanoTime();int ticks,archiveRetries,retryDelay;boolean detached,failed;
        Job(InstanceLedger.Slot slot,ServerLevel level){this.slot=slot;this.level=level;}
    }
    private static final ChunkProgressListener SILENT=new ChunkProgressListener(){
        public void updateSpawnPos(ChunkPos p){} public void onStatusChange(ChunkPos p,ChunkStatus s){}
        public void start(){} public void stop(){}
    };
    public static void tick(MinecraftServer server){
        if(!server.isSameThread())throw new IllegalStateException("Instance lifecycle requires server thread");
        var data=InstanceService.data(server);if(data.blocked())return;
        var job=JOBS.get(server);
        if(job==null){
            var slot=data.slots().stream().filter(s->s.kind()==InstanceLedger.Kind.REALM&&s.state()==InstanceLedger.State.QUARANTINED&&data.session(s).getBoolean("Closed")).findFirst().orElse(null);
            if(slot==null)return;
            job=new Job(slot,InstanceService.level(server,slot));JOBS.put(server,job);
        }
        if(job.failed)return;
        if(job.retryDelay>0){job.retryDelay--;return;}
        try{
            var old=job.level;
            if(old==null)throw new IllegalStateException("Missing quarantined level; refuse online archive");
            if(!job.detached){
            // A dead player may still be waiting on the native respawn screen. Never close underneath it.
            if(!old.players().isEmpty()||server.getPlayerList().getPlayers().stream().anyMatch(p->p.serverLevel()==old))return;
            var chunks=old.getChunkSource();chunks.removeTicketsOnClosing();
            long deadline=System.nanoTime()+2_000_000L;
            chunks.tick(()->System.nanoTime()<deadline,false);
            while(System.nanoTime()<deadline&&chunks.pollTask()){}
            if(chunks.chunkMap.hasWork()){
                if(++job.ticks>2400)throw new IllegalStateException("Realm chunk drain timed out; retained quarantine");
                return;
            }
            // Write the closed/quarantined journal before any filesystem move. A power loss
            // after the atomic move must not reload an older ACTIVE manifest over a blank path.
            data.setDirty();server.overworld().getDataStorage().save();
            old.save(null,true,false);
            if(!server.forgeGetWorldMap().remove(old.dimension(),old))throw new IllegalStateException("Level identity changed during cleanup");
            server.markWorldsDirty();
            MinecraftForge.EVENT_BUS.post(new LevelEvent.Unload(old));
            old.close();old.invalidateCaps();
            // Native createLevels installs an overworld -> child border listener. Release that reference too.
            var border=server.overworld().getWorldBorder();
            for(var listener:((InstanceBorderAccess)border).xiantu$listeners())
                if(listener instanceof BorderChangeListener.DelegateBorderChangeListener&&((InstanceBorderDelegateAccess)listener).xiantu$target()==old.getWorldBorder())border.removeListener(listener);
            // Windows may retain a just-closed region/directory handle briefly. Yield
            // before renaming; a retry must never save or close this detached level again.
            job.detached=true;job.retryDelay=10;return;
            }
            InstanceMaintenance.archive(server,job.slot,data.session(job.slot));
            data.session(job.slot).putBoolean("ArchiveVerified",true);data.setDirty();
            server.overworld().getDataStorage().save();
            replacement(server,old,job.slot);
            if(!data.ledger().cleanedAfterArchive(job.slot))throw new IllegalStateException("Stale cleanup generation");
            data.runtime().remove("NextCheck");if(!data.runtime().contains("NextRealmAtMillis"))data.runtime().putLong("NextRealmAtMillis",InstanceRules.nextRealmAt(System.currentTimeMillis()));data.setDirty();
            // FREE is committed only after both the archive and fresh level exist.
            server.overworld().getDataStorage().save();
            LogUtils.getLogger().info("秘境在线归档完成 {} generation {}; {} ticks, {} ms",job.slot.dimension(),job.slot.generation(),job.ticks,(System.nanoTime()-job.started)/1_000_000);
            JOBS.remove(server);
        }catch(Exception|LinkageError failure){
            if(job.detached&&failure instanceof java.nio.file.FileSystemException
                    &&!data.session(job.slot).getBoolean("ArchiveVerified")&&++job.archiveRetries<=10){
                job.retryDelay=20;
                LogUtils.getLogger().warn("秘境目录暂不可移动，保留隔离并稍后重试 {} attempt {}",job.slot.dimension(),job.archiveRetries);
                return;
            }
            job.failed=true;LogUtils.getLogger().error("秘境在线回收失败，禁止复用未确认内容 {}",job.slot.dimension(),failure);
        }
    }
    private static void replacement(MinecraftServer server,ServerLevel old,InstanceLedger.Slot slot){
        var base=server.registryAccess().registryOrThrow(Registries.LEVEL_STEM).get(old.dimension().location());
        if(base==null||!(base.generator() instanceof InstanceChunkGenerator))throw new IllegalStateException("Unexpected realm dimension definition");
        var access=(InstanceServerAccess)server;var world=server.getWorldData();
        var stem=new LevelStem(base.type(),new InstanceChunkGenerator(base.generator().getBiomeSource(),false,slot.index()));
        var next=new ServerLevel(server,access.xiantu$executor(),access.xiantu$storage(),new DerivedLevelData(world,world.overworldData()),old.dimension(),stem,SILENT,world.isDebugWorld(),BiomeManager.obfuscateSeed(world.worldGenOptions().seed()),List.of(),false,server.overworld().getRandomSequences());
        server.forgeGetWorldMap().put(old.dimension(),next);server.markWorldsDirty();
        MinecraftForge.EVENT_BUS.post(new LevelEvent.Load(next));
    }
    public static void clear(MinecraftServer server){JOBS.remove(server);}
    private InstanceLevelLifecycle(){}
}
