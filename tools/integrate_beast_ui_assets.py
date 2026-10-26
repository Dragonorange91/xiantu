import json
import shutil
import struct
from pathlib import Path


PROJECT = Path(__file__).resolve().parents[1]
WORKSPACE = (Path.home() / 'Documents/Codex/2026-10-05/f')
ASSETS = PROJECT / "src/main/resources/assets/xiantu"
CORE_SOURCE = WORKSPACE / "outputs/images/妖丹材质_圆润通透版_六属性九阶_20261016/64游戏候选"
REALM_SOURCE = WORKSPACE / "outputs/images/修为大境界图标_修仙风_20261016/64游戏候选"
ELEMENTS = ("metal", "wood", "water", "fire", "earth", "thunder")
REALMS = {
    "练气": "qi",
    "筑基": "foundation",
    "金丹": "golden_core",
    "元婴": "nascent_soul",
    "化神": "spirit_transformation",
    "炼虚": "void_refinement",
    "合体": "body_integration",
    "大乘": "great_vehicle",
    "渡劫": "tribulation",
}


def png_size(path: Path) -> tuple[int, int]:
    with path.open("rb") as stream:
        header = stream.read(24)
    if header[:8] != b"\x89PNG\r\n\x1a\n":
        raise ValueError(f"Not a PNG: {path}")
    return struct.unpack(">II", header[16:24])


def copy_64px(source: Path, destination: Path) -> None:
    size = png_size(source)
    if size != (64, 64):
        raise ValueError(f"Expected 64x64 texture, got {size}: {source}")
    destination.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(source, destination)


def main() -> None:
    texture_root = ASSETS / "textures"
    model_root = ASSETS / "models/item/demon_cores"
    model_root.mkdir(parents=True, exist_ok=True)
    for element in ELEMENTS:
        for grade in range(1, 10):
            name = f"{element}_core_t{grade}"
            copy_64px(CORE_SOURCE / f"{name}.png", texture_root / f"item/demon_cores/{name}.png")
            model = {
                "parent": "minecraft:item/generated",
                "textures": {"layer0": f"xiantu:item/demon_cores/{name}"},
            }
            (model_root / f"{name}.json").write_text(json.dumps(model, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    overrides = []
    for element_index, element in enumerate(ELEMENTS):
        for grade in range(1, 10):
            name = f"{element}_core_t{grade}"
            overrides.append({
                "predicate": {"xiantu:core_model": element_index * 9 + grade},
                "model": f"xiantu:item/demon_cores/{name}",
            })
    item_model = {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": "xiantu:item/demon_core"},
        "overrides": overrides,
    }
    (ASSETS / "models/item/demon_core.json").write_text(json.dumps(item_model, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    realm_root = texture_root / "gui/realm"
    for realm_cn, realm_id in REALMS.items():
        copy_64px(REALM_SOURCE / f"{realm_cn}.png", realm_root / f"{realm_id}.png")

    print(f"Integrated {len(ELEMENTS) * 9} demon core icons/models and {len(REALMS)} realm HUD icons.")


if __name__ == "__main__":
    main()
