from build_paths import minecraft_reference
import json
import zipfile
from pathlib import Path

from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "src/main/resources"
ASSETS = RES / "assets/xiantu"
DATA = RES / "data/xiantu"
BLOCKS = {
    "town_red_wood": "朱漆木料",
    "town_gray_brick": "灰纹青砖",
    "town_cyan_roof": "青灰瓦",
    "town_carved_panel": "鎏金雕花门板",
}


def read_json(path: Path):
    return json.loads(path.read_text(encoding="utf-8"))


def main() -> None:
    zh = read_json(ASSETS / "lang/zh_cn.json")
    en = read_json(ASSETS / "lang/en_us.json")
    pickaxe = read_json(RES / "data/minecraft/tags/blocks/mineable/pickaxe.json")["values"]
    axe = read_json(RES / "data/minecraft/tags/blocks/mineable/axe.json")["values"]
    checks = 0
    for block, name in BLOCKS.items():
        with Image.open(ASSETS / f"textures/block/{block}.png") as image:
            assert image.size == (64, 64) and image.mode == "RGBA", block
        state = read_json(ASSETS / f"blockstates/{block}.json")
        assert state == {"variants": {"": {"model": f"xiantu:block/{block}"}}}
        model = read_json(ASSETS / f"models/block/{block}.json")
        assert model == {
            "parent": "minecraft:block/cube_all",
            "textures": {"all": f"xiantu:block/{block}"},
        }
        assert read_json(ASSETS / f"models/item/{block}.json") == {
            "parent": f"xiantu:block/{block}"
        }
        loot = read_json(DATA / f"loot_tables/blocks/{block}.json")["pools"][0]
        assert loot["entries"][0]["name"] == f"xiantu:{block}"
        assert loot["conditions"] == [{"condition": "minecraft:survives_explosion"}]
        recipe = read_json(DATA / f"recipes/{block}.json")
        assert recipe["type"] == "minecraft:crafting_shaped"
        assert recipe["result"]["item"] == f"xiantu:{block}"
        assert f"block.xiantu.{block}" in zh and zh[f"block.xiantu.{block}"] == name
        assert f"item.xiantu.{block}" in zh and zh[f"item.xiantu.{block}"] == name
        assert f"block.xiantu.{block}" in en and f"item.xiantu.{block}" in en
        checks += 9
    for block in ("town_gray_brick", "town_cyan_roof"):
        assert f"xiantu:{block}" in pickaxe
        checks += 1
    for block in ("town_red_wood", "town_carved_panel"):
        assert f"xiantu:{block}" in axe
        checks += 1
    # Traverse actual blockstate -> models -> parents -> textures. This catches
    # missing stair corners and broken inventory JSON, not just the cube texture.
    vanilla = minecraft_reference('client.jar')
    with zipfile.ZipFile(vanilla) as jar:
        cache={}
        def model_tree(id):
            if ':' not in id:id='minecraft:'+id
            if id in cache:return cache[id]
            namespace,path=id.split(':',1)
            file=f'assets/{namespace}/models/{path}.json'
            model=read_json(RES/file) if namespace=='xiantu' else json.loads(jar.read(file))
            parent=model_tree(model['parent']) if 'parent' in model else {}
            merged={**parent,**model,'textures':{**parent.get('textures',{}),**model.get('textures',{})}}
            cache[id]=merged
            return merged
        def verify_model(id):
            merged=model_tree(id)
            for texture in merged['textures'].values():
                seen=set()
                while texture.startswith('#'):
                    assert texture not in seen,(id,'texture cycle')
                    seen.add(texture);texture=merged['textures'][texture[1:]]
                ns,name=texture.split(':',1) if ':' in texture else ('minecraft',texture)
                if ns=='xiantu':assert (RES/f'assets/{ns}/textures/{name}.png').is_file(),(id,texture)
            for element in merged.get('elements',[]):
                for face in element['faces'].values():
                    assert face.get('texture')!='#missing',id
                    assert face['texture'][1:] in merged['textures'],(id,face)
        ids=['town_cyan_roof','town_cyan_roof_stairs','town_cyan_roof_tiles','town_cyan_eaves','town_cyan_eaves_corner','town_cyan_roof_ridge']
        plaques=['wanbaoge_plaque','forging_shop_plaque','alchemy_pavilion_plaque','talisman_shop_plaque']
        for id in ids+plaques:
            verify_model(f'xiantu:item/{id}')
            state=read_json(ASSETS/f'blockstates/{id}.json')
            for entry in state['variants'].values():
                for choice in entry if isinstance(entry,list) else [entry]:verify_model(choice['model'])
            checks+=1
        assert len(read_json(ASSETS/'blockstates/town_cyan_roof_stairs.json')['variants'])==80
        for id in plaques:
            state=read_json(ASSETS/f'blockstates/{id}.json')['variants']
            assert len(state)==96,id
            for part in range(24):
                with Image.open(ASSETS/f'textures/block/{id}_part_{part}.png') as im:
                    assert im.size==(64,64) and im.getchannel('A').getextrema()==(255,255),id
            assert read_json(DATA/f'loot_tables/blocks/{id}.json')['pools'][0]['conditions'][1]['block']==f'xiantu:{id}'
            assert zh[f'block.xiantu.{id}'].endswith('牌匾')
            checks+=27
        checks+=len(cache)
    print(f"Town building material resource checks passed: {checks}")


if __name__ == "__main__":
    main()
