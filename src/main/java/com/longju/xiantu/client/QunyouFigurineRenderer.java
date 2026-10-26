package com.longju.xiantu.client;

import com.longju.xiantu.building.QunyouFigurineEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.resources.ResourceLocation;

/** Shares the immutable item instance and resident GPU buffers, never updates an animation. */
public final class QunyouFigurineRenderer implements BlockEntityRenderer<QunyouFigurineEntity> {
    private static final ResourceLocation ID=new ResourceLocation("xiantu","qunyou_figurine");
    public QunyouFigurineRenderer(BlockEntityRendererProvider.Context context){}
    @Override public void render(QunyouFigurineEntity block,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        pose.pushPose();
        try{
            pose.translate(.5,0,.5);
            pose.mulPose(Axis.YP.rotationDegrees(180-block.getBlockState().getValue(HorizontalDirectionalBlock.FACING).toYRot()));
            pose.translate(-.5,0,-.5);
            GltfItemRenderer.instance().renderStaticBlock(ID,pose,buffers,light,overlay);
        }finally{pose.popPose();}
    }
}
