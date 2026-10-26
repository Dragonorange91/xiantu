"""Reproducible registry/resource import for the 22 independently authored creatures."""
import json,struct,hashlib,shutil,io,math
from pathlib import Path
from PIL import Image
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'asset_sources/creature-completion-20261017'
JAVA=ROOT/'src/main/java/com/longju/xiantu'
RES=ROOT/'src/main/resources/assets/xiantu'
# Runtime identity, authoring identity, name, behavior, locomotion, collision width/height, element.
SPECS=[
('thunder_prison_lion','lei_yu_shi','雷狱狮','H','LAND',1.5,2.0,'thunder'),
('electric_marten','dian_wen_diao','电纹貂','H','LAND',.8,.85,'thunder'),
('flint_armored_lizard','sui_jia_xi','燧甲蜥','N','LAND',1.15,.8,'fire'),
('molten_turtle','rong_wen_gui','熔纹龟','P','LAND',1.3,1.0,'fire'),
('sand_armadillo','sha_jia_qiu_yu','砂甲犰狳','P','LAND',.95,.8,'earth'),
('jade_scale_crocodile','bi_lin_e','碧鳞鳄','H','LAND',1.4,.8,'water'),
('five_element_warden','wu_xing_zhen_yu_shou','五行镇狱兽','H','BOSS',2.6,3.0,'earth'),
('copper_beak_vulture','tong_hui_jiu','铜喙鹫','H','FLY',.8,1.1,'metal'),
('fire_beak_falcon','huo_hui_sun','火喙隼','H','FLY',.7,.9,'fire'),
('purple_lightning_bat','zi_dian_fu','紫电蝠','H','FLY',.65,.7,'thunder'),
('azure_scale_python','cang_lin_mang','沧鳞蟒','H','LAND',1.2,1.55,'water'),
('ancient_branch_python','cang_zhi_mang','苍枝蟒','H','LAND',1.25,1.6,'wood'),
('tide_pattern_ray','chao_wen_yao','潮纹鳐','P','WATER',1.6,.3,'water'),
('dark_shell_crab','xuan_ke_xie','玄壳蟹','N','LAND',1.35,.85,'water'),
('gold_back_beetle','jin_bei_jia_chong','金背甲虫','N','LAND',1.1,.75,'metal'),
('jade_leaf_mantis','bi_ye_tang','碧叶螳','H','LAND',.95,1.45,'wood'),
('mushroom_toad','mo_san_chan','蘑伞蟾','N','LAND',1.1,.95,'wood'),
('battle_general_soul','bai_zhan_jiang_hun','百战将魂','H','BOSS',1.1,2.9,'metal'),
('meteor_general','xing_yun_tian_jiang','星陨天将','H','BOSS',1.2,3.3,'metal'),
('spirit_ape_king','tong_ling_yuan_wang','通灵猿王','H','BOSS',1.4,2.9,'earth'),
('xuantians_remnant','xuan_tian_cheng_zhu','玄天城主残魂','H','BOSS',1.2,3.2,'water'),
('cloud_sea_lord','yun_hai_tian_jun','云海天君遗念','H','BOSS',1.1,3.0,'water')]
COLORS={'thunder':(0x443460,0xCDC4FF),'fire':(0x863C24,0xFFA538),'earth':(0x66523B,0xD4B975),'water':(0x245971,0x8AD4DD),'wood':(0x426B36,0xBED58F),'metal':(0x84622C,0xE8CD80)}

def read(path):
 b=path.read_bytes();assert struct.unpack_from('<III',b)==(0x46546C67,2,len(b))
 n,t=struct.unpack_from('<II',b,12);assert t==0x4E4F534A
 d=json.loads(b[20:20+n]);bn,bt=struct.unpack_from('<II',b,20+n);blob=b[28+n:];assert bt==0x004E4942 and bn==len(blob)
 assert not d.get('extensionsRequired');assert all('uri' not in v for v in d.get('images',[])+d.get('buffers',[]))
 return d,blob
def values(d,b,i):
 a=d['accessors'][i];v=d['bufferViews'][a['bufferView']];fmt,size={5126:('f',4),5123:('H',2),5121:('B',1),5125:('I',4)}[a['componentType']]
 width={'SCALAR':1,'VEC2':2,'VEC3':3,'VEC4':4,'MAT4':16}[a['type']];off=v.get('byteOffset',0)+a.get('byteOffset',0);stride=v.get('byteStride',size*width)
 return [struct.unpack_from('<'+fmt*width,b,off+j*stride) for j in range(a['count'])]
def images(d,b):
 result=[]
 for im in d.get('images',[]):
  v=d['bufferViews'][im['bufferView']];raw=b[v.get('byteOffset',0):v.get('byteOffset',0)+v['byteLength']]
  result.append((hashlib.sha256(raw).hexdigest(),list(Image.open(io.BytesIO(raw)).size)))
 return sorted(result)

def main():
 rows=[]
 for rid,ident,name,behavior,kind,width,height,element in SPECS:
  mf=OUT/(ident+'-manifest.json')
  if not mf.exists():mf=OUT/(ident.replace('_','-')+'-manifest.json')
  m=json.loads(mf.read_text(encoding='utf8'));src=Path(m['source'])
  assert hashlib.sha256(src.read_bytes()).hexdigest()==m['source_sha256']
  d,b=read(OUT/(ident+'.glb'));ld,lb=read(OUT/(ident+'_lod.glb'));sd,sb=read(src)
  assert len(d['meshes'])==len(d['skins'])==len(d['scenes'])==1
  assert not ld.get('skins') and not ld.get('animations')
  original=images(sd,sb);actual=images(d,b)
  assert all(im in actual for im in original),(ident,'original atlas missing')
  assert images(ld,lb)==actual,(ident,'LOD atlas mismatch')
  positions=[];tris=0
  for p in d['meshes'][0]['primitives']:
   assert p.get('mode',4)==4 and {'NORMAL','POSITION','TEXCOORD_0','JOINTS_0','WEIGHTS_0'}<=p['attributes'].keys()
   a=p['attributes'];pos=values(d,b,a['POSITION']);positions+=pos
   assert all(math.isfinite(v) for vs in pos for v in vs)
   assert all(abs(sum(w)-1)<1e-4 for w in values(d,b,a['WEIGHTS_0']))
   assert max(max(j) for j in values(d,b,a['JOINTS_0']))<len(d['skins'][0]['joints'])
   ix=values(d,b,p['indices']);assert max(i[0] for i in ix)<len(pos);tris+=len(ix)//3
  clips=[a['name'] for a in d['animations']]
  assert all(n.startswith(ident+'.') for n in clips)
  assert {ident+'.'+c for c in ['idle','hurt','death']}<=set(clips)
  for a in d['animations']:
   assert a['channels']
   for s in a['samplers']:
    times=[v[0] for v in values(d,b,s['input'])];assert times==sorted(set(times))
    assert all(math.isfinite(v) for vs in values(d,b,s['output']) for v in vs)
  body=m.get('body_height',max(p[1] for p in positions)-min(p[1] for p in positions))
  scale=m['target_height_blocks']/body
  row=dict(runtime_id=rid,model=ident,name=name,behavior=behavior,kind=kind,width=width,height=height,element=element,model_scale=scale,visual_height=m['target_height_blocks'],triangles=tris,joints=len(d['skins'][0]['joints']),clips=clips,images=actual,source=str(src),source_sha256=m['source_sha256'])
  for suffix in ('','_lod'):
   f=OUT/(ident+suffix+'.glb');shutil.copy2(f,RES/'models/entity'/f.name);row['lod_sha256' if suffix else 'sha256']=hashlib.sha256(f.read_bytes()).hexdigest()
  row['status']='integrated_pending_build_and_runtime';rows.append(row)
  (RES/'models/item'/(rid+'_spawn_egg.json')).write_text(json.dumps({'parent':'minecraft:item/template_spawn_egg'}),encoding='utf8')
 for locale in ('zh_cn','en_us'):
  path=RES/'lang'/(locale+'.json');data=json.loads(path.read_text(encoding='utf8'))
  for r in rows:
   name=r['name'] if locale=='zh_cn' else r['runtime_id'].replace('_',' ').title()
   data['entity.xiantu.'+r['runtime_id']]=name
   data['item.xiantu.'+r['runtime_id']+'_spawn_egg']=name+('刷怪蛋' if locale=='zh_cn' else ' Spawn Egg')
  path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
 (OUT/'integration-manifest.json').write_text(json.dumps(rows,ensure_ascii=False,indent=2),encoding='utf8')
 # Definitions deliberately keep server data separate from renderer-only GLB classes.
 lines=[]
 for r in rows:
  base,spot=COLORS[r['element']];boss=r['kind']=='BOSS'
  lines.append(f'        new Definition("{r["runtime_id"]}", "{r["model"]}", "{r["behavior"]}", "{r["kind"]}", "{r["element"]}", {r["width"]}F, {r["height"]}F, {r["model_scale"]:.6f}F, {160 if boss else 36}, {8 if boss else 4}, 0x{base:06X}, 0x{spot:06X})')
 text='''package com.longju.xiantu.entity;
import java.util.*;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;
/** Source-fitted models; combat stats are provisional until the realm rollout. */
public final class CompletionBestiary {
    public record Definition(String id, String model, String behavior, String kind, String element,
            float width, float height, float scale, double health, double damage, int eggBase, int eggSpot) {
        public boolean passive() { return behavior.equals("P"); }
        public boolean hostile() { return behavior.equals("H"); }
        public boolean boss() { return kind.equals("BOSS"); }
    }
    public static final List<Definition> ALL = List.of(
'''+',\n'.join(lines)+''');
    public static Definition get(EntityType<?> type) {
        var key=ForgeRegistries.ENTITY_TYPES.getKey(type);
        return ALL.stream().filter(d -> key != null && d.id().equals(key.getPath())).findFirst().orElseThrow();
    }
    private CompletionBestiary() {}
}
'''
 (JAVA/'entity/CompletionBestiary.java').write_text(text,encoding='utf8')
 print('IMPORTED',len(rows),'independent rigs and static LODs')
if __name__=='__main__':main()
