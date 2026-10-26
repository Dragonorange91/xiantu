package com.longju.xiantu.client;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/** The collision entity has no vanilla item geometry. Photon supplies its surface. */
public final class PhotonProjectileRenderer<T extends Entity> extends EntityRenderer<T> {
    public PhotonProjectileRenderer(EntityRendererProvider.Context context) { super(context); }
    @Override public void render(T entity,float yaw,float partialTick,com.mojang.blaze3d.vertex.PoseStack poses,
            net.minecraft.client.renderer.MultiBufferSource buffers,int light) {
        if(net.minecraftforge.fml.ModList.get().isLoaded("photon"))
            BeastPhotonRuntime.projectile(entity,entity instanceof com.longju.xiantu.entity.FoxfireProjectile?"yanwei_fox":"purple_lightning_bat");
    }
    @Override public ResourceLocation getTextureLocation(T entity) {
        return new ResourceLocation("xiantu","textures/vfx/v9/plasma.png");
    }
}
