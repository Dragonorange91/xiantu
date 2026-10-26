package com.longju.xiantu.combat;

import java.util.Set;

/** Pure, independently testable math. Armor/enchantments still run once in vanilla. */
public final class ElementalRules {
    private ElementalRules() {}
    public static double matchup(Element attack, Element defense, double advantage, double reverse) {
        if (attack == null || defense == null || !attack.elemental() || !defense.elemental()) return 1;
        if (attack.overcomes(defense)) return finiteClamp(advantage, 1, 1, 3);
        if (defense.overcomes(attack)) return finiteClamp(reverse, 1, .1, 1);
        return 1;
    }
    public static double matchup(Element attack, Set<Element> defenses, double advantage, double reverse) {
        if (defenses == null || defenses.isEmpty()) return 1;
        double total = 0; int count = 0;
        for (Element defense : defenses) {
            if (defense != null && defense.elemental()) {
                total += matchup(attack, defense, advantage, reverse); count++;
            }
        }
        return count == 0 ? 1 : total / count;
    }
    public static double resistance(double base, double penetration) {
        return Math.max(-.25, Math.min(.80,
                finiteClamp(base, 0, -.25, .80) - finiteClamp(penetration, 0, 0, 1)));
    }
    public static float damage(float amount, Element attack, Set<Element> defenses,
                               double baseResistance, double penetration, double advantage, double reverse) {
        return damage(amount, attack, defenses, baseResistance, penetration, advantage, reverse, 1);
    }
    public static float damage(float amount, Element attack, Set<Element> defenses,
                               double baseResistance, double penetration, double advantage, double reverse,
                               double elementalFraction) {
        if (!Float.isFinite(amount) || amount <= 0) return 0;
        if (attack == null || !attack.elemental()) return amount;
        double fraction = finiteClamp(elementalFraction, 0, 0, 1);
        double value = amount * ((1 - fraction) + fraction * matchup(attack, defenses, advantage, reverse)
                * (1 - resistance(baseResistance, penetration)));
        return (float) Math.min(Float.MAX_VALUE, value);
    }
    private static double finiteClamp(double value, double fallback, double min, double max) {
        return Double.isFinite(value) ? Math.max(min, Math.min(max, value)) : fallback;
    }
}
