package com.longju.xiantu.equipment;
import com.longju.xiantu.registry.ModMenus;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
public final class AccessoryMenu extends AbstractContainerMenu {
    public static final TagKey<Item> RINGS=TagKey.create(Registries.ITEM,new ResourceLocation("xiantu","accessories/rings"));
    public static final TagKey<Item> NECKLACES=TagKey.create(Registries.ITEM,new ResourceLocation("xiantu","accessories/necklaces"));
    public final AccessoryInventory accessories;
    private final Player owner;
    public AccessoryMenu(int id,Inventory inv,FriendlyByteBuf ignored){this(id,inv);}
    public AccessoryMenu(int id,Inventory inv){
        super(ModMenus.ACCESSORIES.get(),id);owner=inv.player;accessories=new AccessoryInventory(owner);
        for(int i=0;i<3;i++){
            final int index=i;
            addSlot(new Slot(accessories,i,44+i*36,38){
                @Override public boolean mayPlace(ItemStack stack){return stack.is(index==2?NECKLACES:RINGS);}
                @Override public int getMaxStackSize(){return 1;}
            });
        }
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inv,col+row*9+9,8+col*18,84+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inv,col,8+col*18,142));
    }
    @Override public boolean stillValid(Player p){return p==owner && p.isAlive() && !p.isSpectator()
        && (!(p instanceof net.minecraft.server.level.ServerPlayer server) || !com.longju.xiantu.cultivation.InnerDemonService.active(server));}
    @Override public ItemStack quickMoveStack(Player p,int index){
        if(index<0 || index>=slots.size())return ItemStack.EMPTY;
        Slot slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;
        ItemStack stack=slot.getItem(),original=stack.copy();
        if(index<3){if(!moveItemStackTo(stack,3,39,true))return ItemStack.EMPTY;}
        else if(stack.is(RINGS)){if(!moveItemStackTo(stack,0,2,false))return ItemStack.EMPTY;}
        else if(stack.is(NECKLACES)){if(!moveItemStackTo(stack,2,3,false))return ItemStack.EMPTY;}
        else if(index<30){if(!moveItemStackTo(stack,30,39,false))return ItemStack.EMPTY;}
        else if(!moveItemStackTo(stack,3,30,false))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();
        slot.onTake(p,stack);return original;
    }
}
