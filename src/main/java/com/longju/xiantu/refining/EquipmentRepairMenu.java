package com.longju.xiantu.refining;

import com.longju.xiantu.gongfa.GongfaService;
import com.longju.xiantu.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.network.NetworkHooks;

/** Two transient inputs, returned on close. No extractable speculative result to duplicate. */
public final class EquipmentRepairMenu extends AbstractContainerMenu {
 public final BlockPos pos;private final Player owner;public final SimpleContainer inputs=new SimpleContainer(2);
 public EquipmentRepairMenu(int id,Inventory inv,FriendlyByteBuf b){this(id,inv,b.readBlockPos());}
 public EquipmentRepairMenu(int id,Inventory inv,BlockPos pos){super(ModMenus.EQUIPMENT_REPAIR.get(),id);this.pos=pos;owner=inv.player;addSlot(new Slot(inputs,0,86,58){@Override public int getMaxStackSize(){return 1;}});addSlot(new Slot(inputs,1,152,58));for(int y=0;y<3;y++)for(int x=0;x<9;x++)addSlot(new Slot(inv,9+y*9+x,89+x*18,170+y*18));for(int x=0;x<9;x++)addSlot(new Slot(inv,x,89+x*18,228));}
 public static void open(ServerPlayer p,BlockPos pos){NetworkHooks.openScreen(p,new SimpleMenuProvider((id,inv,who)->new EquipmentRepairMenu(id,inv,pos),Component.literal("装备修理台")),b->b.writeBlockPos(pos));}
 @Override public boolean stillValid(Player p){return p==owner&&p.isAlive()&&!p.isSpectator()&&p.level().hasChunkAt(pos)&&p.level().getBlockState(pos).is(EquipmentRepairBlock.BLOCK.get())&&p.distanceToSqr(pos.getCenter())<=64;}
 @Override public boolean clickMenuButton(Player player,int button){if(button!=0||!(player instanceof ServerPlayer p)||p.containerMenu!=this||!stillValid(p))return false;var result=EquipmentRepair.preview(inputs.getItem(0),inputs.getItem(1));String error=result.error();var d=GongfaService.data(p);if(error.isEmpty()&&d.power()*1000<result.cost())error="需要 "+result.cost()/1000.0+" 灵力";if(error.isEmpty()){d.setPower(d.power()-result.cost()/1000.0);inputs.setItem(0,result.stack());inputs.removeItem(1,1);com.longju.xiantu.cultivation.CultivationEvents.sync(p);broadcastChanges();}else p.sendSystemMessage(Component.literal(error));return true;}
 @Override public void removed(Player p){super.removed(p);if(!p.level().isClientSide)clearContainer(p,inputs);}
 @Override public ItemStack quickMoveStack(Player p,int i){if(i<0||i>=slots.size())return ItemStack.EMPTY;var slot=slots.get(i);if(!slot.hasItem())return ItemStack.EMPTY;var s=slot.getItem();var old=s.copy();if(i<2){if(!moveItemStackTo(s,2,38,true))return ItemStack.EMPTY;}else if(!moveItemStackTo(s,0,2,false))return ItemStack.EMPTY;if(s.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();slot.onTake(p,s);return old;}
}
