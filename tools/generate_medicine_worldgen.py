from __future__ import annotations

import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
RESOURCE = ROOT / "src/main/resources/data/xiantu"
MANIFEST = ROOT / "asset_sources/medicine-20261012-v2/manifest.json"

BIOMES = {
    "forest": ["minecraft:forest", "minecraft:flower_forest", "minecraft:birch_forest", "minecraft:old_growth_birch_forest", "minecraft:dark_forest"],
    "plains": ["minecraft:plains", "minecraft:sunflower_plains", "minecraft:meadow"],
    "river": ["minecraft:river", "minecraft:frozen_river"],
    "wetlands": ["minecraft:swamp", "minecraft:mangrove_swamp"],
    "warm_forest": ["minecraft:forest", "minecraft:dark_forest", "minecraft:jungle", "minecraft:sparse_jungle"],
    "dry_grassland": ["minecraft:savanna", "minecraft:savanna_plateau", "minecraft:windswept_savanna", "minecraft:badlands", "minecraft:wooded_badlands"],
    "mountain_meadow": ["minecraft:meadow", "minecraft:grove", "minecraft:windswept_hills", "minecraft:windswept_forest", "minecraft:windswept_gravelly_hills", "minecraft:jagged_peaks", "minecraft:stony_peaks", "minecraft:snowy_slopes"],
    "cold_water": ["minecraft:frozen_river", "minecraft:snowy_plains", "minecraft:ice_spikes", "minecraft:grove", "minecraft:snowy_slopes"],
    "lush_caves": ["minecraft:lush_caves"],
    "deep_caves": ["minecraft:dripstone_caves", "minecraft:lush_caves"],
    "highlands": ["minecraft:meadow", "minecraft:grove", "minecraft:windswept_hills", "minecraft:windswept_forest", "minecraft:windswept_gravelly_hills"],
}

SPECIAL_HABITATS = {
    "一阶·青萝": ["forest", "plains"],
    "一阶·凝血花": ["forest", "wetlands"],
    "一阶·露凝草": ["river", "forest"],
    "一阶·清心叶": ["forest"],
    "一阶·火棘果": ["dry_grassland"],
    "一阶·石心苔": ["deep_caves"],
    "一阶·风铃花": ["plains"],
    "二阶·赤芝": ["warm_forest"],
    "二阶·冰心莲": ["cold_water", "wetlands"],
    "二阶·淬毒菇": ["lush_caves"],
    "二阶·疾风叶": ["plains", "mountain_meadow"],
    "三阶·紫参": ["mountain_meadow"],
    "三阶·紫电花": ["mountain_meadow"],
    "三阶·赤炎果": ["dry_grassland"],
    "三阶·厚土根": ["mountain_meadow", "deep_caves"],
    "三阶·青木心": ["forest", "warm_forest"],
    "四阶·玄莲": ["cold_water"],
    "四阶·金髓花": ["lush_caves"],
    "四阶·凝魂芝": ["deep_caves"],
    "四阶·化血参": ["highlands"],
    "五阶·天道花": ["highlands"],
    "五阶·太虚草": ["highlands", "deep_caves"],
    "六阶·月华草": ["river", "forest"],
    "六阶·月魄莲": ["cold_water"],
    "六阶·虚空藤": ["deep_caves"],
}


def write_json(path: Path, value) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def habitat_for(name: str, tier: int) -> list[str]:
    if tier == 7:
        return ["highlands"]
    if tier == 8:
        return ["highlands", "cold_water"]
    if name in SPECIAL_HABITATS:
        return SPECIAL_HABITATS[name]
    return {
        1: ["forest", "plains"],
        2: ["warm_forest", "wetlands", "cold_water"],
        3: ["mountain_meadow", "deep_caves"],
        4: ["lush_caves", "cold_water", "highlands"],
        5: ["deep_caves", "highlands"],
        6: ["deep_caves", "cold_water", "highlands"],
    }[tier]


def configured_patch(block_id: str, tries: int) -> dict:
    return {
        "type": "minecraft:random_patch",
        "config": {
            "tries": tries,
            "xz_spread": 7,
            "y_spread": 0,
            "feature": {
                "feature": {
                    "type": "minecraft:simple_block",
                    "config": {
                        "to_place": {
                            "type": "minecraft:simple_state_provider",
                            "state": {"Name": block_id},
                        }
                    },
                },
                "placement": [{
                    "type": "minecraft:block_predicate_filter",
                    "predicate": {"type": "minecraft:would_survive", "state": {"Name": block_id}},
                }, {
                    "type": "minecraft:block_predicate_filter",
                    "predicate": {"type": "minecraft:matching_blocks", "blocks": ["minecraft:air"]},
                }],
            },
        },
    }


def main() -> None:
    manifest = json.loads(MANIFEST.read_text(encoding="utf-8"))
    all_herbs = [entry for entry in manifest if entry["kind"] in {"plant_lifecycle", "non_plant_material", "drop_only_herb"}]
    herbs = [entry for entry in all_herbs if entry["kind"] == "plant_lifecycle"]
    # Herb nodes are individually sparse because dozens of species overlap
    # across neighboring habitats; their aggregate density must stay low.
    attempt_chance = {1: 12, 2: 18, 3: 26, 4: 40, 5: 52, 6: 64, 7: 68, 8: 96}
    patch_tries = {tier: 1 for tier in attempt_chance}
    registered = 0

    for entry in herbs:
        tier = int(entry["tier"])
        index = all_herbs.index(entry) + 1
        registry_id = f"herb_{index:04d}"
        block_id = f"xiantu:{registry_id}_wild"
        habitats = habitat_for(entry["name"], tier)
        medicine_plain_habitat = tier <= 8
        habitat_ids = {biome for habitat in habitats for biome in BIOMES[habitat]}
        tag_id = f"medicine/{registry_id}"
        write_json(RESOURCE / "tags/worldgen/biome" / f"{tag_id}.json", {"replace": False, "values": sorted(habitat_ids)})
        configured_id = f"{registry_id}_wild_patch"
        placed_id = f"{registry_id}_wild_patch"
        write_json(RESOURCE / "worldgen/configured_feature" / f"{configured_id}.json",
                   configured_patch(block_id, patch_tries[tier]))

        if habitat_ids == BIOMES.get("deep_caves") or set(habitat_ids).issubset(set(BIOMES["deep_caves"])):
            height = {"type": "minecraft:uniform", "min_inclusive": {"absolute": -52}, "max_inclusive": {"absolute": 20}}
            height_modifier = {"type": "minecraft:height_range", "height": height}
        else:
            height_modifier = {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"}
        write_json(RESOURCE / "worldgen/placed_feature" / f"{placed_id}.json", {
            "feature": f"xiantu:{configured_id}",
            "placement": [
                {"type": "minecraft:rarity_filter", "chance": attempt_chance[tier]},
                {"type": "minecraft:in_square"},
                height_modifier,
                {"type": "minecraft:biome"},
            ],
        })
        write_json(RESOURCE / "forge/biome_modifier" / f"{registry_id}_wild_patch.json", {
            "type": "forge:add_features",
            "biomes": f"#xiantu:{tag_id}",
            "features": [f"xiantu:{placed_id}"],
            "step": "vegetal_decoration",
        })
        if medicine_plain_habitat:
            plain_tag_id = f"medicine/spirit_medicine_plain/{registry_id}"
            plain_feature_id = f"{registry_id}_medicine_plain_wild_patch"
            write_json(RESOURCE / "tags/worldgen/biome" / f"{plain_tag_id}.json", {
                "replace": False, "values": ["xiantu:spirit_medicine_plain"],
            })
            write_json(RESOURCE / "worldgen/configured_feature" / f"{plain_feature_id}.json",
                       configured_patch(block_id, patch_tries[tier]))
            write_json(RESOURCE / "worldgen/placed_feature" / f"{plain_feature_id}.json", {
                "feature": f"xiantu:{plain_feature_id}",
                "placement": [
                    {"type": "minecraft:rarity_filter", "chance": max(1, attempt_chance[tier] // 2)},
                    {"type": "minecraft:in_square"},
                    height_modifier,
                    {"type": "minecraft:biome"},
                ],
            })
            write_json(RESOURCE / "forge/biome_modifier" / f"{plain_feature_id}.json", {
                "type": "forge:add_features",
                "biomes": f"#xiantu:{plain_tag_id}",
                "features": [f"xiantu:{plain_feature_id}"],
                "step": "vegetal_decoration",
            })
        registered += 1

    print(f"Generated medicine worldgen for {registered} wild plants")


if __name__ == "__main__":
    main()
