package com.longju.xiantu.mixin;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
import com.longju.xiantu.worldgen.RuinProtection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class RuinBlockMutationMixin {
    @Inject(method="setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z",at=@At("HEAD"),cancellable=true)
    private void xiantu$protectRuin(BlockPos pos,BlockState state,int flags,int recursion,CallbackInfoReturnable<Boolean> ci){
        if((Object)this instanceof ServerLevel level&&!com.longju.xiantu.instance.InstanceService.writable(level,pos)){ci.setReturnValue(false);return;}
        if((Object)this instanceof ServerLevel level&&(RuinProtection.protectedAt(level,pos)||RuinProtection.lockedChest(level,pos))){
            var old=level.getBlockState(pos);var block=old.getBlock();
            if(state.is(block)&&(block instanceof net.minecraft.world.level.block.DoorBlock||block instanceof net.minecraft.world.level.block.TrapDoorBlock||block instanceof net.minecraft.world.level.block.FenceGateBlock))return;
            ci.setReturnValue(false);
        }
    }
}
