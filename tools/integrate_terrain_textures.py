from __future__ import annotations

import json
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSET_ROOT = ROOT / "asset_sources/terrain-20261012"
RESOURCES = ROOT / "src/main/resources"
BLOCK_IDS = {
    "thunder_rock": "雷纹玄岩",
    "thunder_soil": "焦雷土",
    "lightning_scarred_log": "雷击枯木",
    "meteor_rock": "陨坑岩",
    "meteor_soil": "暗赭荒土",
    "valley_streamstone": "溪石",
    "valley_fertile_soil": "青纹沃土",
}


def write(path: Path, value: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def merge_language(path: Path, entries: dict[str, str]) -> None:
    current = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {}
    current.update(entries)
    path.write_text(json.dumps(current, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def main() -> None:
    for block_id, name in BLOCK_IDS.items():
        if block_id == "lightning_scarred_log":
            for part in ("side", "top"):
                source = ASSET_ROOT / "runtime" / f"{block_id}_{part}_64.png"
                target = RESOURCES / "assets/xiantu/textures/block" / f"{block_id}_{part}.png"
                if not source.is_file():
                    raise SystemExit(f"Missing generated terrain texture: {source}")
                target.parent.mkdir(parents=True, exist_ok=True)
                shutil.copy2(source, target)
            write(RESOURCES / "assets/xiantu/blockstates" / f"{block_id}.json", {
                "variants": {
                    "axis=y": {"model": f"xiantu:block/{block_id}"},
                    "axis=x": {"model": f"xiantu:block/{block_id}", "x": 90, "y": 90},
                    "axis=z": {"model": f"xiantu:block/{block_id}", "x": 90},
                },
            })
            write(RESOURCES / "assets/xiantu/models/block" / f"{block_id}.json", {
                "parent": "minecraft:block/cube_column",
                "textures": {"side": f"xiantu:block/{block_id}_side", "end": f"xiantu:block/{block_id}_top"},
            })
        else:
            source = ASSET_ROOT / "runtime" / f"{block_id}_64.png"
            target = RESOURCES / "assets/xiantu/textures/block" / f"{block_id}.png"
            if not source.is_file():
                raise SystemExit(f"Missing generated terrain texture: {source}")
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(source, target)

            write(RESOURCES / "assets/xiantu/blockstates" / f"{block_id}.json", {
                "variants": {"": {"model": f"xiantu:block/{block_id}"}},
            })
            write(RESOURCES / "assets/xiantu/models/block" / f"{block_id}.json", {
                "parent": "minecraft:block/cube_all",
                "textures": {"all": f"xiantu:block/{block_id}"},
            })
        write(RESOURCES / "assets/xiantu/models/item" / f"{block_id}.json", {
            "parent": f"xiantu:block/{block_id}",
        })
        write(RESOURCES / "data/xiantu/loot_tables/blocks" / f"{block_id}.json", {
            "type": "minecraft:block",
            "pools": [{
                "rolls": 1,
                "entries": [{"type": "minecraft:item", "name": f"xiantu:{block_id}"}],
                "conditions": [{"condition": "minecraft:survives_explosion"}],
            }],
        })

    merge_language(RESOURCES / "assets/xiantu/lang/zh_cn.json", {
        f"block.xiantu.{block_id}": name for block_id, name in BLOCK_IDS.items()
    })
    merge_language(RESOURCES / "assets/xiantu/lang/en_us.json", {
        f"block.xiantu.{block_id}": name for block_id, name in BLOCK_IDS.items()
    })
    for lang_name in ("zh_cn.json", "en_us.json"):
        lang_path = RESOURCES / "assets/xiantu/lang" / lang_name
        lang = json.loads(lang_path.read_text(encoding="utf-8"))
        lang.pop("block.xiantu.cold_marsh_stone", None)
        lang.pop("block.xiantu.moon_frost_soil", None)
        lang_path.write_text(json.dumps(lang, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    manifest_path = ASSET_ROOT / "manifest.json"
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    manifest["lightning_scarred_log"]["status"] = "integrated_build_verified"
    manifest_path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Integrated {len(BLOCK_IDS)} terrain block textures, models, loot tables and translations")


if __name__ == "__main__":
    main()
