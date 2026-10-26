package com.longju.xiantu.instance;
import com.longju.xiantu.registry.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.item.*;
import net.minecraftforge.registries.RegistryObject;
/** Native, MCP-authored stonework shared by realm altars and exploration ruins. */
public final class InstanceDecor {
    public static final RegistryObject<Block> LANTERN=register("realm_spirit_lantern",true);
    public static final RegistryObject<Block> RELIQUARY=register("realm_incense_reliquary",false);
    public static final RegistryObject<Block> PILLAR=register("realm_carved_pillar",false);
    private static RegistryObject<Block> register(String id,boolean lit){
        var block=ModBlocks.BLOCKS.register(id,()->new Block(BlockBehaviour.Properties.copy(Blocks.CHISELED_STONE_BRICKS).noOcclusion().lightLevel(s->lit?12:0)));
        ModItems.ITEMS.register(id,()->new BlockItem(block.get(),new Item.Properties()));return block;
    }
    public static void bootstrap(){}
    private InstanceDecor(){}
}
