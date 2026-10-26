"""Resource integration only, after image review and Blockbench export."""
import json
from generate_building_material_resources import ASSETS, DATA, FAMILIES, MASTER, write_json, recipe, block_loot, merge_tag


def main():
    zh=json.loads((ASSETS/'lang/zh_cn.json').read_text(encoding='utf-8'))
    en=json.loads((ASSETS/'lang/en_us.json').read_text(encoding='utf-8'))
    names=[]
    for family,info in FAMILIES.items():
        stand=family+'_lamp_stand'
        lamp=family+'_stone_lamp'
        for name,label,english in [(stand,info['names'][0]+'灯座',info['english'][0]+' Lamp Stand'),
                                   (lamp,info['names'][0]+'石灯',info['english'][0]+' Stone Lantern')]:
            model=json.loads((MASTER.parent/f'models/{name}.json').read_text(encoding='utf-8'))
            write_json(ASSETS/f'models/block/{name}.json',model)
            write_json(ASSETS/f'blockstates/{name}.json',{'variants':{'':{'model':'xiantu:block/'+name}}})
            write_json(ASSETS/f'models/item/{name}.json',{'parent':'xiantu:block/'+name})
            write_json(DATA/f'loot_tables/blocks/{name}.json',block_loot(name))
            for prefix in ('block','item'):
                zh[f'{prefix}.xiantu.{name}']=label
                en[f'{prefix}.xiantu.{name}']=english
            names.append('xiantu:'+name)
        write_json(DATA/f'recipes/{stand}.json',recipe(stand,['BB'],{'B':{'item':'xiantu:'+family}},2))
        write_json(DATA/f'recipes/{lamp}.json',{'type':'minecraft:crafting_shapeless',
                   'ingredients':[{'item':'xiantu:'+stand},{'item':'minecraft:torch'}],
                   'result':{'item':'xiantu:'+lamp,'count':1}})
    merge_tag('mineable/pickaxe',names)
    for locale,values in [('zh_cn',zh),('en_us',en)]:
        write_json(ASSETS/f'lang/{locale}.json',values)
    print('Generated 10 lamp/stand resource sets; existing tags retained.')


if __name__=='__main__':
    main()
