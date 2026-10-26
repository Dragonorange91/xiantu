"""Candidate export only; production integration is a separate, verified step."""
import bpy,json,hashlib
import completion_authoring as ca
import refine_posture_grip as rp
from bl_ext.blender_org.animal_animator.core import composer
from boss_combat_catalogue import MODELS,catalogue
BASE=ca.ROOT/'asset_sources/boss-replacement-v6-20261018'
OUT=BASE/'ready'
ca.OUT=BASE
OUT.mkdir(parents=True,exist_ok=True)

def export(boss,ident):
    rig,mesh=rp.select_rig(ident);assert mesh.get('combat_v5')
    # Remove orphan NLA entries and duplicate actions rather than exporting stale clip suffixes.
    clips={s.action.name for tr in rig.animation_data.nla_tracks for s in tr.strips if s.action}
    expected={ident+'.'+s['id'] for s in catalogue()[boss]}|{ident+'.'+c for c in ('idle','walk','run','hurt','death','attack')}
    assert expected<=clips,(ident,expected-clips)
    bpy.context.scene.render.fps=24;ca.reset(rig)
    bpy.ops.object.select_all(action='DESELECT');mesh.select_set(True);rig.select_set(True);bpy.context.view_layer.objects.active=rig
    dest=OUT/(ident+'.glb')
    bpy.ops.export_scene.gltf(filepath=str(dest),export_format='GLB',use_selection=True,use_active_scene=True,export_animations=True,export_animation_mode='NLA_TRACKS',export_force_sampling=True,export_frame_range=False,export_skins=True,export_all_influences=False,export_yup=True,export_image_format='AUTO')
    composer.assign_action(rig,bpy.data.actions[ident+'.idle']);bpy.context.scene.frame_set(0);bpy.context.view_layer.update()
    graph=bpy.context.evaluated_depsgraph_get();data=bpy.data.meshes.new_from_object(mesh.evaluated_get(graph),preserve_all_data_layers=True,depsgraph=graph)
    lod=bpy.data.objects.new(ident+'_v4lod',data);bpy.context.scene.collection.objects.link(lod)
    bpy.ops.object.select_all(action='DESELECT');lod.select_set(True);bpy.context.view_layer.objects.active=lod
    dec=lod.modifiers.new('static_distant','DECIMATE');dec.ratio=.55;bpy.ops.object.modifier_apply(modifier=dec.name);lod.vertex_groups.clear();lod.data.validate();lod.data.update()
    lodpath=OUT/(ident+'_lod.glb')
    bpy.ops.export_scene.gltf(filepath=str(lodpath),export_format='GLB',use_selection=True,use_active_scene=True,export_animations=False,export_skins=False,export_yup=True,export_image_format='AUTO')
    bpy.data.objects.remove(lod,do_unlink=True);ca.reset(rig)
    metadata=ca.OUT/(ident+'-manifest.json')
    if not metadata.exists():metadata=ca.OUT/(ident.replace('_','-')+'-manifest.json')
    meta=json.loads(metadata.read_text(encoding='utf8'))
    meta.update(animated_sha256=hashlib.sha256(dest.read_bytes()).hexdigest(),lod_sha256=hashlib.sha256(lodpath.read_bytes()).hexdigest(),joints=len(rig.data.bones),clips=sorted(x.removeprefix(ident+'.') for x in clips),
        authoring='Blender MCP, original surface + UV, rigid facial and ornament constraints, source finger bones, dedicated server-timed skill choreography',posture_revision='boss-replacement-v6',status='candidate_exported_not_gameplay_accepted')
    (OUT/(ident+'-manifest.json')).write_text(json.dumps(meta,ensure_ascii=False,indent=2),encoding='utf8')
    bpy.data.libraries.write(str(OUT/(ident+'-editable.blend')),{bpy.context.scene},fake_user=True,compress=True)
    print('EXPORTED',ident,len(clips),meta['animated_sha256'])

if __name__=='__main__':
    for boss in ('xuantians_remnant','cloud_sea_lord'):export(boss,MODELS[boss])
