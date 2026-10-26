package com.longju.xiantu.qa;

import com.longju.xiantu.formation.FormationCoreBlockEntity;
import com.longju.xiantu.formation.FormationLayout;
import com.longju.xiantu.formation.FormationPartBlock;
import com.longju.xiantu.formation.MeditationCushionBlock;
import com.longju.xiantu.formation.FormationSeatEntity;
import com.longju.xiantu.formation.FormationStructure;
import com.longju.xiantu.registry.ModBlocks;
import com.longju.xiantu.registry.ModEntities;
import com.longju.xiantu.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
public final class FormationIntegration {
    private static final Logger LOGGER = LogManager.getLogger("Xiantu QA");

    public static int run(ServerPlayer player) {
        if (!player.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                .normalize().getFileName().toString().startsWith("Xiantu-QA")) {
            player.sendSystemMessage(Component.literal("QA commands require a Xiantu-QA disposable world"));
            return 0;
        }

        var level = player.serverLevel();
        BlockPos center = player.blockPosition().offset(0, -1, 40);
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                BlockPos pos = center.offset(x, 0, z);
                if (FormationLayout.isBaseRing(x, z)) level.setBlock(pos, ModBlocks.ARRAY_BASE_1.get().defaultBlockState(), 3);
                else if (FormationLayout.isRuneRing(x, z)) level.setBlock(pos, ModBlocks.ARRAY_RUNE_1.get().defaultBlockState(), 3);
            }
        }
        level.setBlock(center, Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(center, ModBlocks.FORMATION_CORE.get().defaultBlockState(), 3);
        level.setBlock(center.above(), ModBlocks.MEDITATION_CUSHION.get().defaultBlockState(), 3);
        level.setBlock(center.above(2), Blocks.AIR.defaultBlockState(), 3);
        for (BlockPos direction : new BlockPos[]{new BlockPos(4, 0, 0), new BlockPos(-4, 0, 0),
                new BlockPos(0, 0, 4), new BlockPos(0, 0, -4)}) {
            level.setBlock(center.offset(direction).above(), ModBlocks.ARRAY_PILLAR_1.get().defaultBlockState(), 3);
        }

        check(player, FormationStructure.isTierOneComplete(level, center), "Generated tier-one structure validates");
        check(player, FormationStructure.findTierOneCore(level, center.offset(0, 1, -4)).orElse(BlockPos.ZERO).equals(center),
                "Pillar resolves to its array core");
        if (!(level.getBlockEntity(center) instanceof FormationCoreBlockEntity core)) {
            throw new IllegalStateException("Formation core block entity was not created");
        }
        core.inventory().setStackInSlot(0, new ItemStack(ModItems.SPIRIT_STONE.get(), 2));
        core.inventory().setStackInSlot(1, new ItemStack(ModItems.SPIRIT_STONE.get(), 2));
        for (int i = 0; i < 20; i++) FormationCoreBlockEntity.serverTick(level, center, level.getBlockState(center), core);
        check(player, core.runtimeTicks() == 2380 && core.bonusTicks() == 2380,
                "Runtime and bonus consume twenty actual ticks after activation");
        check(player, core.inventory().getStackInSlot(0).getCount() == 1 && core.inventory().getStackInSlot(1).getCount() == 1,
                "Runtime and bonus slots consume independently");

        int idleBonusTicks = core.bonusTicks();
        for (int i = 0; i < 20; i++) FormationCoreBlockEntity.serverTick(level, center, level.getBlockState(center), core);
        check(player, core.bonusTicks() == idleBonusTicks - 20,
                "Bonus duration decreases while the formation runs without a meditator");

        long runtimeBeforeIdle = core.runtimeTicks();
        for (int i = 0; i < 20; i++) FormationCoreBlockEntity.serverTick(level, center, level.getBlockState(center), core);
        check(player, core.runtimeTicks() == runtimeBeforeIdle - 20
                        && core.bonusTicks() == idleBonusTicks - 40,
                "Both timers decrease while the formation runs without a meditator");

        try {
        BlockPos cushionPos = center.above();
        player.teleportTo(center.getX() + 0.5D, center.getY() + 2.0D, center.getZ() + 2.5D);
        progress(player, "Using the meditation cushion interaction");
        Block cushionBlock = ModBlocks.MEDITATION_CUSHION.get();
        InteractionResult useResult = cushionBlock.use(
                level.getBlockState(cushionPos), level, cushionPos, player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(cushionPos), net.minecraft.core.Direction.UP, cushionPos, false));
        check(player, useResult.consumesAction(), "Cushion right-click interaction is consumed");
        Entity seatEntity = player.getVehicle();
        check(player, seatEntity instanceof FormationSeatEntity, "Cushion interaction creates and mounts the seat entity");
        progress(player, "Ticking formation seat while player meditates");
        player.getCapability(com.longju.xiantu.cultivation.CultivationCapability.DATA).ifPresent(data -> {
            double progressBefore = data.progress();
            for (int i = 0; i < 20; i++) seatEntity.tick();
            com.longju.xiantu.formation.ChunkAuraService.settleSecond(level);
            check(player, data.progress() > progressBefore, "Cushion-seated player gains current-stage progress through server ticks");
        });
        player.stopRiding();
        seatEntity.discard();
        check(player, core.bonusTicks() == idleBonusTicks - 40 && core.inventory().getStackInSlot(1).getCount() == 1,
                "Meditation uses the active bonus without consuming another queued stone");
        } catch (Throwable error) {
            player.sendSystemMessage(Component.literal("[Formation QA ERROR] " + error.getClass().getSimpleName() + ": " + error.getMessage()));
            LOGGER.error("Formation QA failed during the seat integration checks", error);
            if (error instanceof RuntimeException runtimeException) throw runtimeException;
            if (error instanceof Error fatalError) throw fatalError;
            throw new IllegalStateException(error);
        }

        BlockPos missingBase = center.offset(4, 0, 0);
        level.setBlock(missingBase, Blocks.AIR.defaultBlockState(), 3);
        long runtimeBeforePause = core.runtimeTicks();
        int bonusBeforePause = core.bonusTicks();
        for (int i = 0; i < 20; i++) FormationCoreBlockEntity.serverTick(level, center, level.getBlockState(center), core);
        check(player, !core.structureValid() && core.runtimeTicks() == runtimeBeforePause && core.bonusTicks() == bonusBeforePause,
                "Invalid structure pauses both timers");
        check(player, !level.getBlockState(center).getValue(com.longju.xiantu.formation.FormationCoreBlock.ACTIVE),
                "Invalid structure deactivates the core display");

        level.setBlock(missingBase, ModBlocks.ARRAY_BASE_1.get().defaultBlockState(), 3);
        player.teleportTo(center.getX() + 0.5D, center.getY() + 2.0D, center.getZ() - 6.0D);
        BlockPos corePos = center;
        long droppedBeforeBreak = level.getEntitiesOfClass(ItemEntity.class,
                new net.minecraft.world.phys.AABB(corePos).inflate(2.0D)).stream()
                .filter(item -> item.getItem().is(ModItems.SPIRIT_STONE.get()))
                .mapToLong(item -> item.getItem().getCount()).sum();
        level.setBlock(corePos, Blocks.AIR.defaultBlockState(), 3);
        long droppedStones = level.getEntitiesOfClass(ItemEntity.class,
                new net.minecraft.world.phys.AABB(corePos).inflate(2.0D)).stream()
                .filter(item -> item.getItem().is(ModItems.SPIRIT_STONE.get()))
                .mapToLong(item -> item.getItem().getCount()).sum();
        check(player, droppedStones - droppedBeforeBreak == 2,
                "Breaking the array core drops all unconsumed stones from both slots");
        level.setBlock(corePos, ModBlocks.FORMATION_CORE.get().defaultBlockState(), 3);
        BlockPos tierTwo = center.offset(0, 0, 40);
        buildStructure(level, tierTwo, 2);
        check(player, FormationStructure.isComplete(level, tierTwo, 2), "Tier-two circular array validates with its full structure");
        BlockPos retainedTierOneRing = tierTwo.offset(4, 0, 0);
        level.setBlock(retainedTierOneRing, Blocks.AIR.defaultBlockState(), 3);
        check(player, !FormationStructure.isComplete(level, tierTwo, 2), "Tier-two array requires the retained tier-one ring");
        level.setBlock(retainedTierOneRing, ModBlocks.ARRAY_BASE_1.get().defaultBlockState(), 3);
        BlockPos tierTwoRune = tierTwo.offset(0, 0, 5);
        level.setBlock(tierTwoRune, Blocks.AIR.defaultBlockState(), 3);
        check(player, !FormationStructure.isComplete(level, tierTwo, 2), "Tier-two array requires its new rune ring");
        level.setBlock(tierTwoRune, ModBlocks.ARRAY_RUNE_2.get().defaultBlockState(), 3);
        BlockPos tierTwoBase = tierTwo.offset(7, 0, 0);
        level.setBlock(tierTwoBase, ModBlocks.ARRAY_BASE_1.get().defaultBlockState(), 3);
        check(player, !FormationStructure.isComplete(level, tierTwo, 2), "Tier-two array rejects a mixed-tier base block");
        level.setBlock(tierTwoBase, ModBlocks.ARRAY_BASE_2.get().defaultBlockState(), 3);

        BlockPos tierThree = center.offset(0, 0, 95);
        buildStructure(level, tierThree, 3);
        check(player, FormationStructure.isComplete(level, tierThree, 3), "Tier-three circular array validates with its full structure");
        BlockPos retainedTierTwoRing = tierThree.offset(7, 0, 0);
        level.setBlock(retainedTierTwoRing, Blocks.AIR.defaultBlockState(), 3);
        check(player, !FormationStructure.isComplete(level, tierThree, 3), "Tier-three array requires the retained tier-two ring");
        level.setBlock(retainedTierTwoRing, ModBlocks.ARRAY_BASE_2.get().defaultBlockState(), 3);
        BlockPos tierThreeRune = tierThree.offset(0, 0, 8);
        level.setBlock(tierThreeRune, Blocks.AIR.defaultBlockState(), 3);
        check(player, !FormationStructure.isComplete(level, tierThree, 3), "Tier-three array requires its new rune ring");
        level.setBlock(tierThreeRune, ModBlocks.ARRAY_RUNE_3.get().defaultBlockState(), 3);
        BlockPos tierThreePillar = tierThree.offset(10, 1, 0);
        level.setBlock(tierThreePillar, ModBlocks.ARRAY_PILLAR_2.get().defaultBlockState(), 3);
        check(player, !FormationStructure.isComplete(level, tierThree, 3), "Tier-three array rejects a wrong-tier conduit pillar");
        return 1;
    }

    private static void buildStructure(net.minecraft.server.level.ServerLevel level, BlockPos center, int tier) {
        int radius = FormationLayout.outerRadius(tier);
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                BlockPos pos = center.offset(x, 0, z);
                int baseTier = FormationLayout.baseRingTierAt(tier, x, z);
                int runeTier = FormationLayout.runeRingTierAt(tier, x, z);
                if (baseTier > 0) {
                    level.setBlock(pos, FormationStructure.partBlock(FormationPartBlock.Part.BASE, baseTier).defaultBlockState(), 3);
                } else if (runeTier > 0) {
                    level.setBlock(pos, FormationStructure.partBlock(FormationPartBlock.Part.RUNE, runeTier).defaultBlockState(), 3);
                }
            }
        }
        level.setBlock(center, FormationStructure.coreBlock(tier).defaultBlockState(), 3);
        level.setBlock(center.above(), ModBlocks.MEDITATION_CUSHION.get().defaultBlockState(), 3);
        level.setBlock(center.above(2), Blocks.AIR.defaultBlockState(), 3);
        for (int ringTier = 1; ringTier <= tier; ringTier++) {
            int ringRadius = FormationLayout.outerRadius(ringTier);
            for (BlockPos offset : new BlockPos[]{new BlockPos(ringRadius, 0, 0), new BlockPos(-ringRadius, 0, 0),
                    new BlockPos(0, 0, ringRadius), new BlockPos(0, 0, -ringRadius)}) {
                for (int height = 1; height <= ringTier; height++) {
                    level.setBlock(center.offset(offset).above(height),
                            FormationStructure.partBlock(FormationPartBlock.Part.PILLAR, ringTier).defaultBlockState(), 3);
                }
            }
        }
    }

    private static void check(ServerPlayer player, boolean ok, String label) {
        String line = "[Formation QA " + (ok ? "PASS" : "FAIL") + "] " + label;
        player.sendSystemMessage(Component.literal(line));
        if (!ok) throw new IllegalStateException(line);
    }

    private static void progress(ServerPlayer player, String phase) {
        String line = "[Formation QA] " + phase;
        LOGGER.info(line);
        player.sendSystemMessage(Component.literal(line));
    }
}
