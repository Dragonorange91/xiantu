package com.longju.xiantu.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/** Soft 20% slow, never immobilizes or changes jump input. */
public final class BeastSnareEffect extends MobEffect {
    public BeastSnareEffect() {
        super(MobEffectCategory.HARMFUL,0xDCB77B);
        addAttributeModifier(Attributes.MOVEMENT_SPEED,"8ea33189-48cd-456e-b1d4-87d4d4febae3",-.2,AttributeModifier.Operation.MULTIPLY_TOTAL);
    }
}
