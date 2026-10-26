package com.longju.xiantu.building;

import com.longju.xiantu.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Native container persistence and automation, with event-driven drawer visuals. */
public final class JadeCabinetBlockEntity extends RandomizableContainerBlockEntity {
    private NonNullList<ItemStack> items = NonNullList.withSize(54,ItemStack.EMPTY);
    private final ContainerOpenersCounter openers = new ContainerOpenersCounter() {
        @Override protected void onOpen(Level level, BlockPos pos, BlockState state) { drawers(true); }
        @Override protected void onClose(Level level, BlockPos pos, BlockState state) { drawers(false); }
        @Override protected void openerCountChanged(Level level, BlockPos pos, BlockState state, int before, int after) {}
        @Override protected boolean isOwnContainer(Player p) { return p.containerMenu instanceof ChestMenu menu && menu.getContainer() == JadeCabinetBlockEntity.this; }
    };
    public JadeCabinetBlockEntity(BlockPos pos,BlockState state) { super(ModBlockEntities.CLOUD_JADE_CABINET.get(),pos,state); }
    @Override public int getContainerSize() { return 54; }
    @Override protected NonNullList<ItemStack> getItems() { return items; }
    @Override protected void setItems(NonNullList<ItemStack> list) { items=list; }
    @Override protected Component getDefaultName() { return Component.translatable("container.xiantu.cloud_jade_cabinet"); }
    @Override protected AbstractContainerMenu createMenu(int id, Inventory inventory) { return ChestMenu.sixRows(id,inventory,this); }
    @Override public void load(CompoundTag tag) {
        super.load(tag); items=NonNullList.withSize(54,ItemStack.EMPTY);
        if(!tryLoadLootTable(tag)) ContainerHelper.loadAllItems(tag,items);
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag); if(!trySaveLootTable(tag)) ContainerHelper.saveAllItems(tag,items);
    }
    @Override public void startOpen(Player player) {
        if(!isRemoved() && !player.isSpectator()) openers.incrementOpeners(player,level,worldPosition,getBlockState());
    }
    @Override public void stopOpen(Player player) {
        if(!isRemoved() && !player.isSpectator()) openers.decrementOpeners(player,level,worldPosition,getBlockState());
    }
    public void recheckOpen() {
        if(!isRemoved()) {
            openers.recheckOpeners(level,worldPosition,getBlockState());
            // OPEN is persisted in block states, whereas viewer counts are transient.
            // A fresh counter already at zero does not invoke onClose by itself.
            if(openers.getOpenerCount()==0 && getBlockState().getValue(JadeCabinetBlock.OPEN)) drawers(false);
        }
    }
    private void drawers(boolean open) {
        if(level==null || level.isClientSide || !(getBlockState().getBlock() instanceof JadeCabinetBlock block)) return;
        for(int part=0;part<4;part++) {
            BlockPos p=block.sectionPosition(worldPosition,getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING),part);
            BlockState s=level.getBlockState(p);
            if(s.is(block) && s.getValue(JadeCabinetBlock.SECTION)==part) level.setBlock(p,s.setValue(JadeCabinetBlock.OPEN,open),3);
        }
        level.playSound(null,worldPosition,open?SoundEvents.BARREL_OPEN:SoundEvents.BARREL_CLOSE,SoundSource.BLOCKS,0.5F,0.9F+level.random.nextFloat()*0.1F);
    }
    @Override public void onLoad() {
        super.onLoad();
        if(level!=null && !level.isClientSide && getBlockState().getValue(JadeCabinetBlock.OPEN)) {
            // Recheck once after sections in adjacent chunks finish loading; no idle ticker.
            level.scheduleTick(worldPosition,getBlockState().getBlock(),1);
        }
    }
    @Override public void setChanged() {
        super.setChanged();
        if(level!=null && !level.isClientSide && getBlockState().getBlock() instanceof JadeCabinetBlock block) {
            for(int part=0;part<4;part++) level.updateNeighbourForOutputSignal(
                    block.sectionPosition(worldPosition,getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING),part),block);
        }
    }
}
