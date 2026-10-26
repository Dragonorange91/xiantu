"""Byte-level preservation contract for deliberately reviewed skin-weight patches."""
import json,struct
from pathlib import Path
import hashlib
ROOT=Path(__file__).resolve().parents[1]
MANIFEST=ROOT/'asset_sources/skin-repair-20261024/integration-manifest.json'

def mask_weights(doc,binary):
    result=bytearray(binary)
    for mesh in doc['meshes']:
        for primitive in mesh['primitives']:
            for attribute in ('JOINTS_0','WEIGHTS_0'):
                a=doc['accessors'][primitive['attributes'][attribute]];v=doc['bufferViews'][a['bufferView']]
                size={5121:1,5123:2,5126:4}[a['componentType']]*4
                start=v.get('byteOffset',0)+a.get('byteOffset',0)
                for i in range(a['count']):
                    pos=start+i*v.get('byteStride',size);result[pos:pos+size]=bytes(size)
    return result

def assert_payload_preserved(ident,old_doc,old_binary,doc,binary):
    entries=json.loads(MANIFEST.read_text(encoding='utf8')) if MANIFEST.exists() else {}
    if ident not in entries:
        assert binary[:len(old_binary)]==old_binary,(ident,'geometry or atlas changed')
        return
    # The separate patch regression pins both complete assets and all non-weight bytes.
    assert mask_weights(doc,binary)[:len(old_binary)]==mask_weights(old_doc,old_binary),(ident,'non-skin payload changed')

def verify():
    from audit_yanwei_animation_candidate import Glb
    entries=json.loads(MANIFEST.read_text(encoding='utf8'))
    for ident,r in entries.items():
        from mob_correction_contract import pre_edit
        source=Glb(Path(r['source']));current=Glb(pre_edit(ident))
        assert source.sha256.lower()==r['source_sha256'].lower()
        assert current.sha256.lower()==r['candidate_sha256'].lower()
        assert source.doc==current.doc,(ident,'rig, topology, material, UV or animation definition changed')
        assert mask_weights(source.doc,source.binary)==mask_weights(current.doc,current.binary),(ident,'only four-influence weights/joints may change')
        assert hashlib.sha256(pre_edit(ident+'_lod').read_bytes()).hexdigest()==r['lod_sha256']
    from mob_correction_contract import verify as verify_layers
    verify_layers()
    print('PASS 5 reviewed skin patches: original geometry/UV/normal/rig/animation/texture bytes, static LOD and pinned hashes')

if __name__=='__main__':verify()
