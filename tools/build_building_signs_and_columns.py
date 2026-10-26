"""Extend approved wooden plaques and stone columns using desktop Blockbench MCP."""
import json
from PIL import Image
from blockbench_bridge import Bridge
from build_building_openings_blockbench import MODELS, texture, cuboid, runtime_export, save_export
from generate_building_material_resources import ASSETS, DATA, MASTER, TEXTURES, FAMILIES, write_json, block_loot, merge_tag

PLAQUES = {
    'wooden_plaque_medium': (6, 2, '中型描金木牌匾', 'Medium Gilded Wooden Plaque', ['BBB', 'BBB']),
    'wooden_plaque_large': (8, 3, '大型描金木牌匾', 'Large Gilded Wooden Plaque', ['BBB', 'BBB', 'BBB']),
    'wooden_plaque_vertical': (2, 4, '竖式描金木牌匾', 'Vertical Gilded Wooden Plaque', ['BSB', 'BSB']),
}
TRIMS = {
    'pillar_base': [([2,0,2],[14,4,14]), ([3,4,3],[13,7,13]), ([4,7,4],[12,16,12])],
    'pillar_capital': [([4,0,4],[12,9,12]), ([3,9,3],[13,12,13]), ([2,12,2],[14,16,14])],
}


def package_model(bridge, name, materials):
    save_export(bridge, 'project', MODELS / f'{name}.bbmodel')
    model = runtime_export(bridge, MODELS / f'{name}.json', materials)
    model['parent'] = 'minecraft:block/block'
    write_json(MODELS / f'{name}.json', model)
    write_json(ASSETS / f'models/block/{name}.json', model)
    return model


def main():
    bridge = Bridge()
    installed = {tool['name'] for tool in bridge.request('tools/list', {})['tools']}
    if 'get_capabilities' in installed:
        bridge.call('get_capabilities', include_tools=True)
    else:
        bridge.call('get_project_info')
    assert any(c['id']=='java_block' and c['has_compile'] for c in bridge.value(bridge.call('list_export_formats'))['codecs'])
    labels = {locale: json.loads((ASSETS / f'lang/{locale}.json').read_text(encoding='utf-8')) for locale in ('zh_cn','en_us')}
    plans = []
    for name, (width, height, zh, en, recipe) in PLAQUES.items():
        with Image.open(MASTER / 'wooden_plaque_1024.png') as image:
            source = image.convert('RGBA')
        if height > width:
            source = source.transpose(Image.Transpose.ROTATE_90)
        source.resize((64,64), Image.Resampling.LANCZOS).save(TEXTURES / f'{name}.png')
        bridge.call('create_project', name=name, format='java_block')
        texture(bridge, 'front', TEXTURES / f'{name}.png')
        if width > height:
            item_from, item_to = [0,8-8*height/width,13], [16,8+8*height/width,16]
        else:
            item_from, item_to = [8-8*width/height,0,13], [8+8*width/height,16,16]
        cuboid(bridge, 'wooden_board', item_from, item_to, 'front', {
            'north':[0,0,16,16], 'south':[0,0,16,16],
            'up':[1,0,15,0.5], 'down':[1,15.5,15,16],
            'west':[0,1,0.5,15], 'east':[15.5,1,16,15]})
        model = package_model(bridge,name,['xiantu:block/'+name])
        model['display'] = {'gui':{'rotation':[0,180,0],'translation':[0,0,0],'scale':[0.9,0.9,0.9]}}
        write_json(ASSETS / f'models/block/{name}.json',model)
        write_json(MODELS / f'{name}.json',model)
        write_json(ASSETS / f'models/item/{name}.json', {'parent':'xiantu:block/'+name})
        variants = {}
        for part in range(width*height):
            col, row = part % width, part // width
            part_name = f'{name}_part_{part}'
            tile = source.crop(((width-1-col)*source.width/width,(height-1-row)*source.height/height,
                                (width-col)*source.width/width,(height-row)*source.height/height))
            tile.resize((64,64),Image.Resampling.LANCZOS).save(TEXTURES / f'{part_name}.png')
            bridge.call('create_project', name=part_name, format='java_block')
            texture(bridge,'front',TEXTURES / f'{part_name}.png')
            cuboid(bridge,'wooden_board_section',[0,0,13],[16,16,16],'front', {
                'north':[16,0,0,16], 'south':[16,0,0,16],
                'up':[1,0,15,0.5], 'down':[1,15.5,15,16],
                'west':[0,1,0.5,15], 'east':[15.5,1,16,15]})
            package_model(bridge,part_name,['xiantu:block/'+part_name])
            for facing,angle in [('north',0),('east',90),('south',180),('west',270)]:
                variants[f'facing={facing},part={part}'] = {'model':'xiantu:block/'+part_name,'y':angle,'uvlock':False}
        write_json(ASSETS / f'blockstates/{name}.json', {'variants':variants})
        loot = block_loot(name)
        loot['pools'][0]['conditions'].append({'condition':'minecraft:block_state_property','block':'xiantu:'+name,'properties':{'part':'0'}})
        write_json(DATA / f'loot_tables/blocks/{name}.json', loot)
        write_json(DATA / f'recipes/{name}.json', plaque_recipe(name))
        for locale,label in [('zh_cn',zh),('en_us',en)]:
            for prefix in ('block','item'): labels[locale][f'{prefix}.xiantu.{name}'] = label
        plans.append({'id':name,'size':[width,height],'parts':width*height,'cubes_per_part':1,
                      'faces_per_part':6,'triangles_per_part':12,'texture_size':[64,64],
                      'source':'reuse approved 1024 wooden plaque master; rotate for vertical',
                      'target':'Java Forge 1.20.1 static decorative blocks','shading':'vanilla opaque diffuse',
                      'animation':'none','preference':'near vanilla, texture detail, no block entity'})
        print('EXPORTED',name,flush=True)
    for family,info in FAMILIES.items():
        for suffix,boxes in TRIMS.items():
            name = family+'_'+suffix
            bridge.call('create_project',name=name,format='java_block')
            texture(bridge,'smooth',TEXTURES / f'{family}_smooth.png')
            texture(bridge,'carved',TEXTURES / f'{family}_carved.png')
            for index,(start,end) in enumerate(boxes):
                cuboid(bridge,f'collar_{index}',start,end,'smooth' if index==1 else 'carved')
            package_model(bridge,name,['xiantu:block/'+family+'_smooth','xiantu:block/'+family+'_carved'])
            write_json(ASSETS / f'models/item/{name}.json', {'parent':'xiantu:block/'+name})
            write_json(ASSETS / f'blockstates/{name}.json', {'variants':{
                f'facing={face}':{'model':'xiantu:block/'+name,'y':angle}
                for face,angle in [('north',0),('east',90),('south',180),('west',270)]}})
            write_json(DATA / f'loot_tables/blocks/{name}.json',block_loot(name))
            ingredient = 'smooth' if suffix=='pillar_base' else 'carved'
            write_json(DATA / f'recipes/{name}.json',{'type':'minecraft:crafting_shapeless',
                'ingredients':[{'item':'xiantu:'+family+'_pillar'},{'item':'xiantu:'+family+'_'+ingredient}],
                'result':{'item':'xiantu:'+name,'count':2}})
            for locale,label in [('zh_cn',info['names'][0]+('柱础' if suffix=='pillar_base' else '柱头')),
                                 ('en_us',info['english'][0]+(' Column Base' if suffix=='pillar_base' else ' Column Capital'))]:
                for prefix in ('block','item'): labels[locale][f'{prefix}.xiantu.{name}'] = label
            plans.append({'id':name,'cubes':3,'faces':18,'triangles':36,'geometry':boxes,
                          'texture_size':[64,64],'texels_per_unit':4,'source':'reuse smooth and carved stone',
                          'target':'Java Forge 1.20.1 static block','shading':'vanilla opaque diffuse','animation':'none'})
            print('EXPORTED',name,flush=True)
    merge_tag('mineable/axe',['xiantu:'+name for name in PLAQUES])
    merge_tag('mineable/pickaxe',['xiantu:'+family+'_'+suffix for family in FAMILIES for suffix in TRIMS])
    for locale,values in labels.items(): write_json(ASSETS / f'lang/{locale}.json',values)
    write_json(MODELS / 'signs-and-columns-production.json',plans)


def plaque_recipe(name):
    key = {'B':{'item':'minecraft:stripped_dark_oak_log'}}
    if name == 'wooden_plaque_vertical': key['S'] = {'item':'minecraft:stick'}
    return {'type':'minecraft:crafting_shaped','pattern':PLAQUES[name][4],
            'key':key,'result':{'item':'xiantu:'+name,'count':1}}


if __name__=='__main__': main()
