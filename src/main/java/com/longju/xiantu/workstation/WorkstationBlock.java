package com.longju.xiantu.workstation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import net.minecraft.network.chat.Component;

/** Static, baked multi-cell equipment. No idle ticker, entity renderer or hidden inventory. */
public class WorkstationBlock extends HorizontalDirectionalBlock {
    private static final ThreadLocal<Boolean> REPLACING=ThreadLocal.withInitial(()->false);
    public static void replaceAssembly(Runnable change){
        boolean previous=REPLACING.get();REPLACING.set(true);
        try{change.run();}finally{REPLACING.set(previous);}
    }
    public enum Part implements StringRepresentable {
        P000(0,0,0), P100(1,0,0), P001(0,0,1), P101(1,0,1),
        P010(0,1,0), P110(1,1,0), P011(0,1,1), P111(1,1,1),
        P002(0,0,2), P102(1,0,2), P012(0,1,2), P112(1,1,2),
        P003(0,0,3), P103(1,0,3), P013(0,1,3), P113(1,1,3),
        P004(0,0,4), P104(1,0,4), P014(0,1,4), P114(1,1,4);
        public final int x,y,z;
        Part(int x,int y,int z) { this.x=x;this.y=y;this.z=z; }
        public String getSerializedName() { return x+"_"+y+"_"+z; }
    }
    public static final EnumProperty<Part> PART=EnumProperty.create("part",Part.class,
        Part.P000,Part.P100,Part.P001,Part.P101,Part.P010,Part.P110,Part.P011,Part.P111);
    private final VoxelShape[][] shapes;
    public WorkstationBlock(Properties properties, double[][] boxes) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(partProperty(),Part.P000));
        shapes=new VoxelShape[4][Part.values().length];
        for (int f=0;f<4;f++) for(Part p:partProperty().getPossibleValues()) {
            VoxelShape shape=Shapes.empty();
            for(double[] b:boxes) {
                double x0=Math.max(0,b[0]-p.x*16),y0=Math.max(0,b[1]-p.y*16),z0=Math.max(0,b[2]-p.z*16);
                double x1=Math.min(16,b[3]-p.x*16),y1=Math.min(16,b[4]-p.y*16),z1=Math.min(16,b[5]-p.z*16);
                if(x1<=x0||y1<=y0||z1<=z0)continue;
                for(int r=0;r<f;r++){ double a=x0;x0=16-z1;z1=x1;x1=16-z0;z0=a; }
                shape=Shapes.or(shape,Block.box(x0,y0,z0,x1,y1,z1));
            }
            shapes[f][p.ordinal()]=shape.optimize();
        }
    }
    public EnumProperty<Part> partProperty(){return PART;}
    public static class Table extends WorkstationBlock {
        public static final EnumProperty<Part> TABLE_PART=EnumProperty.create("part",Part.class,Part.P000,Part.P100);
        public Table(Properties p,double[][] boxes){super(p,boxes);}
        @Override public EnumProperty<Part> partProperty(){return TABLE_PART;}
    }
    public static final class RefiningFurnace extends WorkstationBlock implements EntityBlock {
        public RefiningFurnace(Properties p,double[][] boxes){super(p,boxes);}
    }
    public static final class RefiningTable extends Table implements EntityBlock {
        public RefiningTable(Properties p,double[][] boxes){super(p,boxes);}
    }
    private static int turns(Direction f){return switch(f){case EAST->1;case SOUTH->2;case WEST->3;default->0;};}
    public BlockPos offset(Part p,Direction f){return switch(f){
        case EAST->new BlockPos(-p.z,p.y,p.x);case SOUTH->new BlockPos(-p.x,p.y,-p.z);
        case WEST->new BlockPos(p.z,p.y,-p.x);default->new BlockPos(p.x,p.y,p.z);};}
    public BlockPos anchor(BlockPos pos,BlockState state){return pos.subtract(offset(state.getValue(partProperty()),state.getValue(FACING)));}
    public BlockPos position(BlockPos anchor,Part p,Direction f){return anchor.offset(offset(p,f));}
    private boolean matches(BlockState other,Part p,Direction f){return other.is(this)&&other.getValue(partProperty())==p&&other.getValue(FACING)==f;}
    @Override public BlockState getStateForPlacement(BlockPlaceContext context){
        var level=context.getLevel();var pos=context.getClickedPos();Direction f=context.getHorizontalDirection().getOpposite();
        BlockState initial=defaultBlockState().setValue(FACING,f);
        for(Part p:partProperty().getPossibleValues()){
            BlockPos target=position(pos,p,f);BlockState state=initial.setValue(partProperty(),p);
            BlockPlaceContext local=BlockPlaceContext.at(context,target,context.getClickedFace());
            if(!level.isInWorldBounds(target)||!level.getWorldBorder().isWithinBounds(target)||!level.hasChunkAt(target)
                    ||!level.getBlockState(target).canBeReplaced(local)||!level.getFluidState(target).isEmpty()
                    ||!level.isUnobstructed(state,target,CollisionContext.empty())
                    ||!level.noCollision(null,state.getCollisionShape(level,target).move(target.getX(),target.getY(),target.getZ()).bounds())
                    ||(p.y==0&&!level.getBlockState(target.below()).isFaceSturdy(level,target.below(),Direction.UP)))return null;
        }
        return initial;
    }
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,LivingEntity entity,ItemStack stack){
        if(level.getBlockEntity(pos) instanceof com.longju.xiantu.refining.RefiningBlockEntity be && stack.hasTag()){
            be.furnaceQuality=Math.max(1,Math.min(5,stack.getTag().getInt("FurnaceQuality")));be.setChanged();
        }
        Direction f=state.getValue(FACING);
        for(Part p:partProperty().getPossibleValues())if(p!=Part.P000)level.setBlock(position(pos,p,f),state.setValue(partProperty(),p),2);
        for(Part p:partProperty().getPossibleValues())level.updateNeighborsAt(position(pos,p,f),this);
    }
    @Override public void playerWillDestroy(Level level,BlockPos pos,BlockState state,Player player){
        if(!level.isClientSide&&state.getValue(partProperty())!=Part.P000){
            BlockPos origin=anchor(pos,state);
            if(matches(level.getBlockState(origin),Part.P000,state.getValue(FACING)))level.destroyBlock(origin,!player.isCreative(),player);
        }
        super.playerWillDestroy(level,pos,state,player);
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moving){
        if(!next.is(this)&&!level.isClientSide&&!REPLACING.get()){
            if(state.getValue(partProperty())==Part.P000 && level.getBlockEntity(pos) instanceof com.longju.xiantu.refining.RefiningBlockEntity be){
                if(be.alchemyJob!=null)be.alchemyJob.discard(be);
                net.minecraft.world.Containers.dropContents(level,pos,be);
                net.minecraft.world.Containers.dropContents(level,pos,be.alchemyAux);
            }
            BlockPos origin=anchor(pos,state);Direction f=state.getValue(FACING);
            if(state.getValue(partProperty())!=Part.P000){
                if(matches(level.getBlockState(origin),Part.P000,f))level.destroyBlock(origin,true);
            }else for(Part p:partProperty().getPossibleValues())if(p!=Part.P000){
                BlockPos target=position(origin,p,f);
                if(level.hasChunkAt(target)&&matches(level.getBlockState(target),p,f))level.setBlock(target,Blocks.AIR.defaultBlockState(),3);
            }
        }
        super.onRemove(state,level,pos,next,moving);
    }
    @Override public BlockState updateShape(BlockState state,Direction dir,BlockState neighbor,LevelAccessor level,BlockPos pos,BlockPos neighborPos){
        level.scheduleTick(pos,this,1);return state;
    }
    @Override public void onPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean moving){if(!old.is(this))level.scheduleTick(pos,this,1);}
    @Override public void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){
        BlockPos origin=anchor(pos,state);Direction f=state.getValue(FACING);
        for(Part p:partProperty().getPossibleValues())if(!level.hasChunkAt(position(origin,p,f)))return;
        for(Part p:partProperty().getPossibleValues()){
            BlockPos target=position(origin,p,f);
            if(!matches(level.getBlockState(target),p,f)||(p.y==0&&!level.getBlockState(target.below()).isFaceSturdy(level,target.below(),Direction.UP))){
                level.destroyBlock(pos,true);return;
            }
        }
    }
    public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos,BlockState state){
        return state.getValue(partProperty())==Part.P000 && com.longju.xiantu.refining.RefiningBlockEntity.supports(state)?new com.longju.xiantu.refining.RefiningBlockEntity(pos,state):new com.longju.xiantu.refining.WorkstationPartBlockEntity(pos,state);
    }
    public <T extends net.minecraft.world.level.block.entity.BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(Level world,BlockState state,net.minecraft.world.level.block.entity.BlockEntityType<T> type){
        if(world.isClientSide||state.getValue(partProperty())!=Part.P000||type!=com.longju.xiantu.registry.ModBlockEntities.REFINING.get())return null;
        return (l,p,s,b)->com.longju.xiantu.refining.RefiningBlockEntity.tick(l,p,s,(com.longju.xiantu.refining.RefiningBlockEntity)b);
    }
    @Override public InteractionResult use(BlockState state,Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(!level.isClientSide && player instanceof net.minecraft.server.level.ServerPlayer server && level.getBlockEntity(anchor(pos,state)) instanceof com.longju.xiantu.refining.RefiningBlockEntity be){
            net.minecraftforge.network.NetworkHooks.openScreen(server,be,buf->{buf.writeBlockPos(be.getBlockPos());if(!be.alchemy()){buf.writeBoolean(be.forge());buf.writeBoolean(false);}});
            return InteractionResult.CONSUME;
        }
        if(!level.isClientSide&&matches(level.getBlockState(anchor(pos,state)),Part.P000,state.getValue(FACING)))
            player.displayClientMessage(Component.translatable("message.xiantu.workstation.prepared",getName()),true);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return shapes[turns(state.getValue(FACING))][state.getValue(partProperty()).ordinal()];}
    @Override public PushReaction getPistonPushReaction(BlockState state){return PushReaction.BLOCK;}
    @Override public BlockState rotate(BlockState state,Rotation rotation){return state.setValue(FACING,rotation.rotate(state.getValue(FACING)));}
    @Override public BlockState mirror(BlockState state,Mirror mirror){
        if(mirror==Mirror.NONE)return state;
        Part old=state.getValue(partProperty());
        for(Part p:partProperty().getPossibleValues())if(p.x==1-old.x&&p.y==old.y&&p.z==old.z)
            return state.setValue(FACING,mirror.mirror(state.getValue(FACING))).setValue(partProperty(),p);
        throw new IllegalStateException("Missing mirrored workstation part");
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING,partProperty());}
}
