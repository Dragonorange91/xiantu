"""Read-only deployment guard for the locally benchmarked GFBS runtime."""
import argparse
import hashlib
import io
import json
import re
from pathlib import Path
import sys
import tomllib
import zipfile

# 64-joint budget; five Boss GPU/combat regression and ten-Boss FPS evidence:
# docs/GFBS64关节与原始手部验收记录_20261018.md
# 2026-10-18: RenderSystem.deleteTexture keeps Minecraft's texture binding cache
# synchronized when GPU skin palette textures are disposed.
EXPECTED = "C3EF96B6FEFB70A866BA56C083AD90C2326983D8D4B1F3451B602C8247FDEC4C"
PROJECT = Path(__file__).resolve().parents[1]
OPTIMIZED = PROJECT / "libs/gfbs_gltf-1.5.1-xiantu-optimized.jar"
NESTED_PATH = "META-INF/jarjar/gfbs_gltf-1.5.1-xiantu-optimized.jar"


def digest(path):
    with path.open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest().upper()


def verify_payload(payload, label):
    actual = hashlib.sha256(payload).hexdigest().upper()
    if actual != EXPECTED:
        raise ValueError(f"{label}: unaccepted GFBS SHA-256 {actual}")
    with zipfile.ZipFile(io.BytesIO(payload)) as nested:
        assert nested.testzip() is None, "Corrupt GFBS archive"
        assert re.search(r'modId\s*=\s*[\"\x27]gfbs_gltf[\"\x27]', nested.read('META-INF/mods.toml').decode()), 'GFBS mod identity missing'
        assert 'MixinConfigs: gfbs_gltf.mixins.json' in nested.read('META-INF/MANIFEST.MF').decode()
        license_text = nested.read('META-INF/LICENSE_GFBS').decode()
        assert 'MIT License' in license_text and 'LytharaLab' in license_text
        libraries = json.loads(nested.read('META-INF/jarjar/metadata.json'))['jars']
        assert len(libraries) == 6, "GFBS transitive dependency inventory changed"
        for library in libraries:
            with zipfile.ZipFile(io.BytesIO(nested.read(library['path']))) as dependency:
                assert dependency.testzip() is None, library['path']
    print(f"PASS {label}: {actual}; mixins, MIT and six nested libraries intact")


def verify_embedded(path):
    with zipfile.ZipFile(path) as outer:
        metadata = json.loads(outer.read('META-INF/jarjar/metadata.json'))['jars']
        entries = [entry for entry in metadata if entry['identifier'] == {
            'group': 'org.lytharalab.gfbs', 'artifact': 'gfbs_gltf'}]
        assert len(entries) == 1, 'Expected exactly one embedded GFBS'
        entry = entries[0]
        assert entry['path'] == NESTED_PATH
        assert entry['version'] == {'range': '[1.5.1]', 'artifactVersion': '1.5.1'}
        assert entry['isObfuscated'] is False
        assert not any(name.startswith('org/lytharalab/') for name in outer.namelist()), 'GFBS classes were flattened'
        assert 'MIT License' in outer.read('META-INF/LICENSE_GFBS').decode()
        assert 'MixinConfigs: xiantu.mixins.json' in outer.read('META-INF/MANIFEST.MF').decode()
        assert 'xiantu.refmap.json' in outer.namelist()
        verify_payload(outer.read(NESTED_PATH), f'embedded runtime in {path}')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--mods-dir", type=Path, default=Path.home() / "Desktop/仙途/.minecraft/versions/1.20.1-Forge_47.4.26/mods")
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument('--source-only', action='store_true', help='Verify pinned build input without inspecting the installed game')
    mode.add_argument('--jar', type=Path, help='Verify a built or staged Xiantu JAR')
    args = parser.parse_args()
    try:
        verify_payload(OPTIMIZED.read_bytes(), 'pinned optimized build input')
        if args.jar:
            verify_embedded(args.jar)
        elif not args.source_only:
            embedded = []
            external = []
            for path in args.mods_dir.glob('*.jar'):
                with zipfile.ZipFile(path) as archive:
                    names = archive.namelist()
                    if NESTED_PATH in names:
                        embedded.append(path)
                    if 'META-INF/mods.toml' in names:
                        declared = tomllib.loads(archive.read('META-INF/mods.toml').decode()).get('mods', [])
                        if any(mod.get('modId') == 'gfbs_gltf' for mod in declared):
                            external.append(path)
            assert len(embedded) <= 1, f'Multiple Xiantu embedded runtimes: {embedded}'
            if embedded:
                assert not external, f'Remove standalone GFBS from mods; embedded runtime must be authoritative: {external}'
                verify_embedded(embedded[0])
            else:
                assert len(external) == 1, f'Expected one external GFBS before embedded deployment: {external}'
                verify_payload(external[0].read_bytes(), 'legacy installed external runtime')
    except (AssertionError, ValueError, OSError, KeyError, zipfile.BadZipFile) as error:
        print(f'FAIL {error}')
        return 1
    official = PROJECT / "libs/gfbs_gltf-1.5.1-all.jar"
    if official.is_file():
        print(f"INFO compile dependency: {digest(official)}; do not copy over the benchmarked runtime")
    print("This checks runtime identity only; new creature animation and FPS still need in-game tests.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
