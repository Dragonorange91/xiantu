"""Expose the approved 8.12 rules; no new damage/animation behavior is invented here."""
import json,re
from pathlib import Path
project=Path(__file__).resolve().parents[1]
outline=(Path.home() / 'Documents/Codex/2026-10-05/f/outputs/仙途模组大纲_功能模块实施版.md').read_text(encoding='utf-8-sig')
section=outline.split('### 8.12 普通剑斧枪效果表',1)[1].split('护甲四件套：',1)[0]
recipes=json.loads((project/'src/main/resources/data/xiantu/refining/catalog.json').read_text(encoding='utf-8-sig'))['recipes']
names=['凝霜','无影','含光引诀','照隐','破障','惊雷','流虹','三叠离火','龙势','百炼破甲','凿甲','裂地震','断岳迟滞','碎星蚀盾','开天蓄势','破军夺势','镇岳反震','玄铁凿甲','巨灵压制','混沌势','破阵','贯日穿刺','穿云步','弑法','灭道蚀抗','玄龙势','天罡守','仙灵回元','神目','大道定心']
out=[]
for line in section.splitlines():
    columns=[x.strip() for x in line.strip('|').split('|')]
    if len(columns)!=3:continue
    theme,base,condition=columns
    actual=[r for r in recipes if r['theme']==theme and r['kind'] in ('sword','axe','spear') and r['enabled']]
    if not actual:continue
    index=len(out);minimum=min(r['tier'] for r in actual);maximum=max(r['tier'] for r in actual)
    activation='右键蓄力1秒，松开后6秒内的下一次普攻生效；移动或受击取消' if theme=='开天斧' else '右键蓄力0.8秒后松开，前方3格内最多贯穿3个敌人；移动或受击取消' if theme=='贯日枪' else '满足描述条件后自动触发，不占法术槽'
    out.append(dict(theme=theme,name=names[index],kind=actual[0]['kind'],min_tier=minimum,max_tier=maximum,base=base,condition=condition,condition_min_tier=5,condition_available=maximum>=5,activation=activation,source='大纲8.12',presentation_status='留待GPTT6A；当前不增加动画或特效'))
assert len(out)==30 and len({s['theme'] for s in out})==30
data=dict(schema_version=1,id='xiantu:weapon_skill_settings',enabled=True,source='用户确认大纲8.12/8.13',rules={'primary_hand_only':True,'condition_min_tier':5,'quality_does_not_unlock_tiers':True,'sword_axe_staff_attack':'vanilla','spear_attack':'existing_custom','secondary_cannot_retrigger':True,'art_owner':'GPTT6A'},ordinary=out,special={'theme':'齐天','weapon':'金箍棒','required_tier':7,'required_quality':5,'passive':'定海：第三次同目标普攻沉重2秒，单目标冷却8秒；Boss只减速15%','extend':{'name':'如意伸缩','hold_ticks':16,'cost':5000,'cooldown_ticks':500,'range':12},'quake':{'name':'金箍镇岳','hold_ticks':40,'cost':12000,'cooldown_ticks':1800,'radius':4},'input':'主手右键蓄力，0.8秒前取消，0.8至不足2秒伸缩，满2秒镇岳；潜行右键仅切外观；移动或受击取消'})
p=project/'src/main/resources/data/xiantu/refining/weapon_skill_settings.json'
if p.exists():raise RuntimeError('Historical generator: would overwrite current integrated skills. Revise incrementally instead.')
p.write_text(json.dumps(data,ensure_ascii=False,indent=2),encoding='utf-8')
lines=['# 武器技能设定与动作特效交接（2026-10-23）','','规则直接取自大纲8.12、8.13。本轮补齐技能命名、合法阶位、触发与说明界面，不修改既有伤害和攻击动画。','', '一至四阶仅基础被动；五至七阶成品才可触发该武器已定义的高阶效果。品质提升只按既有系数缩放，不能替代阶位。许多早期武器最高仅三或四阶，不能靠极品品质解锁五阶条件效果。','', '仅主手、境界达标且未损坏时生效；范围逐目标结算，不穿墙、不伤队友，二次伤害不递归触发。剑、斧、棍保留原版攻击，长枪保留当前已认可动作。','', '## 三十种普通武器','', '| 武器（合法制作阶位） | 技能名 | 基础被动 | 高阶条件效果与操作 |','|---|---|---|---|']
for s in out:
    condition=s['condition']+'；'+s['activation'] if s['condition_available'] else '当前合法阶位没有高阶条件效果，不新增越阶配方'
    lines.append(f"| {s['theme']}（{s['min_tier']}～{s['max_tier']}） | {s['name']} | {s['base']} | {condition} |")
lines+=['','## 金箍棒','','沿用极品专属定海、如意伸缩与金箍镇岳；5,000/12,000固定灵力、25/90秒冷却，右键蓄力分段互斥。普通品质不获得极品专属技能。齐天装备技能仍使用独立装备技能键，不占法术栏。','','## GPTT6A制作要求','','先按各自机制制作分镜：青霜是命中后凝霜附着；承影是闪避蓄势后一次残影追击；含光是暴击收光再传递到法术前摇；太阿/碎星只表现护盾崩解；裂地是目标落点贴地冲击；断岳是沉重迟滞；开天是可打断蓄势后单次重斩；贯日是短直线贯穿和递减命中；玄龙/龙渊按层数显势，不把全部技能做成换色飞线。','','低阶只做简洁命中辨识；高阶可强化轮廓、蓄势、释放、命中、收势，避免遮挡目标和大面积屏幕覆盖。表现层不得决定伤害、冷却、扣费、碰撞或奖励；复用EquipmentSkillEvent生命周期钩子，缺少的普通武器表现事件需后续由动作制作阶段补接。','','已有实现限制：承影的“成功闪避”回调目前没有正式闪避系统调用，不能把该条件写成随普通移动就触发；应等待合法闪避机制接入。高级条件表中超过该武器合法最高阶的条目仅保留大纲设计，不对玩家宣称已开放。','','本轮没有完成新动作、Photon特效或音效，沿用用户交由GPTT6A完成的分工。']
(project/'docs/武器技能设定与表现交接_20261023.md').write_text('\n'.join(lines)+'\n',encoding='utf-8')
print('Weapon skill settings: 30 ordinary themes + Qitian, derived from actual recipe tier bounds')
