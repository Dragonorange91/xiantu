package com.longju.xiantu.instance;

import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** A reversible altar: persist original/expected states before touching overworld blocks. */
public final class RealmPortal {
    private record Pending(net.minecraft.resources.ResourceKey<Level> dimension,BlockPos cell){}
    private static final Map<UUID,Pending> PENDING=new HashMap<>();
    private static final String DWELL="xiantu_portal_dwell",LAST="xiantu_portal_last",COOL="xiantu_portal_cooldown";
    public static boolean site(ServerLevel l,BlockPos at){
        for(int x=-5;x<=5;x++)for(int z=-4;z<=4;z++){
            var base=at.offset(x,-1,z);
            if(!l.hasChunkAt(base)||!l.getWorldBorder().isWithinBounds(base)||l.getBlockEntity(base)!=null)return false;
            if(!l.getBlockState(base).is(net.minecraft.tags.BlockTags.DIRT)&&!l.getBlockState(base).is(net.minecraft.tags.BlockTags.BASE_STONE_OVERWORLD))return false;
            for(int y=0;y<=6;y++){var p=at.offset(x,y,z);if(l.getBlockEntity(p)!=null||!l.getBlockState(p).canBeReplaced()||!l.getFluidState(p).isEmpty())return false;}
            if(com.longju.xiantu.worldgen.RuinProtection.protectedAt(l,base))return false;
        }return true;
    }
    private static Map<BlockPos,BlockState> altar(BlockPos at,boolean active){
        var out=new LinkedHashMap<BlockPos,BlockState>();
        for(int x=-5;x<=5;x++)for(int z=-3;z<=3;z++)out.put(at.offset(x,-1,z),(Math.abs(x)==5||Math.abs(z)==3?Blocks.CHISELED_STONE_BRICKS:Blocks.STONE_BRICKS).defaultBlockState());
        for(int x=-4;x<=4;x++)out.put(at.offset(x,-1,4),Blocks.STONE_BRICK_STAIRS.defaultBlockState().setValue(StairBlock.FACING,Direction.NORTH));
        // The authored ring owns the silhouette; only its two grounded pedestals
        // have full-block collision. Do not leave the old rectangular lintel inside it.
        for(int x:new int[]{-3,3})out.put(at.offset(x,0,0),Blocks.POLISHED_DEEPSLATE.defaultBlockState());
        for(int x:new int[]{-4,4}){out.put(at.offset(x,0,2),Blocks.CHISELED_STONE_BRICKS.defaultBlockState());out.put(at.offset(x,1,2),InstanceDecor.LANTERN.get().defaultBlockState());}
        for(int x=-2;x<=2;x++)for(int y=0;y<4;y++)out.put(at.offset(x,y,0),(x==0&&y==0?InstanceRegistry.PORTAL:InstanceRegistry.FIELD).get().defaultBlockState().setValue(BlockStateProperties.POWERED,active));
        return out;
    }
    public static void place(ServerLevel l,BlockPos at,CompoundTag n){
        if(!site(l,at))throw new IllegalStateException("Portal altar needs an unobstructed 11x9 site");
        var plan=altar(at,false);var snapshot=new ListTag();
        plan.forEach((pos,state)->{var e=new CompoundTag();e.putLong("Pos",pos.asLong());e.put("Before",NbtUtils.writeBlockState(l.getBlockState(pos)));e.put("Placed",NbtUtils.writeBlockState(state));snapshot.add(e);});
        n.put("PortalBlocks",snapshot);InstanceService.data(l.getServer()).setDirty();l.getServer().overworld().getDataStorage().save();
        InstanceService.internal(()->plan.forEach((pos,state)->l.setBlockAndUpdate(pos,state)));
    }
    public static void activate(ServerLevel l,BlockPos at){InstanceService.internal(()->{for(int x=-2;x<=2;x++)for(int y=0;y<4;y++){var p=at.offset(x,y,0);var state=l.getBlockState(p);if(state.getBlock() instanceof RealmPortalBlock)l.setBlockAndUpdate(p,state.setValue(BlockStateProperties.POWERED,true));}});}
    public static void exit(ServerLevel l,BlockPos at){InstanceService.internal(()->altar(at,true).forEach((pos,state)->l.setBlockAndUpdate(pos,state)));}
    public static void remove(ServerLevel l,CompoundTag n){
        InstanceService.internal(()->{for(var raw:n.getList("PortalBlocks",Tag.TAG_COMPOUND)){var e=(CompoundTag)raw;var at=BlockPos.of(e.getLong("Pos"));var expected=NbtUtils.readBlockState(l.holderLookup(net.minecraft.core.registries.Registries.BLOCK),e.getCompound("Placed"));var current=l.getBlockState(at);
            if(current.equals(expected)||current.getBlock() instanceof RealmPortalBlock&&expected.getBlock() instanceof RealmPortalBlock)l.setBlockAndUpdate(at,NbtUtils.readBlockState(l.holderLookup(net.minecraft.core.registries.Registries.BLOCK),e.getCompound("Before")));
        }});
    }
    public static void touch(ServerPlayer p,BlockPos cell){
        if(!p.isAlive()||p.isSpectator()||p.isPassenger()||p.isVehicle())return;
        var tag=p.getPersistentData();long time=p.server.overworld().getGameTime();if(time<tag.getLong(COOL))return;
        if(tag.getLong(LAST)==time)return;
        int dwell=tag.getLong(LAST)==time-1?tag.getInt(DWELL)+1:1;tag.putLong(LAST,time);tag.putInt(DWELL,dwell);if(dwell<4)return;
        tag.putLong(COOL,time+60);tag.remove(DWELL);PENDING.put(p.getUUID(),new Pending(p.level().dimension(),cell.immutable()));
    }
    /** Defer world changes until all entity movement/collision callbacks have completed. */
    public static void tick(net.minecraft.server.MinecraftServer server){
        for(var e:List.copyOf(PENDING.entrySet())){PENDING.remove(e.getKey());var p=server.getPlayerList().getPlayer(e.getKey());var q=e.getValue();if(p==null||!p.isAlive()||p.level().dimension()!=q.dimension()||p.distanceToSqr(Vec3.atCenterOf(q.cell()))>9)continue;enter(p,q.cell());}
    }
    private static void enter(ServerPlayer p,BlockPos cell){String result="";
        if(InstanceService.temporary(p.level())){var s=InstanceService.slot(p.serverLevel());if(s!=null&&cell.distSqr(InstanceService.corePos(s))<36)InstanceService.returnHome(p);return;}
        if(p.level().dimension()!=Level.OVERWORLD)return;
        var d=InstanceService.data(p.server);for(var s:d.slots())if(s.kind()==InstanceLedger.Kind.REALM&&s.state()==InstanceLedger.State.ACTIVE&&cell.distSqr(BlockPos.of(d.session(s).getLong("Entry")))<36){result=InstanceService.enterRealm(p,s);break;}
        if(!result.isEmpty()&&!result.startsWith("已进入"))p.displayClientMessage(Component.literal(result),true);
    }
    private RealmPortal(){}
}
