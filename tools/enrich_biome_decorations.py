"""Forge 1.20.1 habitat dressing. Does not rewrite climate, herb or ore rates."""
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
DATA=ROOT/'src/main/resources/data/xiantu'
RATES={'thunder_sedge':(5,1),'storm_bamboo':(12,3),
       'meteor_ash_grass':(14,2),'valley_jade_fern':(18,2)}
def read(path):return json.loads((DATA/path).read_text(encoding='utf-8'))
def write(path,data):
    p=DATA/path;p.parent.mkdir(parents=True,exist_ok=True)
    p.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def placement(name,biome,chance=1,count=None,step='vegetal_decoration'):
    modifiers=([{'type':'minecraft:count','count':count}] if count else
               [{'type':'minecraft:rarity_filter','chance':chance}])
    modifiers += [{'type':'minecraft:in_square'},
                  {'type':'minecraft:heightmap','heightmap':'MOTION_BLOCKING_NO_LEAVES'},
                  {'type':'minecraft:biome'}]
    write(f'worldgen/placed_feature/{name}.json',{'feature':f'xiantu:{name}','placement':modifiers})
    write(f'forge/biome_modifier/{name}.json',{'type':'forge:add_features',
        'biomes':f'#xiantu:terrain/{biome}','features':[f'xiantu:{name}'],'step':step})
def patch(state,tries):
    return {'type':'minecraft:random_patch','config':{'tries':tries,'xz_spread':5,'y_spread':0,
        'feature':{'feature':{'type':'xiantu:habitat_simple_block','config':{'to_place':{
            'type':'minecraft:simple_state_provider','state':{'Name':state}}}},'placement':[
            {'type':'minecraft:heightmap','heightmap':'MOTION_BLOCKING_NO_LEAVES'},
            {'type':'minecraft:block_predicate_filter','predicate':{'type':'minecraft:matching_blocks','blocks':['minecraft:air']}},
            {'type':'minecraft:block_predicate_filter','predicate':{'type':'minecraft:would_survive','state':{'Name':state}}}]}}}
def main():
    for plant,(tries,chance) in RATES.items():
        name=plant+'_patch';p=f'worldgen/configured_feature/{name}.json';data=read(p)
        if plant!='storm_bamboo':
            data=patch('xiantu:'+plant,tries)
        write(p,data)
        placed=read(f'worldgen/placed_feature/{name}.json')
        placed['placement'][0]={'type':'minecraft:rarity_filter','chance':chance}
        write(f'worldgen/placed_feature/{name}.json',placed)
    for name,biome,chance in [('thunder_rock_outcrop','thunder_wastes',5),
                              ('meteor_rock_outcrop','meteor_wasteland',3),
                              ('medicine_plain_landmark','spirit_medicine_plain',5)]:
        write(f'worldgen/configured_feature/{name}.json',{'type':'xiantu:habitat_landmark','config':{}})
        placement(name,biome,chance,step='surface_structures')
    for name,state,tries,chance,count in [('medicine_meadow_grass','minecraft:grass',32,1,2),
        ('medicine_meadow_azure','minecraft:azure_bluet',14,3,None),
        ('medicine_meadow_daisy','minecraft:oxeye_daisy',12,4,None),
        ('medicine_meadow_tallgrass','minecraft:tall_grass',12,3,None)]:
        write(f'worldgen/configured_feature/{name}.json',patch(state,tries))
        placement(name,'spirit_medicine_plain',chance,count)
    tree=read('worldgen/placed_feature/lightning_scarred_tree.json')
    tree['placement'][0]['chance']=4
    write('worldgen/placed_feature/lightning_scarred_tree.json',tree)
    # Existing impact-glass fragments now follow each local slope too.
    glass=read('worldgen/configured_feature/impact_glass_patch.json')
    filters=glass['config']['feature']['placement']
    filters[:]=[f for f in filters if f['type'] not in ('minecraft:heightmap','minecraft:biome')]
    filters[:0]=[{'type':'minecraft:heightmap','heightmap':'MOTION_BLOCKING_NO_LEAVES'}]
    glass['config']['feature']['feature']['type']='xiantu:habitat_simple_block'
    write('worldgen/configured_feature/impact_glass_patch.json',glass)
    # More visual seams without increasing meteorite resource yield.
    fissure=read('worldgen/placed_feature/meteor_fissure_patch.json');fissure['placement'][0]['chance']=3
    write('worldgen/placed_feature/meteor_fissure_patch.json',fissure)
    print('Enriched three habitats; climate/medical herb/meteorite/ore rates untouched')
if __name__=='__main__':main()
