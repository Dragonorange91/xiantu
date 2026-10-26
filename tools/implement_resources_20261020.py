from pathlib import Path
import json, shutil
from revise_resources_20261020 import MINERALS, BEASTS
ROOT=Path(__file__).resolve().parents[1]
BACK=ROOT/'asset_sources/biome-herb-materials-20261020/baseline'
J='src/main/java/com/longju/xiantu/'
def save(rel,text):
 p=ROOT/rel;b=BACK/rel
 if p.exists() and not b.exists():b.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(p,b)
 p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text,encoding='utf-8')
def replace(rel,old,new):
 p=ROOT/rel;t=p.read_text(encoding='utf-8')
 if new in t:return
 if old not in t:raise ValueError(f'not found: {rel}: {old[:100]}')
 save(rel,t.replace(old,new))
def js(rel,v):save(rel,json.dumps(v,ensure_ascii=False,indent=2)+'\n')

def medicine():
 rel=J+'medicine/MedicineCropBlock.java'
 replace(rel,'    private final MedicineDefinition definition;', '''    private static final net.minecraft.world.phys.shapes.VoxelShape[] SHAPES = {
            box(5, -1, 5, 11, 3, 11), box(4, -1, 4, 12, 6, 12),
            box(2, -1, 2, 14, 10, 14), box(1, -1, 1, 15, 14, 15)};
    @Override public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state, BlockGetter level,
            BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
        return SHAPES[state.getValue(AGE)];
    }
    private final MedicineDefinition definition;''')
 rel=J+'medicine/MedicinePlantBlock.java'
 replace(rel,'    private final MedicineDefinition definition;', '''    @Override public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state, BlockGetter level,
            BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
        return box(2, 0, 2, 14, 14, 14);
    }
    private final MedicineDefinition definition;''')
 rel=J+'medicine/MedicineSeedItem.java'
 replace(rel,'if (!context.getClickedFace().getAxis().isVertical())',
         'if (context.getClickedFace() != net.minecraft.core.Direction.UP)')
 replace(rel,'        BlockState ground = level.getBlockState(plantPos.below());', '''        if (level.isOutsideBuildHeight(plantPos) || !level.getWorldBorder().isWithinBounds(plantPos)
                || !level.getBlockState(plantPos).canBeReplaced(new net.minecraft.world.item.context.BlockPlaceContext(context))
                || !level.getFluidState(plantPos).isEmpty()
                || (context.getPlayer() != null && !context.getPlayer().mayUseItemAt(plantPos,
                        context.getClickedFace(), context.getItemInHand()))) return InteractionResult.FAIL;
        BlockState ground = level.getBlockState(plantPos.below());''')
 replace(rel,'            level.setBlock(plantPos, crop.defaultBlockState(), 3);',
         '            if (!level.setBlock(plantPos, crop.defaultBlockState(), 3)) return InteractionResult.FAIL;')
 # Stage source integration is handled by the explicit reviewed revision importer below.

def minerals():
 rows=[]
 for ident,name,lo,hi,ch,mn,mx,habitat,desc in MINERALS:
  ore='lodestone_ore' if ident=='lodestone_shard' else ident+'_ore'
  rows.append(f'            new Mineral("{ident}", "{ore}", "{name}", {lo}, {hi}, {ch}, {mn}, {mx}, "{habitat}")')
 save(J+'mining/SpecialMineralRegistry.java','''package com.longju.xiantu.mining;
import java.util.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.*;

/** Plain geological ingredients: no appraisal, cultivation rank, ingot or powder forms. */
public final class SpecialMineralRegistry {
    public record Mineral(String id, String oreId, String name, int minY, int maxY, double chance,
                          int minVein, int maxVein, String habitat) {}
    public static final List<Mineral> ALL = List.of(
'''+',\n'.join(rows)+''');
    public static final Map<String,RegistryObject<Block>> ORES = new LinkedHashMap<>();
    public static final Map<String,RegistryObject<Item>> MATERIALS = new LinkedHashMap<>();
    public static final Map<String,RegistryObject<Item>> ORE_ITEMS = new LinkedHashMap<>();
    public static void registerBlocks(DeferredRegister<Block> blocks) {
        for (var m:ALL) ORES.put(m.id(),blocks.register(m.oreId(),()->new Block(
                BlockBehaviour.Properties.copy(Blocks.STONE).strength(3.0F,3.0F).requiresCorrectToolForDrops())));
    }
    public static void registerItems(DeferredRegister<Item> items) {
        for (var m:ALL) MATERIALS.put(m.id(),items.register(m.id(),()->new Item(new Item.Properties().stacksTo(64))));
    }
    public static void registerBlockItems(DeferredRegister<Item> items) {
        for (var m:ALL) ORE_ITEMS.put(m.id(),items.register(m.oreId(),()->new BlockItem(ORES.get(m.id()).get(),new Item.Properties())));
    }
    private SpecialMineralRegistry() {}
}
''')
 replace(J+'registry/ModBlocks.java','    static { com.longju.xiantu.mining.MetalMaterialRegistry.registerBlocks(BLOCKS); }',
  '    static { com.longju.xiantu.mining.MetalMaterialRegistry.registerBlocks(BLOCKS); com.longju.xiantu.mining.SpecialMineralRegistry.registerBlocks(BLOCKS); }')
 replace(J+'registry/ModItems.java','    static { com.longju.xiantu.mining.MetalMaterialRegistry.registerItems(ITEMS); }',
  '    static { com.longju.xiantu.mining.MetalMaterialRegistry.registerItems(ITEMS); com.longju.xiantu.mining.SpecialMineralRegistry.registerItems(ITEMS); }')
 replace(J+'registry/ModBlockItems.java','    public static final java.util.Map<String, RegistryObject<Item>> METAL_ORES',
  '    static { com.longju.xiantu.mining.SpecialMineralRegistry.registerBlockItems(ITEMS); }\n    public static final java.util.Map<String, RegistryObject<Item>> METAL_ORES')
 replace(J+'registry/ModCreativeTabs.java','                for (var material : com.longju.xiantu.mining.MetalMaterialRegistry.ALL) {',
 '''                com.longju.xiantu.mining.SpecialMineralRegistry.MATERIALS.values().forEach(item -> output.accept(item.get()));
                com.longju.xiantu.mining.SpecialMineralRegistry.ORE_ITEMS.values().forEach(item -> output.accept(item.get()));
                for (var material : com.longju.xiantu.mining.MetalMaterialRegistry.ALL) {''')
 replace(J+'config/XiantuConfig.java','    public static final ForgeConfigSpec SPEC;',
 '''    public static final ForgeConfigSpec SPEC;
    public static final java.util.Map<String, ForgeConfigSpec.DoubleValue> SPECIAL_ORE_CHANCES;
    public static final ForgeConfigSpec.DoubleValue BEAST_AUXILIARY_CHANCE;''')
 replace(J+'config/XiantuConfig.java','        builder.push("beasts");',
 '''        builder.push("specialOres");
        var specialOreChances = new java.util.LinkedHashMap<String,ForgeConfigSpec.DoubleValue>();
        for (var m:com.longju.xiantu.mining.SpecialMineralRegistry.ALL)
            specialOreChances.put(m.id(),builder.comment(m.name()+"每合适区块仅一次概率尝试；矿脉位置可能因母岩不符而失败")
                    .defineInRange(m.id()+"Chance",m.chance(),0D,1D));
        SPECIAL_ORE_CHANCES=java.util.Collections.unmodifiableMap(specialOreChances);
        builder.pop();
        builder.push("beasts");
        BEAST_AUXILIARY_CHANCE=builder.comment("合格自然妖兽额外辅材总概率；成功后等概率只选一种")
                .defineInRange("auxiliaryMaterialChance",0.5D,0D,1D);''')
 replace(J+'registry/ModWorldgen.java','    private ModWorldgen() {}',
 '''    static { for(var m:com.longju.xiantu.mining.SpecialMineralRegistry.ALL)
        FEATURES.register(m.oreId(),()->new com.longju.xiantu.worldgen.SpecialMineralFeature(m)); }
    private ModWorldgen() {}''')
 save(J+'worldgen/SpecialMineralFeature.java','''package com.longju.xiantu.worldgen;
import com.longju.xiantu.config.XiantuConfig;
import com.longju.xiantu.mining.SpecialMineralRegistry;
import com.longju.xiantu.terrain.TerrainBiomes;
import net.minecraft.core.*;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.*;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import java.util.*;

/** One per-chunk roll, one Y origin, bounded connected vein; never rescans existing chunks. */
public final class SpecialMineralFeature extends Feature<NoneFeatureConfiguration> {
    private final SpecialMineralRegistry.Mineral mineral;
    public SpecialMineralFeature(SpecialMineralRegistry.Mineral mineral) { super(NoneFeatureConfiguration.CODEC); this.mineral=mineral; }
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> c) {
        var level=c.level(); var origin=c.origin();
        if (!habitat(level,origin) || c.random().nextDouble()>=XiantuConfig.SPECIAL_ORE_CHANCES.get(mineral.id()).get()) return false;
        if (!host(level,origin)) return false;
        int target=mineral.minVein()+c.random().nextInt(mineral.maxVein()-mineral.minVein()+1);
        List<BlockPos> placed=new ArrayList<>();
        if (!put(level,origin)) return false;
        placed.add(origin.immutable());
        for(int tries=0;placed.size()<target && tries<target*12;tries++) {
            BlockPos pos=placed.get(c.random().nextInt(placed.size())).relative(Direction.values()[c.random().nextInt(6)]);
            if (pos.getY()<mineral.minY() || pos.getY()>mineral.maxY() || placed.contains(pos)
                    || Math.abs(pos.getX()-origin.getX())>3 || Math.abs(pos.getZ()-origin.getZ())>3) continue;
            if (put(level,pos)) placed.add(pos);
        }
        return true;
    }
    private boolean habitat(WorldGenLevel level,BlockPos pos) {
        var biome=level.getBiome(pos);
        return switch(mineral.habitat()) {
            case "mountain" -> biome.is(BiomeTags.IS_MOUNTAIN);
            case "cold" -> biome.value().getBaseTemperature()<0.2F;
            case "thunder" -> biome.is(TerrainBiomes.THUNDER_WASTES);
            case "nether" -> biome.is(Biomes.BASALT_DELTAS)||biome.is(Biomes.NETHER_WASTES);
            case "end" -> (long)pos.getX()*pos.getX()+(long)pos.getZ()*pos.getZ()>1048576L
                    && (biome.is(Biomes.END_HIGHLANDS)||biome.is(Biomes.END_MIDLANDS)||biome.is(Biomes.END_BARRENS));
            case "lava" -> nearLava(level,pos);
            default -> true;
        };
    }
    private boolean nearLava(WorldGenLevel level,BlockPos pos) {
        for(BlockPos q:BlockPos.betweenClosed(pos.offset(-6,-6,-6),pos.offset(6,6,6)))
            if(q.distSqr(pos)<=36 && level.hasChunk(q.getX()>>4,q.getZ()>>4)) {
                var s=level.getBlockState(q);
                if(s.is(Blocks.LAVA) && s.getFluidState().isSource()) return true;
            }
        return false;
    }
    private boolean host(WorldGenLevel level,BlockPos pos) {
        if(level.isOutsideBuildHeight(pos)||!level.ensureCanWrite(pos)||level.getBlockEntity(pos)!=null) return false;
        var state=level.getBlockState(pos);
        return switch(mineral.habitat()) {
            case "deep" -> state.is(Blocks.DEEPSLATE);
            case "nether" -> state.is(Blocks.NETHERRACK)||state.is(Blocks.BASALT)||state.is(Blocks.BLACKSTONE);
            case "end" -> state.is(Blocks.END_STONE);
            default -> state.is(Blocks.STONE)||state.is(Blocks.DEEPSLATE)||state.is(Blocks.GRANITE)
                    ||state.is(Blocks.DIORITE)||state.is(Blocks.ANDESITE)||state.is(Blocks.TUFF)
                    ||state.is(com.longju.xiantu.terrain.TerrainBlockRegistry.BLOCKS_BY_ID.get("thunder_rock").get())
                    ||state.is(com.longju.xiantu.terrain.TerrainBlockRegistry.BLOCKS_BY_ID.get("meteor_rock").get())
                    ||state.is(com.longju.xiantu.terrain.TerrainBlockRegistry.BLOCKS_BY_ID.get("valley_streamstone").get());
        };
    }
    private boolean put(WorldGenLevel level,BlockPos pos) {
        return host(level,pos) && level.setBlock(pos,SpecialMineralRegistry.ORES.get(mineral.id()).get().defaultBlockState(),2);
    }
}
''')
 assets='src/main/resources/assets/xiantu/';data='src/main/resources/data/'
 for ident,name,lo,hi,ch,mn,mx,habitat,desc in MINERALS:
  ore='lodestone_ore' if ident=='lodestone_shard' else ident+'_ore'
  js(assets+'models/item/'+ident+'.json',{'parent':'minecraft:item/generated','textures':{'layer0':'xiantu:item/'+ident}})
  js(assets+'models/block/'+ore+'.json',{'parent':'minecraft:block/cube_all','textures':{'all':'xiantu:block/'+ore}})
  js(assets+'models/item/'+ore+'.json',{'parent':'xiantu:block/'+ore})
  js(assets+'blockstates/'+ore+'.json',{'variants':{'':{'model':'xiantu:block/'+ore}}})
  js(data+'xiantu/loot_tables/blocks/'+ore+'.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'xiantu:'+ident}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
  js(data+'xiantu/worldgen/configured_feature/'+ore+'.json',{'type':'xiantu:'+ore,'config':{}})
  js(data+'xiantu/worldgen/placed_feature/'+ore+'.json',{'feature':'xiantu:'+ore,'placement':[{'type':'minecraft:in_square'},{'type':'minecraft:height_range','height':{'type':'minecraft:uniform','min_inclusive':{'absolute':lo},'max_inclusive':{'absolute':hi}}},{'type':'minecraft:biome'}]})
  biomes='#minecraft:is_overworld'
  if habitat=='nether':biomes=['minecraft:basalt_deltas','minecraft:nether_wastes']
  elif habitat=='end':biomes=['minecraft:end_highlands','minecraft:end_midlands','minecraft:end_barrens']
  elif habitat=='thunder':biomes='xiantu:thunder_wastes'
  elif habitat=='mountain':biomes='#minecraft:is_mountain'
  js(data+'xiantu/forge/biome_modifier/'+ore+'.json',{'type':'forge:add_features','biomes':biomes,'features':['xiantu:'+ore],'step':'underground_ores'})
 for tag in ('mineable/pickaxe','needs_iron_tool'):
  rel=data+'minecraft/tags/blocks/'+tag+'.json';p=ROOT/rel;v=json.loads(p.read_text(encoding='utf-8')) if p.exists() else {'replace':False,'values':[]}
  for m in MINERALS:
   ore='lodestone_ore' if m[0]=='lodestone_shard' else m[0]+'_ore'
   if 'xiantu:'+ore not in v['values']:v['values'].append('xiantu:'+ore)
  js(rel,v)
 lang={}
 for m in MINERALS:
  ore='lodestone_ore' if m[0]=='lodestone_shard' else m[0]+'_ore'
  lang['item.xiantu.'+m[0]]=m[1];lang['block.xiantu.'+ore]=m[1]+'矿石'
 for ident,name,_ in BEASTS:lang['item.xiantu.'+ident]=name
 for locale in ('zh_cn','en_us'):
  rel=assets+'lang/'+locale+'.json';v=json.loads((ROOT/rel).read_text(encoding='utf-8'));v.update(lang);js(rel,v)

if __name__=='__main__':medicine();minerals()
