package com.longju.xiantu.combat;

import net.minecraftforge.common.ForgeConfigSpec;

/** Server-owned balance switches; changing these requires a server restart. */
public final class ElementalConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final ForgeConfigSpec.DoubleValue ADVANTAGE, REVERSE;
    static {
        var builder = new ForgeConfigSpec.Builder();
        builder.push("elementalCombat");
        ENABLED = builder.comment("启用属性攻击结算；雷属性不参与五行克制；修改后重启服务器")
                .worldRestart().define("enabled", true);
        ADVANTAGE = builder.comment("金克木、木克土、土克水、水克火、火克金的伤害倍率")
                .worldRestart().defineInRange("advantageMultiplier", 1.25, 1.0, 3.0);
        REVERSE = builder.comment("攻击被目标属性克制时的倍率；同属性、无关系和雷均为1")
                .worldRestart().defineInRange("reverseMultiplier", .8, .1, 1.0);
        builder.pop(); SPEC = builder.build();
    }
    private ElementalConfig() {}
}
