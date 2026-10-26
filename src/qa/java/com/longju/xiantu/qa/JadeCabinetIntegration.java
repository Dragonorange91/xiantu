package com.longju.xiantu.qa;

import com.longju.xiantu.building.JadeCabinetBlock;
import com.longju.xiantu.building.JadeCabinetBlockEntity;
import com.longju.xiantu.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

public final class JadeCabinetIntegration {
    private static void check(ServerPlayer p,boolean ok,String name) {
        p.sendSystemMessage(Component.literal("[Jade cabinet QA "+(ok?"PASS":"FAIL")+"] "+name));
        if(!ok) throw new IllegalStateException(name);
    }
    private static int drops(ServerPlayer p,BlockPos pos,Item item) {
        return p.serverLevel().getEntitiesOfClass(ItemEntity.class,new AABB(pos.offset(-6,-2,-6),pos.offset(7,5,7)))
                .stream().filter(e->e.getItem().is(item)).mapToInt(e->e.getItem().getCount()).sum();
    }
    private static void prepare(ServerPlayer p,BlockPos pos) {
        var l=p.serverLevel();
        for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++) {
            for(int y=0;y<4;y++)l.setBlock(pos.offset(x,y,z),Blocks.AIR.defaultBlockState(),3);
            l.setBlock(pos.offset(x,-1,z),Blocks.STONE.defaultBlockState(),3);
        }
    }
    private static void place(ServerPlayer p,BlockPos pos,Direction facing) {
        p.setYRot(facing.getOpposite().toYRot());
        Block b=ModBlocks.CLOUD_JADE_CABINET.get();
        var result=((BlockItem)b.asItem()).place(new BlockPlaceContext(p,InteractionHand.MAIN_HAND,new ItemStack(b),new BlockHitResult(Vec3.atBottomCenterOf(pos),Direction.UP,pos,false)));
        check(p,result.consumesAction(),"real BlockItem placement "+facing);
    }
    public static void checkCabinet(ServerPlayer p,BlockPos pos,CraftingContainer grid) {
        var l=p.serverLevel();var cabinet=(JadeCabinetBlock)ModBlocks.CLOUD_JADE_CABINET.get();Block b=cabinet;
        Vec3 original=p.position();float yaw=p.getYRot(),pitch=p.getXRot();var originalMenu=p.containerMenu;
        p.teleportTo(l,pos.getX()+.5,pos.getY(),pos.getZ()-2,0,0);
        try {
        grid.clearContent();
        for(int s:new int[]{0,2,6,8})grid.setItem(s,new ItemStack(ModBlocks.CLOUD_JADE_CARVED.get()));
        for(int s:new int[]{1,3,5,7})grid.setItem(s,new ItemStack(ModBlocks.CLOUD_JADE_SMOOTH.get()));
        var recipe=l.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,grid,l);
        check(p,recipe.isPresent() && recipe.get().getResultItem(l.registryAccess()).is(b.asItem()),"eight jade blocks yield one cabinet");grid.clearContent();
        for(Direction f:Direction.Plane.HORIZONTAL) {
            BlockPos viewpoint=pos.relative(f,3);
            p.teleportTo(l,viewpoint.getX()+.5,pos.getY(),viewpoint.getZ()+.5,f.getOpposite().toYRot(),0);
            prepare(p,pos);l.setBlock(pos,Blocks.WATER.defaultBlockState(),3);place(p,pos,f);
            RandomizableContainerBlockEntity be=(RandomizableContainerBlockEntity)l.getBlockEntity(pos);
            check(p,be instanceof JadeCabinetBlockEntity && be.getContainerSize()==54,"own fixed54-slot inventory "+f);
            for(int part=0;part<4;part++) {
                BlockPos pp=cabinet.sectionPosition(pos,f,part);var s=l.getBlockState(pp);
                check(p,s.is(b) && s.getValue(JadeCabinetBlock.SECTION)==part && s.getValue(BlockStateProperties.HORIZONTAL_FACING)==f,"correct section "+f+"/"+part);
                check(p,(part==0?l.getBlockEntity(pp)==be:l.getBlockEntity(pp)==null) && b.getMenuProvider(s,l,pp)==be,"one BE and common menu "+f+"/"+part);
            }
            check(p,l.getBlockState(pos).getValue(BlockStateProperties.WATERLOGGED) && !l.getBlockState(pos.above()).getValue(BlockStateProperties.WATERLOGGED),"water state per section "+f);
            ItemStack first=new ItemStack(Items.DIAMOND,7);first.setHoverName(Component.literal("QA jade tagged"));
            be.setItem(0,first.copy());be.setItem(53,new ItemStack(Items.EMERALD,9));
            BlockEntity loaded=new JadeCabinetBlockEntity(pos,l.getBlockState(pos));loaded.load(be.saveWithFullMetadata());
            var copy=(RandomizableContainerBlockEntity)loaded;
            check(p,ItemStack.matches(copy.getItem(0),first) && copy.getItem(53).getCount()==9,"NBT first/final slots and tags "+f);
            var handler=be.getCapability(ForgeCapabilities.ITEM_HANDLER,Direction.DOWN).orElseThrow(()->new IllegalStateException("missing hopper capability"));
            check(p,handler.getSlots()==54 && handler.insertItem(1,new ItemStack(Items.IRON_INGOT,3),false).isEmpty() && handler.extractItem(1,3,false).getCount()==3,"native hopper54slot insertion/extraction "+f);
            for(int part=0;part<4;part++) {
                BlockPos pp=cabinet.sectionPosition(pos,f,part);
                check(p,b.getAnalogOutputSignal(l.getBlockState(pp),l,pp)>0,"comparator section "+f+"/"+part);
            }
            var old=p.containerMenu;
            var menu=(ChestMenu)be.createMenu(91,p.getInventory(),p);p.containerMenu=menu;
            check(p,menu.getRowCount()==6,"native six-row GUI "+f);
            ((JadeCabinetBlockEntity)be).recheckOpen();
            check(p,java.util.stream.IntStream.range(0,4).allMatch(i->l.getBlockState(cabinet.sectionPosition(pos,f,i)).getValue(BlockStateProperties.OPEN)),"all sections open "+f);
            menu.removed(p);p.containerMenu=old;
            check(p,java.util.stream.IntStream.range(0,4).noneMatch(i->l.getBlockState(cabinet.sectionPosition(pos,f,i)).getValue(BlockStateProperties.OPEN)),"all sections close "+f);
            // An adjacent whole cabinet owns independent54 slots; it must never merge.
            BlockPos next=pos.relative(f.getClockWise(),2);place(p,next,f);
            var other=(RandomizableContainerBlockEntity)l.getBlockEntity(next);
            check(p,other!=be && other.getContainerSize()==54 && other.isEmpty() && be.getItem(0).getCount()==7,"adjacent cabinet independent fixed capacity "+f);
            l.destroyBlock(next,false);
            int diamonds=drops(p,pos,Items.DIAMOND),emeralds=drops(p,pos,Items.EMERALD),empty=drops(p,pos,b.asItem());
            l.destroyBlock(cabinet.sectionPosition(pos,f,3),true);
            check(p,drops(p,pos,Items.DIAMOND)-diamonds==7 && drops(p,pos,Items.EMERALD)-emeralds==9,"upper section break spills exactly once "+f);
            check(p,drops(p,pos,b.asItem())-empty==1 && java.util.stream.IntStream.range(0,4).noneMatch(i->l.getBlockState(cabinet.sectionPosition(pos,f,i)).is(b)),"one empty cabinet and full cleanup "+f);
            prepare(p,pos);
            BlockPos obstruct=pos.relative(f.getCounterClockWise()).above();l.setBlock(obstruct,Blocks.STONE.defaultBlockState(),3);
            ItemStack stack=new ItemStack(b,3);
            var result=((BlockItem)b.asItem()).place(new BlockPlaceContext(p,InteractionHand.MAIN_HAND,stack,new BlockHitResult(Vec3.atBottomCenterOf(pos),Direction.UP,pos,false)));
            check(p,!result.consumesAction() && stack.getCount()==3 && !l.getBlockState(pos).is(b),"blocked fourth cell refuses placement "+f);
            prepare(p,pos);l.setBlock(pos.relative(f.getCounterClockWise()).below(),Blocks.AIR.defaultBlockState(),3);
            result=((BlockItem)b.asItem()).place(new BlockPlaceContext(p,InteractionHand.MAIN_HAND,stack,new BlockHitResult(Vec3.atBottomCenterOf(pos),Direction.UP,pos,false)));
            check(p,!result.consumesAction() && stack.getCount()==3,"missing second-column floor refuses placement "+f);
        }
        prepare(p,pos);place(p,pos,Direction.NORTH);
        var closedShape=b.getShape(l.getBlockState(pos),l,pos,net.minecraft.world.phys.shapes.CollisionContext.empty());
        check(p,!net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(closedShape,Block.box(0,0,6,2,1,10),net.minecraft.world.phys.shapes.BooleanOp.AND),"raised feet leave real gap below body");
        check(p,net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(closedShape,Block.box(9,0,3,12,4,6),net.minecraft.world.phys.shapes.BooleanOp.AND),"right front foot has matching collision");
        var upper=l.getBlockState(pos.above());
        var openShape=b.getShape(upper.setValue(BlockStateProperties.OPEN,true),l,pos.above(),net.minecraft.world.phys.shapes.CollisionContext.empty());
        check(p,openShape.min(Direction.Axis.Z)<0,"open upper doors project toward viewer");
        check(p,openShape==b.getShape(upper.setValue(BlockStateProperties.OPEN,true),l,pos.above(),net.minecraft.world.phys.shapes.CollisionContext.empty()),"open geometry uses cached shape");
        var be=(RandomizableContainerBlockEntity)l.getBlockEntity(pos);be.setItem(53,new ItemStack(Items.DIAMOND,5));
        int d=drops(p,pos,Items.DIAMOND),box=drops(p,pos,b.asItem());
        BlockPos target=cabinet.sectionPosition(pos,Direction.NORTH,1);
        check(p,p.isCreative(),"fixture remains creative");
        b.playerWillDestroy(l,target,l.getBlockState(target),p);l.destroyBlock(target,false);
        check(p,drops(p,pos,Items.DIAMOND)-d==5 && drops(p,pos,b.asItem())==box,"creative removes cabinet, spills contents, no empty item");
        prepare(p,pos);place(p,pos,Direction.NORTH);be=(RandomizableContainerBlockEntity)l.getBlockEntity(pos);be.setItem(0,new ItemStack(Items.DIAMOND,4));d=drops(p,pos,Items.DIAMOND);
        l.destroyBlock(pos.below(),false);
        check(p,!l.getBlockState(pos).is(b) && !l.getBlockState(pos.west().above()).is(b) && drops(p,pos,Items.DIAMOND)-d==4,"support removal cleans entire cabinet and preserves contents");
        prepare(p,pos);place(p,pos,Direction.NORTH);
        for(int part=0;part<4;part++) {
            BlockPos pp=cabinet.sectionPosition(pos,Direction.NORTH,part);
            l.setBlock(pp,l.getBlockState(pp).setValue(BlockStateProperties.OPEN,true),3);
        }
        ((JadeCabinetBlockEntity)l.getBlockEntity(pos)).recheckOpen();
        check(p,java.util.stream.IntStream.range(0,4).noneMatch(i->l.getBlockState(cabinet.sectionPosition(pos,Direction.NORTH,i)).getValue(BlockStateProperties.OPEN)),"persisted open state resets without viewers");
        // Breaking each of the four physical sections must preserve exactly one inventory.
        for(int part=0;part<4;part++) {
            prepare(p,pos);place(p,pos,Direction.NORTH);
            be=(RandomizableContainerBlockEntity)l.getBlockEntity(pos);be.setItem(53,new ItemStack(Items.DIAMOND,6));
            d=drops(p,pos,Items.DIAMOND);box=drops(p,pos,b.asItem());
            l.destroyBlock(cabinet.sectionPosition(pos,Direction.NORTH,part),true);
            check(p,drops(p,pos,Items.DIAMOND)-d==6 && drops(p,pos,b.asItem())-box==1,"each section break exactly one inventory/item "+part);
        }
        prepare(p,pos);
        } finally {
            if(p.containerMenu!=originalMenu) {p.containerMenu.removed(p);p.containerMenu=originalMenu;}
            p.teleportTo(l,original.x,original.y,original.z,yaw,pitch);
        }
    }
    public static int gallery(ServerPlayer p) {
        if(!p.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).normalize().getFileName().toString().startsWith("Xiantu-QA")) {
            p.sendSystemMessage(Component.literal("QA commands require a Xiantu-QA disposable world"));return 0;
        }
        var l=p.serverLevel();var cabinet=(JadeCabinetBlock)ModBlocks.CLOUD_JADE_CABINET.get();
        for(int attempt=0;attempt<8;attempt++) {
            int x=4864+attempt*96,z=880;boolean clear=true;
            for(int dx=-24;dx<40;dx++)for(int dz=-24;dz<25;dz++)for(int y=-60;y<-55;y++)if(!l.getBlockState(new BlockPos(x+dx,y,z+dz)).isAir())clear=false;
            if(!clear)continue;
            for(int i=0;i<4;i++) {
                BlockPos pos=new BlockPos(x+i*5,-60,z);Direction f=Direction.from2DDataValue(i);
                var s=((Block)cabinet).defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,f);
                l.setBlock(pos,s,3);((Block)cabinet).setPlacedBy(l,pos,s,p,new ItemStack(cabinet));
                ((RandomizableContainerBlockEntity)l.getBlockEntity(pos)).setItem(53,new ItemStack(Items.COBBLESTONE,8));
            }
            p.teleportTo(l,x+10.5,-60,z-4,0,12);
            p.sendSystemMessage(Component.literal("[Jade cabinet gallery] Four facings x"+x+" z"+z+"; slot54 has8cobblestone"));return 1;
        }
        p.sendSystemMessage(Component.literal("Cabinet gallery occupied; no blocks changed"));return 0;
    }
}
