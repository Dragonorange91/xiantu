"""Preserve reviewed screenshot provenance through the explicit alpha-only cleanup."""
from pathlib import Path
import json,hashlib
import numpy as np
from PIL import Image
def verify_icon(path,expected_sha,root):
 digest=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
 if digest(path)==expected_sha:return
 out=root/'asset_sources/held-item-20261025';assets=root/'src/main/resources/assets/xiantu'
 ref='xiantu:'+path.relative_to(assets/'textures').with_suffix('').as_posix()
 row=next(r for r in json.loads((out/'manifest.json').read_text(encoding='utf-8'))['items'] if r['texture']==ref)
 original=out/'before'/path.relative_to(assets)
 assert digest(original)==row['before_sha256']==expected_sha,'Original reviewed screenshot changed'
 assert digest(path)==row['sha256'],'Unexpected postprocessing result'
 a=np.array(Image.open(path).convert('RGBA'));b=np.array(Image.open(original).convert('RGBA'))
 assert a.shape==b.shape,'Icon resize is forbidden'
 assert np.array_equal(a[:,:,3],np.where(b[:,:,3]>=128,255,0)),'Only exact hard-alpha cleanup is permitted'
 solid=a[:,:,3]>0
 assert np.array_equal(a[solid,:3],b[solid,:3]),'Visible screenshot RGB cannot be repainted'
