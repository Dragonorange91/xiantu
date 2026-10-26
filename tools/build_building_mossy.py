"""Reviewed moss textures -> native cube/stair/slab/wall resource templates."""
import json
from PIL import Image
from generate_building_material_resources import (ROOT,ASSETS,DATA,MASTER,TEXTURES,FAMILIES,
    write_json,textured_model,variant_blockstate,slab_blockstate,wall_blockstate,block_loot,merge_tag,recipe)
SUFFIXES=('mossy','mossy_stairs','mossy_slab','mossy_wall')

def registry():
    folder=ROOT/'src/main/java/com/longju/xiantu/registry'
    blocks=[];items=[]
    for f,info in FAMILIES.items():
        key=f.upper(); strength=str(info['strength'])+'F'
        mat='Blocks.BASALT' if f=='black_rock' else 'Blocks.STONE_BRICKS'
        prop=f'buildingProperties({mat}, {strength})'
        blocks += [
            f'    public static final RegistryObject<Block> {key}_MOSSY = registerBuildingBlock("{f}_mossy", {mat}, {strength});',
            f'    public static final RegistryObject<Block> {key}_MOSSY_STAIRS = BLOCKS.register("{f}_mossy_stairs", () -> new StairBlock(() -> {key}_MOSSY.get().defaultBlockState(), {prop}));',
            f'    public static final RegistryObject<Block> {key}_MOSSY_SLAB = BLOCKS.register("{f}_mossy_slab", () -> new SlabBlock({prop}));',
            f'    public static final RegistryObject<Block> {key}_MOSSY_WALL = BLOCKS.register("{f}_mossy_wall", () -> new WallBlock({prop}));']
        for s in SUFFIXES:
            k=(f+'_'+s).upper(); n=f+'_'+s
            items.append(f'    public static final RegistryObject<Item> {k} = building("{n}", ModBlocks.{k});')
    for name,lines,anchor in [('ModBlocks.java',blocks,'    private static RegistryObject<Block> registerEaves('),
                             ('ModBlockItems.java',items,'    private ModBlockItems()')]:
        path=folder/name; value=path.read_text(encoding='utf-8')
        if 'AZURE_BRICK_MOSSY =' not in value:
            assert anchor in value;value=value.replace(anchor,'\n'.join(lines)+'\n\n'+anchor,1)
            path.write_text(value,encoding='utf-8')
    path=folder/'ModCreativeTabs.java';value=path.read_text(encoding='utf-8')
    for f in FAMILIES:
        k=f.upper();anchor=f'                output.accept(ModBlockItems.{k}_CRACKED_SLAB.get());'
        if f'output.accept(ModBlockItems.{k}_MOSSY.get());' not in value:
            assert anchor in value
            value=value.replace(anchor,anchor+'\n'+'\n'.join(f'                output.accept(ModBlockItems.{k}_{s.upper()}.get());' for s in SUFFIXES),1)
    path.write_text(value,encoding='utf-8')

def main():
    for f in FAMILIES:
        for path,size in [(MASTER/f'{f}_mossy_1024.png',(1024,1024)),(TEXTURES/f'{f}_mossy.png',(64,64))]:
            with Image.open(path) as im:assert im.size==size and im.mode=='RGBA' and im.getchannel('A').getextrema()==(255,255)
    assert (ROOT/'asset_sources/building_materials/mossy-textures-review.png').exists()
    langs={lang:json.loads((ASSETS/f'lang/{lang}.json').read_text(encoding='utf-8')) for lang in ('zh_cn','en_us')}
    tags={tag:[] for tag in ('mineable/pickaxe','stairs','slabs','walls')}
    for f,info in FAMILIES.items():
        b=f+'_mossy';st=b+'_stairs';sl=b+'_slab';wa=b+'_wall'
        write_json(ASSETS/f'blockstates/{b}.json',{'variants':{'':{'model':'xiantu:block/'+b}}})
        write_json(ASSETS/f'models/block/{b}.json',textured_model('minecraft:block/cube_all',all=b))
        write_json(ASSETS/f'blockstates/{st}.json',variant_blockstate(st))
        for s,parent in [('', 'stairs'),('_inner','inner_stairs'),('_outer','outer_stairs')]:
            write_json(ASSETS/f'models/block/{st}{s}.json',textured_model('minecraft:block/'+parent,bottom=b,top=b,side=b))
        write_json(ASSETS/f'blockstates/{sl}.json',slab_blockstate(sl))
        for s,parent in [('', 'slab'),('_top','slab_top')]:
            write_json(ASSETS/f'models/block/{sl}{s}.json',textured_model('minecraft:block/'+parent,bottom=b,top=b,side=b))
        write_json(ASSETS/f'models/block/{sl}_double.json',{'parent':'xiantu:block/'+b})
        write_json(ASSETS/f'blockstates/{wa}.json',wall_blockstate(wa))
        for s,parent in [('post','template_wall_post'),('side','template_wall_side'),('side_tall','template_wall_side_tall'),('inventory','wall_inventory')]:
            write_json(ASSETS/f'models/block/{wa}_{s}.json',textured_model('minecraft:block/'+parent,wall=b))
        for n,idx in [(b,0),(st,2),(sl,3),(wa,4)]:
            write_json(ASSETS/f'models/item/{n}.json',{'parent':'xiantu:block/'+n+('_inventory' if n==wa else '')})
            write_json(DATA/f'loot_tables/blocks/{n}.json',block_loot(n,slab=n==sl))
            for lang,prefix,label in [('zh_cn','苔蚀',info['names'][idx]),('en_us','Moss-Weathered ',info['english'][idx])]:
                for kind in ('block','item'):langs[lang][kind+'.xiantu.'+n]=prefix+label
            tags['mineable/pickaxe'].append('xiantu:'+n)
        write_json(DATA/f'recipes/{b}.json',{'type':'minecraft:crafting_shapeless','ingredients':[
            {'item':'xiantu:'+f},{'item':'minecraft:vine'}],'result':{'item':'xiantu:'+b,'count':1}})
        for n,pattern,count in [(st,['B  ','BB ','BBB'],4),(sl,['BBB'],6),(wa,['BBB','BBB'],6)]:
            write_json(DATA/f'recipes/{n}.json',recipe(n,pattern,{'B':{'item':'xiantu:'+b}},count))
        for tag,name in [('stairs',st),('slabs',sl),('walls',wa)]:tags[tag].append('xiantu:'+name)
    for tag,names in tags.items():merge_tag(tag,names)
    for lang,value in langs.items():write_json(ASSETS/f'lang/{lang}.json',value)
    registry();print('Integrated20 moss-weathered building items with native Minecraft templates')
if __name__=='__main__':main()
