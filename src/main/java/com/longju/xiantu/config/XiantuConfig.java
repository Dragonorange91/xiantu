package com.longju.xiantu.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** Server-side knobs for balancing the cultivation loop without rebuilding the mod. */
public final class XiantuConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.DoubleValue SPELL_ELITE_CHANCE,SPELL_BOSS_CHANCE;
    public static final ForgeConfigSpec.DoubleValue AURA_CAPACITY, AURA_RECOVERY, AURA_DEMAND, NATURAL_MEDITATION_MULTIPLIER;
    public static final java.util.Map<String, ForgeConfigSpec.DoubleValue> SPECIAL_ORE_CHANCES;
    public static final ForgeConfigSpec.DoubleValue BEAST_AUXILIARY_CHANCE;
    public static final ForgeConfigSpec.DoubleValue SPIRIT_ROOT_ROLL_CHANCE;
    public static final ForgeConfigSpec.DoubleValue POWER_OVERFLOW_RATIO;
    public static final ForgeConfigSpec.DoubleValue POWER_OVERFLOW_DAMAGE_RATIO;
    public static final ForgeConfigSpec.DoubleValue THUNDER_MITIGATION_RATIO;
    public static final ForgeConfigSpec.IntValue THUNDER_WASTES_STRIKE_MIN_SECONDS;
    public static final ForgeConfigSpec.IntValue THUNDER_WASTES_STRIKE_MAX_SECONDS;
    public static final ForgeConfigSpec.IntValue POWER_CHECK_INTERVAL_TICKS;
    public static final java.util.List<ForgeConfigSpec.IntValue> FORMATION_POWER_PER_MINUTE = new java.util.ArrayList<>();
    public static final ForgeConfigSpec.DoubleValue ARTIFACT_POWER_COST_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue ARTIFACT_COOLDOWN_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue BEAST_ABILITY_COOLDOWN;
    public static final ForgeConfigSpec.DoubleValue ALCHEMY_GRADE_BONUS;
    public static final ForgeConfigSpec.DoubleValue ALCHEMY_DURATION_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue ALCHEMY_FUEL_TICKS;
    public static final ForgeConfigSpec.IntValue ALCHEMY_MASTERY_BATCHES;
    public static final ForgeConfigSpec.IntValue ALCHEMY_UPGRADE_SECOND_COST;
    public static final ForgeConfigSpec.IntValue ALCHEMY_UPGRADE_THIRD_COST;
    public static final ForgeConfigSpec.DoubleValue MEDICINE_TOXICITY_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue SPIRIT_ORE_TIAN_JING_CHANCE;
    public static final ForgeConfigSpec.DoubleValue SPIRIT_ORE_XIAN_YU_CHANCE;
    public static final ForgeConfigSpec.DoubleValue SPIRIT_ORE_DAO_SHI_CHANCE;
    public static final ForgeConfigSpec.DoubleValue SPIRIT_ORE_FAN_LING_SHI_CHANCE;
    public static final ForgeConfigSpec.DoubleValue SPIRIT_ORE_LING_JING_CHANCE;
    public static final ForgeConfigSpec.DoubleValue SPIRIT_ORE_LING_YU_CHANCE;
    public static final ForgeConfigSpec.DoubleValue SPIRIT_ORE_XUAN_JING_CHANCE;
    public static final ForgeConfigSpec.DoubleValue SPIRIT_ORE_DI_SUI_CHANCE;
    public static final java.util.Map<String, ForgeConfigSpec.DoubleValue> METAL_ORE_CHANCES;
    public static final ForgeConfigSpec.DoubleValue SPIRIT_STONE_BACKLASH_START_DAMAGE;
    public static final ForgeConfigSpec.DoubleValue SPIRIT_STONE_BACKLASH_DAMAGE_MULTIPLIER;

    public static final java.util.List<ForgeConfigSpec.DoubleValue> MATERIAL_QUALITY_WEIGHTS=new java.util.ArrayList<>();
    public static final java.util.List<ForgeConfigSpec.DoubleValue> PHYSIQUE_WEIGHTS=new java.util.ArrayList<>();

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("spells");
        SPELL_ELITE_CHANCE=builder.comment("有效同属性精英妖兽法术图纸率；每日每人至多1张").defineInRange("eliteScrollChance",.008,0D,1D);
        SPELL_BOSS_CHANCE=builder.comment("已验证遗迹首领事件法术图纸率；沿用事件唯一结算").defineInRange("bossScrollChance",.05,0D,1D);
        builder.pop();
        builder.push("cultivation");
        AURA_CAPACITY = builder.comment("普通16x16区块灵气容量；整列高度共享，调整容量时保留已有灵气余量比例").defineInRange("chunkAuraCapacity",3600D,1D,1000000D);
        AURA_RECOVERY = builder.comment("已加载且无人自然吐纳的普通区块每秒恢复；离线及卸载暂停").defineInRange("chunkAuraRecovery",1D,0D,10000D);
        AURA_DEMAND = builder.comment("每名自然吐纳玩家每秒灵气需求；同区块按比例分配").defineInRange("naturalAuraDemand",4D,.01D,10000D);
        NATURAL_MEDITATION_MULTIPLIER = builder.comment("普通蒲团满灵气修炼倍率；一级聚灵阵为1.5").defineInRange("naturalMeditationMultiplier",1D,.01D,10D);
        SPIRIT_ROOT_ROLL_CHANCE = builder.comment("独立五行灵根抽取概率").defineInRange("spiritRootRollChance", 0.20D, 0.0D, 1.0D);
        POWER_CHECK_INTERVAL_TICKS = builder.comment("灵力与修为更新间隔（游戏刻）").defineInRange("powerCheckIntervalTicks", 20, 1, 200);
        POWER_OVERFLOW_RATIO = builder.comment("灵力超过上限多少倍时触发反噬").defineInRange("powerOverflowRatio", 1.30D, 1.0D, 10.0D);
        POWER_OVERFLOW_DAMAGE_RATIO = builder.comment("灵力反噬占最大生命的比例").defineInRange("powerOverflowDamageRatio", 0.70D, 0.0D, 10.0D);
        THUNDER_MITIGATION_RATIO = builder.comment("雷劫减免判定阈值").defineInRange("thunderMitigationRatio", 0.70D, 0.0D, 1.0D);
        for (int tier = 1; tier <= 3; tier++) {
            FORMATION_POWER_PER_MINUTE.add(builder.comment("聚灵阵每分钟运行灵力消耗；无人打坐也消耗").defineInRange("formationPowerPerMinute" + tier, new int[]{50,200,600}[tier-1], 1, 1000000));
        }
        builder.pop();
        builder.push("artifacts");
        ARTIFACT_POWER_COST_MULTIPLIER = builder.comment("法宝主动技能灵力费用倍率").defineInRange("powerCostMultiplier", 1.0D, 0.0D, 100.0D);
        ARTIFACT_COOLDOWN_MULTIPLIER = builder.comment("法宝主动技能冷却倍率").defineInRange("cooldownMultiplier", 1.0D, 0.05D, 100.0D);
        builder.pop();
        builder.push("specialOres");
        var specialOreChances = new java.util.LinkedHashMap<String,ForgeConfigSpec.DoubleValue>();
        for (var m:com.longju.xiantu.mining.SpecialMineralRegistry.ALL)
            specialOreChances.put(m.id(),builder.comment(m.name()+"每合适区块仅一次概率尝试；矿脉位置可能因母岩不符而失败")
                    .defineInRange(m.id()+"Chance",m.chance(),0D,1D));
        SPECIAL_ORE_CHANCES=java.util.Collections.unmodifiableMap(specialOreChances);
        builder.pop();
        builder.push("beasts");
        BEAST_AUXILIARY_CHANCE=builder.comment("合格自然妖兽额外辅材总概率；成功后等概率只选一种")
                .defineInRange("auxiliaryMaterialChance",0.5D,0D,1D);
        BEAST_ABILITY_COOLDOWN = builder.comment("妖兽两次开始蓄力的最短间隔（游戏刻，包含25刻蓄力）").defineInRange("abilityCooldownTicks", 160, 40, 1200);
        builder.pop();
        builder.push("alchemy");
        ALCHEMY_GRADE_BONUS = builder.comment("妖核每高一品增加的即时丹药收益比例；不增加永久属性").defineInRange("gradeBonus", 0.35D, 0.0D, 5.0D);
        ALCHEMY_DURATION_MULTIPLIER = builder.comment("配方炼制时间倍率").defineInRange("durationMultiplier", 1.0D, 0.25D, 10.0D);
        ALCHEMY_FUEL_TICKS = builder.comment("烈焰粉提供的炼制刻数；煤/木炭为两倍，烈焰棒为四倍").defineInRange("fuelTicks", 160, 20, 6000);
        ALCHEMY_MASTERY_BATCHES = builder.comment("每级熟练度需要的成丹批次，最多五级；熟练度缩短时间，不改变成丹NBT").defineInRange("masteryBatchesPerLevel", 10, 1, 1000);
        ALCHEMY_UPGRADE_SECOND_COST = builder.comment("一品丹炉升至二品需要的灵石数").defineInRange("secondGradeUpgradeCost", 8, 1, 30000);
        ALCHEMY_UPGRADE_THIRD_COST = builder.comment("二品丹炉升至三品需要的灵石数").defineInRange("thirdGradeUpgradeCost", 16, 1, 30000);
        builder.pop();
        builder.push("medicine");
        MEDICINE_TOXICITY_MULTIPLIER = builder.comment("服药丹毒倍率，0关闭新增丹毒；丹毒范围0至100").defineInRange("toxicityMultiplier", 1.0D, 0.0D, 10.0D);
        builder.pop();
        builder.push("metalOres");
        var metalOreChances = new java.util.LinkedHashMap<String, ForgeConfigSpec.DoubleValue>();
        for (var material : com.longju.xiantu.mining.MetalMaterialRegistry.ALL) {
            double defaultChance = material.chancePercent() / 100.0D;
            metalOreChances.put(material.id(), builder.comment(material.name() + "每区块矿脉尝试概率；0关闭，1为100%")
                    .defineInRange(material.id() + "Chance", defaultChance, 0.0D, 1.0D));
        }
        METAL_ORE_CHANCES = java.util.Collections.unmodifiableMap(metalOreChances);
        builder.pop();
        builder.push("spiritOres");
        SPIRIT_ORE_FAN_LING_SHI_CHANCE = builder.comment("凡灵石矿尝试概率倍率；1为设计概率，0为关闭，范围0至1").defineInRange("fanLingShiChance", 1.0D, 0.0D, 1.0D);
        SPIRIT_ORE_LING_JING_CHANCE = builder.comment("灵晶矿尝试概率倍率；1为设计概率，0为关闭，范围0至1").defineInRange("lingJingChance", 1.0D, 0.0D, 1.0D);
        SPIRIT_ORE_LING_YU_CHANCE = builder.comment("灵玉矿尝试概率倍率；1为设计概率，0为关闭，范围0至1").defineInRange("lingYuChance", 1.0D, 0.0D, 1.0D);
        SPIRIT_ORE_XUAN_JING_CHANCE = builder.comment("玄晶矿尝试概率倍率；1为设计概率，0为关闭，范围0至1").defineInRange("xuanJingChance", 1.0D, 0.0D, 1.0D);
        SPIRIT_ORE_DI_SUI_CHANCE = builder.comment("地髓矿尝试概率倍率；1为设计概率，0为关闭，范围0至1").defineInRange("diSuiChance", 1.0D, 0.0D, 1.0D);
        SPIRIT_ORE_TIAN_JING_CHANCE = builder.comment("天晶矿尝试概率倍率；1为设计概率，0为关闭，范围0至1").defineInRange("tianJingChance", 1.0D, 0.0D, 1.0D);
        SPIRIT_ORE_XIAN_YU_CHANCE = builder.comment("仙玉矿尝试概率倍率；1为设计概率，0为关闭，范围0至1").defineInRange("xianYuChance", 1.0D, 0.0D, 1.0D);
        SPIRIT_ORE_DAO_SHI_CHANCE = builder.comment("道石矿尝试概率倍率；1为设计概率，0为关闭，范围0至1").defineInRange("daoShiChance", 1.0D, 0.0D, 1.0D);
        builder.pop();
        builder.push("spiritStone");
        SPIRIT_STONE_BACKLASH_START_DAMAGE = builder.comment("越阶吸收反噬的基础伤害；实际基础伤害再乘灵石与境界的阶位差，首次脉冲乘伤害倍率").defineInRange("backlashStartDamage", 1.0D, 0.05D, 20.0D);
        SPIRIT_STONE_BACKLASH_DAMAGE_MULTIPLIER = builder.comment("每次越级反噬脉冲的伤害倍率；默认首次2倍，之后4倍、8倍递增，计数不封顶").defineInRange("backlashDamageMultiplier", 2.0D, 1.01D, 10.0D);
        builder.pop();
        builder.push("terrain");
        THUNDER_WASTES_STRIKE_MIN_SECONDS = builder.comment("雷地环境落雷的最短间隔秒数；仅在有玩家处于已加载雷地区域时生效").defineInRange("thunderStrikeMinSeconds", 5, 1, 3600);
        THUNDER_WASTES_STRIKE_MAX_SECONDS = builder.comment("雷地环境落雷的最长间隔秒数；必须大于或等于最短间隔").defineInRange("thunderStrikeMaxSeconds", 20, 1, 3600);
        builder.pop();
        builder.push("materialQuality");
        String[] qualityKeys={"ordinary","low","medium","high","supreme"};
        double[] qualityDefaults={60,25,10,4,1};
        for(int i=0;i<5;i++)MATERIAL_QUALITY_WEIGHTS.add(builder.comment("鉴定权重；按五档权重总和归一化为概率，全部为0时仅出凡品").defineInRange(qualityKeys[i],qualityDefaults[i],0.0D,1000000.0D));
        builder.pop();
        builder.push("physiques");
        for (var type:com.longju.xiantu.cultivation.PhysiqueType.values())
            PHYSIQUE_WEIGHTS.add(builder.comment(type.displayName+"抽取权重；归一化，全部为0时仅出凡体")
                    .defineInRange(type.id,(double)type.defaultWeight,0.0D,1000000.0D));
        builder.pop();
        SPEC = builder.build();
    }

    private XiantuConfig() {
    }
}
