"""Produce five tiled eaves/corner sets through desktop Blockbench MCP."""
import json
from blockbench_bridge import Bridge
from build_building_openings_blockbench import MODELS, texture, cuboid, runtime_export, save_export
from generate_building_material_resources import ASSETS, DATA, TEXTURES, FAMILIES, write_json, block_loot, merge_tag

PARTS = {
    'eaves': [([0,0,0],[16,4,16]), ([0,4,0],[16,6,8]), ([0,6,0],[16,8,4])],
    'eaves_corner': [([0,0,0],[16,4,16]), ([0,4,0],[16,6,8]), ([0,4,8],[8,6,16]),
                     ([0,6,0],[16,8,4]), ([0,6,4],[4,8,16])],
}


def recipe(family, suffix):
    if suffix == 'eaves':
        return {'type':'minecraft:crafting_shaped','pattern':['B ','BB'],
                'key':{'B':{'item':'xiantu:'+family}},
                'result':{'item':'xiantu:'+family+'_eaves','count':6}}
    return {'type':'minecraft:crafting_shapeless',
            'ingredients':[{'item':'xiantu:'+family+'_eaves'}]*2,
            'result':{'item':'xiantu:'+family+'_eaves_corner','count':2}}


def main():
    bridge = Bridge()
    installed = {t['name'] for t in bridge.request('tools/list',{})['tools']}
    if 'get_capabilities' in installed: bridge.call('get_capabilities',include_tools=True)
    else: bridge.call('get_project_info')
    assert any(c['id']=='java_block' and c['has_compile'] for c in bridge.value(bridge.call('list_export_formats'))['codecs'])
    labels = {loc:json.loads((ASSETS/f'lang/{loc}.json').read_text(encoding='utf-8')) for loc in ('zh_cn','en_us')}
    plans=[]
    for family,info in FAMILIES.items():
        for suffix,boxes in PARTS.items():
            name=family+'_'+suffix
            bridge.call('create_project',name=name,format='java_block')
            texture(bridge,'roof',TEXTURES/f'{family}_roof.png')
            texture(bridge,'carved',TEXTURES/f'{family}_carved.png')
            for i,(start,end) in enumerate(boxes):
                cuboid(bridge,f'{suffix}_{i}',start,end,'carved' if i==0 else 'roof')
            save_export(bridge,'project',MODELS/f'{name}.bbmodel')
            model=runtime_export(bridge,MODELS/f'{name}.json',['xiantu:block/'+family+'_roof','xiantu:block/'+family+'_carved'])
            model['parent']='minecraft:block/block'
            write_json(ASSETS/f'models/block/{name}.json',model)
            write_json(ASSETS/f'models/item/{name}.json',{'parent':'xiantu:block/'+name})
            write_json(ASSETS/f'blockstates/{name}.json',{'variants':{
                f'facing={face}':{'model':'xiantu:block/'+name,'y':angle}
                for face,angle in [('north',0),('east',90),('south',180),('west',270)]}})
            write_json(DATA/f'loot_tables/blocks/{name}.json',block_loot(name))
            write_json(DATA/f'recipes/{name}.json',recipe(family,suffix))
            for loc,label in [('zh_cn',info['names'][0]+('飞檐' if suffix=='eaves' else '外角飞檐')),
                              ('en_us',info['english'][0]+(' Eaves' if suffix=='eaves' else 'Outer Corner Eaves'))]:
                for prefix in ('block','item'): labels[loc][f'{prefix}.xiantu.{name}']=label
            plans.append({'id':name,'target':'Minecraft Java 1.20.1 Forge static block, desktop',
                          'preference':'near vanilla silhouette; detail from approved textures',
                          'geometry':boxes,'cubes':len(boxes),'faces':len(boxes)*6,'triangles':len(boxes)*12,
                          'shading':'opaque vanilla diffuse','special_materials':False,'animation':'none',
                          'texture_size':[64,64],'texels_per_model_unit':4,
                          'uv':'proportionate subregions; do not stretch the whole tile onto thin side faces',
                          'connections':'north lip; corner north+west; same-level continuous eaves',
                          'verification':'check quarter-turn and reflected corner collision, side joins and visible underside'})
            print('EXPORTED',name,flush=True)
    for loc,values in labels.items(): write_json(ASSETS/f'lang/{loc}.json',values)
    merge_tag('mineable/pickaxe',['xiantu:'+f+'_'+s for f in FAMILIES for s in PARTS])
    write_json(MODELS/'eaves-production.json',plans)


if __name__ == '__main__': main()
