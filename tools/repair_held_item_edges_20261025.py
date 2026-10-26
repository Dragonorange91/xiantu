"""Clean existing sprite alpha for native generated-item extrusion; no regenerated art."""
from pathlib import Path
import json,hashlib,shutil
import numpy as np
from PIL import Image,ImageDraw
from scipy import ndimage as nd
R=Path(__file__).resolve().parents[1];A=R/'src/main/resources/assets/xiantu';O=R/'asset_sources/held-item-20261025';O.mkdir(parents=True,exist_ok=True)
models={p.relative_to(A/'models').with_suffix('').as_posix():json.loads(p.read_text(encoding='utf-8')) for p in (A/'models/item').rglob('*.json')}
def flat(key,seen=()):
 if key in seen:return None
 if key in ('minecraft:item/generated','minecraft:item/handheld'):return {}
 if not key.startswith('xiantu:'):return None
 m=models.get(key[7:])
 if not m or 'elements' in m or 'loader' in m:return None
 parent=flat(m.get('parent',''),seen+(key,))
 if parent is None:return None
 return parent|m.get('textures',{})
textures={}
for key in models:
 t=flat('xiantu:'+key)
 if t is None:continue
 for layer,ref in t.items():
  if layer.startswith('layer') and ref.startswith('xiantu:item/'):
   textures.setdefault(ref,[]).append(key)
old=json.loads((O/'manifest.json').read_text(encoding='utf-8')) if (O/'manifest.json').exists() else {'items':[]}
records={x['texture']:x for x in old['items']}
for ref,owners in sorted(textures.items()):
 p=A/'textures'/(ref[7:]+'.png')
 if not p.exists():continue
 im=Image.open(p).convert('RGBA');a=np.array(im);soft=(a[:,:,3]>0)&(a[:,:,3]<255)
 if not soft.any():continue
 rel=p.relative_to(A);backup=O/'before'/rel;backup.parent.mkdir(parents=True,exist_ok=True)
 if not backup.exists():shutil.copy2(p,backup)
 original=a.copy();solid=a[:,:,3]>=128
 assert solid.any(),ref+' would be erased'
 # Only single-pixel, fully enclosed pinholes in solid objects, never ring/leaf cutouts.
 filled=0
 if any(x in ref for x in ('ingot_','refining_scrap','equipment_blueprint','failed_pill')):
  holes=nd.binary_fill_holes(solid)&~solid;labels,n=nd.label(holes)
  for k in range(1,n+1):
   region=labels==k
   if region.sum()<=2:solid|=region;filled+=int(region.sum())
 # Keep RGB material/detail untouched; only new opaque hole pixels inherit nearest solid color.
 newly=solid&(original[:,:,3]<128)
 if newly.any():
  _,idx=nd.distance_transform_edt(~(original[:,:,3]>=128),return_indices=True);a[newly,:3]=original[idx[0][newly],idx[1][newly],:3]
 a[:,:,3]=np.where(solid,255,0)
 # Padding uses the nearest opaque texel in transparent RGB, avoiding dark interpolation bleed.
 _,idx=nd.distance_transform_edt(~solid,return_indices=True);a[~solid,:3]=a[idx[0][~solid],idx[1][~solid],:3]
 Image.fromarray(a).save(p)
 assert not ((a[:,:,3]>0)&(a[:,:,3]<255)).any()
 assert np.array_equal(a[solid&~newly,:3],original[solid&~newly,:3]),'Artwork RGB changed'
 records[ref]=dict(texture=ref,models=owners,soft_alpha_before=int(soft.sum()),opaque_before=int((original[:,:,3]>=128).sum()),opaque_after=int(solid.sum()),filled_single_pixel_holes=filled,before_sha256=hashlib.sha256(backup.read_bytes()).hexdigest(),sha256=hashlib.sha256(p.read_bytes()).hexdigest())
manifest=dict(items=list(records.values()),count=len(records),method='alpha128 hard contour,nearest opaque RGB padding; only1-2px enclosed holes on solid materials;existing visible RGB and sprite bounds unchanged',runtime_tested=False)
(O/'manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2),encoding='utf-8')
print('CLEANED',len(records),'unique native item sprite textures; no GLB,block,plant or emissive texture mutation')
