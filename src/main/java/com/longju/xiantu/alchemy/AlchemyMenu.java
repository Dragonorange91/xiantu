package com.longju.xiantu.alchemy;
import com.longju.xiantu.registry.ModMenus;
import com.longju.xiantu.refining.*;
import com.longju.xiantu.item.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public final class AlchemyMenu extends AbstractContainerMenu {
    public final Container contents,auxiliaries;
    public final ContainerData data=new SimpleContainerData(31);
    public final BlockPos pos;
    private int selected;
    public AlchemyMenu(int id,Inventory inv,FriendlyByteBuf b){this(id,inv,new SimpleContainer(12),new SimpleContainer(1),b.readBlockPos());}
    public AlchemyMenu(int id,Inventory inv,RefiningBlockEntity be){this(id,inv,be,be.alchemyAux,be.getBlockPos());}
    private AlchemyMenu(int id,Inventory inv,Container c,Container a,BlockPos pos){
        super(ModMenus.ALCHEMY.get(),id);contents=c;auxiliaries=a;this.pos=pos;addDataSlots(data);
        for(int i=0;i<12;i++){
            int index=i;int x=i>=4?WorkstationLayout.materialX(i-4):i==0?WorkstationLayout.STONE_X:i==3?WorkstationLayout.FIRE_X:WorkstationLayout.OUTPUT_X;int y=i>=4?WorkstationLayout.materialY(i-4):i==0?WorkstationLayout.FUEL_Y:i==3?WorkstationLayout.FIRE_Y:WorkstationLayout.OUTPUT_Y;
            addSlot(new Slot(c,index,x,y){
                @Override public boolean isActive(){return index!=1;}
                @Override public boolean mayPlace(ItemStack s){if(index==0)return s.getItem() instanceof SpiritStoneItem;if(data.get(1)==1)return false;return index==3?SpiritFire.isFire(s):index>=4&&AlchemyRecipes.isMaterial(s);}
                @Override public boolean mayPickup(Player p){return index!=1&&(index==0||index==2||data.get(1)==0)&&(index!=2||!(c instanceof RefiningBlockEntity be)||be.mayTakeOutput(p));}
                @Override public int getMaxStackSize(){return index==0||index==3?1:64;}
            });
        }
        for(int i=0;i<1;i++)addSlot(new Slot(a,i,WorkstationLayout.AUX_X,WorkstationLayout.AUX_Y){
            @Override public boolean mayPlace(ItemStack s){return data.get(1)==0&&s.getItem() instanceof DemonCoreItem;}
            @Override public boolean mayPickup(Player p){return data.get(1)==0;}
            @Override public int getMaxStackSize(){return 8;}
        });
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inv,9+row*9+col,WorkstationLayout.INVENTORY_X+col*18,WorkstationLayout.INVENTORY_Y+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inv,col,WorkstationLayout.INVENTORY_X+col*18,WorkstationLayout.HOTBAR_Y));
        if(c instanceof RefiningBlockEntity)broadcastChanges(inv.player);
    }
    public AlchemyRecipes.Recipe recipe(){int at=contents instanceof RefiningBlockEntity be?(be.alchemyJob==null?selected:AlchemyRecipes.ALL.indexOf(be.alchemyJob.recipe())):data.get(0);return at>=0&&at<AlchemyRecipes.ALL.size()?AlchemyRecipes.ALL.get(at):null;}
    private void broadcastChanges(Player owner){
        if(!(contents instanceof RefiningBlockEntity be))return;
        var r=recipe();var job=be.alchemyJob;int lv=AlchemyProgress.level(owner),mq=0,fire=0,auxBonus=0,auxQ=0,count=0;
        if(job!=null){lv=job.profession;mq=job.materialQuality;fire=job.fireBonus;auxBonus=job.auxSuccess;auxQ=job.auxQualityHalf;}
        else if(r!=null){var used=RefiningRecipes.allocate(r.allocationRecipe(),be.materials());if(used!=null)mq=RefiningRecipes.materialQuality(be.materials(),used);fire=SpiritFire.qualityBonus(be.getItem(3),r.rank());var aux=AlchemyRecipes.auxiliary(r,be.alchemyAux);if(aux!=null){auxBonus=aux.success();auxQ=aux.qualityHalf();count=aux.count();}else count=-1;}
        int[] values={r==null?-1:AlchemyRecipes.ALL.indexOf(r),be.busy()?1:0,AlchemyProgress.level(owner),RefiningRules.progress(AlchemyProgress.xp(owner)),RefiningRules.next(AlchemyProgress.level(owner)),be.furnace(),be.furnaceQuality,be.storedEnergy()%32768,be.storedEnergy()/32768,be.elapsed,be.total,r!=null&&RefiningProgress.knows(owner,r.knowledge())?1:0,job!=null?job.success():r==null?0:AlchemyRules.success(r.kind(),lv,be.furnace(),AlchemyRules.furnace(r.rank()),auxBonus),job==null?0:job.stage(),count,job!=null&&job.committed?1:0,job!=null?1:0,mq,fire,auxQ};
        for(int i=0;i<values.length;i++)data.set(i,values[i]);int[] weights=job!=null?job.qualityWeights():AlchemyRules.qualityWeights(lv,be.furnace(),be.furnaceQuality,mq,fire,auxQ);for(int i=0;i<5;i++)data.set(20+i,weights[i]);data.set(25,job==null?0:job.paused);int mask=0;if(job!=null)for(int i=0;i<6;i++)if(job.events[i]!=0)mask|=1<<i;data.set(26,mask);data.set(27,job==null?20:job.temperature);data.set(28,job==null?0:job.fireMode);data.set(29,r==null?0:job!=null&&job.stage()==0?r.warmFire():r.blendFire());data.set(30,job==null?0:job.heldTicks);
    }
    @Override public void broadcastChanges(){if(contents instanceof RefiningBlockEntity be&&be.getLevel() instanceof net.minecraft.server.level.ServerLevel level){var owner=level.players().stream().filter(p->p.containerMenu==this).findFirst();owner.ifPresent(this::broadcastChanges);}super.broadcastChanges();}
    @Override public boolean stillValid(Player p){return p.level().isClientSide||p.level().hasChunkAt(pos)&&p.level().getBlockEntity(pos)==contents&&p.distanceToSqr(pos.getCenter())<=64;}
    @Override public boolean clickMenuButton(Player p,int action){
        if(!(p instanceof ServerPlayer server)||p.containerMenu!=this||!stillValid(p)||!(contents instanceof RefiningBlockEntity be))return false;
        if(action==999&&!be.busy())selected=-1;
        else if(action>=1000&&action<1000+AlchemyRecipes.ALL.size()&&!be.busy())selected=action-1000;
        else if(action==1){String error=AlchemyJob.start(be,server,recipe());if(!error.isEmpty())p.displayClientMessage(Component.literal(error),true);}
        else if(action==2&&be.alchemyJob!=null&&be.alchemyJob.owner.equals(p.getUUID()))be.alchemyJob.cancel(be);
        else return false;
        broadcastChanges(p);super.broadcastChanges();return true;
    }
    @Override public ItemStack quickMoveStack(Player p,int index){
        if(index<0||index>=slots.size())return ItemStack.EMPTY;var slot=slots.get(index);if(!slot.isActive()||!slot.hasItem()||!slot.mayPickup(p))return ItemStack.EMPTY;var s=slot.getItem();var before=s.copy();
        if(index<13){if(!moveItemStackTo(s,13,slots.size(),true))return ItemStack.EMPTY;}
        else{int from=s.getItem() instanceof SpiritStoneItem?0:SpiritFire.isFire(s)?3:s.getItem() instanceof DemonCoreItem&&!AlchemyRecipes.isMaterial(s)?12:4;int to=from==4?12:from+1;if(!moveItemStackTo(s,from,to,false))return ItemStack.EMPTY;}
        if(s.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();slot.onTake(p,s);return before;
    }
}
