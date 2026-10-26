"""Import reviewed native Blockbench assets; preserve unrelated registry/tag resources."""
from pathlib import Path
import json,shutil,itertools,math
ROOT=Path(__file__).resolve().parents[1]
SOURCE=ROOT/'asset_sources/workstations-large-20261020'
OUT=ROOT/'asset_sources/workstations-herbs-integration-20261020'
ASSET=ROOT/'src/main/resources/assets/xiantu'
BACK=OUT/'backup'
def put(path,data):
 path=ROOT/path if not isinstance(path,Path) else path
 backup=BACK/path.relative_to(ROOT)
 if path.exists() and not backup.exists():backup.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(path,backup)
 path.parent.mkdir(parents=True,exist_ok=True)
 path.write_bytes(data if isinstance(data,bytes) else data.encode('utf-8'))
def js(path,data):put(path,json.dumps(data,ensure_ascii=False,indent=2)+'\n')
def edit(path,old,new):
 p=ROOT/path;t=p.read_text(encoding='utf-8')
 if new in t:return
 assert old in t,path
 put(p,t.replace(old,new,1))
def bounds(e):
 points=[];r=e.get('rotation');
 for v in itertools.product(*zip(e['from'],e['to'])):
  v=list(v)
  if r:
   axis='xyz'.index(r['axis']);a,b=[i for i in range(3) if i!=axis];ang=math.radians(r['angle']*(-1 if axis==1 else 1));x,y=v[a]-r['origin'][a],v[b]-r['origin'][b]
   v[a]=x*math.cos(ang)-y*math.sin(ang)+r['origin'][a];v[b]=x*math.sin(ang)+y*math.cos(ang)+r['origin'][b]
  points.append(v)
 return [math.floor(min(p[i] for p in points)*2)/2 for i in range(3)]+[math.ceil(max(p[i] for p in points)*2)/2 for i in range(3)]
def main():
 manifest=json.loads((SOURCE/'manifest.json').read_text(encoding='utf-8'))
 template=SOURCE/'delivery/resource-template/assets/xiantu'
 for p in template.rglob('*'):
  if p.is_file() and p.parent.name!='lang':put(ASSET/p.relative_to(template),p.read_bytes())
 rows=[]
 for m in manifest:
  ident=m['id'];model=json.loads((template/f'models/block/workstations/{ident}.json').read_text())
  boxes=[bounds(e) for e in model['elements']]
  java='{'+','.join('{'+','.join(str(v) for v in b)+'}' for b in boxes)+'}'
  rows.append(f'        add("{ident}", {str(m["height"]==1).lower()}, new double[][]{java});')
  js(ROOT/f'src/main/resources/data/xiantu/loot_tables/blocks/{ident}.json',{'type':'minecraft:block','pools':[{'rolls':1,'conditions':[{'condition':'minecraft:block_state_property','block':'xiantu:'+ident,'properties':{'part':'0_0_0'}},{'condition':'minecraft:survives_explosion'}],'entries':[{'type':'minecraft:item','name':'xiantu:'+ident}]}]})
 registry='''package com.longju.xiantu.workstation;
import com.longju.xiantu.XiantuMod;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.*;
import net.minecraftforge.eventbus.api.IEventBus;
import java.util.*;
public final class WorkstationRegistry {
    public static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(ForgeRegistries.BLOCKS,XiantuMod.MOD_ID);
    public static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,XiantuMod.MOD_ID);
    public static final Map<String,RegistryObject<Block>> EQUIPMENT=new LinkedHashMap<>();
    public static final Map<String,RegistryObject<Item>> EQUIPMENT_ITEMS=new LinkedHashMap<>();
    static {
ROWS
    }
    private static void add(String id,boolean table,double[][] boxes){
        RegistryObject<Block> block=BLOCKS.register(id,()->{
            var properties=BlockBehaviour.Properties.copy(table?Blocks.OAK_PLANKS:Blocks.COPPER_BLOCK).strength(3.5f).noOcclusion();
            return table?new WorkstationBlock.Table(properties,boxes):new WorkstationBlock(properties,boxes);
        });
        EQUIPMENT.put(id,block);
        EQUIPMENT_ITEMS.put(id,ITEMS.register(id,()->new BlockItem(block.get(),new Item.Properties())));
    }
    public static void register(IEventBus bus){BLOCKS.register(bus);ITEMS.register(bus);}
    private WorkstationRegistry(){}
}
'''.replace('ROWS','\n'.join(rows))
 put(ROOT/'src/main/java/com/longju/xiantu/workstation/WorkstationRegistry.java',registry)
 edit('src/main/java/com/longju/xiantu/XiantuMod.java','        com.longju.xiantu.medicine.MedicineRegistry.register(bus);','        com.longju.xiantu.medicine.MedicineRegistry.register(bus);\n        com.longju.xiantu.workstation.WorkstationRegistry.register(bus);')
 edit('src/main/java/com/longju/xiantu/registry/ModCreativeTabs.java','                output.accept(ModBlockItems.APPRAISAL.get());','                output.accept(ModBlockItems.APPRAISAL.get());\n                com.longju.xiantu.workstation.WorkstationRegistry.EQUIPMENT_ITEMS.values().forEach(item -> output.accept(item.get()));')
 for lang in ['zh_cn','en_us']:
  p=ASSET/f'lang/{lang}.json';d=json.loads(p.read_text(encoding='utf-8'))
  for m in manifest:d['block.xiantu.'+m['id']]=m['title'] if lang=='zh_cn' else m['id'].replace('_',' ').title()
  d['message.xiantu.workstation.prepared']='%s：设备已就位，制作功能尚未开放。' if lang=='zh_cn' else '%s: equipment placed; production is not available yet.'
  js(p,d)
 for tool in ['pickaxe','axe']:
  p=ROOT/f'src/main/resources/data/minecraft/tags/blocks/mineable/{tool}.json';d=json.loads(p.read_text())
  for m in manifest:
   if (m['height']==1)==(tool=='axe') and 'xiantu:'+m['id'] not in d['values']:d['values'].append('xiantu:'+m['id'])
  js(p,d)
 js(OUT/'workstations.json',manifest)
 print('Imported 14 workstations, native models, loot, creative entries, collision geometry.')
if __name__=='__main__':main()
