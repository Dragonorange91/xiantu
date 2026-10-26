"""Desktop Blockbench MCP authoring of small native Java thread-bound books."""
from pathlib import Path
import json, shutil, base64, time
from blockbench_bridge import Bridge
from build_building_openings_blockbench import texture,cuboid,save_export,runtime_export
from generate_gongfa_books import ROOT,OUT,catalog

ASSETS=ROOT/'src/main/resources/assets/xiantu'
FACES=('north','south','east','west','up','down')
DISPLAY={
 'gui':{'rotation':[18,165,-12],'translation':[0,0,0],'scale':[.92,.92,.92]},
 'ground':{'rotation':[0,0,0],'translation':[0,2,0],'scale':[.45,.45,.45]},
 'fixed':{'rotation':[0,180,0],'translation':[0,0,0],'scale':[.8,.8,.8]},
 'firstperson_righthand':{'rotation':[0,165,-12],'translation':[-2,3,0],'scale':[.6,.6,.6]},
 'firstperson_lefthand':{'rotation':[0,-165,12],'translation':[-2,3,0],'scale':[.6,.6,.6]},
 'thirdperson_righthand':{'rotation':[0,-90,0],'translation':[0,3,0],'scale':[.65,.65,.65]},
 'thirdperson_lefthand':{'rotation':[0,90,0],'translation':[0,3,0],'scale':[.65,.65,.65]}}

def write(path,data):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')

def main():
    entries=json.loads((OUT/'texture-manifest.json').read_text(encoding='utf-8'))
    assert len(entries)==24 and all(e['state']=='inspected_for_integration' for e in entries),'Inspect ALL cover textures before modeling'
    assert (OUT/'textures/book_pages.png').exists(),'Paper material must be generated and inspected first'
    models=OUT/'models';models.mkdir(exist_ok=True)
    backup=OUT/'previous-models';backup.mkdir(exist_ok=True)
    for e in entries:
        old=ASSETS/'models/item'/(e['id']+'.json')
        if not (backup/old.name).exists():shutil.copy2(old,backup/old.name)
    b=Bridge();schemas={t['name']:t for t in b.request('tools/list',{})['tools']}
    write(OUT/'blockbench-tools.json',schemas)
    original=b.value(b.call('get_project_info'));write(OUT/'original-project.json',original)
    if not (models/'preserved-original.bbmodel').exists():save_export(b,'project',models/'preserved-original.bbmodel')
    # Static 4-cube template, with no tiny stitch cubes or expensive custom renderer.
    parts=[('front_cover',[2.75,1,6.5],[13.25,15,6.9],'cover'),
           ('back_cover',[2.75,1,9.1],[13.25,15,9.5],'cloth'),
           ('ivory_pages',[3.5,1.25,6.9],[13,14.75,9.1],'paper'),
           ('bound_spine',[2.75,1,6.9],[3.5,15,9.1],'cloth')]
    records=[]
    shutil.copy2(OUT/'textures/book_pages.png',ASSETS/'textures/item/gongfa_book_pages.png')
    for e in entries:
        b.call('create_project',name='线装功法_'+e['name'],format='java_block')
        materials=['cover','cloth','paper']
        files=[OUT/'textures'/(e['id']+'_cover.png'),OUT/'textures'/(e['id']+'_cloth.png'),OUT/'textures/book_pages.png']
        for name,path in zip(materials,files):texture(b,name,path)
        ids={t['name']:t['uuid'] for t in b.value(b.call('list_textures'))}
        for name,start,end,mat in parts:
            cuboid(b,name,start,end,mat)
        # Native undo-aware per-face UV assignment; unavailable as a dedicated tool in this host.
        spec=[]
        for name,start,end,mat in parts:
            dx,dy,dz=[end[i]-start[i] for i in range(3)]
            uv={f:([0,0,dx,dy] if f in ('north','south') else [0,0,dz,dy] if f in ('east','west') else [0,0,dx,dz]) for f in FACES}
            if name=='front_cover':uv['north']=[0,0,16,16]
            if name=='ivory_pages':
                # Paper layers run across thickness Z; rotate the upright fore-edge island.
                uv['east']=[0,0,dy,dz]
                uv['west']=[0,0,dy,dz]
            spec.append(dict(name=name,uv=uv,tex=ids[mat]))
        code='(()=>{const spec='+json.dumps(spec)+';const cubes=spec.map(e=>Cube.all.find(c=>c.name===e.name));if(cubes.some(c=>!c))throw new Error("Missing book part");Undo.initEdit({elements:cubes});spec.forEach((e,i)=>{Object.entries(e.uv).forEach(([face,uv])=>{cubes[i].faces[face].uv=uv;cubes[i].faces[face].texture=e.tex;});});const front=cubes[0];["south","up","down","east","west"].forEach(f=>front.faces[f].texture='+json.dumps(ids['cloth'])+');cubes[2].faces.east.rotation=90;const display='+json.dumps(DISPLAY)+';Object.entries(display).forEach(([slot,value])=>{Project.display_settings[slot]=new DisplaySlot(slot,value);});Canvas.updateAllFaces();Undo.finishEdit("Map thread-bound book faces including exposed inside covers");return {updated:cubes.length};})()'
        result=b.value(b.call('risky_eval',code=code))
        assert isinstance(result,dict) and result.get('updated')==4,result
        save_export(b,'project',models/(e['id']+'.bbmodel'))
        refs=[f"xiantu:item/{e['id']}_cover",f"xiantu:item/{e['id']}_cloth",'xiantu:item/gongfa_book_pages']
        model=runtime_export(b,models/(e['id']+'.json'),refs)
        model['parent']='minecraft:block/block';model['gui_light']='front';model['display']=DISPLAY
        assert len(model['elements'])==4
        assert model['elements'][0]['faces']['north']['uv']==[0,0,16,16]
        faces=sum(len(x['faces']) for x in model['elements'])
        for suffix in ('cover','cloth'):shutil.copy2(OUT/'textures'/(e['id']+'_'+suffix+'.png'),ASSETS/'textures/item'/(e['id']+'_'+suffix+'.png'))
        write(models/(e['id']+'.json'),model);write(ASSETS/'models/item'/(e['id']+'.json'),model)
        records.append(dict(id=e['id'],name=e['name'],cubes=4,faces=faces,triangles=faces*2,model=str(models/(e['id']+'.bbmodel'))))
        print('EXPORTED',e['id'],'faces',faces,flush=True)
        if e['id'] in ('xuan_yuan_yang_qi_jue','jin_guang_xuan_gong','zi_fu_zhou_tian_jing','tai_qing_hun_yuan_jing','tai_shang_yan_dao_jing','tai_chu_dao_jing'):
            b.call('set_camera_angle',position=[-24,24,-48],target=[0,8,0],projection='perspective')
            time.sleep(.3)
            info=b.value(b.call('get_project_info'))
            assert info['project']['name']=='线装功法_'+e['name']
            capture=b.call('capture_screenshot',project=info['project']['uuid'])
            for c in capture.get('content',[]):
                if c.get('type')=='image':(models/(e['id']+'_preview.png')).write_bytes(base64.b64decode(c['data']))
    write(OUT/'model-manifest.json',records)
    print('Integrated24 native static books. Original project preserved; no player rules changed.',flush=True)

if __name__=='__main__':main()
