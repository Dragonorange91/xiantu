package com.longju.xiantu.spell;

import com.longju.xiantu.gongfa.GongfaService;
import com.longju.xiantu.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.network.NetworkHooks;
import java.util.*;

/** A transient, server-driven platform. Passengers stand on its blade rather than sit. */
public final class SpiritSwordEntity extends Entity {
 public static final double DECK_HEIGHT=.22, SPEED=SwordFlightMotion.SPEED, BOOST_SPEED=SwordFlightMotion.BOOST_SPEED;
 private static final EntityDataAccessor<Optional<UUID>> OWNER=SynchedEntityData.defineId(SpiritSwordEntity.class,EntityDataSerializers.OPTIONAL_UUID);
 private float forward,strafe,vertical;
 private int lastInput=-100,idleTicks,lerpSteps;
 private double targetX,targetY,targetZ;
 private float targetYaw;
 private boolean returning;
 private boolean boosting;
 private int inputSequence=-1;
 private float inputYaw,inputPitch;
 private int flightMasteryTicks;
 public SpiritSwordEntity(EntityType<? extends SpiritSwordEntity> type,Level level){super(type,level);setNoGravity(true);}
 @Override protected void defineSynchedData(){entityData.define(OWNER,Optional.empty());}
 public UUID owner(){return entityData.get(OWNER).orElse(null);}
 public String flightDebug(){return "tick="+tickCount+" inputAge="+(tickCount-lastInput)+" input="+forward+","+strafe+","+vertical+" velocity="+getDeltaMovement()+" passenger="+getPassengers().size();}
 public void owner(UUID id){entityData.set(OWNER,Optional.of(id));}
 public static boolean recall(ServerPlayer p){var owned=new ArrayList<SpiritSwordEntity>();for(var level:p.server.getAllLevels())for(var e:level.getAllEntities())if(e instanceof SpiritSwordEntity s&&p.getUUID().equals(s.owner()))owned.add(s);owned.forEach(Entity::discard);return !owned.isEmpty();}
 public static void dismiss(ServerPlayer p){recall(p);}
 public static Vec3 placement(ServerPlayer p){
  Vec3 forward=new Vec3(-Mth.sin(p.getYRot()*Mth.DEG_TO_RAD),0,Mth.cos(p.getYRot()*Mth.DEG_TO_RAD));
  for(double distance:new double[]{2,1.35,.7}){
   Vec3 horizontal=p.position().add(forward.scale(distance));
   if(!p.serverLevel().hasChunkAt(BlockPos.containing(horizontal)))continue;
   var floor=p.level().clip(new ClipContext(horizontal.add(0,1.2,0),horizontal.add(0,-3,0),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));
   if(floor.getType()!=HitResult.Type.BLOCK||floor.getDirection()!=net.minecraft.core.Direction.UP)continue;
   Vec3 at=floor.getLocation().add(0,.04,0);
   var body=p.getBoundingBox().move(at.add(0,DECK_HEIGHT,0).subtract(p.position()));
   if(p.level().getWorldBorder().isWithinBounds(body)&&p.level().noCollision(p,body)&&p.level().noCollision(new AABB(at.x-.45,at.y,at.z-.45,at.x+.45,at.y+.2,at.z+.45))
      &&SpellService.safeDestination(p,at.add(0,DECK_HEIGHT,0).distanceTo(p.position()),at.add(0,DECK_HEIGHT,0).subtract(p.position()).normalize()).distanceToSqr(at.add(0,DECK_HEIGHT,0))<.07)return at;
  }return null;
 }
 public static SpiritSwordEntity summon(ServerPlayer p,Vec3 at){
  var sword=ModEntities.SPIRIT_SWORD.get().create(p.serverLevel());if(sword==null)return null;
  sword.owner(p.getUUID());sword.moveTo(at.x,at.y,at.z,p.getYRot(),0);
  if(!p.serverLevel().addFreshEntity(sword))return null;
  // Replace only after a new valid summon has been accepted by the server.
  for(var old:p.serverLevel().getEntitiesOfClass(SpiritSwordEntity.class,p.getBoundingBox().inflate(128),e->e!=sword&&p.getUUID().equals(e.owner())))old.discard();
  return sword;
 }
 public void input(ServerPlayer p,float f,float s,float v){
  input(p,f,s,v,false,false);
 }
 public void input(ServerPlayer p,float f,float s,float v,boolean boost,boolean dismount){
  input(p,inputSequence+1,f,s,v,p.getYRot(),p.getXRot(),boost,dismount);
 }
 public void input(ServerPlayer p,int sequence,float f,float s,float v,float yaw,float pitch,boolean boost,boolean dismount){
  if(getFirstPassenger()!=p||!p.getUUID().equals(owner()))return;
  if(dismount){p.stopRiding();return;}
  if(sequence<0||sequence<=inputSequence||!Float.isFinite(f)||!Float.isFinite(s)||!Float.isFinite(v)||!Float.isFinite(yaw)||!Float.isFinite(pitch))return;
  inputSequence=sequence;inputYaw=Mth.wrapDegrees(yaw);inputPitch=Mth.clamp(pitch,-90,90);
  lastInput=tickCount;forward=Mth.clamp(f,-1,1);strafe=Mth.clamp(s,-1,1);vertical=Mth.clamp(v,-1,1);boosting=boost;
 }
 @Override public InteractionResult interact(Player player,InteractionHand hand){
  if(!player.getUUID().equals(owner())||player.isShiftKeyDown()||player.isPassenger())return InteractionResult.PASS;
  if(player instanceof ServerPlayer p){
   if(!SpellService.denial(p,SpellCatalog.find("sword_flight"),true).isEmpty()||!p.isAlive())return InteractionResult.FAIL;
   var feet=position().add(0,DECK_HEIGHT,0);var delta=feet.subtract(p.position());
   if(!level().noCollision(p,p.getBoundingBox().move(delta))||SpellService.safeDestination(p,delta.length(),delta.normalize()).distanceToSqr(feet)>.07)return InteractionResult.FAIL;
   p.startRiding(this);idleTicks=0;returning=false;setDeltaMovement(Vec3.ZERO);inputSequence=-1;lastInput=-100;forward=strafe=vertical=0;boosting=false;inputYaw=p.getYRot();inputPitch=p.getXRot();
  }return InteractionResult.sidedSuccess(level().isClientSide);
 }
 @Override public void tick(){
  super.tick();
  if(level().isClientSide){if(getFirstPassenger() instanceof Player rider&&rider.isLocalPlayer()){lerpSteps=0;return;}if(lerpSteps>0){setPos(getX()+(targetX-getX())/lerpSteps,getY()+(targetY-getY())/lerpSteps,getZ()+(targetZ-getZ())/lerpSteps);setYRot(getYRot()+Mth.wrapDegrees(targetYaw-getYRot())/lerpSteps);lerpSteps--;}return;}
  if(owner()==null||!(level().getPlayerByUUID(owner()) instanceof ServerPlayer p)||!p.isAlive()||p.isSpectator()||p.level()!=level()||p.distanceToSqr(this)>128*128){discard();return;}
  if(getFirstPassenger()!=p){if(returning){returnToGround();idleTicks=0;}else if(++idleTicks>600)discard();return;}idleTicks=0;p.fallDistance=0;
  if(!SpellService.denial(p,SpellCatalog.find("sword_flight"),true).isEmpty()){landAndDismiss(p);return;}
  if(tickCount-lastInput>6){forward=strafe=vertical=0;boosting=false;}
  if(tickCount%20==0){var d=GongfaService.data(p);double drain=d.powerMax()*SupportSpellRules.flightDrainRatio(d.majorRealm(),boosting&&(Math.abs(forward)+Math.abs(strafe)+Math.abs(vertical)>.01));if(d.power()+1e-9<drain){landAndDismiss(p);return;}d.setPower(Math.max(0,d.power()-drain));com.longju.xiantu.cultivation.CultivationEvents.sync(p);}
  setYRot(inputYaw);
  var next=SwordFlightMotion.step(flightState(),new SwordFlightMotion.Input(inputSequence,forward,strafe,vertical,inputYaw,inputPitch,boosting));
  Vec3 requested=new Vec3(next.vx(),next.vy(),next.vz());
  Vec3 velocity=SwordFlightCollision.clip(p,this,position(),requested),before=position();
  setDeltaMovement(velocity);move(MoverType.SELF,velocity);setDeltaMovement(position().subtract(before));hasImpulse=true;
  if(!p.isCreative()&&position().distanceToSqr(before)>.0025&&++flightMasteryTicks>=SupportSpellRules.FLIGHT_XP_TICKS){flightMasteryTicks=0;SpellService.data(p).award("sword_flight",(int)Math.round(1000*com.longju.xiantu.cultivation.PhysiqueService.effectiveSpellExperience(GongfaService.data(p),1)),SpellService.now(p),UUID.randomUUID(),null);SpellService.sync(p);}
  com.longju.xiantu.network.XiantuNetwork.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(()->p),new com.longju.xiantu.network.SpiritSwordStatePacket(getId(),inputSequence,flightState(),getDeltaMovement().distanceToSqr(requested)>.000001));
 }
 public SwordFlightMotion.State flightState(){var v=getDeltaMovement();return new SwordFlightMotion.State(getX(),getY(),getZ(),v.x,v.y,v.z);}
 private void returnToGround(){
  // Empty swords settle vertically, stop on real collision geometry and can be boarded again.
  double down=Math.min(.45,Math.max(.06,-getDeltaMovement().y+.025));
  var hit=level().clip(new ClipContext(position().add(0,.001,0),position().add(0,-down-.04,0),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));
  if(hit.getType()==HitResult.Type.BLOCK&&hit.getDirection()==net.minecraft.core.Direction.UP){
   move(MoverType.SELF,new Vec3(0,Math.min(0,hit.getLocation().y+.04-getY()),0));setDeltaMovement(Vec3.ZERO);returning=false;
  }else{
   double y=getY();setDeltaMovement(0,-down,0);move(MoverType.SELF,getDeltaMovement());
   if(onGround()||getY()>y-down+.001){setDeltaMovement(Vec3.ZERO);returning=false;}
  }
  hasImpulse=true;if(getY()<level().getMinBuildHeight()-32)discard();
 }
 private void landAndDismiss(ServerPlayer p){
  p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SLOW_FALLING,200,0,false,false));p.stopRiding();discard();
 }
 @Override protected void positionRider(Entity passenger,Entity.MoveFunction move){if(hasPassenger(passenger)){move.accept(passenger,getX(),getY()+DECK_HEIGHT,getZ());passenger.fallDistance=0;}}
 @Override protected void removePassenger(Entity passenger){super.removePassenger(passenger);if(!level().isClientSide&&passenger instanceof ServerPlayer p){returning=true;idleTicks=0;setDeltaMovement(Vec3.ZERO);p.fallDistance=0;p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SLOW_FALLING,200,0,false,false));}}
 @Override public Vec3 getDismountLocationForPassenger(LivingEntity passenger){return position().add(0,DECK_HEIGHT,0);}
 @Override public boolean shouldRiderSit(){return false;}
 @Override public double getPassengersRidingOffset(){return DECK_HEIGHT;}
 @Override protected boolean canAddPassenger(Entity e){return getPassengers().isEmpty()&&e.getUUID().equals(owner());}
 @Override public boolean isPickable(){return true;}
 @Override public boolean canBeCollidedWith(){return false;}
 @Override public boolean shouldBeSaved(){return false;}
 @Override protected void addAdditionalSaveData(CompoundTag t){}
 @Override protected void readAdditionalSaveData(CompoundTag t){}
 @Override public Packet<ClientGamePacketListener> getAddEntityPacket(){return NetworkHooks.getEntitySpawningPacket(this);}
 @Override public void lerpTo(double x,double y,double z,float yaw,float pitch,int steps,boolean teleport){if(getFirstPassenger() instanceof Player p&&p.isLocalPlayer())return;targetX=x;targetY=y;targetZ=z;targetYaw=yaw;lerpSteps=Math.max(1,steps);}
}
