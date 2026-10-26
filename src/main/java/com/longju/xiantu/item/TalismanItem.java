package com.longju.xiantu.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

public class TalismanItem extends Item {
    private final MobEffect effect;
    private final int duration;
    private final int amplifier;
    public TalismanItem(Properties properties) { this(properties, null, 0, 0); }
    public TalismanItem(Properties properties, MobEffect effect, int duration, int amplifier) {
        super(properties);
        this.effect = effect;
        this.duration = duration;
        this.amplifier = amplifier;
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            var tag = stack.getOrCreateTag();
            int effectDuration = tag.contains("effect_duration") ? tag.getInt("effect_duration") : duration;
            if (effect != null) player.addEffect(new MobEffectInstance(effect, Math.max(effectDuration, 1), amplifier));
            player.getPersistentData().putInt("xiantu_talisman_effect_ticks", Math.max(effectDuration, 1));
            if (!player.getAbilities().instabuild) stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
