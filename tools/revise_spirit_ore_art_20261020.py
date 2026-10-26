"""Resumable image-edit masters; integration requires an explicit reviewed batch."""
from pathlib import Path
import argparse, hashlib, json, subprocess, sys
from PIL import Image, ImageDraw

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'work/visual-revision-20261020/spirit-ores'
ASSET=ROOT/'src/main/resources/assets/xiantu'
API=Path.home()/'.codex/skills/image-api-generator/scripts/generate_image.py'
ORES=[('fan_ling_shi','凡灵石矿','gray stone with small pale ivory crystals, sparse irregular embedded angular mineral pieces'),
 ('ling_jing','灵晶矿','gray stone with translucent mint-green crystal veins and compact angular crystals'),
 ('ling_yu','灵玉矿','dark stone with pale jade-green thick broken mineral bands and jade lenses'),
 ('xuan_jing','玄晶矿','dark slate with deep cyan-blue crystal blades in natural jagged clusters'),
 ('di_sui','地髓矿','dark slate with amber and earthy golden mineral nodules and branching geological veins'),
 ('tian_jing','天晶矿','dark slate with sky-blue and silvery angular crystal deposits, fine mineral fracture lines'),
 ('xian_yu','仙玉矿','dark slate with milky pale lilac-white jade lenses and broken purple seams'),
 ('dao_shi','道石矿','dark slate with violet-gold angular mineral deposits and subdued ancient-looking natural fractures')]

def main():
 p=argparse.ArgumentParser();p.add_argument('--integrate',action='store_true');a=p.parse_args()
 OUT.mkdir(parents=True,exist_ok=True);records=[]
 for ident,name,description in ORES:
  src=ASSET/f'textures/block/ore_{ident}.png';master=OUT/f'{ident}-master.png';candidate=OUT/f'{ident}-64.png'
  ref=OUT/f'{ident}-reference.png'
  if not ref.exists():ref.write_bytes(src.read_bytes())
  prompt=(f'Redesign the supplied Minecraft ore texture for {name}, preserving its mineral color identity. '
   f'One flat seamless square BLOCK FACE tile: {description}. A credible natural ore embedded in host rock, '
   'roughly 25 to 35 percent mineral, with 5 to 9 irregular separated deposits of varied size, geological grain and cleavage. '
   'Use the visual grammar of vanilla Minecraft stone ores, fine sharp hard-edged pixel clusters and restrained colors; '
   'readable at 64x64. No perspective cube, no item crystal icon, no rings, no artificial borders, no runes or stars, '
   'no floating crystals, no glow or bloom, no smooth photorealistic noise, no coarse giant mosaic. Opaque 1024x1024 tile.')
  (OUT/f'{ident}-prompt.txt').write_text(prompt,encoding='utf-8')
  if not master.exists():
   for attempt in range(3):
    print(f'{ident}: image edit attempt {attempt+1}',flush=True)
    result=subprocess.run([sys.executable,str(API),'--input-image',str(ref),'--prompt',prompt,'--size','1024x1024','--quality','high','--model','gpt-image-2.5-sunburst','--response-format','omit','--timeout','240','--output',str(master)],capture_output=True)
    (OUT/f'{ident}-api-{attempt+1}.log').write_bytes(result.stdout+result.stderr)
    if result.returncode==0 and master.exists():break
   if not master.exists():raise RuntimeError(f'{ident}: three API attempts failed; resumable outputs retained')
  im=Image.open(master).convert('RGB').resize((1024,1024),Image.Resampling.LANCZOS)
  im.resize((64,64),Image.Resampling.LANCZOS).quantize(colors=48,method=Image.Quantize.MEDIANCUT).convert('RGBA').save(candidate)
  if a.integrate:src.write_bytes(candidate.read_bytes())
  records.append(dict(id=ident,name=name,master=str(master),candidate=str(candidate),reference_sha256=hashlib.sha256(ref.read_bytes()).hexdigest(),sha256=hashlib.sha256(candidate.read_bytes()).hexdigest()))
  (OUT/'manifest.json').write_text(json.dumps(records,ensure_ascii=False,indent=2),encoding='utf-8')
  print(ident,'prepared' if not a.integrate else 'integrated',flush=True)
 sheet=Image.new('RGB',(4*268,2*300),'#262b32');d=ImageDraw.Draw(sheet)
 for i,r in enumerate(records):
  x=(i%4)*268+6;y=(i//4)*300+4
  sheet.paste(Image.open(r['candidate']).resize((256,256),Image.Resampling.NEAREST),(x,y));d.text((x,y+260),r['id'],fill='white')
 sheet.save(OUT/'review.png')
if __name__=='__main__':main()
