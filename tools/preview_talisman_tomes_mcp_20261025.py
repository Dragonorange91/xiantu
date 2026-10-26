"""Inspect existing four-cuboid books with the native Blockbench bridge; preserve runtime geometry."""
from pathlib import Path
import json,base64,time
from blockbench_native_bridge import NativeBridge
R=Path(__file__).resolve().parents[1];A=R/'src/main/resources/assets/xiantu';O=R/'asset_sources/talisman-tomes-20261025/models';O.mkdir(parents=True,exist_ok=True)
b=NativeBridge();status=b.call('get_status')
if status.get('has_project') and not (O/'previous-project.bbmodel').exists():b.call('export_model',codec='project',path=str(O/'previous-project.bbmodel'))
for ident in ['xuanmen_initial','xuanmen_true','xuanmen_secret']:
 m=json.loads((A/'models/item'/(ident+'.json')).read_text(encoding='utf-8'))
 b.call('new_project',format='java_block',name=ident,texture_width=16,texture_height=16)
 textures={}
 for key in ['0','1','2']:
  ref=m['textures'][key];raw=(A/'textures'/(ref[7:]+'.png')).read_bytes()
  textures[key]=b.call('create_texture',name=ref.split('/')[-1],width=64,height=64,data_url='data:image/png;base64,'+base64.b64encode(raw).decode())['uuid']
 cubes=[]
 for e in m['elements']:
  c=json.loads(json.dumps(e));c['box_uv']=False;c['autouv']=0
  for f in c['faces'].values():f['texture']=textures[f['texture'].removeprefix('#')]
  cubes.append(c)
 assert b.call('add_cubes',cubes=cubes)['created']==4
 b.call('export_model',codec='project',path=str(O/(ident+'.bbmodel')))
 # Preserve the approved native item display in the editable saved project too.
 p=O/(ident+'.bbmodel');j=json.loads(p.read_text(encoding='utf-8'));j['display']=m['display'];p.write_text(json.dumps(j,ensure_ascii=False,indent=2),encoding='utf-8')
 b.call('set_camera_angle',position=[-24,24,-48],target=[0,8,0]);time.sleep(.5)
 assert b.call('get_status')['project']['name']==ident
 shot=b.call('screenshot',width=700,height=700,annotate=False)
 (O/(ident+'-preview.png')).write_bytes(base64.b64decode(shot['data_url'].split(',',1)[1]))
 print('CAPTURED',ident,flush=True)
