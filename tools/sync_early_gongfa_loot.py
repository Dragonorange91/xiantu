"""Add early cultivation books without replacing existing treasure pools."""
from pathlib import Path
import argparse,json,re

ROOT=Path(__file__).resolve().parents[1]
DATA=ROOT/'src/main/resources/data/xiantu'
POOL='xiantu_early_gongfa_inheritance'
BOOKS={
    1:dict(metal='jin_guang_jue',wood='qing_mu_jue',water='xuan_shui_jue',fire='chi_yan_jue',earth='yan_tu_jue'),
    2:dict(metal='geng_jin_ning_yuan_jue',wood='qing_mu_gui_yuan_jue',water='cang_lan_zhen_jing',fire='li_huo_zhen_jing',earth='shan_yue_zhen_jing'),
    3:dict(metal='jin_guang_xuan_gong',wood='bai_cao_zhen_jing',water='you_shui_zhen_jing',fire='fen_chi_chi_huo',earth='qing_shan_zhen_di'),
}

def pool(tier,grade,theme):
    return dict(name=POOL,rolls=1,conditions=[dict(condition='minecraft:random_chance',chance={1:.12,2:.09,3:.06}[tier]*(2 if grade=='core' else 1))],
        entries=[dict(type='minecraft:item',name='xiantu:'+book,weight=3 if element==theme else 1) for element,book in BOOKS[tier].items()])

def sync(check=False):
    referenced=set()
    for file in (DATA/'ruin_blueprints').glob('*.json'):
        spec=json.loads(file.read_text('utf8'))
        referenced.update(m.get('loot','') for m in spec.get('markers',[]))
    reached=set();rows=[]
    registry=(ROOT/'src/main/java/com/longju/xiantu/gongfa/GongfaRegistry.java').read_text('utf8')
    for file in sorted((DATA/'loot_tables/chests/ruins').glob('tier_*.json')):
        match=re.fullmatch(r'tier_([123])_(cache|core)_(metal|wood|water|fire|earth|thunder)',file.stem)
        if not match:continue
        tier,grade,theme=match.groups();tier=int(tier)
        definition=pool(tier,grade,theme);table=json.loads(file.read_text('utf8'))
        for book in BOOKS[tier].values():assert 'method("'+book+'",' in registry,book
        if check:
            actual=[p for p in table['pools'] if p.get('name')==POOL]
            assert actual==[definition],file
        else:
            table['pools']=[p for p in table['pools'] if p.get('name')!=POOL]+[definition]
            file.write_text(json.dumps(table,ensure_ascii=False,indent=2)+'\n','utf8')
        loot='xiantu:chests/ruins/'+file.stem
        if loot in referenced:reached.update(BOOKS[tier].values())
        rows.append(dict(table=loot,chance=definition['conditions'][0]['chance'],referenced=loot in referenced))
    assert len(rows)==19,len(rows)
    assert reached=={b for group in BOOKS.values() for b in group.values()},'Unreachable early book'
    return rows

if __name__=='__main__':
    args=argparse.ArgumentParser();args.add_argument('--check',action='store_true');options=args.parse_args()
    rows=sync(options.check)
    print(('Verified' if options.check else 'Updated')+f' {len(rows)} loot tables; all15 early books reachable from actual ruin markers')
