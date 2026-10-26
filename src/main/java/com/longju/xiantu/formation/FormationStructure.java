package com.longju.xiantu.formation;

import com.longju.xiantu.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.Optional;

public final class FormationStructure {
    private FormationStructure() {}

    /** Structural edits invalidate nearby loaded cores; never load chunks to perform this check. */
    public static void invalidateNear(Level level, BlockPos part) {
        if (level.isClientSide) return;
        for (int dy = -3; dy <= 0; dy++) for (int dx = -10; dx <= 10; dx++) for (int dz = -10; dz <= 10; dz++) {
            BlockPos candidate = part.offset(dx,dy,dz);
            if (level.hasChunkAt(candidate) && level.getBlockEntity(candidate) instanceof FormationCoreBlockEntity core)
                core.invalidateStructure();
        }
    }

    public static boolean isTierOneComplete(Level level, BlockPos center) {
        return isComplete(level, center, 1);
    }

    /** Visual alignment only; no extra required blocks or chunk tickets. */
    public static void alignVisualParts(Level level, BlockPos center, int tier) {
        for (int ringTier = 1; ringTier <= tier; ringTier++) for (boolean outer : new boolean[]{true,false})
            for (var tile : FormationTrace.ringStage(ringTier,outer)) {
            BlockPos pos = center.offset(tile.x(), 0, tile.z());
            var state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof FormationPartBlock)) continue;
            if (state.getValue(FormationPartBlock.TRACE) != tile.trace())
                level.setBlock(pos, state.setValue(FormationPartBlock.TRACE, tile.trace()), Block.UPDATE_CLIENTS);
        }
    }

    public static boolean isComplete(Level level, BlockPos center, int tier) {
        int radius = FormationLayout.outerRadius(tier);
        if (!level.hasChunksAt(center.offset(-radius, 0, -radius), center.offset(radius, tier + 2, radius))) return false;
        if (level.getBlockState(center).getBlock() != coreBlock(tier)) return false;
        if (level.getBlockState(center.above()).getBlock() != ModBlocks.MEDITATION_CUSHION.get()) return false;
        if (!level.getBlockState(center.above(2)).canBeReplaced()) return false;
        for (int ringTier = 1; ringTier <= tier; ringTier++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = center.offset(x, 0, z);
                    if (FormationLayout.baseRingTierAt(tier, x, z) == ringTier
                            && level.getBlockState(pos).getBlock() != partBlock(FormationPartBlock.Part.BASE, ringTier)) return false;
                    if (FormationLayout.runeRingTierAt(tier, x, z) == ringTier
                            && level.getBlockState(pos).getBlock() != partBlock(FormationPartBlock.Part.RUNE, ringTier)) return false;
                }
            }
            int ringRadius = FormationLayout.outerRadius(ringTier);
            for (BlockPos direction : new BlockPos[]{new BlockPos(1, 0, 0), new BlockPos(-1, 0, 0),
                    new BlockPos(0, 0, 1), new BlockPos(0, 0, -1)}) {
                BlockPos pillarBase = center.offset(direction.getX() * ringRadius, 0, direction.getZ() * ringRadius);
                for (int height = 1; height <= ringTier; height++) {
                    if (level.getBlockState(pillarBase.above(height)).getBlock() != partBlock(FormationPartBlock.Part.PILLAR, ringTier)) return false;
                }
            }
        }
        return true;
    }

    public static Optional<BlockPos> findTierOneCore(Level level, BlockPos partPos) {
        return findCore(level, partPos, 1);
    }

    public static Optional<BlockPos> findCore(Level level, BlockPos partPos) {
        for (int tier = 3; tier >= 1; tier--) {
            Optional<BlockPos> found = findCore(level, partPos, tier);
            if (found.isPresent()) return found;
        }
        return Optional.empty();
    }

    private static Optional<BlockPos> findCore(Level level, BlockPos partPos, int tier) {
        int radius = FormationLayout.outerRadius(tier);
        for (int yOffset = 0; yOffset >= -tier; yOffset--) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos candidate = partPos.offset(x, yOffset, z);
                    if (isComplete(level, candidate, tier)) return Optional.of(candidate);
                }
            }
        }
        return Optional.empty();
    }

    public static Block coreBlock(int tier) {
        return switch (tier) {
            case 1 -> ModBlocks.FORMATION_CORE.get();
            case 2 -> ModBlocks.FORMATION_CORE_2.get();
            case 3 -> ModBlocks.FORMATION_CORE_3.get();
            default -> throw new IllegalArgumentException("Invalid formation tier: " + tier);
        };
    }

    public static Block partBlock(FormationPartBlock.Part part, int tier) {
        return switch (part) {
            case BASE -> switch (tier) {
                case 1 -> ModBlocks.ARRAY_BASE_1.get();
                case 2 -> ModBlocks.ARRAY_BASE_2.get();
                case 3 -> ModBlocks.ARRAY_BASE_3.get();
                default -> throw new IllegalArgumentException("Invalid formation tier: " + tier);
            };
            case RUNE -> switch (tier) {
                case 1 -> ModBlocks.ARRAY_RUNE_1.get();
                case 2 -> ModBlocks.ARRAY_RUNE_2.get();
                case 3 -> ModBlocks.ARRAY_RUNE_3.get();
                default -> throw new IllegalArgumentException("Invalid formation tier: " + tier);
            };
            case PILLAR -> switch (tier) {
                case 1 -> ModBlocks.ARRAY_PILLAR_1.get();
                case 2 -> ModBlocks.ARRAY_PILLAR_2.get();
                case 3 -> ModBlocks.ARRAY_PILLAR_3.get();
                default -> throw new IllegalArgumentException("Invalid formation tier: " + tier);
            };
        };
    }
}
