"""Author editable Java models using the installed Blockbench desktop MCP."""
from build_paths import minecraft_reference
import base64
import json
import zipfile
from pathlib import Path
from PIL import Image
from blockbench_bridge import Bridge
from generate_building_material_resources import ROOT, MASTER, TEXTURES, FAMILIES

MODELS = MASTER.parent / 'models'

def save_export(bridge, codec, target):
    value = bridge.value(bridge.call('export_model', codec_id=codec, max_content_length=2000000))
    if value.get('truncated') or not value.get('content'):
        raise RuntimeError(f'Incomplete export: {target}')
    target.write_text(value['content'], encoding='utf-8')
    return json.loads(value['content'])

def texture(bridge, name, path):
    with Image.open(path) as image:
        width, height = image.size
    data = 'data:image/png;base64,' + base64.b64encode(path.read_bytes()).decode()
    bridge.call('create_texture', name=name, width=width, height=height, data=data)

def cuboid(bridge, name, start, end, tex, uv=None):
    # Proportionate UV footprint on every axis; front/back show the material's aligned region.
    x0,y0,z0 = start
    x1,y1,z1 = end
    face_uv = {
        'north': [x0, 16-y1, x1, 16-y0], 'south': [x0,16-y1,x1,16-y0],
        'west': [z0,16-y1,z1,16-y0], 'east': [z0,16-y1,z1,16-y0],
        'up': [x0,z0,x1,z1], 'down': [x0,z0,x1,z1],
    }
    if uv:
        face_uv.update(uv)
    bridge.call('place_cube', elements=[{'name':name,'from':start,'to':end}], texture=tex,
                faces=[{'face':face,'uv':rect} for face,rect in face_uv.items()])
    # This bridge's place_cube writes names where the Java codec expects texture UUIDs.
    textures = bridge.value(bridge.call('list_textures'))
    uuid = next(item['uuid'] for item in textures if item['name'] == tex)
    bridge.call('apply_texture', id=name, texture=uuid, applyTo='all')

def runtime_export(bridge, path, names):
    model = save_export(bridge, 'java_block', path)
    # Blockbench local texture references become stable runtime resource locations.
    model['textures'] = {str(i):name for i,name in enumerate(names)} | {'particle':names[0]}
    model.pop('credit', None)
    model.pop('format_version', None)
    refs = set(model['textures'])
    for element in model.get('elements', []):
        for face in element.get('faces', {}).values():
            ref = face.get('texture', '').removeprefix('#')
            assert ref in refs, (path, ref, refs)
            assert all(0 <= value <= 16 for value in face.get('uv', [])), (path, face)
    if path.name.endswith('_door_item.json'):
        assert model['elements'][0]['faces']['north']['uv'] == [0,0,16,16], ('Door UV unexpectedly rescaled', model)

    path.write_text(json.dumps(model,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    return model

def main():
    MODELS.mkdir(parents=True,exist_ok=True)
    bridge = Bridge()
    original = bridge.value(bridge.call('get_project_info'))
    (MODELS / 'original-project.json').write_text(json.dumps(original,ensure_ascii=False,indent=2),encoding='utf-8')
    # Preserve the existing paused crystal project, without editing or closing its tab.
    if not (MODELS / 'paused-project-snapshot.bbmodel').exists():
        save_export(bridge, 'project', MODELS / 'paused-project-snapshot.bbmodel')
    glass = MODELS / 'vanilla-glass.png'
    with zipfile.ZipFile(minecraft_reference('client.jar')) as jar:
        glass.write_bytes(jar.read('assets/minecraft/textures/block/glass.png'))
    metadata = []
    for family in FAMILIES:
        name = f'{family}_door'
        bridge.call('create_project',name=name,format='java_block')
        for half in ('bottom','top'):
            texture(bridge, half, TEXTURES / f'{name}_{half}.png')
        cuboid(bridge,'lower_leaf',[0,0,0],[16,16,3],'bottom')
        cuboid(bridge,'upper_leaf',[0,16,0],[16,32,3],'top',
                {face:([0,0,16,16] if face in ('north','south') else [0,0,3,16] if face in ('west','east') else [0,0,16,3])
                 for face in ('north','south','west','east','up','down')})
        save_export(bridge,'project',MODELS / f'{name}.bbmodel')
        model = runtime_export(bridge,MODELS / f'{name}_item.json',[f'xiantu:block/{name}_bottom',f'xiantu:block/{name}_top'])
        model['display'] = {
            'gui': {'rotation':[0,180,0],'translation':[0,-4,0],'scale':[0.4,0.4,0.4]},
            'ground': {'translation':[0,2,0],'scale':[0.2,0.2,0.2]},
            'fixed': {'translation':[0,-4,0],'scale':[0.4,0.4,0.4]},
            'thirdperson_righthand': {'rotation':[0,90,0],'translation':[0,2,0],'scale':[0.3,0.3,0.3]},
            'firstperson_righthand': {'rotation':[0,90,0],'translation':[0,0,0],'scale':[0.3,0.3,0.3]},
        }
        (MODELS / f'{name}_item.json').write_text(json.dumps(model,indent=2)+'\n',encoding='utf-8')
        metadata.append({'name':name,'cubes':len(model['elements']),'purpose':'item geometry + editable two-half door; placed block uses native door models'})
        name=f'{family}_trapdoor'
        bridge.call('create_project',name=name,format='java_block')
        texture(bridge,'panel',TEXTURES / f'{name}.png')
        cuboid(bridge,'closed_panel',[0,0,0],[16,3,16],'panel')
        save_export(bridge,'project',MODELS / f'{name}.bbmodel')
        runtime_export(bridge,MODELS / f'{name}.json',[f'xiantu:block/{name}'])
        print(f'EXPORTED {family} door/trapdoor',flush=True)
    (MODELS / 'model-production.json').write_text(json.dumps(metadata,indent=2),encoding='utf-8')
    from build_building_window_models import main as build_windows
    build_windows()
    print('Saved editable opening models; existing paused project retained.')

if __name__=='__main__':
    main()
