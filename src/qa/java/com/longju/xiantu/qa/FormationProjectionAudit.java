package com.longju.xiantu.qa;
import com.longju.xiantu.formation.*;
import com.longju.xiantu.registry.ModItems;
import net.minecraft.commands.Commands;
import net.minecraft.core.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.*;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;
@Mod.EventBusSubscriber(modid="xiantu_qa")
public final class FormationProjectionAudit {
    private static BlockPos reject;private static int checks;
    private static void check(boolean b,String text){checks++;if(!b)throw new IllegalStateException(text);}
    @SubscribeEvent public static void cancel(net.minecraftforge.event.level.BlockEvent.EntityPlaceEvent e){if(reject!=null&&reject.equals(e.getPos()))e.setCanceled(true);}
    @SubscribeEvent public static void commands(RegisterCommandsEvent e){e.getDispatcher().register(Commands.literal("xiantuformationaudit").requires(s->s.hasPermission(2)).executes(c->{
        var p=c.getSource().getPlayerOrException();var l=p.serverLevel();
        if(!p.server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).normalize().getFileName().toString().startsWith("Xiantu-QA"))return 0;
        var inventory=p.getInventory().save(new net.minecraft.nbt.ListTag());var position=p.position();float yaw=p.getYRot(),pitch=p.getXRot();checks=0;
        var center=new BlockPos(10000,-59,10000);var old=new LinkedHashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
        try{
            for(int x=-12;x<=12;x++)for(int z=-12;z<=12;z++)for(int y=-1;y<=5;y++){
                var at=center.offset(x,y,z);check(l.getBlockEntity(at)==null,"isolated empty fixture");old.put(at,l.getBlockState(at));l.setBlock(at,(y==-1?Blocks.STONE:Blocks.AIR).defaultBlockState(),3);
            }
            p.teleportTo(l,10011.5,-59,10000.5,90,0);p.getInventory().clearContent();
            for(int tier=1;tier<=3;tier++){
                int t=tier;var plan=FormationPlan.blocks(tier);var needs=FormationPlan.materials(tier);
                check(!FormationProjection.place(p,tier,center).isEmpty(),"missing materials rejected");
                check(plan.keySet().stream().allMatch(v->l.getBlockState(center.offset(v)).isAir()),"missing materials does not mutate world");
                needs.forEach((item,count)->{int left=count;while(left>0){int n=Math.min(64,left);check(p.getInventory().add(new ItemStack(item,n)),"fixture materials fit");left-=n;}});
                var before=p.getInventory().save(new net.minecraft.nbt.ListTag());
                var obstruction=center.offset(plan.keySet().stream().skip(5).findFirst().orElseThrow());l.setBlock(obstruction,Blocks.DIAMOND_BLOCK.defaultBlockState(),3);
                check(!FormationProjection.place(p,tier,center).isEmpty(),"occupied block rejected");check(before.equals(p.getInventory().save(new net.minecraft.nbt.ListTag())),"occupied block never consumes materials");l.setBlock(obstruction,Blocks.AIR.defaultBlockState(),3);
                reject=obstruction;check(!FormationProjection.place(p,tier,center).isEmpty(),"protection hook veto rejected");reject=null;
                check(before.equals(p.getInventory().save(new net.minecraft.nbt.ListTag())),"cancelled placement keeps inventory");check(plan.keySet().stream().allMatch(v->l.getBlockState(center.offset(v)).isAir()),"cancelled placement rolls back earlier cells");
                check(FormationProjection.place(p,tier,center).isEmpty(),"valid plan placed "+tier);check(FormationStructure.isComplete(l,center,tier),"tier validates "+tier);
                check(p.getInventory().items.stream().allMatch(ItemStack::isEmpty),"exact bill deducted once");
                for(var cell:plan.keySet())l.setBlock(center.offset(cell),Blocks.AIR.defaultBlockState(),3);
            }
            var seal=com.longju.xiantu.registry.ModBlocks.RUIN_ENCOUNTER.get().defaultBlockState();check(seal.getCollisionShape(l,center).isEmpty(),"encounter has no collision");check(seal.getShape(l,center).isEmpty(),"encounter has no selection outline");check(seal.getRenderShape()==net.minecraft.world.level.block.RenderShape.INVISIBLE,"encounter invisible");
            var state=com.longju.xiantu.trade.TownJourney.state(p);boolean issued=state.getBoolean("ManualIssued");state.remove("ManualIssued");
            com.longju.xiantu.trade.TownJourney.issueManual(p);com.longju.xiantu.trade.TownJourney.issueManual(p);check(p.getInventory().countItem(ModItems.CULTIVATION_MANUAL.get())==1,"starter manual exactly once");state.putBoolean("ManualIssued",issued);
            var result="Formation projection audit PASS: "+checks+" checks";try{java.nio.file.Files.writeString(p.server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("formation-projection-audit.txt"),result);}catch(java.io.IOException ex){throw new java.io.UncheckedIOException(ex);}c.getSource().sendSuccess(()->net.minecraft.network.chat.Component.literal(result),false);return 1;
        }finally{reject=null;for(var entry:old.entrySet())l.setBlock(entry.getKey(),entry.getValue(),3);p.getInventory().load(inventory);p.inventoryMenu.broadcastChanges();p.teleportTo(l,position.x,position.y,position.z,yaw,pitch);}
    }));}
}
