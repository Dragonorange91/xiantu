"""Survival resources for the server-owned talisman module."""
from pathlib import Path
import json

ROOT=Path(__file__).resolve().parents[1]
R=ROOT/'src/main/resources'
def write(path,value):
 p=R/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def append_tag(path,value):
 p=R/path;tag=json.loads(p.read_text(encoding='utf-8')) if p.exists() else {'replace':False,'values':[]}
 if value not in tag['values']:tag['values'].append(value)
 write(path,tag)
write('assets/xiantu/blockstates/cinnabar_ore.json',{'variants':{'':{'model':'xiantu:block/cinnabar_ore'}}})
write('assets/xiantu/models/block/cinnabar_ore.json',{'parent':'minecraft:block/cube_all','textures':{'all':'xiantu:block/cinnabar_ore'}})
write('assets/xiantu/models/item/cinnabar_ore.json',{'parent':'xiantu:block/cinnabar_ore'})
write('data/xiantu/worldgen/configured_feature/cinnabar_ore.json',{'type':'xiantu:cinnabar_ore','config':{}})
write('data/xiantu/worldgen/placed_feature/cinnabar_ore.json',{'feature':'xiantu:cinnabar_ore','placement':[{'type':'minecraft:in_square'},{'type':'minecraft:height_range','height':{'type':'minecraft:uniform','min_inclusive':{'absolute':-32},'max_inclusive':{'absolute':48}}},{'type':'minecraft:biome'}]})
write('data/xiantu/forge/biome_modifier/cinnabar_ore.json',{'type':'forge:add_features','biomes':'#minecraft:is_overworld','features':['xiantu:cinnabar_ore'],'step':'underground_ores'})
write('data/xiantu/loot_tables/blocks/cinnabar_ore.json',{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'xiantu:cinnabar','functions':[{'function':'minecraft:set_count','count':{'type':'minecraft:uniform','min':1,'max':2}},{'function':'minecraft:explosion_decay'}]}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
append_tag('data/minecraft/tags/blocks/mineable/pickaxe.json','xiantu:cinnabar_ore')
append_tag('data/minecraft/tags/blocks/needs_stone_tool.json','xiantu:cinnabar_ore')
write('data/xiantu/recipes/talisman_writing_table.json',{'type':'xiantu:facility_crafting','facility':'talisman_table','ingredients':[{'item':'xiantu:qing_wen_elm_planks'}]*4+[{'item':'minecraft:cobblestone'}]*2+[{'item':'minecraft:glass_bottle'},{'item':'xiantu:cinnabar'},{'item':'minecraft:feather'}],'result':{'item':'xiantu:talisman_writing_table'}})
effects={
 'fireball':('火球符','射程20格，火球命中造成火伤，不点燃方块。'),
 'thunder':('雷击符','标记24格内可见目标后落雷，不产生原版雷电或火焰。'),
 'sword_qi':('剑气符','射出20格剑气，最多穿透两个不同目标。'),
 'explosion':('爆裂符','落点延迟一秒爆发，半径3格，不破坏方块。'),
 'diamond':('金刚符','短时通用减伤，与铁壁丹取较高值。'),
 'ward':('护身符','获得临时护盾，同类取较高值，不叠加。'),
 'reflection':('反伤符','反射部分实际近战伤害，设有每秒上限。'),
 'lightbody':('轻身符','短时提高跳跃高度并减少摔落伤害。'),
 'speed':('神行符','短时提高移速，外部速度加成总上限40%。'),
 'invisibility':('隐身符','短时隐身，攻击、施法或受伤后解除。'),
 'concealment':('敛息符','降低妖兽发现新目标的距离，不清除已有仇恨。'),
 'healing':('回春符','六秒内逐步恢复生命，与同类丹药共享冷却。'),
 'detox':('解毒符','解除普通毒，三阶以上可解除瘴毒。'),
 'cleanse':('净体符','清除至多两种普通毒、瘴毒或流血，五阶可清经脉伤。'),
 'soul_seal':('镇魂符','降低妖兽或心魔的特殊攻击伤害，连续控制递减。'),
 'spirit_lock':('锁灵符','暂时禁止灵力主动技能，保留移动和普通攻击。'),
 'demon_seal':('封魔符','打断可中断招式，短时封禁妖兽或心魔特殊技能。'),
 'trap':('困阵符','在可见地面生成短时减速阵，可离开或跳出。'),
 'earth_escape':('遁地符','向前穿越有限天然土石，落点必须有安全支撑。'),
 'blink':('瞬移符','向前瞬移，不穿过实体墙，必须有安全落点。'),
 'long_return':('千里符','同维度返回本人制符台的有效归途印记。'),
 'rebirth':('转生符','记录修为，死亡后取回此符恢复；练气降一层，其余降三小阶段。')}
path=R/'assets/xiantu/lang/zh_cn.json';lang=json.loads(path.read_text(encoding='utf-8'))
lang.update({'itemGroup.xiantu.talisman':'仙途·符箓','item.xiantu.cinnabar':'朱砂','block.xiantu.cinnabar_ore':'朱砂矿','item.xiantu.space_sand':'空间砂','item.xiantu.talisman_residue':'符纸残屑','item.xiantu.broken_talisman_brush':'损坏符笔','item.xiantu.talisman_brush_1':'青纹羽毫笔','item.xiantu.talisman_brush_2':'玄铁霜羽笔','item.xiantu.talisman_brush_3':'星纹焚羽笔','item.xiantu.talisman_blueprint':'符箓典籍','item.xiantu.talisman_paper':'符纸'})
for name,(display,effect) in effects.items():lang['item.xiantu.talisman_'+name]=display;lang['tooltip.xiantu.talisman.'+name]=effect
write('assets/xiantu/lang/zh_cn.json',lang)
print('Talisman ore worldgen, loot, harvesting tags, nine-ingredient table recipe and 22 tooltips written')

append_tag("data/xiantu/tags/items/identifiable_materials.json","xiantu:talisman_paper")
