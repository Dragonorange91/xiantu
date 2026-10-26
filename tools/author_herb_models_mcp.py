from pathlib import Path
import json,base64,subprocess
from integrate_large_workstations import ROOT,OUT
RUNNER=ROOT/'asset_sources/workstations-large-20261020/mcp-runner.mjs'
OUT.mkdir(exist_ok=True);(OUT/'editable').mkdir(exist_ok=True);(OUT/'exports').mkdir(exist_ok=True)
calls=[]
for crop in [False,True]:
 name='herb_crop_cross' if crop else 'herb_wild_cross';base=-1 if crop else 0
 calls.append({'name':'new_project','arguments':{'format':'java_block','name':name,'texture_width':64,'texture_height':64}})
 calls.append({'name':'create_texture','arguments':{'name':'herb_0058_stage4','width':64,'height':64,'data_url':'data:image/png;base64,'+base64.b64encode((OUT/'art/runtime/herb_0058_stage4.png').read_bytes()).decode()}})
 calls.append({'name':'execute_script','arguments':{'params':{'base':base},'code':'''const texture=Texture.all[0];
const elements=[];Undo.initEdit({elements,selection:true});
for(const angle of [45,-45]) {
 const cube=new Cube({name:'cross_'+angle,from:[.8,params.base,8],to:[15.2,params.base+16,8],origin:[8,params.base+8,8],rotation:[0,angle,0],rescale:true,shade:false,box_uv:false});
 for(const [face,value] of Object.entries(cube.faces)){value.texture=['north','south'].includes(face)?texture.uuid:null;value.uv=[0,0,64,64];}
 cube.init();elements.push(cube);
}
Project.ambientocclusion=false;Undo.finishEdit('Native herb planes');Canvas.updateAll();
return {planes:elements.length,visible_faces:4,triangles:8,baseline:params.base};'''}})
 calls.extend([{'name':'save_project','arguments':{'path':str(OUT/f'editable/{name}.bbmodel')}},{'name':'export_model','arguments':{'codec':'java_block','path':str(OUT/f'exports/{name}.json')}},{'name':'set_camera_angle','arguments':{'position':[27,18,-40],'target':[0,7,0],'angle':'ortho'}},{'name':'execute_script','arguments':{'code':'const p=Preview.selected;p.camera.zoom=.23;p.camera.updateProjectionMatrix();p.render();return true;'}},{'name':'screenshot','arguments':{'annotate':False},'save':'../workstations-herbs-integration-20261020/'+name+'-preview'}])
p=OUT/'mcp-calls.json';p.write_text(json.dumps(calls,ensure_ascii=False),encoding='utf-8')
subprocess.run(['C:/Program Files/nodejs/node.exe',str(RUNNER),str(p)],check=True)
