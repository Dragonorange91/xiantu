"""Per-source anatomical calibration, executed inside Blender through MCP.

Starts from the saved pre-correction authoring scene. UVs and original textures remain intact.
"""
import bpy, bmesh, math, json, sys
from pathlib import Path
from mathutils import Vector, Matrix, Quaternion
import numpy as np
sys.path.insert(0,str(Path(__file__).parent))
import completion_authoring as ca
from bl_ext.blender_org.animal_animator.core import composer, posing
OUT=ca.ROOT/'asset_sources/posture-grip-20261018'

def restore(ident):
    """Reload only this model's original datablocks; keep the live MCP timer alive."""
    rig=bpy.data.objects[ident+'_rig']
    scene=next((s for s in rig.users_scene if s.name.startswith('CompletionRig_')),rig.users_scene[0])
    bpy.context.window.scene=scene
    if bpy.context.object and bpy.context.object.mode!='OBJECT':bpy.ops.object.mode_set(mode='OBJECT')
    for ob in list(scene.objects):
        if ob.name.startswith(ident+'_') and ob.type in ('MESH','ARMATURE'):
            bpy.data.objects.remove(ob,do_unlink=True)
    for action in list(bpy.data.actions):
        if action.name.startswith(ident+'.'):bpy.data.actions.remove(action)
    with bpy.data.libraries.load(str(OUT/'baseline-library.blend'),link=False) as (src,dst):
        dst.objects=[ident+'_mesh',ident+'_rig']
    for ob in dst.objects:
        if ob and ob.name not in scene.objects:scene.collection.objects.link(ob)
    ca.reset(bpy.data.objects[ident+'_rig'])
    print('RESTORED',ident)

# Body and head are independently aligned from orthographic anatomical inspection.
SPECS={
 'bai_zhan_jiang_hun':dict(yaw=25,head=10,R=(-.198,-.098,.495),L=(.195,.036,.493),knuckle=.462,tip=.416,radius=.040,weapon=True),
 'xing_yun_tian_jiang':dict(yaw=-16,head=14,R=(-.218,-.022,.490),L=(.179,-.143,.485),knuckle=.457,tip=.420,radius=.041,weapon=True),
 'tong_ling_yuan_wang':dict(yaw=-25,head=23,R=(-.311,.030,.425),L=(.210,-.115,.425),knuckle=.385,tip=.332,radius=.063,weapon=True),
 'xuan_tian_cheng_zhu':dict(yaw=24,head=4,R=(-.170,-.190,.466),L=(.235,-.025,.480),knuckle=.420,tip=.365,radius=.050,weapon=False),
 'yun_hai_tian_jun':dict(yaw=21,head=14,R=(-.115,-.145,.490),L=(.165,.020,.470),knuckle=.455,tip=.420,radius=.041,weapon=False),
}
HOLDING={
 'bai_zhan_jiang_hun':{'axis':(-.55,-.45,-.70),'forearm':-10,'grip_fraction':.16,'roll':70},
 'xing_yun_tian_jiang':{'axis':(-.40,-.50,.768),'forearm':-12,'grip_fraction':.38},
 'tong_ling_yuan_wang':{'axis':(.82,-.42,.39),'forearm':-22,'grip_fraction':.50},
}

def grip_rotation(ident):
    alignment=Matrix.Rotation(math.radians(-SPECS[ident]['yaw']),3,'Z')
    target=alignment.inverted()@Vector(HOLDING[ident]['axis']).normalized()
    return Vector((0,-1,0)).rotation_difference(target).to_matrix()

def select_rig(ident):
    rig=bpy.data.objects[ident+'_rig'];mesh=bpy.data.objects[ident+'_mesh']
    bpy.context.window.scene=next(s for s in rig.users_scene if s.name.startswith('CompletionRig_'))
    ca.reset(rig)
    for tr in rig.animation_data.nla_tracks:tr.mute=True
    bpy.ops.object.select_all(action='DESELECT');rig.select_set(True);bpy.context.view_layer.objects.active=rig
    return rig,mesh

def smooth(a,b,x):
    t=max(0,min(1,(x-a)/(b-a)));return t*t*(3-2*t)

def transform(p,s):
    p=Vector(p);t=smooth(.72,.79,p.z)
    angle=-s['yaw']*(1-t)-s['head']*t
    return Matrix.Rotation(math.radians(angle),3,'Z')@p

def clear_weights(mesh,index):
    for g in list(mesh.data.vertices[index].groups):mesh.vertex_groups[g.group].remove([index])

def weight(mesh,index,name,w=1):
    g=mesh.vertex_groups.get(name) or mesh.vertex_groups.new(name=name);g.add([index],w,'REPLACE')

def segment_distance(p,a,b):
    direction=b-a;t=max(0,min(1,(p-a).dot(direction)/max(1e-10,direction.length_squared)))
    return (p-(a+direction*t)).length,t

def source_grip(ident,rig,mesh,original,body,weapons,hand_info,s):
    """Preserve source finger surfaces exactly; orient the wrist and fit the weapon shaft."""
    selected,chains,wrist,knuckle,tip=hand_info['R']
    alignment=Matrix.Rotation(math.radians(-s['yaw']),3,'Z')
    rotation=grip_rotation(ident)
    def orient(p):return alignment@(wrist+rotation@(p-wrist))
    for i in selected:
        p=original[i];blend=1-smooth(wrist.z-.008,wrist.z+.012,p.z)
        mesh.data.vertices[i].co=(alignment@p).lerp(orient(p),blend)
    bpy.context.view_layer.objects.active=rig;bpy.ops.object.mode_set(mode='EDIT')
    h=rig.data.edit_bones['hand.R'];h.head=alignment@wrist;h.tail=orient(Vector((wrist.x,wrist.y,knuckle)))
    for digit,points in chains.items():
        for j in range(3):
            b=rig.data.edit_bones[f'{digit}_{j+1}.R'];b.head=orient(points[j]);b.tail=orient(points[j+1])
    centre0=sum((chains[d][1] for d in ('index','middle','ring','little')),Vector())/4+Vector((.012,0,.005))
    centre=orient(centre0);axis=(alignment@rotation@Vector((0,-1,0))).normalized()
    socket=rig.data.edit_bones.new('grip_socket.R');socket.head=centre;socket.tail=centre+axis*.04;socket.parent=h
    bpy.ops.object.mode_set(mode='OBJECT')
    pts=[original[i] for i in weapons];lo=min(p.z for p in pts);hi=max(p.z for p in pts)
    fraction=HOLDING[ident]['grip_fraction'];z=lo+(hi-lo)*fraction
    cross=[]
    for edge in mesh.data.edges:
        a,b=edge.vertices
        if a not in weapons or b not in weapons:continue
        p,q=original[a],original[b]
        if min(p.z,q.z)<=z<=max(p.z,q.z) and abs(p.z-q.z)>1e-8:cross.append(p.lerp(q,(z-p.z)/(q.z-p.z)))
    assert cross
    source_grip=sum(cross,Vector())/len(cross);source_grip.z=z
    weapon_rotation=Quaternion(axis,math.radians(HOLDING[ident].get('roll',0))).to_matrix()@Vector((0,0,1)).rotation_difference(axis).to_matrix()@alignment
    for i in weapons:
        mesh.data.vertices[i].co=centre+weapon_rotation@(original[i]-source_grip);clear_weights(mesh,i);weight(mesh,i,'grip_socket.R')
    mesh['grip_centre']=list(centre);mesh['grip_fraction']=fraction
    mesh.data.update()

def correct(ident):
    s=SPECS[ident];rig,mesh=select_rig(ident)
    assert 'index_1.R' not in rig.data.bones,'Restore baseline before recalibration'
    original=[v.co.copy() for v in mesh.data.vertices]
    body={i for p in mesh.data.polygons if p.material_index==0 for i in p.vertices}
    weapons=set(range(len(original)))-body
    # Continuous de-twist preserves seams at the neck and all UV coordinates.
    for i in body:mesh.data.vertices[i].co=transform(original[i],s)
    bpy.ops.object.mode_set(mode='EDIT')
    for b in rig.data.edit_bones:
        b.head=transform(b.head,s);b.tail=transform(b.tail,s)
    hand_info={}
    for side in ('R','L'):
        sign=-1 if side=='R' else 1
        wrist=Vector(s[side]);r=s['radius'];shift=wrist.z-s['R'][2]
        knuckle=s['knuckle']+shift;tip=s['tip']+shift
        selected=[i for i in body if abs(original[i].x-wrist.x)<r and abs(original[i].y-wrist.y)<r*1.1 and tip-.012<original[i].z<wrist.z+.012]
        if len(selected)<20:raise RuntimeError((ident,side,'hand mask too small',len(selected)))
        # Four distal digit centres are isolated by a low transverse slice.
        low=[original[i] for i in selected if original[i].z<tip+.022]
        if len(low)<8:low=[original[i] for i in selected if original[i].z<knuckle-.012]
        if len(low)<8:raise RuntimeError((ident,side,'fingertip landmarks miss actual geometry'))
        a=np.array([list(v) for v in low]);lo,hi=np.quantile(a[:,1],[.12,.88])
        centres=[]
        for y in np.linspace(lo,hi,4):
            neighbours=sorted(low,key=lambda p:abs(p.y-y))[:max(3,len(low)//6)]
            centres.append(sum(neighbours,Vector())/len(neighbours))
        chains={}
        for digit,end in zip(('little','ring','middle','index'),centres):
            start=Vector((wrist.x*.35+end.x*.65,end.y,knuckle))
            length=max(.024,(end-start).length)
            end.z=min(end.z,knuckle-.024)
            points=[start,start.lerp(end,.43),start.lerp(end,.77),end]
            chains[digit]=points
        if ident=='bai_zhan_jiang_hun' and side=='R':
            chains.update({name:[Vector(p) for p in pts] for name,pts in {
                'little':[(-.210,-.086,.457),(-.210,-.088,.442),(-.200,-.085,.431),(-.199,-.085,.426)],
                'ring':[(-.210,-.097,.460),(-.207,-.097,.441),(-.197,-.096,.427),(-.193,-.096,.419)],
                'middle':[(-.209,-.108,.461),(-.205,-.108,.438),(-.196,-.107,.423),(-.190,-.106,.414)],
                'index':[(-.199,-.119,.464),(-.199,-.119,.440),(-.190,-.117,.426),(-.185,-.117,.418)]}.items()})
        thumb_end=Vector((wrist.x-sign*r*.67,wrist.y-r*.55,knuckle-.006))
        thumb_root=Vector((wrist.x-sign*r*.32,wrist.y-r*.3,wrist.z-.012))
        chains['thumb']=[thumb_root,thumb_root.lerp(thumb_end,.4),thumb_root.lerp(thumb_end,.75),thumb_end]
        if ident=='bai_zhan_jiang_hun' and side=='R':
            chains['thumb']=[Vector(p) for p in [(-.184,-.115,.477),(-.174,-.119,.465),(-.170,-.121,.452),(-.164,-.122,.444)]]
        h=rig.data.edit_bones['hand.'+side];h.head=transform(wrist,s);h.tail=transform((wrist.x,wrist.y,knuckle),s)
        forearm=rig.data.edit_bones['arm_lower.'+side];forearm.tail=h.head
        for digit,points in chains.items():
            for j in range(3):
                name=f'{digit}_{j+1}.{side}';b=rig.data.edit_bones.new(name)
                b.head=transform(points[j],s);b.tail=transform(points[j+1],s)
                b.parent=h if j==0 else rig.data.edit_bones[f'{digit}_{j}.{side}'];b.use_deform=True
        hand_info[side]=(selected,chains,wrist,knuckle,tip)
    bpy.ops.object.mode_set(mode='OBJECT')
    # Spatial masks exclude nearby robes and the separately authored weapons.
    for side,(selected,chains,wrist,knuckle,tip) in hand_info.items():
        sign=-1 if side=='R' else 1
        for i in selected:
            p=original[i];clear_weights(mesh,i)
            thumb_d=min(segment_distance(p,a,b)[0] for a,b in zip(chains['thumb'],chains['thumb'][1:]))
            if p.z>knuckle+.005 and thumb_d>.012:
                t=smooth(wrist.z-.008,wrist.z+.012,p.z)
                weight(mesh,i,'hand.'+side,1-t)
                if t:weight(mesh,i,'arm_lower.'+side,t)
                continue
            # Never blend different fingers just because their surfaces touch.
            # Choose one anatomical chain, then interpolate only its adjacent joints.
            candidates=[]
            for digit,points in chains.items():
                if ident=='bai_zhan_jiang_hun' and side=='R' and digit=='thumb' and p.x<-.187:continue
                for j in range(3):
                    dist,t=segment_distance(p,points[j],points[j+1]);candidates.append((dist,digit,j,t))
            dist,digit,j,t=min(candidates)
            nearest=[(dist,f'{digit}_{j+1}.{side}')];inv=[1.0]
            if t<.25 and j>0:
                inv=[.5+t*2,.5-t*2];nearest.append((dist,f'{digit}_{j}.{side}'))
            elif t>.75 and j<2:
                inv=[1-(t-.75)*2,(t-.75)*2];nearest.append((dist,f'{digit}_{j+2}.{side}'))
            total=sum(inv)
            palm=smooth(knuckle-.008,knuckle+.009,p.z) if thumb_d>.012 else 0
            if palm:weight(mesh,i,'hand.'+side,palm)
            for (dist,name),w in zip(nearest,inv):weight(mesh,i,name,(1-palm)*w/total)
    # Geodesic smoothing across actual mesh edges prevents Voronoi weight seams
    # from folding the knuckle webbing into sharp ridges. Never smooth in space
    # across two neighbouring but disconnected fingers.
    selected={i for data in hand_info.values() for i in data[0]}
    neighbours={i:[] for i in selected}
    for e in mesh.data.edges:
        a,b=e.vertices
        if a in neighbours:neighbours[a].append(b)
        if b in neighbours:neighbours[b].append(a)
    all_weights={v.index:{mesh.vertex_groups[g.group].name:g.weight for g in v.groups} for v in mesh.data.vertices}
    for iteration in range(18):
        changed={}
        for i,ns in neighbours.items():
            if not ns:continue
            ws={n:w*.55 for n,w in all_weights[i].items()}
            for j in ns:
                for n,w in all_weights[j].items():ws[n]=ws.get(n,0)+w*.45/len(ns)
            largest=sorted(ws.items(),key=lambda x:-x[1])[:4];total=sum(w for n,w in largest)
            changed[i]={n:w/total for n,w in largest}
        all_weights.update(changed)
    for i in selected:
        clear_weights(mesh,i)
        for n,w in all_weights[i].items():weight(mesh,i,n,w)
    # The imported automatic bind leaked upper-arm/robe weights into fingers.
    # Edge smoothing must never reintroduce those remote influences.
    for side,(indices,chains,wrist,knuckle,tip) in hand_info.items():
        hand_groups={'hand.'+side}|{f'{digit}_{j}.{side}' for digit in chains for j in range(1,4)}
        local=set(indices)
        for i in body:
            v=mesh.data.vertices[i]
            ws={mesh.vertex_groups[g.group].name:g.weight for g in v.groups if g.weight>1e-8}
            if i in local:
                ws={n:w for n,w in ws.items() if n in hand_groups}
                total=sum(ws.values())
                if total<1e-8:ws={'hand.'+side:1};total=1
                wrist_blend=smooth(wrist.z-.008,wrist.z+.012,original[i].z)
                ws={n:w/total*(1-wrist_blend) for n,w in ws.items()}
                if wrist_blend:ws['arm_lower.'+side]=wrist_blend
            elif any(n in hand_groups for n in ws):
                ws={n:w for n,w in ws.items() if n not in hand_groups}
                if not ws:ws={'arm_lower.'+side:1}
            else:continue
            ws=dict(sorted(ws.items(),key=lambda x:-x[1])[:4]);total=sum(ws.values())
            clear_weights(mesh,i)
            for n,w in ws.items():
                if w>1e-8:weight(mesh,i,n,w/total)
    if s['weapon']:
        source_grip(ident,rig,mesh,original,body,weapons,hand_info,s)
    # Key every new digit, including relaxed left hand and caster gestures, in every existing clip.
    finger_names=[b.name for b in rig.data.bones if b.name.split('_')[0] in ('thumb','index','middle','ring','little')]
    actions=[a for a in bpy.data.actions if a.name.startswith(ident+'.')]
    for a in actions:
        composer.assign_action(rig,a);end=round(a.frame_range[1]);clip=a.name.split('.',1)[1]
        arm_samples=[]
        if s['weapon']:
            for f in range(end+1):
                bpy.context.scene.frame_set(f)
                arm_samples.append(rig.pose.bones['arm_lower.R'].rotation_quaternion.copy())
        for f in range(end+1):
            bpy.context.scene.frame_set(f)
            if s['weapon']:
                arm=rig.pose.bones['arm_lower.R']
                axis=arm.bone.matrix_local.to_3x3().inverted()@Vector((1,0,0))
                arm.rotation_quaternion=Quaternion(axis,math.radians(HOLDING[ident]['forearm']))@arm_samples[f]
                arm.keyframe_insert(data_path='rotation_quaternion',frame=f,group=arm.name)
            for name in finger_names:
                angle=0
                if clip in ('cast','attack') and not s['weapon']:angle=4*math.sin(math.pi*f/end)
                elif name.endswith('.L') and clip in ('idle','walk','run'):angle=2*math.sin(math.tau*f/end)
                axis=Matrix.Rotation(math.radians(-s['yaw']),3,'Z')@Vector((0,1,0))
                ca.rotate(rig,name,axis,angle*(-1 if name.endswith('.R') else 1))
                pb=rig.pose.bones[name]
                for prop in ('rotation_quaternion','location','scale'):pb.keyframe_insert(data_path=prop,frame=f,group=name)
    ca.reset(rig)
    mesh['posture_revision']='original-surface-grip-v2'
    (OUT/(ident+'-calibration.json')).write_text(json.dumps({'spec':s,'hand_vertex_counts':{side:len(info[0]) for side,info in hand_info.items()},'finger_bones':finger_names,'joints':len(rig.data.bones),'grip_centre':list(mesh.get('grip_centre',[]))},indent=2),encoding='utf8')
    print('CALIBRATED',ident,len(rig.data.bones),{side:len(info[0]) for side,info in hand_info.items()})

if __name__=='__main__':
    for ident in SPECS:correct(ident)
