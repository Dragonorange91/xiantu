"""Create the black-rock chest using the desktop Blockbench MCP and accepted stone textures."""
import json
from blockbench_bridge import Bridge
from build_building_openings_blockbench import MODELS, texture, cuboid, runtime_export, save_export
from generate_building_material_resources import ROOT, ASSETS, DATA, TEXTURES, write_json, block_loot, merge_tag, recipe

def boxes(kind):
    lo,hi=(1,15) if kind=='single' else (1,16) if kind=='left' else (0,15)
    # North is the front. A hollow stone carcass; double halves omit the joining wall.
    base=[('plinth',[lo,0,1],[hi,2,15],'carved'),
          ('floor',[lo+1 if kind!='right' else lo,2,2],[hi-1 if kind!='left' else hi,3,14],'smooth'),
          ('front',[lo,2,1],[hi,10,3],'smooth'),
          ('back',[lo,2,13],[hi,10,15],'smooth'),
          ('front_border',[lo,3,0.5],[hi,4,1],'carved'),
          ('front_upper',[lo,8,0.5],[hi,10,1],'carved')]
    if kind!='right':
        base += [('left_wall',[lo,2,3],[lo+2,10,13],'smooth'),('left_rim',[lo,8,3],[lo+2,10,13],'carved')]
    if kind!='left':
        base += [('right_wall',[hi-2,2,3],[hi,10,13],'smooth'),('right_rim',[hi-2,8,3],[hi,10,13],'carved')]
    if kind=='single': locklo,lockhi=6,10
    elif kind=='left': locklo,lockhi=14,16
    else: locklo,lockhi=0,2
    lid=[('lid_slab',[lo,10,1],[hi,13,15],'smooth'),
         ('cap',[lo+1 if kind!='right' else lo,13,2],[hi-1 if kind!='left' else hi,14,14],'carved'),
         ('front_band',[lo,11,0.5],[hi,13,1],'carved'),
         ('latch',[locklo,8,0],[lockhi,12,1],'carved')]
    return base,lid

def main():
    b=Bridge();available={x['name'] for x in b.request('tools/list',{})['tools']}
    b.call('get_project_info')
    assert any(c['id']=='java_block' and c['has_compile'] for c in b.value(b.call('list_export_formats'))['codecs'])
    save_export(b,'project',MODELS/'before-stone-chest.bbmodel')
    resources=['xiantu:block/black_rock_smooth','xiantu:block/black_rock_carved']
    counts={}
    for kind in ('left','right','single'):
        base,lid=boxes(kind)
        for part,geometry in [('base',base),('lid',lid),('closed',base+lid)]:
            name=f'black_rock_chest_{part}_{kind}'
            b.call('create_project',name=name,format='java_block')
            for tex in ('smooth','carved'):texture(b,tex,TEXTURES/f'black_rock_{tex}.png')
            for label,a,z,tex in geometry:cuboid(b,label,a,z,tex)
            save_export(b,'project',MODELS/f'{name}.bbmodel')
            model=runtime_export(b,MODELS/f'{name}.json',resources)
            model['parent']='minecraft:block/block'
            write_json(ASSETS/f'models/block/{name}.json',model)
            counts[f'{part}_{kind}']=len(geometry)
    # Item uses the complete closed mesh, with no block-entity renderer in inventory.
    write_json(ASSETS/'models/item/black_rock_chest.json',{'parent':'xiantu:block/black_rock_chest_closed_single'})
    variants={}
    for kind in ('single','left','right'):
        for facing,angle in [('north',0),('east',90),('south',180),('west',270)]:
            variants[f'facing={facing},type={kind}']={'model':f'xiantu:block/black_rock_chest_closed_{kind}','y':angle}
    write_json(ASSETS/'blockstates/black_rock_chest.json',{'variants':variants})
    write_json(DATA/'loot_tables/blocks/black_rock_chest.json',block_loot('black_rock_chest'))
    write_json(DATA/'recipes/black_rock_chest.json',recipe('black_rock_chest',['CSC','S S','CSC'],{'C':{'item':'xiantu:black_rock_carved'},'S':{'item':'xiantu:black_rock_smooth'}},1))
    merge_tag('mineable/pickaxe',['xiantu:black_rock_chest'])
    for lang,label in [('zh_cn','玄岩藏箱'),('en_us','Profound Stone Chest')]:
        p=ASSETS/f'lang/{lang}.json';data=json.loads(p.read_text(encoding='utf-8'))
        for prefix in ('block','item','container'):data[f'{prefix}.xiantu.black_rock_chest']=label
        write_json(p,data)
    write_json(MODELS/'stone-chest-production.json',{'target':'Forge1.20.1 desktop container','preference':'accepted near-vanilla detail; full-size stone chest','shading':'opaque vanilla diffuse; no PBR','dimensions':[14,14,15],'slots':27,'double_slots':54,'counts':counts,'max_triangles_single':counts['closed_single']*12,'textures':'2 accepted64px tiles,4px/model-unit','new_images':0,'new_sounds':0,'sound':'native chest open/close; stone block interactions','animation':'native lid controller,ease-out0..90deg;back hinge[0,10,15]','inventory':'static full mesh; no BEWLR','boxes':{kind:boxes(kind) for kind in ('single','left','right')}})
    print('Blockbench stone chest exported:',counts,flush=True)

if __name__=='__main__':main()
