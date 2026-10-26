"""Shared authoring contract: ticks, active windows and choreography per actual Boss skill."""
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
MODELS={'battle_general_soul':'bai_zhan_jiang_hun','meteor_general':'xing_yun_tian_jiang','spirit_ape_king':'tong_ling_yuan_wang','xuantians_remnant':'xuan_tian_cheng_zhu','cloud_sea_lord':'yun_hai_tian_jun','five_element_warden':'wu_xing_zhen_yu_shou'}
# id, Chinese name, shape, choreography, windup, active ticks, radius/reach, damage multiplier.
ROWS={
'battle_general_soul':[
 ('formation_charge','断阵冲锋','charge','charge',24,24,9,1.3),
 ('banner_command','战旗号令','banner','command',26,60,6,0),
 ('blade_wheel','百战刀轮','ring','sweep',28,28,5,1.3),
 ('armour_counter','残甲反击','counter','guard',20,24,3.5,1.3),
 ('final_formation','终·将魂断阵','cross','finisher',30,28,12,1.3)],
'meteor_general':[
 ('star_javelin','星陨标枪','projectile','thrust',24,24,14,1.3),
 ('blink_pierce','瞬星穿刺','blink_thrust','spear_pierce',26,18,4.8,1.65),
 ('falling_stars','天穹落星','fall_sequence','summon',28,60,2.3,1.3),
 ('thunder_ring','引雷环','gap_ring','sweep',26,28,5,1.1),
 ('star_track','星轨锁定','line','thrust',30,24,14,1.2),
 ('meteor_guard','陨星护体','guard','guard',20,30,3,0),
 ('final_star','终·星陨归一','gap_ring','finisher',30,32,7,1.3)],
'spirit_ape_king':[
 ('staff_array','金箍棍阵','gap_ring','sweep',28,32,6,1.2),
 ('earth_split','裂地棍击','wave','slam',26,26,9,1.3),
 ('cloud_assault','腾云突袭','leap','leap',24,24,9,1.2),
 ('ape_phantoms','猴王幻影','illusion','command',26,30,5,0),
 ('king_roar','齐天怒吼','fan','roar',28,20,7,1.0),
 ('final_staff','终·定海一棍','line','finisher',30,32,12,1.3)],
'xuantians_remnant':[
 ('sword_array','玄天三震拳','fist_combo','punch',24,30,3.8,.65),
 ('wall_fall','踏城震地','wave','slam',28,30,4,1.3),
 ('soul_phantoms','残魂幻影','illusion','command',24,30,5,0),
 ('city_seal','架城反击','counter','guard',24,30,3.5,1.0),
 ('final_city','终·崩城一拳','charge','punch',30,20,6,1.3)],
'cloud_sea_lord':[
 ('cloud_swords','云海剑潮','lanes','weave',28,32,12,1.2),
 ('water_pressure','天水覆压','pressure','summon',28,40,5,1.3),
 ('cloud_locks','云锁九重','links','weave',30,60,5,0),
 ('mirror_water','镜水幻身','illusion','command',24,30,5,0),
 ('palace_cloud','天宫坠云','fall','slam',30,32,4,1.3),
 ('final_river','终·天河倾落','fall_sequence','finisher',30,60,2.8,1.3)],
'five_element_warden':[
 ('five_breath','五行吐息','fan','breath',26,30,8,1.2),
 ('prison_stomp','镇狱践踏','wave','stomp',28,28,8,1.3),
 ('element_cycle','属性轮转','guard','roar',24,30,3,0),
 ('prison_ring','五行封环','gap_ring','roar',28,32,6,1.2),
 ('final_elements','终·五行归元','fall_sequence','finisher',30,60,2.2,1.3)]}

def catalogue():
    return {boss:[dict(id=i,name=n,shape=s,motion=m,windup=w,active=a,recovery=40,cooldown=600 if i.startswith('final_') else 180,reach=r,multiplier=d,ultimate=i.startswith('final_')) for i,n,s,m,w,a,r,d in rows] for boss,rows in ROWS.items()}

if __name__=='__main__':
    out=ROOT/'src/main/resources/assets/xiantu/boss_combat.json'
    out.write_text(json.dumps(catalogue(),ensure_ascii=False,indent=2),encoding='utf8')
    print(out)
