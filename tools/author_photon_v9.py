"""Photon 1.1.17 native-template authoring, using recorded Helper emitter schemas.

No arbitrary binary layout: clone the engine-exported v2 object, patch existing
typed fields, and wrap in the already shipped/exported v1 FX root. Runtime/native
deserialization is validated separately in the game. All images are cropped from
the saved image-API master; black luminance becomes alpha, with edge padding.
"""
import copy, hashlib, json, math, uuid
from pathlib import Path
import nbtlib as n
import numpy as np
from PIL import Image, ImageOps

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'asset_sources/beast-vfx-v9-20261018'
ASSETS=ROOT/'src/main/resources/assets/xiantu'
TEXTURES=['fireball','flame','plasma','lightning','blade','claw','splash','wave',
          'rock','smoke','leaves','spores','vortex','burst','shield','comet']
COLORS={'metal':'FFE6A2','fire':'FF8B32','water':'73DDFC','wood':'A5E67B','earth':'D4B486','thunder':'AA9FFF'}
# Each identity selects a silhouette, volume distribution, timing and secondary
# motif, rather than recolouring a shared ring/beam graph.
SPECIES={
 'yanwei_fox':('fire','fireball','flame',.9,'狐火光核、卷焰外壳、拉长流焰尾迹'),
 'cracked_blade_wolf':('metal','claw','blade',1.1,'三道斜爪、锐利薄刃、短促碎光'),
 'cold_fang_wolf':('water','rock','comet',1.0,'冷晶碎齿、冰雾尾流、凝霜碎裂'),
 'crag_rock_spider':('earth','rock','smoke',1.3,'八向错落岩齿、低尘涌起、中心碎石爆裂'),
 'moss_back_bear':('wood','leaves','rock',1.5,'木屑翻涌、苔叶卷流、沉重掌击'),
 'mountain_bear':('earth','rock','burst',1.9,'大块岩屑抬升、短震尘柱、重拳爆发'),
 'gold_mane_lion':('metal','blade','vortex',1.6,'扇形金刃鬃光、旋切金流、锐芒击中'),
 'red_sun_lion':('fire','flame','fireball',1.6,'胸前赤阳蓄核、扇面烈焰、余烬烟流'),
 'rock_back_boar':('earth','rock','smoke',1.15,'獠牙两侧碎岩、冲撞扬尘、短程岩爆'),
 'thick_horn_rhino':('earth','comet','rock',1.6,'独角聚芒、厚重冲锋气流、大型碎岩'),
 'thunder_feather_raven':('thunder','lightning','comet',1.1,'双翼分叉电羽、俯冲雷尾、落点电爆'),
 'vine_deer':('wood','leaves','smoke',.6,'受惊叶屑与柔和木灵息'),
 'thunder_deer':('thunder','comet','plasma',.6,'鹿角短电丝与受惊流光'),
 'iron_tooth_badger':('metal','rock','smoke',.6,'受惊砂屑，不新增敌对技能'),
 'thunder_prison_lion':('thunder','plasma','lightning',1.7,'胸喉电浆凝聚、宽锥分叉雷息、电离雾'),
 'electric_marten':('thunder','comet','plasma',.75,'狭长闪跃流尾、紧凑电核、短接触爆闪'),
 'flint_armored_lizard':('fire','rock','flame',1.0,'甲缝燧火、低伏炽岩扑咬、飞溅火屑'),
 'molten_turtle':('fire','smoke','flame',.8,'受惊壳缝热雾，不新增攻击'),
 'sand_armadillo':('earth','smoke','rock',.7,'低矮砂尘翻滚，不新增攻击'),
 'jade_scale_crocodile':('water','splash','comet',1.2,'贴地水冲、双颚水冠、咬合飞沫'),
 'copper_beak_vulture':('metal','blade','comet',1.0,'铜喙锐芒、双翼刀羽、斜向撕裂'),
 'fire_beak_falcon':('fire','flame','comet',.85,'喙前炽核、收束火羽、俯冲焰尾'),
 'purple_lightning_bat':('thunder','plasma','vortex',.75,'脉冲电浆球、缠绕电壳、锥形电尾'),
 'azure_scale_python':('water','wave','splash',1.6,'宽幅卷水尾扫、叠浪水冠、飞散水珠'),
 'ancient_branch_python':('wood','leaves','comet',1.6,'贴身藤叶螺流、厚重横扫、碎叶外散'),
 'tide_pattern_ray':('water','splash','wave',.65,'受惊水膜与泡沫，不新增攻击'),
 'dark_shell_crab':('water','claw','splash',1.2,'左右错拍钳刃、双束夹水、接触水冠'),
 'gold_back_beetle':('metal','shield','comet',1.0,'甲壳金膜、前冲尖锥、金屑撞击'),
 'jade_leaf_mantis':('wood','blade','leaves',1.25,'交错双镰弧刃、叶纹锋面、二段收刃'),
 'mushroom_toad':('wood','spores','smoke',1.0,'蘑伞凝聚孢云、团簇呼息、气团缓散')}
TEMPLATE=n.parse_nbt(json.loads((ROOT/'work/qa/beast-photon-20261017/particle-template.json').read_text())['snbt'])
ROOT_TEMPLATE=n.load(ASSETS/'fx/beasts/yanwei_fox/projectile.fx')
MANIFEST=[]

def c(v):return n.Compound({'_type':n.String('Constant'),'number':n.Float(v)})
def vec(x,y,z):return n.Compound({k:n.Float(v) for k,v in zip('xyz',(x,y,z))})
def curve(points):
    # Cubic Bezier segments are the native four-control-point format.
    return n.Compound({'_type':n.String('Curve'),'curves':n.List[n.List[n.Float]]([n.List[n.Float](points)]),
      'defaultValue':n.Float(1),'lockControlPoint':n.Byte(1),'lower':n.Float(0),'upper':n.Float(2),
      'min':n.Float(0),'max':n.Float(4),'xAxis':n.String('life'),'yAxis':n.String('size')})
def signed(argb):return argb if argb<2**31 else argb-2**32

def textures():
    master=Image.open(OUT/'master-atlas.png').convert('RGB');w,h=master.size
    dest=ASSETS/'textures/vfx/v9';dest.mkdir(parents=True,exist_ok=True)
    for i,name in enumerate(TEXTURES):
        box=(i%4*w//4,i//4*h//4,(i%4+1)*w//4,(i//4+1)*h//4)
        im=master.crop(box);rgb=np.asarray(im,dtype=np.float32)/255
        lum=rgb.max(2);lum=np.clip((lum-.009)/.991,0,1)
        # Pure luminance alpha retains wisps without black opaque rectangles.
        alpha=Image.fromarray(np.uint8(lum*255)).resize((240,240),Image.Resampling.LANCZOS)
        im=Image.new('RGBA',(256,256),(255,255,255,0));im.paste(Image.merge('RGBA',(Image.new('L',alpha.size,255),)*3+(alpha,)),(8,8))
        im.save(dest/(name+'.png'))
        if name not in ('fireball','flame','plasma','smoke','wave','vortex'):continue
        atlas=Image.new('RGBA',(1024,1024),(255,255,255,0));src=np.asarray(im)
        yy,xx=np.mgrid[:256,:256]
        for frame in range(16):
            t=frame/16*math.tau
            # Small continuous advection. Silhouette is the generated art, not
            # a newly invented sequence claimed to be API-generated animation.
            dx=3*np.sin(yy*.045-t)*np.sin(xx/255*math.pi)
            dy=2*np.sin(xx*.037+t)*np.sin(yy/255*math.pi)
            sx=np.clip(np.rint(xx+dx),0,255).astype(int);sy=np.clip(np.rint(yy+dy),0,255).astype(int)
            a=Image.fromarray(src[sy,sx]);atlas.paste(a,(frame%4*256,frame//4*256))
        atlas.save(dest/(name+'_flow.png'))

def particle(name,tex,color,size=1,life=10,duration=1,rate=1,pos=(0,0,0),speed=0,radius=0,
             delay=0,rotation=0,spin=0,mode='Billboard',flow=False,space='World',expand=False):
    obj=copy.deepcopy(TEMPLATE);cfg=obj['config'];obj['name']=n.String(name)
    raw=uuid.uuid5(uuid.NAMESPACE_URL,'xiantu-v9-'+name).int
    obj['transform']['id']=n.IntArray([signed((raw>>shift)&0xffffffff) for shift in (96,64,32,0)])
    obj['transform']['localPosition']=vec(*pos)
    cfg['duration']=n.Int(duration);cfg['looping']=n.Byte(0);cfg['maxParticles']=n.Int(max(4,min(64,math.ceil(rate*life)+2)))
    cfg['simulationSpace']=n.String(space);cfg['startDelay']=c(delay);cfg['startLifetime']=c(life)
    cfg['startSpeed']=c(speed);cfg['emission']['emissionRate']=c(rate)
    cfg['startColor']=n.Compound({'_type':n.String('Color'),'number':n.Int(signed(0xff000000|int(color,16)))})
    if isinstance(size,(float,int)):size=(size,)*3
    cfg['startSize']=n.Compound({k:c(v) for k,v in zip('xyz',size)})
    cfg['startRotation']['z']=c(rotation)
    # Photon 1.1.17 Cone.nextPosVel divides sampled radius by radius. Zero
    # yields NaN even for stationary particles (NaN * zero remains NaN).
    cfg['shape']['shape']['angle']=n.Float(80);cfg['shape']['shape']['radius']=n.Float(max(.0001,radius))
    cfg['shape']['shape']['radiusThickness']=n.Float(1)
    cfg['material']['cull']=n.Byte(0);cfg['material']['depthMask']=n.Byte(0);cfg['material']['depthTest']=n.Byte(1)
    cfg['material']['blendMode']['dstColorFactor']=n.String('ONE')
    cfg['material']['material']['texture']=n.String('xiantu:textures/vfx/v9/'+tex+('_flow' if flow else '')+'.png')
    cfg['material']['material']['discardThreshold']=n.Float(.004)
    cfg['renderer']['shade']=n.Byte(0);cfg['renderer']['bloomEffect']=n.Byte(0)
    cfg['renderer']['renderMode']=n.String(mode)
    # No per-emitter bloom pass. Baked luminous falloff is shared texture data.
    cfg['colorOverLifetime']['enable']=n.Byte(1)
    cfg['colorOverLifetime']['color']['a']=n.List[n.Float]([0,0,.10,1,.65,.8,1,0])
    cfg['sizeOverLifetime']['enable']=n.Byte(1)
    points=[0,.25,.12,.8,.65,1.2,1,1.6] if expand else [0,.55,.15,1,.75,.9,1,.1]
    cfg['sizeOverLifetime']['size']=n.Compound({k:curve(points) for k in 'xyz'})
    if spin:
        # Native roll is an angle in degrees at normalized particle age, not a
        # per-tick angular velocity. Animate it instead of a constant .05 deg.
        angle=spin*1800
        cfg['rotationOverLifetime']['enable']=n.Byte(1)
        cfg['rotationOverLifetime']['roll']=curve([0,0,.33,angle*.33,.66,angle*.66,1,angle])
    if flow:
        uv=cfg['uvAnimation'];uv['enable']=n.Byte(1);uv['tiles']=n.Compound({'a':n.Int(4),'b':n.Int(4)})
        uv['cycle']=n.Float(16);uv['frameOverTime']=curve([0,0,.33,.33,.66,.66,1,.999])
    return obj

def write(ident,stage,objects,description):
    assert 1<=len(objects)<=12,(ident,stage,len(objects))
    # Native root cloned from the installed export, only mainFX objects replaced.
    root=copy.deepcopy(ROOT_TEMPLATE);root['fx']['mainFX']['fxObjects']=n.List[n.Compound](objects)
    root['fx']['subFXs']=n.Compound()
    path=ASSETS/'fx/beasts'/ident/(stage+'.fx');path.parent.mkdir(parents=True,exist_ok=True)
    n.File(root,gzipped=True).save(path)
    MANIFEST.append({'id':'xiantu:beasts/'+ident+'/'+stage,'emitters':len(objects),
      'particle_capacity':sum(int(x['config']['maxParticles']) for x in objects),
      'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'description':description})

def species():
    for index,(ident,(element,main,secondary,scale,desc)) in enumerate(SPECIES.items()):
        col=COLORS[element];base=index*13
        def p(label,tex=main,**kw):return particle(ident+' '+label,tex,col,**kw)
        flow=main in ('fireball','flame','plasma','smoke','wave','vortex')
        # Body-local gathering only; explicitly no floor ring/lane/circle.
        write(ident,'warn',[
          p('gather',size=.48*scale,life=8,duration=24,rate=.20,pos=(0,.6,.30),flow=flow,spin=.035,space='Local'),
          p('gather wisps',secondary,size=.32*scale,life=10,duration=18,rate=.22,pos=(0,.65,.35),radius=.23,speed=.012,spin=-.05)
        ],desc+'；蓄力仅在身前')
        write(ident,'trail',[
          p('moving subject',size=.65*scale,life=7,duration=22,rate=.25,pos=(0,.6,.35),flow=flow,spin=.06,space='Local'),
          p('world wake',secondary,size=.6*scale,life=11,duration=22,rate=.6,pos=(0,.45,-.35),radius=.16,speed=.025,spin=-.035),
          p('wake sparks','burst',size=.13,life=7,duration=16,rate=.3,pos=(0,.55,-.5),radius=.3,speed=.04)
        ],desc+'；实体移动产生世界空间尾迹')
        shape=secondary if main in ('claw','blade') else main
        objects=[p('impact body',shape,size=1.5*scale,life=11,flow=shape in ('fireball','flame','plasma','smoke','wave','vortex'),expand=True),
           p('impact inner','burst',size=.65*scale,life=6,rotation=base%180),
           p('impact fragments',secondary,size=.32*scale,life=13,duration=3,rate=3,radius=.15,speed=.10,spin=.08)]
        if ident=='dark_shell_crab':
            objects += [p('left claw','claw',size=1.4,life=9,pos=(-.5,.25,0),rotation=-25),p('right claw','claw',size=1.4,life=9,pos=(.5,.25,0),rotation=25,delay=3)]
        elif ident=='jade_leaf_mantis':
            objects += [p('return blade','blade',size=2.2,life=8,delay=4,rotation=130)]
        elif ident=='crag_rock_spider':
            objects += [p('rock teeth '+str(i),'rock',size=.65,life=13,pos=(math.sin(i*math.pi/2)*.9,.2,math.cos(i*math.pi/2)*.9),delay=i) for i in range(4)]
        write(ident,'impact',objects,desc+'；命中爆发')
        write(ident,'melee',[p('contact','claw' if main not in ('blade','claw') else main,size=.8*scale,life=7,pos=(0,0,.65),rotation=base%100-50),p('contact glint','burst',size=.20,life=5,pos=(0,0,.7),delay=4)],'近战接触与挥击，不改变阵营')
        write(ident,'flee',[p('flee',secondary,size=.25,life=10,duration=10,rate=.25,radius=.2,speed=.03)],'受惊残屑')
        if ident in ('yanwei_fox','purple_lightning_bat'):
            write(ident,'projectile',[
              p('projectile shell',main,size=.72,life=6,duration=64,rate=.2,space='Local',flow=True,spin=.06),
              particle(ident+' white hot core','burst','FFF0D8' if element=='fire' else 'D6F9FF',size=.26,life=5,duration=64,rate=.22,space='Local'),
              p('projectile tail','comet',size=(.4,.9,.4),life=9,duration=64,rate=.6,pos=(0,0,-.25),spin=.01)
            ],desc+'；全Photon投射物，无物品模型')

def bosses():
    cats=json.loads((ASSETS/'boss_combat.json').read_text(encoding='utf8'))
    palettes={'battle_general_soul':'FFD68C','meteor_general':'AEC7FF','spirit_ape_king':'FFD77F','xuantians_remnant':'77EACF','cloud_sea_lord':'8BDBFF','five_element_warden':'D8E7AE'}
    motifs={'battle_general_soul':('blade','comet'),'meteor_general':('plasma','lightning'),
      'spirit_ape_king':('rock','vortex'),'xuantians_remnant':('vortex','burst'),
      'cloud_sea_lord':('wave','smoke'),'five_element_warden':('leaves','rock')}
    for ident,skills in cats.items():
        col=palettes[ident];main,second=motifs[ident]
        for skill in [dict(id='attack',name='普攻',shape='melee',reach=3.6,windup=14,active=12,ultimate=False)]+skills:
            sid=skill['id'];shape=skill['shape'];reach=skill['reach'];duration=skill['active']
            def p(label,tex=main,**kw):return particle(ident+' '+sid+' '+label,tex,col,**kw)
            gather=[p('gather shell',main,size=.65,life=9,duration=skill['windup'],rate=.22,pos=(0,1.45,.6),space='Local',flow=main in ('plasma','vortex','wave'),spin=.05),
              p('gather fragments',second,size=.25,life=9,duration=skill['windup'],rate=.4,pos=(0,1.35,.55),radius=.32,speed=.015)]
            write(ident,sid+'/charge',gather,skill['name']+'：身前聚气，无地面预警')
            objects=[]
            if shape in ('melee','fist_combo','counter','charge','leap'):
                tex='vortex' if ident=='xuantians_remnant' else 'blade' if ident=='battle_general_soul' else main
                pulses=3 if shape=='fist_combo' else 1
                for i in range(pulses):
                    objects.append(p('strike '+str(i),tex,size=1.8 if shape!='charge' else 2.5,life=10,pos=((i%2-.5)*.65,1.25,1.3),delay=i*10,rotation=20-i*55,flow=tex=='vortex',expand=True))
                objects.append(p('wake',second,size=.5,life=10,duration=duration,rate=.4,pos=(0,.9,-.2),speed=.05,radius=.3))
            elif shape in ('projectile','line','lanes','fan','cross'):
                # Actual spatial points and delayed subjects form travelling
                # volumes; not a polygonal line approximation of a spell.
                lanes=(-1,0,1) if shape in ('fan','lanes','cross') else (0,)
                for lane in lanes:
                    for j in range(2):
                        objects.append(p('advancing '+str(lane)+' '+str(j),'blade' if ident=='battle_general_soul' else 'comet',size=(1.2,2.3,1.2),life=12,pos=(lane*(1.3+j),1.25,(j+1)*reach/3),delay=j*6,rotation=lane*35))
                objects.append(p('launch shell',size=1.3,life=12,pos=(0,1.3,.7),flow=main in ('plasma','wave','vortex'),spin=.09))
            elif shape in ('fall','fall_sequence'):
                count=5 if shape=='fall_sequence' else 3
                for i in range(count):
                    angle=i*math.tau/count;r=3 if count==5 else .55
                    objects.append(p('descending '+str(i),'lightning' if ident=='meteor_general' else 'comet',size=(1.8,5.0,1.8),life=11,pos=(math.sin(angle)*r,2.1,math.cos(angle)*r),delay=i*12 if count==5 else i*3))
                    objects.append(p('landing '+str(i),'splash' if ident=='cloud_sea_lord' else 'burst',size=2.8,life=11,pos=(math.sin(angle)*r,.65,math.cos(angle)*r),delay=(i*12 if count==5 else i*3)+5,expand=True))
            elif shape in ('ring','gap_ring','wave'):
                for i in range(6):
                    a=i*math.tau/6
                    if shape=='gap_ring' and abs(a)<.7:continue
                    objects.append(p('outward '+str(i),'wave' if ident=='cloud_sea_lord' else main,size=2.2,life=18,pos=(math.sin(a)*reach*.55,.8,math.cos(a)*reach*.55),rotation=i*60,delay=i%2*2,flow=main in ('plasma','wave','vortex'),expand=True))
                objects.append(p('centre','burst',size=1.2,life=7,pos=(0,.8,0)))
            elif shape in ('guard','counter','seal','pressure','links','banner','illusion'):
                for i in range(3):
                    a=i*math.tau/3
                    objects.append(p('shell '+str(i),'shield' if shape in ('guard','counter','seal','pressure') else main,size=2.2,life=16,duration=duration,rate=.07,pos=(math.sin(a)*1.1,1.5,math.cos(a)*1.1),rotation=i*25,spin=(-1 if i%2 else 1)*.025))
                objects.append(p('sustain',second,size=.35,life=12,duration=duration,rate=.5,pos=(0,1.1,0),radius=.8,speed=.025))
            else:
                objects=[p('main',size=2.4,life=20,pos=(0,1.2,1),flow=main in ('plasma','wave','vortex'),expand=True)]
            if skill['ultimate']:
                objects += [particle(ident+' '+sid+' white core','burst','F1FAFF',size=3.5,life=9,pos=(0,1.2,0)),p('ultimate smoke','smoke',size=2,life=22,duration=5,rate=1,radius=.6,speed=.09,flow=True)]
            write(ident,sid+'/release',objects,skill['name']+'：'+shape+'独立空间分层释放')
            write(ident,sid+'/impact',[p('contact',main,size=2.1,life=12,flow=main in ('plasma','wave','vortex'),expand=True),p('contact flash','burst',size=1.1,life=6),p('debris',second,size=.4,life=14,duration=3,rate=2,radius=.3,speed=.1)],skill['name']+'：命中与余波')
        write(ident,'impact',[particle(ident+' generic contact',main,col,size=1.3,life=10),particle(ident+' contact spark','burst',col,size=.6,life=6)],'实体普适接触，不替代逐招特效')

def main():
    OUT.mkdir(parents=True,exist_ok=True);textures();species();bosses()
    (OUT/'photon-manifest.json').write_text(json.dumps(MANIFEST,ensure_ascii=False,indent=2),encoding='utf8')
    (ASSETS/'vfx_v9_preload.json').write_text(json.dumps({'effects':[x['id'] for x in MANIFEST],
       'textures':['xiantu:'+p.relative_to(ASSETS).as_posix() for p in (ASSETS/'textures/vfx/v9').glob('*.png')]},indent=2),encoding='utf8')
    doc=['# 妖兽战斗特效 v9\n','原始参考：用户2026-10-18提供的光核、流焰、电浆、水浪、刀光。全部使用Photon原生粒子图层；不保留线段批次或地面预警。\n',
      '每招顺序：身体/武器附近聚气 → 独立形态释放 → 接触光核/碎屑 → 淡出。贴图来源是本轮生成的单色素材，再由运行时染色；不是将整张参考图贴到世界。\n',
      '## 普通妖兽\n']
    doc += [f'- {ident}：{row[-1]}。' for ident,row in SPECIES.items()]
    doc += ['\n## 逐招资源\n']+[f'- {x["id"]}：{x["description"]}（{x["emitters"]}层）。' for x in MANIFEST if '/charge' in x['id'] or '/release' in x['id']]
    doc += ['\n## 预算与验收\n','单资源最多12层，每层容量显式受限；不启用每发射器Bloom、碰撞或噪声求解。资源和贴图在重载阶段预热，施法时不读盘解析。跨层深度测试开启，透明不写深度。原2K模型与30格完整骨骼动画保留。\n',
      '当前是作者资源；原生反序列化、实际动作对应、首发/重复施法帧时间、视觉外观需要分别实测，文件生成不等于验收。']
    (ROOT/'docs/妖兽Photon战斗重制_v9_20261018.md').write_text('\n'.join(doc),encoding='utf8')
    print('AUTHORED',len(MANIFEST),'native FX; max emitters',max(x['emitters'] for x in MANIFEST))

if __name__=='__main__':main()
