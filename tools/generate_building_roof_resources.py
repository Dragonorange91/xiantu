"""Native slab/stair roof resources plus the Blockbench-authored ridge."""
import json
from generate_building_material_resources import (ASSETS, DATA, FAMILIES, write_json, recipe, block_loot,
    variant_blockstate, slab_blockstate, textured_model, merge_tag, MASTER)

NAMES = {'azure_brick':'青云灵瓦', 'black_rock':'玄岩阵瓦', 'cloud_jade':'云纹玉瓦',
         'spring_stone':'庭水莲瓦','cloud_floating_stone':'云霞浮瓦'}


def main():
    zh=json.loads((ASSETS / 'lang/zh_cn.json').read_text(encoding='utf-8'))
    en=json.loads((ASSETS / 'lang/en_us.json').read_text(encoding='utf-8'))
    for family,info in FAMILIES.items():
        tiles,stairs,ridge = (family+'_'+part for part in ('roof_tiles','roof_stairs','roof_ridge'))
        surfaces={'bottom':family+'_smooth','top':family+'_roof','side':family+'_roof'}
        write_json(ASSETS / f'blockstates/{tiles}.json',slab_blockstate(tiles))
        for suffix,parent in [('', 'slab'), ('_top','slab_top'), ('_double','cube_bottom_top')]:
            write_json(ASSETS / f'models/block/{tiles}{suffix}.json',textured_model('minecraft:block/'+parent,**surfaces))
        write_json(ASSETS / f'blockstates/{stairs}.json',variant_blockstate(stairs))
        for suffix,parent in [('', 'stairs'), ('_inner','inner_stairs'), ('_outer','outer_stairs')]:
            write_json(ASSETS / f'models/block/{stairs}{suffix}.json',textured_model('minecraft:block/'+parent,**surfaces))
        write_json(ASSETS / f'blockstates/{ridge}.json',{'variants':{
            'facing='+direction:{'model':f'xiantu:block/{ridge}','y':angle,'uvlock':True}
            for direction,angle in [('north',0),('east',90),('south',180),('west',270)]}})
        write_json(ASSETS / f'models/block/{ridge}.json',json.loads((MASTER.parent / f'models/{ridge}.json').read_text()))
        for name,label,suffix in [(tiles,NAMES[family],' Roof Tiles'),(stairs,NAMES[family]+'斜瓦',' Roof Stairs'),
                                  (ridge,info['names'][0]+'屋脊',' Roof Ridge')]:
            write_json(ASSETS / f'models/item/{name}.json',{'parent':f'xiantu:block/{name}'})
            write_json(DATA / f'loot_tables/blocks/{name}.json',block_loot(name,slab=name==tiles))
            for prefix in ('block','item'):
                zh[f'{prefix}.xiantu.{name}']=label
                en[f'{prefix}.xiantu.{name}']=info['english'][0]+suffix
        write_json(DATA / f'recipes/{tiles}.json',recipe(tiles,['B','B','B'],{'B':{'item':f'xiantu:{family}'}},6))
        write_json(DATA / f'recipes/{stairs}.json',recipe(stairs,['T  ','TT ','TTT'],{'T':{'item':f'xiantu:{tiles}'}},4))
        write_json(DATA / f'recipes/{ridge}.json',recipe(ridge,['B  ',' B ','  B'],{'B':{'item':f'xiantu:{family}'}},6))
    merge_tag('slabs',[f'xiantu:{f}_roof_tiles' for f in FAMILIES])
    merge_tag('stairs',[f'xiantu:{f}_roof_stairs' for f in FAMILIES])
    merge_tag('mineable/pickaxe',[f'xiantu:{f}_{p}' for f in FAMILIES for p in ('roof_tiles','roof_stairs','roof_ridge')])
    for name,values in [('zh_cn',zh),('en_us',en)]:
        write_json(ASSETS / f'lang/{name}.json',values)
    print('Generated 15 roof block resource sets; existing tags retained.')


if __name__ == '__main__':
    main()
