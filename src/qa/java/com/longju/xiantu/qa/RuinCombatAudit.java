package com.longju.xiantu.qa;
import com.google.gson.*;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.longju.xiantu.worldgen.*;
import com.longju.xiantu.entity.*;
import com.longju.xiantu.registry.ModBlocks;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

@Mod.EventBusSubscriber(modid="xiantu_qa")
public final class RuinCombatAudit {
    @SubscribeEvent public static void command(RegisterCommandsEvent e){e.getDispatcher().register(Commands.literal("xianturuincombataudit").requires(s->s.hasPermission(2))
        .then(Commands.argument("id",StringArgumentType.word()).then(Commands.argument("origin",BlockPosArgument.blockPos()).executes(c->{
            var source=c.getSource();var p=source.getPlayerOrException();var level=p.serverLevel();String id=StringArgumentType.getString(c,"id");
            if(!p.server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).normalize().getFileName().toString().startsWith("Xiantu-QA"))return 0;
            JsonObject report=new JsonObject();report.addProperty("id",id);
            try {
                BlockPos origin=BlockPosArgument.getLoadedBlockPos(c,"origin");JsonObject spec;
                try(var reader=level.getServer().getResourceManager().getResource(new ResourceLocation("xiantu","ruin_blueprints/"+id+".json")).orElseThrow().openAsReader()){spec=JsonParser.parseReader(reader).getAsJsonObject();}
                var cfg=spec.getAsJsonObject("encounter");var a=cfg.getAsJsonArray("position");BlockPos anchor=origin.offset(a.get(0).getAsInt(),a.get(1).getAsInt(),a.get(2).getAsInt());
                String instance=RuinEncounterService.key(level,id,origin);UUID uuid=UUID.fromString(instance);List<String> pool=new ArrayList<>();for(var v:cfg.getAsJsonArray("species"))pool.add(v.getAsString());int count=cfg.get("count").getAsInt(),cap=cfg.get("maxRealm").getAsInt();
                var ledger=RuinInstanceData.get(level);require(ledger.placed(instance),"instance was initialized");
                if(!ledger.triggered(instance)){
                    require(level.getBlockEntity(anchor) instanceof RuinEncounterBlockEntity,"seal exists");
                    RuinEncounterService.spawnWave(level,anchor,(RuinEncounterBlockEntity)level.getBlockEntity(anchor));
                    if(ledger.triggered(instance)){
                        // Entity section indexes become queryable after this server tick.
                        source.sendSuccess(()->Component.literal("Ruin wave initialized; rerun audit after one tick"),false);
                        return 1;
                    }
                }
                require(ledger.triggered(instance),"wave committed");
                var area=new AABB(anchor).inflate(48);var mobs=level.getEntitiesOfClass(Mob.class,area,m->m.getPersistentData().hasUUID(RuinEncounterService.SOURCE)&&m.getPersistentData().getUUID(RuinEncounterService.SOURCE).equals(uuid));
                require(mobs.size()==count,"expected "+count+" guardians, found "+mobs.size());
                for(var m:mobs){require(BeastRealmService.valid(m),"valid saved realm");require(BeastRealmService.realm(m)<=cap,"realm cap");require(BeastResourceDrops.eligibleSource(m),"material and cultivation source");require(!m.isNoAi(),"real AI enabled");require(pool.contains(BeastRealmService.species(m)),"theme pool");}
                Set<UUID> before=new HashSet<>();mobs.forEach(m->before.add(m.getUUID()));
                var repeated=new RuinEncounterBlockEntity(anchor,ModBlocks.RUIN_ENCOUNTER.get().defaultBlockState());repeated.configure(instance,pool,count,cap);RuinEncounterService.spawnWave(level,anchor,repeated);
                Set<UUID> after=new HashSet<>();level.getEntitiesOfClass(Mob.class,area,m->m.getPersistentData().hasUUID(RuinEncounterService.SOURCE)&&m.getPersistentData().getUUID(RuinEncounterService.SOURCE).equals(uuid)).forEach(m->after.add(m.getUUID()));
                require(before.equals(after),"repeat cannot spawn a second wave");require(!level.getBlockState(anchor).is(ModBlocks.RUIN_ENCOUNTER.get()),"seal removed after wave");
                require(((net.minecraft.world.level.saveddata.SavedData)ledger).save(new net.minecraft.nbt.CompoundTag()).getList("Triggered",8).stream().anyMatch(t->t.getAsString().equals(instance)),"commit serializes");
                int containers=0,stacks=0;
                for(var v:spec.getAsJsonArray("markers")){var m=v.getAsJsonObject();if(!m.get("type").getAsString().equals("reward"))continue;var at=m.getAsJsonArray("position");
                    if(level.getBlockEntity(origin.offset(at.get(0).getAsInt(),at.get(1).getAsInt(),at.get(2).getAsInt())) instanceof RandomizableContainerBlockEntity chest){
                        chest.unpackLootTable(p);String contents=chest.saveWithoutMetadata().toString();chest.unpackLootTable(p);require(contents.equals(chest.saveWithoutMetadata().toString()),"loot does not reroll");containers++;for(int i=0;i<chest.getContainerSize();i++)if(!chest.getItem(i).isEmpty())stacks++;
                    }
                }
                require(containers>0&&stacks>0,"real material loot exists");report.addProperty("passed",true);report.addProperty("guardians",mobs.size());report.addProperty("lootContainers",containers);report.addProperty("lootStacks",stacks);report.addProperty("method","Server-side real spawn transaction and native loot; this alone does not prove approach activation, attack animation, or restart persistence");
                source.sendSuccess(()->Component.literal("Ruin combat PASS "+id+" guardians="+count),false);
            }catch(Throwable ex){report.addProperty("passed",false);report.addProperty("error",ex.toString());org.slf4j.LoggerFactory.getLogger(RuinCombatAudit.class).error("Ruin combat audit",ex);source.sendFailure(Component.literal("Ruin combat FAIL "+id+": "+ex));}
            try{var f=net.minecraftforge.fml.loading.FMLPaths.GAMEDIR.get().resolve("xiantu-qa/ruin-combat-"+id+".json");java.nio.file.Files.createDirectories(f.getParent());java.nio.file.Files.writeString(f,report.toString());}catch(java.io.IOException ex){throw new java.io.UncheckedIOException(ex);}return report.get("passed").getAsBoolean()?1:0;
        }))));}
    private static void require(boolean ok,String message){if(!ok)throw new IllegalStateException(message);}
}
