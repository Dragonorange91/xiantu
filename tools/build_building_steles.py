"""Author decorative stone tablets through the user's desktop Blockbench MCP."""
import json
from PIL import Image
from blockbench_bridge import Bridge
from build_building_openings_blockbench import MODELS, texture, cuboid, runtime_export, save_export
from generate_building_material_resources import ASSETS, DATA, TEXTURES, MASTER, FAMILIES, write_json, block_loot, merge_tag

HEIGHT = 48
BOXES = [([0,0,4],[32,4,12]), ([2,4,5],[30,36,11]),
         ([3,36,5],[29,40,11]), ([5,40,5],[27,43,11]),
         ([8,43,5],[24,46,11]), ([12,46,5],[20,48,11])]


def section_boxes(part):
    x,y=(1-part%2)*16,part//2*16
    result=[]
    for i,(a,b) in enumerate(BOXES):
        lo=[max(a[0],x)-x,max(a[1],y)-y,a[2]]
        hi=[min(b[0],x+16)-x,min(b[1],y+16)-y,b[2]]
        if all(l<h for l,h in zip(lo,hi)): result.append((lo,hi,i))
    return result


def bind_front(bridge, edits):
    # Legacy MCP lacks a per-cube face material tool. Use a scoped native undo edit.
    textures=bridge.value(bridge.call('list_textures'))
    uuid=next(t['uuid'] for t in textures if t['name']=='inscription')
    commands=[]
    for name,uv in edits:
        commands.append({'name':name,'uv':uv})
    bridge.call('risky_eval',code='(()=>{const edits='+json.dumps(commands)+';const cubes=edits.map(e=>Cube.all.find(c=>c.name===e.name));if(cubes.some(c=>!c))throw new Error("Missing stele cube");Undo.initEdit({elements:cubes});edits.forEach((e,i)=>{cubes[i].faces.north.texture='+json.dumps(uuid)+';cubes[i].faces.north.uv=e.uv;});Undo.finishEdit("Bind stele inscription faces");Canvas.updateAll();return cubes.length;})()')


def prepare_textures(family):
    master=MASTER/f'{family}_stele_inscription_1024.png'
    with Image.open(master) as image:
        front=image.convert('RGBA').resize((128,HEIGHT*4),Image.Resampling.LANCZOS)
        image.convert('RGBA').resize((64,64),Image.Resampling.LANCZOS).save(TEXTURES/f'{family}_stele_inscription.png')
    for part in range(6):
        # The north face is viewed from +X to -X: part0 is the visual left half.
        x=(part%2)*64;y=HEIGHT*4-(part//2+1)*64
        tile=Image.new('RGBA',(64,64),front.getpixel((0,0)))
        cropped=front.crop((x,max(y,0),x+64,y+64))
        tile.paste(cropped,(0,max(-y,0)))
        tile.save(TEXTURES/f'{family}_stele_front_{part}.png')
    return front


def main():
    bridge = Bridge()
    installed = {t['name'] for t in bridge.request('tools/list', {})['tools']}
    if 'get_capabilities' in installed:
        bridge.call('get_capabilities', include_tools=True)
    else:
        bridge.call('get_project_info')
    assert any(c['id'] == 'java_block' and c['has_compile'] for c in
               bridge.value(bridge.call('list_export_formats'))['codecs'])
    labels = {loc: json.loads((ASSETS / f'lang/{loc}.json').read_text(encoding='utf-8'))
              for loc in ('zh_cn', 'en_us')}
    plans = []
    for family, info in FAMILIES.items():
        name = family + '_stele'
        prepare_textures(family)
        materials=[f'xiantu:block/{family}_smooth',f'xiantu:block/{family}_carved']
        variants={}
        for part in range(6):
            part_name=f'{name}_part_{part}'
            bridge.call('create_project',name=part_name,format='java_block')
            texture(bridge,'smooth',TEXTURES/f'{family}_smooth.png')
            texture(bridge,'carved',TEXTURES/f'{family}_carved.png')
            texture(bridge,'inscription',TEXTURES/f'{family}_stele_front_{part}.png')
            edits=[]
            for i,(lo,hi,source) in enumerate(section_boxes(part)):
                cuboid(bridge,f'tablet_{i}',lo,hi,'carved' if source==0 else 'smooth')
                if source!=0: edits.append((f'tablet_{i}',[16-hi[0],16-hi[1],16-lo[0],16-lo[1]]))
            bind_front(bridge,edits)
            save_export(bridge,'project',MODELS/f'{part_name}.bbmodel')
            model=runtime_export(bridge,MODELS/f'{part_name}.json',materials+[f'xiantu:block/{family}_stele_front_{part}'])
            model['parent']='minecraft:block/block'
            write_json(MODELS/f'{part_name}.json',model)
            write_json(ASSETS/f'models/block/{part_name}.json',model)
            for facing,angle in [('north',0),('east',90),('south',180),('west',270)]:
                variants[f'facing={facing},part={part}']={'model':'xiantu:block/'+part_name,'y':angle}
        # Fit the taller complete silhouette in the inventory's 16-unit box.
        bridge.call('create_project',name=name,format='java_block')
        texture(bridge,'smooth',TEXTURES/f'{family}_smooth.png')
        texture(bridge,'carved',TEXTURES/f'{family}_carved.png')
        texture(bridge,'inscription',TEXTURES/f'{family}_stele_inscription.png')
        edits=[]
        for i,(a,b) in enumerate(BOXES):
            scale=16/HEIGHT
            lo=[8+(a[0]-16)*scale,a[1]*scale,8+(a[2]-8)*scale]
            hi=[8+(b[0]-16)*scale,b[1]*scale,8+(b[2]-8)*scale]
            cuboid(bridge,f'item_tablet_{i}',lo,hi,'carved' if i==0 else 'smooth')
            if i!=0: edits.append((f'item_tablet_{i}',[(32-b[0])/2,(HEIGHT-b[1])*scale,(32-a[0])/2,(HEIGHT-a[1])*scale]))
        bind_front(bridge,edits)
        save_export(bridge,'project',MODELS/f'{name}.bbmodel')
        model=runtime_export(bridge,MODELS/f'{name}.json',materials+[f'xiantu:block/{family}_stele_inscription'])
        model['parent']='minecraft:block/block'
        write_json(ASSETS/f'models/block/{name}.json',model)
        write_json(ASSETS/f'models/item/{name}.json',{'parent':'xiantu:block/'+name})
        write_json(ASSETS/f'blockstates/{name}.json',{'variants':variants})
        loot=block_loot(name)
        loot['pools'][0]['conditions'].append({'condition':'minecraft:block_state_property','block':'xiantu:'+name,'properties':{'part':'0'}})
        write_json(DATA/f'loot_tables/blocks/{name}.json',loot)
        write_json(DATA / f'recipes/{name}.json', {
            'type': 'minecraft:crafting_shaped', 'pattern': ['C', 'C'],
            'key': {'C': {'item': 'xiantu:' + family + '_carved'}},
            'result': {'item': 'xiantu:' + name, 'count': 2}})
        for loc, label in [('zh_cn', info['names'][0] + '石碑'),
                           ('en_us', info['english'][0] + ' Stele')]:
            for prefix in ('block', 'item'):
                labels[loc][f'{prefix}.xiantu.{name}'] = label
        plans.append({'id': name, 'target': 'Forge 1.20.1 Java static decorative block',
                      'geometry': BOXES, 'size_blocks': [2,3,0.5], 'occupied_cells': 6, 'cubes_per_part': [len(section_boxes(p)) for p in range(6)],
                      'textures': [f'{family}_smooth', f'{family}_carved', f'{family}_stele_inscription'] + [f'{family}_stele_front_{p}' for p in range(6)],
                      'texture_size': [64, 64], 'texels_per_model_unit': 4,
                      'new_images': 1, 'animation': 'none', 'block_entity': False,
                      'silhouette': 'stepped round arch on a low pedestal; no separate cap', 'function': 'weathered seal-script inscription decoration; no teleport or encounter trigger'})
        print('EXPORTED', name, flush=True)
    for loc, values in labels.items():
        write_json(ASSETS / f'lang/{loc}.json', values)
    merge_tag('mineable/pickaxe', ['xiantu:' + f + '_stele' for f in FAMILIES])
    write_json(MODELS / 'steles-production.json', plans)


if __name__ == '__main__':
    main()
