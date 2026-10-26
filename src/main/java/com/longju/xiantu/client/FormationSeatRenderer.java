package com.longju.xiantu.client;

import com.longju.xiantu.formation.FormationSeatEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public final class FormationSeatRenderer extends EntityRenderer<FormationSeatEntity> {
    public FormationSeatRenderer(EntityRendererProvider.Context context) { super(context); }
    @Override public ResourceLocation getTextureLocation(FormationSeatEntity entity) {
        return new ResourceLocation("minecraft", "textures/misc/white.png");
    }
    @Override public void render(FormationSeatEntity entity, float yaw, float partialTick,
                                 com.mojang.blaze3d.vertex.PoseStack poseStack, MultiBufferSource buffers, int packedLight) {}
}
