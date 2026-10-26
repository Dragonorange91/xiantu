package com.longju.xiantu.spell;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import com.longju.xiantu.registry.ModMenus;
import net.minecraftforge.network.NetworkHooks;
public final class SpellMenu extends AbstractContainerMenu{
 public SpellMenu(int id,Inventory inv,FriendlyByteBuf b){this(id,inv);}
 public SpellMenu(int id,Inventory inv){super(ModMenus.SPELLS.get(),id);}
 public static void open(ServerPlayer p){if(p.containerMenu!=p.inventoryMenu||!p.inventoryMenu.getCarried().isEmpty()||!p.isAlive()||p.isSpectator()||com.longju.xiantu.cultivation.InnerDemonService.active(p))return;SpellService.cancel(p);SpellService.sync(p);NetworkHooks.openScreen(p,new SimpleMenuProvider((id,inv,owner)->new SpellMenu(id,inv),Component.literal("法术配置")));}
 @Override public boolean stillValid(Player p){return p.isAlive()&&!p.isSpectator()&&(!(p instanceof ServerPlayer s)||!com.longju.xiantu.cultivation.InnerDemonService.active(s));}
 @Override public ItemStack quickMoveStack(Player p,int index){return ItemStack.EMPTY;}
 @Override public boolean clickMenuButton(Player p,int id){if(id!=90||!stillValid(p)||!(p instanceof ServerPlayer s))return false;SpellService.data(s).toggleHud();SpellService.sync(s);return true;}
}
