package com.longju.xiantu.medicine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

public final class MedicineFarmlandBlock extends Block {
    public static final IntegerProperty MOISTURE = IntegerProperty.create("moisture", 0, 7);
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 15, 16);
    private final int soilTier;

    public MedicineFarmlandBlock(Properties properties, int soilTier) {
        super(properties.randomTicks());
        this.soilTier = soilTier;
        registerDefaultState(defaultBlockState().setValue(MOISTURE, 0));
    }

    public int soilTier() { return soilTier; }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(MOISTURE); }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
    @Override public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }

    @Override
    public void randomTick(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, net.minecraft.util.RandomSource random) {
        boolean hydrated = false;
        int radius = soilTier >= 4 ? 4 : soilTier == 0 ? 4 : 8;
        for (int dx = -radius; dx <= radius && !hydrated; dx++) {
            for (int dz = -radius; dz <= radius && !hydrated; dz++) {
                for (int dy = 0; dy <= 1; dy++) {
                    BlockState water = level.getBlockState(pos.offset(dx, dy, dz));
                    if (water.is(net.minecraft.world.level.block.Blocks.WATER) && water.getFluidState().isSource()) {
                        hydrated = true;
                        break;
                    }
                }
            }
        }
        int moisture = hydrated ? 7 : Math.max(0, state.getValue(MOISTURE) - 1);
        if (moisture != state.getValue(MOISTURE)) level.setBlock(pos, state.setValue(MOISTURE, moisture), 2);
    }

    @Override public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return List.of(new ItemStack(MedicineRegistry.SOIL_ITEMS.get(soilTier).get()));
    }
}
