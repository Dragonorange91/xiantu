"""Integrate reviewed cracked masonry using native Java 1.20.1 templates only."""
import json
from PIL import Image
from generate_building_material_resources import (ROOT,ASSETS,DATA,MASTER,TEXTURES,FAMILIES,
    write_json,textured_model,variant_blockstate,slab_blockstate,block_loot,merge_tag,recipe)

def integrate_registry():
    registry=ROOT/'src/main/java/com/longju/xiantu/registry'
    additions=[]
    for family,info in FAMILIES.items():
        key=family.upper(); strength=str(info['strength'])+'F'
        material='Blocks.BASALT' if family=='black_rock' else 'Blocks.STONE_BRICKS'
        additions.extend([
            f'    public static final RegistryObject<Block> {key}_CRACKED = registerBuildingBlock("{family}_cracked", {material}, {strength});',
            f'    public static final RegistryObject<Block> {key}_CRACKED_STAIRS = BLOCKS.register("{family}_cracked_stairs", () -> new StairBlock(() -> {key}_CRACKED.get().defaultBlockState(), buildingProperties({material}, {strength})));',
            f'    public static final RegistryObject<Block> {key}_CRACKED_SLAB = BLOCKS.register("{family}_cracked_slab", () -> new SlabBlock(buildingProperties({material}, {strength})));'])
    path=registry/'ModBlocks.java'; value=path.read_text(encoding='utf-8')
    if 'AZURE_BRICK_CRACKED =' not in value:
        value=value.replace('    private static RegistryObject<Block> registerEaves(', '\n'.join(additions)+'\n\n    private static RegistryObject<Block> registerEaves(',1)
        path.write_text(value,encoding='utf-8')
    path=registry/'ModBlockItems.java'; value=path.read_text(encoding='utf-8')
    if 'AZURE_BRICK_CRACKED =' not in value:
        additions=[]
        for family in FAMILIES:
            for suffix in ('cracked','cracked_stairs','cracked_slab'):
                key=(family+'_'+suffix).upper(); name=family+'_'+suffix
                additions.append(f'    public static final RegistryObject<Item> {key} = building("{name}", ModBlocks.{key});')
        anchor='    private ModBlockItems()'
        assert anchor in value
        value=value.replace(anchor,'\n'.join(additions)+'\n\n'+anchor,1)
        path.write_text(value,encoding='utf-8')
    path=registry/'ModCreativeTabs.java';value=path.read_text(encoding='utf-8')
    for family in FAMILIES:
        key=family.upper(); anchor=f'                output.accept(ModBlockItems.{key}_STELE.get());'
        if f'output.accept(ModBlockItems.{key}_CRACKED.get())' not in value:
            assert anchor in value
            extra='\n'.join(f'                output.accept(ModBlockItems.{key}_{s.upper()}.get());' for s in ('cracked','cracked_stairs','cracked_slab'))
            value=value.replace(anchor,anchor+'\n'+extra,1)
    path.write_text(value,encoding='utf-8')

def main():
    # Require the complete image batch before producing any model.
    for family in FAMILIES:
        for path,size in [(MASTER/f'{family}_cracked_1024.png',(1024,1024)),(TEXTURES/f'{family}_cracked.png',(64,64))]:
            with Image.open(path) as im:
                assert im.size==size and im.mode=='RGBA' and im.getchannel('A').getextrema()==(255,255),path
    assert (ROOT/'asset_sources/building_materials/cracked-textures-review.png').exists()
    zh=json.loads((ASSETS/'lang/zh_cn.json').read_text(encoding='utf-8'))
    en=json.loads((ASSETS/'lang/en_us.json').read_text(encoding='utf-8'))
    mineable=[]; stairs_tag=[]; slabs_tag=[]
    for family,info in FAMILIES.items():
        base=family+'_cracked'; stairs=base+'_stairs'; slab=base+'_slab'
        write_json(ASSETS/f'blockstates/{base}.json',{'variants':{'':{'model':f'xiantu:block/{base}'}}})
        write_json(ASSETS/f'models/block/{base}.json',textured_model('minecraft:block/cube_all',all=base))
        write_json(ASSETS/f'blockstates/{stairs}.json',variant_blockstate(stairs))
        for suffix,parent in [('', 'stairs'),('_inner','inner_stairs'),('_outer','outer_stairs')]:
            write_json(ASSETS/f'models/block/{stairs}{suffix}.json',textured_model('minecraft:block/'+parent,bottom=base,top=base,side=base))
        write_json(ASSETS/f'blockstates/{slab}.json',slab_blockstate(slab))
        for suffix,parent in [('', 'slab'),('_top','slab_top')]:
            write_json(ASSETS/f'models/block/{slab}{suffix}.json',textured_model('minecraft:block/'+parent,bottom=base,top=base,side=base))
        write_json(ASSETS/f'models/block/{slab}_double.json',{'parent':f'xiantu:block/{base}'})
        for name,cn,english in [(base,'裂损'+info['names'][0],'Cracked '+info['english'][0]),
                (stairs,'裂损'+info['names'][2],'Cracked '+info['english'][2]),
                (slab,'裂损'+info['names'][3],'Cracked '+info['english'][3])]:
            write_json(ASSETS/f'models/item/{name}.json',{'parent':'xiantu:block/'+name})
            write_json(DATA/f'loot_tables/blocks/{name}.json',block_loot(name,slab=name==slab))
            for lang,display in [(zh,cn),(en,english)]:
                lang['block.xiantu.'+name]=display;lang['item.xiantu.'+name]=display
            mineable.append('xiantu:'+name)
        write_json(DATA/f'recipes/{base}.json',{'type':'minecraft:smelting','category':'blocks',
            'ingredient':{'item':'xiantu:'+family},'result':'xiantu:'+base,'experience':0.1,'cookingtime':200})
        write_json(DATA/f'recipes/{stairs}.json',recipe(stairs,['B  ','BB ','BBB'],{'B':{'item':'xiantu:'+base}},4))
        write_json(DATA/f'recipes/{slab}.json',recipe(slab,['BBB'],{'B':{'item':'xiantu:'+base}},6))
        stairs_tag.append('xiantu:'+stairs);slabs_tag.append('xiantu:'+slab)
    merge_tag('mineable/pickaxe',mineable);merge_tag('stairs',stairs_tag);merge_tag('slabs',slabs_tag)
    write_json(ASSETS/'lang/zh_cn.json',zh);write_json(ASSETS/'lang/en_us.json',en)
    integrate_registry()
    print('Integrated 15 cracked building blocks/items, 5 textures and native templates')
if __name__=='__main__':main()
