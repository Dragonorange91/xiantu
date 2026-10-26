package com.longju.xiantu.gongfa;

/** Technique passives are attribute/event modifiers, not potion effects. */
public enum GongfaBuff {
    AGILITY("身法", "移动速度", true),
    SWIFTNESS("迅捷", "攻击速度", true),
    FORTITUDE("坚韧", "击退抗性", false),
    MENDING("调息", "受治疗量", true);

    private final String name;
    private final String attribute;
    private final boolean percent;

    GongfaBuff(String name, String attribute, boolean percent) {
        this.name = name;
        this.attribute = attribute;
        this.percent = percent;
    }

    public String nameText() { return name; }
    public String attributeText() { return attribute; }
    public String format(double amount) {
        return attribute + (percent ? "+" + Math.round(amount * 100) + "%" : "+" + String.format(java.util.Locale.ROOT, "%.2f", amount));
    }
}
