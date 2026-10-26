package com.longju.xiantu.item;

import com.google.gson.JsonParser;

import javax.imageio.ImageIO;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class SpiritStoneAssetRegression {
    private static final List<String> IDS = List.of(
            "fan_ling_shi", "ling_jing", "ling_yu", "xuan_jing",
            "di_sui", "tian_jing", "xian_yu", "dao_shi",
            "hanxing_pickaxe", "jiuyou_pickaxe", "hundun_pickaxe");

    private static int checks;

    public static void main(String[] args) throws Exception {
        for (String id : IDS) {
            String texturePath = "/assets/xiantu/textures/item/" + id + ".png";
            var texture = SpiritStoneAssetRegression.class.getResourceAsStream(texturePath);
            if (texture == null) throw new AssertionError("Missing spirit-stone texture: " + texturePath);
            var image = ImageIO.read(texture);
            require(image != null && image.getWidth() == 64 && image.getHeight() == 64,
                    id + " texture is a readable 64x64 PNG");
            require(image.getColorModel().hasAlpha(), id + " texture has an alpha channel");

            String modelPath = "/assets/xiantu/models/item/" + id + ".json";
            var model = SpiritStoneAssetRegression.class.getResourceAsStream(modelPath);
            if (model == null) throw new AssertionError("Missing spirit-stone model: " + modelPath);
            try (var reader = new InputStreamReader(model, StandardCharsets.UTF_8)) {
                var root = JsonParser.parseReader(reader).getAsJsonObject();
                if (root.has("loader")) {
                    require(root.get("loader").getAsString().equals("forge:separate_transforms"), id + " selects models per display context");
                    require(root.getAsJsonObject("base").get("parent").getAsString().equals("xiantu:item/" + id + "_glb"), id + " preserves its world GLB");
                    var gui = root.getAsJsonObject("perspectives").getAsJsonObject("gui");
                    require(gui.get("parent").getAsString().equals("minecraft:item/generated") && !gui.has("loader"), id + " GUI renders a flat sprite without GLB");
                    require(gui.getAsJsonObject("textures").get("layer0").getAsString().equals("xiantu:item/" + id), id + " GUI uses its own 64px icon");
                    try (var glb = SpiritStoneAssetRegression.class.getResourceAsStream("/assets/xiantu/models/item/" + id + ".glb")) {
                        require(glb != null && glb.readNBytes(4)[0] == 'g', id + " GLB is packaged");
                    }
                    continue; // Complete mesh, atlas, bounds and source integrity checks run in verifyItemGlb.
                }
                String textureId = root.getAsJsonObject("textures").get("layer0").getAsString();
                require(textureId.equals("xiantu:item/" + id), id + " model points to its own icon");
                var elements = root.getAsJsonArray("elements");
                require(elements != null && !elements.isEmpty(), id + " model has extruded geometry");
                require(elements.size() <= 96, id + " model stays within the element budget");
                for (var elementValue : elements) {
                    var element = elementValue.getAsJsonObject();
                    var from = element.getAsJsonArray("from");
                    var to = element.getAsJsonArray("to");
                    require(from.size() == 3 && to.size() == 3, id + " element has three-dimensional bounds");
                    for (int axis = 0; axis < 2; axis++) {
                        double low = from.get(axis).getAsDouble();
                        double high = to.get(axis).getAsDouble();
                        require(low >= 0 && high <= 16 && high > low, id + " element fits item canvas");
                    }
                    double zLow = from.get(2).getAsDouble();
                    double zHigh = to.get(2).getAsDouble();
                    require(zLow >= 0 && zHigh <= 16 && zHigh > zLow, id + " element has valid depth");

                    var faces = element.getAsJsonObject("faces");
                    require(faces.has("north") && faces.has("south"), id + " element has front and back faces");
                    for (String faceName : List.of("north", "south")) {
                        var face = faces.getAsJsonObject(faceName);
                        var uv = face.getAsJsonArray("uv");
                        require(face.get("texture").getAsString().equals("#layer0"), id + " face uses own texture");
                        require(uv.size() == 4 && uv.get(0).getAsDouble() >= 0
                                        && uv.get(1).getAsDouble() >= 0
                                        && uv.get(2).getAsDouble() <= 16
                                        && uv.get(3).getAsDouble() <= 16,
                                id + " face UV fits source texture");
                    }
                }
            }
        }
        System.out.println("Spirit item model regression: " + checks + " passed");
    }

    private static void require(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
        checks++;
    }
}
