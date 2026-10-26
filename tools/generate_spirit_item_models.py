import json
from pathlib import Path

from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
ASSET_ROOT = ROOT / "src/main/resources/assets/xiantu"
SPIRIT_STONES = [
    "fan_ling_shi", "ling_jing", "ling_yu", "xuan_jing",
    "di_sui", "tian_jing", "xian_yu", "dao_shi",
]
PICKAXES = ["hanxing_pickaxe", "jiuyou_pickaxe", "hundun_pickaxe"]
ALPHA_THRESHOLD = 64


def visible_cell(image, x, y):
    alpha = image.getchannel("A").crop((x * 4, y * 4, x * 4 + 4, y * 4 + 4))
    return sum(value * count for value, count in enumerate(alpha.histogram())) >= ALPHA_THRESHOLD * 16


def model_for(asset_id, is_pickaxe):
    texture_path = ASSET_ROOT / f"textures/item/{asset_id}.png"
    image = Image.open(texture_path).convert("RGBA")
    if image.size != (64, 64):
        raise ValueError(f"{texture_path} must be 64x64, got {image.size}")

    depth = 2.5 if is_pickaxe else 4.0
    z0 = round(8.0 - depth / 2.0, 3)
    z1 = round(8.0 + depth / 2.0, 3)
    elements = []

    for y in range(16):
        for x in range(16):
            if not visible_cell(image, x, y):
                continue
            depth = 2.5 if is_pickaxe else 4.0
            if not is_pickaxe:
                bounds = image.getchannel("A").getbbox()
                min_x, min_y, max_x, max_y = (value // 4 for value in bounds)
                center_x = (min_x + max_x) / 2
                center_y = (min_y + max_y) / 2
                radius_x = max((max_x - min_x) / 2, 1)
                radius_y = max((max_y - min_y) / 2, 1)
                edge = max(abs((x + 0.5 - center_x) / radius_x), abs((y + 0.5 - center_y) / radius_y))
                depth = round(1.5 + 4.5 * max(0, 1 - edge) ** 1.4, 3)
            z0 = round(8.0 - depth / 2.0, 3)
            z1 = round(8.0 + depth / 2.0, 3)
            elements.append({
                "from": [x, 15 - y, z0],
                "to": [x + 1, 16 - y, z1],
                "faces": {
                    "north": {"uv": [x, y, x + 1, y + 1], "texture": "#layer0", "shade": False},
                    "south": {"uv": [x, y, x + 1, y + 1], "texture": "#layer0", "shade": False},
                    "west": {"uv": [x, y, x + 1, y + 1], "texture": "#layer0"},
                    "east": {"uv": [x, y, x + 1, y + 1], "texture": "#layer0"},
                    "up": {"uv": [x, y, x + 1, y + 1], "texture": "#layer0"},
                    "down": {"uv": [x, y, x + 1, y + 1], "texture": "#layer0"},
                },
            })

    return {
        "parent": "minecraft:item/handheld" if is_pickaxe else "minecraft:item/generated",
        "textures": {"layer0": f"xiantu:item/{asset_id}"},
        "elements": elements,
    }


def main():
    for asset_id in SPIRIT_STONES + PICKAXES:
        output = ASSET_ROOT / f"models/item/{asset_id}.json"
        if output.exists() and json.loads(output.read_text(encoding="utf-8")).get("loader") in ("gfbs_gltf:gltf", "forge:separate_transforms"):
            print(f"{asset_id}: preserving imported GLB model")
            continue
        model = model_for(asset_id, asset_id in PICKAXES)
        output.write_text(json.dumps(model, indent=2) + "\n", encoding="utf-8")
        print(f"{asset_id}: {len(model['elements'])} textured voxels")


if __name__ == "__main__":
    main()
