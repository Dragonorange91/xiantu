"""Restore previous API sapling artwork and enlarge its silhouette by20%."""
import hashlib
import json
import shutil
import zipfile
from PIL import Image, ImageDraw
from spirit_wood_catalog import ROOT, TREES

BASE = ROOT / 'asset_sources/spirit_wood'
RESTORE = BASE / 'sapling-restore-20261011'
BACKUP = ROOT / 'work/qa/material-quality-20261011/backup/xiantu-0.1.0.jar'
ASSETS = ROOT / 'src/main/resources/assets/xiantu'


def install():
    """Apply after reviewed revisions so regeneration preserves the user choice."""
    state_path = RESTORE / 'status.json'
    if not state_path.exists():
        return
    state = json.loads(state_path.read_text(encoding='utf-8'))
    for record in state['saplings']:
        source = RESTORE / record['file']
        assert hashlib.sha256(source.read_bytes()).hexdigest() == record['sha256']
        destination = BASE / 'candidate' / record['species'] / 'sapling.png'
        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(source, destination)
        shutil.copy2(source, ASSETS / 'textures/block' / (record['species'] + '_sapling.png'))


def main():
    RESTORE.mkdir(parents=True, exist_ok=True)
    records = []
    preview = Image.new('RGB', (8 * 160, 192), (50, 52, 56))
    draw = ImageDraw.Draw(preview)
    with zipfile.ZipFile(BACKUP) as archive:
        for index, (species, name, *_) in enumerate(TREES):
            entry = 'assets/xiantu/textures/block/' + species + '_sapling.png'
            data = archive.read(entry)
            original_path = RESTORE / 'previous' / (species + '.png')
            original_path.parent.mkdir(parents=True, exist_ok=True)
            original_path.write_bytes(data)
            with Image.open(original_path) as source:
                source = source.convert('RGBA')
                assert source.size == (64, 64)
                bounds = source.getchannel('A').getbbox()
                assert bounds
                cropped = source.crop(bounds)
                size = tuple(round(value * 1.2) for value in cropped.size)
                assert max(size) <= 64, (species, size)
                enlarged = cropped.resize(size, Image.Resampling.NEAREST)
                image = Image.new('RGBA', (64, 64))
                # Keep the original aspect ratio and root at ground level.
                image.paste(enlarged, ((64 - size[0]) // 2, 64 - size[1]))
            destination = RESTORE / 'candidate' / species / 'sapling.png'
            destination.parent.mkdir(parents=True, exist_ok=True)
            image.save(destination)
            records.append({'species': species, 'name': name, 'file': str(destination.relative_to(RESTORE)),
                            'source_entry': entry, 'original_bounds': bounds,
                            'new_bounds': image.getchannel('A').getbbox(),
                            'source_sha256': hashlib.sha256(data).hexdigest(),
                            'sha256': hashlib.sha256(destination.read_bytes()).hexdigest()})
            thumb = image.resize((128, 128), Image.Resampling.NEAREST)
            preview.paste(thumb, (index * 160 + 16, 10), thumb)
            draw.text((index * 160 + 2, 150), species.replace('_', '\n'), fill='white')
    state = {'source_backup': str(BACKUP), 'scale': 1.2, 'runtime_size': [64, 64],
             'resampling': 'nearest', 'ground_anchored': True, 'saplings': records,
             'review': 'pending', 'user_acceptance': 'pending'}
    (RESTORE / 'status.json').write_text(json.dumps(state, ensure_ascii=False, indent=2), encoding='utf-8')
    preview.save(RESTORE / 'preview.png')
    install()
    print('Restored eight previous saplings;20% larger,64px,ground anchored.')


if __name__ == '__main__':
    main()
