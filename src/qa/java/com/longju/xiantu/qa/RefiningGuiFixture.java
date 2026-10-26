package com.longju.xiantu.qa;
import com.longju.xiantu.workstation.*;
import com.longju.xiantu.refining.*;
import net.minecraft.commands.Commands;
import net.minecraft.core.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="xiantu_qa")
public final class RefiningGuiFixture {
    @SubscribeEvent public static void register(RegisterCommandsEvent e){e.getDispatcher().register(Commands.literal("xiantuforgegui").requires(s->s.hasPermission(2)).executes(ctx->{
        var p=ctx.getSource().getPlayerOrException();var l=p.serverLevel();
        if(!p.server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).normalize().getFileName().toString().startsWith("Xiantu-QA"))return 0;
        var at=new BlockPos(35000,260,35000);
        for(int x=-1;x<=5;x++)for(int z=-1;z<=5;z++)for(int y=0;y<=3;y++){
            var s=l.getBlockState(at.offset(x,y,z));if(!s.isAir() && !(s.getBlock() instanceof WorkstationBlock))throw new IllegalStateException("Occupied fixture area");
        }
        for(int x=-1;x<=5;x++)for(int z=-1;z<=5;z++)l.setBlock(at.offset(x,-1,z),Blocks.STONE_BRICKS.defaultBlockState(),3);
        var block=(WorkstationBlock)WorkstationRegistry.EQUIPMENT.get("lifeng_refining_furnace").get();var state=((Block)block).defaultBlockState();l.setBlock(at,state,3);((Block)block).setPlacedBy(l,at,state,p,ItemStack.EMPTY);
        var table=(WorkstationBlock)WorkstationRegistry.EQUIPMENT.get("artisan_forging_table").get();var tp=at.offset(3,0,0);l.setBlock(tp,((Block)table).defaultBlockState(),3);((Block)table).setPlacedBy(l,tp,((Block)table).defaultBlockState(),p,ItemStack.EMPTY);
        p.teleportTo(at.getX()+1,at.getY(),at.getZ()+3.7);p.setYRot(180);p.setXRot(20);
        var be=(RefiningBlockEntity)l.getBlockEntity(at);net.minecraftforge.network.NetworkHooks.openScreen(p,be,b->{b.writeBlockPos(at);b.writeBoolean(false);b.writeBoolean(false);});return 1;
    }));}
}
