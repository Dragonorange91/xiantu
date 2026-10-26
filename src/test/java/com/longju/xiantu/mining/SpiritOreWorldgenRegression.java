package com.longju.xiantu.mining;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class SpiritOreWorldgenRegression {
    private static final List<OreSpec> ORES = List.of(
            new OreSpec("fan_ling_shi_ore", 1, -32, 30, 6),
            new OreSpec("ling_jing_ore", 2, -48, 48, 5),
            new OreSpec("ling_yu_ore", 5, -64, 16, 4),
            new OreSpec("xuan_jing_ore", 13, -64, 0, 3),
            new OreSpec("di_sui_ore", 34, -64, -16, 3),
            new OreSpec("tian_jing_ore", 67, -64, -24, 2),
            new OreSpec("xian_yu_ore", 100, -64, -40, 2),
            new OreSpec("dao_shi_ore", 167, -64, -48, 1));

    private static int checks;

    public static void main(String[] args) throws Exception {
        JsonObject biomeModifier = readJson("data/xiantu/forge/biome_modifier/add_spirit_ores.json");
        require(biomeModifier.get("type").getAsString().equals("forge:add_features"),
                "spirit ores use a Forge add-features biome modifier");
        require(biomeModifier.get("biomes").getAsString().equals("#minecraft:is_overworld"),
                "spirit ores are added only to Overworld biomes");
        require(biomeModifier.get("step").getAsString().equals("underground_ores"),
                "spirit ores generate in the underground ores step");

        JsonArray registered = biomeModifier.getAsJsonArray("features");
        require(registered.size() == ORES.size(), "all and only eight spirit ores are registered");
        for (OreSpec ore : ORES) {
            String id = "xiantu:" + ore.name;
            require(contains(registered, id), id + " is registered in the Overworld");

            JsonObject placed = readJson("data/xiantu/worldgen/placed_feature/" + ore.name + ".json");
            require(placed.get("feature").getAsString().equals(id), id + " placed feature points to itself");
            JsonArray placement = placed.getAsJsonArray("placement");
            JsonObject rarity = findPlacement(placement, "minecraft:rarity_filter");
            require(rarity.get("chance").getAsInt() == ore.rarity,
                    id + " retains its configured per-chunk rarity");
            JsonObject height = findPlacement(placement, "minecraft:height_range")
                    .getAsJsonObject("height");
            require(height.get("type").getAsString().equals("minecraft:uniform")
                            && height.getAsJsonObject("min_inclusive").get("absolute").getAsInt() == ore.minY
                            && height.getAsJsonObject("max_inclusive").get("absolute").getAsInt() == ore.maxY,
                    id + " retains its documented height range");

            JsonObject configured = readJson("data/xiantu/worldgen/configured_feature/" + ore.name + ".json");
            String expectedFeature = "xiantu:small_spirit_ore";
            require(configured.get("type").getAsString().equals(expectedFeature),
                    id + " uses an ore feature suitable for its deposit size");
            JsonObject config = configured.getAsJsonObject("config");
            require(config.get("size").getAsInt() == ore.size,
                    id + " retains its documented vein size");
            require(config.get("discard_chance_on_air_exposure").getAsDouble() == 0.0D,
                    id + " does not silently discard exposed ore");
            JsonObject blockModel = readJson("assets/xiantu/models/block/" + ore.name + ".json");
            String texture = blockModel.getAsJsonObject("textures").get("all").getAsString();
            String texturePath = "assets/xiantu/textures/" + texture.substring(texture.indexOf(':') + 1) + ".png";
            require(SpiritOreWorldgenRegression.class.getClassLoader().getResource(texturePath) != null,
                    id + " block model resolves to an existing unique texture");
            JsonArray targets = config.getAsJsonArray("targets");
            require(targets.size() == 2 && targetsBlock(targets, "minecraft:stone")
                            && targetsBlock(targets, "minecraft:deepslate"),
                    id + " replaces both stone and deepslate");
        }
        System.out.println("Spirit ore worldgen regression: " + checks + " passed");
    }

    private static JsonObject readJson(String path) throws Exception {
        var stream = SpiritOreWorldgenRegression.class.getClassLoader().getResourceAsStream(path);
        if (stream == null) throw new AssertionError("Missing classpath resource: " + path);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static JsonObject findPlacement(JsonArray placements, String type) {
        for (var element : placements) {
            JsonObject placement = element.getAsJsonObject();
            if (placement.get("type").getAsString().equals(type)) return placement;
        }
        throw new AssertionError("Missing placement modifier: " + type);
    }

    private static boolean targetsBlock(JsonArray targets, String block) {
        for (var element : targets) {
            JsonObject target = element.getAsJsonObject().getAsJsonObject("target");
            if (target.get("predicate_type").getAsString().equals("minecraft:block_match")
                    && target.get("block").getAsString().equals(block)) return true;
        }
        return false;
    }

    private static boolean contains(JsonArray values, String expected) {
        for (var value : values) if (value.getAsString().equals(expected)) return true;
        return false;
    }

    private static void require(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
        checks++;
        System.out.println("[Spirit ore worldgen PASS] " + label);
    }

    private record OreSpec(String name, int rarity, int minY, int maxY, int size) {}
}
