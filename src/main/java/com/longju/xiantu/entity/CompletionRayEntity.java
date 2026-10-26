package com.longju.xiantu.entity;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.ai.control.SmoothSwimmingMoveControl;
import net.minecraft.world.entity.ai.control.SmoothSwimmingLookControl;
import net.minecraft.world.entity.ai.navigation.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
/** Peaceful ray: swimming and panic, never an invisible land-walking fish. */
public final class CompletionRayEntity extends WaterAnimal implements CompletionAnimated {
    public CompletionRayEntity(EntityType<? extends CompletionRayEntity> type,Level level){
        super(type,level);moveControl=new SmoothSwimmingMoveControl(this,85,10,.07F,.1F,true);lookControl=new SmoothSwimmingLookControl(this,10);
    }
    @Override protected PathNavigation createNavigation(Level level){return new WaterBoundPathNavigation(this,level);}
    @Override protected boolean shouldDespawnInPeaceful(){return false;}
    // Shared ecology service provides the explicit thirty-second out-of-water grace.
    @Override protected void handleAirSupply(int air){}
    @Override protected void registerGoals(){goalSelector.addGoal(0,new PanicGoal(this,1.6));goalSelector.addGoal(2,new RandomSwimmingGoal(this,1,20));goalSelector.addGoal(3,new RandomLookAroundGoal(this));}
    @Override public void travel(Vec3 input){
        if(isEffectiveAi()&&isInWater()){moveRelative(.01F,input);move(MoverType.SELF,getDeltaMovement());setDeltaMovement(getDeltaMovement().scale(.9));}
        else super.travel(input);
    }
    @Override public boolean hurt(net.minecraft.world.damagesource.DamageSource source,float amount){
        boolean hit=super.hurt(source,amount);
        if(hit&&!level().isClientSide&&isAlive())BeastVfx.emit(this,"flee",position(),getLookAngle(),12,true);
        return hit;
    }
    @Override public String animationState(){return isInWater()?"swim":"idle";}
}
