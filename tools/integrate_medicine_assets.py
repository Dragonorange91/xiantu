from __future__ import annotations

import json
import re
import shutil
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
ASSET_ROOT = ROOT / "asset_sources/medicine-20261012-v2"
SOURCE = ROOT / "src/main/resources/assets/xiantu"
DATA = ROOT / "src/main/resources/data/xiantu"
MANIFEST = ASSET_ROOT / "manifest.json"
HERB_ITEM_REVISION = ROOT / "asset_sources/herb-item-icons-20261013-v2"


def read_json(path: Path):
    return json.loads(path.read_text(encoding="utf-8"))


def write_json(path: Path, value) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def copy_texture(source: str, target: Path) -> None:
    path = Path(source)
    if not path.is_absolute():
        path = ROOT / path
    if not path.is_file():
        raise FileNotFoundError(path)
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(path, target)


def add_lang_entries(path: Path, entries: dict[str, str]) -> None:
    current = read_json(path)
    current.update(entries)
    write_json(path, current)


def main() -> None:
    manifest = read_json(MANIFEST)
    herbs = [entry for entry in manifest if entry["kind"] in {"plant_lifecycle", "non_plant_material", "drop_only_herb"}]
    if len(herbs) != 60:
        raise SystemExit(f"Expected 60 registered herb materials, found {len(herbs)}")
    catalog_source = (ROOT / "src/main/java/com/longju/xiantu/medicine/MedicineCatalog.java").read_text(encoding="utf-8")
    catalog_rows = re.findall(r'new MedicineDefinition\("([^"]+)", "([^"]+)"', catalog_source)
    expected_rows = [(f"herb_{index:04d}", entry["name"]) for index, entry in enumerate(herbs, 1)]
    if catalog_rows != expected_rows:
        raise SystemExit("MedicineCatalog IDs/names do not match the approved asset manifest order")

    lang: dict[str, str] = {}
    for index, entry in enumerate(herbs, 1):
        registry_id = f"herb_{index:04d}"
        if entry["name"].startswith("耕地·") or entry["kind"] == "spirit_farmland":
            continue
        if entry["kind"] in {"plant_lifecycle", "non_plant_material", "drop_only_herb"}:
            lang[f"item.xiantu.{registry_id}"] = entry["name"]
            copy_texture(entry["runtime"][0], SOURCE / "textures/item" / f"{registry_id}.png")
        if entry["kind"] != "plant_lifecycle":
            continue

        has_seed = bool(entry.get("seed_registered"))
        wild = f"{registry_id}_wild"
        crop = f"{registry_id}_crop"
        lang[f"block.xiantu.{wild}"] = entry["name"] + "·野生植株"
        lang[f"block.xiantu.{crop}"] = entry["name"] + "·栽培株"
        copy_texture(entry["runtime"][-1], SOURCE / "textures/block" / f"{wild}.png")
        write_json(SOURCE / "blockstates" / f"{wild}.json", {"variants": {"": {"model": f"xiantu:block/{wild}"}}})
        write_json(SOURCE / "models/block" / f"{wild}.json", {
            "parent": "minecraft:block/cross",
            "textures": {"cross": f"xiantu:block/{wild}"},
        })
        if has_seed:
            seed_id = f"{registry_id}_seed"
            lang[f"item.xiantu.{seed_id}"] = entry["name"] + "种子"
            copy_texture(entry["runtime"][1], SOURCE / "textures/item" / f"{seed_id}.png")
            write_json(SOURCE / "models/item" / f"{seed_id}.json", {
                "parent": "minecraft:item/generated",
                "textures": {"layer0": f"xiantu:item/{seed_id}"},
            })
            variants = {}
            for age in range(4):
                stage = min(age + 1, 3)
                texture_id = f"{registry_id}_stage{stage}"
                copy_texture(entry["runtime"][stage + 1], SOURCE / "textures/block" / f"{texture_id}.png")
                write_json(SOURCE / "models/block" / f"{texture_id}.json", {
                    "parent": "minecraft:block/cross",
                    "textures": {"cross": f"xiantu:block/{texture_id}"},
                })
                variants[f"age={age}"] = {"model": f"xiantu:block/{texture_id}"}
            write_json(SOURCE / "blockstates" / f"{crop}.json", {"variants": variants})

    for entry in herbs:
        index = herbs.index(entry) + 1
        registry_id = f"herb_{index:04d}"
        write_json(SOURCE / "models/item" / f"{registry_id}.json", {
            "parent": "minecraft:item/generated",
            "textures": {"layer0": f"xiantu:item/{registry_id}"},
        })

    icon_manifest_path = HERB_ITEM_REVISION / "manifest.json"
    if icon_manifest_path.exists():
        icon_manifest = read_json(icon_manifest_path)
        for index, entry in enumerate(herbs, 1):
            if entry["kind"] != "plant_lifecycle":
                continue
            registry_id = f"herb_{index:04d}"
            icon = icon_manifest.get(registry_id)
            if icon and icon.get("status") in {
                    "integrated_pending_build_and_game_review", "deployed_pending_game_review", "accepted_by_user"}:
                copy_texture(str(HERB_ITEM_REVISION / "runtime" / f"{registry_id}.png"),
                             SOURCE / "textures/item" / f"{registry_id}.png")

    soil_names: dict[int, str] = {}
    farmland_names: dict[int, str] = {}
    for entry in manifest:
        if entry["kind"] == "spirit_soil":
            tier = int(entry["tier"])
            soil_names[tier] = entry["name"]
            copy_texture(entry["runtime"][0], SOURCE / "textures/block" / f"spirit_soil_{tier}.png")
            write_json(SOURCE / "models/block" / f"spirit_soil_{tier}.json", {
                "parent": "minecraft:block/cube_all",
                "textures": {"all": f"xiantu:block/spirit_soil_{tier}"},
            })
            write_json(SOURCE / "blockstates" / f"spirit_soil_{tier}.json", {
                "variants": {"": {"model": f"xiantu:block/spirit_soil_{tier}"}},
            })
            write_json(SOURCE / "models/item" / f"spirit_soil_{tier}.json", {
                "parent": f"xiantu:block/spirit_soil_{tier}",
            })
        elif entry["kind"] == "spirit_farmland":
            tier = int(entry["tier"])
            farmland_names[tier] = entry["name"]
            copy_texture(entry["runtime"][0], SOURCE / "textures/block" / f"spirit_farmland_{tier}.png")
            write_json(SOURCE / "models/block" / f"spirit_farmland_{tier}.json", {
                "parent": "minecraft:block/cube",
                "textures": {
                    "down": f"xiantu:block/spirit_soil_{tier}",
                    "up": f"xiantu:block/spirit_farmland_{tier}",
                    "north": f"xiantu:block/spirit_soil_{tier}",
                    "south": f"xiantu:block/spirit_soil_{tier}",
                    "east": f"xiantu:block/spirit_soil_{tier}",
                    "west": f"xiantu:block/spirit_soil_{tier}",
                    "particle": f"xiantu:block/spirit_soil_{tier}",
                },
            })
            write_json(SOURCE / "blockstates" / f"spirit_farmland_{tier}.json", {
                "variants": {f"moisture={moisture}": {"model": f"xiantu:block/spirit_farmland_{tier}"}
                             for moisture in range(8)},
            })
            write_json(SOURCE / "models/item" / f"spirit_farmland_{tier}.json", {
                "parent": f"xiantu:block/spirit_farmland_{tier}",
            })
            lang[f"block.xiantu.spirit_soil_{tier}"] = soil_names.get(tier, entry["name"].replace("耕地·", ""))
            lang[f"item.xiantu.spirit_soil_{tier}"] = soil_names.get(tier, entry["name"])
            lang[f"block.xiantu.spirit_farmland_{tier}"] = entry["name"]
            lang[f"item.xiantu.spirit_farmland_{tier}"] = entry["name"]

    for path in (SOURCE / "lang/zh_cn.json", SOURCE / "lang/en_us.json"):
        add_lang_entries(path, lang)
    herb_tag_ids = [f"xiantu:herb_{index:04d}" for index, entry in enumerate(herbs, 1)
                    if entry["kind"] == "plant_lifecycle" and entry["name"] != "二阶·淬毒菇"]
    write_json(DATA / "tags/items/alchemy_herbs.json", {"replace": False, "values": herb_tag_ids})
    poison_id = next(f"xiantu:herb_{index:04d}" for index, entry in enumerate(herbs, 1)
                     if entry["name"] == "二阶·淬毒菇")
    write_json(DATA / "tags/items/alchemy_poisons.json", {"replace": False, "values": [poison_id]})
    # Reapply the reviewed four-stage/root-free revision after legacy lifecycle exports.
    if (ROOT / "asset_sources/biome-herb-materials-20261020/art/integrated-plants.json").exists():
        from integrate_resource_revision_20261020 import integrate_plants
        integrate_plants()
    print(f"Integrated {len(herbs)} herb materials, {sum(bool(x.get('seed_registered')) for x in herbs)} seed icons, and {len(soil_names)} soil/farmland pairs")


if __name__ == "__main__":
    main()
