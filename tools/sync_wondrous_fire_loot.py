"""Idempotent post-process for regenerated Xiantu ruin chest tables. Never calls image APIs."""
from pathlib import Path
import json
import re

ROOT = Path(__file__).resolve().parents[1]
NAME = 'xiantu_wondrous_fire'

def sync(root=ROOT):
    records=[]
    for file in sorted((root/'src/main/resources/data/xiantu/loot_tables/chests').rglob('*.json')):
        name=file.stem
        if not (name.startswith(('grand_', 'ruin_','tier_','boss_'))):
            continue
        tier=re.match(r'tier_(\d+)_',name)
        rank=min(6,int(tier[1])) if tier else 6 if name.startswith('boss_') or '_core_' in name else 5 if name.startswith('grand_vault_') else 4 if name.startswith('grand_cache_') else 2
        chance=.15 if name.startswith('boss_') or '_core_' in name else .10 if '_vault_' in name else .06
        data=json.loads(file.read_text(encoding='utf8'))
        pool={'name':NAME,'rolls':1,'conditions':[{'condition':'minecraft:random_chance','chance':chance}],
              'entries':[{'type':'minecraft:item','name':f'xiantu:wondrous_fire_{r}','weight':3 if r==rank else 1} for r in range(max(1,rank-1),rank+1)]}
        pools=[p for p in data.get('pools',[]) if p.get('name')!=NAME]+[pool]
        if data.get('pools')!=pools:
            data['pools']=pools
            file.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
        records.append({'table':file.relative_to(root/'src/main/resources').as_posix(),'maxRank':rank,'chance':chance})
    return records

if __name__=='__main__':
    records=sync()
    print(f'Synchronized {len(records)} ruin fire pools; rerunning preserves existing pools exactly.')
