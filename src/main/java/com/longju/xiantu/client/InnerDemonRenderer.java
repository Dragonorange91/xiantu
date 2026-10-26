package com.longju.xiantu.client;

import com.longju.xiantu.entity.InnerDemonEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.resources.ResourceLocation;

public final class InnerDemonRenderer extends HumanoidMobRenderer<InnerDemonEntity, PlayerModel<InnerDemonEntity>> {
    public InnerDemonRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), context.getModelManager()));
        addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override public ResourceLocation getTextureLocation(InnerDemonEntity entity) {
        if (entity.ownerId() == null) return DefaultPlayerSkin.getDefaultSkin(new java.util.UUID(0L, 0L));
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() != null) {
            PlayerInfo owner = minecraft.getConnection().getPlayerInfo(entity.ownerId());
            if (owner != null) return owner.getSkinLocation();
        }
        return DefaultPlayerSkin.getDefaultSkin(entity.ownerId());
    }
}
