package com.longju.xiantu.client;

import com.longju.xiantu.XiantuMod;
import com.longju.xiantu.gongfa.GongfaItem;
import com.longju.xiantu.gongfa.GongfaRegistry;
import com.longju.xiantu.network.GongfaStudyPacket;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod;
import java.util.*;
import java.util.function.Consumer;

/** Five cuboids from Blockbench, transformed around the spine; no per-frame asset loading. */
public final class GongfaStudyRenderer extends BlockEntityWithoutLevelRenderer {
    private static final Map<UUID, Study> STUDIES = new HashMap<>();
    private static final Set<String> READY = new HashSet<>();
    private static final ThreadLocal<Animated> DRAW = new ThreadLocal<>();
    private static ClientLevel observedLevel;
    private static GongfaStudyRenderer singleton;
    private static final String[] GLYPHS = {"道", "玄", "氣", "神", "元", "靈", "悟", "真"};
    private static final class Study {
        final String method;
        long start, expires;
        Animated model;
        Study(String method) { this.method = method; }
    }
    private GongfaStudyRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }
    public static void install(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (singleton == null) singleton = new GongfaStudyRenderer();
                return singleton;
            }
        });
    }
    public static void receive(GongfaStudyPacket p) {
        ClientLevel level = Minecraft.getInstance().level;
        resetLevel(level);
        if (level == null || p.method().isEmpty() || GongfaRegistry.get(p.method()) == null) {
            STUDIES.remove(p.player()); return;
        }
        Study study = STUDIES.compute(p.player(), (id, old) -> old != null && old.method.equals(p.method()) ? old : new Study(p.method()));
        study.start = level.getGameTime() - Math.max(0, Math.min(200, p.ticks()));
        study.expires = level.getGameTime() + 60; // tolerate short server stalls between 20-tick heartbeats
    }
    private static void resetLevel(ClientLevel level) {
        if (observedLevel != level) { STUDIES.clear(); DRAW.remove(); observedLevel = level; }
    }
    private static Study study(LivingEntity owner, ItemStack stack) {
        ClientLevel level = Minecraft.getInstance().level;
        resetLevel(level);
        if (level == null || owner == null || !owner.isAlive() || !owner.isUsingItem()
                || !(owner.getVehicle() instanceof com.longju.xiantu.formation.FormationSeatEntity)
                || !(stack.getItem() instanceof GongfaItem book)
                || !ItemStack.matches(owner.getUseItem(), stack)) return null;
        Study study = STUDIES.get(owner.getUUID());
        return study != null && study.expires >= level.getGameTime() && study.method.equals(book.definitionId()) ? study : null;
    }
    private static ResourceLocation part(String method, String side) {
        return new ResourceLocation("xiantu", "item/gongfa_study/" + method + "_" + side);
    }
    private static class Wrapped implements BakedModel {
        final BakedModel closed;
        Wrapped(BakedModel closed) { this.closed = closed; }
        @Override public List<BakedQuad> getQuads(BlockState s, Direction d, RandomSource r) { return closed.getQuads(s,d,r); }
        @Override public boolean useAmbientOcclusion() { return closed.useAmbientOcclusion(); }
        @Override public boolean isGui3d() { return closed.isGui3d(); }
        @Override public boolean usesBlockLight() { return closed.usesBlockLight(); }
        @Override public boolean isCustomRenderer() { return false; }
        @Override public TextureAtlasSprite getParticleIcon() { return closed.getParticleIcon(); }
        @Override public ItemTransforms getTransforms() { return closed.getTransforms(); }
        @Override public ItemOverrides getOverrides() { return closed.getOverrides(); }
    }
    private static final class Book extends Wrapped {
        private final ItemOverrides overrides = new ItemOverrides() {
            @Override public BakedModel resolve(BakedModel model, ItemStack stack, ClientLevel level, LivingEntity owner, int seed) {
                Study study = study(owner, stack);
                if (study == null || !ready(study.method)) return closed;
                if (study.model == null || study.model.closed != closed)
                    study.model = new Animated(closed, owner.getUUID());
                return study.model;
            }
        };
        Book(BakedModel closed) { super(closed); }
        @Override public ItemOverrides getOverrides() { return overrides; }
    }
    private static final class Animated extends Wrapped {
        final UUID owner;
        Animated(BakedModel closed, UUID owner) { super(closed); this.owner = owner; }
        @Override public boolean isCustomRenderer() { return true; }
        @Override public BakedModel applyTransform(ItemDisplayContext context, PoseStack poses, boolean left) {
            if (context == ItemDisplayContext.GUI || context == ItemDisplayContext.GROUND
                    || context == ItemDisplayContext.FIXED || context == ItemDisplayContext.NONE) {
                DRAW.remove(); return closed.applyTransform(context, poses, left);
            }
            DRAW.set(this);
            if (context.firstPerson()) {
                // Vanilla hand placement already adds +/-0.56 horizontally and -0.52 vertically.
                // Undo its horizontal offset so both use hands put the book at screen center.
                poses.translate(left ? .56 : -.56, .28, -.10);
                // With the Y180 reading-facing turn, negative X tilts the page normal upward.
                // Lower the spread below the eyeline rather than presenting its underside.
                poses.mulPose(Axis.XP.rotationDegrees(-40));
                poses.mulPose(Axis.YP.rotationDegrees(180));
                poses.scale(.65f,.65f,.65f);
            } else {
                poses.translate(0,.15,0);
                poses.mulPose(Axis.XP.rotationDegrees(65));
                poses.scale(.75f,.75f,.75f);
            }
            return this;
        }
    }
    @Override public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poses,
                                      MultiBufferSource buffers, int light, int overlay) {
        Animated selected = DRAW.get(); DRAW.remove();
        Minecraft mc = Minecraft.getInstance();
        LivingEntity owner = selected == null || mc.level == null ? null : mc.level.getPlayerByUUID(selected.owner);
        Study study = study(owner, stack);
        if (study == null) {
            if (selected != null) renderPart(selected.closed, poses, buffers, light, overlay);
            return;
        }
        if(ready(study.method))renderSpread(study, poses, buffers, light, overlay, context.firstPerson());
        else if(selected!=null)renderPart(selected.closed,poses,buffers,light,overlay);
    }
    private static void renderSpread(Study study, PoseStack poses, MultiBufferSource buffers,
                                     int light, int overlay, boolean firstPerson) {
        Minecraft mc = Minecraft.getInstance();
        float time = (float)(mc.level.getGameTime() - study.start) + mc.getFrameTime();
        float angle = 85 - 73 * Math.min(1, time / 10);
        drawWing(part(study.method,"left"), -angle, poses,buffers,light,overlay);
        drawWing(part(study.method,"right"), angle, poses,buffers,light,overlay);
        if (time >= 10) {
            float turn = ((time - 10) % 24) / 24;
            drawWing(new ResourceLocation("xiantu","item/gongfa_study/template_leaf"), 12 + 156 * turn, poses,buffers,light,overlay);
            glyphs(time, poses, buffers, firstPerson);
        }
    }
    private static boolean ready(String method) {
        return StudyPageGeometry.ready(method);
    }
    public static boolean validPart(BakedModel model,BakedModel missing) {
        if(model==null||model==missing)return false;
        boolean faces=false;
        var random=RandomSource.create(0);
        for(int d=-1;d<6;d++)for(var quad:model.getQuads(null,d<0?null:Direction.values()[d],random)) {
            faces=true;
            if(quad.getSprite().contents().name().equals(net.minecraft.client.renderer.texture.MissingTextureAtlasSprite.getLocation()))return false;
        }
        return faces;
    }
    private static void drawWing(ResourceLocation model, float angle, PoseStack poses, MultiBufferSource buffers, int light, int overlay) {
        poses.pushPose();
        poses.translate(.5,.5,.5); poses.mulPose(Axis.YP.rotationDegrees(angle)); poses.translate(-.5,-.5,-.5);
        StudyPageGeometry.draw(model.getPath().substring("item/gongfa_study/".length()),poses,buffers,light,overlay);
        poses.popPose();
    }
    private static void renderPart(BakedModel model, PoseStack poses, MultiBufferSource buffers, int light, int overlay) {
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(poses.last(),
                buffers.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS)),null,model,1,1,1,light,overlay);
    }
    private static void glyphs(float time, PoseStack poses, MultiBufferSource buffers, boolean firstPerson) {
        Minecraft mc = Minecraft.getInstance();
        // Rising words belong to the visible reading surface, facing the reader with the open book.
        for (int i=0;i<GLYPHS.length;i++) {
            float age = (time + i * 6) % 48 / 48;
            int alpha = (int)(190 * Math.sin(age * Math.PI));
            if (alpha < 12) continue;
            poses.pushPose();
            poses.translate(.18 + (i % 4) * .2 + Math.sin(age*4+i)*.035, .68 + age*.45, .36-(i/4)*.07);
            // Book's reading-facing turn must not mirror the Chinese glyphs.
            if (firstPerson) poses.mulPose(Axis.YP.rotationDegrees(180));
            poses.scale(.014f,-.014f,.014f);
            mc.font.drawInBatch(GLYPHS[i],-mc.font.width(GLYPHS[i])/2f,0,(alpha<<24)|0xE6C77B,false,poses.last().pose(),buffers,Font.DisplayMode.NORMAL,0,0xF000F0);
            poses.popPose();
        }
    }
    public static final class Models {
        public static void additional(ModelEvent.RegisterAdditional event) {
            event.register(new ResourceLocation("xiantu","item/gongfa_study/template_leaf"));
            for (var method : GongfaRegistry.ALL) for (String side : List.of("left","right")) event.register(part(method.id(),side));
            com.mojang.logging.LogUtils.getLogger().info("[Xiantu study] Registered {} additional page models",GongfaRegistry.ALL.size()*2+1);
        }
        public static void baked(ModelEvent.ModifyBakingResult event) {
            READY.clear();
            int count=0;
            for (var method : GongfaRegistry.ALL) {
                ModelResourceLocation id = new ModelResourceLocation("xiantu",method.id(),"inventory");
                BakedModel model = event.getModels().get(id);
                if (model != null && !(model instanceof Book)) {event.getModels().put(id,new Book(model));count++;}
            }
            com.mojang.logging.LogUtils.getLogger().info("[Xiantu study] Wrapped {} book models",count);
            STUDIES.values().forEach(s->s.model=null);
            DRAW.remove();
        }
        public static void completed(ModelEvent.BakingCompleted event) {
            READY.clear();
            var models=event.getModels();var missing=event.getModelManager().getMissingModel();
            var leaf=models.get(new ResourceLocation("xiantu","item/gongfa_study/template_leaf"));
            for(var method:GongfaRegistry.ALL) {
                boolean valid=validPart(leaf,missing)
                    &&validPart(models.get(part(method.id(),"left")),missing)
                    &&validPart(models.get(part(method.id(),"right")),missing);
                if(valid)READY.add(method.id());
                else com.mojang.logging.LogUtils.getLogger().error("[Xiantu study] Invalid baked page or missing texture for {}; retaining closed book",method.id());
            }
            com.mojang.logging.LogUtils.getLogger().info("[Xiantu study] Verified {}/{} animated books with real page textures",READY.size(),GongfaRegistry.ALL.size());
        }
    }
    @Mod.EventBusSubscriber(modid=XiantuMod.MOD_ID,value=Dist.CLIENT)
    public static final class Lifecycle {
        /** Render the local study directly: held-model caching/override order must not hide the spread. */
        @SubscribeEvent(priority=EventPriority.LOW) public static void hand(RenderHandEvent event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || event.getHand() != mc.player.getUsedItemHand()) return;
            Study active = study(mc.player, event.getItemStack());
            if (active == null || !ready(active.method)) return;
            boolean left = (event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND
                    ? mc.player.getMainArm() : mc.player.getMainArm().getOpposite()) == net.minecraft.world.entity.HumanoidArm.LEFT;
            var poses = event.getPoseStack();
            poses.pushPose();
            // Reproduce vanilla hand placement followed by the accepted centered reading pose.
            poses.translate(left ? -.56 : .56, -.52 - .6 * event.getEquipProgress(), -.72);
            poses.translate(left ? .56 : -.56, .28, -.10);
            poses.mulPose(Axis.XP.rotationDegrees(-40));
            poses.mulPose(Axis.YP.rotationDegrees(180));
            poses.scale(.65f,.65f,.65f);
            poses.translate(-.5,-.5,-.5);
            renderSpread(active, poses, event.getMultiBufferSource(), event.getPackedLight(),
                    net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, true);
            poses.popPose();
            DRAW.remove();
            event.setCanceled(true);
        }
        @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
            if (event.phase != TickEvent.Phase.END) return;
            ClientLevel level = Minecraft.getInstance().level;
            resetLevel(level);
            if (level != null) STUDIES.values().removeIf(s->s.expires < level.getGameTime());
        }
    }
}
