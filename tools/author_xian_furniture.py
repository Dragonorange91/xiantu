"""Editable Java block models authored/exported through the live Blockbench bridge.
Uses original vanilla materials; imports copies extracted from the installed game JAR.
"""
from pathlib import Path
import json, zipfile, re, base64, io, random, shutil
from PIL import Image,ImageDraw
from blockbench_native_bridge import NativeBridge

P=Path(__file__).resolve().parents[1]; A=P/'src/main/resources/assets/xiantu'; D=P/'src/main/resources/data'
O=P/'asset_sources/xian-furniture-20261024'; O.mkdir(parents=True,exist_ok=True)
J=(Path.home() / 'Desktop/仙途/.minecraft/versions/1.20.1-Forge_47.4.26/1.20.1-Forge_47.4.26.jar')
def dump(p,v): p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(v,ensure_ascii=False,indent=2),encoding='utf8')
def box(n,a,b,t='wood'):
    sizes=[b[i]-a[i] for i in range(3)]
    faces={f:{'uv':[0,0,w,h],'texture':t+'.png'} for f,w,h in [('north',sizes[0],sizes[1]),('south',sizes[0],sizes[1]),('east',sizes[2],sizes[1]),('west',sizes[2],sizes[1]),('up',sizes[0],sizes[2]),('down',sizes[0],sizes[2])]}
    return dict(name=n,**{'from':a,'to':b},box_uv=False,autouv=0,faces=faces)
def legs(g,h=9):
    for x in [2,12]:
        for z in [2,12]:
            g.append(box(f'leg_{x}_{z}',[x,0,z],[x+2,h,z+2]))
            g.append(box(f'foot_{x}_{z}',[x-.3,0,z-.3],[x+2.3,1,z+2.3],'edge'))
def table(round_=False):
    g=[];legs(g,11)
    if round_:
        for i,(lo,hi) in enumerate([(3,13),(1,15),(0,16),(1,15),(3,13)]):
            z=[0,2,5,11,14][i]; Z=[2,5,11,14,16][i]
            g.append(box(f'round_top_{i}',[lo,11,z],[hi,13,Z],'stone'))
        # Simple smooth-stone furniture; clean silhouette, no jewel trim.
        return [dict(c,faces={f:dict(v,texture='stone.png') for f,v in c['faces'].items()})for c in g]
    g.append(box('top',[.4,11,.4],[15.6,13,15.6],'wood'))
    for z in [1,14]:g.append(box('apron'+str(z),[2,8,z],[14,11,z+1],'edge'))
    for x in [1,14]:g.append(box('side_apron'+str(x),[x,8,2],[x+1,11,14],'edge'))
    return g
chair=[];legs(chair,7)
chair += [box('seat',[1,7,1],[15,8.6,15]),box('back_left',[1,8,12],[3,16,14],'edge'),box('back_right',[13,8,12],[15,16,14],'edge'),box('crest',[1,14.5,12],[15,16,14],'edge'),box('back_rail',[2,10,12.5],[14,11,13.5])]
for x in [5,8,11]:chair.append(box('back_splat'+str(x),[x-.4,10,12.5],[x+.4,15,13.5]))
for x in [1.5,13]:chair.extend([box('arm_post'+str(x),[x,8,3],[x+1.5,11,4.5],'edge'),box('arm'+str(x),[x,10.5,2],[x+1.5,12,13],'edge')])
bench=[box('seat',[0,8,3],[16,10,13])]
for x in [2,12]:bench.extend([box('bench_leg'+str(x),[x,0,4],[x+2,8,12]),box('foot'+str(x),[x-1,0,3],[x+3,1.5,13],'edge')])
bench.append(box('stretcher',[3,3,7],[13,4,9],'edge'))
def shelving(display=False):
    g=[box('plinth',[.5,0,2],[15.5,2,15]),box('crown',[0,14.5,1],[16,16,15.5],'edge'),box('left',[1,1,2],[2.5,15,15],'edge'),box('right',[13.5,1,2],[15,15,15],'edge')]
    if not display:g.append(box('back',[2,1,13.5],[14,15,15]))
    for y in [5.5,10]:g.append(box('shelf'+str(y),[2,y,2],[14,y+1,14],'wood'))
    g.append(box('partition',[7.5,2,3],[8.5,10,14],'edge'))
    if display:
        for x,y in [(4,2),(10,6.5),(4,11)]:
            g.extend([box('object_base'+str(y),[x,y,7],[x+3,y+1,11],'edge'),box('jade_vase'+str(y),[x+.5,y+1,7.5],[x+2.5,y+3,10.5],'jade')])
    else:
        for y in [2,6.5,11]:
            for i in range(8):
                x=2.6+i*1.25
                g.append(box(f'book_{y}_{i}',[x,y,7+(i%2)],[x+.85,y+2.8+(i%3)*.2,13],'book'+str(i%3)))
    return g
cab=[box('base',[1,0,2],[15,2,15]),box('body',[1,2,3],[15,14,15]),box('top',[0,14,1],[16,16,16],'edge')]
for x in [2,8]:
    cab.extend([box('panel'+str(x),[x,3,2],[x+5.5,13,3],'edge'),box('inset'+str(x),[x+.5,4,1.8],[x+5,12,2.2]),box('handle'+str(x),[x+2,7,1.4],[x+3,8,2],'gold')])
lan=[]
for i,(lo,hi,y,Y) in enumerate([(5,11,1,3),(3,13,3,5),(2,14,5,10),(3,13,10,12),(5,11,12,14)]):
    lan.append(box('red_silk'+str(i),[lo,y,lo],[hi,Y,hi],'red'))
for y in [2.8,11.8]:lan.append(box('gold_rim'+str(y),[3.2,y,3.2],[12.8,y+.65,12.8],'gold'))
lan.extend([box('loop',[7,14,7],[9,16,9],'gold'),box('tassel',[7.3,0,7.3],[8.7,2,8.7],'red')])
for x,z in [(2,7.5),(13.5,7.5),(7.5,2),(7.5,13.5)]:lan.append(box('rib'+str((x,z)),[x,5,z],[x+.5,10,z+.5],'gold'))
stool=[box('foot',[3,0,3],[13,2,13],'stone'),box('stem',[5,2,5],[11,7,11],'stone'),box('seat',[2,7,2],[14,9,14],'stone')]
desk=table();desk +=[box('inkstone',[3,13,9],[6,13.5,12],'stone'),box('scroll',[7,13,6],[13,13.3,12],'paper'),box('brush',[10,13.3,8],[10.5,13.8,13],'edge')]
# A higher back keeps a real seat height; the desk and round table occupy real cells.
for c in chair:
    for edge in ['from','to']:
        if c[edge][1]>8.6:c[edge][1]=8.6+(c[edge][1]-8.6)*2.2
def stretch(cubes,x=1,z=1):
    for c in cubes:
        for edge in ['from','to']:c[edge][0]*=x;c[edge][2]*=z
        sizes=[c['to'][i]-c['from'][i]for i in range(3)]
        for f,w,h in [('north',sizes[0],sizes[1]),('south',sizes[0],sizes[1]),('east',sizes[2],sizes[1]),('west',sizes[2],sizes[1]),('up',sizes[0],sizes[2]),('down',sizes[0],sizes[2])]:c['faces'][f]['uv']=[0,0,min(16,w),min(16,h)]
    return cubes
models=[('xian_wood_chair','木制高背圈椅',chair),('xian_wood_table','木制书桌',stretch(table(),2)),('xian_writing_desk','木制书案',stretch(desk,2)),('xian_wood_bench','木制长凳',bench),('xian_bookshelf','木制藏书架',shelving()),('xian_display_shelf','木制博古架',shelving(True)),('xian_wood_cabinet','木制储物柜',cab),('xian_red_lantern','中式红灯笼',lan),('xian_stone_stool','石凳',stool),('xian_round_stone_table','圆石桌',stretch(table(True),2,2))]
dimensions={'xian_wood_chair':[1,2,1],'xian_wood_table':[2,1,1],'xian_writing_desk':[2,1,1],'xian_round_stone_table':[2,1,2]}
from xian_furniture_reference_shapes import reference_models
models=reference_models(models)
dimensions.update({'xian_bookshelf':[2,2,1],'xian_wood_wardrobe':[2,2,1]})
tex={'wood':'spruce_planks','edge':'dark_oak_planks','stone':'smooth_stone','red':'red_wool','gold':'gold_block','jade':'prismarine_bricks','paper':'white_wool','book0':'red_terracotta','book1':'cyan_terracotta','book2':'yellow_terracotta'}
b=NativeBridge();dump(O/'host-status.json',b.call('get_status'));dump(O/'formats.json',b.call('list_formats'))
with zipfile.ZipFile(J)as jar:
    for t,id in tex.items():(O/(t+'.png')).write_bytes(jar.read('assets/minecraft/textures/block/'+id+'.png'))
    # No slab side/border: sample only the interior of vanilla smooth stone.
    im=Image.open(io.BytesIO(jar.read('assets/minecraft/textures/block/smooth_stone.png'))).convert('RGB')
    im.crop((2,2,14,14)).resize((64,64),Image.Resampling.NEAREST).save(O/'stone.png')
    grain=Image.open(io.BytesIO(jar.read('assets/minecraft/textures/block/stripped_spruce_log.png'))).convert('L').resize((64,64),Image.Resampling.NEAREST)
    for t,mul in [('wood',(1.25,.59,.28)),('edge',(.55,.27,.16))]:
        out=Image.new('RGB',(64,64));out.putdata([tuple(min(255,int(v*a))for a in mul)for v in grain.getdata()]);out.save(O/(t+'.png'))
texpaths={t:'minecraft:block/'+id for t,id in tex.items()}
for t in ['stone','wood','edge']:texpaths[t]='xiantu:block/xian_furniture_'+t
rng=random.Random(20261024);im=Image.new('RGB',(32,32));im.putdata([(255,180+rng.randrange(30),61+rng.randrange(20))for _ in range(1024)]);im.save(O/'amber.png')
texpaths['amber']='xiantu:block/xian_furniture_amber'
im=Image.open(O/'wood.png').resize((128,128),Image.Resampling.NEAREST);dr=ImageDraw.Draw(im)
for pad in [5,10]:dr.rectangle((pad,pad,127-pad,127-pad),outline='#5d2611',width=2)
# Original low-relief bamboo and cloud scrolls; no watermark/photo pixels imported.
for x,y,X,Y in [(44,104,54,27),(74,102,70,40),(55,73,31,55),(54,59,79,35),(72,80,97,61)]:
    dr.line((x,y,X,Y),fill='#693116',width=3)
    dr.line((x+2,y,X+2,Y),fill='#bb6e32',width=1)
    for k in [.25,.55,.8]:
        ax=x+(X-x)*k;by=y+(Y-y)*k;dr.line((ax,by,ax-12,by-6),fill='#65301a',width=2);dr.line((ax,by,ax+10,by-9),fill='#d68a40',width=1)
for y in [16,109]:
    for x in [16,92]:dr.line([(x,y+7),(x,y),(x+17,y),(x+17,y+5),(x+7,y+5),(x+7,y+10),(x+20,y+10)],fill='#c17a37',width=2)
im.save(O/'carving.png');texpaths['carving']='xiantu:block/xian_furniture_carving'
# Chair-specific vertical peony/cloud relief. Paint in the real narrow aspect,
# then store in a square tile; mapping the full tile restores that aspect.
chairart=Image.open(O/'wood.png').resize((32,128),Image.Resampling.NEAREST);cd=ImageDraw.Draw(chairart)
for x in [2,29]:cd.line((x,4,x,123),fill='#9d5528',width=1)
cd.line([(16,106),(13,92),(18,78),(14,64),(16,52)],fill='#e8a757',width=2)
cd.line([(17,106),(14,92),(19,78),(15,64),(17,52)],fill='#632b14',width=1)
for x,y,side in [(14,94,-1),(17,82,1),(15,68,-1)]:
    cd.ellipse((x-8 if side<0 else x,y-8,x if side<0 else x+8,y-2),fill='#9c592b',outline='#e0a35c')
for angle in range(0,360,45):
    import math
    px=16+5*math.cos(math.radians(angle));py=43+7*math.sin(math.radians(angle))
    cd.ellipse((px-3,py-4,px+3,py+4),fill='#b5783c',outline='#e2ae65')
cd.ellipse((13,39,19,47),fill='#d8a05b',outline='#6b3218')
cd.arc((8,17,23,32),0,280,fill='#dda15a',width=2);cd.arc((13,20,24,31),70,310,fill='#dda15a',width=1)
cd.arc((7,108,24,121),0,310,fill='#dda15a',width=2)
chairart.resize((128,128),Image.Resampling.NEAREST).save(O/'chair_carving.png');texpaths['chair_carving']='xiantu:block/xian_furniture_chair_carving'
for t,resource in texpaths.items():
    if resource.startswith('xiantu:'):
        target=A/('textures/'+resource.split(':')[1]+'.png');target.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(O/(t+'.png'),target)
refs=['5a50f98e-4be0-4d2f-b862-b316a4e1ae84','758d5166-0918-43d2-9ad6-edf9ad647a4c','c63251f3-e679-4b32-ae7f-dcb6638bd972','aaecbea2-32b8-41fa-8053-d99f46e0abe8','4fe105bb-692e-45a8-a319-ccd6cbdc2eb9','f46a9921-692c-4841-a2c2-600c7c20f6ad']
(O/'references').mkdir(exist_ok=True)
for k,id in enumerate(refs):
    src=(Path.home() / 'AppData/Local/Temp')/('codex-clipboard-'+id+'.png')
    if src.exists():shutil.copy2(src,O/'references'/f'{k+1}.png')
records=[]
for id,name,cubes in models:
    b.call('new_project',format='java_block',name=id,texture_width=16,texture_height=16)
    used={f['texture'][:-4]for c in cubes for f in c['faces'].values()}
    for t in sorted(used):b.call('import_texture',path=str(O/(t+'.png')),name=t+'.png')
    b.call('add_cubes',cubes=cubes)
    b.call('save_project',path=str(O/(id+'.bbmodel')))
    b.call('export_project',path=str(O/(id+'.json')))
    report=b.call('check_model');dump(O/(id+'-audit.json'),report)
    b.call('set_camera_angle',position=[34,26,-34],target=[8,8,8])
    shot=b.call('screenshot',view='front_right',width=768,height=768,annotate=False)
    # Bridge versions return either a data_url or image field.
    if isinstance(shot,dict):
        data=next((v for v in shot.values()if isinstance(v,str)and v.startswith('data:image')),None)
        if data:(O/(id+'.png')).write_bytes(base64.b64decode(data.split(',',1)[1]))
    m=json.loads((O/(id+'.json')).read_text(encoding='utf8'));m.pop('format_version',None)
    m['textures']={k:texpaths[Path(v.replace('\\','/')).stem] for k,v in m['textures'].items()}
    m['textures']['particle']=texpaths['stone'if 'stone'in id else 'amber'if 'lantern'in id else 'wood']
    dims=dimensions.get(id,[1,1,1]);sc=.8/max(dims)
    m['display']={'gui':{'rotation':[22,225,0],'translation':[-4*(dims[0]-1),-3*(dims[1]-1),0],'scale':[sc,sc,sc]},'ground':{'translation':[0,3,0],'scale':[.3,.3,.3]}}
    if 'lantern'in id:
        for e in m['elements']:
            if e.get('name','').startswith('luminous_paper'):e['forge_data']={'block_light':15,'sky_light':15};e['shade']=False
    dump(A/f'models/block/{id}.json',m);dump(A/f'models/item/{id}.json',{'parent':'xiantu:block/'+id})
    dump(A/f'blockstates/{id}.json',{'variants':{f'facing={f}':{'model':'xiantu:block/'+id,'y':i*90}for i,f in enumerate(['north','east','south','west'])}})
    if id in dimensions:
        import copy
        dims=dimensions[id];variants={}
        for x in range(dims[0]):
          for y in range(dims[1]):
           for z in range(dims[2]):
            part=f'{x}_{y}_{z}';partmodel=copy.deepcopy(m);partmodel['elements']=[]
            for e in m['elements']:
                owner=[min(dims[i]-1,int((e['from'][i]+e['to'][i])/32))for i in range(3)]
                if owner!=[x,y,z]:continue
                e=copy.deepcopy(e)
                for edge in ['from','to']:e[edge]=[e[edge][i]-owner[i]*16 for i in range(3)]
                partmodel['elements'].append(e)
            dump(A/f'models/block/furniture/{id}/{part}.json',partmodel)
            for turn,facing in enumerate(['north','east','south','west']):variants[f'facing={facing},part={part}']={'model':f'xiantu:block/furniture/{id}/{part}','y':turn*90}
        dump(A/f'blockstates/{id}.json',{'variants':variants})
    dump(D/f'xiantu/loot_tables/blocks/{id}.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'xiantu:'+id}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
    patterns={'xian_wood_chair':['P P','PPP','S S'],'xian_wood_table':['PPP',' S ','S S'],'xian_writing_desk':['PPP','SSS','S S'],'xian_wood_bench':['   ','PPP','S S'],'xian_bookshelf':['PPP','PBP','PPP'],'xian_display_shelf':['PPP','S S','PPP'],'xian_wood_cabinet':['PPP','PSP','PPP'],'xian_red_lantern':[' S ','PPP',' S '],'xian_stone_stool':[' PP',' S ',' S '],'xian_round_stone_table':['PPP',' S ',' S ']}
    patterns['xian_wood_wardrobe']=['PPP','P P','PBP']
    pattern=patterns[id];keys={'P':{'item':'minecraft:smooth_stone'if 'stone'in id else 'minecraft:glowstone_dust'if 'lantern'in id else 'minecraft:spruce_planks'},'S':{'item':'minecraft:iron_nugget'if 'lantern'in id else 'minecraft:stick'},'B':{'item':'minecraft:chest'if 'wardrobe'in id else 'minecraft:book'}}
    dump(D/f'xiantu/recipes/{id}.json',{'type':'minecraft:crafting_shaped','pattern':pattern,'key':{k:v for k,v in keys.items()if any(k in r for r in pattern)},'result':{'item':'xiantu:'+id,'count':1}})
    if id in dimensions:
        loot=D/f'xiantu/loot_tables/blocks/{id}.json';v=json.loads(loot.read_text());v['pools'][0]['conditions'].insert(0,{'condition':'minecraft:block_state_property','block':'xiantu:'+id,'properties':{'part':'0_0_0'}});dump(loot,v)
    records.append({'id':id,'name':name,'cubes':len(cubes),'dimensions':dimensions.get(id,[1,1,1]),'boxes':[e['from']+e['to'] for e in m['elements']]})
    print(id,len(cubes),flush=True)
dump(D/'xiantu/xian_furniture.json',{'source':'BlockbenchMCP renderer bridge protocol 1; vanilla Minecraft materials','models':records})
for lang in ['zh_cn','en_us']:
    p=A/f'lang/{lang}.json';v=json.loads(p.read_text(encoding='utf8'))
    for r in records:v['block.xiantu.'+r['id']]=r['name']if lang=='zh_cn'else r['id'].replace('_',' ').title()
    dump(p,v)
# Keep one static baked registry and the existing automatic block-item/creative integration.
p=P/'src/main/java/com/longju/xiantu/building/TownMaterialRegistry.java';s=p.read_text(encoding='utf8')
if 'XianFurniture.register'not in s:s=s.replace('        return Collections.unmodifiableMap(result);','        XianFurniture.register(registry, result);\n        return Collections.unmodifiableMap(result);');p.write_text(s,encoding='utf8')
lines=[]
for r in records:
    boxes='{'+','.join('{'+','.join(map(str,a))+'}'for a in r['boxes'])+'}'
    prop='Blocks.SMOOTH_STONE'if 'stone'in r['id']else 'Blocks.OAK_PLANKS'
    light='.lightLevel(s -> 15)'if 'lantern'in r['id']else ''
    cls={'xian_wood_chair':'TallChair','xian_wood_table':'LongDesk','xian_writing_desk':'LongDesk','xian_round_stone_table':'RoundTable','xian_bookshelf':'WideTall','xian_wood_wardrobe':'WideTall'}.get(r['id'])
    ctor=('XianFurnitureBlock.'+cls)if cls else 'TownShapedBlock'
    arg=''if cls else 'false, '
    lines.append(f'        out.put("{r["id"]}", registry.register("{r["id"]}", () -> new {ctor}(BlockBehaviour.Properties.copy({prop}).strength(2F).noOcclusion(){light}, {arg}new double[][]{boxes})));')
(P/'src/main/java/com/longju/xiantu/building/XianFurniture.java').write_text('''package com.longju.xiantu.building;
import java.util.Map;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.*;
/** Baked wooden furniture; no ticker or entity renderer. Source: author_xian_furniture.py. */
public final class XianFurniture {
    public static void register(DeferredRegister<Block> registry, Map<String,RegistryObject<Block>> out) {
'''+ '\n'.join(lines)+ '\n    }\n    private XianFurniture() {}\n}\n',encoding='utf8')
for tag,ids in [('axe',[r['id']for r in records if 'stone'not in r['id']]),('pickaxe',[r['id']for r in records if 'stone'in r['id']])]:
    p=D/f'minecraft/tags/blocks/mineable/{tag}.json';v=json.loads(p.read_text(encoding='utf8'));v['values']=sorted(set(v['values'])|{'xiantu:'+id for id in ids});dump(p,v)
print('Furniture runtime resources and static registrations integrated.')
