package com.longju.xiantu.refining;
import com.longju.xiantu.registry.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.item.*;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.registries.RegistryObject;
public final class EquipmentRepairBlock extends com.longju.xiantu.workstation.WorkstationBlock {
 public static final net.minecraft.world.level.block.state.properties.EnumProperty<Part> REPAIR_PART=net.minecraft.world.level.block.state.properties.EnumProperty.create("part",Part.class,Part.P000,Part.P100,Part.P010,Part.P110);
 @Override public net.minecraft.world.level.block.state.properties.EnumProperty<Part> partProperty(){return REPAIR_PART;}
 public static final RegistryObject<Block> BLOCK=ModBlocks.EQUIPMENT_REPAIR;
 public EquipmentRepairBlock(){super(Properties.copy(Blocks.SMITHING_TABLE).strength(3).noOcclusion(),com.longju.xiantu.building.RuinFurniture.REPAIR_BOXES);}
 @Override public void tick(BlockState s,net.minecraft.server.level.ServerLevel l,BlockPos p,net.minecraft.util.RandomSource r){if(com.longju.xiantu.worldgen.RuinPlacementQueue.busy()){l.scheduleTick(p,this,5);return;}super.tick(s,l,p,r);}
 @Override public InteractionResult use(BlockState s,Level l,BlockPos pos,Player p,InteractionHand h,BlockHitResult hit){if(p instanceof ServerPlayer server&&h==InteractionHand.MAIN_HAND)EquipmentRepairMenu.open(server,anchor(pos,s));return InteractionResult.sidedSuccess(l.isClientSide);}
}
