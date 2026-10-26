package com.longju.xiantu.qa;

import com.longju.xiantu.mining.SpiritOreBlock;
import com.longju.xiantu.mining.MiningTiers;
import com.longju.xiantu.registry.ModBlocks;
import com.longju.xiantu.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.server.TickTask;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
public final class OreMiningIntegration {
    private OreMiningIntegration() {}

    public static int run(ServerPlayer player) {
        if (!isQaWorld(player)) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "QA commands require a Xiantu-QA disposable world"));
            return 0;
        }

        checkValue(player, "Hanxing harvest level is 4", MiningTiers.HANXING.getLevel() == 4);
        checkValue(player, "Jiuyou harvest level is 5", MiningTiers.JIUYOU.getLevel() == 5);
        checkValue(player, "Hundun harvest level is 6", MiningTiers.HUNDUN.getLevel() == 6);

        check(player, "stone harvests tier one", Items.STONE_PICKAXE, ModBlocks.FAN_LING_SHI_ORE.get(), true);
        check(player, "stone cannot harvest tier two", Items.STONE_PICKAXE, ModBlocks.LING_JING_ORE.get(), false);
        check(player, "iron harvests tier two", Items.IRON_PICKAXE, ModBlocks.LING_JING_ORE.get(), true);
        check(player, "iron cannot harvest tier three", Items.IRON_PICKAXE, ModBlocks.LING_YU_ORE.get(), false);
        check(player, "Hanxing harvests tier three", ModItems.HANXING_PICKAXE.get(), ModBlocks.LING_YU_ORE.get(), true);
        check(player, "Hanxing cannot harvest tier four", ModItems.HANXING_PICKAXE.get(), ModBlocks.XUAN_JING_ORE.get(), false);
        check(player, "Jiuyou harvests tier four", ModItems.JIUYOU_PICKAXE.get(), ModBlocks.XUAN_JING_ORE.get(), true);
        check(player, "Jiuyou harvests tier six", ModItems.JIUYOU_PICKAXE.get(), ModBlocks.TIAN_JING_ORE.get(), true);
        check(player, "Jiuyou cannot harvest tier seven", ModItems.JIUYOU_PICKAXE.get(), ModBlocks.XIAN_YU_ORE.get(), false);
        check(player, "Hundun harvests tier six", ModItems.HUNDUN_PICKAXE.get(), ModBlocks.TIAN_JING_ORE.get(), true);
        check(player, "Hundun harvests tier eight", ModItems.HUNDUN_PICKAXE.get(), ModBlocks.DAO_SHI_ORE.get(), true);
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "[Ore tier QA] 11 tool-tier assertions passed"));
        return 1;
    }

    private static void checkValue(ServerPlayer player, String label, boolean condition) {
        String result = "[Ore tier " + (condition ? "PASS" : "FAIL") + "] " + label;
        player.sendSystemMessage(Component.literal(result));
        if (!condition) throw new AssertionError(result);
    }

    public static int runSurvivalMining(ServerPlayer player) {
        if (!isQaWorld(player)) {
            player.sendSystemMessage(Component.literal("QA commands require a Xiantu-QA disposable world"));
            return 0;
        }

        var level = player.serverLevel();
        var server = player.getServer();
        GameType oldMode = player.gameMode.getGameModeForPlayer();
        ItemStack oldHand = player.getMainHandItem().copy();
        int oldXp = player.getPersistentData().getInt("xiantu_spirit_vein_xp");
        int oldLevel = player.getPersistentData().getInt("xiantu_spirit_vein_level");
        List<BlockPos> positions = new ArrayList<>();
        List<BlockState> oldStates = new ArrayList<>();

        int testY = Math.min(level.getMaxBuildHeight() - 2, player.blockPosition().getY() + 2);
        BlockPos origin = new BlockPos(player.blockPosition().getX() + 4, testY, player.blockPosition().getZ());
        for (int i = 0; i < 4; i++) {
            BlockPos pos = origin.offset(i * 4, 0, 0);
            positions.add(pos);
            oldStates.add(level.getBlockState(pos));
        }
        clearTestDrops(level, positions);

        try {
            player.setGameMode(GameType.SURVIVAL);
            player.getPersistentData().putInt("xiantu_spirit_vein_xp", 0);
            player.getPersistentData().putInt("xiantu_spirit_vein_level", 0);

            mine(player, positions.get(0), ModBlocks.FAN_LING_SHI_ORE.get(), new ItemStack(Items.STONE_PICKAXE));
            ItemStack enchantedIron = new ItemStack(Items.IRON_PICKAXE);
            enchantedIron.enchant(Enchantments.SILK_TOUCH, 1);
            enchantedIron.enchant(Enchantments.BLOCK_FORTUNE, 3);
            mine(player, positions.get(1), ModBlocks.LING_JING_ORE.get(), enchantedIron);
            mine(player, positions.get(2), ModBlocks.LING_JING_ORE.get(), new ItemStack(Items.STONE_PICKAXE));

            player.getPersistentData().putInt("xiantu_spirit_vein_xp", 127);
            ItemStack hanxing = new ItemStack(ModItems.HANXING_PICKAXE.get());
            hanxing.getOrCreateTag().putInt("quality", 5);
            mine(player, positions.get(3), ModBlocks.FAN_LING_SHI_ORE.get(), hanxing);

            server.tell(new TickTask(server.getTickCount() + 1,
                    () -> finishSurvivalMining(player, oldMode, oldHand, oldXp, oldLevel, positions, oldStates)));
            player.sendSystemMessage(Component.literal("[Ore mining QA] Survival harvest checks queued"));
            return 1;
        } catch (RuntimeException failure) {
            clearTestDrops(level, positions);
            restore(player, oldMode, oldHand, oldXp, oldLevel, positions, oldStates);
            throw failure;
        }
    }

    public static int scanWorldgen(ServerPlayer player) {
        if (!isQaWorld(player)) {
            player.sendSystemMessage(Component.literal("QA commands require a Xiantu-QA disposable world"));
            return 0;
        }
        var level = player.serverLevel();
        Block[] ores = {
                ModBlocks.FAN_LING_SHI_ORE.get(), ModBlocks.LING_JING_ORE.get(),
                ModBlocks.LING_YU_ORE.get(), ModBlocks.XUAN_JING_ORE.get(),
                ModBlocks.DI_SUI_ORE.get(), ModBlocks.TIAN_JING_ORE.get(),
                ModBlocks.XIAN_YU_ORE.get(), ModBlocks.DAO_SHI_ORE.get()
        };
        Map<Block, Integer> counts = new IdentityHashMap<>();
        for (Block ore : ores) counts.put(ore, 0);

        int centerChunkX = player.blockPosition().getX() >> 4;
        int centerChunkZ = player.blockPosition().getZ() >> 4;
        int minY = level.getMinBuildHeight();
        int maxY = Math.min(level.getMaxBuildHeight() - 1, 96);
        player.sendSystemMessage(Component.literal("[Ore worldgen QA] Generating and scanning 16x16 chunks"));
        for (int chunkX = centerChunkX - 8; chunkX < centerChunkX + 8; chunkX++) {
            for (int chunkZ = centerChunkZ - 8; chunkZ < centerChunkZ + 8; chunkZ++) {
                level.getChunk(chunkX, chunkZ);
                int minX = chunkX << 4;
                int minZ = chunkZ << 4;
                for (int x = minX; x < minX + 16; x++) {
                    for (int z = minZ; z < minZ + 16; z++) {
                        for (int y = minY; y <= maxY; y++) {
                            Block block = level.getBlockState(new BlockPos(x, y, z)).getBlock();
                            Integer count = counts.get(block);
                            if (count != null) counts.put(block, count + 1);
                        }
                    }
                }
            }
        }
        for (int i = 0; i < ores.length; i++) {
            player.sendSystemMessage(Component.literal("[Ore worldgen] tier " + (i + 1)
                    + " generated blocks: " + counts.get(ores[i])));
        }
        boolean baseTiersPresent = counts.get(ores[0]) > 0 && counts.get(ores[1]) > 0
                && counts.get(ores[2]) > 0 && counts.get(ores[3]) > 0;
        player.sendSystemMessage(Component.literal("[Ore worldgen QA " + (baseTiersPresent ? "PASS" : "FAIL")
                + "] first four tiers present in generated overworld chunks"));
        if (!baseTiersPresent) throw new AssertionError("Expected common ore tiers were absent in 256 generated chunks");
        return 1;
    }

    private static void mine(ServerPlayer player, BlockPos pos, Block block, ItemStack tool) {
        var level = player.serverLevel();
        level.setBlock(pos, block.defaultBlockState(), 3);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, tool);
        if (!player.gameMode.destroyBlock(pos)) {
            throw new AssertionError("Server survival destroyBlock rejected " + block);
        }
        if (!level.getBlockState(pos).isAir()) {
            throw new AssertionError("Server survival destroyBlock did not remove " + block);
        }
    }

    private static void finishSurvivalMining(ServerPlayer player, GameType oldMode, ItemStack oldHand,
                                             int oldXp, int oldLevel, List<BlockPos> positions,
                                             List<BlockState> oldStates) {
        try {
            var level = player.serverLevel();
            checkResult(player, countDrops(level, positions.get(0), ModItems.FAN_LING_SHI.get()) == 1,
                    "Stone pickaxe gets one tier-one base drop");
            checkResult(player, countDrops(level, positions.get(1), ModItems.LING_JING.get()) == 1,
                    "Iron pickaxe gets one tier-two drop despite Fortune and Silk Touch");
            checkResult(player, countDrops(level, positions.get(2), ModItems.LING_JING.get()) == 0,
                    "Stone pickaxe breaks tier two without drops");
            checkResult(player, countDrops(level, positions.get(3), ModItems.FAN_LING_SHI.get()) >= 1,
                    "Cultivation pickaxe retains the fixed base drop");
            checkResult(player, player.getPersistentData().getInt("xiantu_spirit_vein_xp") == 128
                            && player.getPersistentData().getInt("xiantu_spirit_vein_level") == 1,
                    "Valid survival cultivation-pick mining grants vein XP and level");
            player.sendSystemMessage(Component.literal("[Ore mining QA] All survival harvest assertions passed"));
        } finally {
            clearTestDrops(player.serverLevel(), positions);
            restore(player, oldMode, oldHand, oldXp, oldLevel, positions, oldStates);
        }
    }

    private static void clearTestDrops(net.minecraft.server.level.ServerLevel level, List<BlockPos> positions) {
        for (int i = 0; i < positions.size(); i++) {
            net.minecraft.world.item.Item expected = i == 1 || i == 2
                    ? ModItems.LING_JING.get() : ModItems.FAN_LING_SHI.get();
            for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class,
                    new AABB(positions.get(i)).inflate(1.5D))) {
                if (entity.getItem().is(expected)) entity.discard();
            }
        }
    }

    private static int countDrops(net.minecraft.server.level.ServerLevel level, BlockPos pos,
                                  net.minecraft.world.item.Item item) {
        AABB area = new AABB(pos).inflate(1.5D);
        return level.getEntitiesOfClass(ItemEntity.class, area).stream()
                .filter(entity -> entity.getItem().is(item))
                .mapToInt(entity -> entity.getItem().getCount()).sum();
    }

    private static void checkResult(ServerPlayer player, boolean condition, String label) {
        String line = "[Ore mining " + (condition ? "PASS" : "FAIL") + "] " + label;
        player.sendSystemMessage(Component.literal(line));
        if (!condition) throw new AssertionError(line);
    }

    private static void restore(ServerPlayer player, GameType mode, ItemStack hand, int xp, int veinLevel,
                                List<BlockPos> positions, List<BlockState> states) {
        var level = player.serverLevel();
        for (int i = 0; i < positions.size(); i++) level.setBlock(positions.get(i), states.get(i), 3);
        player.getPersistentData().putInt("xiantu_spirit_vein_xp", xp);
        player.getPersistentData().putInt("xiantu_spirit_vein_level", veinLevel);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, hand);
        player.setGameMode(mode);
    }

    private static boolean isQaWorld(ServerPlayer player) {
        return player.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                .normalize().getFileName().toString().startsWith("Xiantu-QA");
    }

    private static void check(ServerPlayer player, String label, net.minecraft.world.item.Item tool,
                              net.minecraft.world.level.block.Block block, boolean expected) {
        boolean actual = new ItemStack(tool).isCorrectToolForDrops(block.defaultBlockState());
        if (actual != expected) {
            throw new AssertionError(label + ": expected " + expected + " but was " + actual);
        }
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("[Ore tier PASS] " + label));
    }
}
