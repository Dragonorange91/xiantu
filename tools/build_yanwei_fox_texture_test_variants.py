"""Create texture-resolution-only QA variants of the animated GFBS fox."""
import argparse
import hashlib
import io
import json
import pathlib
import struct

from PIL import Image


ROOT = pathlib.Path(__file__).resolve().parents[1]
DEFAULT_SOURCE = ROOT / "src/main/resources/assets/xiantu/models/entity/yanwei_fox.glb"
DEFAULT_OUT = ROOT / "work/qa/yanwei-fox-perf-20261014/texture-variants"


def read_glb(path):
    data = path.read_bytes()
    if struct.unpack_from("<III", data, 0) != (0x46546C67, 2, len(data)):
        raise ValueError(f"Invalid GLB header: {path}")
    offset = 12
    chunks = {}
    while offset < len(data):
        length, kind = struct.unpack_from("<II", data, offset)
        chunks[kind] = data[offset + 8:offset + 8 + length]
        offset += 8 + length
    return json.loads(chunks[0x4E4F534A]), chunks[0x004E4942]


def view_bytes(doc, binary, index):
    view = doc["bufferViews"][index]
    start = view.get("byteOffset", 0)
    return binary[start:start + view["byteLength"]]


def write_variant(source_doc, source_binary, source_bytes, output, target_sizes):
    doc = json.loads(json.dumps(source_doc))
    replacements = {}
    report_images = []
    image_roles = {}
    for material in source_doc.get("materials", []):
        pbr = material.get("pbrMetallicRoughness", {})
        for role, texture_info in (
            ("base_color", pbr.get("baseColorTexture")),
            ("metallic_roughness", pbr.get("metallicRoughnessTexture")),
            ("normal", material.get("normalTexture")),
        ):
            if texture_info is not None:
                texture = source_doc["textures"][texture_info["index"]]
                image_roles[texture["source"]] = role
    for index, image in enumerate(source_doc["images"]):
        view_index = image["bufferView"]
        original_bytes = view_bytes(source_doc, source_binary, view_index)
        with Image.open(io.BytesIO(original_bytes)) as source_image:
            source_image = source_image.convert("RGB")
            source_size = list(source_image.size)
            target = target_sizes[index]
            if source_image.size != (target, target):
                resized = source_image.resize((target, target), Image.Resampling.LANCZOS)
                encoded = io.BytesIO()
                resized.save(encoded, format="JPEG", quality=95, subsampling=0, optimize=True)
                replacements[view_index] = encoded.getvalue()
                doc["images"][index]["mimeType"] = "image/jpeg"
                output_size = list(resized.size)
            else:
                output_size = source_size
        report_images.append({
            "image_index": index,
            "role": image_roles[index],
            "before": source_size,
            "after": output_size,
            "source_bytes": len(original_bytes),
            "output_bytes": len(replacements.get(view_index, original_bytes)),
        })

    new_binary = bytearray()
    for index, view in enumerate(doc["bufferViews"]):
        if view.get("buffer", 0) != 0:
            raise ValueError("Only a single embedded GLB buffer is supported")
        new_binary.extend(b"\0" * (-len(new_binary) % 4))
        view["byteOffset"] = len(new_binary)
        payload = replacements.get(index, view_bytes(source_doc, source_binary, index))
        view["byteLength"] = len(payload)
        new_binary.extend(payload)
    doc["buffers"][0]["byteLength"] = len(new_binary)
    json_chunk = json.dumps(doc, separators=(",", ":"), ensure_ascii=False).encode("utf-8")
    json_chunk += b" " * (-len(json_chunk) % 4)
    new_binary.extend(b"\0" * (-len(new_binary) % 4))
    total = 12 + 8 + len(json_chunk) + 8 + len(new_binary)
    result = (struct.pack("<III", 0x46546C67, 2, total)
              + struct.pack("<II", len(json_chunk), 0x4E4F534A) + json_chunk
              + struct.pack("<II", len(new_binary), 0x004E4942) + new_binary)
    output.write_bytes(result)

    check, check_binary = read_glb(output)
    for key in source_doc:
        if key not in {"buffers", "bufferViews", "images"} and check.get(key) != source_doc.get(key):
            raise AssertionError(f"Non-texture GLB section changed: {key}")
    if check.get("animations") != source_doc.get("animations"):
        raise AssertionError("Animation clips changed")
    if check.get("skins") != source_doc.get("skins"):
        raise AssertionError("Skeleton changed")
    for index, view in enumerate(check["bufferViews"]):
        if index not in replacements and view_bytes(check, check_binary, index) != view_bytes(source_doc, source_binary, index):
            raise AssertionError(f"Non-texture buffer view changed: {index}")
    for index, image in enumerate(check["images"]):
        with Image.open(io.BytesIO(view_bytes(check, check_binary, image["bufferView"]))) as verify_image:
            expected_size = (target_sizes[index], target_sizes[index])
            if verify_image.size != expected_size:
                raise AssertionError(
                    f"Image {index} has size {verify_image.size}; expected {expected_size} in {output.name}"
                )
    return {
        "source_sha256": hashlib.sha256(source_bytes).hexdigest().upper(),
        "output_sha256": hashlib.sha256(result).hexdigest().upper(),
        "source_bytes": len(source_bytes),
        "output_bytes": len(result),
        "triangles": sum(check["accessors"][primitive["indices"]]["count"] // 3
                         for mesh in check["meshes"] for primitive in mesh["primitives"]),
        "joints": len(check["skins"][0]["joints"]),
        "animation_names": sorted(animation["name"] for animation in check["animations"]),
        "images": report_images,
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", type=pathlib.Path, default=DEFAULT_SOURCE)
    parser.add_argument("--out-dir", type=pathlib.Path, default=DEFAULT_OUT)
    args = parser.parse_args()
    args.out_dir.mkdir(parents=True, exist_ok=True)
    source_bytes = args.source.read_bytes()
    source_doc, source_binary = read_glb(args.source)
    source_sizes = []
    for image in source_doc["images"]:
        with Image.open(io.BytesIO(view_bytes(source_doc, source_binary, image["bufferView"]))) as im:
            source_sizes.append(im.size)
    if len(source_sizes) != 3 or any(size != (2048, 2048) for size in source_sizes):
        raise ValueError(f"Expected three 2048x2048 source maps, found {source_sizes}")

    roles = {}
    for material in source_doc.get("materials", []):
        pbr = material.get("pbrMetallicRoughness", {})
        for role, texture_info in (
            ("base_color", pbr.get("baseColorTexture")),
            ("metallic_roughness", pbr.get("metallicRoughnessTexture")),
            ("normal", material.get("normalTexture")),
        ):
            if texture_info is not None:
                roles[source_doc["textures"][texture_info["index"]]["source"]] = role
    if set(roles.values()) != {"base_color", "metallic_roughness", "normal"}:
        raise ValueError(f"Unexpected glTF texture roles: {roles}")

    reports = {}
    for label, sizes in (
        ("2k", [2048, 2048, 2048]),
        ("512", [512 if roles[index] == "base_color" else 256 for index in range(3)]),
    ):
        output = args.out_dir / f"yanwei_fox_{label}.glb"
        reports[label] = write_variant(source_doc, source_binary, source_bytes, output, sizes)
        reports[label]["path"] = str(output)
    if reports["2k"]["output_sha256"] != reports["512"]["output_sha256"]:
        for key in ("triangles", "joints", "animation_names"):
            if reports["2k"][key] != reports["512"][key]:
                raise AssertionError(f"Variants differ outside texture resolution: {key}")
    manifest = {
        "purpose": "10 animated GFBS entity in-game texture-resolution comparison",
        "source": str(args.source),
        "variants": reports,
        "controlled_constants": ["mesh", "indices", "UVs", "skin", "joint weights", "animations", "material parameters"],
        "resolution_change": "base color 2048->512; normal and metallic-roughness 2048->256",
        "status": "GLB variant generation and structural comparison passed; game measurement pending",
    }
    manifest_path = args.out_dir / "manifest.json"
    manifest_path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(manifest, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
