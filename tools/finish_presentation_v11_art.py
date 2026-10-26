from pathlib import Path
from PIL import Image
import json,shutil
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'asset_sources/presentation-20261024'
A=ROOT/'src/main/resources/assets/xiantu'
def save(im,rel):
 p=A/rel;b=OUT/'before'/p.relative_to(ROOT)
 if p.exists() and not b.exists():b.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(p,b)
 p.parent.mkdir(parents=True,exist_ok=True);im.save(p)
def model(rel,edit):
 p=A/rel;b=OUT/'before'/p.relative_to(ROOT)
 if not b.exists():b.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(p,b)
 d=json.loads(p.read_text(encoding='utf8'));edit(d);p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
im=Image.open(OUT/'tokens-master.png').convert('RGBA')
icons=[]
for i,name in enumerate(['dongfu_token','dongfu_child_token','realm_test_jade']):
 src=im.crop((i*im.width//3,0,(i+1)*im.width//3,im.height))
 data=[]
 for r,g,b,a in src.getdata():
  # Source is explicitly authored on black: remove that backdrop, including the jade-ring hole.
  alpha=round(max(0,min(1,(max(r,g,b)-12)/20))*255)
  data.append((r,g,b,alpha))
 src.putdata(data);src=src.crop(src.getbbox());src.thumbnail((56,56),Image.Resampling.LANCZOS)
 icon=Image.new('RGBA',(64,64));icon.alpha_composite(src,((64-src.width)//2,(64-src.height)//2))
 save(icon,f'textures/item/{name}.png');icons.append(icon)
 model(f'models/item/{name}.json',lambda d,n=name:d.update(parent='minecraft:item/generated',textures={'layer0':f'xiantu:item/{n}'}))
preview=Image.new('RGBA',(192,64),(44,50,53,255))
for i,icon in enumerate(icons):preview.alpha_composite(icon,(64*i,0))
preview.resize((768,256),Image.Resampling.NEAREST).save(OUT/'token-review.png')
im=Image.open(OUT/'blocks-master.png').convert('RGBA');w=im.width//2;h=im.height//2
for name,box in [('smelting_furnace_front',(0,0,w,h)),('smelting_furnace_side',(w,0,im.width,h)),('smelting_furnace_top',(0,h,w,im.height)),('spirit_gold_essence_ore',(w,h,im.width,im.height))]:
 save(im.crop(box).resize((64,64),Image.Resampling.LANCZOS),f'textures/block/{name}.png')
model('models/block/smelting_furnace.json',lambda d:d.update(textures={k:f'xiantu:block/smelting_furnace_{k}' for k in ('top','front','side')}))
model('models/block/spirit_gold_essence_ore.json',lambda d:d.update(textures={'all':'xiantu:block/spirit_gold_essence_ore'}))
print('3 transparent item icons, 4 block tiles integrated')
