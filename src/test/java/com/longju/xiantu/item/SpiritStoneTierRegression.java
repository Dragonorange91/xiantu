package com.longju.xiantu.item;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

public final class SpiritStoneTierRegression {
    private static final int[] EXPECTED_POWER = {100, 310, 670, 2_790, 8_620, 35_300, 73_500, 299_000};

    public static void main(String[] args) {
        SpiritStoneTier[] tiers = SpiritStoneTier.values();
        require(tiers.length == EXPECTED_POWER.length, "expected eight spirit-stone tiers");
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < tiers.length; i++) {
            SpiritStoneTier tier = tiers[i];
            require(tier.rank() == i + 1, "tier ranks must be contiguous from one");
            require(tier.spiritualPower() == EXPECTED_POWER[i], "wrong spiritual power at tier " + (i + 1));
            require(ids.add(tier.registryName()), "duplicate registry id: " + tier.registryName());
            if (i > 0) {
                int expected = new int[]{0,3,2,4,3,4,2,4}[i];
                require(tier.downcraftCount() == expected, "irregular adjacent output");
                long loss = tier.spiritualPower() - expected * tiers[i-1].spiritualPower();
                require(loss > 0 && loss < tier.spiritualPower() * 0.08, "small deterministic downcraft loss");
            }
        }
        verifyDowncraftRecipes(tiers);
        System.out.println("PASS: 8 spirit-stone tiers, IDs, ranks, power values, and 7 downcraft recipes");
    }

    private static void verifyDowncraftRecipes(SpiritStoneTier[] tiers) {
        for (int i = 1; i < tiers.length; i++) {
            String path = "/data/xiantu/recipes/spirit_stone_downcraft_"
                    + tiers[i].registryName() + ".json";
            try (var stream = SpiritStoneTierRegression.class.getResourceAsStream(path)) {
                require(stream != null, "missing downcraft recipe for " + tiers[i].registryName());
                JsonObject recipe = JsonParser.parseReader(
                        new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
                require("xiantu:spirit_stone_downcraft".equals(recipe.get("type").getAsString()),
                        "downcraft uses the full-charge-checked shapeless recipe");
                require(recipe.get("input").getAsString().equals(tiers[i].registryName()),
                        "downcraft must consume the next higher tier");
            } catch (Exception exception) {
                throw new AssertionError("invalid downcraft recipe: " + path, exception);
            }
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
