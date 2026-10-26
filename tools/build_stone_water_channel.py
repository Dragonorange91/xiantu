"""Author channel connection geometry via desktop Blockbench, using approved stone tiles."""
from blockbench_bridge import Bridge
from build_building_openings_blockbench import MODELS,texture,cuboid,runtime_export,save_export
from generate_building_material_resources import ASSETS,DATA,TEXTURES,write_json,block_loot,merge_tag,recipe
import json

def boxes(mask):
    parts=[('floor',[0,0,0],[16,8,16],'smooth')]
    for x in (0,14):
        for z in (0,14):parts.append((f'corner_{x}_{z}',[x,8,z],[x+2,16,z+2],'carved'))
    for bit,name,lo,hi in [(1,'north',[2,8,0],[14,16,2]),(2,'east',[14,8,2],[16,16,14]),
                            (4,'south',[2,8,14],[14,16,16]),(8,'west',[0,8,2],[2,16,14])]:
        if not mask&bit:parts.append((name,lo,hi,'carved'))
    return parts

def main():
    b=Bridge();installed={t['name'] for t in b.request('tools/list',{})['tools']}
    b.call('get_capabilities',include_tools=True) if 'get_capabilities' in installed else b.call('get_project_info')
    assert any(c['id']=='java_block' and c['has_compile'] for c in b.value(b.call('list_export_formats'))['codecs'])
    save_export(b,'project',MODELS/'before-stone-water-channel.bbmodel')
    variants={}
    for mask in range(16):
        name=f'spring_stone_water_channel_{mask}'
        b.call('create_project',name=name,format='java_block')
        for tex in ('smooth','carved'):texture(b,tex,TEXTURES/f'spring_stone_{tex}.png')
        for n,lo,hi,tex in boxes(mask):cuboid(b,n,lo,hi,tex)
        save_export(b,'project',MODELS/f'{name}.bbmodel')
        m=runtime_export(b,MODELS/f'{name}.json',['xiantu:block/spring_stone_smooth','xiantu:block/spring_stone_carved'])
        m['parent']='minecraft:block/block'
        # Boundary faces only; internal and top faces must remain visible.
        for e in m['elements']:
            for face,d in e['faces'].items():
                axis,value,limit={'north':(2,e['from'][2],0),'south':(2,e['to'][2],16),
                                  'west':(0,e['from'][0],0),'east':(0,e['to'][0],16),
                                  'down':(1,e['from'][1],0),'up':(1,e['to'][1],16)}[face]
                if value==limit:d['cullface']=face
        write_json(ASSETS/f'models/block/{name}.json',m)
        key=','.join(f'{d}={str(bool(mask&bit)).lower()}' for bit,d in [(1,'north'),(2,'east'),(4,'south'),(8,'west')])
        variants[key]={'model':'xiantu:block/'+name}
    name='spring_stone_water_channel'
    write_json(ASSETS/f'blockstates/{name}.json',{'variants':variants})
    write_json(ASSETS/f'models/item/{name}.json',{'parent':'xiantu:block/'+name+'_0'})
    write_json(DATA/f'loot_tables/blocks/{name}.json',block_loot(name))
    # Five blocks shaped like an open trough; no blueprint or intermediate material.
    write_json(DATA/f'recipes/{name}.json',recipe(name,['S S','SCS'],{'S':{'item':'xiantu:spring_stone_smooth'},'C':{'item':'xiantu:spring_stone_carved'}},4))
    merge_tag('mineable/pickaxe',['xiantu:'+name])
    for lang,label in [('zh_cn','庭水引水槽'),('en_us','Courtyard Water Channel')]:
        p=ASSETS/f'lang/{lang}.json';data=json.loads(p.read_text(encoding='utf-8'))
        for prefix in ('block','item'):data[f'{prefix}.xiantu.{name}']=label
        write_json(p,data)
    write_json(MODELS/'water-channel-production.json',{'target':'Forge Java1.20.1 static building block','device':'desktop','preference':'accepted near vanilla, carved detail in texture','shading':'opaque native diffuse/AO; vanilla fluid pass','texel_density':4,'texture_size':[64,64],'texture_reuse':['spring_stone_smooth','spring_stone_carved'],'new_images':0,'models':16,'geometry':[boxes(m) for m in range(16)],'max_cubes':9,'max_triangles':108,'animation':'none; native water rendering','block_entity':False,'per_tick_work':'none; native scheduled fluid updates only','size':'one block; half-block floor, half-block basin with2unit rims','materials':'4smooth+1carved produces4; workbench direct'})
    print('Blockbench channel exports complete:16connection models')

if __name__=='__main__':main()
