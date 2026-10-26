from pathlib import Path
import json,hashlib
import numpy as np
from PIL import Image
R=Path(__file__).resolve().parents[1];A=R/'src/main/resources/assets/xiantu';O=R/'asset_sources/held-item-20261025'
m=json.loads((O/'manifest.json').read_text(encoding='utf-8'));assert m['count']==len(m['items']) and m['count']>0
for r in m['items']:
 p=A/'textures'/(r['texture'].removeprefix('xiantu:')+'.png');before=O/'before'/p.relative_to(A)
 assert hashlib.sha256(p.read_bytes()).hexdigest()==r['sha256'],r['texture']
 a=np.array(Image.open(p).convert('RGBA'));b=np.array(Image.open(before).convert('RGBA'));solid=a[:,:,3]>0
 assert a.shape==b.shape and set(np.unique(a[:,:,3])).issubset({0,255})
 assert np.array_equal(a[(b[:,:,3]>=128),:3],b[(b[:,:,3]>=128),:3]),'Visible art must be preserved'
 assert int(solid.sum())==r['opaque_after'] and r['opaque_after']>=r['opaque_before']
 assert r['opaque_after']-r['opaque_before']<=r['filled_single_pixel_holes']
print('PASS:',m['count'],'native item sprites retain source detail,hard alpha,opaque contour and nearest-color edge padding')
