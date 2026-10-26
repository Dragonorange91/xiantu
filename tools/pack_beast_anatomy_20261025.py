"""Rebind MCP-fitted pivots without changing source mesh, UVs or embedded images."""
from pathlib import Path
import copy,json,struct,hashlib
import numpy as np
from scipy.spatial import cKDTree
from audit_yanwei_animation_candidate import Glb
from integrate_blender_mob_corrections_20261025 import load,write,array
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'asset_sources/beast-rig-full-review-20261025';MODELS=ROOT/'src/main/resources/assets/xiantu/models/entity'
def append(d,b,values,kind):
 values=np.asarray(values,dtype='<f4');b.extend(b'\0'*(-len(b)%4));v=len(d['bufferViews']);d['bufferViews'].append({'buffer':0,'byteOffset':len(b),'byteLength':values.nbytes});b.extend(values.tobytes());i=len(d['accessors']);a={'bufferView':v,'componentType':5126,'count':len(values),'type':kind}
 if kind=='VEC3':a.update(min=values.min(0).tolist(),max=values.max(0).tolist())
 d['accessors'].append(a);return i
def pack(plan):
 ident=plan['model'];source=Path(plan['source']);g=Glb(source);g.clips['_rest']={'channels':[],'samplers':[]};oldM=g.matrices('_rest',0);d,b=load(source);record=json.loads((OUT/(ident+'-mcp.json')).read_text('utf8'));assert record['source_sha256']==g.sha256
 target={n:np.array(p) for n,p in record['after'].items()};desired=oldM.copy()
 for i,name in enumerate(g.names):
  if name in target:desired[i,:3,3]=target[name]
 deltas={}
 for i,name in enumerate(g.names):
  parent=g.parents.get(i);local=np.linalg.inv(desired[parent])@desired[i] if parent is not None else desired[i]
  current=np.array(g.nodes[i].get('translation',[0,0,0]));delta=local[:3,3]-current
  if np.linalg.norm(delta)>1e-6:
   assert 'matrix' not in d['nodes'][i];d['nodes'][i]['translation']=local[:3,3].tolist();deltas[i]=delta
 for skin in d['skins']:
  old=g.accessor(skin['inverseBindMatrices']).reshape(-1,4,4).transpose(0,2,1);updated=[]
  for k,node in enumerate(skin['joints']):updated.append(np.linalg.inv(desired[node])@oldM[node]@old[k])
  skin['inverseBindMatrices']=append(d,b,np.stack(updated).transpose(0,2,1).reshape(-1,16),'MAT4')
 # Translation curves carry absolute local positions. Rebase them by the new rest delta.
 # Rotations/scales, timing and all combat clip names remain byte-for-byte source data.
 for animation in d.get('animations',[]):
  for channel in animation['channels']:
   node=channel['target']['node']
   if channel['target']['path']!='translation' or node not in deltas:continue
   sampler=animation['samplers'][channel['sampler']];sampler['output']=append(d,b,g.accessor(sampler['output'])+deltas[node],'VEC3')
 points=np.array([v['position'] for v in record['vertices']]);tree=cKDTree(points);skinnames=[g.names[n] for n in d['skins'][0]['joints']];modified=0
 from audit_beast_models import skin_parts
 world=skin_parts(g,oldM);offset=0
 for node in d['nodes']:
  if 'mesh' not in node:continue
  for p in d['meshes'][node['mesh']]['primitives']:
   attrs=p['attributes'];n=d['accessors'][attrs['POSITION']]['count'];dist,ix=tree.query(world[offset:offset+n]);assert max(dist)<3e-5,(ident,max(dist))
   joints=array(d,b,attrs['JOINTS_0']);weights=array(d,b,attrs['WEIGHTS_0'])
   oldj=joints.copy();oldw=weights.copy()
   for v,k in enumerate(ix):
    override=next((r['weights'] for r in record.get('local_weight_refinements',[]) if np.linalg.norm(world[offset+v]-r['position'])<2e-6),None)
    if record.get('preserve_source_weights') and override is None:continue
    authored=override if override is not None else record['vertices'][k]['weights']
    pairs=sorted([(skinnames.index(name),w) for name,w in authored.items() if w>1e-7],key=lambda x:(-x[1],x[0]))[:4];total=sum(w for _,w in pairs);assert total>0;joints[v]=0;weights[v]=0
    for s,(j,w) in enumerate(pairs):joints[v,s]=j;weights[v,s]=w/total
   modified+=int(np.any((oldj!=joints)|(abs(oldw-weights)>1e-6),axis=1).sum());offset+=n
 path=OUT/(ident+'.glb');write(path,d,b);new=Glb(path);new.clips['_rest']={'channels':[],'samplers':[]};newM=new.matrices('_rest',0)
 assert np.max(np.abs(skin_parts(new,newM)-world))<3e-6,(ident,'rest geometry changed')
 for name,p in plan['targets'].items():assert np.linalg.norm(newM[new.names.index(name),:3,3]-p)<3e-6
 return {'source':str(source),'source_sha256':g.sha256.lower(),'candidate':str(path),'sha256':new.sha256.lower(),'fitted':list(plan['targets']),'translation_nodes':[g.names[i] for i in deltas],'reweighted_vertices':modified,'rest_max_error':float(np.max(np.abs(skin_parts(new,newM)-world)))}
def main():
 records={}
 for plan in json.loads((OUT/'fitting-plan.json').read_text('utf8')):
  if plan['targets']:
   r=pack(plan);records[plan['model']]=r;print(plan['model'],len(r['fitted']),'pivots',r['reweighted_vertices'],'weighted vertices')
 (OUT/'candidate-manifest.json').write_text(json.dumps(records,indent=2),'utf8')
if __name__=='__main__':main()
