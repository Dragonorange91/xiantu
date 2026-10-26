package com.longju.xiantu.mining;

import com.longju.xiantu.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;

public final class SpiritOreEvents {
    private SpiritOreEvents() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void breakOre(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof net.minecraft.server.level.ServerLevel level)
                || !(event.getPlayer() instanceof ServerPlayer player)
                || !(level.getBlockState(event.getPos()).getBlock() instanceof SpiritOreBlock ore)) return;

        ItemStack tool = player.getMainHandItem();
        if (event.isCanceled() || player.getAbilities().instabuild
                || !(tool.getItem() instanceof MiningPickaxeItem)
                || !tool.isCorrectToolForDrops(level.getBlockState(event.getPos()))) return;
        var pos = event.getPos().immutable();
        int oreRank = ore.tier().rank();
        int pickaxeQuality = MiningPickaxeItem.quality(tool);
        level.getServer().execute(() -> {
            if (!level.isLoaded(pos) || !level.getBlockState(pos).isAir() || !player.isAlive()) return;
            SpiritVeinProgress.award(player, SpiritOreMiningRules.experienceForOre(oreRank));
            int veinLevel = SpiritVeinProgress.level(player);
            int bonus = SpiritOreMiningRules.rollBonusDrops(veinLevel, pickaxeQuality, level.random::nextDouble);
            if (bonus > 0) {
                net.minecraft.world.level.block.Block.popResource(level, pos,
                        new ItemStack(ModItems.getSpiritStone(ore.tier()), bonus));
            }
        });
    }
}
