"""Validate packaged equipment contracts, not gameplay balance or visual approval."""
from pathlib import Path
import collections
import hashlib
import json
import math
import base64
import io
from PIL import Image
from held_item_edge_contract import verify_icon

def load(p):
    return json.loads(p.read_text(encoding='utf8'))

def sha(p):
    return hashlib.sha256(p.read_bytes()).hexdigest()

def winding(q, normal):
    a=[q[1][i]-q[0][i] for i in range(3)]
    b=[q[2][i]-q[0][i] for i in range(3)]
    c=[a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]]
    assert sum(c[i]*normal[i] for i in range(3))>1e-13, 'degenerate or inverted face'

def armor(path):
    data=load(path);counts=collections.Counter()
    assert data['schema']=='xiantu.native-armor-quads/1'
    for element in data['cubes']:
        assert 'mannequin' not in element['name']
        allowed={'head':{'head'},'chest':{'body','left_arm','right_arm'},'legs':{'left_leg','right_leg','body'},'feet':{'left_leg','right_leg'}}
        assert element['part'] in allowed[element['slot']]
        a=element['vertices'];assert a and len(a)%32==0
        count=0
        for j in range(0,len(a),32):
            q=[a[j+i*8:j+(i+1)*8] for i in range(4)]
            for v in q:
                assert all(math.isfinite(x) for x in v)
                assert 0<=v[3]<=1 and 0<=v[4]<=1
                assert abs(sum(x*x for x in v[5:])-1)<1e-4
            winding(q,q[0][5:])
            count+=1 if q[2]==q[3] else 2
        assert count==element['triangles']
        counts[element['slot']]+=count
    assert set(counts)=={'head','chest','legs','feet'}
    return dict(counts)

def obj(path):
    vertices=[];uv=[];normals=[];count=0;material=None
    for line in path.read_text(encoding='utf8').splitlines():
        p=line.split()
        if not p:continue
        if p[0] in ['v','vt','vn']:
            v=[float(x) for x in p[1:]];assert all(math.isfinite(x) for x in v)
            if p[0]=='v':assert len(v)==3;vertices.append(v)
            elif p[0]=='vt':assert len(v)==2 and all(0<=x<=1 for x in v);uv.append(v)
            else:assert len(v)==3 and abs(sum(x*x for x in v)-1)<1e-4;normals.append(v)
        elif p[0]=='f':
            refs=[[int(v)-1 for v in s.split('/')] for s in p[1:]]
            assert len(refs) in (3,4)
            for a,b,c in refs:assert 0<=a<len(vertices) and 0<=b<len(uv) and 0<=c<len(normals)
            winding([vertices[i[0]] for i in refs],normals[refs[0][2]])
            count+=len(refs)-2
        elif p[0]=='mtllib':
            assert len(p)==2 and Path(p[1]).name==p[1]
            material=path.parent/p[1]
    assert count and material and material.exists()
    return {'weapon':count},material

def verify(project):
    r=project/'src/main/resources/assets/xiantu'
    source=project/'asset_sources/equipment-full-20261021'
    manifest=load(source/'integration-manifest.json');rows=manifest['items']
    expected={f'preview_equipment_{n:03}' for n in [*range(2,31),*range(35,89)]}
    assert len(rows)==83 and {e['registryId'] for e in rows}==expected
    code=(project/'src/main/java/com/longju/xiantu/equipment/EquipmentModelBatch.java').read_text(encoding='utf8')
    assert code.count('ModItems.ITEMS.register(')==83
    for file,needle in [('ModItems.java','EquipmentModelBatch.bootstrap()'),('ModCreativeTabs.java','EquipmentModelBatch.items()')]:
        assert needle in (project/'src/main/java/com/longju/xiantu/registry'/file).read_text(encoding='utf8')
    sprites={x.get('sprite') for x in load(r.parent/'minecraft/atlases/blocks.json')['sources']}
    language=load(r/'lang/zh_cn.json');checked={}
    for row in rows:
        identity=row['registryId'];mat=row['material'];folder=source/row['source']
        assert f'"{identity}"' in code and f'item.xiantu.{identity}' in language
        state=load(folder/'status.json');assert state['status']=='reviewed'
        assert sha(folder/'native-faces.json')==row['sourceNativeSha256']==state['reviewedNativeSha256']
        assert sha(folder/'model.bbmodel')==state['reviewedProjectSha256']
        embedded_data=load(folder/'model.bbmodel')['textures'][0]['source'].split(',',1)[1]
        embedded=Image.open(io.BytesIO(base64.b64decode(embedded_data))).convert('RGBA')
        tex=r/f'textures/models/equipment/{mat}.png'
        assert sha(tex)==state['atlasSha256']==sha(source/f'materials/{mat}/atlas.png')
        assert f'xiantu:models/equipment/{mat}' in sprites
        with Image.open(tex) as im:
            assert im.mode=='RGBA' and im.size==(256,256) and im.getchannel('A').getextrema()==(255,255)
            assert embedded.size==im.size and embedded.tobytes()==im.tobytes(),'Native screenshot source has stale texture pixels'
        icon=r/f'textures/item/equipment_preview/{identity}.png'
        assert row['iconSha256']==state['reviewedIconSha256'][row['capture']]==sha(folder/(row['capture']+'.png'))
        verify_icon(icon,row['iconSha256'],project)
        with Image.open(icon) as im:
            assert im.mode=='RGBA' and im.size==(128,128)
            a=im.getchannel('A');box=a.getbbox();assert box and a.getextrema()==(0,255)
            assert min(box[0],box[1],128-box[2],128-box[3])>=4
        item=load(r/f'models/item/{identity}.json')
        if row['category']=='armor':
            model=row['armorModel']
            if model not in checked:checked[model]=armor(r/f'models/armor/{model}.json')
            assert checked[model]==row['triangles']
            assert sha(tex)==sha(r/f'textures/models/armor/batch_{mat}.png')
            assert item['textures']['layer0']==f'xiantu:item/equipment_preview/{identity}'
        else:
            assert item['loader']=='forge:separate_transforms'
            assert item['perspectives']['gui']['textures']['layer0']==f'xiantu:item/equipment_preview/{identity}'
            assert item['base']['parent']==f'xiantu:item/{identity}_held'
            held=load(r/f'models/item/{identity}_held.json');assert held['loader']=='forge:obj' and held['flip_v'] is False
            assert held['model']==f'xiantu:models/item/equipment/{row["modelId"]}.obj'
            counts,mtl=obj(r/held['model'].split(':')[1]);assert counts==row['triangles']
            assert f'map_Kd xiantu:models/equipment/{mat}' in mtl.read_text(encoding='utf8')
            assert {'firstperson_righthand','thirdperson_righthand'}<=held['display'].keys()
    print('PASS:83 registrations/resources, native counts/winding, screenshot provenance, atlas mappings and armor slots')

if __name__=='__main__':
    verify(Path(__file__).resolve().parents[1])
