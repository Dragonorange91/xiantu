import json
from pathlib import Path

from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/xiantu"
DATA = ROOT / "src/main/resources/data/xiantu"
MASTER = ROOT / "asset_sources/building_materials/master"
TEXTURES = ASSETS / "textures/block"

FAMILIES = {
    "azure_brick": {
        "texture": "azure_cloud_spirit_brick",
        "names": ("青云灵砖", "青云灵砖柱", "青云灵砖楼梯", "青云灵砖台阶", "青云灵砖墙"),
        "english": ("Azure Cloud Spirit Brick", "Azure Cloud Spirit Brick Pillar", "Azure Cloud Spirit Brick Stairs", "Azure Cloud Spirit Brick Slab", "Azure Cloud Spirit Brick Wall"),
        "strength": 2.5,
    },
    "black_rock": {
        "texture": "xuan_rock_formation_stone",
        "names": ("玄岩阵石", "玄岩阵石柱", "玄岩阵石楼梯", "玄岩阵石台阶", "玄岩阵石墙"),
        "english": ("Profound Formation Stone", "Profound Formation Stone Pillar", "Profound Formation Stone Stairs", "Profound Formation Stone Slab", "Profound Formation Stone Wall"),
        "strength": 4.0,
    },
    "cloud_jade": {
        "texture": "cloud_pattern_jade_stone",
        "names": ("云纹玉石", "云纹玉石柱", "云纹玉石楼梯", "云纹玉石台阶", "云纹玉石墙"),
        "english": ("Cloud-Pattern Jade Stone", "Cloud-Pattern Jade Pillar", "Cloud-Pattern Jade Stairs", "Cloud-Pattern Jade Slab", "Cloud-Pattern Jade Wall"),
        "strength": 3.0,
    },
    "spring_stone": {
        "texture": "courtyard_water_stone",
        "names": ("庭水石材", "庭水石柱", "庭水石楼梯", "庭水石台阶", "庭水石墙"),
        "english": ("Courtyard Water Stone", "Courtyard Water Stone Pillar", "Courtyard Water Stone Stairs", "Courtyard Water Stone Slab", "Courtyard Water Stone Wall"),
        "strength": 3.0,
    },
    "cloud_floating_stone": {
        "texture": "cloud_floating_stone",
        "names": ("云霞浮石", "云霞浮石柱", "云霞浮石楼梯", "云霞浮石台阶", "云霞浮石墙"),
        "english": ("Cloudglow Floating Stone", "Cloudglow Floating Stone Pillar", "Cloudglow Floating Stone Stairs", "Cloudglow Floating Stone Slab", "Cloudglow Floating Stone Wall"),
        "strength": 3.5,
    },
}


def write_json(path: Path, value: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def variant_blockstate(name: str) -> dict:
    variants = {}
    for facing, rotation in (("east", 0), ("south", 90), ("west", 180), ("north", 270)):
        for half, x_rotation in (("bottom", 0), ("top", 180)):
            for shape in ("straight", "inner_left", "inner_right", "outer_left", "outer_right"):
                model_suffix = "" if shape == "straight" else "_inner" if shape.startswith("inner") else "_outer"
                shape_rotation = (rotation - 90) % 360 if shape.endswith("left") else rotation
                if half == "top" and shape != "straight":
                    shape_rotation = (shape_rotation + 90) % 360
                key = f"facing={facing},half={half},shape={shape},waterlogged=false"
                model = {"model": f"xiantu:block/{name}{model_suffix}"}
                if shape_rotation:
                    model["y"] = shape_rotation
                if x_rotation:
                    model["x"] = x_rotation
                if shape_rotation or x_rotation:
                    model["uvlock"] = True
                variants[key] = model
                variants[key[:-5] + "true"] = dict(model)
    return {"variants": variants}


def slab_blockstate(name: str) -> dict:
    return {"variants": {
        "type=bottom": {"model": f"xiantu:block/{name}"},
        "type=top": {"model": f"xiantu:block/{name}_top"},
        "type=double": {"model": f"xiantu:block/{name}_double"},
    }}


def wall_blockstate(name: str) -> dict:
    multipart = [{"when": {"up": "true"}, "apply": {"model": f"xiantu:block/{name}_post"}}]
    for direction, rotation in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for height in ("low", "tall"):
            model = f"{name}_side" if height == "low" else f"{name}_side_tall"
            multipart.append({"when": {direction: height}, "apply": {"model": f"xiantu:block/{model}", "y": rotation, "uvlock": True}})
    return {"multipart": multipart}


def textured_model(parent: str, **textures: str) -> dict:
    return {"parent": parent, "textures": {key: f"xiantu:block/{value}" for key, value in textures.items()}}


def recipe(name: str, pattern: list[str], key: dict, result_count: int) -> dict:
    return {"type": "minecraft:crafting_shaped", "pattern": pattern, "key": key,
            "result": {"item": f"xiantu:{name}", "count": result_count}}


def block_loot(name: str, slab: bool = False) -> dict:
    entry = {"type": "minecraft:item", "name": f"xiantu:{name}"}
    if slab:
        entry["functions"] = [
            {"function": "minecraft:set_count", "count": 2, "add": False,
             "conditions": [{"condition": "minecraft:block_state_property", "block": f"xiantu:{name}",
                             "properties": {"type": "double"}}]},
            {"function": "minecraft:explosion_decay"},
        ]
    pool = {"rolls": 1, "entries": [entry]}
    if not slab:
        pool["conditions"] = [{"condition": "minecraft:survives_explosion"}]
    return {"type": "minecraft:block", "pools": [pool]}


def merge_tag(name: str, additions: list[str]) -> None:
    path = ROOT / f"src/main/resources/data/minecraft/tags/blocks/{name}.json"
    existing = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {"replace": False, "values": []}
    values = existing.setdefault("values", [])
    values.extend(value for value in additions if value not in values)
    write_json(path, existing)


def main() -> None:
    master_names = {entry["texture"] for entry in FAMILIES.values()} | {f"{family}_{suffix}" for family in FAMILIES for suffix in ("smooth", "carved")}
    for texture in master_names:
        source = MASTER / f"{texture}_1024.png"
        target = TEXTURES / f"{texture}.png"
        if not source.exists():
            raise FileNotFoundError(source)
        image = Image.open(source).convert("RGBA")
        if image.size != (1024, 1024):
            image = image.resize((1024, 1024), Image.Resampling.LANCZOS)
        image.resize((64, 64), Image.Resampling.LANCZOS).save(target)

    zh = json.loads((ASSETS / "lang/zh_cn.json").read_text(encoding="utf-8"))
    en = json.loads((ASSETS / "lang/en_us.json").read_text(encoding="utf-8"))
    for translations in (zh, en):
        for key in list(translations):
            if ".xuan_rock" in key:
                translations.pop(key)
    mineable = []
    for family, info in FAMILIES.items():
        base = family
        pillar, stairs, slab, wall = (f"{family}_{suffix}" for suffix in ("pillar", "stairs", "slab", "wall"))
        names = (base, pillar, stairs, slab, wall)
        mineable.extend(f"xiantu:{block}" for block in names)
        texture = info["texture"]

        for block, display, english in zip(names, info["names"], info["english"]):
            zh[f"block.xiantu.{block}"] = display
            zh[f"item.xiantu.{block}"] = display
            en[f"block.xiantu.{block}"] = english
            en[f"item.xiantu.{block}"] = english

        write_json(ASSETS / f"blockstates/{base}.json", {"variants": {"": {"model": f"xiantu:block/{base}"}}})
        write_json(ASSETS / f"models/block/{base}.json", textured_model("minecraft:block/cube_all", all=texture))
        write_json(ASSETS / f"models/item/{base}.json", {"parent": f"xiantu:block/{base}"})
        write_json(DATA / f"loot_tables/blocks/{base}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"xiantu:{base}"}]}]})

        write_json(ASSETS / f"blockstates/{pillar}.json", {"variants": {
            "axis=x": {"model": f"xiantu:block/{pillar}", "x": 90, "y": 90},
            "axis=y": {"model": f"xiantu:block/{pillar}"},
            "axis=z": {"model": f"xiantu:block/{pillar}", "x": 90},
        }})
        # Preserve the narrowed native Blockbench export; never restore the rejected full cube.
        pillar_source = MASTER.parent / 'models' / f'{pillar}.json'
        if not pillar_source.exists():
            raise RuntimeError('Run tools/build_building_columns.py through Blockbench before rebuilding pillars')
        write_json(ASSETS / f"models/block/{pillar}.json", json.loads(pillar_source.read_text(encoding='utf-8')))
        write_json(ASSETS / f"models/item/{pillar}.json", {"parent": f"xiantu:block/{pillar}"})
        write_json(DATA / f"loot_tables/blocks/{pillar}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"xiantu:{pillar}"}]}]})

        write_json(ASSETS / f"blockstates/{stairs}.json", variant_blockstate(stairs))
        stair_textures = {"bottom": texture, "top": texture, "side": texture}
        write_json(ASSETS / f"models/block/{stairs}.json", textured_model("minecraft:block/stairs", **stair_textures))
        write_json(ASSETS / f"models/block/{stairs}_inner.json", textured_model("minecraft:block/inner_stairs", **stair_textures))
        write_json(ASSETS / f"models/block/{stairs}_outer.json", textured_model("minecraft:block/outer_stairs", **stair_textures))
        write_json(ASSETS / f"models/item/{stairs}.json", {"parent": f"xiantu:block/{stairs}"})
        write_json(DATA / f"loot_tables/blocks/{stairs}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"xiantu:{stairs}"}]}]})

        write_json(ASSETS / f"blockstates/{slab}.json", slab_blockstate(slab))
        write_json(ASSETS / f"models/block/{slab}.json", textured_model("minecraft:block/slab", **{"bottom": texture, "top": texture, "side": texture}))
        write_json(ASSETS / f"models/block/{slab}_top.json", textured_model("minecraft:block/slab_top", **{"bottom": texture, "top": texture, "side": texture}))
        write_json(ASSETS / f"models/block/{slab}_double.json", {"parent": f"xiantu:block/{base}"})
        write_json(ASSETS / f"models/item/{slab}.json", {"parent": f"xiantu:block/{slab}"})
        write_json(DATA / f"loot_tables/blocks/{slab}.json", block_loot(slab, slab=True))

        write_json(ASSETS / f"blockstates/{wall}.json", wall_blockstate(wall))
        wall_textures = {"wall": texture}
        for model, parent in (("post", "minecraft:block/template_wall_post"), ("side", "minecraft:block/template_wall_side"), ("side_tall", "minecraft:block/template_wall_side_tall")):
            write_json(ASSETS / f"models/block/{wall}_{model}.json", textured_model(parent, **wall_textures))
        write_json(ASSETS / f"models/item/{wall}.json", textured_model("minecraft:block/wall_inventory", wall=texture))
        write_json(DATA / f"loot_tables/blocks/{wall}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"xiantu:{wall}"}]}]})

        if family == "azure_brick":
            write_json(DATA / f"recipes/{base}.json", recipe(base, ["SBS", "BFB", "SBS"], {
                "S": {"item": "minecraft:sand"}, "B": {"item": "minecraft:stone_bricks"}, "F": {"item": "xiantu:fan_ling_shi"}}, 4))
        elif family == "black_rock":
            write_json(DATA / f"recipes/{base}.json", recipe(base, ["DB", "BD"], {
                "D": {"item": "minecraft:cobbled_deepslate"}, "B": {"item": "minecraft:basalt"}}, 4))
        elif family == "cloud_jade":
            write_json(DATA / f"recipes/{base}.json", {"type": "minecraft:crafting_shapeless", "ingredients": [
                {"item": "minecraft:calcite"}, {"item": "minecraft:calcite"}, {"item": "minecraft:quartz"}],
                "result": {"item": f"xiantu:{base}", "count": 2}})
        elif family == "cloud_floating_stone":
            write_json(DATA / f"recipes/{base}.json", {"type": "minecraft:crafting_shapeless", "ingredients": [
                {"item": "xiantu:cloud_jade"}, {"item": "xiantu:cloud_jade"}, {"item": "minecraft:calcite"}],
                "result": {"item": f"xiantu:{base}", "count": 4}})
        else:
            write_json(DATA / f"recipes/{base}.json", {"type": "minecraft:crafting_shapeless", "ingredients": [
                {"item": "minecraft:prismarine_shard"}, {"item": "minecraft:stone_bricks"}, {"item": "minecraft:stone_bricks"}],
                "result": {"item": f"xiantu:{base}", "count": 2}})

        write_json(DATA / f"recipes/{pillar}.json", {"type": "minecraft:crafting_shapeless", "ingredients": [{"item": f"xiantu:{base}"}], "result": {"item": f"xiantu:{pillar}", "count": 1}})
        write_json(DATA / f"recipes/{stairs}.json", recipe(stairs, ["B  ", "BB ", "BBB"], {"B": {"item": f"xiantu:{base}"}}, 4))
        write_json(DATA / f"recipes/{slab}.json", recipe(slab, ["BBB"], {"B": {"item": f"xiantu:{base}"}}, 6))
        write_json(DATA / f"recipes/{wall}.json", recipe(wall, ["BBB", "BBB"], {"B": {"item": f"xiantu:{base}"}}, 6))

    for family in FAMILIES:
        for suffix in ("", "_pillar", "_stairs", "_wall"):
            name = family + suffix
            write_json(DATA / f"loot_tables/blocks/{name}.json", block_loot(name))
    for family, info in FAMILIES.items():
        for suffix, chinese, english in (("smooth", "去纹", "Smooth"), ("carved", "雕花", "Carved")):
            name = f"{family}_{suffix}"
            write_json(ASSETS / f"blockstates/{name}.json", {"variants": {"": {"model": f"xiantu:block/{name}"}}})
            write_json(ASSETS / f"models/block/{name}.json", textured_model("minecraft:block/cube_all", all=name))
            write_json(ASSETS / f"models/item/{name}.json", {"parent": f"xiantu:block/{name}"})
            write_json(DATA / f"loot_tables/blocks/{name}.json", block_loot(name))
            for lang, display in ((zh, chinese + info["names"][0]), (en, english + " " + info["english"][0])):
                lang[f"block.xiantu.{name}"] = display
                lang[f"item.xiantu.{name}"] = display
            mineable.append(f"xiantu:{name}")
        write_json(DATA / f"recipes/{family}_smooth.json", recipe(f"{family}_smooth", ["BB", "BB"], {"B": {"item": f"xiantu:{family}"}}, 4))
        write_json(DATA / f"recipes/{family}_carved.json", recipe(f"{family}_carved", ["B", "B"], {"B": {"item": f"xiantu:{family}_slab"}}, 1))
        railing = f"{family}_railing"
        multipart = [{"apply": {"model": f"xiantu:block/{railing}_post"}}]
        for direction, rotation in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
            multipart.append({"when": {direction: "true"}, "apply": {"model": f"xiantu:block/{railing}_side", "y": rotation, "uvlock": True}})
        write_json(ASSETS / f"blockstates/{railing}.json", {"multipart": multipart})
        for part in ("post", "side", "inventory"):
            write_json(ASSETS / f"models/block/{railing}_{part}.json", textured_model(f"minecraft:block/fence_{part}", texture=info["texture"]))
        write_json(ASSETS / f"models/item/{railing}.json", {"parent": f"xiantu:block/{railing}_inventory"})
        write_json(DATA / f"loot_tables/blocks/{railing}.json", block_loot(railing))
        write_json(DATA / f"recipes/{railing}.json", recipe(railing, ["BB", "BB", "BB"], {"B": {"item": f"xiantu:{family}"}}, 6))
        for lang, display in ((zh, info["names"][0] + "栏杆"), (en, info["english"][0] + " Railing")):
            lang[f"block.xiantu.{railing}"] = display
            lang[f"item.xiantu.{railing}"] = display
        mineable.append(f"xiantu:{railing}")
    ores = [f"xiantu:{name}_ore" for name in ("fan_ling_shi", "ling_jing", "ling_yu", "xuan_jing", "di_sui", "tian_jing", "xian_yu", "dao_shi")]
    merge_tag("mineable/pickaxe", mineable + ores)
    for suffix, tag in (("wall", "walls"), ("stairs", "stairs"), ("slab", "slabs")):
        merge_tag(tag, [f"xiantu:{family}_{suffix}" for family in FAMILIES])
    merge_tag("fences", [f"xiantu:{family}_railing" for family in FAMILIES])
    (ASSETS / "lang/zh_cn.json").write_text(json.dumps(zh, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    (ASSETS / "lang/en_us.json").write_text(json.dumps(en, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Generated {len(master_names)} material textures and {len(FAMILIES) * 8} complete block resource sets.")


if __name__ == "__main__":
    main()
