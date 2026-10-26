package com.longju.xiantu.qa;

import com.longju.xiantu.client.FormationGatheringRenderer;
import com.longju.xiantu.formation.FormationCoreBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Read-only integrated-client diagnostics; never belongs in the production jar. */
@Mod.EventBusSubscriber(modid="xiantu_qa",value=Dist.CLIENT)
public final class FormationFlowAudit {
    @SubscribeEvent public static void commands(RegisterCommandsEvent event){
        event.getDispatcher().register(Commands.literal("xiantuformationflowaudit").requires(s->s.hasPermission(2)).executes(c->{
            var server=c.getSource().getServer();
            if(!server.getWorldPath(LevelResource.ROOT).normalize().getFileName().toString().equals("Xiantu-QA-BeastFlat-20261017"))return 0;
            Minecraft.getInstance().execute(()->{
                try{
                    var mc=Minecraft.getInstance();var p=new BlockPos(7040,-61,1408);
                    var state=mc.level.getBlockState(p);var be=mc.level.getBlockEntity(p);
                    var f=FormationGatheringRenderer.class.getDeclaredField("ARRAYS");f.setAccessible(true);
                    var arrays=(java.util.Map<?,?>)f.get(null);
                    String result="state="+state+" be="+be+" entries="+arrays.size()+" gameTime="+mc.level.getGameTime()+" distance="+mc.player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(p));
                    for(var e:arrays.entrySet()){
                        var last=e.getValue().getClass().getDeclaredField("last");last.setAccessible(true);
                        result+=" entry="+e.getKey()+" last="+last.getLong(e.getValue());
                    }
                    java.nio.file.Files.writeString(mc.gameDirectory.toPath().resolve("formation-flow-audit.txt"),result);
                    org.slf4j.LoggerFactory.getLogger(FormationFlowAudit.class).info("[Formation flow QA] {}",result);
                }catch(Exception ex){throw new RuntimeException(ex);}
            });return 1;
        }));
    }
}
