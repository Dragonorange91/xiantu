package com.longju.xiantu.entity;

import java.util.*;

/** Anatomical candidates, not independent rolls for every body part. */
public final class BeastResourceRules {
    public static final Set<String> SPECIES=Set.of(
            "yanwei_fox","cracked_blade_wolf","crag_rock_spider","cold_fang_wolf","thunder_feather_raven",
            "vine_deer","mountain_bear","gold_mane_lion","red_sun_lion","thick_horn_rhino",
            "moss_back_bear","thunder_deer","iron_tooth_badger","rock_back_boar",
            "thunder_prison_lion","electric_marten","flint_armored_lizard","molten_turtle","sand_armadillo",
            "jade_scale_crocodile","copper_beak_vulture","fire_beak_falcon","purple_lightning_bat",
            "azure_scale_python","ancient_branch_python","tide_pattern_ray","dark_shell_crab","gold_back_beetle",
            "jade_leaf_mantis","mushroom_toad");
    private static final Map<String,Set<String>> SOURCES=Map.of(
            "beast_bone",Set.of("iron_tooth_badger","cracked_blade_wolf","cold_fang_wolf","gold_mane_lion","moss_back_bear","mountain_bear","red_sun_lion","thunder_prison_lion"),
            "beast_fang",Set.of("iron_tooth_badger","cracked_blade_wolf","cold_fang_wolf","yanwei_fox","jade_scale_crocodile","ancient_branch_python","azure_scale_python","rock_back_boar"),
            "beast_claw",Set.of("cracked_blade_wolf","cold_fang_wolf","gold_mane_lion","moss_back_bear","mountain_bear","red_sun_lion","thunder_prison_lion"),
            "beast_sinew",Set.of("cracked_blade_wolf","cold_fang_wolf","vine_deer","thunder_deer","rock_back_boar","thick_horn_rhino"),
            "beast_scale",Set.of("jade_scale_crocodile","ancient_branch_python","azure_scale_python","flint_armored_lizard"),
            "beast_horn",Set.of("vine_deer","thunder_deer","thick_horn_rhino"),
            "beast_venom_sac",Set.of("mushroom_toad","crag_rock_spider"),
            "beast_silk",Set.of("crag_rock_spider"));
    public static final Map<String,String> EXCLUSIVE=Map.of("cracked_blade_wolf","wolf_hair","yanwei_fox","fox_hair",
            "copper_beak_vulture","vulture_feather","gold_mane_lion","lion_mane","fire_beak_falcon","falcon_feather",
            "thunder_feather_raven","thunder_crow_feather");
    public static String mainMaterial(String species) {
        if(Set.of("copper_beak_vulture","fire_beak_falcon").contains(species)) return "beast_feather";
        if(Set.of("gold_back_beetle","jade_leaf_mantis","dark_shell_crab","crag_rock_spider").contains(species)) return "beast_carapace";
        return "beast_hide";
    }
    public static List<String> candidates(String species) {
        return SOURCES.entrySet().stream().filter(e->e.getValue().contains(species)).map(Map.Entry::getKey).sorted().toList();
    }
    public static String selectAuxiliary(String species,double chance,double roll,int candidateIndex) {
        var candidates=candidates(species);
        if(candidates.isEmpty()||roll>=chance||chance<=0) return "";
        return candidates.get(Math.floorMod(candidateIndex,candidates.size()));
    }
    private BeastResourceRules() {}
}
