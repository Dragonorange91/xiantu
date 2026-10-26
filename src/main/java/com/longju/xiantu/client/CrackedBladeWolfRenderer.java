package com.longju.xiantu.client;

import com.longju.xiantu.XiantuMod;
import com.longju.xiantu.entity.CrackedBladeWolfEntity;
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
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import org.lytharalab.gfbs.gltf.api.animation.LoopMode;
import org.lytharalab.gfbs.gltf.api.animation.PlaybackOptions;
import org.lytharalab.gfbs.gltf.api.client.ClientGltfApi;
import org.lytharalab.gfbs.gltf.api.client.GltfInstance;
import org.lytharalab.gfbs.gltf.api.client.GltfRenderer;
import org.lytharalab.gfbs.gltf.api.model.GltfAsset;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CancellationException;

/** GFBS renderer with an independent animated instance per wolf and a static distant LOD. */
public final class CrackedBladeWolfRenderer extends EntityRenderer<CrackedBladeWolfEntity> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation MODEL = new ResourceLocation(XiantuMod.MOD_ID, "models/entity/lie_ren_lang.glb");
    private static final ResourceLocation STATIC_LOD = new ResourceLocation(XiantuMod.MOD_ID, "models/entity/lie_ren_lang_lod.glb");
    private static final float MODEL_SCALE = 1.7F;
    private static final float MODEL_VERTICAL_OFFSET = 0.43F;
    private static final double ANIMATION_DISTANCE_SQR = 30.0D * 30.0D;
    private static final Map<UUID, Entry> INSTANCES = new HashMap<>();
    private static long generation;

    public CrackedBladeWolfRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.48F;
    }

    @Override
    public void render(CrackedBladeWolfEntity wolf, float entityYaw, float partialTick, PoseStack poses,
                       MultiBufferSource buffers, int packedLight) {
        UUID id = wolf.getUUID();
        Entry entry = INSTANCES.computeIfAbsent(id, ignored -> new Entry());
        if ((entry.instance == null && !entry.loading && !entry.failed)
                || (entry.staticLod == null && !entry.loadingStatic && !entry.failedStatic)) load(wolf, entry);

        var camera = Minecraft.getInstance().getCameraEntity();
        double distanceSqr = camera == null ? 0.0D : wolf.distanceToSqr(camera);
        GltfInstance renderInstance = camera == null || distanceSqr <= ANIMATION_DISTANCE_SQR
                ? entry.instance : entry.staticLod;
        if (renderInstance != null) {
            if (renderInstance == entry.instance) updateAnimation(wolf, entry);
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
        super.render(wolf, entityYaw, partialTick, poses, buffers, packedLight);
    }

    private static void updateAnimation(CrackedBladeWolfEntity wolf, Entry entry) {
        long now = System.nanoTime();
        boolean paused = Minecraft.getInstance().isPaused();
        double delta = BeastAnimationClock.deltaSeconds(entry.lastUpdateNanos, now, paused, entry.wasPaused);
        entry.wasPaused = paused;
        entry.lastUpdateNanos = now;
        String requested = animationFor(wolf);
        if (!requested.equals(entry.animation)) {
            if (!entry.instance.asset().animationNames().contains(requested)) {
                LOGGER.warn("[Xiantu GLB] Missing Cracked-Blade Wolf animation {} in {}", requested, MODEL);
                requested = "lie_ren_lang.idle";
            }
            boolean loop = requested.endsWith(".idle") || requested.endsWith(".walk") || requested.endsWith(".run");
            entry.instance.animations().play(requested, new PlaybackOptions(1.0F,
                    loop ? LoopMode.LOOP : LoopMode.ONCE, loop ? 0.12D : 0.06D, 0.0D));
            entry.animation = requested;
        }
        entry.instance.update(delta);
    }

    private static String animationFor(CrackedBladeWolfEntity wolf) {
        if (wolf.isDeadOrDying()) return "lie_ren_lang.death";
        if (wolf.hurtTime > 0) return "lie_ren_lang.hurt";
        if (wolf.isAttacking()) return "lie_ren_lang.attack";
        if (wolf.isRunning()) return "lie_ren_lang.run";
        if (wolf.getDeltaMovement().horizontalDistanceSqr() > 0.0004D) return "lie_ren_lang.walk";
        return "lie_ren_lang.idle";
    }

    @Override public ResourceLocation getTextureLocation(CrackedBladeWolfEntity wolf) { return MODEL; }

    private static void load(CrackedBladeWolfEntity wolf, Entry entry) {
        long requestedGeneration = generation;
        UUID id = wolf.getUUID();
        if (entry.instance == null && !entry.loading && !entry.failed) {
            entry.loading = true;
            ClientGltfApi.models().load(MODEL).whenComplete((asset, error) -> Minecraft.getInstance().execute(() -> {
                if (generation != requestedGeneration || INSTANCES.get(id) != entry || wolf.isRemoved()) return;
                entry.loading = false;
                if (error != null) {
                    if (error instanceof CancellationException || error.getCause() instanceof CancellationException) return;
                    entry.failed = true;
                    LOGGER.error("[Xiantu GLB] Failed to load Cracked-Blade Wolf model {}", MODEL, error);
                    return;
                }
                entry.instance = new GltfInstance(asset);
                entry.instance.renderOptions().frustumCulling(true);
                entry.instance.animations().play("lie_ren_lang.idle", PlaybackOptions.loop());
                entry.animation = "lie_ren_lang.idle";
                entry.lastUpdateNanos = System.nanoTime();
                LOGGER.info("[Xiantu GLB] Loaded animated Cracked-Blade Wolf: {}, skins={}, animations={}",
                        MODEL, asset.skins().size(), asset.animationNames());
            }));
        }
        if (entry.staticLod == null && !entry.loadingStatic && !entry.failedStatic) {
            entry.loadingStatic = true;
            ClientGltfApi.models().load(STATIC_LOD).whenComplete((asset, error) -> Minecraft.getInstance().execute(() -> {
                if (generation != requestedGeneration || INSTANCES.get(id) != entry || wolf.isRemoved()) return;
                entry.loadingStatic = false;
                if (error != null) {
                    if (error instanceof CancellationException || error.getCause() instanceof CancellationException) return;
                    entry.failedStatic = true;
                    LOGGER.error("[Xiantu GLB] Failed to load Cracked-Blade Wolf static LOD {}", STATIC_LOD, error);
                    return;
                }
                entry.staticLod = new GltfInstance(asset);
                entry.staticLod.renderOptions().frustumCulling(true);
                LOGGER.info("[Xiantu GLB] Loaded Cracked-Blade Wolf static LOD: {}, skins={}, animations={}",
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
            if (event.getEntity() instanceof CrackedBladeWolfEntity wolf) {
                UUID id = wolf.getUUID();
                Minecraft.getInstance().execute(() -> close(id));
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
