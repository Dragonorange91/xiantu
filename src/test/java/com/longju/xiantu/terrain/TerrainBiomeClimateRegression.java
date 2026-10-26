package com.longju.xiantu.terrain;

import net.minecraft.world.level.biome.Climate;
import terrablender.api.ParameterUtils;

public final class TerrainBiomeClimateRegression {
    private static int checks;

    public static void main(String[] args) {
        Climate.Parameter[] temperatures = {
                TerrainClimateBands.THUNDER_TEMPERATURE, TerrainClimateBands.METEOR_TEMPERATURE,
                TerrainClimateBands.MEDICINE_TEMPERATURE};
        Climate.Parameter[] humidities = {
                TerrainClimateBands.THUNDER_HUMIDITY, TerrainClimateBands.METEOR_HUMIDITY,
                TerrainClimateBands.MEDICINE_HUMIDITY};
        Climate.Parameter[] erosions = {
                TerrainClimateBands.THUNDER_EROSION, TerrainClimateBands.METEOR_EROSION,
                TerrainClimateBands.MEDICINE_EROSION};
        Climate.Parameter[] weirdnesses = {
                TerrainClimateBands.THUNDER_WEIRDNESS, TerrainClimateBands.METEOR_WEIRDNESS,
                TerrainClimateBands.MEDICINE_WEIRDNESS};
        String[] names = {"thunder_wastes", "meteor_wasteland", "spirit_medicine_plain"};
        require(names.length == 3, "exactly three Xiantu surface biomes are configured; spirit woods use vanilla forests");
        for (int i = 0; i < temperatures.length; i++) {
            var points = TerrainClimateBands.climatePoints(temperatures[i], humidities[i],
                    TerrainClimateBands.SURFACE_CONTINENTALNESS, TerrainClimateBands.SURFACE_DEPTH,
                    erosions[i], weirdnesses[i]);
            require(!points.isEmpty(), "surface climate points are produced for " + names[i]);
            for (Climate.ParameterPoint point : points) {
                require(same(point.depth(), ParameterUtils.Depth.SURFACE.parameter()),
                        "surface biome is mapped only at surface depth");
                require(same(point.continentalness(), TerrainClimateBands.SURFACE_CONTINENTALNESS),
                        "surface biome stays within the inland-only continentalness window");
                require(same(point.erosion(), erosions[i]),
                        "surface biome uses its assigned erosion range");
                require(same(point.weirdness(), weirdnesses[i]),
                        "surface biome uses its assigned terrain-relief range");
            }
            for (int j = i + 1; j < temperatures.length; j++) {
                require(separated(temperatures[i], temperatures[j]) || separated(humidities[i], humidities[j]),
                        "surface climate niches have a buffer between " + names[i] + " and " + names[j]);
            }
        }
        require(same(TerrainClimateBands.SURFACE_CONTINENTALNESS,
                        ParameterUtils.Continentalness.span(ParameterUtils.Continentalness.MID_INLAND,
                                ParameterUtils.Continentalness.FAR_INLAND)),
                "surface biomes exclude ocean and coastal continentalness values");
        require(!same(TerrainClimateBands.THUNDER_EROSION, TerrainClimateBands.METEOR_EROSION)
                        && !same(TerrainClimateBands.THUNDER_EROSION, TerrainClimateBands.MEDICINE_EROSION),
                "biomes no longer share a single erosion band");
        require(!same(TerrainClimateBands.THUNDER_WEIRDNESS, TerrainClimateBands.METEOR_WEIRDNESS)
                        && !same(TerrainClimateBands.THUNDER_WEIRDNESS, TerrainClimateBands.MEDICINE_WEIRDNESS),
                "biomes no longer share a single terrain-relief band");
        require(TerrainClimateBands.SPECIAL_ROCK_DEPTH == 17,
                "custom terrain rock extends 18 blocks beneath the surface");
        require(same(TerrainClimateBands.METEOR_EROSION, Climate.Parameter.span(0.35F, 0.70F))
                        && same(TerrainClimateBands.METEOR_WEIRDNESS, Climate.Parameter.span(-0.10F, 0.10F)),
                "meteor wasteland favors a low-relief inland climate window");

        System.out.println("Terrain biome climate regression: " + checks + " passed");
    }

    private static boolean same(Climate.Parameter actual, Climate.Parameter expected) {
        return actual.min() == expected.min() && actual.max() == expected.max();
    }

    private static boolean separated(Climate.Parameter first, Climate.Parameter second) {
        return first.max() < second.min() || second.max() < first.min();
    }

    private static void require(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
        checks++;
        System.out.println("[Terrain biome climate PASS] " + label);
    }

    private TerrainBiomeClimateRegression() {}
}
