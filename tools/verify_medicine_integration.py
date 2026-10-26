from __future__ import annotations

import json
import hashlib
import re
from pathlib import Path

from PIL import Image
from held_item_edge_contract import verify_icon
ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "asset_sources/medicine-20261012-v2"
MANIFEST = json.loads((ASSETS / "manifest.json").read_text(encoding="utf-8"))
RESOURCES = ROOT / "src/main/resources"
ASSETS_OUT = RESOURCES / "assets/xiantu"
DATA_OUT = RESOURCES / "data/xiantu"
JAVA = ROOT / "src/main/java/com/longju/xiantu/medicine/MedicineCatalog.java"
CLIENT_JAVA = ROOT / "src/main/java/com/longju/xiantu/client/XiantuClient.java"
HERB_ITEM_REVISION = ROOT / "asset_sources/herb-item-icons-20261013-v2"


def check(ok: bool, message: str) -> None:
    if not ok:
        raise AssertionError(message)


def load(path: Path):
    return json.loads(path.read_text(encoding="utf-8"))


def main() -> None:
    herbs = [entry for entry in MANIFEST if entry["kind"] in {"plant_lifecycle", "non_plant_material", "drop_only_herb"}]
    catalog = JAVA.read_text(encoding="utf-8")
    rows = re.findall(r'new MedicineDefinition\("([^"]+)", "([^"]+)"', catalog)
    check(len(herbs) == len(rows) == 60, "catalog and manifest both contain 60 herb materials")
    checks = 1
    crop_count = 0
    wild_count = 0
    seed_count = 0

    for index, entry in enumerate(herbs, 1):
        item_id = f"herb_{index:04d}"
        check(rows[index - 1] == (item_id, entry["name"]), f"stable catalog row {item_id}")
        item_texture = ASSETS_OUT / "textures/item" / f"{item_id}.png"
        model = load(ASSETS_OUT / "models/item" / f"{item_id}.json")
        check(model["parent"] == "minecraft:item/generated", f"flat static item model for {item_id}")
        with Image.open(item_texture) as image:
            check(image.size == (64, 64), f"64x64 item icon {item_id}")
        checks += 3

        if entry["kind"] != "plant_lifecycle":
            continue
        wild_id = f"{item_id}_wild"
        wild_state = load(ASSETS_OUT / "blockstates" / f"{wild_id}.json")
        wild_model = load(ASSETS_OUT / "models/block" / f"{wild_id}.json")
        check(wild_model["parent"] == "xiantu:block/herb_wild_cross", f"native crossed wild plant model {wild_id}")
        cross=load(ASSETS_OUT / 'models/block/herb_wild_cross.json')
        check(len(cross['elements'])==2 and all(e['from'][2]==e['to'][2] and e['from'][1]==0 for e in cross['elements']), 'two grounded plant planes')
        check({e['rotation']['angle'] for e in cross['elements']}=={-45,45}, 'perpendicular crossed plant planes')
        check((ASSETS_OUT / "textures/block" / f"{wild_id}.png").is_file(), f"wild texture exists {wild_id}")
        check(bool(wild_state["variants"][""]["model"]), f"wild blockstate resolves {wild_id}")
        wild_count += 1
        checks += 3

        if entry.get("seed_registered"):
            seed_id = f"{item_id}_seed"
            crop_id = f"{item_id}_crop"
            crop_state = load(ASSETS_OUT / "blockstates" / f"{crop_id}.json")
            check(set(crop_state["variants"]) == {f"age={age}" for age in range(4)}, f"four crop age states {crop_id}")
            models=[crop_state["variants"][f"age={age}"]["model"] for age in range(4)]
            check(len(set(models))==4,f"four distinct growth models {crop_id}")
            hashes=[]
            for age in range(4):
                texture=ASSETS_OUT/"textures/block"/f"{item_id}_stage{age+1}.png"
                hashes.append(hashlib.sha256(texture.read_bytes()).hexdigest())
            check(len(set(hashes))==4,f"four distinct growth textures {crop_id}")
            seed_model = load(ASSETS_OUT / "models/item" / f"{seed_id}.json")
            check(seed_model["parent"] == "minecraft:item/generated", f"flat static seed model {seed_id}")
            with Image.open(ASSETS_OUT / "textures/item" / f"{seed_id}.png") as image:
                check(image.size == (64, 64), f"64x64 seed icon {seed_id}")
            seed_count += 1
            crop_count += 1
            checks += 4

    icon_manifest_path = HERB_ITEM_REVISION / "manifest.json"
    if icon_manifest_path.exists():
        icon_manifest = load(icon_manifest_path)
        reviewed_icons = [entry for entry in herbs if entry["kind"] == "plant_lifecycle"]
        check(len(reviewed_icons) == 54, "54 independent herb item icons have registry entries")
        for index, entry in enumerate(herbs, 1):
            if entry["kind"] != "plant_lifecycle":
                continue
            item_id = f"herb_{index:04d}"
            icon = icon_manifest[item_id]
            check(icon["status"] == "accepted_by_user",
                  f"accepted icon integration state {item_id}")
            texture = ASSETS_OUT / "textures/item" / f"{item_id}.png"
            verify_icon(texture,icon["sha256"],ROOT)
            checks += 2

    check(wild_count == 54 and crop_count == seed_count == 39, "wild/crop/seed totals match the design list")
    checks += 1

    client_source = CLIENT_JAVA.read_text(encoding="utf-8")
    check("ItemBlockRenderTypes.setRenderLayer(block.get(), RenderType.cutout())" in client_source,
          "crossed wild plants and crops use the cutout render layer")
    check('"parent": "minecraft:item/generated"' in (ASSETS_OUT / "models/item/herb_0001.json").read_text(encoding="utf-8"),
          "held herb icon uses a flat static item sprite, separate from the block model")
    checks += 2

    for tier in range(7):
        soil_model = load(ASSETS_OUT / "models/block" / f"spirit_soil_{tier}.json")
        farmland_state = load(ASSETS_OUT / "blockstates" / f"spirit_farmland_{tier}.json")
        check(soil_model["parent"] == "minecraft:block/cube_bottom_top", f"soil {tier} is a full cube with distinct top and sides")
        check(len(farmland_state["variants"]) == 8, f"farmland {tier} has 8 moisture states")
        with Image.open(ASSETS_OUT / "textures/block" / f"spirit_soil_{tier}.png") as image:
            check(image.size == (64, 64) and image.getchannel("A").getextrema() == (255, 255),
                  f"soil {tier} is opaque and 64x64")
        checks += 3

    modifiers = list((DATA_OUT / "forge/biome_modifier").glob("herb_*_wild_patch.json"))
    ordinary_modifiers = [path for path in modifiers if "_medicine_plain_" not in path.name]
    medicine_plain_modifiers = list((DATA_OUT / "forge/biome_modifier").glob("herb_*_medicine_plain_wild_patch.json"))
    check(len(ordinary_modifiers) == 54, "first through eighth tier wild plants receive ordinary worldgen")
    check(len(medicine_plain_modifiers) == 54,
          "all wild plants through eighth tier receive doubled medicine-plain features")
    for path in ordinary_modifiers:
        modifier = load(path)
        feature = modifier["features"][0].split(":", 1)[1]
        placed = load(DATA_OUT / "worldgen/placed_feature" / f"{feature}.json")
        configured_id = placed["feature"].split(":", 1)[1]
        configured = load(DATA_OUT / "worldgen/configured_feature" / f"{configured_id}.json")
        check(configured["type"] == "minecraft:random_patch", f"valid patch configuration for {feature}")
        checks += 1

    for feature in ("medicine_garden_ruin", "ruin_shrine"):
        for folder in ("worldgen/configured_feature", "worldgen/placed_feature"):
            check(not (DATA_OUT / folder / f"{feature}.json").exists(), f"removed test ruin stays absent: {feature}/{folder}")
        modifier = "xiantu_shrines" if feature == "ruin_shrine" else feature
        check(not (DATA_OUT / "forge/biome_modifier" / f"{modifier}.json").exists(), f"removed test ruin modifier: {feature}")
        checks += 3
    check(not list(DATA_OUT.glob("worldgen/placed_feature/*spirit_soil*.json")),
          "spirit soil is absent from ordinary worldgen features")
    checks += 1

    for tier in range(7):
        recipe = load(DATA_OUT / "recipes" / f"spirit_soil_{tier}.json")
        check(recipe["type"] == "xiantu:spirit_soil", f"custom spirit soil recipe {tier}")
        check(recipe["tier"] == tier, f"spirit soil recipe tier metadata {tier}")
        checks += 2
    old_ids = ("xiantu:spirit_herb", "xiantu:fire_herb", "xiantu:water_herb", "xiantu:poison_mushroom")
    for path in DATA_OUT.rglob("*.json"):
        text = path.read_text(encoding="utf-8")
        check(not any(old_id in text for old_id in old_ids), f"no obsolete herb item IDs in {path.relative_to(DATA_OUT)}")
        checks += 1

    print(f"Medicine integration assets: {checks} checks passed ({wild_count} wild plants, {crop_count} crops, {seed_count} seeds, 7 soils/farmlands)")


if __name__ == "__main__":
    main()
