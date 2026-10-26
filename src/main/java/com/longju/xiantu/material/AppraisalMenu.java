package com.longju.xiantu.material;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import com.longju.xiantu.registry.ModMenus;
public final class AppraisalMenu extends AbstractContainerMenu {
    public final Container materials;public final BlockPos pos;
    public AppraisalMenu(int id,Inventory inv,FriendlyByteBuf buf){this(id,inv,new SimpleContainer(6),buf.readBlockPos());}
    public AppraisalMenu(int id,Inventory inv,Container materials,BlockPos p){
        super(ModMenus.APPRAISAL.get(),id);this.materials=materials;pos=p;materials.startOpen(inv.player);
        addSlot(new Slot(materials,0,80,28){@Override public boolean mayPlace(ItemStack s){return MaterialQuality.eligible(s)&&MaterialQuality.get(s)==0;}});
        for(int i=1;i<=5;i++)addSlot(new Slot(materials,i,44+(i-1)*18,80){@Override public boolean mayPlace(ItemStack s){return false;}});
        for(int y=0;y<3;y++)for(int x=0;x<9;x++)addSlot(new Slot(inv,9+y*9+x,8+x*18,116+y*18));
        for(int x=0;x<9;x++)addSlot(new Slot(inv,x,8+x*18,174));
    }
    public boolean canIdentify(){if(!MaterialQuality.eligible(materials.getItem(0))||MaterialQuality.get(materials.getItem(0))!=0)return false;for(int i=1;i<=5;i++)if(!materials.getItem(i).isEmpty())return false;return true;}
    @Override public boolean clickMenuButton(Player p,int button){
        if(button!=0||!stillValid(p)||p.containerMenu!=this||!(materials instanceof AppraisalBlockEntity be)||!be.identify())return false;
        broadcastChanges();return true;
    }
    @Override public boolean stillValid(Player p){return p.level().getBlockState(pos).getBlock() instanceof AppraisalBlock&&p.level().getBlockEntity(pos)==materials&&p.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)<=64;}
    @Override public void removed(Player p){super.removed(p);materials.stopOpen(p);}
    @Override public ItemStack quickMoveStack(Player p,int i){
        if(i<0||i>=slots.size())return ItemStack.EMPTY;Slot slot=slots.get(i);if(!slot.hasItem())return ItemStack.EMPTY;
        ItemStack stack=slot.getItem(),original=stack.copy();
        if(i<6){if(!moveItemStackTo(stack,6,slots.size(),true))return ItemStack.EMPTY;}
        else if(!MaterialQuality.eligible(stack)||MaterialQuality.get(stack)!=0||!moveItemStackTo(stack,0,1,false))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();slot.onTake(p,stack);return original;
    }
}
