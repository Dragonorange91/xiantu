"""Render representative poses from each authored Yanwei Fox action."""
import pathlib
import sys

import bpy
from mathutils import Vector


ROOT = pathlib.Path(__file__).resolve().parents[1]
BLEND = ROOT / "asset_sources/yanwei-fox-gfbs-animation-20261014/yanwei_fox_animated.blend"
OUT = ROOT / "asset_sources/yanwei-fox-gfbs-animation-20261014/previews"
if "--" in sys.argv:
    BLEND = pathlib.Path(sys.argv[sys.argv.index("--") + 1])
    OUT = BLEND.parent / "previews_animated_lod"
OUT.mkdir(parents=True, exist_ok=True)
bpy.ops.wm.open_mainfile(filepath=str(BLEND))
scene = bpy.context.scene
scene.render.engine = "BLENDER_EEVEE"
scene.render.resolution_x = 900
scene.render.resolution_y = 700
scene.render.resolution_percentage = 100
scene.render.image_settings.file_format = "PNG"
scene.render.film_transparent = False
scene.view_settings.view_transform = "Standard"
scene.view_settings.look = "Medium High Contrast"

world = bpy.data.worlds.new("FoxPreviewWorld")
world.color = (0.23, 0.28, 0.34)
scene.world = world

for index, (position, energy, size) in enumerate((((2, -3, 4), 500, 4), ((-3, 1, 2), 300, 3))):
    data = bpy.data.lights.new(f"FoxPreviewLight{index}", "AREA")
    data.energy = energy
    data.shape = "DISK"
    data.size = size
    light = bpy.data.objects.new(data.name, data)
    scene.collection.objects.link(light)
    light.location = position
    light.rotation_euler = (Vector((0, 0, 0.62)) - light.location).to_track_quat("-Z", "Y").to_euler()

camera_data = bpy.data.cameras.new("FoxPreviewCamera")
camera = bpy.data.objects.new("FoxPreviewCamera", camera_data)
scene.collection.objects.link(camera)
camera.location = (2.5, -4.7, 1.5)
camera.rotation_euler = (Vector((0, 0, 0.62)) - camera.location).to_track_quat("-Z", "Y").to_euler()
camera_data.type = "ORTHO"
camera_data.ortho_scale = 2.55
scene.camera = camera

armature = next(obj for obj in scene.objects if obj.type == "ARMATURE")
tracks = {track.name.rsplit(".", 1)[-1]: track for track in armature.animation_data.nla_tracks}
root = next(obj for obj in scene.objects if obj.name == "FoxAnimationRoot")
root_tracks = {track.name.rsplit(".", 1)[-1]: track for track in root.animation_data.nla_tracks}
samples = {"idle": 31, "walk": 13, "run": 10, "alert": 18, "attack": 15, "hurt": 7, "death": 24}
for name, frame in samples.items():
    for track in armature.animation_data.nla_tracks:
        track.mute = track != tracks[name]
    for track in root.animation_data.nla_tracks:
        track.mute = track != root_tracks[name]
    scene.frame_set(frame)
    scene.render.filepath = str(OUT / f"{name}.png")
    bpy.ops.render.render(write_still=True)

print("YANWEI_FOX_PREVIEWS", str(OUT))
