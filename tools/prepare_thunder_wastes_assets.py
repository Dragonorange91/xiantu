from __future__ import annotations

import json
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "asset_sources/thunder-wastes-20261012/masters"
ASSET_ROOT = ROOT / "asset_sources/terrain-20261012"


def normalize(source: Path, master: Path, runtime: Path, alpha: bool) -> None:
    mode = "RGBA"
    with Image.open(source) as image:
        image = image.convert(mode)
        if not alpha:
            image.putalpha(255)
            side = min(image.width, image.height)
            left = (image.width - side) // 2
            top = (image.height - side) // 2
            image = image.crop((left, top, left + side, top + side))
            image = image.resize((1024, 1024), Image.Resampling.LANCZOS)
        else:
            alpha_channel = image.getchannel("A").point(lambda value: 0 if value < 8 else value)
            image.putalpha(alpha_channel)
            bounds = alpha_channel.getbbox()
            if bounds is None:
                raise SystemExit(f"Generated sprite has no visible pixels: {source}")
            image = image.crop(bounds)
            scale = min(880 / image.width, 920 / image.height)
            size = (round(image.width * scale), round(image.height * scale))
            image = image.resize(size, Image.Resampling.LANCZOS)
            canvas = Image.new("RGBA", (1024, 1024), (0, 0, 0, 0))
            canvas.alpha_composite(image, ((1024 - size[0]) // 2, 1016 - size[1]))
            image = canvas
        master.parent.mkdir(parents=True, exist_ok=True)
        runtime.parent.mkdir(parents=True, exist_ok=True)
        image.save(master, optimize=True)
        image.resize((64, 64), Image.Resampling.LANCZOS).save(runtime, optimize=True)


def main() -> None:
    definitions = {
        "lightning_scarred_log_side": ("lightning_scarred_log_side.png", ASSET_ROOT / "masters/lightning_scarred_log_side_1024.png", ASSET_ROOT / "runtime/lightning_scarred_log_side_64.png", False),
        "lightning_scarred_log_top": ("lightning_scarred_log_top.png", ASSET_ROOT / "masters/lightning_scarred_log_top_1024.png", ASSET_ROOT / "runtime/lightning_scarred_log_top_64.png", False),
        "storm_bamboo": ("storm_bamboo.png", ASSET_ROOT / "masters/plants/storm_bamboo_1024.png", ASSET_ROOT / "runtime/plants/storm_bamboo_64.png", True),
        "thunder_sedge": ("thunder_sedge.png", ASSET_ROOT / "masters/plants/thunder_sedge_1024.png", ASSET_ROOT / "runtime/plants/thunder_sedge_64.png", True),
    }
    for _, (filename, master, runtime, alpha) in definitions.items():
        source = SOURCE / filename
        if not source.is_file():
            raise SystemExit(f"Missing generated image candidate: {source}")
        normalize(source, master, runtime, alpha)

    manifest_path = ASSET_ROOT / "manifest.json"
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    manifest["lightning_scarred_log"] = {
        "name": "雷击枯木",
        "side_master": str(ASSET_ROOT / "masters/lightning_scarred_log_side_1024.png"),
        "top_master": str(ASSET_ROOT / "masters/lightning_scarred_log_top_1024.png"),
        "side_runtime": str(ASSET_ROOT / "runtime/lightning_scarred_log_side_64.png"),
        "top_runtime": str(ASSET_ROOT / "runtime/lightning_scarred_log_top_64.png"),
        "status": "prepared_not_runtime_verified",
    }
    manifest_path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    plant_manifest_path = ASSET_ROOT / "plants_manifest.json"
    plants = json.loads(plant_manifest_path.read_text(encoding="utf-8"))
    for key, name in (("storm_bamboo", "雷竹"), ("thunder_sedge", "电弧草")):
        plants[key] = {
            "name": name,
            "master": str(ASSET_ROOT / f"masters/plants/{key}_1024.png"),
            "runtime": str(ASSET_ROOT / f"runtime/plants/{key}_64.png"),
            "status": "prepared_not_runtime_verified",
        }
    plant_manifest_path.write_text(json.dumps(plants, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print("Prepared two log textures and two cutout plant sprites at 1024px/64px")


if __name__ == "__main__":
    main()
