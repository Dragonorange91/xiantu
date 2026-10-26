"""API-authored soil and loss-limited pixel cleanup of existing plant artwork."""
from build_paths import review_font
from pathlib import Path
import json,sys,subprocess,hashlib,argparse
from PIL import Image,ImageDraw,ImageFont
from integrate_large_workstations import ROOT,OUT,ASSET,put,js
ART=OUT/'art';API=Path.home()/'.codex/skills/image-api-generator/scripts/generate_image.py'
SOILS=[
 ('凡灵土','warm umber loam, broad irregular soft clods and a few tiny root fibres, very few stones'),
 ('一阶灵土','rich chocolate humus with small muted moss-green organic flecks and curled root fibres, organic crumb structure'),
 ('二阶灵土','cool taupe silty earth with fine chalky pale-gray sediment seams, flat overlapping compacted clods'),
 ('三阶灵土','deep terracotta fertile clay, chunky rounded clay aggregates and narrow dark cracks, few ivory mineral grains'),
 ('四阶灵土','dark olive-brown alluvial soil with visibly elongated fibrous organic fragments and compact fine grains'),
 ('五阶灵土','black-brown volcanic humus with porous charcoal lumps, rare subdued ochre mineral flecks, no lava'),
 ('六阶灵土','deep cool plum-black rich soil with sparse small pearly lavender angular grains and broad dark clods, no crystal spikes')]
def soil():
 for tier,(name,desc) in enumerate(SOILS):
  master=ART/f'masters/soil_{tier}.png';raw=ART/f'originals/soil_{tier}.png'
  prompt=(f'Create a Minecraft Java Chinese cultivation soil texture atlas for {name}. Material: {desc}. '
   'EXACTLY four equal square flat texture tiles in a 2 by 2 grid, each tile extends completely to its edges, no gutters or frame. '
   'Top left: natural untilled SOIL TOP, irregular clod pattern with NO furrows. Top right: SOIL SIDE cross section, subtle horizontal earth strata, no grass, no outside object. '
   'Bottom left: DRY TILLED TOP with four parallel straight vertical planting furrows, alternating raised soil rows and narrow darker recesses. '
   'Bottom right: matching MOIST TILLED TOP, exact same vertical rows with darker saturated soil; no standing water. '
   'All tiles flat orthographic surface textures, opaque, NOT isometric blocks or a landscape. '
   'Authentic Minecraft pixel art: deliberate hard-edged pixel clusters, limited earthy palette, broad readable clods, low visual noise, restrained highlights. '
   'Each tile will become 64x64 pixels, so no miniature mottled photo details. No baked perspective, no glow, no circles, no symbols, no labels or lettering. 1024x1024 master.')
  master.parent.mkdir(parents=True,exist_ok=True);raw.parent.mkdir(parents=True,exist_ok=True)
  master.with_suffix('.prompt.txt').write_text(prompt,encoding='utf-8')
  if not master.exists():
   for attempt in range(2):
    r=subprocess.run([sys.executable,str(API),'--prompt',prompt,'--size','1024x1024','--quality','high','--response-format','omit','--timeout','240','--output',str(raw)],capture_output=True,text=True,encoding='utf-8',errors='replace')
    if r.returncode==0 and raw.exists():break
    print(f'API failed soil {tier}, attempt {attempt+1}: {r.stderr[-180:]}',flush=True)
   else:raise RuntimeError('Soil API unavailable')
   Image.open(raw).convert('RGBA').resize((1024,1024),Image.Resampling.LANCZOS).save(master)
  im=Image.open(master)
  for n,suffix in enumerate(['top','side','dry','wet']):
   tile=im.crop((n%2*512,n//2*512,n%2*512+512,n//2*512+512)).convert('RGB').resize((64,64),Image.Resampling.BOX)
   tile=tile.quantize(colors=32,dither=Image.Dither.NONE).convert('RGBA');tile.putalpha(255)
   target=ART/f'runtime/soil_{tier}_{suffix}.png';target.parent.mkdir(parents=True,exist_ok=True);tile.save(target)
  print(f'Ready soil {tier}: {name}',flush=True)
  js(ART/'soil-progress.json',{'completed_through':tier})
 preview_soil()
def preview_soil():
 out=Image.new('RGB',(7*160,4*180+40),'#dad5c8');d=ImageDraw.Draw(out);font=review_font(16)
 for i,(name,_) in enumerate(SOILS):
  d.text((i*160+8,5),name,font=font,fill='#333333')
  for y,s in enumerate(['top','side','dry','wet']):
   p=ART/f'runtime/soil_{i}_{s}.png'
   if p.exists():out.paste(Image.open(p).resize((144,144),Image.Resampling.NEAREST),(i*160+8,35+y*180))
 out.save(OUT/'灵土耕土新贴图.png')
def plants():
 source=ROOT/'asset_sources/biome-herb-materials-20261020/art/runtime'
 oldrows=json.loads((source.parent/'integrated-plants.json').read_text(encoding='utf-8'))
 changes=[]
 for row in oldrows:
  ident=row['id'];hashes=[]
  for age in range(1,5):
   im=Image.open(source/f'{ident}_stage{age}.png').convert('RGBA')
   # Keep exact accepted silhouette and stage size; remove thousands of near-identical gradient shades.
   alpha=im.getchannel('A');rgb=im.convert('RGB')
   opaque=Image.new('RGB',im.size,(32,48,24));opaque.paste(rgb,mask=alpha)
   crisp=opaque.quantize(colors=48,method=Image.Quantize.MEDIANCUT,dither=Image.Dither.NONE).convert('RGBA');crisp.putalpha(alpha)
   pixels=crisp.load()
   for y in range(64):
    for x in range(64):
     if pixels[x,y][3]==0:pixels[x,y]=(0,0,0,0)
   p=ART/f'runtime/{ident}_stage{age}.png';p.parent.mkdir(parents=True,exist_ok=True);crisp.save(p)
   hashes.append(hashlib.sha256(p.read_bytes()).hexdigest())
  changes.append({'id':ident,'name':row['name'],'stages':[{'age':a,'sha256':h} for a,h in enumerate(hashes)],'wild_uses_stage':4})
 js(ART/'plants.json',changes)
 out=Image.new('RGB',(8*180,7*180),'#555950');d=ImageDraw.Draw(out);font=review_font(13)
 for n,row in enumerate(changes):
  x=n%8*180;y=n//8*180
  for c,folder in enumerate([source,ART/'runtime']):
   im=Image.open(folder/f'{row["id"]}_stage4.png').resize((80,80),Image.Resampling.NEAREST);out.paste(im,(x+c*85,y+25),im)
  d.text((x,y),row['name'],font=font,fill='white')
  d.text((x,y+110),'原图          清理后',font=font,fill='white')
 out.save(OUT/'草药清晰度对比.png')
 print(f'Prepared {len(changes)} plant sets; harvested item icons untouched.')
def integrate():
 rows=json.loads((ART/'plants.json').read_text(encoding='utf-8'))
 for row in rows:
  for a in range(1,5):put(ASSET/f'textures/block/{row["id"]}_stage{a}.png',(ART/f'runtime/{row["id"]}_stage{a}.png').read_bytes())
  put(ASSET/f'textures/block/{row["id"]}_wild.png',(ART/f'runtime/{row["id"]}_stage4.png').read_bytes())
 # Shared parent retains vanilla crossed plants, no random offset or heavy per-block renderer.
 for crop in [False,True]:
  exported=json.loads((OUT/f'exports/herb_{"crop" if crop else "wild"}_cross.json').read_text(encoding='utf-8'))
  elements=exported['elements']
  for element in elements:
   element.pop('name',None);element['shade']=False
   for face in element['faces'].values():face['texture']='#cross'
  js(ASSET/f'models/block/herb_{"crop" if crop else "wild"}_cross.json',{'ambientocclusion':False,'textures':{'particle':'#cross'},'elements':elements})
 for row in rows:js(ASSET/f'models/block/{row["id"]}_wild.json',{'parent':'xiantu:block/herb_wild_cross','textures':{'cross':f'xiantu:block/{row["id"]}_wild'}})
 js(ROOT/'asset_sources/biome-herb-materials-20261020/art/integrated-plants.json',rows)
 for i in range(7):
  for s in ['top','side','dry','wet']:put(ASSET/f'textures/block/spirit_soil_{i}_{s}.png',(ART/f'runtime/soil_{i}_{s}.png').read_bytes())
  # Retain existing entry names for particle/generator compatibility.
  put(ASSET/f'textures/block/spirit_soil_{i}.png',(ART/f'runtime/soil_{i}_side.png').read_bytes())
  put(ASSET/f'textures/block/spirit_farmland_{i}.png',(ART/f'runtime/soil_{i}_dry.png').read_bytes())
  js(ASSET/f'models/block/spirit_soil_{i}.json',{'parent':'minecraft:block/cube_bottom_top','textures':{'top':f'xiantu:block/spirit_soil_{i}_top','side':f'xiantu:block/spirit_soil_{i}_side','bottom':f'xiantu:block/spirit_soil_{i}_side'}})
  for wet in [False,True]:
   suffix='_moist' if wet else ''
   js(ASSET/f'models/block/spirit_farmland_{i}{suffix}.json',{'parent':'minecraft:block/farmland','textures':{'dirt':f'xiantu:block/spirit_soil_{i}_side','top':f'xiantu:block/spirit_soil_{i}_{"wet" if wet else "dry"}'}})
  js(ASSET/f'blockstates/spirit_farmland_{i}.json',{'variants':{f'moisture={m}':{'model':f'xiantu:block/spirit_farmland_{i}'+('_moist' if m>0 else '')} for m in range(8)}})
 p=ASSET/'lang/zh_cn.json';lang=json.loads(p.read_text(encoding='utf-8'))
 for i,(name,_) in enumerate(SOILS):lang[f'item.xiantu.spirit_soil_{i}']=name
 js(p,lang)
 js(ART/'integrated.json',{'plants':len(rows),'soil_sets':7,'plant_palette':48,'render':'vanilla cutout, 2 planes / 8 triangles','production_gameplay':False})
 print('Integrated clear plants + separate soil top/side/dry/moist textures.')
if __name__=='__main__':
 parser=argparse.ArgumentParser();parser.add_argument('action',choices=['plants','soil','integrate','review']);a=parser.parse_args()
 {'plants':plants,'soil':soil,'integrate':integrate,'review':preview_soil}[a.action]()
