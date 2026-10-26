"""Author editable combat clips and portal geometry through the live Blockbench MCP."""
from pathlib import Path
import json, math, base64, sys
from blockbench_native_bridge import NativeBridge

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'asset_sources/combat-realms-20261026'
OUT.mkdir(parents=True,exist_ok=True)
B=NativeBridge()
def call(action,**kw):
    result=B.call(action,**kw)
    return result
def shot(name,**kw):
    r=call('screenshot',width=900,height=760,annotate=False,view='front_right',**kw)
    (OUT/(name+'.response.json')).write_text(json.dumps(r),encoding='utf8')
    def find(v):
        if isinstance(v,str) and v.startswith('data:image/'): return v
        if isinstance(v,dict):
            for a in v.values():
                f=find(a)
                if f:return f
        if isinstance(v,list):
            for a in v:
                f=find(a)
                if f:return f
    image=find(r)
    if image:(OUT/(name+'.png')).write_bytes(base64.b64decode(image.split(',',1)[1]))
def save(name):
    call('save_project',path=str(OUT/(name+'.bbmodel')))
    return json.loads((OUT/(name+'.bbmodel')).read_text('utf8'))

def motions():
    call('new_project',format='free',name='xiantu_weapon_choreography',texture_width=16,texture_height=16)
    # Minecraft player proxy: source clips are independent of each weapon's mesh.
    call('add_groups',groups=[{'name':n,'origin':o} for n,o in [('body',[0,12,0]),('right_arm',[5,22,0]),('left_arm',[-5,22,0]),('weapon',[5,12,-2])]])
    call('add_cubes',cubes=[{'name':n,'parent':p,'from':a,'to':b} for n,p,a,b in [
        ('torso','body',[-4,12,-2],[4,24,2]),('arm_right','right_arm',[4,10,-2],[8,22,2]),
        ('arm_left','left_arm',[-8,10,-2],[-4,22,2]),('haft','weapon',[4.5,11,-24],[5.5,12,-1]),
        ('tip','weapon',[4,10.5,-28],[6,12.5,-24])]])
    families={
      'sword_cut': [[60,-36,26],[92,30,-30],[80,42,-38],[35,12,-12],[0,0,0]],
      'sword_thrust':[[70,-12,5],[98,-4,0],[91,0,0],[48,-4,2],[0,0,0]],
      'spear_thrust':[[72,-8,3],[102,0,0],[95,2,0],[56,-4,2],[0,0,0]],
      'axe_slam':[[148,-8,12],[56,8,-8],[32,12,-12],[20,5,-4],[0,0,0]],
      'staff_sweep':[[82,-65,18],[84,45,-28],[70,63,-32],[40,20,-10],[0,0,0]],
      'guard':[[66,-28,20],[84,-38,28],[84,-38,28],[58,-22,15],[0,0,0]],
      'staff_slam':[[138,0,5],[54,0,-3],[32,0,-5],[24,0,-2],[0,0,0]],
    }
    lengths={'sword_cut':.65,'sword_thrust':.6,'spear_thrust':.65,'axe_slam':.9,'staff_sweep':.85,'guard':1.05,'staff_slam':.95}
    for name,poses in families.items():
        length=lengths[name];call('create_animation',name=name,length=length,loop='once')
        times=[0,.10,.24,.62,1];keys=[]
        for i,(t,p) in enumerate(zip(times,poses)):
            for bone,rot in [('right_arm',p),('left_arm',[v*.62 for v in p]),('body',[0,p[1]*.15,0])]:
                keys.append(dict(bone=bone,channel='rotation',time=round(t*length,4),value=rot,interpolation='catmullrom'))
            thrust=name in ('sword_thrust','spear_thrust')
            pos=[0,[.2,.1,0,0,0][i],([1,-9,-8,-2,0] if thrust else [0,0,0,0,0])[i]]
            keys.append(dict(bone='weapon',channel='position',time=round(t*length,4),value=pos,interpolation='catmullrom'))
            # Weapon held forward in proxy; runtime applies the same authored wrist rotation.
            rot=[p[0]-poses[0][0],p[1]*.5,p[2]*.5] if i<4 else [0,0,0]
            keys.append(dict(bone='weapon',channel='rotation',time=round(t*length,4),value=rot,interpolation='catmullrom'))
        call('add_keyframes',animation=name,keyframes=keys)
    data=save('weapon_choreography')
    # Compile the actual saved MCP-edited keyframes, not the pre-edit Python arrays.
    clips={}
    for a in data['animations']:
        tracks={}
        for v in a['animators'].values():
            if v.get('type')!='bone':continue
            tracks[v['name']]={}
            for k in v['keyframes']:
                d=k['data_points'][0]
                tracks[v['name']].setdefault(k['channel'],[]).append([k['time'],*[float(d[x]) for x in ('x','y','z')]])
        clips[a['name']]={'length':a['length'],'tracks':tracks}
    dest=ROOT/'src/main/resources/assets/xiantu/animations/weapon_choreography.json'
    dest.parent.mkdir(parents=True,exist_ok=True);dest.write_text(json.dumps(clips,indent=2),encoding='utf8')
    for name in ['spear_thrust','axe_slam','staff_sweep']:
        result=call('preview_animation',animation=name,times=[0,.1,lengths[name]*.5,lengths[name]],views=['front_right'],width=420,height=420)
        (OUT/(name+'-preview.json')).write_text(json.dumps(result),encoding='utf8')
    print('MCP motion clips exported:',len(clips),flush=True)

def portal():
    call('new_project',format='free',name='realm_astral_gate',texture_width=16,texture_height=16)
    call('add_groups',groups=[{'name':'frame','origin':[0,0,0]}])
    cubes=[]
    def cube(name,a,b,material='stone',rotation=None,origin=None):
        c={'name':name,'parent':'frame','from':a,'to':b,'box_uv':False,'autouv':0}
        if rotation:c.update(rotation=rotation,origin=origin)
        cubes.append(c)
    # A faceted circular crown above a grounded two-foot gate. Opening stays 5x4 blocks.
    for i in range(20):
        a=math.pi*2*i/20; x,y=math.sin(a)*45,math.cos(a)*37+32
        cube('bronze_segment_%02d'%i,[x-6.9,y-3.3,-5],[x+6.9,y+3.3,5],rotation=[0,0,-math.degrees(a)],origin=[x,y,0])
        cube('jade_inlay_%02d'%i,[x-4.8,y-1.1,-5.3],[x+4.8,y+1.1,-5.05],rotation=[0,0,-math.degrees(a)],origin=[x,y,0])
    for sign in [-1,1]:
        x=sign*48
        for k in range(3):cube('plinth_%s_%s'%(sign,k),[x-11+k*2,-16+k*3,-10+k],[x+11-k*2,-13+k*3,10-k])
        cube('pillar_%s'%sign,[x-4,-7,-4],[x+4,32,4])
        for y in [0,12,26,36]:cube('collar_%s_%s'%(sign,y),[x-5.5,y,-5.5],[x+5.5,y+2,5.5])
        for k in range(3):cube('wing_%s_%s'%(sign,k),[x-3,38+k*5,-3],[x+3,47+k*5,3],rotation=[0,0,-sign*(28+k*12)],origin=[x,38+k*5,0])
    cube('crown_socket',[-8,65,-6],[8,70,6])
    cube('jade_crown',[-5,70,-3],[5,80,3],rotation=[0,0,45],origin=[0,75,0])
    call('add_cubes',cubes=cubes)
    shot('portal-blockout')
    for name,file in [('stone','black_rock_carved.png'),('jade','azure_cloud_spirit_brick.png')]:
        call('import_texture',path=str(ROOT/'src/main/resources/assets/xiantu/textures/block'/file),name=name)
    for c in cubes:
        file='azure_cloud_spirit_brick.png' if c['name'].startswith('jade') else 'black_rock_carved.png'
        a,b=c['from'],c['to'];dx,dy,dz=[b[i]-a[i] for i in range(3)]
        faces={f:{'texture':file,'uv':[0,0,min(w,16),min(h,16)]} for f,w,h in [('north',dx,dy),('south',dx,dy),('east',dz,dy),('west',dz,dy),('up',dx,dz),('down',dx,dz)]}
        call('set_cube_uv',cube=c['name'],faces=faces)
    save('realm_astral_gate');shot('portal-textured')
    result=call('check_model');(OUT/'portal-audit.json').write_text(json.dumps(result,indent=2),encoding='utf8')
    print('MCP gate saved:',len(cubes),'elements',flush=True)

if __name__=='__main__':
    globals()[sys.argv[1]]()
