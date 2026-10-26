package com.longju.xiantu.client;

import com.longju.xiantu.XiantuMod;
import com.longju.xiantu.entity.YanweiFoxEntity;
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

/** Renders and animates the skinned Yanwei Fox GLB with one independent instance per entity. */
public final class YanweiFoxRenderer extends EntityRenderer<YanweiFoxEntity> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation MODEL = new ResourceLocation(XiantuMod.MOD_ID, "models/entity/yanwei_fox.glb");
    private static final ResourceLocation STATIC_LOD = new ResourceLocation(XiantuMod.MOD_ID, "models/entity/yanwei_fox_lod.glb");
    // The 2026-10-16 GLB is 0.674805 units tall; match the 1.35-block entity height.
    // Both animated and static LOD instances share this transform.
    private static final float MODEL_SCALE = 2.0F;
    private static final double ANIMATION_DISTANCE_SQR = 30.0D * 30.0D;
    private static final Map<UUID, Entry> INSTANCES = new HashMap<>();
    private static long generation;

    public YanweiFoxRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.42F;
    }

    @Override
    public void render(YanweiFoxEntity fox, float entityYaw, float partialTick, PoseStack poses,
                       MultiBufferSource buffers, int packedLight) {
        UUID id = fox.getUUID();
        Entry entry = INSTANCES.get(id);
        if (entry == null) {
            entry = new Entry();
            INSTANCES.put(id, entry);
        }
        if ((entry.instance == null && !entry.loading && !entry.failed)
                || (entry.staticLod == null && !entry.loadingStatic && !entry.failedStatic)) load(fox, entry);

        var camera = Minecraft.getInstance().getCameraEntity();
        double distanceSqr = camera == null ? 0.0D : fox.distanceToSqr(camera);
        GltfInstance renderInstance;
        boolean animate = camera == null || distanceSqr <= ANIMATION_DISTANCE_SQR;
        renderInstance = animate ? entry.instance : entry.staticLod;
        if (renderInstance != null) {
            if (renderInstance == entry.instance) updateAnimation(fox, entry);
            poses.pushPose();
            try {
                poses.mulPose(Axis.YP.rotationDegrees(-entityYaw));
                poses.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
                GltfRenderer.render(renderInstance, poses, buffers, packedLight, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
            } finally {
                poses.popPose();
            }
        }
        super.render(fox, entityYaw, partialTick, poses, buffers, packedLight);
    }

    private static void updateAnimation(YanweiFoxEntity fox, Entry entry) {
        long now = System.nanoTime();
        boolean paused = Minecraft.getInstance().isPaused();
        double deltaSeconds = BeastAnimationClock.deltaSeconds(entry.lastUpdateNanos, now, paused, entry.wasPaused);
        entry.wasPaused = paused;
        entry.lastUpdateNanos = now;

        String requested = animationFor(fox);
        GltfInstance instance = entry.instance;
        String activeAnimation = entry.animation;
        if (!requested.equals(activeAnimation)) {
            if (!instance.asset().animationNames().contains(requested)) {
                LOGGER.warn("[Xiantu GLB] Missing Yanwei Fox animation {} in {}", requested, MODEL);
                requested = "yanwei_fox.idle";
            }
            boolean loop = requested.endsWith(".idle") || requested.endsWith(".walk")
                    || requested.endsWith(".run") || requested.endsWith(".alert");
            float playbackSpeed = requested.endsWith(".hurt") ? 1.2F
                    : requested.endsWith(".death") ? 1.25F : 1.0F;
            PlaybackOptions options = new PlaybackOptions(playbackSpeed, loop ? LoopMode.LOOP : LoopMode.ONCE,
                    loop ? 0.12D : 0.06D, 0.0D);
            instance.animations().play(requested, options);
            entry.animation = requested;
        }
        instance.update(deltaSeconds);
    }

    private static String animationFor(YanweiFoxEntity fox) {
        if (fox.isDeadOrDying()) return "yanwei_fox.death";
        if (fox.hurtTime > 0) return "yanwei_fox.hurt";
        if (fox.isAttacking()) return "yanwei_fox.attack";
        if (fox.isRunning()) return "yanwei_fox.run";
        if (fox.getDeltaMovement().horizontalDistanceSqr() > 0.0004D) return "yanwei_fox.walk";
        if (fox.isAlerted()) return "yanwei_fox.alert";
        return "yanwei_fox.idle";
    }

    @Override
    public ResourceLocation getTextureLocation(YanweiFoxEntity fox) {
        return MODEL;
    }

    private static void load(YanweiFoxEntity fox, Entry entry) {
        long requestedGeneration = generation;
        UUID id = fox.getUUID();
        if (entry.instance == null && !entry.loading && !entry.failed) {
            entry.loading = true;
            ClientGltfApi.models().load(MODEL).whenComplete((asset, error) -> Minecraft.getInstance().execute(() -> {
            if (generation != requestedGeneration || INSTANCES.get(id) != entry || fox.isRemoved()) return;
            entry.loading = false;
            if (error != null) {
                if (error instanceof CancellationException || error.getCause() instanceof CancellationException) return;
                entry.failed = true;
                LOGGER.error("[Xiantu GLB] Failed to load static Yanwei Fox model {}", MODEL, error);
                return;
            }
            entry.instance = new GltfInstance(asset);
            entry.instance.renderOptions().frustumCulling(true);
            entry.instance.animations().play("yanwei_fox.idle", PlaybackOptions.loop());
            entry.animation = "yanwei_fox.idle";
            entry.lastUpdateNanos = System.nanoTime();
            if (!entry.logged) {
                entry.logged = true;
                LOGGER.info("[Xiantu GLB] Loaded animated Yanwei Fox: {}, skins={}, animations={}",
                        MODEL, asset.skins().size(), asset.animationNames());
            }
            }));
        }
        if (entry.staticLod == null && !entry.loadingStatic && !entry.failedStatic) {
            entry.loadingStatic = true;
            ClientGltfApi.models().load(STATIC_LOD).whenComplete((asset, error) -> Minecraft.getInstance().execute(() -> {
                if (generation != requestedGeneration || INSTANCES.get(id) != entry || fox.isRemoved()) return;
                entry.loadingStatic = false;
                if (error != null) {
                    if (error instanceof CancellationException || error.getCause() instanceof CancellationException) return;
                    entry.failedStatic = true;
                    LOGGER.error("[Xiantu GLB] Failed to load static Yanwei Fox LOD {}", STATIC_LOD, error);
                    return;
                }
                entry.staticLod = new GltfInstance(asset);
                entry.staticLod.renderOptions().frustumCulling(true);
                LOGGER.info("[Xiantu GLB] Loaded static Yanwei Fox LOD: {}, skins={}, animations={}",
                        STATIC_LOD, asset.skins().size(), asset.animationNames());
            }));
        }
    }

    public static SimplePreparableReloadListener<Void> reloadListener() {
        return new SimplePreparableReloadListener<>() {
            @Override protected Void prepare(ResourceManager manager, ProfilerFiller profiler) { return null; }
            @Override protected void apply(Void ignored, ResourceManager manager, ProfilerFiller profiler) {
                clearInstances();
                LOGGER.info("[Xiantu GLB] Cleared Yanwei Fox instances for resource reload; generation {}", generation);
            }
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
            if (event.getEntity() instanceof YanweiFoxEntity fox) {
                UUID id = fox.getUUID();
                Minecraft.getInstance().execute(() -> close(id));
            }
        }

        @SubscribeEvent public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
            clearInstances();
        }
    }

    private static final class Entry {
        private GltfInstance instance;
        private GltfInstance staticLod;
        private boolean loading;
        private boolean failed;
        private boolean loadingStatic;
        private boolean failedStatic;
        private boolean logged;
        private String animation;
        private long lastUpdateNanos;
        private boolean wasPaused;
        private Entry() {}
    }
}
