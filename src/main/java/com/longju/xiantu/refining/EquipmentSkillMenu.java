package com.longju.xiantu.refining;
import com.longju.xiantu.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkHooks;
public final class EquipmentSkillMenu extends AbstractContainerMenu {
    public EquipmentSkillMenu(int id,Inventory inventory,FriendlyByteBuf b){this(id,inventory);}
    public EquipmentSkillMenu(int id,Inventory inventory){super(ModMenus.EQUIPMENT_SKILLS.get(),id);}
    public static void open(ServerPlayer p){if(QitianSkills.pieces(p)!=4||p.containerMenu!=p.inventoryMenu||!p.inventoryMenu.getCarried().isEmpty()||!p.isAlive()||p.isSpectator())return;QitianSkills.cancel(p);QitianSkills.sync(p);NetworkHooks.openScreen(p,new SimpleMenuProvider((id,i,t)->new EquipmentSkillMenu(id,i),Component.literal("大圣神通")));}
    @Override public boolean stillValid(Player p){return p.isAlive()&&!p.isSpectator()&&QitianSkills.pieces(p)==4;}
    @Override public boolean clickMenuButton(Player player,int id){if(!(player instanceof ServerPlayer p)||p.containerMenu!=this||!stillValid(p))return false;if(id>=0&&id<QitianSkills.IDS.length){QitianSkills.state(p).putInt("Selected",id);QitianSkills.sync(p);return true;}if(id>=100&&id<100+QitianSkills.DISGUISES.size()){String error=QitianSkills.disguise(p,id-100);if(!error.isEmpty())p.sendSystemMessage(Component.literal(error));else p.closeContainer();return true;}return false;}
    @Override public ItemStack quickMoveStack(Player p,int index){return ItemStack.EMPTY;}
}
