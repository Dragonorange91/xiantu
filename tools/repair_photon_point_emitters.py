"""Idempotent correction of active native Photon graphs, preserving combat timing.

Only resources in the runtime preload manifest are touched; sealed bosses remain
archived. Backup every original before replacing. This does not re-author graphs.
"""
from pathlib import Path
import json, shutil
import nbtlib as n

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/xiantu'
OUT = ROOT / 'work/qa/module-audit-20261024/point-emitter-repair'
manifest = json.loads((ASSETS / 'vfx_v10_preload.json').read_text())
records = []
for ident in manifest['effects']:
    path = ASSETS / 'fx' / (ident.split(':', 1)[1] + '.fx')
    root = n.load(path)
    fixes = []
    for obj in root['fx']['mainFX']['fxObjects']:
        cfg = obj.get('config', {})
        shape = cfg.get('shape', {}).get('shape', {})
        if str(shape.get('_type', '')) == 'cone' and float(shape.get('radius', 1)) == 0:
            shape['radius'] = n.Float(.0001)
            fixes.append(str(obj.get('name', '')) + ': finite cone radius')
        uv = cfg.get('uvAnimation', {})
        tex = str(cfg.get('material', {}).get('material', {}).get('texture', ''))
        # Authored normalized curves need the absolute tile count in Photon.
        # Keep custom v10 non-normalized/constant-frame presets untouched.
        if uv.get('enable') and '/v11/' in tex or uv.get('enable') and '/v9/' in tex and tex.endswith('_flow.png'):
            count = int(uv['tiles']['a']) * int(uv['tiles']['b'])
            if float(uv.get('cycle', 1)) != count:
                uv['cycle'] = n.Float(count)
                points = uv.get('frameOverTime', {}).get('curves', [])
                for segment in points:
                    if float(segment[-1]) == 1:
                        segment[-1] = n.Float(.999)
                fixes.append(str(obj.get('name', '')) + ': complete atlas')
    if fixes:
        backup = OUT / 'before' / path.relative_to(ASSETS)
        if not backup.exists():
            backup.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(path, backup)
        n.File(root, gzipped=True).save(path)
        records.append({'effect': ident, 'fixes': fixes})
OUT.mkdir(parents=True, exist_ok=True)
(OUT / 'changes-latest.json').write_text(json.dumps(records, indent=2), encoding='utf8')
print(f'Corrected {len(records)} active graphs; {sum(len(r["fixes"]) for r in records)} emitter settings')
