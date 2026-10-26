"""Validate the Cracked-Blade Wolf's packaged GLB assets and Forge wiring."""
import hashlib
import json
import pathlib
import re
import struct


ROOT = pathlib.Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/xiantu"
MODEL = ASSETS / "models/entity/lie_ren_lang.glb"
STATIC_LOD = ASSETS / "models/entity/lie_ren_lang_lod.glb"
SPAWN_EGG = ASSETS / "models/item/cracked_blade_wolf_spawn_egg.json"
CLIPS = {
    "lie_ren_lang.idle", "lie_ren_lang.walk", "lie_ren_lang.run",
    "lie_ren_lang.attack", "lie_ren_lang.hurt", "lie_ren_lang.death",
}


def require(condition, message):
    if not condition:
        raise AssertionError(message)


def read_glb(path):
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
lod_data, lod = read_glb(STATIC_LOD)
require(len(document.get("skins", [])) == 1, "wolf animation GLB must contain one skin")
skin = document["skins"][0]
require(16 <= len(skin.get("joints", [])) <= 32, "wolf rig joint count is outside the reviewed 16-32 range")
require(len(document.get("meshes", [])) == 1, "unexpected animated wolf mesh count")
mesh_node = next((node for node in document.get("nodes", []) if node.get("mesh") is not None), None)
require(mesh_node is not None and mesh_node.get("skin") == 0, "wolf mesh is not bound to its skin")
primitive_count = 0
vertex_count = triangle_count = 0
for mesh in document["meshes"]:
    for primitive in mesh["primitives"]:
        primitive_count += 1
        attrs = primitive["attributes"]
        for attribute in ("POSITION", "NORMAL", "TEXCOORD_0", "JOINTS_0", "WEIGHTS_0"):
            require(attribute in attrs, f"animated wolf is missing {attribute}")
        require(primitive.get("mode", 4) == 4, "wolf mesh must use triangle topology")
        vertex_count += document["accessors"][attrs["POSITION"]]["count"]
        triangle_count += document["accessors"][primitive["indices"]]["count"] // 3
require(primitive_count > 0 and vertex_count > 0 and triangle_count > 0, "animated wolf geometry is empty")
animation_names = {animation.get("name") for animation in document.get("animations", [])}
require(animation_names == CLIPS, f"wolf clip inventory mismatch: {animation_names}")
for animation in document["animations"]:
    require(animation.get("channels") and animation.get("samplers"),
            f"empty wolf animation: {animation.get('name')}")
    for channel in animation["channels"]:
        require(channel["target"].get("node") is not None, "wolf animation has an unbound channel")
        require(channel["sampler"] < len(animation["samplers"]), "wolf animation sampler index is invalid")

require(not lod.get("skins") and not lod.get("animations"), "distant wolf LOD must be static")
require(any(node.get("mesh") is not None for node in lod.get("nodes", [])), "static wolf LOD has no mesh")
require(json.loads(SPAWN_EGG.read_text(encoding="utf-8")) ==
        {"parent": "minecraft:item/template_spawn_egg"}, "wolf spawn egg item model is invalid")

entity = (ROOT / "src/main/java/com/longju/xiantu/entity/CrackedBladeWolfEntity.java").read_text(encoding="utf-8")
entities = (ROOT / "src/main/java/com/longju/xiantu/registry/ModEntities.java").read_text(encoding="utf-8")
items = (ROOT / "src/main/java/com/longju/xiantu/registry/ModItems.java").read_text(encoding="utf-8")
renderer = (ROOT / "src/main/java/com/longju/xiantu/client/CrackedBladeWolfRenderer.java").read_text(encoding="utf-8")
locales = [ASSETS / "lang/zh_cn.json", ASSETS / "lang/en_us.json"]
require("CRACKED_BLADE_WOLF" in entities and "Attributes.ATTACK_DAMAGE, 3" in entities,
        "wolf spawn registration must include its melee attack attribute")
require("CRACKED_BLADE_WOLF_SPAWN_EGG" in items, "wolf spawn egg is not registered")
for clip in CLIPS:
    require(clip in renderer, f"renderer does not route wolf clip {clip}")
require("30.0D * 30.0D" in renderer and "STATIC_LOD" in renderer,
        "renderer must retain full animation within 30 blocks and static LOD outside")
require("MODEL_SCALE = 1.7F" in renderer, "wolf model scale must preserve the reviewed in-game size increase")
offset_match = re.search(r"MODEL_VERTICAL_OFFSET\s*=\s*([0-9.]+)F", renderer)
require(offset_match is not None, "wolf renderer must compensate for its below-origin model pivot")
vertical_offset = float(offset_match.group(1))
for label, glb in (("animated", document), ("static LOD", lod)):
    position_bounds = [
        glb["accessors"][primitive["attributes"]["POSITION"]]
        for mesh in glb.get("meshes", [])
        for primitive in mesh.get("primitives", [])
        if "POSITION" in primitive.get("attributes", [])
    ]
    require(position_bounds and all("min" in accessor for accessor in position_bounds),
            f"{label} wolf positions must publish bounds")
    lowest_y = min(accessor["min"][1] for accessor in position_bounds)
    require(vertical_offset + lowest_y >= -0.01,
            f"{label} wolf feet remain below entity origin: offset={vertical_offset}, min_y={lowest_y}")
require("poses.translate(0.0D, MODEL_VERTICAL_OFFSET, 0.0D)" in renderer,
        "wolf pivot correction must apply to both animated and static render paths")
require("return !isPeacefulTestMode() && !isAlerted() && pouncePhase == 0 && super.canUse();" in entity
        and "return !isPeacefulTestMode() && !isAlerted() && pouncePhase == 0 && super.canContinueToUse();" in entity,
        "wolf melee must wait until the warning and pounce phases finish")
require("getTarget() == null && !isAlerted() && super.canUse();" in entity,
        "wolf must not roam during its hostile warning")
for stage in ('warn','trail','impact'):
    require(f'BeastVfx.emit(CrackedBladeWolfEntity.this,"{stage}"' in entity,
            f"missing server-owned wolf {stage} event")
    require((ROOT / f'src/main/resources/assets/xiantu/fx/beasts/cracked_blade_wolf/{stage}.fx').is_file(),
            f"missing packaged native Photon {stage}")
require("BeastVfx.cancel(CrackedBladeWolfEntity.this)" in entity,
        "interrupted wolf attack must cancel its telegraph")
for locale in locales:
    language = json.loads(locale.read_text(encoding="utf-8"))
    require("item.xiantu.cracked_blade_wolf_spawn_egg" in language,
            f"missing localized wolf spawn egg name in {locale.name}")

print(f"PASS animated wolf: {len(skin['joints'])} joints, {vertex_count} vertices, {triangle_count} triangles")
print("PASS clips: " + ", ".join(sorted(animation_names)))
print(f"PASS static LOD: {len(lod.get('meshes', []))} mesh(es), no skin/animation")
print(f"PASS spawn egg model/locales and Forge runtime routes; GLB sha256={hashlib.sha256(data).hexdigest().upper()}")
print(f"INFO animated={len(data)} bytes, static_lod={len(lod_data)} bytes; runtime gameplay remains unverified by this static check")
