package com.longju.xiantu.qa;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/** Disposable client-only check of the actual model bakery and context selection. */
@Mod.EventBusSubscriber(modid = "xiantu_qa", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ItemModelIntegration {
    private static final List<String> IDS = List.of("fan_ling_shi", "ling_jing", "ling_yu", "xuan_jing",
            "di_sui", "tian_jing", "xian_yu", "dao_shi", "hanxing_pickaxe", "jiuyou_pickaxe", "hundun_pickaxe", "qunyou_figurine");

    @SubscribeEvent
    public static void modelsBaked(ModelEvent.BakingCompleted event) {
        try {
            for (String id : IDS) {
                BakedModel wrapper = event.getModels().get(new ModelResourceLocation("xiantu", id, "inventory"));
                if (wrapper == null) throw new AssertionError(id + " missing registered inventory model");
                BakedModel gui = wrapper.applyTransform(ItemDisplayContext.GUI, new PoseStack(), false);
                BakedModel hand = wrapper.applyTransform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, new PoseStack(), false);
                int guiQuads = gui.getQuads(null, null, RandomSource.create(0)).size();
                int handQuads = hand.getQuads(null, null, RandomSource.create(0)).size();
                if (gui == hand || guiQuads < 2 || guiQuads > 1024 || handQuads != 0
                        || gui.isCustomRenderer() || !hand.isCustomRenderer())
                    throw new AssertionError(id + " failed flat GUI routing: " + guiQuads + " vs " + handQuads);
                ResourceLocation sprite = gui.getParticleIcon().contents().name();
                if (!sprite.equals(new ResourceLocation("xiantu", "item/" + id)))
                    throw new AssertionError(id + " GUI sprite incorrect: " + sprite);
                if (id.equals("qunyou_figurine") && wrapper.usesBlockLight())
                    throw new AssertionError("Figurine screenshot needs flat front GUI lighting");
                var extension = net.minecraftforge.client.extensions.common.IClientItemExtensions.of(
                        net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new ResourceLocation("xiantu", id)));
                if (!(extension.getCustomRenderer() instanceof com.longju.xiantu.client.GltfItemRenderer))
                    throw new AssertionError(id + " missing native renderer extension");
                LogUtils.getLogger().info("[Item model QA] PASS {}: GUI {} quads; native hand {} baked quads; sprite {}",
                        id, guiQuads, handQuads, sprite);
            }
            LogUtils.getLogger().info("[Item model QA] All {} actual baked GUI/GLB routes passed", IDS.size());
        } catch (RuntimeException | LinkageError | AssertionError failure) {
            LogUtils.getLogger().error("[Item model QA] FAIL model routing", failure);
        }
    }
}
