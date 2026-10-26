"""Pinned Blender-authored distal-hand layer over the reviewed anatomy asset."""
from pathlib import Path
import json,numpy as np
from audit_beast_models import Glb
from integrate_blender_mob_corrections_20261025 import load,array
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'asset_sources/yuan-hand-20261025'

def verify(path):
 record=json.loads((OUT/'candidate-audit.json').read_text())
 before=OUT/'before.glb';a=Glb(before);b=Glb(path)
 assert a.sha256.lower()==record['source_sha256'] and b.sha256.lower()==record['candidate_sha256']
 assert a.doc==b.doc,'hand repair may only change existing skin weights'
 d,binary=load(before);names=[a.names[n] for n in d['skins'][0]['joints']]
 from scipy.spatial import cKDTree
 from audit_beast_models import skin_parts
 a.clips['_rest']={'channels':[],'samplers':[]};rest=skin_parts(a,a.matrices('_rest',0))
 rows=json.loads((OUT/'hand-weight-authoring.json').read_text())['changes'];tree=cKDTree([r['position'] for r in rows]);dist,ix=tree.query(rest)
 offset=0;count=0
 for node in d['nodes']:
  if 'mesh' not in node:continue
  for p in d['meshes'][node['mesh']]['primitives']:
   attrs=p['attributes'];jj=array(d,binary,attrs['JOINTS_0']);ww=array(d,binary,attrs['WEIGHTS_0'])
   for v in range(len(jj)):
    if dist[offset+v]>2e-6:continue
    row=rows[ix[offset+v]];ws=row['weights'];assert len(ws)<=4 and abs(sum(ws.values())-1)<1e-6
    jj[v]=0;ww[v]=0
    for k,(n,w) in enumerate(sorted(ws.items(),key=lambda x:-x[1])):jj[v,k]=names.index(n);ww[v,k]=w
    count+=1
   offset+=len(jj)
 assert bytes(binary)==b.binary,'unreviewed geometry, animation, map or weights changed'
 assert count==370==record['changed_mesh_vertices']
 for clip,stats in record['after'].items():
  old=record['before'][clip]
  assert stats['max_edge_growth']<=old['max_edge_growth']+1e-6
  assert stats['bad_edges_5cm_2x']<=old['bad_edges_5cm_2x']
 print('PASS Yuan hand: 370 authored vertices; exact original geometry/maps/clips; all 14 clips nonregressing')
 return before
