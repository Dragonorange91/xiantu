"""Check herb worldgen density, placement safety, and biome references."""
from __future__ import annotations

import json
import re
from pathlib import Path
from generate_medicine_worldgen import habitat_for

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / "src/main/resources/data/xiantu"
MANIFEST = ROOT / "asset_sources/medicine-20261012-v2/manifest.json"
CHANCE_BY_TIER = {1: 12, 2: 18, 3: 26, 4: 40, 5: 52, 6: 64, 7: 68, 8: 96}


def load(path: Path):
    return json.loads(path.read_text(encoding="utf-8"))


def main() -> None:
    manifest = load(MANIFEST)
    entries = [row for row in manifest if row["kind"] == "plant_lifecycle" and int(row["tier"]) <= 8
               and row.get("wild_plant", True)]
    all_herbs = [row for row in manifest if row["kind"] in {"plant_lifecycle", "non_plant_material", "drop_only_herb"}]
    checks = 0

    def check(ok: bool, label: str) -> None:
        nonlocal checks
        if not ok:
            raise AssertionError(label)
        checks += 1

    expected_ids = set()
    catalog_text = (ROOT / "src/main/java/com/longju/xiantu/medicine/MedicineCatalog.java").read_text(encoding="utf-8")
    for row in entries:
        index = all_herbs.index(row) + 1
        registry_id = f"herb_{index:04d}"
        expected_ids.add(registry_id)
        tag_id = f"medicine/{registry_id}"
        tag = load(DATA / "tags/worldgen/biome" / f"{tag_id}.json")
        configured = load(DATA / "worldgen/configured_feature" / f"{registry_id}_wild_patch.json")
        placed = load(DATA / "worldgen/placed_feature" / f"{registry_id}_wild_patch.json")
        modifier = load(DATA / "forge/biome_modifier" / f"{registry_id}_wild_patch.json")

        check(tag["values"], f"{registry_id} has at least one habitat")
        check(modifier["biomes"] == f"#xiantu:{tag_id}", f"{registry_id} modifier uses its habitat tag")
        check(modifier["features"] == [f"xiantu:{registry_id}_wild_patch"], f"{registry_id} modifier adds one patch")
        check(placed["feature"] == f"xiantu:{registry_id}_wild_patch", f"{registry_id} placed feature resolves")
        check(placed["placement"][0] == {"type": "minecraft:rarity_filter", "chance": CHANCE_BY_TIER[int(row["tier"])]},
              f"{registry_id} uses reduced per-chunk attempt rate")
        check(configured["config"]["tries"] == 1, f"{registry_id} places at most one plant per successful patch")
        check(configured["config"]["y_spread"] == 0, f"{registry_id} has no vertical offset")
        predicates = configured["config"]["feature"]["placement"]
        check(any(p["predicate"].get("type") == "minecraft:would_survive" for p in predicates),
              f"{registry_id} checks plant support before placement")
        check(any(p["predicate"].get("type") == "minecraft:matching_blocks"
                  and p["predicate"].get("blocks") == ["minecraft:air"] for p in predicates),
              f"{registry_id} only places into air")
        plain_feature_id = f"{registry_id}_medicine_plain_wild_patch"
        has_plain_bonus = int(row["tier"]) <= 8
        if has_plain_bonus:
            plain_tag = load(DATA / "tags/worldgen/biome/medicine/spirit_medicine_plain" / f"{registry_id}.json")
            plain_placed = load(DATA / "worldgen/placed_feature" / f"{plain_feature_id}.json")
            plain_modifier = load(DATA / "forge/biome_modifier" / f"{plain_feature_id}.json")
            check(plain_tag["values"] == ["xiantu:spirit_medicine_plain"], f"{registry_id} gets a medicine-plain bonus tag")
            check(plain_placed["placement"][0] == {
                "type": "minecraft:rarity_filter", "chance": CHANCE_BY_TIER[int(row["tier"])] // 2
            }, f"{registry_id} spawns twice as often in the medicine plain")
            check(plain_modifier["biomes"] == f"#xiantu:medicine/spirit_medicine_plain/{registry_id}",
                  f"{registry_id} bonus applies only in the medicine plain")
        else:
            check(not (DATA / "worldgen/placed_feature" / f"{plain_feature_id}.json").exists(),
                  f"{registry_id} does not get an unrelated medicine-plain bonus")
        if int(row["tier"]) >= 7:
            definition = re.search(
                rf'new MedicineDefinition\("{registry_id}",\s*"[^"]+",\s*{row["tier"]},\s*(true|false),\s*(true|false),\s*(true|false)\)',
                catalog_text,
            )
            check(definition is not None and definition.groups() == ("false", "true", "false"),
                  f"{registry_id} is wild-only, non-seed, and non-cultivable")
            check("xiantu:spirit_medicine_plain" not in tag["values"],
                  f"{registry_id} keeps the plain bonus in its separate feature")

    for path in DATA.rglob("*.json"):
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except json.JSONDecodeError as error:
            raise AssertionError(f"invalid JSON: {path}: {error}") from error
        def contains_obsolete_biome(value) -> bool:
            if isinstance(value, str):
                return value == "xiantu:cold_marsh" or any(obsolete in value for obsolete in (
                    "xiantu:ghost_earth_cavern", "xiantu:spirit_medicine_valley", "xiantu:moonstone_grove"))
            if isinstance(value, list):
                return any(contains_obsolete_biome(item) for item in value)
            if isinstance(value, dict):
                return any(contains_obsolete_biome(item) for item in value.values())
            return False

        check(not contains_obsolete_biome(data),
              f"obsolete biome id removed from {path.relative_to(DATA)}")

    biome_dir = DATA / "worldgen/biome"
    check(not (biome_dir / "cold_marsh.json").exists(), "obsolete cold_marsh biome file removed")
    check(not (biome_dir / "moonstone_grove.json").exists(), "removed moonstone_grove biome file is absent")
    check((biome_dir / "spirit_medicine_plain.json").is_file(), "renamed spirit_medicine_plain biome exists")
    check(not (biome_dir / "ghost_earth_cavern.json").exists(), "ghost_earth_cavern biome is removed")
    print(f"Medicine worldgen verification: {checks} checks passed ({len(expected_ids)} herb features)")


if __name__ == "__main__":
    main()
