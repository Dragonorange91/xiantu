package com.longju.xiantu.talisman;
import com.longju.xiantu.registry.ModMenus;
import com.longju.xiantu.item.SpiritStoneItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
public final class TalismanMenu extends AbstractContainerMenu {
 public final Container materials;public final BlockPos pos;private final ContainerData data;
 public TalismanMenu(int id,Inventory inv,FriendlyByteBuf b){this(id,inv,new SimpleContainer(10),b.readBlockPos());}
 public TalismanMenu(int id,Inventory inv,Container c,BlockPos p){super(ModMenus.TALISMAN.get(),id);materials=c;pos=p;
  data=c instanceof TalismanTableEntity be?new ContainerData(){public int getCount(){return 21;}public int get(int i){return switch(i){case 0->be.selection;case 1->be.elapsed;case 2->be.total();case 3->be.drawing.stroke;case 4->be.manual?1:0;case 5->be.sealed?1:0;case 6->(int)(be.energy/1000%32768);case 7->(int)(be.energy/1000/32768);case 8->be.mode;case 9->TalismanProgress.level(inv.player);case 10->be.owns(inv.player)?1:0;case 11->TalismanItems.brush(c.getItem(0));case 12,13,14,15->(int)((be.nonce>>((i-12)*15))&32767);case 16->{int mask=0;for(int k=0;k<TalismanTomes.ALL.size();k++)if(TalismanProgress.knows(inv.player,TalismanTomes.ALL.get(k).id()))mask|=1<<k;yield mask;}case 17->0;case 18->be.drawing.progress;case 19->be.drawing.mistakes;case 20->0;default->0;};}public void set(int i,int v){}}:new SimpleContainerData(21);addDataSlots(data);
  addSlot(new Slot(c,0,280,68){public boolean mayPlace(ItemStack s){return !busy()&&TalismanItems.brush(s)>0;}public boolean mayPickup(Player p){return !busy();}});
  for(int i=1;i<=7;i++)addSlot(new Slot(c,i,280+((i-1)%4)*22,112+((i-1)/4)*22){public boolean mayPlace(ItemStack s){return !busy();}public boolean mayPickup(Player p){return !busy();}});
  addSlot(new Slot(c,8,346,68){public boolean mayPlace(ItemStack s){return SpiritStoneItem.isFullyCharged(s);}});
  addSlot(new Slot(c,9,324,181){public boolean mayPlace(ItemStack s){return false;}public boolean mayPickup(Player p){return !(c instanceof TalismanTableEntity be)||be.mayOutput(p);}public void onTake(Player p,ItemStack s){super.onTake(p,s);if(c instanceof TalismanTableEntity be)be.outputTaken();}});
  for(int y=0;y<3;y++)for(int x=0;x<9;x++)addSlot(new Slot(inv,9+y*9+x,108+x*18,250+y*18));for(int x=0;x<9;x++)addSlot(new Slot(inv,x,108+x*18,308));
 }
 public boolean busy(){return data.get(2)>0;}public int selected(){return data.get(0);}public int elapsed(){return data.get(1);}public int total(){return data.get(2);}public int stroke(){return data.get(3);}public boolean manual(){return data.get(4)==1;}public boolean sealed(){return data.get(5)==1;}public int energy(){return data.get(6)+data.get(7)*32768;}public int mode(){return data.get(8);}public int profession(){return data.get(9);}public boolean owns(){return data.get(10)==1;}public int mistakes(){return data.get(19);}
 public long nonce(){long n=0;for(int i=0;i<4;i++)n|=(long)data.get(12+i)<<(15*i);return n;}
 public boolean learned(TalismanRecipes.Recipe recipe){String key=recipe.knowledge();if(key.isEmpty())return true;var tome=TalismanTomes.find(key);if(tome!=null)return (data.get(16)&(1<<TalismanTomes.ALL.indexOf(tome)))!=0;return false;}
 public TalismanRecipes.Recipe recipe(){return TalismanRecipes.ALL.get(Math.max(0,Math.min(TalismanRecipes.ALL.size()-1,selected())));}
 @Override public boolean stillValid(Player p){return p.level().getBlockEntity(pos)==materials&&p.distanceToSqr(pos.getCenter())<=64;}
 @Override public boolean clickMenuButton(Player p,int button){if(!(p instanceof net.minecraft.server.level.ServerPlayer sp)||!stillValid(p)||p.containerMenu!=this||!(materials instanceof TalismanTableEntity be))return false;
  if(button>=100&&button<100+TalismanRecipes.ALL.size()){if(be.busy())return false;be.selection=button-100;be.setChanged();}
  else if(button==0||button==1){String error=be.start(sp,button==1);if(!error.isEmpty()){p.displayClientMessage(Component.literal(error),true);return false;}}
  else if(button==2){if(!be.cancel(sp))return false;}else if(button==3){if(!be.seal(sp))return false;}else if(button==4){if(be.busy())return false;be.mode=(be.mode+1)%3;be.setChanged();}else if(button==5)be.bind(sp);else return false;broadcastChanges();return true;
 }
 @Override public ItemStack quickMoveStack(Player p,int i){if(i<0||i>=slots.size())return ItemStack.EMPTY;var slot=slots.get(i);if(!slot.hasItem()||!slot.mayPickup(p))return ItemStack.EMPTY;var s=slot.getItem();var original=s.copy();if(i<10){if(!moveItemStackTo(s,10,slots.size(),true))return ItemStack.EMPTY;}else if(SpiritStoneItem.isFullyCharged(s)){if(!moveItemStackTo(s,8,9,false))return ItemStack.EMPTY;}else if(!busy()){if(TalismanItems.brush(s)>0){if(!moveItemStackTo(s,0,1,false))return ItemStack.EMPTY;}else if(!moveItemStackTo(s,1,8,false))return ItemStack.EMPTY;}else return ItemStack.EMPTY;if(s.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();slot.onTake(p,s);return original;}
}
