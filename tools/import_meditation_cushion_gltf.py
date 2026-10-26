"""Preserve supplied static glTF geometry/UV in Forge's native chunk-baked OBJ loader."""
from pathlib import Path
import json, base64, struct, math, hashlib
ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'asset_sources/cushion-uv-fix-20261020/source-export.gltf'
EDITABLE = ROOT / 'asset_sources/cushion-uv-fix-20261020/source.bbmodel'
ASSETS = ROOT / 'src/main/resources/assets/xiantu'
doc = json.loads(SOURCE.read_text(encoding='utf8'))
assert doc['asset']['version'] == '2.0' and not doc.get('skins') and not doc.get('animations')
assert not doc.get('extensionsRequired')
def embedded(uri):
    assert uri.startswith('data:') and ';base64,' in uri
    return base64.b64decode(uri.split(',',1)[1])
buffers = [embedded(b['uri']) for b in doc['buffers']]
for buffer, info in zip(buffers,doc['buffers']): assert len(buffer)==info['byteLength']
def accessor(index):
    a=doc['accessors'][index]; assert not a.get('sparse')
    b=doc['bufferViews'][a['bufferView']]
    components={'SCALAR':1,'VEC2':2,'VEC3':3,'VEC4':4}[a['type']]
    fmt='<'+{5126:'f',5125:'I',5123:'H',5121:'B'}[a['componentType']]*components
    size=struct.calcsize(fmt); stride=b.get('byteStride',size)
    start=b.get('byteOffset',0)+a.get('byteOffset',0)
    return [struct.unpack_from(fmt,buffers[b['buffer']],start+i*stride) for i in range(a['count'])]
def transform(v,node,normal=False):
    assert 'matrix' not in node and not node.get('children')
    scale=node.get('scale',[1,1,1]);v=[v[i]/scale[i] if normal else v[i]*scale[i] for i in range(3)]
    x,y,z,w=node.get('rotation',[0,0,0,1]);a,b,c=v
    v=[(1-2*(y*y+z*z))*a+2*(x*y-z*w)*b+2*(x*z+y*w)*c,
       2*(x*y+z*w)*a+(1-2*(x*x+z*z))*b+2*(y*z-x*w)*c,
       2*(x*z-y*w)*a+2*(y*z+x*w)*b+(1-2*(x*x+y*y))*c]
    if normal:
        length=math.sqrt(sum(t*t for t in v));return [t/length for t in v]
    return [v[i]+node.get('translation',[0,0,0])[i] for i in range(3)]
parts=[];allpoints=[]
for index in doc['scenes'][doc.get('scene',0)]['nodes']:
    node=doc['nodes'][index]
    if 'mesh' not in node: continue
    for primitive in doc['meshes'][node['mesh']]['primitives']:
        assert primitive.get('mode',4)==4 and primitive['material']==0
        a=primitive['attributes'];points=[transform(v,node) for v in accessor(a['POSITION'])]
        normals=[transform(v,node,True) for v in accessor(a['NORMAL'])]
        uvs=accessor(a['TEXCOORD_0']);indices=[i[0] for i in accessor(primitive['indices'])]
        assert len(points)==len(normals)==len(uvs) and len(indices)%3==0
        parts.append((points,normals,uvs,indices));allpoints.extend(points)
low=[min(v[i] for v in allpoints) for i in range(3)];high=[max(v[i] for v in allpoints) for i in range(3)]
scale=.94/max(high[0]-low[0],high[2]-low[2]);center=[(a+b)/2 for a,b in zip(low,high)]
lines=['# User-supplied cushion, baked once into ordinary chunk/item geometry','mtllib meditation_cushion.mtl','usemtl cushion']
offset=1;triangles=0
for points,normals,uvs,indices in parts:
    for p in points:lines.append('v '+' '.join(f'{v:.8f}' for v in [(p[0]-center[0])*scale+.5,(p[1]-low[1])*scale,(p[2]-center[2])*scale+.5]))
    for uv in uvs:lines.append('vt '+' '.join(f'{v:.8f}' for v in uv))
    for n in normals:lines.append('vn '+' '.join(f'{v:.8f}' for v in n))
    for i in range(0,len(indices),3):
        lines.append('f '+' '.join(f'{j+offset}/{j+offset}/{j+offset}' for j in indices[i:i+3]));triangles+=1
    offset+=len(points)
model=ASSETS/'models/block/meditation_cushion'
model.with_suffix('.obj').write_text('\n'.join(lines)+'\n',encoding='utf8')
model.with_suffix('.mtl').write_text('newmtl cushion\nKa 0 0 0\nKd 1 1 1\nd 1\nforge_TintIndex -1\nmap_Kd xiantu:block/meditation_cushion\n',encoding='utf8')
image=embedded(doc['images'][doc['textures'][0]['source']]['uri'])
(ASSETS/'textures/block/meditation_cushion.png').write_bytes(image)
# glTF TEXCOORD_0 and Minecraft atlas coordinates both use image-top V=0.
# We retain glTF UVs verbatim in this OBJ, so Forge must NOT apply OBJ's usual V flip.
model.with_suffix('.json').write_text(json.dumps({'parent':'minecraft:block/block','loader':'forge:obj','model':'xiantu:models/block/meditation_cushion.obj','automatic_culling':False,'shade_quads':True,'flip_v':False,'emissive_ambient':False,'render_type':'minecraft:cutout','textures':{'particle':'xiantu:block/meditation_cushion'}},indent=2),encoding='utf8')
# Keep collision and seated elevation aligned to the normalized mesh.
height=(high[1]-low[1])*scale
p=ROOT/'src/main/java/com/longju/xiantu/formation/FormationVisualShapes.java'
s=p.read_text(encoding='utf8');start=s.index('    public static final double CUSHION_HEIGHT') if '    public static final double CUSHION_HEIGHT' in s else s.index('    public static final VoxelShape CUSHION =');end=s.index('    private static final VoxelShape[][] PILLARS',start)
s=s[:start]+f'''    public static final double CUSHION_HEIGHT = {height:.8f};
    public static final VoxelShape CUSHION = shape(new double[][] {{
        {{2.5, 0, .48, 13.5, {height*16:.8f}, 15.52}},
        {{.48, 0, 2.5, 15.52, {height*16:.8f}, 13.5}},
    }});
'''+s[end:];p.write_text(s,encoding='utf8')
report={'source_sha256':hashlib.sha256(SOURCE.read_bytes()).hexdigest(),'source_parts':len(parts),'triangles':triangles,'vertices':offset-1,'original_bounds':[low,high],'scale':scale,'height_blocks':height,'width_blocks':.94,'texture_sha256':hashlib.sha256(image).hexdigest(),'runtime':'forge:obj static chunk baked; no BER/GFBS/no per-frame skinning'}
report.update(editable_source=str(EDITABLE),editable_sha256=hashlib.sha256(EDITABLE.read_bytes()).hexdigest(),uv_origin='top-left; glTF UV verbatim; Forge flip_v=false')
(ROOT/'asset_sources/cushion-uv-fix-20261020/model-manifest.json').write_text(json.dumps(report,indent=2),encoding='utf8')
print(json.dumps(report,indent=2))
