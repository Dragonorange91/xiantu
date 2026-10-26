"""Eight spirit-wood families. Only reviewed API candidates enter Forge resources."""
import json,shutil
from pathlib import Path
from spirit_wood_catalog import ROOT,TREES,TILES,SPRITES
from generate_building_material_resources import ASSETS,DATA,write_json,merge_tag,variant_blockstate,slab_blockstate,wall_blockstate,block_loot
from build_building_beams import beam_blockstate
BASE=ROOT/'asset_sources/spirit_wood'
BUILDING=['smooth','carved','pillar','stairs','slab','wall','railing','smooth_stairs','smooth_slab','carved_stairs','carved_slab','door','trapdoor','window','beam','bracket','eaves','eaves_corner','pillar_base','pillar_capital','roof_tiles','roof_stairs','roof_ridge','frieze','plaque_board']
NAMES=dict(zip(['log','stripped_log','planks','leaves','sapling','stick','long_shaft','fiber']+BUILDING,
 ['原木','去皮原木','木板','树叶','树苗','木棍','长杆','纤维','去纹板','雕花板','柱','楼梯','台阶','墙','栏杆','去纹楼梯','去纹台阶','雕花楼梯','雕花台阶','门','活板门','格栅窗','横梁','斗拱','飞檐','转角飞檐','柱基','柱头','屋瓦','斜瓦','屋脊','饰条','牌匾底板']))
def model(name,parent,textures):write_json(ASSETS/f'models/block/{name}.json',{'parent':parent,'textures':textures})
def item(name,parent):write_json(ASSETS/f'models/item/{name}.json',{'parent':parent})
def recipe(name,pattern,key,count=1,wood=None):
    data={'type':'xiantu:spirit_wood_processing' if wood else 'minecraft:crafting_shaped','pattern':pattern,'key':{k:{'item':v} for k,v in key.items()},'result':{'item':'xiantu:'+name,'count':count}}
    if wood:data.update(wood=wood)
    write_json(DATA/f'recipes/{name}.json',data)
def resources():
    from revise_spirit_wood_art import install_reviewed
    install_reviewed()
    status=json.loads((BASE/'production-status.json').read_text(encoding='utf-8'))
    assert len(status)==16 and all(v['review']=='passed' for v in status.values()),'Inspect every image before resource/model phase'
    labels={loc:json.loads((ASSETS/f'lang/{loc}.json').read_text(encoding='utf-8')) for loc in ('zh_cn','en_us')}
    # Remove obsolete standalone-biome naming from generated resources.
    for path in (
        DATA/'worldgen/biome/qingmu_forest.json',
        DATA/'tags/worldgen/biome/terrain/qingmu_forest.json',
        DATA/'tags/worldgen/biome/spirit_wood/qingmu_forest.json',
        DATA/'forge/biome_modifier/qingmu_forest.json',
        DATA/'forge/biome_modifier/qing_wen_elm_tree_forest.json',
        DATA/'worldgen/placed_feature/qing_wen_elm_tree_forest.json',
    ):
        path.unlink(missing_ok=True)
    for locale in ('zh_cn', 'en_us'):
        labels[locale].pop('biome.xiantu.qingmu_forest', None)
    for path in DATA.glob("recipes/*_purification.json"):path.unlink()
    allblocks=[];logs=[];leaves=[];walls=[];fences=[];doors=[];trapdoors=[]
    for tree in TREES:
        f,cn,wood,*_=tree
        def bid(k):return f'{f}_{k}'
        def ref(k):return 'xiantu:'+bid(k)
        def tex(k):return 'xiantu:block/'+bid(k)
        for suffix in TILES+['leaves','sapling']:
            if suffix=='window_frame':continue
            target=ASSETS/f'textures/block/{bid(suffix)}.png';target.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(BASE/f'candidate/{f}/{suffix}.png',target)
        for suffix in ['stick','long_shaft','fiber']:
            target=ASSETS/f'textures/item/{bid(suffix)}.png';shutil.copy2(BASE/f'candidate/{f}/{suffix}.png',target)
            write_json(ASSETS/f'models/item/{bid(suffix)}.json',{'parent':'minecraft:item/generated','textures':{'layer0':'xiantu:item/'+bid(suffix)}})
        for k in ['log','stripped_log']:
            name=bid(k);model(name,'minecraft:block/cube_column',{'side':tex(k+'_side'),'end':tex(k+'_top')})
            write_json(ASSETS/f'blockstates/{name}.json',{'variants':{'axis=y':{'model':'xiantu:block/'+name},'axis=x':{'model':'xiantu:block/'+name,'x':90,'y':90},'axis=z':{'model':'xiantu:block/'+name,'x':90}}});item(name,'xiantu:block/'+name);logs.append(ref(k))
        for k in ['planks','smooth','carved','frieze','plaque_board','leaves']:
            name=bid(k);model(name,'minecraft:block/leaves' if k=='leaves' else 'minecraft:block/cube_all',{'all':tex(k)})
            if k=='leaves':
                obj=json.loads((ASSETS/f'models/block/{name}.json').read_text());obj['render_type']='minecraft:cutout_mipped';write_json(ASSETS/f'models/block/{name}.json',obj);leaves.append(ref(k))
            write_json(ASSETS/f'blockstates/{name}.json',{'variants':{'':{'model':'xiantu:block/'+name}}});item(name,'xiantu:block/'+name)
        name=bid('sapling');model(name,'minecraft:block/cross',{'cross':tex('sapling')});obj=json.loads((ASSETS/f'models/block/{name}.json').read_text());obj['render_type']='minecraft:cutout';write_json(ASSETS/f'models/block/{name}.json',obj);write_json(ASSETS/f'blockstates/{name}.json',{'variants':{'':{'model':'xiantu:block/'+name}}});write_json(ASSETS/f'models/item/{name}.json',{'parent':'minecraft:item/generated','textures':{'layer0':tex('sapling')}})
        # Native shape parents and their exact1.20.1 states, never regenerate accepted stone assets.
        for kind in ['stairs','smooth_stairs','carved_stairs','roof_stairs']:
            name=bid(kind);t=tex('smooth' if kind.startswith('smooth') else 'carved' if kind.startswith('carved') else 'roof' if kind.startswith('roof') else 'planks')
            for suffix,parent in [('', 'stairs'),('_inner','inner_stairs'),('_outer','outer_stairs')]:model(name+suffix,'minecraft:block/'+parent,{'bottom':t,'top':t,'side':t})
            write_json(ASSETS/f'blockstates/{name}.json',variant_blockstate(name));item(name,'xiantu:block/'+name)
        for kind in ['slab','smooth_slab','carved_slab','roof_tiles']:
            name=bid(kind);t=tex('smooth' if kind.startswith('smooth') else 'carved' if kind.startswith('carved') else 'roof' if kind.startswith('roof') else 'planks')
            for suffix,parent in [('', 'slab'),('_top','slab_top'),('_double','cube_all')]:model(name+suffix,'minecraft:block/'+parent,{'all':t} if suffix=='_double' else {'bottom':t,'top':t,'side':t})
            write_json(ASSETS/f'blockstates/{name}.json',slab_blockstate(name));item(name,'xiantu:block/'+name)
        name=bid('wall');walls.append(ref('wall'))
        for suffix,parent in [('_post','template_wall_post'),('_side','template_wall_side'),('_side_tall','template_wall_side_tall'),('_inventory','wall_inventory')]:model(name+suffix,'minecraft:block/'+parent,{'wall':tex('planks')})
        write_json(ASSETS/f'blockstates/{name}.json',wall_blockstate(name));item(name,'xiantu:block/'+name+'_inventory')
        name=bid('railing');fences.append(ref('railing'))
        for suffix,parent in [('_post','fence_post'),('_side','fence_side'),('_inventory','fence_inventory')]:model(name+suffix,'minecraft:block/'+parent,{'texture':tex('planks')})
        state={'multipart':[{'apply':{'model':'xiantu:block/'+name+'_post'}}]+[{'when':{d:'true'},'apply':{'model':'xiantu:block/'+name+'_side','y':angle,'uvlock':True}} for d,angle in [('north',0),('east',90),('south',180),('west',270)]]};write_json(ASSETS/f'blockstates/{name}.json',state);item(name,'xiantu:block/'+name+'_inventory')
        for kind in ['door','trapdoor']:
            name=bid(kind);template=ASSETS/f'blockstates/azure_brick_{kind}.json';write_json(ASSETS/f'blockstates/{name}.json',json.loads(template.read_text().replace('azure_brick',f)))
            suffixes=['bottom_left','bottom_right','bottom_left_open','bottom_right_open','top_left','top_right','top_left_open','top_right_open'] if kind=='door' else ['bottom','top','open']
            for suffix in suffixes:
                template=ASSETS/f'models/block/azure_brick_{kind}_{suffix}.json';obj=json.loads(template.read_text());obj['textures']={'bottom':tex('door_bottom'),'top':tex('door_top')} if kind=='door' else {'texture':tex('trapdoor')};write_json(ASSETS/f'models/block/{name}_{suffix}.json',obj)
            if kind=='door':doors.append(ref(kind))
            else:trapdoors.append(ref(kind));item(name,'xiantu:block/'+name+'_bottom')
        for kind in ['pillar','pillar_base','pillar_capital','beam','bracket','eaves','eaves_corner','roof_ridge','window']:
            template=ASSETS/f'blockstates/azure_brick_{kind}.json';write_json(ASSETS/f'blockstates/{bid(kind)}.json',json.loads(template.read_text().replace('azure_brick',f)))
            item(bid(kind),'xiantu:block/'+bid(kind))
        for kind in ['log','stripped_log','planks','sapling']+BUILDING:
            name=bid(kind);allblocks.append(ref(kind));loot=block_loot(name)
            if kind in ['slab','smooth_slab','carved_slab','roof_tiles']:
                loot['pools'][0]['entries'][0]['functions']=[{'function':'minecraft:set_count','count':2,'conditions':[{'condition':'minecraft:block_state_property','block':ref(kind),'properties':{'type':'double'}}]}]
            if kind=='door':loot['pools'][0]['conditions']=[{'condition':'minecraft:block_state_property','block':ref(kind),'properties':{'half':'lower'}}]
            write_json(DATA/f'loot_tables/blocks/{name}.json',loot)
        allblocks.append(ref('leaves'));write_json(DATA/f'loot_tables/blocks/{bid("leaves")}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':ref('leaves')}],'conditions':[{'condition':'minecraft:match_tool','predicate':{'items':['minecraft:shears']}}]}]})
        # Processing includes no blueprints, fuel or professional XP.
        recipe(bid('planks'),['L'],{'L':ref('log')},4,f)
        recipe(bid('planks_from_stripped'),['L'],{'L':ref('stripped_log')},4,f);p=DATA/f'recipes/{bid("planks_from_stripped")}.json';obj=json.loads(p.read_text());obj['result']['item']=ref('planks');write_json(p,obj)
        for kind,pattern,source,count in [('stick',['P','P'],'planks',4),('long_shaft',['PP','PP'],'stick',1),('fiber',['P'],'planks',2)]:recipe(bid(kind),pattern,{'P':ref(source)},count,f)
        for kind,pattern,source,count in [('smooth',['BB','BB'],'planks',4),('carved',['B','B'],'slab',1),('pillar',['B','B'],'smooth',2),('stairs',['B  ','BB ','BBB'],'planks',4),('slab',['BBB'],'planks',6),('wall',['BBB','BBB'],'planks',6),('railing',['BB','BB','BB'],'planks',6),('smooth_stairs',['B  ','BB ','BBB'],'smooth',4),('smooth_slab',['BBB'],'smooth',6),('carved_stairs',['B  ','BB ','BBB'],'carved',4),('carved_slab',['BBB'],'carved',6),('door',['BB','BB','BB'],'smooth',3),('trapdoor',['BBB','BBB'],'carved',2),('roof_tiles',['B','B','B'],'planks',6),('roof_stairs',['B  ','BB ','BBB'],'roof_tiles',4),('roof_ridge',['B  ',' B ','  B'],'smooth',6),('frieze',['B','B','B'],'carved',6),('plaque_board',['BB'],'smooth',2),('pillar_base',['B','B'],'pillar',2),('pillar_capital',['BB'],'pillar',2),('eaves_corner',['BB'],'eaves',2)]:recipe(bid(kind),pattern,{'B':ref(source)},count)
        for kind,pattern in [('beam',['BB','F ']),('bracket',['BFB']),('eaves',['B ','FB'])]:recipe(bid(kind),pattern,{'B':ref('planks'),'F':ref('fiber')},2)
        recipe(bid('window'),['BGB','BGB'],{'B':ref('planks'),'G':'minecraft:glass'},4)
        for kind,suffix in NAMES.items():
            cnbase=cn if kind in ['log','stripped_log','leaves','sapling'] else wood.removesuffix('木') if kind=='stick' else wood
            for prefix in ['block','item'] if kind not in SPRITES or kind in ['sapling','leaves'] else ['item']:
                labels['zh_cn'][f'{prefix}.xiantu.{bid(kind)}']=cnbase+suffix
                labels['en_us'][f'{prefix}.xiantu.{bid(kind)}']=f.replace('_',' ').title()+' '+kind.replace('_',' ').title()
        # Native chunk-generation pipeline: exactly one attempt per suitable chunk, internal exact probability.
        write_json(DATA/f'worldgen/configured_feature/{f}_tree.json',{'type':'xiantu:'+f+'_tree','config':{}})
        placement=[]
        placement += [{'type':'minecraft:in_square'},{'type':'minecraft:heightmap','heightmap':'WORLD_SURFACE_WG'},{'type':'minecraft:biome'}]
        write_json(DATA/f'worldgen/placed_feature/{f}_tree.json',{'feature':'xiantu:'+f+'_tree','placement':placement})
        write_json(DATA/f'forge/biome_modifier/{f}_tree.json',{'type':'forge:add_features','biomes':'#xiantu:spirit_wood/'+f,'features':['xiantu:'+f+'_tree'],'step':'vegetal_decoration'})
        habitats=[['#xiantu:spirit_wood/vanilla_forests'],['birch_forest','old_growth_birch_forest','meadow','#xiantu:spirit_wood/vanilla_forests'],['forest','meadow','windswept_forest','#xiantu:spirit_wood/vanilla_forests'],['taiga','snowy_taiga','old_growth_pine_taiga','windswept_hills','#xiantu:spirit_wood/vanilla_forests'],['jungle','sparse_jungle','dark_forest','#xiantu:spirit_wood/vanilla_forests'],['grove','old_growth_spruce_taiga','windswept_forest','#xiantu:spirit_wood/vanilla_forests'],['meadow','grove','windswept_hills','#xiantu:spirit_wood/vanilla_forests'],['jagged_peaks','frozen_peaks','stony_peaks','windswept_hills','#xiantu:spirit_wood/vanilla_forests']][TREES.index(tree)]
        write_json(DATA/f'tags/worldgen/biome/spirit_wood/{f}.json',{'replace':False,'values':[h if ':' in h else 'minecraft:'+h for h in habitats]})
    write_json(DATA/'tags/worldgen/biome/spirit_wood/vanilla_forests.json',{
        'replace':False,'values':['minecraft:forest','minecraft:flower_forest','minecraft:birch_forest',
                                  'minecraft:old_growth_birch_forest','minecraft:dark_forest']})
    for loc,value in labels.items():write_json(ASSETS/f'lang/{loc}.json',value)
    merge_tag('mineable/axe',[b for b in allblocks if not b.endswith(('_leaves','_sapling'))]);merge_tag('logs',logs);merge_tag('leaves',leaves);merge_tag('walls',walls);merge_tag('fences',fences);merge_tag('wooden_fences',fences);merge_tag('doors',doors);merge_tag('wooden_doors',doors);merge_tag('trapdoors',trapdoors);merge_tag('wooden_trapdoors',trapdoors)
    write_json(BASE/'resource-production.json',{'blocks':240,'material_items':24,'building_items':200,'runtime_textures':160,'blueprints':0,'phase':'resources produced; models and runtime QA pending'})
    print('Produced eight families:240 blocks,24 material items,200 building items; no wood blueprints.')
if __name__=='__main__':resources()
