"""Generate Xiantu herb, seed, growth-stage and spirit-soil artwork.

The batch is intentionally serial and resumable. Each plant gets one API
master containing six panels in a 3x2 grid, cropped into the runtime item,
seed and three growth stages. Ninth-tier design-only materials are excluded.
"""
from __future__ import annotations

from pathlib import Path
import argparse
import hashlib
import json
import subprocess
import sys
import time
from collections import deque

from PIL import Image, ImageEnhance, ImageOps

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "asset_sources" / "medicine-20261012-v2"
MASTERS = OUT / "masters"
RUNTIME = OUT / "runtime"
API = Path.home() / ".codex" / "skills" / "image-api-generator" / "scripts" / "generate_image.py"

TIERS = {
    1: "一阶", 2: "二阶", 3: "三阶", 4: "四阶",
    5: "五阶", 6: "六阶", 7: "七阶", 8: "八阶",
}

# Plant entries are the active 1-8 tier design. Tier 7/8 plants have no
# registered seed or sustainable farmland block, but retain wild growth art.
PLANTS = [
    (1, "青萝"), (1, "露凝草"), (1, "火棘果"), (1, "石心苔"),
    (1, "风铃花"), (1, "清心叶"), (1, "凝血花"),
    (2, "赤芝"), (2, "冰心莲"), (2, "破妄花"), (2, "铁骨藤"),
    (2, "疾风叶"), (2, "养魂木"), (2, "淬毒菇"),
    (3, "紫参"), (3, "紫电花"), (3, "赤炎果"), (3, "厚土根"),
    (3, "青木心"),
    (4, "玄莲"), (4, "洗髓果"), (4, "通灵草"), (4, "破障花"),
    (4, "窥天藤"), (4, "凝魂芝"), (4, "化血参"),
    (5, "金髓花"), (5, "造化藤"), (5, "天道花"), (5, "太虚草"),
    (5, "混元果"), (5, "无极花"), (5, "阴阳芝"),
    (6, "天罡草"), (6, "日精花"), (6, "虚空藤"), (6, "混沌叶"),
    (6, "鸿蒙果"), (6, "月华草"), (6, "月魄莲"),
    (7, "仙灵果"), (7, "仙灵草"), (7, "道韵果"), (7, "天机花"),
    (7, "轮回芝"), (7, "长生藤"), (7, "不朽果"),
    (8, "道韵花"), (8, "天道果"), (8, "雷劫花"), (8, "心魔莲"),
    (8, "业火果"), (8, "因果花"), (8, "轮回果"),
]

NON_PLANTS = [
    (3, "玄冰髓", "ice-veined mineral shard"),
    (3, "金精砂", "heavy golden metallic mineral sand"),
    (6, "星辰砂", "weighty star-fall silvery sand grains"),
    (6, "月华露", "pale moonlight alchemical dew in a small droplet"),
    (6, "沧鳞蟒心血", "a sealed crimson alchemical blood vial with a subtle scale pattern"),
]

SOILS = ["凡灵土", "一阶灵土", "二阶灵土", "三阶灵土", "四阶灵土", "五阶灵土", "六阶灵土"]

SOIL_DESIGNS = [
    "ordinary chestnut-brown loam, coarse crumbly clods, sparse pale root fibers, no mineral seams",
    "warm umber root-rich earth, small fibrous root threads and fine granular crumbs, visibly loose texture",
    "cool gray-brown compact silt, thin broken sediment layers and tiny mica grains, visibly stratified",
    "deep red-brown volcanic earth, porous dark lapilli and irregular pits, rough broken clods",
    "olive-brown clay loam, dense curved root mat and smooth compressed patches, no crystal shapes",
    "dark ochre humus, layered compost-like earth and scattered amber mineral grains, broad horizontal bands",
    "near-black ancient compact subsoil, sparse pale blue mineral nodules embedded in heavy angular plates",
]

FARMLAND_DESIGNS = [
    "wide uneven furrows cut into chunky chestnut loam, a few tiny pebbles between ridges",
    "short neat furrows over warm umber earth, thin root fibers crossing some ridge edges",
    "diagonal shallow furrows over compact gray-brown silt, broken fine sediment bands",
    "deep irregular furrows between red-brown porous volcanic clods, rough but plantable surface",
    "narrow winding furrows through olive-brown clay, dense root threads follow the rows",
    "broad layered ridges in dark ochre humus, a few amber grains embedded in the furrow walls",
    "heavy raised furrows in near-black ancient earth, angular plates and sparse pale blue nodules",
]

ITEM_ONLY = [
    (2, "赤心果", "a small heart-shaped spiritual birch fruit with a deep vermilion skin, a pale cream cut stem, and one subtle natural heartwood-colored stripe; complete single fruit, not a cluster"),
]

def safe_id(text: str) -> str:
    return text.replace("·", "_").replace(" ", "_")

def item_prompt(tier: int, name: str) -> str:
    return f"""Create one clean 1024x1024 square Minecraft Java item texture for the Chinese xianxia alchemy material {TIERS[tier]}·{name}. The subject is {name}, a distinct harvested medicinal ingredient with a clear readable silhouette and a material design that matches its name and cultivation lore. Use a SOLID FLAT HOT MAGENTA background #FF00FF only for later chroma-key removal: do not draw a checkerboard, grid, shadow, frame or other background. No text, no watermark, no container, no UI. Minecraft-compatible pixel art: hard stepped clusters, restrained palette, three-tone diffuse shading, crisp edges, no painterly noise, no photorealism, no soft gradients, no bloom. Do not imitate a generic recolored herb; give this ingredient its own shape, vein, crystal, fruit, fungus, root or dew identity. Keep generous magenta margins and make it readable when reduced to 64x64."""

def stage_prompt(tier: int, name: str, seed_allowed: bool) -> str:
    seed_note = "The seed panel is a real usable seed icon." if seed_allowed else "The second panel is only a dormant wild-bud reference; do not imply a usable seed item."
    return f"""Create a 1024x1024 square sprite sheet for the xianxia medicinal plant {TIERS[tier]}·{name}, for a Minecraft Java mod. Divide it into exactly six equal square panels in a 3 columns by 2 rows grid, with clean empty separation. Fill every unused margin and separator with one SOLID FLAT HOT MAGENTA color #FF00FF for later chroma-key removal; do not draw a checkerboard, grid, labels, letters, watermark or shadow. Panels in reading order: 1 harvested material item, 2 seed or dormant bud, 3 tiny planted seedling, 4 young growing plant, 5 mature harvestable plant, 6 full mature wild specimen. {seed_note} Every panel must show the same recognizable species, but with real growth changes. Design the plant from its name and lore, not as a recolored generic crop. Use crisp Minecraft-compatible pixel art, hard stepped clusters, restrained palette, simple blocky diffuse shading, no painterly texture, no photorealism, no soft gradients, no bloom. Each panel must remain readable after cropping to 64x64."""

def soil_prompt(name: str, index: int) -> str:
    design = SOIL_DESIGNS[index]
    return f"""Create a 1024x1024 seamless top-face block texture for Minecraft Java 1.20.1: {name}, cultivation soil tier {index}. The entire square is opaque soil surface, with no block side, no object, no border, no text, no symbols and no glow. Material identity and structure: {design}. This must differ from every other soil tier through its actual grain size, clod geometry, sediment or pore structure, not by hue change alone. Keep it believable as plantable earth. Pixel-art texture language, crisp stepped clusters, restrained palette, diffuse block lighting, no gradients, no photorealism. Make all four tile edges seamless; design for reduction from 1024x1024 to 64x64."""

def farmland_prompt(name: str, index: int) -> str:
    design = FARMLAND_DESIGNS[index]
    return f"""Create a 1024x1024 seamless top-face texture for a tilled cultivation farmland block in Minecraft Java 1.20.1: {name} farmland, tier {index}. Depict only the top surface of actual workable soil with clearly visible planting furrows and raised ridges; the geometry is a vanilla-style farmland block and this texture supplies its top face. Material design: {design}. Preserve a distinct structure from the other six farmland tiers; do not rely on color alone. Opaque full square, no block side, no seeds, no sprouts, no crop, no water pool, no border, no text, no symbols, no glow. Pixel-art texture with crisp stepped clusters and restrained diffuse shading. Make all edges seamless and readable after reduction from 1024x1024 to 64x64."""

def run_api(prompt: str, dest: Path) -> tuple[int, int]:
    dest.parent.mkdir(parents=True, exist_ok=True)
    args = [sys.executable, str(API), "--prompt", prompt, "--size", "1024x1024",
            "--quality", "high", "--response-format", "omit", "--timeout", "240",
            "--output", str(dest)]
    last_error = None
    for attempt in range(1, 4):
        try:
            subprocess.run(args, check=True)
            last_error = None
            break
        except subprocess.CalledProcessError as exc:
            last_error = exc
            print(f"RETRY {dest.stem} attempt={attempt}", flush=True)
            if dest.exists():
                dest.unlink()
            if attempt < 3:
                time.sleep(3 * attempt)
    if last_error is not None:
        raise last_error
    with Image.open(dest) as im:
        original = im.size
        normalized = im.convert("RGBA").resize((1024, 1024), Image.Resampling.LANCZOS)
        normalized.save(dest)
    return original

def crop_panel(sheet: Image.Image, col: int, row: int, anchor_bottom: bool = False) -> Image.Image:
    # The six-panel sheet is 3 columns by 2 rows, so each panel is tall.
    # Crop the full cell, then fit only its visible subject into the item icon.
    left = round(col * sheet.width / 3)
    right = round((col + 1) * sheet.width / 3)
    top = round(row * sheet.height / 2)
    bottom = round((row + 1) * sheet.height / 2)
    panel = sheet.crop((left, top, right, bottom)).convert("RGBA")
    alpha = panel.getchannel("A")
    bbox = alpha.getbbox()
    if bbox:
        panel = panel.crop(bbox)

    target_size = 54
    scale = min(target_size / panel.width, target_size / panel.height)
    size = (max(1, round(panel.width * scale)), max(1, round(panel.height * scale)))
    subject = panel.resize(size, Image.Resampling.LANCZOS)
    canvas = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    x = (64 - subject.width) // 2
    y = 64 - subject.height if anchor_bottom else (64 - subject.height) // 2
    canvas.alpha_composite(subject, (x, y))
    return canvas

def fit_item_icon(image: Image.Image) -> Image.Image:
    image = remove_edge_background(image.convert("RGBA"))
    bbox = image.getchannel("A").getbbox()
    canvas = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    if not bbox:
        return canvas
    subject = image.crop(bbox)
    scale = min(54 / subject.width, 54 / subject.height)
    size = (max(1, round(subject.width * scale)), max(1, round(subject.height * scale)))
    subject = subject.resize(size, Image.Resampling.LANCZOS)
    canvas.alpha_composite(subject, ((64 - size[0]) // 2, (64 - size[1]) // 2))
    return canvas

def clean_runtime(image: Image.Image, opaque: bool = False) -> Image.Image:
    image = image.convert("RGBA")
    if opaque:
        image.putalpha(255)
    else:
        # Preserve generated alpha; only sharpen after reduction.
        image = ImageEnhance.Sharpness(image).enhance(1.25)
    return image

def remove_edge_background(image: Image.Image) -> Image.Image:
    """Remove a connected magenta/checker-like background without color-keying the subject."""
    im = image.convert("RGBA")
    px = im.load(); w, h = im.size
    visited = bytearray(w * h)
    q = deque()
    for x in range(w):
        q.extend(((x, 0), (x, h - 1)))
    for y in range(h):
        q.extend(((0, y), (w - 1, y)))
    def bg(c: tuple[int, int, int, int]) -> bool:
        r, g, b, a = c
        if a == 0: return True
        if r > 210 and b > 170 and g < 110 and r - g > 100: return True
        # Common baked checker colors used by image models; only flood from edges.
        return r > 215 and g > 215 and b > 215 or (abs(r-g) < 8 and abs(g-b) < 8 and 175 < r < 235)
    while q:
        x, y = q.popleft(); i = y * w + x
        if x < 0 or y < 0 or x >= w or y >= h or visited[i]: continue
        visited[i] = 1
        if not bg(px[x, y]): continue
        px[x, y] = (0, 0, 0, 0)
        q.extend(((x-1,y),(x+1,y),(x,y-1),(x,y+1)))
    return im

def write_record(record: dict) -> None:
    path = OUT / "manifest.json"
    rows = []
    if path.exists():
        rows = json.loads(path.read_text(encoding="utf-8"))
    existing = {r["id"]: r for r in rows}
    existing[record["id"]] = record
    path.write_text(json.dumps(sorted(existing.values(), key=lambda r: r["id"]), ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

def generate_plants(selected: set[str] | None) -> None:
    for tier, name in PLANTS:
        ident = f"herb_{safe_id(name)}"
        if selected and ident not in selected:
            continue
        master = MASTERS / f"{ident}_lifecycle.png"
        if not master.exists():
            seed_allowed = tier <= 6 and name != "月魄莲"
            prompt = stage_prompt(tier, name, seed_allowed)
            (MASTERS / f"{ident}_lifecycle.prompt.txt").write_text(prompt, encoding="utf-8")
            print(f"GENERATING {ident}", flush=True)
            original = run_api(prompt, master)
        else:
            with Image.open(master) as im:
                original = im.size
            print(f"REUSE {ident}", flush=True)
        with Image.open(master) as sheet:
            sheet = remove_edge_background(sheet)
            panel_names = ["item", "seed", "stage1", "stage2", "stage3", "wild_mature"]
            outputs = []
            for idx, panel_name in enumerate(panel_names):
                target = RUNTIME / ident / f"{panel_name}.png"
                if not target.exists() or panel_name in {"stage1", "stage2", "stage3", "wild_mature"}:
                    target.parent.mkdir(parents=True, exist_ok=True)
                    crop_panel(sheet, idx % 3, idx // 3, anchor_bottom=idx >= 2).save(target)
                outputs.append(str(target))
        write_record({"id": ident, "kind": "plant_lifecycle", "tier": tier,
                      "name": f"{TIERS[tier]}·{name}", "seed_registered": tier <= 6 and name != "月魄莲",
                      "master": str(master), "master_original_size": list(original),
                      "runtime": outputs, "state": "generated_pending_visual_inspection"})

def generate_nonplants(selected: set[str] | None) -> None:
    for tier, name, identity in NON_PLANTS:
        ident = f"herb_{safe_id(name)}"
        if selected and ident not in selected:
            continue
        master = MASTERS / f"{ident}.png"
        if not master.exists():
            prompt = item_prompt(tier, name) + f" Material identity: {identity}. Render only the harvested item, not a plant."
            (MASTERS / f"{ident}.prompt.txt").write_text(prompt, encoding="utf-8")
            print(f"GENERATING {ident}", flush=True)
            original = run_api(prompt, master)
        else:
            with Image.open(master) as im:
                original = im.size
            print(f"REUSE {ident}", flush=True)
        target = RUNTIME / ident / "item.png"
        if not target.exists():
            target.parent.mkdir(parents=True, exist_ok=True)
            with Image.open(master) as im:
                clean_runtime(remove_edge_background(im).resize((64, 64), Image.Resampling.LANCZOS)).save(target)
        write_record({"id": ident, "kind": "non_plant_material", "tier": tier,
                      "name": f"{TIERS[tier]}·{name}", "seed_registered": False,
                      "master": str(master), "master_original_size": list(original),
                      "runtime": [str(target)], "state": "generated_pending_visual_inspection"})

def generate_plant_items(selected: set[str] | None) -> None:
    """Regenerate complete harvested ingredient icons independently of lifecycle sheets."""
    item_masters = MASTERS / "items"
    item_masters.mkdir(parents=True, exist_ok=True)
    for tier, name in PLANTS:
        ident = f"herb_{safe_id(name)}"
        if selected and ident not in selected:
            continue
        master = item_masters / f"{ident}.png"
        if not master.exists():
            prompt = item_prompt(tier, name) + " The harvested ingredient must be a COMPLETE, self-contained object, fully visible from edge to edge, never a sliced half, cropped plant, or partial bundle. Center it with at least 18 percent empty magenta margin on every side."
            (item_masters / f"{ident}.prompt.txt").write_text(prompt, encoding="utf-8")
            print(f"GENERATING ITEM {ident}", flush=True)
            original = run_api(prompt, master)
        else:
            with Image.open(master) as im:
                original = im.size
            print(f"REUSE ITEM {ident}", flush=True)
        target = RUNTIME / ident / "item.png"
        with Image.open(master) as im:
            clean_runtime(remove_edge_background(im).resize((64, 64), Image.Resampling.LANCZOS)).save(target)
        records = json.loads((OUT / "manifest.json").read_text(encoding="utf-8"))
        for row in records:
            if row["id"] == ident:
                row["item_master"] = str(master)
                row["item_master_original_size"] = list(original)
                row["runtime"][0] = str(target)
        (OUT / "manifest.json").write_text(json.dumps(records, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

def repair_plant_item_crops(selected: set[str] | None) -> None:
    """Rebuild all plant item icons from complete lifecycle-sheet panels."""
    manifest = OUT / "manifest.json"
    records = json.loads(manifest.read_text(encoding="utf-8"))
    by_id = {row["id"]: row for row in records}
    for tier, name in PLANTS:
        ident = f"herb_{safe_id(name)}"
        if selected and ident not in selected:
            continue
        master = MASTERS / f"{ident}_lifecycle.png"
        target = RUNTIME / ident / "item.png"
        with Image.open(master) as sheet:
            sheet = remove_edge_background(sheet)
            icon = crop_panel(sheet, 0, 0)
            clean_runtime(icon).save(target)
        record = by_id[ident]
        record["runtime"][0] = str(target)
        record["item_source"] = "complete_first_lifecycle_panel"
        record["state"] = "crop_repaired_pending_visual_inspection"
        print(f"REPAIRED {ident}", flush=True)
    manifest.write_text(json.dumps(records, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

def generate_soils(selected: set[str] | None) -> None:
    for index, name in enumerate(SOILS):
        ident = f"soil_{index}_{safe_id(name)}"
        if selected and ident not in selected:
            continue
        master = MASTERS / f"{ident}.png"
        if not master.exists():
            prompt = soil_prompt(name, index)
            (MASTERS / f"{ident}.prompt.txt").write_text(prompt, encoding="utf-8")
            print(f"GENERATING {ident}", flush=True)
            original = run_api(prompt, master)
        else:
            with Image.open(master) as im:
                original = im.size
            print(f"REUSE {ident}", flush=True)
        target = RUNTIME / "soils" / f"{ident}.png"
        if not target.exists():
            target.parent.mkdir(parents=True, exist_ok=True)
            with Image.open(master) as im:
                clean_runtime(im.resize((64, 64), Image.Resampling.LANCZOS), opaque=True).save(target)
        write_record({"id": ident, "kind": "spirit_soil", "tier": index,
                      "name": name, "master": str(master), "master_original_size": list(original),
                      "runtime": [str(target)], "state": "generated_pending_visual_inspection"})

def generate_soil_revision(selected: set[str] | None) -> None:
    masters = MASTERS / "soil_revision"
    soil_runtime = RUNTIME / "soil_revision"
    farmland_runtime = RUNTIME / "farmland"
    masters.mkdir(parents=True, exist_ok=True)
    soil_runtime.mkdir(parents=True, exist_ok=True)
    farmland_runtime.mkdir(parents=True, exist_ok=True)
    rows = json.loads((OUT / "manifest.json").read_text(encoding="utf-8"))
    records = {row["id"]: row for row in rows}
    for index, name in enumerate(SOILS):
        soil_id = f"soil_{index}_{safe_id(name)}"
        farmland_id = f"farmland_{index}_{safe_id(name)}"
        if selected and soil_id not in selected and farmland_id not in selected:
            continue
        soil_master = masters / f"{soil_id}.png"
        if not soil_master.exists():
            prompt = soil_prompt(name, index)
            (masters / f"{soil_id}.prompt.txt").write_text(prompt, encoding="utf-8")
            print(f"GENERATING SOIL {soil_id}", flush=True)
            original = run_api(prompt, soil_master)
        else:
            with Image.open(soil_master) as image: original = image.size
            print(f"REUSE SOIL {soil_id}", flush=True)
        soil_path = soil_runtime / f"{soil_id}.png"
        soil_path.parent.mkdir(parents=True, exist_ok=True)
        with Image.open(soil_master) as image:
            clean_runtime(image.resize((64, 64), Image.Resampling.LANCZOS), opaque=True).save(soil_path)
        records[soil_id].update({"master": str(soil_master), "master_original_size": list(original),
                                 "runtime": [str(soil_path)], "state": "redesigned_pending_visual_inspection"})

        farmland_master = masters / f"{farmland_id}.png"
        if not farmland_master.exists():
            prompt = farmland_prompt(name, index)
            (masters / f"{farmland_id}.prompt.txt").write_text(prompt, encoding="utf-8")
            print(f"GENERATING FARMLAND {farmland_id}", flush=True)
            original = run_api(prompt, farmland_master)
        else:
            with Image.open(farmland_master) as image: original = image.size
            print(f"REUSE FARMLAND {farmland_id}", flush=True)
        farmland_path = farmland_runtime / f"{farmland_id}.png"
        farmland_path.parent.mkdir(parents=True, exist_ok=True)
        with Image.open(farmland_master) as image:
            clean_runtime(image.resize((64, 64), Image.Resampling.LANCZOS), opaque=True).save(farmland_path)
        records[farmland_id] = {"id": farmland_id, "kind": "spirit_farmland", "tier": index,
                                "name": f"耕地·{name}", "master": str(farmland_master),
                                "master_original_size": list(original), "runtime": [str(farmland_path)],
                                "state": "generated_pending_visual_inspection"}
        (OUT / "manifest.json").write_text(json.dumps(sorted(records.values(), key=lambda row: row["id"]), ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    (OUT / "manifest.json").write_text(json.dumps(sorted(records.values(), key=lambda row: row["id"]), ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

def generate_item_only(selected: set[str] | None) -> None:
    for tier, name, identity in ITEM_ONLY:
        ident = f"herb_{safe_id(name)}"
        if selected and ident not in selected:
            continue
        master = MASTERS / f"{ident}.png"
        if not master.exists():
            prompt = item_prompt(tier, name) + f" Identity: {identity}."
            (MASTERS / f"{ident}.prompt.txt").write_text(prompt, encoding="utf-8")
            print(f"GENERATING ITEM {ident}", flush=True)
            original = run_api(prompt, master)
        else:
            with Image.open(master) as image: original = image.size
            print(f"REUSE ITEM {ident}", flush=True)
        target = RUNTIME / ident / "item.png"
        target.parent.mkdir(parents=True, exist_ok=True)
        with Image.open(master) as image:
            fit_item_icon(image).save(target)
        write_record({"id": ident, "kind": "drop_only_herb", "tier": tier,
                      "name": f"{TIERS[tier]}·{name}", "seed_registered": False,
                      "master": str(master), "master_original_size": list(original),
                      "runtime": [str(target)], "state": "generated_pending_visual_inspection"})

def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--ids", nargs="*", help="optional IDs for a resumable subset")
    parser.add_argument("--plants-only", action="store_true")
    parser.add_argument("--nonplants-only", action="store_true")
    parser.add_argument("--soil-only", action="store_true")
    parser.add_argument("--plant-items-only", action="store_true")
    parser.add_argument("--repair-plant-item-crops", action="store_true")
    parser.add_argument("--soil-revision-only", action="store_true")
    parser.add_argument("--item-only", action="store_true")
    args = parser.parse_args()
    OUT.mkdir(parents=True, exist_ok=True)
    MASTERS.mkdir(parents=True, exist_ok=True)
    RUNTIME.mkdir(parents=True, exist_ok=True)
    selected = set(args.ids or [])
    if args.repair_plant_item_crops:
        repair_plant_item_crops(selected)
        print("DONE", flush=True)
        return
    if args.soil_revision_only:
        generate_soil_revision(selected)
        print("DONE", flush=True)
        return
    if args.item_only:
        generate_item_only(selected)
        print("DONE", flush=True)
        return
    if args.plant_items_only:
        generate_plant_items(selected)
        print("DONE", flush=True)
        return
    if not args.nonplants_only and not args.soil_only:
        generate_plants(selected)
    if not args.plants_only and not args.soil_only:
        generate_nonplants(selected)
    if not args.plants_only and not args.nonplants_only:
        generate_soils(selected)
    print("DONE", flush=True)

if __name__ == "__main__":
    main()
