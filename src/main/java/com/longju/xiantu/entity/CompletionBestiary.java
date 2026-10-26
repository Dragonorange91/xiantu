package com.longju.xiantu.entity;
import java.util.*;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;
/** Source-fitted models; combat stats are provisional until the realm rollout. */
public final class CompletionBestiary {
    public record Definition(String id, String model, String behavior, String kind, String element,
            float width, float height, float scale, double health, double damage, int eggBase, int eggSpot) {
        public boolean passive() { return behavior.equals("P"); }
        public boolean hostile() { return behavior.equals("H"); }
        public boolean boss() { return kind.equals("BOSS"); }
    }
    public static final List<Definition> ARCHIVED_BOSSES = List.of(
        new Definition("battle_general_soul", "bai_zhan_jiang_hun", "H", "BOSS", "metal", 1.1F, 2.9F, 3.152386F, 160, 8, 0x84622C, 0xE8CD80),
        new Definition("xuantians_remnant", "xuan_tian_cheng_zhu", "H", "BOSS", "water", 1.2F, 3.2F, 3.576372F, 160, 8, 0x245971, 0x8AD4DD),
        new Definition("cloud_sea_lord", "yun_hai_tian_jun", "H", "BOSS", "water", 1.1F, 3.0F, 3.362546F, 160, 8, 0x245971, 0x8AD4DD));
    public static final List<Definition> ALL = List.of(
        new Definition("thunder_prison_lion", "lei_yu_shi", "H", "LAND", "thunder", 1.5F, 2.0F, 3.451683F, 36, 4, 0x443460, 0xCDC4FF),
        new Definition("electric_marten", "dian_wen_diao", "H", "LAND", "thunder", 0.8F, 0.85F, 1.776172F, 36, 4, 0x443460, 0xCDC4FF),
        new Definition("flint_armored_lizard", "sui_jia_xi", "N", "LAND", "fire", 1.15F, 0.8F, 2.549803F, 36, 4, 0x863C24, 0xFFA538),
        new Definition("molten_turtle", "rong_wen_gui", "P", "LAND", "fire", 1.3F, 1.0F, 2.585861F, 36, 4, 0x863C24, 0xFFA538),
        new Definition("sand_armadillo", "sha_jia_qiu_yu", "P", "LAND", "earth", 0.95F, 0.8F, 2.537794F, 36, 4, 0x66523B, 0xD4B975),
        new Definition("jade_scale_crocodile", "bi_lin_e", "H", "LAND", "water", 1.4F, 0.8F, 2.583925F, 36, 4, 0x245971, 0x8AD4DD),
        new Definition("five_element_warden", "wu_xing_zhen_yu_shou", "H", "BOSS", "earth", 2.6F, 3.0F, 6.981818F, 160, 8, 0x66523B, 0xD4B975),
        new Definition("copper_beak_vulture", "tong_hui_jiu", "H", "FLY", "metal", 0.8F, 1.1F, 1.798528F, 36, 4, 0x84622C, 0xE8CD80),
        new Definition("fire_beak_falcon", "huo_hui_sun", "H", "FLY", "fire", 0.7F, 0.9F, 1.700591F, 36, 4, 0x863C24, 0xFFA538),
        new Definition("purple_lightning_bat", "zi_dian_fu", "H", "FLY", "thunder", 0.65F, 0.7F, 1.780132F, 36, 4, 0x443460, 0xCDC4FF),
        new Definition("azure_scale_python", "cang_lin_mang", "H", "LAND", "water", 1.2F, 1.55F, 2.398127F, 36, 4, 0x245971, 0x8AD4DD),
        new Definition("ancient_branch_python", "cang_zhi_mang", "H", "LAND", "wood", 1.25F, 1.6F, 2.425721F, 36, 4, 0x426B36, 0xBED58F),
        new Definition("tide_pattern_ray", "chao_wen_yao", "P", "WATER", "water", 1.6F, 0.3F, 2.755150F, 36, 4, 0x245971, 0x8AD4DD),
        new Definition("dark_shell_crab", "xuan_ke_xie", "N", "LAND", "water", 1.35F, 0.85F, 2.095629F, 36, 4, 0x245971, 0x8AD4DD),
        new Definition("gold_back_beetle", "jin_bei_jia_chong", "N", "LAND", "metal", 1.1F, 0.75F, 2.250549F, 36, 4, 0x84622C, 0xE8CD80),
        new Definition("jade_leaf_mantis", "bi_ye_tang", "H", "LAND", "wood", 0.95F, 1.45F, 2.283244F, 36, 4, 0x426B36, 0xBED58F),
        new Definition("mushroom_toad", "mo_san_chan", "N", "LAND", "wood", 1.1F, 0.95F, 1.857597F, 36, 4, 0x426B36, 0xBED58F),
        new Definition("meteor_general", "xing_yun_tian_jiang", "H", "BOSS", "metal", 1.2F, 3.3F, 3.677784F, 160, 8, 0x84622C, 0xE8CD80),
        new Definition("spirit_ape_king", "tong_ling_yuan_wang", "H", "BOSS", "earth", 1.4F, 2.9F, 3.360822F, 160, 8, 0x66523B, 0xD4B975));
    public static Definition get(EntityType<?> type) {
        var key=ForgeRegistries.ENTITY_TYPES.getKey(type);
        return ALL.stream().filter(d -> key != null && d.id().equals(key.getPath())).findFirst().orElseThrow();
    }
    private CompletionBestiary() {}
}
