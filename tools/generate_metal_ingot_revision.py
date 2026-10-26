from __future__ import annotations

import json
import subprocess
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "asset_sources/mineral-material-textures-20261013/ingot-shape-revision-v3"
GENERATOR = Path.home() / ".codex/skills/image-api-generator/scripts/generate_image.py"
REFERENCE = (Path.home() / 'AppData/Local/Temp/codex-clipboard-971f7b9c-06a9-44fb-bad5-9a47485a8dbd.png')
METALS = [
    ("hanxing_iron", "寒星铁", "冷灰银色陨铁，细小冰白星点和沉静金属拉丝"),
    ("chisui_copper", "赤髓铜", "赤红铜髓，暗铜底色带自然赤色层纹与少量青绿矿物包裹体"),
    ("zixiao_copper", "紫霄铜", "深紫铜质，内部有紫霄色层状金属流纹和少量冷紫晶质"),
    ("jiuyou_iron", "九幽寒铁", "近黑的幽蓝寒铁，内部有克制的冰蓝断层与冷冽细纹"),
    ("taiyi_gold", "太乙精金", "温润明亮的太乙金色精金，蜂蜜金与浅金不规则金属层理"),
    ("zifu_silver", "紫府仙银", "月白银色仙银，夹少量淡紫银丝和柔和冷光金属纹"),
    ("hunyuan_stone", "混沌元石", "深炭黑混沌重金属，错位断层中仅有少量金紫异质纹理"),
    ("taichu_shenjin", "太初神金", "温润象牙白与淡金色太初神金，细密日轮层纹和极少量先天晶脉"),
]


def prompt(name: str, material: str) -> str:
    return (
        f"Redesign only the material and color of the supplied vanilla Minecraft iron ingot icon for {name}. "
        "Preserve the reference's exact compact forged-ingot silhouette and diagonal pose: a slim flat metal lozenge, "
        "about 1.6 times as wide as it is tall, with two short beveled ends and a thin visible side edge. "
        "The ingot must look like the vanilla iron ingot, not a cuboid, brick, billet, chunky bar, or weapon. "
        f"Material palette and fine pixel-cluster grain: {material}. Keep the contour simple and recognizable; material detail stays inside the existing shape. "
        "One object only, centered with generous transparent padding on all sides. True transparent background; no checkerboard, shadow, ground, text, UI, or border. "
        "Minecraft Java inventory pixel art, crisp stepped clusters, limited palette, clean hard edges, readable at 64x64. "
        "1024x1024 high-resolution master, no blur, painterly gradients, photorealism, engraving, ornament, or extra geometry."
    )


def load_font(size: int):
    for path in (r"C:\Windows\Fonts\msyh.ttc", r"C:\Windows\Fonts\simhei.ttf"):
        if Path(path).exists():
            return ImageFont.truetype(path, size)
    return ImageFont.load_default()


def create_master(asset_id: str, name: str, material: str) -> Path:
    masters = OUTPUT / "masters"
    originals = OUTPUT / "api-originals"
    masters.mkdir(parents=True, exist_ok=True)
    originals.mkdir(parents=True, exist_ok=True)
    target = masters / f"ingot_{asset_id}_1024.png"
    if target.exists():
        return target
    raw = originals / target.name
    command = [
        sys.executable, str(GENERATOR), "--prompt", prompt(name, material),
        "--input-image", str(REFERENCE), "--size", "1024x1024", "--quality", "high",
        "--response-format", "omit", "--output", str(raw),
    ]
    for attempt in range(1, 4):
        result = subprocess.run(command, capture_output=True, text=True, encoding="utf-8", errors="replace")
        if result.returncode == 0 and raw.exists():
            break
        print(f"Attempt {attempt}/3 failed for {asset_id}: {result.stderr[-700:]}", flush=True)
    else:
        raise RuntimeError(f"Image API failed for {asset_id}")

    with Image.open(raw) as opened:
        source = opened.convert("RGBA")
        if source.width != source.height:
            raise ValueError(f"Non-square API response for {asset_id}: {source.size}")
        dimensions = list(source.size)
        if source.size != (1024, 1024):
            source = source.resize((1024, 1024), Image.Resampling.LANCZOS)
        source.save(target, optimize=True)

    info = OUTPUT / "api_source_dimensions.json"
    data = json.loads(info.read_text(encoding="utf-8")) if info.exists() else {}
    data[target.name] = dimensions
    info.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return target


def make_icon(master: Path, output: Path) -> Image.Image:
    with Image.open(master) as opened:
        image = opened.convert("RGBA")
    alpha = image.getchannel("A")
    bounds = alpha.getbbox()
    if not bounds:
        raise ValueError(f"No transparent silhouette in {master.name}")
    subject = image.crop(bounds)
    subject.thumbnail((52, 36), Image.Resampling.LANCZOS)
    canvas = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    canvas.alpha_composite(subject, ((64 - subject.width) // 2, (64 - subject.height) // 2))
    canvas.save(output, optimize=True)
    return canvas


def make_contact_sheet(items: list[tuple[str, Image.Image]]) -> None:
    margin, header, cell_w, cell_h, columns = 24, 56, 250, 260, 4
    rows = (len(items) + columns - 1) // columns
    sheet = Image.new("RGB", (margin * 2 + columns * cell_w, margin * 2 + header + rows * cell_h), (28, 30, 35))
    draw = ImageDraw.Draw(sheet)
    draw.rounded_rectangle((margin, margin, sheet.width - margin, margin + header - 8), radius=4, fill=(74, 101, 119))
    draw.text((margin + 14, margin + 11), "炼器金属锭物品图标 · 8种 · 64×64", fill="white", font=load_font(22))
    for i, (name, icon) in enumerate(items):
        col, row = i % columns, i // columns
        x, y = margin + col * cell_w, margin + header + row * cell_h
        draw.rounded_rectangle((x + 4, y + 4, x + cell_w - 4, y + cell_h - 8), radius=3, fill=(39, 42, 49))
        preview = icon.resize((192, 192), Image.Resampling.NEAREST)
        checker = Image.new("RGB", preview.size, (54, 56, 62))
        sheet.paste(checker, (x + 29, y + 8))
        sheet.paste(preview, (x + 29, y + 8), preview.getchannel("A"))
        bbox = draw.textbbox((0, 0), name, font=load_font(18))
        draw.text((x + (cell_w - (bbox[2] - bbox[0])) // 2, y + 211), name, fill=(238, 238, 238), font=load_font(18))
    sheet.save(OUTPUT / "ingot_contact_sheet.png", optimize=True)


def main() -> int:
    if not REFERENCE.exists():
        raise FileNotFoundError(REFERENCE)
    if not GENERATOR.exists():
        raise FileNotFoundError(GENERATOR)
    runtime = OUTPUT / "runtime"
    runtime.mkdir(parents=True, exist_ok=True)
    icons = []
    for index, (asset_id, name, material) in enumerate(METALS, 1):
        master = create_master(asset_id, name, material)
        icon = make_icon(master, runtime / f"ingot_{asset_id}.png")
        icons.append((name, icon))
        print(f"[{index}/{len(METALS)}] ready: ingot_{asset_id}.png", flush=True)
    make_contact_sheet(icons)
    print(f"Contact sheet: {OUTPUT / 'ingot_contact_sheet.png'}", flush=True)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
