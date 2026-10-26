"""Run inside Blender MCP: verify source surface fidelity and sampled hand deformation."""
import bpy, math, json, hashlib
import numpy as np
from mathutils import Vector, Matrix
import completion_authoring as ca
import refine_posture_grip as rp
from bl_ext.blender_org.animal_animator.core import composer

OUT=rp.OUT/'original-hand-review'
OUT.mkdir(exist_ok=True)

def capture(mesh,path,target,offset,scale):
    scene=bpy.context.scene
    for o in scene.objects:
        if o.type=='MESH': o.hide_render=o!=mesh
    scene.render.engine='BLENDER_WORKBENCH'
    scene.render.resolution_x=800;scene.render.resolution_y=800;scene.render.resolution_percentage=100
    scene.display.shading.color_type='TEXTURE';scene.display.shading.light='STUDIO'
    scene.display.shading.show_shadows=False;scene.display.shading.show_cavity=False
    scene.display.shading.background_type='VIEWPORT';scene.display.shading.background_color=(.18,.18,.18)
    data=bpy.data.cameras.new('hand-review');cam=bpy.data.objects.new('hand-review',data)
    scene.collection.objects.link(cam);scene.camera=cam;data.type='ORTHO';data.ortho_scale=scale
    cam.location=Vector(target)+Vector(offset)
    cam.rotation_euler=(Vector(target)-cam.location).to_track_quat('-Z','Y').to_euler()
    scene.render.filepath=str(path);bpy.ops.render.render(write_still=True)
    bpy.data.objects.remove(cam,do_unlink=True);bpy.data.cameras.remove(data)

def texture_hashes(mesh):
    result=[]
    for mat in mesh.data.materials:
        for node in mat.node_tree.nodes:
            if node.type=='TEX_IMAGE' and node.image:
                im=node.image
                if im.packed_file: result.append(hashlib.sha256(im.packed_file.data).hexdigest())
    return sorted(set(result))

def review(ident):
    rig,mesh=rp.select_rig(ident);s=rp.SPECS[ident]
    temporary={key:set(getattr(bpy.data,key)) for key in ('objects','actions','meshes','armatures','materials','images')}
    with bpy.data.libraries.load(str(rp.OUT/'baseline-library.blend'),link=False) as (src,dst):
        dst.objects=[ident+'_mesh']
    original=dst.objects[0];bpy.context.scene.collection.objects.link(original)
    original.parent=None;original.matrix_world.identity();original.modifiers.clear()
    original.hide_render=False;original.hide_set(False)
    before=np.array([v.co[:] for v in original.data.vertices]);after=np.array([v.co[:] for v in mesh.data.vertices])
    report={'id':ident,'topology_identical':len(original.data.vertices)==len(mesh.data.vertices) and [tuple(p.vertices) for p in original.data.polygons]==[tuple(p.vertices) for p in mesh.data.polygons],
            'uv_identical':[[tuple(x.uv) for x in uv.data] for uv in original.data.uv_layers]==[[tuple(x.uv) for x in uv.data] for uv in mesh.data.uv_layers],
            'original_images_identical':texture_hashes(original)==texture_hashes(mesh),'hands':{}}
    body={i for p in original.data.polygons if p.material_index==0 for i in p.vertices}
    align=Matrix.Rotation(math.radians(-s['yaw']),3,'Z')
    hands={}
    for side in ('R','L'):
        w=Vector(s[side]);radius=s['radius'];tip=s['tip']+w.z-s['R'][2]
        ids=[i for i in body if abs(before[i,0]-w.x)<radius and abs(before[i,1]-w.y)<radius*1.1 and tip-.012<before[i,2]<w.z-.008]
        rotation=rp.grip_rotation(ident) if side=='R' and s['weapon'] else Matrix.Identity(3)
        expected=np.array([tuple(align@(w+rotation@(Vector(before[i])-w))) for i in ids])
        err=np.linalg.norm(after[ids]-expected,axis=1)
        report['hands'][side]={'core_vertices':len(ids),'max_rigid_surface_error':float(err.max())}
        hands[side]=set(ids)
    assert report['topology_identical'] and report['uv_identical'] and report['original_images_identical'], report
    assert max(h['max_rigid_surface_error'] for h in report['hands'].values())<1e-6,report
    # Matched cameras isolate orientation from geometry differences.
    w=Vector(s['R']);target=w+Vector((0,0,-.035));rot=rp.grip_rotation(ident) if s['weapon'] else Matrix.Identity(3)
    for label,offset in [('outer',(-3,-1,0)),('palm',(3,-1,0))]:
        capture(original,OUT/(ident+'-source-'+label+'.png'),target,offset,s['radius']*3.8)
        capture(mesh,OUT/(ident+'-rest-'+label+'.png'),align@(w+rot@(target-w)),align@rot@Vector(offset),s['radius']*3.8)
    bpy.data.objects.remove(original,do_unlink=True)
    for key in ('objects','actions','meshes','armatures','materials','images'):
        collection=getattr(bpy.data,key)
        for item in set(collection)-temporary[key]:collection.remove(item,do_unlink=True)
    edges=[tuple(e.vertices) for e in mesh.data.edges if all(i in hands['R'] for i in e.vertices)]
    edge_array=np.array(edges);rest_lengths=np.linalg.norm(after[edge_array[:,0]]-after[edge_array[:,1]],axis=1)
    valid=rest_lengths>1e-5
    report['sampled_actions']={}
    for clip in ('idle','walk','run','attack','combo','cast','hurt','death'):
        ca.reset(rig);action=bpy.data.actions[ident+'.'+clip];composer.assign_action(rig,action)
        end=int(action.frame_range[1]);stretch=[]
        for f in sorted(set([0,end//4,end//2,3*end//4,end])):
            bpy.context.scene.frame_set(f);bpy.context.view_layer.update()
            evaluated=mesh.evaluated_get(bpy.context.evaluated_depsgraph_get())
            pts=np.array([v.co[:] for v in evaluated.data.vertices])
            lengths=np.linalg.norm(pts[edge_array[:,0]]-pts[edge_array[:,1]],axis=1)
            stretch.extend(abs(lengths[valid]/rest_lengths[valid]-1).tolist())
            if clip in ('attack','cast','combo') and f==end//2:
                hand=rig.pose.bones['hand.R'];centre=hand.matrix@rig.data.bones['hand.R'].matrix_local.inverted()@Vector(mesh.get('grip_centre',rig.data.bones['hand.R'].tail_local))
                offset=hand.matrix.to_3x3()@rig.data.bones['hand.R'].matrix_local.to_3x3().inverted()@align@rot@Vector((-3,-1,0))
                capture(mesh,OUT/(ident+'-'+clip+'.png'),centre,offset,s['radius']*4.7)
        report['sampled_actions'][clip]={'max_edge_strain':float(max(stretch)),'p95_edge_strain':float(np.quantile(stretch,.95))}
    ca.reset(rig);mesh.hide_render=False
    mesh['posture_revision']='original-surface-grip-v2'
    (OUT/(ident+'-audit.json')).write_text(json.dumps(report,indent=2),encoding='utf-8')
    print(json.dumps(report))
    return report
