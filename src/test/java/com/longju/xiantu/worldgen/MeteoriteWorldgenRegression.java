package com.longju.xiantu.worldgen;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class MeteoriteWorldgenRegression {
    private static int checks;

    public static void main(String[] args) throws Exception {
        assertShape(MeteoriteShape.SMALL, 5, 5);
        assertShape(MeteoriteShape.MEDIUM, 7, 7);
        assertShape(MeteoriteShape.LARGE, 9, 9);
        require(MeteoriteShape.SMALL.starDustCount() == 1 && MeteoriteShape.SMALL.starOreCount() == 1,
                "small meteor contains both internal resources");
        require(MeteoriteShape.MEDIUM.starDustCount() == 4 && MeteoriteShape.MEDIUM.starOreCount() == 3,
                "medium meteor has increased internal resource capacity");
        require(MeteoriteShape.LARGE.starDustCount() == 8 && MeteoriteShape.LARGE.starOreCount() == 6,
                "large meteor has the greatest internal resource capacity");

        int[] rolls = new int[3];
        for (int roll = 0; roll < 10; roll++) rolls[MeteoriteShape.fromRoll(roll).ordinal()]++;
        require(rolls[0] == 3 && rolls[1] == 5 && rolls[2] == 2,
                "meteor sizes use the documented 30/50/20 distribution");

        JsonObject biomeModifier = readJson("data/xiantu/forge/biome_modifier/meteor_crater.json");
        require(biomeModifier.get("biomes").getAsString().equals("#xiantu:terrain/meteor_wasteland")
                        && biomeModifier.getAsJsonArray("features").get(0).getAsString().equals("xiantu:meteor_crater"),
                "meteor structures stay inside Meteor Wasteland crater generation");
        JsonObject feature = readJson("data/xiantu/worldgen/placed_feature/meteor_crater.json");
        require(feature.getAsJsonArray("placement").get(0).getAsJsonObject().get("chance").getAsInt() == 16,
                "meteor crater placement retains its one-in-sixteen chunk attempt");

        JsonObject names = readJson("assets/xiantu/lang/zh_cn.json");
        require(names.get("block.xiantu.meteorite_stone").getAsString().equals("陨石"),
                "meteorite block is named 陨石 in Chinese");
        System.out.println("Meteorite worldgen regression: " + checks + " passed");
    }

    private static void assertShape(MeteoriteShape shape, int width, int height) {
        List<MeteoriteShape.Cell> cells = shape.cells();
        List<MeteoriteShape.Cell> interior = shape.interiorCells();
        require(shape.maxWidth() == width && shape.height() == height,
                shape + " has the approved dimensions");
        require(cells.size() > interior.size() && !interior.isEmpty(),
                shape + " is a solid stepped mass with enclosed interior cells");
        require(interior.size() >= shape.starDustCount() + shape.starOreCount(),
                shape + " has enough enclosed cells for both resource deposits");
        require(shape.maxWidth() == shape.height(),
                shape + " stays compact instead of forming a wide wall");
        require(shape.cells().stream().allMatch(cell -> {
                    int layerRadius = (shape.maxWidth() - 1) / 2;
                    return cell.x() * cell.x() + cell.z() * cell.z() <= layerRadius * layerRadius;
                }), shape + " uses rounded horizontal cross-sections");
        for (MeteoriteShape.Cell cell : interior) {
            require(cells.contains(new MeteoriteShape.Cell(cell.x() + 1, cell.y(), cell.z()))
                            && cells.contains(new MeteoriteShape.Cell(cell.x() - 1, cell.y(), cell.z()))
                            && cells.contains(new MeteoriteShape.Cell(cell.x(), cell.y() + 1, cell.z()))
                            && cells.contains(new MeteoriteShape.Cell(cell.x(), cell.y() - 1, cell.z()))
                            && cells.contains(new MeteoriteShape.Cell(cell.x(), cell.y(), cell.z() + 1))
                            && cells.contains(new MeteoriteShape.Cell(cell.x(), cell.y(), cell.z() - 1)),
                    shape + " resource cells remain enclosed by meteorite blocks");
        }
    }

    private static JsonObject readJson(String path) throws Exception {
        var stream = MeteoriteWorldgenRegression.class.getClassLoader().getResourceAsStream(path);
        if (stream == null) throw new AssertionError("Missing classpath resource: " + path);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static void require(boolean condition, String description) {
        if (!condition) throw new AssertionError(description);
        checks++;
    }

    private MeteoriteWorldgenRegression() {}
}
