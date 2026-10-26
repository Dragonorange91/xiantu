package com.longju.xiantu.terrain;

import net.minecraft.world.level.biome.Climate;
import java.util.List;

import static terrablender.api.ParameterUtils.*;

final class TerrainClimateBands {
    static final int SPECIAL_ROCK_DEPTH = 17;
    static final Climate.Parameter SURFACE_CONTINENTALNESS =
            Continentalness.span(Continentalness.MID_INLAND, Continentalness.FAR_INLAND);
    static final Climate.Parameter SURFACE_DEPTH = Depth.span(Depth.SURFACE, Depth.SURFACE);
    static final Climate.Parameter THUNDER_TEMPERATURE = Climate.Parameter.span(-0.60F, -0.35F);
    static final Climate.Parameter THUNDER_HUMIDITY = Climate.Parameter.span(-0.65F, -0.35F);
    static final Climate.Parameter THUNDER_EROSION = Climate.Parameter.span(0.20F, 0.65F);
    static final Climate.Parameter THUNDER_WEIRDNESS = Climate.Parameter.span(-0.15F, 0.15F);
    static final Climate.Parameter METEOR_TEMPERATURE = Climate.Parameter.span(0.50F, 0.80F);
    static final Climate.Parameter METEOR_HUMIDITY = Climate.Parameter.span(-0.85F, -0.55F);
    static final Climate.Parameter METEOR_EROSION = Climate.Parameter.span(0.35F, 0.70F);
    static final Climate.Parameter METEOR_WEIRDNESS = Climate.Parameter.span(-0.10F, 0.10F);
    static final Climate.Parameter MEDICINE_TEMPERATURE = Climate.Parameter.span(0.65F, 0.82F);
    static final Climate.Parameter MEDICINE_HUMIDITY = Climate.Parameter.span(0.65F, 0.82F);
    static final Climate.Parameter MEDICINE_EROSION = Climate.Parameter.span(0.25F, 0.65F);
    static final Climate.Parameter MEDICINE_WEIRDNESS = Climate.Parameter.span(-0.10F, 0.30F);
    static List<Climate.ParameterPoint> climatePoints(Climate.Parameter temperature,
                                                      Climate.Parameter humidity,
                                                      Climate.Parameter continentalness,
                                                      Climate.Parameter depth,
                                                      Climate.Parameter erosion,
                                                      Climate.Parameter weirdness) {
        return new ParameterPointListBuilder()
                .temperature(temperature)
                .humidity(humidity)
                .continentalness(continentalness)
                .erosion(erosion)
                .depth(depth)
                .weirdness(weirdness)
                .build();
    }

    private TerrainClimateBands() {}
}
