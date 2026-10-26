"""Author bounded native Photon graphs through the installed MCP-HELPER.

Schema comes from the live particle/beam templates recorded alongside QA.
Only this task's xiantu:beasts/* resources are written. Native exporter supplies
the NBT/UUID layout; this script never fabricates a .fx binary.
"""
import json,math,time,shutil
from pathlib import Path
from beast_photon_helper import ROOT,QA,call,guard

SPECIES={
 'yanwei_fox':('D','#FF792C'),
 'cracked_blade_wolf':('A','#DCEBFF'),
 'cold_fang_wolf':('A','#86DCFF'),
 'crag_rock_spider':('E','#DCB77B'),
 'moss_back_bear':('I','#8ECC71'),
 'mountain_bear':('I','#C5A279'),
 'gold_mane_lion':('I','#FFD768'),
 'red_sun_lion':('I','#FF6A27'),
 'rock_back_boar':('B','#B99A6C'),
 'thick_horn_rhino':('B','#DDD2A7'),
 'thunder_feather_raven':('H','#A9BFFF'),
 'vine_deer':('P','#A0DF86'),
 'thunder_deer':('P','#A5CFFF'),
 'iron_tooth_badger':('P','#D2DCE5'),
}

def snbt(v):
    if isinstance(v,bool):return '1b' if v else '0b'
    if isinstance(v,str):return json.dumps(v)
    if isinstance(v,float):return f'{v:.6f}f'
    if isinstance(v,int):return str(v)
    if isinstance(v,list):return '['+','.join(map(snbt,v))+']'
    return '{'+','.join(k+':'+snbt(x) for k,x in v.items())+'}'
def number(v):return {'_type':'Constant','number':float(v)}
def vector(x,y,z):return dict(x=float(x),y=float(y),z=float(z))
def color(c):return int(c.lstrip('#'),16)-16777216

def beam(a,b,c,ticks,width=.09):
    return {'type':'beam','name':'rune segment','x':a[0],'y':a[1],'z':a[2],
      'config':snbt({'duration':ticks,'looping':False,'width':number(width),
       'color':{'_type':'Color','number':color(c)},'end':vector(*(b[i]-a[i] for i in range(3))),
       'material':{'cull':False,'depthTest':True,'depthMask':False,'material':{'_type':'TextureMaterial','texture':'xiantu:textures/mcp/beasts/rune_beam.png','discardThreshold':.01}},'renderer':{'bloomEffect':False}})}

def ring(radius,c,ticks,segments=16,y=.065):
    points=[(math.sin(i*math.tau/segments)*radius,y,math.cos(i*math.tau/segments)*radius) for i in range(segments)]
    return [beam(points[i],points[(i+1)%segments],c,ticks) for i in range(segments)]

def motes(c,ticks=12,size=.12,rate=2,lifetime=12,y=.3,speed=.04,radius=.45):
    # Native template's cone and world simulation; small bounded clouds, no bloom.
    return {'type':'particle','name':'spirit fragments','color':c,'size':size,'rate':rate,'lifetime':lifetime,
      'duration':ticks,'looping':False,'maxParticles':48,'y':y,
      'config':snbt({'simulationSpace':'World','startSpeed':number(speed),
       'shape':{'shape':{'_type':'cone','angle':40.,'radius':radius,'radiusThickness':1.0,'arc':360.}},
       'material':{'cull':False,'depthTest':True,'depthMask':False},
       'renderer':{'bloomEffect':False},
       'colorOverLifetime':{'enable':True,'color':{'_type':'Gradient','a':[0.,1.,.65,.8,1.,0.],
         'r':[0.,1.,1.,1.],'g':[0.,1.,1.,1.],'b':[0.,1.,1.,1.]}},
       'sizeOverLifetime':{'enable':True,'size':{k:{'_type':'Curve','curves':[[0.,1.,.25,1.,.75,.5,1.,0.]],'defaultValue':1.,'min':0.,'max':1.,'lower':0.,'upper':1.,'lockControlPoint':True,'xAxis':'life','yAxis':'size'} for k in 'xyz'}}})}

def graph(skill,c,stage):
    if stage=='flee':return {'emitters':[motes(c,10,.12,1,10,.25,.025,.2)]}
    if stage=='projectile':
        core=motes('#FFF1B1',30,.32,1,4,0.,0.,0.)
        core['config']=core['config'].replace('"World"','"Local"')
        return {'emitters':[core,motes(c,30,.2,2,7,0.,.01,.08)]}
    if stage=='trail':return {'emitters':[motes(c,18,.12,2,9,.4,.015,.18)]}
    if stage=='impact':
        r=2 if skill=='E' else 1.05 if skill in ('I','B') else .5
        return {'emitters':ring(r,c,7,12)+[motes(c,4,.18,7,12,.15,.12,r*.6)]}
    ticks={'A':16,'B':24,'D':20,'E':24,'H':30,'I':30}[skill]
    if skill=='I':
        pts=[(-1.,.065,0.),(-1.,.065,3.),(1.,.065,3.),(1.,.065,0.)]
        e=[beam(pts[i],pts[(i+1)%4],c,ticks,.09) for i in range(4)]
        e += [beam((-.8,.065,z),(.8,.065,z),c,ticks,.04) for z in (.7,1.5,2.3)]
    elif skill in ('A','B'):
        length=6. if skill=='A' else 8.;width=.5 if skill=='A' else .75
        e=[beam((-width,.065,0),(-width,.065,length),c,ticks),beam((width,.065,0),(width,.065,length),c,ticks)]
        for z in (2.,4.,length):e += [beam((-width,.065,z-.4),(0.,.065,z),c,ticks),beam((width,.065,z-.4),(0.,.065,z),c,ticks)]
    elif skill=='E':
        e=ring(2.,c,ticks)+ring(1.,c,ticks,8)
        for i in range(8):
            a=i*math.tau/8;e.append(beam((0.,.07,0.),(2*math.sin(a),.07,2*math.cos(a)),c,ticks,.04))
    elif skill=='D':e=[motes(c,20,.18,2,8,1.,.012,.22),motes('#FFF1B1',20,.09,1,6,1.,.01,.08)]
    else:
        e=[motes(c,30,.12,1,7,.55,.015,.7)]
        for side in (-1,1):
            for a,b in [((side*.2,.8,0),(side*.5,1.,.1)),((side*.5,1.,.1),(side*.65,.85,0)),((side*.65,.85,0),(side*.9,1.05,.1))]:e.append(beam(a,b,c,ticks,.045))
    return {'emitters':e}

if __name__=='__main__':
    import argparse
    parser=argparse.ArgumentParser();parser.add_argument('--only',nargs='*');args=parser.parse_args()
    guard();existing=call('photon_list_fx');(QA/'preexisting-fx.json').write_text(json.dumps(existing,indent=2),encoding='utf8')
    records={};out=ROOT/'asset_sources/beast-photon-20261017';out.mkdir(parents=True,exist_ok=True)
    game=next(Path.home().joinpath('Desktop').glob('*/.minecraft/versions/1.20.1-Forge_47.4.26'))
    managed=game/'resourcepacks/mcpmod-generated-vfx/assets/xiantu/fx/beasts'
    for ident,(skill,c) in SPECIES.items():
        if args.only and ident not in args.only:continue
        stages=['flee'] if skill=='P' else ['warn','impact','projectile' if skill=='D' else 'trail']
        for stage in stages:
            effect='xiantu:beasts/'+ident+'/'+stage;document=graph(skill,c,stage)
            (out/(ident+'-'+stage+'.json')).write_text(json.dumps(document,indent=2),encoding='utf8')
            guard()
            result=call('photon_write_fx',id=effect,mode='replace' if (managed/ident/(stage+'.fx')).exists() else 'create',document=document)
            native=call('photon_read_fx',id=effect)
            (out/(ident+'-'+stage+'.snbt')).write_text(native['snbt'],encoding='utf8')
            records[effect]={'species':ident,'skill':skill,'stage':stage,'emitters':len(document['emitters']),'result':result}
            print('AUTHORED',effect,len(document['emitters']),flush=True)
    deadline=time.monotonic()+55
    while time.monotonic()<deadline:
        state=call('photon_status')
        if state['reload']=='complete':break
        if str(state['reload']).startswith('failed'):raise RuntimeError(state)
        time.sleep(.5)
    else:raise TimeoutError('Photon resource reload incomplete')
    call('photon_clear_fx_cache')
    for effect,entry in records.items():
        entry['diagnosis']=call('photon_diagnose_fx',id=effect)
        print('DIAGNOSED',effect,entry['diagnosis'],flush=True)
    (out/('manifest-'+('-'.join(args.only) if args.only else 'all')+'.json')).write_text(json.dumps(records,ensure_ascii=False,indent=2),encoding='utf8')
