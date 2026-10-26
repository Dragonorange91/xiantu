package com.longju.xiantu.mixin;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.RelativeMovement;
import com.longju.xiantu.instance.InstanceService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import java.util.Set;

@Mixin(ServerPlayer.class)
public abstract class InstanceTeleportMixin {
    @Inject(method="teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDFF)V",at=@At("HEAD"),cancellable=true)
    private void xiantu$teleport(ServerLevel level,double x,double y,double z,float yaw,float pitch,CallbackInfo ci){if(!InstanceService.mayTeleport((ServerPlayer)(Object)this,level,x,y,z))ci.cancel();}
    @Inject(method="teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDLjava/util/Set;FF)Z",at=@At("HEAD"),cancellable=true)
    private void xiantu$relative(ServerLevel level,double x,double y,double z,Set<RelativeMovement> relative,float yaw,float pitch,CallbackInfoReturnable<Boolean> ci){
        var p=(ServerPlayer)(Object)this;
        double ax=x+(relative.contains(RelativeMovement.X)?p.getX():0),ay=y+(relative.contains(RelativeMovement.Y)?p.getY():0),az=z+(relative.contains(RelativeMovement.Z)?p.getZ():0);
        if(!InstanceService.mayTeleport(p,level,ax,ay,az))ci.setReturnValue(false);
    }
}
