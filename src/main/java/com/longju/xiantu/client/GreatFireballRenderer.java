package com.longju.xiantu.client;

import com.longju.xiantu.network.GreatFireballStatePacket;
import com.longju.xiantu.spell.GreatFireballRules;
import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import java.util.*;

/** Solar convection on a cached 3D sphere: no full-screen ray march or particle cloud. */
@Mod.EventBusSubscriber(modid="xiantu",value=Dist.CLIENT)
public final class GreatFireballRenderer {
    private static final class Orb {
        UUID owner;int entity,phase,ticks;long received;Vec3 position,velocity;
    }
    private static final Map<UUID,Orb> ORBS=new LinkedHashMap<>();
    private static ClientLevel world;
    private static VertexBuffer sphere;
    private static ShaderInstance shader;
    private static boolean world(){var current=Minecraft.getInstance().level;if(current!=world){ORBS.clear();world=current;}return current!=null;}
    public static void clear(){ORBS.clear();world=null;}
    private static void disposeMesh(){if(sphere!=null){sphere.close();sphere=null;}}
    public static void receive(GreatFireballStatePacket p){
        if(!world()||!world.dimension().location().equals(p.dimension())||p.phase()<0||p.phase()>3||p.ticks()<0||p.ticks()>100||!Double.isFinite(p.position().lengthSqr())||!Double.isFinite(p.velocity().lengthSqr()))return;
        var owner=world.getEntity(p.entity());if(owner==null||!owner.getUUID().equals(p.owner()))return;
        if(p.phase()==3){ORBS.remove(p.transaction());SkillCastAnimations.end(p.owner());return;}
        var orb=ORBS.get(p.transaction());boolean fresh=orb==null;if(fresh){if(ORBS.size()>=24)return;orb=new Orb();ORBS.put(p.transaction(),orb);}
        int oldPhase=orb.phase;orb.owner=p.owner();orb.entity=p.entity();orb.phase=p.phase();orb.ticks=p.ticks();orb.position=p.position();orb.velocity=p.velocity();orb.received=world.getGameTime();
        if(orb.phase==0&&fresh)SkillCastAnimations.begin(orb.owner,"great_fireball");
        if(orb.phase==1&&(fresh||oldPhase==0))SkillCastAnimations.release(orb.owner);
        if(orb.phase==2){SkillCastAnimations.end(orb.owner);if(fresh||oldPhase!=2){for(int i=0;i<24;i++){double a=i*Math.PI*2/24,r=GreatFireballRules.blastRadius(orb.ticks)*.55;world.addParticle(net.minecraft.core.particles.ParticleTypes.FLAME,orb.position.x,orb.position.y,orb.position.z,Math.cos(a)*r*.12,.04+world.random.nextDouble()*.12,Math.sin(a)*r*.12);}}}
    }
    public static double chargeProgress(){if(!world())return 0;var p=Minecraft.getInstance().player;if(p==null)return 0;for(var orb:ORBS.values())if(orb.phase==0&&orb.owner.equals(p.getUUID()))return GreatFireballRules.progress(orb.ticks+Math.min(4,world.getGameTime()-orb.received));return 0;}
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e){clear();}
    @SubscribeEvent public static void draw(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES||!world()||ORBS.isEmpty()||shader==null)return;
        double time=world.getGameTime()+event.getPartialTick();var camera=event.getCamera().getPosition();
        ORBS.entrySet().removeIf(e->{var o=e.getValue();return world.getEntity(o.entity)==null||time-o.received>(o.phase==2?18:o.phase==0?20:12);});
        if(sphere==null)upload();
        var poses=event.getPoseStack();
        for(var orb:ORBS.values()){
            var entity=world.getEntity(orb.entity);if(entity==null)continue;double age=time-orb.received,ticks=orb.phase==0?Math.min(100,orb.ticks+Math.min(4,age)):orb.ticks;
            double radius=GreatFireballRules.radius(ticks);Vec3 at=orb.position;
            if(orb.phase==0){at=entity.getEyePosition(event.getPartialTick()).add(entity.getViewVector(event.getPartialTick()).scale(radius+2.1)).add(0,GreatFireballRules.lift(radius),0);}
            else if(orb.phase==1)at=at.add(orb.velocity.scale(Math.max(0,Math.min(3,age))));
            if(at.distanceToSqr(camera)>112*112)continue;
            float opacity=1;if(orb.phase==2){double u=Math.min(1,age/18);radius*=1+1.6*u;opacity=(float)((1-u)*(1-u));}
            poses.pushPose();poses.translate(at.x-camera.x,at.y-camera.y,at.z-camera.z);poses.scale((float)radius,(float)radius,(float)radius);
            var matrix=new Matrix4f(RenderSystem.getModelViewMatrix()).mul(poses.last().pose());
            set("Time",(float)(time/20));set("Opacity",opacity);set("Layer",0);drawMesh(matrix,orb.phase==2?Types.CORONA:Types.BODY);
            poses.scale(1.065F,1.065F,1.065F);matrix=new Matrix4f(RenderSystem.getModelViewMatrix()).mul(poses.last().pose());set("Layer",1);drawMesh(matrix,Types.CORONA);
            poses.popPose();
        }
    }
    private static void set(String name,float value){var uniform=shader.getUniform(name);if(uniform!=null)uniform.set(value);}
    private static void drawMesh(Matrix4f matrix,RenderType type){type.setupRenderState();try{sphere.bind();sphere.drawWithShader(matrix,RenderSystem.getProjectionMatrix(),shader);}finally{VertexBuffer.unbind();type.clearRenderState();}}
    private static void upload(){
        var builder=new BufferBuilder(160000);builder.begin(VertexFormat.Mode.TRIANGLES,DefaultVertexFormat.POSITION);
        int rings=32,slices=64;for(int y=0;y<rings;y++)for(int x=0;x<slices;x++){
            double a=y*Math.PI/rings,b=(y+1)*Math.PI/rings,c=x*Math.PI*2/slices,d=(x+1)*Math.PI*2/slices;
            point(builder,a,c);point(builder,b,d);point(builder,b,c);point(builder,a,c);point(builder,a,d);point(builder,b,d);
        }
        sphere=new VertexBuffer(VertexBuffer.Usage.STATIC);try{sphere.bind();sphere.upload(builder.end());}finally{VertexBuffer.unbind();}
    }
    private static void point(BufferBuilder b,double latitude,double longitude){b.vertex(Math.sin(latitude)*Math.cos(longitude),Math.cos(latitude),Math.sin(latitude)*Math.sin(longitude)).endVertex();}
    private static final class Types extends RenderType {
        private static RenderType make(String name,boolean glow){return create(name,DefaultVertexFormat.POSITION,VertexFormat.Mode.TRIANGLES,256,false,false,CompositeState.builder().setShaderState(new ShaderStateShard(()->shader)).setTransparencyState(glow?TRANSLUCENT_TRANSPARENCY:NO_TRANSPARENCY).setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(glow?COLOR_WRITE:COLOR_DEPTH_WRITE).setCullState(CULL).setOutputState(PARTICLES_TARGET).createCompositeState(false));}
        static final RenderType BODY=make("xiantu_great_fireball",false),CORONA=make("xiantu_great_fireball_corona",true);
        Types(){super("xiantu_great_fireball",DefaultVertexFormat.POSITION,VertexFormat.Mode.TRIANGLES,256,false,false,()->{},()->{});}
    }
    @Mod.EventBusSubscriber(modid="xiantu",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static final class Shaders {
        @SubscribeEvent public static void register(RegisterShadersEvent event)throws java.io.IOException{
            disposeMesh();clear();event.registerShader(new ShaderInstance(event.getResourceProvider(),new ResourceLocation("xiantu","great_fireball"),DefaultVertexFormat.POSITION),s->shader=s);
        }
    }
    private GreatFireballRenderer(){}
}
