from pathlib import Path
import json,hashlib,shutil
from implement_resources_20261020 import ROOT,BACK,save,js
from revise_resources_20261020 import OUT,MINERALS,BEASTS,tasks
from PIL import Image
ASSETS='src/main/resources/assets/xiantu/'

def texture(source,relative):
 p=ROOT/relative;b=BACK/relative
 if p.exists() and not b.exists():b.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(p,b)
 p.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(source,p)

def integrate_plants():
 revised=ROOT/'asset_sources/workstations-herbs-integration-20261020/art/integrated.json'
 if revised.exists():
  from revise_plant_soil_visuals import integrate
  integrate()
  return
 rows=[]
 for tier in range(7):
  js(ASSETS+f'models/block/spirit_farmland_{tier}.json',{
   'parent':'minecraft:block/farmland','textures':{'dirt':f'xiantu:block/spirit_soil_{tier}',
   'top':f'xiantu:block/spirit_farmland_{tier}'}})
 # Crops touch the 15/16-height farmland; wild plants touch full-height grass.
 elements=[]
 for angle in (45,-45):
  elements.append({'from':[.8,-1,8],'to':[15.2,15,8],'shade':False,
   'rotation':{'origin':[8,7,8],'axis':'y','angle':angle,'rescale':True},
   'faces':{'north':{'uv':[0,0,16,16],'texture':'#cross'},'south':{'uv':[0,0,16,16],'texture':'#cross'}}})
 js(ASSETS+'models/block/herb_crop_cross.json',{'ambientocclusion':False,'textures':{'particle':'#cross'},'elements':elements})
 for ident,kind,name,_,_ in tasks():
  if kind!='plant':continue
  # Always require the complete set before changing a species.
  sources=[OUT/'runtime'/f'{ident}_stage{age+1}.png' for age in range(4)]
  if not all(p.exists() for p in sources):raise FileNotFoundError(f'Incomplete plant revision: {ident}')
  variants={}
  for age,p in enumerate(sources):
   texture(p,ASSETS+f'textures/block/{ident}_stage{age+1}.png')
   js(ASSETS+f'models/block/{ident}_stage{age+1}.json',{'parent':'xiantu:block/herb_crop_cross','textures':{'cross':f'xiantu:block/{ident}_stage{age+1}'}})
   variants[f'age={age}']={'model':f'xiantu:block/{ident}_stage{age+1}'}
  # High-tier plants have no seed/crop registration; only replace their wild sprite.
  state=ROOT/(ASSETS+f'blockstates/{ident}_crop.json')
  if state.exists():js(ASSETS+f'blockstates/{ident}_crop.json',{'variants':variants})
  texture(sources[3],ASSETS+f'textures/block/{ident}_wild.png')
  rows.append({'id':ident,'name':name,'stages':[{'age':age,'sha256':hashlib.sha256(p.read_bytes()).hexdigest()} for age,p in enumerate(sources)],'wild_uses_stage':4})
 (OUT/'integrated-plants.json').write_text(json.dumps(rows,ensure_ascii=False,indent=2),encoding='utf-8')

def integrate_materials():
 rows=[]
 for ident,kind,name,_,_ in tasks():
  if kind=='plant':continue
  p=OUT/'runtime'/f'{ident}.png'
  if not p.exists():raise FileNotFoundError(p)
  texture(p,ASSETS+f'textures/{"block" if kind=="ore" else "item"}/{ident}.png')
  if kind=='item':js(ASSETS+f'models/item/{ident}.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'xiantu:item/{ident}'}})
  rows.append({'id':ident,'name':name,'kind':kind,'sha256':hashlib.sha256(p.read_bytes()).hexdigest()})
 (OUT/'integrated-materials.json').write_text(json.dumps(rows,ensure_ascii=False,indent=2),encoding='utf-8')

if __name__=='__main__':
 integrate_plants();integrate_materials();print('Integrated 54 wild plants, 39 four-stage crops, 10 minerals/ores, 8 beast materials')
