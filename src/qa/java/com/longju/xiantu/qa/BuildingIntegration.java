package com.longju.xiantu.qa;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Mirror;
import com.longju.xiantu.building.RoofRidgeBlock;
import com.longju.xiantu.building.PlaqueBlock;
import com.longju.xiantu.building.ConnectedStoneBeamBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.LinkedHashMap;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.BooleanOp;
import java.util.stream.Collectors;

/** Runtime registry, loot, connection and crafting checks in a disposable world. */
public final class BuildingIntegration {
    private static final String[] FAMILIES = {"azure_brick", "black_rock", "cloud_jade", "spring_stone", "cloud_floating_stone"};
    private static void check(ServerPlayer player, boolean ok, String label) {
        String message = "[Building QA " + (ok ? "PASS" : "FAIL") + "] " + label;
        player.sendSystemMessage(Component.literal(message));
        if (!ok) throw new IllegalStateException(message);
    }
    private static Block block(String name) {
        return ForgeRegistries.BLOCKS.getValue(new ResourceLocation("xiantu", name));
    }
    private static int countDrops(ServerPlayer player, BlockState state, BlockPos pos, Block expected) {
        return Block.getDrops(state, player.serverLevel(), pos, null, player, new ItemStack(Items.DIAMOND_PICKAXE))
                .stream().filter(stack -> stack.is(expected.asItem())).mapToInt(ItemStack::getCount).sum();
    }
    public static int run(ServerPlayer player) {
        if (!player.getServer().getWorldPath(LevelResource.ROOT).normalize().getFileName().toString().startsWith("Xiantu-QA")) {
            player.sendSystemMessage(Component.literal("QA commands require a Xiantu-QA disposable world"));
            return 0;
        }
        var level = player.serverLevel();
        float originalYaw = player.getYRot(), originalPitch = player.getXRot();
        BlockPos origin = player.blockPosition().offset(160, 0, 160);
        var saved = new LinkedHashMap<BlockPos, BlockState>();
        // Refuse to touch functional block entities; restore all neighboring state after the test.
        for (int x = -8; x <= 8; x++) for (int z = -8; z <= 8; z++) for (int y = -1; y <= 4; y++) {
            BlockPos pos = origin.offset(x, y, z);
            if (level.getBlockEntity(pos) != null) {
                player.sendSystemMessage(Component.literal("Building QA site contains a block entity; move before retrying"));
                return 0;
            }
            saved.put(pos, level.getBlockState(pos));
        }
        AABB fixtureBounds = new AABB(origin.offset(-9,-2,-9), origin.offset(10,6,10));
        var originalDrops = level.getEntitiesOfClass(ItemEntity.class, fixtureBounds).stream()
                .map(Entity::getUUID).collect(Collectors.toSet());
        try {
            for (BlockPos pos : saved.keySet()) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(origin.below(), Blocks.STONE.defaultBlockState(), 3);
            level.setBlock(origin.east().below(), Blocks.STONE.defaultBlockState(), 3);
            for (String family : FAMILIES) {
                Block slab = block(family + "_slab");
                check(player, countDrops(player, slab.defaultBlockState(), origin, slab) == 1, family + " single slab drops one");
                check(player, countDrops(player, slab.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.DOUBLE), origin, slab) == 2,
                        family + " double slab drops two");
                Block wall = block(family + "_wall");
                level.setBlock(origin, wall.defaultBlockState(), 3);
                level.setBlock(origin.east(), Blocks.COBBLESTONE_WALL.defaultBlockState(), 3);
                check(player, level.getBlockState(origin).getValue(WallBlock.EAST_WALL) != WallSide.NONE
                        && level.getBlockState(origin.east()).getValue(WallBlock.WEST_WALL) != WallSide.NONE,
                        family + " wall connects both ways with vanilla wall");
                level.setBlock(origin.east(), Blocks.AIR.defaultBlockState(), 3);
                check(player, level.getBlockState(origin).getValue(WallBlock.EAST_WALL) == WallSide.NONE,
                        family + " wall disconnects after neighbor removal");
                level.setBlock(origin, Blocks.AIR.defaultBlockState(), 3);
                Block railing = block(family + "_railing");
                level.setBlock(origin, railing.defaultBlockState(), 3);
                level.setBlock(origin.east(), Blocks.NETHER_BRICK_FENCE.defaultBlockState(), 3);
                check(player, level.getBlockState(origin).getValue(BlockStateProperties.EAST)
                        && level.getBlockState(origin.east()).getValue(BlockStateProperties.WEST),
                        family + " railing connects both ways with non-wooden vanilla fence");
                level.setBlock(origin.east(), Blocks.OAK_FENCE.defaultBlockState(), 3);
                check(player, !level.getBlockState(origin).getValue(BlockStateProperties.EAST)
                        && !level.getBlockState(origin.east()).getValue(BlockStateProperties.WEST),
                        family + " stone railing keeps vanilla wood/non-wood connection separation");
                check(player, railing.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true).getFluidState().is(FluidTags.WATER),
                        family + " railing supports vanilla waterlogging");
                check(player, railing.defaultBlockState().getCollisionShape(level, origin).max(Direction.Axis.Y) == 1.5,
                        family + " railing retains 1.5-block vanilla fence collision");
                for (String suffix : new String[]{"smooth", "carved", "railing", "trapdoor", "window"}) {
                    Block variant = block(family + "_" + suffix);
                    check(player, countDrops(player, variant.defaultBlockState(), origin, variant) == 1,
                            family + " " + suffix + " drops itself");
                }
                level.setBlock(origin.east(), Blocks.AIR.defaultBlockState(), 3);
                level.setBlock(origin, Blocks.AIR.defaultBlockState(), 3);
                Block door = block(family + "_door");
                ((BlockItem) door.asItem()).place(new BlockPlaceContext(player, InteractionHand.MAIN_HAND, new ItemStack(door),
                        new BlockHitResult(Vec3.atCenterOf(origin.below()), Direction.UP, origin.below(), false)));
                BlockState lower = level.getBlockState(origin);
                check(player, level.getBlockState(origin.above()).is(door)
                        && level.getBlockState(origin.above()).getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER,
                        family + " door placement creates upper half");
                check(player, countDrops(player, lower, origin, door) == 1
                        && countDrops(player, level.getBlockState(origin.above()), origin.above(), door) == 0,
                        family + " door halves jointly drop one item");
                door.use(lower, level, origin, player, InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(origin), Direction.NORTH, origin, false));
                check(player, level.getBlockState(origin).getValue(DoorBlock.OPEN)
                        && level.getBlockState(origin.above()).getValue(DoorBlock.OPEN), family + " hand opens both halves");
                level.setBlock(origin.east(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
                check(player, level.getBlockState(origin).getValue(DoorBlock.POWERED)
                        && level.getBlockState(origin.above()).getValue(DoorBlock.POWERED), family + " door redstone powers both halves");
                level.setBlock(origin.east(), Blocks.AIR.defaultBlockState(), 3);
                check(player, !level.getBlockState(origin).getValue(DoorBlock.OPEN)
                        && !level.getBlockState(origin.above()).getValue(DoorBlock.OPEN), family + " door closes after redstone removed");
                level.setBlock(origin.above(), Blocks.AIR.defaultBlockState(), 3);
                check(player, level.getBlockState(origin).isAir(), family + " missing upper half removes lower half");
                Block trap = block(family + "_trapdoor");
                level.setBlock(origin, trap.defaultBlockState(), 3);
                trap.use(level.getBlockState(origin), level, origin, player, InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(origin), Direction.UP, origin, false));
                check(player, level.getBlockState(origin).getValue(TrapDoorBlock.OPEN), family + " hand opens trapdoor");
                level.setBlock(origin.east(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
                check(player, level.getBlockState(origin).getValue(TrapDoorBlock.POWERED), family + " redstone powers trapdoor");
                level.setBlock(origin.east(), Blocks.AIR.defaultBlockState(), 3);
                check(player, !level.getBlockState(origin).getValue(TrapDoorBlock.OPEN), family + " trapdoor closes after redstone removed");
                check(player, trap.defaultBlockState().setValue(TrapDoorBlock.WATERLOGGED, true).getFluidState().is(FluidTags.WATER),
                        family + " trapdoor retains waterlogged fluid");
                Block window = block(family + "_window");
                var isolated = window.defaultBlockState().getCollisionShape(level, origin).bounds();
                check(player, isolated.getXsize() == 0.125 && isolated.getZsize() == 0.125 && isolated.getYsize() == 1.0,
                        family + " isolated window has thin pane collision");
                var directions = new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
                var properties = java.util.List.of(BlockStateProperties.NORTH, BlockStateProperties.EAST,
                        BlockStateProperties.SOUTH, BlockStateProperties.WEST);
                for (int mask = 0; mask < 16; mask++) {
                    BlockState candidate = window.defaultBlockState();
                    BlockState reference = Blocks.GLASS_PANE.defaultBlockState();
                    for (int bit = 0; bit < 4; bit++) {
                        candidate = candidate.setValue(properties.get(bit), (mask & (1 << bit)) != 0);
                        reference = reference.setValue(properties.get(bit), (mask & (1 << bit)) != 0);
                    }
                    check(player, !Shapes.joinIsNotEmpty(candidate.getCollisionShape(level, origin),
                                    reference.getCollisionShape(level, origin), BooleanOp.NOT_SAME),
                            family + " pane collision matches vanilla connection mask " + mask);
                }
                for (int bit = 0; bit < 4; bit++) {
                    Direction direction = directions[bit];
                    BlockPos neighbor = origin.relative(direction);
                    level.setBlock(origin, window.defaultBlockState(), 3);
                    level.setBlock(neighbor, Blocks.GLASS_PANE.defaultBlockState(), 3);
                    check(player, level.getBlockState(origin).getValue(properties.get(bit))
                                    && level.getBlockState(neighbor).getValue(properties.get((bit + 2) % 4)),
                            family + " window connects both ways with glass pane toward " + direction);
                    level.setBlock(neighbor, Blocks.STONE.defaultBlockState(), 3);
                    check(player, level.getBlockState(origin).getValue(properties.get(bit)),
                            family + " window connects to solid frame toward " + direction);
                    level.setBlock(neighbor, Blocks.AIR.defaultBlockState(), 3);
                    check(player, !level.getBlockState(origin).getValue(properties.get(bit)),
                            family + " window disconnects toward " + direction);
                }
                level.setBlock(origin.east(), block("cloud_jade_window").defaultBlockState(), 3);
                check(player, level.getBlockState(origin).getValue(BlockStateProperties.EAST)
                                && level.getBlockState(origin.east()).getValue(BlockStateProperties.WEST),
                        family + " window connects both ways with another material window");
                level.setBlock(origin.east(), Blocks.AIR.defaultBlockState(), 3);
                check(player, window.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true).getFluidState().is(FluidTags.WATER),
                        family + " window supports waterlogging");
                check(player, window.defaultBlockState().getLightBlock(level, origin) == 0,
                        family + " window passes skylight like glass");
                level.setBlock(origin, Blocks.AIR.defaultBlockState(), 3);
                Block tiles = block(family + "_roof_tiles");
                check(player, countDrops(player, tiles.defaultBlockState(), origin, tiles) == 1
                                && countDrops(player, tiles.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.DOUBLE), origin, tiles) == 2,
                        family + " roof slab drops one or two for double");
                check(player, tiles.defaultBlockState().setValue(SlabBlock.WATERLOGGED, true).getFluidState().is(FluidTags.WATER),
                        family + " roof slab supports waterlogging");
                Block roofStairs = block(family + "_roof_stairs");
                check(player, roofStairs instanceof StairBlock && countDrops(player, roofStairs.defaultBlockState(), origin, roofStairs) == 1,
                        family + " roof stairs use native stairs and drop one");
                Block ridge = block(family + "_roof_ridge");
                check(player, countDrops(player, ridge.defaultBlockState(), origin, ridge) == 1,
                        family + " ridge drops one");
                for (Direction facing : Direction.Plane.HORIZONTAL) {
                    BlockState ridgeState = ridge.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
                    var expectedShape = facing.getAxis() == Direction.Axis.Z
                            ? Shapes.or(Block.box(0,0,0,16,4,16),Block.box(0,4,4,16,8,12),Block.box(0,8,6,16,10,10))
                            : Shapes.or(Block.box(0,0,0,16,4,16),Block.box(4,4,0,12,8,16),Block.box(6,8,0,10,10,16));
                    check(player, !Shapes.joinIsNotEmpty(ridgeState.getCollisionShape(level, origin),expectedShape,BooleanOp.NOT_SAME),
                            family + " ridge rotated collision " + facing);
                    check(player, ridgeState.rotate(Rotation.CLOCKWISE_90).getValue(BlockStateProperties.HORIZONTAL_FACING) == facing.getClockWise(),
                            family + " ridge rotates correctly " + facing);
                    check(player, ridgeState.mirror(Mirror.LEFT_RIGHT).getValue(BlockStateProperties.HORIZONTAL_FACING) == Mirror.LEFT_RIGHT.mirror(facing),
                            family + " ridge mirrors correctly " + facing);
                }
                check(player, ridge.defaultBlockState().setValue(RoofRidgeBlock.WATERLOGGED,true).getFluidState().is(FluidTags.WATER),
                        family + " ridge supports waterlogging");
                for (String suffix : new String[]{"lamp_stand", "stone_lamp"}) {
                    Block decoration = block(family + "_" + suffix);
                    boolean lit = suffix.equals("stone_lamp");
                    BlockState state = decoration.defaultBlockState();
                    check(player, countDrops(player,state,origin,decoration)==1, family + " " + suffix + " drops itself");
                    check(player, state.getLightEmission()==(lit ? 15 : 0), family + " " + suffix + " native light level");
                    var bounds=state.getCollisionShape(level,origin).bounds();
                    check(player, bounds.getXsize()==(lit ? 0.875 : 0.75)
                                    && bounds.getZsize()==bounds.getXsize() && bounds.getYsize()==1.0,
                            family + " " + suffix + " collision follows sculpted outline");
                    level.setBlock(origin,Blocks.WATER.defaultBlockState(),3);
                    ((BlockItem) decoration.asItem()).place(new BlockPlaceContext(player, InteractionHand.MAIN_HAND,
                            new ItemStack(decoration), new BlockHitResult(Vec3.atCenterOf(origin.below()),Direction.UP,origin.below(),false)));
                    check(player,level.getBlockState(origin).is(decoration)
                                    && level.getBlockState(origin).getValue(BlockStateProperties.WATERLOGGED)
                                    && level.getBlockState(origin).getFluidState().is(FluidTags.WATER),
                            family + " " + suffix + " actual submerged item placement retains water");
                    check(player,level.getBlockEntity(origin)==null, family + " " + suffix + " has no ticking block entity");
                    level.setBlock(origin,Blocks.AIR.defaultBlockState(),3);
                }
                Block stand=block(family+"_lamp_stand"), lamp=block(family+"_stone_lamp");
                level.setBlock(origin,stand.defaultBlockState(),3);
                var result=((BlockItem) lamp.asItem()).place(new BlockPlaceContext(player,InteractionHand.MAIN_HAND,
                        new ItemStack(lamp),new BlockHitResult(Vec3.atBottomCenterOf(origin.above()),Direction.UP,origin,false)));
                check(player,result.consumesAction() && level.getBlockState(origin.above()).is(lamp),
                        family+" actual lamp item places above pedestal");
                var support=level.getBlockState(origin).getCollisionShape(level,origin);
                var lampBottom=level.getBlockState(origin.above()).getCollisionShape(level,origin.above());
                check(player,support.bounds().maxY==1.0 && lampBottom.bounds().minY==0.0,
                        family+" stacked lamp meets pedestal without vertical gap");
                var cap=Block.box(2,14,2,14,16,14);
                check(player,!Shapes.joinIsNotEmpty(cap,support,BooleanOp.ONLY_FIRST),
                        family+" solid pedestal cap supports entire lamp foot");
                level.setBlock(origin.above(),Blocks.AIR.defaultBlockState(),3);
                level.setBlock(origin,Blocks.AIR.defaultBlockState(),3);
                if (family.equals("azure_brick")) {
                for (String plaqueName : new String[]{"wooden_plaque","wooden_plaque_medium","wooden_plaque_large","wooden_plaque_vertical"}) {
                Block plaque = block(plaqueName);
                PlaqueBlock geometry = (PlaqueBlock)plaque;
                check(player, geometry.partCount()==geometry.width()*geometry.height(), plaqueName + " section dimensions");
                check(player, geometry.partProperty().getPossibleValues().size()==geometry.partCount(), plaqueName + " exact state range");
                check(player, countDrops(player, plaque.defaultBlockState(), origin, plaque) == 1,
                        family + " plaque drops itself");
                for (Direction facing : Direction.Plane.HORIZONTAL) {
                    BlockPos supportPos = origin.relative(facing.getOpposite());
                    for (int part = 0; part < geometry.partCount(); part++)
                        level.setBlock(geometry.sectionPosition(supportPos, facing, part), Blocks.STONE.defaultBlockState(), 3);
                    BlockState state = plaque.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
                    check(player, state.canSurvive(level, origin), family + " plaque support " + facing);
                    var placed = ((BlockItem) plaque.asItem()).place(new BlockPlaceContext(player, InteractionHand.MAIN_HAND,
                            new ItemStack(plaque), new BlockHitResult(Vec3.atCenterOf(supportPos).relative(facing, 0.5),
                            facing, supportPos, false)));
                    check(player, placed.consumesAction() && level.getBlockState(origin).is(plaque)
                                    && level.getBlockState(origin).getValue(BlockStateProperties.HORIZONTAL_FACING) == facing,
                            family + " actual wall plaque placement " + facing);
                    for (int part = 0; part < geometry.partCount(); part++) {
                        BlockPos target = geometry.sectionPosition(origin, facing, part);
                        check(player, level.getBlockState(target).is(plaque)
                                && level.getBlockState(target).getValue(geometry.partProperty()) == part,
                                plaqueName + " footprint " + facing + " part " + part);
                        check(player, countDrops(player, level.getBlockState(target), target, plaque) == (part == 0 ? 1 : 0),
                                "plaque single item loot " + facing + " part " + part);
                    }
                    VoxelShape expected = switch (facing) {
                        case NORTH -> Block.box(0,0,13,16,16,16);
                        case EAST -> Block.box(0,0,0,3,16,16);
                        case SOUTH -> Block.box(0,0,0,16,16,3);
                        case WEST -> Block.box(13,0,0,16,16,16);
                        default -> throw new IllegalStateException();
                    };
                    check(player, !Shapes.joinIsNotEmpty(state.getCollisionShape(level, origin), expected, BooleanOp.NOT_SAME),
                            family + " plaque collision matches rotated model " + facing);
                    check(player, state.rotate(Rotation.CLOCKWISE_90).getValue(BlockStateProperties.HORIZONTAL_FACING) == facing.getClockWise()
                                    && state.mirror(Mirror.LEFT_RIGHT).getValue(BlockStateProperties.HORIZONTAL_FACING) == Mirror.LEFT_RIGHT.mirror(facing),
                            family + " plaque rotate/mirror " + facing);
                    level.setBlock(supportPos, Blocks.AIR.defaultBlockState(), 3);
                    check(player, level.getBlockState(origin).isAir(), family + " plaque detaches when support removed " + facing);
                    for (int part = 0; part < geometry.partCount(); part++) {
                        check(player, !level.getBlockState(geometry.sectionPosition(origin, facing, part)).is(plaque),
                                "plaque removes all sections " + facing + " part " + part);
                        level.setBlock(geometry.sectionPosition(supportPos, facing, part), Blocks.AIR.defaultBlockState(), 3);
                    }
                }
                for (int part = 0; part < geometry.partCount(); part++)
                    level.setBlock(geometry.sectionPosition(origin.south(), Direction.NORTH, part), Blocks.STONE.defaultBlockState(), 3);
                level.setBlock(origin, Blocks.WATER.defaultBlockState(), 3);
                ((BlockItem) plaque.asItem()).place(new BlockPlaceContext(player, InteractionHand.MAIN_HAND,
                        new ItemStack(plaque), new BlockHitResult(Vec3.atCenterOf(origin.south()).relative(Direction.NORTH, 0.5),
                        Direction.NORTH, origin.south(), false)));
                check(player, level.getBlockState(origin).is(plaque)
                                && level.getBlockState(origin).getValue(BlockStateProperties.WATERLOGGED)
                                && level.getFluidState(origin).is(FluidTags.WATER), family + " plaque actual underwater placement");
                check(player, level.getBlockEntity(origin) == null, family + " plaque has no ticking block entity");
                int beforePlaqueDrops = level.getEntitiesOfClass(ItemEntity.class,fixtureBounds).stream()
                        .filter(e -> e.getItem().is(plaque.asItem())).mapToInt(e -> e.getItem().getCount()).sum();
                level.destroyBlock(geometry.sectionPosition(origin, Direction.NORTH, geometry.partCount() - 1), true);
                check(player, !level.getBlockState(origin).is(plaque), "breaking non-anchor removes whole plaque");
                int afterPlaqueDrops = level.getEntitiesOfClass(ItemEntity.class,fixtureBounds).stream()
                        .filter(e -> e.getItem().is(plaque.asItem())).mapToInt(e -> e.getItem().getCount()).sum();
                check(player, afterPlaqueDrops-beforePlaqueDrops == 1, "breaking non-anchor drops exactly one whole plaque");
                check(player, plaque.defaultBlockState().getPistonPushReaction() == net.minecraft.world.level.material.PushReaction.BLOCK,
                        "pistons cannot separate plaque sections");
                level.setBlock(origin, Blocks.AIR.defaultBlockState(), 3);
                for (int part = 0; part < geometry.partCount(); part++)
                    level.setBlock(geometry.sectionPosition(origin.south(), Direction.NORTH, part), Blocks.AIR.defaultBlockState(), 3);
                level.setBlock(origin.south(), Blocks.STONE.defaultBlockState(), 3);
                var refused = ((BlockItem) plaque.asItem()).place(new BlockPlaceContext(player, InteractionHand.MAIN_HAND,
                        new ItemStack(plaque), new BlockHitResult(Vec3.atCenterOf(origin.south()).relative(Direction.NORTH, 0.5),
                        Direction.NORTH, origin.south(), false)));
                check(player, !refused.consumesAction() && !level.getBlockState(origin).is(plaque),
                        plaqueName + " rejects incomplete supporting wall");
                level.setBlock(origin.south(), Blocks.AIR.defaultBlockState(), 3);
                }
                }
                Block fragment = block(family + "_broken_pillar");
                check(player, countDrops(player, fragment.defaultBlockState(), origin, fragment) == 1,
                        family + " broken pillar drops one fragment");
                check(player, fragment.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true)
                        .getFluidState().is(FluidTags.WATER), family + " broken pillar waterlogging");
                for (Direction facing : Direction.Plane.HORIZONTAL) {
                    BlockState state = fragment.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
                    VoxelShape expected = Shapes.empty();
                    double[][] boxes = {{2,0,2,12,2,14},{12,0,5,14,2,13},{3,2,3,13,4,13},{4,4,4,12,8,12},{4,8,4,7,13,12},{7,8,8,12,11,12},{7,8,4,10,10,8},{4,13,7,6,16,11},{10,0,0,14,2,2},{0,0,8,2,3,12},{3.2,2.65,2.7,12.7,3.25,3.05},{3.2,2.65,12.85,12.7,3.25,13.2},{3.75,4.1,3.7,4.25,7.8,4.2},{3.75,4.1,11.8,4.25,7.8,12.3},{7.75,4.1,3.7,8.25,7.8,4.2},{7.75,4.1,11.8,8.25,7.8,12.3},{11.75,4.1,3.7,12.25,7.8,4.2},{11.75,4.1,11.8,12.25,7.8,12.3},{4,12.6,4,6.5,13.25,7},{7,10,6,8.5,11.2,8},{9,10,8,11,11.75,10},{6,13,8,6.8,14.2,10}};
                    int turns = switch(facing) { case NORTH -> 0; case EAST -> 1; case SOUTH -> 2; case WEST -> 3; default -> 0; };
                    for (double[] source : boxes) {
                        double[] b = source.clone();
                        for (int i = 0; i < turns; i++) b = new double[]{16-b[5],b[1],b[0],16-b[2],b[4],b[3]};
                        expected = Shapes.or(expected,Block.box(b[0],b[1],b[2],b[3],b[4],b[5]));
                    }
                    check(player, !Shapes.joinIsNotEmpty(state.getCollisionShape(level,origin),expected,BooleanOp.NOT_SAME),
                            family + " broken pillar collision matches geometry " + facing);
                    check(player, state.rotate(Rotation.CLOCKWISE_90).getValue(BlockStateProperties.HORIZONTAL_FACING) == facing.getClockWise(),
                            family + " broken pillar rotation " + facing);
                }
                level.setBlock(origin,Blocks.WATER.defaultBlockState(),3);
                player.setYRot(0);
                var fragmentPlace = ((BlockItem)fragment.asItem()).place(new BlockPlaceContext(player,InteractionHand.MAIN_HAND,
                        new ItemStack(fragment), new BlockHitResult(Vec3.atBottomCenterOf(origin),Direction.UP,origin.below(),false)));
                check(player, fragmentPlace.consumesAction() && level.getBlockState(origin).is(fragment)
                        && level.getBlockState(origin).getValue(BlockStateProperties.WATERLOGGED), family + " actual broken pillar underwater placement");
                check(player, level.getBlockEntity(origin) == null, family + " broken pillar has no block entity");
                level.setBlock(origin,Blocks.AIR.defaultBlockState(),3);
                Block rubble = block(family + "_rubble");
                check(player, countDrops(player,rubble.defaultBlockState(),origin,rubble) == 1, family + " rubble drops itself");
                for (Direction facing : Direction.Plane.HORIZONTAL) {
                    BlockState state = rubble.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,facing);
                    VoxelShape expected = Shapes.empty();
                    double[][] boxes = {{1,0,2,8,3,9},{9,0,1,14,2,6},{7,0,9,15,4,14},
                            {3,3,4,7,6,8},{8,4,10,12,7,13}};
                    int turns = switch(facing) {case NORTH -> 0; case EAST -> 1; case SOUTH -> 2; case WEST -> 3; default -> 0;};
                    for (double[] source : boxes) {
                        double[] b = source.clone();
                        for (int i=0;i<turns;i++) b = new double[]{16-b[5],b[1],b[0],16-b[2],b[4],b[3]};
                        expected = Shapes.or(expected,Block.box(b[0],b[1],b[2],b[3],b[4],b[5]));
                    }
                    check(player,!Shapes.joinIsNotEmpty(state.getCollisionShape(level,origin),expected,BooleanOp.NOT_SAME),
                            family + " rubble collision " + facing);
                    check(player,state.rotate(Rotation.CLOCKWISE_90).getValue(BlockStateProperties.HORIZONTAL_FACING)==facing.getClockWise(),
                            family + " rubble rotation " + facing);
                }
                level.setBlock(origin,Blocks.WATER.defaultBlockState(),3);
                var rubblePlace = ((BlockItem)rubble.asItem()).place(new BlockPlaceContext(player,InteractionHand.MAIN_HAND,
                        new ItemStack(rubble),new BlockHitResult(Vec3.atBottomCenterOf(origin),Direction.UP,origin.below(),false)));
                check(player,rubblePlace.consumesAction() && level.getBlockState(origin).is(rubble)
                        && level.getFluidState(origin).is(FluidTags.WATER), family + " actual rubble underwater placement");
                check(player,level.getBlockEntity(origin)==null,family + " rubble has no block entity");
                level.setBlock(origin,Blocks.AIR.defaultBlockState(),3);
                for (String suffix : new String[]{"pillar_base","pillar_capital"}) {
                    Block trim = block(family+"_"+suffix);
                    check(player,countDrops(player,trim.defaultBlockState(),origin,trim)==1,family+" "+suffix+" loot");
                    var shape = trim.defaultBlockState().getCollisionShape(level,origin);
                    check(player,shape.bounds().minY==0 && shape.bounds().maxY==1,family+" "+suffix+" no vertical gap");
                    boolean base = suffix.equals("pillar_base");
                    check(player,!Shapes.joinIsNotEmpty(base ? Block.box(2,0,2,14,1,14) : Block.box(4,0,4,12,1,12),shape,BooleanOp.ONLY_FIRST)
                            && !Shapes.joinIsNotEmpty(base ? Block.box(4,15,4,12,16,12) : Block.box(2,15,2,14,16,14),shape,BooleanOp.ONLY_FIRST)
                            && shape.bounds().getXsize()==0.75 && shape.bounds().getZsize()==0.75,
                            family+" "+suffix+" narrow shaft contact and 12-unit collar");
                    level.setBlock(origin,Blocks.WATER.defaultBlockState(),3);
                    var placement = ((BlockItem)trim.asItem()).place(new BlockPlaceContext(player,InteractionHand.MAIN_HAND,
                            new ItemStack(trim),new BlockHitResult(Vec3.atBottomCenterOf(origin),Direction.UP,origin.below(),false)));
                    check(player,placement.consumesAction() && level.getBlockState(origin).is(trim)
                            && level.getFluidState(origin).is(FluidTags.WATER),family+" "+suffix+" underwater placement");
                    check(player,level.getBlockEntity(origin)==null,family+" "+suffix+" no block entity");
                    level.setBlock(origin,Blocks.AIR.defaultBlockState(),3);
                }
            }
            for (String family : FAMILIES) {
                for (String suffix : new String[]{"beam","bracket"}) {
                    Block beam = block(family+"_"+suffix);
                    double[][] boxes = suffix.equals("beam")
                            ? new double[][]{{0,0,4,16,6,12},{0,6,3,16,8,13},{0,8,5,16,16,11}}
                            : new double[][]{{4,0,4,12,6,12},{3,6,3,13,12,13},{0,12,4,16,16,12}};
                    for (Direction facing : Direction.Plane.HORIZONTAL) {
                        int quarterTurns=switch(facing){case NORTH->0;case EAST->1;case SOUTH->2;case WEST->3;default->0;};
                        BlockState state = beam.defaultBlockState().rotate(Rotation.values()[quarterTurns]);
                        VoxelShape expected=Shapes.empty();
                        int turns=switch(facing){case NORTH->0;case EAST->1;case SOUTH->2;case WEST->3;default->0;};
                        for (double[] source:boxes) {
                            double[] b=source.clone();
                            for(int i=0;i<turns;i++) b=new double[]{16-b[5],b[1],b[0],16-b[2],b[4],b[3]};
                            expected=Shapes.or(expected,Block.box(b[0],b[1],b[2],b[3],b[4],b[5]));
                        }
                        check(player,!Shapes.joinIsNotEmpty(state.getCollisionShape(level,origin),expected,BooleanOp.NOT_SAME),family+" "+suffix+" rotated geometry "+facing);
                        check(player,state.rotate(Rotation.CLOCKWISE_90).getValue(BlockStateProperties.HORIZONTAL_FACING)==facing.getClockWise()
                                && state.mirror(Mirror.LEFT_RIGHT).getValue(BlockStateProperties.HORIZONTAL_FACING)==Mirror.LEFT_RIGHT.mirror(facing),family+" "+suffix+" rotate/mirror "+facing);
                    }
                    check(player,countDrops(player,beam.defaultBlockState(),origin,beam)==1,family+" "+suffix+" drops one");
                    level.setBlock(origin,Blocks.WATER.defaultBlockState(),3);
                    player.setYRot(0);
                    var placed=((BlockItem)beam.asItem()).place(new BlockPlaceContext(player,InteractionHand.MAIN_HAND,new ItemStack(beam),
                            new BlockHitResult(Vec3.atBottomCenterOf(origin),Direction.UP,origin.below(),false)));
                    check(player,placed.consumesAction() && level.getBlockState(origin).is(beam)
                            && level.getFluidState(origin).is(FluidTags.WATER),family+" "+suffix+" actual underwater placement");
                    check(player,level.getBlockEntity(origin)==null,family+" "+suffix+" no block entity");
                    level.setBlock(origin,Blocks.AIR.defaultBlockState(),3);
                }
                Block beam=block(family+"_beam"), bracket=block(family+"_bracket");
                level.setBlock(origin,bracket.defaultBlockState(),3);
                ((BlockItem)beam.asItem()).place(new BlockPlaceContext(player,InteractionHand.MAIN_HAND,new ItemStack(beam),
                        new BlockHitResult(Vec3.atCenterOf(origin).add(0,0.5,0),Direction.UP,origin,false)));
                ((BlockItem)beam.asItem()).place(new BlockPlaceContext(player,InteractionHand.MAIN_HAND,new ItemStack(beam),
                        new BlockHitResult(Vec3.atCenterOf(origin.above()).add(0.5,0,0),Direction.EAST,origin.above(),false)));
                check(player,level.getBlockState(origin.above()).is(beam) && level.getBlockState(origin.above().east()).is(beam),family+" beams actual adjacent/stacked placement");
                var upper=level.getBlockState(origin.above()).getCollisionShape(level,origin.above()).bounds();
                var lower=level.getBlockState(origin).getCollisionShape(level,origin).bounds();
                check(player,lower.maxY==1 && upper.minY==0 && upper.maxY==1 && upper.minX==0 && upper.maxX==1,family+" beam/bracket and roof-height bearing contact");
                level.setBlock(origin.above().east(),Blocks.AIR.defaultBlockState(),3);
                level.setBlock(origin.above(),Blocks.AIR.defaultBlockState(),3);
                level.setBlock(origin,Blocks.AIR.defaultBlockState(),3);
            }
            checkColumnRoofStack(player, origin);
            checkBeamConnections(player, origin);
            checkEaves(player,origin);
            checkSteles(player, origin);
            AbstractContainerMenu menu = new AbstractContainerMenu(MenuType.CRAFTING, -1) {
                public ItemStack quickMoveStack(Player p, int slot) { return ItemStack.EMPTY; }
                public boolean stillValid(Player p) { return true; }
            };
            CraftingContainer grid = new TransientCraftingContainer(menu, 3, 3);
            checkCrackedMaterials(player, origin, grid);
            checkMossyMaterials(player, origin, grid);
            checkFinishedSteps(player, origin, grid);
            checkJadeTable(player, origin, grid);
            StoneChestIntegration.checkChest(player,origin,grid);
            JadeCabinetIntegration.checkCabinet(player,origin,grid);
            WaterChannelIntegration.checkChannel(player,origin,grid);
            for (String family : FAMILIES) {
                grid.clearContent();
                grid.setItem(0, new ItemStack(block(family + "_carved")));
                grid.setItem(3, new ItemStack(block(family + "_carved")));
                checkRecipe(player, grid, family + "_stele", 2);
                grid.clearContent();
                grid.setItem(0,new ItemStack(block(family+"_pillar")));
                grid.setItem(1,new ItemStack(block(family+"_pillar")));
                checkRecipe(player,grid,family+"_beam",2);
                grid.clearContent();
                grid.setItem(0,new ItemStack(block(family+"_pillar")));
                grid.setItem(8,new ItemStack(block(family+"_pillar_capital")));
                checkRecipe(player,grid,family+"_bracket",2);
                grid.clearContent();
                for(int slot:new int[]{0,3,4}) grid.setItem(slot,new ItemStack(block(family)));
                checkRecipe(player,grid,family+"_eaves",6);
                grid.clearContent();
                for(int slot:new int[]{0,8}) grid.setItem(slot,new ItemStack(block(family+"_eaves")));
                checkRecipe(player,grid,family+"_eaves_corner",2);
                grid.clearContent();
                grid.setItem(0, new ItemStack(block(family)));
                checkRecipe(player, grid, family + "_pillar", 1);
                grid.clearContent();
                grid.setItem(0,new ItemStack(block(family+"_pillar")));
                grid.setItem(3,new ItemStack(block(family+"_pillar")));
                checkRecipe(player, grid, family + "_broken_pillar", 2);
                grid.clearContent();
                grid.setItem(8,new ItemStack(block(family+"_broken_pillar")));
                checkRecipe(player,grid,family+"_rubble",1);
                for (String suffix : new String[]{"pillar_base","pillar_capital"}) {
                    grid.clearContent();
                    grid.setItem(0,new ItemStack(block(family+"_pillar")));
                    grid.setItem(8,new ItemStack(block(family+(suffix.equals("pillar_base")?"_smooth":"_carved"))));
                    checkRecipe(player,grid,family+"_"+suffix,2);
                }
                grid.clearContent();
                grid.setItem(0,new ItemStack(block(family)));
                for (int slot : new int[]{1, 3, 4}) grid.setItem(slot, new ItemStack(block(family)));
                checkRecipe(player, grid, family + "_smooth", 4);
                grid.setItem(6, new ItemStack(block(family)));
                grid.setItem(7, new ItemStack(block(family)));
                checkRecipe(player, grid, family + "_railing", 6);
                grid.clearContent();
                grid.setItem(0, new ItemStack(block(family + "_slab")));
                grid.setItem(3, new ItemStack(block(family + "_slab")));
                checkRecipe(player, grid, family + "_carved", 1);
                grid.clearContent();
                for (int slot : new int[]{0, 1, 3, 4, 6, 7}) grid.setItem(slot, new ItemStack(block(family + "_smooth")));
                checkRecipe(player, grid, family + "_door", 3);
                grid.clearContent();
                for (int slot : new int[]{0, 1, 2, 3, 4, 5}) grid.setItem(slot, new ItemStack(block(family + "_carved")));
                checkRecipe(player, grid, family + "_trapdoor", 2);
                grid.clearContent();
                for (int slot : new int[]{0, 2, 3, 5}) grid.setItem(slot, new ItemStack(block(family)));
                for (int slot : new int[]{1, 4}) grid.setItem(slot, new ItemStack(Items.GLASS));
                checkRecipe(player, grid, family + "_window", 4);
                grid.clearContent();
                for (int slot : new int[]{0,3,6}) grid.setItem(slot,new ItemStack(block(family)));
                checkRecipe(player,grid,family+"_roof_tiles",6);
                grid.clearContent();
                for (int slot : new int[]{0,3,4,6,7,8}) grid.setItem(slot,new ItemStack(block(family+"_roof_tiles")));
                checkRecipe(player,grid,family+"_roof_stairs",4);
                grid.clearContent();
                for (int slot : new int[]{0,4,8}) grid.setItem(slot,new ItemStack(block(family)));
                checkRecipe(player,grid,family+"_roof_ridge",6);
                grid.clearContent();
                for (int slot : new int[]{0,1}) grid.setItem(slot,new ItemStack(block(family)));
                checkRecipe(player,grid,family+"_lamp_stand",2);
                grid.clearContent();
                if (family.equals("azure_brick")) {
                    for (int slot : new int[]{0,3}) grid.setItem(slot,new ItemStack(Items.STRIPPED_DARK_OAK_LOG));
                    checkRecipe(player,grid,"wooden_plaque",2);
                    grid.clearContent();
                    for (int slot : new int[]{0,1,2,3,4,5}) grid.setItem(slot,new ItemStack(Items.STRIPPED_DARK_OAK_LOG));
                    checkRecipe(player,grid,"wooden_plaque_medium",1);
                    for (int slot : new int[]{6,7,8}) grid.setItem(slot,new ItemStack(Items.STRIPPED_DARK_OAK_LOG));
                    checkRecipe(player,grid,"wooden_plaque_large",1);
                    grid.clearContent();
                    for (int slot : new int[]{0,2,3,5}) grid.setItem(slot,new ItemStack(Items.STRIPPED_DARK_OAK_LOG));
                    for (int slot : new int[]{1,4}) grid.setItem(slot,new ItemStack(Items.STICK));
                    checkRecipe(player,grid,"wooden_plaque_vertical",1);
                    grid.clearContent();
                    for (int slot : new int[]{0,1,3,4,6,7}) grid.setItem(slot,new ItemStack(Items.DARK_OAK_PLANKS));
                    ItemStack vanillaDoor = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,grid,player.level())
                            .map(recipe -> recipe.assemble(grid,level.registryAccess())).orElse(ItemStack.EMPTY);
                    check(player,vanillaDoor.is(Items.DARK_OAK_DOOR) && vanillaDoor.getCount()==3,"vanilla dark oak doors remain craftable");
                    grid.clearContent();
                    for (int slot : new int[]{0,3}) grid.setItem(slot,new ItemStack(Items.DARK_OAK_PLANKS));
                    ItemStack vanillaStick = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,grid,player.level())
                            .map(recipe -> recipe.assemble(grid,level.registryAccess())).orElse(ItemStack.EMPTY);
                    check(player,vanillaStick.is(Items.STICK) && vanillaStick.getCount()==4,"vanilla sticks remain craftable with dark oak planks");
                    grid.clearContent();
                    for (int slot : new int[]{0,1,2,3,4,5}) grid.setItem(slot,new ItemStack(Items.DARK_OAK_PLANKS));
                    ItemStack vanillaTrapdoor = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,grid,player.level())
                            .map(recipe -> recipe.assemble(grid,level.registryAccess())).orElse(ItemStack.EMPTY);
                    check(player,vanillaTrapdoor.is(Items.DARK_OAK_TRAPDOOR) && vanillaTrapdoor.getCount()==2,"vanilla dark oak trapdoors remain craftable");
                }
                grid.clearContent();
                grid.setItem(2,new ItemStack(block(family+"_lamp_stand")));
                grid.setItem(6,new ItemStack(Items.TORCH));
                checkRecipe(player,grid,family+"_stone_lamp",1);
            }
            player.sendSystemMessage(Component.literal("[Building QA] All runtime assertions passed; fixture restored"));
            return 1;
        } catch (RuntimeException | LinkageError error) {
            com.mojang.logging.LogUtils.getLogger().error("[Building QA FAIL] Runtime check interrupted", error);
            player.sendSystemMessage(Component.literal("[Building QA FAIL] " + error));
            return 0;
        } finally {
            player.setYRot(originalYaw);
            player.setXRot(originalPitch);
            for (var entry : saved.entrySet()) level.setBlock(entry.getKey(), entry.getValue(), 2);
            for (ItemEntity drop : level.getEntitiesOfClass(ItemEntity.class, fixtureBounds)) {
                if (!originalDrops.contains(((Entity) drop).getUUID())) ((Entity) drop).discard();
            }
        }
    }
    private static void checkColumnRoofStack(ServerPlayer player, BlockPos origin) {
        var level = player.serverLevel();
        for (String family : FAMILIES) {
            Block shaft = block(family+"_pillar");
            for (Direction axisFace : new Direction[]{Direction.UP,Direction.EAST,Direction.SOUTH}) {
                level.setBlock(origin,Blocks.AIR.defaultBlockState(),3);
                var placed = ((BlockItem)shaft.asItem()).place(new BlockPlaceContext(player,InteractionHand.MAIN_HAND,
                        new ItemStack(shaft),new BlockHitResult(Vec3.atCenterOf(origin),axisFace,origin,false)));
                BlockState state = level.getBlockState(origin);
                VoxelShape expected = switch (axisFace.getAxis()) {
                    case X -> Block.box(0,4,4,16,12,12);
                    case Y -> Block.box(4,0,4,12,16,12);
                    case Z -> Block.box(4,4,0,12,12,16);
                };
                check(player,placed.consumesAction() && state.is(shaft)
                        && state.getValue(BlockStateProperties.AXIS)==axisFace.getAxis()
                        && !Shapes.joinIsNotEmpty(state.getCollisionShape(level,origin),expected,BooleanOp.NOT_SAME),
                        family+" half-block shaft actual placement/collision "+axisFace.getAxis());
                level.setBlock(origin,Blocks.AIR.defaultBlockState(),3);
            }
            String[] stack = {"pillar_base","pillar","pillar_capital","bracket","beam","roof_tiles"};
            for (int i=0;i<stack.length;i++) level.setBlock(origin.above(i-1),Blocks.AIR.defaultBlockState(),3);
            for (int i=0;i<stack.length;i++) {
                BlockPos pos = origin.above(i-1);
                Block part = block(family+"_"+stack[i]);
                var placed = ((BlockItem)part.asItem()).place(new BlockPlaceContext(player,InteractionHand.MAIN_HAND,
                        new ItemStack(part),new BlockHitResult(Vec3.atBottomCenterOf(pos),Direction.UP,pos,false)));
                check(player,placed.consumesAction() && level.getBlockState(pos).is(part),family+" full support stack placement "+stack[i]);
                if (i>0) {
                    var lower = level.getBlockState(pos.below()).getCollisionShape(level,pos.below()).toAabbs();
                    var upper = level.getBlockState(pos).getCollisionShape(level,pos).toAabbs();
                    boolean touches = lower.stream().anyMatch(a -> a.maxY==1 && upper.stream().anyMatch(b -> b.minY==0
                            && Math.min(a.maxX,b.maxX)>Math.max(a.minX,b.minX)
                            && Math.min(a.maxZ,b.maxZ)>Math.max(a.minZ,b.minZ)));
                    check(player,touches,family+" full support stack no air gap "+stack[i-1]+" -> "+stack[i]);
                }
            }
            for (int i=0;i<stack.length;i++) level.setBlock(origin.above(i-1),Blocks.AIR.defaultBlockState(),3);
        }
        level.setBlock(origin.below(),Blocks.STONE.defaultBlockState(),3);
    }
    private static void checkSteles(ServerPlayer player, BlockPos origin) {
        var level=player.serverLevel();
        for(String family:FAMILIES) {
            Block block=block(family+"_stele");
            var stele=(com.longju.xiantu.building.StoneSteleBlock)block;
            for(Direction facing:new Direction[]{Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST}) {
                player.setYRot(facing.getOpposite().toYRot());
                for(int part=0;part<6;part++) {
                    BlockPos p=stele.sectionPosition(origin,facing,part);
                    level.setBlock(p,Blocks.AIR.defaultBlockState(),3);
                    if(part<2) level.setBlock(p.below(),Blocks.STONE.defaultBlockState(),3);
                }
                BlockPos blocked=stele.sectionPosition(origin,facing,5);
                level.setBlock(blocked,Blocks.STONE.defaultBlockState(),3);
                ItemStack stack=new ItemStack(block,2);
                var denied=((BlockItem)block.asItem()).place(new BlockPlaceContext(player,InteractionHand.MAIN_HAND,stack,
                        new BlockHitResult(Vec3.atBottomCenterOf(origin),Direction.UP,origin,false)));
                check(player,!denied.consumesAction() && stack.getCount()==2 && level.getBlockState(origin).isAir(),family+" stele occupied top rejects entire placement "+facing);
                level.setBlock(blocked,Blocks.AIR.defaultBlockState(),3);
                for(int breakPart=0;breakPart<6;breakPart++) {
                    for(int part=0;part<6;part++) level.setBlock(stele.sectionPosition(origin,facing,part),Blocks.WATER.defaultBlockState(),3);
                    var placed=((BlockItem)block.asItem()).place(new BlockPlaceContext(player,InteractionHand.MAIN_HAND,
                            new ItemStack(block),new BlockHitResult(Vec3.atBottomCenterOf(origin),Direction.UP,origin,false)));
                    check(player,placed.consumesAction(),family+" large stele actual placement "+facing+" break part="+breakPart);
                    for(int part=0;part<6;part++) {
                        BlockPos p=stele.sectionPosition(origin,facing,part);
                        BlockState state=level.getBlockState(p);
                        check(player,state.is(block) && state.getValue(stele.partProperty())==part
                                && state.getValue(BlockStateProperties.HORIZONTAL_FACING)==facing,
                                family+" stele occupied section "+facing+" part="+part);
                        check(player,state.getFluidState().is(FluidTags.WATER) && level.getBlockEntity(p)==null,
                                family+" stele waterlogged/static section "+part);
                        check(player,countDrops(player,state,p,block)==(part==0?1:0),family+" stele anchor-only loot "+part);
                        var shape=state.getCollisionShape(level,p);
                        check(player,!shape.isEmpty() && shape.max(Direction.Axis.Y)==1,
                                family+" stele real3-block collision part="+part);
                    }
                    var area=new AABB(origin.offset(-4,-1,-4),origin.offset(5,4,5));
                    int before=level.getEntitiesOfClass(ItemEntity.class,area).stream().filter(e->e.getItem().is(block.asItem())).mapToInt(e->e.getItem().getCount()).sum();
                    level.destroyBlock(stele.sectionPosition(origin,facing,breakPart),true);
                    int after=level.getEntitiesOfClass(ItemEntity.class,area).stream().filter(e->e.getItem().is(block.asItem())).mapToInt(e->e.getItem().getCount()).sum();
                    check(player,after-before==1,family+" breaking any stele section drops exactly one "+breakPart);
                    check(player,java.util.stream.IntStream.range(0,6).noneMatch(part->level.getBlockState(stele.sectionPosition(origin,facing,part)).is(block)),
                            family+" any section break removes whole stele "+breakPart);
                    for(int part=0;part<6;part++) level.setBlock(stele.sectionPosition(origin,facing,part),Blocks.AIR.defaultBlockState(),3);
                }
            }
        }
    }

    public static int stelesGallery(ServerPlayer player) {
        if(!player.getServer().getWorldPath(LevelResource.ROOT).normalize().getFileName().toString().startsWith("Xiantu-QA")) return 0;
        var level=player.serverLevel();
        for(int attempt=0;attempt<8;attempt++) {
            int start=3104+attempt*96;
            boolean clear=true;
            for(BlockPos p:BlockPos.betweenClosed(start-20,-60,380,start+44,-56,420))
                if(!level.getBlockState(p).isAir()) {clear=false;break;}
            if(!clear) continue;
            // Actual BlockItem placement reads player yaw; point every front toward the viewer.
            player.setYRot(0);
            for(int i=0;i<FAMILIES.length;i++) {
                BlockPos p=new BlockPos(start+i*5,-60,400);
                Block stele=block(FAMILIES[i]+"_stele");
                var placed=((BlockItem)stele.asItem()).place(new BlockPlaceContext(player,InteractionHand.MAIN_HAND,new ItemStack(stele),
                        new BlockHitResult(Vec3.atBottomCenterOf(p),Direction.UP,p,false)));
                if(!placed.consumesAction()) {
                    player.sendSystemMessage(Component.literal("[Stele gallery] Placement failed at "+p));
                    return 0;
                }
            }
            player.teleportTo(level,start+10,-60,394,0,5);
            player.sendSystemMessage(Component.literal("[Stele gallery] Five2x3 decorative tablets at x"+start+"-"+(start+20)+" y-60 z400"));
            return 1;
        }
        player.sendSystemMessage(Component.literal("Stele gallery site occupied; refusing overwrite")); return 0;
    }

    private static void checkBeamConnections(ServerPlayer player, BlockPos origin) {
        var level=player.serverLevel();
        Direction[] sides={Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST};
        String[] joins={"straight_x","straight_z","straight_x","corner_ne","straight_z","straight_z","corner_se","tee_nes",
                "straight_x","corner_nw","straight_x","tee_wne","corner_sw","tee_swn","tee_esw","cross"};
        player.setYRot(0);
        for (String family:FAMILIES) {
            Block beam=block(family+"_beam");
            for (int mask=0;mask<16;mask++) {
                level.setBlock(origin,mask%2==0?Blocks.WATER.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
                ((BlockItem)beam.asItem()).place(new BlockPlaceContext(player,InteractionHand.MAIN_HAND,new ItemStack(beam),
                        new BlockHitResult(Vec3.atBottomCenterOf(origin),Direction.UP,origin,false)));
                for (int side=0;side<4;side++) if ((mask&(1<<side))!=0) {
                    BlockPos pos=origin.relative(sides[side]);
                    // Cross-family joints are supported, and placement order must not affect the center.
                    Block neighbor=block(FAMILIES[(java.util.Arrays.asList(FAMILIES).indexOf(family)+1)%FAMILIES.length]+"_beam");
                    var placed=((BlockItem)neighbor.asItem()).place(new BlockPlaceContext(player,InteractionHand.MAIN_HAND,new ItemStack(neighbor),
                            new BlockHitResult(Vec3.atBottomCenterOf(pos),Direction.UP,pos,false)));
                    check(player,placed.consumesAction() && level.getBlockState(pos).is(neighbor),family+" beam neighbor actual placement mask="+mask+" "+sides[side]);
                }
                BlockState state=level.getBlockState(origin);
                check(player,((net.minecraft.util.StringRepresentable)state.getValue(ConnectedStoneBeamBlock.JOIN)).getSerializedName().equals(joins[mask]),
                        family+" automatic beam join mask="+mask+" -> "+joins[mask]);
                check(player,state.getValue(BlockStateProperties.WATERLOGGED)==(mask%2==0),family+" join preserves waterlogging mask="+mask);
                for (int side=0;side<4;side++) if ((mask&(1<<side))!=0) {
                    VoxelShape shape=state.getCollisionShape(level,origin);
                    VoxelShape adjacent=level.getBlockState(origin.relative(sides[side])).getCollisionShape(level,origin.relative(sides[side]));
                    for(int[] band:new int[][]{{4,12,0,6},{3,13,6,8},{5,11,8,16}}) {
                        int lo=band[0],hi=band[1],y=band[2],top=band[3];
                        double[][] contact={{lo,y,0,hi,top,1},{15,y,lo,16,top,hi},{lo,y,15,hi,top,16},{0,y,lo,1,top,hi}};
                        double[] a=contact[side], b=contact[(side+2)%4];
                        check(player,!Shapes.joinIsNotEmpty(Block.box(a[0],a[1],a[2],a[3],a[4],a[5]),shape,BooleanOp.ONLY_FIRST)
                                && !Shapes.joinIsNotEmpty(Block.box(b[0],b[1],b[2],b[3],b[4],b[5]),adjacent,BooleanOp.ONLY_FIRST),
                                family+" joined beam full layer contact mask="+mask+" "+sides[side]+" y="+y);
                    }
                }
                // Reversing the entire connection footprint must mirror its physical collision.
                for (Mirror mirror:new Mirror[]{Mirror.LEFT_RIGHT,Mirror.FRONT_BACK}) {
                    VoxelShape expected=Shapes.empty();
                    for(var box:state.getCollisionShape(level,origin).toAabbs())
                        expected=Shapes.or(expected,Shapes.create(new AABB(mirror==Mirror.FRONT_BACK?1-box.maxX:box.minX,box.minY,
                                mirror==Mirror.LEFT_RIGHT?1-box.maxZ:box.minZ,mirror==Mirror.FRONT_BACK?1-box.minX:box.maxX,box.maxY,
                                mirror==Mirror.LEFT_RIGHT?1-box.minZ:box.maxZ)));
                    check(player,!Shapes.joinIsNotEmpty(state.mirror(mirror).getCollisionShape(level,origin),expected,BooleanOp.NOT_SAME),
                            family+" join physical mirror mask="+mask+" "+mirror);
                }
                VoxelShape rotated=Shapes.empty();
                for(var box:state.getCollisionShape(level,origin).toAabbs())
                    rotated=Shapes.or(rotated,Shapes.create(new AABB(1-box.maxZ,box.minY,box.minX,1-box.minZ,box.maxY,box.maxX)));
                check(player,!Shapes.joinIsNotEmpty(state.rotate(Rotation.CLOCKWISE_90).getCollisionShape(level,origin),rotated,BooleanOp.NOT_SAME),
                        family+" join physical rotation mask="+mask);
                level.setBlock(origin,Blocks.AIR.defaultBlockState(),3);
                ((BlockItem)beam.asItem()).place(new BlockPlaceContext(player,InteractionHand.MAIN_HAND,new ItemStack(beam),
                        new BlockHitResult(Vec3.atBottomCenterOf(origin),Direction.UP,origin,false)));
                check(player,level.getBlockState(origin).getValue(ConnectedStoneBeamBlock.JOIN)==state.getValue(ConnectedStoneBeamBlock.JOIN),
                        family+" center-last placement keeps join mask="+mask);
                for (Direction side:sides) level.setBlock(origin.relative(side),Blocks.AIR.defaultBlockState(),3);
                check(player,level.getBlockState(origin).getValue(ConnectedStoneBeamBlock.JOIN)==ConnectedStoneBeamBlock.Join.STRAIGHT_X,
                        family+" removing neighbors restores straight span mask="+mask);
                level.setBlock(origin,Blocks.AIR.defaultBlockState(),3);
            }
        }
    }
    private static void checkEaves(ServerPlayer player,BlockPos origin) {
        var level=player.serverLevel();
        for(String family:FAMILIES) {
            for(String suffix:new String[]{"eaves","eaves_corner"}) {
                Block roof=block(family+"_"+suffix);
                double[][] boxes=suffix.equals("eaves")
                        ?new double[][]{{0,0,0,16,4,16},{0,4,0,16,6,8},{0,6,0,16,8,4}}
                        :new double[][]{{0,0,0,16,4,16},{0,4,0,16,6,8},{0,4,8,8,6,16},{0,6,0,16,8,4},{0,6,4,4,8,16}};
                for(Direction facing:Direction.Plane.HORIZONTAL) {
                    BlockState state=roof.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,facing);
                    int turns=switch(facing){case NORTH->0;case EAST->1;case SOUTH->2;case WEST->3;default->0;};
                    var oriented=new java.util.ArrayList<double[]>();
                    for(double[] source:boxes) {
                        double[] b=source.clone();
                        for(int i=0;i<turns;i++) b=new double[]{16-b[5],b[1],b[0],16-b[2],b[4],b[3]};
                        oriented.add(b);
                    }
                    VoxelShape expected=Shapes.empty();
                    for(double[] b:oriented) expected=Shapes.or(expected,Block.box(b[0],b[1],b[2],b[3],b[4],b[5]));
                    check(player,!Shapes.joinIsNotEmpty(state.getCollisionShape(level,origin),expected,BooleanOp.NOT_SAME),family+" "+suffix+" shape "+facing);
                    check(player,state.rotate(Rotation.CLOCKWISE_90).getValue(BlockStateProperties.HORIZONTAL_FACING)==facing.getClockWise(),family+" "+suffix+" rotation "+facing);
                    for(Mirror mirror:Mirror.values()) {
                        VoxelShape reflected=Shapes.empty();
                        for(double[] source:oriented) {
                            double[] b=source.clone();
                            if(mirror==Mirror.LEFT_RIGHT) {double z=b[2];b[2]=16-b[5];b[5]=16-z;}
                            if(mirror==Mirror.FRONT_BACK) {double x=b[0];b[0]=16-b[3];b[3]=16-x;}
                            reflected=Shapes.or(reflected,Block.box(b[0],b[1],b[2],b[3],b[4],b[5]));
                        }
                        check(player,!Shapes.joinIsNotEmpty(state.mirror(mirror).getCollisionShape(level,origin),reflected,BooleanOp.NOT_SAME),family+" "+suffix+" physical reflection "+facing+" "+mirror);
                    }
                    level.setBlock(origin,Blocks.WATER.defaultBlockState(),3);
                    player.setYRot(facing.getOpposite().toYRot());
                    var place=((BlockItem)roof.asItem()).place(new BlockPlaceContext(player,InteractionHand.MAIN_HAND,new ItemStack(roof),
                            new BlockHitResult(Vec3.atBottomCenterOf(origin),Direction.UP,origin.below(),false)));
                    check(player,place.consumesAction() && level.getBlockState(origin).is(roof)
                            && level.getBlockState(origin).getValue(BlockStateProperties.HORIZONTAL_FACING)==facing
                            && level.getFluidState(origin).is(FluidTags.WATER),family+" "+suffix+" actual underwater facing "+facing);
                    check(player,level.getBlockEntity(origin)==null,family+" "+suffix+" no block entity "+facing);
                    level.setBlock(origin,Blocks.AIR.defaultBlockState(),3);
                }
                var state=roof.defaultBlockState();
                check(player,countDrops(player,state,origin,roof)==1,family+" "+suffix+" one drop");
                check(player,state.getCollisionShape(level,origin).min(Direction.Axis.Y)==0
                        && state.getCollisionShape(level,origin).max(Direction.Axis.Y)==0.5,family+" "+suffix+" half-height outline");
            }
            var corner=block(family+"_eaves_corner").defaultBlockState().getCollisionShape(level,origin);
            var straight=block(family+"_eaves").defaultBlockState();
            var eastSlice=Block.box(15,0,0,16,16,16);
            var southSlice=Block.box(0,0,15,16,16,16);
            check(player,!Shapes.joinIsNotEmpty(Shapes.join(corner,eastSlice,BooleanOp.AND),
                    Shapes.join(straight.getCollisionShape(level,origin),eastSlice,BooleanOp.AND),BooleanOp.NOT_SAME),family+" corner east joins north eaves without gap");
            check(player,!Shapes.joinIsNotEmpty(Shapes.join(corner,southSlice,BooleanOp.AND),
                    Shapes.join(straight.setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.WEST).getCollisionShape(level,origin),southSlice,BooleanOp.AND),BooleanOp.NOT_SAME),family+" corner south joins west eaves without gap");
        }
    }
    private static void checkRecipe(ServerPlayer player, CraftingContainer grid, String name, int count) {
        ItemStack result = player.serverLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, player.level())
                .map(recipe -> recipe.assemble(grid, player.level().registryAccess())).orElse(ItemStack.EMPTY);
        check(player, result.is(block(name).asItem()) && result.getCount() == count, name + " actual crafting grid yields " + count);
    }
    /** A repeatable visual display isolated from the old x=0 and x=30 sample sites. */
    public static int gallery(ServerPlayer player) {
        if (!player.getServer().getWorldPath(LevelResource.ROOT).normalize().getFileName().toString().startsWith("Xiantu-QA")) return 0;
        var level = player.serverLevel();
        var display = new LinkedHashMap<BlockPos, BlockState>();
        for (int family = 0; family < FAMILIES.length; family++) {
            int x = 64 + 5 * family;
            for (int column = 0; column < 3; column++) {
                for (int row = 0; row < 3; row++) {
                    display.put(new BlockPos(x + column, -60 + row, 4),
                            block(FAMILIES[family] + (row == 2 ? "_carved" : "_smooth")).defaultBlockState());
                }
                display.put(new BlockPos(x + column, -60, 2), block(FAMILIES[family] + "_railing").defaultBlockState()
                        .setValue(BlockStateProperties.EAST, column < 2).setValue(BlockStateProperties.WEST, column > 0));
            }
        }
        for (var entry : display.entrySet()) {
            BlockState existing = level.getBlockState(entry.getKey());
            if (level.getBlockEntity(entry.getKey()) != null || (!existing.isAir() && !existing.is(entry.getValue().getBlock()))) {
                player.sendSystemMessage(Component.literal("Building gallery site occupied; refusing to overwrite blocks"));
                return 0;
            }
        }
        for (var entry : display.entrySet()) level.setBlock(entry.getKey(), entry.getValue(), 3);
        player.teleportTo(level, 75, -58, -14, 0, 10);
        player.sendSystemMessage(Component.literal("[Building gallery] Five material families placed at x64-86; west to east: azure, black, jade, water, floating"));
        return 1;
    }
    /** Dedicated opening display, beyond every prior sample wall. */
    public static int openingsGallery(ServerPlayer player) {
        if (!player.getServer().getWorldPath(LevelResource.ROOT).normalize().getFileName().toString().startsWith("Xiantu-QA")) return 0;
        var level = player.serverLevel();
        var display = new LinkedHashMap<BlockPos, BlockState>();
        for (int family = 0; family < FAMILIES.length; family++) {
            int x = 120 + family * 6;
            Block stone = block(FAMILIES[family] + "_smooth");
            for (int col = 0; col < 4; col++) for (int row = 0; row < 3; row++) {
                display.put(new BlockPos(x + col, -60 + row, 4), stone.defaultBlockState());
            }
            Block door = block(FAMILIES[family] + "_door");
            display.put(new BlockPos(x, -60, 4), door.defaultBlockState().setValue(DoorBlock.FACING, Direction.NORTH));
            display.put(new BlockPos(x, -59, 4), door.defaultBlockState().setValue(DoorBlock.FACING, Direction.NORTH).setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
            display.put(new BlockPos(x + 2, -59, 4), block(FAMILIES[family] + "_window").defaultBlockState());
            display.put(new BlockPos(x + 2, -60, 2), block(FAMILIES[family] + "_trapdoor").defaultBlockState());
        }
        for (var entry : display.entrySet()) {
            BlockState old = level.getBlockState(entry.getKey());
            if (level.getBlockEntity(entry.getKey()) != null || (!old.isAir() && !old.is(entry.getValue().getBlock()))) {
                player.sendSystemMessage(Component.literal("Opening gallery site occupied; refusing overwrite"));
                return 0;
            }
        }
        for (var entry : display.entrySet()) level.setBlock(entry.getKey(), entry.getValue(), 2);
        for (var entry : display.entrySet()) level.updateNeighborsAt(entry.getKey(), entry.getValue().getBlock());
        player.teleportTo(level, 134, -59, -12, 0, 5);
        player.sendSystemMessage(Component.literal("[Building openings] Display x120-147, west to east: azure/black/jade/water/floating"));
        return 1;
    }
    /** Large connected windows at least 30 blocks from the preceding openings gallery. */
    public static int windowsGallery(ServerPlayer player) {
        if (!player.getServer().getWorldPath(LevelResource.ROOT).normalize().getFileName().toString().startsWith("Xiantu-QA")) return 0;
        var level = player.serverLevel();
        var display = new LinkedHashMap<BlockPos, BlockState>();
        for (int family = 0; family < FAMILIES.length; family++) {
            int x = 180 + family * 7;
            for (int col = 0; col < 5; col++) for (int row = 0; row < 4; row++) {
                boolean pane = col > 0 && col < 4 && row > 0 && row < 3;
                BlockState state = block(FAMILIES[family] + (pane ? "_window" : "_smooth")).defaultBlockState();
                if (pane) state = state.setValue(BlockStateProperties.EAST, true).setValue(BlockStateProperties.WEST, true);
                display.put(new BlockPos(x + col, -60 + row, 4), state);
            }
        }
        for (var entry : display.entrySet()) {
            BlockState old = level.getBlockState(entry.getKey());
            if (level.getBlockEntity(entry.getKey()) != null || (!old.isAir() && !old.is(entry.getValue().getBlock()))) {
                player.sendSystemMessage(Component.literal("Window gallery site occupied; refusing overwrite"));
                return 0;
            }
        }
        for (var entry : display.entrySet()) level.setBlock(entry.getKey(), entry.getValue(), 3);
        player.teleportTo(level, 197, -58, -10, 0, 4);
        player.sendSystemMessage(Component.literal("[Building windows] Five 3x2 connected windows at x180-212"));
        return 1;
    }
    public static int roofsGallery(ServerPlayer player) {
        if (!player.getServer().getWorldPath(LevelResource.ROOT).normalize().getFileName().toString().startsWith("Xiantu-QA")) return 0;
        var level = player.serverLevel();
        var display = new LinkedHashMap<BlockPos,BlockState>();
        for (int family=0;family<FAMILIES.length;family++) {
            int x=360+family*11;
            for (int z=60;z<=64;z++) {
                display.put(new BlockPos(x,-58,z),block(FAMILIES[family]+"_roof_stairs").defaultBlockState().setValue(StairBlock.FACING,Direction.EAST));
                display.put(new BlockPos(x+1,-57,z),block(FAMILIES[family]+"_roof_stairs").defaultBlockState().setValue(StairBlock.FACING,Direction.EAST));
                display.put(new BlockPos(x+2,-56,z),block(FAMILIES[family]+"_roof_ridge").defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.EAST));
                display.put(new BlockPos(x+3,-57,z),block(FAMILIES[family]+"_roof_stairs").defaultBlockState().setValue(StairBlock.FACING,Direction.WEST));
                display.put(new BlockPos(x+4,-58,z),block(FAMILIES[family]+"_roof_stairs").defaultBlockState().setValue(StairBlock.FACING,Direction.WEST));
                display.put(new BlockPos(x,-59,z),block(FAMILIES[family]+"_roof_tiles").defaultBlockState());
                display.put(new BlockPos(x+4,-59,z),block(FAMILIES[family]+"_roof_tiles").defaultBlockState());
            }
            for (int col : new int[]{1,3}) for (int z : new int[]{61,63}) for (int y=-60;y<=-58;y++)
                display.put(new BlockPos(x+col,y,z),block(FAMILIES[family]+"_pillar").defaultBlockState());
        }
        // Include a 20-block clearance band, so villages and old fixtures cannot merge with the display.
        for (BlockPos pos : BlockPos.betweenClosed(340,-60,40,428,-35,84)) {
            BlockState expected=display.get(pos);
            BlockState old=level.getBlockState(pos);
            if (level.getBlockEntity(pos)!=null || (!old.isAir() && (expected==null || !old.is(expected.getBlock())))) {
                player.sendSystemMessage(Component.literal("Roof gallery clearance occupied at " + pos.toShortString() + "; refusing overwrite")); return 0;
            }
        }
        for (var entry:display.entrySet()) level.setBlock(entry.getKey(),entry.getValue(),3);
        player.teleportTo(level,384,-55,40,0,10);
        player.sendSystemMessage(Component.literal("[Building roofs] Five roof samples at x360-408 z60-64; west to east azure/black/jade/water/floating"));
        return 1;
    }
    /** Each new display uses a clear site including a twenty-block margin. */
    public static int lampsGallery(ServerPlayer player) {
        if (!player.getServer().getWorldPath(LevelResource.ROOT).normalize().getFileName().toString().startsWith("Xiantu-QA")) return 0;
        var level=player.serverLevel();
        for (int attempt=0;attempt<8;attempt++) {
            int start=480+attempt*96;
            var display=new LinkedHashMap<BlockPos,BlockState>();
            for (int family=0;family<FAMILIES.length;family++) {
                int x=start+family*6;
                display.put(new BlockPos(x,-60,80),block(FAMILIES[family]+"_lamp_stand").defaultBlockState());
                display.put(new BlockPos(x+2,-60,80),block(FAMILIES[family]+"_stone_lamp").defaultBlockState());
                display.put(new BlockPos(x+4,-60,80),block(FAMILIES[family]+"_lamp_stand").defaultBlockState());
                display.put(new BlockPos(x+4,-59,80),block(FAMILIES[family]+"_stone_lamp").defaultBlockState());
            }
            boolean clear=true;
            for (BlockPos pos:BlockPos.betweenClosed(start-20,-60,60,start+48,-35,100)) {
                BlockState expected=display.get(pos), old=level.getBlockState(pos);
                if (level.getBlockEntity(pos)!=null || (!old.isAir() && (expected==null || !old.is(expected.getBlock())))) {
                    clear=false; break;
                }
            }
            if (!clear) continue;
            for (var entry:display.entrySet()) level.setBlock(entry.getKey(),entry.getValue(),3);
            player.teleportTo(level,start+13,-60,72,0,0);
            player.sendSystemMessage(Component.literal("[Building lamps] Five pedestal/lantern/stacked samples at x"+start+"-"+(start+28)+" y-60 z80; west to east azure/black/jade/water/floating"));
            return 1;
        }
        player.sendSystemMessage(Component.literal("Lamp gallery found no clear site; no blocks placed"));
        return 0;
    }
    public static int plaquesGallery(ServerPlayer player) {
        if (!player.getServer().getWorldPath(LevelResource.ROOT).normalize().getFileName().toString().startsWith("Xiantu-QA")) return 0;
        var level = player.serverLevel();
        for (int attempt = 0; attempt < 8; attempt++) {
            int start = 1984 + attempt * 160;
            var display = new LinkedHashMap<BlockPos, BlockState>();
            String[] signs = {"wooden_plaque","wooden_plaque_medium","wooden_plaque_large","wooden_plaque_vertical"};
            int[] offsets = {0,12,28,48};
            for (int i=0;i<signs.length;i++) {
                int x=start+offsets[i];
                PlaqueBlock plaque=(PlaqueBlock)block(signs[i]);
                for (int col=0;col<plaque.width();col++) for (int row=0;row<plaque.height()+1;row++)
                    display.put(new BlockPos(x+col,-60+row,160),block("cloud_jade_smooth").defaultBlockState());
                BlockPos anchor=new BlockPos(x+plaque.width()-1,-60,159);
                for (int part=0;part<plaque.partCount();part++)
                    display.put(plaque.sectionPosition(anchor,Direction.NORTH,part),
                            ((Block)plaque).defaultBlockState().setValue(plaque.partProperty(),part));
            }
            for (int family=0;family<FAMILIES.length;family++) {
                int x=start+4+family*10;
                display.put(new BlockPos(x,-60,178),block(FAMILIES[family]+"_pillar_base").defaultBlockState());
                for (int y=-59;y<=-58;y++) display.put(new BlockPos(x,y,178),block(FAMILIES[family]+"_pillar").defaultBlockState());
                display.put(new BlockPos(x,-57,178),block(FAMILIES[family]+"_pillar_capital").defaultBlockState());
                display.put(new BlockPos(x+1,-60,178),block(FAMILIES[family]+"_rubble").defaultBlockState());
                display.put(new BlockPos(x+2,-60,178),block(FAMILIES[family]+"_broken_pillar").defaultBlockState());
            }
            boolean clear = true;
            for (BlockPos pos : BlockPos.betweenClosed(start - 20, -60, 139, start + 70, -35, 198)) {
                BlockState expected = display.get(pos), old = level.getBlockState(pos);
                if (level.getBlockEntity(pos) != null || (!old.isAir() && (expected == null || !old.is(expected.getBlock())))) {
                    clear = false;
                    break;
                }
            }
            if (!clear) continue;
            for (var entry : display.entrySet()) level.setBlock(entry.getKey(), entry.getValue(), 3);
            player.teleportTo(level, start + 25, -59, 143, 0, 0);
            player.sendSystemMessage(Component.literal("[Building plaques] 4x2, 6x2, 8x3 and 2x4 plaques at x" + start + "-" + (start + 49) + " z159; column trims at z178; no text editing"));
            return 1;
        }
        player.sendSystemMessage(Component.literal("Plaque gallery found no clear site; no blocks placed"));
        return 0;
    }

    public static int beamsGallery(ServerPlayer player) {
        if (!player.getServer().getWorldPath(LevelResource.ROOT).normalize().getFileName().toString().startsWith("Xiantu-QA")) return 0;
        var level=player.serverLevel();
        for(int attempt=0;attempt<8;attempt++) {
            int start=2304+attempt*160;
            var display=new LinkedHashMap<BlockPos,BlockState>();
            for(int i=0;i<FAMILIES.length;i++) {
                int x=start+i*12;
                String f=FAMILIES[i];
                for(int dx:new int[]{0,4}) {
                    display.put(new BlockPos(x+dx,-60,240),block(f+"_pillar_base").defaultBlockState());
                    for(int y=-59;y<=-58;y++) display.put(new BlockPos(x+dx,y,240),block(f+"_pillar").defaultBlockState());
                    display.put(new BlockPos(x+dx,-57,240),block(f+"_pillar_capital").defaultBlockState());
                    display.put(new BlockPos(x+dx,-56,240),block(f+"_bracket").defaultBlockState());
                }
                for(int dx=0;dx<=4;dx++) display.put(new BlockPos(x+dx,-55,240),block(f+"_beam").defaultBlockState());
            }
            boolean clear=true;
            for(BlockPos p:BlockPos.betweenClosed(start-20,-60,220,start+72,-35,260)) {
                BlockState old=level.getBlockState(p), expected=display.get(p);
                if(level.getBlockEntity(p)!=null || (!old.isAir() && (expected==null || !old.is(expected.getBlock())))) {clear=false;break;}
            }
            if(!clear) continue;
            for(var e:display.entrySet()) level.setBlock(e.getKey(),e.getValue(),3);
            player.teleportTo(level,start+26,-59,224,0,0);
            player.sendSystemMessage(Component.literal("[Building beams] Five joined porticos at x"+start+"-"+(start+52)+" y-60..-55 z240; west to east azure/black/jade/water/floating"));
            return 1;
        }
        player.sendSystemMessage(Component.literal("Beam gallery found no clear site; no blocks placed"));
        return 0;
    }
    public static int eavesGallery(ServerPlayer player) {
        if(!player.getServer().getWorldPath(LevelResource.ROOT).normalize().getFileName().toString().startsWith("Xiantu-QA")) return 0;
        var level=player.serverLevel();
        for(int attempt=0;attempt<8;attempt++) {
            int start=2688+attempt*192;
            var display=new LinkedHashMap<BlockPos,BlockState>();
            for(int i=0;i<FAMILIES.length;i++) {
                int x=start+i*14;
                String f=FAMILIES[i];
                for(int dx:new int[]{1,5}) for(int dz:new int[]{1,5}) {
                    display.put(new BlockPos(x+dx,-60,320+dz),block(f+"_pillar_base").defaultBlockState());
                    for(int y=-59;y<=-58;y++) display.put(new BlockPos(x+dx,y,320+dz),block(f+"_pillar").defaultBlockState());
                    display.put(new BlockPos(x+dx,-57,320+dz),block(f+"_pillar_capital").defaultBlockState());
                    display.put(new BlockPos(x+dx,-56,320+dz),block(f+"_bracket").defaultBlockState());
                }
                for(int dx=1;dx<=5;dx++) for(int dz=1;dz<=5;dz++) {
                    display.put(new BlockPos(x+dx,-54,320+dz),block(f+"_roof_tiles").defaultBlockState());
                    if(dx==1 || dx==5 || dz==1 || dz==5) {
                        Direction facing=(dz==1 || dz==5)?Direction.NORTH:Direction.EAST;
                        display.put(new BlockPos(x+dx,-55,320+dz),block(f+"_beam").defaultBlockState().rotate(facing==Direction.NORTH?Rotation.NONE:Rotation.CLOCKWISE_90));
                    }
                }
                for(int k=1;k<=5;k++) {
                    for(Direction side:Direction.Plane.HORIZONTAL) {
                        int dx=side==Direction.WEST?0:side==Direction.EAST?6:k;
                        int dz=side==Direction.NORTH?0:side==Direction.SOUTH?6:k;
                        display.put(new BlockPos(x+dx,-54,320+dz),block(f+"_eaves").defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,side));
                    }
                }
                int[][] corners={{0,0},{6,0},{6,6},{0,6}};
                Direction[] facing={Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST};
                for(int k=0;k<4;k++) display.put(new BlockPos(x+corners[k][0],-54,320+corners[k][1]),
                        block(f+"_eaves_corner").defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,facing[k]));
            }
            boolean clear=true;
            for(BlockPos p:BlockPos.betweenClosed(start-20,-60,300,start+82,-35,346)) {
                var old=level.getBlockState(p);var expected=display.get(p);
                if(level.getBlockEntity(p)!=null || (!old.isAir() && (expected==null || !old.is(expected.getBlock())))) {clear=false;break;}
            }
            if(!clear) continue;
            for(var entry:display.entrySet()) level.setBlock(entry.getKey(),entry.getValue(),3);
            // Explicit templates set blockstates directly, so settle all joins after the whole ring exists.
            for(var entry:display.entrySet()) if(entry.getValue().getBlock() instanceof ConnectedStoneBeamBlock)
                level.setBlock(entry.getKey(),Block.updateFromNeighbourShapes(level.getBlockState(entry.getKey()),level,entry.getKey()),3);
            player.teleportTo(level,start+31,-60,304,0,-10);
            player.sendSystemMessage(Component.literal("[Building eaves] Five 7x7 sample pavilions at x"+start+"-"+(start+62)+" z320-326; straight edges and four outer corners; no ruin generation"));
            return 1;
        }
        player.sendSystemMessage(Component.literal("Eaves gallery found no clear site; no blocks placed"));
        return 0;
    }
    private static void checkCrackedMaterials(ServerPlayer player, BlockPos origin, CraftingContainer grid) {
        var level = player.serverLevel();
        for (String family : FAMILIES) {
            Block cracked = block(family + "_cracked");
            Block stairs = block(family + "_cracked_stairs");
            Block slab = block(family + "_cracked_slab");
            var cooking = level.getRecipeManager().getRecipeFor(RecipeType.SMELTING,
                    new net.minecraft.world.SimpleContainer(new ItemStack(block(family))), level);
            check(player, cooking.isPresent() && cooking.get().getResultItem(level.registryAccess()).is(cracked.asItem())
                    && cooking.get().getCookingTime() == 200, family + " base stone smelts into one cracked block");
            grid.clearContent();
            for (int slot : new int[]{0,3,4,6,7,8}) grid.setItem(slot, new ItemStack(cracked));
            checkRecipe(player, grid, family + "_cracked_stairs", 4);
            grid.clearContent();
            for (int slot : new int[]{3,4,5}) grid.setItem(slot, new ItemStack(cracked));
            checkRecipe(player, grid, family + "_cracked_slab", 6);
            for (Block material : new Block[]{cracked,stairs,slab}) {
                check(player,countDrops(player,material.defaultBlockState(),origin,material)==1,
                        family + " cracked material single drop " + ForgeRegistries.BLOCKS.getKey(material));
            }
            check(player,countDrops(player,slab.defaultBlockState().setValue(SlabBlock.TYPE,SlabType.DOUBLE),origin,slab)==2,
                    family + " cracked double slab drops two");
            level.setBlock(origin,Blocks.WATER.defaultBlockState(),3);
            var placed=((BlockItem)slab.asItem()).place(new BlockPlaceContext(player,InteractionHand.MAIN_HAND,
                    new ItemStack(slab),new BlockHitResult(Vec3.atBottomCenterOf(origin),Direction.UP,origin.below(),false)));
            check(player,placed.consumesAction() && level.getBlockState(origin).is(slab)
                    && level.getFluidState(origin).is(FluidTags.WATER),family+" cracked slab underwater placement");
            level.setBlock(origin,Blocks.WATER.defaultBlockState(),3);
            placed=((BlockItem)stairs.asItem()).place(new BlockPlaceContext(player,InteractionHand.MAIN_HAND,
                    new ItemStack(stairs),new BlockHitResult(Vec3.atBottomCenterOf(origin),Direction.UP,origin.below(),false)));
            check(player,placed.consumesAction() && level.getBlockState(origin).is(stairs)
                    && level.getFluidState(origin).is(FluidTags.WATER),family+" cracked stairs underwater placement");
            check(player,level.getBlockEntity(origin)==null,family+" cracked building material stays static");
            level.setBlock(origin,Blocks.AIR.defaultBlockState(),3);
        }
        grid.clearContent();
    }

    public static int crackedGallery(ServerPlayer player) {
        if(!player.getServer().getWorldPath(LevelResource.ROOT).normalize().getFileName().toString().startsWith("Xiantu-QA")) return 0;
        var level=player.serverLevel();
        for(int attempt=0;attempt<8;attempt++) {
            int start=3504+attempt*128;
            boolean clear=true;
            for(BlockPos p:BlockPos.betweenClosed(start-20,-60,460,start+68,-55,504))
                if(!level.getBlockState(p).isAir()) {clear=false;break;}
            if(!clear) continue;
            for(int i=0;i<FAMILIES.length;i++) {
                int x=start+i*10;String family=FAMILIES[i];
                // A small weathered wall beside intact material, with chipped steps.
                for(int dx=0;dx<3;dx++) for(int dy=0;dy<3;dy++)
                    level.setBlock(new BlockPos(x+dx,-60+dy,480),block(family+"_cracked").defaultBlockState(),3);
                for(int dy=0;dy<3;dy++) level.setBlock(new BlockPos(x+3,-60+dy,480),block(family).defaultBlockState(),3);
                level.setBlock(new BlockPos(x,-60,479),block(family+"_cracked_stairs").defaultBlockState()
                        .setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.SOUTH),3);
                level.setBlock(new BlockPos(x+1,-60,479),block(family+"_cracked_slab").defaultBlockState(),3);
            }
            player.teleportTo(level,start+22,-59,469,0,5);
            player.sendSystemMessage(Component.literal("[Cracked gallery] Five cracked material samples at x"+start+"-"+(start+43)+" z479-480"));
            return 1;
        }
        player.sendSystemMessage(Component.literal("Cracked gallery occupied; no blocks changed"));return 0;
    }
    private static void checkMossyMaterials(ServerPlayer player, BlockPos origin, CraftingContainer grid) {
        var level=player.serverLevel();
        for(String family:FAMILIES) {
            Block base=block(family+"_mossy"), stairs=block(family+"_mossy_stairs"), slab=block(family+"_mossy_slab"), wall=block(family+"_mossy_wall");
            grid.clearContent();grid.setItem(0,new ItemStack(block(family)));grid.setItem(8,new ItemStack(Items.VINE));
            checkRecipe(player,grid,family+"_mossy",1);
            grid.clearContent();for(int slot:new int[]{0,3,4,6,7,8})grid.setItem(slot,new ItemStack(base));
            checkRecipe(player,grid,family+"_mossy_stairs",4);
            grid.clearContent();for(int slot:new int[]{0,1,2})grid.setItem(slot,new ItemStack(base));
            checkRecipe(player,grid,family+"_mossy_slab",6);
            for(int slot:new int[]{3,4,5})grid.setItem(slot,new ItemStack(base));
            checkRecipe(player,grid,family+"_mossy_wall",6);
            for(Block b:new Block[]{base,stairs,slab,wall})
                check(player,countDrops(player,b.defaultBlockState(),origin,b)==1,family+" moss-weathered single drop "+ForgeRegistries.BLOCKS.getKey(b));
            check(player,countDrops(player,slab.defaultBlockState().setValue(SlabBlock.TYPE,SlabType.DOUBLE),origin,slab)==2,family+" moss-weathered double slab drops two");
            for(Block b:new Block[]{stairs,slab,wall}) {
                level.setBlock(origin,Blocks.WATER.defaultBlockState(),3);
                var result=((BlockItem)b.asItem()).place(new BlockPlaceContext(player,InteractionHand.MAIN_HAND,new ItemStack(b),
                        new BlockHitResult(Vec3.atBottomCenterOf(origin),Direction.UP,origin.below(),false)));
                check(player,result.consumesAction() && level.getBlockState(origin).is(b) && level.getFluidState(origin).is(FluidTags.WATER),family+" moss-weathered underwater placement "+ForgeRegistries.BLOCKS.getKey(b));
                check(player,level.getBlockEntity(origin)==null,family+" moss-weathered static material");
                level.setBlock(origin,Blocks.AIR.defaultBlockState(),3);
            }
            level.setBlock(origin,wall.defaultBlockState(),3);
            level.setBlock(origin.east(),Blocks.COBBLESTONE_WALL.defaultBlockState(),3);
            check(player,level.getBlockState(origin).getValue(WallBlock.EAST_WALL)!=net.minecraft.world.level.block.state.properties.WallSide.NONE
                    && level.getBlockState(origin.east()).getValue(WallBlock.WEST_WALL)!=net.minecraft.world.level.block.state.properties.WallSide.NONE,
                    family+" moss-weathered wall connects both ways to vanilla wall");
            level.setBlock(origin.east(),Blocks.AIR.defaultBlockState(),3);
            check(player,level.getBlockState(origin).getValue(WallBlock.EAST_WALL)==net.minecraft.world.level.block.state.properties.WallSide.NONE,
                    family+" moss-weathered wall updates after neighbor removal");
            level.setBlock(origin,Blocks.AIR.defaultBlockState(),3);
        }
        grid.clearContent();
    }

    public static int mossyGallery(ServerPlayer player) {
        if(!player.getServer().getWorldPath(LevelResource.ROOT).normalize().getFileName().toString().startsWith("Xiantu-QA"))return 0;
        var level=player.serverLevel();
        for(int attempt=0;attempt<8;attempt++) {
            int start=3760+attempt*128;boolean clear=true;
            for(BlockPos p:BlockPos.betweenClosed(start-20,-60,540,start+70,-55,586))
                if(!level.getBlockState(p).isAir()){clear=false;break;}
            if(!clear)continue;
            for(int i=0;i<FAMILIES.length;i++) {
                int x=start+i*10;String f=FAMILIES[i];
                for(int dx=0;dx<3;dx++)for(int dy=0;dy<3;dy++)
                    level.setBlock(new BlockPos(x+dx,-60+dy,560),block(f+"_mossy").defaultBlockState(),3);
                for(int dy=0;dy<3;dy++)level.setBlock(new BlockPos(x+3,-60+dy,560),block(f).defaultBlockState(),3);
                level.setBlock(new BlockPos(x,-60,559),block(f+"_mossy_stairs").defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.SOUTH),3);
                level.setBlock(new BlockPos(x+1,-60,559),block(f+"_mossy_slab").defaultBlockState(),3);
                for(int dz=0;dz<3;dz++)level.setBlock(new BlockPos(x+4,-60,558+dz),block(f+"_mossy_wall").defaultBlockState(),3);
            }
            player.teleportTo(level,start+21.5,-60,555,0,0);
            player.sendSystemMessage(Component.literal("[Mossy gallery] Five moss-weathered walls and steps at x"+start+"-"+(start+44)+" z558-560"));return 1;
        }
        player.sendSystemMessage(Component.literal("Mossy gallery occupied; no blocks changed"));return 0;
    }
    private static void checkFinishedSteps(ServerPlayer player, BlockPos origin, CraftingContainer grid) {
        var level=player.serverLevel();
        for(String family:FAMILIES) for(String finish:new String[]{"smooth","carved"}) {
            String id=family+"_"+finish;Block base=block(id),stairs=block(id+"_stairs"),slab=block(id+"_slab");
            grid.clearContent();for(int slot:new int[]{0,3,4,6,7,8})grid.setItem(slot,new ItemStack(base));
            checkRecipe(player,grid,id+"_stairs",4);
            grid.clearContent();for(int slot:new int[]{0,1,2})grid.setItem(slot,new ItemStack(base));
            checkRecipe(player,grid,id+"_slab",6);
            check(player,countDrops(player,stairs.defaultBlockState(),origin,stairs)==1,id+" finished stairs single drop");
            check(player,countDrops(player,slab.defaultBlockState(),origin,slab)==1,id+" finished slab single drop");
            check(player,countDrops(player,slab.defaultBlockState().setValue(SlabBlock.TYPE,SlabType.DOUBLE),origin,slab)==2,id+" finished double slab drops two");
            for(Block b:new Block[]{stairs,slab}) {
                level.setBlock(origin,Blocks.WATER.defaultBlockState(),3);
                var result=((BlockItem)b.asItem()).place(new BlockPlaceContext(player,InteractionHand.MAIN_HAND,new ItemStack(b),
                        new BlockHitResult(Vec3.atBottomCenterOf(origin),Direction.UP,origin.below(),false)));
                check(player,result.consumesAction() && level.getBlockState(origin).is(b) && level.getFluidState(origin).is(FluidTags.WATER),id+" finished underwater placement "+ForgeRegistries.BLOCKS.getKey(b));
                check(player,level.getBlockEntity(origin)==null,id+" finished static step");
                level.setBlock(origin,Blocks.AIR.defaultBlockState(),3);
            }
            for(Direction face:Direction.Plane.HORIZONTAL) {
                BlockState placed=stairs.defaultBlockState().setValue(StairBlock.FACING,face);
                level.setBlock(origin,placed,3);
                level.setBlock(origin.relative(face),stairs.defaultBlockState().setValue(StairBlock.FACING,face.getClockWise()),3);
                check(player,level.getBlockState(origin).getValue(StairBlock.SHAPE)!=net.minecraft.world.level.block.state.properties.StairsShape.STRAIGHT,id+" finished stairs corner "+face);
                level.setBlock(origin.relative(face),Blocks.AIR.defaultBlockState(),3);
                check(player,level.getBlockState(origin).getValue(StairBlock.SHAPE)==net.minecraft.world.level.block.state.properties.StairsShape.STRAIGHT,id+" finished stairs removed corner "+face);
                level.setBlock(origin,Blocks.AIR.defaultBlockState(),3);
            }
        }
        grid.clearContent();
    }

    public static int finishedStepsGallery(ServerPlayer player) {
        if(!player.getServer().getWorldPath(LevelResource.ROOT).normalize().getFileName().toString().startsWith("Xiantu-QA"))return 0;
        var level=player.serverLevel();
        for(int attempt=0;attempt<8;attempt++) {
            int start=4096+attempt*128;boolean clear=true;
            for(BlockPos p:BlockPos.betweenClosed(start-20,-60,620,start+70,-54,666))
                if(!level.getBlockState(p).isAir()){clear=false;break;}
            if(!clear)continue;
            for(int i=0;i<FAMILIES.length;i++) for(int row=0;row<2;row++) {
                String id=FAMILIES[i]+(row==0?"_smooth":"_carved");int x=start+i*10,z=640+row*4;
                for(int dx=0;dx<4;dx++) {
                    level.setBlock(new BlockPos(x+dx,-60,z),block(id+"_slab").defaultBlockState(),3);
                    level.setBlock(new BlockPos(x+dx,-60,z+1),block(id+"_stairs").defaultBlockState().setValue(StairBlock.FACING,Direction.SOUTH),3);
                    level.setBlock(new BlockPos(x+dx,-60,z+2),block(id).defaultBlockState(),3);
                    level.setBlock(new BlockPos(x+dx,-59,z+2),block(id+"_slab").defaultBlockState(),3);
                }
            }
            player.teleportTo(level,start+21.5,-58,635,0,15);
            player.sendSystemMessage(Component.literal("[Finished steps gallery] Smooth/carved stairs and slabs at x"+start+"-"+(start+43)+" z640-646"));return 1;
        }
        player.sendSystemMessage(Component.literal("Finished steps gallery occupied; no blocks changed"));return 0;
    }
    private static void checkJadeTable(ServerPlayer player,BlockPos origin,CraftingContainer grid) {
        var level=player.serverLevel();Block b=block("cloud_jade_table");PlaqueBlock table=(PlaqueBlock)b;
        grid.clearContent();for(int slot:new int[]{0,1,2})grid.setItem(slot,new ItemStack(block("cloud_jade_carved")));
        for(int slot:new int[]{3,5})grid.setItem(slot,new ItemStack(block("cloud_jade_smooth")));
        checkRecipe(player,grid,"cloud_jade_table",1);grid.clearContent();
        for(Direction facing:Direction.Plane.HORIZONTAL) {
            player.setYRot(facing.getOpposite().toYRot());
            for(int part=0;part<2;part++) {
                BlockPos p=table.sectionPosition(origin,facing,part);level.setBlock(p,Blocks.AIR.defaultBlockState(),3);level.setBlock(p.below(),Blocks.STONE.defaultBlockState(),3);
            }
            BlockPos far=table.sectionPosition(origin,facing,1);
            level.setBlock(far,Blocks.STONE.defaultBlockState(),3);ItemStack deniedStack=new ItemStack(b,2);
            var denied=((BlockItem)b.asItem()).place(new BlockPlaceContext(player,InteractionHand.MAIN_HAND,deniedStack,new BlockHitResult(Vec3.atBottomCenterOf(origin),Direction.UP,origin,false)));
            check(player,!denied.consumesAction() && deniedStack.getCount()==2 && level.getBlockState(origin).isAir(),"jade desk occupied second cell rejects without consumption "+facing);
            level.setBlock(far,Blocks.AIR.defaultBlockState(),3);
            level.setBlock(far.below(),Blocks.AIR.defaultBlockState(),3);
            denied=((BlockItem)b.asItem()).place(new BlockPlaceContext(player,InteractionHand.MAIN_HAND,deniedStack,new BlockHitResult(Vec3.atBottomCenterOf(origin),Direction.UP,origin,false)));
            check(player,!denied.consumesAction() && level.getBlockState(origin).isAir(),"jade desk missing second support rejects "+facing);
            level.setBlock(far.below(),Blocks.STONE.defaultBlockState(),3);
            for(int removed=0;removed<2;removed++) {
                for(int p=0;p<2;p++)level.setBlock(table.sectionPosition(origin,facing,p),Blocks.WATER.defaultBlockState(),3);
                var result=((BlockItem)b.asItem()).place(new BlockPlaceContext(player,InteractionHand.MAIN_HAND,new ItemStack(b),new BlockHitResult(Vec3.atBottomCenterOf(origin),Direction.UP,origin,false)));
                check(player,result.consumesAction(),"jade desk actual two-cell placement "+facing);
                for(int p=0;p<2;p++) {
                    BlockPos pos=table.sectionPosition(origin,facing,p);BlockState state=level.getBlockState(pos);
                    check(player,state.is(b) && state.getValue(table.partProperty())==p && state.getValue(BlockStateProperties.HORIZONTAL_FACING)==facing,"jade desk correct section "+facing+" "+p);
                    check(player,level.getFluidState(pos).is(FluidTags.WATER) && level.getBlockEntity(pos)==null,"jade desk waterlogged and static "+p);
                    check(player,countDrops(player,state,pos,b)==(p==0?1:0),"jade desk anchor-only loot "+p);
                    VoxelShape shape=state.getCollisionShape(level,pos);
                    check(player,shape.min(Direction.Axis.Y)==0 && shape.max(Direction.Axis.Y)==1,"jade desk full height collision "+p);
                    check(player,!shape.bounds().equals(new AABB(0,0,0,1,1,1)),"jade desk shaped outline "+p);
                }
                AABB area=new AABB(origin.offset(-3,-1,-3),origin.offset(4,3,4));
                int before=level.getEntitiesOfClass(ItemEntity.class,area).stream().filter(e->e.getItem().is(b.asItem())).mapToInt(e->e.getItem().getCount()).sum();
                level.destroyBlock(table.sectionPosition(origin,facing,removed),true);
                int after=level.getEntitiesOfClass(ItemEntity.class,area).stream().filter(e->e.getItem().is(b.asItem())).mapToInt(e->e.getItem().getCount()).sum();
                check(player,after-before==1 && !level.getBlockState(origin).is(b) && !level.getBlockState(far).is(b),"jade desk removing either half cleans whole and drops one "+removed);
                for(int p=0;p<2;p++)level.setBlock(table.sectionPosition(origin,facing,p),Blocks.AIR.defaultBlockState(),3);
            }
        }
    }
    public static int jadeTableGallery(ServerPlayer player) {
        if(!player.getServer().getWorldPath(LevelResource.ROOT).normalize().getFileName().toString().startsWith("Xiantu-QA"))return 0;
        var level=player.serverLevel();
        for(int attempt=0;attempt<8;attempt++) {
            int x=4352+attempt*96,z=720;boolean clear=true;
            for(BlockPos p:BlockPos.betweenClosed(x-22,-60,z-22,x+34,-56,z+24))
                if(!level.getBlockState(p).isAir()){clear=false;break;}
            if(!clear)continue;
            Block b=block("cloud_jade_table");PlaqueBlock table=(PlaqueBlock)b;
            for(int i=0;i<4;i++) {
                Direction facing=new Direction[]{Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST}[i];BlockPos pos=new BlockPos(x+i*4,-60,z);
                for(int part=0;part<2;part++)level.setBlock(table.sectionPosition(pos,facing,part),b.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,facing).setValue(table.partProperty(),part),3);
            }
            player.teleportTo(level,x-0.5,-60,z-4,0,10);
            player.sendSystemMessage(Component.literal("[Jade desk gallery] Four two-block tables at x"+x+" z"+z));return 1;
        }
        player.sendSystemMessage(Component.literal("Jade desk gallery occupied; no blocks changed"));return 0;
    }
    private BuildingIntegration() {}
}
