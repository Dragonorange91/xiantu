package com.longju.xiantu.client;

import com.longju.xiantu.registry.ModItems;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** A small directional marker while actually holding the stone, not a world waypoint/entity. */
@Mod.EventBusSubscriber(modid="xiantu",value=Dist.CLIENT)
public final class GuidingStoneOverlay {
    @SubscribeEvent public static void render(RenderGuiEvent.Post event){
        Minecraft mc=Minecraft.getInstance();var p=mc.player;
        if(p==null||mc.options.hideGui||mc.screen!=null)return;
        var stone=p.getMainHandItem().is(ModItems.GUIDING_STONE.get())?p.getMainHandItem():p.getOffhandItem();
        if(!stone.is(ModItems.GUIDING_STONE.get()))return;
        var tag=stone.getTag();String text="正在感应修仙城镇…";
        int x=event.getGuiGraphics().guiWidth()/2,y=event.getGuiGraphics().guiHeight()-77;
        var g=event.getGuiGraphics();
        if(p.level().dimension()!=Level.OVERWORLD)text="请返回主世界继续引路";
        else if(tag!=null&&tag.contains("TownTarget")){
            BlockPos pos=BlockPos.of(tag.getLong("TownTarget"));double dx=pos.getX()+.5-p.getX(),dz=pos.getZ()+.5-p.getZ();
            float angle=(float)(p.getYRot()-Math.toDegrees(Math.atan2(-dx,dz)));
            g.pose().pushPose();g.pose().translate(x,y-10,0);g.pose().mulPose(Axis.ZP.rotationDegrees(angle));
            // Symmetric stepped arrow, native GUI geometry remains crisp at every GUI scale.
            for(int i=0;i<5;i++)g.fill(-i,-7+i,i+1,-6+i,0xFFF3D282);
            g.fill(-1,-2,2,6,0xFF8ED5C5);g.pose().popPose();
            text="修仙城镇 · "+Math.round(Math.hypot(dx,dz))+" 格";
        }else if(tag!=null&&tag.getString("SearchState").equals("unavailable"))text="未寻得城镇 · 右键查看";
        g.drawCenteredString(mc.font,text,x,y+2,0xFFE8D8B1);
    }
}
