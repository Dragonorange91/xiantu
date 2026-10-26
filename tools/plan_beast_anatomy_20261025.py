"""Measure each species before fitting; pivots inside the body are not surface defects."""
from pathlib import Path
import json,sys,hashlib,shutil
import numpy as np
from scipy.spatial import cKDTree,ConvexHull,QhullError
from audit_beast_models import inventory,Glb,skin_parts
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'asset_sources/beast-rig-full-review-20261025';MODELS=ROOT/'src/main/resources/assets/xiantu/models/entity'
OUT.mkdir(exist_ok=True,parents=True)
def main():
 plans=[]
 for row in inventory():
  ident=row['model'];source=OUT/(ident+'.before.glb')
  if not source.exists():shutil.copy2(MODELS/(ident+'.glb'),source)
  g=Glb(source);g.clips['_rest']={'channels':[],'samplers':[]};M=g.matrices('_rest',0);P=skin_parts(g,M);extent=max(np.ptp(P,axis=0));height=np.ptp(P[:,1]);J=[];W=[]
  for n in g.nodes:
   if 'mesh' not in n:continue
   for p in g.doc['meshes'][n['mesh']]['primitives']:J.append(g.accessor(p['attributes']['JOINTS_0']));W.append(g.accessor(p['attributes']['WEIGHTS_0']))
  J=np.concatenate(J);W=np.concatenate(W);targets={};audit=[];tree=cKDTree(P)
  for k,i in enumerate(g.doc['skins'][0]['joints']):
   name=g.names[i];point=M[i,:3,3];weight=(W*(J==k)).sum(1);cloud=P[weight>.22];outside=0;distance=tree.query(point)[0]
   if len(cloud)>=16:
    try:outside=max(0,float(np.max(ConvexHull(cloud,qhull_options='QJ').equations[:,:3]@point+ConvexHull(cloud,qhull_options='QJ').equations[:,3])))
    except QhullError:outside=0
   peripheral=any(s in name for s in ['upper','lower','foot','wing','claw','fin','leg_']) and name not in ['grip_socket.R']
   if peripheral and outside>extent*.025 and distance>extent*.025 and len(cloud)>=16:
    nearest=cloud[np.argsort(np.linalg.norm(cloud-point,axis=1))[:max(16,min(48,len(cloud)//3))]];target=np.mean(nearest,axis=0);shift=target-point;length=np.linalg.norm(shift)
    if length>extent*.12:target=point+shift*(extent*.12/length)
    if np.linalg.norm(target-point)>extent*.012:targets[name]=target.tolist()
   audit.append({'name':name,'world':point.tolist(),'nearest_surface':float(distance),'outside_own_weighted_hull':outside,'weighted_vertices':int(len(cloud))})
  if ident in ['xing_yun_tian_jiang','tong_ling_yuan_wang']:
   fits=json.loads((ROOT/'asset_sources/support-spells-beast-motion-20261025'/(ident+'-rig-fit.json')).read_text('utf8'))
   for name,delta in fits['shifts'].items():targets[name]=(M[g.names.index(name),:3,3]+delta).tolist()
  if ident=='lie_ren_lang':
   targets.update({'hips':[-.015,.015,-.05],'spine':[0,.055,.11],'chest':[.005,.09,.265],'neck':[.025,.17,.35],'head':[.055,.225,.405],
    'front_upper.L':[.095,-.005,.29],'front_lower.L':[.095,-.18,.30],'front_foot.L':[.09,-.37,.31],
    'front_upper.R':[-.12,-.005,.32],'front_lower.R':[-.16,-.18,.35],'front_foot.R':[-.175,-.37,.36],
    'hind_upper.L':[.085,-.015,-.07],'hind_lower.L':[.11,-.20,-.12],'hind_foot.L':[.12,-.38,-.16],
    'hind_upper.R':[-.10,-.015,-.065],'hind_lower.R':[-.14,-.20,-.11],'hind_foot.R':[-.15,-.38,-.15]})
  if ident=='lie_yan_zhu':
   targets={'body':[0,.205,-.11],'head':[0,.17,.285]}
   for side,sign in [('L',-1),('R',1)]:
    for leg,(root_z,knee_z,foot_z) in enumerate([(.19,.34,.46),(.13,.22,.24),(.05,.09,.035),(-.015,-.04,-.12)],1):
     base='leg_'+side+str(leg);targets[base]=[sign*.115,.205,root_z];targets[base+'_mid']=[sign*(.26 if leg<3 else .28),.265,knee_z];targets[base+'_foot']=[sign*(.34 if leg==1 else .40 if leg==2 else .37 if leg==3 else .315),.025,foot_z]
  reviewed=OUT/'reviewed-targets.json'
  if reviewed.exists():
   fixed=json.loads(reviewed.read_text('utf8')).get(ident)
   if fixed:
    assert fixed['source_sha256']==g.sha256.lower(),(ident,'reviewed anatomy source changed')
    targets=fixed['targets']
  plans.append({**row,'source':str(source),'sha256':g.sha256,'extent':float(extent),'height':float(height),'targets':targets,'audit':audit})
  print(ident,len(targets),'anatomical pivots require fitting')
 (OUT/'fitting-plan.json').write_text(json.dumps(plans,indent=2),'utf8')
if __name__=='__main__':main()
