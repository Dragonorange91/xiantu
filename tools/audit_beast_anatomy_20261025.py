"""All species, every runtime clip: rest preservation and sampled surface strain."""
from pathlib import Path
import json,numpy as np
from audit_beast_models import inventory,Glb,skin_parts
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'asset_sources/beast-rig-full-review-20261025'
def measure(path,scale):
 g=Glb(path);g.clips['_rest']={'channels':[],'samplers':[]};M=g.matrices('_rest',0);rest=skin_parts(g,M);edges=[];offset=0
 for n in g.nodes:
  if 'mesh' not in n:continue
  for p in g.doc['meshes'][n['mesh']]['primitives']:
   ix=g.accessor(p['indices']).reshape(-1,3).astype(int)+offset;offset+=g.doc['accessors'][p['attributes']['POSITION']]['count'];edges.extend([ix[:,[0,1]],ix[:,[1,2]],ix[:,[2,0]]])
 edges=np.unique(np.sort(np.concatenate(edges),axis=1),axis=0);length=np.linalg.norm(rest[edges[:,0]]-rest[edges[:,1]],axis=1);valid=(length>1e-5);edges=edges[valid];length=length[valid];records={}
 for name,clip in g.clips.items():
  if name=='_rest':continue
  duration=max(float(g.accessor(s['input'])[-1,0]) for s in clip['samplers']);maxgrowth=0;maxbad=0;p99=0;lowest=1e10
  for t in np.linspace(0,duration,max(13,min(61,round(duration*18)+1))):
   pts=skin_parts(g,g.matrices(name,float(t)));d=np.linalg.norm(pts[edges[:,0]]-pts[edges[:,1]],axis=1);growth=(d-length)*scale
   maxgrowth=max(maxgrowth,float(growth.max()));maxbad=max(maxbad,int(np.sum((growth>.05)&(d>2*length))));p99=max(p99,float(np.percentile(growth,99)));lowest=min(lowest,float(pts[:,1].min())*scale)
  records[name]={'max_edge_growth':maxgrowth,'bad_edges_5cm_2x':maxbad,'p99_growth':p99,'lowest':lowest,'duration':duration}
 return records
def main():
 plans={r['model']:r for r in json.loads((OUT/'fitting-plan.json').read_text('utf8'))};manifest=json.loads((OUT/'candidate-manifest.json').read_text('utf8'));results={};dest=OUT/'motion-audit';dest.mkdir(exist_ok=True)
 for row in inventory():
  id=row['model'];cache=dest/(id+'.json')
  source_sha=Glb(Path(plans[id]['source'])).sha256.lower()
  candidate_sha=Glb(Path(manifest[id]['candidate'])).sha256.lower() if id in manifest else source_sha
  if cache.exists():
   record=json.loads(cache.read_text('utf8'))
   if record.get('source_sha256')==source_sha and record.get('candidate_sha256')==candidate_sha:results[id]=record;continue
  source=Path(plans[id]['source']);before=measure(source,row['scale']);after=measure(Path(manifest[id]['candidate']),row['scale']) if id in manifest else before
  regress=[]
  for name,stats in before.items():
   a=after[name]
   if a['max_edge_growth']>max(stats['max_edge_growth']*1.15,stats['max_edge_growth']+.04) or a['bad_edges_5cm_2x']>max(stats['bad_edges_5cm_2x']*1.3,stats['bad_edges_5cm_2x']+12):regress.append(name)
  record={'before':before,'after':after,'regressions':regress,'clips':len(before),'changed':id in manifest,'source_sha256':source_sha,'candidate_sha256':candidate_sha};cache.write_text(json.dumps(record,indent=2),'utf8');results[id]=record
  print(id,'clips',len(before),'worst',round(max(s['max_edge_growth'] for s in before.values()),3),'->',round(max(s['max_edge_growth'] for s in after.values()),3),'regress',regress,flush=True)
 (OUT/'motion-audit.json').write_text(json.dumps(results,indent=2),'utf8')
if __name__=='__main__':main()
