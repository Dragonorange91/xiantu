from pathlib import Path
import sys,json,copy,uuid
import nbtlib as n
sys.path.insert(0,'tools');import author_photon_v9 as v
A=Path('src/main/resources/assets/xiantu');O=Path('asset_sources/active-weapons-20261025');O.mkdir(parents=True,exist_ok=True)
def p(name,tex='qi',color='FFFFFF',**kw):
 o=v.particle('active '+name,tex,color,**kw);cfg=o['config'];cfg['material']['material']['texture']=n.String('xiantu:textures/vfx/v11/'+tex+'.png');uv=cfg['uvAnimation'];uv['enable']=n.Byte(1);frames=32 if tex in ('burst','thunder')else 16;uv['tiles']=n.Compound({'a':n.Int(4),'b':n.Int(frames//4)});uv['cycle']=n.Float(frames);uv['frameOverTime']=v.curve([0,0,.33,.33,.66,.66,1,.999]);return o
specs={
 'shadow_sever':('三道错层狭长剑影瞬发截脉，碎光由近及远','blade','B9DFFF','line',6),
 'radiant_return':('收光向胸前汇聚，环形玉光与悬浮剑片构成护罩','qi','FAE8B3','guard',0),
 'dragon_pierce':('螺旋龙形剑气穿透前路，三层气旋沿剑尖推进','qi','67D9C4','helix',8),
 'forged_guard':('四枚厚重赤金护锋交叉成盾，锻火火星收束','blade','FFC788','guard',0),
 'star_shatter':('重斧扇面爆出多束碎星，厚尘与星核崩裂分层展开','burst','C6C7FF','fan',4),
 'chaos_suppress':('近身混沌气浪由内向外翻卷，八方尘柱延迟爆发','dust','C5AEFF','radial',3.5),
 'godslayer_pierce':('暗红锋芒沿枪轴骤进，分段锐刺穿过目标','blade','F39B9B','line',6),
 'dao_corrosion':('墨紫蚀脉双螺旋围绕细长枪芒，命中气团内缩','qi','CCAAFF','helix',7),
 'celestial_guard':('六面天罡气盾错层升起，星芒沿盾缘轮转','qi','9CBFFF','guard',0),
 'spirit_sweep':('青玉仙灵扇面弧光铺展，柔和回生气息回流至持枪者','qi','ADF4CF','fan',4.5),
 'divine_reveal':('白金枪芒贯穿，三处星芒依次点亮并留短暂碎光','blade','FFF1C5','line',8),
 'great_dao_quake':('金玉道纹气浪分层外推，四向厚重光柱与低尘震荡','qi','F7D79E','radial',4),
 'heaven_split':('开天蓄势后竖向斧芒凝聚，重击释放狭长崩裂锋光','blade','FFB874','heavy',3),
 'sun_pierce':('金白日核沿枪尖向前贯穿，内核与外层气流交错','burst','FFE5A1','line',3)}
import math
for ident,(desc,tex,col,shape,reach)in specs.items():
 objects=[]
 if shape in ('line','helix'):
  for i in range(6):
   z=.6+reach*i/5;x=.28*math.sin(i*1.8)if shape=='helix'else 0;y=1.2+(.28*math.cos(i*1.8)if shape=='helix'else 0)
   objects.append(p(ident+str(i),tex,col,size=(.7,.7,1.8),life=11,pos=(x,y,z),delay=i,rotation=20*i,spin=.03))
 elif shape=='guard':
  for i in range(6):
   t=i*math.tau/6;objects.append(p(ident+str(i),tex,col,size=1.2,life=26,pos=(.7*math.cos(t),1+.7*math.sin(t),.35),rotation=i*60,delay=i,spin=.025))
 elif shape=='fan':
  for i in range(7):
   t=(i-3)*.18;objects.append(p(ident+str(i),tex,col,size=1.7,life=16,pos=(math.sin(t)*reach,1,math.cos(t)*reach),delay=abs(i-3),rotation=(i-3)*18,expand=True))
 elif shape=='radial':
  objects.append(p(ident+' main ring','qi',col,size=reach*2,life=20,pos=(0,.15,0),mode='Horizontal',expand=True))
  for i in range(8):
   t=i*math.tau/8;objects.append(p(ident+str(i),tex,col,size=(1,1.8,1),life=16,pos=(math.sin(t)*reach,.5,math.cos(t)*reach),delay=3,expand=True))
 else:
  objects=[p(ident+' towering edge',tex,col,size=(1.1,3.2,1),life=18,pos=(0,1.4,1.3)),p(ident+' fragments','dust',col,size=2.2,life=16,pos=(0,.3,2),expand=True)]
 objects.append(p(ident+' core','qi',col,size=.8,life=12,pos=(0,1,.5)))
 for stage,objs in [('release',objects),('impact',[p(ident+' contact',tex,col,size=1.5,life=14,expand=True),p(ident+' fragments','blade',col,size=.18,life=11,duration=3,rate=2,radius=.08,speed=.08)])]:
  root=copy.deepcopy(v.ROOT_TEMPLATE);root['fx']['mainFX']['fxObjects']=n.List[n.Compound](objs);root['fx']['subFXs']=n.Compound();dest=A/f'fx/skills/{ident}/{stage}.fx';dest.parent.mkdir(parents=True,exist_ok=True);n.File(root,gzipped=True).save(dest)
manifest=A/'vfx_v10_preload.json';data=json.loads(manifest.read_text());data['effects']=sorted(set(data['effects'])|{f'xiantu:skills/{id}/{st}'for id in specs for st in('release','impact')});manifest.write_text(json.dumps(data,ensure_ascii=False,indent=2),encoding='utf8')
(O/'design.json').write_text(json.dumps({k:{'description':x[0],'shape':x[3],'reach':x[4]}for k,x in specs.items()},ensure_ascii=False,indent=2),encoding='utf8')
print('Authored',len(specs)*2,'independent native graphs')
