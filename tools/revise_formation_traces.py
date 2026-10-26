"""Repack approved API artwork into reciprocal trace tiles; no new painted artwork.

Texture phase precedes native Blockbench export. Keep accepted pillars/cushion untouched.
"""
from pathlib import Path
from itertools import combinations
import argparse, json, math, shutil
from PIL import Image, ImageDraw

ROOT=Path(__file__).resolve().parents[1]
OLD=ROOT/'asset_sources/formation-20261011'
OUT=ROOT/'asset_sources/formation-traces-20261011'
ASSETS=ROOT/'src/main/resources/assets/xiantu'
DIRS=[(0,-1),(1,-1),(1,0),(1,1),(0,1),(-1,1),(-1,0),(-1,-1)]
PAIRS=list(combinations(range(8),2))

def write(path,obj):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(obj,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')

def ring(r):
    return sorted([(x,z) for x in range(-r,r+1) for z in range(-r,r+1) if (r-1)**2<x*x+z*z<=r*r],key=lambda p:math.atan2(p[1],p[0]))

def trace_ids(r):
    cells=ring(r);result={}
    for i,p in enumerate(cells):
        neighbors=[cells[(i-1)%len(cells)],cells[(i+1)%len(cells)]]
        ports=tuple(sorted(DIRS.index((n[0]-p[0],n[1]-p[1])) for n in neighbors))
        result[p]=PAIRS.index(ports)+1
    return result

def prepare():
    target=OUT/'textures';target.mkdir(parents=True,exist_ok=True)
    manifest=[]
    for tier in (1,2,3):
        stone=Image.open(OLD/'textures'/f'array_{tier}_stone.png').convert('RGBA')
        source=Image.open(OLD/'textures'/f'array_{tier}_base_top.png').convert('RGBA')
        # Six-pixel approved copper/silver track, free of the central medallion.
        strip=source.crop((29,0,35,24))
        arms=[]
        for dx,dz in DIRS:
            length=math.hypot(dx,dz);vx,vz=dx/length,dz/length;ux,uz=-vz,vx
            reach=31.5*length
            coeff=(ux,uz,3-31.5*(ux+uz),-24*vx/reach,-24*vz/reach,24+24*31.5*(vx+vz)/reach)
            arms.append(strip.transform((64,64),Image.Transform.AFFINE,coeff,Image.Resampling.NEAREST))
        for kind in ('base','rune'):
            center=Image.open(OLD/'textures'/f'array_{tier}_{kind}_top.png').convert('RGBA')
            # Preserve the approved seal, crop away its old four fixed exits.
            ornament=center.crop((24,24,40,40)) if kind=='base' else center.crop((17,17,47,47))
            # Keep only inlaid metal from the approved seal. Its square stone
            # backdrop otherwise erases diagonal arms and creates new gaps.
            for y in range(ornament.height):
                for x in range(ornament.width):
                    rr,gg,bb,aa=ornament.getpixel((x,y))
                    metal=(rr>gg*1.12 and rr>bb*1.15) if tier<3 else (rr>=gg*.8 and gg>=bb*.85 and max(rr,gg,bb)>100)
                    if not metal:ornament.putpixel((x,y),(rr,gg,bb,0))
            for trace,(a,b) in enumerate(PAIRS,1):
                tile=stone.copy()
                tile.alpha_composite(arms[a]);tile.alpha_composite(arms[b])
                tile.alpha_composite(ornament,((64-ornament.width)//2,(64-ornament.height)//2))
                stem=f'array_{tier}_{kind}_trace_{trace}'
                tile.save(target/f'{stem}.png')
                manifest.append(dict(texture=stem,ports=[a,b],size=[64,64],state='pending_inspection',
                    provenance=str(OLD/'masters'/f'array_material_{tier}_1024.png'),operation='approved artwork crop/UV affine repack'))
    write(OUT/'texture-manifest.json',manifest)
    sheet=Image.new('RGB',(1408,1408),(24,25,28));draw=ImageDraw.Draw(sheet)
    for tier,(outer,inner) in enumerate([(4,2),(7,3),(10,4)],1):
        scale=28 if tier==3 else 40
        side=(outer*2+1)*scale
        panel=Image.new('RGBA',(side,side),(25,28,30,255))
        for kind,r in [('base',outer),('rune',inner)]:
            for (x,z),trace in trace_ids(r).items():
                tile=Image.open(target/f'array_{tier}_{kind}_trace_{trace}.png').resize((scale,scale),Image.Resampling.NEAREST)
                panel.paste(tile,((x+outer)*scale,(z+outer)*scale))
        p=(20,40) if tier==1 else (420,40) if tier==2 else (420,720)
        sheet.paste(panel,p);draw.text((p[0],p[1]-22),f'Tier {tier}: unchanged functional ring footprint',fill='white')
    sheet.save(OUT/'ring-review.png')
    print('Prepared',len(manifest),'repacked64px tiles. Review:',OUT/'ring-review.png',flush=True)

def models():
    from blockbench_bridge import Bridge
    import build_formation_models as native
    from build_building_openings_blockbench import save_export
    entries=json.loads((OUT/'texture-manifest.json').read_text(encoding='utf-8'))
    assert all(e['state']=='inspected_for_integration' for e in entries),'Inspect complete texture batch first'
    for e in entries:shutil.copy2(OUT/'textures'/(e['texture']+'.png'),ASSETS/'textures/block'/(e['texture']+'.png'))
    b=Bridge();schemas=b.request('tools/list',{})['tools'];write(OUT/'blockbench-tools.json',schemas)
    if not (OUT/'preserved-project.bbmodel').exists():save_export(b,'project',OUT/'preserved-project.bbmodel')
    native.MODELS=OUT/'models'
    native.MODELS.mkdir(parents=True,exist_ok=True)
    for tier in (1,2,3):
        for kind in ('base','rune'):
            name=f'array_{tier}_{kind}_connected'
            # Re-export the approved native geometry, using plain stone on top.
            native.model(b,name,native.floor('stone'),tier)
            path=ASSETS/'models/block'/f'{name}.json';model=json.loads(path.read_text())
            top=model['elements'][-1]['faces']['up'];model['textures']['trace']=f'xiantu:block/array_{tier}_{kind}_trace_1'
            top['texture']='#trace';top['uv']=[0,0,16,16]
            for i,e in enumerate(model['elements']):
                if i>0:e['faces'].pop('down',None)
                if i<len(model['elements'])-1:e['faces'].pop('up',None)
                for face in e['faces']:e['faces'][face]['cullface']=face
            # Same geometry and face assignments also stored natively, with default trace imported.
            from build_building_openings_blockbench import texture
            texture(b,'trace',OUT/'textures'/f'array_{tier}_{kind}_trace_1.png')
            texids={t['name']:t['uuid'] for t in b.value(b.call('list_textures'))}
            result=b.value(b.call('risky_eval',code='(()=>{const c=Cube.all.find(c=>c.name==="etched_conductive_plate");Undo.initEdit({elements:[c]});c.faces.up.texture='+json.dumps(texids['trace'])+';c.faces.up.uv=[0,0,16,16];Canvas.updateAllFaces();Undo.finishEdit("Assign connected trace");return {updated:1};})()'))
            assert isinstance(result,dict) and result.get('updated')==1,result
            save_export(b,'project',native.MODELS/f'{name}.bbmodel')
            write(path,model);write(native.MODELS/f'{name}.json',model)
            for trace in range(1,29):
                write(ASSETS/'models/block'/f'array_{tier}_{kind}_trace_{trace}.json',{'parent':f'xiantu:block/{name}',
                    'textures':{'trace':f'xiantu:block/array_{tier}_{kind}_trace_{trace}'}})
            block=f'array_base_{tier}' if kind=='base' else f'array_rune_brick_{tier}'
            variants={f'trace=0,facing={f}':{'model':f'xiantu:block/array_{tier}_{kind}','y':angle}
                      for f,angle in [('north',0),('east',90),('south',180),('west',270)]}
            variants.update({f'trace={n}':{'model':f'xiantu:block/array_{tier}_{kind}_trace_{n}'} for n in range(1,29)})
            write(ASSETS/'blockstates'/f'{block}.json',{'variants':variants})
    print('Exported6 editable native models;168 inherited texture variants',flush=True)

if __name__=='__main__':
    ap=argparse.ArgumentParser();ap.add_argument('--models',action='store_true');args=ap.parse_args()
    models() if args.models else prepare()
