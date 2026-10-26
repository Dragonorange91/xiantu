package com.longju.xiantu.smelting;

import com.longju.xiantu.item.SpiritStoneItem;
import com.longju.xiantu.refining.RefiningProgress;
import com.longju.xiantu.registry.ModBlockEntities;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import java.util.UUID;
import javax.annotation.Nullable;

public final class SmeltingBlockEntity extends RandomizableContainerBlockEntity implements WorldlyContainer {
    public static final long CAPACITY=1_000_000_000L;
    private NonNullList<ItemStack> items=NonNullList.withSize(3,ItemStack.EMPTY);
    private long energy;
    private ItemStack reserved=ItemStack.EMPTY;
    private SmeltingRecipes.Recipe recipe;
    private UUID owner,outputOwner,transaction;
    private int elapsed;
    public SmeltingBlockEntity(BlockPos p,BlockState s){super(ModBlockEntities.SMELTING.get(),p,s);}
    @Override public int getContainerSize(){return 3;}
    @Override protected NonNullList<ItemStack> getItems(){return items;}
    @Override protected void setItems(NonNullList<ItemStack> n){items=n;}
    @Override protected Component getDefaultName(){return Component.literal("熔炼炉");}
    @Override protected AbstractContainerMenu createMenu(int id,Inventory inv){return new SmeltingMenu(id,inv,this,worldPosition);}
    public boolean busy(){return recipe!=null;}
    public long energyMilli(){return energy;}
    public int elapsed(){return elapsed;}
    public int total(){return busy()?recipe.ticks():0;}
    public boolean mayInput(Player p){return !busy()&&RefiningProgress.knows(p,SmeltingRecipes.KNOWLEDGE);}
    public boolean mayOutput(Player p){return !busy()&&(outputOwner==null||outputOwner.equals(p.getUUID()));}
    public boolean ownsJob(Player p){return busy()&&p.getUUID().equals(owner);}
    @Override public boolean canPlaceItem(int slot,ItemStack s){return slot==0?!busy()&&SmeltingRecipes.find(s)!=null:slot==1&&SpiritStoneItem.isFullyCharged(s);}
    // Block both Forge item handlers and vanilla sided hoppers. Player access is menu-only.
    @Override public int[] getSlotsForFace(Direction d){return new int[0];}
    @Override public boolean canPlaceItemThroughFace(int slot,ItemStack s,@Nullable Direction d){return false;}
    @Override public boolean canTakeItemThroughFace(int slot,ItemStack s,Direction d){return false;}
    @Override public boolean canTakeItem(Container target,int slot,ItemStack s){return false;}
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap,@Nullable Direction side){return cap==ForgeCapabilities.ITEM_HANDLER?LazyOptional.empty():super.getCapability(cap,side);}
    public boolean charge(){
        ItemStack stone=items.get(1);
        if(!SpiritStoneItem.isFullyCharged(stone))return false;
        long amount=((SpiritStoneItem)stone.getItem()).chargeMilli(stone),held=busy()?recipe.power()*1000L:0;
        // Reserve room for cancellation's refund, even while charging an active furnace.
        if(amount>CAPACITY-energy-held)return false;
        energy+=amount;stone.shrink(1);setChanged();return true;
    }
    public String start(Player p){
        if(level==null||level.isClientSide||isRemoved())return "熔炼炉不可用";
        if(busy())return "已有熔炼任务";
        if(!RefiningProgress.knows(p,SmeltingRecipes.KNOWLEDGE))return "请先学习熔炼炉设施图";
        var r=SmeltingRecipes.find(items.get(0));if(r==null)return "放入三至八阶金属原矿";
        if(!mayOutput(p)||!canFit(r.output()))return "请先取走炉内成品";
        if(energy<r.power()*1000L)return "炉内灵力不足";
        reserved=items.get(0).split(1);energy-=r.power()*1000L;recipe=r;owner=p.getUUID();transaction=UUID.randomUUID();elapsed=0;setChanged();return "";
    }
    private boolean canFit(ItemStack out){var current=items.get(2);return current.isEmpty()||ItemStack.isSameItemSameTags(current,out)&&current.getCount()+out.getCount()<=current.getMaxStackSize();}
    public boolean cancel(Player p){
        if(level==null||level.isClientSide||!ownsJob(p))return false;
        var input=items.get(0);
        if(!input.isEmpty()&&(!ItemStack.isSameItemSameTags(input,reserved)||input.getCount()>=input.getMaxStackSize()))return false;
        if(input.isEmpty())items.set(0,reserved.copy());else input.grow(1);
        energy+=recipe.power()*1000L;clearJob();setChanged();return true;
    }
    private void clearJob(){reserved=ItemStack.EMPTY;recipe=null;owner=null;transaction=null;elapsed=0;}
    public void discardJob(){clearJob();energy=0;setChanged();}
    public static void tick(Level level,BlockPos pos,BlockState state,SmeltingBlockEntity be){
        be.charge();
        if(!be.busy())return;
        var player=((ServerLevel)level).getServer().getPlayerList().getPlayer(be.owner);
        if(player==null)return; // No offline catch-up; loaded progress is counted only by live ticks.
        if(!be.canFit(be.recipe.output()))return;
        if(++be.elapsed>=be.recipe.ticks()){
            ItemStack out=be.recipe.output();if(be.items.get(2).isEmpty())be.items.set(2,out);else be.items.get(2).grow(1);
            be.outputOwner=be.owner;be.clearJob();
        }
        be.setChanged();
    }
    @Override public void load(CompoundTag n){
        super.load(n);items=NonNullList.withSize(3,ItemStack.EMPTY);ContainerHelper.loadAllItems(n,items);
        clearJob();energy=Math.max(0,Math.min(CAPACITY,n.getLong("EnergyMilli")));outputOwner=n.hasUUID("OutputOwner")?n.getUUID("OutputOwner"):null;
        if(n.hasUUID("Transaction")&&n.hasUUID("Owner")){
            var r=SmeltingRecipes.find(n.getString("Recipe"));var raw=ItemStack.of(n.getCompound("Reserved"));
            if(r!=null&&raw.getCount()==1&&r.matches(raw)){
                recipe=r;reserved=raw;owner=n.getUUID("Owner");transaction=n.getUUID("Transaction");elapsed=Math.max(0,Math.min(r.ticks()-1,n.getInt("Elapsed")));
                energy=Math.min(energy,CAPACITY-r.power()*1000L);
            }
        }
    }
    @Override protected void saveAdditional(CompoundTag n){
        super.saveAdditional(n);ContainerHelper.saveAllItems(n,items);n.putLong("EnergyMilli",energy);
        if(!items.get(2).isEmpty()&&outputOwner!=null)n.putUUID("OutputOwner",outputOwner);
        if(busy()){n.putString("Recipe",recipe.metal());n.put("Reserved",reserved.save(new CompoundTag()));n.putUUID("Owner",owner);n.putUUID("Transaction",transaction);n.putInt("Elapsed",elapsed);}
    }
    public void outputTaken(){if(items.get(2).isEmpty()){outputOwner=null;setChanged();}}
}
