package com.longju.xiantu.alchemy;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.longju.xiantu.item.PillItem;
import com.longju.xiantu.registry.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.RegistryObject;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Server-owned definitions: item NBT selects quality, never arbitrary effect magnitudes. */
public final class PillCatalog {
    public record Definition(String id, String name, int rank, String kind, int duration,
                             List<Double> values, int cooldown, String description) {
        public double value(int quality) { return values.get(Math.max(1, Math.min(5, quality)) - 1); }
        public boolean breakthrough() { return kind.equals("breakthrough") || kind.equals("auxiliary"); }
    }
    public static final String[] QUALITIES = {"凡品", "下品", "中品", "上品", "极品"};
    public static final List<Definition> ALL = load();
    public static final Map<String, RegistryObject<Item>> ITEMS = new LinkedHashMap<>();
    private static List<Definition> load() {
        try (var in = PillCatalog.class.getResourceAsStream("/data/xiantu/pills.json")) {
            if (in == null) throw new IllegalStateException("Missing pill catalogue");
            List<Definition> rows = new Gson().fromJson(new InputStreamReader(in, StandardCharsets.UTF_8),
                    new TypeToken<List<Definition>>() {}.getType());
            if (rows.size() != 36 || rows.stream().map(Definition::id).distinct().count() != 36)
                throw new IllegalStateException("Expected 36 distinct pills");
            for (var d : rows) if (d.values.size() != 5 || d.rank < 1 || d.rank > 8
                    || d.values.stream().anyMatch(v -> !Double.isFinite(v) || v < 0))
                throw new IllegalStateException("Invalid pill " + d.id);
            return List.copyOf(rows);
        } catch (Exception e) { throw new IllegalStateException("Invalid pill catalogue", e); }
    }
    public static void bootstrap() {
        if (!ITEMS.isEmpty()) return;
        for (var d : ALL) ITEMS.put(d.id(), ModItems.ITEMS.register(d.id(), () -> new PillItem(d)));
    }
    public static Definition get(String id) { return ALL.stream().filter(d -> d.id().equals(id)).findFirst().orElseThrow(); }
    public static int quality(ItemStack stack) {
        return stack.hasTag() ? Math.max(1, Math.min(5, stack.getTag().getInt("pill_quality"))) : 1;
    }
    public static ItemStack stack(String id, int quality) {
        ItemStack stack = new ItemStack(ITEMS.get(id).get());
        stack.getOrCreateTag().putInt("pill_quality", Math.max(1, Math.min(5, quality)));
        return stack;
    }
    private PillCatalog() {}
}
