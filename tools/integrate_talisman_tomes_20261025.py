"""Integrate reviewed API covers using the approved native cultivation-book model."""
from build_paths import review_font
from pathlib import Path
import json,hashlib
from PIL import Image,ImageDraw,ImageFont
R=Path(__file__).resolve().parents[1];O=R/'asset_sources/talisman-tomes-20261025';A=R/'src/main/resources/assets/xiantu'
B=['xuanmen_initial','xuanmen_true','xuanmen_secret'];titles=['玄门符箓·初解','玄门符箓·真诠','玄门符箓·秘典']
base=json.loads((A/'models/item/xuan_yuan_yang_qi_jue.json').read_text(encoding='utf-8'));records=[]
canvas=Image.new('RGB',(1000,690),(30,32,37));d=ImageDraw.Draw(canvas);f=review_font(23)
for i,(ident,title) in enumerate(zip(B,titles)):
 im=Image.open(O/(ident+'.png')).convert('RGBA');cover=im.resize((64,64),Image.Resampling.LANCZOS);cloth=im.crop((117,190,135,800)).resize((64,64),Image.Resampling.LANCZOS)
 cover.save(A/'textures/item'/(ident+'_cover.png'));cloth.save(A/'textures/item'/(ident+'_cloth.png'));cover.save(O/(ident+'_64.png'))
 model=json.loads(json.dumps(base));model['textures'].update({'0':'xiantu:item/'+ident+'_cover','1':'xiantu:item/'+ident+'_cloth','particle':'xiantu:item/'+ident+'_cover'})
 (A/'models/item'/(ident+'.json')).write_text(json.dumps(model,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
 x=i*330+15;d.text((x,12),title,font=f,fill='white');canvas.paste(im.resize((220,290)),(x,55));canvas.paste(cover.resize((220,290),Image.Resampling.NEAREST),(x,355));canvas.paste(cover,(x+240,55));d.text((x+235,128),'64px',font=f,fill='white')
 records.append(dict(id=ident,title=title,model_data=i+1,master_sha256=hashlib.sha256((O/(ident+'.png')).read_bytes()).hexdigest(),texture_sha256=hashlib.sha256((A/'textures/item'/(ident+'_cover.png')).read_bytes()).hexdigest(),native_model_cubes=4,faces=24))
route={'parent':'xiantu:item/xuanmen_initial','overrides':[{'predicate':{'custom_model_data':i+1},'model':'xiantu:item/'+ident}for i,ident in enumerate(B)]}
(A/'models/item/talisman_blueprint.json').write_text(json.dumps(route,indent=2)+'\n',encoding='utf-8')
(A/'textures/item/talisman_blueprint.png').unlink(missing_ok=True)
canvas.save(O/'书籍贴图检查.png')
(O/'integration.json').write_text(json.dumps(dict(reference='existing cultivation manual 4-cuboid geometry and display',books=records,master_size=[1024,1024],runtime_size=[64,64],status='source_integrated_pending_build_and_runtime'),ensure_ascii=False,indent=2),encoding='utf-8')
print('Integrated3 native talisman books:4 cubes,24 faces each')
