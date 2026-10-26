"""Model phase after ALL images: native desktop MCP Java exports plus editable bbmodels."""
from build_paths import minecraft_reference
import json,zipfile
from spirit_wood_catalog import ROOT,TREES
from build_spirit_wood_resources import BASE,ASSETS,write_json
from blockbench_bridge import Bridge
from build_building_openings_blockbench import texture,cuboid,save_export,runtime_export
from build_building_beams import PARTS,CONNECTION_PARTS
from build_building_eaves import PARTS as EAVES
from build_building_signs_and_columns import TRIMS
MODELS=BASE/'models'
def author(bridge,name,boxes,materials):
    source=MODELS/f'{name}.bbmodel';target=ASSETS/f'models/block/{name}.json'
    if source.exists() and target.exists():return json.loads(target.read_text())
    bridge.call('create_project',name=name,format='java_block')
    for label,path,ref in materials:texture(bridge,label,path)
    for i,(lo,hi,label,uv) in enumerate(boxes):cuboid(bridge,f'part_{i}',lo,hi,label,uv)
    save_export(bridge,'project',source)
    model=runtime_export(bridge,MODELS/f'{name}.json',[m[2] for m in materials]);model['parent']='minecraft:block/block'
    write_json(target,model)
    print('EXPORTED',name,len(boxes),'cubes',flush=True);return model
def main():
    status=json.loads((BASE/'production-status.json').read_text(encoding='utf-8'))
    assert len(status)==16 and all(v['review']=='passed' for v in status.values()),'Image phase is unfinished'
    MODELS.mkdir(parents=True,exist_ok=True);bridge=Bridge()
    tools={t['name'] for t in bridge.request('tools/list',{})['tools']}
    bridge.call('get_capabilities',include_tools=True) if 'get_capabilities' in tools else bridge.call('get_project_info')
    assert any(c['id']=='java_block' and c['has_compile'] for c in bridge.value(bridge.call('list_export_formats'))['codecs'])
    if not (MODELS/'prior-active-project.bbmodel').exists():save_export(bridge,'project',MODELS/'prior-active-project.bbmodel')
    glass=MODELS/'vanilla-glass.png'
    with zipfile.ZipFile(minecraft_reference('client.jar')) as jar:glass.write_bytes(jar.read('assets/minecraft/textures/block/glass.png'))
    plans=[]
    for tree in TREES:
        f=tree[0]
        def material(label,suffix=None):
            suffix=suffix or label;return(label,ASSETS/f'textures/block/{f}_{suffix}.png',f'xiantu:block/{f}_{suffix}')
        shapes={'pillar':[([4,0,4],[12,16,12])],**TRIMS,**PARTS,**EAVES,'roof_ridge':[([0,0,0],[16,4,16]),([4,4,0],[12,8,16]),([6,8,0],[10,10,16])]}
        shapes.update({'beam_'+k:v for k,v in CONNECTION_PARTS.items()})
        for suffix,cubes in shapes.items():
            mats=[material('smooth'),material('carved')]
            if suffix=='pillar':mats=[material('grain','stripped_log_side')]
            if suffix.startswith('eaves') or suffix=='roof_ridge':mats=[material('roof')]
            boxes=[(lo,hi,mats[min(1,i%2)][0] if len(mats)>1 else mats[0][0],None) for i,(lo,hi) in enumerate(cubes)]
            model=author(bridge,f+'_'+suffix,boxes,mats);plans.append({'id':f+'_'+suffix,'cubes':len(cubes),'triangles':len(cubes)*12})
        # Two thin parts selected by native pane masks. The inventory model is a complete straight pane.
        post=[([7,0,7],[9,16,9],'carved',None)]
        arm=[([7,0,0],[9,1,7],'smooth',None),([7,15,0],[9,16,7],'smooth',None),([7,7.5,0],[9,8.5,7],'smooth',None),([7,1,3],[9,15,4],'smooth',None),([7.25,1,0],[8.75,15,7],'glass',None)]
        panel=post.copy()
        for side in ['left','right']:
            for lo,hi,label,uv in arm:
                x0,x1=(lo[2],hi[2]) if side=='left' else (16-hi[2],16-lo[2]);panel.append(([x0,lo[1],lo[0]],[x1,hi[1],hi[0]],label,uv))
        for suffix,boxes in [('_post',post),('_side',arm),('',panel)]:
            name=f+'_window'+suffix;model=author(bridge,name,boxes,[material('smooth'),material('carved'),('glass',glass,'minecraft:block/glass')]);model['render_type']='minecraft:cutout';write_json(ASSETS/f'models/block/{name}.json',model);plans.append({'id':name,'cubes':len(boxes),'triangles':len(boxes)*12})
        # Inventory uses the same proportionate upper/lower images as the placed native door.
        upperuv={face:([0,0,16,16] if face in ('north','south') else [0,0,3,16] if face in ('west','east') else [0,0,16,3]) for face in ['north','south','west','east','up','down']}
        name=f+'_door_item';model=author(bridge,name,[([0,0,0],[16,16,3],'bottom',None),([0,16,0],[16,32,3],'top',upperuv)],[material('bottom','door_bottom'),material('top','door_top')]);model['display']={'gui':{'rotation':[0,180,0],'translation':[0,-4,0],'scale':[.4,.4,.4]},'ground':{'translation':[0,2,0],'scale':[.2,.2,.2]},'thirdperson_righthand':{'rotation':[0,90,0],'translation':[0,2,0],'scale':[.3,.3,.3]},'firstperson_righthand':{'rotation':[0,90,0],'translation':[0,0,0],'scale':[.3,.3,.3]}};write_json(ASSETS/f'models/item/{f}_door.json',model);plans.append({'id':name,'cubes':2,'triangles':24})
    write_json(BASE/'models-production.json',{'models':plans,'authoring':'Blockbench desktop MCP native export_model','target':'Forge Java1.20.1 static baked models','texture_size':[64,64],'texels_per_unit':4,'animation':'native door states only; no new animations','performance':'max11 cubes/model; no BER or per-frame geometry'})
    print('Model phase complete:',len(plans),'exports',flush=True)
if __name__=='__main__':main()
