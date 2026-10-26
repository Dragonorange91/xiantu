package com.longju.xiantu.gongfa;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.List;

public final class GongfaRegistry {
    private GongfaRegistry() {}
    public static final List<GongfaDefinition> ALL = List.of(
        method("xuan_yuan_yang_qi_jue", "玄元养气决", 0, 0, 0, 0, 0, 0, 0, 0, 0, null, 0),
        method("jin_guang_jue", "金光诀", 1, 1, 0, 2, .06, .02, .05, 0, 0, GongfaBuff.SWIFTNESS, .04),
        method("qing_mu_jue", "青木诀", 1, 2, 0, 2, .04, .05, 0, .06, 0, GongfaBuff.MENDING, .05),
        method("xuan_shui_jue", "玄水诀", 1, 3, 0, 2, .10, .02, 0, .04, 0, GongfaBuff.AGILITY, .03),
        method("chi_yan_jue", "赤焰诀", 1, 4, 0, 2, .06, .03, .05, 0, 0, GongfaBuff.SWIFTNESS, .05),
        method("yan_tu_jue", "岩土诀", 1, 5, 0, 2, .08, .02, 0, .04, 1, GongfaBuff.FORTITUDE, .04),
        method("geng_jin_ning_yuan_jue", "庚金凝元诀", 2, 1, 0, 2, .12, .04, .07, 0, 1, GongfaBuff.SWIFTNESS, .06),
        method("qing_mu_gui_yuan_jue", "青木归元诀", 2, 2, 0, 2, .08, .08, 0, .08, 0, GongfaBuff.MENDING, .08),
        method("cang_lan_zhen_jing", "沧澜真经", 2, 3, 0, 2, .20, .04, 0, .06, 1, GongfaBuff.AGILITY, .05),
        method("li_huo_zhen_jing", "离火真经", 2, 4, 0, 2, .12, .06, .08, 0, 0, GongfaBuff.SWIFTNESS, .07),
        method("shan_yue_zhen_jing", "山岳真经", 2, 5, 0, 2, .16, .04, 0, .08, 2, GongfaBuff.FORTITUDE, .07),
        method("jin_guang_xuan_gong", "金光玄功", 3, 1, 1, 6, .21, .06, .10, 0, 1, GongfaBuff.SWIFTNESS, .09),
        method("bai_cao_zhen_jing", "百草真经", 3, 2, 1, 6, .14, .12, 0, .10, 1, GongfaBuff.MENDING, .10),
        method("you_shui_zhen_jing", "幽水真经", 3, 3, 1, 6, .35, .06, 0, .12, 1, GongfaBuff.AGILITY, .07),
        method("fen_chi_chi_huo", "赤霄真经", 3, 4, 1, 6, .21, .09, .12, .02, 1, GongfaBuff.SWIFTNESS, .10),
        method("qing_shan_zhen_di", "擎山真经", 3, 5, 1, 6, .28, .06, 0, .14, 3, GongfaBuff.FORTITUDE, .09),
        method("wu_xing_gui_cang_jing", "五行归藏经", 4, 0, 1, 6, .50, .08, .09, .10, 2, GongfaBuff.FORTITUDE, .08),
        method("zi_fu_zhou_tian_jing", "紫府周天经", 4, 0, 1, 6, .30, .16, .06, .12, 1, GongfaBuff.AGILITY, .08),
        method("tai_qing_hun_yuan_jing", "太清混元经", 5, 0, 1, 6, .65, .10, .11, .14, 2, GongfaBuff.FORTITUDE, .11),
        method("xuan_pin_yang_shen_jing", "玄牝养神经", 5, 0, 1, 6, .39, .20, .05, .18, 1, GongfaBuff.MENDING, .12),
        method("tai_shang_yan_dao_jing", "太上衍道经", 6, 0, 5, 8, .80, .12, .15, .12, 2, GongfaBuff.SWIFTNESS, .13),
        method("wu_ji_dong_xuan_jing", "无极洞玄经", 6, 0, 5, 8, .48, .24, .07, .22, 2, GongfaBuff.AGILITY, .10),
        method("tai_chu_dao_jing", "太初道经", 7, 0, 0, 8, 1, .15, .12, .18, 2, GongfaBuff.MENDING, .15),
        method("hun_dun_gui_yi_jing", "混沌归一经", 7, 0, 5, 8, .60, .30, .15, .20, 3, GongfaBuff.FORTITUDE, .12)
    );
    public static final Map<String, GongfaDefinition> DEFINITIONS = ALL.stream().collect(Collectors.toUnmodifiableMap(GongfaDefinition::id, Function.identity()));
    private static GongfaDefinition method(String id, String name, int tier, int element, int min, int max,
                                           double capacity, double recovery, double attack, double health, double armor,
                                           GongfaBuff buff, double buffBonus) {
        return new GongfaDefinition(id, name, tier, element, min, max, capacity, recovery, attack, health, armor, buff, buffBonus);
    }
    public static int index(String id) { for (int i=0;i<ALL.size();i++) if (ALL.get(i).id().equals(id)) return i; return -1; }
    public static GongfaDefinition get(String id) { return DEFINITIONS.get(id); }
}
