package com.longju.xiantu.wood;

import com.longju.xiantu.XiantuMod;
import com.longju.xiantu.building.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.item.*;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.*;
import net.minecraftforge.eventbus.api.IEventBus;
import java.util.*;
import java.util.function.Supplier;

/** Independent namespace entries; do not add spirit planks to generic vanilla recipes. */
public final class SpiritWoodRegistry {
    public static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(ForgeRegistries.BLOCKS,XiantuMod.MOD_ID);
    public static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,XiantuMod.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES,XiantuMod.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(ForgeRegistries.MENU_TYPES,XiantuMod.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPES=DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS,XiantuMod.MOD_ID);
    public static final Map<String,RegistryObject<Block>> B=new LinkedHashMap<>();
    public static final Map<String,RegistryObject<Item>> I=new LinkedHashMap<>();
    public static final String[] BUILDING={"smooth","carved","pillar","stairs","slab","wall","railing","smooth_stairs","smooth_slab","carved_stairs","carved_slab","door","trapdoor","window","beam","bracket","eaves","eaves_corner","pillar_base","pillar_capital","roof_tiles","roof_stairs","roof_ridge","frieze","plaque_board"};
    static {
        for(SpiritWoodType t:SpiritWoodType.values()){
            add(t,"log",()->new SpiritLogBlock(props(t),t,false),true);
            add(t,"stripped_log",()->new SpiritLogBlock(props(t),t,true),true);
            add(t,"planks",()->new SpiritPlanksBlock(props(t),t),true);
            add(t,"leaves",()->new WoodNativeBlocks.Leaves(BlockBehaviour.Properties.copy(Blocks.OAK_LEAVES)),false);
            add(t,"sapling",()->new SpiritSaplingBlock(BlockBehaviour.Properties.copy(Blocks.OAK_SAPLING),t),false);
            for(String name:new String[]{"stick","long_shaft","fiber"}){
                String id=t.id+"_"+name;I.put(id,ITEMS.register(id,()->new WoodMaterialItem(t)));
            }
            for(String kind:BUILDING)add(t,kind,()->building(t,kind),false);
        }
    }
    public static final RegistryObject<BlockEntityType<SpiritWoodBlockEntity>> ENTITY=ENTITIES.register("spirit_wood",()->BlockEntityType.Builder.of(SpiritWoodBlockEntity::new,
        B.entrySet().stream().filter(e->e.getKey().endsWith("_log")||e.getKey().endsWith("_sapling")).map(e->e.getValue().get()).toArray(Block[]::new)).build(null));
    public static final RegistryObject<MenuType<SaplingGrowthMenu>> MENU=MENUS.register("sapling_growth",()->IForgeMenuType.create(SaplingGrowthMenu::new));
    public static final RegistryObject<RecipeSerializer<SpiritWoodProcessingRecipe>> RECIPE=RECIPES.register("spirit_wood_processing",SpiritWoodProcessingRecipe.Serializer::new);
    private static BlockBehaviour.Properties props(SpiritWoodType t){return BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS).strength(2+t.ordinal(),3).sound(SoundType.WOOD);}
    private static void add(SpiritWoodType t,String suffix,Supplier<Block> factory,boolean material){
        String id=t.id+"_"+suffix;RegistryObject<Block> b=BLOCKS.register(id,factory);B.put(id,b);
        I.put(id,ITEMS.register(id,()->suffix.equals("door")?new DoubleHighBlockItem(b.get(),new Item.Properties()){
            @Override public int getBurnTime(ItemStack s,net.minecraft.world.item.crafting.RecipeType<?> r){return 0;}
        }:new WoodBlockItem(b.get(),t,material)));
    }
    public static Block block(SpiritWoodType t,String suffix){return B.get(t.id+"_"+suffix).get();}
    public static Item item(SpiritWoodType t,String suffix){return I.get(t.id+"_"+suffix).get();}
    public static SpiritWoodType type(Block block){for(SpiritWoodType t:SpiritWoodType.values())for(String suffix:new String[]{"log","stripped_log","sapling"})if(block(t,suffix)==block)return t;throw new IllegalArgumentException("Not spirit tree block");}
    private static Block building(SpiritWoodType t,String k){
        var p=props(t).noOcclusion();
        return switch(k){
            case "pillar"->new StonePillarBlock(p);
            case "slab","smooth_slab","carved_slab","roof_tiles"->new WoodNativeBlocks.Slab(p);
            case "stairs","smooth_stairs","carved_stairs","roof_stairs"->new WoodNativeBlocks.Stairs(()->block(t,k.startsWith("smooth")?"smooth":k.startsWith("carved")?"carved":"planks").defaultBlockState(),p);
            case "wall"->new WoodNativeBlocks.Wall(p);
            case "railing"->new WoodNativeBlocks.Fence(p);
            case "door"->new WoodNativeBlocks.Door(p);
            case "trapdoor"->new WoodNativeBlocks.Trapdoor(p);
            case "window"->new WoodNativeBlocks.Window(p);
            case "beam"->new ConnectedStoneBeamBlock(p);
            case "bracket"->new WoodenShapeBlock(p,new double[][]{{4,0,4,12,6,12},{3,6,3,13,12,13},{0,12,4,16,16,12}});
            case "eaves","eaves_corner"->new StoneEavesBlock(p,k.equals("eaves_corner"));
            case "pillar_base","pillar_capital"->new PillarTrimBlock(p,k.equals("pillar_capital"));
            case "roof_ridge"->new RoofRidgeBlock(p);
            default->new WoodenBlock(props(t));
        };
    }
    public static void register(IEventBus bus){BLOCKS.register(bus);ITEMS.register(bus);ENTITIES.register(bus);MENUS.register(bus);RECIPES.register(bus);net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(SpiritWoodRules.class);}
    private SpiritWoodRegistry(){}
}
