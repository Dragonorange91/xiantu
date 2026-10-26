"""Texture-first native Blockbench authoring of the material appraisal altar."""
import json,shutil
from blockbench_bridge import Bridge
from build_building_openings_blockbench import texture,cuboid,runtime_export,save_export
from generate_building_material_resources import ROOT,ASSETS,DATA,write_json,merge_tag,block_loot,recipe
BASE=ROOT/'asset_sources/material_appraisal'
def main():
    from PIL import Image
    im=Image.open(BASE/'top.png');assert im.size==(64,64) and im.mode=='RGBA' and im.getextrema()[3]==(255,255)
    shutil.copy2(BASE/'top.png',ASSETS/'textures/block/appraisal_altar_top.png')
    b=Bridge();tools={t['name'] for t in b.request('tools/list',{})['tools']}
    b.call('get_capabilities',include_tools=True) if 'get_capabilities' in tools else b.call('get_project_info')
    save_export(b,'project',BASE/'before-appraisal.bbmodel')
    b.call('create_project',name='appraisal_altar',format='java_block')
    names=['cloud_jade_smooth','cloud_jade_carved','appraisal_altar_top']
    for name in names:texture(b,name,ASSETS/f'textures/block/{name}.png')
    parts=[('base',[1,0,1],[15,3,15],names[0]),('carved_pedestal',[3,3,3],[13,11,13],names[1]),('tabletop',[0,11,0],[16,15,16],names[0]),('formation_plate',[1,15,1],[15,15.5,15],names[2])]
    for n,lo,hi,tex in parts:cuboid(b,n,lo,hi,tex,{'up':[0,0,16,16]} if n=='formation_plate' else None)
    save_export(b,'project',BASE/'appraisal_altar.bbmodel')
    model=runtime_export(b,BASE/'appraisal_altar.json',['xiantu:block/'+n for n in names]);model['parent']='minecraft:block/block'
    write_json(ASSETS/'models/block/appraisal_altar.json',model)
    write_json(ASSETS/'models/item/appraisal_altar.json',{'parent':'xiantu:block/appraisal_altar'})
    write_json(ASSETS/'blockstates/appraisal_altar.json',{'variants':{'':{'model':'xiantu:block/appraisal_altar'}}})
    write_json(DATA/'loot_tables/blocks/appraisal_altar.json',block_loot('appraisal_altar'))
    write_json(DATA/'recipes/appraisal_altar.json',recipe('appraisal_altar',['SSS','SGS','C C'],{'S':{'item':'minecraft:stone'},'G':{'item':'minecraft:glass'},'C':{'item':'minecraft:copper_ingot'}},1))
    merge_tag('mineable/pickaxe',['xiantu:appraisal_altar'])
    for lang,name in [('zh_cn','鉴定灵台'),('en_us','Material Appraisal Altar')]:
        path=ASSETS/f'lang/{lang}.json';labels=json.loads(path.read_text(encoding='utf-8'))
        for prefix in ['item','block']:labels[f'{prefix}.xiantu.appraisal_altar']=name
        write_json(path,labels)
    write_json(BASE/'production.json',{'target':'Forge Java1.20.1 block','device':'desktop','preference':'near-vanilla balanced, stone detail in texture','shading':'native opaque diffuse/AO','max_cubes':4,'max_triangles':48,'new_images':1,'source_dimensions':[1254,1254],'master_dimensions':[1024,1024],'runtime_dimensions':[64,64],'texel_density':4,'special_plate_uv':'unique14x14 top panel uses full64 texture intentionally','textures':names,'animation':'none','geometry':parts,'native_export':True,'user_acceptance':'pending'})
    print('Appraisal altar complete:1 API master,1 runtime texture,1 MCP model+editable source; native static rendering.')
if __name__=='__main__':main()
