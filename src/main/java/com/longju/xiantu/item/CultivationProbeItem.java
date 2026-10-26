package com.longju.xiantu.item;

import com.longju.xiantu.cultivation.CultivationStatusMenu;
import com.longju.xiantu.cultivation.CultivationEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

public final class CultivationProbeItem extends Item {
    public CultivationProbeItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND) return InteractionResultHolder.pass(stack);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            CultivationEvents.sync(serverPlayer);
            NetworkHooks.openScreen(serverPlayer,
                    new net.minecraft.world.SimpleMenuProvider((id, inventory, ignored) -> new CultivationStatusMenu(id, inventory),
                            Component.translatable("gui.xiantu.cultivation.title")));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
