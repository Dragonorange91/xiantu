"""Staff release graphs: solid accepted mesh supplies the shaft; Photon adds restrained energy/contact."""
from pathlib import Path
import importlib.util,copy
import nbtlib as n
ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('v9',ROOT/'tools/author_photon_v9.py');v=importlib.util.module_from_spec(spec);spec.loader.exec_module(v)
def p(name,tex,**kw):
 o=v.particle('staff20261025 '+name,'burst','FFD26B',**kw);c=o['config'];c['material']['material']['texture']=n.String(f'xiantu:textures/vfx/v11/{tex}.png');c['uvAnimation']['enable']=n.Byte(1);count=16;c['uvAnimation']['tiles']=n.Compound({'a':n.Int(4),'b':n.Int(count//4)});c['uvAnimation']['cycle']=n.Float(count);c['uvAnimation']['frameOverTime']=v.curve([0,0,.33,.33,.66,.66,1,.999]);return o
for skill,objects in {
 'staff_extend':[p('grip corona','qi',size=.6,life=9,pos=(0,0,.65)),p('shaft sparks','blade',size=.15,life=7,duration=3,rate=2,radius=.06,speed=.04,pos=(0,0,1))],
 'staff_quake':[p('delayed ground cloud','dust',size=4.5,life=18,delay=7,pos=(0,.2,3),expand=True),p('golden shock','qi',size=4.8,life=13,delay=7,pos=(0,.08,3),mode='Horizontal',expand=True),p('small debris','dust',size=.3,life=12,duration=3,rate=2,delay=7,radius=1.5,speed=.07,pos=(0,.2,3))]
}.items():
 root=copy.deepcopy(v.ROOT_TEMPLATE);root['fx']['mainFX']['fxObjects']=n.List[n.Compound](objects);root['fx']['subFXs']=n.Compound();n.File(root,gzipped=True).save(ROOT/f'src/main/resources/assets/xiantu/fx/skills/{skill}/release.fx')
print('Staff FX authored: bounded solid-mesh support and delayed quake, no fake forward cuts')
