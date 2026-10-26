"""Editable roof blockouts and low ridge runtime geometry via Blockbench MCP."""
import json
from blockbench_bridge import Bridge
from build_building_openings_blockbench import MODELS, texture, cuboid, runtime_export, save_export
from generate_building_material_resources import FAMILIES, TEXTURES


def main():
    bridge = Bridge()
    bridge.value(bridge.call('get_project_info'))
    bridge.request('tools/list', {})
    parts = {
        'roof_tiles': [('tile_slab',[0,0,0],[16,8,16],'roof')],
        'roof_stairs': [('lower_step',[0,0,0],[16,8,16],'roof'), ('upper_step',[8,8,0],[16,16,16],'roof')],
        'roof_ridge': [('base_cap',[0,0,0],[16,4,16],'roof'),
                       ('ridge_band',[0,4,4],[16,8,12],'stone'),
                       ('carved_crest',[0,8,6],[16,10,10],'carved')],
    }
    metadata = []
    for family in FAMILIES:
        for suffix,cubes in parts.items():
            name = family+'_'+suffix
            bridge.call('create_project',name=name,format='java_block')
            for tex, resource in [('roof',family+'_roof'), ('stone',family+'_smooth'), ('carved',family+'_carved')]:
                texture(bridge,tex,TEXTURES / (resource+'.png'))
            for label,start,end,tex in cubes:
                cuboid(bridge,label,start,end,tex)
            save_export(bridge,'project',MODELS / (name+'.bbmodel'))
            model=runtime_export(bridge,MODELS / (name+'.json'),
                                 [f'xiantu:block/{family}_roof',f'xiantu:block/{family}_smooth',f'xiantu:block/{family}_carved'])
            if suffix == 'roof_ridge':
                model['parent'] = 'minecraft:block/block'
                for element in model['elements']:
                    for face,axis,boundary in [('down',1,0), ('up',1,16), ('north',2,0), ('south',2,16), ('west',0,0), ('east',0,16)]:
                        if element['from' if boundary==0 else 'to'][axis] == boundary:
                            element['faces'][face]['cullface'] = face
                (MODELS / (name+'.json')).write_text(json.dumps(model,indent=2)+'\n',encoding='utf-8')
            metadata.append({'name':name,'elements':len(cubes),'density':'4px/unit; 64x64 textures; proportional UV',
                             'runtime':'native slab/stair states' if suffix != 'roof_ridge' else '3 cubes; facing + waterlogged',
                             'target':'Forge Java 1.20.1 desktop repeated static blocks; no PBR or animation'})
            print('EXPORTED '+name,flush=True)
    (MODELS / 'roof-model-production.json').write_text(json.dumps(metadata,indent=2)+'\n',encoding='utf-8')


if __name__ == '__main__':
    main()
