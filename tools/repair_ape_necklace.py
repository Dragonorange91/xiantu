"""Blender MCP: preserve every original bead, expose it above the corrected mane,
and restore the continuous dark cord between them. No replacement bead meshes."""
import bpy,math,json
import numpy as np
from mathutils import Vector,Matrix
import completion_authoring as ca
import refine_posture_grip as rp
import audit_original_boss_hands as audit
OUT=ca.ROOT/'asset_sources/boss-combat-v4-20261018'
def repair():
    ident='tong_ling_yuan_wang';rig,mesh=rp.select_rig(ident)
    assert not mesh.get('necklace_cord_v4'),'Do not append the cord twice'
    with bpy.data.libraries.load(str(rp.OUT/'baseline-library.blend'),link=False) as (src,dst):dst.objects=[ident+'_mesh']
    source=dst.objects[0];body={i for f in source.data.polygons if f.material_index==0 for i in f.vertices};adj={i:[] for i in body}
    for e in source.data.edges:
        a,b=e.vertices
        if a in body and b in body:adj[a].append(b);adj[b].append(a)
    remaining=set(body);beads=[];rot=Matrix.Rotation(math.radians(-rp.SPECS[ident]['yaw']),3,'Z')
    while remaining:
        q=[remaining.pop()];part=[]
        while q:
            i=q.pop();part.append(i)
            for j in adj[i]:
                if j in remaining:remaining.remove(j);q.append(j)
        centre=sum((source.data.vertices[i].co for i in part),Vector())/len(part)
        if 18<=len(part)<=32 and .65<centre.z<.83:beads.append((part,rot@centre))
    assert len(beads)>=12,len(beads)
    centres=np.array([c[:] for _,c in beads]);origin=centres.mean(axis=0);_,_,axes=np.linalg.svd(centres-origin)
    xy=(centres-origin)@axes[:2].T;order=np.argsort(np.arctan2(xy[:,1],xy[:,0]));points=[]
    errors=[]
    for k in order:
        part,centre=beads[k]
        # Keep original bead scale/rotation/UV. Bring the loop above the mane surface.
        offset=Vector((float(centre.x-origin[0])*.12,-.020,float(centre.z-origin[2])*.06))
        for i in part:
            mesh.data.vertices[i].co=rot@source.data.vertices[i].co+offset
            rp.clear_weights(mesh,i);rp.weight(mesh,i,'chest')
        points.append(centre+offset)
        errors.append(max((mesh.data.vertices[i].co-(rot@source.data.vertices[i].co+offset)).length for i in part))
    bpy.data.objects.remove(source,do_unlink=True)
    previous=bpy.data.objects.get('ape_necklace_cord')
    if previous:bpy.data.objects.remove(previous,do_unlink=True)
    curve=bpy.data.curves.new('Original necklace continuous cord','CURVE');curve.dimensions='3D';curve.resolution_u=1
    spline=curve.splines.new('POLY');spline.points.add(len(points)-1)
    for p,co in zip(spline.points,points):p.co=(*co,1)
    spline.use_cyclic_u=True;curve.bevel_depth=.004;curve.bevel_resolution=1;curve.resolution_u=1
    cord=bpy.data.objects.new('ape_necklace_cord',curve);bpy.context.scene.collection.objects.link(cord)
    mat=bpy.data.materials.get('Necklace dark woven cord') or bpy.data.materials.new('Necklace dark woven cord');mat.diffuse_color=(.045,.029,.012,1);mat.use_nodes=True
    bsdf=next(n for n in mat.node_tree.nodes if n.type=='BSDF_PRINCIPLED');bsdf.inputs['Base Color'].default_value=(.045,.029,.012,1);bsdf.inputs['Roughness'].default_value=.85;cord.data.materials.append(mat)
    bpy.ops.object.select_all(action='DESELECT');cord.select_set(True);bpy.context.view_layer.objects.active=cord;bpy.ops.object.convert(target='MESH')
    cord=bpy.context.object;g=cord.vertex_groups.new(name='chest');g.add(list(range(len(cord.data.vertices))),1,'REPLACE')
    added=len(cord.data.polygons);mesh.select_set(True);bpy.context.view_layer.objects.active=mesh;bpy.ops.object.join()
    mesh['necklace_cord_v4']=True;mesh['necklace_cord_faces']=added
    audit.capture(mesh,OUT/'ape-necklace-restored.png',(0,0,.77),(0,-3,.2),.54)
    (OUT/'necklace-reconstruction.json').write_text(json.dumps({'source_beads':len(beads),'bead_shape_max_error':max(errors),'original_bead_uv_preserved':True,'added_cord_faces':added,'binding':'chest; shared existing rig'},indent=2),encoding='utf8')
    print('NECKLACE',len(beads),'source beads',added,'cord faces')
