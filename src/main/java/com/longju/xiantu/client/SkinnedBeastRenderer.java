package com.longju.xiantu.client;

import com.longju.xiantu.XiantuMod;
import net.minecraft.world.entity.Mob;
import java.util.function.Function;
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

/** GFBS renderer with an independent animated instance per creature and a static distant LOD. */
public final class SkinnedBeastRenderer<T extends Mob> extends EntityRenderer<T> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final ResourceLocation model;
    private final ResourceLocation staticLod;
    private final String prefix;
    private final Function<T, String> animationState;
    private final float modelScale;
    private final float verticalOffset;
    private static final double ANIMATION_DISTANCE_SQR = 30.0D * 30.0D;
    private static final Map<UUID, Entry> INSTANCES = new HashMap<>();
    private static long generation;

    public SkinnedBeastRenderer(EntityRendererProvider.Context context, String prefix,
                                     float modelScale, float verticalOffset, float shadow, Function<T, String> animationState) {
        super(context);
        this.prefix = prefix;
        this.animationState = animationState;
        this.modelScale = modelScale;
        this.verticalOffset = verticalOffset;
        model = new ResourceLocation(XiantuMod.MOD_ID, "models/entity/" + prefix + ".glb");
        staticLod = new ResourceLocation(XiantuMod.MOD_ID, "models/entity/" + prefix + "_lod.glb");
        shadowRadius = shadow;
    }

    @Override
    public void render(T wolf, float entityYaw, float partialTick, PoseStack poses,
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
                if (wolf instanceof com.longju.xiantu.entity.CompletionFlyingEntity
                        || wolf instanceof com.longju.xiantu.entity.ThunderFeatherRavenEntity) {
                    float pitch = net.minecraft.util.Mth.rotLerp(partialTick, wolf.xRotO, wolf.getXRot());
                    poses.mulPose(Axis.XP.rotationDegrees(pitch));
                }
                poses.scale(modelScale, modelScale, modelScale);
                poses.translate(0.0D, verticalOffset, 0.0D);
                GltfRenderer.render(renderInstance, poses, buffers, packedLight,
                        net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
            } finally {
                poses.popPose();
            }
        }
        renderSpiritEchoes(wolf,entry,entityYaw,partialTick,poses,buffers,packedLight,distanceSqr);
        super.render(wolf, entityYaw, partialTick, poses, buffers, packedLight);
    }

    /** Reuse the existing immutable distant-pose asset for non-attacking spirit decoys.
     * Three brief translucent draws, no extra animated instance, AI or hitbox. */
    private void renderSpiritEchoes(T entity,Entry entry,float yaw,float partialTick,PoseStack poses,
                                   MultiBufferSource buffers,int light,double distanceSqr){
        if(entry.staticLod==null||distanceSqr>30*30||!(entity instanceof com.longju.xiantu.entity.CompletionLandEntity boss)||!boss.boss())return;
        var state=boss.bossVisual();if(state.isEmpty()||state.getBoolean("cancelled"))return;
        var skill=com.longju.xiantu.entity.BossCombatSkills.find(boss.definitionId(),state.getString("skill"));
        if(skill==null||!skill.shape().equals("illusion"))return;
        double t=entity.level().getGameTime()+partialTick-state.getLong("start")-skill.windup();
        if(t<0||t>skill.active())return;
        float alpha=(float)(.30*Math.min(1,t/5)*Math.min(1,(skill.active()-t)/7));
        var options=entry.staticLod.renderOptions();float oldAlpha=options.alpha();var oldType=options.overrideRenderType();var oldLight=options.lightMode();
        try{
            options.alpha(alpha).light(org.lytharalab.gfbs.gltf.api.client.GltfRenderOptions.LightMode.FULLBRIGHT)
                .renderType(BossEchoRenderType.ECHO);
            double x=net.minecraft.util.Mth.lerp(partialTick,entity.xOld,entity.getX()),z=net.minecraft.util.Mth.lerp(partialTick,entity.zOld,entity.getZ());
            for(int i=0;i<3;i++){
                double angle=i*Math.PI*2/3;poses.pushPose();
                try{
                    poses.translate(state.getDouble("originX")-x+Math.sin(angle)*3,.06*Math.sin(t*.15+i),state.getDouble("originZ")-z+Math.cos(angle)*3);
                    poses.mulPose(Axis.YP.rotationDegrees(-yaw));poses.scale(modelScale,modelScale,modelScale);poses.translate(0,verticalOffset,0);
                    GltfRenderer.render(entry.staticLod,poses,buffers,light,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
                }finally{poses.popPose();}
            }
        }finally{options.alpha(oldAlpha).light(oldLight).renderType(oldType);}
    }

    private void updateAnimation(T wolf, Entry entry) {
        long now = System.nanoTime();
        boolean paused = Minecraft.getInstance().isPaused();
        double delta = BeastAnimationClock.deltaSeconds(entry.lastUpdateNanos, now, paused, entry.wasPaused);
        entry.wasPaused = paused;
        entry.lastUpdateNanos = now;
        String requested = animationFor(wolf);
        int sequence=wolf instanceof com.longju.xiantu.entity.CompletionLandEntity boss ? boss.bossVisual().getInt("sequence") : 0;
        if (!requested.equals(entry.animation) || sequence!=entry.bossSequence) {
            if (!entry.instance.asset().animationNames().contains(requested)) {
                LOGGER.warn("[Xiantu GLB] Missing quadruped animation {} in {}", requested, model);
                requested = prefix + ".idle";
            }
            boolean loop = requested.endsWith(".idle") || requested.endsWith(".walk") || requested.endsWith(".run")
                    || requested.endsWith(".fly") || requested.endsWith(".glide") || requested.endsWith(".swim");
            double initialTime=0;
            if(wolf instanceof com.longju.xiantu.entity.CompletionLandEntity boss && boss.boss() && boss.hasCombatAction() && !boss.isDeadOrDying())
                initialTime=Math.max(0,(wolf.level().getGameTime()-boss.bossVisual().getLong(requested.endsWith(".counter_hit")?"counterStart":"start"))/20.0);
            entry.instance.animations().play(requested, new PlaybackOptions(requested.endsWith(".death") ? 2.0F : 1.0F,
                    loop ? LoopMode.LOOP : LoopMode.ONCE, loop ? 0.12D : 0.06D, initialTime));
            entry.animation = requested;
            entry.bossSequence=sequence;
        }
        // Planted stance occupies 62% of each authored gait cycle. Match its
        // backward foot travel to actual client displacement without restarting clips.
        if (prefix.equals("xing_yun_tian_jiang") || prefix.equals("tong_ling_yuan_wang")) {
            float speed=requested.endsWith(".death")?2F:1F;
            if(requested.endsWith(".walk")||requested.endsWith(".run")) {
                boolean running=requested.endsWith(".run");
                double motion=wolf.getDeltaMovement().horizontalDistance()*20;
                double distance=(running?.38:.24)*modelScale/.62;
                speed=(float)net.minecraft.util.Mth.clamp(motion*(running?22.0/24:32.0/24)/distance,.45,2.6);
            }
            float blend=(float)(1-Math.exp(-delta*12));
            entry.playbackSpeed+=(speed-entry.playbackSpeed)*blend;
            entry.instance.animations().setSpeed(entry.playbackSpeed);
        }
        entry.instance.update(delta);
    }

    private String animationFor(T beast) {
        if (beast.isDeadOrDying()) return prefix + ".death";
        if (beast instanceof com.longju.xiantu.entity.CompletionLandEntity boss
                && boss.definitionId().equals("meteor_general") && boss.animationState().equals("blink_pierce"))
            return prefix+".star_javelin";
        // A non-interrupted server attack must not disappear under a hurt animation while
        // its damage timeline continues. Short hurt reaction remains available at rest.
        if (beast.hurtTime > 0 && !(beast instanceof com.longju.xiantu.entity.CompletionLandEntity b && b.boss() && b.hasCombatAction())) return prefix + ".hurt";
        if(beast.getPersistentData().getBoolean("xiantu_qitian_visual_proxy")){
            boolean moving=beast.getDeltaMovement().horizontalDistanceSqr()>.0004;
            if(beast instanceof com.longju.xiantu.entity.CompletionFlyingEntity||beast instanceof com.longju.xiantu.entity.ThunderFeatherRavenEntity)
                return prefix+"."+(moving?"fly":"idle");
        }
        return prefix + "." + animationState.apply(beast);
    }

    @Override public ResourceLocation getTextureLocation(T wolf) { return model; }

    private void load(T wolf, Entry entry) {
        long requestedGeneration = generation;
        UUID id = wolf.getUUID();
        if (entry.instance == null && !entry.loading && !entry.failed) {
            entry.loading = true;
            ClientGltfApi.models().load(model).whenComplete((asset, error) -> Minecraft.getInstance().execute(() -> {
                if (generation != requestedGeneration || INSTANCES.get(id) != entry || wolf.isRemoved()) return;
                entry.loading = false;
                if (error != null) {
                    if (error instanceof CancellationException || error.getCause() instanceof CancellationException) return;
                    entry.failed = true;
                    LOGGER.error("[Xiantu GLB] Failed to load quadruped model {}", model, error);
                    return;
                }
                entry.instance = new GltfInstance(asset);
                entry.instance.renderOptions().frustumCulling(true);
                entry.instance.animations().play(prefix + ".idle", PlaybackOptions.loop());
                entry.animation = prefix + ".idle";
                entry.lastUpdateNanos = System.nanoTime();
                LOGGER.info("[Xiantu GLB] Loaded animated quadruped: {}, skins={}, animations={}",
                        model, asset.skins().size(), asset.animationNames());
            }));
        }
        if (entry.staticLod == null && !entry.loadingStatic && !entry.failedStatic) {
            entry.loadingStatic = true;
            ClientGltfApi.models().load(staticLod).whenComplete((asset, error) -> Minecraft.getInstance().execute(() -> {
                if (generation != requestedGeneration || INSTANCES.get(id) != entry || wolf.isRemoved()) return;
                entry.loadingStatic = false;
                if (error != null) {
                    if (error instanceof CancellationException || error.getCause() instanceof CancellationException) return;
                    entry.failedStatic = true;
                    LOGGER.error("[Xiantu GLB] Failed to load quadruped static LOD {}", staticLod, error);
                    return;
                }
                entry.staticLod = new GltfInstance(asset);
                entry.staticLod.renderOptions().frustumCulling(true);
                LOGGER.info("[Xiantu GLB] Loaded quadruped static LOD: {}, skins={}, animations={}",
                        staticLod, asset.skins().size(), asset.animationNames());
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
            if (event.getLevel().isClientSide() && INSTANCES.containsKey(event.getEntity().getUUID())) {
                UUID id = event.getEntity().getUUID();
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
        private int bossSequence;
        private float playbackSpeed=1;
    }
}
