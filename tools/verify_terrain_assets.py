from __future__ import annotations

import json
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
RESOURCES = ROOT / "src/main/resources"
ASSETS = ROOT / "asset_sources/terrain-20261012"
SPECIAL_ASSETS = ROOT / "asset_sources/special-ore-textures-20261013"
METEORITE_ASSETS = ROOT / "asset_sources/meteorite-stone-v2-20261013"
BLOCKS = {
    "thunder_rock", "thunder_soil", "lightning_scarred_log", "meteor_rock", "meteor_soil",
    "star_dust_sand", "impact_glass_rock", "meteor_glow_fissure", "valley_streamstone", "valley_fertile_soil",
}
SPECIAL_BLOCKS = {
    "meteorite_stone": "meteorite_stone",
    "ore_xingchen_sand": "ore_xingchen_sand",
    "ore_xingyun_sand": "ore_xingyun_sand",
}
PLANTS = {
    "thunder_sedge": "thunder_wastes",
    "storm_bamboo": "thunder_wastes",
    "meteor_ash_grass": "meteor_wasteland",
    "valley_jade_fern": "spirit_medicine_plain",
}
CHECKS = 0


def check(ok: bool, label: str) -> None:
    global CHECKS
    if not ok:
        raise AssertionError(label)
    CHECKS += 1


def load(path: Path):
    return json.loads(path.read_text(encoding="utf-8"))


def main() -> None:
    creative_tabs = (ROOT / "src/main/java/com/longju/xiantu/registry/ModCreativeTabs.java").read_text(encoding="utf-8")
    cushion_recipe = load(RESOURCES / "data/xiantu/recipes/meditation_cushion.json")
    check("output.accept(ModBlockItems.MEDITATION_CUSHION.get())" in creative_tabs,
          "meditation cushion is searchable in the Xiantu creative tab")
    check(cushion_recipe["result"]["item"] == "xiantu:meditation_cushion"
          and cushion_recipe["pattern"] == ["WWW", " S "]
          and cushion_recipe["key"]["W"]["item"] == "minecraft:white_wool"
          and cushion_recipe["key"]["S"]["item"] == "minecraft:string",
          "meditation cushion has a valid wool-and-string crafting recipe")
    manifest = load(ASSETS / "manifest.json")
    active_manifest = {key for key, record in manifest.items() if record.get("status") != "retired_unregistered"}
    check(active_manifest == BLOCKS, "all active terrain materials have generation records")
    for block in sorted(BLOCKS):
        parts = ("side", "top") if block == "lightning_scarred_log" else (None,)
        for part in parts:
            suffix = f"_{part}" if part else ""
            master = ASSETS / "masters" / f"{block}{suffix}_1024.png"
            runtime = RESOURCES / "assets/xiantu/textures/block" / f"{block}{suffix}.png"
            check(master.is_file() and runtime.is_file(), f"terrain source and integrated texture exist: {block}{suffix}")
            with Image.open(master) as image:
                check(image.size == (1024, 1024), f"1024 master: {block}{suffix}")
            with Image.open(runtime) as image:
                check(image.size == (64, 64) and image.getchannel("A").getextrema() == (255, 255),
                      f"opaque 64 texture: {block}{suffix}")
        expected_model = "minecraft:block/cube_column" if block == "lightning_scarred_log" else "minecraft:block/cube_all"
        check(load(RESOURCES / f"assets/xiantu/models/block/{block}.json")["parent"] == expected_model,
              f"terrain block model: {block}")
        check(load(RESOURCES / f"data/xiantu/loot_tables/blocks/{block}.json")["type"] == "minecraft:block",
              f"survival block loot exists: {block}")
        if block in {"impact_glass_rock", "meteor_glow_fissure"}:
            check(load(RESOURCES / f"assets/xiantu/models/item/{block}.json")["parent"]
                  == f"xiantu:block/{block}",
                  f"full-cube meteor block item uses its 3D block model: {block}")

    for block, texture_id in SPECIAL_BLOCKS.items():
        asset_root = METEORITE_ASSETS if block == "meteorite_stone" else SPECIAL_ASSETS
        master = asset_root / "masters" / f"{texture_id}_1024.png"
        runtime = RESOURCES / "assets/xiantu/textures/block" / f"{block}.png"
        check(master.is_file() and runtime.is_file(), f"special meteor source and integrated texture exist: {block}")
        with Image.open(master) as image:
            check(image.size == (1024, 1024), f"1024 master: {block}")
        with Image.open(runtime) as image:
            check(image.size == (64, 64) and image.getchannel("A").getextrema() == (255, 255),
                  f"opaque 64 texture: {block}")
        if block == "meteorite_stone":
            candidate = METEORITE_ASSETS / "runtime" / "meteorite_stone.png"
            check(candidate.is_file(), "approved meteorite 64px candidate exists")
            with Image.open(candidate) as image:
                check(image.size == (64, 64) and image.mode == "RGBA"
                      and image.getchannel("A").getextrema() == (255, 255),
                      "meteorite runtime candidate is opaque 64px RGBA")
                with Image.open(runtime) as integrated:
                    check(image.convert("RGB").tobytes() == integrated.convert("RGB").tobytes(),
                          "integrated meteorite texture matches the approved candidate")
        variants = load(RESOURCES / f"assets/xiantu/blockstates/{block}.json")["variants"]
        expected_variants = {"": f"xiantu:block/{block}"} if block == "meteorite_stone" else {
            "natural_node=false": f"xiantu:block/{block}",
            "natural_node=true": f"xiantu:block/{block}",
        }
        check({state: entry["model"] for state, entry in variants.items()} == expected_variants,
              f"special meteor blockstate: {block}")
        check(load(RESOURCES / f"assets/xiantu/models/block/{block}.json")["parent"] == "minecraft:block/cube_all",
              f"special meteor block model: {block}")
        check(load(RESOURCES / f"assets/xiantu/models/item/{block}.json")["parent"] == f"xiantu:block/{block}",
              f"special meteor item keeps a full-cube model: {block}")
        check(load(RESOURCES / f"data/xiantu/loot_tables/blocks/{block}.json")["type"] == "minecraft:block",
              f"special meteor survival loot exists: {block}")

    special_registry = (ROOT / "src/main/java/com/longju/xiantu/terrain/TerrainBlockRegistry.java").read_text(encoding="utf-8")
    crater_java = (ROOT / "src/main/java/com/longju/xiantu/worldgen/MeteorCraterFeature.java").read_text(encoding="utf-8")
    hanxing_tag = load(RESOURCES / "data/xiantu/tags/blocks/needs_hanxing_tool.json")["values"]
    jiuyou_tag = load(RESOURCES / "data/xiantu/tags/blocks/needs_jiuyou_tool.json")["values"]
    check('register("meteorite_stone", false)' in special_registry
          and 'registerFallingOre("ore_xingchen_sand"' in special_registry
          and 'registerNaturalOre("ore_xingyun_sand"' in special_registry,
          "special meteor blocks are registered in the terrain registry")
    check("xiantu:ore_xingchen_sand" in hanxing_tag and "xiantu:ore_xingyun_sand" in jiuyou_tag,
          "special meteor ores use the Hanxing and Jiuyou harvest tiers")
    star_dust_loot = load(RESOURCES / "data/xiantu/loot_tables/blocks/ore_xingchen_sand.json")
    star_ore_loot = load(RESOURCES / "data/xiantu/loot_tables/blocks/ore_xingyun_sand.json")
    check(star_dust_loot["pools"][0]["entries"][0]["name"] == "xiantu:herb_0020"
          and star_dust_loot["pools"][0]["conditions"][1]["properties"] == {"natural_node": "true"},
          "natural stardust sand ore drops the existing sixth-tier herb and placed copies are inert")
    check(star_ore_loot["pools"][0]["conditions"][1]["properties"] == {"natural_node": "true"},
          "placed starfall ore does not duplicate its one-time natural resource")
    check('placeMeteorite(level, craterFloor, random)' in crater_java
          and 'get("ore_xingchen_sand")' in crater_java
          and 'get("ore_xingyun_sand")' in crater_java
          and "NaturalResourceOreBlock.NATURAL_NODE, true" in crater_java
          and 'getBiome(surface).is(TerrainBiomes.METEOR_WASTELAND)' in crater_java,
          "meteorite and its two internal resources are confined to meteor craters")
    meteorite_names = load(RESOURCES / "assets/xiantu/lang/zh_cn.json")
    check(meteorite_names["block.xiantu.meteorite_stone"] == "陨石",
          "meteorite block uses the concise player-facing name")
    meteorite_shape = (ROOT / "src/main/java/com/longju/xiantu/worldgen/MeteoriteShape.java").read_text(encoding="utf-8")
    check("SMALL(new int[]{0, 1, 2, 1, 0}, 1, 1)" in meteorite_shape
          and "MEDIUM(new int[]{0, 2, 2, 3, 2, 2, 0}, 4, 3)" in meteorite_shape
          and "LARGE(new int[]{0, 2, 3, 3, 4, 3, 3, 2, 0}, 8, 6)" in meteorite_shape
          and "x * x + z * z <= radius * radius" in meteorite_shape
          and "interiorCells()" in crater_java
          and "MeteoriteShape.fromRoll(random.nextInt(10))" in crater_java,
          "compact spherical meteorites retain closed interiors and scaled ore capacity")

    plant_manifest = load(ASSETS / "plants_manifest.json")
    active_plants = {key for key, record in plant_manifest.items() if record.get("status") != "retired_unregistered"}
    check(active_plants == set(PLANTS), "all active habitat plants have resumable generation records")
    for plant, biome in PLANTS.items():
        configured = load(RESOURCES / f"data/xiantu/worldgen/configured_feature/{plant}_patch.json")
        texture = RESOURCES / f"assets/xiantu/textures/block/{plant}.png"
        with Image.open(texture) as image:
            alpha = image.convert("RGBA").getchannel("A")
            check(image.size == (64, 64) and alpha.getextrema() == (0, 255), f"cutout 64 sprite: {plant}")
            bounds = alpha.getbbox()
            check(bounds is not None and bounds[0] > 0 and bounds[2] < 64 and bounds[1] > 0,
                  f"sprite silhouette has transparent padding: {plant}")
        if plant == "storm_bamboo":
            blockstate = load(RESOURCES / f"assets/xiantu/blockstates/{plant}.json")
            check(set(blockstate["variants"]) == {"half=lower", "half=upper"},
                  "storm bamboo has both double-plant halves")
            for part in ("bottom", "top"):
                model = load(RESOURCES / f"assets/xiantu/models/block/{plant}_{part}.json")
                check(all(0 <= value <= 16 for element in model["elements"]
                          for corner in (element["from"], element["to"]) for value in corner),
                      f"storm bamboo {part} model stays within vanilla block bounds")
            check(configured["type"] == "xiantu:storm_bamboo_patch"
                  and "StormBambooPatchFeature" in (ROOT / "src/main/java/com/longju/xiantu/worldgen/StormBambooPatchFeature.java").read_text(encoding="utf-8"),
                  "storm bamboo worldgen places complete two-block plants")
            loot = load(RESOURCES / f"data/xiantu/loot_tables/blocks/{plant}.json")
            check(loot["pools"][0]["conditions"][0]["properties"] == {"half": "lower"},
                  "breaking a two-block bamboo drops one item")
        else:
            plant_model = load(RESOURCES / f"assets/xiantu/models/block/{plant}.json")
            check(plant_model["parent"] == "minecraft:block/cross",
                  f"vanilla crossed plant model: {plant}")
        check(load(RESOURCES / f"assets/xiantu/models/item/{plant}.json")["parent"] == "minecraft:item/generated",
              f"static generated held icon: {plant}")
        placed = load(RESOURCES / f"data/xiantu/worldgen/placed_feature/{plant}_patch.json")
        modifier = load(RESOURCES / f"data/xiantu/forge/biome_modifier/{plant}_patch.json")
        check(placed["feature"] == f"xiantu:{plant}_patch", f"placed plant feature resolves: {plant}")
        expected_tries = {"thunder_sedge": 5, "storm_bamboo": 12, "meteor_ash_grass": 14, "valley_jade_fern": 18}[plant]
        check((configured["type"] == "xiantu:storm_bamboo_patch" if plant == "storm_bamboo"
               else configured["type"] == "minecraft:random_patch" and configured["config"]["tries"] == expected_tries),
              f"plant patch adds a small multi-plant cluster: {plant}")
        if plant != "storm_bamboo":
            inner=configured["config"]["feature"]
            check(inner["feature"]["type"] == "xiantu:habitat_simple_block"
                  and not any(f["type"] == "minecraft:biome" for f in inner["placement"]),
                  "nested patches use direct biome guards, never topFeature-dependent BiomeFilter")
            check(inner["placement"][0] == {"type":"minecraft:heightmap", "heightmap":"MOTION_BLOCKING_NO_LEAVES"},
                  "each offset independently follows the surface")
            check(configured["config"]["xz_spread"] == 5,
                  f"plant cluster stays spatially tight: {plant}")
        expected_chance = {"thunder_sedge": 1, "storm_bamboo": 3, "meteor_ash_grass": 2,
                           "valley_jade_fern": 2}[plant]
        check(placed["placement"][0] == {"type": "minecraft:rarity_filter", "chance": expected_chance},
              f"signature plant cluster rate is tuned for its habitat: {plant}")
        if plant == "thunder_sedge":
            check(configured["config"]["tries"] == 5, "thunder sedge patch attempts reduced again from ten to five")
        if plant in {"thunder_sedge", "storm_bamboo"}:
            plant_source = (ROOT / "src/main/java/com/longju/xiantu/terrain/TerrainPlantRegistry.java").read_text(encoding="utf-8")
            expected_registration = 'registerStormBamboo()' if plant == "storm_bamboo" else f'register("{plant}", "thunder_rock"'
            check(expected_registration in plant_source,
                  f"{plant} can root in the thunder-rock surface")
        check(modifier["biomes"] == f"#xiantu:terrain/{biome}", f"plant targets only its biome: {plant}")
        check(load(RESOURCES / f"data/xiantu/tags/worldgen/biome/terrain/{biome}.json")["values"] == [f"xiantu:{biome}"],
              f"biome feature tag resolves: {biome}")

    outcrops = {
        "thunder_rock_outcrop": ("thunder_rock", "thunder_rock", "thunder_wastes"),
        "meteor_rock_outcrop": ("meteor_rock", "meteor_soil", "meteor_wasteland"),
    }
    for feature, (rock, soil, biome) in outcrops.items():
        configured = load(RESOURCES / f"data/xiantu/worldgen/configured_feature/{feature}.json")
        placed = load(RESOURCES / f"data/xiantu/worldgen/placed_feature/{feature}.json")
        modifier = load(RESOURCES / f"data/xiantu/forge/biome_modifier/{feature}.json")
        check(configured["type"] == "xiantu:habitat_landmark", "terrain silhouettes replace isolated single cubes")
        check(placed["placement"][0] == {"type": "minecraft:rarity_filter", "chance": 5 if feature.startswith("thunder") else 3}
              and modifier["biomes"] == f"#xiantu:terrain/{biome}"
              and modifier["step"] == "surface_structures", "landmarks precede vegetation in their own biome")

    tree_placed = load(RESOURCES / "data/xiantu/worldgen/placed_feature/lightning_scarred_tree.json")
    tree_modifier = load(RESOURCES / "data/xiantu/forge/biome_modifier/lightning_scarred_tree.json")
    check(tree_placed["placement"][0] == {"type": "minecraft:rarity_filter", "chance": 4}
          and tree_modifier["biomes"] == "#xiantu:terrain/thunder_wastes",
          "lightning-scarred trees are occasional and thunder-wastes-only")
    tree_source = (ROOT / "src/main/java/com/longju/xiantu/worldgen/LightningScarredTreeFeature.java").read_text(encoding="utf-8")
    check('get("thunder_rock")' in tree_source,
          "lightning-scarred trees can root in the thunder-rock surface")
    tree_blockstate = load(RESOURCES / "assets/xiantu/blockstates/lightning_scarred_log.json")
    check(set(tree_blockstate["variants"]) == {"axis=x", "axis=y", "axis=z"},
          "lightning-scarred log supports all pillar axes")

    for biome in sorted(PLANTS.values()):
        data = load(RESOURCES / f"data/xiantu/worldgen/biome/{biome}.json")
        check(len(data["features"]) == 11, f"vanilla generation stages preserved: {biome}")
        check(bool(data["carvers"]), f"vanilla cave carvers preserved: {biome}")

    medicine_plain = load(RESOURCES / "data/xiantu/worldgen/biome/spirit_medicine_plain.json")
    check(medicine_plain["effects"].get("grass_color") == 0x35582F,
          "spirit medicine plain uses the chosen deep green grass tint")
    thunder = load(RESOURCES / "data/xiantu/worldgen/biome/thunder_wastes.json")
    check(thunder["effects"]["sky_color"] == 0x51485F and thunder["effects"]["fog_color"] == 0x3B3B4A,
          "thunder wastes has subdued storm-purple sky and charcoal fog")
    meteor = load(RESOURCES / "data/xiantu/worldgen/biome/meteor_wasteland.json")
    check(meteor["effects"]["sky_color"] == 0x242239 and meteor["effects"]["fog_color"] == 0x251430
          and meteor["has_precipitation"] is False,
          "meteor wasteland keeps a dark indigo sky without changing world weather")
    meteor_crater = load(RESOURCES / "data/xiantu/forge/biome_modifier/meteor_crater.json")
    meteor_crater_feature = load(RESOURCES / "data/xiantu/worldgen/placed_feature/meteor_crater.json")
    crater_java = (ROOT / "src/main/java/com/longju/xiantu/worldgen/MeteorCraterFeature.java").read_text(encoding="utf-8")
    check(meteor_crater["biomes"] == "#xiantu:terrain/meteor_wasteland"
          and meteor_crater["features"] == ["xiantu:meteor_crater"]
          and meteor_crater_feature["placement"][0] == {"type": "minecraft:rarity_filter", "chance": 16}
          and "impact_glass_rock" in crater_java and "maxDepth = 2 + random.nextInt(3)" in crater_java
          and "radius = 6 + random.nextInt(4)" in crater_java
          and "level.getBiome(surface).is(TerrainBiomes.METEOR_WASTELAND)" in crater_java,
          "meteor craters form large irregular glass-lined basins only inside their biome")
    fissure_config = load(RESOURCES / "data/xiantu/worldgen/configured_feature/meteor_fissure_patch.json")
    fissure_placed = load(RESOURCES / "data/xiantu/worldgen/placed_feature/meteor_fissure_patch.json")
    fissure_java = (ROOT / "src/main/java/com/longju/xiantu/worldgen/MeteorFissureFeature.java").read_text(encoding="utf-8")
    check(fissure_config["type"] == "xiantu:meteor_fissure"
          and fissure_placed["placement"][0] == {"type": "minecraft:rarity_filter", "chance": 3}
          and "segments = 1 + random.nextInt(2)" in fissure_java
          and "length = segment == 0 ? 5 + random.nextInt(6) : 3 + random.nextInt(4)" in fissure_java
          and "level.getBiome(surface).is(TerrainBiomes.METEOR_WASTELAND)" in fissure_java,
          "glowing fissures form connected branches confined to the meteor biome")
    glass_patch = load(RESOURCES / "data/xiantu/worldgen/configured_feature/impact_glass_patch.json")
    glass_placed = load(RESOURCES / "data/xiantu/worldgen/placed_feature/impact_glass_patch.json")
    check(glass_patch["config"]["tries"] == 6
          and glass_placed["placement"][0] == {"type": "minecraft:rarity_filter", "chance": 8},
          "impact glass has a visible surface patch rate")
    check("star_dust_sand" in (ROOT / "src/main/java/com/longju/xiantu/terrain/TerrainWorldgenRegistration.java").read_text(encoding="utf-8")
          and "new FallingBlock" in (ROOT / "src/main/java/com/longju/xiantu/terrain/TerrainBlockRegistry.java").read_text(encoding="utf-8"),
          "star-dust sand is the meteor surface and uses vanilla falling-block behavior")
    atmosphere_java = (ROOT / "src/main/java/com/longju/xiantu/client/TerrainAtmosphereClient.java").read_text(encoding="utf-8")
    check("DustParticleOptions" in atmosphere_java and "nextInt(5)" in atmosphere_java,
          "meteor aurora appears as an occasional lightweight client-side particle band")
    sound_manifest = load(RESOURCES / "assets/xiantu/sounds.json")
    sound_event = sound_manifest["terrain.thunder_wastes_entry"]
    sound_file = RESOURCES / "assets/xiantu/sounds/ambient/thunder_wastes_thunder.ogg"
    check(sound_event["category"] == "weather"
          and sound_event["sounds"][0]["name"] == "xiantu:ambient/thunder_wastes_thunder"
          and sound_file.is_file() and sound_file.read_bytes().startswith(b"OggS"),
          "selected thunder-wastes entry sound is registered and packaged as Ogg")
    config_source = (ROOT / "src/main/java/com/longju/xiantu/config/XiantuConfig.java").read_text(encoding="utf-8")
    check('defineInRange("thunderStrikeMinSeconds", 5, 1, 3600)' in config_source
          and 'defineInRange("thunderStrikeMaxSeconds", 20, 1, 3600)' in config_source,
          "thunder-wastes strike defaults span 5 to 20 seconds")
    lightning_source = (ROOT / "src/main/java/com/longju/xiantu/terrain/TerrainLightningEvents.java").read_text(encoding="utf-8")
    check("maximum - minimum + 1" in lightning_source and ") * 20L" in lightning_source,
          "thunder strike interval includes both configured endpoints in seconds")
    qingmu_biome = RESOURCES / "data/xiantu/worldgen/biome/qingmu_forest.json"
    check(not qingmu_biome.exists(), "spirit woods do not register a separate biome that cuts across vanilla terrain")
    check(not (RESOURCES / "data/xiantu/tags/worldgen/biome/terrain/qingmu_forest.json").exists()
          and not (RESOURCES / "data/xiantu/tags/worldgen/biome/spirit_wood/qingmu_forest.json").exists()
          and not (RESOURCES / "data/xiantu/forge/biome_modifier/qingmu_forest.json").exists(),
          "standalone Qingmu biome tags and modifiers are removed")
    forest_tag_path = RESOURCES / "data/xiantu/tags/worldgen/biome/spirit_wood/vanilla_forests.json"
    forest_biomes = load(forest_tag_path)["values"]
    check(forest_biomes == ["minecraft:forest", "minecraft:flower_forest", "minecraft:birch_forest",
                            "minecraft:old_growth_birch_forest", "minecraft:dark_forest"],
          "spirit-wood forest habitat is limited to vanilla forest biomes")
    check(not (RESOURCES / "data/xiantu/forge/biome_modifier/qing_wen_elm_tree_forest.json").exists(),
          "dense first-tier forest modifier removed")
    check(not (RESOURCES / "data/xiantu/worldgen/placed_feature/qing_wen_elm_tree_forest.json").exists(),
          "eight-candidate first-tier forest placement removed")
    elm_habitats = load(RESOURCES / "data/xiantu/tags/worldgen/biome/spirit_wood/qing_wen_elm.json")["values"]
    check(elm_habitats == ["#xiantu:spirit_wood/vanilla_forests"],
          "first-tier spirit wood restricted to forests; plains excluded")
    worldgen_rules = (ROOT / "src/main/java/com/longju/xiantu/wood/SpiritWoodWorldgenRules.java").read_text(encoding="utf-8")
    check("if (type.rank() == 1) return type.chance;" in worldgen_rules,
          "first-tier forest feature uses the ordinary species chance")
    for tree in ("chi_xin_birch", "zi_wen_maple", "xuan_tie_pine", "jin_si_nanmu", "zi_fu_cedar", "xing_wen_wutong", "tian_qiong_sandalwood"):
        values = load(RESOURCES / f"data/xiantu/tags/worldgen/biome/spirit_wood/{tree}.json")["values"]
        check("#xiantu:spirit_wood/vanilla_forests" in values, f"higher-tier tree habitat includes vanilla forests: {tree}")
    check(not (RESOURCES / "data/xiantu/worldgen/biome/ghost_earth_cavern.json").exists(),
          "removed ghost-earth cavern is absent from biome data")

    check("ItemBlockRenderTypes.setRenderLayer(block.get(), RenderType.cutout())" in
          (ROOT / "src/main/java/com/longju/xiantu/client/XiantuClient.java").read_text(encoding="utf-8"),
          "cross plants use cutout rendering")
    check("terrablender" in (ROOT / "src/main/resources/META-INF/mods.toml").read_text(encoding="utf-8"),
          "TerraBlender runtime dependency is declared")
    first_tier_tree = load(RESOURCES / "data/xiantu/worldgen/placed_feature/qing_wen_elm_tree.json")
    check([p["type"] for p in first_tier_tree["placement"]] == ["minecraft:in_square", "minecraft:heightmap", "minecraft:biome"],
          "first-tier spirit wood has one candidate and no second rarity gate")
    terrain_source = (ROOT / "src/main/java/com/longju/xiantu/terrain/TerrainWorldgenRegistration.java").read_text(encoding="utf-8")
    check("SurfaceRules.not(SurfaceRules.hole())" in terrain_source,
          "custom surface layers skip cave openings")
    check("ghost_earth_cavern" not in terrain_source and "cave_ghost_soil" not in terrain_source,
          "removed cave biome and soil are absent from terrain surface rules")
    check(not (RESOURCES / "data/xiantu/worldgen/biome/moonstone_grove.json").exists(),
          "removed moonstone grove biome is absent")
    check(not (RESOURCES / "data/xiantu/forge/biome_modifier/moonstone_outcrop.json").exists(),
          "removed moonstone outcrop modifier is absent")
    for obsolete in ("cold_marsh_stone", "moon_frost_soil", "cold_silver_reed"):
        check(not (RESOURCES / f"assets/xiantu/textures/block/{obsolete}.png").exists()
              and not (RESOURCES / f"data/xiantu/loot_tables/blocks/{obsolete}.json").exists(),
              f"removed moonstone-only asset is absent: {obsolete}")
    plant_registry = (ROOT / "src/main/java/com/longju/xiantu/terrain/TerrainPlantRegistry.java").read_text(encoding="utf-8")
    block_registry = (ROOT / "src/main/java/com/longju/xiantu/terrain/TerrainBlockRegistry.java").read_text(encoding="utf-8")
    check("cold_silver_reed" not in plant_registry and "cold_marsh_stone" not in block_registry
          and "moon_frost_soil" not in block_registry,
          "moonstone-only blocks and plant stay unregistered")
    print(f"Terrain resource verification: {CHECKS} checks passed ({len(BLOCKS)} landforms, {len(SPECIAL_BLOCKS)} meteor blocks, {len(PLANTS)} plants, 3 Xiantu surface biomes)")


if __name__ == "__main__":
    main()
