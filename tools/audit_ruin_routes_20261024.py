"""Independent conservative integer-voxel walking audit; not visual acceptance.
Two blocks of headroom, supporting floor and one-block steps; water permits swimming.
Absent cells in underground blueprints are natural rock, not free air.
"""
from pathlib import Path
import json,sys
from collections import deque
import numpy as np
from package_ruins_20261024 import read
P=Path(__file__).resolve().parents[1];O=P/'asset_sources/ruins-rework-20261024'
PASS=['air','water','rail','grass','fern','vine','herb_','flower','torch']
def audit(p):
 b=json.loads(p.read_text(encoding='utf8'))
 if 'blocks'not in b:
  size,b['blocks']=read(P/f'src/main/resources/data/xiantu/structures/ruins/{p.stem}.nbt')
  assert size==b['size'],p.stem
 w,h,d=b['size'];under=b['placement']['mode']=='underground'
 solid=np.full((w,h,d),under,dtype=bool);water=np.zeros_like(solid);cells={tuple(c[:3]):c[3]for c in b['blocks']}
 for (x,y,z),s in cells.items():
  name=s.split(':')[-1].split('[')[0]
  solid[x,y,z]=not(any(name==v or (v=='herb_' and name.startswith(v))for v in PASS) or name.endswith('torch'))
  water[x,y,z]=name=='water'
 free=~solid;walk=np.zeros_like(solid);walk[:,1:-1,:]=free[:,1:-1,:]&free[:,2:,:]&(solid[:,:-2,:]|water[:,1:-1,:])
 def valid(p):return all(0<=p[k]<[w,h,d][k]for k in range(3))and walk[tuple(p)]
 def near(p,r=3):
  if valid(p):return tuple(p)
  poss=[(abs(x-p[0])+abs(y-p[1])+abs(z-p[2]),x,y,z)for x in range(max(0,p[0]-r),min(w,p[0]+r+1))for y in range(max(1,p[1]-r),min(h-1,p[1]+r+1))for z in range(max(0,p[2]-r),min(d,p[2]+r+1))if walk[x,y,z]]
  return min(poss)[1:]if poss else None
 start=near(b['entry'],4);seen=set();q=deque()
 if start:seen.add(start);q.append(start)
 while q:
  x,y,z=q.popleft()
  for dx,dz in [(1,0),(-1,0),(0,1),(0,-1)]:
   X,Z=x+dx,z+dz
   if not(0<=X<w and 0<=Z<d):continue
   for dy in [0,1,-1]:
    Y=y+dy
    if not(1<=Y<h-1)or not walk[X,Y,Z]:continue
    if dy==1 and solid[x,y+2,z]:continue
    if dy==-1 and solid[X,y+1,Z]:continue
    at=(X,Y,Z)
    if at not in seen:seen.add(at);q.append(at)
   if water[x,y,z]:
    for Y in [y-1,y+1]:
     if 0<Y<h-1 and water[x,Y,z] and (x,Y,z)not in seen:seen.add((x,Y,z));q.append((x,Y,z))
 targets=[]
 for m in b['markers']:
  if m['type']!='reward':continue
  at=tuple(m['position']);a=m.get('approach', [at[0],at[1],at[2]+1]);v=near(a,1)
  targets.append({'id':m['id'],'position':list(at),'approach':a,'block':cells.get(at),'walk':v,'reachable':v in seen})
 guards=[]
 for e in b['encounters']:
  at=e['position'];v=near(at,2);guards.append({'id':e['id'],'position':at,'walk':v,'reachable':v in seen,'block':cells.get(tuple(at))})
 return dict(id=b['id'],start=start,walkable=int(walk.sum()),reached=len(seen),rewards=targets,guards=guards,failed=[v for v in targets+guards if not v['reachable']])
def main():
 report=[]
 authored='--authored'in sys.argv;selected=[s for s in sys.argv[1:]if not s.startswith('--')]
 folder=O/'structures'if authored else P/'src/main/resources/data/xiantu/ruin_blueprints'
 for p in sorted(folder.glob('*.json')):
  if selected and p.stem not in selected:continue
  v=audit(p);report.append(v);print(v['id'],v['reached'],'/',v['walkable'],'FAIL',len(v['failed']),flush=True)
  for f in v['failed']:print(' ',f['id'],f['position'],'near',f['walk'],flush=True)
 (O/'route-audit.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf8')
 assert report and all(not v['failed']for v in report),'Unreachable ruin encounter or chest approach'
if __name__=='__main__':main()
