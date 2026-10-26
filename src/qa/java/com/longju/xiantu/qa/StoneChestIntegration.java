package com.longju.xiantu.qa;

import com.longju.xiantu.registry.ModBlocks;
import com.longju.xiantu.registry.ModBlockEntities;
import com.longju.xiantu.building.StoneChestBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

/** Tests the real registered native container, not a stand-in inventory. */
public final class StoneChestIntegration {
    private static void check(ServerPlayer p, boolean ok, String label) {
        p.sendSystemMessage(Component.literal("[Stone chest QA "+(ok?"PASS":"FAIL")+"] "+label));
        if(!ok)throw new IllegalStateException(label);
    }
    public static void checkChest(ServerPlayer player,BlockPos origin,CraftingContainer grid) {
        var level=player.serverLevel();Block box=ModBlocks.BLACK_ROCK_CHEST.get();
        grid.clearContent();
        for(int slot:new int[]{0,2,6,8})grid.setItem(slot,new ItemStack(ModBlocks.BLACK_ROCK_CARVED.get()));
        for(int slot:new int[]{1,3,5,7})grid.setItem(slot,new ItemStack(ModBlocks.BLACK_ROCK_SMOOTH.get()));
        var recipe=level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,grid,level);
        check(player,recipe.isPresent() && recipe.get().getResultItem(level.registryAccess()).is(box.asItem()),"eight-stone recipe yields chest");grid.clearContent();
        for(Direction facing:Direction.Plane.HORIZONTAL) {
            player.setYRot(facing.getOpposite().toYRot());
            BlockPos second=origin.relative(facing.getClockWise());
            level.setBlock(origin,Blocks.WATER.defaultBlockState(),3);
            level.setBlock(second,Blocks.AIR.defaultBlockState(),3);
            var item=(BlockItem)box.asItem();
            var result=item.place(new BlockPlaceContext(player,InteractionHand.MAIN_HAND,new ItemStack(box),new BlockHitResult(Vec3.atBottomCenterOf(origin),Direction.UP,origin,false)));
            check(player,result.consumesAction() && level.getBlockState(origin).getValue(BlockStateProperties.HORIZONTAL_FACING)==facing,"actual placement facing "+facing);
            var entity=(ChestBlockEntity)level.getBlockEntity(origin);
            check(player,entity instanceof StoneChestBlockEntity && entity.getType()==ModBlockEntities.BLACK_ROCK_CHEST.get() && entity.getContainerSize()==27,"own block entity,27 slots "+facing);
            check(player,level.getBlockState(origin).getValue(BlockStateProperties.WATERLOGGED),"native waterlogged placement "+facing);
            var menu=(ChestMenu)entity.createMenu(97,player.getInventory(),player);
            check(player,menu.getRowCount()==3,"native three-row menu "+facing);menu.removed(player);
            ItemStack stored=new ItemStack(Items.DIAMOND,7);stored.setHoverName(Component.literal("QA tagged stone chest contents"));
            entity.setItem(26,stored);
            ChestBlockEntity restored=new StoneChestBlockEntity(origin,level.getBlockState(origin));restored.load(entity.saveWithFullMetadata());
            check(player,ItemStack.matches(restored.getItem(26),stored),"NBT round trip preserves final slot and tags "+facing);
            var handler=entity.getCapability(ForgeCapabilities.ITEM_HANDLER,Direction.DOWN).orElseThrow(()->new IllegalStateException("missing hopper capability"));
            check(player,handler.getSlots()==27 && handler.insertItem(0,new ItemStack(Items.IRON_INGOT,3),false).isEmpty() && handler.extractItem(0,3,false).getCount()==3,"hopper insertion/extraction "+facing);
            check(player,box.getAnalogOutputSignal(level.getBlockState(origin),level,origin)>0,"comparator reads stored contents "+facing);
            entity.clearContent();
            result=item.place(new BlockPlaceContext(player,InteractionHand.MAIN_HAND,new ItemStack(box),new BlockHitResult(Vec3.atBottomCenterOf(second),Direction.UP,second,false)));
            check(player,result.consumesAction() && level.getBlockState(origin).getValue(BlockStateProperties.CHEST_TYPE)!=ChestType.SINGLE && level.getBlockState(second).getValue(BlockStateProperties.CHEST_TYPE)!=ChestType.SINGLE,"same-type double chest connection "+facing);
            var container=ChestBlock.getContainer((ChestBlock)box,level.getBlockState(origin),level,origin,true);
            check(player,container!=null && container.getContainerSize()==54,"combined54slots "+facing);
            var provider=box.getMenuProvider(level.getBlockState(origin),level,origin);
            var doubleMenu=(ChestMenu)provider.createMenu(98,player.getInventory(),player);
            check(player,doubleMenu.getRowCount()==6,"native six-row combined menu "+facing);doubleMenu.removed(player);
            level.setBlock(second,Blocks.AIR.defaultBlockState(),3);
            check(player,level.getBlockState(origin).getValue(BlockStateProperties.CHEST_TYPE)==ChestType.SINGLE,"split returns27slots "+facing);
            level.setBlock(second,Blocks.CHEST.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,facing),3);
            check(player,level.getBlockState(origin).getValue(BlockStateProperties.CHEST_TYPE)==ChestType.SINGLE && level.getBlockState(second).getValue(BlockStateProperties.CHEST_TYPE)==ChestType.SINGLE,"no merge with vanilla chest "+facing);
            level.setBlock(second,Blocks.AIR.defaultBlockState(),3);
            level.setBlock(origin.above(),Blocks.STONE.defaultBlockState(),3);
            check(player,box.getMenuProvider(level.getBlockState(origin),level,origin)==null,"solid lid obstruction blocks opening "+facing);
            level.setBlock(origin.above(),Blocks.AIR.defaultBlockState(),3);
            entity.triggerEvent(1,1);
            for(int i=0;i<11;i++)ChestBlockEntity.lidAnimateTick(level,origin,level.getBlockState(origin),entity);
            check(player,entity.getOpenNess(1)>=0.99F,"native lid reaches fully open "+facing);
            entity.triggerEvent(1,0);
            for(int i=0;i<11;i++)ChestBlockEntity.lidAnimateTick(level,origin,level.getBlockState(origin),entity);
            check(player,entity.getOpenNess(1)<=0.01F,"native lid returns closed "+facing);
            entity.setItem(26,stored.copy());
            var area=new AABB(origin.offset(-3,-2,-3),origin.offset(4,4,4));
            int before=diamonds(level.getEntitiesOfClass(ItemEntity.class,area));
            int beforeBox=level.getEntitiesOfClass(ItemEntity.class,area).stream().filter(e->e.getItem().is(box.asItem())).mapToInt(e->e.getItem().getCount()).sum();
            level.destroyBlock(origin,true);
            check(player,diamonds(level.getEntitiesOfClass(ItemEntity.class,area))-before==7,"break spills contents exactly once "+facing);
            int afterBox=level.getEntitiesOfClass(ItemEntity.class,area).stream().filter(e->e.getItem().is(box.asItem())).mapToInt(e->e.getItem().getCount()).sum();
            check(player,afterBox-beforeBox==1 && level.getBlockEntity(origin)==null,"break drops one empty chest/removes entity "+facing);
        }
    }
    private static int diamonds(java.util.List<ItemEntity> entities) {
        return entities.stream().filter(e->e.getItem().is(Items.DIAMOND)).mapToInt(e->e.getItem().getCount()).sum();
    }
    public static int gallery(ServerPlayer player) {
        if(!player.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).normalize().getFileName().toString().startsWith("Xiantu-QA"))return 0;
        var level=player.serverLevel();
        for(int attempt=0;attempt<8;attempt++) {
            int x=4608+attempt*96,z=800;boolean clear=true;
            for(int dx=-20;dx<35;dx++)for(int dz=-20;dz<22;dz++)for(int y=-60;y<-54;y++)if(!level.getBlockState(new BlockPos(x+dx,y,z+dz)).isAir())clear=false;
            if(!clear)continue;
            for(int i=0;i<4;i++)level.setBlock(new BlockPos(x+i*4,-60,z),ModBlocks.BLACK_ROCK_CHEST.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.from2DDataValue(i)),3);
            BlockPos a=new BlockPos(x+16,-60,z);var box=ModBlocks.BLACK_ROCK_CHEST.get();
            level.setBlock(a,box.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.NORTH).setValue(BlockStateProperties.CHEST_TYPE,ChestType.LEFT),3);
            level.setBlock(a.east(),box.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.NORTH).setValue(BlockStateProperties.CHEST_TYPE,ChestType.RIGHT),3);
            // Populate the sample to test reopening and persistence manually.
            ((ChestBlockEntity)level.getBlockEntity(a)).setItem(0,new ItemStack(Items.COBBLESTONE,8));
            player.teleportTo(level,x+2.5,-60,z-3,0,10);
            player.sendSystemMessage(Component.literal("[Stone chest gallery] Four facings and double chest x"+x+" z"+z));return 1;
        }
        player.sendSystemMessage(Component.literal("Stone chest gallery occupied; no blocks changed"));return 0;
    }
}
