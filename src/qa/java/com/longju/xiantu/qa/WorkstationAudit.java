package com.longju.xiantu.qa;

import com.longju.xiantu.workstation.*;
import net.minecraft.commands.Commands;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

public final class WorkstationAudit {
    private static int checks;
    private static void require(boolean pass,String label){checks++;if(!pass)throw new IllegalStateException(label);}
    public static void commands(RegisterCommandsEvent e){
        System.out.println("[Workstation QA] Registering workstation audit");
        e.getDispatcher().register(Commands.literal("xiantuworkstationaudit").requires(s->s.hasPermission(2)).executes(c->{
            try {
            var source=c.getSource();var level=source.getLevel();
            String worldName=level.getServer().getWorldPath(LevelResource.ROOT).normalize().getFileName().toString();
            if(!worldName.startsWith("Xiantu-QA")||worldName.startsWith("Xiantu-QA-Ruins-")){
                source.sendFailure(Component.literal("Workstation QA requires Xiantu-QA world: "+level.getServer().getWorldPath(LevelResource.ROOT)));return 0;
            }
            source.sendSuccess(()->Component.literal("Starting workstation audit"),false);
            checks=0;var before=new LinkedHashMap<BlockPos,BlockState>();
            net.minecraft.server.level.ServerPlayer player=FakePlayerFactory.getMinecraft(level);
            net.minecraft.world.entity.player.Player inventoryOwner=player;
            var inventory=inventoryOwner.getInventory().save(new net.minecraft.nbt.ListTag());
            var mode=player.gameMode.getGameModeForPlayer();var oldpos=((net.minecraft.world.entity.Entity)player).position();float yaw=((net.minecraft.world.entity.Entity)player).getYRot();
            BlockPos base=source.getPlayerOrException().blockPosition().offset(0,12,16);
            AABB area=new AABB(base.offset(-6,-2,-6),base.offset(7,4,7));
            Set<UUID> oldItems=new HashSet<>();level.getEntitiesOfClass(ItemEntity.class,area).forEach(i->oldItems.add(i.getUUID()));
            try{
                for(int x=-5;x<=5;x++)for(int y=-1;y<=2;y++)for(int z=-5;z<=5;z++){
                    BlockPos p=base.offset(x,y,z);require(level.getBlockEntity(p)==null,"fixture has no block entities");before.put(p,level.getBlockState(p));
                    level.setBlock(p,y==-1?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
                }
                var tested=new LinkedHashMap<String,WorkstationBlock>();
                WorkstationRegistry.EQUIPMENT.forEach((id,b)->tested.put(id,(WorkstationBlock)b.get()));
                tested.put("equipment_repair_table",(WorkstationBlock)com.longju.xiantu.registry.ModBlocks.EQUIPMENT_REPAIR.get());
                tested.put("ancient_burial_coffin",(WorkstationBlock)net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(new net.minecraft.resources.ResourceLocation("xiantu","ancient_burial_coffin")));
                for(var entry:tested.entrySet()){
                    var block=entry.getValue();Item item=((Block)block).asItem();
                    for(Direction f:Direction.Plane.HORIZONTAL){
                        ((net.minecraft.world.entity.Entity)player).setPos(base.getX()+f.getStepX()*6+.5,base.getY(),base.getZ()+f.getStepZ()*6+.5);
                        player.setYRot(f.getOpposite().toYRot());
                        player.setGameMode(GameType.SURVIVAL);
                        for(var broken:block.partProperty().getPossibleValues()){
                            var stack=new ItemStack(item,2);player.setItemInHand(InteractionHand.MAIN_HAND,stack);
                            var hit=new BlockHitResult(Vec3.atCenterOf(base.below()).add(0,.5,0),Direction.UP,base.below(),false);
                            require(item.useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,hit)).consumesAction(),"placement "+entry.getKey()+" "+f);
                            require(stack.getCount()==1,"one item consumed");
                            for(var part:block.partProperty().getPossibleValues()){
                                var pos=block.position(base,part,f);var state=level.getBlockState(pos);
                                require(state.is(block)&&state.getValue(block.partProperty())==part&&state.getValue(HorizontalDirectionalBlock.FACING)==f,"correct part state");
                                require(block.anchor(pos,state).equals(base),"part finds master");
                                require(!state.getShape(level,pos).isEmpty(),"selection shape exists");
                                require(Block.getDrops(state,level,pos,null).size()==(part==WorkstationBlock.Part.P000?1:0),"master-only loot");
                            }
                            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.DIAMOND_PICKAXE));
                            require(player.gameMode.destroyBlock(block.position(base,broken,f)),"break part "+broken);
                            for(var part:block.partProperty().getPossibleValues())require(level.getBlockState(block.position(base,part,f)).isAir(),"all parts removed");
                            var drops=level.getEntitiesOfClass(ItemEntity.class,area,i->!oldItems.contains(i.getUUID()));
                            require(drops.stream().filter(i->i.getItem().is(item)).mapToInt(i->i.getItem().getCount()).sum()==1,"exactly one device drop");
                            drops.forEach(ItemEntity::discard);
                        }
                        // Every cell obstructed in turn must reject without consuming or partially placing.
                        for(var blocked:block.partProperty().getPossibleValues()){
                            var pos=block.position(base,blocked,f);level.setBlock(pos,Blocks.STONE.defaultBlockState(),3);
                            var stack=new ItemStack(item,2);player.setItemInHand(InteractionHand.MAIN_HAND,stack);
                            var hit=new BlockHitResult(Vec3.atCenterOf(base.below()).add(0,.5,0),Direction.UP,base.below(),false);
                            require(!item.useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,hit)).consumesAction(),"obstructed placement refused");
                            require(stack.getCount()==2,"refusal free of charge");
                            for(var part:block.partProperty().getPossibleValues())require(!level.getBlockState(block.position(base,part,f)).is(block),"no partial placement");
                            level.setBlock(pos,Blocks.AIR.defaultBlockState(),3);
                        }
                    }
                }
                source.sendSuccess(()->Component.literal("Workstations PASS: "+checks+" checks"),false);
                java.nio.file.Files.writeString(net.minecraftforge.fml.loading.FMLPaths.GAMEDIR.get().resolve("workstation-audit.json"),"{\"passed\":true,\"checks\":"+checks+"}");
                return checks;
            }catch(Throwable ex){
                java.nio.file.Files.writeString(net.minecraftforge.fml.loading.FMLPaths.GAMEDIR.get().resolve("workstation-audit.json"),"FAILED after "+checks+": "+ex);
                source.sendFailure(Component.literal("Workstation audit FAILED: "+ex));ex.printStackTrace();return 0;
            }
            finally{
                before.forEach((p,s)->level.setBlock(p,Blocks.AIR.defaultBlockState(),2));before.forEach((p,s)->level.setBlock(p,s,3));
                level.getEntitiesOfClass(ItemEntity.class,area,i->!oldItems.contains(i.getUUID())).forEach(ItemEntity::discard);
                inventoryOwner.getInventory().load(inventory);player.setGameMode(mode);((net.minecraft.world.entity.Entity)player).setPos(oldpos);player.setYRot(yaw);
            }
            } catch(Throwable ex) {
                try{java.nio.file.Files.writeString(net.minecraftforge.fml.loading.FMLPaths.GAMEDIR.get().resolve("workstation-audit.json"),"FAILED initialization: "+ex);}catch(Exception ignored){}
                c.getSource().sendFailure(Component.literal("Workstation audit initialization FAILED: "+ex));ex.printStackTrace();return 0;
            }
        }));
    }
}
