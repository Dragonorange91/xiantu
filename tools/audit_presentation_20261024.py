from pathlib import Path
import json,re
from PIL import Image,ImageDraw,ImageFont
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'asset_sources/presentation-20261024';OUT.mkdir(parents=True,exist_ok=True)
REF=(Path.home() / 'Desktop/法术技能特效参考')
font=ImageFont.truetype('C:/Windows/Fonts/consola.ttf',14)
gifs=sorted(REF.rglob('*.gif'))
inventory=[]
for index,p in enumerate(gifs):
    im=Image.open(p);n=getattr(im,'n_frames',1);im.seek(min(n-1,n//2))
    inventory.append({'index':index,'path':str(p),'frames':n,'size':im.size})
for start in range(0,len(gifs),80):
    page=Image.new('RGB',(1280,128*10),(24,28,33));d=ImageDraw.Draw(page)
    for j,row in enumerate(inventory[start:start+80]):
        im=Image.open(row['path']);im.seek(row['frames']//2);im=im.convert('RGBA');im.thumbnail((152,102))
        x=j%8*160;y=j//8*128;page.paste(im,(x+(160-im.width)//2,y),im)
        d.text((x+4,y+104),f"{row['index']:03} {Path(row['path']).stem[:14]}",font=font,fill='white')
    page.save(OUT/f'references-{start//80}.jpg')
(OUT/'reference-index.json').write_text(json.dumps(inventory,ensure_ascii=False,indent=2),encoding='utf-8')
screens=[]
for p in (ROOT/'src/main/java').rglob('*Screen.java'):
    s=p.read_text(encoding='utf-8-sig');screens.append({'file':str(p.relative_to(ROOT)),'textures':re.findall(r'"([^"\n]*(?:textures/|\.png)[^"\n]*)"',s),'fills':s.count('.fill(')})
(OUT/'screen-audit.json').write_text(json.dumps(screens,ensure_ascii=False,indent=2),encoding='utf-8')
assets=ROOT/'src/main/resources/assets/xiantu';placeholder=[];missing=[]
for p in (assets/'models/item').glob('*.json'):
    obj=json.loads(p.read_text(encoding='utf-8-sig'));tex=obj.get('textures',{});parent=obj.get('parent','')
    vanilla=[v for v in tex.values() if isinstance(v,str) and v.startswith('minecraft:')]
    if vanilla:placeholder.append({'id':p.stem,'vanilla':vanilla,'parent':parent})
    for v in tex.values():
        if isinstance(v,str) and v.startswith('xiantu:') and not (assets/'textures'/(v.split(':',1)[1]+'.png')).exists():missing.append({'id':p.stem,'texture':v})
(OUT/'item-audit.json').write_text(json.dumps({'vanilla':placeholder,'missing':missing},ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps({'gifs':len(gifs),'screens':screens,'vanilla_item_count':len(placeholder),'vanilla_items':placeholder,'missing':missing},ensure_ascii=False))
