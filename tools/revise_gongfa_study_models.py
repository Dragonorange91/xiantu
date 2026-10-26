"""Repair exposed closed-book faces and author native animated book parts through desktop MCP."""
import json
from blockbench_bridge import Bridge
from build_building_openings_blockbench import texture, cuboid, save_export, runtime_export
from build_gongfa_book_models import ASSETS, DISPLAY, write
from generate_gongfa_books import OUT, catalog

def main():
    b=Bridge()
    schemas={t['name']:t for t in b.request('tools/list',{})['tools']}
    assert all(t in schemas for t in ('risky_eval','create_animation','export_model'))
    projects=b.value(b.call('risky_eval',code='ModelProject.all.map(p=>({name:p.name,uuid:p.uuid}))'))
    for entry in catalog():
        found=[p for p in projects if p['name']=='线装功法_'+entry['name']]
        assert len(found)==1,(entry,found)
        code='(()=>{ModelProject.all.find(p=>p.uuid==='+json.dumps(found[0]['uuid'])+').select();const cs=Cube.all;const ts=Object.fromEntries(Texture.all.map(t=>[t.name,t.uuid]));Undo.initEdit({elements:cs});cs.forEach(c=>{for(const f of Object.keys(c.faces)){if(c.faces[f].texture===null)c.faces[f].texture=ts[c.name==="ivory_pages"?"paper":"cloth"];}});Canvas.updateAllFaces();Undo.finishEdit("Restore exposed inner book faces");return {cubes:cs.length,faces:cs.reduce((n,c)=>n+Object.values(c.faces).filter(f=>f.texture!==null).length,0)};})()'
        result=b.value(b.call('risky_eval',code=code));assert result['faces']==24,result
        save_export(b,'project',OUT/'models'/(entry['id']+'.bbmodel'))
        refs=[f"xiantu:item/{entry['id']}_cover",f"xiantu:item/{entry['id']}_cloth",'xiantu:item/gongfa_book_pages']
        model=runtime_export(b,OUT/'models'/(entry['id']+'.json'),refs)
        model.update(parent='minecraft:block/block',gui_light='front',display=DISPLAY)
        write(OUT/'models'/(entry['id']+'.json'),model);write(ASSETS/'models/item'/(entry['id']+'.json'),model)
        print('CLOSED_FACE_FIX',entry['id'],flush=True)
    parts=[('left_cover',[-2.5,1,8.3],[8,15,8.6],'cloth','left'),
           ('left_pages',[-2.2,1.2,7.95],[8,14.8,8.3],'paper','left'),
           ('right_cover',[8,1,8.3],[18.5,15,8.6],'cover','right'),
           ('right_pages',[8,1.2,7.95],[18.2,14.8,8.3],'paper','right'),
           ('turning_leaf',[8,1.2,7.85],[18.2,14.8,7.94],'paper','leaf')]
    dest=OUT/'study';dest.mkdir(exist_ok=True)
    for fmt in ('java_block','free'):
        b.call('create_project',name='功法研习_'+fmt,format=fmt)
        first=catalog()[0]
        for mat,file in [('cover',first['id']+'_cover.png'),('cloth',first['id']+'_cloth.png'),('paper','book_pages.png'),('reading','book_reading_paper.png')]:texture(b,mat,OUT/'textures'/file)
        for name,start,end,mat,group in parts:
            dx,dy,dz=[end[i]-start[i] for i in range(3)]
            cuboid(b,name,start,end,mat,{f:([0,0,16,16] if f in ('north','south') else [0,0,dz,dy] if f in ('east','west') else [0,0,dx,dz]) for f in ('north','south','east','west','up','down')})
        spec=[dict(name=n,group=g) for n,_,_,_,g in parts]
        code='(()=>{const cs=Cube.all;Undo.initEdit({elements:cs,outliner:true});const gs={};["left","right","leaf"].forEach(n=>gs[n]=new Group({name:n,origin:[8,8,8]}).init());'+json.dumps(spec)+'.forEach(s=>Cube.all.find(c=>c.name===s.name).addTo(gs[s.group]));const ts=Object.fromEntries(Texture.all.map(t=>[t.name,t.uuid]));Cube.all.filter(c=>c.name.endsWith("cover")).forEach(c=>{c.faces.north.texture=ts.cloth;["up","down","east","west"].forEach(f=>c.faces[f].texture=ts.cloth)});Cube.all.filter(c=>c.name.endsWith("pages")||c.name==="turning_leaf").forEach(c=>["north","south"].forEach(f=>c.faces[f].texture=ts.reading));Canvas.updateAll();Undo.finishEdit("Bind spine hinges and written pages");return {groups:Group.all.length,cubes:Cube.all.length};})()'
        result=b.value(b.call('risky_eval',code=code));assert result==dict(groups=3,cubes=5),result
        if fmt=='java_block':
            full=runtime_export(b,dest/'parts.json',[f"xiantu:item/{first['id']}_cover",f"xiantu:item/{first['id']}_cloth",'xiantu:item/gongfa_book_pages','xiantu:item/gongfa_reading_paper'])
            save_export(b,'project',dest/'parts.bbmodel')
            for group,indices in [('left',[0,1]),('right',[2,3]),('leaf',[4])]:
                m=dict(full);m['elements']=[full['elements'][i] for i in indices];m.update(parent='minecraft:block/block',ambientocclusion=False)
                write(ASSETS/'models/item/gongfa_study'/('template_'+group+'.json'),m)
            for e in catalog():
                for group in ('left','right'):
                    m={'parent':'xiantu:item/gongfa_study/template_'+group,'textures':{'0':f"xiantu:item/{e['id']}_cover",'1':f"xiantu:item/{e['id']}_cloth"}}
                    write(ASSETS/'models/item/gongfa_study'/(e['id']+'_'+group+'.json'),m)
        else:
            b.call('create_animation',name='open',animation_length=.5,loop=False,bones={
                'left':[{'time':0,'rotation':[0,-85,0]},{'time':.5,'rotation':[0,-12,0]}],
                'right':[{'time':0,'rotation':[0,85,0]},{'time':.5,'rotation':[0,12,0]}]})
            b.call('create_animation',name='page_turn',animation_length=1.2,loop=True,bones={
                'leaf':[{'time':0,'rotation':[0,12,0]},{'time':.6,'rotation':[0,90,0]},{'time':1.2,'rotation':[0,168,0]}]})
            result=b.value(b.call('risky_eval',code='(()=>{const a=Animation.all.find(a=>a.name.endsWith("page_turn"));if(!a)throw new Error("Missing animation");a.select();return {selected:a.uuid};})()'))
            assert result.get('selected')
            save_export(b,'project',dest/'gongfa_study_animated.bbmodel')
            b.call('animation_timeline',action='set_time',time=.6)
            b.call('set_camera_angle',position=[-18,20,-42],target=[8,8,8],projection='perspective')
            import base64,time
            time.sleep(.5)
            for c in b.call('capture_screenshot').get('content',[]):
                if c.get('type')=='image':(dest/'animated_preview.png').write_bytes(base64.b64decode(c['data']))
    write(dest/'production.json',{'closed_books':24,'closed_faces_each':24,'animated_cubes':5,'native_parts':3,'variant_models':48,'open_seconds':.5,'page_turn_seconds':1.2,'runtime':'native baked part transforms; fonts rendered by Minecraft; no GLB/GeckoLib'})
    print('Authored closed fixes and native open/turn rigs',flush=True)

if __name__=='__main__':main()
