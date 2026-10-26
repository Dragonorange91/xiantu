"""Validate the standalone Crag-Rock Spider GLB authoring candidate."""
from __future__ import annotations

import hashlib
import json
import pathlib
import struct


ROOT = pathlib.Path(__file__).resolve().parents[1]
OUT = ROOT / "asset_sources/creature-rig-pilot-20261017"
SOURCE = (pathlib.Path.home() / 'Downloads/裂岩蛛.glb')
MODEL = OUT / "裂岩蛛_骨骼动作候选.glb"
LOD = OUT / "裂岩蛛_静态LOD_候选.glb"
CLIPS = {
    "lie_yan_zhu.idle",
    "lie_yan_zhu.walk",
    "lie_yan_zhu.run",
    "lie_yan_zhu.attack",
    "lie_yan_zhu.hurt",
    "lie_yan_zhu.death",
}


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def read_glb(path: pathlib.Path):
    data = path.read_bytes()
    require(len(data) >= 28, f"truncated GLB: {path.name}")
    magic, version, length = struct.unpack_from("<III", data, 0)
    require(magic == 0x46546C67 and version == 2 and length == len(data),
            f"invalid GLB header/length: {path.name}")
    json_length, json_type = struct.unpack_from("<II", data, 12)
    require(json_type == 0x4E4F534A and 20 + json_length <= len(data),
            f"invalid JSON chunk: {path.name}")
    document = json.loads(data[20:20 + json_length].decode("utf-8").rstrip(" \t\r\n\0"))
    binary_header = 20 + json_length
    binary_length, binary_type = struct.unpack_from("<II", data, binary_header)
    require(binary_type == 0x004E4942 and binary_header + 8 + binary_length == len(data),
            f"invalid BIN chunk: {path.name}")
    return data, document


data, document = read_glb(MODEL)
lod_data, lod = read_glb(LOD)
require(len(document.get("skins", [])) == 1, "spider animation GLB must contain one skin")
joint_count = len(document["skins"][0].get("joints", []))
require(joint_count == 27, f"spider rig joint count changed: {joint_count}")
require(len(document.get("meshes", [])) == 1, "unexpected animated spider mesh count")
mesh_nodes = [node for node in document.get("nodes", []) if node.get("mesh") is not None]
require(len(mesh_nodes) == 1 and mesh_nodes[0].get("skin") == 0,
        "spider mesh is not bound to its skin")

primitive_count = 0
vertex_count = triangle_count = 0
for mesh in document["meshes"]:
    for primitive in mesh["primitives"]:
        primitive_count += 1
        attrs = primitive["attributes"]
        for attribute in ("POSITION", "NORMAL", "TEXCOORD_0", "JOINTS_0", "WEIGHTS_0"):
            require(attribute in attrs, f"animated spider is missing {attribute}")
        require(primitive.get("mode", 4) == 4, "spider mesh must use triangle topology")
        vertex_count += document["accessors"][attrs["POSITION"]]["count"]
        require("indices" in primitive, "animated spider primitive must be indexed")
        triangle_count += document["accessors"][primitive["indices"]]["count"] // 3
require(primitive_count == 1 and vertex_count == 4726 and triangle_count == 4652,
        f"animated spider geometry changed: primitives={primitive_count}, vertices={vertex_count}, triangles={triangle_count}")

animation_names = {animation.get("name") for animation in document.get("animations", [])}
require(animation_names == CLIPS, f"spider clip inventory mismatch: {animation_names}")
for animation in document["animations"]:
    require(animation.get("channels") and animation.get("samplers"),
            f"empty spider animation: {animation.get('name')}")
    for channel in animation["channels"]:
        require(channel["target"].get("node") is not None, "spider animation has an unbound channel")
        require(channel["sampler"] < len(animation["samplers"]), "spider sampler index is invalid")

require(not lod.get("skins") and not lod.get("animations"), "spider LOD must be static")
lod_primitives = [primitive for mesh in lod.get("meshes", []) for primitive in mesh.get("primitives", [])]
require(len(lod_primitives) == 1 and lod_primitives[0].get("mode", 4) == 4,
        "spider LOD must contain one triangle primitive")
lod_triangles = lod["accessors"][lod_primitives[0]["indices"]]["count"] // 3
require(lod_triangles == 2093, f"spider LOD triangle count changed: {lod_triangles}")

entities = (ROOT / "src/main/java/com/longju/xiantu/registry/ModEntities.java").read_text(encoding="utf-8")
items = (ROOT / "src/main/java/com/longju/xiantu/registry/ModItems.java").read_text(encoding="utf-8")
client = (ROOT / "src/main/java/com/longju/xiantu/client/XiantuClient.java").read_text(encoding="utf-8")
renderer = (ROOT / "src/main/java/com/longju/xiantu/client/CragRockSpiderRenderer.java").read_text(encoding="utf-8")
require("CRAG_ROCK_SPIDER" in entities and "Attributes.ATTACK_DAMAGE, 5" in entities,
        "spider entity registration must include its melee attack attribute")
require("CRAG_ROCK_SPIDER_SPAWN_EGG" in items, "spider spawn egg is not registered")
require("CragRockSpiderRenderer.reloadListener()" in client
        and "ModEntities.CRAG_ROCK_SPIDER.get()" in client,
        "spider renderer is not wired to client setup")
for clip in CLIPS:
    require(clip in renderer, f"spider renderer does not route {clip}")
require("30.0D * 30.0D" in renderer and "STATIC_LOD" in renderer,
        "spider renderer must retain 30-block animation distance and static LOD")
require((ROOT / "src/main/resources/assets/xiantu/models/entity/lie_yan_zhu.glb").read_bytes() == data,
        "packaged animated spider does not match audited candidate")
require((ROOT / "src/main/resources/assets/xiantu/models/entity/lie_yan_zhu_lod.glb").read_bytes() == lod_data,
        "packaged spider LOD does not match audited candidate")

print(f"PASS animated spider: {joint_count} joints, {vertex_count} vertices, {triangle_count} triangles")
print("PASS clips: " + ", ".join(sorted(animation_names)))
print(f"PASS static LOD: {lod_triangles} triangles, no skin/animation")
print(f"PASS source sha256={hashlib.sha256(SOURCE.read_bytes()).hexdigest().upper()}")
print(f"INFO animated sha256={hashlib.sha256(data).hexdigest().upper()}")
print(f"INFO static_lod sha256={hashlib.sha256(lod_data).hexdigest().upper()}")
print("PASS Forge entity, renderer, spawn egg, and packaged GLB wiring")
print("INFO natural spawn, combat drops, skills, and gameplay remain pending")

