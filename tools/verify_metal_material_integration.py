import json
import struct
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "src/main/resources"
MATERIALS = [
    ("hanxing_iron", 1, 8, -32, 64, 1.00, "minecraft:needs_stone_tool"),
    ("chisui_copper", 2, 6, -48, 48, 0.75, "minecraft:needs_iron_tool"),
    ("zixiao_copper", 3, 5, -64, 32, 0.40, "xiantu:needs_hanxing_tool"),
    ("jiuyou_iron", 4, 4, -64, 16, 0.20, "xiantu:needs_jiuyou_tool"),
    ("taiyi_gold", 5, 3, -64, 0, 0.08, "xiantu:needs_jiuyou_tool"),
    ("zifu_silver", 6, 3, -64, -24, 0.03, "xiantu:needs_jiuyou_tool"),
    ("hunyuan_stone", 7, 2, -64, -40, 0.01, "xiantu:needs_hundun_tool"),
    ("taichu_shenjin", 8, 1, -64, -48, 0.005, "xiantu:needs_hundun_tool"),
]
checks = 0


def load(relative):
    global checks
    checks += 1
    return json.loads((RES / relative).read_text(encoding="utf-8"))


def require(condition, message):
    global checks
    checks += 1
    if not condition:
        raise AssertionError(message)


lang = load("assets/xiantu/lang/zh_cn.json")
modifier = load("data/xiantu/forge/biome_modifier/add_metal_ores.json")
require(len(modifier["features"]) == 8, "expected eight metal ore features")
for item_id, tier, size, min_y, max_y, chance, tag in MATERIALS:
    ore, raw, ingot, block = f"{item_id}_ore", f"{item_id}_raw", f"{item_id}_ingot", f"{item_id}_block"
    require(f"xiantu:{ore}" in modifier["features"], f"{ore} missing from overworld modifier")
    for path in [
        f"assets/xiantu/textures/block/ore_{item_id}.png",
        f"assets/xiantu/textures/item/ingot_{item_id}.png",
        f"assets/xiantu/textures/block/block_{item_id}.png",
    ]:
        png = (RES / path).read_bytes()
        require(png[:8] == b"\x89PNG\r\n\x1a\n", f"invalid PNG: {path}")
        width, height = struct.unpack(">II", png[16:24])
        require((width, height) == (64, 64), f"expected 64x64 texture: {path} got {width}x{height}")
    feature = load(f"data/xiantu/worldgen/configured_feature/{ore}.json")
    require(feature["config"]["size"] == size, f"wrong vein size: {ore}")
    placed = load(f"data/xiantu/worldgen/placed_feature/{ore}.json")
    height = next(entry["height"] for entry in placed["placement"] if entry["type"] == "minecraft:height_range")
    require((height["min_inclusive"]["absolute"], height["max_inclusive"]["absolute"]) == (min_y, max_y), f"wrong height: {ore}")
    require(placed["placement"][0] == {"type": "minecraft:rarity_filter", "chance": 1}, f"chance must come from config: {ore}")
    loot = load(f"data/xiantu/loot_tables/blocks/{ore}.json")
    children = loot["pools"][0]["entries"][0]["children"]
    require(children[0]["name"] == f"xiantu:{ore}" and children[1]["name"] == f"xiantu:{raw}", f"silk touch/raw drops missing: {ore}")
    namespace, tag_name = tag.split(":", 1)
    harvest = load(f"data/{namespace}/tags/blocks/{tag_name}.json")
    require(f"xiantu:{ore}" in harvest["values"], f"tool harvest tag missing: {ore}")
    require(lang.get(f"item.xiantu.{raw}") and lang.get(f"item.xiantu.{ingot}") and lang.get(f"block.xiantu.{ore}") and lang.get(f"block.xiantu.{block}"), f"translation missing: {item_id}")
    load(f"data/xiantu/recipes/{block}_from_ingots.json")
    load(f"data/xiantu/recipes/{ingot}s_from_{block}.json")
    if tier <= 2:
        load(f"data/xiantu/recipes/{ingot}_from_{raw}_smelting.json")
        load(f"data/xiantu/recipes/{ingot}_from_{raw}_blasting.json")
    else:
        require(not (RES / f"data/xiantu/recipes/{ingot}_from_{raw}_smelting.json").exists(), f"high-tier vanilla smelting bypass: {item_id}")

source = (ROOT / "src/main/java/com/longju/xiantu/mining/MetalMaterialRegistry.java").read_text(encoding="utf-8")
mining_tiers = (ROOT / "src/main/java/com/longju/xiantu/mining/MiningTiers.java").read_text(encoding="utf-8")
require('register("hanxing", 4,' in mining_tiers, "Hanxing must use Forge harvest level 4")
require('register("jiuyou", 5,' in mining_tiers, "Jiuyou must use Forge harvest level 5")
require('register("hundun", 6,' in mining_tiers, "Hundun must use Forge harvest level 6")
require("METAL_ORE_CHANCES" in (ROOT / "src/main/java/com/longju/xiantu/config/XiantuConfig.java").read_text(encoding="utf-8"), "metal ore config is not registered")
for item_id, *_ in MATERIALS:
    require(f'"{item_id}"' in source, f"registry missing {item_id}")

print(f"Metal material integration: PASS ({checks} checks)")
