"""Serial API roof textures, inspected before any roof modeling begins."""
import json
import subprocess
import sys
from pathlib import Path
from PIL import Image, ImageDraw
from generate_building_material_resources import MASTER, TEXTURES

API = Path.home() / '.codex/skills/image-api-generator/scripts/generate_image.py'
ROOFS = {
    'azure_brick': ('青云灵瓦', 'blue gray fired stone tiles, pale cloud-shaped chips at a few tile corners, sober ancient temple roof'),
    'black_rock': ('玄岩阵瓦', 'charcoal basalt tiles, angular trapezoid tile edges, sparse bronze formation-line grooves, underground shrine roof'),
    'cloud_jade': ('云纹玉瓦', 'ivory celadon jade tiles, pale green mineral veins, restrained auspicious cloud engraving on occasional tiles, jade pavilion roof'),
    'spring_stone': ('庭水莲瓦', 'muted teal gray tiles, water-worn stepped lips and sparse lotus-petal relief, riverside courtyard roof'),
    'cloud_floating_stone': ('云霞浮瓦', 'fog gray lavender light porous stone tiles, pale peach mineral bands and restrained cloud crests, ancient floating pavilion roof'),
}


def main():
    records_path = MASTER.parent / 'roof-generation.json'
    records = json.loads(records_path.read_text(encoding='utf-8')) if records_path.exists() else {}
    thumbs = []
    for family, (name, identity) in ROOFS.items():
        target = MASTER / f'{family}_roof_1024.png'
        if not target.exists():
            prompt = ('A seamless repeating square Minecraft Java block albedo texture, flat orthographic top view. '
                      'Entire canvas is a roof surface, zero background, zero margins. Chinese cultivation architecture: '
                      + identity + '. Exactly four broad vertical channels of overlapping traditional Chinese curved '
                      'roof tiles, three staggered courses, each tile has a clear stepped lower lip. '
                      'Keep tile relief readable, shallow and fully painted; do not depict a whole building, pitched roof '
                      'silhouette, roof frame or scenery. Minecraft hard square pixel clusters, restrained limited palette, '
                      'crisp stair-step shading, readable at 64x64. Old slightly chipped roof but no dense moss or noisy grit. '
                      'No text, glow, glossy highlights, photorealism, smooth gradients or outlines. Fully opaque 1024x1024. '
                      'Reference gives material palette only, replace wall decoration with actual roof tile courses.')
            args = [sys.executable,str(API),'--prompt',prompt,'--size','1024x1024', '--quality','high',
                    '--response-format','omit','--input-image',str(MASTER / f'{family}_smooth_1024.png'),
                    '--output',str(target)]
            for attempt in range(2):
                print(f'BEGIN {name} attempt={attempt+1}',flush=True)
                outcome = subprocess.run(args)
                if outcome.returncode == 0 and target.exists():
                    break
            else:
                return 1
        with Image.open(target) as source:
            source.load()
            records.setdefault(family, {'source_size':list(source.size),'source_mode':source.mode})
            master = source.convert('RGBA').resize((1024,1024),Image.Resampling.LANCZOS)
        master.save(target)
        small = master.resize((64,64),Image.Resampling.LANCZOS)
        assert small.getchannel('A').getextrema() == (255,255), (family,'Roof must be opaque')
        small.save(TEXTURES / f'{family}_roof.png')
        thumbs.append(small.resize((256,256),Image.Resampling.NEAREST))
        records_path.write_text(json.dumps(records,indent=2)+'\n',encoding='utf-8')
        print(f'READY {family} 1024 master + 64 runtime',flush=True)
    review = Image.new('RGB',(5*256,280),(32,35,40))
    for column,(family,thumb) in enumerate(zip(ROOFS,thumbs)):
        review.paste(thumb,(column*256,24))
        ImageDraw.Draw(review).text((column*256+8,6),family,fill='white')
    review.save(MASTER.parent / 'roof-review.png')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
