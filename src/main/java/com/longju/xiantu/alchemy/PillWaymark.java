package com.longju.xiantu.alchemy;
import com.longju.xiantu.registry.ModBlocks;
import com.longju.xiantu.registry.ModItems;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.item.*;
import net.minecraft.network.chat.Component;
import net.minecraftforge.registries.RegistryObject;

public final class PillWaymark extends Block {
    public static final RegistryObject<Block> BLOCK=ModBlocks.BLOCKS.register("dimension_waymark",PillWaymark::new);
    public static void bootstrap(){ModItems.ITEMS.register("dimension_waymark",()->new BlockItem(BLOCK.get(),new Item.Properties()));}
    private PillWaymark(){super(Properties.copy(Blocks.CHISELED_STONE_BRICKS).strength(4));}
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,net.minecraft.world.entity.LivingEntity placer,ItemStack stack){
        if(placer instanceof ServerPlayer p)WaymarkData.get(p.serverLevel()).put(p.serverLevel(),pos,p.getUUID());
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
        if(!state.is(next.getBlock())&&level instanceof net.minecraft.server.level.ServerLevel server)WaymarkData.get(server).remove(server,pos);
        super.onRemove(state,level,pos,next,moving);
    }
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(player instanceof ServerPlayer p){
            if(p.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)>9 || !WaymarkData.get(p.serverLevel()).owned(p.serverLevel(),pos,p.getUUID())){
                p.sendSystemMessage(Component.literal("只能亲自绑定3格内本人放置的界标"));return InteractionResult.FAIL;
            }
            var tag=MedicineService.data(p).medicine().state();
            tag.putUUID("waymark_world",WaymarkData.get(p.serverLevel()).world());tag.putUUID("waymark_owner",p.getUUID());
            tag.putString("waymark_dimension",level.dimension().location().toString());tag.putLong("waymark_position",pos.asLong());
            p.sendSystemMessage(Component.literal("已绑定本人界标；破界丹只传送到已生成且安全的跨维落点"));}
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
