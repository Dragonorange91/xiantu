package com.longju.xiantu.qa;

import com.google.gson.*;
import com.longju.xiantu.workstation.WorkstationBlock;
import net.minecraft.commands.Commands;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/** Complete native multicell creation, rotated offsets, support and dismantling. */
@Mod.EventBusSubscriber(modid="xiantu_qa")
public final class FurnitureAudit {
    private static int checks;
    private static void check(boolean ok,String why){checks++;if(!ok)throw new IllegalStateException(why);}
    @SubscribeEvent public static void register(RegisterCommandsEvent e){e.getDispatcher().register(Commands.literal("xiantufurnitureaudit").requires(s->s.hasPermission(2)).executes(c->{
        ServerPlayer p=c.getSource().getPlayerOrException();
        if(!p.server.getWorldPath(LevelResource.ROOT).normalize().getFileName().toString().startsWith("Xiantu-QA-Ruins-Rework")){c.getSource().sendFailure(Component.literal("仅允许本次全新遗迹重做测试存档"));return 0;}
        JsonObject report=new JsonObject();checks=0;
        try(var reader=p.server.getResourceManager().getResource(new ResourceLocation("xiantu","xian_furniture.json")).orElseThrow().openAsReader()){
            var models=JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("models");int n=0;
            for(var v:models){String id=v.getAsJsonObject().get("id").getAsString();Block block=ForgeRegistries.BLOCKS.getValue(new ResourceLocation("xiantu",id));check(block!=null&&!block.defaultBlockState().isAir(),id+" registered");
                BlockPos at=new BlockPos(n++*6,-59,1000);var l=p.serverLevel();l.getChunk(at);
                for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++){l.setBlock(at.offset(x,-1,z),Blocks.SMOOTH_STONE.defaultBlockState(),3);for(int y=0;y<4;y++)l.setBlock(at.offset(x,y,z),Blocks.AIR.defaultBlockState(),3);}
                for(Direction facing:Direction.Plane.HORIZONTAL){
                    BlockState state=block.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,facing);l.setBlock(at,state,3);
                    if(block instanceof WorkstationBlock multi){
                        block.setPlacedBy(l,at,state,p,new ItemStack(block));block.tick(state,l,at,l.random);
                        for(var part:multi.partProperty().getPossibleValues()){
                            BlockPos pos=multi.position(at,part,facing);BlockState actual=l.getBlockState(pos);check(actual.is(block),id+" complete cell "+part+" "+facing);check(actual.getValue(multi.partProperty())==part,id+" part identity");check(multi.anchor(pos,actual).equals(at),id+" rotated anchor");
                        }
                        var last=multi.partProperty().getPossibleValues().stream().filter(t->t!=WorkstationBlock.Part.P000).findFirst().orElseThrow();
                        l.destroyBlock(multi.position(at,last,facing),false);
                        for(var part:multi.partProperty().getPossibleValues())check(l.getBlockState(multi.position(at,part,facing)).isAir(),id+" secondary break removes whole assembly");
                    }else {check(!state.getShape(l,at).isEmpty(),id+" static shape");l.destroyBlock(at,false);}
                }
                BlockState display=block.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.NORTH);l.setBlock(at,display,3);if(block instanceof WorkstationBlock)block.setPlacedBy(l,at,display,p,new ItemStack(block));
            }
            report.addProperty("passed",true);report.addProperty("models",models.size());report.addProperty("checks",checks);report.addProperty("showcase","x=0..60 y=-59 z=1000");c.getSource().sendSuccess(()->Component.literal("家具实测 PASS："+checks+" 项；展示位 0 -59 1000"),false);
        }catch(Throwable ex){report.addProperty("passed",false);report.addProperty("error",ex.toString());c.getSource().sendFailure(Component.literal("家具实测失败："+ex));org.slf4j.LoggerFactory.getLogger(FurnitureAudit.class).error("Furniture audit",ex);}
        try{var file=net.minecraftforge.fml.loading.FMLPaths.GAMEDIR.get().resolve("xiantu-qa/furniture-reference-20261024.json");java.nio.file.Files.createDirectories(file.getParent());java.nio.file.Files.writeString(file,report.toString());}catch(java.io.IOException ex){throw new java.io.UncheckedIOException(ex);}return report.get("passed").getAsBoolean()?1:0;
    }));}
    private FurnitureAudit(){}
}
