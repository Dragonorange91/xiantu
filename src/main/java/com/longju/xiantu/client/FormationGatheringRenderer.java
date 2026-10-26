package com.longju.xiantu.client;

import com.longju.xiantu.formation.FormationCoreBlock;
import com.longju.xiantu.formation.FormationQiFlow;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import java.util.*;

/** Transparent smoke ribbons with real inward motion. No per-block emitter, world scan or screen-space pass. */
@Mod.EventBusSubscriber(modid="xiantu",value=Dist.CLIENT)
public final class FormationGatheringRenderer {
    private static final int MAX_ARRAYS=8,SEGMENTS=36;
    private static final class Array {
        int tier;long last;
        final double[] points=new double[(SEGMENTS+1)*3],width=new double[SEGMENTS+1],alpha=new double[SEGMENTS+1];
    }
    private static final Map<BlockPos,Array> ARRAYS=new LinkedHashMap<>();
    private static final MultiBufferSource.BufferSource BUFFERS=MultiBufferSource.immediate(new BufferBuilder(65536));
    private static ClientLevel world;
    public static void clear(){ARRAYS.clear();world=null;}
    private static boolean world(){var l=Minecraft.getInstance().level;if(l!=world){clear();world=l;}return world!=null;}
    public static void touch(BlockPos pos,int tier){
        if(!world()||tier<1||tier>3)return;var p=Minecraft.getInstance().player;
        if(p==null||p.distanceToSqr(Vec3.atCenterOf(pos))>48*48)return;
        var a=ARRAYS.get(pos);if(a==null){if(ARRAYS.size()>=MAX_ARRAYS)return;a=new Array();ARRAYS.put(pos.immutable(),a);}
        a.tier=tier;a.last=world.getGameTime();
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(e.phase!=TickEvent.Phase.END||!world())return;var mc=Minecraft.getInstance();long now=world.getGameTime();
        ARRAYS.entrySet().removeIf(entry->{var pos=entry.getKey();if(now-entry.getValue().last>3||mc.player==null||mc.player.distanceToSqr(Vec3.atCenterOf(pos))>48*48||!world.hasChunkAt(pos))return true;
            var state=world.getBlockState(pos);return !(state.getBlock() instanceof FormationCoreBlock)||!state.getValue(FormationCoreBlock.ACTIVE);});
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e){clear();}
    @SubscribeEvent public static void draw(RenderLevelStageEvent e){
        if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES||!world()||ARRAYS.isEmpty())return;
        Vec3 camera=e.getCamera().getPosition();double time=world.getGameTime()+e.getPartialTick();
        var pose=e.getPoseStack().last().pose();var consumer=BUFFERS.getBuffer(FlowType.QI);
        for(var entry:ARRAYS.entrySet()){
            var pos=entry.getKey();var array=entry.getValue();double distance=Vec3.atCenterOf(pos).distanceTo(camera);
            if(distance>48||!world.hasChunkAt(pos))continue;
            int count=FormationQiFlow.streams(array.tier),segments=distance>28?18:SEGMENTS;
            double fade=Math.min(1,(48-distance)/12),seed=(pos.asLong()&1023)*.013;
            for(int stream=0;stream<count;stream++)for(int packet=0;packet<2;packet++){
                double clock=time/FormationQiFlow.PERIOD+stream*.61803398875+packet*.5+seed;
                double cycle=Math.floor(clock),head=(clock-cycle)*(1+FormationQiFlow.TRAIL);
                // The tail follows the same emitted streamline, rather than sweeping sideways as a rigid ring.
                double phase=stream*2.3999632297+packet*1.13+cycle*.91+seed;
                for(int i=0;i<=segments;i++){
                    double u=(double)i/segments,p=head-FormationQiFlow.TRAIL*(1-u);
                    FormationQiFlow.point(array.tier,Math.max(0,Math.min(1,p)),phase,array.points,i*3);
                    double envelope=p<=0||p>=1?0:FormationQiFlow.envelope(p);
                    array.width[i]=(.09+.20*Math.sin(Math.PI*u))*(.5+.5*envelope)*(1+.14*array.tier);
                    array.alpha[i]=Math.pow(Math.sin(Math.PI*u),.65)*envelope*fade;
                }
                for(int i=0;i<segments;i++)segment(consumer,pose,array,i,camera,pos,phase);
            }
        }
        BUFFERS.endBatch(FlowType.QI);
    }
    private static void segment(VertexConsumer c,Matrix4f m,Array a,int i,Vec3 camera,BlockPos origin,double phase){
        int j=i*3,k=j+3;double[] p=a.points;
        double ax=p[j]+origin.getX()+.5,ay=p[j+1]+origin.getY(),az=p[j+2]+origin.getZ()+.5;
        double bx=p[k]+origin.getX()+.5,by=p[k+1]+origin.getY(),bz=p[k+2]+origin.getZ()+.5;
        double tx=bx-ax,ty=by-ay,tz=bz-az,vx=camera.x-(ax+bx)*.5,vy=camera.y-(ay+by)*.5,vz=camera.z-(az+bz)*.5;
        double sx=ty*vz-tz*vy,sy=tz*vx-tx*vz,sz=tx*vy-ty*vx,len=Math.sqrt(sx*sx+sy*sy+sz*sz);
        if(len<1e-7)return;sx/=len;sy/=len;sz/=len;
        // Feathered mist + a narrow pale filament, both use normal alpha blending and read-only scene depth.
        for(int layer=0;layer<2;layer++){
            double scale=layer==0?1.65:.34,wa=a.width[i]*scale,wb=a.width[i+1]*scale;
            float aa=(float)(a.alpha[i]*(layer==0?.32:.62)),ab=(float)(a.alpha[i+1]*(layer==0?.32:.62));
            float u=(float)(i*.083+phase*.17),v=(float)((i+1)*.083+phase*.17);
            float r=layer==0?.32f:.64f,g=layer==0?.88f:.98f;
            vertex(c,m,ax-sx*wa-camera.x,ay-sy*wa-camera.y,az-sz*wa-camera.z,u,0,r,g,aa);
            vertex(c,m,ax+sx*wa-camera.x,ay+sy*wa-camera.y,az+sz*wa-camera.z,u,1,r,g,aa);
            vertex(c,m,bx+sx*wb-camera.x,by+sy*wb-camera.y,bz+sz*wb-camera.z,v,1,r,g,ab);
            vertex(c,m,bx-sx*wb-camera.x,by-sy*wb-camera.y,bz-sz*wb-camera.z,v,0,r,g,ab);
        }
    }
    private static void vertex(VertexConsumer c,Matrix4f m,double x,double y,double z,float u,float v,float r,float g,float alpha){c.vertex(m,(float)x,(float)y,(float)z).uv(u,v).color(r,g,1,alpha).endVertex();}
    private static final class FlowType extends RenderType {
        static final RenderType QI=create("xiantu_formation_qi",DefaultVertexFormat.POSITION_TEX_COLOR,VertexFormat.Mode.QUADS,65536,false,true,
            CompositeState.builder().setShaderState(new ShaderStateShard(GameRenderer::getPositionTexColorShader))
                .setTextureState(new TextureStateShard(new ResourceLocation("xiantu","textures/vfx/formation/qi_flow.png"),true,false))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY).setDepthTestState(LEQUAL_DEPTH_TEST)
                .setWriteMaskState(COLOR_WRITE).setCullState(NO_CULL).setOutputState(PARTICLES_TARGET).createCompositeState(false));
        private FlowType(){super("xiantu_formation_qi",DefaultVertexFormat.POSITION_TEX_COLOR,VertexFormat.Mode.QUADS,256,false,false,()->{},()->{});}
    }
    private FormationGatheringRenderer(){}
}
