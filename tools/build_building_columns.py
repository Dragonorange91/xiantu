"""Focused Blockbench rebuild of shafts and collars, preserving accepted plaques."""
from blockbench_bridge import Bridge
from build_building_openings_blockbench import MODELS, texture, cuboid
from build_building_signs_and_columns import TRIMS, package_model
from generate_building_material_resources import FAMILIES, TEXTURES, write_json


def main():
    bridge = Bridge()
    installed = {tool['name'] for tool in bridge.request('tools/list', {})['tools']}
    bridge.call('get_capabilities', include_tools=True) if 'get_capabilities' in installed else bridge.call('get_project_info')
    assert any(c['id']=='java_block' and c['has_compile'] for c in bridge.value(bridge.call('list_export_formats'))['codecs'])
    plans = []
    for family,info in FAMILIES.items():
        for suffix,boxes in {'pillar':[([4,0,4],[12,16,12])], **TRIMS}.items():
            name = family+'_'+suffix
            bridge.call('create_project', name=name, format='java_block')
            if suffix == 'pillar':
                texture(bridge, 'stone', TEXTURES / (info['texture'].split('/')[-1]+'.png'))
                materials = ['xiantu:block/'+info['texture']]
            else:
                texture(bridge, 'smooth', TEXTURES / f'{family}_smooth.png')
                texture(bridge, 'carved', TEXTURES / f'{family}_carved.png')
                materials = [f'xiantu:block/{family}_smooth', f'xiantu:block/{family}_carved']
            for i,(start,end) in enumerate(boxes):
                material = 'stone' if suffix=='pillar' else ('smooth' if (suffix=='pillar_base' and i==2) or (suffix=='pillar_capital' and i==0) else 'carved')
                cuboid(bridge, f'{suffix}_{i}', start, end, material)
            package_model(bridge, name, materials)
            plans.append({'id':name, 'geometry':boxes, 'cubes':len(boxes), 'triangles':len(boxes)*12,
                          'target':'Java Forge 1.20.1 static block', 'shading':'vanilla diffuse', 'animation':'none',
                          'texture_size':[64,64], 'texels_per_model_unit':4,
                          'contacts':'8x8 shaft; base/capital outer width12; vertical boundary contacts preserved'})
            print('EXPORTED', name, flush=True)
    write_json(MODELS/'columns-production.json', plans)


if __name__ == '__main__': main()
