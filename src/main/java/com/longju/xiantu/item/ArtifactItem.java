package com.longju.xiantu.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.longju.xiantu.config.XiantuConfig;
import com.longju.xiantu.cultivation.CultivationCapability;
import com.longju.xiantu.cultivation.CultivationEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.UUID;

/** Stack attributes are authoritative on the server; legacy forged tags remain readable. */
public class ArtifactItem extends Item {
    private static final UUID MAIN_ARMOR = UUID.fromString("c2acf6a3-cdac-4d32-a632-e13912eb3791");
    private static final UUID OFF_ARMOR = UUID.fromString("c2acf6a3-cdac-4d32-a632-e13912eb3792");
    private final MobEffect activeEffect;
    private final int effectDuration, effectAmplifier, baseDurability, cooldownTicks;
    private final double baseAttack, baseDefense, powerCost;
    public ArtifactItem(Properties properties) { this(properties, null, 0, 0, 100, 1, 0.5, 0, 0); }
    public ArtifactItem(Properties properties, MobEffect effect, int duration, int amplifier,
                        int durability, double attack, double defense, double cost, int cooldown) {
        super(properties.durability(durability));
        activeEffect = effect; effectDuration = duration; effectAmplifier = amplifier;
        baseDurability = durability; baseAttack = attack; baseDefense = defense;
        powerCost = cost; cooldownTicks = cooldown;
    }
    private double stat(ItemStack stack, String key, double fallback, double maximum) {
        double value = stack.hasTag() && stack.getTag().contains(key, net.minecraft.nbt.Tag.TAG_ANY_NUMERIC)
                ? stack.getTag().getDouble(key) : fallback;
        return Double.isFinite(value) ? Math.max(0, Math.min(maximum, value)) : fallback;
    }
    @Override public int getMaxDamage(ItemStack stack) {
        return Math.max(1, (int) stat(stack, "durability", baseDurability, 2000));
    }
    @Override public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        var attributes = ImmutableMultimap.<Attribute, AttributeModifier>builder();
        if (slot == EquipmentSlot.MAINHAND) {
            attributes.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID,
                    "xiantu_artifact_attack", stat(stack, "attack_bonus", baseAttack, 20), AttributeModifier.Operation.ADDITION));
            attributes.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID,
                    "xiantu_artifact_speed", -2.0, AttributeModifier.Operation.ADDITION));
        }
        if (slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND) {
            attributes.put(Attributes.ARMOR, new AttributeModifier(slot == EquipmentSlot.MAINHAND ? MAIN_ARMOR : OFF_ARMOR,
                    "xiantu_artifact_defense", stat(stack, "defense_bonus", baseDefense, 10), AttributeModifier.Operation.ADDITION));
        }
        return attributes.build();
    }
    @Override public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        stack.hurtAndBreak(2, attacker, entity -> entity.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        return true;
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (activeEffect == null) return InteractionResultHolder.pass(stack);
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        double cost = powerCost * XiantuConfig.ARTIFACT_POWER_COST_MULTIPLIER.get();
        boolean paid = player.getAbilities().instabuild || player.getCapability(CultivationCapability.DATA).map(data -> {
            if (data.power() < cost) return false;
            data.setPower(data.power() - cost);
            return true;
        }).orElse(false);
        if (!paid) {
            player.displayClientMessage(Component.translatable("message.xiantu.artifact.no_power", cost), true);
            return InteractionResultHolder.fail(stack);
        }
        int duration = (int) stat(stack, "effect_duration", effectDuration, 2400);
        player.addEffect(new MobEffectInstance(activeEffect, Math.max(1, duration), effectAmplifier));
        player.getCooldowns().addCooldown(this, Math.max(1, (int) (cooldownTicks * XiantuConfig.ARTIFACT_COOLDOWN_MULTIPLIER.get())));
        stack.hurtAndBreak(5, player, entity -> entity.broadcastBreakEvent(hand));
        if (player instanceof ServerPlayer serverPlayer) CultivationEvents.sync(serverPlayer);
        return InteractionResultHolder.consume(stack);
    }
    @Override public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.xiantu.artifact.level", (int) stat(stack, "level", 1, 10)).withStyle(ChatFormatting.GOLD));
        lines.add(Component.translatable("tooltip.xiantu.artifact.durability", Math.max(0, getMaxDamage(stack) - stack.getDamageValue()), getMaxDamage(stack)));
        if (activeEffect != null) {
            lines.add(Component.translatable("tooltip.xiantu.artifact.skill", Component.translatable(activeEffect.getDescriptionId()), effectDuration / 20));
            lines.add(Component.translatable("tooltip.xiantu.artifact.cost", powerCost * XiantuConfig.ARTIFACT_POWER_COST_MULTIPLIER.get(),
                    cooldownTicks / 20.0 * XiantuConfig.ARTIFACT_COOLDOWN_MULTIPLIER.get()).withStyle(ChatFormatting.AQUA));
        }
        lines.add(Component.translatable("tooltip.xiantu.artifact.wear").withStyle(ChatFormatting.GRAY));
    }
}
