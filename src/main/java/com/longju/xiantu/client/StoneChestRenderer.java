package com.longju.xiantu.client;

import com.longju.xiantu.building.StoneChestBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Reuses resource-baked Blockbench meshes; only the native lid angle changes each frame. */
public final class StoneChestRenderer implements BlockEntityRenderer<StoneChestBlockEntity> {
    public StoneChestRenderer(BlockEntityRendererProvider.Context context) {}

    @Override public void render(StoneChestBlockEntity chest, float partialTick, PoseStack pose,
                                 MultiBufferSource buffers, int light, int overlay) {
        BlockState state=chest.getBlockState();
        String type=state.getValue(BlockStateProperties.CHEST_TYPE).getSerializedName();
        float angle=chest.getOpenNess(partialTick);
        angle=1.0F-(float)Math.pow(1.0F-angle,3);
        pose.pushPose();
        pose.translate(0.5,0,0.5);
        pose.mulPose(Axis.YP.rotationDegrees(-state.getValue(BlockStateProperties.HORIZONTAL_FACING).toYRot()+180));
        pose.translate(-0.5,0,-0.5);
        draw("base_"+type,state,pose,buffers,light,overlay);
        pose.translate(0,10.0/16,15.0/16);
        // These JSON models use upward-positive Y, unlike native inverted ModelPart chest coordinates.
        pose.mulPose(Axis.XP.rotationDegrees(90*angle));
        pose.translate(0,-10.0/16,-15.0/16);
        draw("lid_"+type,state,pose,buffers,light,overlay);
        pose.popPose();
    }

    private static void draw(String part,BlockState state,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        Minecraft client=Minecraft.getInstance();
        BakedModel model=client.getModelManager().getModel(new ResourceLocation("xiantu","block/black_rock_chest_"+part));
        client.getBlockRenderer().getModelRenderer().renderModel(pose.last(),buffers.getBuffer(RenderType.solid()),
                state,model,1,1,1,light,overlay);
    }
}
