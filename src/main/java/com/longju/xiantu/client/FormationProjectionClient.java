package com.longju.xiantu.client;
import com.longju.xiantu.formation.FormationPlan;
import com.longju.xiantu.network.FormationProjectionPacket;
import com.longju.xiantu.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.core.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid="xiantu",value=Dist.CLIENT)
public final class FormationProjectionClient {
    private static int tier;private static BlockPos locked;private static net.minecraft.client.multiplayer.ClientLevel world;
    private static java.util.Map<BlockPos,net.minecraft.world.level.block.state.BlockState> plan=java.util.Map.of();
    public static void receive(FormationProjectionPacket packet){tier=packet.tier()>=1&&packet.tier()<=3?packet.tier():0;locked=packet.locked();world=Minecraft.getInstance().level;plan=tier==0?java.util.Map.of():FormationPlan.blocks(tier);}
    public static boolean active(){return tier!=0&&world==Minecraft.getInstance().level;}
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e){tier=0;locked=null;world=null;plan=java.util.Map.of();}
    @SubscribeEvent public static void draw(RenderLevelStageEvent e){
        var mc=Minecraft.getInstance();if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS||!active()||mc.player==null||!mc.player.getMainHandItem().is(ModItems.CULTIVATION_MANUAL.get()))return;
        BlockPos center=locked;
        if(center==null){var hit=mc.player.pick(12,e.getPartialTick(),false);if(!(hit instanceof BlockHitResult h)||h.getType()!=HitResult.Type.BLOCK||h.getDirection()!=Direction.UP)return;center=h.getBlockPos().above();}
        var pose=e.getPoseStack();var camera=e.getCamera().getPosition();pose.pushPose();pose.translate(-camera.x,-camera.y,-camera.z);
        var buffers=mc.renderBuffers().bufferSource();var lines=buffers.getBuffer(RenderType.lines());
        for(var cell:plan.entrySet()){
            var pos=center.offset(cell.getKey());boolean free=world.getBlockState(pos).canBeReplaced()&&world.getFluidState(pos).isEmpty();
            var shape=cell.getValue().getShape(world,pos);for(var box:shape.toAabbs())LevelRenderer.renderLineBox(pose,lines,box.move(pos).inflate(.002),free?.2f:1f,free?.85f:.15f,free?1f:.15f,.8f);
        }
        buffers.endBatch(RenderType.lines());pose.popPose();
    }
    private FormationProjectionClient(){}
}
