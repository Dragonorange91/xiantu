from pathlib import Path
import json,shutil
r=Path('src/main/resources/assets/xiantu');w=Path('work/qa/realm-renewal-20261023');calls=[]
for id in ['realm_spirit_lantern','realm_incense_reliquary','realm_carved_pillar']:
 src=Path('asset_sources/realm-renewal-20261023')/(id+'.json');m=json.loads(src.read_text(encoding='utf8'));m.pop('format_version',None);m['ambientocclusion']=True;m['textures']['particle']='xiantu:block/black_rock_smooth';m['display']={'gui':{'rotation':[30,225,0],'translation':[0,0,0],'scale':[.7,.7,.7]},'ground':{'translation':[0,3,0],'scale':[.4,.4,.4]}}
 if id=='realm_spirit_lantern':
  # Forge 1.20.1 reads forge_data on individual BlockElements. Only the lamp core glows.
  m['textures']['3']='xiantu:block/stone_lamp_core'
  core=next(e for e in m['elements'] if e['name']=='light_core')
  for face in core['faces'].values():face['texture']='#3'
  core['shade']=False;core['forge_data']={'block_light':15,'sky_light':15,'ambient_occlusion':False}
 (r/f'models/block/{id}.json').write_text(json.dumps(m,indent=2));(r/f'models/item/{id}.json').write_text(json.dumps({'parent':'xiantu:block/'+id}));(r/f'blockstates/{id}.json').write_text(json.dumps({'variants':{'':{'model':'xiantu:block/'+id}}}))
 calls.extend([{'name':'load_project','arguments':{'path':str(src.with_suffix('.bbmodel').resolve())},'save':id+'-load'},{'name':'set_camera_angle','arguments':{'position':[33,27,-33],'target':[8,8,8]},'save':id+'-camera'},{'name':'screenshot','arguments':{'width':768,'height':768,'annotate':False},'save':id+'-framed'}])
(r/'models/item/realm_test_jade.json').write_text(json.dumps({'parent':'minecraft:item/generated','textures':{'layer0':'minecraft:item/ender_eye'}}))
(w/'frame.json').write_text(json.dumps(calls))
shutil.copy2('work/qa/instance-adventure-20261023/helper.py',w/'helper.py')
