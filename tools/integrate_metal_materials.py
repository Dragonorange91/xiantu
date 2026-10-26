import json
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "src/main/resources"
ART = ROOT / "asset_sources/mineral-material-textures-20261013/style-revision-v2/runtime"
ORE_ART = ROOT / "asset_sources/material-textures-20261013/runtime"
MATERIALS = [
    ("hanxing_iron", "寒星铁", 1, 8, -32, 64, 1.00),
    ("chisui_copper", "赤髓铜", 2, 6, -48, 48, 0.75),
    ("zixiao_copper", "紫霄铜", 3, 5, -64, 32, 0.40),
    ("jiuyou_iron", "九幽寒铁", 4, 4, -64, 16, 0.20),
    ("taiyi_gold", "太乙精金", 5, 3, -64, 0, 0.08),
    ("zifu_silver", "紫府仙银", 6, 3, -64, -24, 0.03),
    ("hunyuan_stone", "混沌元石", 7, 2, -64, -40, 0.01),
    ("taichu_shenjin", "太初神金", 8, 1, -64, -48, 0.005),
]


def write_json(relative, value):
    path = RES / relative
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def append_tag(relative, ids):
    path = RES / relative
    if path.exists():
        data = json.loads(path.read_text(encoding="utf-8"))
    else:
        data = {"replace": False, "values": []}
    data["replace"] = False
    data["values"] = list(dict.fromkeys([*data.get("values", []), *ids]))
    write_json(relative, data)


def tool_tag(tier):
    if tier == 1:
        return "minecraft:needs_stone_tool"
    if tier == 2:
        return "minecraft:needs_iron_tool"
    if tier == 3:
        return "xiantu:needs_hanxing_tool"
    if tier <= 6:
        return "xiantu:needs_jiuyou_tool"
    return "xiantu:needs_hundun_tool"


features = []
tool_values = {}
for item_id, display, tier, size, min_y, max_y, chance in MATERIALS:
    ore = f"{item_id}_ore"
    raw = f"{item_id}_raw"
    ingot = f"{item_id}_ingot"
    block = f"{item_id}_block"
    features.append(f"xiantu:{ore}")

    ore_source = ART / f"ore_{item_id}.png"
    if not ore_source.is_file():
        ore_source = ORE_ART / f"ore_{item_id}.png"
    for source, target in [
        (ore_source, RES / f"assets/xiantu/textures/block/ore_{item_id}.png"),
        (ART / f"ingot_{item_id}.png", RES / f"assets/xiantu/textures/item/ingot_{item_id}.png"),
        (ART / f"block_{item_id}.png", RES / f"assets/xiantu/textures/block/block_{item_id}.png"),
    ]:
        if not source.is_file():
            raise FileNotFoundError(source)
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(source, target)

    write_json(f"assets/xiantu/blockstates/{ore}.json", {"variants": {"": {"model": f"xiantu:block/{ore}"}}})
    write_json(f"assets/xiantu/blockstates/{block}.json", {"variants": {"": {"model": f"xiantu:block/{block}"}}})
    write_json(f"assets/xiantu/models/block/{ore}.json", {
        "parent": "minecraft:block/cube_all", "textures": {"all": f"xiantu:block/ore_{item_id}"}})
    write_json(f"assets/xiantu/models/block/{block}.json", {
        "parent": "minecraft:block/cube_all", "textures": {"all": f"xiantu:block/block_{item_id}"}})
    write_json(f"assets/xiantu/models/item/{ore}.json", {"parent": f"xiantu:block/{ore}"})
    write_json(f"assets/xiantu/models/item/{block}.json", {"parent": f"xiantu:block/{block}"})
    write_json(f"assets/xiantu/models/item/{raw}.json", {
        "parent": f"xiantu:block/{ore}"})
    write_json(f"assets/xiantu/models/item/{ingot}.json", {
        "parent": "minecraft:item/generated", "textures": {"layer0": f"xiantu:item/ingot_{item_id}"}})

    write_json(f"data/xiantu/loot_tables/blocks/{ore}.json", {
        "type": "minecraft:block",
        "pools": [{"rolls": 1, "entries": [{"type": "minecraft:alternatives", "children": [
            {"type": "minecraft:item", "name": f"xiantu:{ore}", "conditions": [{
                "condition": "minecraft:match_tool", "predicate": {"enchantments": [{
                    "enchantments": ["minecraft:silk_touch"], "levels": {"min": 1}}]}}]},
            {"type": "minecraft:item", "name": f"xiantu:{raw}"},
        ]}], "conditions": [{"condition": "minecraft:survives_explosion"}]}],
    })
    write_json(f"data/xiantu/loot_tables/blocks/{block}.json", {
        "type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"xiantu:{block}"}],
                                                 "conditions": [{"condition": "minecraft:survives_explosion"}]}]})

    write_json(f"data/xiantu/worldgen/configured_feature/{ore}.json", {
        "type": "xiantu:small_spirit_ore",
        "config": {"size": size, "discard_chance_on_air_exposure": 0.0, "targets": [
            {"target": {"predicate_type": "minecraft:block_match", "block": "minecraft:stone"}, "state": {"Name": f"xiantu:{ore}"}},
            {"target": {"predicate_type": "minecraft:block_match", "block": "minecraft:deepslate"}, "state": {"Name": f"xiantu:{ore}"}},
        ]},
    })
    write_json(f"data/xiantu/worldgen/placed_feature/{ore}.json", {
        "feature": f"xiantu:{ore}", "placement": [
            {"type": "minecraft:rarity_filter", "chance": 1},
            {"type": "minecraft:in_square"},
            {"type": "minecraft:height_range", "height": {"type": "minecraft:uniform",
                "min_inclusive": {"absolute": min_y}, "max_inclusive": {"absolute": max_y}}},
            {"type": "minecraft:biome"},
        ],
    })

    tool_values.setdefault(tool_tag(tier), []).append(f"xiantu:{ore}")
    write_json(f"data/xiantu/recipes/{block}_from_ingots.json", {
        "type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["III", "III", "III"],
        "key": {"I": {"item": f"xiantu:{ingot}"}}, "result": {"item": f"xiantu:{block}"}})
    write_json(f"data/xiantu/recipes/{ingot}s_from_{block}.json", {
        "type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": [{"item": f"xiantu:{block}"}],
        "result": {"item": f"xiantu:{ingot}", "count": 9}})

    if tier <= 2:
        for kind, cooking_time in [("smelting", 200), ("blasting", 100)]:
            write_json(f"data/xiantu/recipes/{ingot}_from_{raw}_{kind}.json", {
                "type": f"minecraft:{kind}", "category": "misc", "ingredient": {"item": f"xiantu:{raw}"},
                "result": f"xiantu:{ingot}", "experience": 0.7, "cookingtime": cooking_time})

append_tag("data/minecraft/tags/blocks/needs_stone_tool.json", tool_values.get("minecraft:needs_stone_tool", []))
append_tag("data/minecraft/tags/blocks/needs_iron_tool.json", tool_values.get("minecraft:needs_iron_tool", []))
for tag in ("needs_hanxing_tool", "needs_jiuyou_tool", "needs_hundun_tool"):
    append_tag(f"data/xiantu/tags/blocks/{tag}.json", tool_values.get(f"xiantu:{tag}", []))

write_json("data/xiantu/forge/biome_modifier/add_metal_ores.json", {
    "type": "forge:add_features", "biomes": "#minecraft:is_overworld", "features": features, "step": "underground_ores"})
