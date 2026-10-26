package com.longju.xiantu.client;

import com.longju.xiantu.XiantuMod;
import com.longju.xiantu.entity.CragRockSpiderEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import org.lytharalab.gfbs.gltf.api.animation.LoopMode;
import org.lytharalab.gfbs.gltf.api.animation.PlaybackOptions;
import org.lytharalab.gfbs.gltf.api.client.ClientGltfApi;
import org.lytharalab.gfbs.gltf.api.client.GltfInstance;
import org.lytharalab.gfbs.gltf.api.client.GltfRenderer;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CancellationException;

public final class CragRockSpiderRenderer extends EntityRenderer<CragRockSpiderEntity> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation MODEL = new ResourceLocation(XiantuMod.MOD_ID, "models/entity/lie_yan_zhu.glb");
    private static final ResourceLocation STATIC_LOD = new ResourceLocation(XiantuMod.MOD_ID, "models/entity/lie_yan_zhu_lod.glb");
    private static final float MODEL_SCALE = 1.4F;
    private static final float MODEL_VERTICAL_OFFSET = 0.02F;
    private static final double ANIMATION_DISTANCE_SQR = 30.0D * 30.0D;
    private static final Map<UUID, Entry> INSTANCES = new HashMap<>();
    private static long generation;

    public CragRockSpiderRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.55F;
    }

    @Override
    public void render(CragRockSpiderEntity spider, float entityYaw, float partialTick, PoseStack poses,
                       MultiBufferSource buffers, int packedLight) {
        UUID id = spider.getUUID();
        Entry entry = INSTANCES.computeIfAbsent(id, ignored -> new Entry());
        if ((entry.instance == null && !entry.loading && !entry.failed)
                || (entry.staticLod == null && !entry.loadingStatic && !entry.failedStatic)) load(spider, entry);
        var camera = Minecraft.getInstance().getCameraEntity();
        double distanceSqr = camera == null ? 0.0D : spider.distanceToSqr(camera);
        GltfInstance renderInstance = camera == null || distanceSqr <= ANIMATION_DISTANCE_SQR
                ? entry.instance : entry.staticLod;
        if (renderInstance != null) {
            if (renderInstance == entry.instance) updateAnimation(spider, entry);
            poses.pushPose();
            try {
                poses.mulPose(Axis.YP.rotationDegrees(-entityYaw));
                poses.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
                poses.translate(0.0D, MODEL_VERTICAL_OFFSET, 0.0D);
                GltfRenderer.render(renderInstance, poses, buffers, packedLight,
                        net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
            } finally {
                poses.popPose();
            }
        }
        super.render(spider, entityYaw, partialTick, poses, buffers, packedLight);
    }

    private static void updateAnimation(CragRockSpiderEntity spider, Entry entry) {
        long now = System.nanoTime();
        boolean paused = Minecraft.getInstance().isPaused();
        double delta = BeastAnimationClock.deltaSeconds(entry.lastUpdateNanos, now, paused, entry.wasPaused);
        entry.wasPaused = paused;
        entry.lastUpdateNanos = now;
        String requested = animationFor(spider);
        if (!requested.equals(entry.animation)) {
            if (!entry.instance.asset().animationNames().contains(requested)) {
                LOGGER.warn("[Xiantu GLB] Missing Crag-Rock Spider animation {} in {}", requested, MODEL);
                requested = "lie_yan_zhu.idle";
            }
            boolean loop = requested.endsWith(".idle") || requested.endsWith(".walk") || requested.endsWith(".run");
            entry.instance.animations().play(requested,
                    new PlaybackOptions(1.0F, loop ? LoopMode.LOOP : LoopMode.ONCE,
                            loop ? 0.12D : 0.06D, 0.0D));
            entry.animation = requested;
        }
        entry.instance.update(delta);
    }

    private static String animationFor(CragRockSpiderEntity spider) {
        if (spider.isDeadOrDying()) return "lie_yan_zhu.death";
        if (spider.hurtTime > 0) return "lie_yan_zhu.hurt";
        if (spider.isAttacking()) return "lie_yan_zhu.attack";
        if (spider.isRunning()) return "lie_yan_zhu.run";
        if (spider.getDeltaMovement().horizontalDistanceSqr() > 0.0004D) return "lie_yan_zhu.walk";
        return "lie_yan_zhu.idle";
    }

    @Override
    public ResourceLocation getTextureLocation(CragRockSpiderEntity spider) {
        return MODEL;
    }

    private static void load(CragRockSpiderEntity spider, Entry entry) {
        long requestedGeneration = generation;
        UUID id = spider.getUUID();
        if (entry.instance == null && !entry.loading && !entry.failed) {
            entry.loading = true;
            ClientGltfApi.models().load(MODEL).whenComplete((asset, error) ->
                    Minecraft.getInstance().execute(() -> {
                        if (generation != requestedGeneration || INSTANCES.get(id) != entry || spider.isRemoved()) return;
                        entry.loading = false;
                        if (error != null) {
                            if (error instanceof CancellationException || error.getCause() instanceof CancellationException) return;
                            entry.failed = true;
                            LOGGER.error("[Xiantu GLB] Failed to load Crag-Rock Spider model {}", MODEL, error);
                            return;
                        }
                        entry.instance = new GltfInstance(asset);
                        entry.instance.renderOptions().frustumCulling(true);
                        entry.instance.animations().play("lie_yan_zhu.idle", PlaybackOptions.loop());
                        entry.animation = "lie_yan_zhu.idle";
                        entry.lastUpdateNanos = System.nanoTime();
                        LOGGER.info("[Xiantu GLB] Loaded animated Crag-Rock Spider: {}, skins={}, animations={}",
                                MODEL, asset.skins().size(), asset.animationNames());
                    }));
        }
        if (entry.staticLod == null && !entry.loadingStatic && !entry.failedStatic) {
            entry.loadingStatic = true;
            ClientGltfApi.models().load(STATIC_LOD).whenComplete((asset, error) ->
                    Minecraft.getInstance().execute(() -> {
                        if (generation != requestedGeneration || INSTANCES.get(id) != entry || spider.isRemoved()) return;
                        entry.loadingStatic = false;
                        if (error != null) {
                            if (error instanceof CancellationException || error.getCause() instanceof CancellationException) return;
                            entry.failedStatic = true;
                            LOGGER.error("[Xiantu GLB] Failed to load Crag-Rock Spider static LOD {}", STATIC_LOD, error);
                            return;
                        }
                        entry.staticLod = new GltfInstance(asset);
                        entry.staticLod.renderOptions().frustumCulling(true);
                        LOGGER.info("[Xiantu GLB] Loaded Crag-Rock Spider static LOD: {}, skins={}, animations={}",
                                STATIC_LOD, asset.skins().size(), asset.animationNames());
                    }));
        }
    }

    public static SimplePreparableReloadListener<Void> reloadListener() {
        return new SimplePreparableReloadListener<>() {
            @Override protected Void prepare(ResourceManager manager, ProfilerFiller profiler) { return null; }
            @Override protected void apply(Void ignored, ResourceManager manager, ProfilerFiller profiler) { clearInstances(); }
        };
    }

    private static void close(UUID id) {
        Entry entry = INSTANCES.remove(id);
        if (entry != null) {
            if (entry.instance != null) entry.instance.close();
            if (entry.staticLod != null) entry.staticLod.close();
        }
    }

    private static void clearInstances() {
        generation++;
        INSTANCES.values().forEach(entry -> {
            if (entry.instance != null) entry.instance.close();
            if (entry.staticLod != null) entry.staticLod.close();
        });
        INSTANCES.clear();
    }

    @Mod.EventBusSubscriber(modid = XiantuMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static final class Lifecycle {
        @SubscribeEvent public static void onEntityLeave(EntityLeaveLevelEvent event) {
            if (event.getEntity() instanceof CragRockSpiderEntity spider) {
                Minecraft.getInstance().execute(() -> close(spider.getUUID()));
            }
        }
        @SubscribeEvent public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) { clearInstances(); }
    }

    private static final class Entry {
        private GltfInstance instance;
        private GltfInstance staticLod;
        private boolean loading, failed, loadingStatic, failedStatic;
        private String animation;
        private long lastUpdateNanos;
        private boolean wasPaused;
    }
}

