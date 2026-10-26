package com.longju.xiantu.medicine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;

public final class MedicineSeedItem extends Item {
    private final MedicineDefinition definition;

    public MedicineSeedItem(Properties properties, MedicineDefinition definition) {
        super(properties);
        this.definition = definition;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        var level = context.getLevel();
        BlockPos plantPos = context.getClickedPos().relative(context.getClickedFace());
        if (context.getClickedFace() != net.minecraft.core.Direction.UP) {
            return InteractionResult.PASS;
        }
        if (level.isOutsideBuildHeight(plantPos) || !level.getWorldBorder().isWithinBounds(plantPos)
                || !level.getBlockState(plantPos).canBeReplaced(new net.minecraft.world.item.context.BlockPlaceContext(context))
                || !level.getFluidState(plantPos).isEmpty()
                || (context.getPlayer() != null && !context.getPlayer().mayUseItemAt(plantPos,
                        context.getClickedFace(), context.getItemInHand()))) return InteractionResult.FAIL;
        BlockState ground = level.getBlockState(plantPos.below());
        int soilTier = ground.getBlock() instanceof MedicineFarmlandBlock farmland ? farmland.soilTier() : -1;
        if (!MedicineRules.canPlant(definition.tier(), ground.is(net.minecraft.world.level.block.Blocks.FARMLAND), soilTier)) {
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide) {
            var crop = MedicineRegistry.CROPS.get(definition.registryId()).get();
            if (!crop.defaultBlockState().canSurvive(level, plantPos)) return InteractionResult.FAIL;
            if (!level.setBlock(plantPos, crop.defaultBlockState(), 3)) return InteractionResult.FAIL;
            if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) context.getItemInHand().shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
