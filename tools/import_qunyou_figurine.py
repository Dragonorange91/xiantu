"""Import the supplied static GLB without changing mesh, UVs, maps or material."""
from pathlib import Path
import json,hashlib,shutil
import numpy as np
from PIL import Image
from import_item_glb import parse_glb,encode_glb,set_pixel_art_sampling
ROOT=Path(__file__).resolve().parents[1];A=ROOT/'src/main/resources/assets/xiantu';D=ROOT/'src/main/resources/data/xiantu';OUT=ROOT/'asset_sources/qunyou-figurine-20261026';OUT.mkdir(exist_ok=True)
src=(Path.home() / 'Downloads/群友.glb');raw=src.read_bytes();source_sha=hashlib.sha256(raw).hexdigest()
assert source_sha=='e2bb07dd50b68fed7ab97e2bb025afa36b336aae4a2f0b2915c80c06afb0ec15'
d,b=parse_glb(raw);assert not d.get('skins') and not d.get('animations') and not d.get('extensionsRequired')
assert len(d['nodes'])==1 and len(d['meshes'])==1 and np.allclose(np.array(d['nodes'][0]['matrix']).reshape(4,4),np.eye(4))
set_pixel_art_sampling(d);runtime=encode_glb(d,b);(A/'models/item/qunyou_figurine.glb').write_bytes(runtime)
primitive=d['meshes'][0]['primitives'][0];bounds=d['accessors'][primitive['attributes']['POSITION']];lo=bounds['min'];hi=bounds['max'];scale=.80/max(y-x for x,y in zip(lo,hi));height=(hi[1]-lo[1])*scale
translation=[.5-(lo[0]+hi[0])*.5*scale,.5-(lo[1]+hi[1])*.5*scale,.5-(lo[2]+hi[2])*.5*scale]
def save(p,v):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(v,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
display={}
for side in ['righthand','lefthand']:
 display['firstperson_'+side]={'rotation':[0,-35 if side=='righthand' else 35,0],'translation':[0,1,0],'scale':[.48,.48,.48]}
 display['thirdperson_'+side]={'rotation':[0,0,0],'translation':[0,2,0],'scale':[.5,.5,.5]}
display.update(ground={'translation':[0,2,0],'scale':[.5,.5,.5]},fixed={'rotation':[0,180,0],'scale':[.8,.8,.8]})
save(A/'models/item/qunyou_figurine_glb.json',{'parent':'minecraft:builtin/entity','model':'xiantu:models/item/qunyou_figurine.glb','textures':{'particle':'xiantu:item/qunyou_figurine'},'scale':scale,'translation':translation,'block_offset_y':-(.5-height*.5),'display':display})
save(A/'models/item/qunyou_figurine.json',{'parent':'minecraft:block/block','gui_light':'front','loader':'forge:separate_transforms','textures':{'particle':'xiantu:item/qunyou_figurine'},'base':{'parent':'xiantu:item/qunyou_figurine_glb'},'perspectives':{'gui':{'parent':'minecraft:item/generated','textures':{'layer0':'xiantu:item/qunyou_figurine'}}}})
save(A/'models/block/qunyou_figurine.json',{'parent':'minecraft:block/block','textures':{'particle':'xiantu:item/qunyou_figurine'},'elements':[]})
save(A/'blockstates/qunyou_figurine.json',{'variants':{'facing='+s:{'model':'xiantu:block/qunyou_figurine'} for s in ['north','east','south','west']}})
save(D/'recipes/qunyou_figurine.json',{'type':'minecraft:crafting_shapeless','category':'misc','ingredients':[{'item':'minecraft:porkchop'}]*3,'result':{'item':'xiantu:qunyou_figurine','count':1}})
save(D/'loot_tables/blocks/qunyou_figurine.json',{'type':'minecraft:block','pools':[{'rolls':1,'conditions':[{'condition':'minecraft:survives_explosion'}],'entries':[{'type':'minecraft:item','name':'xiantu:qunyou_figurine'}]}]})
for locale,name in [('zh_cn','群友手办'),('en_us','Qunyou Figurine')]:
 p=A/f'lang/{locale}.json';v=json.loads(p.read_text('utf8'));v['block.xiantu.qunyou_figurine']=name;save(p,v)
save(OUT/'import-manifest.json',{'source':str(src),'source_sha256':source_sha,'runtime_sha256':hashlib.sha256(runtime).hexdigest(),'triangles':d['accessors'][primitive['indices']]['count']//3,'bounds':[lo,hi],'world_size':[(y-x)*scale for x,y in zip(lo,hi)],'scale':scale,'translation':translation,'changes':['sampler NEAREST only'],'static':True,'gui':'fixed rendered screenshot'})
print('Imported static figurine, dimensions',[(y-x)*scale for x,y in zip(lo,hi)])
