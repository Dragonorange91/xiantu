"""Export rigid caps and stretchable shaft from the accepted authored native OBJ, preserving every face/UV."""
from pathlib import Path
import json,hashlib
ROOT=Path(__file__).resolve().parents[1]
root=ROOT/'src/main/resources/assets/xiantu/models/item'
source=root/'jingubang.obj';lines=source.read_text().splitlines()
vertices=[];uvs=[];normals=[];parts={'shaft':[],'upper':[],'lower':[]};group='shaft'
for line in lines:
 if line.startswith('v '):vertices.append(line)
 elif line.startswith('vt '):uvs.append(line)
 elif line.startswith('vn '):normals.append(line)
 elif line.startswith('g '):group='upper' if '__upper_' in line else 'lower' if '__lower_' in line else 'shaft'
 elif line.startswith('f '):parts[group].append(line)
assert sum(map(len,parts.values()))==sum(l.startswith('f ') for l in lines)
for name,faces in parts.items():
 (root/f'jingubang_cast_{name}.obj').write_text('\n'.join(['mtllib jingubang.mtl','usemtl gold']+vertices+uvs+normals+faces)+'\n')
 (root/f'jingubang_cast_{name}.json').write_text(json.dumps({'loader':'forge:obj','model':f'xiantu:models/item/jingubang_cast_{name}.obj','automatic_culling':False,'shade_quads':True,'flip_v':False,'textures':{'particle':'xiantu:models/armor/wukong_materials'}},indent=2))
print('Staff cast partitions:',{k:len(v) for k,v in parts.items()},'source SHA256',hashlib.sha256(source.read_bytes()).hexdigest())
