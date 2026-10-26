import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "src" / "main" / "resources"
ORES = [
    ("fan_ling_shi", 1, 6, -32, 30, 1, "stone"),
    ("ling_jing", 2, 5, -48, 48, 2, "stone"),
    ("ling_yu", 3, 4, -64, 16, 5, "deepslate"),
    ("xuan_jing", 4, 3, -64, 0, 13, "deepslate"),
    ("di_sui", 5, 3, -64, -16, 34, "deepslate"),
    ("tian_jing", 6, 2, -64, -24, 67, "deepslate"),
    ("xian_yu", 7, 2, -64, -40, 100, "deepslate"),
    ("dao_shi", 8, 1, -64, -48, 167, "deepslate"),
]


def write_json(relative, data):
    path = ROOT / relative
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


features = []
for name, tier, size, min_y, max_y, rarity, host in ORES:
    block_id = f"{name}_ore"
    features.append(f"xiantu:{name}_ore")
    write_json(f"assets/xiantu/blockstates/{block_id}.json", {
        "variants": {"": {"model": f"xiantu:block/{block_id}"}}
    })
    write_json(f"assets/xiantu/models/block/{block_id}.json", {
        "parent": "minecraft:block/cube_all",
        "textures": {"all": f"xiantu:block/ore_{name}"},
    })
    write_json(f"assets/xiantu/models/item/{block_id}.json", {
        "parent": f"xiantu:block/{block_id}"
    })
    write_json(f"data/xiantu/loot_tables/blocks/{block_id}.json", {
        "type": "minecraft:block",
        "pools": [{
            "rolls": 1,
            "entries": [{"type": "minecraft:item", "name": f"xiantu:{name}"}],
            "conditions": [{"condition": "minecraft:survives_explosion"}],
        }],
    })
    write_json(f"data/xiantu/worldgen/configured_feature/{name}_ore.json", {
        "type": "xiantu:small_spirit_ore",
        "config": {
            "size": size,
            "discard_chance_on_air_exposure": 0.0,
            "targets": [
                {"target": {"predicate_type": "minecraft:block_match", "block": "minecraft:stone"},
                 "state": {"Name": f"xiantu:{block_id}"}},
                {"target": {"predicate_type": "minecraft:block_match", "block": "minecraft:deepslate"},
                 "state": {"Name": f"xiantu:{block_id}"}},
            ],
        },
    })
    write_json(f"data/xiantu/worldgen/placed_feature/{name}_ore.json", {
        "feature": f"xiantu:{name}_ore",
        "placement": [
            {"type": "minecraft:rarity_filter", "chance": rarity},
            {"type": "minecraft:in_square"},
            {"type": "minecraft:height_range", "height": {
                "type": "minecraft:uniform",
                "min_inclusive": {"absolute": min_y},
                "max_inclusive": {"absolute": max_y},
            }},
            {"type": "minecraft:biome"},
        ],
    })

write_json("data/xiantu/forge/biome_modifier/add_spirit_ores.json", {
    "type": "forge:add_features",
    "biomes": "#minecraft:is_overworld",
    "features": features,
    "step": "underground_ores",
})

