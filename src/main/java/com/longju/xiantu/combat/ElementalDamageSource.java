package com.longju.xiantu.combat;

import java.util.Objects;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** Explicit per-hit type for future spells; retains original ownership, tags and death message. */
public final class ElementalDamageSource extends DamageSource {
    private final DamageSource original;
    private final Element element;
    private final double elementalFraction;
    public ElementalDamageSource(DamageSource original, Element element) {
        this(original, element, 1);
    }
    public ElementalDamageSource(DamageSource original, Element element, double elementalFraction) {
        super(original.typeHolder(), original.getDirectEntity(), original.getEntity());
        this.original = original; this.element = Objects.requireNonNull(element);
        if (!Double.isFinite(elementalFraction) || elementalFraction < 0 || elementalFraction > 1)
            throw new IllegalArgumentException("Elemental fraction must be finite and within [0, 1]");
        this.elementalFraction = element.elemental() ? elementalFraction : 0;
    }
    public Element element() { return element; }
    public double elementalFraction() { return elementalFraction; }
    @Override public Vec3 getSourcePosition() { return original.getSourcePosition(); }
    @Override public Component getLocalizedDeathMessage(LivingEntity victim) { return original.getLocalizedDeathMessage(victim); }
}
