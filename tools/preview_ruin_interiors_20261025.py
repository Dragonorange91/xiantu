"""Room cutaways from shipped model geometry; explicitly offline, not game captures."""
from pathlib import Path
import json
import preview_ruins_rework_20261024 as preview
P=Path(__file__).resolve().parents[1];O=P/'asset_sources/ruin-interiors-20261025'
preview.V=O/'previews';preview.V.mkdir(exist_ok=True)
samples=[('water_curtain_caverns_grand',0),('water_curtain_caverns_grand',7),('fallen_cultivator_necropolis',8),('taixu_tomb_passage',7),('earthvein_mine_palace',7),('cinnabar_seal_tower',1),('skyship_wreck',1),('water_palace',0)]
for id,ri in samples:
 b=json.loads((O/'structures'/(id+'.json')).read_text(encoding='utf8'));r=b['rooms'][ri];x,y,z,X,Y,Z=r['bounds'];cut=min(Y-1,y+5)
 cells=[[a-x+1,h-y+1,c-z+1,s]for a,h,c,s in b['blocks']if x-1<=a<=X+1 and y<=h<cut and z-1<=c<=Z+1]
 sample={'id':f'{id}-room{ri}','name':b['name']+' · '+r['name'],'size':[X-x+3,cut-y+2,Z-z+3],'blocks':cells}
 preview.render(sample,cut-y+1)
