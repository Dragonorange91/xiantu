package com.longju.xiantu.client;
import com.longju.xiantu.spell.SpiritSwordEntity;
import com.longju.xiantu.spell.SwordFlightMotion;
import com.longju.xiantu.spell.SwordFlightPrediction;
import com.longju.xiantu.spell.SwordFlightCollision;
import com.longju.xiantu.network.*;
import net.minecraft.client.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="xiantu",value=Dist.CLIENT)
public final class SpiritSwordClient {
 private static CameraType previous;
 private static SpiritSwordEntity mounted;
 private static SwordFlightPrediction prediction;
 private static Vec3 visualOffset=Vec3.ZERO;
 private static int sequence;
 private static boolean predictionBlocked;
 private static final com.longju.xiantu.spell.SwordFlightControls controls=new com.longju.xiantu.spell.SwordFlightControls();
 @SubscribeEvent public static void logout(net.minecraftforge.client.event.ClientPlayerNetworkEvent.LoggingOut event){SpiritSwordRenderer.clear();}
 @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
  if(e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();
  var sword=mc.player!=null&&mc.player.getVehicle() instanceof SpiritSwordEntity s?s:null;
  if(sword!=null){
   if(mounted!=sword){if(mounted==null){previous=mc.options.getCameraType();mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);}controls.reset();prediction=new SwordFlightPrediction(sword.flightState());sequence=0;visualOffset=Vec3.ZERO;predictionBlocked=false;}mounted=sword;
   boolean open=mc.screen!=null;
   var action=controls.tick(mc.player.tickCount,mc.options.keyUp.isDown(),mc.options.keyShift.isDown(),mc.options.keySprint.isDown(),open);
   if(!mc.isPaused()){float up=open?0:(mc.options.keyJump.isDown()?1:0)-(mc.options.keyShift.isDown()?1:0);
    // Read configurable actions directly; vanilla's movement impulses can be zeroed on mounts.
    float forward=open?0:(mc.options.keyUp.isDown()?1:0)-(mc.options.keyDown.isDown()?1:0);
    float strafe=open?0:(mc.options.keyLeft.isDown()?1:0)-(mc.options.keyRight.isDown()?1:0);
    var input=new SwordFlightMotion.Input(sequence++,forward,strafe,up,mc.player.getYRot(),mc.player.getXRot(),action.boost());
    if(!action.dismount())prediction.advance(input,SpiritSwordClient::step);
    visualOffset=visualOffset.scale(.55);var state=prediction.state();
    sword.setPos(state.x()+visualOffset.x,state.y()+visualOffset.y,state.z()+visualOffset.z);
    sword.setDeltaMovement(state.vx(),state.vy(),state.vz());sword.setYRot(input.yaw());sword.positionRider(mc.player);
    XiantuNetwork.CHANNEL.sendToServer(new SpiritSwordInputPacket(sword.getId(),input.sequence(),forward,strafe,up,input.yaw(),input.pitch(),action.boost(),action.dismount()));}
  }else if(mounted!=null){if(previous!=null&&mc.options.getCameraType()==CameraType.THIRD_PERSON_BACK)mc.options.setCameraType(previous);mounted=null;prediction=null;previous=null;visualOffset=Vec3.ZERO;controls.reset();}
 }
 private static SwordFlightMotion.State step(SwordFlightMotion.State state,SwordFlightMotion.Input input){
  if(predictionBlocked)return new SwordFlightMotion.State(state.x(),state.y(),state.z(),0,0,0);
  var next=SwordFlightMotion.step(state,input);var mc=Minecraft.getInstance();
  Vec3 at=new Vec3(state.x(),state.y(),state.z());
  Vec3 safe=SwordFlightCollision.clip(mc.player,mounted,at,new Vec3(next.vx(),next.vy(),next.vz()));
  return new SwordFlightMotion.State(at.x+safe.x,at.y+safe.y,at.z+safe.z,safe.x,safe.y,safe.z);
 }
 public static void receive(SpiritSwordStatePacket packet){
  var mc=Minecraft.getInstance();
  if(mc.player==null||mc.level==null||mounted==null||prediction==null||mc.player.getVehicle()!=mounted||mounted.getId()!=packet.entity())return;
  boolean previousBlocked=predictionBlocked;predictionBlocked=packet.blocked();
  if(!prediction.reconcile(packet.acknowledged(),packet.state(),SpiritSwordClient::step)){predictionBlocked=previousBlocked;return;}
  var state=prediction.state();Vec3 target=new Vec3(state.x(),state.y(),state.z());
  visualOffset=mounted.position().subtract(target);
  // Collision corrections and teleports are exact; only ordinary network jitter is eased.
  if(packet.blocked()||visualOffset.lengthSqr()>16){visualOffset=Vec3.ZERO;mounted.setPos(target);mounted.setOldPosAndRot();mounted.positionRider(mc.player);mc.player.setOldPosAndRot();}
 }
 private SpiritSwordClient(){}
}
