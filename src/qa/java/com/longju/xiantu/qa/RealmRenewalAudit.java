package com.longju.xiantu.qa;

import com.longju.xiantu.instance.*;
import com.longju.xiantu.cultivation.*;
import com.longju.xiantu.gongfa.GongfaService;
import com.longju.xiantu.entity.BeastRealmService;
import net.minecraft.commands.Commands;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Real-player regression for both content modes and native entityInside portal admission/exit. */
@Mod.EventBusSubscriber(modid="xiantu_qa")
public final class RealmRenewalAudit {
    private static Run run;
    private static final class Run {
        ServerPlayer p;ServerLevel original;Vec3 pos;float yaw,pitch,health;GameType mode;int food;float saturation;
        CompoundTag cultivation,persistent,returns;ListTag inv;Map<BlockPos,BlockState> ground=new LinkedHashMap<>();
        BlockPos entry;InstanceLedger.Slot slot;int phase,wait,checks;boolean tour,resume;
    }
    @SubscribeEvent public static void commands(RegisterCommandsEvent e){e.getDispatcher().register(Commands.literal("xianturealmqa").requires(s->s.hasPermission(2))
        .then(Commands.literal("start").executes(c->{start(c.getSource().getPlayerOrException());return 1;}))
        .then(Commands.literal("tour").executes(c->{start(c.getSource().getPlayerOrException());if(run!=null)run.tour=true;return 1;}))
        .then(Commands.literal("walk").executes(c->{if(run!=null&&run.phase==20){run.phase=2;run.wait=0;}return 1;}))
        .then(Commands.literal("next").executes(c->{if(run!=null){run.resume=true;run.wait=0;}return 1;}))
        .then(Commands.literal("continue").executes(c->{if(run!=null){run.tour=false;run.wait=0;}return 1;}))
        .then(Commands.literal("abort").executes(c->{restore();return 1;}))
        .then(Commands.literal("view").then(Commands.argument("site",com.mojang.brigadier.arguments.IntegerArgumentType.integer(0,3)).executes(c->{
            if(run==null||(run.phase!=7&&run.phase!=21)||!run.tour)return 0;double[][] sites={{64.5,69,88,0,7},{54,72,31,0,20},{-64,69,29,0,30},{-13,69,72,-30,20}};if(run.phase==21)sites=new double[][]{{64,92,-64,140,23},{-68,91,64,-135,22},{80,90,82,40,20},{.5,70,7,180,12}};var v=sites[com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c,"site")];var p=run.p;InstanceService.internal(()->p.teleportTo(p.serverLevel(),v[0],v[1],v[2],(float)v[3],(float)v[4]));return 1;}))));}
    private static void check(boolean b,String reason){run.checks++;if(!b)throw new IllegalStateException(reason);com.mojang.logging.LogUtils.getLogger().info("[Realm renewal check] {} {}",run.checks,reason);}
    private static void start(ServerPlayer p){
        if(run!=null||!p.server.getWorldPath(LevelResource.ROOT).normalize().getFileName().toString().startsWith("Xiantu-QA")||p.level().dimension()!=Level.OVERWORLD)return;
        var d=InstanceService.data(p.server);if(d.blocked()||d.ledger().activeRealm()!=null){p.sendSystemMessage(net.minecraft.network.chat.Component.literal("请先关闭当前秘境"));return;}
        var r=new Run();run=r;r.p=p;r.original=p.serverLevel();r.pos=p.position();r.yaw=p.getYRot();r.pitch=p.getXRot();r.health=p.getHealth();r.mode=p.gameMode.getGameModeForPlayer();r.food=p.getFoodData().getFoodLevel();r.saturation=p.getFoodData().getSaturationLevel();r.persistent=p.getPersistentData().copy();r.returns=d.runtime().getCompound("Returns").copy();r.inv=p.getInventory().save(new ListTag());r.cultivation=new CompoundTag();((PlayerCultivationData)GongfaService.data(p)).save(r.cultivation);
        try{
            p.setGameMode(GameType.CREATIVE);p.getInventory().clearContent();var data=GongfaService.data(p);data.setCultivationUnlocked(false);data.setRealmStage(0,0);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(InstanceRegistry.REALM_TEST.get()));
            Item test=InstanceRegistry.REALM_TEST.get();test.use(p.level(),p,InteractionHand.MAIN_HAND);check(data.cultivationUnlocked()&&data.majorRealm()==0,"mortal test use unlocks Qi one, no skipped major realm");
            p.getCooldowns().removeCooldown(test);test.use(p.level(),p,InteractionHand.MAIN_HAND);check(data.majorRealm()==1&&data.stage()==0,"each second use advances exactly one major realm");check(p.getMainHandItem().getCount()==1,"test jade reusable");
            data.setRealmStage(8,0);p.getCooldowns().removeCooldown(test);test.use(p.level(),p,InteractionHand.MAIN_HAND);check(data.majorRealm()==8,"test jade clamps highest realm");data.setRealmStage(1,0);CultivationService.applyRealmAttributes(p,data);CultivationEvents.sync(p);
            var cp=p.chunkPosition();r.entry=new BlockPos(cp.getMinBlockX()+8,301,cp.getMinBlockZ()+8);
            InstanceService.internal(()->{for(int x=-6;x<=6;x++)for(int z=-5;z<=5;z++){var at=r.entry.offset(x,-1,z);r.ground.put(at,r.original.getBlockState(at));r.original.setBlockAndUpdate(at,Blocks.STONE.defaultBlockState());}p.teleportTo(r.original,r.entry.getX()+.5,301,r.entry.getZ()+3.5,180,0);});
            check(RealmPortal.site(r.original,r.entry),"loaded clear altar site");create(true);r.phase=1;
        }catch(Exception|LinkageError ex){fail(ex);}
    }
    private static void create(boolean natural){
        var r=run;var a=new InstanceRules.Parameters(InstanceRules.Abundance.RICH,InstanceRules.Size.COMPACT,16,30,true,false);
        String result=InstanceService.createRealm(r.p.server,r.entry,a,0,natural);var d=InstanceService.data(r.p.server);r.slot=d.slots().stream().filter(s->s.kind()==InstanceLedger.Kind.REALM&&s.state()==InstanceLedger.State.PREPARING).findFirst().orElseThrow(()->new IllegalStateException(result));
        check(d.session(r.slot).getBoolean("Natural")==natural,"explicit natural/ruins mode persisted");
        check(r.original.getBlockState(r.entry).is(InstanceRegistry.PORTAL.get()),"altar marker placed");
    }
    private static void atPortal(ServerLevel l,BlockPos at){var p=run.p;InstanceService.internal(()->p.teleportTo(l,at.getX()+.5,at.getY(),at.getZ()+.5,180,0));}
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e){
        if(e.phase!=TickEvent.Phase.END||run==null)return;var r=run;var p=r.p;
        try{
            if(++r.wait>(r.tour?18000:6000))throw new IllegalStateException("realm renewal QA timeout");var d=InstanceService.data(p.server);var n=d.session(r.slot);
            if(r.phase==1){
                if(d.ledger().find(r.slot.dimension()).orElseThrow().state()!=InstanceLedger.State.ACTIVE)return;
                check(n.getInt("BuildIndex")==0,"natural realm has no mandatory court blueprint");String opening=InstanceService.opening(n);check(opening.contains("人数：")&&opening.contains("修为：")&&opening.contains("坐标：")&&opening.contains("大小：")&&opening.length()<140,"compact opening contains only requested fields");
                var l=InstanceService.level(p.server,r.slot);for(int cx:new int[]{-6,-5,4,5})for(int cz:new int[]{-6,-5,4,5})l.getChunk(cx,cz);int plants=0,logs=0,ores=0;
                for(int x=-96;x<96;x++)for(int z=-96;z<96;z++){if(Math.abs(x)<64||Math.abs(z)<64)continue;int h=l.getHeight( net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z);for(int y=20;y<=h+9;y++){var state=l.getBlockState(new BlockPos(x,y,z));if(state.is(net.minecraft.tags.BlockTags.LOGS))logs++;if(state.is(Blocks.GRASS)||state.is(Blocks.FERN)||state.is(net.minecraft.tags.BlockTags.FLOWERS))plants++;var id=net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(state.getBlock());if(id!=null&&id.getNamespace().equals("xiantu")&&id.getPath().contains("ore"))ores++;}}
                check(plants>100&&logs>10&&ores>10,"chunk generated flora/trees/spirit ores: "+plants+"/"+logs+"/"+ores);
                com.mojang.logging.LogUtils.getLogger().info("[Realm renewal ecology] plants="+plants+", logs="+logs+", ores="+ores);
                if(r.tour){r.phase=20;p.sendSystemMessage(net.minecraft.network.chat.Component.literal("QA：祭坛巡视就绪，next进入自然秘境"));return;}atPortal(r.original,r.entry);r.phase=2;return;
            }
            if(r.phase==20){if(!r.resume)return;r.resume=false;atPortal(r.original,r.entry);r.phase=2;return;}
            if(r.phase==21){if(!r.resume)return;r.resume=false;atPortal(p.serverLevel(),InstanceService.corePos(r.slot));r.phase=3;return;}
            if(r.phase==2){if(!InstanceService.temporary(p.level()))return;
                check(n.getCompound("Members").size()==1,"native walk-in collision admits exactly one member");check(p.blockPosition().distSqr(new BlockPos(0,65,0))<9,"native walk-in reaches safe arrival: "+p.position());
                if(r.tour){r.phase=21;p.sendSystemMessage(net.minecraft.network.chat.Component.literal("QA：自然秘境巡视就绪，next开始返回验证"));return;}atPortal(p.serverLevel(),InstanceService.corePos(r.slot));r.phase=3;return;
            }
            if(r.phase==3){if(p.level().dimension()!=Level.OVERWORLD)return;check(n.getCompound("Members").isEmpty(),"exit portal releases capacity");check(Math.abs(p.getZ()-(r.entry.getZ()+3.5))<1,"return outside portal avoids idle reentry");
                InstanceService.close(p.server,r.slot,"QA");check(r.original.getBlockState(r.entry).isAir(),"altar marker removed on close");check(r.original.getBlockState(r.entry.below()).is(Blocks.STONE),"altar restores original ground");r.phase=4;return;
            }
            if(r.phase==4){if(d.ledger().activeRealm()!=null)return;create(false);r.phase=5;return;}
            if(r.phase==5){if(d.ledger().find(r.slot.dimension()).orElseThrow().state()!=InstanceLedger.State.ACTIVE)return;
                var l=InstanceService.level(p.server,r.slot);check(l.getBlockEntity(RealmLayout.get(p.server,n.getString("Theme")).rewards().get("core")) instanceof Container,"ruins reward and partitioned hall exist");check(RealmLayout.get(p.server,n.getString("Theme")).blocks().stream().anyMatch(c->c.state().equals("xiantu:realm_incense_reliquary")&&l.getBlockState(c.at()).is(InstanceDecor.RELIQUARY.get())),"MCP-authored interior decoration placed");
                atPortal(r.original,r.entry);r.phase=6;return;
            }
            if(r.phase==6){if(!InstanceService.temporary(p.level()))return;check(!n.getBoolean("Natural"),"second online generation uses ruins mode");
                if(r.tour){r.phase=7;com.mojang.logging.LogUtils.getLogger().info("[Realm renewal QA] TOUR_READY "+r.checks);p.sendSystemMessage(net.minecraft.network.chat.Component.literal("秘境QA巡视就绪"));return;}
                InstanceService.close(p.server,r.slot,"QA");r.phase=8;return;
            }
            if(r.phase==7){if(r.tour)return;InstanceService.close(p.server,r.slot,"QA");r.phase=8;return;}
            if(r.phase==8){if(d.ledger().activeRealm()!=null)return;check(p.level().dimension()==Level.OVERWORLD,"closing returns player");com.mojang.logging.LogUtils.getLogger().info("[Realm renewal QA] PASS "+r.checks);p.sendSystemMessage(net.minecraft.network.chat.Component.literal("[Realm renewal QA] PASS "+r.checks));restore();}
        }catch(Exception|LinkageError ex){fail(ex);}
    }
    private static void fail(Throwable ex){com.mojang.logging.LogUtils.getLogger().error("[Realm renewal QA] FAIL "+(run==null?0:run.checks),ex);if(run!=null)run.p.sendSystemMessage(net.minecraft.network.chat.Component.literal("秘境QA失败："+ex));restore();}
    private static void restore(){var r=run;if(r==null)return;run=null;try{
        if(r.slot!=null)InstanceService.close(r.p.server,r.slot,"QA结束");InstanceService.returnHome(r.p);r.p.closeContainer();InstanceService.internal(()->{r.ground.forEach(r.original::setBlockAndUpdate);r.p.teleportTo(r.original,r.pos.x,r.pos.y,r.pos.z,r.yaw,r.pitch);});
        r.p.getPersistentData().getAllKeys().clear();r.p.getPersistentData().merge(r.persistent);r.p.getInventory().load(r.inv);((PlayerCultivationData)GongfaService.data(r.p)).load(r.cultivation);r.p.setGameMode(r.mode);r.p.getFoodData().setFoodLevel(r.food);r.p.getFoodData().setSaturation(r.saturation);GongfaService.refresh(r.p);CultivationService.applyRealmAttributes(r.p,GongfaService.data(r.p));r.p.setHealth(Math.min(r.health,r.p.getMaxHealth()));var d=InstanceService.data(r.p.server);d.runtime().put("Returns",r.returns);((net.minecraft.world.level.saveddata.SavedData)d).setDirty();CultivationEvents.sync(r.p);r.p.inventoryMenu.broadcastChanges();
    }catch(Exception|LinkageError ex){ex.printStackTrace();}}
    private RealmRenewalAudit(){}
}
