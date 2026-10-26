package com.longju.xiantu.cultivation;

/** Independent random specialization; neither a cultivation realm nor an item quality. */
public enum PhysiqueType {
    ORDINARY("ordinary", "凡体", 60, 1, 1, 0, 0, 1, 0, 1, 0, 0, 1, 1, 1),
    QINGLING("qingling", "九窍玲珑体", 6.5, 1.35, 1, 0, 0, 1, 0, 1, 0, 0, 1, 1, 1),
    MINGWU("mingwu", "菩提悟道体", 5, 1, 2, 0, 0, 1, 0, 1, 0, 0, 1, 1, 1),
    PANSHI("panshi", "玄黄镇岳体", 5, 1, 1, .10, 0, 1, 2, 1, 0, 0, 1, 1, 1),
    CHANGXI("changxi", "沧海归元体", 5, 1, 1, 0, .15, 1.25, 0, 1, 0, 0, 1, 1, 1),
    WUXING("wuxing", "万法通玄体", 4, 1, 1, 0, 0, 1, 0, 1, 0, 0, 1, 1.5, 1),
    BAILIAN("bailian", "天工造化体", 3, 1, 1, 0, 0, 1, 0, 1, 0, 0, 1, 1, 1.5),
    HUNYUAN("hunyuan", "太初道体", 1, 1.5, 1.5, 0, 0, 1, 0, 1, 0, 0, 1, 1, 1),
    QINGDI("qingdi", "青帝长生体", 4, 1, 1, .05, 0, 1, 0, 1.10, 0, 0, 1, 1, 1),
    JINGLIAN("jinglian", "无垢净莲体", 3, 1, 1, 0, 0, 1, 0, 1, 0, 0, .80, 1, 1),
    DOUZHAN("douzhan", "斗战圣体", 2.5, 1, 1, 0, 0, 1, 1, 1, .08, 0, 1, 1, 1),
    XINGCHEN("xingchen", "周天星辰体", 1, 1.20, 1, 0, 0, 1, 0, 1, 0, .03, 1, 1, 1);

    public final String id, displayName;
    public final double defaultWeight;
    public final int armor;
    public final double cultivation, methodExperience, healthBonus, powerBonus, meditationRecovery,
            healing, meleeBonus, breakthroughBonus, demonGrowth, spellExperience, professionExperience;

    PhysiqueType(String id, String name, double weight, double cultivation, double methodExperience,
                 double healthBonus, double powerBonus, double meditationRecovery, int armor,
                 double healing, double meleeBonus, double breakthroughBonus, double demonGrowth,
                 double spellExperience, double professionExperience) {
        this.id=id; displayName=name; defaultWeight=weight; this.cultivation=cultivation;
        this.methodExperience=methodExperience; this.healthBonus=healthBonus; this.powerBonus=powerBonus;
        this.meditationRecovery=meditationRecovery; this.armor=armor; this.healing=healing;
        this.meleeBonus=meleeBonus; this.breakthroughBonus=breakthroughBonus; this.demonGrowth=demonGrowth;
        this.spellExperience=spellExperience; this.professionExperience=professionExperience;
    }
    public static PhysiqueType byId(String id) {
        for (PhysiqueType type:values()) if (type.id.equals(id)) return type;
        return ORDINARY;
    }
    public static double[] defaultWeights() {
        return java.util.Arrays.stream(values()).mapToDouble(t->t.defaultWeight).toArray();
    }
    /** Half-open weighted intervals; invalid/all-zero weights safely fall back to ordinary. */
    public static PhysiqueType roll(double unit, double[] weights) {
        if (weights.length != values().length) throw new IllegalArgumentException("12 physique weights required");
        double total=0;
        for (double w:weights) if (Double.isFinite(w) && w>0) total+=w;
        if (!(total>0) || !Double.isFinite(total)) return ORDINARY;
        double point=Math.max(0,Math.min(Math.nextDown(1.0),Double.isFinite(unit)?unit:0))*total;
        for (int i=0;i<weights.length;i++) {
            double w=Double.isFinite(weights[i])?Math.max(0,weights[i]):0;
            if (point<w) return values()[i];
            point-=w;
        }
        return ORDINARY;
    }
    public java.util.List<String> description() {
        return switch(this) {
            case ORDINARY -> java.util.List.of("无额外加成");
            case QINGLING -> java.util.List.of("打坐修炼 ×1.35");
            case MINGWU -> java.util.List.of("主功法经验 ×2.00（打坐 / 杀怪）");
            case PANSHI -> java.util.List.of("最大生命 +10%","护甲 +2");
            case CHANGXI -> java.util.List.of("最大灵力 +15%","打坐灵力回复 ×1.25");
            case WUXING -> java.util.List.of("法术熟练经验 ×1.50（待法术模块）");
            case BAILIAN -> java.util.List.of("制造职业经验 ×1.50（待职业模块）");
            case HUNYUAN -> java.util.List.of("打坐修炼 ×1.50","主功法经验 ×1.50");
            case QINGDI -> java.util.List.of("最大生命 +5%","有效治疗量 +10%");
            case JINGLIAN -> java.util.List.of("正向心魔增长量 ×0.80");
            case DOUZHAN -> java.util.List.of("直接近战伤害 +8%","护甲 +1");
            case XINGCHEN -> java.util.List.of("打坐修炼 ×1.20","突破成功率 +3个百分点");
        };
    }
}
