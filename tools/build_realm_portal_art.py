import json, pathlib, numpy as np
from PIL import Image
root=pathlib.Path('.');out=root/'src/main/resources/assets/xiantu';w=root/'work/qa/realm-renewal-20261023';(out/'textures/effect').mkdir(parents=True,exist_ok=True)
N=256
v,u=np.mgrid[-1:1:complex(N),-1:1:complex(N)];r=np.sqrt(u*u+v*v);a=np.arctan2(v,u);frames=[]
for i in range(32):
 t=i/32*2*np.pi
 # Periodic domain-warped mist with hairline caustics; avoids broad opaque pinwheel lobes.
 warp=np.sin(u*4+v*3+t)*.17+np.cos(v*7-u*2-t)*.09
 x=u+warp;y=v+np.sin(u*6-v*3+t)*.13
 cloud=np.zeros_like(u)
 for k,amp in [(3,.34),(7,.19),(15,.1),(29,.055),(53,.025)]:
  cloud+=amp*(np.sin(x*k+y*k*.63+t)+np.cos(y*k-x*k*.71-t))
 cloud=np.clip(.42+cloud*.45,0,1)
 box=(np.abs(u)**6+np.abs(v)**6)**(1/6)
 edge=np.exp(-((box-.92)/.035)**2)
 arc=a*5-r*24+t*2+cloud*5
 threads=np.maximum(0,np.cos(arc))**85*np.exp(-((r-.71)/.41)**2)
 cross=np.maximum(0,np.cos(x*17+y*12+t*2+cloud*8))**70
 haze=cloud*np.clip(1-box,0,1)**.35
 warm=threads*np.maximum(0,np.sin(a*7+t*3))**8
 lum=np.clip(edge*.65+threads*.75+cross*.14+haze*.18,0,1)
 rgba=np.stack([np.clip(0.32+lum*.58+warm*.3,0,1),np.clip(0.68+lum*.3+warm*.1,0,1),np.clip(0.9+lum*.1,0,1),np.clip((haze*.55+edge*.85+threads*.85+cross*.16)*np.clip((1.02-box)*24,0,1),0,1)],axis=-1)
 frames.append((rgba*255).astype('uint8'))
Image.fromarray(np.concatenate(frames,axis=0),'RGBA').save(out/'textures/effect/realm_portal.png')
Image.fromarray(frames[7],'RGBA').resize((768,768)).save(w/'portal-field-preview.png')
for name in ['realm_portal','realm_portal_field']:
 (out/f'blockstates/{name}.json').write_text(json.dumps({'variants':{'':{'model':'minecraft:block/air'}}}))
lang=out/'lang/zh_cn.json';d=json.loads(lang.read_text(encoding='utf8'));d.update({'item.xiantu.realm_test_jade':'升境测试玉','block.xiantu.realm_portal':'秘境传送门','block.xiantu.realm_portal_field':'秘境门面','block.xiantu.realm_spirit_lantern':'灵纹石灯','block.xiantu.realm_incense_reliquary':'古修香龛','block.xiantu.realm_carved_pillar':'残境纹柱'});lang.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
(out/'models/item/realm_test_jade.json').write_text(json.dumps({'parent':'minecraft:item/generated','textures':{'layer0':'minecraft:item/ender_eye'}}))
print('portal flipbook: 32 seamless frames, 256x8192; localization/models written')
