package com.longju.xiantu.instance;

import com.mojang.logging.LogUtils;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;

/** Verified archival shared by online lifecycle and startup crash recovery. */
@Mod.EventBusSubscriber(modid="xiantu")
public final class InstanceMaintenance {
    private static Path root(MinecraftServer s){return s.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();}
    private static Path directory(MinecraftServer s,InstanceLedger.Slot slot){
        if(!slot.dimension().matches("xiantu:(realm_\\d{2}|dongfu_\\d{3})"))throw new IllegalArgumentException("Invalid instance directory");
        return root(s).resolve("dimensions/xiantu").resolve(slot.dimension().substring(7)).normalize();
    }
    private static String identity(InstanceLedger.Slot s){return s.dimension()+"\n"+s.instance()+"\n"+s.generation()+"\n"+(s.owner()==null?"temporary":s.owner());}
    private static void noLinks(Path root,Path path)throws IOException{
        if(!path.startsWith(root)||path.equals(root))throw new IOException("Outside instance root");
        for(var p=path;p!=null&&p.startsWith(root);p=p.getParent())if(Files.isSymbolicLink(p))throw new IOException("Symbolic instance directory");
        if(Files.exists(path))try(var walk=Files.walk(path)){if(walk.anyMatch(Files::isSymbolicLink))throw new IOException("Linked file in instance");}
    }
    public static void mark(MinecraftServer server,InstanceLedger.Slot s)throws IOException{
        var path=directory(server,s);noLinks(root(server),path);Files.createDirectories(path);var marker=path.resolve("xiantu-instance.txt");
        if(Files.exists(marker)){if(!Files.readString(marker).equals(identity(s)))throw new IOException("Instance identity mismatch");return;}
        for(String section:List.of("region","entities","poi")){var folder=path.resolve(section);if(Files.exists(folder))try(var files=Files.list(folder)){if(files.findAny().isPresent())throw new IOException("Unidentified old instance content");}}
        Files.writeString(marker,identity(s),StandardOpenOption.CREATE_NEW);
    }
    /** Caller must have closed/detached the level, or run before any levels are created. */
    static void archive(MinecraftServer server,InstanceLedger.Slot slot,CompoundTag session)throws IOException{
        if(slot.kind()!=InstanceLedger.Kind.REALM||!session.getBoolean("Closed"))throw new IOException("Only closed temporary realms can be archived");
        var source=directory(server,slot);var backup=root(server).resolve("xiantu-instance-backups").resolve(slot.dimension().substring(7)+"-"+slot.generation()+"-"+slot.instance());
        noLinks(root(server),source);noLinks(root(server),backup);
        if(Files.exists(source)&&Files.exists(backup)&&session.getBoolean("ArchiveVerified")){
            // Crash between constructing a blank replacement and committing FREE. Only empty
            // directories may remain; any unexplained file still blocks recovery.
            try(var walk=Files.walk(source)){if(walk.anyMatch(Files::isRegularFile))throw new IOException("Uncommitted replacement has unexpected files");}
        }else if(Files.exists(source)){
            if(!Files.readString(source.resolve("xiantu-instance.txt")).equals(identity(slot))||Files.exists(backup))throw new IOException("Unknown source or archive conflict");
            Files.createDirectories(backup.getParent());Files.move(source,backup,StandardCopyOption.ATOMIC_MOVE);
        }
        if(!Files.isDirectory(backup)||!Files.readString(backup.resolve("xiantu-instance.txt")).equals(identity(slot)))throw new IOException("Missing verified archive");
        Files.writeString(backup.resolve("session.snbt"),session.toString(),StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING);
    }
    @SubscribeEvent public static void beforeLevels(ServerAboutToStartEvent event){
        var server=event.getServer();var file=root(server).resolve("data/xiantu_instances.dat");if(!Files.isRegularFile(file))return;
        try{
            var all=NbtIo.readCompressed(file.toFile());var payload=all.getCompound("data");var ledger=InstanceLedger.load(payload.getString("Manifest"));boolean changed=false;
            for(var slot:ledger.slots()){
                if(slot.kind()!=InstanceLedger.Kind.REALM||slot.state()!=InstanceLedger.State.QUARANTINED)continue;
                var session=payload.getCompound("Runtime").getCompound(slot.instance().toString());if(!session.getBoolean("Closed"))continue;
                try{
                    archive(server,slot,session);
                    if(!ledger.cleanedAfterArchive(slot))throw new IOException("Stale cleanup generation");changed=true;
                    LogUtils.getLogger().info("秘境离线归档完成 {} generation {}",slot.dimension(),slot.generation());
                }catch(IOException failure){LogUtils.getLogger().error("秘境归档失败，继续隔离 {}",slot.dimension(),failure);}
            }
            if(changed){
                var archive=root(server).resolve("xiantu-instance-backups");Files.createDirectories(archive);Files.copy(file,archive.resolve("manifest-before-"+System.currentTimeMillis()+".dat"));
                payload.putString("Manifest",ledger.save());payload.getCompound("Runtime").remove("NextCheck");if(!payload.getCompound("Runtime").contains("NextRealmAtMillis"))payload.getCompound("Runtime").putLong("NextRealmAtMillis",InstanceRules.nextRealmAt(System.currentTimeMillis()));var temporary=file.resolveSibling("xiantu_instances.startup.tmp");NbtIo.writeCompressed(all,temporary.toFile());Files.move(temporary,file,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);
            }
        }catch(Exception failure){LogUtils.getLogger().error("实例启动检查失败，未清理任何未确认目录",failure);}
    }
    private InstanceMaintenance(){}
}
