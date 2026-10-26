from pathlib import Path
import sys,json,struct,shutil,numpy as np
from scipy.spatial import cKDTree
sys.path.insert(0,'tools');from audit_beast_models import Glb,MODELS
out=Path('asset_sources/python-head-20261025');src=MODELS/'cang_lin_mang.glb';pinned=out/'cang_lin_mang.before.glb'
if not pinned.exists():shutil.copy2(src,pinned)
g=Glb(pinned);p=g.doc['meshes'][0]['primitives'][0];a=p['attributes'];pos=g.accessor(a['POSITION']).astype(float);w=g.accessor(a['WEIGHTS_0']);j=g.accessor(a['JOINTS_0']);head=(w*(j==1)).sum(1)
# Skull sagittal plane is translated 0.046 units to the right of the neck pivot.
# Preserve its shape; taper the correction only through the lower neck.
blend=np.clip((pos[:,1]-.405)/.10,0,1);blend=blend*blend*(3-2*blend);blend*=head
shift=np.c_[-.046*blend,np.zeros((len(pos),2))];new=pos+shift
raw=bytearray(pinned.read_bytes());jl=struct.unpack_from('<I',raw,12)[0];acc=g.doc['accessors'][a['POSITION']];view=g.doc['bufferViews'][acc['bufferView']];start=28+jl+view.get('byteOffset',0)+acc.get('byteOffset',0);array=np.ndarray((acc['count'],3),dtype='<f4',buffer=raw,offset=start,strides=(view.get('byteStride',12),4));array[:]=new
candidate=out/'cang_lin_mang.glb';candidate.write_bytes(raw)
# Matching static LOD, using nearest high-detail authored surface displacement.
lod=MODELS/'cang_lin_mang_lod.glb';lp=out/'cang_lin_mang_lod.before.glb'
if not lp.exists():shutil.copy2(lod,lp)
lraw=bytearray(lp.read_bytes());llen=struct.unpack_from('<I',lraw,12)[0];ld=json.loads(lraw[20:20+llen]);tree=cKDTree(pos)
for mesh in ld['meshes']:
 for prim in mesh['primitives']:
  acc=ld['accessors'][prim['attributes']['POSITION']];v=ld['bufferViews'][acc['bufferView']];arr=np.ndarray((acc['count'],3),dtype='<f4',buffer=lraw,offset=28+llen+v.get('byteOffset',0)+acc.get('byteOffset',0),strides=(v.get('byteStride',12),4));arr[:]+=shift[tree.query(arr)[1]]
(out/'cang_lin_mang_lod.glb').write_bytes(lraw)
print('candidate preserved all non-position bytes, max displacement',np.linalg.norm(shift,axis=1).max())
# Candidate-only offline render; never edits production for preview.
import preview_beast_audit as render;render.MODELS=out;row={'model':'cang_lin_mang','scale':2.398127,'id':'azure_scale_python'}
from PIL import Image
im=Image.new('RGB',(920,460));im.paste(render.render(row,'_rest',0,False),(0,0));im.paste(render.render(row,'_rest',0,True),(460,0));im.save(out/'candidate.png')
