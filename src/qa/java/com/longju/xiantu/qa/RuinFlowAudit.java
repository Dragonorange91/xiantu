package com.longju.xiantu.qa;

import com.longju.xiantu.worldgen.*;

import com.google.gson.*;
import com.longju.xiantu.cultivation.*;
import com.longju.xiantu.item.DemonCoreItem;
import com.longju.xiantu.registry.ModItems;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Isolated server transaction audit, not a visual or real-time combat playthrough. */
@Mod.EventBusSubscriber(modid="xiantu_qa")
public final class RuinFlowAudit {
    private static int checks;
    private static void bossTick(net.minecraft.server.level.ServerLevel level,RuinAdventureData.Instance record) throws ReflectiveOperationException {
        var tick=RuinAdventureService.class.getDeclaredMethod("bossTick",net.minecraft.server.level.ServerLevel.class,RuinAdventureData.Instance.class);
        tick.setAccessible(true);tick.invoke(null,level,record);
    }
    private static void check(boolean ok,String reason){checks++;if(!ok)throw new IllegalStateException(reason);}
    @SubscribeEvent public static void register(RegisterCommandsEvent event){event.getDispatcher().register(Commands.literal("xianturuinflowaudit").requires(s->s.hasPermission(2)).executes(c->{
        ServerPlayer p=c.getSource().getPlayerOrException();var l=p.serverLevel();
        if(!l.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).normalize().getFileName().toString().startsWith("Xiantu-QA")||p.getVehicle()!=null)return 0;
        var data=(PlayerCultivationData)p.getCapability(CultivationCapability.DATA).orElseThrow(IllegalStateException::new);
        var saved=new CompoundTag();data.save(saved);var pos=p.position();float yaw=p.getYRot(),pitch=p.getXRot(),health=p.getHealth();var mode=p.gameMode.getGameModeForPlayer();var hand=p.getMainHandItem().copy();var difficulty=l.getDifficulty();
        var report=new JsonObject();checks=0;var original=new LinkedHashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();var records=new ArrayList<RuinAdventureData.Instance>();var entities=new ArrayList<Entity>();var previousItems=new HashSet<UUID>();
        try{
            // Do not alter an existing build or reuse a half-completed fixture.
            for(int n=0;n<3;n++){var origin=new BlockPos(34600+n*80,250,34600);for(int x=0;x<32;x++)for(int z=0;z<32;z++){
                var at=origin.offset(x,0,z);l.getChunk(at);if(!l.getBlockState(at).isAir())throw new IllegalStateException("Audit fixture occupied: "+at);
                original.put(at,l.getBlockState(at));
            }}
            for(int n=0;n<3;n++)for(Entity item:l.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(34600+n*80,250,34600,34632+n*80,266,34632)))previousItems.add(item.getUUID());
            RuinProtection.internal(()->{original.keySet().forEach(at->l.setBlock(at,Blocks.STONE_BRICKS.defaultBlockState(),3));return true;});
            p.setGameMode(GameType.SURVIVAL);data.setCultivationUnlocked(true);data.setRealmStage(0,0);l.getServer().setDifficulty(net.minecraft.world.Difficulty.EASY,true);
            var ids=List.of("five_elements_prison_grand","starfall_sanctuary_grand","water_curtain_caverns_grand");
            for(int n=0;n<ids.size();n++){
                String id=ids.get(n);var origin=new BlockPos(34600+n*80,250,34600);var spec=RuinSpecs.load(l.getServer(),id).deepCopy();
                spec.add("size",JsonParser.parseString("[32,16,32]"));spec.add("entry",JsonParser.parseString("[16,1,30]"));spec.add("markers",new JsonArray());
                var plan=spec.getAsJsonObject("bossPlan");plan.add("position",JsonParser.parseString("[16,1,16]"));plan.add("bounds",JsonParser.parseString("[1,1,1,30,14,30]"));plan.add("realmRange",JsonParser.parseString("[0,8]"));
                var waves=new JsonArray();for(int step=0;step<5;step++){var wave=new JsonObject();wave.addProperty("id","audit_"+step);wave.addProperty("lootTheme",List.of("metal","wood","water","fire","earth").get(step));wave.add("position",new Gson().toJsonTree(List.of(4+step*5,1,5)));waves.add(wave);}spec.add("encounters",waves);
                String root=RuinEncounterService.key(l,id,origin);var record=RuinAdventureData.get(l).ensure(root,id,origin,spec);records.add(record);for(String key:List.copyOf(record.state.getAllKeys()))record.state.remove(key);
                // Exercise the real per-wave death ledger, including a duplicated notification.
                for(int step=0;step<5;step++){
                    Mob guard=net.minecraft.world.entity.EntityType.ZOMBIE.create(l);entities.add(guard);guard.moveTo(origin.getX()+3,251,origin.getZ()+3,0,0);
                    guard.getPersistentData().putUUID(RuinEncounterService.SOURCE,UUID.fromString(root));guard.getPersistentData().putString(RuinAdventureService.WAVE,"audit_"+step);
                    var be=new RuinEncounterBlockEntity(origin,com.longju.xiantu.registry.ModBlocks.RUIN_ENCOUNTER.get().defaultBlockState());be.bind(root,id,origin,"audit_"+step);
                    RuinAdventureService.waveSpawned(l,be,List.of(guard));check(!record.waveDone("audit_"+step),id+" live guardian blocks progression");
                    var death=new LivingDeathEvent(guard,l.damageSources().playerAttack(p));RuinAdventureService.death(death);RuinAdventureService.death(death);check(record.waveDone("audit_"+step),id+" death completes once");
                }
                check(record.guardiansDone(),id+" all waves complete");p.teleportTo(l,origin.getX()+16.5,251,origin.getZ()+16.5,0,0);
                if(n==0){for(var node:RuinTrials.nodes(record))check(RuinTrials.interact(l,p,node,InteractionHand.MAIN_HAND),"element interaction handled");check(record.state.getInt("RuneStep")==5,"all five runes");}
                if(n==1){for(var node:RuinTrials.nodes(record).subList(0,3)){p.teleportTo(l,node.getX()+.5,node.getY()+1,node.getZ()+.5,0,0);RuinTrials.tick(l,record,List.of(p));}check(record.state.getInt("StarStep")==3,"three stellar steps");}
                if(n==2){for(String element:List.of("metal","wood","water","fire","earth")){
                    var offering=new ItemStack(ModItems.DEMON_CORE.get());DemonCoreItem.setData(offering,1,element);p.setItemInHand(InteractionHand.MAIN_HAND,offering);
                    RuinTrials.interact(l,p,RuinTrials.nodes(record).get(0),InteractionHand.MAIN_HAND);check(p.getMainHandItem().isEmpty(),"one correct offering consumed");
                }check(record.state.getInt("Offering")==5,"five offerings");
                    for(int phase=0;phase<3;phase++){RuinTrials.tick(l,record,List.of(p));var rune=origin.offset(22,1,16+(phase-1)*5);original.putIfAbsent(rune,Blocks.AIR.defaultBlockState());check(RuinTrials.strike(l,p,rune),"phantom core strike");}
                }
                p.teleportTo(l,origin.getX()+16.5,251,origin.getZ()+16.5,0,0);
                // Fast-forward only the timer; the final tick and all interactions are production code.
                record.state.putInt("HazardTicks",n==2?1790:590);RuinTrials.tick(l,record,List.of(p));check(record.state.getBoolean("HazardPassed"),"hazard terminal transition");
                var core=origin.offset(22,1,16);original.putIfAbsent(core,Blocks.AIR.defaultBlockState());if(n!=0)RuinTrials.interact(l,p,core,InteractionHand.MAIN_HAND);
                check(record.state.getBoolean("TrialComplete"),id+" complete trial");
                p.teleportTo(l,origin.getX()+27.5,251,origin.getZ()+27.5,0,0);
                bossTick(l,record);check(record.state.getBoolean("BossCommitted"),"boss committed");
                for(int t=0;t<3;t++)bossTick(l,record);
                Entity boss=l.getEntity(record.state.getUUID("BossUUID"));check(boss instanceof Mob,"real boss inserted");entities.add(boss);UUID bossId=boss.getUUID();
                bossTick(l,record);check(bossId.equals(record.state.getUUID("BossUUID"))&&l.getEntity(bossId)==boss,"same boss retained");
                var death=new LivingDeathEvent((LivingEntity)boss,l.damageSources().playerAttack(p));RuinAdventureService.death(death);
                check(record.defeated()&&record.state.getBoolean("RewardUnlocked"),"valid defeat unlocks core");String settled=record.state.toString();RuinAdventureService.death(death);check(settled.equals(record.state.toString()),"duplicate death cannot settle twice");
            }
            report.addProperty("passed",true);c.getSource().sendSuccess(()->Component.literal("Ruin flow transaction PASS: "+checks),false);
        }catch(Throwable ex){report.addProperty("passed",false);report.addProperty("error",ex.toString());org.slf4j.LoggerFactory.getLogger(RuinFlowAudit.class).error("Ruin flow audit",ex);c.getSource().sendFailure(Component.literal("Ruin flow FAIL: "+ex));}
        finally{
            for(var record:records)record.state.putBoolean("Defeated",true);entities.forEach(Entity::discard);
            for(var record:records)for(Entity item:l.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,record.bounds))if(!previousItems.contains(item.getUUID()))item.discard();
            RuinProtection.internal(()->{original.forEach((at,state)->l.setBlock(at,state,3));return true;});
            ((net.minecraft.world.level.saveddata.SavedData)RuinAdventureData.get(l)).setDirty();data.load(saved);p.setItemInHand(InteractionHand.MAIN_HAND,hand);p.setGameMode(mode);p.teleportTo(l,pos.x,pos.y,pos.z,yaw,pitch);p.setHealth(health);l.getServer().setDifficulty(difficulty,true);CultivationEvents.sync(p);
        }
        report.addProperty("checks",checks);report.addProperty("scope","server transactions; timers accelerated, no visual combat verification");try{var path=net.minecraftforge.fml.loading.FMLPaths.GAMEDIR.get().resolve("xiantu-qa/ruin-flow.json");java.nio.file.Files.createDirectories(path.getParent());java.nio.file.Files.writeString(path,report.toString());}catch(java.io.IOException ex){throw new java.io.UncheckedIOException(ex);}return report.get("passed").getAsBoolean()?1:0;
    }));}
}
