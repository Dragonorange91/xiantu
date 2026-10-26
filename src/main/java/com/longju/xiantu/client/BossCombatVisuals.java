package com.longju.xiantu.client;

import com.longju.xiantu.entity.*;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Server-clock spear paths and targetable astral cores, independent of optional Photon. */
@Mod.EventBusSubscriber(modid="xiantu",value=Dist.CLIENT)
public final class BossCombatVisuals {
    private static final MultiBufferSource.BufferSource ART=MultiBufferSource.immediate(new BufferBuilder(32768));
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        var mc=Minecraft.getInstance();if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_PARTICLES||mc.level==null||mc.player==null)return;
        var camera=event.getCamera().getPosition();var stack=event.getPoseStack();
        var artMatrix=new org.joml.Matrix4f(stack.last().pose());
        stack.pushPose();stack.translate(-camera.x,-camera.y,-camera.z);
        var v=mc.renderBuffers().bufferSource().getBuffer(Types.ASTRAL);
        for(var entity:mc.level.entitiesForRendering()){
            if(!(entity instanceof CompletionLandEntity boss)||!boss.definitionId().equals("meteor_general")||!boss.isAlive()||boss.distanceToSqr(camera)>64*64)continue;
            var n=boss.bossVisual();if(n.isEmpty()||n.getBoolean("cancelled"))continue;
            var skill=BossCombatSkills.find(boss.definitionId(),n.getString("skill"));if(skill==null)continue;
            double age=mc.level.getGameTime()+event.getPartialTick()-n.getLong("start");if(age<0||age>=skill.duration())continue;
            BossSpellGeometry.meteor(v,stack.last().pose(),camera,boss,skill,n,age,event.getPartialTick());
            if(java.util.Set.of("attack","blink_pierce","star_javelin","star_track").contains(skill.id())){
                double activeAge=age-skill.windup();
                if(activeAge<skill.active()){
                    Vec3 forward=new Vec3(n.getDouble("directionX"),n.getDouble("directionY"),n.getDouble("directionZ")).normalize();
                    if(forward.lengthSqr()<1e-6)continue;
                    Vec3 side=new Vec3(-forward.z,0,forward.x).normalize();
                    if(side.lengthSqr()<1e-8)side=new Vec3(1,0,0);
                    Vec3 up=forward.cross(side).normalize();
                    WeaponSkillArt.draw(ART,artMatrix,camera,activeAge<0?"charge":"spear",
                        boss.getPosition(event.getPartialTick()).add(0,1.5,0),forward,side,up,
                        activeAge<0?age:activeAge*24/Math.max(1,skill.active()),skill.reach(),0x9DDFFF);
                }
            }
        }
        stack.popPose();mc.renderBuffers().bufferSource().endBatch(Types.ASTRAL);ART.endBatch();
    }
    private static final class Types extends RenderType {
        static final RenderType ASTRAL=create("xiantu_astral_spear",DefaultVertexFormat.POSITION_COLOR,VertexFormat.Mode.QUADS,32768,false,true,
            CompositeState.builder().setShaderState(new ShaderStateShard(GameRenderer::getPositionColorShader))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY).setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(COLOR_WRITE)
                .setCullState(NO_CULL).setOutputState(PARTICLES_TARGET).createCompositeState(false));
        private Types(){super("xiantu_astral_spear",DefaultVertexFormat.POSITION_COLOR,VertexFormat.Mode.QUADS,256,false,false,()->{},()->{});}
    }
    private BossCombatVisuals(){}
}
