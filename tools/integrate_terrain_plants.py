from __future__ import annotations

import json
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "asset_sources/terrain-20261012/runtime/plants"
RESOURCES = ROOT / "src/main/resources"
PLANTS = {
    "thunder_sedge": ("电弧草", "thunder_wastes", 4, "thunder_soil"),
    "storm_bamboo": ("雷竹", "thunder_wastes", 10, "thunder_rock"),
    "meteor_ash_grass": ("灰烬草", "meteor_wasteland", 12, "meteor_soil"),
    "valley_jade_fern": ("谷地蕨", "spirit_medicine_plain", 8, "valley_fertile_soil"),
}


def write(path: Path, value: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def patch_feature(block_id: str, tries: int = 4) -> dict:
    state = {"Name": f"xiantu:{block_id}"}
    return {
        "type": "minecraft:random_patch",
        "config": {
            "tries": tries,
            "xz_spread": 4,
            "y_spread": 0,
            "feature": {
                "feature": {
                    "type": "minecraft:simple_block",
                    "config": {"to_place": {"type": "minecraft:simple_state_provider", "state": state}},
                },
                "placement": [{
                    "type": "minecraft:block_predicate_filter",
                    "predicate": {"type": "minecraft:would_survive", "state": state},
                }, {
                    "type": "minecraft:block_predicate_filter",
                    "predicate": {"type": "minecraft:matching_blocks", "blocks": ["minecraft:air"]},
                }],
            },
        },
    }


def rock_outcrop_feature(rock_id: str, soil_id: str, tries: int = 2) -> dict:
    return {
        "type": "minecraft:random_patch",
        "config": {
            "tries": tries,
            "xz_spread": 3,
            "y_spread": 0,
            "feature": {
                "feature": {
                    "type": "minecraft:simple_block",
                    "config": {"to_place": {"type": "minecraft:simple_state_provider",
                                                "state": {"Name": f"xiantu:{rock_id}"}}},
                },
                "placement": [{
                    "type": "minecraft:block_predicate_filter",
                    "predicate": {"type": "minecraft:matching_blocks", "blocks": ["minecraft:air"]},
                }, {
                    "type": "minecraft:block_predicate_filter",
                    "predicate": {"type": "minecraft:matching_blocks", "blocks": [f"xiantu:{soil_id}"],
                                  "offset": [0, -1, 0]},
                }],
            },
        },
    }


def main() -> None:
    for plant, (name, biome, rarity, _) in PLANTS.items():
        texture_source = ASSETS / f"{plant}_64.png"
        if not texture_source.is_file():
            raise SystemExit(f"Missing generated plant sprite: {texture_source}")
        shutil.copy2(texture_source, RESOURCES / f"assets/xiantu/textures/block/{plant}.png")
        if plant == "storm_bamboo":
            write(RESOURCES / f"assets/xiantu/blockstates/{plant}.json", {
                "variants": {
                    "half=lower": {"model": f"xiantu:block/{plant}_bottom"},
                    "half=upper": {"model": f"xiantu:block/{plant}_top"},
                },
            })
            for part, v0, v1 in (("bottom", 8, 16), ("top", 0, 8)):
                faces = {
                    "north": {"uv": [0, v0, 16, v1], "texture": "#cross"},
                    "south": {"uv": [0, v0, 16, v1], "texture": "#cross"},
                    "west": {"uv": [0, v0, 16, v1], "texture": "#cross"},
                    "east": {"uv": [0, v0, 16, v1], "texture": "#cross"},
                }
                write(RESOURCES / f"assets/xiantu/models/block/{plant}_{part}.json", {
                    "ambientocclusion": False,
                    "textures": {"cross": f"xiantu:block/{plant}"},
                    "elements": [
                        {"from": [2, 0, 8], "to": [14, 16, 8], "shade": False,
                         "faces": {"north": faces["north"], "south": faces["south"]}},
                        {"from": [8, 0, 2], "to": [8, 16, 14], "shade": False,
                         "faces": {"west": faces["west"], "east": faces["east"]}},
                    ],
                })
        else:
            write(RESOURCES / f"assets/xiantu/blockstates/{plant}.json", {
                "variants": {"": {"model": f"xiantu:block/{plant}"}},
            })
            write(RESOURCES / f"assets/xiantu/models/block/{plant}.json", {
                "parent": "minecraft:block/cross", "textures": {"cross": f"xiantu:block/{plant}"},
            })
        write(RESOURCES / f"assets/xiantu/models/item/{plant}.json", {
            "parent": "minecraft:item/generated", "textures": {"layer0": f"xiantu:block/{plant}"},
        })
        loot = {
            "type": "minecraft:block",
            "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"xiantu:{plant}"}]}],
        }
        if plant == "storm_bamboo":
            loot["pools"][0]["conditions"] = [{
                "condition": "minecraft:block_state_property", "block": "xiantu:storm_bamboo",
                "properties": {"half": "lower"},
            }]
        write(RESOURCES / f"data/xiantu/loot_tables/blocks/{plant}.json", loot)
        feature_id = f"{plant}_patch"
        tries = 6 if plant == "thunder_sedge" else 5 if plant == "storm_bamboo" else 4
        if plant == "storm_bamboo":
            write(RESOURCES / f"data/xiantu/worldgen/configured_feature/{feature_id}.json", {
                "type": "xiantu:storm_bamboo_patch", "config": {},
            })
        else:
            write(RESOURCES / f"data/xiantu/worldgen/configured_feature/{feature_id}.json",
                  patch_feature(plant, tries))
        write(RESOURCES / f"data/xiantu/worldgen/placed_feature/{feature_id}.json", {
            "feature": f"xiantu:{feature_id}",
            "placement": [
                {"type": "minecraft:rarity_filter", "chance": rarity},
                {"type": "minecraft:in_square"},
                {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"},
                {"type": "minecraft:biome"},
            ],
        })
        biome_tag = f"terrain/{biome}"
        write(RESOURCES / f"data/xiantu/tags/worldgen/biome/{biome_tag}.json", {
            "replace": False, "values": [f"xiantu:{biome}"],
        })
        write(RESOURCES / f"data/xiantu/forge/biome_modifier/{feature_id}.json", {
            "type": "forge:add_features",
            "biomes": f"#xiantu:{biome_tag}",
            "features": [f"xiantu:{feature_id}"],
            "step": "vegetal_decoration",
        })

    outcrops = {
        "thunder_rock_outcrop": ("thunder_rock", "thunder_soil", "thunder_wastes", 16),
        "meteor_rock_outcrop": ("meteor_rock", "meteor_soil", "meteor_wasteland", 48),
    }
    for feature_id, (rock, soil, biome, rarity) in outcrops.items():
        write(RESOURCES / f"data/xiantu/worldgen/configured_feature/{feature_id}.json",
              rock_outcrop_feature(rock, soil, 3 if feature_id == "thunder_rock_outcrop" else 2))
        write(RESOURCES / f"data/xiantu/worldgen/placed_feature/{feature_id}.json", {
            "feature": f"xiantu:{feature_id}",
            "placement": [
                {"type": "minecraft:rarity_filter", "chance": rarity},
                {"type": "minecraft:in_square"},
                {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"},
                {"type": "minecraft:biome"},
            ],
        })
        write(RESOURCES / f"data/xiantu/forge/biome_modifier/{feature_id}.json", {
            "type": "forge:add_features",
            "biomes": f"#xiantu:terrain/{biome}",
            "features": [f"xiantu:{feature_id}"],
            "step": "vegetal_decoration",
        })

    lang_path = RESOURCES / "assets/xiantu/lang/zh_cn.json"
    lang = json.loads(lang_path.read_text(encoding="utf-8"))
    lang.update({f"block.xiantu.{plant}": value[0] for plant, value in PLANTS.items()})
    lang.pop("block.xiantu.cold_silver_reed", None)
    lang_path.write_text(json.dumps(lang, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    plant_manifest_path = ASSETS.parent.parent / "plants_manifest.json"
    plant_manifest = json.loads(plant_manifest_path.read_text(encoding="utf-8"))
    for plant in PLANTS:
        plant_manifest[plant]["status"] = "integrated_build_verified"
    plant_manifest_path.write_text(json.dumps(plant_manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    en_lang_path = RESOURCES / "assets/xiantu/lang/en_us.json"
    en_lang = json.loads(en_lang_path.read_text(encoding="utf-8"))
    en_lang.update({f"block.xiantu.{plant}": value[0] for plant, value in PLANTS.items()})
    en_lang.pop("block.xiantu.cold_silver_reed", None)
    en_lang_path.write_text(json.dumps(en_lang, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Integrated {len(PLANTS)} native-style decorative plants and biome features")
    # Preserve the accepted slope-aware habitat dressing when regenerating assets.
    from enrich_biome_decorations import main as enrich
    enrich()


if __name__ == "__main__":
    main()
