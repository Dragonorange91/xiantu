"""Transfer MCP-authored animation data while preserving accepted mesh/skin/images.

Blender's glTF importer/exporter recalculates normals by about 0.000128. Avoid
that unrelated change: retain all production geometry/weights/materials and map
the exported animation channels by node name. Repack only referenced views.
"""
import copy
import json
import struct

from audit_yanwei_animation_candidate import Glb, ROOT, OUT


def pack(base=None, raw=None, candidate=None, only_clips=None):
    source = Glb(base or ROOT / 'src/main/resources/assets/xiantu/models/entity/yanwei_fox.glb')
    raw = raw or OUT / 'yanwei_fox_blender_export.glb'
    candidate = candidate or OUT / 'yanwei_fox_optimized_candidate.glb'
    if not raw.exists():
        raw.write_bytes(candidate.read_bytes())
    animated = Glb(raw)
    doc = copy.deepcopy(source.doc)
    binary = bytearray()
    views, accessors = [], []
    view_map, accessor_map = {}, {}

    def view(g, index):
        key = (str(g.path), index)
        if key in view_map:
            return view_map[key]
        old = g.doc['bufferViews'][index]
        while len(binary) % 4:
            binary.append(0)
        new = copy.deepcopy(old)
        new['byteOffset'] = len(binary)
        new['buffer'] = 0
        start = old.get('byteOffset', 0)
        binary.extend(g.binary[start:start + old['byteLength']])
        view_map[key] = len(views)
        views.append(new)
        return view_map[key]

    def accessor(g, index):
        key = (str(g.path), index)
        if key in accessor_map:
            return accessor_map[key]
        a = copy.deepcopy(g.doc['accessors'][index])
        assert 'sparse' not in a
        a['bufferView'] = view(g, a['bufferView'])
        accessor_map[key] = len(accessors)
        accessors.append(a)
        return accessor_map[key]

    for mesh in doc['meshes']:
        for primitive in mesh['primitives']:
            primitive['attributes'] = {k: accessor(source, v) for k, v in primitive['attributes'].items()}
            if 'indices' in primitive:
                primitive['indices'] = accessor(source, primitive['indices'])
            for target in primitive.get('targets', []):
                for k, v in target.items():
                    target[k] = accessor(source, v)
    for skin in doc['skins']:
        skin['inverseBindMatrices'] = accessor(source, skin['inverseBindMatrices'])
    for image in doc['images']:
        image['bufferView'] = view(source, image['bufferView'])
    selected = {}
    for animation in source.doc['animations']:
        name = animation['name']
        owner = animated if only_clips is None or name in only_clips else source
        selected[name] = owner
    doc['animations'] = [copy.deepcopy(selected[a['name']].clips[a['name']])
                         for a in source.doc['animations']]
    for animation in doc['animations']:
        owner = selected[animation['name']]
        for channel in animation['channels']:
            target = channel['target']
            target['node'] = source.names.index(owner.names[target['node']])
        for sampler in animation['samplers']:
            sampler['input'] = accessor(owner, sampler['input'])
            sampler['output'] = accessor(owner, sampler['output'])
    doc['accessors'], doc['bufferViews'] = accessors, views
    doc['buffers'] = [{'byteLength': len(binary)}]
    metadata = json.dumps(doc, ensure_ascii=False, separators=(',', ':')).encode('utf-8')
    metadata += b' ' * (-len(metadata) % 4)
    binary.extend(b'\0' * (-len(binary) % 4))
    length = 28 + len(metadata) + len(binary)
    candidate.write_bytes(struct.pack('<III', 0x46546C67, 2, length)
                          + struct.pack('<II', len(metadata), 0x4E4F534A) + metadata
                          + struct.pack('<II', len(binary), 0x004E4942) + binary)
    print(f'Packed animation-only candidate: {candidate} ({length} bytes)')


if __name__ == '__main__':
    import argparse
    from pathlib import Path
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--base', type=Path)
    parser.add_argument('--animated', type=Path)
    parser.add_argument('--output', type=Path)
    parser.add_argument('--clip', action='append')
    args = parser.parse_args()
    pack(args.base, args.animated, args.output, set(args.clip) if args.clip else None)
