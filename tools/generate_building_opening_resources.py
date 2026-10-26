"""Map generated art and native Java 1.20.1 door state/model templates."""
from build_paths import minecraft_reference
import json
import sys
import zipfile
from PIL import Image
from generate_building_material_resources import ROOT, ASSETS, DATA, MASTER, TEXTURES, FAMILIES, write_json, recipe, block_loot, merge_tag

VANILLA = ROOT.__class__(minecraft_reference('client.jar'))
MODELS = MASTER.parent / 'models'

def prepare_textures():
    for family in FAMILIES:
        name = f'{family}_door'
        image = Image.open(MASTER / f'{name}_1024.png').convert('RGBA')
        for half, box in [('top',(0,0,1024,512)), ('bottom',(0,512,1024,1024))]:
            image.crop(box).resize((64,64),Image.Resampling.LANCZOS).save(TEXTURES / f'{name}_{half}.png')
        name = f'{family}_trapdoor'
        Image.open(MASTER / f'{name}_1024.png').convert('RGBA').resize((64,64),Image.Resampling.LANCZOS).save(TEXTURES / f'{name}.png')
    print('Prepared all 15 opening runtime tiles from 10 API masters.')

def main():
    zh = json.loads((ASSETS / 'lang/zh_cn.json').read_text(encoding='utf-8'))
    en = json.loads((ASSETS / 'lang/en_us.json').read_text(encoding='utf-8'))
    with zipfile.ZipFile(VANILLA) as jar:
        for family, info in FAMILIES.items():
            door, trap, window = (f'{family}_{part}' for part in ('door', 'trapdoor', 'window'))
            image = Image.open(MASTER / f'{door}_1024.png').convert('RGBA')
            # Each door half is a 64-square tile, avoiding accidental flipbook strips.
            for half, box in [('top', (0, 0, 1024, 512)), ('bottom', (0, 512, 1024, 1024))]:
                image.crop(box).resize((64, 64), Image.Resampling.LANCZOS).save(TEXTURES / f'{door}_{half}.png')
            Image.open(MASTER / f'{trap}_1024.png').convert('RGBA').resize((64,64), Image.Resampling.LANCZOS).save(TEXTURES / f'{trap}.png')
            for name, vanilla in [(door, 'oak_door'), (trap, 'oak_trapdoor')]:
                state = json.loads(jar.read(f'assets/minecraft/blockstates/{vanilla}.json'))
                write_json(ASSETS / f'blockstates/{name}.json', json.loads(json.dumps(state).replace(f'minecraft:block/{vanilla}', f'xiantu:block/{name}')))
                source_models = {entry['model'] for entry in state['variants'].values()}
                for source in source_models:
                    reference = json.loads(jar.read('assets/' + source.replace(':', '/models/') + '.json'))
                    textures = {'top': f'xiantu:block/{door}_top', 'bottom': f'xiantu:block/{door}_bottom'} if name == door else {'texture': f'xiantu:block/{trap}'}
                    reference['textures'] = textures
                    reference['render_type'] = 'minecraft:cutout'
                    target = source.replace(f'minecraft:block/{vanilla}', name)
                    write_json(ASSETS / f'models/block/{target}.json', reference)
            # Door item is actual two-cube geometry, not a squashed generated sprite.
            door_item = json.loads((MODELS / f'{door}_item.json').read_text(encoding='utf-8'))
            write_json(ASSETS / f'models/item/{door}.json', door_item)
            write_json(ASSETS / f'models/item/{trap}.json', {'parent': f'xiantu:block/{trap}_bottom'})
            door_loot = block_loot(door)
            door_loot['pools'][0]['conditions'].append({'condition': 'minecraft:block_state_property', 'block': f'xiantu:{door}', 'properties': {'half': 'lower'}})
            write_json(DATA / f'loot_tables/blocks/{door}.json', door_loot)
            write_json(DATA / f'loot_tables/blocks/{trap}.json', block_loot(trap))
            for suffix in ('', '_post', '_side'):
                model = json.loads((MODELS / f'{window}{suffix}.json').read_text(encoding='utf-8'))
                model['render_type'] = 'minecraft:cutout'
                write_json(ASSETS / f'models/block/{window}{suffix}.json', model)
            multipart = [{'apply': {'model': f'xiantu:block/{window}_post'}}]
            for direction, angle in [('north',0), ('east',90), ('south',180), ('west',270)]:
                multipart.append({'when': {direction:'true'}, 'apply': {
                    'model': f'xiantu:block/{window}_side', 'y': angle, 'uvlock': True}})
            write_json(ASSETS / f'blockstates/{window}.json', {'multipart': multipart})
            write_json(ASSETS / f'models/item/{window}.json', {'parent': f'xiantu:block/{window}'})
            write_json(DATA / f'loot_tables/blocks/{window}.json', block_loot(window))
            write_json(DATA / f'recipes/{door}.json', recipe(door, ['BB','BB','BB'], {'B': {'item': f'xiantu:{family}_smooth'}}, 3))
            write_json(DATA / f'recipes/{trap}.json', recipe(trap, ['BBB','BBB'], {'B': {'item': f'xiantu:{family}_carved'}}, 2))
            write_json(DATA / f'recipes/{window}.json', recipe(window, ['BGB','BGB'], {'B': {'item': f'xiantu:{family}'}, 'G': {'item': 'minecraft:glass'}}, 4))
            for part, suffix, english in [('door','门','Door'), ('trapdoor','活板门','Trapdoor'), ('window','窗','Window')]:
                name = f'{family}_{part}'
                for prefix in ('block', 'item'):
                    zh[f'{prefix}.xiantu.{name}'] = info['names'][0] + suffix
                    en[f'{prefix}.xiantu.{name}'] = info['english'][0] + ' ' + english
    for suffix, tag in [('door','doors'), ('trapdoor','trapdoors')]:
        merge_tag(tag, [f'xiantu:{family}_{suffix}' for family in FAMILIES])
    merge_tag('mineable/pickaxe', [f'xiantu:{family}_{suffix}' for family in FAMILIES for suffix in ('door','trapdoor','window')])
    for lang, content in [('zh_cn',zh), ('en_us',en)]:
        write_json(ASSETS / f'lang/{lang}.json', content)
    print('Generated 15 stone openings, 15 runtime tiles and native door/trapdoor state resources.')

if __name__ == '__main__':
    if '--textures-only' in sys.argv:
        prepare_textures()
    else:
        main()
