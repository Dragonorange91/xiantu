"""Validate the animated Yanwei Fox GLB and its Forge/GFBS runtime wiring."""
import hashlib
import json
import pathlib
import struct


ROOT = pathlib.Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/xiantu"
from beast_anatomy_contract import pre_anatomy
MODEL = pre_anatomy('yanwei_fox')
STATIC_LOD = ASSETS / "models/entity/yanwei_fox_lod.glb"
MANIFEST = ROOT / "asset_sources/creature-batch-20261016/fox-animation-manifest.json"
CLIPS = {
    "yanwei_fox.idle", "yanwei_fox.walk", "yanwei_fox.run", "yanwei_fox.alert",
    "yanwei_fox.attack", "yanwei_fox.hurt", "yanwei_fox.death", "yanwei_fox.tail",
}


def require(condition, message):
    if not condition:
        raise AssertionError(message)


def read_glb(path):
    content = path.read_bytes()
    magic, version, length = struct.unpack_from("<III", content, 0)
    require(magic == 0x46546C67 and version == 2 and length == len(content), f"invalid GLB header: {path.name}")
    json_length, json_type = struct.unpack_from("<II", content, 12)
    require(json_type == 0x4E4F534A, f"first chunk is not JSON: {path.name}")
    document = json.loads(content[20:20 + json_length].decode("utf-8").rstrip(" \t\r\n\0"))
    binary_header = 20 + json_length
    binary_length, binary_type = struct.unpack_from("<II", content, binary_header)
    require(binary_type == 0x004E4942 and binary_header + 8 + binary_length == len(content),
            f"invalid GLB binary chunk: {path.name}")
    return document, content[binary_header + 8:]


def read_float_accessor(document, binary, accessor_index):
    accessor = document["accessors"][accessor_index]
    require(accessor["componentType"] == 5126, "animation output is not float data")
    width = {"SCALAR": 1, "VEC3": 3, "VEC4": 4}[accessor["type"]]
    view = document["bufferViews"][accessor["bufferView"]]
    item_bytes = width * 4
    stride = view.get("byteStride", item_bytes)
    start = view.get("byteOffset", 0) + accessor.get("byteOffset", 0)
    return [struct.unpack_from("<" + "f" * width, binary, start + i * stride)
            for i in range(accessor["count"])]


data = MODEL.read_bytes()
digest = hashlib.sha256(data).hexdigest().upper()
manifest = json.loads(MANIFEST.read_text(encoding="utf-8"))
require(digest == manifest["output_sha256"], "runtime model differs from the authored animation export")
source_path = pathlib.Path(manifest["source"])
require(source_path.is_file(), "authored source GLB is missing")
require(hashlib.sha256(source_path.read_bytes()).hexdigest().upper() == manifest["source_sha256"],
        "animation source GLB hash changed")
require(manifest["runtime"] == "GFBS glTF 1.5.1", "unexpected model runtime")

magic, version, declared_length = struct.unpack_from("<III", data, 0)
require(magic == 0x46546C67 and version == 2 and declared_length == len(data), "invalid GLB header/length")
json_length, json_type = struct.unpack_from("<II", data, 12)
require(json_type == 0x4E4F534A, "first GLB chunk is not JSON")
document = json.loads(data[20:20 + json_length].decode("utf-8").rstrip(" \t\r\n\0"))
binary_header = 20 + json_length
binary_length, binary_type = struct.unpack_from("<II", data, binary_header)
require(binary_type == 0x004E4942 and binary_header + 8 + binary_length == len(data), "invalid GLB BIN chunk")
binary = data[binary_header + 8:]

views = document["bufferViews"]
for view in views:
    require(view.get("buffer", 0) == 0, "external/multiple buffer is not supported")
    start = view.get("byteOffset", 0)
    require(start >= 0 and start + view["byteLength"] <= len(binary), "bufferView exceeds BIN chunk")

require(len(document.get("scenes", [])) == 1, "expected one scene")
require(len(document.get("skins", [])) == 1, "expected one skinned rig")
require(len(document["skins"][0]["joints"]) == manifest["bones"], "skin joint count differs from the asset manifest")
require(len(document.get("meshes", [])) == 1 and len(document["materials"]) == 1, "unexpected mesh/material change")
require(len(document.get("images", [])) == 3, "the original embedded texture set was not retained")
for image in document["images"]:
    require(image.get("mimeType") == "image/jpeg", "unexpected embedded image format")
    require(0 <= image["bufferView"] < len(views), "invalid embedded image bufferView")

mesh_node = next((node for node in document["nodes"] if node.get("mesh") is not None), None)
require(mesh_node is not None and mesh_node.get("skin") == 0, "mesh is not bound to the supplied skin")
primitive_count = vertex_count = index_count = 0
for mesh in document["meshes"]:
    for primitive in mesh["primitives"]:
        primitive_count += 1
        attributes = primitive["attributes"]
        for attribute in ("POSITION", "NORMAL", "TEXCOORD_0", "JOINTS_0", "WEIGHTS_0"):
            require(attribute in attributes, f"missing skinned mesh attribute {attribute}")
        require(primitive.get("mode", 4) == 4, "expected triangle-list mesh")
        vertex_count += document["accessors"][attributes["POSITION"]]["count"]
        index_count += document["accessors"][primitive["indices"]]["count"]
require(primitive_count == 1 and vertex_count == manifest["vertices"] and index_count == manifest["indices"],
        "mesh geometry differs from the reviewed animated candidate")

animations = document.get("animations", [])
animation_names = {animation["name"] for animation in animations}
require(animation_names == CLIPS, f"animation clips mismatch: {animation_names}")
for animation in animations:
    require(animation["channels"] and animation["samplers"], f"empty animation {animation['name']}")
    for channel in animation["channels"]:
        require(channel["target"].get("node") is not None, "animation channel has no node target")
        require(channel["target"].get("path") in {"rotation", "translation", "scale"}, "unsupported animation channel")
        sampler = animation["samplers"][channel["sampler"]]
        require(sampler["input"] < len(document["accessors"]) and sampler["output"] < len(document["accessors"]), "invalid animation sampler")

for clip_name in ("yanwei_fox.walk", "yanwei_fox.run"):
    clip = next(animation for animation in animations if animation["name"] == clip_name)
    animated_nodes = {channel["target"]["node"] for channel in clip["channels"]}
    require(len(animated_nodes) >= 20, f"{clip_name} must animate the body, legs, and tail")
    require(any(channel["target"].get("path") == "rotation" for channel in clip["channels"]),
            f"{clip_name} has no joint rotation channels")
    leg_names = {"hind_foot.L", "hind_foot.R", "hind_lower.L", "hind_lower.R",
                 "hind_upper.L", "hind_upper.R", "front_foot.L", "front_foot.R",
                 "front_lower.L", "front_lower.R", "front_upper.L", "front_upper.R"}
    leg_nodes = {i for i, node in enumerate(document["nodes"]) if node.get("name") in leg_names}
    leg_channels = [channel for channel in clip["channels"]
                    if channel["target"].get("node") in leg_nodes and channel["target"].get("path") == "rotation"]
    require(leg_channels, f"{clip_name} does not animate a leg joint")
    movement = False
    for channel in leg_channels:
        values = read_float_accessor(document, binary, clip["samplers"][channel["sampler"]]["output"])
        if any(max(row[axis] for row in values) - min(row[axis] for row in values) > 0.02
               for axis in range(len(values[0]))):
            movement = True
            break
    require(movement, f"{clip_name} leg rotation channels contain no visible keyframe movement")

entity = (ROOT / "src/main/java/com/longju/xiantu/entity/YanweiFoxEntity.java").read_text(encoding="utf-8")
entities = (ROOT / "src/main/java/com/longju/xiantu/registry/ModEntities.java").read_text(encoding="utf-8")
renderer = (ROOT / "src/main/java/com/longju/xiantu/client/YanweiFoxRenderer.java").read_text(encoding="utf-8")
client = (ROOT / "src/main/java/com/longju/xiantu/client/XiantuClient.java").read_text(encoding="utf-8")
build = (ROOT / "build.gradle").read_text(encoding="utf-8").lower()
metadata = (ROOT / "src/main/resources/META-INF/mods.toml").read_text(encoding="utf-8").lower()
require("geckolib" not in entity.lower() and "geckolib" not in renderer.lower(), "GeckoLib code must stay removed")
require("geckolib" not in build and 'modid="geckolib"' not in metadata, "GeckoLib must not be a project dependency")
require("ClientGltfApi.models().load(MODEL)" in renderer, "missing asynchronous GFBS asset load")
require("new GltfInstance(asset)" in renderer and "GltfRenderer.render(renderInstance" in renderer,
        "missing per-entity GFBS instance render")
require("instance.update(deltaSeconds)" in renderer and "System.nanoTime()" in renderer,
        "animation must advance using elapsed seconds")
require("instance.animations().play(requested, options)" in renderer, "missing runtime state-to-clip routing")
require('requested.endsWith(".run")' in renderer,
        "the run animation must loop while the fox remains in its running state")
require("isDeadOrDying()" in renderer and "hurtTime > 0" in renderer and "getDeltaMovement()" in renderer, "missing death/hurt/movement routing")
require("fox.isAlerted()" in renderer and "fox.isAttacking()" in renderer,
        "hostile fox animation must use synchronized warning and attack states")
require("ANIMATION_DISTANCE_SQR = 30.0D * 30.0D" in renderer,
        "animated skinned model must remain active within 30 blocks")
require("boolean animate = camera == null || distanceSqr <= ANIMATION_DISTANCE_SQR" in renderer
        and "renderInstance = animate ? entry.instance : entry.staticLod" in renderer,
        "use the full animated model within 30 blocks and the static model beyond")
require("animatedLod" not in renderer and "ANIMATED_LOD" not in renderer,
        "experimental animated LOD must not be wired into the renderer")
require('return "yanwei_fox.attack"' in renderer and 'return "yanwei_fox.run"' in renderer,
        "hostile attack and run states must select distinct animations")
require("Axis.YP.rotationDegrees(-entityYaw)" in renderer,
        "the GLB forward axis must align with Minecraft entity yaw without a 180-degree reversal")
require("PLAYER_ALERT_DISTANCE = 16.0D" in entity and "WARNING_TICKS = 30" in entity,
        "hostile warning must cover 16 blocks and last 1.5 seconds")
require("new NearestAttackableTargetGoal<>(this, Player.class" in entity
        and "new FoxfireAttackGoal()" in entity,
        "missing hostile player targeting or fire projectile attack")
require("new HurtByTargetGoal(this)" in entity and "targetSelector.addGoal(0" in entity,
        "being hit must acquire the attacker as a target")
require("getNavigation().isInProgress()" in entity and "fox.isRunning()" in renderer
        and 'return \"yanwei_fox.run\"' in renderer,
        "actual target navigation must enter the run animation")
require("RUNNING_AWAY" not in entity and "isRunningAway" not in renderer,
        "hostile pursuit must not be routed through a flee-only state")
require("public boolean hurt(DamageSource source, float amount)" in entity
        and "setTarget(attacker)" in entity and "warningUntil = level().getGameTime()" in entity,
        "an accepted hit must immediately retaliate instead of restarting the warning delay")
require("distanceToSqr(target) >= 4.0D * 4.0D" in entity
        and "public boolean doHurtTarget(" in entity,
        "close-range pursuit must release the ranged goal to permit melee attacks")
require("ball.setDamage((float)getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE))" in entity and "RANGED_ATTACK_COOLDOWN = 80" in entity,
        "hostile fox must use a visible ranged attack with a bounded cooldown")
require("entityData.set(ATTACKING, attackAnimationTicks > 0)" in entity and "serverLevel.addFreshEntity(ball)" in entity,
        "attack clip must be synchronized when a real projectile is fired")
require("FOLLOW_RANGE, 16" in (ROOT / "src/main/java/com/longju/xiantu/registry/ModEntities.java").read_text(encoding="utf-8"),
        "hostile target range must be configured to 16 blocks")
fox_attributes = entities.split("event.put(YANWEI_FOX.get()", 1)[1].split("));", 1)[0]
require("Attributes.ATTACK_DAMAGE, 4" in fox_attributes,
        "MeleeAttackGoal requires Yanwei Fox to register attack damage")
require("new WaterAvoidingRandomStrollGoal(this, 1.0D)" in entity
        and "getDayTime() % 24_000L" not in entity,
        "unlocked foxes must roam without a dawn/dusk restriction")
require(renderer.index("fox.isAttacking()") < renderer.index('getDeltaMovement()') < renderer.index("fox.isAlerted()"),
        "attack animation must take priority over movement and warning")
require("EntityLeaveLevelEvent" in renderer and "LoggingOut" in renderer and "clearInstances()" in renderer, "missing instance lifecycle cleanup")
require("YanweiFoxRenderer.reloadListener()" in client, "missing resource reload cleanup")
lod_document, lod_binary = read_glb(STATIC_LOD)
require("skins" not in lod_document and "animations" not in lod_document,
        "static LOD must not retain skinning or animation data")
require(binary.startswith(lod_binary), "static LOD changed source mesh or embedded 2K texture bytes")
lod_primitive = lod_document["meshes"][0]["primitives"][0]
require("JOINTS_0" not in lod_primitive["attributes"] and "WEIGHTS_0" not in lod_primitive["attributes"],
        "static LOD still routes through CPU skinning")
lod_mesh_node = next((node for node in lod_document["nodes"] if node.get("mesh") is not None), None)
require(lod_mesh_node is not None and "skin" not in lod_mesh_node,
        "static LOD mesh node still references a skin")
require("yanwei_fox_lod.glb" in renderer, "renderer is not wired to the static LOD asset")
require("ANIMATION_DISTANCE_SQR" in renderer and "entry.staticLod" in renderer,
        "renderer is missing distance-based animation/render selection")
require("30.0D * 30.0D" in renderer and "distanceSqr <= ANIMATION_DISTANCE_SQR" in renderer,
        "skinned animation must render through 30 blocks and switch to static LOD beyond that")

print(f"Yanwei Fox animation PASS: sha256={digest}, {vertex_count} vertices, {index_count // 3} triangles, {manifest['bones']} joints, {len(animations)} clips")
print(f"Yanwei Fox static LOD PASS: sha256={hashlib.sha256(STATIC_LOD.read_bytes()).hexdigest().upper()}, 2K texture payload preserved")
