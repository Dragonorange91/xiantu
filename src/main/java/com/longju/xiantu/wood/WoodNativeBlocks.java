package com.longju.xiantu.wood;
import net.minecraft.core.*;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import java.util.function.Supplier;
/** Native placement and collisions, with explicit wood fire behavior. */
final class WoodNativeBlocks {
    static final class Slab extends SlabBlock {
        public Slab(Properties p){super(p);}
        @Override public int getFlammability(BlockState s,BlockGetter l,BlockPos p,Direction f){return 20;}
        @Override public int getFireSpreadSpeed(BlockState s,BlockGetter l,BlockPos p,Direction f){return 5;}
    }
    static final class Stairs extends StairBlock {
        public Stairs(Supplier<BlockState> state,Properties p){super(state,p);}
        @Override public int getFlammability(BlockState s,BlockGetter l,BlockPos p,Direction f){return 20;}
        @Override public int getFireSpreadSpeed(BlockState s,BlockGetter l,BlockPos p,Direction f){return 5;}
    }
    static final class Wall extends WallBlock {
        public Wall(Properties p){super(p);}
        @Override public int getFlammability(BlockState s,BlockGetter l,BlockPos p,Direction f){return 20;}
        @Override public int getFireSpreadSpeed(BlockState s,BlockGetter l,BlockPos p,Direction f){return 5;}
    }
    static final class Fence extends FenceBlock {
        public Fence(Properties p){super(p);}
        @Override public int getFlammability(BlockState s,BlockGetter l,BlockPos p,Direction f){return 20;}
        @Override public int getFireSpreadSpeed(BlockState s,BlockGetter l,BlockPos p,Direction f){return 5;}
    }
    static final class Door extends DoorBlock {
        public Door(Properties p){super(p,BlockSetType.OAK);}
        @Override public int getFlammability(BlockState s,BlockGetter l,BlockPos p,Direction f){return 20;}
        @Override public int getFireSpreadSpeed(BlockState s,BlockGetter l,BlockPos p,Direction f){return 5;}
    }
    static final class Trapdoor extends TrapDoorBlock {
        public Trapdoor(Properties p){super(p,BlockSetType.OAK);}
        @Override public int getFlammability(BlockState s,BlockGetter l,BlockPos p,Direction f){return 20;}
        @Override public int getFireSpreadSpeed(BlockState s,BlockGetter l,BlockPos p,Direction f){return 5;}
    }
    static final class Window extends IronBarsBlock {
        public Window(Properties p){super(p);}
        @Override public int getFlammability(BlockState s,BlockGetter l,BlockPos p,Direction f){return 20;}
        @Override public int getFireSpreadSpeed(BlockState s,BlockGetter l,BlockPos p,Direction f){return 5;}
    }
    static final class Leaves extends LeavesBlock {
        public Leaves(Properties p){super(p);}
        @Override public int getFlammability(BlockState s,BlockGetter l,BlockPos p,Direction f){return 60;}
        @Override public int getFireSpreadSpeed(BlockState s,BlockGetter l,BlockPos p,Direction f){return 30;}
    }
}
