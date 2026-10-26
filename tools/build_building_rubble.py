"""Author low ruin rubble through Blockbench, reusing the approved smooth masonry textures."""
import json
from blockbench_bridge import Bridge
from build_building_openings_blockbench import MODELS, texture, cuboid, runtime_export, save_export
from generate_building_material_resources import FAMILIES, ASSETS, DATA, TEXTURES, write_json, block_loot, merge_tag

BOXES = [([1,0,2],[8,3,9]), ([9,0,1],[14,2,6]), ([7,0,9],[15,4,14]),
         ([3,3,4],[7,6,8]), ([8,4,10],[12,7,13])]


def main():
    bridge = Bridge()
    installed = {tool['name'] for tool in bridge.request('tools/list', {})['tools']}
    if 'get_capabilities' in installed:
        bridge.call('get_capabilities', include_tools=True)
    else:
        bridge.call('get_project_info')
        bridge.call('list_export_formats')
    MODELS.mkdir(parents=True, exist_ok=True)
    labels = {locale: json.loads((ASSETS / f'lang/{locale}.json').read_text(encoding='utf-8'))
              for locale in ('zh_cn','en_us')}
    plans = []
    for family, info in FAMILIES.items():
        name = family + '_rubble'
        bridge.call('create_project', name=name, format='java_block')
        texture(bridge, 'stone', TEXTURES / f'{family}_smooth.png')
        for i, (start, end) in enumerate(BOXES):
            cuboid(bridge, f'broken_stone_{i}', start, end, 'stone')
        save_export(bridge, 'project', MODELS / f'{name}.bbmodel')
        model = runtime_export(bridge, MODELS / f'{name}.json', ['xiantu:block/'+family+'_smooth'])
        model['parent'] = 'minecraft:block/block'
        write_json(ASSETS / f'models/block/{name}.json', model)
        write_json(ASSETS / f'models/item/{name}.json', {'parent':'xiantu:block/'+name})
        write_json(ASSETS / f'blockstates/{name}.json', {'variants':{
            f'facing={face}': {'model':'xiantu:block/'+name,'y':angle}
            for face,angle in [('north',0),('east',90),('south',180),('west',270)]}})
        write_json(DATA / f'loot_tables/blocks/{name}.json', block_loot(name))
        write_json(DATA / f'recipes/{name}.json', {'type':'minecraft:crafting_shapeless',
            'ingredients':[{'item':'xiantu:'+family+'_broken_pillar'}],
            'result':{'item':'xiantu:'+name,'count':1}})
        labels['zh_cn']['block.xiantu.'+name] = info['names'][0]+'碎石堆'
        labels['en_us']['block.xiantu.'+name] = info['english'][0]+' Rubble'
        for locale in labels:
            labels[locale]['item.xiantu.'+name] = labels[locale]['block.xiantu.'+name]
        plans.append({'id':name,'target':'Java Forge 1.20.1 static block',
                      'shading':'vanilla diffuse','material':'opaque','cubes':5,'faces':30,'triangles':60,
                      'texture_size':[64,64],'texels_per_unit':4,'uv':'tiled proportionate existing smooth stone',
                      'animation':'none','device':'Minecraft desktop','geometry':BOXES})
        print('EXPORTED',name,flush=True)
    merge_tag('mineable/pickaxe', ['xiantu:'+family+'_rubble' for family in FAMILIES])
    for locale, values in labels.items():
        write_json(ASSETS / f'lang/{locale}.json', values)
    write_json(MODELS / 'rubble-production.json', plans)


if __name__ == '__main__':
    main()
