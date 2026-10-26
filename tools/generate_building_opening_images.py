"""Serial API-only artwork; door master maps to two 64-square runtime tiles."""
import json
import subprocess
import sys
from pathlib import Path
from PIL import Image
from generate_building_material_resources import FAMILIES, MASTER

API = Path.home() / '.codex/skills/image-api-generator/scripts/generate_image.py'
IDENTITY = {
    'azure_brick': 'blue gray spirit stone, restrained ivory auspicious cloud relief, worn pale stone bands',
    'black_rock': 'charcoal basalt, angular nested square formation relief, sparse muted bronze groove accents',
    'cloud_jade': 'ivory celadon jade, curved cloud relief, green mineral inclusions, pale jade edging',
    'spring_stone': 'gray teal water-worn stone, lotus and ripple relief, soft weathered edges',
    'cloud_floating_stone': 'fog gray lavender porous stone, stepped celestial cloud bands, tiny peach gold mineral accents',
}

def main():
    record_path = MASTER.parent / 'openings-generation.json'
    records = json.loads(record_path.read_text(encoding='utf-8')) if record_path.exists() else {}
    for family, identity in IDENTITY.items():
        for kind in ('door', 'trapdoor'):
            name = f'{family}_{kind}'
            output = MASTER / f'{name}_1024.png'
            if output.exists():
                with Image.open(output) as existing:
                    existing.load()
                    normalized = existing.convert('RGBA').resize((1024,1024), Image.Resampling.LANCZOS)
                normalized.save(output)
                print(f'EXISTS {name}; verified and normalized without API request', flush=True)
                continue
            layout = (
                'One complete CLOSED tall door leaf front surface as a square UV texture. '
                'The entire canvas is the material surface, zero background, zero margin. '
                'This square will be mapped to a 1-block wide, 2-block tall door. '
                'Upper half: one large restrained relief panel. Lower half: solid lower relief panel '
                'with ONE small bronze handle near right edge. Continuous narrow rectangular stone edging. '
                'No doorway, no wall, no double doors, no perspective. Do not depict an open door. '
            ) if kind == 'door' else (
                'One CLOSED square stone trapdoor panel viewed exactly face-on. Full-bleed square surface, '
                'zero margin or background. Broad sturdy stone edging, central relief medallion, '
                'one small muted bronze latch at bottom edge. No hinges protruding, no scene, no perspective. '
            )
            prompt = (layout + 'Chinese cultivation temple architectural material: ' + identity + '. '
                      'Minecraft Java block texture style, hard-edged square pixel clusters, limited palette, '
                      'stepped shading, readable at 64 pixels. Diffuse albedo only; no gloss, bloom, glow, '
                      'photorealism, soft gradients, text, letters or symbols pretending to be text. '
                      'Fully opaque 1024x1024 square master. Use reference for material palette only.')
            args = [sys.executable, str(API), '--prompt', prompt, '--size', '1024x1024',
                    '--response-format', 'omit', '--quality', 'high', '--input-image', str(MASTER / f'{family}_carved_1024.png'),
                    '--output', str(output)]
            for attempt in range(2):
                print(f'BEGIN {name} attempt={attempt + 1}', flush=True)
                result = subprocess.run(args)
                if result.returncode == 0 and output.exists():
                    break
            else:
                print(f'FAILED {name}; no substitute generated', flush=True)
                return 1
            with Image.open(output) as source:
                records[name] = {'source_size': list(source.size), 'source_mode': source.mode}
                normalized = source.convert('RGBA').resize((1024, 1024), Image.Resampling.LANCZOS)
            normalized.save(output)
            print(f'READY {name} source={records[name]}', flush=True)
            (MASTER.parent / 'openings-generation.json').write_text(json.dumps(records, indent=2), encoding='utf-8')
    return 0

if __name__ == '__main__':
    raise SystemExit(main())
