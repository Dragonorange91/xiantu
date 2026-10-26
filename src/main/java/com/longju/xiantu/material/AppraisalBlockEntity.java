package com.longju.xiantu.material;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import com.longju.xiantu.registry.ModBlockEntities;
public final class AppraisalBlockEntity extends RandomizableContainerBlockEntity {
    private NonNullList<ItemStack> items=NonNullList.withSize(6,ItemStack.EMPTY);
    public AppraisalBlockEntity(BlockPos p,BlockState s){super(ModBlockEntities.APPRAISAL.get(),p,s);}
    @Override public int getContainerSize(){return 6;}
    @Override protected NonNullList<ItemStack> getItems(){return items;}
    @Override protected void setItems(NonNullList<ItemStack> n){items=n;}
    @Override protected Component getDefaultName(){return Component.literal("鉴定灵台");}
    @Override protected AbstractContainerMenu createMenu(int id,Inventory inv){return new AppraisalMenu(id,inv,this,worldPosition);}
    @Override public boolean canPlaceItem(int slot,ItemStack stack){return slot==0&&MaterialQuality.eligible(stack)&&MaterialQuality.get(stack)==0;}
    @Override public void load(CompoundTag tag){super.load(tag);items=NonNullList.withSize(6,ItemStack.EMPTY);ContainerHelper.loadAllItems(tag,items);}
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);ContainerHelper.saveAllItems(tag,items);}
    /** All preflight checks precede RNG; each item draws once and server writes the whole result. */
    public boolean identify(){
        if(level==null||level.isClientSide||isRemoved())return false;
        ItemStack input=getItem(0);
        if(!canPlaceItem(0,input)||input.getCount()>input.getMaxStackSize())return false;
        for(int slot=1;slot<=5;slot++)if(!getItem(slot).isEmpty())return false;
        int[] counts=new int[6];for(int i=0;i<input.getCount();i++)counts[MaterialQuality.roll(level.random)]++;
        for(int quality=1;quality<=5;quality++)if(counts[quality]>0){ItemStack result=input.copy();result.setCount(counts[quality]);items.set(quality,MaterialQuality.set(result,quality));}
        items.set(0,ItemStack.EMPTY);setChanged();return true;
    }
}
