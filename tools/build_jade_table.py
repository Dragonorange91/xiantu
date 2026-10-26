"""Author the large decorative jade desk through desktop Blockbench."""
import json
from blockbench_bridge import Bridge
from build_building_openings_blockbench import MODELS,texture,cuboid,runtime_export,save_export
from generate_building_material_resources import ROOT,ASSETS,DATA,TEXTURES,write_json,block_loot,merge_tag,recipe
BOXES=json.loads((ROOT/'asset_sources/building_materials/jade-table-boxes.json').read_text(encoding='utf-8'))

def main():
    b=Bridge();tools={t['name'] for t in b.request('tools/list',{})['tools']}
    if 'get_capabilities' in tools:b.call('get_capabilities',include_tools=True)
    else:b.call('get_project_info')
    assert any(c['id']=='java_block' and c['has_compile'] for c in b.value(b.call('list_export_formats'))['codecs'])
    MODELS.mkdir(exist_ok=True);save_export(b,'project',MODELS/'before-jade-table.bbmodel')
    resources=['xiantu:block/cloud_jade_smooth','xiantu:block/cloud_jade_carved'];variants={};counts=[]
    for part in range(2):
        name=f'cloud_jade_table_part_{part}';x=(1-part)*16
        b.call('create_project',name=name,format='java_block')
        for tex in ('smooth','carved'):texture(b,tex,TEXTURES/f'cloud_jade_{tex}.png')
        count=0
        for i,box in enumerate(BOXES):
            a,z=box[:3],box[3:];lo=[max(a[0],x)-x,a[1],a[2]];hi=[min(z[0],x+16)-x,z[1],z[2]]
            if lo[0]>=hi[0]:continue
            cuboid(b,f'jade_joinery_{i}',lo,hi,'smooth' if i in (0,1,11,13,15,17) else 'carved');count+=1
        save_export(b,'project',MODELS/f'{name}.bbmodel')
        model=runtime_export(b,MODELS/f'{name}.json',resources);model['parent']='minecraft:block/block'
        write_json(ASSETS/f'models/block/{name}.json',model);counts.append(count)
        for facing,angle in [('north',0),('east',90),('south',180),('west',270)]:variants[f'facing={facing},part={part}']={'model':'xiantu:block/'+name,'y':angle}
    b.call('create_project',name='cloud_jade_table',format='java_block')
    for tex in ('smooth','carved'):texture(b,tex,TEXTURES/f'cloud_jade_{tex}.png')
    for i,box in enumerate(BOXES):
        a,z=box[:3],box[3:]
        cuboid(b,f'jade_item_{i}',[v*.5 if axis!=1 else 4+v*.5 for axis,v in enumerate(a)],
               [v*.5 if axis!=1 else 4+v*.5 for axis,v in enumerate(z)],'smooth' if i in (0,1,11,13,15,17) else 'carved')
    save_export(b,'project',MODELS/'cloud_jade_table.bbmodel')
    model=runtime_export(b,MODELS/'cloud_jade_table.json',resources);model['parent']='minecraft:block/block'
    write_json(ASSETS/'models/block/cloud_jade_table.json',model)
    write_json(ASSETS/'models/item/cloud_jade_table.json',{'parent':'xiantu:block/cloud_jade_table'})
    write_json(ASSETS/'blockstates/cloud_jade_table.json',{'variants':variants})
    loot=block_loot('cloud_jade_table');loot['pools'][0]['conditions'].append({'condition':'minecraft:block_state_property','block':'xiantu:cloud_jade_table','properties':{'part':'0'}})
    write_json(DATA/'loot_tables/blocks/cloud_jade_table.json',loot)
    write_json(DATA/'recipes/cloud_jade_table.json',recipe('cloud_jade_table',['CCC','S S'],{'C':{'item':'xiantu:cloud_jade_carved'},'S':{'item':'xiantu:cloud_jade_smooth'}},1))
    merge_tag('mineable/pickaxe',['xiantu:cloud_jade_table'])
    for lang,label in [('zh_cn','云纹玉案'),('en_us','Cloud-Pattern Jade Desk')]:
        p=ASSETS/f'lang/{lang}.json';data=json.loads(p.read_text(encoding='utf-8'))
        for prefix in ('block','item'):data[f'{prefix}.xiantu.cloud_jade_table']=label
        write_json(p,data)
    write_json(MODELS/'jade-table-production.json',{'target':'Forge1.20.1 desktop repeated static furniture','preference':'near vanilla with finer joinery; user requested larger size','shading':'opaque vanilla diffuse/AO; no PBR','size_units':[32,16,14],'occupied_cells':2,'full_cubes':len(BOXES),'part_cubes':counts,'max_triangles_per_part':max(counts)*12,'UV':'64px accepted tiles;4pixels per model unit; compact item uses half scale','animation':'none','block_entity':False,'new_images':0,'boxes':BOXES})
    print('Blockbench authored jade desk:',len(BOXES),'cuboids; parts',counts)

if __name__=='__main__':main()
