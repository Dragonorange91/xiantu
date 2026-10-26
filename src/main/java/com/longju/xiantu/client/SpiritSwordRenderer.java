package com.longju.xiantu.client;
import com.longju.xiantu.spell.SpiritSwordEntity;
import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.util.*;
import java.util.*;
import org.joml.Matrix4f;
/** Upload the accepted immutable mesh once. All three light passes reuse static GPU vertices. */
public final class SpiritSwordRenderer extends EntityRenderer<SpiritSwordEntity> {
 private static final ResourceLocation MODEL=new ResourceLocation("xiantu","item/spirit_flying_sword");
 private static BakedModel cachedModel;
 private static VertexBuffer[] mesh;
 private static int uploads;
 public static int uploadCount(){return uploads;}
 public static boolean gpuReady(){return mesh!=null;}
 public static void clear(){
  if(!RenderSystem.isOnRenderThread()){RenderSystem.recordRenderCall(SpiritSwordRenderer::clear);return;}
  if(mesh!=null){for(var buffer:mesh)if(buffer!=null)buffer.close();mesh=null;}
  cachedModel=null;
 }
 public SpiritSwordRenderer(EntityRendererProvider.Context c){super(c);shadowRadius=0;}
 @Override public ResourceLocation getTextureLocation(SpiritSwordEntity e){return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;}
 @Override public void render(SpiritSwordEntity e,float yaw,float partial,PoseStack poses,MultiBufferSource buffers,int light){
  poses.pushPose();poses.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(partial,e.yRotO,e.getYRot())));
  // Native blade axis Y becomes +Z forward; its upper deck meets passenger feet at .22.
  poses.translate(0,.173,-1.55);poses.mulPose(Axis.XP.rotationDegrees(90));poses.scale(1.65F,1.65F,.55F);poses.translate(-.5,0,-.5);
  var model=Minecraft.getInstance().getModelManager().getModel(MODEL);
  if(cachedModel!=model||mesh==null){
   clear();if(model==Minecraft.getInstance().getModelManager().getMissingModel()){poses.popPose();return;}
   var quads=new ArrayList<BakedQuad>();var random=RandomSource.create(42);
   for(int side=-1;side<6;side++){random.setSeed(42);quads.addAll(model.getQuads(null,side<0?null:Direction.values()[side],random));}
   mesh=new VertexBuffer[3];
   try{mesh[0]=upload(quads,0,0,0,1);mesh[1]=upload(quads,.46F,.84F,1F,.56F);mesh[2]=upload(quads,.18F,.60F,1F,.22F);cachedModel=model;uploads++;
    com.mojang.logging.LogUtils.getLogger().info("[Xiantu sword GPU] Cached {} quads / 3 static passes, upload {}",quads.size(),uploads);
   }catch(RuntimeException failure){clear();throw failure;}
  }
  // Finish each pass locally: the body must not run before its own nearest-surface mask.
  // All passes use identical geometry; hidden back faces cannot stack their light through it.
  var transform=new Matrix4f(RenderSystem.getModelViewMatrix()).mul(poses.last().pose());
  draw(mesh[0],transform,SpiritSwordRenderType.DEPTH);
  draw(mesh[1],transform,SpiritSwordRenderType.BODY);
  draw(mesh[2],transform,SpiritSwordRenderType.GLOW);
  poses.popPose();super.render(e,yaw,partial,poses,buffers,light);
 }
 private static VertexBuffer upload(List<BakedQuad> quads,float r,float g,float b,float alpha){
  var builder=new BufferBuilder(Math.max(4096,quads.size()*4*16));builder.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);
  for(var quad:quads){int[] data=quad.getVertices();int stride=data.length/4;float shade=quad.getDirection().getAxis()==Direction.Axis.Z?1:.92F;
   for(int v=0;v<4;v++){int at=v*stride;builder.vertex(Float.intBitsToFloat(data[at]),Float.intBitsToFloat(data[at+1]),Float.intBitsToFloat(data[at+2])).color(r*shade,g*shade,b*shade,alpha).endVertex();}
  }
  var buffer=new VertexBuffer(VertexBuffer.Usage.STATIC);
  try{buffer.bind();buffer.upload(builder.end());return buffer;}
  catch(RuntimeException failure){buffer.close();throw failure;}
  finally{VertexBuffer.unbind();}
 }
 private static void draw(VertexBuffer buffer,Matrix4f transform,RenderType type){
  type.setupRenderState();
  try{buffer.bind();buffer.drawWithShader(transform,RenderSystem.getProjectionMatrix(),RenderSystem.getShader());}
  finally{VertexBuffer.unbind();type.clearRenderState();}
 }
}
