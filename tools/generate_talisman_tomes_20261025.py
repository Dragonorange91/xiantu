"""Three API-generated thread-bound talisman tomes, resumable serial generation."""
from pathlib import Path
import json, subprocess, sys
from PIL import Image, ImageDraw, ImageFont
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'asset_sources/talisman-tomes-20261025'
API=(Path.home() / '.codex/skills/image-api-generator/scripts/generate_image.py')
REF=ROOT/'asset_sources/gongfa-books-20261011/masters/xuan_yuan_yang_qi_jue.png'
BOOKS=[('xuanmen_initial','玄门符箓·初解','warm ochre yellow cloth, small red cinnabar Daoist talisman seal and modest red embroidered cloud corners; ivory binding'),('xuanmen_true','玄门符箓·真诠','deep indigo brocade, structured vermilion talisman geometry and fine copper cloud border; pale copper binding'),('xuanmen_secret','玄门符箓·秘典','dark plum silk, layered antique gold clouds surrounding a finely inscribed vermilion grand talisman seal; exquisite antique gold binding')]
def main():
 OUT.mkdir(parents=True,exist_ok=True)
 for ident,title,theme in BOOKS:
  dest=OUT/(ident+'.png')
  if dest.exists(): print('REUSE',ident,flush=True);continue
  prompt=f"""Create ONE usable FRONT COVER UV TEXTURE for a traditional Chinese thread-bound cultivation book matching the supplied approved Minecraft book reference. Square1024x1024 opaque artwork filled by flat cover edge to edge, orthographic, no perspective, no pages, no thickness, no outside shadow or backdrop. Binding seam and five ivory short horizontal stitch marks in LEFTMOST12%. Ivory vertical title label at x68..91%,y8..92%. Exact black Chinese title written TOP TO BOTTOM in one column: {title}. Every character appears once, including the centered dot. No additional text, no watermark, no rank captions. The full exact title is {title}. Cover identity: {theme}. Motifs occupy LEFT and MIDDLE, never behind label. Distinct attractive embroidered cloth, restrained3-tone stepped shading, hard edged fine square pixel clusters, no painterly blur, no bloom, no realistic noise. Designed on a64x64 logical pixel grid. This square UV maps onto a tall10.5x14 book face; preserve clear readable glyphs when reduced. Use the reference for cover construction and pixel language only; replace its title and motifs entirely."""
  (OUT/(ident+'.prompt.txt')).write_text(prompt,encoding='utf-8')
  for attempt in range(3):
   print('GENERATING',ident,attempt+1,flush=True)
   result=subprocess.run([sys.executable,str(API),'--prompt',prompt,'--input-image',str(REF),'--size','1024x1024','--quality','high','--response-format','omit','--timeout','240','--output',str(dest)])
   if result.returncode==0:break
  else:raise RuntimeError('Image API failed: '+ident)
  with Image.open(dest) as im:im.convert('RGBA').resize((1024,1024),Image.Resampling.LANCZOS).save(dest)
 print('ALL MASTERS READY',flush=True)
if __name__=='__main__':main()
