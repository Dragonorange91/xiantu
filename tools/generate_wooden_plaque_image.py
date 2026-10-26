"""Generate a blank dark-wood plaque albedo master, then reduce it to a 64px UV tile."""
import subprocess
import sys
from pathlib import Path
from PIL import Image
from generate_building_material_resources import MASTER, TEXTURES


def main():
    output = MASTER / 'wooden_plaque_1024.png'
    if not output.exists():
        prompt = (
            'One square flat orthographic diffuse albedo texture atlas for a Minecraft Java Chinese cultivation '
            'wooden shop plaque. The square canvas packs a 2:1 horizontal plaque front stretched vertically for UV '
            'mapping; this is a surface texture, not a product photograph or scene. Dark almost-black lacquered '
            'rosewood center, perfectly flat smooth wood surface with restrained subtle horizontal grain. '
            'Thin elegant antique gold geometric Chinese meander border runs around all four edges, straight '
            'regular right-angle stepped key pattern and narrow dark wood outer trim. Broad calm empty central '
            'writing field occupying at least 75 percent of width, ZERO letters, ZERO Chinese characters, '
            'ZERO symbols in center. Gold frame like a traditional Chinese shop signboard, rectangular not stone. '
            'Minecraft hard square pixel clusters, clear stepped shading, limited black-brown and warm gold '
            'palette, readable after reduction to 64x64. No bevel perspective, no stone, cracks, moss, rough rock, '
            'photorealism, bloom, smooth gradients, drop shadow, background or margins. Fully opaque fills canvas '
            'edge to edge. 1024x1024 high quality master. Use the provided image only for wood and gold frame '
            'design; remove its text completely and isolate the plaque surface.'
        )
        api = Path.home() / '.codex/skills/image-api-generator/scripts/generate_image.py'
        subprocess.run([sys.executable, str(api), '--prompt', prompt, '--size', '1024x1024',
                        '--quality', 'high', '--response-format', 'omit', '--input-image',
                        str(Path.home()/'AppData/Local/Temp/codex-clipboard-8cfabdda-49ba-482e-9099-c09e302f65fc.png'),
                        '--output', str(output)], check=True)
    with Image.open(output) as image:
        print('API source:', image.size, image.mode)
        master = image.convert('RGBA').resize((1024,1024), Image.Resampling.LANCZOS)
    master.save(output)
    small = master.resize((64,64), Image.Resampling.LANCZOS)
    assert small.getchannel('A').getextrema() == (255,255)
    small.save(TEXTURES / 'wooden_plaque.png')
    small.resize((512,512), Image.Resampling.NEAREST).save(MASTER.parent / 'wooden-plaque-review.png')


if __name__ == '__main__':
    main()
