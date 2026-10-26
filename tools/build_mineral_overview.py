from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[1]
METAL_DIR = ROOT / "asset_sources/material-textures-20261013/runtime"
SPECIAL_DIR = ROOT / "asset_sources/special-ore-textures-20261013/runtime"
SPIRIT_ORE_DIR = ROOT / "src/main/resources/assets/xiantu/textures/block"
OUTPUT = ROOT / "asset_sources/mineral-textures-contact-20261013.png"

GROUPS = [
    (
        "灵石矿 · 8种（游戏现用贴图）",
        SPIRIT_ORE_DIR,
        [
            ("ore_fan_ling_shi.png", "一阶 凡灵石矿"),
            ("ore_di_sui.png", "二阶 地髓矿"),
            ("ore_dao_shi.png", "三阶 道石矿"),
            ("ore_ling_jing.png", "四阶 灵晶矿"),
            ("ore_ling_yu.png", "五阶 灵玉矿"),
            ("ore_xuan_jing.png", "六阶 玄晶矿"),
            ("ore_tian_jing.png", "七阶 天晶矿"),
            ("ore_xian_yu.png", "八阶 仙玉矿"),
        ],
        (92, 119, 102),
    ),
    (
        "炼器金属矿 · 7种",
        METAL_DIR,
        [
            ("ore_hanxing_iron.png", "一阶 寒星铁"),
            ("ore_chisui_copper.png", "二阶 赤髓铜"),
            ("ore_zixiao_copper.png", "三阶 紫霄铜"),
            ("ore_jiuyou_iron.png", "四阶 九幽寒铁"),
            ("ore_taiyi_gold.png", "五阶 太乙精金"),
            ("ore_zifu_silver.png", "六阶 紫府仙银"),
            ("ore_hunyuan_stone.png", "七阶 混沌元石"),
        ],
        (79, 120, 142),
    ),
    (
        "特殊矿物与陨星岩 · 8种",
        SPECIAL_DIR,
        [
            ("ore_zhusha.png", "朱砂矿"),
            ("ore_xingchen_sand.png", "星辰砂矿"),
            ("ore_lunhui_crystal.png", "轮回晶矿"),
            ("ore_xingyun_sand.png", "星陨矿"),
            ("ore_leijie_stone.png", "雷劫石矿"),
            ("ore_yehuo_stone.png", "业火石矿"),
            ("ore_yinguo_stone.png", "因果石矿点"),
            ("meteorite_stone.png", "陨星岩"),
        ],
        (142, 104, 76),
    ),
]


def load_font(size: int) -> ImageFont.FreeTypeFont | ImageFont.ImageFont:
    for font_path in (r"C:\Windows\Fonts\msyh.ttc", r"C:\Windows\Fonts\simhei.ttf"):
        if Path(font_path).exists():
            return ImageFont.truetype(font_path, size)
    return ImageFont.load_default()


def main() -> int:
    columns = 4
    cell_w, cell_h = 260, 250
    margin = 28
    header_h = 58
    gap = 12
    group_heights = [header_h + ((len(items) + columns - 1) // columns) * cell_h + gap for _, _, items, _ in GROUPS]
    width = margin * 2 + columns * cell_w
    height = margin * 2 + sum(group_heights) + gap * (len(GROUPS) - 1)
    sheet = Image.new("RGB", (width, height), (27, 29, 34))
    draw = ImageDraw.Draw(sheet)
    title_font = load_font(24)
    label_font = load_font(18)
    tile_size = 192
    y = margin

    for title, folder, items, accent in GROUPS:
        draw.rounded_rectangle((margin, y, width - margin, y + header_h - 8), radius=4, fill=accent)
        draw.text((margin + 18, y + 12), title, fill=(255, 255, 255), font=title_font)
        y += header_h
        for index, (filename, label) in enumerate(items):
            path = folder / filename
            if not path.exists():
                raise FileNotFoundError(path)
            with Image.open(path) as opened:
                texture = opened.convert("RGB")
                if texture.size != (64, 64):
                    raise ValueError(f"Expected 64x64 texture, found {texture.size}: {path}")

            col, row = index % columns, index // columns
            x0 = margin + col * cell_w
            y0 = y + row * cell_h
            draw.rounded_rectangle((x0 + 4, y0 + 4, x0 + cell_w - 4, y0 + cell_h - 8), radius=3, fill=(39, 42, 49))
            enlarged = texture.resize((tile_size, tile_size), Image.Resampling.NEAREST)
            x = x0 + (cell_w - tile_size) // 2
            sheet.paste(enlarged, (x, y0 + 14))
            bbox = draw.textbbox((0, 0), label, font=label_font)
            label_w = bbox[2] - bbox[0]
            draw.text((x0 + (cell_w - label_w) // 2, y0 + 213), label, fill=(238, 238, 238), font=label_font)

        y += ((len(items) + columns - 1) // columns) * cell_h + gap

    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(OUTPUT, optimize=True)
    print(OUTPUT)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
