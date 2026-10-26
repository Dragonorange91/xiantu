"""Package the single wooden plaque; stone-family signboards are cancelled."""
import json
from generate_building_material_resources import ASSETS, DATA, MASTER, ROOT, FAMILIES, write_json, block_loot, merge_tag


def main():
    name = 'wooden_plaque'
    model = json.loads((MASTER.parent/'models'/f'{name}.json').read_text(encoding='utf-8'))
    write_json(ASSETS/f'models/block/{name}.json', model)
    write_json(ASSETS/f'models/item/{name}.json', {'parent':'xiantu:block/'+name})
    variants = {f'facing={facing},part={part}': {'model':f'xiantu:block/{name}_part_{part}','y':angle,'uvlock':False}
                for part in range(8) for facing,angle in [('north',0),('east',90),('south',180),('west',270)]}
    for part in range(8):
        section = json.loads((MASTER.parent/'models'/f'{name}_part_{part}.json').read_text(encoding='utf-8'))
        write_json(ASSETS/f'models/block/{name}_part_{part}.json', section)
    write_json(ASSETS/f'blockstates/{name}.json', {'variants':variants})
    loot = block_loot(name)
    loot['pools'][0]['conditions'].append({'condition':'minecraft:block_state_property',
        'block':'xiantu:'+name,'properties':{'part':'0'}})
    write_json(DATA/f'loot_tables/blocks/{name}.json',loot)
    write_json(DATA/f'recipes/{name}.json', {
        'type':'minecraft:crafting_shaped','pattern':['B','B'],
        'key':{'B':{'item':'minecraft:stripped_dark_oak_log'}},
        'result':{'item':'xiantu:'+name,'count':2}})
    merge_tag('mineable/axe',['xiantu:'+name])
    pickaxe_tag = ROOT/'src/main/resources/data/minecraft/tags/blocks/mineable/pickaxe.json'
    tag = json.loads(pickaxe_tag.read_text(encoding='utf-8'))
    cancelled = {'xiantu:' + family + '_plaque' for family in FAMILIES}
    tag['values'] = [value for value in tag['values'] if value not in cancelled]
    write_json(pickaxe_tag, tag)
    for locale,label in [('zh_cn','描金木牌匾底板'),('en_us','Gilded Wooden Plaque Backing')]:
        path=ASSETS/f'lang/{locale}.json'
        values=json.loads(path.read_text(encoding='utf-8'))
        for family in FAMILIES:
            for prefix in ('block', 'item'):
                values.pop(f'{prefix}.xiantu.{family}_plaque', None)
        values[f'block.xiantu.{name}']=label
        values[f'item.xiantu.{name}']=label
        write_json(path,values)
    print('Generated one wooden plaque resource set.')


if __name__ == '__main__':
    main()
