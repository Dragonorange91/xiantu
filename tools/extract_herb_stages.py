"""Extract whole plants from lifecycle masters, even across nominal panel edges."""
import hashlib
import numpy as np
from scipy import ndimage
from PIL import Image

HEIGHTS = (12, 25, 41, 56)


def extract_stages(source, ident):
    rgba = np.array(source.convert('RGBA'))
    visible = rgba[:, :, 3] >= 100
    labels, count = ndimage.label(visible, np.ones((3, 3), dtype=bool))
    sizes = np.bincount(labels.ravel())
    if count < 4:
        raise ValueError(f'{ident}: expected four separate plants')
    main = sorted(range(1, count + 1), key=lambda label: sizes[label], reverse=True)[:4]
    centers = ndimage.center_of_mass(visible, labels, main)
    slots = {}
    for label, (y, x) in zip(main, centers):
        slot = int(y >= source.height / 2) * 2 + int(x >= source.width / 2)
        if slot in slots:
            raise ValueError(f'{ident}: ambiguous stage positions; inspect master')
        slots[slot] = label
    if set(slots) != set(range(4)):
        raise ValueError(f'{ident}: missing stage')

    # Detached leaves/petals belong to the nearest complete subject. A full-sheet
    # mask, rather than four rectangular crops, retains tips above the midline.
    anchors = np.isin(labels, main)
    nearest = ndimage.distance_transform_edt(~anchors, return_distances=False,
                                            return_indices=True)
    nearest_label = labels[tuple(nearest)]
    assigned = {label: label for label in main}
    ignored_pixels = 0
    for label in range(1, count + 1):
        if label in assigned:
            continue
        if sizes[label] < 16:
            ignored_pixels += int(sizes[label])
            continue
        neighbors = nearest_label[labels == label]
        assigned[label] = int(np.bincount(neighbors).argmax())

    outputs, records = [], []
    for slot in range(4):
        components = [label for label, owner in assigned.items() if owner == slots[slot]]
        mask = np.isin(labels, components)
        complete_pixels = int(mask.sum())
        yy, xx = np.nonzero(mask)
        complete_bounds = [int(xx.min()), int(yy.min()), int(xx.max()+1), int(yy.max()+1)]
        # Original API sheets for these root medicines include underground roots.
        # Preserve the existing aboveground-only rule using GLOBAL coordinates.
        removed_roots = 0
        if ident == 'herb_0010' or (ident == 'herb_0009' and slot == 3):
            cutoff = (slot // 2) * 512 + (290 if ident == 'herb_0010' else 260)
            rgb = rgba[:, :, :3].astype(float)
            green = (rgb[:, :, 1] > rgb[:, :, 0]*1.08) & (rgb[:, :, 1] > rgb[:, :, 2]*1.1)
            underground = np.arange(source.height)[:, None] >= cutoff
            remove = mask & underground & ~green
            removed_roots = int(remove.sum())
            mask &= ~remove
        if not mask.any():
            raise ValueError(f'{ident}: empty aboveground stage {slot+1}')
        pixels = rgba.copy()
        pixels[~mask] = 0  # prevents invisible RGB bleeding into the reduced edge
        pixels[:, :, 3] = mask.astype(np.uint8)*255
        tile = Image.fromarray(pixels)
        bounds = tile.getbbox()
        tile = tile.crop(bounds)
        tile.thumbnail((56, HEIGHTS[slot]), Image.Resampling.LANCZOS)
        result = Image.new('RGBA', (64, 64))
        result.alpha_composite(tile, ((64-tile.width)//2, 64-tile.height))
        result.putalpha(result.getchannel('A').point(lambda a: 255 if a >= 100 else 0))
        final_bounds = result.getbbox()
        if not final_bounds:
            raise ValueError(f'{ident}: vanished stage {slot+1}')
        tile = result.crop(final_bounds)
        result = Image.new('RGBA', (64, 64))
        result.alpha_composite(tile, ((64-tile.width)//2, 64-tile.height))
        outputs.append(result)
        panel = [slot % 2 * 512, slot // 2 * 512, (slot % 2+1)*512, (slot // 2+1)*512]
        records.append({'stage': slot+1, 'source_bounds': complete_bounds,
                        'aboveground_bounds': list(bounds), 'source_pixels': complete_pixels,
                        'aboveground_pixels': int(mask.sum()), 'removed_root_pixels': removed_roots,
                        'crossed_panel': any((complete_bounds[i] < panel[i] if i < 2 else complete_bounds[i] > panel[i]) for i in range(4)),
                        'components': components,
                        'silhouette_sha256': hashlib.sha256(result.getchannel('A').tobytes()).hexdigest()})
    return outputs, {'id': ident, 'method': 'whole-sheet connected subjects, no panel clipping',
                     'master_rgba_sha256': hashlib.sha256(rgba.tobytes()).hexdigest(),
                     'ignored_speck_pixels': ignored_pixels, 'stages': records}
