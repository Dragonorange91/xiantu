"""Native geometry, UV, slot and screenshot provenance checks. User removed face cap."""
from pathlib import Path
import json,math,collections
from PIL import Image
from held_item_edge_contract import verify_icon
R=Path(__file__).resolve().parents[1]/'src/main/resources/assets/xiantu'
reports={}
for name in ['cangming_zhenyue','qitian_armor']:
 data=json.loads((R/f'models/armor/{name}.json').read_text(encoding='utf8'));counts=collections.Counter()
 for cube in data['cubes']:
  a=cube['vertices'];assert len(a)>0 and len(a)%32==0
  assert cube['slot'] in ['head','chest','legs','feet']
  assert cube['part'] in ['head','body','right_arm','left_arm','right_leg','left_leg']
  assert 'mannequin' not in cube['name'] and 'display_' not in cube['name']
  for i in range(0,len(a),32):
   q=[a[i+j*8:i+(j+1)*8] for j in range(4)]
   for v in q:
    assert all(math.isfinite(x) for x in v)
    assert 0<=v[3]<=1 and 0<=v[4]<=1
    assert abs(sum(x*x for x in v[5:])-1)<1e-4
   u=[q[1][j]-q[0][j] for j in range(3)];v=[q[2][j]-q[0][j] for j in range(3)]
   cross=[u[1]*v[2]-u[2]*v[1],u[2]*v[0]-u[0]*v[2],u[0]*v[1]-u[1]*v[0]]
   assert sum(cross[j]*q[0][5+j] for j in range(3))>0,(name,cube['name'],'inverted winding')
  actual=0
  for i in range(0,len(a),32):
   actual+=1 if a[i+16:i+24]==a[i+24:i+32] else 2
  assert cube.get('triangles',actual)==actual
  counts[cube['slot']]+=actual
 reports[name]=dict(counts)
for name in ['preview_qingshuang_sword','preview_jingubang']:
 data=json.loads((R/f'models/item/{name}_held.json').read_text(encoding='utf8'))
 assert 'format_version' not in data
 count=0
 for c in data.get('elements',[]):
  assert all(-16<=n<=32 for n in c['from']+c['to'])
  assert all(c['from'][i]<c['to'][i] for i in range(3))
  assert c.get('rotation',{}).get('angle',0) in [-45,-22.5,0,22.5,45]
  for f in c['faces'].values():assert all(0<=n<=16 for n in f['uv'])
  count+=len(c['faces'])*2
 if data.get('loader')=='forge:obj':
  assert data['flip_v'] is False
  obj=R/data['model'].split(':')[1];assert obj.exists()
  vs=[];uvs=[];ns=[]
  for line in obj.read_text(encoding='utf8').splitlines():
   fields=line.split()
   if not fields:continue
   if fields[0] in ('v','vt','vn'):
    nums=[float(x) for x in fields[1:]];assert all(math.isfinite(x) for x in nums)
    if fields[0]=='v':vs.append(nums)
    elif fields[0]=='vt':assert all(0<=x<=1 for x in nums);uvs.append(nums)
    else:assert abs(sum(x*x for x in nums)-1)<1e-4;ns.append(nums)
   elif fields[0]=='f':
    refs=[[int(x)-1 for x in v.split('/')] for v in fields[1:]]
    assert len(refs) in (3,4)
    for a,b,c in refs:assert 0<=a<len(vs) and 0<=b<len(uvs) and 0<=c<len(ns)
    q=[vs[x[0]] for x in refs];u=[q[1][j]-q[0][j] for j in range(3)];v=[q[2][j]-q[0][j] for j in range(3)]
    cross=[u[1]*v[2]-u[2]*v[1],u[2]*v[0]-u[0]*v[2],u[0]*v[1]-u[1]*v[0]]
    assert sum(cross[j]*ns[refs[0][2]][j] for j in range(3))>0
    count+=len(refs)-2
  assert count>0
 for v in data['textures'].values():
  ns,p=v.split(':');path=R/f'textures/{p}.png';assert ns=='xiantu' and path.exists()
 reports[name]=count
for name,size in [('wukong_materials',256),('cangming_materials',256)]:
 im=Image.open(R/f'textures/models/armor/{name}.png');assert im.size==(size,size) and im.mode=='RGBA'
 assert im.getchannel('A').getextrema()==(255,255)
import hashlib
atlas=json.loads((R.parent/'minecraft/atlases/blocks.json').read_text(encoding='utf8'))
sprites={s['sprite'] for s in atlas['sources'] if s['type']=='minecraft:single'}
assert {'xiantu:models/armor/cangming_materials','xiantu:models/armor/wukong_materials'}<=sprites
manifest=json.loads((R.parents[4]/'asset_sources/equipment-preview-manifest.json').read_text(encoding='utf8'))
assert len(manifest['icons'])==10
for item,record in manifest['icons'].items():
 p=R/record['path'];verify_icon(p,record['sha256'],R.parents[4])
 im=Image.open(p);assert im.mode=='RGBA' and im.size==(128,128)
 bbox=im.getchannel('A').getbbox();assert bbox and min(bbox[0],bbox[1],128-bbox[2],128-bbox[3])>=4
 assert im.getchannel('A').getextrema()==(0,255)
print(json.dumps(reports,ensure_ascii=True))
print('PASS: native face winding/UV/finite normals, slot split, source exclusion, static OBJ and ten model screenshot icons')
