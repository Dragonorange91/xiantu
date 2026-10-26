package com.longju.xiantu.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;

/** A nearest-surface mask keeps the translucent light from accumulating through the mesh. */
final class SpiritSwordRenderType extends RenderType {
 static final RenderType TRAIL=create("xiantu_spirit_sword_trail",DefaultVertexFormat.POSITION_COLOR,
  VertexFormat.Mode.QUADS,4096,false,true,CompositeState.builder()
   .setShaderState(POSITION_COLOR_SHADER).setTransparencyState(TRANSLUCENT_TRANSPARENCY)
   .setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_WRITE)
   .setCullState(NO_CULL).createCompositeState(false));
 static final RenderType DEPTH=create("xiantu_spirit_sword_depth",DefaultVertexFormat.POSITION_COLOR,
  VertexFormat.Mode.QUADS,256,false,false,CompositeState.builder()
   .setShaderState(POSITION_COLOR_SHADER).setTransparencyState(NO_TRANSPARENCY)
   .setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(DEPTH_WRITE)
   .setCullState(NO_CULL).createCompositeState(false));
 static final RenderType BODY=create("xiantu_spirit_sword",DefaultVertexFormat.POSITION_COLOR,
  VertexFormat.Mode.QUADS,256,false,false,CompositeState.builder()
   .setShaderState(POSITION_COLOR_SHADER).setTransparencyState(TRANSLUCENT_TRANSPARENCY)
   .setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_WRITE)
   .setCullState(NO_CULL).createCompositeState(false));
 static final RenderType GLOW=create("xiantu_spirit_sword_glow",DefaultVertexFormat.POSITION_COLOR,
  VertexFormat.Mode.QUADS,256,false,false,CompositeState.builder()
   .setShaderState(POSITION_COLOR_SHADER).setTransparencyState(ADDITIVE_TRANSPARENCY)
   .setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_WRITE)
   .setCullState(NO_CULL).createCompositeState(false));
 private SpiritSwordRenderType(){super("xiantu_spirit_sword",DefaultVertexFormat.POSITION_COLOR,VertexFormat.Mode.QUADS,256,false,false,()->{},()->{});}
}
