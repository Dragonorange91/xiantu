package com.longju.xiantu.refining;
import com.longju.xiantu.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public final class RefiningMenu extends AbstractContainerMenu {
    private final Player owner;
    public final Container contents;public final BlockPos pos;public final boolean forge,alchemy;
    public final ContainerData data;private int selected,selectedFacility=-1;
    public RefiningMenu(int id,Inventory inv,FriendlyByteBuf b){this(id,inv,new SimpleContainer(RefiningBlockEntity.SIZE),b.readBlockPos(),b.readBoolean(),b.readBoolean());}
    public RefiningMenu(int id,Inventory inv,Container c,BlockPos p,boolean forge){this(id,inv,c,p,forge,false);}
    public RefiningMenu(int id,Inventory inv,Container c,BlockPos p,boolean forge,boolean alchemy){
        super(ModMenus.REFINING.get(),id);contents=c;pos=p;this.forge=forge;this.alchemy=alchemy;owner=inv.player;
        data=new SimpleContainerData(24);addDataSlots(data);
        deviceSlot(0,WorkstationLayout.STONE_X,WorkstationLayout.FUEL_Y,!forge);deviceSlot(1,WorkstationLayout.BLANK_X,WorkstationLayout.BLANK_Y,forge);
        deviceSlot(2,WorkstationLayout.OUTPUT_X,WorkstationLayout.OUTPUT_Y,!alchemy);deviceSlot(3,WorkstationLayout.FIRE_X,WorkstationLayout.FIRE_Y,!forge);
        for(int i=4;i<RefiningBlockEntity.SIZE;i++)deviceSlot(i,WorkstationLayout.materialX(i-4),WorkstationLayout.materialY(i-4),!forge&&!alchemy);
        for(int y=0;y<3;y++)for(int x=0;x<9;x++)addSlot(new Slot(inv,9+y*9+x,WorkstationLayout.INVENTORY_X+x*18,WorkstationLayout.INVENTORY_Y+y*18));
        for(int x=0;x<9;x++)addSlot(new Slot(inv,x,WorkstationLayout.INVENTORY_X+x*18,WorkstationLayout.HOTBAR_Y));
    }
    private void deviceSlot(int index,int x,int y,boolean visible){
        addSlot(new Slot(contents,index,x,y){
            @Override public boolean isActive(){return visible;}
            @Override public boolean mayPlace(ItemStack s){return visible&&accepts(index,s);}
            @Override public boolean mayPickup(Player p){return visible&&(index==0||index==2||data.get(12)==0)&&(index!=2||!(contents instanceof RefiningBlockEntity be)||be.mayTakeOutput(p));}
            @Override public int getMaxStackSize(){return index==0||index==3?1:64;}
        });
    }
    private boolean accepts(int index,ItemStack s){
        if(index==0)return !forge&&s.getItem() instanceof com.longju.xiantu.item.SpiritStoneItem;
        if(data.get(12)!=0)return false;
        if(index==1)return forge&&s.is(RefiningItems.BLANK.get());
        if(index==3)return !forge&&SpiritFire.isFire(s);
        if(index>=4&&!forge&&!alchemy){
            if(!RefiningRecipes.isMaterial(s)&&!FacilityRecipes.isMaterial(s))return false;
            for(int i=4;i<RefiningBlockEntity.SIZE;i++)if(i!=index&&!contents.getItem(i).isEmpty()&&contents.getItem(i).is(s.getItem())&&com.longju.xiantu.material.MaterialQuality.get(contents.getItem(i))!=com.longju.xiantu.material.MaterialQuality.get(s))return false;
            return true;
        }return false;
    }
    @Override public void broadcastChanges(){
        if(contents instanceof RefiningBlockEntity be){
            var r=recipe();int q=0,ft=be.furnace(),fq=be.furnaceQuality,lv=RefiningProgress.level(owner),fireBonus=0;
            if(be.busy()){q=be.materialQuality;ft=be.sourceFurnace;fq=be.furnaceQuality;lv=be.profession;fireBonus=be.fireBonus;}
            else if(forge&&be.getLevel() instanceof net.minecraft.server.level.ServerLevel l){var blank=RefiningLedger.get(l).inspect(contents.getItem(1));if(blank!=null){q=blank.getInt("Material");ft=blank.getInt("Furnace");fq=blank.getInt("FurnaceQuality");fireBonus=blank.getInt("FireBonus");}}
            else if(r!=null){var allocation=RefiningRecipes.allocate(r,be.materials());if(allocation!=null)q=RefiningRecipes.materialQuality(be.materials(),allocation);fireBonus=SpiritFire.qualityBonus(contents.getItem(3),r.tier());}
            int[] values={selected,0,RefiningProgress.level(owner),RefiningRules.progress(RefiningProgress.xp(owner)),RefiningRules.next(RefiningProgress.level(owner)),ft,fq,be.storedEnergy()%32768,be.storedEnergy()/32768,be.elapsed,be.total,q,be.busy()?1:0,r==null?0:RefiningRules.successPercent(lv,ft,r.furnace()),r!=null&&RefiningProgress.knows(owner,r.tier()<=3?CraftingTreatises.REFINING_KEY:r.id())?1:0};
            for(int i=0;i<15;i++)data.set(i,values[i]);var weights=RefiningRules.qualityWeights(lv,ft,fq,q,fireBonus);for(int i=0;i<5;i++)data.set(15+i,weights[i]);
            data.set(20,be.busy()?RefiningRecipes.ALL.indexOf(be.job()):-1);data.set(21,fireBonus);
            data.set(22,selectedFacility);data.set(23,be.assembly==null?-1:FacilityRecipes.ALL.indexOf(be.assembly.recipe()));
            var facility=facilityRecipe();if(facility!=null){data.set(13,be.furnace()<facility.furnace()?0:100);data.set(14,RefiningProgress.knows(owner,facility.knowledge())?1:0);}
        }super.broadcastChanges();
    }
    public RefiningRecipes.Recipe recipe(){
        if(alchemy)return null;
        if(facilityRecipe()!=null)return null;
        if(contents instanceof RefiningBlockEntity be&&be.busy())return be.job();
        if(!(contents instanceof RefiningBlockEntity)&&data.get(12)==1){int job=data.get(20);return job>=0&&job<RefiningRecipes.ALL.size()?RefiningRecipes.ALL.get(job):null;}
        if(forge)return RefiningItems.recipe(contents.getItem(1));
        int i=contents instanceof RefiningBlockEntity?selected:data.get(0);return i>=0&&i<RefiningRecipes.ALL.size()?RefiningRecipes.ALL.get(i):null;
    }
    public FacilityRecipes.Recipe facilityRecipe(){
        if(forge||alchemy)return null;
        if(contents instanceof RefiningBlockEntity be&&be.assembly!=null)return be.assembly.recipe();
        int i=contents instanceof RefiningBlockEntity?selectedFacility:data.get(data.get(12)==1?23:22);
        return i>=0&&i<FacilityRecipes.ALL.size()?FacilityRecipes.ALL.get(i):null;
    }
    @Override public boolean clickMenuButton(Player player,int button){
        if(!(player instanceof ServerPlayer p)||!stillValid(p)||p.containerMenu!=this||!(contents instanceof RefiningBlockEntity be))return false;
        if(button==2&&!alchemy){String error=facilityRecipe()==null?be.start(p,recipe()):FacilityAssembly.start(be,p,facilityRecipe());if(!error.isEmpty())p.displayClientMessage(Component.literal(error),true);}
        else if(!be.busy()&&!forge&&!alchemy&&button>=1000&&button<1000+RefiningRecipes.ALL.size()){selected=button-1000;selectedFacility=-1;}
        else if(!be.busy()&&!forge&&!alchemy&&button>=2000&&button<2000+FacilityRecipes.ALL.size())selectedFacility=button-2000;
        else return false;
        broadcastChanges();return true;
    }
    @Override public boolean stillValid(Player p){return p.level().isClientSide||p.level().hasChunkAt(pos)&&p.level().getBlockEntity(pos)==contents&&p.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)<=64;}
    @Override public ItemStack quickMoveStack(Player p,int i){
        if(i<0||i>=slots.size())return ItemStack.EMPTY;var slot=slots.get(i);if(!slot.isActive()||!slot.hasItem()||!slot.mayPickup(p))return ItemStack.EMPTY;
        var stack=slot.getItem();var copy=stack.copy();
        if(i<RefiningBlockEntity.SIZE){if(!moveItemStackTo(stack,RefiningBlockEntity.SIZE,slots.size(),true))return ItemStack.EMPTY;}
        else{
            int target=forge?1:stack.getItem() instanceof com.longju.xiantu.item.SpiritStoneItem?0:SpiritFire.isFire(stack)?3:4;
            if(target==4&&alchemy||!accepts(target,stack)||!moveItemStackTo(stack,target,target==4?RefiningBlockEntity.SIZE:target+1,false))return ItemStack.EMPTY;
        }
        if(stack.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();slot.onTake(p,stack);return copy;
    }
}
