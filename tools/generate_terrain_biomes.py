from __future__ import annotations

import json
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CLIENT = (Path.home() / 'Desktop/仙途/.minecraft/libraries/net/minecraft/client/1.20.1-20230612.114412/client-1.20.1-20230612.114412-extra.jar')
OUT = ROOT / "src/main/resources/data/xiantu/worldgen/biome"
BIOMES = {
    "thunder_wastes": ("plains", 0.5, 0.25, True, (59, 59, 74, 5326943)),
    "meteor_wasteland": ("badlands", 1.8, 0.0, False, (145, 113, 82, 7602048)),
    "spirit_medicine_plain": ("plains", 0.35, 0.8, True, (139, 185, 169, 7900312)),
}


def main() -> None:
    if not CLIENT.is_file():
        raise SystemExit(f"Missing Minecraft 1.20.1 client data jar: {CLIENT}")
    OUT.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(CLIENT) as archive:
        for biome_id, (base, temperature, downfall, rain, colors) in BIOMES.items():
            raw = archive.read(f"data/minecraft/worldgen/biome/{base}.json")
            biome = json.loads(raw)
            biome["temperature"] = temperature
            biome["downfall"] = downfall
            biome["has_precipitation"] = rain
            biome["effects"]["fog_color"] = colors[0] * 65536 + colors[1] * 256 + colors[2]
            biome["effects"]["sky_color"] = colors[3]
            biome["effects"]["water_color"] = colors[0] * 65536 + colors[1] * 256 + colors[2]
            biome["effects"]["water_fog_color"] = max(0, colors[3] - 1225000)
            if biome_id == "thunder_wastes":
                biome["effects"]["fog_color"] = 0x3B3B4A
                biome["effects"]["water_color"] = 0x59626F
                biome["effects"]["water_fog_color"] = 0x373B46
            if biome_id == "spirit_medicine_plain":
                biome["effects"]["grass_color"] = 0x35582F
            (OUT / f"{biome_id}.json").write_text(
                json.dumps(biome, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    for obsolete_id in ("spirit_medicine_valley", "ghost_earth_cavern", "moonstone_grove", "qingmu_forest"):
        (OUT / f"{obsolete_id}.json").unlink(missing_ok=True)
        (ROOT / f"src/main/resources/data/xiantu/tags/worldgen/biome/terrain/{obsolete_id}.json").unlink(missing_ok=True)
    lang = ROOT / "src/main/resources/assets/xiantu/lang/zh_cn.json"
    translations = json.loads(lang.read_text(encoding="utf-8"))
    translations.update({
        "biome.xiantu.thunder_wastes": "雷地",
        "biome.xiantu.meteor_wasteland": "陨星荒原",
        "biome.xiantu.spirit_medicine_plain": "灵药原",
    })
    translations.pop("biome.xiantu.spirit_medicine_valley", None)
    translations.pop("biome.xiantu.ghost_earth_cavern", None)
    translations.pop("biome.xiantu.moonstone_grove", None)
    translations.pop("biome.xiantu.qingmu_forest", None)
    for key in ("block.xiantu.cave_strata_rock", "block.xiantu.cave_ghost_soil", "block.xiantu.ghost_lantern_fungus"):
        translations.pop(key, None)
    lang.write_text(json.dumps(translations, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    english = ROOT / "src/main/resources/assets/xiantu/lang/en_us.json"
    en_translations = json.loads(english.read_text(encoding="utf-8"))
    en_translations.update({
        "biome.xiantu.thunder_wastes": "Thunder Wastes",
        "biome.xiantu.meteor_wasteland": "Meteor Wasteland",
        "biome.xiantu.spirit_medicine_plain": "Spirit Medicine Plain",
    })
    en_translations.pop("biome.xiantu.spirit_medicine_valley", None)
    en_translations.pop("biome.xiantu.ghost_earth_cavern", None)
    en_translations.pop("biome.xiantu.moonstone_grove", None)
    en_translations.pop("biome.xiantu.qingmu_forest", None)
    for key in ("block.xiantu.cave_strata_rock", "block.xiantu.cave_ghost_soil", "block.xiantu.ghost_lantern_fungus"):
        en_translations.pop(key, None)
    english.write_text(json.dumps(en_translations, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Generated {len(BIOMES)} Xiantu biome templates from the installed 1.20.1 client data")


if __name__ == "__main__":
    main()
