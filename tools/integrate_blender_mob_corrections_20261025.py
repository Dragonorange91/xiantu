"""Layer MCP edits over pinned runtime bytes; never round-trip accepted atlases."""
from pathlib import Path
import sys,copy,json,struct,hashlib,shutil
import numpy as np
from scipy.spatial import cKDTree
from audit_yanwei_animation_candidate import Glb
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'asset_sources/support-spells-beast-motion-20261025'
MODELS=ROOT/'src/main/resources/assets/xiantu/models/entity'
def digest(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def load(p):
 raw=p.read_bytes();n=struct.unpack_from('<I',raw,12)[0];return json.loads(raw[20:20+n]),bytearray(raw[28+n:])
def write(p,d,b):
 d['buffers'][0]['byteLength']=len(b);j=json.dumps(d,separators=(',',':'),ensure_ascii=False).encode();j+=b' '*(-len(j)%4);b+=b'\0'*(-len(b)%4)
 p.write_bytes(struct.pack('<III',0x46546c67,2,28+len(j)+len(b))+struct.pack('<II',len(j),0x4e4f534a)+j+struct.pack('<II',len(b),0x004e4942)+b)
def array(d,b,index):
 a=d['accessors'][index];v=d['bufferViews'][a['bufferView']];dtype={5121:'u1',5123:'<u2',5126:'<f4'}[a['componentType']];n={'VEC3':3,'VEC4':4}[a['type']];size=np.dtype(dtype).itemsize
 return np.ndarray((a['count'],n),dtype=dtype,buffer=b,offset=v.get('byteOffset',0)+a.get('byteOffset',0),strides=(v.get('byteStride',size*n),size))
def baseline(ident):
 p=OUT/(ident+'.before.glb')
 if not p.exists():shutil.copy2(MODELS/(ident+'.glb'),p)
 return p
def main():
 records={}
 def record(id,kind):
  p=MODELS/(id+'.glb');records[id]={'kind':kind,'baseline':str(baseline(id)),'baseline_sha256':digest(baseline(id)),'sha256':digest(p)}
 q=json.loads((OUT/'wolf-orientation.json').read_text('utf8'))['quaternion']
 for id in ['lie_ren_lang','lie_ren_lang_lod']:
  d,b=load(baseline(id));roots=d['scenes'][d.get('scene',0)]['nodes'];index=len(d['nodes']);d['nodes'].append({'name':'Xiantu_Forward_Correction','rotation':q,'children':roots});d['scenes'][d.get('scene',0)]['nodes']=[index];write(MODELS/(id+'.glb'),d,b);record(id,'heading')
 authored=json.loads((OUT/'python-mcp-vertices.json').read_text('utf8'));old=np.array([r['old'] for r in authored]);new=np.array([r['new'] for r in authored]);tree=cKDTree(old)
 g=Glb(baseline('cang_lin_mang'));jointnames=[g.names[n] for n in g.doc['skins'][0]['joints']]
 for id in ['cang_lin_mang','cang_lin_mang_lod']:
  d,b=load(baseline(id))
  for mesh in d['meshes']:
   for p in mesh['primitives']:
    attrs=p['attributes'];pos=array(d,b,attrs['POSITION']);dist,ix=tree.query(pos)
    if id=='cang_lin_mang':assert max(dist)<1e-5
    pos[:]+=new[ix]-old[ix]
    if 'WEIGHTS_0' in attrs:
     joints=array(d,b,attrs['JOINTS_0']);weights=array(d,b,attrs['WEIGHTS_0'])
     for v,k in enumerate(ix):
      pairs=sorted([(jointnames.index(n),w) for n,w in authored[k]['weights'].items() if w>1e-7],key=lambda a:-a[1])[:4];total=sum(w for _,w in pairs);joints[v]=0;weights[v]=0
      for s,(j,w) in enumerate(pairs):joints[v,s]=j;weights[v,s]=w/total
  write(MODELS/(id+'.glb'),d,b);record(id,'head_neck')
 for id in ['xing_yun_tian_jiang','tong_ling_yuan_wang']:
  d,b=load(baseline(id));source=Glb(baseline(id));raw=Glb(OUT/(id+'-motion.glb'))
  for clip in ['walk','run']:
   # New author_action is named for the imported rig; old clips remain as references.
   choices=[a for a in raw.doc['animations'] if a['name'].endswith('.'+clip) and a['name']!=id+'.'+clip];assert len(choices)==1
   a=copy.deepcopy(choices[0]);a['name']=id+'.'+clip;views={};accessors={}
   def accessor(index):
    if index in accessors:return accessors[index]
    acc=copy.deepcopy(raw.doc['accessors'][index]);vi=acc['bufferView']
    if vi not in views:
     v=copy.deepcopy(raw.doc['bufferViews'][vi]);start=v.get('byteOffset',0);b.extend(b'\0'*(-len(b)%4));v['byteOffset']=len(b);b.extend(raw.binary[start:start+v['byteLength']]);views[vi]=len(d['bufferViews']);d['bufferViews'].append(v)
    acc['bufferView']=views[vi];accessors[index]=len(d['accessors']);d['accessors'].append(acc);return accessors[index]
   for c in a['channels']:c['target']['node']=source.names.index(raw.names[c['target']['node']])
   for s in a['samplers']:s['input']=accessor(s['input']);s['output']=accessor(s['output'])
   d['animations'][next(i for i,v in enumerate(d['animations']) if v['name']==a['name'])]=a
  write(MODELS/(id+'.glb'),d,b);record(id,'gait')
 (OUT/'integration-manifest.json').write_text(json.dumps(records,indent=2),'utf8')
 print('Integrated six MCP model layers with pinned pre-edit assets')
if __name__=='__main__':main()
