package com.longju.xiantu.refining;

import com.longju.xiantu.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkHooks;

/** Confirmation rechecks the original held stack; replayed/stale buttons never consume a second paper. */
public final class BlueprintLearningMenu extends AbstractContainerMenu {
    public final String name;
    public final java.util.List<String> details;
    private final Player owner;
    private final InteractionHand hand;
    private final ItemStack original;
    private boolean committed;
    public BlueprintLearningMenu(int id,Inventory inv,FriendlyByteBuf b){super(ModMenus.BLUEPRINT_LEARNING.get(),id);owner=inv.player;name=b.readUtf(160);int count=b.readVarInt();if(count<0||count>10)throw new IllegalArgumentException("Learning details");var rows=new java.util.ArrayList<String>();for(int i=0;i<count;i++)rows.add(b.readUtf(200));details=java.util.List.copyOf(rows);hand=InteractionHand.MAIN_HAND;original=ItemStack.EMPTY;}
    public BlueprintLearningMenu(int id,Inventory inv,InteractionHand hand){super(ModMenus.BLUEPRINT_LEARNING.get(),id);owner=inv.player;this.hand=hand;original=owner.getItemInHand(hand).copy();name=RefiningItems.diagramName(original);details=details(original);}
    private static java.util.List<String> details(ItemStack s){
        String id=com.longju.xiantu.talisman.TalismanBlueprints.key(s);
        if(id.isEmpty())return java.util.List.of(CraftingTreatises.isTreatiseName(RefiningItems.diagramName(s))?"研习后消耗一本，掌握一至三阶工艺。":"学习后消耗图纸，永久掌握对应工艺。","重复学习不消耗；境界不足也可先学习。");
        var tome=com.longju.xiantu.talisman.TalismanTomes.find(id);
        if(tome!=null)return java.util.List.of(tome.description(),"包含对应阶位的制纸、制笔与制符工艺。","制作仍检查境界、符纸、妖丹及触媒。","确认后消耗一本；重复学习不消耗。");
        return java.util.List.of("学习后永久掌握对应加工工艺。","确认后消耗一份工艺图；重复学习不消耗。");
    }
    public static void open(ServerPlayer p,InteractionHand hand){
        var s=p.getItemInHand(hand);if(RefiningItems.knowledge(s).isEmpty())return;
        NetworkHooks.openScreen(p,new SimpleMenuProvider((id,inv,title)->new BlueprintLearningMenu(id,inv,hand),Component.literal(CraftingTreatises.knowledge(s).isEmpty()&&com.longju.xiantu.talisman.TalismanTomes.find(com.longju.xiantu.talisman.TalismanBlueprints.key(s))==null?"确认学习图纸":"确认研习典籍")),b->{b.writeUtf(RefiningItems.diagramName(s),160);var rows=details(s);b.writeVarInt(rows.size());rows.forEach(row->b.writeUtf(row,200));});
    }
    @Override public boolean stillValid(Player p){return p==owner&&!committed&&ItemStack.matches(original,p.getItemInHand(hand));}
    @Override public boolean clickMenuButton(Player p,int action){
        if(!(p instanceof ServerPlayer)||p.containerMenu!=this||!stillValid(p)||action!=0)return false;
        var s=p.getItemInHand(hand);String key=RefiningItems.knowledge(s);if(key.isEmpty())return false;
        if(key.startsWith("talisman/")){String error=com.longju.xiantu.talisman.TalismanBlueprints.learn(p,s);p.displayClientMessage(Component.literal(error.isEmpty()?"已学会："+name:error),true);committed=error.isEmpty();if(committed)p.closeContainer();return committed;}
        committed=true;
        if(RefiningProgress.knows(p,key))p.displayClientMessage(Component.literal("已经掌握，不消耗物品"),true);
        else{RefiningProgress.learn(p,key);s.shrink(1);p.displayClientMessage(Component.literal("已学会："+name),true);}
        p.closeContainer();return true;
    }
    @Override public ItemStack quickMoveStack(Player p,int slot){return ItemStack.EMPTY;}
}
