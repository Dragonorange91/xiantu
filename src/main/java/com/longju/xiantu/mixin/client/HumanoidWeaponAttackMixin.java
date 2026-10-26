package com.longju.xiantu.mixin.client;

import com.longju.xiantu.client.equipment.WeaponAttackAnimations;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public abstract class HumanoidWeaponAttackMixin {
    @Unique private boolean xiantu$customAttack;
    @Unique private float xiantu$rightX,xiantu$leftX,xiantu$rightZ,xiantu$leftZ;
    @Inject(method="setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V",at=@At("TAIL"))
    private void xiantu$swordStanding(LivingEntity entity,float limb,float amount,float age,float yaw,float pitch,CallbackInfo ci){
        if(!(entity.getVehicle() instanceof com.longju.xiantu.spell.SpiritSwordEntity))return;
        var model=(HumanoidModel<?>)(Object)this;
        model.rightLeg.xRot=0;model.leftLeg.xRot=0;model.rightLeg.yRot=0;model.leftLeg.yRot=0;
        model.rightLeg.zRot=.035F;model.leftLeg.zRot=-.035F;
    }
    @Inject(method="setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V",at=@At("HEAD"))
    private void xiantu$restoreShoulders(LivingEntity entity,float limb,float amount,float age,float yaw,float pitch,CallbackInfo ci){
        if(!xiantu$customAttack)return;
        var model=(HumanoidModel<?>)(Object)this;
        model.rightArm.x=xiantu$rightX;model.leftArm.x=xiantu$leftX;
        model.rightArm.z=xiantu$rightZ;model.leftArm.z=xiantu$leftZ;
        xiantu$customAttack=false;
    }
    @Inject(method="setupAttackAnimation",at=@At("HEAD"),cancellable=true)
    private void xiantu$weaponAttack(LivingEntity entity,float age,CallbackInfo ci){
        var model=(HumanoidModel<?>)(Object)this;
        xiantu$rightX=model.rightArm.x;xiantu$leftX=model.leftArm.x;
        xiantu$rightZ=model.rightArm.z;xiantu$leftZ=model.leftArm.z;
        if(WeaponAttackAnimations.thirdPerson(model,entity)){xiantu$customAttack=true;ci.cancel();}
    }
}
