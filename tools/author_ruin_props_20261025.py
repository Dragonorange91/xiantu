"""Author coffin and enlarged repair bench through live Blockbench native MCP.
Opaque tiled legacy materials, proportional UVs, segmented baked multicell models.
"""
from pathlib import Path
import json, math, copy, base64
from blockbench_native_bridge import NativeBridge
P=Path(__file__).resolve().parents[1];A=P/'src/main/resources/assets/xiantu';D=P/'src/main/resources/data'
O=P/'asset_sources/ruin-interiors-20261025';O.mkdir(parents=True,exist_ok=True)
def dump(p,v):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(v,ensure_ascii=False,indent=2),encoding='utf8')
B=NativeBridge()
print(B.call('get_status'),flush=True)
print(B.call('list_formats'),flush=True)
tex={'wood':'xian_furniture_wood','metal':'block_hanxing_iron','copper':'block_chisui_copper','jade':'cloud_jade_smooth'}
def box(g,n,a,b,t='wood'):
    # Split large beams into cell-sized source pieces; never stretch a tile over 3 blocks.
    cuts=[[a[i]]+[float(x) for x in range(16,97,16) if a[i]<x<b[i]]+[b[i]] for i in range(3)]
    for ix in range(len(cuts[0])-1):
      for iy in range(len(cuts[1])-1):
       for iz in range(len(cuts[2])-1):
        lo=[cuts[i][j]for i,j in enumerate([ix,iy,iz])];hi=[cuts[i][j+1]for i,j in enumerate([ix,iy,iz])];x,y,z=[hi[i]-lo[i]for i in range(3)]
        faces={f:{'uv':[0,0,w,h],'texture':tex[t]+'.png'}for f,w,h in [('north',x,y),('south',x,y),('east',z,y),('west',z,y),('up',x,z),('down',x,z)]}
        g.append({'name':f'{n}_{ix}_{iy}_{iz}','from':lo,'to':hi,'faces':faces,'autouv':0,'box_uv':False})
def coffin():
 g=[]
 # Pedestal, tapered body and vaulted lid: large 2x3x2 burial furnishing.
 for x in [3,24]:
  for z in [4,39]:box(g,f'beast_foot_{x}_{z}',[x,0,z],[x+5,5,z+5],'copper')
 box(g,'lower_plinth',[1,4,1],[31,7,47])
 box(g,'plinth_bead',[.8,6,1],[31.2,8,47],'copper')
 for y,width in [(8,27),(12,28),(16,29),(20,30)]:
  box(g,'tapered_body_'+str(y),[(32-width)/2,y,2],[(32+width)/2,y+4,46])
 box(g,'lid_seam',[.6,24,.6],[31.4,25.2,47.4],'metal')
 for i in range(8):
  width=30-i*2.65
  box(g,'vaulted_lid_'+str(i),[(32-width)/2,25+i*.8,1+i*.45],[(32+width)/2,25.8+i*.8,47-i*.45])
 box(g,'lid_spine',[13.8,31.4,5],[18.2,32,43],'copper')
 # Recessed panels, continuous corner stiles, bronze carrying rings.
 for x in [1,29.6]:
  for z in [2.6,43.5]:box(g,f'corner_{x}_{z}',[x,8,z],[x+1.4,24,z+1.8],'copper')
 for side in [1.0,30.35]:
  for z in [9,22,35]:
   box(g,f'side_panel_{side}_{z}',[side,12,z],[side+.65,21,z+8],'metal')
   for dz in [0,7.3]:box(g,f'panel_upright_{side}_{z}_{dz}',[side-.1,12,z+dz],[side+.85,21,z+dz+.7],'copper')
   for yy in [12,20.3]:box(g,f'panel_rail_{side}_{z}_{yy}',[side-.1,yy,z],[side+.85,yy+.7,z+8],'copper')
   # A pierced square carrying ring, anchored on the metal escutcheon.
   for yy in [14.2,17.8]:box(g,f'ring_rail_{side}_{z}_{yy}',[side-.6,yy,z+2],[side+1.1,yy+.7,z+5.6],'copper')
   for dz in [2,4.9]:box(g,f'ring_side_{side}_{z}_{dz}',[side-.6,14.9,z+dz],[side+1.1,17.8,z+dz+.7],'copper')
 for z in [1.3,46.1]:
  box(g,'end_panel_'+str(z),[7,10,z],[25,23,z+.7],'metal')
  # Symmetric stepped seal, a coherent emblem instead of random scrap strips.
  for yy,w in [(12,4),(13.5,10),(15,14),(16.5,10),(18,4)]:box(g,f'seal_{z}_{yy}',[16-w/2,yy,z-.2],[16+w/2,yy+1,z+.95],'copper')
  box(g,'seal_center_'+str(z),[14.5,14,z-.35],[17.5,18,z+1.05],'jade')
 return g
def repair():
 old=json.loads((P/'asset_sources/equipment-repair-20261024/equipment_repair_table.json').read_text(encoding='utf8'))
 g=[]
 for i,e in enumerate(old['elements']):
  n=e.get('name',str(i));lo=e['from'];hi=e['to']
  t=next(iter(e['faces'].values()))['texture'];t=old['textures'].get(t.lstrip('#'),t)
  key=next((k for k,v in tex.items() if t.endswith('/'+v)),'wood')
  # Widen the structure; instruments retain their authored proportions on left half.
  is_structure=any(v in n for v in ['worktop','shelf','waist','front_inlay'])
  shift=16 if (lo[0]+hi[0])/2>11.9 and any(v in n for v in ['foot','leg','collar','rack_post']) else 0
  xx=lambda x:x*2 if is_structure or n=='rack_rail' else x+shift
  yy=lambda y:y*1.22
  box(g,n,[xx(lo[0]),yy(lo[1]),lo[2]],[xx(hi[0]),yy(hi[1]),hi[2]],key)
 # Worktop and support on the enlarged right-hand station; tool silhouettes stay low.
 box(g,'right_writing_surface',[17,14.03,1],[29,14.55,11])
 for x in [19,25]:
  box(g,'right_ingot_'+str(x),[x,14.6,5],[x+3.5,15.5,9],'metal')
 box(g,'right_gauge',[19,14.6,2],[28,15,2.6],'copper')
 box(g,'right_rear_crossbar',[16,15,14],[30,15.6,15])
 return g
def author(id,g,dims):
 if id=='ancient_burial_coffin':
  old=g;g=[]
  for e in old:
   t=next(k for k,v in tex.items()if e['faces']['up']['texture']==v+'.png')
   box(g,e['name'],[e['from'][0],e['from'][1],e['from'][2]*5/3],[e['to'][0],e['to'][1],e['to'][2]*5/3],t)
 # Full editable assembly uses unrestricted format; only clipped cells use Java codec.
 B.call('new_project',format='free',name=id,texture_width=16,texture_height=16)
 B.call('add_groups',groups=[{'name':'assembly','origin':[0,0,0]}])
 for e in g:e['parent']='assembly'
 for k,v in tex.items():B.call('import_texture',path=str(A/'textures/block'/f'{v}.png'),name=v+'.png')
 B.call('add_cubes',cubes=g)
 for c in g:B.call('set_cube_uv',cube=c['name'],faces=c['faces'])
 dump(O/(id+'-check.json'),B.call('check_model'))
 B.call('save_project',path=str(O/(id+'.bbmodel')))
 B.call('export_model',codec='java_block',path=str(O/(id+'.json')))
 B.call('set_camera_angle',position=[104,72,-104],target=[0,11,0])
 shot=B.call('screenshot',view='isometric_right_front',width=1000,height=900,annotate=False)
 for v in shot.values():
  if isinstance(v,str)and v.startswith('data:image'):(O/(id+'.png')).write_bytes(base64.b64decode(v.split(',',1)[1]))
 m=json.loads((O/(id+'.json')).read_text(encoding='utf8'));m.pop('format_version',None)
 m['textures']={k:'xiantu:block/'+Path(v.replace('\\','/')).stem for k,v in m['textures'].items()};m['textures']['particle']='xiantu:block/xian_furniture_wood'
 # GUI item is uniformly normalized; placed parts retain the actual dimensions.
 scale=.77/max(dims);m['display']={'gui':{'rotation':[25,225,0],'translation':[-3,-3,-2],'scale':[scale]*3},'ground':{'scale':[.2]*3},'fixed':{'scale':[scale]*3}}
 # Every source cube was segmented at exact cell boundaries; no invisible internal faces.
 variants={}
 for x in range(dims[0]):
  for y in range(dims[1]):
   for z in range(dims[2]):
    es=[]
    for e in m['elements']:
     owner=[min(dims[i]-1,int((e['from'][i]+e['to'][i])/32))for i in range(3)]
     if owner!=[x,y,z]:continue
     e=copy.deepcopy(e)
     for edge in ['from','to']:e[edge]=[round(e[edge][i]-owner[i]*16,5)for i in range(3)]
     es.append(e)
    part=f'{x}_{y}_{z}';dump(A/f'models/block/ruin_props/{id}/{part}.json',dict(m,elements=es))
    for i,f in enumerate(['north','east','south','west']):variants[f'facing={f},part={part}']={'model':f'xiantu:block/ruin_props/{id}/{part}','y':i*90}
 dump(A/f'blockstates/{id}.json',{'variants':variants})
 # A full three-block Java item exceeds the legacy +/-32 element range: uniformly
 # normalize geometry itself for the item only; source and placed parts remain full size.
 item=copy.deepcopy(m)
 for e in item['elements']:
  for edge in ['from','to']:e[edge]=[v/max(dims) for v in e[edge]]
 item['display']={'gui':{'rotation':[25,225,0],'scale':[.85]*3},'ground':{'scale':[.3]*3},'fixed':{'scale':[.7]*3}}
 dump(A/f'models/block/{id}.json',item);dump(A/f'models/item/{id}.json',{'parent':f'xiantu:block/{id}'})
 dump(D/f'xiantu/loot_tables/blocks/{id}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'xiantu:'+id}],'conditions':[{'condition':'minecraft:block_state_property','block':'xiantu:'+id,'properties':{'part':'0_0_0'}},{'condition':'minecraft:survives_explosion'}]}]})
 boxes=[e['from']+e['to']for e in m['elements']]
 dump(O/(id+'-geometry.json'),{'dimensions':dims,'boxes':boxes,'cubes':len(g),'faces':sum(len(e['faces'])for e in m['elements'])})
 print(id,len(g),flush=True)
 return boxes
dump(O/'model-plan.json',{'target':'Forge1.20.1 baked multicell Java block, desktop vanilla shading','quality':'appearance/detail; preserve requested large silhouettes','budget':'static baked models; no per-frame renderer','textures':'existing 64px albedo, proportional 4px/model-unit tiled faces, no PBR','animation':'none','coffin':'2x5 footprint, 2 blocks tall, vaulted lid, recessed panels, bronze handles, seal','repair':'2x1 footprint, tabletop raised from11.5 to14.03 model units, raised tools occupy upper cells'})
c=author('ancient_burial_coffin',coffin(),[2,2,5]);r=author('equipment_repair_table',repair(),[2,2,1])
# Geometry and collision share the same authored boxes; static registration no BE.
def literal(boxes):return '{'+','.join('{'+','.join(map(str,a))+'}'for a in boxes)+'}'
java='''package com.longju.xiantu.building;
import java.util.Map;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.*;
/** Generated by author_ruin_props_20261025.py via live Blockbench MCP. */
public final class RuinFurniture {
 public static final double[][] REPAIR_BOXES=new double[][]REPAIR;
 public static void register(DeferredRegister<Block> registry,Map<String,RegistryObject<Block>> out){
  out.put("ancient_burial_coffin",registry.register("ancient_burial_coffin",()->new XianFurnitureBlock.Coffin(BlockBehaviour.Properties.copy(Blocks.DARK_OAK_PLANKS).strength(3).noOcclusion(),new double[][]COFFIN)));
 }
 private RuinFurniture(){}
}
'''.replace('REPAIR;',literal(r)+';').replace('COFFIN)',literal(c)+')')
(P/'src/main/java/com/longju/xiantu/building/RuinFurniture.java').write_text(java,encoding='utf8')
for lang,label in [('zh_cn','古修镇魂棺'),('en_us','Ancient Burial Coffin')]:
 p=A/f'lang/{lang}.json';v=json.loads(p.read_text(encoding='utf8'));v['block.xiantu.ancient_burial_coffin']=label;dump(p,v)
tag=D/'minecraft/tags/blocks/mineable/axe.json';v=json.loads(tag.read_text());v['values']=list(dict.fromkeys(v['values']+['xiantu:ancient_burial_coffin']));dump(tag,v)
dump(D/'xiantu/recipes/ancient_burial_coffin.json',{'type':'minecraft:crafting_shaped','pattern':['PPP','PIP','PPP'],'key':{'P':{'item':'minecraft:dark_oak_planks'},'I':{'item':'minecraft:gold_ingot'}},'result':{'item':'xiantu:ancient_burial_coffin'}})
