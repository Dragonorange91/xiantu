package com.longju.xiantu.qa;

import com.google.gson.*;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.longju.xiantu.worldgen.*;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Proximity activation and live AI observation in disposable QA worlds, without forcing targets. */
@Mod.EventBusSubscriber(modid="xiantu_qa")
public final class RuinLiveCombatAudit {
    private static final Map<UUID,Run> RUNS=new HashMap<>();
    private static final class Run {
        ServerPlayer player;String id,instance;BlockPos anchor;Vec3 savedPos;float yaw,pitch,health,saturation;int food,age,attacks,maxGuardians,targetTicks;boolean invulnerable,triggered;GameType mode;
    }
    @SubscribeEvent public static void command(RegisterCommandsEvent e){e.getDispatcher().register(Commands.literal("xianturuinliveaudit").requires(s->s.hasPermission(2))
        .then(Commands.argument("id",StringArgumentType.word()).then(Commands.argument("origin",BlockPosArgument.blockPos()).executes(c->{
            var p=c.getSource().getPlayerOrException();var l=p.serverLevel();
            if(!p.server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).normalize().getFileName().toString().startsWith("Xiantu-QA")||RUNS.containsKey(p.getUUID()))return 0;
            String id=StringArgumentType.getString(c,"id");BlockPos o=BlockPosArgument.getLoadedBlockPos(c,"origin");
            String instance=RuinEncounterService.key(l,id,o);if(RuinInstanceData.get(l).triggered(instance)){c.getSource().sendFailure(Component.literal("Use an untriggered ruin sample"));return 0;}
            try(var reader=l.getServer().getResourceManager().getResource(new ResourceLocation("xiantu","ruin_blueprints/"+id+".json")).orElseThrow().openAsReader()){
                var spec=JsonParser.parseReader(reader).getAsJsonObject();var a=spec.getAsJsonObject("encounter").getAsJsonArray("position");var t=spec.getAsJsonArray("routeTargets");var last=t.get(t.size()-1).getAsJsonArray();
                Run r=new Run();r.player=p;r.id=id;r.instance=instance;r.anchor=o.offset(a.get(0).getAsInt(),a.get(1).getAsInt(),a.get(2).getAsInt());r.savedPos=p.position();r.yaw=p.getYRot();r.pitch=p.getXRot();r.health=p.getHealth();r.food=p.getFoodData().getFoodLevel();r.saturation=p.getFoodData().getSaturationLevel();r.mode=p.gameMode.getGameModeForPlayer();r.invulnerable=p.getAbilities().invulnerable;RUNS.put(p.getUUID(),r);
                p.teleportTo(o.getX()+last.get(0).getAsDouble()+.5,o.getY()+last.get(1).getAsDouble(),o.getZ()+last.get(2).getAsDouble()+.5);
                p.setGameMode(GameType.SURVIVAL);p.getAbilities().invulnerable=false;p.onUpdateAbilities();
                c.getSource().sendSuccess(()->Component.literal("Live ruin observation started for 20 seconds; damage is intercepted and player state will be restored"),false);return 1;
            }catch(Exception ex){throw new RuntimeException(ex);}
        }))));}
    @SubscribeEvent public static void attack(LivingAttackEvent e){
        if(!(e.getEntity() instanceof ServerPlayer p))return;Run r=RUNS.get(p.getUUID());if(r==null)return;
        if(e.getSource().getEntity() instanceof Mob m&&m.getPersistentData().hasUUID(RuinEncounterService.SOURCE)&&m.getPersistentData().getUUID(RuinEncounterService.SOURCE).toString().equals(r.instance))r.attacks++;
        e.setCanceled(true);
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e){
        if(e.phase!=TickEvent.Phase.END)return;
        for(Run r:new ArrayList<>(RUNS.values())){
            try{
                r.age++;var p=r.player;var l=p.serverLevel();r.triggered|=RuinInstanceData.get(l).triggered(r.instance);
                var mobs=l.getEntitiesOfClass(Mob.class,new AABB(r.anchor).inflate(48),m->m.getPersistentData().hasUUID(RuinEncounterService.SOURCE)&&m.getPersistentData().getUUID(RuinEncounterService.SOURCE).toString().equals(r.instance));
                r.maxGuardians=Math.max(r.maxGuardians,mobs.size());if(mobs.stream().anyMatch(m->m.getTarget()==p))r.targetTicks++;
                if(r.age==80&&!mobs.isEmpty()){
                    Mob nearest=mobs.stream().min(Comparator.comparingDouble(m->m.distanceToSqr(p))).orElseThrow();
                    // Use an empty neighbouring standing point, never force AI target or attack state.
                    approach:for(int dx:new int[]{1,-1,0})for(int dz:new int[]{1,-1,0})if(dx!=0||dz!=0){
                        double x=nearest.getX()+dx*1.4,z=nearest.getZ()+dz*1.4,y=nearest.getY();
                        AABB body=new AABB(x-.3,y+.01,z-.3,x+.3,y+1.8,z+.3);
                        if(l.noCollision(body)&&l.getBlockState(BlockPos.containing(x,y-.05,z)).isSolidRender(l,BlockPos.containing(x,y-.05,z))){p.teleportTo(x,y,z);break approach;}
                    }
                }
                if(r.age>=400||p.hasDisconnected())finish(r,null);
            }catch(Throwable ex){finish(r,ex.toString());}
        }
    }
    private static void finish(Run r,String error){
        RUNS.remove(r.player.getUUID());var p=r.player;
        try{p.setGameMode(r.mode);p.getAbilities().invulnerable=r.invulnerable;p.onUpdateAbilities();p.teleportTo(r.savedPos.x,r.savedPos.y,r.savedPos.z);p.setYRot(r.yaw);p.setXRot(r.pitch);p.setHealth(r.health);p.getFoodData().setFoodLevel(r.food);p.getFoodData().setSaturation(r.saturation);
            JsonObject j=new JsonObject();j.addProperty("id",r.id);j.addProperty("instance",r.instance);j.addProperty("proximityTriggered",r.triggered);j.addProperty("guardiansSeen",r.maxGuardians);j.addProperty("targetTicks",r.targetTicks);j.addProperty("attackAttempts",r.attacks);j.addProperty("passed",error==null&&r.triggered&&r.maxGuardians>0&&r.targetTicks>0&&r.attacks>0);j.addProperty("method","Actual survival proximity activation and unforced AI; damage intercepted to protect player; no DPS or kill/drop claim");if(error!=null)j.addProperty("error",error);
            var f=net.minecraftforge.fml.loading.FMLPaths.GAMEDIR.get().resolve("xiantu-qa/ruin-live-"+r.id+".json");java.nio.file.Files.createDirectories(f.getParent());java.nio.file.Files.writeString(f,j.toString());p.sendSystemMessage(Component.literal("Ruin live observation "+j));
        }catch(Throwable ex){org.slf4j.LoggerFactory.getLogger(RuinLiveCombatAudit.class).error("Unable to finish live ruin QA",ex);}
    }
}
