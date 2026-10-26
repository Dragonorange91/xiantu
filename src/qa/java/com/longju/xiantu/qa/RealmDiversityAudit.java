package com.longju.xiantu.qa;

import com.longju.xiantu.instance.*;
import com.longju.xiantu.cultivation.*;
import com.longju.xiantu.gongfa.GongfaService;
import com.longju.xiantu.building.RewardChestBlockEntity;
import com.longju.xiantu.entity.*;
import com.longju.xiantu.medicine.*;
import com.longju.xiantu.registry.ModBlocks;
import net.minecraft.commands.Commands;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.*;

/** Actual player, seven online sessions; no production commands or fake camera. */
@Mod.EventBusSubscriber(modid="xiantu_qa")
public final class RealmDiversityAudit {
    private static Run r;
    private static final class Run {
        ServerPlayer p; ServerLevel original; Vec3 pos; float yaw,pitch,health,saturation;int food,index,phase,wait,checks;
        GameType mode; CompoundTag persistent,cultivation,returns,abilities;ListTag inventory;
        BlockPos entry; Map<BlockPos,BlockState> ground=new LinkedHashMap<>();InstanceLedger.Slot slot;
        boolean resume;long plants,logs,caveAir,stone,deepslate,vanillaOres,spiritOres,spiritLogs,herbs;int sample;BlockPos tree;
    }
    @SubscribeEvent public static void commands(RegisterCommandsEvent e){
        e.getDispatcher().register(Commands.literal("xiantudiversity").requires(s->s.hasPermission(2))
            .then(Commands.literal("start").executes(c->{start(c.getSource().getPlayerOrException());return 1;}))
            .then(Commands.literal("next").executes(c->{if(r!=null)r.resume=true;return 1;}))
            .then(Commands.literal("abort").executes(c->{restore();return 1;}))
            .then(Commands.literal("ecology").executes(c->{if(r==null||r.phase!=4||r.tree==null)return 0;var p=r.tree;InstanceService.internal(()->r.p.teleportTo(InstanceService.level(r.p.server,r.slot),p.getX()+8.5,p.getY()+5,p.getZ()+10.5,140,15));return 1;}))
            .then(Commands.literal("view").then(Commands.argument("zone",com.mojang.brigadier.arguments.IntegerArgumentType.integer(0,5)).executes(c->{
                if(r==null||r.phase!=4)return 0;var l=InstanceService.level(r.p.server,r.slot);var n=InstanceService.data(r.p.server).session(r.slot);var z=RealmLayout.get(r.p.server,n.getString("Theme")).zones().get(com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c,"zone"));
                InstanceService.internal(()->r.p.teleportTo(l,z.x()+20.5,z.y()+15,z.z()+25.5,140,24));return 1;
            }))));
    }
    private static void check(boolean ok,String label){r.checks++;if(!ok)throw new IllegalStateException(label);com.mojang.logging.LogUtils.getLogger().info("[Realm diversity check] {} {}",r.checks,label);}
    private static void start(ServerPlayer p){
        if(r!=null||p.level().dimension()!=Level.OVERWORLD||!p.server.getWorldPath(LevelResource.ROOT).normalize().getFileName().toString().startsWith("Xiantu-QA"))return;
        var d=InstanceService.data(p.server);if(d.blocked()||d.ledger().activeRealm()!=null)return;
        r=new Run();r.p=p;r.original=p.serverLevel();r.pos=p.position();r.yaw=p.getYRot();r.pitch=p.getXRot();r.health=p.getHealth();r.mode=p.gameMode.getGameModeForPlayer();r.food=p.getFoodData().getFoodLevel();r.saturation=p.getFoodData().getSaturationLevel();r.persistent=p.getPersistentData().copy();r.returns=d.runtime().getCompound("Returns").copy();r.inventory=p.getInventory().save(new ListTag());r.abilities=new CompoundTag();p.getAbilities().addSaveData(r.abilities);r.cultivation=new CompoundTag();((PlayerCultivationData)GongfaService.data(p)).save(r.cultivation);
        try{p.setGameMode(GameType.CREATIVE);var data=GongfaService.data(p);data.setCultivationUnlocked(true);data.setRealmStage(0,0);CultivationService.applyRealmAttributes(p,data);CultivationEvents.sync(p);
            var cp=p.chunkPosition();r.entry=new BlockPos(cp.getMinBlockX()+8,301,cp.getMinBlockZ()+8);
            InstanceService.internal(()->{for(int x=-6;x<=6;x++)for(int z=-5;z<=5;z++){var at=r.entry.offset(x,-1,z);r.ground.put(at,r.original.getBlockState(at));r.original.setBlockAndUpdate(at,Blocks.STONE.defaultBlockState());}p.teleportTo(r.original,r.entry.getX()+.5,301,r.entry.getZ()+3.5,180,0);});create();
        }catch(Exception|LinkageError ex){fail(ex);}
    }
    private static void create(){
        String theme=r.index==0?"wood":InstanceTerrain.THEMES[r.index-1];
        var a=new InstanceRules.Parameters(InstanceRules.Abundance.NORMAL,InstanceRules.Size.COMPACT,16,40,false,true);
        String result=InstanceService.createRealm(r.p.server,r.entry,a,0,r.index==0,theme);var d=InstanceService.data(r.p.server);
        r.slot=d.slots().stream().filter(s->s.state()==InstanceLedger.State.PREPARING&&s.kind()==InstanceLedger.Kind.REALM).findFirst().orElseThrow(()->new IllegalStateException(result));r.phase=1;r.wait=0;
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e){
        if(e.phase!=TickEvent.Phase.END||r==null)return;
        try{if(++r.wait>24000)throw new IllegalStateException("diversity audit timeout");var p=r.p;var d=InstanceService.data(p.server);var n=d.session(r.slot);var l=InstanceService.level(p.server,r.slot);
            if(r.phase==1){if(d.ledger().find(r.slot.dimension()).orElseThrow().state()!=InstanceLedger.State.ACTIVE)return;
                check(!n.getBoolean("SoulReward"),"Qi-entry realm suppresses overrank soul reward even when requested");
                check(n.getBoolean("Natural")== (r.index==0),"requested natural/ruins mode generated");
                check(r.index==0||n.getInt("Diameter")>=32,"spread ruins fit session border");
                String opening=InstanceService.opening(n);check(opening.contains("练气至筑基"),"announcement reflects Qi to Foundation cap: "+opening);
                check(InstanceService.enterRealm(p,r.slot).contains("已进入"),"real Qi player admitted");
                if(r.index>0){var layout=RealmLayout.get(p.server,n.getString("Theme"));int chests=0;
                    var allowed=new HashSet<String>(List.of("fan_ling_shi","ling_jing","beast_bone"));MedicineCatalog.HERBS.stream().filter(h->h.tier()<=2).forEach(h->allowed.add(h.registryId()));
                    for(var at:layout.rewards().values())if(l.getBlockEntity(at) instanceof Container c){chests++;check(l.getBlockState(at).is(ModBlocks.RUIN_REWARD_CHEST.get())&&c instanceof RewardChestBlockEntity,"custom treasure chest is real container");int occupied=0,highest=-1;for(int slot=0;slot<c.getContainerSize();slot++)if(!c.getItem(slot).isEmpty()){occupied++;highest=slot;var stack=c.getItem(slot);check(allowed.contains(ForgeRegistries.ITEMS.getKey(stack.getItem()).getPath()),"all sampled loot within Qi/Foundation materials: "+stack);if(stack.is(com.longju.xiantu.registry.ModItems.BEAST_BONE.get()))check(com.longju.xiantu.item.BeastMaterialItem.grade(stack)>=1&&com.longju.xiantu.item.BeastMaterialItem.grade(stack)<=2,"reward bone carries usable bounded grade");}check(occupied>0&&highest>=occupied,"loot placed in scattered slots, not sequential prefix");}
                    check(chests==3,"normal abundance creates core/quarry/herb treasures");
                    for(var z:layout.zones())check(!l.getBlockState(z.at().below()).isAir(),"district foundation exists: "+z.id());
                    for(var entity:l.getAllEntities())if(entity instanceof net.minecraft.world.entity.Mob mob&&mob.getPersistentData().hasUUID("xiantu_realm_instance"))check(mob.getPersistentData().getInt(BeastResourceDrops.REALM)<=1,"guardian never above Foundation");
                }
                r.sample=0;r.phase=2;r.wait=0;return;
            }
            if(r.phase==2){
                if(r.index==0&&r.sample<32){int cx=-6+r.sample%8,cz=-6+r.sample/8;var chunk=l.getChunk(cx,cz);var at=new BlockPos.MutableBlockPos();
                    for(int x=cx*16;x<cx*16+16;x++)for(int z=cz*16;z<cz*16+16;z++){int surface=l.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR,x,z);for(int y=-56;y<Math.min(160,surface+18);y++){
                        var state=l.getBlockState(at.set(x,y,z));if(state.is(net.minecraft.tags.BlockTags.LOGS))r.logs++;if(state.getBlock() instanceof BushBlock)r.plants++;if(y<30&&y<surface-8&&state.isAir())r.caveAir++;if(state.is(Blocks.STONE))r.stone++;if(state.is(Blocks.DEEPSLATE))r.deepslate++;
                        var id=ForgeRegistries.BLOCKS.getKey(state.getBlock());if(state.getBlock() instanceof com.longju.xiantu.wood.SpiritLogBlock){r.spiritLogs++;if(r.tree==null)r.tree=at.immutable();check(id.getPath().startsWith("qing_wen_elm_")||id.getPath().startsWith("chi_xin_birch_"),"natural spirit tree stays tier one/two");}if(state.getBlock() instanceof MedicinePlantBlock)r.herbs++;if(id!=null&&id.getPath().endsWith("_ore")){if(id.getNamespace().equals("minecraft"))r.vanillaOres++;else if(id.getNamespace().equals("xiantu"))r.spiritOres++;}
                    }}r.sample++;return;
                }
                if(r.index==0){check(r.logs>0&&r.plants>0,"native forest and plants generated: "+r.logs+"/"+r.plants);check(r.caveAir>0&&r.stone>0&&r.deepslate>0,"native caves/stone/deepslate: "+r.caveAir+"/"+r.stone+"/"+r.deepslate);check(r.vanillaOres>0&&r.spiritOres>0,"underground vanilla/spirit ore blocks: "+r.vanillaOres+"/"+r.spiritOres);check(r.spiritLogs>0,"natural realm has visible spirit trees: "+r.spiritLogs);com.mojang.logging.LogUtils.getLogger().info("[Realm diversity ecology] chunks=32 spiritLogs={} herbs={} tree={}",r.spiritLogs,r.herbs,r.tree);}
                r.phase=4;r.wait=0;com.mojang.logging.LogUtils.getLogger().info("[Realm diversity QA] VIEW_READY index={} theme={} checks={}",r.index,n.getString("Theme"),r.checks);p.sendSystemMessage(net.minecraft.network.chat.Component.literal("秘境巡视就绪 "+r.index+" "+n.getString("Theme")));return;
            }
            if(r.phase==4){if(!r.resume)return;r.resume=false;InstanceService.close(p.server,r.slot,"QA");r.phase=5;r.wait=0;return;}
            if(r.phase==5){if(d.ledger().activeRealm()!=null)return;check(p.level().dimension()==Level.OVERWORLD,"online close returns actual player and releases slot");if(++r.index<7){create();return;}
                com.mojang.logging.LogUtils.getLogger().info("[Realm diversity QA] PASS {}",r.checks);p.sendSystemMessage(net.minecraft.network.chat.Component.literal("[Realm diversity QA] PASS "+r.checks));restore();
            }
        }catch(Exception|LinkageError ex){fail(ex);}
    }
    private static void fail(Throwable ex){com.mojang.logging.LogUtils.getLogger().error("[Realm diversity QA] FAIL",ex);restore();}
    private static void restore(){var v=r;if(v==null)return;r=null;try{
        if(v.slot!=null)InstanceService.close(v.p.server,v.slot,"QA结束");InstanceService.returnHome(v.p);v.p.closeContainer();InstanceService.internal(()->{v.ground.forEach(v.original::setBlockAndUpdate);v.p.teleportTo(v.original,v.pos.x,v.pos.y,v.pos.z,v.yaw,v.pitch);});
        v.p.getPersistentData().getAllKeys().clear();v.p.getPersistentData().merge(v.persistent);v.p.getInventory().load(v.inventory);((PlayerCultivationData)GongfaService.data(v.p)).load(v.cultivation);v.p.setGameMode(v.mode);v.p.getAbilities().loadSaveData(v.abilities);v.p.onUpdateAbilities();v.p.getFoodData().setFoodLevel(v.food);v.p.getFoodData().setSaturation(v.saturation);GongfaService.refresh(v.p);CultivationService.applyRealmAttributes(v.p,GongfaService.data(v.p));v.p.setHealth(Math.min(v.health,v.p.getMaxHealth()));var d=InstanceService.data(v.p.server);d.runtime().put("Returns",v.returns);((net.minecraft.world.level.saveddata.SavedData)d).setDirty();CultivationEvents.sync(v.p);v.p.inventoryMenu.broadcastChanges();
    }catch(Exception|LinkageError ex){com.mojang.logging.LogUtils.getLogger().error("Diversity QA restore failed",ex);}}
    private RealmDiversityAudit(){}
}
