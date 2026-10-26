"""Generate five stone-inscription masters serially; model production follows review."""
import subprocess
import sys
from pathlib import Path
from PIL import Image
from generate_building_material_resources import FAMILIES, MASTER

API = Path.home() / '.codex/skills/image-api-generator/scripts/generate_image.py'
BRIEFS = {
    'azure_brick': 'weathered blue-gray ancient mountain stone, sparse muted silver mineral grain',
    'black_rock': 'charcoal basalt, chipped dark gray mineral planes, pale worn incision edges',
    'cloud_jade': 'ivory celadon jade, sparse irregular emerald mineral inclusions',
    'spring_stone': 'gray teal water-worn stone, subtle erosion channels and dark damp fissures',
    'cloud_floating_stone': 'fog gray pale lavender stone, subtle cold blue veins and sparse peach inclusions',
}


def main():
    for family, material in BRIEFS.items():
        output = MASTER / f'{family}_stele_inscription_1024.png'
        if output.exists():
            print('RETAIN', output.name, flush=True)
            continue
        prompt = (
            'One flat orthographic diffuse albedo surface texture for a Minecraft Java cultivation stone stele. '
            'Square 1024x1024 texture, stone completely fills every edge, no object silhouette, no scenery, '
            'no photographed monument, no pedestal or frame painted into the image. Material: ' + material + '. '
            'Preserve the reference stone palette and mineral identity, remove all existing cloud ornament. '
            'In the center engrave five vertical columns of ancient Chinese seal-script Daoist inscription, '
            'with around six large glyphs per column and deliberately chipped missing strokes. '
            'Ancient text inspired by 道法自然 萬象歸元 玄門妙理 太初無極 心合天道. '
            'Letters occupy central 75 percent width and from 15 to 83 percent height. Keep top and bottom stone margins. '
            'Glyphs are recessed dark stone cuts with restrained lighter edges, not painted calligraphy, not glowing runes. '
            'Readable large angular incised glyph clusters, avoid dense tiny writing. Ancient cultivation ruin atmosphere. '
            'Minecraft-compatible hard square pixel clusters, limited palette, stepped shading, crisp low-resolution material language '
            'designed for a128x160 whole front split into64x64 tile sections. No soft gradients, no gloss, no bloom, '
            'no vignette, no photorealism, no Latin letters, no modern UI, no golden magical border.'
        )
        print('GENERATE', family, flush=True)
        subprocess.run([sys.executable, str(API), '--prompt', prompt, '--size', '1024x1024',
                        '--quality', 'high', '--response-format', 'omit', '--timeout', '240',
                        '--input-image', str(MASTER / f'{family}_smooth_1024.png'),
                        '--output', str(output)], check=True)
        with Image.open(output) as image:
            source_size = image.size
            image.convert('RGBA').resize((1024, 1024), Image.Resampling.LANCZOS).save(output)
        print('READY', family, 'source', source_size, flush=True)


if __name__ == '__main__':
    main()
