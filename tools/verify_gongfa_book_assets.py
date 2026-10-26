"""Validate book geometry/texture contracts in source or a deployed archive."""
from pathlib import Path
import argparse, io, json, zipfile
from PIL import Image
from generate_gongfa_books import ROOT,OUT,catalog

def verify(jar=None):
    z=zipfile.ZipFile(jar) if jar else None
    def read(name):return z.read(name) if z else (ROOT/'src/main/resources'/name).read_bytes()
    resource_names=set()
    rows=[]
    for e in catalog():
        name='assets/xiantu/models/item/'+e['id']+'.json';data=read(name);model=json.loads(data)
        resource_names.add(name)
        assert len(model['elements'])==4,(e['id'],'geometry')
        assert model['gui_light']=='front' and 'gui' in model['display']
        assert 'loader' not in model,'Native static books only'
        for p in model['elements']:
            assert set(p['faces'])=={'north','south','east','west','up','down'}, (e['id'],'exposed inner faces missing')
            assert all(0<=x<=16 for x in p['from']+p['to'])
            for f in p['faces'].values():
                ref=f['texture'].removeprefix('#');assert ref in model['textures']
                assert all(0<=x<=16 for x in f['uv'])
        assert model['elements'][0]['faces']['north']['texture']=='#0'
        assert model['elements'][0]['faces']['north']['uv']==[0,0,16,16]
        assert model['textures']['0']==f"xiantu:item/{e['id']}_cover",'No shared/recolored cover fallback'
        for ref in model['textures'].values():
            ns,path=ref.split(':');tex=f'assets/{ns}/textures/{path}.png';resource_names.add(tex)
            with Image.open(io.BytesIO(read(tex))) as image:
                assert image.size==(64,64),(tex,image.size)
                assert image.convert('RGBA').getextrema()[3]==(255,255),'Opaque book cloth/pages'
        rows.append(dict(id=e['id'],name=e['name'],faces=sum(len(p['faces']) for p in model['elements'])))
        for side in ('left','right'):
            name='assets/xiantu/models/item/gongfa_study/'+e['id']+'_'+side+'.json'
            resource_names.add(name);part=json.loads(read(name))
            assert part['parent']=='xiantu:item/gongfa_study/template_'+side
            assert part['textures']['0']==f"xiantu:item/{e['id']}_cover"
    for side,count in [('left',2),('right',2),('leaf',1)]:
        name='assets/xiantu/models/item/gongfa_study/template_'+side+'.json'
        resource_names.add(name);part=json.loads(read(name))
        assert len(part['elements'])==count
        assert part['textures']['3']=='xiantu:item/gongfa_reading_paper'
        for ref in part['textures'].values():
            ns,path=ref.split(':');tex=f'assets/{ns}/textures/{path}.png';resource_names.add(tex)
            with Image.open(io.BytesIO(read(tex))) as image:
                assert image.size==(64,64),(tex,image.size)
                assert image.convert('RGBA').getextrema()[3]==(255,255)
        for element in part['elements']:
            assert 'rotation' not in element,('direct animated page expects spine transforms, not rotated cuboids',side)
            assert len(element['faces'])==6,('animated backface',side)
            for face in element['faces'].values():
                assert face.get('rotation',0)==0,('direct reading UV rotation',side)
                assert face['texture'].removeprefix('#') in part['textures'],('resolved reading texture',side)
                assert len(face['uv'])==4 and all(0<=v<=16 for v in face['uv'])
            if element.get('name','').endswith('pages') or element.get('name')=='turning_leaf':
                assert all(element['faces'][f]['texture']=='#3' for f in ('north','south'))
                assert all(element['faces'][f]['texture']=='#2' for f in ('up','down','east','west'))
    if z:
        for name in resource_names:
            assert read(name)==(ROOT/'src/main/resources'/name).read_bytes(),('Deployed resource mismatch',name)
        assert not any('gongfa-books-20261011/masters' in n for n in z.namelist())
        z.close()
    result=dict(books=len(rows),resources=len(resource_names),models=75,textures=50,
                cubes_per_book=4,faces=sorted(set(e['faces'] for e in rows)),jar=str(jar) if jar else None,
                deployed_byte_comparison=bool(jar),rows=rows)
    (OUT/'resource-validation.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8')
    print(json.dumps({k:v for k,v in result.items() if k!='rows'},ensure_ascii=False))
    return result

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--jar');a=p.parse_args();verify(a.jar)
