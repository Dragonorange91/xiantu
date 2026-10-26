"""Build a reduced skinned Yanwei Fox LOD for animated mid-range rendering."""
import hashlib
import json
import pathlib

import bpy


ROOT = pathlib.Path(__file__).resolve().parents[1]
ANIMATION_DIR = ROOT / "asset_sources/yanwei-fox-gfbs-animation-20261014"
BLEND = ANIMATION_DIR / "yanwei_fox_animated.blend"
OUTPUT_BLEND = ANIMATION_DIR / "yanwei_fox_animated_lod.blend"
OUTPUT = ANIMATION_DIR / "yanwei_fox_animated_lod.glb"
MANIFEST = ANIMATION_DIR / "animation-lod-manifest.json"
RUNTIME_OUTPUT = ROOT / "src/main/resources/assets/xiantu/models/entity/yanwei_fox_animated_lod.glb"
DECIMATE_RATIO = 0.10
TEXTURE_SIZE = 512


bpy.ops.wm.open_mainfile(filepath=str(BLEND))
scene = bpy.context.scene
armature = next(obj for obj in scene.objects if obj.type == "ARMATURE")
source_mesh = next(obj for obj in scene.objects if obj.type == "MESH" and obj.name == "Mesh_0")
root = next(obj for obj in scene.objects if obj.name == "FoxAnimationRoot")
for obj in list(scene.objects):
    if obj.type == "MESH" and obj != source_mesh:
        bpy.data.objects.remove(obj, do_unlink=True)

mesh = source_mesh.copy()
mesh.data = source_mesh.data.copy()
mesh.name = "Mesh_0_AnimatedLod"
scene.collection.objects.link(mesh)
source_vertices = len(mesh.data.vertices)
source_triangles = sum(max(0, len(poly.vertices) - 2) for poly in mesh.data.polygons)
modifier = mesh.modifiers.new("AnimatedDistanceLod", "DECIMATE")
modifier.decimate_type = "COLLAPSE"
modifier.ratio = DECIMATE_RATIO
bpy.context.view_layer.objects.active = mesh
armature_modifier = next((item for item in mesh.modifiers if item.type == "ARMATURE"), None)
if armature_modifier is None:
    raise RuntimeError("Animated LOD source mesh has no armature modifier")
while mesh.modifiers.find(modifier.name) > mesh.modifiers.find(armature_modifier.name):
    bpy.ops.object.modifier_move_up(modifier=modifier.name)
bpy.ops.object.modifier_apply(modifier=modifier.name)

transfer = mesh.modifiers.new("RestoreBoneWeights", "DATA_TRANSFER")
transfer.object = source_mesh
transfer.use_vert_data = True
transfer.data_types_verts = {"VGROUP_WEIGHTS"}
transfer.vert_mapping = "POLYINTERP_NEAREST"
transfer.layers_vgroup_select_src = "ALL"
transfer.layers_vgroup_select_dst = "INDEX"
transfer.mix_mode = "REPLACE"
transfer.mix_factor = 1.0
bpy.context.view_layer.objects.active = mesh
bpy.ops.object.modifier_apply(modifier=transfer.name)
bpy.data.objects.remove(source_mesh, do_unlink=True)
lod_vertices = len(mesh.data.vertices)
lod_triangles = sum(max(0, len(poly.vertices) - 2) for poly in mesh.data.polygons)
for image in bpy.data.images:
    if image.source == "FILE" and image.size[0] > TEXTURE_SIZE and image.size[1] > TEXTURE_SIZE:
        image.scale(TEXTURE_SIZE, TEXTURE_SIZE)

for obj in bpy.context.selected_objects:
    obj.select_set(False)
for obj in (armature, mesh, root):
    obj.select_set(True)
bpy.context.view_layer.objects.active = armature
bpy.ops.wm.save_as_mainfile(filepath=str(OUTPUT_BLEND))

bpy.ops.export_scene.gltf(
    filepath=str(OUTPUT),
    export_format="GLB",
    use_selection=True,
    export_animations=True,
    export_animation_mode="NLA_TRACKS",
    export_skins=True,
    export_force_sampling=True,
    export_optimize_animation_size=False,
)

manifest = {
    "source_glb_sha256": hashlib.sha256((ANIMATION_DIR / "yanwei_fox_animated.glb").read_bytes()).hexdigest().upper(),
    "output_glb_sha256": hashlib.sha256(OUTPUT.read_bytes()).hexdigest().upper(),
    "decimate_ratio": DECIMATE_RATIO,
    "texture_size": TEXTURE_SIZE,
    "source_vertices": source_vertices,
    "source_triangles": source_triangles,
    "lod_vertices": lod_vertices,
    "lod_triangles": lod_triangles,
    "bones": len(armature.data.bones),
    "clips": [track.name for track in armature.animation_data.nla_tracks],
}
MANIFEST.write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")
RUNTIME_OUTPUT.parent.mkdir(parents=True, exist_ok=True)
RUNTIME_OUTPUT.write_bytes(OUTPUT.read_bytes())
print("YANWEI_FOX_ANIMATED_LOD", json.dumps(manifest, ensure_ascii=False))
