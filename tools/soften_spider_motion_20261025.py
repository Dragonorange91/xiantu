from pathlib import Path
import sys,json
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'work'))
from blender_mcp_bridge import call
from audit_beast_models import Glb
from pack_beast_anatomy_20261025 import OUT,append,pack
from integrate_blender_mob_corrections_20261025 import load,write
def main():
 plan=next(p for p in json.loads((OUT/'fitting-plan.json').read_text()) if p['model']=='lie_yan_zhu');pack(plan)
 p=OUT/'lie_yan_zhu.glb';g=Glb(p);rows=[]
 for name,clip in g.clips.items():
  for ch in clip['channels']:
   if ch['target']['path']!='rotation':continue
   sampler=clip['samplers'][ch['sampler']];i=ch['target']['node']
   rows.append({'clip':name,'sampler':ch['sampler'],'rest':g.nodes[i].get('rotation',[0,0,0,1]),'values':g.accessor(sampler['output']).tolist()})
 (OUT/'spider-rotation-input.json').write_text(json.dumps(rows))
 script=Path(__file__).resolve().parents[1]/'work/soften_spider_rotations_mcp.py'
 r=call('execute_code',{'code':f"exec(compile(open({str(script)!r},encoding='utf8').read(), 'spider quaternion authoring', 'exec'))"});assert r['ok'],r;print(r)
 d,b=load(p)
 for row in json.loads((OUT/'spider-rotation-output.json').read_text()):
  clip=next(c for c in d['animations'] if c['name']==row['clip']);clip['samplers'][row['sampler']]['output']=append(d,b,row['values'],'VEC4')
 write(p,d,b)
 m=json.loads((OUT/'candidate-manifest.json').read_text());m['lie_yan_zhu']['sha256']=Glb(p).sha256.lower();m['lie_yan_zhu']['rotation_amplitude_factor']=.35
 (OUT/'candidate-manifest.json').write_text(json.dumps(m,indent=2))
if __name__=='__main__':main()
