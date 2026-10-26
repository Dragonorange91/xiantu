"""Dedicated skill artwork: serial API masters, then inspected native 64px tiles."""
from pathlib import Path
import json,subprocess,sys,hashlib
from PIL import Image,ImageDraw
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'asset_sources/spell-icons-20261025'
API=(Path.home() / '.codex/skills/image-api-generator/scripts/generate_image.py')
BRIEFS={
 'fire_bolt':('火球术','A single compact blazing spherical fireball traveling diagonally from lower left to upper right. A bright ivory-hot round core, strong orange and vermilion flame lobes, three curved tapering flame tails behind it, a few restrained ember sparks. The round projectile is the dominant recognizable silhouette. Warm orange gold and deep red palette, no lightning, no scroll, no book.'),
 'thunder_strike':('雷引术','One thick brilliant jagged lightning bolt descending from a compact dark storm cloud at the upper center, splitting into two asymmetric smaller branches and striking a tiny concentrated violet electric impact at the bottom. Strong angular thunder silhouette, pale cyan-white core with indigo violet stepped edges. Cool silver cyan and violet palette, no flames, no scroll, no book.'),
 'sword_flight':('御剑术','One magnificent broad Chinese straight flying sword formed entirely from luminous azure-blue spiritual energy, soaring diagonally from lower left to upper right. Clearly readable long double-edged blade, pointed tip, elegant crossguard, short grip and round pommel. A brilliant icy white central blade ridge with cobalt blue and cyan stepped edges, surrounded by two graceful trailing cyan ribbons showing forward flight. The sword itself dominates the frame. No rider, no flames, no lightning, no scroll, no book.')
}
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def main():
 OUT.mkdir(parents=True,exist_ok=True);manifest={}
 for ident,(name,brief) in BRIEFS.items():
  raw=OUT/(ident+'-raw.png');master=OUT/(ident+'-master.png');runtime=OUT/(ident+'.png')
  prompt='Create ONE square spell ability HUD icon for a Chinese xianxia Minecraft RPG. '+brief+' Full-bleed flat opaque dark midnight-teal background, restrained darker corner texture. Center the complete skill within the central 84 percent of the square. No border or badge frame, no lettering, no Chinese characters, no numbers, no watermark, no UI mockup, no hands or characters. Design for a 64x64 logical pixel grid: crisp hard-edged square pixel clusters, deliberate small clusters and clear stepped shading, compact limited palette, excellent readability when displayed at 20 pixels wide. Rich magical energy with clear graphic silhouette, without painterly haze, bloom blur, photorealistic noise or a smooth airbrushed gradient. One independent 1024x1024 finished icon, not a sprite sheet.'
  (OUT/(ident+'.prompt.txt')).write_text(prompt,'utf8')
  if not raw.exists():
   subprocess.run([sys.executable,str(API),'--prompt',prompt,'--size','1024x1024','--quality','high','--response-format','omit','--timeout','240','--output',str(raw)],check=True)
  with Image.open(raw) as im:
   source_size=list(im.size);im=im.convert('RGBA').resize((1024,1024),Image.Resampling.LANCZOS);im.save(master)
   tile=im.resize((64,64),Image.Resampling.LANCZOS);tile.putalpha(255);tile.save(runtime)
  manifest[ident]=dict(name=name,source_size=source_size,master_sha256=sha(master),runtime_sha256=sha(runtime),runtime_size=[64,64],opaque=True)
  board=Image.new('RGB',(1408,1024),'#101b22');board.paste(Image.open(master).convert('RGB'),(0,0));board.paste(tile.resize((320,320),Image.Resampling.NEAREST).convert('RGB'),(1056,24));board.paste(tile.convert('RGB'),(1056,380));board.save(OUT/(ident+'-review.png'))
  print('READY',ident,flush=True)
 (OUT/'manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2),'utf8')
if __name__=='__main__':main()
