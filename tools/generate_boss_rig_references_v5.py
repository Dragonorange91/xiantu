"""Image API redesigns for Meshy; references only, not replacement runtime models."""
import subprocess, sys, json, os
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'asset_sources/boss-art-v5-20261018/references'
SOURCE=(Path.home() / 'Documents/Codex/2026-10-05/f/outputs/images/妖兽掉落物_掌柜魂体_Boss参考_20261016/Boss')
SCRIPT=Path.home()/'.codex/skills/image-api-generator/scripts/generate_image.py'
COMMON='''Redesign the provided character as ONE full-body production-ready 3D character reference for Meshy T2.5, Minecraft-inspired crisp pixel textures on clean angular low-poly triangle geometry. Retain the identity, face, main costume colors and Chinese xianxia dignity. Highest artistic quality, elegant carefully designed silhouette, NOT noisy tiny voxel chunks. Straight frontal orthographic view at chest height. Symmetric neutral relaxed A-pose, arms 35 degrees away from torso, elbows slightly bent, palms facing forward, all five fingers distinguishable with gentle natural separation. Head, neck, sternum, navel align on the same vertical centerline. Head looks straight forward, never twisted. Two complete separate legs, boots shoulder width apart and pointing forward. Clear negative space between arms and torso, between thighs and calves. Fitted layered clothing, separate short shoulder plates, visible elbow, wrist, knee and ankle articulation. Split knee-length robe panels attach at waist and hang beside thighs without joining legs or fingers; visible trousers and lower legs. Hair tied behind head, short side locks end above shoulders. No floating props, smoke, aura, particles, trailing ribbons, cloak, ground base, weapon, text, extra views or additional character. All magic and embroidery are flat painted pixel details on opaque surfaces, no translucent limbs or sculpted glowing effects. Neutral light-gray background, even studio lighting, sharp readable pixels, full silhouette with generous margin. Rich but economical surfaces suitable for around 2000 triangles; do not draw tessellation wireframes.'''
BRIEFS={
 '玄天城主残魂': 'A regal ancient Chinese city lord, stern handsome masculine face, solid pale icy-blue skin, compact dark navy ceremonial crown with small cyan inset stone, midnight-blue and antique-gold fitted lamellar armor, restrained cyan engraved city-seal emblem centered on chest. Broken-city motif painted on armor, pristine readable body anatomy. Sleeveless upper armor, fitted forearm guards, two distinct hands and boots. Ghost identity comes from color and expression, not transparent holes or smoke.',
 '云海天君遗念': 'An elegant adult male Chinese celestial immortal, white tied hair, handsome composed face, compact jade-and-gold crown, ivory and pale-aqua fitted cross-collar robes with refined flat cloud embroideries, navy inner trousers visible between split panels, jade belt clasp. Narrow sleeves end at wrists. White boots, gold edging; majestic xianxia rather than Western wizard. No ankle-length tubular dress, no wings, no orbiting ribbons.'}
if __name__=='__main__':
 OUT.mkdir(parents=True,exist_ok=True)
 for name,brief in BRIEFS.items():
  if (OUT/(name+'.png')).exists():continue
  prompt=COMMON+'\n'+brief
  (OUT/(name+'-prompt.txt')).write_text(prompt,encoding='utf8')
  result=subprocess.run([sys.executable,str(SCRIPT),'--prompt',prompt,'--input-image',str(SOURCE/(name+'.png')),'--model','gpt-image-2.5-sunburst','--size','1024x1024','--quality','high','--response-format','omit','--timeout','300','--output',str(OUT/(name+'.png'))],capture_output=True,text=True,encoding='utf8',errors='replace',env=os.environ|{'PYTHONIOENCODING':'utf-8'})
  (OUT/(name+'-generation.log')).write_text(result.stdout+'\n'+result.stderr,encoding='utf8')
  print(name,'exit',result.returncode,flush=True)
  if result.returncode: print(result.stderr[-600:],flush=True)
