"""Audited layers over previous skin/animation contracts, not a baseline reset."""
from pathlib import Path
import json,hashlib,copy,math
import numpy as np
from audit_yanwei_animation_candidate import Glb
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'asset_sources/support-spells-beast-motion-20261025'
MODELS=ROOT/'src/main/resources/assets/xiantu/models/entity'
def manifest():return json.loads((OUT/'integration-manifest.json').read_text('utf8'))
def pre_edit(ident):
 r=manifest().get(ident)
 if r:
  p=Path(r['baseline']);assert hashlib.sha256(p.read_bytes()).hexdigest()==r['baseline_sha256'];return p
 from beast_anatomy_contract import pre_anatomy
 return pre_anatomy(ident)
def legacy_heading(doc):
 d=copy.deepcopy(doc)
 if d['nodes'][-1].get('name')=='Xiantu_Forward_Correction':
  n=d['nodes'].pop();d['scenes'][d.get('scene',0)]['nodes']=n['children']
 return d
def verify():
 count=0
 for ident,r in manifest().items():
  from beast_anatomy_contract import pre_anatomy
  current=pre_anatomy(ident);assert hashlib.sha256(current.read_bytes()).hexdigest()==r['sha256'];a=Glb(pre_edit(ident));b=Glb(current)
  if r['kind']=='heading':
   assert legacy_heading(b.doc)==a.doc and b.binary==a.binary
   n=b.nodes[-1];angle=json.loads((OUT/'wolf-orientation.json').read_text('utf8'))['radians'];assert np.allclose(n['rotation'],[0,math.sin(angle/2),0,math.cos(angle/2)])
   assert n['children']==a.doc['scenes'][a.doc.get('scene',0)]['nodes']
  elif r['kind']=='gait':
   for field in ['nodes','meshes','skins','images','materials','textures','samplers']:assert a.doc.get(field)==b.doc.get(field),(ident,field)
   assert b.binary[:len(a.binary)]==a.binary,'all previous geometry/weights/textures and combat bytes retained'
   assert a.clips.keys()==b.clips.keys()
   for name,clip in a.clips.items():
    if name.rsplit('.',1)[-1] not in ['walk','run']:assert clip==b.clips[name]
   for clip in ['walk','run']:
    name=ident+'.'+clip;c=b.clips[name];duration=max(b.accessor(s['input'])[-1,0] for s in c['samplers'])
    assert abs(duration-(32/24 if clip=='walk' else 22/24))<1e-5
    for channel in c['channels']:
     sampler=c['samplers'][channel['sampler']];v=b.accessor(sampler['output']);times=b.accessor(sampler['input']);assert np.all(np.diff(times[:,0])>0) and np.isfinite(v).all()
     assert np.max(abs(v[0]-v[-1]))<1e-4,'loop discontinuity'
    heights=[]
    for t in np.linspace(0,duration,49):
     m=b.matrices(name,float(t));heights.append([m[b.names.index('foot.'+s),1,3] for s in ['L','R']])
    assert np.min(np.ptp(heights,axis=0))>(.05 if clip=='walk' else .08),'invisible lifted foot'
  else:
   assert a.doc==b.doc,'head correction retains complete rig, topology, textures and animations'
   allowed=set()
   for ma,mb in zip(a.doc['meshes'],b.doc['meshes']):
    for pa,pb in zip(ma['primitives'],mb['primitives']):
     for key,index in pa['attributes'].items():
      old=a.accessor(index);new=b.accessor(pb['attributes'][key])
      if key=='POSITION':
       assert np.array_equal(old[:,1:],new[:,1:]);assert np.max(abs(new-old))<=.05201
      elif key in ['JOINTS_0','WEIGHTS_0']:
       if key=='WEIGHTS_0':assert np.max(abs(new.sum(1)-1))<1e-5
      else:assert np.array_equal(old,new),(ident,key)
      if key in ['POSITION','JOINTS_0','WEIGHTS_0']:
       acc=b.doc['accessors'][index];v=b.doc['bufferViews'][acc['bufferView']];size={5121:1,5123:2,5126:4}[acc['componentType']]*{'VEC3':3,'VEC4':4}[acc['type']];start=v.get('byteOffset',0)+acc.get('byteOffset',0)
       for i in range(acc['count']):allowed.update(range(start+i*v.get('byteStride',size),start+i*v.get('byteStride',size)+size))
   assert len(a.binary)==len(b.binary) and all(x==y or i in allowed for i,(x,y) in enumerate(zip(a.binary,b.binary)))
  count+=1
 print('PASS',count,'Blender MCP corrections: pinned layers, original atlases/combat, matching LOD, gait contact and loop endpoints')
if __name__=='__main__':verify()
