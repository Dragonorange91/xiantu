"""Check the native Photon graph inventory and v10 texture pack before build."""
import json
from pathlib import Path

import nbtlib
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/xiantu"
manifest = json.loads((ASSETS / "vfx_v10_preload.json").read_text(encoding="utf-8"))
assert len(manifest["effects"]) >= 200
assert len(manifest["textures"]) >= 31
archived_bosses = {"battle_general_soul", "xuantians_remnant", "cloud_sea_lord"}
assert not any(any(f"xiantu:beasts/{boss}/" in effect for boss in archived_bosses)
               for effect in manifest["effects"]), "Archived boss VFX must stay out of the runtime preload"
for identifier in manifest["effects"]:
    path = ASSETS / "fx" / (identifier.split(":", 1)[1] + ".fx")
    assert path.read_bytes()[:2] == b"\x1f\x8b", f"Photon FX must be gzip-compressed: {identifier}"
    tag = nbtlib.load(path)
    assert tag["_version"] == 1 and tag["fx"]["mainFX"]["fxObjects"], identifier
    # Photon indexes each runtime's objects by transform UUID. A duplicate
    # silently replaces an emitter, even when both objects otherwise parse.
    groups = [("main", tag["fx"]["mainFX"])] + list(tag["fx"].get("subFXs", {}).items())
    for group_name, group in groups:
        ids = [tuple(map(int, obj["transform"]["id"])) for obj in group.get("fxObjects", [])]
        assert len(ids) == len(set(ids)), f"Duplicate Photon emitter UUID: {identifier}/{group_name}"
    for obj in tag['fx']['mainFX']['fxObjects']:
        config = obj.get('config', {})
        shape = config.get('shape', {}).get('shape', {})
        if str(shape.get('_type', '')) == 'cone':
            assert float(shape.get('radius', 1)) > 0, f'Photon cone division by zero: {identifier}/{obj.get("name")}'
        texture = str(config.get('material', {}).get('material', {}).get('texture', ''))
        uv = config.get('uvAnimation', {})
        if uv.get('enable') and '/v11/' in texture:
            count = int(uv['tiles']['a']) * int(uv['tiles']['b'])
            assert float(uv['cycle']) == count, f'Incomplete atlas playback: {identifier}'
for identifier in manifest["textures"]:
    assert identifier.endswith(".png"), f"Texture ID must include the PNG suffix: {identifier}"
    namespace, path = identifier.split(":", 1)
    texture = ROOT / "src/main/resources/assets" / namespace / path
    assert texture.is_file(), f"Missing Photon texture: {identifier} -> {texture}"
for path in (ASSETS / "textures/vfx/v10").glob("*.png"):
    with Image.open(path) as image:
        assert image.size == (1024, 1024) and image.mode == "RGBA", (path, image.size, image.mode)
print(f"PASS Photon native graphs: {len(manifest['effects'])}; textures: {len(manifest['textures'])}")
