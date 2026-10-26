"""Native MCP edit of the existing study rig's reading faces, without rebuilding covers."""
import json
from blockbench_bridge import Bridge
from build_building_openings_blockbench import texture, save_export, runtime_export
from build_gongfa_book_models import ASSETS, write
from generate_gongfa_books import OUT, catalog

def main():
    b=Bridge()
    schemas={t['name']:t for t in b.request('tools/list',{})['tools']}
    assert all(t in schemas for t in ('risky_eval','create_texture','export_model'))
    projects=b.value(b.call('risky_eval',code='ModelProject.all.filter(p=>p.name.startsWith("功法研习_")).map(p=>({name:p.name,uuid:p.uuid}))'))
    print(json.dumps(projects,ensure_ascii=False),flush=True)
    first=catalog()[0];dest=OUT/'study'
    for fmt,filename in [('java_block','parts.bbmodel'),('free','gongfa_study_animated.bbmodel')]:
        expected=json.loads((dest/filename).read_text(encoding='utf8'))
        expected_ids=sorted(e['uuid'] for e in expected['elements'])
        candidates=[p for p in projects if p['name']=='功法研习_'+fmt]
        found=[]
        for p in candidates:
            r=b.value(b.call('risky_eval',code='(()=>{ModelProject.all.find(p=>p.uuid==='+json.dumps(p['uuid'])+').select();return Cube.all.map(c=>c.uuid).sort();})()'))
            if r==expected_ids: found.append(p)
        assert len(found)==1,found
        r=b.value(b.call('risky_eval',code='(()=>{ModelProject.all.find(p=>p.uuid==='+json.dumps(found[0]['uuid'])+').select();return {name:Project.name,cubes:Cube.all.length,textures:Texture.all.map(t=>t.name)};})()'))
        assert r['cubes']==5,r
        previous=dest/('before-written-'+filename)
        if not previous.exists(): save_export(b,'project',previous)
        if 'reading' not in r['textures']: texture(b,'reading',OUT/'textures/book_reading_paper.png')
        code='''(()=>{const cs=Cube.all.filter(c=>c.name.endsWith("pages")||c.name==="turning_leaf");const t=Texture.all.find(t=>t.name==="reading");if(!t)throw new Error("No written paper");Undo.initEdit({elements:cs});cs.forEach(c=>["north","south"].forEach(f=>{c.faces[f].texture=t.uuid;c.faces[f].uv=[0,0,16,16];c.faces[f].rotation=0;}));Canvas.updateAllFaces();Undo.finishEdit("White written reading pages");return {updated:cs.length,faces:cs.map(c=>({name:c.name,north:c.faces.north.texture,south:c.faces.south.texture}))};})()'''
        r=b.value(b.call('risky_eval',code=code));assert r['updated']==3,r
        print(json.dumps(r,ensure_ascii=False),flush=True)
        save_export(b,'project',dest/filename)
        if fmt=='java_block':
            full=runtime_export(b,dest/'parts.json',[f"xiantu:item/{first['id']}_cover",f"xiantu:item/{first['id']}_cloth",'xiantu:item/gongfa_book_pages','xiantu:item/gongfa_reading_paper'])
            for group,indices in [('left',[0,1]),('right',[2,3]),('leaf',[4])]:
                m=dict(full);m['elements']=[full['elements'][i] for i in indices];m.update(parent='minecraft:block/block',ambientocclusion=False)
                write(ASSETS/'models/item/gongfa_study'/('template_'+group+'.json'),m)
    meta=json.loads((dest/'reading_paper_production.json').read_text(encoding='utf8'))
    meta.update(visual_review='64px opaque white page and twelve large printed glyphs inspected',reading_faces=6,edge_material_preserved=True,closed_book_models_changed=False)
    write(dest/'reading_paper_production.json',meta)
    print('Updated written reading faces via desktop Blockbench MCP',flush=True)

if __name__=='__main__': main()
