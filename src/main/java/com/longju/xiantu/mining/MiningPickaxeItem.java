package com.longju.xiantu.mining;

import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ItemStack;

public final class MiningPickaxeItem extends PickaxeItem {
    public MiningPickaxeItem(net.minecraft.world.item.Tier tier, int durability) {
        super(tier, 1, -2.8F,
                new Properties().durability(durability));
    }

    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        com.longju.xiantu.client.GltfItemRenderer.install(consumer);
    }

    public static MiningPickaxeItem hanxing() { return new MiningPickaxeItem(MiningTiers.HANXING, 520); }
    public static MiningPickaxeItem jiuyou() { return new MiningPickaxeItem(MiningTiers.JIUYOU, 1120); }
    public static MiningPickaxeItem hundun() { return new MiningPickaxeItem(MiningTiers.HUNDUN, 2200); }

    @Override public void onCraftedBy(ItemStack stack,net.minecraft.world.level.Level level,net.minecraft.world.entity.player.Player player){
        super.onCraftedBy(stack,level,player);
        // Vanilla invokes this before shift-click copies the result into inventory,
        // and again from ResultSlot. Never reroll that second callback or a preview.
        if(level.isClientSide||stack.hasTag()&&stack.getTag().contains("quality"))return;
        int quality=com.longju.xiantu.refining.RefiningRules.rollQuality(player.getRandom().nextInt(10000),
                com.longju.xiantu.refining.RefiningRules.qualityWeights(com.longju.xiantu.refining.RefiningProgress.level(player),1,1,0));
        stack.getOrCreateTag().putInt("quality",quality);
    }

    public static int quality(ItemStack stack) {
        return Math.max(1, Math.min(5, stack.hasTag() ? stack.getTag().getInt("quality") : 1));
    }
}
