"""Actual candidate skinning, texture and pose screen; offline, not GFBS gameplay."""
from pathlib import Path
import json,sys
import numpy as np
from PIL import Image
import preview_beast_audit as preview
from audit_beast_models import inventory,Glb
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'asset_sources/beast-rig-full-review-20261025'
def main():
 preview.MODELS=OUT
 manifest=json.loads((OUT/'candidate-manifest.json').read_text())
 dest=OUT/'previews';dest.mkdir(exist_ok=True)
 for row in inventory():
  id=row['model']
  if len(sys.argv)>1 and id not in sys.argv[1:]:continue
  if id not in manifest:
   # source was already inspected by MCP; render the same unchanged bytes.
   import shutil
   if not (OUT/(id+'.glb')).exists():shutil.copy2(OUT/(id+'.before.glb'),OUT/(id+'.glb'))
  g=Glb(OUT/(id+'.glb'))
  moves=[n for n in g.clips if n.rsplit('.',1)[-1] in ['run','walk','fly','swim']]
  move=next((n for n in moves if n.endswith('.run')),moves[0])
  attack=next((n for n in g.clips if n.endswith('.attack')),next(iter(g.clips)))
  duration=lambda n:max(float(g.accessor(s['input'])[-1,0]) for s in g.clips[n]['samplers'])
  states=[('_rest',0),(move,duration(move)*.25),(move,duration(move)*.67),(attack,duration(attack)*.35)]
  canvas=Image.new('RGB',(1840,920))
  for col,(clip,t) in enumerate(states):
   for side in [False,True]:canvas.paste(preview.render(row,clip,t,side),(col*460,int(side)*460))
  canvas.save(dest/(id+'-poses.png'))
  print('POSES',id,flush=True)
if __name__=='__main__':main()
