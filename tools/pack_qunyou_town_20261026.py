from pathlib import Path
import json,shutil,nbtlib
from nbtlib import tag as T
from package_ruins_20261024 import state,statestring
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'asset_sources/qunyou-figurine-20261026';D=ROOT/'src/main/resources/data/xiantu/structures/town'
for entry in json.loads((OUT/'town-placement.json').read_text()):
 p=D/(entry['template']+'.nbt');backup=OUT/'before'/p.relative_to(D);backup.parent.mkdir(parents=True,exist_ok=True)
 if not backup.exists():shutil.copy2(p,backup)
 f=nbtlib.load(backup);pal=[statestring(s) for s in f['palette']];pos=entry['pos'];found=False
 below=next(b for b in f['blocks'] if list(map(int,b['pos']))==[pos[0],pos[1]-1,pos[2]])
 assert pal[int(below['state'])].startswith('minecraft:dark_oak_slab[type=top'),pal[int(below['state'])]
 for x,y,z,s in entry['blocks']:
  if s not in pal:pal.append(s);f['palette'].append(state(s))
  for block in f['blocks']:
   if list(map(int,block['pos']))==[x,y,z]:
    assert pal[int(block['state'])]=='minecraft:air';block['state']=T.Int(pal.index(s));found=True;break
  if not found:f['blocks'].append(T.Compound({'pos':T.List[T.Int]([x,y,z]),'state':T.Int(pal.index(s))}))
 f.save(p,gzipped=True);print('Placed',entry['template'],pos)
