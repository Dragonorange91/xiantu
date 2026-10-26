"""Generate standalone, higher-detail item sprites for the Xiantu medicinal plants."""
from __future__ import annotations
from build_paths import review_font

import argparse
import hashlib
import json
import re
import shutil
import subprocess
import sys
import time
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

from generate_medicine_assets import PLANTS, TIERS, fit_item_icon, run_api

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "asset_sources/herb-item-icons-20261013-v2"
SOURCE = ROOT / "src/main/resources/assets/xiantu"
CATALOG = ROOT / "src/main/java/com/longju/xiantu/medicine/MedicineCatalog.java"
BACKUP = ROOT / "work/asset-backups/herb-item-icons-20261013"
API = Path.home() / ".codex/skills/image-api-generator/scripts/generate_image.py"


def catalog_rows() -> list[dict]:
    source = CATALOG.read_text(encoding="utf-8")
    rows = re.findall(r'new MedicineDefinition\("(herb_\d{4})", "([^"]+)", (\d+),', source)
    plant_names = {name for _, name in PLANTS}
    return [
        {"id": item_id, "name": name, "tier": int(tier)}
        for item_id, name, tier in rows
        if name.split("·", 1)[-1] in plant_names
    ]


def prompt_for(row: dict) -> str:
    plant = row["name"].split("·", 1)[-1]
    return (
        f"Edit and redraw the provided existing Minecraft item sprite as a clean 1024x1024 pixel-art master for "
        f"{row['name']} ({plant}), a harvested medicinal ingredient in a Chinese xianxia mod. Preserve the exact "
        "existing object's identity, silhouette, pose, number of parts, proportions, dominant colors and composition. "
        "Do not turn it into a whole rooted potted plant, add roots/leaves/stems that are not in the source, or change "
        "a fruit, fungus, leaf, flower or root into another type. Reconstruct the same object with crisp Minecraft "
        "Java texture quality: many fine 1-3 logical-pixel clusters, sharply separated material tones, readable fine "
        "veins/facets/petals/fibers where present, and no broad blocky mosaic patches. Use a limited palette and "
        "stepped shading. Replace only the background with solid hot magenta #FF00FF. Fully show the complete source "
        "object with its current margins. No checkerboard, blur, soft antialiasing, painterly noise, photorealism, "
        "smooth gradients, bloom, outline, shadow, text, labels, frame, container, UI or watermark. The final result "
        "must remain recognizable and crisp after reduction to a 64x64 RGBA Minecraft item texture."
    )


def run_api_edit(prompt: str, reference: Path, dest: Path) -> tuple[int, int]:
    args = [sys.executable, str(API), "--prompt", prompt, "--input-image", str(reference),
            "--size", "1024x1024", "--quality", "high", "--response-format", "omit",
            "--timeout", "240", "--output", str(dest)]
    last_error = None
    for attempt in range(1, 4):
        try:
            subprocess.run(args, check=True)
            last_error = None
            break
        except subprocess.CalledProcessError as error:
            last_error = error
            print(f"RETRY {dest.stem} attempt={attempt}", flush=True)
            if dest.exists():
                dest.unlink()
            if attempt < 3:
                time.sleep(3 * attempt)
    if last_error is not None:
        raise last_error
    with Image.open(dest) as image:
        original_size = image.size
        image.convert("RGBA").resize((1024, 1024), Image.Resampling.LANCZOS).save(dest)
    return original_size


def render_preview(rows: list[dict]) -> None:
    font = review_font(14)
    columns, tile = 6, 160
    sheet = Image.new("RGB", (columns * tile, ((len(rows) + columns - 1) // columns) * (tile + 24)), (35, 37, 40))
    draw = ImageDraw.Draw(sheet)
    for index, row in enumerate(rows):
        x, y = (index % columns) * tile, (index // columns) * (tile + 24)
        path = OUT / "runtime" / f"{row['id']}.png"
        with Image.open(path) as image:
            preview = image.convert("RGBA").resize((tile, tile), Image.Resampling.NEAREST)
        sheet.paste(preview, (x, y), preview)
        draw.text((x + 2, y + tile + 3), f"{row['id']} {row['name']}", font=font, fill=(240, 238, 230))
    sheet.save(OUT / "review.png")


def generate(rows: list[dict]) -> None:
    masters = OUT / "masters"
    runtime = OUT / "runtime"
    masters.mkdir(parents=True, exist_ok=True)
    runtime.mkdir(parents=True, exist_ok=True)
    manifest_path = OUT / "manifest.json"
    records = json.loads(manifest_path.read_text(encoding="utf-8")) if manifest_path.exists() else {}
    for row in rows:
        item_id = row["id"]
        master = masters / f"{item_id}_1024.png"
        target = runtime / f"{item_id}.png"
        if not master.exists():
            prompt = prompt_for(row)
            (masters / f"{item_id}.prompt.txt").write_text(prompt, encoding="utf-8")
            print(f"GENERATING {item_id} {row['name']}", flush=True)
            reference = SOURCE / "textures/item" / f"{item_id}.png"
            original_size = run_api_edit(prompt, reference, master)
        else:
            with Image.open(master) as image:
                original_size = image.size
            print(f"REUSE {item_id}", flush=True)
        if not target.exists():
            with Image.open(master) as image:
                fit_item_icon(image).save(target)
        with Image.open(target) as image:
            assert image.size == (64, 64) and image.mode == "RGBA"
            assert image.getchannel("A").getextrema() == (0, 255)
            bounds = image.getchannel("A").getbbox()
            assert bounds is not None and bounds[0] > 0 and bounds[2] < 64
            assert bounds[1] > 0 and bounds[3] < 64
        records[item_id] = {
            **row,
            "master": str(master),
            "api_source_size": list(original_size),
            "runtime": str(target),
            "sha256": hashlib.sha256(target.read_bytes()).hexdigest(),
            "status": "generated_for_visual_review",
        }
        manifest_path.write_text(json.dumps(records, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    render_preview([records[row["id"]] for row in catalog_rows() if row["id"] in records])


def integrate(rows: list[dict]) -> None:
    records = json.loads((OUT / "manifest.json").read_text(encoding="utf-8"))
    if any(row["id"] not in records for row in rows):
        raise SystemExit("Generate the full requested icon set before integration")
    for row in rows:
        item_id = row["id"]
        source = OUT / "runtime" / f"{item_id}.png"
        destination = SOURCE / "textures/item" / f"{item_id}.png"
        with Image.open(source) as image:
            if image.size != (64, 64) or image.mode != "RGBA":
                raise SystemExit(f"Invalid candidate texture: {source}")
        backup = BACKUP / destination.name
        backup.parent.mkdir(parents=True, exist_ok=True)
        if destination.exists() and not backup.exists():
            shutil.copy2(destination, backup)
        shutil.copy2(source, destination)
        records[item_id]["status"] = "integrated_pending_build_and_game_review"
    (OUT / "manifest.json").write_text(json.dumps(records, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Integrated {len(rows)} herb item icons; originals backed up at {BACKUP}")


def mark_deployed(rows: list[dict]) -> None:
    manifest_path = OUT / "manifest.json"
    records = json.loads(manifest_path.read_text(encoding="utf-8"))
    if any(row["id"] not in records for row in rows):
        raise SystemExit("Cannot mark missing herb icons as deployed")
    for row in rows:
        if records[row["id"]]["status"] not in {
                "integrated_pending_build_and_game_review", "deployed_pending_game_review"}:
            raise SystemExit(f"Icon was not integrated: {row['id']}")
        records[row["id"]]["status"] = "deployed_pending_game_review"
    manifest_path.write_text(json.dumps(records, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Marked {len(rows)} herb item icons deployed; in-game review remains pending")


def mark_accepted(rows: list[dict]) -> None:
    manifest_path = OUT / "manifest.json"
    records = json.loads(manifest_path.read_text(encoding="utf-8"))
    if any(row["id"] not in records for row in rows):
        raise SystemExit("Cannot accept missing herb icons")
    for row in rows:
        if records[row["id"]]["status"] not in {
                "deployed_pending_game_review", "accepted_by_user"}:
            raise SystemExit(f"Icon was not deployed for review: {row['id']}")
        records[row["id"]]["status"] = "accepted_by_user"
    manifest_path.write_text(json.dumps(records, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Marked {len(rows)} herb item icons accepted by user")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--ids", nargs="*", help="stable item IDs such as herb_0001; omit to process all medicinal plants")
    parser.add_argument("--integrate", action="store_true", help="copy generated 64x64 sprites into Forge resources after review")
    parser.add_argument("--mark-deployed", action="store_true", help="record that the integrated textures are in the installed mod")
    parser.add_argument("--mark-accepted", action="store_true", help="record user in-game acceptance of deployed icons")
    args = parser.parse_args()
    rows = catalog_rows()
    if len(rows) != 54:
        raise SystemExit(f"Expected 54 medicinal plant item icons, found {len(rows)}")
    if args.ids:
        selected = set(args.ids)
        unknown = selected - {row["id"] for row in rows}
        if unknown:
            raise SystemExit(f"Unknown medicinal plant IDs: {sorted(unknown)}")
        rows = [row for row in rows if row["id"] in selected]
    if args.mark_accepted:
        mark_accepted(rows)
    elif args.mark_deployed:
        mark_deployed(rows)
    elif args.integrate:
        integrate(rows)
    else:
        generate(rows)
        print(f"Review image: {OUT / 'review.png'}", flush=True)


if __name__ == "__main__":
    main()
