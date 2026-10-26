"""Pinned anatomical layer, independently verified above historical animation checks."""
from pathlib import Path
import hashlib,json,copy
import numpy as np
from audit_beast_models import Glb,skin_parts
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'asset_sources/beast-rig-full-review-20261025'
MODELS=ROOT/'src/main/resources/assets/xiantu/models/entity'
def records():
 p=OUT/'integration-manifest.json'
 return json.loads(p.read_text('utf8')) if p.exists() else {}
def pre_anatomy(ident):
 r=records().get(ident)
 if not r:return MODELS/(ident+'.glb')
 p=Path(r['source']);assert hashlib.sha256(p.read_bytes()).hexdigest()==r['source_sha256'],(ident,'anatomical source changed')
 return p
def verify():
 results=json.loads((OUT/'motion-audit.json').read_text())
 for ident,r in records().items():
  a=Glb(pre_anatomy(ident));runtime=MODELS/(ident+'.glb')
  if ident=='tong_ling_yuan_wang' and r.get('hand_layer_sha256'):
   from yuan_hand_contract import verify as verify_hand
   assert Glb(runtime).sha256.lower()==r['hand_layer_sha256']
   b=Glb(verify_hand(runtime))
  else:b=Glb(runtime)
  assert b.sha256.lower()==r['sha256'],(ident,'runtime differs from reviewed anatomy')
  assert a.image_info()==b.image_info()
  for field in ['materials','textures','samplers','images','scenes','scene']:
   assert a.doc.get(field)==b.doc.get(field),(ident,field)
  assert a.names==b.names and a.parents==b.parents
  for na,nb in zip(a.nodes,b.nodes):
   aa=copy.deepcopy(na);bb=copy.deepcopy(nb);aa.pop('translation',None);bb.pop('translation',None)
   assert aa==bb,(ident,'only joint rest translations may change')
  for sa,sb in zip(a.doc['skins'],b.doc['skins']):
   aa=dict(sa);bb=dict(sb);aa.pop('inverseBindMatrices');bb.pop('inverseBindMatrices');assert aa==bb
  for ma,mb in zip(a.doc['meshes'],b.doc['meshes']):
   assert ma==mb
   for pa,pb in zip(ma['primitives'],mb['primitives']):
    assert np.array_equal(a.accessor(pa['indices']),b.accessor(pb['indices']))
    for key,i in pa['attributes'].items():
     av=a.accessor(i);bv=b.accessor(pb['attributes'][key])
     if key=='WEIGHTS_0':assert np.all(bv>=0) and np.max(abs(bv.sum(1)-1))<1e-5
     elif key=='JOINTS_0':assert np.min(bv)>=0 and np.max(bv)<len(b.doc['skins'][0]['joints'])
     else:assert np.array_equal(av,bv),(ident,key)
  assert a.clips.keys()==b.clips.keys()
  for name,old in a.clips.items():
   new=b.clips[name];assert old['channels']==new['channels']
   for ch in old['channels']:
    sa=old['samplers'][ch['sampler']];sb=new['samplers'][ch['sampler']]
    assert sa.get('interpolation','LINEAR')==sb.get('interpolation','LINEAR')
    assert np.array_equal(a.accessor(sa['input']),b.accessor(sb['input']))
    av=a.accessor(sa['output']);bv=b.accessor(sb['output'])
    if ch['target']['path']=='translation':
     node=ch['target']['node'];delta=np.array(b.nodes[node].get('translation',[0,0,0]))-a.nodes[node].get('translation',[0,0,0])
     assert np.max(abs((bv-av)-delta))<2e-6,(ident,name,'translation rebase')
    elif ch['target']['path']=='rotation' and r.get('rotation_amplitude_factor'):
     assert ident=='lie_yan_zhu' and r['rotation_amplitude_factor']==.35
     rest=np.array(a.nodes[ch['target']['node']].get('rotation',[0,0,0,1]));rest=rest/np.linalg.norm(rest)
     old_q=av/np.linalg.norm(av,axis=1)[:,None];old_q=np.where((old_q@rest<0)[:,None],-old_q,old_q)
     dot=np.clip(old_q@rest,-1,1);theta=np.arccos(dot);den=np.sin(theta);factor=r['rotation_amplitude_factor']
     aweight=np.divide(np.sin((1-factor)*theta),den,out=np.full_like(den,1-factor),where=abs(den)>1e-5)
     bweight=np.divide(np.sin(factor*theta),den,out=np.full_like(den,factor),where=abs(den)>1e-5)
     expected=rest*aweight[:,None]+old_q*bweight[:,None];expected/=np.linalg.norm(expected,axis=1)[:,None]
     assert np.max(np.minimum(np.linalg.norm(bv-expected,axis=1),np.linalg.norm(bv+expected,axis=1)))<2e-6,(ident,name,'eight-leg bounded rotation correction')
    else:assert np.array_equal(av,bv),(ident,name,'original rotation or scale changed')
  for g in [a,b]:g.clips['_rest']={'channels':[],'samplers':[]}
  assert np.max(abs(skin_parts(a,a.matrices('_rest',0))-skin_parts(b,b.matrices('_rest',0))))<3e-6
  authored=json.loads((OUT/(ident+'-mcp.json')).read_text('utf8'))
  refinements=authored.get('local_weight_refinements',[])
  if refinements:
   assert ident in ['xing_yun_tian_jiang','tong_ling_yuan_wang'] and authored.get('preserve_source_weights')
   assert len(refinements)==(19 if ident=='xing_yun_tian_jiang' else 7)
   rest=skin_parts(b,b.matrices('_rest',0));offset=0
   names=[b.names[n] for n in b.doc['skins'][0]['joints']]
   for node in b.nodes:
    if 'mesh' not in node:continue
    for p in b.doc['meshes'][node['mesh']]['primitives']:
     attrs=p['attributes'];j=b.accessor(attrs['JOINTS_0']);w=b.accessor(attrs['WEIGHTS_0'])
     oldj=a.accessor(attrs['JOINTS_0']);oldw=a.accessor(attrs['WEIGHTS_0'])
     touched=np.zeros(len(j),dtype=bool)
     for row in refinements:
      ids=np.flatnonzero(np.linalg.norm(rest[offset:offset+len(j)]-row['position'],axis=1)<2e-6)
      for v in ids:
       touched[v]=True;expected=np.array([row['weights'].get(n,0) for n in names])
       actual=np.zeros(len(names))
       np.add.at(actual,j[v],w[v])
       assert np.max(abs(expected-actual))<1e-6,(ident,'sparse authored weights lost')
     assert np.array_equal(j[~touched],oldj[~touched]) and np.array_equal(w[~touched],oldw[~touched]),(ident,'unreviewed weights changed')
     offset+=len(j)
  plan=next(p for p in json.loads((OUT/'fitting-plan.json').read_text()) if p['model']==ident)
  world=b.matrices('_rest',0)
  for name,target in plan['targets'].items():assert np.linalg.norm(world[b.names.index(name),:3,3]-target)<3e-6
  assert not results[ident]['regressions'],(ident,'rejected strain regression')
  assert results[ident]['source_sha256']==a.sha256.lower() and results[ident]['candidate_sha256']==b.sha256.lower()
  if ident=='lie_yan_zhu':
   primitive=b.doc['meshes'][0]['primitives'][0];j=b.accessor(primitive['attributes']['JOINTS_0']);w=b.accessor(primitive['attributes']['WEIGHTS_0'])
   joints=b.doc['skins'][0]['joints']
   for side in 'LR':
    for leg in range(1,5):
     for suffix in ['', '_mid','_foot']:
      name=f'leg_{side}{leg}{suffix}';index=joints.index(b.names.index(name))
      assert int(((w*(j==index)).sum(1)>.1).sum())>=20,(name,'unbound spider leg segment')
 assert len(results)==33
 print('PASS anatomical review: 33 species, pinned sources, fitted joints, unchanged rest surfaces/UV/maps, normalized skin and every clip strain screen')
if __name__=='__main__':verify()
