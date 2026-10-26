"""Check generated resources against vanilla 1.20.1 reference behavior."""
from build_paths import minecraft_reference
import json
import zipfile
from pathlib import Path
from PIL import Image
from generate_building_material_resources import ASSETS, DATA, FAMILIES, ROOT
from build_building_signs_and_columns import PLAQUES, TRIMS, plaque_recipe
from build_building_beams import PARTS, CONNECTION_PARTS, JOIN_MODELS, beam_blockstate, recipe as beam_recipe
from build_building_eaves import PARTS as EAVES_PARTS, recipe as eaves_recipe
from build_building_steles import section_boxes


def main():
    vanilla = minecraft_reference('client.jar')
    checks = 0
    with zipfile.ZipFile(vanilla) as jar:
        stairs = json.loads(jar.read("assets/minecraft/blockstates/stone_brick_stairs.json"))["variants"]
        for family in FAMILIES:
            name = family + "_stairs"
            actual = json.loads((ASSETS / f"blockstates/{name}.json").read_text())["variants"]
            for state, expected in stairs.items():
                for waterlogged in ("false", "true"):
                    candidate = actual[state + ",waterlogged=" + waterlogged]
                    for axis in ("x", "y"):
                        assert candidate.get(axis, 0) == expected.get(axis, 0), (name, state, axis)
                    suffix = expected["model"].removeprefix("minecraft:block/stone_brick_stairs")
                    assert candidate["model"] == "xiantu:block/" + name + suffix
                    assert candidate.get("uvlock", False) == expected.get("uvlock", False), (name, state, "uvlock")
                    checks += 1
            # Vanilla loot functions are evaluated in order: double count, then explosion decay.
            loot = json.loads((DATA / f"loot_tables/blocks/{family}_slab.json").read_text())
            entry = loot["pools"][0]["entries"][0]
            functions = entry["functions"]
            assert functions[0]["count"] == 2
            assert functions[0]["conditions"][0]["properties"] == {"type": "double"}
            assert functions[1]["function"] == "minecraft:explosion_decay"
            checks += 1
            # Compare railing connection rendering directly to vanilla nether-brick fences.
            actual = json.loads((ASSETS / f"blockstates/{family}_railing.json").read_text())["multipart"]
            reference = json.loads(jar.read("assets/minecraft/blockstates/nether_brick_fence.json"))["multipart"]
            for entry, expected in zip(actual, reference):
                assert entry.get("when") == expected.get("when")
                for key in ("x", "y", "uvlock"):
                    assert entry["apply"].get(key, False if key == "uvlock" else 0) == expected["apply"].get(key, False if key == "uvlock" else 0)
                checks += 1
            assert len(actual) == len(reference)
            for suffix in ("smooth", "carved"):
                texture = ASSETS / f"textures/block/{family}_{suffix}.png"
                with Image.open(texture) as image:
                    assert image.size == (64, 64) and image.mode == "RGBA"
                    assert image.getchannel("A").getextrema() == (255, 255)
                model = json.loads((ASSETS / f"models/block/{family}_{suffix}.json").read_text())
                assert model["textures"]["all"] == f"xiantu:block/{family}_{suffix}"
                checks += 1
        # Opening orientation and hinge models must track the native Java door/trapdoor tables.
        for family in FAMILIES:
            for suffix, vanilla_name in (("door", "oak_door"), ("trapdoor", "oak_trapdoor")):
                path = ASSETS / f"blockstates/{family}_{suffix}.json"
                if not path.exists():
                    raise AssertionError(f"Missing registered opening resources: {path}")
                actual = json.loads(path.read_text())["variants"]
                expected = json.loads(jar.read(f"assets/minecraft/blockstates/{vanilla_name}.json"))["variants"]
                translated = json.loads(json.dumps(expected).replace(f"minecraft:block/{vanilla_name}", f"xiantu:block/{family}_{suffix}"))
                assert actual == translated
                checks += len(expected)
                for entry in actual.values():
                    model_path = ASSETS / (entry["model"].replace("xiantu:", "models/") + ".json")
                    model = json.loads(model_path.read_text())
                    assert model["render_type"] == "minecraft:cutout"
                    for texture in model["textures"].values():
                        if texture.startswith("xiantu:"):
                            assert (ASSETS / (texture.replace("xiantu:", "textures/") + ".png")).exists()
            for suffix in ("door_top", "door_bottom", "trapdoor"):
                with Image.open(ASSETS / f"textures/block/{family}_{suffix}.png") as image:
                    assert image.size == (64,64) and image.mode == "RGBA"
                    assert image.getchannel("A").getextrema() == (255,255)
            door_pool = json.loads((DATA / f"loot_tables/blocks/{family}_door.json").read_text())["pools"][0]
            assert any(c.get("properties") == {"half":"lower"} for c in door_pool["conditions"])
            window = json.loads((ASSETS / f"models/block/{family}_window.json").read_text())
            assert len(window["elements"]) == 11 and window["render_type"] == "minecraft:cutout"
            assert "minecraft:block/glass" in window["textures"].values()
            state = json.loads((ASSETS / f"blockstates/{family}_window.json").read_text())["multipart"]
            assert len(state) == 5 and 'when' not in state[0]
            for entry, direction, angle in zip(state[1:], ('north','east','south','west'), (0,90,180,270)):
                assert entry['when'] == {direction:'true'}
                assert entry['apply']['y'] == angle and entry['apply']['uvlock']
                checks += 1
            for suffix, count in (('_post',1), ('_side',5), ('',11)):
                part = json.loads((ASSETS / f"models/block/{family}_window{suffix}.json").read_text())
                assert len(part['elements']) == count
                for element in part['elements']:
                    assert all(0 <= lo < hi <= 16 for lo,hi in zip(element['from'],element['to']))
                    thickness_axis = 2 if suffix == '' else 0
                    assert element['to'][thickness_axis] - element['from'][thickness_axis] <= 2
                    for face in element['faces'].values():
                        assert face['texture'].removeprefix('#') in part['textures']
                        assert all(0 <= uv <= 16 for uv in face['uv'])
                    checks += 1
            checks += 5
        for family in FAMILIES:
            with Image.open(ASSETS / f'textures/block/{family}_roof.png') as image:
                assert image.size == (64,64) and image.mode == 'RGBA'
                assert image.getchannel('A').getextrema() == (255,255)
            actual=json.loads((ASSETS / f'blockstates/{family}_roof_stairs.json').read_text())['variants']
            for state,expected in stairs.items():
                for water in ('false','true'):
                    value=actual[state+',waterlogged='+water]
                    assert value.get('x',0)==expected.get('x',0) and value.get('y',0)==expected.get('y',0)
                    assert value.get('uvlock',False)==expected.get('uvlock',False)
                    checks += 1
            slab=json.loads((DATA / f'loot_tables/blocks/{family}_roof_tiles.json').read_text())
            assert slab['pools'][0]['entries'][0]['functions'][0]['conditions'][0]['properties']=={'type':'double'}
            ridge=json.loads((ASSETS / f'models/block/{family}_roof_ridge.json').read_text())
            assert len(ridge['elements'])==3
            assert [(e['from'],e['to']) for e in ridge['elements']]==[
                ([0,0,0],[16,4,16]),([0,4,4],[16,8,12]),([0,8,6],[16,10,10])]
            for element in ridge['elements']:
                for face in element['faces'].values():
                    assert face['texture'].removeprefix('#') in ridge['textures']
                    assert all(0<=uv<=16 for uv in face['uv'])
            checks += 3
    for suffix, tag in (("wall", "walls"), ("stairs", "stairs"), ("slab", "slabs"), ("railing", "fences")):
        values = json.loads((ROOT / f"src/main/resources/data/minecraft/tags/blocks/{tag}.json").read_text())["values"]
        assert all(f"xiantu:{family}_{suffix}" in values for family in FAMILIES)
        checks += 1
    mineable = json.loads((ROOT / "src/main/resources/data/minecraft/tags/blocks/mineable/pickaxe.json").read_text())["values"]
    for name in ("fan_ling_shi", "ling_jing", "ling_yu", "xuan_jing", "di_sui", "tian_jing", "xian_yu", "dao_shi"):
        assert f"xiantu:{name}_ore" in mineable
        checks += 1
    with Image.open(ASSETS / 'textures/block/stone_lamp_core.png') as image:
        assert image.size == (64,64) and image.mode == 'RGBA'
        assert image.getchannel('A').getextrema() == (255,255)
    checks += 1
    for family in FAMILIES:
        for suffix, count in (('lamp_stand',3),('stone_lamp',11)):
            name=family+'_'+suffix
            state=json.loads((ASSETS/f'blockstates/{name}.json').read_text())
            assert state == {'variants':{'':{'model':'xiantu:block/'+name}}}
            model=json.loads((ASSETS/f'models/block/{name}.json').read_text())
            assert len(model['elements']) == count and model['parent']=='minecraft:block/block'
            assert 'format_version' not in model
            for resource in model['textures'].values():
                assert (ASSETS/(resource.replace('xiantu:','textures/')+'.png')).exists()
            for element in model['elements']:
                assert all(0<=a<b<=16 for a,b in zip(element['from'],element['to']))
                assert set(element['faces']) == {'north','south','east','west','up','down'}, (name, element['name'], 'open face')
                for face in element['faces'].values():
                    assert face['texture'].removeprefix('#') in model['textures']
                    assert all(0<=uv<=16 for uv in face['uv'])
                checks += 1
            if suffix == 'lamp_stand':
                cap=next(e for e in model['elements'] if e['name']=='offering_plate')
                assert cap['from']==[2,14,2] and cap['to']==[14,16,14], (name,'cap must meet full lamp footprint at y16')
                stem=next(e for e in model['elements'] if e['name']=='carved_stem')
                assert stem['from'][1]==3 and stem['to'][1]==cap['from'][1]
            else:
                foot=next(e for e in model['elements'] if e['name']=='plinth')
                assert foot['from']==[2,0,2] and foot['to']==[14,3,14], (name,'lamp foot must meet pedestal cap')
            checks += 1
            item=json.loads((ASSETS/f'models/item/{name}.json').read_text())
            assert item == {'parent':'xiantu:block/'+name}
            loot=json.loads((DATA/f'loot_tables/blocks/{name}.json').read_text())
            assert loot['pools'][0]['entries'][0]['name']=='xiantu:'+name
            assert loot['pools'][0]['conditions']==[{'condition':'minecraft:survives_explosion'}]
            assert 'xiantu:'+name in mineable
            checks += 5
        recipe=json.loads((DATA/f'recipes/{family}_stone_lamp.json').read_text())
        assert recipe['type']=='minecraft:crafting_shapeless'
        assert sorted(i['item'] for i in recipe['ingredients'])==sorted(['minecraft:torch','xiantu:'+family+'_lamp_stand'])
        assert recipe['result']=={'item':'xiantu:'+family+'_stone_lamp','count':1}
        checks += 1
    for name in ('wooden_plaque',):
        model = json.loads((ASSETS / f'models/block/{name}.json').read_text())
        assert len(model['elements']) == 1 and model['parent'] == 'minecraft:block/block'
        assert 'format_version' not in model
        for element in model['elements']:
            assert all(0 <= a < b <= 16 for a,b in zip(element['from'], element['to']))
            assert set(element['faces']) == {'north','south','east','west','up','down'}
            for face in element['faces'].values():
                assert face['texture'].removeprefix('#') in model['textures']
                assert all(0 <= uv <= 16 for uv in face['uv'])
            checks += 1
        state = json.loads((ASSETS / f'blockstates/{name}.json').read_text())['variants']
        assert state == {f'facing={facing},part={part}': {'model': f'xiantu:block/{name}_part_{part}', 'y': angle, 'uvlock': False}
                         for part in range(8) for facing, angle in [('north',0),('east',90),('south',180),('west',270)]}
        for part in range(8):
            section = json.loads((ASSETS / f'models/block/{name}_part_{part}.json').read_text())
            element = section['elements'][0]
            assert element['from'] == [0,0,13] and element['to'] == [16,16,16]
            col, row = part % 4, part // 4
            assert element['faces']['north']['uv'] == [16,0,0,16]
            assert section['textures']['0'] == f'xiantu:block/{name}_part_{part}'
            with Image.open(ASSETS / f'textures/block/{name}_part_{part}.png') as image:
                assert image.size == (64,64) and image.mode == 'RGBA'
            checks += 1
        assert {'condition':'minecraft:block_state_property','block':'xiantu:'+name,'properties':{'part':'0'}} in json.loads((DATA / f'loot_tables/blocks/{name}.json').read_text())['pools'][0]['conditions']
        assert json.loads((ASSETS / f'models/item/{name}.json').read_text()) == {'parent': 'xiantu:block/' + name}
        assert json.loads((DATA / f'loot_tables/blocks/{name}.json').read_text())['pools'][0]['entries'][0]['name'] == 'xiantu:' + name
        axe_tag = json.loads((ROOT/'src/main/resources/data/minecraft/tags/blocks/mineable/axe.json').read_text())['values']
        assert 'xiantu:' + name in axe_tag
        checks += 4
    # A normalized shaped recipe signature also accounts for vanilla mirrored matching.
    # Distinct construction shapes must not be shadowed by another building recipe.
    signatures = {}
    basic_plaque_recipe = json.loads((DATA/'recipes/wooden_plaque.json').read_text())
    assert basic_plaque_recipe['result'] == {'item':'xiantu:wooden_plaque','count':2}
    assert basic_plaque_recipe['key'] == {'B':{'item':'minecraft:stripped_dark_oak_log'}}
    checks += 1
    for family in FAMILIES:
        name = family + '_broken_pillar'
        model = json.loads((ASSETS / f'models/block/{name}.json').read_text())
        assert len(model['elements']) == 22 and model['parent'] == 'minecraft:block/block'
        assert model['textures']['0'] == 'xiantu:block/' + family + '_carved'
        for element in model['elements']:
            assert all(0 <= a < b <= 16 for a,b in zip(element['from'],element['to']))
            assert set(element['faces']) == {'north','south','east','west','up','down'}
        state = json.loads((ASSETS / f'blockstates/{name}.json').read_text())['variants']
        assert state == {f'facing={facing}':{'model':'xiantu:block/'+name,'y':angle}
                         for facing,angle in [('north',0),('east',90),('south',180),('west',270)]}
        recipe = json.loads((DATA / f'recipes/{name}.json').read_text())
        assert recipe['pattern'] == ['P','P'] and recipe['result'] == {'item':'xiantu:'+name,'count':2}
        assert recipe['key'] == {'P':{'item':'xiantu:'+family+'_pillar'}}
        assert 'xiantu:'+name in mineable
        assert json.loads((DATA / f'loot_tables/blocks/{name}.json').read_text())['pools'][0]['entries'][0]['name'] == 'xiantu:'+name
        checks += 6
    for family in FAMILIES:
        name = family + '_rubble'
        model = json.loads((ASSETS / f'models/block/{name}.json').read_text())
        assert len(model['elements']) == 5 and model['textures']['0'] == 'xiantu:block/' + family + '_smooth'
        assert max(e['to'][1] for e in model['elements']) == 7
        for element in model['elements']:
            assert all(0 <= a < b <= 16 for a,b in zip(element['from'], element['to']))
            for face in element['faces'].values():
                assert face['texture'].removeprefix('#') in model['textures']
                assert all(0 <= uv <= 16 for uv in face['uv'])
        assert json.loads((ASSETS / f'blockstates/{name}.json').read_text())['variants'] == {
            f'facing={facing}':{'model':'xiantu:block/'+name,'y':angle}
            for facing,angle in [('north',0),('east',90),('south',180),('west',270)]}
        recipe = json.loads((DATA / f'recipes/{name}.json').read_text())
        assert recipe == {'type':'minecraft:crafting_shapeless', 'ingredients':[{'item':'xiantu:'+family+'_broken_pillar'}],
                          'result':{'item':'xiantu:'+name,'count':1}}
        assert 'xiantu:'+name in mineable
        assert json.loads((DATA / f'loot_tables/blocks/{name}.json').read_text())['pools'][0]['entries'][0]['name'] == 'xiantu:'+name
        checks += 6
    for name,(width,height,zh,en,pattern) in PLAQUES.items():
        state = json.loads((ASSETS / f'blockstates/{name}.json').read_text())['variants']
        assert len(state)==width*height*4
        for part in range(width*height):
            part_name = f'{name}_part_{part}'
            model = json.loads((ASSETS / f'models/block/{part_name}.json').read_text())
            assert len(model['elements'])==1 and model['elements'][0]['from']==[0,0,13] and model['elements'][0]['to']==[16,16,16]
            assert model['elements'][0]['faces']['north']['uv']==[16,0,0,16]
            assert model['textures']['0']=='xiantu:block/'+part_name
            with Image.open(ASSETS / f'textures/block/{part_name}.png') as image:
                assert image.size==(64,64) and image.mode=='RGBA'
            for face,angle in [('north',0),('east',90),('south',180),('west',270)]:
                assert state[f'facing={face},part={part}']=={'model':'xiantu:block/'+part_name,'y':angle,'uvlock':False}
            checks+=1
        loot=json.loads((DATA/f'loot_tables/blocks/{name}.json').read_text())
        assert {'condition':'minecraft:block_state_property','block':'xiantu:'+name,'properties':{'part':'0'}} in loot['pools'][0]['conditions']
        recipe=json.loads((DATA/f'recipes/{name}.json').read_text())
        assert recipe == plaque_recipe(name)
        assert recipe['result']=={'item':'xiantu:'+name,'count':1}
        assert json.loads((ASSETS/f'models/item/{name}.json').read_text())=={'parent':'xiantu:block/'+name}
        assert 'xiantu:'+name in axe_tag
        checks+=4
    for family in FAMILIES:
        shaft=json.loads((ASSETS/f'models/block/{family}_pillar.json').read_text())
        for ref in shaft['textures'].values():
            namespace,path=ref.split(':')
            assert namespace=='xiantu' and (ASSETS/f'textures/{path}.png').exists(),(family,'unresolved pillar texture',ref)
        assert [(e['from'],e['to']) for e in shaft['elements']]==[([4,0,4],[12,16,12])],(family,'shaft must be half a block wide')
        states=json.loads((ASSETS/f'blockstates/{family}_pillar.json').read_text())['variants']
        assert states=={'axis=x':{'model':f'xiantu:block/{family}_pillar','x':90,'y':90},
                        'axis=y':{'model':f'xiantu:block/{family}_pillar'},
                        'axis=z':{'model':f'xiantu:block/{family}_pillar','x':90}}
        for element in shaft['elements']:
            assert set(element['faces'])=={'north','south','east','west','up','down'}
            assert all(face['texture'].removeprefix('#') in shaft['textures'] for face in element['faces'].values())
        checks+=3
        for suffix,boxes in TRIMS.items():
            name=family+'_'+suffix
            model=json.loads((ASSETS/f'models/block/{name}.json').read_text())
            assert len(model['elements'])==3
            for element,(start,end) in zip(model['elements'],boxes):
                assert element['from']==start and element['to']==end
                for face in element['faces'].values():
                    assert face['texture'].removeprefix('#') in model['textures']
                    assert all(0<=value<=16 for value in face['uv'])
            assert model['elements'][0]['from'][1]==0 and model['elements'][-1]['to'][1]==16
            assert json.loads((ASSETS/f'blockstates/{name}.json').read_text())['variants']=={
                f'facing={face}':{'model':'xiantu:block/'+name,'y':angle}
                for face,angle in [('north',0),('east',90),('south',180),('west',270)]}
            recipe=json.loads((DATA/f'recipes/{name}.json').read_text())
            assert recipe['result']=={'item':'xiantu:'+name,'count':2}
            assert recipe['ingredients']==[{'item':'xiantu:'+family+'_pillar'},
                                          {'item':'xiantu:'+family+('_smooth' if suffix=='pillar_base' else '_carved')}]
            assert 'xiantu:'+name in mineable
            assert json.loads((DATA/f'loot_tables/blocks/{name}.json').read_text())['pools'][0]['entries'][0]['name']=='xiantu:'+name
            checks+=5
    for family in FAMILIES:
        for suffix, boxes in (PARTS | EAVES_PARTS).items():
            name = family+'_'+suffix
            model = json.loads((ASSETS/f'models/block/{name}.json').read_text())
            assert 'format_version' not in model and model['parent']=='minecraft:block/block'
            assert [(e['from'],e['to']) for e in model['elements']]==boxes
            for element in model['elements']:
                assert set(element['faces'])=={'north','south','east','west','up','down'}
                for face_name, face in element['faces'].items():
                    assert face['texture'].removeprefix('#') in model['textures']
                    assert all(0<=uv<=16 for uv in face['uv'])
                    spans=[b-a for a,b in zip(element['from'],element['to'])]
                    axes=(0,1) if face_name in ('north','south') else (2,1) if face_name in ('east','west') else (0,2)
                    u,v,uu,vv=face['uv']
                    assert (abs(uu-u),abs(vv-v))==tuple(spans[a] for a in axes),(name,face_name,'UV density')
                checks+=1
            expected_states=beam_blockstate(family)['variants'] if suffix=='beam' else {
                f'facing={f}':{'model':'xiantu:block/'+name,'y':a}
                for f,a in [('north',0),('east',90),('south',180),('west',270)]}
            assert json.loads((ASSETS/f'blockstates/{name}.json').read_text())['variants']==expected_states
            assert json.loads((ASSETS/f'models/item/{name}.json').read_text())=={'parent':'xiantu:block/'+name}
            expected_recipe=eaves_recipe(family,suffix) if suffix in EAVES_PARTS else beam_recipe(family,suffix)
            assert json.loads((DATA/f'recipes/{name}.json').read_text())==expected_recipe
            assert json.loads((DATA/f'loot_tables/blocks/{name}.json').read_text())['pools'][0]['entries'][0]['name']=='xiantu:'+name
            assert 'xiantu:'+name in mineable
            checks+=6
        # Check the complete exported support chain, rather than only bracket/beam.
        masks={'straight_x':10,'straight_z':5,'corner_ne':3,'corner_se':6,'corner_sw':12,'corner_nw':9,
               'tee_nes':7,'tee_esw':14,'tee_swn':13,'tee_wne':11,'cross':15}
        for kind,boxes in CONNECTION_PARTS.items():
            name=f'{family}_beam_{kind}'
            model=json.loads((ASSETS/f'models/block/{name}.json').read_text())
            assert [(e['from'],e['to']) for e in model['elements']]==boxes
            for e in model['elements']:
                assert set(e['faces'])=={'north','south','east','west','up','down'}
                for face_name,face in e['faces'].items():
                    ref=model['textures'][face['texture'].removeprefix('#')]
                    assert ref.startswith('xiantu:block/') and (ASSETS/f'textures/{ref.split(":")[1]}.png').exists()
                    spans=[b-a for a,b in zip(e['from'],e['to'])]
                    axes=(0,1) if face_name in ('north','south') else (2,1) if face_name in ('east','west') else (0,2)
                    u,v,uu,vv=face['uv']
                    assert (abs(uu-u),abs(vv-v))==tuple(spans[a] for a in axes),(name,'stretched UV',face_name)
                checks+=1
        for join,(suffix,angle) in JOIN_MODELS.items():
            model=json.loads((ASSETS/f'models/block/{family}_beam{suffix}.json').read_text())
            occupied=set()
            for e in model['elements']:
                for x in range(e['from'][0],e['to'][0]):
                    for y in range(e['from'][1],e['to'][1]):
                        for z in range(e['from'][2],e['to'][2]):
                            point=(x,y,z)
                            for _ in range(angle//90): point=(15-point[2],point[1],point[0])
                            assert point not in occupied,(family,join,'overlapping coplanar geometry')
                            occupied.add(point)
            expected=set()
            for y in range(16):
                lo,hi=(4,12) if y<6 else (3,13) if y<8 else (5,11)
                for x in range(16):
                    for z in range(16):
                        mask=masks[join]
                        if (lo<=x<hi and lo<=z<hi) or (mask&1 and lo<=x<hi and z<lo) or (mask&2 and x>=hi and lo<=z<hi) or (mask&4 and lo<=x<hi and z>=hi) or (mask&8 and x<lo and lo<=z<hi):
                            expected.add((x,y,z))
            assert occupied==expected,(family,join,'connection geometry mismatch or corner gap')
            checks+=1
        chain=['pillar_base','pillar','pillar_capital','bracket','beam','roof_tiles']
        elements=[json.loads((ASSETS/f'models/block/{family}_{s}.json').read_text())['elements']
                  for s in chain[:-1]]
        # roof_tiles inherits the native bottom slab model.
        roof=json.loads((ASSETS/f'models/block/{family}_roof_tiles.json').read_text())
        assert roof['parent']=='minecraft:block/slab'
        elements.append([{'from':[0,0,0],'to':[16,8,16]}])
        for a,b,lower,upper in zip(chain,chain[1:],elements,elements[1:]):
            assert any(l['to'][1]==16 and u['from'][1]==0
                       and min(l['to'][0],u['to'][0])>max(l['from'][0],u['from'][0])
                       and min(l['to'][2],u['to'][2])>max(l['from'][2],u['from'][2])
                       for l in lower for u in upper),(family,a,b,'floating support')
            checks+=1
    for family in FAMILIES:
        name = family + '_stele'
        with Image.open(ROOT/f'asset_sources/building_materials/master/{family}_stele_inscription_1024.png') as master:
            assert master.size==(1024,1024)
            front=master.convert('RGBA').resize((128,192),Image.Resampling.LANCZOS)
        for part in range(6):
            part_name=f'{name}_part_{part}'
            model=json.loads((ASSETS/f'models/block/{part_name}.json').read_text())
            assert [(e['from'],e['to']) for e in model['elements']]==[(a,b) for a,b,_ in section_boxes(part)]
            # North is viewed right-to-left in world X; inscriptions must stay readable
            # and continuous across the two columns and all three height sections.
            for element, (lo,hi,source) in zip(model['elements'],section_boxes(part)):
                if source==0:
                    continue
                north=element['faces']['north']
                assert model['textures'][north['texture'].removeprefix('#')]==f'xiantu:block/{family}_stele_front_{part}'
                assert north['uv']==[16-hi[0],16-hi[1],16-lo[0],16-lo[1]], (part,north)
                with Image.open(ASSETS/f'textures/block/{family}_stele_front_{part}.png') as tile:
                    assert tile.size==(64,64)
                    row=part//2
                    source_y=max(192-(row+1)*64,0)
                    pad=max((row+1)*64-192,0)
                    actual=tile.convert('RGBA').crop((0,pad,64,64))
                    expected=front.crop((part%2*64,source_y,part%2*64+64,192-row*64))
                    assert actual.tobytes()==expected.tobytes(), (family,part,'inscription section continuity')
                    assert tile.convert('RGBA').getextrema()[3]==(255,255)
                checks+=1
            for element in model['elements']:
                assert len(element['faces'])==6
                for face_name,face in element['faces'].items():
                    texture=model['textures'][face['texture'].removeprefix('#')]
                    assert texture.startswith('xiantu:block/') and (ASSETS/f'textures/{texture.split(":")[1]}.png').exists()
                    spans=[b-a for a,b in zip(element['from'],element['to'])]
                    axes=(0,1) if face_name in ('north','south') else (2,1) if face_name in ('east','west') else (0,2)
                    u,v,uu,vv=face['uv']
                    assert (abs(uu-u),abs(vv-v))==tuple(spans[a] for a in axes)
                    checks+=1
            assert (ROOT/f'asset_sources/building_materials/models/{part_name}.bbmodel').exists()
        state=json.loads((ASSETS/f'blockstates/{name}.json').read_text())['variants']
        assert len(state)==24
        assert all(state[f'facing={f},part={p}']['y']==a for f,a in [('north',0),('east',90),('south',180),('west',270)] for p in range(6))
        loot=json.loads((DATA/f'loot_tables/blocks/{name}.json').read_text())
        assert any(c.get('properties')=={'part':'0'} for c in loot['pools'][0]['conditions'])
        checks+=1
        for path in DATA.glob(f"recipes/{family}*.json"):
            value = json.loads(path.read_text())
            if value["type"] != "minecraft:crafting_shaped":
                continue
            pattern = value["pattern"]
            cells = tuple(tuple(json.dumps(value["key"].get(char, {}),sort_keys=True) for char in row) for row in pattern)
            mirror = tuple(tuple(reversed(row)) for row in cells)
            signature = min(cells, mirror)
            assert signature not in signatures, (path, signatures.get(signature))
            signatures[signature] = path
            checks += 1
    from verify_building_cracked import verify
    checks += verify()
    from verify_building_mossy import verify as verify_mossy
    checks += verify_mossy()
    from verify_building_finished_steps import verify as verify_finished_steps
    checks += verify_finished_steps()
    from verify_jade_table import verify as verify_jade_table
    checks += verify_jade_table()
    from verify_stone_chest import verify as verify_stone_chest
    checks += verify_stone_chest()
    from verify_jade_cabinet import verify as verify_jade_cabinet
    checks += verify_jade_cabinet()
    from verify_stone_water_channel import verify as verify_water_channel
    checks += verify_water_channel()
    print(f"Building material resource regression: {checks} cases passed.")


if __name__ == "__main__":
    main()
