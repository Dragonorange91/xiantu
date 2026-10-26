"""Snapshot current accepted runtime once, then compile new editable VoxelBuilder maps.
No installed JAR, live world or legacy authoring output is mutated by this tool.
"""
from pathlib import Path
import json,shutil,sys,nbtlib,hashlib
from nbtlib import tag as T
P=Path(__file__).resolve().parents[1];R=P/'src/main/resources';D=R/'data/xiantu';O=P/'asset_sources/ruins-rework-20261024'
def dump(p,v):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(v,ensure_ascii=False),encoding='utf8')
def state(s):
    v={'Name':T.String(s.split('[')[0])}
    if '['in s:v['Properties']=T.Compound({k:T.String(x) for k,x in (p.split('=')for p in s.split('[')[1].rstrip(']').split(','))})
    return T.Compound(v)
def statestring(s):return str(s['Name'])+('['+','.join(f'{k}={v}'for k,v in s['Properties'].items())+']'if 'Properties'in s else '')
def read(p):
    f=nbtlib.load(p);pal=[statestring(s)for s in f['palette']]
    return list(map(int,f['size'])),[[*map(int,b['pos']),pal[int(b['state'])]]for b in f['blocks']]
def write(p,size,blocks):
    palette=sorted({c[3]for c in blocks});idx={s:i for i,s in enumerate(palette)}
    f=nbtlib.File({'DataVersion':T.Int(3465),'size':T.List[T.Int](size),'palette':T.List[T.Compound]([state(s)for s in palette]),'blocks':T.List[T.Compound]([T.Compound({'pos':T.List[T.Int](c[:3]),'state':T.Int(idx[c[3]])})for c in blocks]),'entities':T.List[T.Compound]([])})
    p.parent.mkdir(parents=True,exist_ok=True);f.save(p,gzipped=True)
def main():
    if '--snapshot'in sys.argv:
        for p in (D/'ruin_blueprints').glob('*.json'):
            out=O/'baseline'/p.name
            if out.exists():continue
            v=json.loads(p.read_text(encoding='utf8'));size,blocks=read(D/f'structures/ruins/{p.stem}.nbt');assert size==v['size'];v['blocks']=blocks;dump(out,v)
            d=O/'runtime-backup';d.mkdir(exist_ok=True);shutil.copy2(p,d/p.name);shutil.copy2(D/f'structures/ruins/{p.stem}.nbt',d/(p.stem+'.nbt'))
        print('20 original runtime templates frozen, including metadata; no source generator replay.')
        sys.exit()
    selected={arg for arg in sys.argv[1:] if not arg.startswith('--')}
    manifest_path=O/'manifest.json'
    manifest_by_id={v['id']:v for v in json.loads(manifest_path.read_text(encoding='utf8'))} if manifest_path.exists() else {}
    for p in (O/'structures').glob('*.json'):
        if selected and p.stem not in selected:continue
        v=json.loads(p.read_text(encoding='utf8'));blocks=v.pop('blocks');bid=v['id'];write(D/f'structures/ruins/{bid}.nbt',v['size'],blocks);dump(D/f'ruin_blueprints/{bid}.json',v)
        manifest_by_id[bid]={'id':bid,'size':v['size'],'cells':len(blocks),'sha256':hashlib.sha256((D/f'structures/ruins/{bid}.nbt').read_bytes()).hexdigest()}
    dump(manifest_path,list(manifest_by_id.values()))
    print(f'Compiled {len(selected) or len(manifest_by_id)} revised native templates; original loot tables retained.')

if __name__=='__main__':main()
