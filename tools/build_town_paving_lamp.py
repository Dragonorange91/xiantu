"""Author a flush stone lantern through the active Blockbench MCP bridge (8787).
Reuses Mojang textures by resource reference; no generated/repainted texture assets.
"""
import base64, json, zipfile
from pathlib import Path
import requests

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'asset_sources/town/paving-lamp-20261019'
OUT.mkdir(parents=True,exist_ok=True)

def call(action, **params):
    r=requests.post('http://127.0.0.1:8787/command',json={'action':action,'params':params},timeout=30)
    r.raise_for_status();v=r.json()
    if not v.get('ok'):raise RuntimeError(v)
    return v['result']

status=call('get_status')
if status.get('has_project') and not (OUT/'previous-project.bbmodel').exists():
    call('export_model',codec='project',path=str(OUT/'previous-project.bbmodel'))
formats=call('list_formats');assert any(f['id']=='java_block' for f in formats)
call('new_project',format='java_block',name='town_paving_lamp',texture_width=16,texture_height=16)
refs=['polished_andesite','ochre_froglight_top','cut_copper'];textures=[]
with zipfile.ZipFile(r'C:\gradle-user\caches\forge_gradle\minecraft_repo\versions\1.20.1\client.jar') as jar:
    for name in refs:
        data=jar.read('assets/minecraft/textures/block/'+name+'.png')
        textures.append(call('create_texture',name=name,width=16,height=16,data_url='data:image/png;base64,'+base64.b64encode(data).decode())['uuid'])

specs=[('stone_base',[0,0,0],[16,14,16],0),
       ('rim_north',[0,14,0],[16,16,4],0),('rim_south',[0,14,12],[16,16,16],0),
       ('rim_west',[0,14,4],[4,16,12],0),('rim_east',[12,14,4],[16,16,12],0),
       ('warm_core',[4,14,4],[12,15.6,12],1)]
for x in [6,9]:specs.append(('copper_bar_'+str(x),[x,15.6,4],[x+1,16,12],2))
for a,b in [(4,6),(7,9),(10,12)]:specs.append(('copper_cross_'+str(a),[a,15.6,7.5],[b,16,8.5],2))
cubes=[]
for name,start,end,t in specs:
    x,y,z=start;X,Y,Z=end
    uv={'north':[x,16-Y,X,16-y],'south':[x,16-Y,X,16-y],
        'west':[z,16-Y,Z,16-y],'east':[z,16-Y,Z,16-y],'up':[x,z,X,Z],'down':[x,z,X,Z]}
    cubes.append({'name':name,'from':start,'to':end,'box_uv':False,'autouv':0,
        'faces':{f:{'uv':v,'texture':textures[t]}for f,v in uv.items()}})
assert call('add_cubes',cubes=cubes)['created']==11
call('export_model',codec='project',path=str(OUT/'town_paving_lamp.bbmodel'))
call('export_model',codec='java_block',path=str(OUT/'town_paving_lamp.raw.json'))
m=json.loads((OUT/'town_paving_lamp.raw.json').read_text(encoding='utf8'))
m.pop('format_version',None);m.pop('credit',None)
m['parent']='minecraft:block/block'
m['textures']={str(i):'minecraft:block/'+n for i,n in enumerate(refs)}|{'particle':'minecraft:block/polished_andesite'}
# Only outer faces can be culled by a neighboring solid block.
for e in m['elements']:
    for face,f in e['faces'].items():
        axis={'west':0,'east':0,'down':1,'up':1,'north':2,'south':2}[face]
        edge=e['from'][axis] if face in ['west','down','north'] else e['to'][axis]
        if edge==(0 if face in ['west','down','north'] else 16):f['cullface']=face
        assert f['texture'].removeprefix('#') in m['textures']
model=ROOT/'src/main/resources/assets/xiantu/models/block/town_paving_lamp.json'
model.write_text(json.dumps(m,ensure_ascii=False,indent=2),encoding='utf8')
call('set_camera_angle',position=[33,37,33],target=[8,8,8])
shot=call('screenshot',width=700,height=700,annotate=False)
(OUT/'preview.png').write_bytes(base64.b64decode(shot['data_url'].split(',',1)[1]))
(OUT/'manifest.json').write_text(json.dumps({'target':'Forge 1.20.1 static opaque chunk-baked block',
 'budget':'11 cubes / one solid render layer / no block entity or animation',
 'collision':'full cube; top grille flush y=16; luminous recess 0.4 model units',
 'textureSources':['Mojang Java 1.20.1 client.jar: '+n for n in refs],
 'uv':'1 vanilla pixel per model unit, proportional cropped tiles',
 'bridge':'127.0.0.1:8787','before':status},ensure_ascii=False,indent=2),encoding='utf8')
print('MCP exported editable model, native JSON and preview:',OUT)
