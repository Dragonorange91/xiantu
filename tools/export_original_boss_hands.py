"""Export only the five reviewed Bosses from Blender MCP; preserve production until validation."""
import bpy, json, hashlib
from pathlib import Path
import completion_authoring as ca
import refine_posture_grip as rp
from bl_ext.blender_org.animal_animator.core import composer

OUT=rp.OUT/'ready'
OUT.mkdir(exist_ok=True)

def export(ident):
    rig,mesh=rp.select_rig(ident)
    assert mesh.get('posture_revision')=='original-surface-grip-v2'
    if ident in rp.HOLDING:assert mesh.get('holding_motion_v3')
    meta=json.loads((ca.ROOT/'asset_sources/creature-completion-20261017'/(ident+'-manifest.json')).read_text(encoding='utf8'))
    ca.reset(rig);scene=bpy.context.scene;scene.render.fps=24
    bpy.ops.object.select_all(action='DESELECT');mesh.select_set(True);rig.select_set(True);bpy.context.view_layer.objects.active=rig
    path=OUT/(ident+'.glb')
    bpy.ops.export_scene.gltf(filepath=str(path),export_format='GLB',use_selection=True,use_active_scene=True,export_animations=True,export_animation_mode='NLA_TRACKS',export_force_sampling=True,export_frame_range=False,export_skins=True,export_all_influences=False,export_yup=True,export_image_format='AUTO')
    # Match the visible idle pose for static far LOD, including the new forearm stance.
    composer.assign_action(rig,bpy.data.actions[ident+'.idle']);scene.frame_set(0);bpy.context.view_layer.update()
    ev=mesh.evaluated_get(bpy.context.evaluated_depsgraph_get())
    data=bpy.data.meshes.new_from_object(ev,preserve_all_data_layers=True,depsgraph=bpy.context.evaluated_depsgraph_get())
    lod=bpy.data.objects.new(ident+'_lod_review',data);scene.collection.objects.link(lod)
    bpy.ops.object.select_all(action='DESELECT');lod.select_set(True);bpy.context.view_layer.objects.active=lod
    dec=lod.modifiers.new('far_static_only','DECIMATE');dec.ratio=.55;bpy.ops.object.modifier_apply(modifier=dec.name)
    lod.vertex_groups.clear();lod.data.validate(clean_customdata=True);lod.data.update()
    lodpath=OUT/(ident+'_lod.glb')
    bpy.ops.export_scene.gltf(filepath=str(lodpath),export_format='GLB',use_selection=True,use_active_scene=True,export_animations=False,export_skins=False,export_yup=True,export_image_format='AUTO')
    bpy.data.objects.remove(lod,do_unlink=True)
    ca.reset(rig)
    meta.update(animated_sha256=hashlib.sha256(path.read_bytes()).hexdigest(),lod_sha256=hashlib.sha256(lodpath.read_bytes()).hexdigest(),joints=len(rig.data.bones),authoring='Blender MCP; original hand surface/UV/maps preserved; per-finger binding; source-weight isolation; differentiated angled weapon poses and grounded attack arcs',status='candidate_exported_pending_runtime_and_user_review')
    meta['posture_revision']='original-surface-grip-v2';meta['holding']=rp.HOLDING.get(ident)
    meta['original_hand_review']=str(rp.OUT/'original-hand-review'/(ident+'-audit.json'))
    (OUT/(ident+'-manifest.json')).write_text(json.dumps(meta,ensure_ascii=False,indent=2),encoding='utf8')
    bpy.data.libraries.write(str(OUT/(ident+'-editable.blend')),{scene},fake_user=True,compress=True)
    print('EXPORTED',ident,meta['joints'],meta['animated_sha256'])
