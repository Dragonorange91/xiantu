"""Author five masonry beam/bracket sets through the installed Blockbench MCP."""
import json
from blockbench_bridge import Bridge
from build_building_openings_blockbench import MODELS, texture, cuboid, runtime_export, save_export
from generate_building_material_resources import ASSETS, DATA, TEXTURES, FAMILIES, write_json, block_loot, merge_tag

PARTS = {
    'beam': [([0,0,4],[16,6,12]), ([0,6,3],[16,8,13]), ([0,8,5],[16,16,11])],
    'bracket': [([4,0,4],[12,6,12]), ([3,6,3],[13,12,13]), ([0,12,4],[16,16,12])],
}

def connection_boxes(kind):
    boxes=[]
    for lo,hi,bottom,top in [(4,12,0,6),(3,13,6,8),(5,11,8,16)]:
        if kind=='corner':
            boxes += [([lo,bottom,0],[hi,top,hi]),([hi,bottom,lo],[16,top,hi])]
        else:
            boxes += [([0,bottom,lo],[16,top,hi]),([lo,bottom,0],[hi,top,lo])]
            if kind=='cross': boxes += [([lo,bottom,hi],[hi,top,16])]
    return boxes

CONNECTION_PARTS = {kind:connection_boxes(kind) for kind in ('corner','tee','cross')}
JOIN_MODELS = {
    'straight_x':('',0),'straight_z':('',90),
    'corner_ne':('_corner',0),'corner_se':('_corner',90),
    'corner_sw':('_corner',180),'corner_nw':('_corner',270),
    'tee_wne':('_tee',0),'tee_nes':('_tee',90),
    'tee_esw':('_tee',180),'tee_swn':('_tee',270),'cross':('_cross',0),
}

def beam_blockstate(family):
    return {'variants':{f'join={join}':{'model':f'xiantu:block/{family}_beam{suffix}','y':angle}
                        for join,(suffix,angle) in JOIN_MODELS.items()}}


def recipe(family, suffix):
    if suffix == 'beam':
        return {'type':'minecraft:crafting_shaped','pattern':['PP'],
                'key':{'P':{'item':'xiantu:'+family+'_pillar'}},
                'result':{'item':'xiantu:'+family+'_beam','count':2}}
    return {'type':'minecraft:crafting_shapeless',
            'ingredients':[{'item':'xiantu:'+family+'_pillar'}, {'item':'xiantu:'+family+'_pillar_capital'}],
            'result':{'item':'xiantu:'+family+'_bracket','count':2}}


def main():
    bridge = Bridge()
    installed = {tool['name'] for tool in bridge.request('tools/list',{})['tools']}
    if 'get_capabilities' in installed: bridge.call('get_capabilities',include_tools=True)
    else: bridge.call('get_project_info')
    assert any(c['id']=='java_block' and c['has_compile'] for c in bridge.value(bridge.call('list_export_formats'))['codecs'])
    labels = {loc:json.loads((ASSETS/f'lang/{loc}.json').read_text(encoding='utf-8')) for loc in ('zh_cn','en_us')}
    plans = []
    for family,info in FAMILIES.items():
        for suffix,boxes in PARTS.items():
            name = family+'_'+suffix
            bridge.call('create_project',name=name,format='java_block')
            texture(bridge,'smooth',TEXTURES/f'{family}_smooth.png')
            texture(bridge,'carved',TEXTURES/f'{family}_carved.png')
            for i,(start,end) in enumerate(boxes):
                cuboid(bridge,f'{suffix}_{i}',start,end,'carved' if i==(1 if suffix=='beam' else 2) else 'smooth')
            save_export(bridge,'project',MODELS/f'{name}.bbmodel')
            model = runtime_export(bridge,MODELS/f'{name}.json',
                                   ['xiantu:block/'+family+'_smooth','xiantu:block/'+family+'_carved'])
            model['parent'] = 'minecraft:block/block'
            write_json(ASSETS/f'models/block/{name}.json',model)
            write_json(ASSETS/f'models/item/{name}.json',{'parent':'xiantu:block/'+name})
            write_json(ASSETS/f'blockstates/{name}.json',{'variants':{
                f'facing={face}':{'model':'xiantu:block/'+name,'y':angle}
                for face,angle in [('north',0),('east',90),('south',180),('west',270)]}})
            write_json(DATA/f'loot_tables/blocks/{name}.json',block_loot(name))
            write_json(DATA/f'recipes/{name}.json',recipe(family,suffix))
            for loc,label in [('zh_cn',info['names'][0]+('横梁' if suffix=='beam' else '承梁托座')),
                              ('en_us',info['english'][0]+(' Beam' if suffix=='beam' else ' Beam Bracket'))]:
                for prefix in ('block','item'): labels[loc][f'{prefix}.xiantu.{name}'] = label
            plans.append({'id':name,'target':'Forge Java 1.20.1 static building block',
                          'preference':'near vanilla geometry and texture detail','device':'Minecraft desktop',
                          'cubes':len(boxes),'faces':len(boxes)*6,'triangles':len(boxes)*12,'geometry':boxes,
                          'shading':'vanilla opaque diffuse','animation':'none','texture_size':[64,64],
                          'texels_per_model_unit':4,'uv':'proportionate sections of approved smooth/carved textures',
                          'seams':'bracket top16 meets beam bottom0; raised bearing reaches16 and meets roof bottom0'})
            print('EXPORTED',name,flush=True)
        for kind,boxes in CONNECTION_PARTS.items():
            name = family+'_beam_'+kind
            bridge.call('create_project',name=name,format='java_block')
            texture(bridge,'smooth',TEXTURES/f'{family}_smooth.png')
            texture(bridge,'carved',TEXTURES/f'{family}_carved.png')
            for i,(start,end) in enumerate(boxes):
                cuboid(bridge,f'{kind}_{i}',start,end,'carved' if start[1]==6 else 'smooth')
            save_export(bridge,'project',MODELS/f'{name}.bbmodel')
            model=runtime_export(bridge,MODELS/f'{name}.json',
                                 ['xiantu:block/'+family+'_smooth','xiantu:block/'+family+'_carved'])
            model['parent']='minecraft:block/block'
            write_json(MODELS/f'{name}.json',model)
            write_json(ASSETS/f'models/block/{name}.json',model)
            plans.append({'id':name,'model_only':True,'geometry':boxes,'cubes':len(boxes),
                          'triangles':len(boxes)*12,'texture_size':[64,64],'texels_per_model_unit':4,
                          'target':'Java Forge 1.20.1 static neighbor-connected beam',
                          'shading':'vanilla diffuse','animation':'none',
                          'seams':'layer widths8/10/6 match straight spans at all connected cell boundaries'})
            print('EXPORTED',name,flush=True)
        write_json(ASSETS/f'blockstates/{family}_beam.json',beam_blockstate(family))
    for loc,values in labels.items(): write_json(ASSETS/f'lang/{loc}.json',values)
    merge_tag('mineable/pickaxe',['xiantu:'+f+'_'+s for f in FAMILIES for s in PARTS])
    write_json(MODELS/'beams-production.json',plans)


if __name__ == '__main__': main()
