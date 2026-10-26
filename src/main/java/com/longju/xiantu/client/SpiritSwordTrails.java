package com.longju.xiantu.client;

import com.longju.xiantu.spell.SpiritSwordEntity;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import java.util.*;

/** Fixed-tick history at the blade's rear socket. No per-render history insertion. */
@Mod.EventBusSubscriber(modid="xiantu",value=Dist.CLIENT)
public final class SpiritSwordTrails {
 private record Point(Vec3 at,long tick){}
 private static final class Trail {final ArrayDeque<Point> points=new ArrayDeque<>();long sampled=-1;SpiritSwordEntity sword;}
 private static final Map<UUID,Trail> trails=new HashMap<>();
 private static final MultiBufferSource.BufferSource buffers=MultiBufferSource.immediate(new BufferBuilder(4096));
 private static ClientLevel world;
 private static final int LIFE=14,MAX_TRAILS=32,MAX_POINTS=24;
 private static Vec3 socket(SpiritSwordEntity sword,Vec3 at){double yaw=Math.toRadians(sword.getYRot());return at.add(Math.sin(yaw)*1.15,.17,-Math.cos(yaw)*1.15);}
 @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
  if(e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();
  if(world!=mc.level){trails.clear();world=mc.level;}if(world==null||mc.player==null)return;
  long now=world.getGameTime();
  for(var entity:world.entitiesForRendering())if(entity instanceof SpiritSwordEntity sword&&sword.isVehicle()&&sword.distanceToSqr(mc.player)<96*96){
   Trail t=trails.get(sword.getUUID());if(t==null){if(trails.size()>=MAX_TRAILS)continue;t=new Trail();trails.put(sword.getUUID(),t);}
   t.sword=sword;if(t.sampled==now)continue;t.sampled=now;Vec3 at=socket(sword,sword.position());Point last=t.points.peekLast();
   if(last!=null&&last.at.distanceToSqr(at)>64)t.points.clear(); // Teleports break the ribbon.
   last=t.points.peekLast();if(last==null||last.at.distanceToSqr(at)>.0016)t.points.addLast(new Point(at,now));
   while(t.points.size()>MAX_POINTS)t.points.removeFirst();
  }
  trails.values().removeIf(t->{while(!t.points.isEmpty()&&now-t.points.getFirst().tick>LIFE)t.points.removeFirst();return t.points.isEmpty();});
 }
 @SubscribeEvent public static void draw(RenderLevelStageEvent e){
  if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES||world==null||trails.isEmpty())return;
  var mc=Minecraft.getInstance();if(mc.level!=world||mc.player==null)return;
  Vec3 camera=e.getCamera().getPosition();double now=world.getGameTime()+e.getPartialTick();
  var consumer=buffers.getBuffer(SpiritSwordRenderType.TRAIL);var pose=e.getPoseStack().last().pose();
  for(var entry:trails.entrySet()){
   Point previous=null;
   for(var point:entry.getValue().points){
    if(previous!=null)segment(consumer,pose,previous.at,point.at,previous.tick,point.tick,now,camera);
    previous=point;
   }
   // Only the newest endpoint interpolates; historical samples never change with frame rate.
   var s=entry.getValue().sword;
   if(s!=null&&!s.isRemoved()&&s.isVehicle()){
    Vec3 at=socket(s,s.getPosition(e.getPartialTick()));if(previous!=null&&previous.at.distanceToSqr(at)<64)segment(consumer,pose,previous.at,at,previous.tick,world.getGameTime()+1,now,camera);
   }
  }buffers.endBatch(SpiritSwordRenderType.TRAIL);
 }
 private static void segment(VertexConsumer c,Matrix4f pose,Vec3 a,Vec3 b,double ta,double tb,double now,Vec3 camera){
  double tx=b.x-a.x,ty=b.y-a.y,tz=b.z-a.z,length=tx*tx+ty*ty+tz*tz;if(length<1e-7)return;
  length=Math.sqrt(length);tx/=length;ty/=length;tz/=length;
  double vx=camera.x-(a.x+b.x)*.5,vy=camera.y-(a.y+b.y)*.5,vz=camera.z-(a.z+b.z)*.5;
  double sx=ty*vz-tz*vy,sy=tz*vx-tx*vz,sz=tx*vy-ty*vx;
  length=sx*sx+sy*sy+sz*sz;
  if(length<1e-8){if(Math.abs(ty)<.9){sx=-tz;sy=0;sz=tx;}else{sx=0;sy=tz;sz=-ty;}length=sx*sx+sy*sy+sz*sz;}
  length=Math.sqrt(length);sx/=length;sy/=length;sz/=length;
  float fa=(float)Math.min(1,Math.max(0,1-(now-ta)/LIFE)),fb=(float)Math.min(1,Math.max(0,1-(now-tb)/LIFE));
  ribbon(c,pose,a,b,sx,sy,sz,camera,.32*fa,.32*fb,fa*.12F,fb*.12F,.12F,.4F,1);
  ribbon(c,pose,a,b,sx,sy,sz,camera,.13*fa,.13*fb,fa*.38F,fb*.38F,.18F,.67F,1);
  ribbon(c,pose,a,b,sx,sy,sz,camera,.035*fa,.035*fb,fa*.85F,fb*.85F,.64F,.91F,1);
 }
 private static void ribbon(VertexConsumer c,Matrix4f pose,Vec3 a,Vec3 b,double sx,double sy,double sz,Vec3 camera,double wa,double wb,float aa,float ab,float r,float g,float blue){
  vertex(c,pose,a.x-sx*wa-camera.x,a.y-sy*wa-camera.y,a.z-sz*wa-camera.z,r,g,blue,aa);
  vertex(c,pose,a.x+sx*wa-camera.x,a.y+sy*wa-camera.y,a.z+sz*wa-camera.z,r,g,blue,aa);
  vertex(c,pose,b.x+sx*wb-camera.x,b.y+sy*wb-camera.y,b.z+sz*wb-camera.z,r,g,blue,ab);
  vertex(c,pose,b.x-sx*wb-camera.x,b.y-sy*wb-camera.y,b.z-sz*wb-camera.z,r,g,blue,ab);
 }
 private static void vertex(VertexConsumer c,Matrix4f pose,double x,double y,double z,float r,float g,float b,float a){c.vertex(pose,(float)x,(float)y,(float)z).color(r,g,b,a).endVertex();}
 private SpiritSwordTrails(){}
}
