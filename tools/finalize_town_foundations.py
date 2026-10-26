"""Repair roof resources and install the geometry actually exported by Blockbench."""
import json
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
ASSET=ROOT/'src/main/resources/assets/xiantu'
OUT=ROOT/'asset_sources/town/shop-plaques-20261019'
STEMS=['wanbaoge','forging_shop','alchemy_pavilion','talisman_shop']

def dump(path,value):
    path.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n',encoding='utf8')

def main():
    for stem in STEMS:
        id=stem+'_plaque'
        for p in [None,*range(24)]:
            name=id if p is None else f'{id}_part_{p}'
            model=json.loads((OUT/f'{name}.json').read_text(encoding='utf8'))
            assert all(face.get('texture')!='#missing' for cube in model['elements'] for face in cube['faces'].values()),name
            # Java 1.20.1 uses the legacy cube model format, not 26.x metadata.
            model.pop('format_version',None)
            model['parent']='minecraft:block/block'
            keys={face['texture'][1:] for cube in model['elements'] for face in cube['faces'].values()}
            model['textures']={k:f'xiantu:block/{name}' for k in keys}
            model['textures']['particle']=f'xiantu:block/{name}'
            if p is None:
                model['display']={
                    'gui':{'rotation':[0,180,0],'translation':[0,0,0],'scale':[0.9,0.9,0.9]},
                    'fixed':{'rotation':[0,180,0],'translation':[0,0,-6],'scale':[1,1,1]}}
            dump(ASSET/f'models/block/{name}.json',model)
    for suffix in ['','_inner','_outer']:
        dump(ASSET/f'models/block/town_cyan_roof_stairs{suffix}.json',{
            'parent':'minecraft:block/'+{'':'stairs','_inner':'inner_stairs','_outer':'outer_stairs'}[suffix],
            'textures':{k:'xiantu:block/town_cyan_roof' for k in ['top','bottom','side']}})
    for name in ['town_cyan_roof','town_cyan_roof_stairs','town_cyan_roof_tiles','town_cyan_eaves','town_cyan_eaves_corner','town_cyan_roof_ridge']:
        dump(ASSET/f'models/item/{name}.json',{'parent':f'xiantu:block/{name}'})
    print('Installed 100 MCP-exported plaque models; repaired six roof item models and all stair shapes.')

if __name__=='__main__':main()
