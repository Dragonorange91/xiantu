package com.longju.xiantu.mining;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.ForgeTier;
import net.minecraftforge.common.TierSortingRegistry;

import java.util.List;

public final class MiningTiers {
    public static final TagKey<Block> NEEDS_HANXING = blockTag("needs_hanxing_tool");
    public static final TagKey<Block> NEEDS_JIUYOU = blockTag("needs_jiuyou_tool");
    public static final TagKey<Block> NEEDS_HUNDUN = blockTag("needs_hundun_tool");

    public static final Tier HANXING = register("hanxing", 4, 520, 7.5F, 14,
            NEEDS_HANXING, Tiers.NETHERITE);
    public static final Tier JIUYOU = register("jiuyou", 5, 1120, 8.5F, 18,
            NEEDS_JIUYOU, HANXING);
    public static final Tier HUNDUN = register("hundun", 6, 2200, 9.5F, 22,
            NEEDS_HUNDUN, JIUYOU);

    private MiningTiers() {}

    public static void bootstrap() {
        // Forces tier registration before item registry suppliers are evaluated.
    }

    private static Tier register(String name, int level, int uses, float speed, int enchantability,
                                 TagKey<Block> tag, Tier after) {
        Tier tier = new ForgeTier(level, uses, speed, 0.0F, enchantability, tag,
                () -> Ingredient.of(net.minecraft.world.item.Items.NETHERITE_INGOT));
        return TierSortingRegistry.registerTier(tier, new ResourceLocation("xiantu", name),
                List.of(after), List.of());
    }

    private static TagKey<Block> blockTag(String name) {
        return TagKey.create(Registries.BLOCK, new ResourceLocation("xiantu", name));
    }
}
