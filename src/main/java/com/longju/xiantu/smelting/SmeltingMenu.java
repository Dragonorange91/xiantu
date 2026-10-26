package com.longju.xiantu.smelting;

import com.longju.xiantu.item.SpiritStoneItem;
import com.longju.xiantu.registry.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public final class SmeltingMenu extends AbstractContainerMenu {
    public final Container materials;
    public final BlockPos pos;
    private final ContainerData data;
    public SmeltingMenu(int id,Inventory inv,FriendlyByteBuf b){this(id,inv,new SimpleContainer(3),b.readBlockPos());}
    public SmeltingMenu(int id,Inventory inv,Container c,BlockPos p){
        super(ModMenus.SMELTING.get(),id);materials=c;pos=p;
        data=c instanceof SmeltingBlockEntity be?new ContainerData(){
            public int getCount(){return 6;}
            public int get(int i){return switch(i){case 0->(int)(be.energyMilli()/1000%32768);case 1->(int)(be.energyMilli()/1000/32768);case 2->be.elapsed();case 3->be.total();case 4->be.mayInput(inv.player)?1:0;case 5->be.ownsJob(inv.player)?1:0;default->0;};}
            public void set(int i,int value){}
        }:new SimpleContainerData(6);
        addDataSlots(data);
        addSlot(new Slot(c,0,44,48){public boolean mayPlace(ItemStack s){return inputAllowed(inv.player)&&SmeltingRecipes.find(s)!=null;}public boolean mayPickup(Player p){return inputAllowed(p);}});
        addSlot(new Slot(c,1,44,96){public boolean mayPlace(ItemStack s){return SpiritStoneItem.isFullyCharged(s);}public boolean mayPickup(Player p){return true;}});
        addSlot(new Slot(c,2,180,48){public boolean mayPlace(ItemStack s){return false;}public boolean mayPickup(Player p){return !(c instanceof SmeltingBlockEntity be)||be.mayOutput(p);}public void onTake(Player p,ItemStack s){super.onTake(p,s);if(c instanceof SmeltingBlockEntity be)be.outputTaken();}});
        for(int y=0;y<3;y++)for(int x=0;x<9;x++)addSlot(new Slot(inv,9+y*9+x,40+x*18,150+y*18));
        for(int x=0;x<9;x++)addSlot(new Slot(inv,x,40+x*18,208));
    }
    private boolean inputAllowed(Player p){return materials instanceof SmeltingBlockEntity be?be.mayInput(p):data.get(4)==1;}
    public int energy(){return data.get(0)+data.get(1)*32768;}
    public int elapsed(){return data.get(2);}
    public int total(){return data.get(3);}
    public boolean busy(){return total()>0;}
    public boolean canStart(){return data.get(4)==1&&!materials.getItem(0).isEmpty();}
    public boolean canCancel(){return data.get(5)==1;}
    @Override public boolean stillValid(Player p){return p.level().getBlockEntity(pos)==materials&&p.level().getBlockState(pos).is(ModBlocks.SMELTING_FURNACE.get())&&p.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)<=64;}
    @Override public boolean clickMenuButton(Player p,int button){
        if(!stillValid(p)||p.containerMenu!=this||!(materials instanceof SmeltingBlockEntity be))return false;
        if(button==0){var error=be.start(p);if(!error.isEmpty()){p.displayClientMessage(Component.literal(error),true);return false;}}
        else if(button==1){if(!be.cancel(p))return false;}else return false;
        broadcastChanges();return true;
    }
    @Override public ItemStack quickMoveStack(Player p,int i){
        if(i<0||i>=slots.size())return ItemStack.EMPTY;Slot slot=slots.get(i);if(!slot.hasItem()||!slot.mayPickup(p))return ItemStack.EMPTY;
        ItemStack stack=slot.getItem(),original=stack.copy();
        if(i<3){if(!moveItemStackTo(stack,3,slots.size(),true))return ItemStack.EMPTY;}
        else if(SpiritStoneItem.isFullyCharged(stack)){if(!moveItemStackTo(stack,1,2,false))return ItemStack.EMPTY;}
        else if(!inputAllowed(p)||SmeltingRecipes.find(stack)==null||!moveItemStackTo(stack,0,1,false))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();slot.onTake(p,stack);return original;
    }
}
