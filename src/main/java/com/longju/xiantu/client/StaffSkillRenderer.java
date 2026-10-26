package com.longju.xiantu.client;

import com.longju.xiantu.network.SkillVfxPacket;
import com.longju.xiantu.refining.RefinedEquipment;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import java.util.*;

/** Server-triggered solid staff motion, independent of optional Photon. Uses the actual accepted mesh/UV. */
@Mod.EventBusSubscriber(modid="xiantu",value=Dist.CLIENT)
public final class StaffSkillRenderer {
    private record Cast(UUID owner,long start,String skill,Vec3 from,Vec3 displacement){}
    private static final Map<UUID,Cast> CASTS=new HashMap<>();
    private static ClientLevel world;
    private static void checkWorld(){var current=Minecraft.getInstance().level;if(current!=world){CASTS.clear();world=current;}}
    public static void clear(){CASTS.clear();world=null;}
    public static void receive(SkillVfxPacket p){
        checkWorld();if(world==null||!world.dimension().location().equals(p.dimension())||!Set.of("staff_extend","staff_quake").contains(p.skill())||!Double.isFinite(p.position().lengthSqr())||!Double.isFinite(p.direction().lengthSqr()))return;
        var entity=world.getEntity(p.entityId());if(entity==null||!entity.getUUID().equals(p.owner())||!(entity instanceof LivingEntity))return;
        if(p.phase().equals("CANCEL")||p.phase().equals("END")){CASTS.remove(p.owner());return;}
        if(p.phase().equals("RELEASE")&&(CASTS.containsKey(p.owner())||CASTS.size()<32)){CASTS.put(p.owner(),new Cast(p.owner(),world.getGameTime(),p.skill(),p.position(),p.direction()));SkillCastAnimations.pulse(p.owner(),p.skill());}
    }
    public static boolean active(UUID owner){checkWorld();return CASTS.containsKey(owner);}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){if(e.phase!=TickEvent.Phase.END)return;checkWorld();if(world==null)return;
        CASTS.values().removeIf(c->{var p=world.getPlayerByUUID(c.owner);var r=p==null?null:RefinedEquipment.recipe(p.getMainHandItem());return world.getGameTime()-c.start>=20||p==null||!p.isAlive()||p.position().distanceToSqr(c.from)>25||r==null||!r.id().equals("jingu_staff");});
    }
    @SubscribeEvent public static void hand(RenderHandEvent e){var p=Minecraft.getInstance().player;if(p!=null&&e.getHand()==net.minecraft.world.InteractionHand.MAIN_HAND&&active(p.getUUID()))e.setCanceled(true);}
    private static double smooth(double t){t=Math.max(0,Math.min(1,t));return t*t*(3-2*t);}
    @SubscribeEvent public static void render(RenderLevelStageEvent e){
        if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_ENTITIES)return;checkWorld();if(world==null||CASTS.isEmpty())return;
        var mc=Minecraft.getInstance();var camera=e.getCamera().getPosition();var poses=e.getPoseStack();var buffers=mc.renderBuffers().bufferSource();var type=RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS);
        for(var c:CASTS.values()){
            var owner=world.getPlayerByUUID(c.owner);if(owner==null||owner.distanceToSqr(camera)>96*96)continue;int light=net.minecraft.client.renderer.LevelRenderer.getLightColor(world,owner.blockPosition());
            double age=world.getGameTime()-c.start+e.getPartialTick();double length;Vec3 direction;
            if(c.skill.equals("staff_extend")){
                double reach=c.displacement.length();double k=age<=4?smooth(age/4):age<=7?1:1-smooth((age-7)/11);
                length=Math.max(.45,Math.min(reach,1.35+(reach-1.35)*k));direction=c.displacement.normalize();
            }else{
                double strike=smooth(age/7);double recover=smooth((age-10)/9);var forward=c.displacement.multiply(1,0,1).normalize();
                direction=forward.scale(Math.sin(strike*Math.PI*.63)).add(0,Math.cos(strike*Math.PI*.63),0).normalize();
                length=2.8-1.3*recover;
            }
            if(direction.lengthSqr()<.001)continue;
            poses.pushPose();poses.translate(c.from.x-camera.x,c.from.y-camera.y,c.from.z-camera.z);
            poses.mulPose(new org.joml.Quaternionf().rotationTo(new org.joml.Vector3f(0,1,0),new org.joml.Vector3f((float)direction.x,(float)direction.y,(float)direction.z)));
            // Accepted model spans -.85..1.85 along Y, with its axis at X=Z=.5.
            // The shaft stretches; the ornate ends translate rigidly to retain their proportions.
            poses.translate(-.5,0,-.5);float scale=(float)((length+.55)/2.7);
            poses.pushPose();poses.translate(0,-.55+.85*scale,0);poses.scale(1,scale,1);draw("shaft",poses,buffers,type,mc,light);poses.popPose();
            poses.pushPose();poses.translate(0,.30,0);draw("lower",poses,buffers,type,mc,light);poses.popPose();
            poses.pushPose();poses.translate(0,length-1.85,0);draw("upper",poses,buffers,type,mc,light);poses.popPose();
            poses.popPose();
        }
        buffers.endBatch(type);
    }
    private static void draw(String part,PoseStack pose,net.minecraft.client.renderer.MultiBufferSource buffers,RenderType type,Minecraft mc,int light){
        var model=mc.getModelManager().getModel(new ResourceLocation("xiantu","item/jingubang_cast_"+part));
        mc.getBlockRenderer().getModelRenderer().renderModel(pose.last(),buffers.getBuffer(type),null,model,1,1,1,light,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
    }
    /** Owner-local override suppresses only the ordinary held copy while its world-space mesh is visible. */
    private static final class Held implements BakedModel {
        final BakedModel original;final boolean hidden;
        Held(BakedModel original,boolean hidden){this.original=original;this.hidden=hidden;}
        public List<BakedQuad> getQuads(BlockState s,Direction d,RandomSource r){return hidden?List.of():original.getQuads(s,d,r);}
        public boolean useAmbientOcclusion(){return original.useAmbientOcclusion();}public boolean isGui3d(){return original.isGui3d();}
        public boolean usesBlockLight(){return original.usesBlockLight();}public boolean isCustomRenderer(){return false;}
        public TextureAtlasSprite getParticleIcon(){return original.getParticleIcon();}public ItemTransforms getTransforms(){return original.getTransforms();}
        public BakedModel applyTransform(net.minecraft.world.item.ItemDisplayContext context,PoseStack poses,boolean left){if(context==net.minecraft.world.item.ItemDisplayContext.GUI||context==net.minecraft.world.item.ItemDisplayContext.GROUND||context==net.minecraft.world.item.ItemDisplayContext.FIXED)return original.applyTransform(context,poses,left);original.applyTransform(context,poses,left);return this;}
        public ItemOverrides getOverrides(){return new ItemOverrides(){public BakedModel resolve(BakedModel model,ItemStack item,ClientLevel level,LivingEntity owner,int seed){return owner!=null&&active(owner.getUUID())?new Held(original,true):original;}};}
    }
    @Mod.EventBusSubscriber(modid="xiantu",value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static final class Models {
        @SubscribeEvent public static void additional(ModelEvent.RegisterAdditional e){for(String p:List.of("shaft","lower","upper"))e.register(new ResourceLocation("xiantu","item/jingubang_cast_"+p));}
        @SubscribeEvent public static void baked(ModelEvent.ModifyBakingResult e){clear();for(var entry:e.getModels().entrySet())if(entry.getKey().getNamespace().equals("xiantu")&&(entry.getKey().getPath().equals("jingu_staff")||entry.getKey().getPath().equals("preview_jingubang")))entry.setValue(new Held(entry.getValue(),false));}
    }
    private StaffSkillRenderer(){}
}
