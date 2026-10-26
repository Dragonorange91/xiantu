from pathlib import Path
import json,base64,sys
sys.path.insert(0,'tools')
from blockbench_native_bridge import NativeBridge
root=Path('.').resolve();out=root/'asset_sources/equipment-repair-20261024';out.mkdir(parents=True,exist_ok=True)
plan={'target':'Forge 1.20.1 Java block, single-block tabletop repair workshop','preference':'appearance, carved hardwood body with jade inlay, reinforced metal anvil and tool rack','view':'1-8 blocks','shading':'vanilla opaque albedo; no assumed PBR','geometry':'silhouette, recessed shelf, anvil horn, clamps, hammer, tongs and tool rack; fine grain in existing albedo','texture_density':'shared 4 pixels/model-unit, size-proportioned UVs','animation':'static','budget':'approximately 90 cubes; omit buried faces when safe'}
(out/'plan.json').write_text(json.dumps(plan,ensure_ascii=False,indent=2),encoding='utf-8')
bridge=NativeBridge();print(bridge.call('get_status'));print(bridge.call('new_project',format='java_block',name='equipment_repair_table',texture_width=16,texture_height=16))
tex={'wood':'xian_furniture_wood','metal':'block_hanxing_iron','copper':'block_chisui_copper','jade':'cloud_jade_smooth'}
geo=[]
def box(name,a,b,t='wood',rot=None,hidden=()):
 dx,dy,dz=[b[i]-a[i] for i in range(3)]
 faces={face:{'uv':[0,0,w,h],'texture':None} for face,w,h in [('north',dx,dy),('south',dx,dy),('east',dz,dy),('west',dz,dy),('up',dx,dz),('down',dx,dz)] if face not in hidden}
 c={'name':name,'from':a,'to':b,'faces':faces,'autouv':0,'box_uv':False}
 if rot:c.update(rotation=rot[0],origin=rot[1])
 geo.append((c,t))
for x in [1,12]:
 for z in [1,12]:
  box(f'foot_{x}_{z}',[x-.3,0,z-.3],[x+3.3,1.5,z+3.3],'copper')
  box(f'leg_{x}_{z}',[x,1,z],[x+3,10,z+3])
  box(f'collar_{x}_{z}',[x-.15,7.8,z-.15],[x+3.15,9,z+3.15],'copper')
box('low_shelf',[1.3,2.2,1.3],[14.7,3.5,14.7])
box('worktop_lower',[0,9,0],[16,10.4,16],'copper')
box('worktop',[.3,10.4,.3],[15.7,11.5,15.7])
for x in [0,15.2]:box(f'top_trim_{x}',[x,11.4,.2],[x+.8,11.8,15.8],'copper')
for z in [.1,15.1]:box(f'top_trim_z_{z}',[.85,11.4,z],[15.1,11.8,z+.8],'copper')
for z in [1.1,13.9]:box(f'waist_{z}',[3,7.2,z],[13,8.8,z+1])
for x in [1.1,13.9]:box(f'waist_x_{x}',[x,7.2,3],[x+1,8.8,13])
# Central anvil: layered base, waisted body and projecting tapered horn.
box('anvil_plinth',[4,11.5,5],[11.6,12.1,11.8],'copper')
box('anvil_foot',[4.5,12.1,5.5],[11.1,12.7,11.3],'metal')
box('anvil_waist',[6,12.7,6.7],[9.8,14.1,10.2],'metal')
box('anvil_face',[4.4,14.1,5.6],[11.6,15.25,11.4],'metal')
box('anvil_horn_root',[11.4,14.2,7],[13.1,15.1,10],'metal')
box('anvil_horn_mid',[13.0,14.35,7.4],[14.2,15,9.6],'metal')
box('anvil_horn_tip',[14.1,14.5,7.9],[15.35,14.9,9.1],'metal')
box('jade_insert',[6,15.25,6.5],[10.3,15.32,10.4],'jade')
# Lower storage has folded cloth, paired ingots, a wood box with metal strapping.
box('storage_box',[8.6,3.5,8.3],[13.2,6.7,13.5])
for z in [9,12]:box(f'box_band_{z}',[8.55,3.6,z+.08],[13.25,6.8,z+.48],'copper')
for n in range(3):box(f'stored_ingot_{n}',[2.4+n*1.7,3.5,9.4],[3.8+n*1.7,4.4,12.3],'metal')
box('folded_cloth',[2.5,3.5,3.2],[6.8,4.2,7.4],'jade')
# Back tool rail and a front clamp, all within one block.
for x in [1.5,13.5]:box(f'rack_post_{x}',[x,11.5,14],[x+1,15.7,15])
box('rack_rail',[1.35,14.6,13.8],[14.65,15.4,15.2])
for i,x in enumerate([3.3,5.4,7.5,9.6,11.7]):
 box(f'tool_shaft_{i}',[x,12.2,13.35],[x+.35,15.4,13.7],'copper')
 box(f'tool_blade_{i}',[x-.25,12.0,13.2],[x+.6,12.6,13.85],'metal')
box('vise_fixed',[1,11.8,2],[3.2,13.1,4.6],'metal')
box('vise_moving',[4.3,11.8,2],[5.2,13.1,4.6],'metal')
box('vise_screw',[2.9,12.2,3.0],[5.9,12.6,3.5],'copper')
box('vise_handle',[5.5,11.7,3.1],[5.85,13.2,3.45],'metal')
box('hammer_handle',[7.2,11.6,1.3],[12.1,12.0,1.8])
box('hammer_head',[11.6,11.5,.8],[13.3,12.6,2.8],'metal')
for z in [3.1,3.75]:box(f'tong_arm_{z}',[8,11.5,z],[13.8,11.8,z+.28],'metal')
for x in [5.4,6.5,7.6,8.7,9.8]:
 box(f'front_inlay_{x}',[x,8.0,.95],[x+.45,8.55,1.09],'jade')
print('blockout',bridge.call('add_cubes',cubes=[c for c,t in geo])['created'])
for name,file in tex.items():bridge.call('import_texture',path=str(root/'src/main/resources/assets/xiantu/textures/block'/f'{file}.png'),name=file+'.png')
for c,t in geo:
 faces={f:dict(v,texture=tex[t]+'.png') for f,v in c['faces'].items()}
 bridge.call('set_cube_uv',cube=c['name'],faces=faces)
print('check',bridge.call('check_model'))
print(bridge.call('save_project',path=str(out/'equipment_repair_table.bbmodel')))
print(bridge.call('export_project',path=str(out/'equipment_repair_table.json')))
bridge.call('set_camera_angle',position=[32,28,-32],target=[0,8,0])
shot=bridge.call('screenshot',width=900,height=900,annotate=False)
(out/'screenshot-response.json').write_text(json.dumps({k:v for k,v in shot.items() if not isinstance(v,str) or len(v)<500}),encoding='utf8')
for v in shot.values():
 if isinstance(v,str) and v.startswith('data:image'):(out/'preview.png').write_bytes(base64.b64decode(v.split(',',1)[1]))
model=json.loads((out/'equipment_repair_table.json').read_text(encoding='utf-8'))
# Blockbench export retains source file texture paths. Resolve them to packaged IDs.
model.pop('format_version',None)
model['textures']={k:'xiantu:block/'+Path(v.replace('\\','/')).stem for k,v in model['textures'].items()}
model['textures']['particle']='xiantu:block/xian_furniture_wood'
model['display']={'gui':{'rotation':[30,225,0],'translation':[0,0,0],'scale':[.7,.7,.7]},'fixed':{'rotation':[0,180,0],'scale':[.6,.6,.6]},'ground':{'translation':[0,3,0],'scale':[.25,.25,.25]}}
res=root/'src/main/resources/assets/xiantu'
(res/'models/block/equipment_repair_table.json').write_text(json.dumps(model,indent=2),encoding='utf8')
(res/'models/item/equipment_repair_table.json').write_text(json.dumps({'parent':'xiantu:block/equipment_repair_table'}),encoding='utf8')
(res/'blockstates/equipment_repair_table.json').write_text(json.dumps({'variants':{'facing='+d:{'model':'xiantu:block/equipment_repair_table',**({'y':y} if y else {})} for d,y in [('north',0),('east',90),('south',180),('west',270)]}},indent=2),encoding='utf8')
print('export elements',len(model['elements']),'faces',sum(len(x['faces']) for x in model['elements']))
print('preview',out/'preview.png')
