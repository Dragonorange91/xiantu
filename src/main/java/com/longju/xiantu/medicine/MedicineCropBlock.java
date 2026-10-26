package com.longju.xiantu.medicine;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;
import java.util.List;

/** Server-grown reharvestable herb crop using the vanilla AGE/cross model pattern. */
public final class MedicineCropBlock extends BushBlock implements BonemealableBlock {
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 3);
    private static final net.minecraft.world.phys.shapes.VoxelShape[] SHAPES = {
            box(5, -1, 5, 11, 3, 11), box(4, -1, 4, 12, 6, 12),
            box(2, -1, 2, 14, 10, 14), box(1, -1, 1, 15, 14, 15)};
    @Override public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state, BlockGetter level,
            BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
        return SHAPES[state.getValue(AGE)];
    }
    private final MedicineDefinition definition;

    public MedicineCropBlock(Properties properties, MedicineDefinition definition) {
        super(properties);
        this.definition = definition;
        registerDefaultState(stateDefinition.any().setValue(AGE, 0));
    }

    public MedicineDefinition definition() {
        return definition;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        if (state.is(Blocks.FARMLAND)) return definition.tier() == 1;
        return state.getBlock() instanceof MedicineFarmlandBlock farmland
                && farmland.soilTier() >= Math.max(0, definition.tier() - 1);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return mayPlaceOn(level.getBlockState(pos.below()), level, pos.below());
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(AGE) >= 3 || !hasGrowingConditions(level, pos)) return;
        int soilTier = level.getBlockState(pos.below()).getBlock() instanceof MedicineFarmlandBlock farmland
                ? farmland.soilTier() : 0;
        int seconds = MedicineRules.growthSeconds(definition.tier(), soilTier);
        double stageTicks = seconds * 20.0D / 3.0D;
        double randomTickInterval = 4096.0D / Math.max(1, level.getGameRules().getInt(net.minecraft.world.level.GameRules.RULE_RANDOMTICKING));
        float chance = (float) Math.min(1.0D, randomTickInterval / stageTicks);
        if (random.nextFloat() < chance) {
            level.setBlock(pos, state.setValue(AGE, state.getValue(AGE) + 1), 2);
        }
    }

    private boolean hasGrowingConditions(LevelReader level, BlockPos pos) {
        if (level.getRawBrightness(pos.above(), 0) < 9) return false;
        BlockPos soilPos = pos.below();
        int radius = level.getBlockState(soilPos).getBlock() instanceof MedicineFarmlandBlock farmland
                ? farmland.soilTier() >= 4 ? 4 : 8 : 4;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = 0; dy <= 1; dy++) {
                    BlockState water = level.getBlockState(soilPos.offset(dx, dy, dz));
                    if (water.is(Blocks.WATER) && water.getFluidState().isSource()) return true;
                }
            }
        }
        return false;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || state.getValue(AGE) < 3) return InteractionResult.PASS;
        if (!level.isClientSide) {
            player.getInventory().placeItemBackInInventory(new ItemStack(MedicineRegistry.herb(definition.registryId()),
                    level.random.nextBoolean() ? 1 : 2));
            if (definition.seedItem() && level.random.nextFloat() < MedicineRules.cultivatedSeedChance(definition.tier())) {
                player.getInventory().placeItemBackInInventory(new ItemStack(MedicineRegistry.seed(definition.registryId()), 1));
            }
            level.setBlock(pos, state.setValue(AGE, 0), 2);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        if (state.getValue(AGE) < 3) return List.of();
        ArrayList<ItemStack> drops = new ArrayList<>();
        drops.add(new ItemStack(MedicineRegistry.herb(definition.registryId()), builder.getLevel().random.nextBoolean() ? 1 : 2));
        if (definition.seedItem()
                && builder.getOptionalParameter(LootContextParams.THIS_ENTITY) instanceof Player
                && builder.getLevel().random.nextFloat() < MedicineRules.cultivatedSeedChance(definition.tier())) {
            drops.add(new ItemStack(MedicineRegistry.seed(definition.registryId()), 1));
        }
        return drops;
    }

    @Override public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, boolean client) { return false; }
    @Override public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) { return false; }
    @Override public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {}
}
