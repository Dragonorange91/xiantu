package com.longju.xiantu.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.registries.ForgeRegistries;
import org.lytharalab.gfbs.gltf.api.client.ClientGltfApi;
import org.lytharalab.gfbs.gltf.api.client.GltfInstance;
import org.lytharalab.gfbs.gltf.api.client.GltfRenderer;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.function.Consumer;

/** Client-only native triangle rendering; GUI context remains an ordinary flat sprite. */
public final class GltfItemRenderer extends BlockEntityWithoutLevelRenderer {
    public static final List<String> IDS = List.of("fan_ling_shi", "ling_jing", "ling_yu", "xuan_jing",
            "di_sui", "tian_jing", "xian_yu", "dao_shi", "hanxing_pickaxe", "jiuyou_pickaxe", "hundun_pickaxe",
            "reincarnation_stone", "qunyou_figurine");
    private static GltfItemRenderer singleton;
    private final Map<ResourceLocation, Entry> entries = new HashMap<>();
    private long generation;

    private GltfItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    public static GltfItemRenderer instance() {
        if (singleton == null) singleton = new GltfItemRenderer();
        return singleton;
    }

    /** Called only by Forge's client initializeClient hook, never from item constructors. */
    public static void install(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return instance();
            }
        });
    }

    public SimplePreparableReloadListener<Map<ResourceLocation, Placement>> reloadListener() {
        return new SimplePreparableReloadListener<>() {
            @Override
            protected Map<ResourceLocation, Placement> prepare(ResourceManager manager, ProfilerFiller profiler) {
                Map<ResourceLocation, Placement> placements = new HashMap<>();
                for (String id : IDS) {
                    ResourceLocation location = new ResourceLocation("xiantu", "models/item/" + id + "_glb.json");
                    try (var reader = manager.getResourceOrThrow(location).openAsReader()) {
                        JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                        var offset = json.getAsJsonArray("translation");
                        placements.put(new ResourceLocation("xiantu", id), new Placement(
                                new ResourceLocation(json.get("model").getAsString()),
                                json.get("scale").getAsFloat(), offset.get(0).getAsDouble(),
                                offset.get(1).getAsDouble(), offset.get(2).getAsDouble(),
                                json.has("block_offset_y") ? json.get("block_offset_y").getAsDouble() : 0));
                    } catch (IOException | RuntimeException error) {
                        throw new IllegalStateException("Cannot load GLB item placement: " + location, error);
                    }
                }
                return placements;
            }

            @Override
            protected void apply(Map<ResourceLocation, Placement> placements, ResourceManager manager, ProfilerFiller profiler) {
                generation++;
                for (Entry entry : entries.values()) if (entry.instance != null) entry.instance.close();
                entries.clear();
                placements.forEach((id, placement) -> entries.put(id, new Entry(placement)));
                // GFBS owns CPU/GPU asset invalidation. Load lazily after all reload listeners finish.
                LogUtils.getLogger().info("[Xiantu GLB] Native item cache reset: {} items, generation {}", entries.size(), generation);
            }
        };
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poses,
                             MultiBufferSource buffers, int light, int overlay) {
        if (context == ItemDisplayContext.GUI) return;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        draw(id, poses, buffers, light, overlay, false);
    }

    public void renderStaticBlock(ResourceLocation id, PoseStack poses, MultiBufferSource buffers, int light, int overlay) {
        draw(id, poses, buffers, light, overlay, true);
    }

    private void draw(ResourceLocation id, PoseStack poses, MultiBufferSource buffers, int light, int overlay, boolean block) {
        Entry entry = entries.get(id);
        if (entry == null || entry.failed) return;
        if (entry.instance == null) {
            if (!entry.loading) load(id, entry);
            return;
        }
        poses.pushPose();
        try {
            Placement placement = entry.placement;
            poses.translate(placement.x, placement.y + (block ? placement.blockOffsetY : 0), placement.z);
            poses.scale(placement.scale, placement.scale, placement.scale);
            // No animations/morph/skin or RenderType overrides: GFBS uses its resident GPU triangle path.
            // Minecraft has already applied the display transform and the item -0.5 origin offset.
            GltfRenderer.render(entry.instance, poses, buffers, light, overlay);
            if (!entry.loggedDraw) {
                entry.loggedDraw = true;
                LogUtils.getLogger().info("[Xiantu GLB] Native static model rendered: {} (block={})", id, block);
            }
        } finally {
            poses.popPose();
        }
    }

    private void load(ResourceLocation id, Entry entry) {
        entry.loading = true;
        long requestGeneration = generation;
        ClientGltfApi.models().load(entry.placement.model).whenComplete((asset, error) ->
                Minecraft.getInstance().execute(() -> {
                    // Discard old completion callbacks without letting them resurrect a reloaded instance.
                    if (generation != requestGeneration || entries.get(id) != entry) return;
                    entry.loading = false;
                    if (error != null) {
                        if (error instanceof CancellationException || error.getCause() instanceof CancellationException) return;
                        entry.failed = true;
                        LogUtils.getLogger().error("[Xiantu GLB] Failed to load native item {}", id, error);
                        return;
                    }
                    entry.instance = new GltfInstance(asset);
                    entry.instance.renderOptions().frustumCulling(false).occlusionCulling(false);
                    // One immutable-pose instance per item ID is safe for these animation-free static assets.
                    // Multiple stacks and views share GFBS GPU buffers; no per-frame load/new/update.
                }));
    }

    private record Placement(ResourceLocation model, float scale, double x, double y, double z, double blockOffsetY) {}
    private static final class Entry {
        private final Placement placement;
        private GltfInstance instance;
        private boolean loading, failed, loggedDraw;
        private Entry(Placement placement) { this.placement = placement; }
    }
}
