"""Compare independent Blender exports without bypassing production-asset checks.

Checks the actual GLB payloads, including interpolation and CPU skinning at 48 Hz.
This is an offline asset audit, not a GFBS performance or visual-acceptance test.
"""
import hashlib
import io
import json
import pathlib
import struct

import numpy as np
from PIL import Image

ROOT = pathlib.Path(__file__).resolve().parents[1]
OUT = ROOT / "asset_sources/yanwei-fox-optimize-20261015"


class Glb:
    def __init__(self, path):
        self.path = pathlib.Path(path)
        raw = self.path.read_bytes()
        assert struct.unpack_from('<III', raw) == (0x46546C67, 2, len(raw))
        length, kind = struct.unpack_from('<II', raw, 12)
        assert kind == 0x4E4F534A
        self.doc = json.loads(raw[20:20 + length])
        blen, bkind = struct.unpack_from('<II', raw, 20 + length)
        assert bkind == 0x004E4942 and 28 + length + blen == len(raw)
        self.binary = raw[28 + length:]
        self.sha256 = hashlib.sha256(raw).hexdigest().upper()
        self.nodes = self.doc['nodes']
        self.names = [n.get('name', '') for n in self.nodes]
        assert len(self.names) == len(set(self.names)), 'ambiguous node names'
        self.clips = {a['name']: a for a in self.doc.get('animations',[])}
        self.parents = {c: i for i, n in enumerate(self.nodes) for c in n.get('children', [])}
        self.cache = {}
        assert not self.doc.get('extensionsRequired'), 'unsupported required extensions'
        assert all('uri' not in b for b in self.doc['buffers'])
        for v in self.doc['bufferViews']:
            assert v.get('byteOffset', 0) + v['byteLength'] <= len(self.binary)

    def accessor(self, index):
        if index in self.cache:
            return self.cache[index]
        a = self.doc['accessors'][index]
        assert 'sparse' not in a
        v = self.doc['bufferViews'][a['bufferView']]
        dtype = {5120: 'i1', 5121: 'u1', 5122: '<i2', 5123: '<u2', 5125: '<u4', 5126: '<f4'}[a['componentType']]
        width = {'SCALAR': 1, 'VEC2': 2, 'VEC3': 3, 'VEC4': 4, 'MAT4': 16}[a['type']]
        size = np.dtype(dtype).itemsize
        start = v.get('byteOffset', 0) + a.get('byteOffset', 0)
        value = np.ndarray((a['count'], width), dtype=dtype, buffer=self.binary,
                           offset=start, strides=(v.get('byteStride', width * size), size)).copy()
        assert np.isfinite(value).all()
        self.cache[index] = value
        return value

    def image_info(self):
        result = []
        for image in self.doc['images']:
            v = self.doc['bufferViews'][image['bufferView']]
            data = self.binary[v.get('byteOffset', 0):v.get('byteOffset', 0) + v['byteLength']]
            result.append({'sha256': hashlib.sha256(data).hexdigest(),
                           'size': list(Image.open(io.BytesIO(data)).size)})
        return result

    def sample(self, animation, channel, time):
        s = animation['samplers'][channel['sampler']]
        times = self.accessor(s['input'])[:, 0]
        values = self.accessor(s['output']).astype(float)
        assert np.all(np.diff(times) > 0)
        interpolation = s.get('interpolation', 'LINEAR')
        assert interpolation in {'LINEAR', 'STEP'}
        i = int(np.searchsorted(times, time, side='right'))
        if i == 0:
            return values[0]
        if i >= len(times):
            return values[-1]
        if interpolation == 'STEP':
            return values[i - 1]
        f = (time - times[i - 1]) / (times[i] - times[i - 1])
        a, b = values[i - 1], values[i]
        if channel['target']['path'] != 'rotation':
            return a * (1 - f) + b * f
        a /= np.linalg.norm(a)
        b /= np.linalg.norm(b)
        dot = float(np.dot(a, b))
        if dot < 0:
            b = -b
            dot = -dot
        dot = min(1, dot)
        if dot > .9995:
            q = a * (1 - f) + b * f
        else:
            theta = np.arccos(dot)
            q = (np.sin((1 - f) * theta) * a + np.sin(f * theta) * b) / np.sin(theta)
        return q / np.linalg.norm(q)

    def matrices(self, clip, time):
        poses = [{'translation': n.get('translation', [0, 0, 0]),
                  'rotation': n.get('rotation', [0, 0, 0, 1]),
                  'scale': n.get('scale', [1, 1, 1])} for n in self.nodes]
        for c in self.clips[clip]['channels']:
            poses[c['target']['node']][c['target']['path']] = self.sample(self.clips[clip], c, time)
        world = {}

        def visit(i):
            if i in world:
                return world[i]
            if 'matrix' in self.nodes[i]:
                m = np.array(self.nodes[i]['matrix']).reshape(4, 4, order='F')
            else:
                x, y, z, w = poses[i]['rotation']
                rot = np.array([[1-2*(y*y+z*z), 2*(x*y-z*w), 2*(x*z+y*w)],
                                [2*(x*y+z*w), 1-2*(x*x+z*z), 2*(y*z-x*w)],
                                [2*(x*z-y*w), 2*(y*z+x*w), 1-2*(x*x+y*y)]])
                m = np.eye(4)
                m[:3, :3] = rot @ np.diag(poses[i]['scale'])
                m[:3, 3] = poses[i]['translation']
            world[i] = visit(self.parents[i]) @ m if i in self.parents else m
            return world[i]
        return np.stack([visit(i) for i in range(len(self.nodes))])

    def skinned(self, matrices):
        meshnode = next(n for n in self.nodes if 'mesh' in n)
        prim = self.doc['meshes'][meshnode['mesh']]['primitives'][0]
        attrs = prim['attributes']
        pos = self.accessor(attrs['POSITION'])
        homogeneous = np.concatenate([pos, np.ones((len(pos), 1))], axis=1)
        joints = self.accessor(attrs['JOINTS_0']).astype(int)
        weights = self.accessor(attrs['WEIGHTS_0']).astype(float)
        weights /= weights.sum(axis=1, keepdims=True)
        skin = self.doc['skins'][meshnode['skin']]
        ibm = self.accessor(skin['inverseBindMatrices']).reshape(-1, 4, 4).transpose(0, 2, 1)
        transforms = matrices[skin['joints']] @ ibm
        weighted = np.sum(transforms[joints] * weights[:, :, None, None], axis=1)
        return np.einsum('vij,vj->vi', weighted, homogeneous)[:, :3]


def audit(baseline, candidate):
    a, b = Glb(baseline), Glb(candidate)
    assert set(a.names) == set(b.names), 'node inventory changed'
    assert a.clips.keys() == b.clips.keys(), 'seven required clips changed'
    sa, sb = a.doc['skins'][0], b.doc['skins'][0]
    assert [a.names[i] for i in sa['joints']] == [b.names[i] for i in sb['joints']], 'joint order changed'
    assert len(sa['joints']) == 43
    assert a.image_info() == b.image_info(), 'embedded texture payload changed'
    assert a.doc['materials'] == b.doc['materials'], 'material properties changed'
    pa = a.doc['meshes'][0]['primitives'][0]
    pb = b.doc['meshes'][0]['primitives'][0]
    assert pa.get('mode', 4) == pb.get('mode', 4) == 4
    assert np.array_equal(a.accessor(pa['indices']), b.accessor(pb['indices'])), 'index order changed'
    for attribute in pa['attributes']:
        assert np.allclose(a.accessor(pa['attributes'][attribute]), b.accessor(pb['attributes'][attribute]),
                           rtol=0, atol=2e-6), f'{attribute} geometry/weights changed'
    mapping = [b.names.index(name) for name in a.names]
    report = {'source_sha256': a.sha256, 'candidate_sha256': b.sha256,
              'source_bytes': a.path.stat().st_size, 'candidate_bytes': b.path.stat().st_size,
              'joints': 43, 'vertices': 5040, 'triangles': 4060, 'images': a.image_info(), 'clips': []}
    for clip, anim in a.clips.items():
        da = max(a.accessor(s['input'])[-1, 0] for s in anim['samplers'])
        db = max(b.accessor(s['input'])[-1, 0] for s in b.clips[clip]['samplers'])
        assert abs(da - db) < 1e-6, f'{clip} duration changed'
        matrix_error = vertex_error = 0
        for time in np.linspace(0, da, round(da * 48) + 1):
            ma = a.matrices(clip, time)
            mb = b.matrices(clip, time)
            matrix_error = max(matrix_error, float(np.max(np.abs(ma - mb[mapping]))))
            delta = np.linalg.norm(a.skinned(ma) - b.skinned(mb), axis=1)
            vertex_error = max(vertex_error, float(delta.max()))
        assert vertex_error < .001, f'{clip} skin deformation exceeds 1 mm: {vertex_error}'
        report['clips'].append({'name': clip, 'duration': float(da), 'max_matrix_component_error': matrix_error,
                                'max_world_vertex_error_units': vertex_error,
                                'keys_before': sum(a.doc['accessors'][s['input']]['count'] for s in anim['samplers']),
                                'keys_after': sum(b.doc['accessors'][s['input']]['count'] for s in b.clips[clip]['samplers'])})
    report['status'] = 'offline_geometry_texture_and_48Hz_skin_comparison_passed; game_test_pending'
    return report


if __name__ == '__main__':
    result = audit(ROOT / 'src/main/resources/assets/xiantu/models/entity/yanwei_fox.glb',
                   OUT / 'yanwei_fox_optimized_candidate.glb')
    (OUT / 'candidate-audit.json').write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding='utf-8')
    print(json.dumps(result, ensure_ascii=False, indent=2))
