"""Offline geometric inspection, not a Minecraft screenshot.

Uses the shipped Java blockstate/model elements and the mean of their referenced
textures. Orthographic shaded polygons make roofs, stairs, pillars and furniture
readable without pretending to reproduce Minecraft lighting or transparency.
"""
import io,json,math,zipfile
from functools import lru_cache
from pathlib import Path
from PIL import Image,ImageDraw,ImageFont,ImageStat

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'work/qa/realm-architecture-20261024'
VANILLA=zipfile.ZipFile((Path.home() / 'Desktop/仙途/.minecraft/versions/1.20.1-Forge_47.4.26/1.20.1-Forge_47.4.26.jar'))
FONT='C:/Windows/Fonts/msyh.ttc'
@lru_cache(None)
def resource(ns,kind,name):
    path=f'assets/{ns}/{kind}/{name}'
    if ns=='minecraft':return VANILLA.read(path)
    return (ROOT/'src/main/resources'/path).read_bytes()
@lru_cache(None)
def model(ident):
    ns,_,name=ident.partition(':')
    if not name:ns,name='minecraft',ns
    d=json.loads(resource(ns,'models',name+'.json'))
    parent=model(d['parent']) if d.get('parent') else {}
    return {**parent,**d,'textures':{**parent.get('textures',{}),**d.get('textures',{})}}
@lru_cache(None)
def color(ident):
    try:
        ns,_,name=ident.partition(':')
        if not name:ns,name='minecraft',ns
        im=Image.open(io.BytesIO(resource(ns,'textures',name+'.png'))).convert('RGBA')
        # First frame only; alpha-weighted texture mean.
        if im.height>im.width:im=im.crop((0,0,im.width,im.width))
        rgb=Image.new('RGB',im.size,(50,65,65));rgb.paste(im,mask=im.getchannel('A'))
        return tuple(round(v) for v in ImageStat.Stat(rgb).mean)
    except (KeyError,FileNotFoundError):return (148,125,104)
def resolve_tex(t,tex):
    for _ in range(12):
        if not t.startswith('#'):return t
        t=tex.get(t[1:],'minecraft:block/stone')
    return 'minecraft:block/stone'
@lru_cache(None)
def state_models(state):
    name,_,props=state.partition('[');ns,name=name.split(':')
    p=dict(v.split('=') for v in props.rstrip(']').split(',') if '=' in v)
    defaults={'axis':'y','facing':'north','half':'bottom','shape':'straight','type':'bottom','face':'floor','hanging':'false','waterlogged':'false','lit':'false','open':'false','powered':'false','in_wall':'false','north':'none','south':'none','east':'none','west':'none','up':'true'}
    try:d=json.loads(resource(ns,'blockstates',name+'.json'))
    except (FileNotFoundError,KeyError):return []
    selected=[]
    if 'variants' in d:
        for k,v in d['variants'].items():
            if all(p.get(a,defaults.get(a,c))==c for a,c in (q.split('=') for q in k.split(',') if '=' in q)):
                selected.append(v[0] if isinstance(v,list) else v);break
    else:
        def matches(test):
            if not test:return True
            if 'OR' in test:return any(matches(v) for v in test['OR'])
            if 'AND' in test:return all(matches(v) for v in test['AND'])
            return all(p.get(k,'false') in v.split('|') for k,v in test.items())
        for part in d.get('multipart',[]):
            if matches(part.get('when',{})):
                v=part['apply'];selected.append(v[0] if isinstance(v,list) else v)
    return selected
def rotate(v,axis,degrees,origin=(.5,.5,.5)):
    if not degrees:return v
    a=math.radians(degrees);c,s=math.cos(a),math.sin(a)
    x,y,z=[v[i]-origin[i] for i in range(3)]
    if axis=='y':x,z=x*c+z*s,-x*s+z*c
    elif axis=='x':y,z=y*c-z*s,y*s+z*c
    else:x,y=x*c-y*s,x*s+y*c
    return tuple(v+origin[i] for i,v in enumerate((x,y,z)))
@lru_cache(None)
def shapes(state):
    if state.endswith(':air'):return []
    if state.endswith(':water'):return [((0,0,0),(1,.88,1),(49,110,137),{})]
    out=[]
    for v in state_models(state):
        m=model(v['model']);tex=m.get('textures',{})
        for e in m.get('elements',[]):
            face=next(iter(e.get('faces',{}).values()),{})
            co=color(resolve_tex(face.get('texture','#particle'),tex))
            out.append((tuple(t/16 for t in e['from']),tuple(t/16 for t in e['to']),co,dict(element=e.get('rotation'),x=v.get('x',0),y=v.get('y',0))))
    return out
def render(theme,l,zoneid='core',cut=False):
    s=next(v for v in l['zones'] if v['id']==zoneid);cx,cy,cz=s['x'],s['y'],s['z']
    W,H=1280,1040;scale=19
    def project(p):
        a,h,c=p
        return (W/2+(a-c)*.78*scale,720+(a+c)*.36*scale-h*scale)
    cells={(x-cx,y-cy,z-cz):st for x,y,z,st in l['blocks'] if abs(x-cx)<=s['rx']+3 and abs(z-cz)<=s['rz']+3 and st!='minecraft:air'}
    if cut:cells={p:st for p,st in cells.items() if p[1]<4 or p[0]<-3 and p[2]<-3}
    polys=[]
    # Camera is on the south-east side; show only the outward-facing cuboid faces.
    for (x,y,z),state in cells.items():
        if y< -2:continue
        ss=shapes(state)
        for lo,hi,co,rot in ss:
            X,Y,Z=lo;A,B,C=hi
            corners=[(X,Y,Z),(A,Y,Z),(A,B,Z),(X,B,Z),(X,Y,C),(A,Y,C),(A,B,C),(X,B,C)]
            for i,p in enumerate(corners):
                if rot.get('element'):
                    r=rot['element'];p=rotate(p,r['axis'],r['angle'],tuple(t/16 for t in r['origin']))
                p=rotate(p,'x',-rot.get('x',0));p=rotate(p,'y',-rot.get('y',0))
                corners[i]=(x+p[0],y+p[1],z+p[2])
            for inds,light,step in [([3,7,6,2],1.1,(0,1,0)),([0,1,5,4],.6,(0,-1,0)),([1,2,6,5],.8,(1,0,0)),([0,4,7,3],.8,(-1,0,0)),([4,5,6,7],.93,(0,0,1)),([0,3,2,1],.93,(0,0,-1))]:
                neighbor=cells.get((x+step[0],y+step[1],z+step[2]))
                if len(ss)==1 and neighbor and len(shapes(neighbor))==1:
                    n=shapes(neighbor)[0]
                    if n[0]==(0,0,0) and n[1]==(1,1,1):continue
                points=[corners[i] for i in inds]
                u=[points[1][j]-points[0][j] for j in range(3)];v=[points[2][j]-points[0][j] for j in range(3)]
                normal=(u[1]*v[2]-u[2]*v[1],u[2]*v[0]-u[0]*v[2],u[0]*v[1]-u[1]*v[0])
                if normal[0]+normal[2]+normal[1]*.8<=0:continue
                depth=sum(a+c+h*.18 for a,h,c in points)/4
                polys.append((depth,[project(p) for p in points],tuple(min(255,int(t*light)) for t in co)))
    im=Image.new('RGB',(W,H),'#18262c');d=ImageDraw.Draw(im)
    for _,pts,co in sorted(polys,key=lambda p:p[0]):d.polygon(pts,fill=co)
    f=ImageFont.truetype(FONT,29);small=ImageFont.truetype(FONT,18)
    d.text((28,20),l['title']+' · '+zoneid+(' · 剖切检查' if cut else ' · 建筑蓝图'),font=f,fill='#ead6b7')
    d.text((28,H-42),'离线模型几何预览：使用现有方块模型／材质均色，不是游戏截图',font=small,fill='#b1c5c7')
    dst=OUT/f'{theme}-{zoneid}{"-cutaway" if cut else ""}.png'
    im.save(dst);return im
if __name__=='__main__':
    import sys
    data=json.loads((ROOT/'src/main/resources/data/xiantu/instance/realm_courts.json').read_text(encoding='utf8'))['layouts']
    themes=sys.argv[1:] or list(data)
    sheet=Image.new('RGB',(1440,780),'#18262c')
    for i,theme in enumerate(themes):
        l=data[theme];im=render(theme,l);sheet.paste(im.resize((480,390)),((i%3)*480,(i//3)*390))
        render(theme,l,cut=True)
        print(theme,'rendered',flush=True)
    if len(themes)==6:sheet.save(OUT/'six-architecture.png')
