package com.longju.xiantu.worldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.event.level.*;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import java.util.function.Supplier;

/** A single mutation guard covers fire/fluid/other-mod direct setBlock paths as well as player actions. */
@Mod.EventBusSubscriber(modid="xiantu")
public final class RuinProtection {
    private static final ThreadLocal<Integer> INTERNAL=ThreadLocal.withInitial(()->0);
    public static <T>T internal(Supplier<T> body){int previous=INTERNAL.get();INTERNAL.set(previous+1);try{return body.get();}finally{INTERNAL.set(previous);}}
    public static boolean protectedAt(ServerLevel l,BlockPos p){
        if(INTERNAL.get()>0)return false;
        if(l.hasChunkAt(p)&&l.getBlockState(p).getBlock() instanceof RuinEncounterBlock)return true;
        for(var i:RuinAdventureData.get(l).at(p))if(i.boss()&&!i.defeated()&&i.bounds.contains(net.minecraft.world.phys.Vec3.atCenterOf(p)))return true;
        return false;
    }
    public static boolean lockedChest(ServerLevel l,BlockPos p){return INTERNAL.get()==0&&l.hasChunkAt(p)&&l.getBlockEntity(p)instanceof com.longju.xiantu.building.RewardChestBlockEntity c&&c.ruinLocked();}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void breaking(BlockEvent.BreakEvent e){if(e.getLevel()instanceof ServerLevel l&&(protectedAt(l,e.getPos())||lockedChest(l,e.getPos())))e.setCanceled(true);}
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void placing(BlockEvent.EntityPlaceEvent e){if(e.getLevel()instanceof ServerLevel l&&protectedAt(l,e.getPos()))e.setCanceled(true);}
    @SubscribeEvent public static void explosion(ExplosionEvent.Detonate e){if(e.getLevel()instanceof ServerLevel l)e.getAffectedBlocks().removeIf(p->protectedAt(l,p)||lockedChest(l,p));}
    @SubscribeEvent public static void piston(PistonEvent.Pre e){
        if(!(e.getLevel()instanceof ServerLevel l))return;
        if(protectedAt(l,e.getPos())||protectedAt(l,e.getFaceOffsetPos())){e.setCanceled(true);return;}
        var resolver=e.getStructureHelper();if(resolver==null||!resolver.resolve())return;
        var direction=e.getPistonMoveType().isExtend?e.getDirection():e.getDirection().getOpposite();
        for(var p:resolver.getToPush())if(protectedAt(l,p)||protectedAt(l,p.relative(direction))||lockedChest(l,p)){e.setCanceled(true);return;}
        for(var p:resolver.getToDestroy())if(protectedAt(l,p)||lockedChest(l,p)){e.setCanceled(true);return;}
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void interact(PlayerInteractEvent.RightClickBlock e){
        if(!(e.getLevel()instanceof ServerLevel l))return;
        if(RuinTrials.interact(l,e.getEntity(),e.getPos(),e.getHand())){e.setCanceled(true);e.setCancellationResult(InteractionResult.SUCCESS);return;}
        if(lockedChest(l,e.getPos())){e.getEntity().displayClientMessage(net.minecraft.network.chat.Component.literal("先完成本遗迹守护与试炼，核心宝函尚未解封"),true);e.setCanceled(true);e.setCancellationResult(InteractionResult.FAIL);}
    }
    @SubscribeEvent public static void target(LivingChangeTargetEvent e){
        if(!(e.getEntity().level()instanceof ServerLevel l)||!e.getEntity().getPersistentData().getBoolean(RuinAdventureService.BOSS))return;
        var n=e.getEntity().getPersistentData();if(!n.hasUUID(RuinEncounterService.SOURCE))return;var i=RuinAdventureData.get(l).find(n.getUUID(RuinEncounterService.SOURCE).toString());
        if(i!=null&&(!(e.getNewTarget()instanceof net.minecraft.server.level.ServerPlayer p)||!RuinAdventureService.legal(p,i)||!i.arena().contains(p.position())))e.setNewTarget(null);
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void strike(PlayerInteractEvent.LeftClickBlock e){if(e.getLevel()instanceof ServerLevel l&&RuinTrials.strike(l,e.getEntity(),e.getPos()))e.setCanceled(true);}
    private RuinProtection(){}
}
