package com.longju.xiantu.gongfa;

import com.longju.xiantu.cultivation.CultivationEvents;
import com.longju.xiantu.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkHooks;

/** Confirms only the exact held book. There is no learned-method selection page. */
public final class GongfaMenu extends AbstractContainerMenu {
    private final ServerPlayer owner;
    private final InteractionHand hand;
    private final ItemStack snapshot;
    private final String methodId;
    public GongfaMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id,inventory,buffer.readEnum(InteractionHand.class),buffer.readUtf());
    }
    public GongfaMenu(int id, Inventory inventory, InteractionHand hand, String methodId) {
        super(ModMenus.GONGFA_MENU.get(),id);
        owner = inventory.player instanceof ServerPlayer player ? player : null;
        this.hand = hand; this.methodId = methodId;
        snapshot = inventory.player.getItemInHand(hand).copy();
    }
    public String methodId() { return methodId; }
    public static void open(ServerPlayer player, InteractionHand hand) {
        if (!(player.getItemInHand(hand).getItem() instanceof GongfaItem book)) return;
        String id = book.definitionId();
        if (!GongfaService.denial(player,id).isEmpty()) return;
        CultivationEvents.sync(player);
        NetworkHooks.openScreen(player,new SimpleMenuProvider((menuId,inventory,title) -> new GongfaMenu(menuId,inventory,hand,id),
                Component.literal("修习功法确认")),buffer -> { buffer.writeEnum(hand); buffer.writeUtf(id); });
    }
    @Override public boolean clickMenuButton(Player player, int button) {
        if (owner == null || player != owner || player.containerMenu != this || !stillValid(player)) return false;
        if (button == 0) {
            boolean confirmed = GongfaService.confirm(owner,methodId,hand,snapshot);
            owner.closeContainer();
            if (confirmed) owner.displayClientMessage(Component.literal("已确认，请盘坐蒲团，长按右键研习10秒；功法书不会消耗"),true);
            return confirmed;
        }
        if (button == 1) { GongfaService.cancel(owner,""); owner.closeContainer(); return true; }
        return false;
    }
    @Override public boolean stillValid(Player player) {
        return player.isAlive() && (owner == null || player == owner && GongfaService.heldBook(owner,hand,snapshot,methodId)
                && GongfaService.denial(owner,methodId).isEmpty());
    }
    @Override public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
}
