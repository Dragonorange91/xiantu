"""Authoritative eight-tree production catalog, ordered by material rank."""
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
TREES=[
 ('qing_wen_elm','青纹榆','青纹木',0,20,2,25,'warm pale brown straight grain, gray cyan furrowed bark, fresh green round elm leaves; modest graceful spirit elm'),
 ('chi_xin_birch','赤心桦','赤心木',1,30,4,12,'ivory birch bark with horizontal dark lenticels, red brown heartwood rings, copper orange and green sparse oval leaves'),
 ('zi_wen_maple','紫纹枫','紫纹木',2,45,6,6,'pale gray shallow bark, purple brown flowing wavy grain, crimson purple sharply lobed maple leaves'),
 ('xuan_tie_pine','玄铁松','玄铁木',3,60,8,2.5,'deep graphite vertically fissured bark, dense nearly black brown tough growth rings, dark ink green pointed pine needles; rugged mountain tree'),
 ('jin_si_nanmu','金丝楠','金丝木',4,90,12,1,'brown gray scaly bark, honey brown wood with restrained very fine golden silk grain, deep green broad glossy shaped leaves; Chinese precious nanmu'),
 ('zi_fu_cedar','紫府杉','紫府木',5,120,16,.4,'silver gray fine fissured bark, purple silver distinct rings and straight fine fibers, cyan violet slim cedar needles; subtle ancient cultivation character'),
 ('xing_wen_wutong','星纹梧桐','星纹木',7,180,24,.15,'gray white mottled plane-tree bark, midnight blue brown heartwood with sparse tiny pale star inclusions embedded naturally in grain, green blue broad palmate leaves'),
 ('tian_qiong_sandalwood','天穹檀','天穹木',8,240,32,.05,'silver black deeply split sandalwood bark, rich blue gold branching wood grain with restrained tiny star dust at leaf edges, deep teal oval leaves; celestial precious hardwood, finest elegant detail but no glow')]
TILES=['log_side','log_top','stripped_log_side','stripped_log_top','planks','carved','door_top','door_bottom','trapdoor','window_frame','roof','frieze','column','beam','smooth','plaque_board']
SPRITES=['leaves','sapling','stick','unused_1','unused_2','long_shaft','fiber','unused_3']
