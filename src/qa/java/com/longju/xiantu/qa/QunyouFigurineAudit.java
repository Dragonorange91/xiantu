package com.longju.xiantu.qa;

import com.longju.xiantu.registry.ModBlocks;
import com.longju.xiantu.building.QunyouFigurineEntity;
import net.minecraft.commands.Commands;
import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

@Mod.EventBusSubscriber(modid="xiantu_qa")
public final class QunyouFigurineAudit {
    private static int checks;
    private static void check(boolean pass,String label){checks++;if(!pass)throw new IllegalStateException(label);}
    @SubscribeEvent public static void commands(RegisterCommandsEvent event){
        event.getDispatcher().register(Commands.literal("xiantufigurineaudit").executes(c->{
            var source=c.getSource();var level=source.getLevel();
            if(!level.getServer().getWorldPath(LevelResource.ROOT).normalize().getFileName().toString().equals("Xiantu-QA-BeastFlat-20261017")){
                source.sendFailure(Component.literal("Figurine audit requires isolated BeastFlat QA world"));return 0;
            }
            var fake=FakePlayerFactory.getMinecraft(level);
            net.minecraft.world.entity.Entity actor=fake;
            net.minecraft.world.entity.player.Player owner=fake;
            var inventory=owner.getInventory().save(new net.minecraft.nbt.ListTag());
            var mode=fake.gameMode.getGameModeForPlayer();var oldPos=actor.position();float oldYaw=actor.getYRot();
            var base=source.getPlayerOrException().blockPosition().offset(0,14,0);
            var bounds=new AABB(base.offset(-3,-2,-3),base.offset(4,3,4));
            var before=new LinkedHashMap<BlockPos,BlockState>();var oldItems=new HashSet<UUID>();
            level.getEntitiesOfClass(ItemEntity.class,bounds).forEach(e->oldItems.add(e.getUUID()));
            checks=0;
            try{
                for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)for(int y=-1;y<=1;y++){
                    var at=base.offset(x,y,z);check(level.getBlockEntity(at)==null&&level.getBlockState(at).isAir(),"unoccupied aerial fixture");
                    before.put(at,level.getBlockState(at));
                }
                var block=ModBlocks.QUNYOU_FIGURINE.get();var item=block.asItem();
                for(Direction facing:Direction.Plane.HORIZONTAL){
                    level.setBlockAndUpdate(base.below(),Blocks.DARK_OAK_SLAB.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.SLAB_TYPE,net.minecraft.world.level.block.state.properties.SlabType.TOP));
                    actor.setPos(base.getX()+.5+facing.getStepX()*2,base.getY(),base.getZ()+.5+facing.getStepZ()*2);
                    actor.setYRot(facing.getOpposite().toYRot());fake.setGameMode(GameType.SURVIVAL);
                    var stack=new ItemStack(item,2);owner.setItemInHand(InteractionHand.MAIN_HAND,stack);
                    var hit=new BlockHitResult(Vec3.atCenterOf(base.below()).add(0,.5,0),Direction.UP,base.below(),false);
                    check(item.useOn(new UseOnContext(fake,InteractionHand.MAIN_HAND,hit)).consumesAction(),"real BlockItem placement "+facing);
                    check(stack.getCount()==1,"one item consumed");
                    var state=level.getBlockState(base);
                    check(state.is(block)&&state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)==facing,"facing "+facing);
                    check(level.getBlockEntity(base) instanceof QunyouFigurineEntity,"block entity created");
                    check(!state.getCollisionShape(level,base).isEmpty(),"physical shape");
                    var drops=Block.getDrops(state,level,base,level.getBlockEntity(base));
                    check(drops.size()==1&&drops.get(0).is(item)&&drops.get(0).getCount()==1,"self loot");
                    check(fake.gameMode.destroyBlock(base),"survival break");
                    check(level.getBlockState(base).isAir(),"removed by mining");
                    var spawned=level.getEntitiesOfClass(ItemEntity.class,bounds,e->!oldItems.contains(e.getUUID()));
                    check(spawned.stream().filter(e->e.getItem().is(item)).mapToInt(e->e.getItem().getCount()).sum()==1,"single mined drop");
                    spawned.forEach(ItemEntity::discard);
                    owner.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(item));
                    check(item.useOn(new UseOnContext(fake,InteractionHand.MAIN_HAND,hit)).consumesAction(),"re-place for support test");
                    level.destroyBlock(base.below(),false);
                    check(level.getBlockState(base).isAir(),"support removal clears ornament");
                    spawned=level.getEntitiesOfClass(ItemEntity.class,bounds,e->!oldItems.contains(e.getUUID()));
                    check(spawned.stream().filter(e->e.getItem().is(item)).mapToInt(e->e.getItem().getCount()).sum()==1,"single support-removal drop");
                    spawned.forEach(ItemEntity::discard);
                }
                var menu=new AbstractContainerMenu(null,-1){
                    @Override public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player p,int slot){return ItemStack.EMPTY;}
                    @Override public boolean stillValid(net.minecraft.world.entity.player.Player p){return true;}
                };
                var grid=new TransientCraftingContainer(menu,3,3);
                for(int slot:new int[]{0,4,8})grid.setItem(slot,new ItemStack(Items.PORKCHOP));
                var recipe=level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,grid,level).orElseThrow();
                check(recipe.getId().equals(new ResourceLocation("xiantu","qunyou_figurine")),"shapeless recipe resolves");
                var result=recipe.assemble(grid,level.registryAccess());
                check(result.is(item)&&result.getCount()==1,"three raw pork produce one figurine");
                grid.setItem(8,ItemStack.EMPTY);
                check(!recipe.matches(grid,level),"two pork rejected");
                grid.setItem(8,new ItemStack(Items.COOKED_PORKCHOP));
                check(!recipe.matches(grid,level),"cooked pork rejected");
                for(String name:new String[]{"cultivation_town","wanbao_pavilion","tiles/cell_02_0_01"}){
                    var template=level.getStructureManager().get(new ResourceLocation("xiantu","town/"+name)).orElseThrow();
                    check(!template.filterBlocks(BlockPos.ZERO,new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings(),block).isEmpty(),"town template figurine "+name);
                }
                source.sendSuccess(()->Component.literal("[Figurine QA] PASS "+checks+" checks"),false);
                System.out.println("[Figurine QA] PASS "+checks+" checks");return 1;
            }catch(Exception|LinkageError failure){
                failure.printStackTrace();source.sendFailure(Component.literal("[Figurine QA] FAIL "+failure));return 0;
            }finally{
                before.forEach((p,s)->level.setBlock(p,s,3));
                level.getEntitiesOfClass(ItemEntity.class,bounds,e->!oldItems.contains(e.getUUID())).forEach(ItemEntity::discard);
                owner.getInventory().load(inventory);fake.setGameMode(mode);actor.setPos(oldPos);actor.setYRot(oldYaw);
            }
        }));
    }
}
