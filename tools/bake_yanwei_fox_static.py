"""Bake the authored rest pose of the Yanwei Fox GLB into a static mesh."""
import argparse
import pathlib
import sys

import bpy


def parse_args():
    args = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else []
    parser = argparse.ArgumentParser()
    parser.add_argument("--input", required=True, type=pathlib.Path)
    parser.add_argument("--output", required=True, type=pathlib.Path)
    return parser.parse_args(args)


args = parse_args()
if not args.input.is_file():
    raise FileNotFoundError(args.input)
args.output.parent.mkdir(parents=True, exist_ok=True)

bpy.ops.object.select_all(action="SELECT")
bpy.ops.object.delete(use_global=False)
bpy.ops.import_scene.gltf(filepath=str(args.input), bone_heuristic="TEMPERANCE")

mesh_objects = [obj for obj in bpy.context.scene.objects if obj.type == "MESH"]
if len(mesh_objects) != 1:
    raise RuntimeError(f"Expected exactly one mesh, found {len(mesh_objects)}")

mesh_obj = mesh_objects[0]
depsgraph = bpy.context.evaluated_depsgraph_get()
evaluated = mesh_obj.evaluated_get(depsgraph)
baked_mesh = bpy.data.meshes.new_from_object(evaluated, depsgraph=depsgraph, preserve_all_data_layers=True)
old_mesh = mesh_obj.data
mesh_obj.modifiers.clear()
mesh_obj.parent = None
mesh_obj.matrix_parent_inverse.identity()
mesh_obj.matrix_world = evaluated.matrix_world.copy()
mesh_obj.data = baked_mesh
if old_mesh.users == 0:
    bpy.data.meshes.remove(old_mesh)

for obj in list(bpy.context.scene.objects):
    if obj != mesh_obj:
        bpy.data.objects.remove(obj, do_unlink=True)

bpy.ops.object.select_all(action="DESELECT")
mesh_obj.select_set(True)
bpy.context.view_layer.objects.active = mesh_obj
bpy.ops.export_scene.gltf(
    filepath=str(args.output),
    export_format="GLB",
    use_selection=True,
    export_apply=True,
    export_animations=False,
    export_skins=False,
    export_image_format="AUTO",
    export_materials="EXPORT",
)

print(f"Baked rest pose: {mesh_obj.name}; vertices={len(mesh_obj.data.vertices)}; polygons={len(mesh_obj.data.polygons)}; output={args.output}")
