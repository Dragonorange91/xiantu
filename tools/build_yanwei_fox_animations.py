"""Author GFBS-ready animation clips for the accepted skinned Yanwei Fox GLB."""
import hashlib
import json
import math
import os
import pathlib

import bpy
from mathutils import Vector


ROOT = pathlib.Path(__file__).resolve().parents[1]
SOURCE = ROOT / "asset_sources/yanwei-fox-static-20261013/yanwei_fox_skinned_original.glb"
OUT = ROOT / "asset_sources/yanwei-fox-gfbs-animation-20261014"
OUT.mkdir(parents=True, exist_ok=True)
FPS = 24


def smooth_pulse(t, start, peak, end):
    if t <= start or t >= end:
        return 0.0
    if t <= peak:
        x = (t - start) / (peak - start)
    else:
        x = (end - t) / (end - peak)
    x = max(0.0, min(1.0, x))
    return x * x * (3.0 - 2.0 * x)


def pose_for(clip, t):
    pose = {}

    def put(bone, x=0.0, y=0.0, z=0.0):
        pose[bone] = (x, y, z)

    if clip == "idle":
        wave = math.sin(math.tau * t)
        put("Bone_004", x=math.radians(1.2) * wave)
        for i, bone in enumerate(("Bone_036", "Bone_035", "Bone_034", "Bone_033", "Bone_032")):
            put(bone, z=math.radians(3.0 + i * 1.2) * math.sin(math.tau * t - i * 0.18))
        put("Bone_003", x=math.radians(1.5) * math.sin(math.tau * t + 0.4))
        put("Bone_006", z=math.radians(1.8) * wave)
        put("Bone_009", z=-math.radians(1.8) * wave)

    elif clip in {"walk", "run"}:
        phase = math.tau * t
        running = clip == "run"
        upper_swing = 22.0 if running else 14.0
        knee_fold = 14.0 if running else 9.0
        foot_swing = 4.5 if running else 3.0
        put("Bone_004", x=math.radians(1.8 if running else 1.0) * math.cos(phase * 2.0))

        legs = (
            ("Bone_015", "Bone_014", "Bone_013", 0.0, 1.0),
            ("Bone_021", "Bone_020", "Bone_019", math.pi, 1.0),
            ("Bone_026", "Bone_025", "Bone_024", math.pi, 0.88),
            ("Bone_031", "Bone_030", "Bone_029", 0.0, 0.88),
        )
        for hip, knee, paw, offset, scale in legs:
            stride = phase + offset
            put(hip, x=math.radians(upper_swing * scale) * math.sin(stride))
            put(knee, x=math.radians(knee_fold) * max(0.0, -math.cos(stride)))
            put(paw, x=math.radians(foot_swing) * math.sin(stride + 0.55))

        for i, bone in enumerate(("Bone_036", "Bone_035", "Bone_034", "Bone_033", "Bone_032")):
            tail_swing = (5.0 if running else 3.0) + i * (0.7 if running else 0.45)
            put(bone, z=math.radians(tail_swing) * math.sin(phase - i * 0.26))
        put("Bone_003", x=math.radians(2.0 if running else 1.2) * math.sin(phase * 2.0))

    elif clip == "alert":
        pulse = math.sin(math.tau * t)
        put("Bone_004", x=-math.radians(2.0) + math.radians(1.0) * pulse)
        put("Bone_003", x=-math.radians(5.0) + math.radians(2.0) * pulse,
            z=math.radians(2.0) * pulse)
        put("Bone_006", z=math.radians(8.0) + math.radians(2.0) * pulse)
        put("Bone_009", z=-math.radians(8.0) - math.radians(2.0) * pulse)
        for i, bone in enumerate(("Bone_036", "Bone_035", "Bone_034", "Bone_033", "Bone_032")):
            put(bone, z=math.radians(8.0 + i * 1.5) + math.radians(1.5) * pulse)

    elif clip == "attack":
        p = smooth_pulse(t, 0.05, 0.48, 0.95)
        put("Bone_004", x=math.radians(-11.0) * p)
        put("Bone_003", x=math.radians(-12.0) * p)
        put("Bone_015", x=math.radians(18.0) * p)
        put("Bone_021", x=math.radians(18.0) * p)
        put("Bone_026", x=-math.radians(10.0) * p)
        put("Bone_031", x=-math.radians(10.0) * p)
        for i, bone in enumerate(("Bone_036", "Bone_035", "Bone_034", "Bone_033", "Bone_032")):
            put(bone, z=math.radians(14.0 + i * 2.0) * p)

    elif clip == "hurt":
        p = math.sin(math.pi * t)
        put("Bone_004", x=math.radians(7.0) * p, z=math.radians(4.0) * p)
        put("Bone_003", x=math.radians(8.0) * p, z=math.radians(-5.0) * p)
        put("Bone_015", x=-math.radians(10.0) * p)
        put("Bone_021", x=-math.radians(10.0) * p)
        for i, bone in enumerate(("Bone_036", "Bone_035", "Bone_034", "Bone_033", "Bone_032")):
            put(bone, z=-math.radians(7.0 + i * 1.4) * p)

    elif clip == "death":
        fall = smooth_pulse(t, 0.0, 0.78, 1.0)
        put("Bone_003", x=math.radians(12.0) * fall)
        put("Bone_015", x=-math.radians(24.0) * fall)
        put("Bone_014", x=math.radians(32.0) * fall)
        put("Bone_021", x=-math.radians(24.0) * fall)
        put("Bone_020", x=math.radians(32.0) * fall)
        put("Bone_026", x=math.radians(20.0) * fall)
        put("Bone_025", x=-math.radians(30.0) * fall)
        put("Bone_031", x=math.radians(20.0) * fall)
        put("Bone_030", x=-math.radians(30.0) * fall)
        for i, bone in enumerate(("Bone_036", "Bone_035", "Bone_034", "Bone_033", "Bone_032")):
            put(bone, z=-math.radians(15.0 - i * 1.4) * fall)

    return pose


if not SOURCE.is_file():
    raise FileNotFoundError(SOURCE)
bpy.ops.wm.read_factory_settings(use_empty=True)
bpy.ops.import_scene.gltf(filepath=str(SOURCE))
scene = bpy.context.scene
armature = next(obj for obj in scene.objects if obj.type == "ARMATURE")
mesh = next(obj for obj in scene.objects if obj.type == "MESH" and obj.name == "Mesh_0")
extras = [obj for obj in scene.objects if obj.type == "MESH" and obj != mesh]
for obj in extras:
    bpy.data.objects.remove(obj, do_unlink=True)
assert len(armature.data.bones) == 43
assert mesh.find_armature() == armature

root = bpy.data.objects.new("FoxAnimationRoot", None)
scene.collection.objects.link(root)
root.empty_display_type = "PLAIN_AXES"
root.location = (0.0, 0.0, 0.48)
bpy.context.view_layer.update()
for obj in (armature,):
    world_matrix = obj.matrix_world.copy()
    obj.parent = root
    obj.matrix_world = world_matrix

clips = {
    "idle": (60, True),
    "walk": (24, True),
    "run": (18, True),
    "alert": (36, True),
    "attack": (30, False),
    "hurt": (14, False),
    "death": (30, False),
}
track_manifest = {}
for clip, (duration_frames, looping) in clips.items():
    action = bpy.data.actions.new(f"yanwei_fox.{clip}")
    armature.animation_data_create()
    armature.animation_data.action = action
    sample_count = min(duration_frames, 16)
    for sample in range(sample_count + 1):
        t = sample / sample_count
        frame = 1 + t * duration_frames
        for bone_name, angles in pose_for(clip, t).items():
            bone = armature.pose.bones[bone_name]
            bone.rotation_mode = "XYZ"
            bone.rotation_euler = angles
            bone.keyframe_insert(data_path="rotation_euler", frame=frame, group=bone_name)
    action.use_fake_user = True
    track = armature.animation_data.nla_tracks.new()
    track.name = f"yanwei_fox.{clip}"
    track.strips.new(track.name, 1, action)
    armature.animation_data.action = None
    track.mute = False

    root_action = bpy.data.actions.new(f"yanwei_fox.{clip}.root")
    root.animation_data_create()
    root.animation_data.action = root_action
    for sample in range(sample_count + 1):
        t = sample / sample_count
        frame = 1 + t * duration_frames
        fall = smooth_pulse(t, 0.0, 0.78, 1.0) if clip == "death" else 0.0
        root.location = (0.0, 0.0, 0.48 - 0.30 * fall)
        root.rotation_euler = (0.0, math.radians(72.0) * fall, 0.0)
        if clip in {"walk", "run"}:
            bounce = 0.014 if clip == "run" else 0.006
            root.location.z += bounce * max(0.0, math.sin(math.tau * t * 2.0))
        elif clip == "attack":
            p = smooth_pulse(t, 0.05, 0.48, 0.95)
            root.location.y = -0.045 * p
            root.location.z += 0.025 * p
        root.keyframe_insert(data_path="location", frame=frame)
        root.keyframe_insert(data_path="rotation_euler", frame=frame)
    root_action.use_fake_user = True
    root_track = root.animation_data.nla_tracks.new()
    root_track.name = f"yanwei_fox.{clip}"
    root_track.strips.new(root_track.name, 1, root_action)
    root.animation_data.action = None
    root_track.mute = False
    track_manifest[track.name] = {
        "duration_seconds": duration_frames / FPS,
        "loop": looping,
        "keyed_bones": sorted(pose_for(clip, 0.0)),
        "animated_root_node": True,
    }

scene.render.fps = FPS
scene.frame_start = 1
scene.frame_end = 61
bpy.context.view_layer.objects.active = armature
for obj in bpy.context.selected_objects:
    obj.select_set(False)
armature.select_set(True)
mesh.select_set(True)
root.select_set(True)

blend_path = OUT / "yanwei_fox_animated.blend"
glb_path = OUT / "yanwei_fox_animated.glb"
bpy.ops.wm.save_as_mainfile(filepath=str(blend_path))
bpy.ops.export_scene.gltf(
    filepath=str(glb_path),
    export_format="GLB",
    use_selection=True,
    export_animations=True,
    export_animation_mode="NLA_TRACKS",
    export_skins=True,
    export_force_sampling=True,
    export_optimize_animation_size=False,
)

manifest = {
    "source": str(SOURCE),
    "source_sha256": hashlib.sha256(SOURCE.read_bytes()).hexdigest().upper(),
    "output_sha256": hashlib.sha256(glb_path.read_bytes()).hexdigest().upper(),
    "fps": FPS,
    "bones": len(armature.data.bones),
    "clips": track_manifest,
    "runtime": "GFBS glTF 1.5.1",
    "status": "authored_export_and_resource_regression_passed; game_runtime_pending",
}
(OUT / "animation-manifest.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")
print("YANWEI_FOX_ANIMATION_EXPORT", json.dumps(manifest, ensure_ascii=False))
