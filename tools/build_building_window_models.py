"""Author thin connecting window parts through the local Blockbench MCP."""
from build_paths import minecraft_reference
import json
import zipfile
from blockbench_bridge import Bridge
from build_building_openings_blockbench import MODELS, texture, cuboid, runtime_export, save_export
from generate_building_material_resources import FAMILIES, TEXTURES


def export_part(bridge, family, suffix, cubes):
    name = f'{family}_window{suffix}'
    bridge.call('create_project', name=name, format='java_block')
    texture(bridge, 'stone', TEXTURES / f'{family}_smooth.png')
    texture(bridge, 'carved', TEXTURES / f'{family}_carved.png')
    texture(bridge, 'glass', MODELS / 'vanilla-glass.png')
    for label, start, end, material in cubes:
        cuboid(bridge, label, start, end, material)
    save_export(bridge, 'project', MODELS / f'{name}.bbmodel')
    model = runtime_export(bridge, MODELS / f'{name}.json',
                           [f'xiantu:block/{family}_smooth', f'xiantu:block/{family}_carved', 'minecraft:block/glass'])
    model['render_type'] = 'minecraft:cutout'
    # Only faces on the block boundary may be culled by the adjacent pane/stone.
    for element in model['elements']:
        for face, axis, boundary in [('down',1,0), ('up',1,16), ('north',2,0), ('south',2,16), ('west',0,0), ('east',0,16)]:
            coordinate = element['from' if boundary == 0 else 'to'][axis]
            if coordinate == boundary:
                element['faces'][face]['cullface'] = face
    (MODELS / f'{name}.json').write_text(json.dumps(model, indent=2)+'\n', encoding='utf-8')
    return model


def main():
    MODELS.mkdir(parents=True, exist_ok=True)
    bridge = Bridge()
    bridge.value(bridge.call('get_project_info'))
    bridge.request('tools/list', {})
    with zipfile.ZipFile(minecraft_reference('client.jar')) as jar:
        (MODELS / 'vanilla-glass.png').write_bytes(jar.read('assets/minecraft/textures/block/glass.png'))
    post = [('center_mullion',[7,0,7],[9,16,9],'carved')]
    arm = [('bottom_rail',[7,0,0],[9,1,7],'stone'),
           ('top_rail',[7,15,0],[9,16,7],'stone'),
           ('cross_rail',[7,7.5,0],[9,8.5,7],'stone'),
           ('lattice_upright',[7,1,3],[9,15,4],'stone'),
           ('glass',[7.25,1,0],[8.75,15,7],'glass')]
    # Item: a straight, complete panel. Placed blocks select arms with vanilla connections.
    panel = post.copy()
    for side in ('left','right'):
        for label, start, end, material in arm:
            x0, x1 = (start[2],end[2]) if side == 'left' else (16-end[2],16-start[2])
            panel.append((side+'_'+label,[x0,start[1],start[0]],[x1,end[1],end[0]],material))
    metadata_path = MODELS / 'model-production.json'
    metadata = json.loads(metadata_path.read_text()) if metadata_path.exists() else []
    metadata = [entry for entry in metadata if not entry['name'].endswith('_window')]
    for family in FAMILIES:
        for suffix, cubes in [('_post',post), ('_side',arm), ('',panel)]:
            export_part(bridge,family,suffix,cubes)
        metadata.append({'name':family+'_window','parts':{'post':1,'side':5,'item':11},
                         'thickness':2,'collision':'native IronBarsBlock / glass-pane connections',
                         'render':'cutout','density':'stone 4px/unit, native glass 1px/unit',
                         'animation':'none','target':'Java 1.20.1 Forge desktop repeated building blocks'})
        print('EXPORTED thin window '+family,flush=True)
    metadata_path.write_text(json.dumps(metadata,indent=2)+'\n',encoding='utf-8')


if __name__ == '__main__':
    main()
