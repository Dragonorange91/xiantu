package com.longju.xiantu.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.Level;

import java.util.List;

public final class SpiritStoneItem extends Item {
    private static final String CHARGE_TAG = "xiantu_spiritual_power_milli";
    private static final long MILLI_PER_POWER = 1_000L;
    private final SpiritStoneTier tier;

    public SpiritStoneItem(Properties properties, SpiritStoneTier tier) {
        super(properties.stacksTo(64));
        this.tier = tier;
    }

    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        com.longju.xiantu.client.GltfItemRenderer.install(consumer);
    }

    @Override public Component getName(ItemStack stack) {
        return com.longju.xiantu.material.MaterialQuality.name(super.getName(stack),tier.rank(),stack,false);
    }

    public SpiritStoneTier tier() {
        return tier;
    }

    public long spiritualPower() {
        return tier.spiritualPower();
    }

    public long maxChargeMilli() {
        return tier.spiritualPower() * MILLI_PER_POWER;
    }

    public long chargeMilli(ItemStack stack) {
        long maximum = maxChargeMilli();
        if (!stack.hasTag() || !stack.getTag().contains(CHARGE_TAG)) return maximum;
        return Math.max(0L, Math.min(maximum, stack.getTag().getLong(CHARGE_TAG)));
    }

    public void setChargeMilli(ItemStack stack, long charge) {
        long bounded = Math.max(0L, Math.min(maxChargeMilli(), charge));
        if (bounded == maxChargeMilli()) {
            if (stack.hasTag()) {
                stack.getTag().remove(CHARGE_TAG);
                if (stack.getTag().isEmpty()) stack.setTag(null);
            }
            return;
        }
        stack.getOrCreateTag().putLong(CHARGE_TAG, bounded);
    }

    public static long chargeMilli(ItemStack stack, SpiritStoneTier tier) {
        return stack.getItem() instanceof SpiritStoneItem stone && stone.tier == tier
                ? stone.chargeMilli(stack) : 0L;
    }

    public static boolean isFullyCharged(ItemStack stack) {
        return stack.getItem() instanceof SpiritStoneItem stone
                && stone.chargeMilli(stack) == stone.maxChargeMilli();
    }

    public static boolean isSpiritStone(ItemStack stack) {
        return stack.getItem() instanceof SpiritStoneItem;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND) return InteractionResultHolder.pass(stack);
        boolean canStart = level.isClientSide;
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            canStart = SpiritStoneAbsorption.begin(serverPlayer, tier, stack);
            if (canStart) {
                stack = isolateOneForUse(serverPlayer, hand, stack);
                if (stack.isEmpty()) {
                    SpiritStoneAbsorption.cancel(serverPlayer);
                    serverPlayer.displayClientMessage(Component.translatable(
                            "message.xiantu.stone.need_inventory_space"), true);
                    return InteractionResultHolder.fail(player.getItemInHand(hand));
                }
                SpiritStoneAbsorption.onStarted(serverPlayer, tier);
            }
        }
        if (!canStart) return InteractionResultHolder.fail(stack);
        player.startUsingItem(hand);
        // Consume the input without the SUCCESS arm swing; absorption stays server-authoritative.
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 40;
    }

    @Override
    public net.minecraft.world.item.UseAnim getUseAnimation(ItemStack stack) {
        return net.minecraft.world.item.UseAnim.NONE;
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remainingUseDuration) {
        if (!level.isClientSide && user instanceof ServerPlayer player) {
            int elapsedTicks = Math.max(1, Math.min(40, getUseDuration(stack) - remainingUseDuration));
            if (!SpiritStoneAbsorption.tick(player, tier, stack, elapsedTicks)) player.stopUsingItem();
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int timeLeft) {
        if (user instanceof ServerPlayer player) SpiritStoneAbsorption.cancel(player, stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (!level.isClientSide && user instanceof ServerPlayer player) {
            return SpiritStoneAbsorption.complete(player, tier, stack);
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.level.Level level, List<Component> tooltip,
                                net.minecraft.world.item.TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.xiantu.spirit_stone.charge",
                formatPower(chargeMilli(stack)), formatPower(maxChargeMilli())).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.xiantu.spirit_stone.absorb"));
    }

    private ItemStack isolateOneForUse(ServerPlayer player, InteractionHand hand, ItemStack stack) {
        if (stack.getCount() <= 1) return stack;
        Inventory inventory = player.getInventory();
        int heldSlot = inventory.selected;
        ItemStack remainder = stack.copy();
        remainder.shrink(1);

        int capacity = 0;
        for (int slot = 0; slot < inventory.items.size(); slot++) {
            if (slot == heldSlot) continue;
            ItemStack existing = inventory.items.get(slot);
            int limit = Math.min(inventory.getMaxStackSize(), remainder.getMaxStackSize());
            if (existing.isEmpty()) capacity += limit;
            else if (ItemStack.isSameItemSameTags(existing, remainder)) {
                capacity += Math.max(0, limit - existing.getCount());
            }
        }
        if (capacity < remainder.getCount()) return ItemStack.EMPTY;

        int left = remainder.getCount();
        for (int slot = 0; slot < inventory.items.size() && left > 0; slot++) {
            if (slot == heldSlot) continue;
            ItemStack existing = inventory.items.get(slot);
            int limit = Math.min(inventory.getMaxStackSize(), remainder.getMaxStackSize());
            if (existing.isEmpty()) {
                int moved = Math.min(left, limit);
                inventory.setItem(slot, remainder.copyWithCount(moved));
                left -= moved;
            } else if (ItemStack.isSameItemSameTags(existing, remainder)) {
                int moved = Math.min(left, Math.max(0, limit - existing.getCount()));
                if (moved > 0) {
                    existing.grow(moved);
                    left -= moved;
                }
            }
        }
        if (left != 0) return ItemStack.EMPTY;

        ItemStack activeStone = stack.copyWithCount(1);
        player.setItemInHand(hand, activeStone);
        inventory.setChanged();
        return activeStone;
    }

    private static String formatPower(long milliPower) {
        long whole = milliPower / MILLI_PER_POWER;
        long fraction = milliPower % MILLI_PER_POWER;
        if (fraction == 0) return Long.toString(whole);
        String digits = String.format(java.util.Locale.ROOT, "%03d", fraction).replaceFirst("0+$", "");
        return whole + "." + digits;
    }

    public static SpiritStoneTier tierOf(ItemStack stack) {
        return stack.getItem() instanceof SpiritStoneItem stone ? stone.tier : null;
    }
}
