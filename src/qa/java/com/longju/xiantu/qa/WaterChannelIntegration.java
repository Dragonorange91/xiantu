package com.longju.xiantu.qa;

import com.longju.xiantu.building.StoneWaterChannelBlock;
import com.longju.xiantu.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

public final class WaterChannelIntegration {
    private static void check(ServerPlayer p,boolean ok,String name) {
        p.sendSystemMessage(Component.literal("[Water channel QA "+(ok?"PASS":"FAIL")+"] "+name));
        if(!ok)throw new IllegalStateException(name);
    }
    private static int bit(Direction d) {return switch(d){case NORTH->1;case EAST->2;case SOUTH->4;case WEST->8;default->0;};}
    private static int transform(int mask,java.util.function.Function<Direction,Direction> map) {
        int result=0;for(Direction d:Direction.Plane.HORIZONTAL)if((mask&bit(d))!=0)result|=bit(map.apply(d));return result;
    }
    private static VoxelShape physical(VoxelShape input,java.util.function.Function<double[],double[]> map) {
        VoxelShape[] out={Shapes.empty()};input.forAllBoxes((a,b,c,d,e,f)->{double[] q=map.apply(new double[]{a,b,c,d,e,f});out[0]=Shapes.or(out[0],Shapes.box(q[0],q[1],q[2],q[3],q[4],q[5]));});return out[0];
    }
    public static void checkChannel(ServerPlayer p,BlockPos pos,CraftingContainer grid) {
        var level=p.serverLevel();Block channel=ModBlocks.SPRING_STONE_WATER_CHANNEL.get();
        grid.clearContent();for(int s:new int[]{0,2,3,5})grid.setItem(s,new ItemStack(ModBlocks.SPRING_STONE_SMOOTH.get()));
        grid.setItem(4,new ItemStack(ModBlocks.SPRING_STONE_CARVED.get()));
        var recipe=level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,grid,level);
        check(p,recipe.isPresent() && recipe.get().getResultItem(level.registryAccess()).is(channel.asItem()) && recipe.get().getResultItem(level.registryAccess()).getCount()==4,"actual trough crafting grid yields4");grid.clearContent();
        for(int mask=0;mask<16;mask++) {
            level.setBlock(pos,Blocks.AIR.defaultBlockState(),3);
            for(Direction d:Direction.Plane.HORIZONTAL)level.setBlock(pos.relative(d),Blocks.AIR.defaultBlockState(),3);
            for(Direction d:Direction.Plane.HORIZONTAL)if((mask&bit(d))!=0)level.setBlock(pos.relative(d),channel.defaultBlockState(),3);
            var result=((BlockItem)channel.asItem()).place(new BlockPlaceContext(p,InteractionHand.MAIN_HAND,new ItemStack(channel),new BlockHitResult(Vec3.atBottomCenterOf(pos),Direction.UP,pos,false)));
            BlockState s=level.getBlockState(pos);
            check(p,result.consumesAction() && s.is(channel) && StoneWaterChannelBlock.mask(s)==mask,"native placement and mask "+mask);
            check(p,level.getBlockEntity(pos)==null,"static no block entity "+mask);
            VoxelShape shape=s.getShape(level,pos);
            check(p,!Shapes.joinIsNotEmpty(shape,Shapes.box(.4,.6,.4,.6,.8,.6),BooleanOp.AND),"hollow central trough "+mask);
            for(Direction d:Direction.Plane.HORIZONTAL)if((mask&bit(d))!=0)
                check(p,(StoneWaterChannelBlock.mask(level.getBlockState(pos.relative(d)))&bit(d.getOpposite()))!=0,"reciprocal connection "+mask+"/"+d);
            for(Rotation r:Rotation.values())check(p,StoneWaterChannelBlock.mask(channel.rotate(s,r))==transform(mask,r::rotate),"rotate mask "+mask+"/"+r);
            for(Mirror m:Mirror.values()) {
                BlockState reflected=channel.mirror(s,m);
                check(p,StoneWaterChannelBlock.mask(reflected)==transform(mask,m::mirror),"mirror mask "+mask+"/"+m);
                VoxelShape expected=physical(shape,q->m==Mirror.FRONT_BACK?new double[]{1-q[3],q[1],q[2],1-q[0],q[4],q[5]}:m==Mirror.LEFT_RIGHT?new double[]{q[0],q[1],1-q[5],q[3],q[4],1-q[2]}:q);
                check(p,!Shapes.joinIsNotEmpty(expected,reflected.getShape(level,pos),BooleanOp.NOT_SAME),"physical mirror collision "+mask+"/"+m);
            }
            var drops=Block.getDrops(s,level,pos,null,p,new ItemStack(Items.DIAMOND_PICKAXE));
            check(p,drops.stream().filter(i->i.is(channel.asItem())).mapToInt(ItemStack::getCount).sum()==1,"one intact item loot "+mask);
            for(Direction d:Direction.Plane.HORIZONTAL)level.setBlock(pos.relative(d),Blocks.AIR.defaultBlockState(),3);
            check(p,StoneWaterChannelBlock.mask(level.getBlockState(pos))==0,"neighbor removal closes rim "+mask);
        }
        var water=(SimpleWaterloggedBlock)channel;
        check(p,water.placeLiquid(level,pos,level.getBlockState(pos),Fluids.WATER.getSource(false)),"native water bucket insertion");
        check(p,level.getBlockState(pos).getValue(BlockStateProperties.WATERLOGGED) && level.getFluidState(pos).getType()==Fluids.WATER,"native water source state");
        level.setBlock(pos.east(),channel.defaultBlockState(),3);
        check(p,StoneWaterChannelBlock.mask(level.getBlockState(pos))==2 && level.getBlockState(pos).getValue(BlockStateProperties.WATERLOGGED),"connection changes preserve water");
        check(p,water.pickupBlock(level,pos,level.getBlockState(pos)).is(Items.WATER_BUCKET) && !level.getBlockState(pos).getValue(BlockStateProperties.WATERLOGGED),"native bucket extraction");
        level.setBlock(pos,Blocks.WATER.defaultBlockState(),3);
        var result=((BlockItem)channel.asItem()).place(new BlockPlaceContext(p,InteractionHand.MAIN_HAND,new ItemStack(channel),new BlockHitResult(Vec3.atBottomCenterOf(pos),Direction.UP,pos,false)));
        check(p,result.consumesAction() && level.getBlockState(pos).getValue(BlockStateProperties.WATERLOGGED),"placement into existing water");
        level.destroyBlock(pos,false);
        check(p,level.getFluidState(pos).getType()==Fluids.WATER,"breaking waterlogged channel leaves native water");
        level.setBlock(pos,Blocks.AIR.defaultBlockState(),3);level.setBlock(pos.east(),Blocks.AIR.defaultBlockState(),3);
    }
    public static int gallery(ServerPlayer p) {
        if(!p.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).normalize().getFileName().toString().startsWith("Xiantu-QA")) {p.sendSystemMessage(Component.literal("QA commands require a Xiantu-QA world"));return 0;}
        var level=p.serverLevel();Block b=ModBlocks.SPRING_STONE_WATER_CHANNEL.get();
        for(int attempt=0;attempt<8;attempt++) {
            int x=5216+attempt*96,z=976;boolean clear=true;
            for(int dx=-24;dx<44;dx++)for(int dz=-24;dz<38;dz++)for(int y=-60;y<-55;y++)if(!level.getBlockState(new BlockPos(x+dx,y,z+dz)).isAir())clear=false;
            if(!clear)continue;
            java.util.List<BlockPos> positions=new java.util.ArrayList<>();
            // Isolated basin, straight dry run, turn, T, cross; separate wet rectangle.
            int[][] pattern={{0,0},{4,0},{5,0},{6,0},{10,0},{11,0},{11,1},{15,0},{15,1},{14,1},{16,1},{0,5},{0,6},{-1,6},{1,6},{0,7}};
            for(int[] q:pattern)positions.add(new BlockPos(x+q[0],-60,z+q[1]));
            for(int a=7;a<=13;a++)for(int c=5;c<=10;c++)if(a==7||a==13||c==5||c==10)positions.add(new BlockPos(x+a,-60,z+c));
            for(BlockPos pos:positions)level.setBlock(pos,b.defaultBlockState(),3);
            for(BlockPos pos:positions) {
                BlockState s=level.getBlockState(pos);
                for(Direction d:Direction.Plane.HORIZONTAL)s=((Block)b).updateShape(s,d,level.getBlockState(pos.relative(d)),level,pos,pos.relative(d));
                if(pos.getX()>=x+7 && pos.getX()<=x+13 && pos.getZ()>=z+5)s=s.setValue(BlockStateProperties.WATERLOGGED,true);
                level.setBlock(pos,s,3);
                if(s.getValue(BlockStateProperties.WATERLOGGED))level.scheduleTick(pos,Fluids.WATER,5);
            }
            p.teleportTo(level,x+8,-57,z-5,0,28);
            p.sendSystemMessage(Component.literal("[Water channel gallery] Dry connections and wet loop x"+x+" z"+z));return 1;
        }
        p.sendSystemMessage(Component.literal("Channel gallery occupied; no blocks changed"));return 0;
    }
}
