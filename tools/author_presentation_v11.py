"""Reproducible native Photon atlas/GUI integration from user-owned references.
Never executes old authoring entry points; preserves authored combat timing.
"""
from pathlib import Path
import copy, json, hashlib, importlib.util, math, shutil
from PIL import Image, ImageOps, ImageEnhance
import nbtlib as n
ROOT=Path(__file__).resolve().parents[1]
A=ROOT/'src/main/resources/assets/xiantu'
OUT=ROOT/'asset_sources/presentation-20261024'
BACK=OUT/'before'
def backup(p):
    q=BACK/p.relative_to(ROOT)
    if p.exists() and not q.exists():q.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(p,q)
def saveim(im,p):
    backup(p);p.parent.mkdir(parents=True,exist_ok=True);im.save(p)
spec=importlib.util.spec_from_file_location('v9',ROOT/'tools/author_photon_v9.py')
v=importlib.util.module_from_spec(spec);spec.loader.exec_module(v)
REF=(Path.home() / 'Desktop/法术技能特效参考/国风修仙技能特效spine骨骼动画源文件仙侠UI场景光效CG美术素材/战斗特效')
SEQS={'fire':('E54131','A_fire_ball_*'),'thunder':('E54080','A_thunder_big_*'),
 'dust':('E54181','A_smoke_L_*'),'qi':('E54181','A_qixuan_*'),
 'blade':('E53048','nezha*'),'burst':('E54015','huabao*')}
manifest=[]
for key,(folder,pattern) in SEQS.items():
    files=sorted((REF/folder/'images').glob(pattern))
    files=[p for p in files if p.suffix.lower()=='.png'];assert files,(key,folder)
    ims=[Image.open(p).convert('RGBA') for p in files]
    # One canvas and one scale for the full sequence: retain motion/pivot.
    w=max(i.width for i in ims);h=max(i.height for i in ims)
    frames=32 if len(ims)>16 else 16;atlas=Image.new('RGBA',(512,128*(frames//4)))
    for i in range(frames):
        src=ims[round(i*(len(ims)-1)/(frames-1))]
        canvas=Image.new('RGBA',(w,h));canvas.paste(src,((w-src.width)//2,(h-src.height)//2))
        canvas.thumbnail((120,120),Image.Resampling.LANCZOS)
        atlas.paste(canvas,(i%4*128+(128-canvas.width)//2,i//4*128+(128-canvas.height)//2))
    path=A/f'textures/vfx/v11/{key}.png';saveim(atlas,path)
    manifest.append({'texture':str(path.relative_to(A)), 'frames':frames,'sources':[str(p) for p in files],
      'sha256':hashlib.sha256(path.read_bytes()).hexdigest()})
def p(name,tex='qi',**kw):
    o=v.particle('v11 '+name,'burst','FFFFFF',**kw);cfg=o['config']
    cfg['material']['material']['texture']=n.String(f'xiantu:textures/vfx/v11/{tex}.png')
    cfg['uvAnimation']['enable']=n.Byte(1)
    count=32 if tex in ('thunder','burst') else 16
    cfg['uvAnimation']['tiles']=n.Compound({'a':n.Int(4),'b':n.Int(count//4)})
    cfg['uvAnimation']['cycle']=n.Float(count)
    cfg['uvAnimation']['frameOverTime']=v.curve([0,0,.33,.33,.66,.66,1,.999])
    return o
effects=[]
def fx(ident,objects):
    root=copy.deepcopy(v.ROOT_TEMPLATE);root['fx']['mainFX']['fxObjects']=n.List[n.Compound](objects);root['fx']['subFXs']=n.Compound()
    path=A/f'fx/{ident}.fx';backup(path);path.parent.mkdir(parents=True,exist_ok=True);n.File(root,gzipped=True).save(path)
    effects.append('xiantu:'+ident)
for spell in ('fire_bolt','thunder_strike'):
    tex='fire' if spell=='fire_bolt' else 'thunder'
    fx(f'skills/{spell}/charge',[p(spell+' gathering',tex,size=.48,life=8,duration=16,rate=.18,pos=(0,1.2,.45),space='Local'),
       p(spell+' inward wisps','qi',size=.7,life=12,duration=12,rate=.16,pos=(0,1.2,.45),spin=.04,space='Local')])
    fx(f'skills/{spell}/impact',[p(spell+' contact', 'burst' if tex=='fire' else tex,size=2.1,life=20),
       p(spell+' discharge','qi',size=2.4,life=16,expand=True),p(spell+' embers',tex,size=.25,life=10,duration=3,rate=2,speed=.09,radius=.2)])
fx('skills/fire_bolt/flight',[p('fire sphere','fire',size=.7,life=8,duration=24,rate=.18,space='Local'),
 p('fire wake','burst',size=.32,life=7,duration=24,rate=.6,radius=.07)])
fx('skills/thunder_strike/release',[p('descending branched thunder','thunder',size=(2.7,5.5,2.7),life=14,pos=(0,2.0,0)),
 p('second discharge','thunder',size=(1.8,4.3,1.8),life=12,delay=4,pos=(.2,1.7,0)),p('contact corona','qi',size=2.5,life=16,pos=(0,.1,0),expand=True)])
# Equipment abilities retain their own spatial footprint and true release timing.
fx('skills/staff_extend/release',[p(f'staff forward golden cut {i}','blade',size=(2.4,2.4,2.4),life=12,pos=(0,1,2+i*2),delay=i) for i in range(5)])
fx('skills/staff_quake/release',[p('staff heavy impact','dust',size=5.5,life=22,pos=(0,.2,3),expand=True),
 p('staff gold shock','blade',size=4.8,life=14,pos=(0,.4,3)),p('staff rising debris','dust',size=.6,life=15,duration=4,rate=3,radius=2,speed=.08,pos=(0,.4,3))])
fx('skills/cloud_step/release',[p('cloud departure','qi',size=2.3,life=20,pos=(0,.2,0),spin=.04),p('cloud drift','dust',size=1.7,life=20,pos=(0,.1,0))])
fx('skills/great_sage_form/release',[p('sage ascension','blade',size=(3.5,4.5,3.5),life=28,pos=(0,1.2,0)),p('sage mist','qi',size=4,life=24,spin=-.04)])
fx('skills/seventy_two_forms/release',[p('transformation cloud','qi',size=2.8,life=24,pos=(0,1,0),spin=.07),p('transformation sparks','blade',size=.4,life=12,duration=4,rate=2,radius=.4,speed=.06,pos=(0,1,0))])
fx('skills/weapon_fire/impact',[p('true fire proc','burst',size=1.6,life=18)])
fx('skills/weapon_earth/impact',[p('heavy weapon proc','dust',size=2.5,life=18,expand=True)])
fx('skills/weapon_metal/impact',[p('cutting proc','blade',size=1.5,life=12)])
fx('skills/weapon_charge/release',[p('charged weapon','blade',size=1.4,life=15,pos=(0,1,.7))])
# Long-lived restrained formation: several counter-rotating layers, not a shower.
ring=p('formation circulation','qi',size=5,life=65,duration=1200,rate=.025,pos=(0,.15,0),mode='Horizontal',space='Local',spin=.025)
fx('formation/gather',[ring,p('formation higher counter flow','qi',size=3.6,life=75,duration=1200,rate=.02,pos=(0,.65,0),space='Local',spin=-.018),
 p('formation gathering mist','qi',size=1.3,life=42,duration=1200,rate=.1,pos=(0,.6,0),radius=1.3,speed=.012),
 p('formation golden motes','blade',size=.13,life=35,duration=1200,rate=.09,pos=(0,.25,0),radius=2,speed=.016)])
# Upgrade already-wired creature/boss subjects in place; do not alter damage/timing.
replacements={'fireball':'fire','flame':'burst','lightning':'thunder','blade':'blade','smoke':'dust','vortex':'qi'}
changed=0
for path in (A/'fx/beasts').rglob('*.fx'):
    if any(s in path.parts for s in ('battle_general_soul','xuantians_remnant','cloud_sea_lord')):continue
    root=n.load(path);dirty=False
    for obj in root['fx']['mainFX']['fxObjects']:
        cfg=obj.get('config');
        if cfg is None:continue
        mat=cfg.get('material',{}).get('material',{});old=str(mat.get('texture',''))
        if '/v9/' not in old:continue
        name=Path(old).stem.removesuffix('_flow');key=replacements.get(name)
        if not key:continue
        mat['texture']=n.String(f'xiantu:textures/vfx/v11/{key}.png')
        cfg['startColor']=n.Compound({'_type':n.String('Color'),'number':n.Int(-1)})
        uv=cfg['uvAnimation'];uv['enable']=n.Byte(1);uv['tiles']=n.Compound({'a':n.Int(4),'b':n.Int(8 if key in ('thunder','burst') else 4)})
        uv['cycle']=n.Float(int(uv['tiles']['a'])*int(uv['tiles']['b']));uv['frameOverTime']=v.curve([0,0,.33,.33,.66,.66,1,.999]);dirty=True
    if dirty:backup(path);n.File(root,gzipped=True).save(path);changed+=1
pre=A/'vfx_v10_preload.json';backup(pre);data=json.loads(pre.read_text())
data['effects']=sorted(set(data['effects']+effects));data['textures']=sorted(set(data['textures']+['xiantu:'+m['texture'] for m in manifest]))
pre.write_text(json.dumps(data,indent=2),encoding='utf8')
# Source atlas has four equal quadrants. Borders are nine-sliced at runtime.
master=Image.open(OUT/'gui-master.png').convert('RGBA');w,h=master.size
panel=master.crop((0,0,w//2,h//2)).resize((256,256),Image.Resampling.LANCZOS)
paper=master.crop((w//2,0,w,h//2)).convert('L');paper=ImageOps.colorize(paper,(188,177,148),(239,228,204)).convert('RGBA').resize((256,256))
slot=master.crop((0,h//2,w//2,h)).resize((32,32),Image.Resampling.LANCZOS)
bagua=master.crop((w//2,h//2,w,h)).resize((192,192),Image.Resampling.LANCZOS)
# Remove only the black background; retain luminous metal and jade details.
pixels=bagua.load()
for y in range(192):
 for x in range(192):
    r,g,b,a=pixels[x,y];pixels[x,y]=(r,g,b,0 if max(r,g,b)<18 else a)
for name,im in [('panel',panel),('paper',paper),('slot',slot),('bagua',bagua)]:saveim(im,A/f'textures/gui/cultivation/{name}.png')
(OUT/'authored.json').write_text(json.dumps({'reference_sequences':manifest,'new_effects':effects,'upgraded_graphs':changed},ensure_ascii=False,indent=2),encoding='utf8')
print('Authored',len(effects),'effects;',changed,'existing graphs upgraded')

# Latest user-approved controls use an actual elongating solid staff. Keep specialized graphs.
if __name__ == "__main__":
    import runpy
    runpy.run_path(str(ROOT/"tools/author_staff_effects_20261025.py"), run_name="__main__")
