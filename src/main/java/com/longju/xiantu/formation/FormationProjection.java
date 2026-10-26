package com.longju.xiantu.formation;

import com.longju.xiantu.network.*;
import com.longju.xiantu.registry.ModItems;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import java.util.*;

/** Two deliberate clicks; no world or inventory mutation while aiming or locking. */
@Mod.EventBusSubscriber(modid="xiantu")
public final class FormationProjection {
    private record Session(int tier,net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,BlockPos locked,long expires,long lastClick){}
    private static final Map<UUID,Session> SESSIONS=new HashMap<>();
    private static final Map<UUID,Long> FINISHED=new HashMap<>();
    private static void send(ServerPlayer p,int tier,BlockPos locked){XiantuNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new FormationProjectionPacket(tier,locked));}
    public static boolean active(ServerPlayer p){return SESSIONS.containsKey(p.getUUID())||FINISHED.getOrDefault(p.getUUID(),-1L)>p.level().getGameTime();}
    public static void begin(ServerPlayer p,int tier){
        if(tier<1||tier>3||!p.getMainHandItem().is(ModItems.CULTIVATION_MANUAL.get())||p.isSpectator())return;
        FINISHED.remove(p.getUUID());SESSIONS.put(p.getUUID(),new Session(tier,p.level().dimension(),null,p.level().getGameTime()+12000,-100));p.closeContainer();send(p,tier,null);
        p.displayClientMessage(Component.literal("聚灵阵投影：右键固定，再右键搭建；潜行右键取消"),true);
    }
    public static void clear(ServerPlayer p){SESSIONS.remove(p.getUUID());FINISHED.put(p.getUUID(),p.level().getGameTime()+6);send(p,0,null);}
    public static boolean click(ServerPlayer p){
        var session=SESSIONS.get(p.getUUID());if(session==null)return active(p);
        long now=p.level().getGameTime();
        if(!p.isAlive()||p.isSpectator()||p.level().dimension()!=session.dimension||now>session.expires){clear(p);return true;}
        if(now-session.lastClick<6)return true;
        if(p.isShiftKeyDown()){clear(p);return true;}
        if(session.locked==null){
            var hit=p.pick(12,0,false);
            if(!(hit instanceof BlockHitResult block)||hit.getType()!=HitResult.Type.BLOCK||block.getDirection()!=Direction.UP){p.displayClientMessage(Component.literal("请选中可承托聚灵阵的地面"),true);return true;}
            var center=block.getBlockPos().above();String error=validate(p,session.tier,center);
            if(!error.isEmpty()){p.displayClientMessage(Component.literal(error),true);return true;}
            SESSIONS.put(p.getUUID(),new Session(session.tier,session.dimension,center,session.expires,now));send(p,session.tier,center);
            p.displayClientMessage(Component.literal("位置已固定，再次右键消耗材料搭建；潜行右键取消"),true);return true;
        }
        SESSIONS.put(p.getUUID(),new Session(session.tier,session.dimension,session.locked,session.expires,now));
        String error=place(p,session.tier,session.locked);
        if(error.isEmpty()){clear(p);p.displayClientMessage(Component.literal("聚灵阵已搭建"),true);}else p.displayClientMessage(Component.literal(error),true);
        return true;
    }
    @SubscribeEvent public static void tick(net.minecraftforge.event.TickEvent.PlayerTickEvent e){
        if(e.phase!=net.minecraftforge.event.TickEvent.Phase.END||!(e.player instanceof ServerPlayer p)||p.tickCount%20!=0)return;
        if(FINISHED.getOrDefault(p.getUUID(),Long.MAX_VALUE)<=p.level().getGameTime())FINISHED.remove(p.getUUID());
        var s=SESSIONS.get(p.getUUID());if(s!=null&&(p.level().getGameTime()>s.expires||p.level().dimension()!=s.dimension))clear(p);
    }
    public static String validate(ServerPlayer p,int tier,BlockPos center){
        var level=p.serverLevel();var plan=FormationPlan.blocks(tier);
        if(p.distanceToSqr(Vec3.atCenterOf(center))>256)return "距离过远，请靠近投影";
        if(!level.hasChunksAt(center.offset(-11,0,-11),center.offset(11,5,11)))return "投影区域尚未加载";
        for(var cell:plan.entrySet()){
            var pos=center.offset(cell.getKey());
            if(level.isOutsideBuildHeight(pos)||!level.getWorldBorder().isWithinBounds(pos)||!level.mayInteract(p,pos)||!p.mayBuild()
                ||(com.longju.xiantu.instance.InstanceService.isDimension(level.dimension().location())&&!com.longju.xiantu.instance.InstanceService.mayAct(p,pos))||com.longju.xiantu.worldgen.RuinProtection.protectedAt(level,pos))return "此处禁止搭建";
            var old=level.getBlockState(pos);
            if(!old.canBeReplaced()||!old.getFluidState().isEmpty()||level.getBlockEntity(pos)!=null)return "投影与现有建筑或液体重叠";
            if(cell.getKey().getY()==0&&!level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),Direction.UP))return "地面不平或缺少承托，请先整理场地";
            var shape=cell.getValue().getCollisionShape(level,pos);
            if(!shape.isEmpty()&&!level.getEntities((net.minecraft.world.entity.Entity)null,shape.bounds().move(pos),e->!e.isSpectator()&&e.isPickable()).isEmpty())return "请离开阵法占用区域";
        }
        if(!level.getBlockState(center.above(2)).canBeReplaced())return "蒲团上方需要留出空间";
        return "";
    }
    public static String place(ServerPlayer p,int tier,BlockPos center){
        String error=validate(p,tier,center);if(!error.isEmpty())return error;
        var inventory=p.getInventory();var copy=new ArrayList<net.minecraft.world.item.ItemStack>();inventory.items.forEach(s->copy.add(s.copy()));
        for(var need:FormationPlan.materials(tier).entrySet()){
            int left=need.getValue();for(var stack:copy)if(stack.is(need.getKey())){int n=Math.min(left,stack.getCount());stack.shrink(n);left-=n;if(left==0)break;}
            if(left>0)return "材料不足："+need.getKey().getDescription().getString()+"，缺少 "+left;
        }
        var level=p.serverLevel();var snapshots=new ArrayList<BlockSnapshot>();boolean complete=false;
        try{
            for(var cell:FormationPlan.blocks(tier).entrySet()){
                var pos=center.offset(cell.getKey());var snapshot=BlockSnapshot.create(level.dimension(),level,pos);snapshots.add(snapshot);
                if(!level.setBlock(pos,cell.getValue(),Block.UPDATE_CLIENTS)||net.minecraftforge.event.ForgeEventFactory.onBlockPlace(p,snapshot,Direction.UP))return "放置被保护规则阻止，材料未消耗";
            }
            if(!FormationStructure.isComplete(level,center,tier))return "结构校验未通过，材料未消耗";
            complete=true;
            for(int i=0;i<copy.size();i++)inventory.items.set(i,copy.get(i));inventory.setChanged();p.inventoryMenu.broadcastChanges();
            for(var cell:FormationPlan.blocks(tier).entrySet())level.updateNeighborsAt(center.offset(cell.getKey()),cell.getValue().getBlock());
            return "";
        }finally{if(!complete)for(int i=snapshots.size()-1;i>=0;i--)snapshots.get(i).restore(true,false);}
    }
    @SubscribeEvent(priority=EventPriority.HIGH) public static void block(PlayerInteractEvent.RightClickBlock e){
        if(e.getHand()==InteractionHand.MAIN_HAND&&e.getEntity() instanceof ServerPlayer p&&p.getMainHandItem().is(ModItems.CULTIVATION_MANUAL.get())&&active(p)){click(p);e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);}
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e){SESSIONS.remove(e.getEntity().getUUID());FINISHED.remove(e.getEntity().getUUID());}
    @SubscribeEvent public static void death(net.minecraftforge.event.entity.living.LivingDeathEvent e){if(e.getEntity() instanceof ServerPlayer p)clear(p);}
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent e){if(e.getEntity() instanceof ServerPlayer p)clear(p);}
    private FormationProjection(){}
}
