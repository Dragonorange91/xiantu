from pathlib import Path
import json
from package_ruins_20261024 import write
P=Path(__file__).resolve().parents[1];D=P/'src/main/resources/data/xiantu';O=P/'asset_sources/ruin-interiors-20261025'
for f in (O/'structures').glob('*.json'):
 b=json.loads(f.read_text(encoding='utf8'));blocks=b.pop('blocks');write(D/f'structures/ruins/{f.stem}.nbt',b['size'],blocks);(D/'ruin_blueprints'/f.name).write_text(json.dumps(b,ensure_ascii=False),encoding='utf8')
print('Compiled20 runtime blueprints from this task only')
