"""Build an unskinned, same-texture rest-pose LOD from the accepted animated fox GLB."""
import hashlib
import json
import pathlib
import struct


ROOT = pathlib.Path(__file__).resolve().parents[1]
from beast_anatomy_contract import pre_anatomy
SOURCE = pre_anatomy('yanwei_fox')
OUTPUT = ROOT / "src/main/resources/assets/xiantu/models/entity/yanwei_fox_lod.glb"
MANIFEST = ROOT / "asset_sources/creature-batch-20261016/fox-animation-manifest.json"
GLB_MAGIC = 0x46546C67
JSON_CHUNK = 0x4E4F534A
BIN_CHUNK = 0x004E4942


def read_glb(path):
    data = path.read_bytes()
    magic, version, declared_length = struct.unpack_from("<III", data, 0)
    if magic != GLB_MAGIC or version != 2 or declared_length != len(data):
        raise ValueError(f"Invalid GLB header: {path}")
    json_length, json_type = struct.unpack_from("<II", data, 12)
    if json_type != JSON_CHUNK:
        raise ValueError("GLB JSON chunk is missing")
    json_start = 20
    document = json.loads(data[json_start:json_start + json_length].decode("utf-8").rstrip(" \t\r\n\0"))
    binary_header = json_start + json_length
    binary_length, binary_type = struct.unpack_from("<II", data, binary_header)
    if binary_type != BIN_CHUNK or binary_header + 8 + binary_length != len(data):
        raise ValueError("Invalid GLB binary chunk")
    return document, data[binary_header + 8:]


def padded_json(document):
    encoded = json.dumps(document, ensure_ascii=False, separators=(",", ":")).encode("utf-8")
    return encoded + b" " * ((-len(encoded)) % 4)


source_hash = hashlib.sha256(SOURCE.read_bytes()).hexdigest().upper()
manifest = json.loads(MANIFEST.read_text(encoding="utf-8"))
if source_hash != manifest.get("output_sha256"):
    raise SystemExit(f"Animated source does not match its manifest: {source_hash}")

document, binary = read_glb(SOURCE)
expected_clips = set(manifest.get("clips", {}))
actual_clips = {animation.get("name") for animation in document.get("animations", [])}
if len(document.get("skins", [])) != 1 or actual_clips != expected_clips:
    raise SystemExit("Unexpected source rig or animation clip count")
if len(document.get("images", [])) != 3:
    raise SystemExit("Expected the accepted embedded 2K texture set")

for mesh in document["meshes"]:
    for primitive in mesh["primitives"]:
        attributes = primitive["attributes"]
        if "JOINTS_0" not in attributes or "WEIGHTS_0" not in attributes:
            raise SystemExit("Source primitive is missing its expected skin attributes")
        del attributes["JOINTS_0"]
        del attributes["WEIGHTS_0"]

for node in document["nodes"]:
    node.pop("skin", None)
document.pop("skins", None)
document.pop("animations", None)

# Animation-only revisions do not invalidate a same-geometry static LOD.
# Do not duplicate unused new animation buffers into the distant model.
if OUTPUT.exists():
    old, old_binary = read_glb(OUTPUT)
    fields = ("meshes", "nodes", "materials", "images", "textures", "samplers")
    if all(old.get(k) == document.get(k) for k in fields) and binary.startswith(old_binary):
        print("Static Yanwei Fox LOD retained: geometry and 2K texture payload unchanged")
        raise SystemExit(0)

json_bytes = padded_json(document)
binary_bytes = binary + b"\0" * ((-len(binary)) % 4)
body = struct.pack("<II", len(json_bytes), JSON_CHUNK) + json_bytes
body += struct.pack("<II", len(binary_bytes), BIN_CHUNK) + binary_bytes
OUTPUT.parent.mkdir(parents=True, exist_ok=True)
OUTPUT.write_bytes(struct.pack("<III", GLB_MAGIC, 2, 12 + len(body)) + body)

print(f"Static Yanwei Fox LOD built: sha256={hashlib.sha256(OUTPUT.read_bytes()).hexdigest().upper()}, bytes={OUTPUT.stat().st_size}")
