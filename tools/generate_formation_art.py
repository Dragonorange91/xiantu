"""Serial API masters -> separately cropped64px formation material tiles.

Do not generate geometry until the whole texture batch has been inspected.
"""
from pathlib import Path
import argparse, json, subprocess, sys
from PIL import Image, ImageOps, ImageEnhance, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'asset_sources/formation-20261011'
API = (Path.home() / '.codex/skills/image-api-generator/scripts/generate_image.py')
TILES = ['stone', 'metal', 'wood', 'base_top', 'rune_top', 'eye_top', 'pillar_side', 'crystal', 'eye_lit']
THEMES = {
    1: 'Tier one: cool blue-gray hewn spirit stone, aged warm copper, dark natural oak. Restrained pale turquoise crystal. Humble but lovingly crafted Daoist academy apparatus, not rusty scrap.',
    2: 'Tier two: reddish black basalt, red marrow copper with amber-gold highlights, lacquer-red spirit wood inlays, ember-red translucent mineral. Warm regal furnace-sect ritual apparatus, rich but no excessive gold.',
    3: 'Tier three: very dark blue-black ironstone, pale silver and icy cyan engraved lines, charcoal ironwood, clear cyan-white crystalline mineral. Ancient austere celestial observatory apparatus, refined luxurious silver inlay, not a recolor of the lower tiers.'
}
STYLE = '''Minecraft Java cultivation-mod real usable UV texture sheet. Orthographic flat material artwork, not a screenshot or rendered object. 1024x1024 opaque square. Strict crisp square pixel clusters, stepped3-tone shading, restrained16-24color palette, clearly readable at64px per tile, no photography, no realistic noise, no soft brush, no bloom, no typography, no letters, no labels, no borders between tiles, no perspective, no shadows cast across panels. Exact equal grid cells, each panel completely filled edge-to-edge. Chinese Daoist cultivation identity, copper/silver angular seal strokes and cloud-scroll relief. Surfaces carry believable mineral/crafted-material variation. Every panel is a DIFFERENT specified surface, not the same motif recolored.'''

def prompts():
    result = {}
    for tier, theme in THEMES.items():
        result[f'array_material_{tier}'] = STYLE + '\n' + theme + '''\nAn exact3by3 UV material grid. Reading row-major:
1 top left: plain seamless stone material, broad small angular mineral clusters, no symbols.
2 top middle: plain seamless aged metal material, pixel stepped metal tones, no symbols.
3 top right: seamless upright wood-grain material, sensible lengthwise fibers, no symbols.
4 middle left: square outer-ring pavement TOP: two narrow parallel metal conduits run straight from the middle top edge to middle bottom edge, a tiny angular cloud flourish at center, plain stone around them. No enclosed border; tracks meet both opposite tile edges.
5 middle middle: inner-ring pavement TOP: one large concise angular Daoist seal sigil set in stone, bent spiral cloud and branching inward channels, more ornament than the outer ring, no readable text.
6 middle right: dormant central eye TOP: one concentric circular Bagua-inspired ritual disk, dark central gemstone, engraved radial spokes and restrained metal arcs, circle centered in tile.
7 bottom left: pillar SIDE: upright symmetrical long recessed vertical conduit down the center, flanked by angular carved cloud-scroll stone relief; no base or crown drawn, those are modeled separately. Pattern uses entire tile.
8 bottom middle: plain seamless faceted crystalline-mineral material, broad stepped facets with internal depth, no separate jewelry/object.
9 bottom right: ACTIVE version of exactly the central eye disk in panel6, center gem brighter with thin turquoise/amber/cyan powered engraving matching tier. No flame or bloom. Only the core artwork differs between dormant and powered.'''
    result['meditation_cushion_material'] = STYLE + '''\nAn exact2by2 UV grid for a simple Chinese Daoist round meditation rush cushion, warm wheat-gold and dark brown with restrained muted red cloth edge. Reading row-major:
1 top left: TOP surface, entire square filled with tightly coiled concentric rush-fiber weave centered at square center, no outer background and no depicted3D silhouette; fine but chunky woven concentric tracks and3-tone rush shadows.
2 top right: SIDE strip material, entire square filled with horizontal braided rush bundles and small knot weave, no separate cushion silhouette.
3 bottom left: BOTTOM material, plain coarse crossed reed mat weave, earthy brown, seamless.
4 bottom right: plain muted vermilion and ochre woven-cloth band material, subtle narrow regular stitching and simple angular cloud trim; entire square filled, no text. Not a luxurious throne. This is a woven circular sitting cushion.'''
    return result

def generate():
    (OUT/'masters').mkdir(parents=True, exist_ok=True)
    for name, prompt in prompts().items():
        dest = OUT/'masters'/f'{name}_1024.png'
        (OUT/'masters'/f'{name}.prompt.txt').write_text(prompt, encoding='utf-8')
        if dest.exists():
            print('REUSE', name, flush=True); continue
        print('GENERATING', name, flush=True)
        cmd=[sys.executable,str(API),'--prompt',prompt,'--size','1024x1024','--quality','high','--response-format','omit','--timeout','240','--output',str(dest)]
        subprocess.run(cmd,check=True)
        with Image.open(dest) as im:
            actual=im.size
            im.convert('RGBA').resize((1024,1024),Image.Resampling.LANCZOS).save(dest)
        print('MASTER',name,'source_dimensions',actual,flush=True)

def prepare():
    target=OUT/'textures';target.mkdir(parents=True,exist_ok=True)
    manifest=[]
    for name in prompts():
        master=OUT/'masters'/f'{name}_1024.png'
        with Image.open(master) as im:
            source=im.convert('RGBA')
        cells=TILES if name.startswith('array_') else ['top','side','bottom','trim']
        size=3 if name.startswith('array_') else 2
        for i,cell in enumerate(cells):
            col,row=i%size,i//size
            bounds=tuple(round(n*1024/size) for n in (col,row,col+1,row+1))
            image=source.crop(bounds).resize((64,64),Image.Resampling.LANCZOS)
            image=ImageEnhance.Sharpness(image).enhance(1.2)
            stem=(f'array_{name[-1]}_{cell}' if name.startswith('array_') else f'meditation_cushion_{cell}')
            image.save(target/f'{stem}.png')
            manifest.append({'texture':stem,'master':str(master),'crop':bounds,'size':[64,64],'state':'generated_pending_visual_inspection'})
    (OUT/'texture-manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    # Review contact sheet displays64px tiles enlarged nearest-neighbor, never generates artwork.
    sheet=Image.new('RGB',(9*140,4*160),(30,27,24));draw=ImageDraw.Draw(sheet)
    for row in range(4):
        selected=manifest[row*9:(row+1)*9] if row<3 else manifest[27:]
        for col,entry in enumerate(selected):
            with Image.open(target/(entry['texture']+'.png')) as im: tile=im.convert('RGB').resize((128,128),Image.Resampling.NEAREST)
            sheet.paste(tile,(col*140,row*160))
            draw.text((col*140,row*160+131),entry['texture'].replace('meditation_cushion_','cushion_'),fill='white')
    sheet.save(OUT/'texture-review.png')
    print('Prepared',len(manifest),'runtime tiles; review',OUT/'texture-review.png',flush=True)

if __name__=='__main__':
    ap=argparse.ArgumentParser();ap.add_argument('--prepare-only',action='store_true');args=ap.parse_args()
    if not args.prepare_only: generate()
    prepare()
