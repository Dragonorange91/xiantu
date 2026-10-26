"""Generate all cracked-masonry textures serially before creating resource models."""
import subprocess
import sys
from pathlib import Path
from PIL import Image
from generate_building_material_resources import FAMILIES, MASTER, TEXTURES

API = Path.home()/'.codex/skills/image-api-generator/scripts/generate_image.py'
BRIEFS = {
    'azure_brick': 'weathered blue-gray spirit bricks, broken muted silver cloud engravings; cracks follow the uneven brick joints with only two deeper diagonal fractures',
    'black_rock': 'ancient charcoal basalt formation stone; dense broad mineral plates split by angular stepped hairline fractures, broken bronze formation channels, no purple glow',
    'cloud_jade': 'old ivory celadon jade masonry; chipped pale jade planes, emerald inclusions and a few branching dark green stress cracks interrupting worn cloud ornament',
    'spring_stone': 'water-worn gray teal courtyard masonry; dark damp irregular fissures, chipped ripple motifs and broad abraded stone surfaces, no actual liquid',
    'cloud_floating_stone': 'fog-gray pale lavender porous celestial ruin masonry; fractured cold blue mineral streaks and sparse peach-gold inclusions, chipped cloud motif and irregular erosion pits',
}


def main():
    for family, brief in BRIEFS.items():
        output=MASTER/f'{family}_cracked_1024.png'
        if not output.exists():
            prompt=(
                'A single seamless flat diffuse square Minecraft Java cultivation building BLOCK TEXTURE, '
                '1024x1024. Edit the supplied accepted stone texture into an ancient cracked ruined version. '
                'Preserve its material identity, mineral palette, ornament scale and original masonry arrangement. '
                +brief+'. Keep most stone intact, varied asymmetric cracks, clear but restrained damage. '
                'The texture completely fills every edge, suitable for a repeated building wall. '
                'Hard square pixel clusters, limited palette and stepped shading that remain crisp at64x64. '
                'Crack widths mostly one or two runtime pixels. No thick black spiderweb, no huge hole, '
                'no moss or vines, no shiny bloom, no soft photorealism, no perspective, no cube render, '
                'no frame, no words, no transparent background. Cultivation ancient ruin, not generic recolored stone.'
            )
            print('GENERATE',family,flush=True)
            subprocess.run([sys.executable,str(API),'--prompt',prompt,'--size','1024x1024',
                '--quality','high','--response-format','omit','--timeout','240',
                '--input-image',str(MASTER/f"{FAMILIES[family]['texture']}_1024.png"),
                '--output',str(output)],check=True)
        with Image.open(output) as im:
            original_size=im.size
            master=im.convert('RGBA').resize((1024,1024),Image.Resampling.LANCZOS)
        master.save(output)
        master.resize((64,64),Image.Resampling.LANCZOS).save(TEXTURES/f'{family}_cracked.png')
        print('READY',family,'source',original_size,flush=True)


if __name__=='__main__':
    main()
