"""Repeatable API artwork production; never edits approved harvested herb icons."""
from build_paths import review_font
from pathlib import Path
import argparse, concurrent.futures, json, os, subprocess, sys, threading
from PIL import Image, ImageDraw, ImageFont

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'asset_sources/biome-herb-materials-20261020/art'
API=Path.home()/'.codex/skills/image-api-generator/scripts/generate_image.py'
ASSETS=ROOT/'src/main/resources/assets/xiantu'
MINERALS=[
 ('purple_pattern_crystal','紫纹晶',-16,40,.10,3,5,'stone','gray-purple plate crystal with distinct parallel violet striations'),
 ('starlight_stone','星辉石',48,160,.06,2,3,'mountain','blue-gray rock with silver-white speckles and broken white veins, no glow'),
 ('chaos_crystal','混沌晶',-56,-32,.01,1,2,'deep','near-black angular crystal with cyan-purple cleavage planes'),
 ('cold_soul_crystal','寒魄晶',-48,24,.08,2,3,'cold','dark gray crystal with ice-blue needles and milky white fractures'),
 ('flame_heart_stone','炎心石',-48,16,.04,1,2,'lava','porous brown-black stone with dark red fused granules, restrained non-emissive'),
 ('thunder_stone','雷鸣石',-24,40,.08,2,3,'thunder','gray-green jagged rock cut by irregular pale-gold lightning-shaped veins'),
 ('lodestone_shard','玄磁石',-48,24,.09,3,5,'deep','dense charcoal gray metallic mineral with black and silver parallel magnetic striations'),
 ('moonlit_stone','月辉石',48,144,.06,2,4,'mountain','cool gray stone with crescent-like curved milky-white crystal inclusions'),
 ('scoria_core','炽曜石',20,100,.08,2,4,'nether','black basalt containing a dark-red glassy core with angular dark garnet fractures'),
 ('void_crystal','虚空晶',0,96,.03,1,2,'end','ink-blue crystal with thin cyan-violet facets embedded in pale end-stone, no glow'),
]
BEASTS=[('beast_bone','妖骨','one ivory beast bone, heavy angular ends, subtle ancient cultivation markings'),
 ('beast_fang','妖牙','one curved ivory fang with dark amber root, sharp pointed stepped silhouette'),
 ('beast_claw','妖爪','one curved obsidian talon with gray keratin ridges, sharp compact hooked silhouette'),
 ('beast_sinew','妖筋','a compact coil of taut tan-gold animal sinew, visible thin bound fibers'),
 ('beast_scale','妖鳞','three overlapping dark teal armored scales with pale hard bevels'),
 ('beast_horn','妖角','one ridged charcoal horn with warm ivory tip, diagonally curved'),
 ('beast_venom_sac','妖毒囊','one small natural green-purple leathery venom gland with sealed neck, no bottle'),
 ('beast_silk','妖丝','one neatly wound bundle of fine pearl-white beast silk, no spider or spool')]
LOCK=threading.Lock()

def tasks():
 manifest=json.loads((ROOT/'asset_sources/medicine-20261012-v2/manifest.json').read_text(encoding='utf-8'))
 herbs=[e for e in manifest if e['kind'] in {'plant_lifecycle','non_plant_material','drop_only_herb'}]
 for i,e in enumerate(herbs,1):
  if e['kind']!='plant_lifecycle': continue
  ident=f'herb_{i:04d}'
  prompt=(f'Create a revised growth sprite sheet for the Chinese cultivation herb {e["name"]}. '
   'Use the supplied image ONLY to preserve the botanical species identity, its distinctive leaf shape, flower or fruit colors. '
   'EXACTLY FOUR equal square cells arranged 2 columns by 2 rows, no dividers or labels. Order: top-left tiny seedling, '
   'top-right juvenile leafy plant, bottom-left almost mature budding plant, bottom-right fully mature flowering or fruiting plant. '
   'For fungi use tiny pin, young cap, opening cap, mature mushroom; moss uses four increasingly full living clusters. '
   'ALL plants rooted in the ground: show ONLY aboveground tissue; NO soil, NO earth clump, NO planter, NO roots, NO pebble base, '
   'NO hovering harvested ingredient. Tiny seedling has only 2 small leaves and NO flower or fruit. Juvenile has no flower or fruit. '
   'Fully mature is botanically complete, beautifully distinct and matches the original ingredient identity. '
   'Each subject centered inside its cell with clear transparent separation and ample margin, straight front-on view for crossed plant rendering. '
   'True transparent RGBA background, NOT a checkerboard. Minecraft Java pixel sprite aesthetic with crisp hard edged clusters, '
   'limited palette and stepped shading, designed to remain readable at 64x64; no smooth render, no glow haze, no lettering. 1024x1024 master.')
  yield (ident,'plant',e['name'],prompt,Path(e['master']))
 for ident,name,lo,hi,ch,mn,mx,habitat,desc in MINERALS:
  ore='lodestone_ore' if ident=='lodestone_shard' else ident+'_ore'
  host={'deep':'dark deepslate','nether':'black basalt and dark netherrack','end':'pale end stone'}.get(habitat,'natural medium-gray stone')
  yield (ident,'item',name,f'One Minecraft Java inventory icon of {name}: {desc}. Single naturally broken mineral piece, compact distinctive silhouette, three-quarter view. Transparent RGBA background, no checkerboard, no ground, no text. Crisp fine pixel-art clusters, restrained palette, hard edges and stepped shading, sharp at 64x64, 1024x1024 master. Not an ingot, not powder, no aura.',None)
  yield (ore,'ore',name+'矿石',f'One seamless square Minecraft Java ore BLOCK FACE TEXTURE for {name}. Material identity: {desc}. Mineral inclusions occupy about 30 percent, embedded naturally in {host}; several large readable mineral bodies, different irregular sizes and broken geological seams. Flat face-on opaque 1024x1024 tile, no perspective cube, no border, no text, no lighting glow. Fine deliberate pixel-art clusters designed to stay distinct at 64x64; not a recolored generic diamond ore, no blurry photorealism.',None)
 for ident,name,desc in BEASTS:
  yield (ident,'item',name,f'Create one Chinese cultivation Minecraft Java inventory material icon: {name}. Show {desc}. Organic harvested material, not a magic weapon. Whole compact object centered with transparent margins, true RGBA transparency, no checkerboard, no floor or shadow, no lettering. Fine crisp Minecraft pixel-art clusters, stepped shading, clear readable silhouette at 64x64; 1024x1024 master, not painterly or photorealistic.',None)

def generate(task):
 ident,kind,name,prompt,reference=task
 master=OUT/'masters'/f'{ident}.png'; raw=OUT/'originals'/f'{ident}.png'
 master.parent.mkdir(parents=True,exist_ok=True); raw.parent.mkdir(parents=True,exist_ok=True)
 (master.with_suffix('.prompt.txt')).write_text(prompt,encoding='utf-8')
 if not master.exists():
  args=[sys.executable,str(API),'--prompt',prompt,'--size','1024x1024','--quality','high','--response-format','omit','--timeout','240','--output',str(raw)]
  if reference: args+=['--input-image',str(reference)]
  for attempt in range(3):
   result=subprocess.run(args,capture_output=True,text=True,encoding='utf-8',errors='replace')
   if result.returncode==0 and raw.exists(): break
   print(f'API retry {ident} {attempt+1}: {result.stderr[-250:]}',flush=True)
  else: raise RuntimeError(f'Image API unavailable: {ident}')
  with Image.open(raw) as im: im.convert('RGBA').resize((1024,1024),Image.Resampling.LANCZOS).save(master)
 with Image.open(master) as source:
  source=source.convert('RGBA')
  if kind!='ore' and source.getchannel('A').getextrema()[0]==255:
   raise ValueError(f'{ident}: no real transparency; reject rather than key away subject pixels')
  runtime=OUT/'runtime';runtime.mkdir(exist_ok=True)
  if kind=='plant':
   from extract_herb_stages import extract_stages
   stages,record=extract_stages(source,ident)
   for age,result in enumerate(stages,1):
    result.save(runtime/f'{ident}_stage{age}.png')
   audit=OUT/'extraction';audit.mkdir(exist_ok=True)
   (audit/f'{ident}.json').write_text(json.dumps(record,ensure_ascii=False,indent=2),encoding='utf-8')
  elif kind=='ore':
   result=source.resize((64,64),Image.Resampling.LANCZOS);result.putalpha(255);result.save(runtime/f'{ident}.png')
  else:
   bounds=source.getchannel('A').point(lambda x:255 if x>=100 else 0).getbbox()
   tile=source.crop(bounds);tile.thumbnail((56,56),Image.Resampling.LANCZOS)
   result=Image.new('RGBA',(64,64));result.alpha_composite(tile,((64-tile.width)//2,(64-tile.height)//2))
   result.putalpha(result.getchannel('A').point(lambda x:255 if x>=100 else 0));result.save(runtime/f'{ident}.png')
 with LOCK: print(f'READY {ident} {name}',flush=True)
 return ident

def review():
 ts=list(tasks());font=review_font(14)
 for kind in ('plant','item','ore'):
  selected=[t for t in ts if t[1]==kind]
  cols=4 if kind=='plant' else 6; cw=280 if kind=='plant' else 132;ch=110 if kind=='plant' else 150
  sheet=Image.new('RGB',(cols*cw,((len(selected)+cols-1)//cols)*ch),(70,72,68));d=ImageDraw.Draw(sheet)
  for n,(ident,_,name,_,_) in enumerate(selected):
   x=n%cols*cw;y=n//cols*ch
   for age in range(4 if kind=='plant' else 1):
    p=OUT/'runtime'/f'{ident}_stage{age+1}.png' if kind=='plant' else OUT/'runtime'/f'{ident}.png'
    if not p.exists():continue
    im=Image.open(p).convert('RGBA').resize((64,64) if kind=='plant' else (120,120),Image.Resampling.NEAREST)
    sheet.paste(im,(x+age*68,y),im)
   d.text((x,y+(68 if kind=='plant' else 122)),name,font=font,fill='white')
  sheet.save(OUT/f'review-{kind}.png')

if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--kind',choices=['plant','item','ore','all'],default='all');p.add_argument('--limit',type=int);p.add_argument('--workers',type=int,default=3);p.add_argument('--review',action='store_true');a=p.parse_args()
 if a.review:review();sys.exit()
 ts=[t for t in tasks() if a.kind=='all' or t[1]==a.kind]
 if a.limit:ts=ts[:a.limit]
 with concurrent.futures.ThreadPoolExecutor(max_workers=a.workers) as pool:
  fs={pool.submit(generate,t):t[0] for t in ts}
  failures=[]
  for f in concurrent.futures.as_completed(fs):
   try:f.result()
   except Exception as e:failures.append(fs[f]);print(f'FAILED {fs[f]}: {e}',flush=True)
 review()
 (OUT/'generation-status.json').write_text(json.dumps({'failed':failures,'completed':[t[0] for t in ts if t[0] not in failures]},ensure_ascii=False,indent=2),encoding='utf-8')
 sys.exit(bool(failures))
