"""Edit the real complete template and standalone shop blueprints from a frozen baseline.
Every workstation is a real complete multicell assembly, not an oversized model anchor.
"""
from pathlib import Path
import json,shutil,nbtlib
from nbtlib import tag as T
from package_ruins_20261024 import state,statestring
from assemble_xian_furniture import assemble
P=Path(__file__).resolve().parents[1];D=P/'src/main/resources/data/xiantu';O=P/'asset_sources/ruins-rework-20261024/town';O.mkdir(parents=True,exist_ok=True)
def load(name):
    src=D/f'structures/town/{name}.nbt';baseline=O/(name+'.nbt')
    if not baseline.exists():shutil.copy2(src,baseline)
    f=nbtlib.load(baseline);pal=[statestring(s)for s in f['palette']]
    return f,{tuple(map(int,b['pos'])):(pal[int(b['state'])],b.get('nbt'))for b in f['blocks']}
def save(name,f,cells):
    pal=sorted({v[0]for v in cells.values()});idx={s:i for i,s in enumerate(pal)}
    f['palette']=T.List[T.Compound]([state(s)for s in pal]);blocks=[]
    for at,(s,nbt)in cells.items():
        b=T.Compound({'pos':T.List[T.Int](at),'state':T.Int(idx[s])})
        if nbt is not None:b['nbt']=nbt
        blocks.append(b)
    f['blocks']=T.List[T.Compound](blocks);f.save(D/f'structures/town/{name}.nbt',gzipped=True)
def workstation(cells,id,x,y,z,table=False):
    for dx in range(2):
        for dy in range(1 if table else 2):
            for dz in range(1 if table else 2):cells[x+dx,y+dy,z+dz]=(f'xiantu:{id}[facing=north,part={dx}_{dy}_{dz}]',None)
def dress(name,cells):
    # Existing wet/coal display props are replaced by functioning tier-one machines.
    if name=='alchemy_pavilion':
        for x in range(6,12):
            for z in range(14,25):
                for y in range(2,5):cells[x,y,z]=('minecraft:air',None)
        for x in range(31,37):
            for z in range(14,25):
                for y in range(2,5):cells[x,y,z]=('minecraft:air',None)
        workstation(cells,'qingmu_alchemy_furnace',8,2,17);workstation(cells,'qingmu_alchemy_furnace',33,2,19)
        for x,z,id in [(7,23,'xian_wood_cabinet'),(34,15,'xian_display_shelf'),(12,22,'ruin_weathered_herb_rack'),(29,22,'ruin_weathered_herb_rack')]:cells[x,2,z]=('xiantu:'+id+'[facing=north]',None)
    elif name=='forging_shop':
        for x in range(29,39):
            for z in range(13,26):
                for y in range(1,5):cells[x,y,z]=('minecraft:air',None)
        for x in range(29,39):
            for z in range(13,26):cells[x,0,z]=('minecraft:stone_bricks',None)
        workstation(cells,'lifeng_refining_furnace',32,1,22);workstation(cells,'artisan_forging_table',33,1,15,True)
        for x,z in [(30,14),(36,23)]:cells[x,1,z]=('xiantu:xian_display_shelf[facing=west]',None)
    elif name=='residence_small':
        for x in [7,8,9]:cells[x,2,9]=('minecraft:air',None)
        for x,y,z,id,facing in [(8,2,9,'xian_writing_desk','north'),(8,2,8,'xian_wood_chair','south'),(10,2,11,'xian_bookshelf','north'),(12,2,6,'xian_wood_cabinet','west'),(4,2,8,'xian_wood_table','north'),(11,2,8,'xian_wood_bench','west')]:
            cells[x,y,z]=('xiantu:'+id+'[facing='+facing+']',None)
        cells[8,2,7]=('minecraft:air',None)
        cells[12,2,6]=('xiantu:xian_wood_wardrobe[facing=west]',None)
    elif name=='talisman_workshop':
        for x,y,z,id in [(8,2,11,'xian_bookshelf'),(25,2,12,'xian_display_shelf'),(8,2,26,'xian_wood_cabinet')]:
            cells[x,y,z]=('xiantu:'+id+'[facing=north]',None)
    for at,(s,n)in list(cells.items()):
        if s.startswith('minecraft:lantern[')and 'hanging=true'in s:cells[at]=('xiantu:xian_red_lantern[facing=north]',None)
    simple={p:s for p,(s,n)in cells.items()};assemble(simple)
    for p,s in simple.items():
        if p not in cells or cells[p][0]!=s:cells[p]=(s,None)
def main():
    layout=json.loads((D/'town-layout.json').read_text(encoding='utf8'));town,tcells=load('cultivation_town')
    for name in ['alchemy_pavilion','forging_shop','residence_small','talisman_workshop']:
        f,cells=load(name);dress(name,cells);save(name,f,cells)
        for origin in [p for p in layout['placements']if p['id']==name]:
          ox,oy,oz=origin['origin'];turns=origin['rotation']//90;w,h,d=map(int,f['size'])
        # Update only modified cells in the placed building, preserving accepted town lighting.
          _,old=load(name)
          for (x,y,z),(s,n)in cells.items():
            if old.get((x,y,z))==(s,n):continue
            X,Z=x,z
            if turns==2:X,Z=w-1-x,d-1-z
            elif turns==1:X,Z=d-1-z,x
            elif turns==3:X,Z=z,w-1-x
            if '[facing='in s:
                for facing in ['north','east','south','west']:
                    if 'facing='+facing in s:s=s.replace('facing='+facing,'facing='+['north','east','south','west'][(['north','east','south','west'].index(facing)+turns)%4]);break
            tcells[ox+X,oy+y,oz+Z]=(s,n)
    # Replace hanging lanterns in homes and porticoes, retaining their exact support locations.
    for at,(s,n)in list(tcells.items()):
        if s.startswith('minecraft:lantern[')and 'hanging=true'in s:tcells[at]=('xiantu:xian_red_lantern[facing=north]',None)
    save('cultivation_town',town,tcells)
    # Keep the older chunk-library exports byte-consistent in content too.
    by_tile={}
    for (x,y,z),value in tcells.items():
        key=(x//16,y//32,z//16)
        by_tile.setdefault(key,{})[x%16,y%32,z%16]=value
    for path in (D/'structures/town/tiles').glob('cell_*.nbt'):
        _,x,y,z=path.stem.split('_');key=tuple(map(int,(x,y,z)))
        backup=O/'tiles'/path.name;backup.parent.mkdir(exist_ok=True)
        if not backup.exists():shutil.copy2(path,backup)
        f=nbtlib.load(backup)
        save('tiles/'+path.stem,f,by_tile.get(key,{}))
    print('丹阁/炼器铺 standalone and full-town templates updated with complete real workstation assemblies.')

if __name__=='__main__':main()
