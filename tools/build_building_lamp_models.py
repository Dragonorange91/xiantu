"""Author ten static shrine lamp models in the installed desktop Blockbench MCP."""
import json
from blockbench_bridge import Bridge
from build_building_openings_blockbench import MODELS, texture, cuboid, runtime_export, save_export
from generate_building_material_resources import FAMILIES, TEXTURES

STAND = [
    ('plinth', [2,0,2], [14,3,14], 'smooth'),
    ('carved_stem', [4,3,4], [12,14,12], 'carved'),
    ('offering_plate', [2,14,2], [14,16,14], 'smooth'),
]
LAMP_BASE = [
    ('plinth', [2,0,2], [14,3,14], 'smooth'),
    ('carved_stem', [4,3,4], [12,7,12], 'carved'),
    ('offering_plate', [3,7,3], [13,9,13], 'smooth'),
]
LANTERN = [
    ('amber_core', [6,9,6], [10,13,10], 'core'),
    ('northwest_post', [4,9,4], [5,13,5], 'smooth'),
    ('northeast_post', [11,9,4], [12,13,5], 'smooth'),
    ('southwest_post', [4,9,11], [5,13,12], 'smooth'),
    ('southeast_post', [11,9,11], [12,13,12], 'smooth'),
    ('wide_eaves', [1,13,1], [15,14,15], 'carved'),
    ('roof_cap', [3,14,3], [13,15,13], 'roof'),
    ('roof_finial', [5,15,5], [11,16,11], 'roof'),
]


def main():
    bridge = Bridge()
    info = bridge.value(bridge.call('get_project_info'))
    tools = bridge.request('tools/list', {}).get('tools', [])
    if any(tool['name']=='get_capabilities' for tool in tools):
        bridge.call('get_capabilities', include_tools=True)
    assert any(c['id']=='java_block' and c['has_compile'] for c in
               bridge.value(bridge.call('list_export_formats'))['codecs'])
    MODELS.mkdir(parents=True, exist_ok=True)
    backup = MODELS / 'before-stone-lamps.bbmodel'
    if not backup.exists():
        save_export(bridge, 'project', backup)
    plan = {'target':'Forge Java 1.20.1 desktop repeated building blocks',
            'style':'user selected near-vanilla geometry; approved carving represented by albedo',
            'shading':'vanilla diffuse/AO, opaque; native block light only, no PBR/bloom',
            'budget':'3 cubes pedestal; 11 cubes lamp; maximum 132 triangles per lamp',
            'texture':'64x64 shared tiles, 4 pixels per model unit with proportional UV',
            'animation':'none; no block entity or ticking; waterlogged block state',
            'source_project':info.get('project'), 'models':[]}
    for family in FAMILIES:
        for suffix, cubes in [('lamp_stand',STAND),('stone_lamp',LAMP_BASE+LANTERN)]:
            name = family+'_'+suffix
            bridge.call('create_project',name=name,format='java_block')
            sources = [('smooth',family+'_smooth'),('carved',family+'_carved'),
                       ('roof',family+'_roof'),('core','stone_lamp_core')]
            for tex, resource in sources:
                texture(bridge,tex,TEXTURES/(resource+'.png'))
            for label,start,end,tex in cubes:
                cuboid(bridge,label,start,end,tex)
            save_export(bridge,'project',MODELS/(name+'.bbmodel'))
            model = runtime_export(bridge,MODELS/(name+'.json'),
                                   ['xiantu:block/'+resource for _,resource in sources])
            model['parent']='minecraft:block/block'
            for element in model['elements']:
                if element['name']=='amber_core':
                    element['shade']=False
                # Preserve faces: overhanging trays and eaves are visible from below.
                # Shared block edges also need their own faces when used separately.
            (MODELS/(name+'.json')).write_text(json.dumps(model,indent=2)+'\n',encoding='utf-8')
            plan['models'].append({'name':name,'cubes':len(cubes),'faces':sum(len(e['faces']) for e in model['elements'])})
            print('EXPORTED '+name,flush=True)
    (MODELS/'stone-lamp-production.json').write_text(json.dumps(plan,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')


if __name__=='__main__':
    main()
