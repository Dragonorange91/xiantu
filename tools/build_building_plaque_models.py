"""Author one wooden wall plaque via the desktop Blockbench MCP."""
import json
from PIL import Image
from blockbench_bridge import Bridge
from build_building_openings_blockbench import MODELS, texture, cuboid, runtime_export, save_export
from generate_building_material_resources import TEXTURES


def main():
    bridge = Bridge()
    codecs = bridge.value(bridge.call('list_export_formats'))['codecs']
    assert any(c['id'] == 'java_block' and c['has_compile'] for c in codecs)
    MODELS.mkdir(parents=True, exist_ok=True)
    name = 'wooden_plaque'
    bridge.call('create_project', name=name, format='java_block')
    texture(bridge, 'front', TEXTURES / 'wooden_plaque.png')
    # The front tile is intentionally prepacked for a 2:1 sign face.
    # Edge faces sample the dark outer trim, not the ornamental border.
    cuboid(bridge, 'smooth_wooden_board', [0,4,13], [16,12,16], 'front', {
        'north': [0,0,16,16], 'south': [0,0,16,16],
        'up': [1,0,15,0.5], 'down': [1,15.5,15,16],
        'west': [0,1,0.5,15], 'east': [15.5,1,16,15],
    })
    save_export(bridge, 'project', MODELS / (name + '.bbmodel'))
    model = runtime_export(bridge, MODELS / (name + '.json'), ['xiantu:block/wooden_plaque'])
    model['parent'] = 'minecraft:block/block'
    model['display'] = {
        'gui': {'rotation': [0,180,0], 'translation': [0,0,0], 'scale': [0.9,0.9,0.9]},
        'fixed': {'rotation': [0,180,0], 'translation': [0,0,-6], 'scale': [1,1,1]},
    }
    (MODELS / (name + '.json')).write_text(json.dumps(model,indent=2)+'\n',encoding='utf-8')
    # Eight local models cover one continuous four-by-two face.
    for part in range(8):
        col, row = part % 4, part // 4
        part_name = f'{name}_part_{part}'
        with Image.open(MODELS.parent / 'master/wooden_plaque_1024.png') as source:
            width, height = source.size
            tile = source.convert('RGBA').crop(((3-col)*width/4,(1-row)*height/2,
                                                (4-col)*width/4,(2-row)*height/2))
            tile.resize((64,64),Image.Resampling.LANCZOS).save(TEXTURES / f'{part_name}.png')
        bridge.call('create_project', name=part_name, format='java_block')
        texture(bridge, 'front', TEXTURES / f'{part_name}.png')
        front = [16,0,0,16]
        cuboid(bridge, 'wooden_board_section', [0,0,13], [16,16,16], 'front', {
            'north': front, 'south': front,
            'up': [1,0,15,0.5], 'down': [1,15.5,15,16],
            'west': [0,1,0.5,15], 'east': [15.5,1,16,15],
        })
        save_export(bridge, 'project', MODELS / (part_name + '.bbmodel'))
        section = runtime_export(bridge, MODELS / (part_name + '.json'), ['xiantu:block/'+part_name])
        section['parent'] = 'minecraft:block/block'
        (MODELS / (part_name + '.json')).write_text(json.dumps(section,indent=2)+'\n',encoding='utf-8')
    plan = {'target':'Minecraft Java 1.20.1 Forge repeated decorative block',
            'preference':'near-vanilla geometry, smooth dark wood and gold border in albedo',
            'budget':'4x2 blocks, one cube per section; no animation/ticking/block entity',
            'uv':'eight 64px sections sliced from the 1024 master for one continuous 2:1 front',
            'model':'wooden_plaque', 'source':'user reference 2026-10-10'}
    (MODELS/'plaque-production.json').write_text(json.dumps(plan,indent=2)+'\n',encoding='utf-8')
    print('EXPORTED wooden_plaque')


if __name__ == '__main__':
    main()
