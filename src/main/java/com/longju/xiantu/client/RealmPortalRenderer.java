package com.longju.xiantu.client;

import com.longju.xiantu.instance.RealmPortalBlockEntity;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.joml.Matrix4f;

/** One precomputed sheet plus bounded additive ribbons; no live simulation, IO or shader compilation per portal/frame. */
public final class RealmPortalRenderer implements BlockEntityRenderer<RealmPortalBlockEntity> {
    private static final ResourceLocation VEIL=new ResourceLocation("xiantu:textures/effect/realm_portal.png");
    public RealmPortalRenderer(BlockEntityRendererProvider.Context ignored){}
    @Override public int getViewDistance(){return 96;}
    @Override public boolean shouldRenderOffScreen(RealmPortalBlockEntity be){return true;}
    @Override public void render(RealmPortalBlockEntity be,float partial,PoseStack stack,MultiBufferSource buffers,int light,int overlay){
        if(be.getLevel()==null)return;boolean open=be.getBlockState().getValue(BlockStateProperties.POWERED);
        RealmGateMesh.draw(stack,buffers,light,open);
        double time=be.getLevel().getGameTime()+partial;float pulse=(float)(.83+.17*Math.sin(time*.04));
        if(open&&be.clientOpenedAt<0)be.clientOpenedAt=be.getLevel().getGameTime();if(!open)be.clientOpenedAt=-1;
        float opening=open?(float)Math.min(1,(time-be.clientOpenedAt)/40):0;
        stack.pushPose();stack.translate(.5,2,.5);var matrix=stack.last().pose();
        if(open){
            int frame=Math.floorMod((int)(time/2),32);float v0=frame/32f,v1=(frame+1)/32f;
            var veil=buffers.getBuffer(RenderType.entityTranslucentEmissive(VEIL));
            quad(veil,matrix,-2.48f*opening,-1.99f*opening,2.48f*opening,1.99f*opening,0,v0,v1,0.85f*opening);
        }
        var beam=buffers.getBuffer(RenderType.lightning());
        // Layered, broken travelling light around the stone portal, with a warm inner rim.
        for(int layer=0;layer<3;layer++)for(int i=0;i<96;i++){
            double a=(i/96.0)*Math.PI*2+time*(layer%2==0?.006:-.009);
            double b=a+Math.PI*2/96*.78;float rx=2.42f+layer*.09f,ry=1.93f+layer*.06f;
            float ax=signedRoot(Math.sin(a))*rx,ay=signedRoot(Math.cos(a))*ry,bx=signedRoot(Math.sin(b))*rx,by=signedRoot(Math.cos(b))*ry;
            float strength=(open?.48f:.12f)*pulse;line(beam,matrix,ax,ay,bx,by,.03f+layer*.012f,layer*.025f,layer==1?1:.3f,layer==1?.75f:.7f,1,strength);
        }
        if(open)for(int ribbon=0;ribbon<5;ribbon++)for(int segment=0;segment<36;segment++){
            double x=-2.4+segment*4.8/36,x2=x+4.8/36;
            float y=(float)(Math.sin(x*1.6+time*.045+ribbon*1.4)*(.34+ribbon*.08)-1.4+ribbon*.58);
            float y2=(float)(Math.sin(x2*1.6+time*.045+ribbon*1.4)*(.34+ribbon*.08)-1.4+ribbon*.58);
            for(int glow=0;glow<2;glow++)line(beam,matrix,(float)x,y,(float)x2,y2,glow==0?.045f:.009f,(float)Math.sin(time*.025+ribbon)*.35f,glow==0?.16f:.7f,.65f,1,(glow==0?.045f:.25f)*opening);
        }
        if(open)for(int i=0;i<44;i++){
            double age=(time*.008+i*.61803398875)%1,angle=i*2.399963+time*.006;
            float x=(float)(Math.sin(angle)*(2.7+age*.7)),y=(float)(-1.8+age*4.8),z=(float)(Math.cos(angle)*.65);
            line(beam,matrix,x,y,x+(float)Math.sin(angle)*.07f,y+.08f,.014f,z,i%4==0?1:.48f,i%4==0?.64f:.83f,i%4==0?.24f:1,(float)((1-age)*.75));
        }
        stack.popPose();
    }
    private static float signedRoot(double v){return (float)Math.copySign(Math.pow(Math.abs(v),1.0/3),v);}
    private static void quad(VertexConsumer v,Matrix4f m,float x0,float y0,float x1,float y1,float z,float v0,float v1,float alpha){
        vertex(v,m,x0,y0,z,0,v1,alpha);vertex(v,m,x1,y0,z,1,v1,alpha);vertex(v,m,x1,y1,z,1,v0,alpha);vertex(v,m,x0,y1,z,0,v0,alpha);
        vertex(v,m,x0,y1,z-.008f,0,v0,alpha);vertex(v,m,x1,y1,z-.008f,1,v0,alpha);vertex(v,m,x1,y0,z-.008f,1,v1,alpha);vertex(v,m,x0,y0,z-.008f,0,v1,alpha);
    }
    private static void vertex(VertexConsumer v,Matrix4f m,float x,float y,float z,float u,float w,float alpha){v.vertex(m,x,y,z).color(1,1,1,alpha).uv(u,w).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(15728880).normal(0,0,1).endVertex();}
    private static void line(VertexConsumer v,Matrix4f m,float ax,float ay,float bx,float by,float width,float z,float r,float g,float b,float a){
        double len=Math.hypot(bx-ax,by-ay);if(len<1e-5)return;float dx=(float)((by-ay)/len*width),dy=(float)((ax-bx)/len*width);
        v.vertex(m,ax-dx,ay-dy,z).color(r,g,b,a).endVertex();v.vertex(m,bx-dx,by-dy,z).color(r,g,b,a).endVertex();v.vertex(m,bx+dx,by+dy,z).color(r,g,b,a).endVertex();v.vertex(m,ax+dx,ay+dy,z).color(r,g,b,a).endVertex();
    }
}
