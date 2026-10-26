"""Import the user's replacement rigid pickaxes, preserving 64px GUI and cached triangle rendering."""
import json,hashlib,io,copy,shutil
from pathlib import Path
from PIL import Image
from import_item_glb import ROOT,ASSETS,parse_glb,encode_glb,set_pixel_art_sampling
source_dir=ROOT/'asset_sources/creature-batch-20261016/sources'
manifest=ROOT/'asset_sources/item_glb/import-manifest.json'
backup=ROOT/'asset_sources/creature-completion-20261017/previous-pickaxes'
backup.mkdir(exist_ok=True)
records=json.loads(manifest.read_text(encoding='utf8'))
if not (backup/manifest.name).exists():shutil.copy2(manifest,backup/manifest.name)
display=json.loads((ASSETS/'models/item/hundun_pickaxe_glb.json').read_text(encoding='utf8'))['display']
for item,name in [('hanxing_pickaxe','寒星镐'),('jiuyou_pickaxe','九幽镐'),('hundun_pickaxe','混沌镐')]:
 source=source_dir/(name+'.glb');d,b=parse_glb(source.read_bytes())
 assert len(d['meshes'])==len(d['materials'])==len(d['nodes'])==1 and not d.get('skins') and not d.get('animations')
 assert d['nodes'][0].get('matrix')==[1,0,0,0,0,1,0,0,0,0,1,0,0,0,0,1]
 set_pixel_art_sampling(d);raw=encode_glb(d,b)
 path=ASSETS/'models/item'/(item+'.glb')
 if not (backup/path.name).exists():shutil.copy2(path,backup/path.name)
 path.write_bytes(raw)
 p=d['meshes'][0]['primitives'][0];pos=d['accessors'][p['attributes']['POSITION']];lo,hi=pos['min'],pos['max']
 scale=.9/max(y-x for x,y in zip(lo,hi));translation=[.5-(x+y)*.5*scale for x,y in zip(lo,hi)]
 mat=d['materials'][0];im=d['images'][d['textures'][mat['pbrMetallicRoughness']['baseColorTexture']['index']]['source']]
 view=d['bufferViews'][im['bufferView']];png=b[view.get('byteOffset',0):view.get('byteOffset',0)+view['byteLength']]
 with Image.open(io.BytesIO(png)) as image:resolution=list(image.size)
 (ASSETS/'textures/item'/(item+'_glb_base_color.png')).write_bytes(png)
 model_file=ASSETS/'models/item'/(item+'_glb.json');model=json.loads(model_file.read_text(encoding='utf8'))
 if not (backup/model_file.name).exists():shutil.copy2(model_file,backup/model_file.name)
 model.update(scale=scale,translation=translation,display=copy.deepcopy(display));model_file.write_text(json.dumps(model,indent=2)+'\n',encoding='utf8')
 record=next(r for r in records if r['item']==item);tri=d['accessors'][p['indices']]['count']//3
 record.update(source=str(source),source_sha256=hashlib.sha256(source.read_bytes()).hexdigest(),sha256=hashlib.sha256(raw).hexdigest(),texture_sha256=hashlib.sha256(png).hexdigest(),texture_size=resolution,triangles=tri,source_bounds=[lo,hi],scale=scale,translation=translation,static_baked_quads_avoided=tri*6)
manifest.write_text(json.dumps(records,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
print('Imported three replacement pickaxes; GUI icons and native triangle cache retained')
