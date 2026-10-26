from __future__ import annotations

import json
import subprocess
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[1]
ASSET_ROOT = ROOT / "asset_sources/mineral-material-textures-20261013/style-revision-v2"
ORE_ROOTS = {
    "metal": ROOT / "asset_sources/material-textures-20261013/runtime",
    "special": ROOT / "asset_sources/special-ore-textures-20261013/runtime",
}
GENERATOR = Path.home() / ".codex/skills/image-api-generator/scripts/generate_image.py"
INGOT_REFERENCE = (Path.home() / 'AppData/Local/Temp/codex-clipboard-971f7b9c-06a9-44fb-bad5-9a47485a8dbd.png')
BLOCK_REFERENCE = (Path.home() / 'AppData/Local/Temp/codex-clipboard-fff326bf-aa85-4fb4-bb7c-0a22e1a3a398.png')

METALS = [
    ("hanxing_iron", "寒星铁", "冷灰银色陨铁，细小冰白星点和沉静金属拉丝"),
    ("chisui_copper", "赤髓铜", "赤红铜髓，暗铜底色带自然赤色层纹与少量青绿矿物包裹体"),
    ("zixiao_copper", "紫霄铜", "深紫铜质，内部有紫霄色层状金属流纹和少量冷紫晶质"),
    ("jiuyou_iron", "九幽寒铁", "近黑的幽蓝寒铁，内部有克制的冰蓝断层与冷冽细纹"),
    ("taiyi_gold", "太乙精金", "温润明亮的太乙金色精金，蜂蜜金与浅金不规则金属层理"),
    ("zifu_silver", "紫府仙银", "月白银色仙银，夹少量淡紫银丝和柔和冷光金属纹"),
    ("hunyuan_stone", "混沌元石", "深炭黑混沌重金属，错位断层中仅有少量金紫异质纹理"),
    ("taichu_shenjin", "太初神金", "温润象牙白与淡金色太初神金，细密日轮层纹和极少量先天晶脉，庄严纯净而非纯金色"),
]

STYLE = (
    "Minecraft Java mod inventory sprite, authentic stepped pixel-art clusters, crisp hard edges, "
    "restrained palette, readable at 64x64, high-detail 1024x1024 master, one centered object, "
    "no text, no letters, no border, no watermark, no extra objects."
)


def load_font(size: int):
    for path in (r"C:\Windows\Fonts\msyh.ttc", r"C:\Windows\Fonts\simhei.ttf"):
        if Path(path).exists():
            return ImageFont.truetype(path, size)
    return ImageFont.load_default()


def task_list():
    tasks = []
    for asset_id, name, material in METALS:
        tasks.append((f"ingot_{asset_id}", f"{name}锭", "ingot", INGOT_REFERENCE, material))
        tasks.append((f"block_{asset_id}", f"{name}块", "block", BLOCK_REFERENCE, material))
    ore_reference = ROOT / "asset_sources/material-textures-20261013/masters/ore_hunyuan_stone_1024.png"
    tasks.append(("ore_taichu_shenjin", "八阶 太初神金矿", "ore", ore_reference, "象牙白太初神金晶脉嵌在整塊淡金与烟灰色原生矿岩中，细密日轮层理，少数断面露出晶体"))
    return tasks


def make_prompt(name: str, kind: str, material: str) -> str:
    if kind == "ore":
        return (
            f"Redesign the supplied highest-tier ore reference into the eighth-tier Minecraft Java ore block texture for {name}. "
            f"Geology and material: {material}. Use a completely distinct host-rock structure: pale ash and warm ivory mineral strata, "
            "irregular compact golden-white crystal seams embedded through the rock, small angular breaks, fine pixel-scale grain. "
            "The raw gold-white mineral must be unmistakable but must not cover most of the face; avoid the old black-purple mixed-yuan look. "
            "One opaque seamless square block-face texture, flat face-on view, no perspective, no cube render, no vignette, "
            "no border, no symbols, no text, no broad glow. All four edges tile cleanly. Minecraft Java block pixel art, "
            "deliberate 64x64-readable pixel clusters and high-detail 1024x1024 master."
        )
    if kind == "block":
        return (
            f"Create a single seamless opaque square face texture for the {name} storage block. Use the supplied vanilla iron-block image as the strict material-language reference: "
            "smooth forged metal faces, clear squared edges, restrained highlights, and organized horizontal bands or shallow parallel ridges. "
            f"Apply the unique material identity {material} through palette and a subtle restrained grain only. "
            "This is a manufactured metal storage block, not an ore: absolutely no host rock, mineral veins, crystals, scattered ore chunks, or rough geology. "
            "Flat orthographic texture, no perspective, no cube render, no cast shadow, no empty margin, no border or vignette. "
            "Opaque seamless Minecraft Java pixel-art block face, crisp stepped clusters, legible at 64x64, modest surface detail, not photorealistic."
        )
    if kind == "ingot":
        return (
            f"Create one Minecraft Java inventory sprite for {name}. Use the supplied vanilla Minecraft iron ingot image as a strict shape and pose reference. "
            "Keep the same compact, squat, short trapezoid silhouette, slight diagonal upward-right tilt, small beveled ends, and simple flat ingot construction. "
            "Do not make a long billet, rectangular brick, thick cuboid, engraved bar, or ornate fantasy weapon. "
            f"Recolor and texture it as {material}; give it clear metal highlights and a few deliberate material-specific pixel clusters, keeping the silhouette close to vanilla. "
            "Single centered object, true transparent background, no shadow beyond the silhouette, no checkerboard, no UI, no text, no border. "
            "Crisp hard-edged stepped Minecraft item pixels that remain sharp at 64x64; avoid painterly gradients, blur, and photorealism. " + STYLE
        )
    else:
        shape = "one small distinctive natural material drop, compact irregular mineral chunk or sand pile as appropriate to the material, three-quarter view, tilted diagonally upward to the right"
    return (
        f"Create a single Minecraft Java inventory item sprite for {name}. Show {shape}. "
        f"Material identity and palette: {material}. "
        "Keep the entire silhouette inside the canvas with generous transparent margin. True transparent background, not a checkerboard, "
        "no ground plane, no cast shadow beyond the object, no UI slot, no border, no text or extra objects. "
        "Use crisp hard-edged pixel-art clusters and stepped shading, with enough structure to read cleanly at 64x64; "
        "not painterly, not photorealistic, not smooth-gradient, not a blurry 3D render. " + STYLE
    )


def create_master(asset_id: str, name: str, kind: str, reference: Path, material: str) -> Path:
    masters = ASSET_ROOT / "masters"
    raw_dir = ASSET_ROOT / "api-originals"
    masters.mkdir(parents=True, exist_ok=True)
    raw_dir.mkdir(parents=True, exist_ok=True)
    target = masters / f"{asset_id}_1024.png"
    if target.exists():
        return target
    raw_target = raw_dir / target.name
    command = [
        sys.executable,
        str(GENERATOR),
        "--prompt",
        make_prompt(name, kind, material),
        "--input-image",
        str(reference),
        "--size",
        "1024x1024",
        "--quality",
        "high",
        "--response-format",
        "omit",
        "--output",
        str(raw_target),
    ]
    for attempt in range(1, 4):
        result = subprocess.run(command, capture_output=True, text=True, encoding="utf-8", errors="replace")
        if result.returncode == 0 and raw_target.exists():
            break
        print(f"Attempt {attempt}/3 failed for {asset_id}: {result.stderr[-700:]}", flush=True)
    else:
        raise RuntimeError(f"Image API failed for {asset_id}")

    with Image.open(raw_target) as opened:
        original = opened.convert("RGBA")
        dimensions = list(original.size)
        if original.width != original.height:
            raise ValueError(f"Non-square API response for {asset_id}: {original.size}")
        normalized = original.resize((1024, 1024), Image.Resampling.LANCZOS) if original.size != (1024, 1024) else original.copy()
        normalized.save(target, optimize=True)

    dim_path = ASSET_ROOT / "api_source_dimensions.json"
    dimensions_map = json.loads(dim_path.read_text(encoding="utf-8")) if dim_path.exists() else {}
    dimensions_map[target.name] = dimensions
    dim_path.write_text(json.dumps(dimensions_map, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return target


def build_previews(tasks):
    runtime = ASSET_ROOT / "runtime"
    runtime.mkdir(parents=True, exist_ok=True)
    groups = {"ingot": [], "block": [], "ore": []}
    for asset_id, name, kind, _, _ in tasks:
        master = ASSET_ROOT / "masters" / f"{asset_id}_1024.png"
        with Image.open(master) as opened:
            reduced = opened.convert("RGBA").resize((64, 64), Image.Resampling.LANCZOS)
        output = runtime / f"{asset_id}.png"
        reduced.save(output, optimize=True)
        group = "ingot" if kind == "ingot" else ("block" if kind == "block" else "ore")
        groups[group].append((name, reduced.copy(), master))

    labels = load_font(18)
    group_names = {
        "ingot": "炼器金属锭物品图标 · 8种",
        "block": "炼器金属储存块方块贴图 · 8种",
        "ore": "八阶太初神金矿方块贴图",
    }
    for group, items in groups.items():
        columns, cell_w, cell_h, margin, header_h = 4, 250, 260, 24, 56
        rows = (len(items) + columns - 1) // columns
        sheet = Image.new("RGB", (margin * 2 + columns * cell_w, margin * 2 + header_h + rows * cell_h), (28, 30, 35))
        draw = ImageDraw.Draw(sheet)
        draw.rounded_rectangle((margin, margin, sheet.width - margin, margin + header_h - 8), radius=4, fill=(74, 101, 119))
        draw.text((margin + 14, margin + 11), group_names[group], fill=(255, 255, 255), font=load_font(22))
        for index, (name, texture, _) in enumerate(items):
            col, row = index % columns, index // columns
            x = margin + col * cell_w
            y = margin + header_h + row * cell_h
            draw.rounded_rectangle((x + 4, y + 4, x + cell_w - 4, y + cell_h - 8), radius=3, fill=(39, 42, 49))
            preview = texture.resize((192, 192), Image.Resampling.NEAREST)
            if preview.mode == "RGBA":
                checker = Image.new("RGB", preview.size, (54, 56, 62))
                sheet.paste(checker, (x + 29, y + 8))
                sheet.paste(preview, (x + 29, y + 8), preview.getchannel("A"))
            else:
                sheet.paste(preview.convert("RGB"), (x + 29, y + 8))
            bbox = draw.textbbox((0, 0), name, font=labels)
            draw.text((x + (cell_w - (bbox[2] - bbox[0])) // 2, y + 211), name, fill=(238, 238, 238), font=labels)
        sheet.save(ASSET_ROOT / f"{group}_contact_sheet.png", optimize=True)


def main() -> int:
    tasks = task_list()
    for index, (asset_id, name, kind, reference, material) in enumerate(tasks, 1):
        if not reference.exists():
            raise FileNotFoundError(reference)
        path = create_master(asset_id, name, kind, reference, material)
        print(f"[{index}/{len(tasks)}] ready: {path.name}", flush=True)
    build_previews(tasks)
    print(f"Contact sheets: {ASSET_ROOT}", flush=True)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
