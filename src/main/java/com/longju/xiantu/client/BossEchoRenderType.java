package com.longju.xiantu.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/** GLB primitives are triangles; vanilla entityTranslucent is QUADS and is incompatible. */
final class BossEchoRenderType extends RenderType {
    static final RenderType ECHO=create("xiantu_spirit_echo",DefaultVertexFormat.NEW_ENTITY,
        VertexFormat.Mode.TRIANGLES,262144,false,true,CompositeState.builder()
            .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
            .setTextureState(new TextureStateShard(new ResourceLocation("xiantu","textures/entity/spirit_echo.png"),false,false))
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL)
            .setLightmapState(LIGHTMAP).setOverlayState(OVERLAY)
            .createCompositeState(false));
    private BossEchoRenderType(String n,VertexFormat f,VertexFormat.Mode m,int size,boolean crumbling,
                               boolean sort,Runnable setup,Runnable clear){super(n,f,m,size,crumbling,sort,setup,clear);}
}
