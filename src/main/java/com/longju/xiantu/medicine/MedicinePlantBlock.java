package com.longju.xiantu.medicine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.ArrayList;
import java.util.List;

/** One-shot wild resource node rendered as a vanilla-style crossed plant. */
public final class MedicinePlantBlock extends BushBlock {
    @Override public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state, BlockGetter level,
            BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
        return box(2, 0, 2, 14, 14, 14);
    }
    private final MedicineDefinition definition;

    public MedicinePlantBlock(Properties properties, MedicineDefinition definition) {
        super(properties);
        this.definition = definition;
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(Blocks.GRASS_BLOCK);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        ArrayList<ItemStack> drops = new ArrayList<>();
        int count = definition.tier() == 1 ? 1 + builder.getLevel().random.nextInt(3)
                : definition.tier() <= 3 ? 1 + builder.getLevel().random.nextInt(2) : 1;
        drops.add(new ItemStack(MedicineRegistry.herb(definition.registryId()), count));
        Entity harvester = builder.getOptionalParameter(LootContextParams.THIS_ENTITY);
        if (definition.seedItem() && harvester instanceof Player
                && builder.getLevel().random.nextFloat() < MedicineRules.wildSeedChance(definition.tier())) {
            drops.add(new ItemStack(MedicineRegistry.seed(definition.registryId()), 1));
        }
        return drops;
    }

}
