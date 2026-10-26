"""Expand furniture anchors into real complete per-cell assemblies in templates."""
import json,re
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
MANIFEST=ROOT/'src/main/resources/data/xiantu/xian_furniture.json'

def assemble(cells,reserved=()):
    dims={v['id']:v['dimensions']for v in json.loads(MANIFEST.read_text(encoding='utf8'))['models']if v.get('dimensions',[1,1,1])!=[1,1,1]}
    planned=[(p,s)for p,s in cells.items()if s.split(':')[-1].split('[')[0]in dims and 'part='not in s]
    for p,s in planned:cells[p]='minecraft:air'
    reserved=set(reserved);report=[]
    for p,s in planned:
        id=s.split(':')[-1].split('[')[0];w,h,d=dims[id];f=re.search(r'facing=(\w+)',s);f=f.group(1)if f else'north';first=['north','east','south','west'].index(f)
        offsets=[(0,0)]+[(dx,dz)for r in [1,2]for dx in range(-r,r+1)for dz in range(-r,r+1)if max(abs(dx),abs(dz))==r]
        accepted=False
        for dx,dz in offsets:
          if accepted:break
          for turn in [first,(first+1)%4,(first+3)%4,(first+2)%4]:
            parts=[]
            for x in range(w):
             for y in range(h):
              for z in range(d):
               X,Z=[(x,z),(-z,x),(-x,-z),(z,-x)][turn];at=(p[0]+dx+X,p[1]+y,p[2]+dz+Z);parts.append((at,(x,y,z)))
            if any(at in reserved or cells.get(at,'minecraft:air')!='minecraft:air'for at,_ in parts):continue
            if any(cells.get((at[0],at[1]-1,at[2]),'minecraft:air').split('[')[0]in ['minecraft:air','minecraft:water']for at,part in parts if part[1]==0):continue
            for at,part in parts:cells[at]=f'xiantu:{id}[facing={['north','east','south','west'][turn]},part={part[0]}_{part[1]}_{part[2]}]'
            accepted=True;report.append({'id':id,'from':p,'anchor':[p[0]+dx,p[1],p[2]+dz],'parts':len(parts)});break
        if not accepted:report.append({'id':id,'from':p,'omitted':'no supported unobstructed footprint'})
    return report

def main():
    folder=ROOT/'asset_sources/ruins-rework-20261024/structures';report={}
    for file in folder.glob('*.json'):
        b=json.loads(file.read_text(encoding='utf8'));cells={tuple(v[:3]):v[3]for v in b['blocks']};reserved=[]
        for m in b['markers']:
            if m['type']=='reward'and m.get('approach'):reserved.extend((m['approach'][0],m['approach'][1]+y,m['approach'][2])for y in [0,1])
        report[b['id']]=assemble(cells,reserved);b['blocks']=[[*p,s]for p,s in cells.items()];file.write_text(json.dumps(b,ensure_ascii=False),encoding='utf8')
    (folder.parent/'furniture-assemblies.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf8')
    print('Ruin furniture assemblies',sum(len(v)for v in report.values()),'omitted',sum('omitted'in r for v in report.values()for r in v))
if __name__=='__main__':main()
