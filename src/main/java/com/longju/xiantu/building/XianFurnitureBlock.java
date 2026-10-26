package com.longju.xiantu.building;

import com.longju.xiantu.workstation.WorkstationBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/** Static multicell furniture, sharing placement/support/removal rules with equipment. */
public abstract class XianFurnitureBlock extends WorkstationBlock {
    protected XianFurnitureBlock(Properties p,double[][] boxes){super(p,boxes);}
    public static final class Coffin extends XianFurnitureBlock {
        public static final EnumProperty<Part> PARTS=EnumProperty.create("part",Part.class);
        public Coffin(Properties p,double[][] b){super(p,b);}
        @Override public EnumProperty<Part> partProperty(){return PARTS;}
    }
    public static final class TallChair extends XianFurnitureBlock {
        public static final EnumProperty<Part> PARTS=EnumProperty.create("part",Part.class,Part.P000,Part.P010);
        public TallChair(Properties p,double[][] b){super(p,b);}
        @Override public EnumProperty<Part> partProperty(){return PARTS;}
    }
    public static final class LongDesk extends XianFurnitureBlock {
        public static final EnumProperty<Part> PARTS=EnumProperty.create("part",Part.class,Part.P000,Part.P100);
        public LongDesk(Properties p,double[][] b){super(p,b);}
        @Override public EnumProperty<Part> partProperty(){return PARTS;}
    }
    public static final class RoundTable extends XianFurnitureBlock {
        public static final EnumProperty<Part> PARTS=EnumProperty.create("part",Part.class,Part.P000,Part.P100,Part.P001,Part.P101);
        public RoundTable(Properties p,double[][] b){super(p,b);}
        @Override public EnumProperty<Part> partProperty(){return PARTS;}
    }
    public static final class WideTall extends XianFurnitureBlock {
        public static final EnumProperty<Part> PARTS=EnumProperty.create("part",Part.class,Part.P000,Part.P100,Part.P010,Part.P110);
        public WideTall(Properties p,double[][] b){super(p,b);}
        @Override public EnumProperty<Part> partProperty(){return PARTS;}
    }
    @Override public InteractionResult use(BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){return InteractionResult.PASS;}
    @Override public void onPlace(BlockState s,Level l,BlockPos p,BlockState old,boolean moving){if(!old.is(this))l.scheduleTick(p,this,5);}
    @Override public void tick(BlockState s,ServerLevel l,BlockPos p,RandomSource random){
        if(com.longju.xiantu.worldgen.RuinPlacementQueue.busy()){l.scheduleTick(p,this,5);return;}
        super.tick(s,l,p,random);
    }
}
